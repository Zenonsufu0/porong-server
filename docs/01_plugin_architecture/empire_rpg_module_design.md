# EmpireRPG 모듈 설계 문서

> 작성 기준: 2026-05-20  
> 대상: EmpireRPG 플러그인 (custom-plugins/empire-rpg)  
> 목적: "무엇이 어떻게 구현됐는가"를 기록하는 기술 설계 문서

---

## 1. 전체 구조 개요

```
EmpireRPGPlugin (진입점)
│
├── Foundation Layer
│   ├── CommonFoundationBootstrap → FoundationContext
│   │   ├── SqliteConnectionProvider   (DB 연결)
│   │   ├── CommonPluginLoggerFactory  (로깅)
│   │   └── CommonConfigLoader         (config.yml)
│   └── MasterRegistryBootstrap → MasterRegistryContext
│       ├── ItemMasterRegistry
│       ├── SkillMasterRegistry
│       ├── BossMasterRegistry
│       └── (Quest, Achievement, Region, Town, NPC)
│
├── Engine Layer (각 엔진 독립 부트스트랩)
│   ├── CombatEngineBootstrap → CombatEngineRuntime
│   ├── BossEngineBootstrap   → BossEngineRuntime
│   ├── GrowthEngineBootstrap → GrowthEngineRuntime
│   ├── LifeEngineBootstrap   → LifeEngineRuntime
│   ├── QuestAchievementBootstrap → QuestAchievementRuntime
│   └── OperationsQueryBootstrap  → OperationsQueryRuntime
│
├── Service Layer (상태 관리 서비스)
│   ├── PlayerDataManager          (세션 캐시)
│   ├── GrowthStateStore           (성장 상태 캐시)
│   ├── IslandStorageStore         (영지 저장고 캐시)
│   ├── IslandTerritoryStateStore  (영지 영토 캐시)
│   ├── PlayerPersistenceService   (JSON 저장/로드)
│   ├── ScoreboardService          (스코어보드)
│   ├── HotbarService              (핫바 유지)
│   ├── CombatStateService         (전투 상태 5초)
│   ├── CooldownManager            (스킬 쿨타임)
│   ├── AuthService                (Discord 인증)
│   └── BossRoomManager            (보스 방 관리)
│
├── Listener Layer (26개 이벤트 리스너)
├── Command Layer  (/empire, /verify, /메뉴, /장비, /캐릭터)
└── HTTP Server    (포트 8765, Discord 봇 API)
```

---

## 2. Foundation Layer

### 2.1 CommonFoundationBootstrap

**역할:** DB 연결, 로거, 설정을 초기화해 `FoundationContext`를 생성한다.

**초기화 순서:**
1. `config.yml` 로드 → `CommonConfig`
2. `SqliteConnectionProvider` 생성 (DB 경로: `plugins/EmpireRPG/empire.db`)
3. `DatabaseBootstrapper.migrate()` → DDL 테이블 생성/마이그레이션
4. 로거 팩토리 초기화

**관련 파일:**
```
common/config/CommonFoundationBootstrap.java
common/config/FoundationContext.java
common/db/SqliteConnectionProvider.java
common/db/DatabaseBootstrapper.java
```

### 2.2 MasterRegistryBootstrap

**역할:** CSV 시드 파일을 읽어 마스터 데이터를 메모리에 로드한다.

**로드 순서:**
1. `RegistryBootstrapper`가 `seeds/*.csv`를 읽음
2. 각 `*MasterRegistry`에 upsert

**주요 시드 파일:**

| 파일 | 레지스트리 | 내용 |
|---|---|---|
| `boss_master.csv` | BossMasterRegistry | 보스 ID, 이름, HP, 페이즈 수 |
| `boss_entry_rule.csv` | BossEngineBootstrap | 입장 조건 (레벨, 그룹 크기, 시간 제한) |
| `boss_pattern_seed.csv` | BossEngineBootstrap | 패턴 모듈 수치 (데미지, 범위, 쿨타임) |
| `state_master.csv` | CombatEngineBootstrap | 버프/디버프 상태 정의 |

---

## 3. Combat Engine

### 3.1 개요

무기 스킬 실행, 쿨타임, 리소스, 버프/디버프, 피해 계산을 담당한다.

### 3.2 스킬 실행 흐름

```
플레이어 입력 (좌클릭/우클릭/Shift+우클릭/F)
    │
SkillInputListener (HotbarInteractListener)
    │
SkillService.useSkill(player, inputType)
    ├── 무기 타입 확인 (PlayerData.weaponClass)
    ├── CooldownManager.isOnCooldown() 검사
    ├── ResourceTracker.hasEnough() 검사
    ├── WeaponSkill.execute(player, SkillContext)
    │       ├── 이동/데미지/상태 처리
    │       └── StateApplier.apply(targets, stateId)
    ├── CooldownManager.startCooldown()
    └── ResourceTracker.consume()
```

