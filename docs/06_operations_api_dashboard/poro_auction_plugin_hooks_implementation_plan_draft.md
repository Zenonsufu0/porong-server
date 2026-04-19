# 경매 플러그인 훅 4종 시그니처 및 구현 계획 v1

> **상태**: 2026-04-19 implementation-reviewer 1차 초안.
> **선행 승인**: 2026-04-19 사용자 후속 작업 승인. 선행 문서 `poro_auction_monitoring_dashboard_schema_draft.md` 405행 스키마.
> **대상**: 경매 시세 모니터링 대시보드 v1 스키마에 원천 3테이블(`auction_listing` / `auction_bid_log` / `auction_transaction`)이 누락 없이 적재되도록, 경매 플러그인 훅 4종(등록·입찰·낙찰·유찰)의 시그니처와 PR 분해를 확정한다.
> **범위 경계**: 집계 테이블 배치·디스코드 캐시·비정상 감지 쿼리는 본 문서 범위 밖(스키마 문서 §2~§5가 담당).

---

## 1. 목표

1. 경매 플러그인이 원천 3테이블에 무손실 append하도록 훅 4종 시그니처를 코드 표면 수준에서 확정한다.
2. `axis`(equip/cosmetic/material/consumable) + `cosmetic_source`(direct_buy/lootbox/event/ranker) 이중 키의 **해결 시점**을 훅 단계별로 고정한다.
3. Bukkit Event 기반(플래그 저장소 Q4 A안)으로 이벤트 네이밍을 통일해, 디스코드 브리지·웹 어댑터가 동일 경로로 구독 가능하게 한다.
4. 1차 릴리즈 범위를 6~8 PR로 분해하고 플래그 저장소 PR0(SQLite 드라이버·initialize 호출) 선행 조건을 명시한다.
5. 경매 플러그인 다운·비동기 순서 역전·axis 판정 실패에 대한 fallback 정책을 사전에 못박는다.

---

## 2. 선행조건

### 2-1. 필수 선행(블로킹)
- **플래그 저장소 PR0 머지 완료**
  - `build.gradle.kts`에 `org.xerial:sqlite-jdbc` `runtimeOnly` 추가.
  - `EmpireRPGPlugin.onEnable()`에서 `foundationContext.databaseBootstrapper().initialize()` 호출 추가.
  - 근거: `FoundationContext`의 `ConnectionProvider` + `TransactionHelper` + `DatabaseBootstrapper`를 그대로 재사용해야 하며, 드라이버·initialize 누락 시 경매 훅의 INSERT가 런타임 `ClassNotFoundException`·테이블 미존재로 실패한다. (근거 문서: `poro_flag_store_v01_consultation_response_v1.md` §질문1)
- **경매 플러그인 실물 존재 여부 확인**
  - `custom-plugins/empire-rpg` 내에 경매 서비스·핸들러 클래스가 아직 **없다**(2026-04-19 `wsl-setup` 브랜치 기준). 본 계획은 "경매 플러그인 스켈레톤 PR" 자체를 PR1에 포함하는 구조이며, "기존 경매 플러그인에 훅을 붙이는" 구조가 아니다. 이 전제가 사용자 결정과 일치해야 한다.
- **`wallet_log` 테이블·월렛 서비스 1차 존재**
  - 스키마 쿼리 5-4(확률 버프 조인)는 `wallet_log.reason='estate_lv_bonus'` 행이 존재해야 작동. 월렛 서비스 자체는 본 PR 범위 밖이지만, PR6(월렛 훅 확장) 착수 전 월렛 스키마가 존재해야 한다. **없으면 PR6는 대기**.

