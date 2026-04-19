# 플레이어 플래그 저장소 v0.1 스프린트 계획

## 문서 목적
수도 반경 제한·영지 외 블록 금지·각인 활성화 3시스템이 공통으로 참조할 **플레이어 플래그 저장소 v0.1** 구현 PR 분해. implementation-reviewer 1차 초안.

## 배경
- 현재 `PlayerQuestHonorState` 자료구조만 존재, **영속성·API·변경 이벤트 전무** (실질적 미구현)
- 수도 반경 제한·영지 외 블록 금지·각인 활성화 3시스템 선행 필수
- v0.1 목표: "읽기·쓰기·변경 이벤트 동작 + 서버 재시작 후 생존"

## 선행 조건 (2026-04-19 갱신)
- **PR0 — FoundationContext 인프라 구멍 3개 메우기 (0.5 man-day, 사용자 승인 2026-04-19)**:
  - (a) `build.gradle.kts` SQLite JDBC 드라이버 의존성 추가.
  - (b) `CommonFoundationBootstrap.onEnable()`에 `DatabaseBootstrapper.initialize()` 호출 훅 삽입.
  - (c) HikariCP 풀 도입(선택, per-call `DriverManager.getConnection()` 대체).
  - 완료 시 `FoundationContext` 주입 완료 상태가 실제 가동 상태로 승격 → PR1 착수 가능.
- **`NoopMigrationEntryPoint` 교체 PR = D-0 선행 머지 필수** — 마이그레이션 러너가 없으면 DDL이 데드코드. **현재 단계 = 착수 전 확정(2026-04-19 자문 응답)**. 러너 후보(Flyway / Liquibase / 자체 구현) 사용자 결정 대기. **비상안**: PR1 내부 `CREATE TABLE IF NOT EXISTS` 스텁 + 러너 도입 후 PR1.5로 제거.
- SQLite 연결 풀·`DataSource` 주입 경로가 `FoundationContext`에 존재 **확인 완료(2026-04-19 자문 응답)** — 주입 완료, 단 PR0 인프라 구멍 3개 해소 필요.
- `FlagKey` 네임스페이스 규약 합의 (예: `quest.capital.reach_radius` / `estate.build_permission` / `engraving.slot.activated` 도메인.카테고리.키 3단)
- 변경 이벤트 방식 결정 (Bukkit Event vs 내부 이벤트 버스) — **자문 권장안 도착, 사용자 최종 결정 대기** (`poro_flag_store_v01_consultation_response_v1.md` 질문 4 참조)

## 스키마 제한 (v0.1)
- `FlagValueType` enum: **BOOL / LONG / STRING만** 허용 (JSON은 v0.2)
- 플레이어 단위 단일 스레드 가정 (set 호출 직렬화 규약 코드 주석 명시)
- write-through 모드 (즉시 DB 쓰기)

---

## PR 8개 분해 (총 10~12 man-day)

### PR1 — SQLite 스키마 DDL + 마이그레이션 등록 (1 man-day)
- `player_flags(player_uuid, flag_key, value_type, value_text, value_long, updated_at, version)` 테이블
- 복합 PK `(player_uuid, flag_key)`, `updated_at` 인덱스
- `NoopMigrationEntryPoint` 교체 러너에 등록만

**테스트**: DDL 재실행 idempotent / 복합 PK 중복 거부 / 인덱스 생성 확인

### PR2 — FlagKey 검증기 + 타입 정의 (1 man-day)
- `FlagKey` value object, `FlagValueType` enum
- 네임스페이스 형식 검증, 길이 64자 제한, 금지 문자
- 순수 단위 테스트 가능, Bukkit 의존 없음

**테스트**: 정상 키 통과 / 금지 문자 거부 / 빈 문자열·64자 초과 거부

### PR3 — PlayerFlagRepository JDBC (1~1.5 man-day)
- `findByPlayer(UUID)` / `upsert(UUID, FlagKey, FlagValue)` / `delete(UUID, FlagKey)` / `findByFlag(FlagKey)` (디버깅)
- 메서드 단위 트랜잭션
- `Result<T>` 패턴 기존 코드 스타일

**테스트**: upsert insert·update 양쪽 커버 / Optional.empty / 트랜잭션 롤백

### PR4 — PlayerFlagStore 도메인 파사드 + 인메모리 캐시 (1~1.5 man-day)
- 플레이어 입장 시 전체 로드, 로그아웃 시 캐시 제거
- `get/set/remove/has` API만 공개, Repository 감춤
- `ConcurrentHashMap` 1개 (TTL 정책은 v0.2)
- `batchSet(Map<FlagKey, FlagValue>)` 포함 (각인 일괄 변경 대비)

**테스트**: DB→캐시 적재 / set·get 값 일치 / 로그아웃 시 캐시 제거

### PR5 — Lifecycle Hooks (0.5~1 man-day)
- `PlayerJoinEvent` → Store.load (메인 스레드 동기 로드, v0.1)
- `PlayerQuitEvent` → Store.flush + evict
- 비동기 로드는 v0.2 (100~200명 규모에서 join 지연 체감 없음)

**테스트**: Join 훅 1회만 / Quit 후 재입장 재로드 / 예외 시 플레이어 입장 자체는 막지 않음

### PR6 — PlayerFlagChangedEvent 발행 (0.5 man-day)
- `PlayerFlagChangedEvent(player, flagKey, oldValue, newValue, source)` Bukkit 이벤트
- `Store.set/remove`에서 트랜잭션 성공 후 발행
- 후속 3시스템 단일 구독 진입점

**테스트**: set 성공 시만 발행 / oldValue·newValue 정확성 / 동일 값 set 시 발행 정책