### 3.3 무기별 스킬 목록

| 무기 | 고유 자원 | 스킬 1 | 스킬 2 | 스킬 3 | 스킬 4 |
|---|---|---|---|---|---|
| 검 | 검세 (3) | FlashSlash | TripleStrike | GuardCounter | FinalStrike |
| 창 | 압박 (5) | Thrust | Crescent | Charge | Thunderstrike |
| 석궁 | 조준 (1) | RapidFire | EvadeFire | PierceBolt | Sniper |
| 망치 | 충격 (3) | Smash | CrushCharge | Unyielding | ColossalDrop |
| 낫 | 그림자 흐름 (3) | DeathSlash | ShadowSpin | GrimStrike | Execution |
| 스태프 | 마력 충전 (5) | ArcaneOrb | ElementalBurst | ArcaneRush | Starburst |

### 3.4 주요 클래스

| 클래스 | 역할 |
|---|---|
| `SkillService` | 스킬 등록/실행 진입점 |
| `CooldownManager` | 스킬 쿨타임 추적 및 진행도 계산 |
| `ResourceTracker` | 무기별 리소스 스택 관리 |
| `CombatStateService` | 전투 상태 5초 유지 (마지막 피해 후) |
| `StateApplier` | 버프/디버프 대상에게 적용 |
| `BuffDebuffService` | 활성 상태 목록 관리 (만료 처리 포함) |
| `CombatFormulaResolver` | 최종 피해량 계산 공식 |
| `WeaponPowerCalculator` | 장비 기반 무기 공격력 계산 |

### 3.5 피해 계산 공식

```
최종 피해 = 기본 공격력 × 스킬 배율 × (1 + 강화 보너스) × (1 + 각인 보너스)
           × 상태 배율 × 치명타 배율 (조건부)
```

피해 공식 상세는 `CombatFormulaResolver.java` 참조.

---

## 4. Growth Engine

### 4.1 개요

장비, 강화, 잠재, 룬, 각인의 성장 데이터를 관리한다.

### 4.2 PlayerGrowthState 구조

```
PlayerGrowthState (UUID별 인메모리)
├── wallet              골드/재화 보유량
├── equipment[6]        장비 슬롯 (무기/갑옷/투구/장갑/신발/반지)
│   └── PlayerEquipmentItem
│       ├── itemId      마스터 아이템 ID
│       ├── enhanceLv   강화 레벨 (0~25)
│       └── potentialProfile 잠재 (4라인)
├── runeSlots[6]        룬 슬롯
├── classEngraving      직업각인 (A 또는 B)
├── commonEngravings[5] 공용각인 (1차 시즌 미사용)
├── heirloom            홍염/세트 보너스
└── xp / level          경험치 / 레벨
```

### 4.3 강화 흐름

```
EnhancementGui 클릭
    │
GrowthGuiListener
    │
EnhancementService.tryEnhance(state, slot)
    ├── EnhancementRuleRegistry.getRule(slot, enhanceLv)
    ├── 성공률 계산 (룰 테이블 기반)
    ├── 비용 차감 (wallet에서 골드 + 강화석)
    ├── 성공: equipment.enhanceLv++
    │   └── 실패: 레벨 유지 (파괴 없음)
    └── GUI 갱신
```

**강화 최대 레벨:** 25강 (시즌 이론 최고)  
**18강:** 일반 유저 현실적 목표  
**22강:** 상위권 구간  
**25강:** ATK 192, 기대 시도 약 3,226회

### 4.4 잠재 흐름

```
PotentialGui → GrowthGuiListener
    │
PotentialService.reroll(state, slot)
    ├── 큐브 비용 차감
    ├── PotentialOptionRegistry에서 등급별 옵션 풀 조회
    ├── 랜덤 4라인 선택
    └── potentialProfile 교체
```

**잠재 등급:** 커먼 → 레어 → 에픽 → 유니크  
**레전더리:** 2차 시즌 후보

### 4.5 주요 클래스

| 클래스 | 역할 |
|---|---|
| `GrowthStateStore` | UUID → PlayerGrowthState 인메모리 캐시 |
| `EnhancementService` | 강화 성공률/비용 계산 |
| `EnhancementRuleRegistry` | 강화 테이블 (등급별 성공률, 비용) |
| `PotentialService` | 잠재 리롤, 등급 상승 |
| `PotentialOptionRegistry` | 잠재 옵션 풀 (스탯 종류, 수치 범위) |
| `RuneService` | 룬 장착/제거 |
| `EngravingService` | 각인 장착 |
| `LevelingService` | 경험치 처리, 레벨업 (공식: 550 × n^1.5) |