### 2-2. 결정 대기(논블로킹, 기본값 적용)
- 아이템 메타데이터에 `axis`·`cosmetic_source`가 NBT 태그로 박혀 있는지, 혹은 외부 카탈로그(CSV/DB)에서 `item_code`로 조회해야 하는지. **기본값**: 카탈로그 조회 우선(§6 참조), NBT는 보조.
- 낙찰 수수료율 `fee_rate` 시즌 중 변경 가능 여부. **기본값**: v1에서는 고정값(`config.yml`의 단일 상수), 스키마 오픈 질문 5번에 따라 v2에서 `fee_rate_snapshot` 컬럼 검토.
- 랜덤박스 개봉 로그(`lootbox_open_log`) 존재 여부. **기본값**: 1차는 `cosmetic_source`를 **카탈로그 정적 태그**로 처리(아이템 자체가 "이 아이템은 랜덤박스 유입 아이템이다"를 나타냄). 개봉 이력 연결은 2차.

---

## 3. 훅 4종 시그니처 (Bukkit Event 표준)

### 3-1. 네이밍 규약
- 모든 이벤트는 `com.poro.empire.auction.event` 패키지, `extends org.bukkit.event.Event`, `HandlerList` 정적 필드 1개.
- 클래스명 규약: `Auction<Phase>Event` — 경매 도메인 전용 접두어 `Auction` 고정.
- 기존 EmpireRPG 리스너 패턴(`HealthHudListener` 등) 및 플래그 저장소 `PlayerFlagChangedEvent`와 동일한 규약.
- 이벤트 발행 시점: **DB 트랜잭션 성공 직후**. 실패 시 미발행(트랜잭션 롤백과 이벤트 발행이 어긋나지 않게 한다).
- `@Cancellable`는 사용하지 않음(사후 통지 전용, 경매 진행 중단은 서비스 레이어에서 분리 처리).

### 3-2. 인터페이스 선언 (시그니처만)

```java
// AuctionListingCreatedEvent (등록 시점, 트랜잭션 성공 직후)
public record AuctionListingPayload(
    long listingId, UUID sellerUuid, String itemCode, String itemName,
    String axis, String categoryPath, String rarity, String cosmeticSource,
    int quantity, long startPrice, Long buyoutPrice,
    Instant listedAt, Instant expiresAt, int seasonId) {}

public class AuctionListingCreatedEvent extends Event { AuctionListingPayload payload; ... }
public class AuctionBidPlacedEvent extends Event {
    long listingId; UUID bidderUuid; long bidPrice; boolean isBuyout; Instant bidAt; ... }
public class AuctionTransactionCompletedEvent extends Event {
    long txId; long listingId; UUID sellerUuid; UUID buyerUuid;
    long finalPrice; long feeAmount; long netToSeller; Instant settledAt;
    String axisSnapshot; String cosmeticSourceSnapshot; int seasonId; ... }
public class AuctionListingExpiredEvent extends Event {
    long listingId; UUID sellerUuid; Instant closedAt; String reason; ... }
```

### 3-3. 파라미터 근거와 axis 해결 시점

| 이벤트 | 파라미터 선정 근거 | `axis`/`cosmetic_source` 해결 |
|---|---|---|
| `AuctionListingCreatedEvent` | `auction_listing` INSERT 1행에 필요한 모든 컬럼을 payload 안에 포함(스냅샷). 집계 재빌드 시 원본 재현 가능. | **등록 시점 1회 해결**. 등록 서비스가 아이템 카탈로그에서 `axis`·`category_path`·기본 `cosmetic_source`를 조회 후 `auction_listing` 행에 저장. 이후 낙찰·유찰 단계에서는 이 행을 참조만 한다. |
| `AuctionBidPlacedEvent` | `auction_bid_log` INSERT 1행에 최소 필드. `is_buyout` 플래그로 즉시구매·일반 입찰 구분. | 해결 안 함(입찰 로그에는 axis 컬럼 없음, `listing_id` JOIN으로 충분). |
| `AuctionTransactionCompletedEvent` | `auction_transaction` INSERT + `auction_listing.state='sold'` UPDATE 양측에 필요한 필드. `axisSnapshot`/`cosmeticSourceSnapshot`은 **집계 리스너에서 조인 비용을 줄이기 위한 사본**. | **낙찰 정산 시 스냅샷 복제**. 등록 시점 값과 동일하지만, 집계 배치가 `auction_listing` 삭제/시즌 격리된 상황에서도 동작하도록 이벤트 페이로드에 동봉. |
| `AuctionListingExpiredEvent` | 유찰은 `auction_listing.state='expired'` UPDATE 1건만 필요. `reason`은 `timeout`(시간 만료) / `canceled`(판매자 취소) 2종 분리. | 해결 안 함(등록 시점 값 재사용). |