### PR7 — OP 디버그 커맨드 (1.5 man-day)
- `/poroflag get <player> <key>` / `set <player> <key> <type> <value>` / `list <player>` / `clear <player> <key>`
- 권한: `poro.admin.flag`
- **v0.1 필수** (없으면 후속 디버깅 불가)

**테스트**: OP 거부 / 잘못된 타입 에러 메시지 / list 페이지네이션

### PR8 — 3개 도메인 파사드 얇은 래퍼 (1.5~2 man-day)
- `QuestFlagFacade` / `EstateFlagFacade` / `EngravingFlagFacade`
- 각 도메인 `FlagKey` 상수 + 1~2개 핵심 메서드
- 나머지는 각 시스템 구현 PR에서 증식

**테스트**: 상수 중복 없음 / 타입 캐스팅 오류 / 네임스페이스 격리

---

## 의존성 그래프 · 머지 순서
```
PR1(DDL)   PR2(FlagKey)
   \          /
    PR3(Repository)
         |
     PR4(Store)
      /      \
  PR5(Hooks) PR6(Event)
         \   /
         PR7(Command)
              |
         PR8(파사드)
```

권장 순서: **PR1 → PR2 → PR3 → PR4 → PR5 → PR6 → PR7 → PR8**
- PR1·PR2 병렬 작업 가능, 머지는 PR1 먼저
- PR5·PR6 PR4 머지 후 병렬
- 2명 풀타임 병렬 시 **6~8 영업일** 단축 가능

## 공수 요약
- PR1~PR7 = 8~10 man-day
- PR8 = 1.5~2 man-day
- **합계 10~12 man-day**, 1명 풀타임 **2~2.5주**

## 후속 3시스템 통합 순서 (플래그 저장소 v0.1 머지 = D-day 기준)
1. **수도 반경 제한** (D+0 즉시, 1순위) — 읽기 위주, 가장 단순
2. **영지 외 블록 금지** (D+0~) — WorldGuard·커스텀 권역 판정 선행 조사 필요
3. **각인 활성화** (가장 늦게) — 각인 UI·슬롯 모델 별도 준비 필요

## 리스크
- **마이그레이션 러너 부재**: `NoopMigrationEntryPoint` 교체 지연 시 PR1 블로킹 → 비상안: PR1에 `CREATE TABLE IF NOT EXISTS` 직접 실행 스텁, 교체 PR 머지 시 **PR1.5로 스텁 제거**
- **Value 직렬화 스키마 폭주**: v0.1은 BOOL/LONG/STRING만 허용, JSON은 v0.2 (마이그레이션 비용 폭증 방지)
- **동시성**: v0.1 `version` 컬럼 존재하되 실제 낙관적 락은 v0.2. 플레이어 단위 단일 스레드 가정
- **이벤트 폭주**: 각인 일괄 변경 시 N개 발행 허용, `batchSet` API로 v0.2 배치 이벤트 전환 대비
- **로그아웃 중 쓰기 손실**: write-through로 원천 차단

## 오픈 질문 (2026-04-19 자문 응답 `poro_flag_store_v01_consultation_response_v1.md` 도착, 사용자 권장안 전체 수용)
- ~~`FoundationContext`에 SQLite `DataSource`/`ConnectionProvider` 실제 주입 상태~~ **확정** — 주입 완료 + PR0(0.5 md, 2026-04-19 승인)로 인프라 3구멍 해소 예정.
- ~~`NoopMigrationEntryPoint` 교체 PR 현재 단계~~ **확정 (2026-04-19 사용자 승인)** — 상태 = 착수 전. **비상안 채택**: PR1 내부 `CREATE TABLE IF NOT EXISTS` 스텁 + 러너 도입 후 PR1.5로 제거. 러너 최종 선택(Flyway/Liquibase/자체)은 PR1.5 착수 시점 별도 결정.
- ~~`FlagValueType`에 JSON 허용 여부~~ **확정 (2026-04-19 사용자 승인) — C안 채택**: v0.1 3종(BOOL/LONG/STRING) 유지 + **v0.2 JSON 허용 PR을 v0.1 머지 + 2주 내 착수로 마스터플래닝에 예약**. 각인 활성화는 v0.2 JSON 도입 후 착수. v0.2 예약분: `FlagValueType.JSON` + `value_json TEXT` 컬럼 ADD 마이그레이션 + Store/Repo JSON 저장·전체 읽기 지원(부분 인덱싱 X).
- ~~변경 이벤트 방식 (Bukkit `Event` vs 내부 이벤트 버스)~~ **확정 (2026-04-19 사용자 승인) — A안 채택**: Bukkit `Event`. `PlayerFlagChangedEvent(player, flagKey, oldValue, newValue, source)` 동기 메인스레드 발행, 트랜잭션 성공 직후 `Bukkit.getPluginManager().callEvent(...)`. `batchSet`은 N개 개별 발행(v0.1). `PlayerFlagBatchChangedEvent`는 각인 도입 시점 v0.2 예약. 동일 값 set 시 미발행 규약(이벤트 폭주 방지).

## 다음 추천 작업
- **PR1~PR2 브랜치 동시 착수** (스펙 1페이지 선합의: FlagKey 규약 + 테이블 스키마)
- `NoopMigrationEntryPoint` 교체 PR 현재 상태 확인 후 본 스프린트 착수 시점 결정
- `master planning` 에 "플래그 저장소 v0.1 스프린트" 착수 상태 섹션 추가

## 상위 문서 참조
- 수도 반경 제한: `../poro_master_planning.md` 수도 반경 블록 제한 섹션
- 영지 외 블록 금지: `../poro_master_planning.md` 영지 외 블록 설치·제작 금지 섹션
- 각인 활성화: `../poro_master_planning.md` 각인 시스템 섹션