---

## 5. Boss Engine

### 5.1 개요

보스 입장 조건 검증, 전투 실행, 패턴 스케줄링, 클리어 보상을 담당한다.

### 5.2 보스 전투 흐름

```
플레이어 BossHubGui → BossLobbyGui → BossRoomListGui
    │
방장: BossCreateGui → BossRoomManager.createRoom()
일반: BossRoomListGui → BossRoomManager.joinRoom()
    │
방장이 시작 버튼 클릭
    │
BossEntryValidator.validate(room)
    ├── 레벨 조건 검사
    ├── 입장 제한 기간 검사
    └── 그룹 크기 검사
    │ (통과 시)
BossRunService.startRun(room)
    ├── BossRun 인스턴스 생성
    ├── 몹 스폰 (MythicMobs 연동)
    ├── BossPatternScheduler 시작
    ├── BossPhaseController 등록
    └── 전투 진행
    │ (클리어 시)
BossResultSummary 생성
BossRewardResolverHook.onRunEnded()
    └── BossPartyAuctionHook → PartyAuctionService
```

### 5.3 파티 경매 흐름

```
보스 클리어
    │
PartyAuctionService.startAuction(party, items)
    │
각 파티원에게 PartyAuctionGui 열기
    │
60초 동안 채팅 /bid <금액> 입력
    │
60초 후 resolveSession()
    ├── 최고 입찰자 → 아이템 지급
    └── 입찰 골드 → 파티원 균등 분배
```

**고대흔적 등급:** 4등급 (TraceItemFactory)  
솔로 플레이 시 경매 없이 즉시 지급.

### 5.4 주요 클래스

| 클래스 | 역할 |
|---|---|
| `BossRoomManager` | 파티 방 생성/조인/해체 관리 |
| `BossRun` | 개별 보스 전투 인스턴스 (HP, 페이즈, 참여자) |
| `BossRunService` | 전투 실행, 페이즈 전환, 광란 상태 |
| `BossEntryValidator` | 입장 조건 검사 |
| `BossPatternScheduler` | 패턴 타이밍 스케줄링 |
| `BossPhaseController` | HP 임계값 기반 페이즈 전환 |
| `PartyAuctionService` | 60초 파티 경매 세션 관리 |

---

## 6. Life Engine (영지 시스템)

### 6.1 개요

영지 저장고, 영토 상태, 기계 생산, 공방 가공을 담당한다.

### 6.2 영지 계층 구조

```
IridiumSkyblock 섬 (섬 보호/방문 껍데기)
    │
IslandTerritoryState (영토: 작물, 구조물, 작위 등급)
    │
IslandStorage (DB 기반 가상 저장고)
    ├── 재료별 수량 관리
    ├── 작위 등급별 최대 보관량
    └── 자동 입금 권한 플래그
```

### 6.3 영지 작위 등급

| 작위 | 영지명 | 재료별 보관량 |
|---|---|---:|
| — | 개척지 | 500 |
| 기사 | 기사령 | 900 |
| 준남작 | 준남작령 | 1,500 |
| 남작 | 남작령 | 2,500 |
| 자작 | 자작령 | 3,500 |
| 백작 | 백작령 | 5,000 |
| 후작 | 후작령 | 7,000 |
| 공작 | 공작령 | 10,000 |

### 6.4 자동 생산 흐름 (20분 주기)

```
MachineProductionScheduler.tick() (20분마다)
    │
IslandTerritoryStateStore → 전체 플레이어 영토 순회
    │
활성 기계 확인 (자동 재배기 등)
    │
생산량 계산 (기계 레벨 × 작물 종류)
    │
IslandStorageStore → 해당 재료 수량 증가
    └── (보관량 상한 초과 시 무시)
```

### 6.5 공방 가공기 탭 구성

| 탭 | 내용 |
|---|---|
| 요리 | 음식 제작 |
| 포션 | 버프 포션 제작 |
| 광물 정제 | 광석 → 정제 재료 |
| 연료 | 연료 아이템 제작 |
| 강화 보조 | 강화석/보조재 제작 |
| 큐브 촉매 | 잠재 큐브 촉매 제작 |
| 치장/영지 | 치장 재료 가공 |
| 보스 보상권 | 보스 어시스트 아이템 |