### 3-4. 이벤트 발행 타이밍 (트랜잭션 경계)

발행 순서는 반드시 **트랜잭션 커밋 직후 동기 발행**. 메인스레드가 아닐 가능성이 있는 경우(PaperMC 비동기 채팅/네트워크 계통 등)는 `Bukkit.getScheduler().runTask(plugin, () -> callEvent(...))`로 메인스레드 호출을 강제한다. 이는 플래그 저장소 Q4 A안 권장과 동일한 정책.

---

## 4. 작업 분해 (PR 그래프)

총 **7 PR / 12.5 man-day**. 모든 PR은 플래그 저장소 PR0 머지 후 착수 가능.

### PR-A1. 경매 도메인 스켈레톤 (2.0 md)
- 의존성: 플래그 저장소 PR0 완료.
- 범위: `com.poro.empire.auction` 패키지 신설. `AuctionService`(인터페이스), `AuctionRepository`(JDBC), 도메인 레코드(`AuctionListing`/`AuctionBid`/`AuctionTransaction`) 최소 골격.
- DDL: `auction_listing`·`auction_bid_log`·`auction_transaction` 3테이블 `CREATE TABLE IF NOT EXISTS`. 플래그 저장소 PR1 스텁과 동일한 비상안 패턴(§질문2 권장안) 따라감. `FlagStoreSchemaStub`에 이어 `AuctionSchemaStub`으로 분리.
- 산출: 컴파일·기본 JUnit 동작. 기능은 아직 없음.

### PR-A2. 아이템 카탈로그 조회 어댑터 (1.5 md)
- 의존성: PR-A1.
- 범위: `ItemCatalogLookup.resolveAxis(itemCode) → AxisResolution(axis, categoryPath, rarity, cosmeticSource?)` 인터페이스.
- 1차 구현: `resources/auction/item_axis_catalog.csv` 파일 기반 정적 로더. 컬럼 = `item_code, axis, category_path, rarity, cosmetic_source(nullable)`.
- Fallback 정책: 카탈로그 미등록 item_code는 `axis='unknown'` + 경고 로그 + `auction_listing.axis='unknown'` 행 기록(삽입 차단하지 않음 — 스키마 §371 치장 축 NULL 검증과 별개, 미등록 축도 대시보드에서 "분류 실패 카운트"로 감시).

### PR-A3. 훅 4종 이벤트 클래스 + 발행 지점 (1.5 md)
- 의존성: PR-A1.
- 범위: §3-2 시그니처 그대로 4 이벤트 클래스 추가. 각 Event에 `HandlerList` + payload 레코드 포함. `AuctionService` 구현부에서 트랜잭션 커밋 직후 `Bukkit.getPluginManager().callEvent(...)` 호출 1줄씩.
- 테스트: `MockBukkit` 기반 리스너 호출 카운트 테스트 4종.

### PR-A4. 원천 3테이블 INSERT/UPDATE 리스너 (2.0 md)
- 의존성: PR-A2, PR-A3.
- 범위: `AuctionFactTableListener` 1개 클래스가 4 이벤트 모두 구독. 각 이벤트마다 `TransactionHelper.inTransaction(...)`으로 해당 테이블에 INSERT/UPDATE.
- **중요**: 이 리스너의 트랜잭션은 서비스 레이어 트랜잭션과 **분리**된다. 서비스가 먼저 커밋, 그 후 이벤트가 발행되고, 리스너가 자기 트랜잭션에서 INSERT. 즉 이 리스너가 실패해도 경매 자체는 성립한다. 실패 시 `ops_dead_letter`(별도 테이블, PR-A7에서 신설)에 원본 payload JSON 저장.
- 테스트: 등록→입찰→낙찰 시나리오 통합 테스트에서 3테이블에 행이 쌓이는지 검증.