가공은 시간이 소요되며, 대기열 제한이 있다. 재료는 영지 저장고에서 차감, 완성품은 영지 저장고로 입금된다.

### 6.6 주요 클래스

| 클래스 | 역할 |
|---|---|
| `IslandStorageStore` | 플레이어 저장고 인메모리 캐시 |
| `IslandStorage` | 저장고 CRUD (재료별 수량 관리) |
| `IslandTerritoryStateStore` | 영토 상태 캐시 |
| `IslandTerritoryState` | 작물, 구조물, 기계 상태 |
| `IslandRank` | 영지 등급 (작위) |
| `MachineProductionScheduler` | 20분 주기 자동 생산 |
| `WorkshopRecipe` | 공방 레시피 정의 |
| `WorkshopJob` | 진행 중인 공방 작업 |

---

## 7. GUI 시스템

### 7.1 GUI 계층

```
/메뉴 → MainHubGui (54칸)
    ├── /장비 → EquipmentHubGui (27칸)
    │       ├── EnhancementGui    강화
    │       ├── PotentialGui      잠재
    │       ├── EngravingGui      각인
    │       └── CharacterGui      캐릭터 정보
    ├── TerritoryHubGui (27칸)  영지 관리
    │       ├── TerritoryStatusGui  영지 상태
    │       ├── TerritorySettingsGui 영지 설정
    │       ├── StorageGui          저장고
    │       ├── FarmGui             농사
    │       └── WorkshopGui         공방
    ├── BossHubGui (27칸)  보스
    │       ├── BossLobbyGui        보스 로비
    │       ├── BossRoomListGui     방 목록
    │       ├── BossCreateGui       방 생성
    │       ├── BossInfoGui         보스 정보
    │       └── BossClearRecordGui  클리어 기록
    └── ExploreHubGui (27칸)  탐험/필드
            └── FieldTeleportService  필드 이동
```

### 7.2 커스텀 GUI 배경 (비트맵 폰트 오버레이)

창고 GUI 배경에 PNG 이미지를 오버레이하는 방식:

```
창고 타이틀 Component에 커스텀 폰트 글리프 삽입
    │
Component.text("").font(Key.key("poro","gui"))
    │
리소스팩 assets/poro/font/gui.json
    ├──  → space advance -8 (좌측 정렬 보정)
    └──  → bitmap glyph (menu_main.png, 176×141px)
```

| GUI | 글리프 | PNG |
|---|---|---|
| MainHubGui |  | `poro:gui/menu_main.png` |
| EquipmentHubGui |  | `poro:gui/menu_hub_equipment.png` |
| TerritoryHubGui |  | `poro:gui/menu_hub_territory.png` |
| BossHubGui |  | `poro:gui/menu_hub_boss.png` |

---

## 8. 데이터 영속화

### 8.1 저장 대상

| 데이터 | 저장 방식 | 저장 위치 |
|---|---|---|
| 플레이어 성장 (장비, 강화, 잠재, 각인, 레벨) | JSON 파일 | `plugins/EmpireRPG/player_data/<UUID>.json` |
| 영지 저장고 수량 | JSON 파일 | `plugins/EmpireRPG/player_data/<UUID>.json` |
| Discord 연동 정보 | SQLite | `empire.db → discord_link` |
| 플레이어 플래그 | SQLite | `empire.db → player_flags` |
| 보스 클리어 기록 | SQLite | `empire.db → boss_clear_log` |
| 경매 아이템 | SQLite | `empire.db → auction_listings` |
| 마스터 데이터 | SQLite (CSV upsert) | `empire.db → *_master` |

### 8.2 자동 저장 스케줄

| 주기 | 작업 |
|---|---|
| 5분 | 온라인 전체 플레이어 JSON 저장 |
| 퇴장 시 | 해당 플레이어 즉시 JSON 저장 |
| 20분 | 영지 생산 자동 실행 |
| 30초 | 필드보스 상태 갱신 |
| 2분 | 만료된 인증 코드 정리 |

### 8.3 PlayerSaveData 구조

```json
{
  "weaponClass": "SWORD",
  "level": 12,
  "xp": 4500,
  "wallet": {
    "gold": 18000
  },
  "equipment": {
    "WEAPON": { "itemId": "sword_t1", "enhanceLv": 7, "potential": [...] },
    "ARMOR": { ... }
  },
  "islandStorage": {
    "wild_leather": 320,
    "enhancement_stone": 45
  },
  "tutorialCompleted": true
}
```

---

## 9. HTTP API

**포트:** 8765 (config.yml `http.port`)  
**인증:** `X-Api-Key` 헤더 (config.yml `http.api-key`)