### PR-A5. 월렛 훅 확장 (`estate_lv_bonus` reason) (1.5 md)
- 의존성: 월렛 서비스 1차 존재(본 PR 외부).
- 범위: 기존 월렛 훅(존재 시) 또는 월렛 서비스의 `credit()` 시그니처에 `reason` 파라미터 추가. `reason='estate_lv_bonus'` 상수는 `WalletReasons` 상수 클래스로 분리. **별도 Bukkit 이벤트는 신설하지 않음**(월렛 변경은 append-only 로그 테이블 직접 기록으로 충분, 이중 채널 방지).
- 근거: 확률 버프 보너스 발생 경로는 제작 시스템이 호출하는 `WalletService.credit(player, currency, qty, reason='estate_lv_bonus', sourceRef)` 1지점뿐. 새로운 이벤트 채널을 만들면 기존 wallet_log append와 Bukkit 이벤트 2경로가 생겨 감사 역산이 어려워진다.
- 테스트: 가공·연금·요리 보너스 발생 시 `wallet_log.reason='estate_lv_bonus'` 행 기록 검증, 스키마 쿼리 5-4 재현.

### PR-A6. Dead Letter + 감사 로그 (1.5 md)
- 의존성: PR-A4.
- 범위: `ops_auction_dead_letter(id, event_type, payload_json, failed_at, error_code, retry_count)` 테이블 신설. `AuctionFactTableListener` 실패 경로가 여기로 기록. 수동 재처리 명령어 `/porops auction replay <id>` 1개.
- 1차 범위: 자동 재시도 없음(수동 재처리만). 자동화는 2차.

### PR-A7. 통합 테스트 + 지표 파이프 검증 (2.5 md)
- 의존성: PR-A4, PR-A5, PR-A6.
- 범위: 등록→입찰(×3)→낙찰→집계 배치(수동 호출)→지표 쿼리 5-1~5-4 전 과정 엔드투엔드 테스트. 유찰 시나리오 별도. 치장 축 `cosmetic_source` NULL 누락 케이스 음성 테스트.
- 성공 기준: 스키마 §371~373 테스트 포인트 5종 통과.

### 의존성 그래프
```
FlagStore PR0 (0.5 md, 선행)
    │
    ▼
PR-A1 (2.0) ──► PR-A2 (1.5) ──► PR-A4 (2.0) ──► PR-A6 (1.5) ──► PR-A7 (2.5)
    │                               ▲
    └──► PR-A3 (1.5) ────────────────┘
                                  PR-A5 (1.5, 독립 트랙)
```

PR-A5는 월렛 서비스 존재가 전제라 경매 트랙과 병행 가능. 전체 크리티컬 패스는 PR-A1 → PR-A2 → PR-A4 → PR-A6 → PR-A7 = 9.5 md, 병행 포함 총 12.5 md.

---

## 5. 기술 리스크

### R1. 경매 플러그인(=EmpireRPG 경매 모듈) 다운 시 원천 테이블 누락
- 현상: 서버 크래시 도중 `auction_listing` INSERT는 성공했는데 `auction_bid_log`는 실패한 상태로 남음 → 집계 재빌드 시 데이터 불일치.
- 완화: (a) 서비스 레이어 트랜잭션이 `listing+bid+transaction` 중 필요한 것만 **단일 트랜잭션에 묶는다**. 즉 낙찰 정산은 `auction_transaction` INSERT + `auction_listing` UPDATE를 한 트랜잭션에서. (b) 이벤트 발행 리스너(`AuctionFactTableListener`)는 별도 트랜잭션이지만 실패 시 `ops_auction_dead_letter`로 격리, 수동 재처리. (c) 서버 콜드 스타트 시 `state='active'`인데 `expires_at < now()` 행을 스캔해 자동 `state='expired'`로 정리하는 정리 루틴을 PR-A1에 포함.