| 메서드 | 경로 | 설명 |
|---|---|---|
| POST | `/auth/pending` | Discord 봇 → 인증 코드 생성 요청 |
| GET | `/auth/role-queue` | 역할 부여 큐 조회 |
| POST | `/auth/role-granted` | 역할 부여 완료 확인 |
| GET | `/field-status` | 현재 필드보스 스폰 상태 |
| GET | `/operations/admin/dashboard` | 관리자 대시보드 데이터 |
| GET | `/operations/admin/dashboard/bosses` | 보스 통계 |
| GET | `/operations/admin/dashboard/economy` | 경제 통계 |
| GET | `/operations/admin/dashboard/life` | 영지 통계 |
| GET | `/admin/players/{userId}` | 개별 플레이어 상세 |
| GET | `/operations/snapshot` | Discord 카드용 공개 스냅샷 |

---

## 10. 이벤트 리스너 목록

| 리스너 | 처리 이벤트 |
|---|---|
| PlayerJoinListener | 접속 → 데이터 로드, 핫바 지급, 튜토리얼 |
| PlayerQuitListener | 퇴장 → 데이터 저장, 캐시 제거 |
| SkillInputListener | 좌클릭/우클릭/F 입력 → 스킬 실행 |
| HotbarInteractListener | 핫바 우클릭 → 탐험/메뉴 열기 |
| CombatStateListener | 피해 이벤트 → 전투 상태 5초 마킹 |
| HealthHudListener | 체력 변경 → 이름 위 HUD 갱신 |
| WeaponSelectionGuiListener | 무기 선택 GUI 클릭 → weaponClass 저장 |
| GrowthGuiListener | 강화/잠재/각인 GUI 클릭 |
| MainHubListener | 메인 허브 탭 전환 |
| BossRoomListener | 보스 방 생성/조인/시작 |
| TerritoryStatusGuiListener | 영지 상태 GUI |
| StorageGuiListener | 저장고 입출금 |
| WorkshopGuiListener | 공방 제작 |
| FarmGuiListener | 작물 수확 |
| AuctionGuiListener | 경매 검색/입찰 |
| FieldDropListener | 필드보스 드랍 처리 |
| BossDefenseListener | 보스 방어력 스케일링 |
| SeasonBossListener | 시즌보스 이벤트 |
| ConsumableUseListener | 소비 아이템 사용 |
| HungerLockListener | 배고픔 고정 |
| AfkMonitorListener | AFK 감지 |
| CrossbowArrowListener | 석궁 화살 추적 |
| StaffProjectileListener | 스태프 발사체 |
| SwordParryListener | 검 패리 |
| TraceOpenListener | 고대흔적 개봉 |
| TraceEngraveListener | 고대흔적 각인 |

---

## 11. 스코어보드

**ScoreboardService** 가 접속/갱신 시마다 다음 항목을 표시한다.

```
포로 서버
───────────
직업: 검사 (검)
Lv.12 | XP 4,500
ATK 84 | 강화 +7
─────────────
영지: 기사령
저장고: 320/900
─────────────
전투 중 / 안전지대
```

HUD 아이콘(경험치 바, 체력 이름 위 표시)은 `HealthHudListener`와 리소스팩 커스텀 폰트로 처리한다.

---

## 12. 인증 흐름

```
Discord 봇: POST /auth/pending { discordId, mcNickname }
    │
AuthService.createPendingCode(discordId, mcNickname)
    ├── 6자리 코드 생성
    ├── PendingAuth DB 저장 (10분 만료)
    └── 응답: { code: "ABC123" }

플레이어: 마크 서버 접속 → /연동 ABC123
    │
VerifyCommand → AuthService.verify(player, "ABC123")
    ├── PendingAuth 조회 (만료 검사)
    ├── 닉네임 일치 검사
    ├── AuthRepository.link(discordId, playerUUID)
    └── 역할 부여 큐 추가

Discord 봇: GET /auth/role-queue
    │
    역할 부여 실행
    │
Discord 봇: POST /auth/role-granted
    └── 큐에서 제거
```

---

## 13. 빌드 및 배포

**빌드 명령:**
```bash
cd custom-plugins/empire-rpg
./gradlew jar
```

**출력:** `build/libs/empire-rpg-0.1.0.jar`

**배포:**
```bash
cp build/libs/empire-rpg-0.1.0.jar server/plugins/EmpireRPG.jar
```

**서버 재시작 필요:** 플러그인은 핫 리로드를 지원하지 않는다.

**Java 버전:** 21 (toolchain 설정)  
**Paper API 버전:** 1.21.10-R0.1-SNAPSHOT