### R2. 비동기 write 순서 역전
- 현상: `AuctionBidPlacedEvent`가 빠르게 연속 발생하면 리스너 트랜잭션이 역순으로 커밋되어 `bid_at` 정렬과 실제 INSERT 순서가 다름.
- 완화: 리스너 내부에 **단일 쓰레드 executor**(`Executors.newSingleThreadExecutor()`) 1개 전용 사용. 이벤트 수신은 메인스레드에서, DB 쓰기는 이 단일 쓰레드에서 직렬. 입찰 피크(분당 500건)까지는 SQLite WAL과 이 단일 쓰레드로 충분(스키마 §373 동시성 스트레스 테스트와 정합).

### R3. axis 판정 실패 fallback
- 현상: 카탈로그에 없는 신규 아이템이 경매에 올라감 → `axis='unknown'` 행이 집계 테이블에 편입되면 대시보드 축이 오염.
- 완화: (a) 등록 서비스에서 `axis='unknown'`이면 경고 로그 + 관리자 디스코드 알림 1건. (b) 집계 배치의 `auction_category_summary` 빌드 쿼리에 `WHERE axis != 'unknown'` 추가(미분류는 집계에서 제외, 원천 테이블에는 남김). (c) 운영자 대시보드에 "미분류 매물 수" 카드 추가(3-7 유찰률 카드와 한 줄로).

### R4. `cosmetic_source` NULL 누락 역편입
- 현상: 장비·재료 행인데 실수로 `cosmetic_source` 값이 들어감 → 치장 집계로 잘못 편입. 스키마 §371 동일 리스크.
- 완화: PR-A2의 `AxisResolution`에서 `axis != 'cosmetic'`이면 `cosmeticSource`를 **강제 NULL**로 고정. INSERT 직전 체크 제약 추가(`CHECK (axis = 'cosmetic' OR cosmetic_source IS NULL)`).

### R5. 플래그 저장소 PR0 미완 상태에서 PR-A1 착수
- 현상: 드라이버·initialize 누락으로 `AuctionSchemaStub`이 실행되지 않아 테이블 부재.
- 완화: 본 문서 §2-1에 **블로킹 의존성**으로 명시. PR-A1 머지 조건에 "PR0 머지 확인" 체크박스.

### R6. 이벤트 발행 시점과 트랜잭션 실패의 괴리
- 현상: 서비스 트랜잭션 롤백됐는데 이벤트가 이미 발행되어 리스너가 없는 행에 UPDATE 시도 → 예외.
- 완화: 서비스 레이어에서 **커밋 성공 반환 후에만** `callEvent(...)` 호출. `TransactionHelper.inTransaction(...)`의 `Result<T>`가 success일 때만 이벤트 발행. 실패 시 이벤트 생성 자체 안 함.

---

## 6. 치장 축 판정 로직 상세

### 6-1. 훅 파라미터 vs 메타데이터 유도
- **결정**: 훅 파라미터로 직접 받지 **않는다**. 등록 서비스가 `ItemCatalogLookup`으로 조회한 결과를 `auction_listing` INSERT 시 확정 저장하고, 이벤트 페이로드에는 **이미 확정된 값**을 담는다.
- 근거: 클라이언트가 보내는 `axis`/`cosmetic_source`를 신뢰하면 조작 여지가 생긴다. 카탈로그(서버 소유)가 유일 소스.

### 6-2. 카탈로그 스키마 (1차, CSV)
```csv
item_code, axis, category_path, rarity, cosmetic_source
t1_sword_s1_v1, equip, equip/t1/weapon, common, 
t2_sword_s1_v1, equip, equip/t2/weapon, epic, 
enhance_stone_t1, material, material/enhance/t1/stone, common, 
enhance_stone_t2, material, material/enhance/t2/stone, rare, 
cosmetic_skin_weapon_sword_red, cosmetic, cosmetic/skin/weapon, rare, direct_buy
cosmetic_skin_weapon_sword_gold_box, cosmetic, cosmetic/skin/weapon, epic, lootbox
```
- `cosmetic_source`는 `axis='cosmetic'`인 행에만 값, 나머지는 공란(NULL).
- `direct_buy` / `lootbox` 2종 1차, `event` / `ranker`는 2차.

### 6-3. NBT 보조 판정 (2차)
- 1차는 `item_code` 단일 키. 2차에 NBT `poro:axis`, `poro:cosmetic_source` 태그를 참조해 카탈로그와 교차 검증. 불일치 시 카탈로그 우선 + 경고 로그.

---

## 7. 테스트 포인트

### 7-1. 단위 테스트 (훅 발동 시점)
- 등록 트랜잭션 성공 → `AuctionListingCreatedEvent` 1회 발행. 실패(롤백) → 미발행.
- 입찰 트랜잭션 성공 → `AuctionBidPlacedEvent` 1회. 즉시구매 시 `isBuyout=true` 여부.
- 낙찰 정산 → `AuctionTransactionCompletedEvent` 1회 + `auction_listing.state='sold'` UPDATE 동일 트랜잭션 커밋 확인.
- 유찰 `timeout`·`canceled` 2케이스 각각 `AuctionListingExpiredEvent` 발행, `reason` 값 검증.

### 7-2. 통합 테스트 (등록→입찰→낙찰→지표)
- 시나리오: T2 강화석 10개 등록 → 3명 입찰 → 낙찰 정산 → 수동 배치 → `auction_daily_summary` 해당 행 확인.
- 유찰 시나리오: 등록 후 `expires_at` 경과 → 유찰 → 집계에서 `expired_count` 증가.
- 치장 시나리오: `direct_buy`와 `lootbox` 동일 item 2건 등록 → 낙찰 → `auction_cosmetic_summary`의 `source_ratio` 계산 검증.
- 확률 버프 조인: 제작 보너스 발생 → `wallet_log.reason='estate_lv_bonus'` 행 + 쿼리 5-4 결과 조인 가능 여부.

### 7-3. 장애 테스트
- `AuctionFactTableListener` 의도적 예외 → `ops_auction_dead_letter`에 행 기록 + 경매 자체는 성립(등록 성공).
- 서버 강제 종료 → 콜드 스타트 정리 루틴이 `expires_at < now()` 행을 `expired`로 정정.
- 카탈로그 미등록 item → `axis='unknown'` 기록 + 집계 테이블 편입 안 됨 + 관리자 알림 1건.

### 7-4. 동시성 테스트
- 분당 500건 입찰 시뮬(스키마 §373) → 단일 쓰레드 executor 대기열 max depth 측정, WAL 대기 측정.
- 동일 `listing_id`에 3 쓰레드 동시 입찰 → 한 쓰레드만 성공, 나머지는 낙관적 락 실패로 재시도 또는 사용자 피드백.

---

## 8. 1차 버전 권장안

### 포함
- 훅 4종 이벤트 클래스 + 발행 지점(PR-A3).
- 원천 3테이블 INSERT/UPDATE 리스너(PR-A4).
- 아이템 카탈로그 CSV + `axis`/`cosmetic_source` 정적 해결(PR-A2).
- 월렛 `reason='estate_lv_bonus'` 파라미터화(PR-A5).
- Dead letter + 수동 재처리 명령어(PR-A6).
- 통합 테스트 + 단위 테스트(PR-A7).

### 수치 목표
- 원천 3테이블 append 성공률 99.5% 이상(나머지 0.5%는 dead letter로 격리 + 수동 재처리).
- 이벤트 발행 지연(트랜잭션 커밋 → `callEvent` 호출) 평균 10ms 이하.
- 카탈로그 미등록 item 발생률 1% 미만(목표: 0%).

---

## 9. 나중으로 미뤄도 되는 것

### 2차 이후
- **자동 dead letter 재처리** — 1차는 수동 명령어. 자동 재시도는 exponential backoff + 최대 N회 제한 설계 필요해 별도 PR.
- **`cosmetic_source='event'` / `'ranker'` 지원** — 1차는 `direct_buy`/`lootbox` 2종. 이벤트 보상·시즌 랭커 보상의 유입 경로 설계가 선행돼야 함.
- **NBT 기반 2차 판정** — 1차는 카탈로그 CSV 단일 소스. NBT 교차 검증은 카탈로그가 충분히 안정된 후.
- **`AuctionBidBatchPlacedEvent`** — 1차는 단건 발행 N회. 플래그 저장소 Q4의 `PlayerFlagBatchChangedEvent`와 동일한 문제를 경매가 재현한다면 2차에 배치 이벤트 추가.
- **`fee_rate_snapshot` 컬럼** — 수수료율 시즌 중 변경 허용 결정이 나면 `auction_transaction`에 컬럼 추가. 스키마 오픈 질문 5번과 연동.
- **랜덤박스 개봉 로그 연동** — `lootbox_open_log` 테이블이 생기면 `cosmetic_source='lootbox'` 판정을 카탈로그 정적 태그가 아닌 실제 개봉 이력 기반으로 전환. 스키마 오픈 질문 1번과 연동.
- **`auction_listing`의 `enhance_lv`/`potential_grade` 컬럼 분리** — 스키마 오픈 질문 2번. 1차는 `item_code`에 인코딩.
- **실시간 이벤트 스트림(Kafka/Redis Stream)** — 스키마 §42 비범위. 1차는 5분 캐시 + 자정 배치.

---

## 10. 오픈 질문

1. 경매 플러그인 실물이 현재 브랜치에 존재하지 않는다는 전제가 맞는가? (본 계획은 "신규 경매 모듈을 EmpireRPG 내에 추가"하는 구조. 별도 외부 플러그인을 붙일 계획이면 이벤트 브리지 설계 추가 필요)
2. 아이템 카탈로그 CSV의 **관리 주체**는 누가인가? 리소스팩 CustomModelData 레지스트리와 통합할지, 별도 관리할지.
3. 낙찰 수수료율 시즌 중 변경 허용 여부 — v1 고정값 유지가 맞는지 사용자 확정.
4. Dead letter 수동 재처리 권한은 운영자 한정인가 개발자도 허용인가. `/porops auction replay` 권한 노드 정의.
5. `AuctionFactTableListener`의 단일 쓰레드 executor를 플러그인 전역으로 공유할지, 경매 전용으로 분리할지. 월렛 로그 append와 경쟁할 경우 분리가 안전.

---

## 이번에 정리한 핵심
- 훅 4종을 Bukkit Event(`AuctionListingCreatedEvent`/`AuctionBidPlacedEvent`/`AuctionTransactionCompletedEvent`/`AuctionListingExpiredEvent`)로 확정, 발행 시점은 서비스 레이어 트랜잭션 커밋 직후 동기.
- `axis`/`cosmetic_source`는 **등록 시점 1회 해결**, 이후 단계는 참조·스냅샷 복제.
- 월렛 `estate_lv_bonus`는 **별도 이벤트 대신 `credit()` reason 파라미터**로 해결, append 경로 단일화.
- 7 PR · 12.5 md · 플래그 저장소 PR0 머지 필수 선행.

## 남은 확인 포인트
- §2-1 경매 플러그인 실물 존재 여부(신규 모듈 추가 전제).
- §10 오픈 질문 1·2·3 사용자 결정.
- §2-1 월렛 서비스 1차 존재 여부 → PR-A5 착수 시점 영향.

## 다음 추천 작업
- 본 계획을 사용자가 승인하면 `poro_master_planning.md` 변경 로그에 "경매 훅 4종 시그니처 확정 + 7 PR 분해" 반영.
- PR-A2 아이템 카탈로그 CSV **초기 스캐폴드**(장비 T1/T2 5세트 × 3무기 + 강화석 2종 + 치장 5종) 초안 작성.
- `poro_open_questions_registry.md`에 본 문서 §10 5건 등록.

## 상위 문서 참조
- `poro_auction_monitoring_dashboard_schema_draft.md` — 요구 데이터 축 및 원천 3테이블 DDL.
- `../00_index_and_execution/poro_flag_store_v01_consultation_response_v1.md` §질문1·§질문4 — SQLite 선행 조건, Bukkit Event 권장 근거.
- `../poro_master_planning.md` — 경매·월렛·확률 버프 섹션.
- `../14_world_and_maps/poro_resourcepack_custom_ui_weapon_cosmetic_draft.md` §4 — 치장 귀속 X, 랜덤박스·확정 구매 병행.
- `../../CLAUDE.md` — 구현 계획 작성 원칙.
