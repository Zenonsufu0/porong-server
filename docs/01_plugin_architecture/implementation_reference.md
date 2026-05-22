# 포로 서버 1차 시즌 — EmpireRPG 구현 레퍼런스

> **[STATUS: REFERENCE]** — EmpireRPG 구현 상세 참조. 공식 방향성은 `../final_master_plan.md`, 플러그인 경계는 `CANON.md`가 우선.

> 소스: `../final_master_plan.md` (2026-05-21 기준)  
> 용도: EmpireRPG 플러그인 구현 레퍼런스. 클래스 배정 / DB 스키마 / 이벤트 훅 / 알고리즘 정의.  
> **충돌 시 `final_master_plan.md`가 우선.**

---

## 목차

1. [플러그인 아키텍처](#1-플러그인-아키텍처)
2. [DB 스키마](#2-db-스키마)
3. [전투 시스템](#3-전투-시스템)
4. [장비 아이템 체계](#4-장비-아이템-체계)
5. [장비 성장](#5-장비-성장)
6. [개인 영지](#6-개인-영지)
7. [필드 시스템](#7-필드-시스템)
8. [보스 시스템](#8-보스-시스템)
9. [GUI 전체](#9-gui-전체)
10. [훈련장](#10-훈련장)
11. [HTTP API](#11-http-api)
12. [관리자 커맨드](#12-관리자-커맨드)
13. [개발 Phase](#13-개발-phase)

---

## 1. 플러그인 아키텍처

### 1.1 역할 분담

| 플러그인 | 역할 |
|---|---|
| **EmpireRPG** | 전투 / 장비 / 영지 / 보스 / 보상 / DB / API (코어 전부) |
| MythicMobs | 몹/보스 스폰, 바닐라 강화형 외형, 간단한 스킬 시각 이펙트 |
| IridiumSkyblock | 개인 영지 생성/보호/방문 껍데기만 |
| LuckPerms / Vault | 권한 / 경제 인터페이스 |
| EssentialsX | 운영 편의 (텔포, 홈, 채팅) |
| WorldEdit / WorldGuard | 지역 보호 |
| Multiverse-Core | 월드 분리 |

### 1.2 EmpireRPG 모듈 구조

```
EmpireRPGPlugin (onEnable 진입점)
│
├── 1. CommonFoundationBootstrap  → FoundationContext
│       SqliteConnectionProvider  (empire.db)
│       CommonPluginLoggerFactory
│       CommonConfigLoader        (config.yml)
│
├── 2. MasterRegistryBootstrap    → MasterRegistryContext
│       ItemMasterRegistry        (items.yml → PDC/CMD 매핑)
│       SkillMasterRegistry       (skills.yml → 계수/쿨타임)
│       BossMasterRegistry        (boss_spawn.yml)
│       RecipeMasterRegistry      (life_recipe_master.csv)
│       DropTableRegistry         (drop_tables.yml)
│
├── 3. CombatEngineBootstrap      → CombatEngineRuntime
│       WeaponSkillRegistry
│       CooldownManager           (스킬 쿨타임, per-player)
│       ResourceTracker           (스택/충전, per-player)
│       EngravingService          (각인 효과 적용)
│       DamageCalculator
│
├── 4. BossEngineBootstrap        → BossEngineRuntime
│       FieldBossScheduler        (30분 정시 스폰)
│       SeasonBossManager         (파티 입장/처치/보상)
│       BossContributionTracker   (기여도 3% 기준)
│       AntiOneshotGuard          (85% 클램프)
│
├── 5. GrowthEngineBootstrap      → GrowthEngineRuntime
│       EnhancementService        (강화, pity 카운터)
│       PotentialService          (큐브, 메모리얼)
│       SuccessionService         (전승)
│
├── 6. LifeEngineBootstrap        → LifeEngineRuntime
│       IslandRankService         (작위 승급)
│       FacilitySlotService       (시설 배정)
│       MachineProductionScheduler (20분 사이클)
│       IslandStorageService      (입출금)
│       WorkshopQueueService      (공방 대기열)
│       MineralGeneratorListener  (BlockFormEvent 가로채기)
│
├── 7. QuestAchievementBootstrap  (1차 최소 구현)
│
└── 8. OperationsQueryBootstrap   → OperationsQueryRuntime
        HttpApiServer             (포트 8765)
        DiscordWebhookService     (필드보스 알림)
        SnapshotService           (Discord 카드용)
```

### 1.3 플레이어 데이터 관리

- **인메모리 캐시:** `PlayerDataManager` — 로그인 시 JSON 로드
- **저장 주기:** 5분 타이머 + 로그아웃 즉시 저장
- **저장 경로:** `player_data/<UUID>.json`
- **캐시 키:** `UUID → PlayerSaveData`

### 1.4 월드 구성

| 월드 | 용도 |
|---|---|
| `world_main` | 제국 수도 + 필드 5개 + 훈련장 |
| `world_farm` | 개인 영지 (IridiumSkyblock) |
| `world_boss` | 시즌보스/균열왕 인스턴스 |
| `world_test` | 개발/QA 전용 |

---

## 2. DB 스키마

> SQLite (`empire.db`). 모든 PK는 `player_uuid TEXT`.

### 2.1 플레이어 기본

```sql
player_profile (
  player_uuid   TEXT PRIMARY KEY,
  nickname      TEXT,
  discord_id    TEXT,
  gold          INTEGER DEFAULT 0,
  weapon_type   TEXT,    -- SWORD / AXE / SPEAR / CROSSBOW / SCYTHE / STAFF
  engraving     TEXT,    -- 각인 코드
  tutorial_done INTEGER DEFAULT 0,
  created_ts    INTEGER,
  last_seen_ts  INTEGER
)
```

### 2.2 장비

```sql
player_equipment (
  player_uuid     TEXT,
  slot            TEXT,   -- WEAPON / HELMET / CHESTPLATE / LEGGINGS / BOOTS
  item_id         TEXT,
  enhance_level   INTEGER DEFAULT 0,
  potential_grade TEXT,   -- COMMON / RARE / EPIC / UNIQUE / LEGENDARY
  potential_opt1  TEXT,   -- option_code:value
  potential_opt2  TEXT,
  potential_opt3  TEXT,
  cosmetic_skin   TEXT,   -- 치장템 아이템 ID
  cosmetic_hidden INTEGER DEFAULT 0,
  PRIMARY KEY (player_uuid, slot)
)

-- 강화 pity 카운터 (슬롯별, 서버 재시작 후에도 유지)
player_enhance_pity (
  player_uuid   TEXT,
  slot          TEXT,
  current_tries INTEGER DEFAULT 0,
  ceiling       INTEGER DEFAULT 0,
  PRIMARY KEY (player_uuid, slot)
)
```

### 2.3 영지

```sql
island_territory (
  player_uuid   TEXT PRIMARY KEY,
  rank          TEXT DEFAULT 'PIONEER',  -- PIONEER/KNIGHT/BARON_JR/BARON/VISCOUNT/EARL/MARQUESS/DUKE
  storage_cap   INTEGER DEFAULT 500,
  upgraded_ts   INTEGER
)

-- 영지 설정 (공개 여부 + 세부설정)
island_settings (
  player_uuid    TEXT PRIMARY KEY,
  is_public      INTEGER DEFAULT 0,   -- 0=비공개, 1=공개
  seed_protect   INTEGER DEFAULT 1,   -- 씨앗 보호
  time_mode      TEXT DEFAULT 'FREE', -- FREE / DAY / NIGHT
  weather_mode   TEXT DEFAULT 'FREE', -- FREE / CLEAR / RAIN
  crop_protect   INTEGER DEFAULT 1,   -- 작물 파괴 보호
  water_protect  INTEGER DEFAULT 1    -- 물 파괴 보호
)

island_facility_slots (
  player_uuid     TEXT,
  slot_index      INTEGER,   -- 0~17
  facility_type   TEXT,      -- HERB_PLOT / ORE_EXTRACTOR / WORKSHOP / EMPTY
  facility_level  INTEGER DEFAULT 1,
  last_produced_ts INTEGER DEFAULT 0,
  PRIMARY KEY (player_uuid, slot_index)
)

island_storage (
  player_uuid   TEXT,
  item_id       TEXT,
  quantity      INTEGER DEFAULT 0,
  PRIMARY KEY (player_uuid, item_id)
)

-- 영지민 멤버 목록
island_members (
  island_owner_uuid  TEXT,
  member_uuid        TEXT,
  role               TEXT,    -- RESIDENT / VICE_LORD
  joined_ts          INTEGER,
  PRIMARY KEY (island_owner_uuid, member_uuid)
)

-- 영지 역할별 권한 설정
island_role_permissions (
  island_owner_uuid  TEXT,
  role               TEXT,    -- VISITOR / RESIDENT / VICE_LORD
  permission         TEXT,    -- BLOCK_PLACE / BLOCK_BREAK / CHEST_OPEN / CROP_HARVEST /
                               -- DOOR_USE / LEVER_BUTTON / MOB_ATTACK / STORAGE_ACCESS /
                               -- ISLAND_INVITE / ISLAND_SETTING / MEMBER_MANAGE
  allowed            INTEGER DEFAULT 0,
  PRIMARY KEY (island_owner_uuid, role, permission)
)

island_workshop_queue (
  id            INTEGER PRIMARY KEY AUTOINCREMENT,
  player_uuid   TEXT,
  slot_index    INTEGER,
  recipe_id     TEXT,
  queued_sets   INTEGER,
  queued_ts     INTEGER,
  completed_ts  INTEGER   -- NULL = 미완료
)
```

### 2.4 보스 / 기여도

```sql
boss_contribution_log (
  boss_instance_id  TEXT,
  player_uuid       TEXT,
  damage_dealt      INTEGER,
  PRIMARY KEY (boss_instance_id, player_uuid)
)

season_boss_clear_record (
  id            INTEGER PRIMARY KEY AUTOINCREMENT,
  boss_id       TEXT,
  party_leader  TEXT,
  party_members TEXT,   -- JSON 배열
  clear_time_ms INTEGER,
  cleared_ts    INTEGER
)
```

### 2.5 기타

```sql
-- IL 경고 카운터
player_il_warning (
  player_uuid  TEXT,
  field_id     TEXT,
  warn_count   INTEGER DEFAULT 0,
  PRIMARY KEY (player_uuid, field_id)
)

-- 버프/포션 상태
player_buff_state (
  player_uuid   TEXT PRIMARY KEY,
  feast_type    TEXT,      -- 만찬 종류
  feast_exp_ts  INTEGER,
  boost_gold_exp_ts   INTEGER,
  boost_enhance_exp_ts INTEGER,
  boost_exp_exp_ts    INTEGER,
  boss_potion_uses    TEXT  -- JSON: {boss_instance_id: count}
)
```

---

## 3. 전투 시스템

### 3.1 입력 이벤트 매핑

| 입력 | 이벤트 | 조건 |
|---|---|---|
| LC (근거리) | `PlayerAttackEntityEvent` | 무기 태그 있음 + 타겟 Entity |
| LC (원거리) | `LEFT_CLICK_AIR` + `PlayerAttackEntityEvent` | 무기 태그 있음 |
| RC | `RIGHT_CLICK_AIR` + `RIGHT_CLICK_BLOCK` | 무기 태그 있음 |
| SRC | `PlayerToggleSneakEvent` true → `RIGHT_CLICK` 조합 | shift 상태 감지 |
| F | `PlayerSwapHandItemsEvent` | 무기 태그 있음 |

**차단 이벤트:**
- `PlayerInteractEvent`: 석궁 장전·발사 / 삼지창 투척 → `setCancelled(true)`
- `BlockBreakEvent`: PDC 무기 태그 아이템으로 파괴 → `setCancelled(true)`
- `CraftItemEvent` / `AnvilEvent`: 무기 아이템 재료 사용 → 취소

**영지 내 전투 차단:**
IridiumSkyblock API로 해당 섬 영역 확인 → 스킬 입력 전부 차단.

### 3.2 스킬 실행 흐름

```
입력 이벤트 수신
  → CooldownManager: 쿨타임 남아있음? → 무시
  → ResourceTracker: 스택 조건 충족?  → 무시
  → 영지 내? → 차단 메시지 후 무시
  → 전투 중 차단 아이템(핫바 7·8) 사용 중? → 차단
  → SkillExecutor.execute(player, skillSlot)
      → 스킬 타입별 분기 (MELEE / PROJECTILE / AOE / DASH_MELEE 등)
      → DamageCalculator.calculate(caster, target, skill)
      → ResourceTracker.consumeOrGain(player, delta)
      → CooldownManager.setCooldown(player, skillSlot, duration)
      → 이펙트 재생 (파티클/소리)
```

### 3.3 피해 계산

```
최종 피해 = 기본 공격력
           × 스킬 계수
           × (1 + 강화 보너스 합산)
           × (1 + 각인 보너스)
           × (1 + 잠재 ATK%/스킬피해%/태그피해% 합산)
           × (1 + 만찬 버프)
           × 치명타 배율 (크리 발동 시 × (1 + 치명타 데미지%))
           × (1 - 대상 방어력 × (1 - 방어력 무시%))
```

**치명타 판정:** `Math.random() < 치명타확률` → 발동.

**원샷 방지 (필드보스/시즌보스):**
```
타격 피해 >= 보스 최대HP × 0.85
  → 피해를 보스최대HP × 0.85로 클램프
  → 해당 플레이어의 이후 공격: 0 피해 (전투 종료까지)
  → 클램프된 피해는 기여도 정상 반영
```

### 3.4 스택(자원) 시스템

```java
// ResourceTracker per-player 구조
Map<UUID, Integer> stackMap;  // 현재 스택
Map<UUID, Integer> maxStack;  // 각인 타입별 최대 (소모형=3~5 / 유지형=6)
```

**소모형 만충 판정:** `current == maxForEngraving`  
**유지형 6스택 특수 효과:** EngravingService에서 6스택 도달 시 특수 상태 플래그 활성.

### 3.5 무기 6종 스킬 요약

> 전체 상세: `final_master_plan.md §5.4`

| 무기 | 자원 | 소모형 max | 유지형 max | LC | RC | SRC | F |
|---|---|---|---|---|---|---|---|
| 검 | 검세 | 3 | 6 | 섬광베기 | 연속참 | 수호반격 | 결전일섬 |
| 도끼 | 충격 | 3 | 6 | 철퇴강타 | 파쇄돌진 | 불굴자세 | 거신추락 |
| 창 | 압박 | 5 | 6 | 관통찌르기 | 반월창 | 돌파창 | 천뢰일창 |
| 석궁 | 명중 | 3 | 6 | 속사(×3) | 회피사격 | 관통볼트 | 저격태세 |
| 낫 | 그림자 흐름 | 3 | 6 | 사신베기 | 월영회전 | 그믐참 | 처형낫 |
| 스태프 | 마력 충전 | 5 | 6 | 마력탄 | 속성폭발 | 마력쇄도 | 별빛쇄도 |

**각인 구조:**
- **A각인 (소모형):** F 발동 시 자원 전체 소모, 만충 시 최대 스케일링
- **B각인 (유지형):** 자원 최대 6스택, F 소모 없음, 6스택 달성 시 특수 효과
- 공용각인 1차 제외

### 3.6 특수 구현 메모

- **낫 RC (월영회전) 이동:** `Player.getVelocity()` 수평 벡터로 이동 방향 결정. 정지 상태이면 우측 기본값.
- **석궁/스태프 LC:** `LEFT_CLICK_AIR` 허용 (논타겟). 바닐라 화살 발사는 `PlayerInteractEvent`에서 차단.
- **창 SRC (돌파창):** 대시 경로상 모든 엔티티에 피해. `BoundingBox` 슬라이딩으로 구현.

---

## 4. 장비 아이템 체계

### 4.1 T1 아이템 목록

**무기 (튜토리얼 지급, 클래스 선택)**

| item_id | 이름 | 베이스 아이템 | CMD | 0강 ATK |
|---|---|---|---|---:|
| `weapon_sword_t1` | 제국 장검 | `NETHERITE_SWORD` | 1001 | 50 |
| `weapon_axe_t1` | 제국 전투도끼 | `NETHERITE_AXE` | 1002 | 55 |
| `weapon_spear_t1` | 제국 장창 | `TRIDENT` | 1003 | 48 |
| `weapon_crossbow_t1` | 제국 석궁 | `CROSSBOW` | 1004 | 45 |
| `weapon_scythe_t1` | 제국 낫 | `NETHERITE_HOE` | 1005 | 50 |
| `weapon_staff_t1` | 제국 스태프 | `BLAZE_ROD` | 1006 | 42 |

**방어구 (튜토리얼 지급, 자동 장착)**

| item_id | 이름 | 베이스 아이템 | 0강 HP | 0강 DEF |
|---|---|---|---:|---:|
| `armor_helmet_t1` | 제국 투구 | `NETHERITE_HELMET` | +150 | +3% |
| `armor_chestplate_t1` | 제국 흉갑 | `NETHERITE_CHESTPLATE` | +250 | +5% |
| `armor_leggings_t1` | 제국 각반 | `NETHERITE_LEGGINGS` | +200 | +4% |
| `armor_boots_t1` | 제국 부츠 | `NETHERITE_BOOTS` | +100 | +2% |

### 4.2 아이템 식별 (PDC 태그)

| 태그 키 | 값 예시 | 용도 |
|---|---|---|
| `poro:item_id` | `weapon_sword_t1` | 아이템 종류 식별 |
| `poro:weapon_type` | `SWORD` | 무기 인식 (CMD도 함께 확인) |
| `poro:equipment_slot` | `WEAPON` | 장비 슬롯 분류 |
| `poro:equipment_locked` | `true` | 드랍/장착해제 차단 대상 |
| `poro:enhance_level` | `10` | 강화 수치 저장 |
| `poro:potential_grade` | `LEGENDARY` | 잠재 등급 |

### 4.3 아이템 로어 템플릿

```
이름: §f{아이템명} §e(+{N}강)

§8──────────────────────
§7강화§8: §e{N}강   §7등급§8: §a{등급명}
§8──────────────────────
§7고정 스텟
  §f공격력§8: §f+{n}         (무기)
  §f최대 HP§8: §f+{n}        (방어구)
  §f방어력§8: §f+{n}%
§7부가 스텟
  §b{부가스텟명}§8: §b+{n}%
§8──────────────────────
§d잠재능력§8: §6{잠재등급}
  §6· {옵션1} +{n}%
  §6· {옵션2} +{n}%   (등급별 1~3줄)
§8──────────────────────
§9스킬                     (무기 전용)
  §f[LC] {스킬명} §8(쿨 {N}초)
  §f[RC] {스킬명} §8(쿨 {N}초)
  §f[SRC] {스킬명} §8(쿨 {N}초)
  §f[F] {스킬명} §8(쿨 {N}초)
§8──────────────────────
```

바닐라 속성 숨김: `ItemMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES)`

### 4.4 잠금 규칙

| 규칙 | 구현 |
|---|---|
| 드랍 금지 | `PlayerDropItemEvent` → `poro:equipment_locked` 확인 → `setCancelled(true)` |
| 장착 해제 금지 | `InventoryClickEvent` → 장비 슬롯 클릭 + 이동 감지 → `setCancelled(true)` |
| 인벤토리 내 이동 | 허용 (슬롯 정리 가능) |

### 4.5 잠재 옵션 풀

| 슬롯 | 옵션 종류 |
|---|---|
| 무기 | ATK%, 방어력무시%, 스킬피해%, 보스피해%, 치명타확률%, 치명타데미지%, 쿨감%, 태그피해%(4종 중 랜덤) |
| 투구 | HP%, DEF%, 치명타확률%, 태그피해%, [유니크+]받는피해감소% |
| 상의 | HP%, DEF%, 스킬피해%, 태그피해%, [유니크+]받는피해감소% |
| 하의 | HP%, DEF%, 쿨감%, 보스피해%, 태그피해%, [유니크+]받는피해감소% |
| 신발 | 이동속도%, HP%, 방어력무시%, ATK%, 쿨감%, 태그피해% |

**등급별 대표 수치 (1라인 기준)**

| 옵션 | 커먼 | 레어 | 에픽 | 유니크 | 레전더리 |
|---|---:|---:|---:|---:|---:|
| ATK% | +1% | +4% | +10% | +13% | +18% |
| 스킬피해% | +5% | +8% | +11% | +13% | +15% |
| 보스피해% | +1% | +3% | +7% | +11% | +16% |
| 치명타확률% | +5% | +10% | +14% | +18% | +25% |
| 치명타데미지% | +2% | +5% | +9% | +14% | +21% |
| HP% | +1% | +4% | +7% | +11% | +15% |
| 받는피해감소% | — | — | — | +2% | +4% |

---

## 5. 장비 성장

### 5.1 강화 시스템

**소모 재화:** 골드 + 강화석(`mat_stone_enhance`)  
**방어구 강화석 소모:** `ceil(무기 강화석 ÷ 1.5)`  
**파괴/강등 없음 (T1 확정)**

**강화 테이블 (T1)**

| 강화 | 성공률 | 골드 | 무기 강화석 | 방어구 강화석 | 가호 천장 |
|---|---:|---:|---:|---:|---:|
| 1강 | 100% | 2,000G | 3 | 2 | — |
| 2강 | 100% | 3,000G | 4 | 3 | — |
| 3강 | 100% | 4,500G | 5 | 4 | — |
| 4강 | 95% | 6,000G | 6 | 4 | — |
| 5강 | 90% | 8,000G | 7 | 5 | — |
| 6강 | 85% | 10,000G | 8 | 6 | — |
| 7강 | 80% | 12,500G | 9 | 6 | — |
| 8강 | 75% | 15,000G | 10 | 7 | — |
| 9강 | 70% | 17,500G | 11 | 8 | — |
| 10강 | 65% | 20,000G | 12 | 8 | — |
| 11강 | 50% | 21,500G | 14 | 10 | 4회 |
| 12강 | 40% | 22,500G | 16 | 11 | 5회 |
| 13강 | 35% | 23,000G | 18 | 12 | 6회 |
| 14강 | 30% | 23,500G | 20 | 14 | 7회 |
| 15강 | 25% | 24,000G | 23 | 16 | 8회 |
| 16강 | 20% | 24,500G | 26 | 18 | 10회 |
| 17강 | 10% | 25,000G | 30 | 20 | 20회 |
| 18강 | 5% | 25,500G | 35 | 24 | 40회 |
| 19강 | 2% | 26,000G | 41 | 28 | 100회 |
| 20강 | 1% | 26,500G | 48 | 32 | 200회 |
| 21강 | 2% | 27,000G | 56 | 38 | 100회 |
| 22강 | 1% | 27,000G | 64 | 43 | 200회 |
| 23강 | 0.5% | 27,000G | 72 | 48 | 400회 |
| 24강 | 0.15% | 27,000G | 79 | 53 | 1,334회 |
| 25강 | 0.05% | 27,000G | 86 | 58 | 4,000회 |

**가호 천장 (pity) 알고리즘:**
```
11강 이상 강화 시:
  ceiling = ceil(2 / 성공률)
  current_tries += 1
  if current_tries >= ceiling → 강제 성공, current_tries = 0
  else → 확률 판정
성공 시: current_tries = 0
```
`player_enhance_pity` 테이블에 슬롯별 DB 저장 필수.

**강화 흔적 소모품 (10강 이상 선택)**

| item_id | 성공률 보정 | 공방 제작 재료 |
|---|---:|---|
| `mat_trace_star` (별) | +20% | 마도합금×1 + 철블럭×2 + 금블럭×2 |
| `mat_trace_moon` (달) | +30% | 마도합금×2 + 철블럭×4 + 금블럭×4 + 다이아블럭×1 + 에메블럭×1 |
| `mat_trace_sun` (태양) | +50% | 마도합금×3 + 철블럭×8 + 금블럭×8 + 다이아블럭×2 + 에메블럭×2 |

보정은 **가산(+%)**: 25강 0.05% + 태양 +50% = 50.05%

### 5.2 큐브(잠재) 시스템

**큐브 수급:** 큐브 조각(정예몹 드랍) × 10개 → 자동 전환 → 큐브 1개  
**사용 비용:** 5,000G / 1회  
**동작:** 전 라인 옵션 재롤 + 등업 시도 (등급 하락 없음)

**등업 확률**

| 현재 → 다음 | 확률 |
|---|---:|
| 커먼 → 레어 | 30% |
| 레어 → 에픽 | 6% |
| 에픽 → 유니크 | 1.8% |
| 유니크 → 레전더리 | 0.3% |

**라인별 이탈 (더 높은 등급 옵션 부여)**

| 잠재 등급 | 1번째 줄 | 2번째 줄 | 3번째 줄 |
|---|---|---|---|
| 커먼 | 커먼 | 커먼 | 커먼 |
| 레어 | 레어 | 커먼 (이탈 10%→레어) | 커먼 (이탈 5%→레어) |
| 에픽 | 에픽 | 레어 (이탈 10%→에픽) | 레어 (이탈 5%→에픽) |
| 유니크 | 유니크 | 에픽 (이탈 10%→유니크) | 에픽 (이탈 5%→유니크) |
| 레전더리 | 레전더리 | 유니크 (이탈 10%→레전더리) | 유니크 (이탈 5%→레전더리) |

**메모리얼 시스템 (선택 플로우):**
```
큐브 사용 → 새 옵션 계산
  ├── 등업 없음 → [새 옵션 선택] or [현재 유지] or [아무 선택 없이 재롤 반복]
  └── 등업 발생 → GUI 잠금 → [새 옵션 선택(등업 등급)] or [현재 유지(기존 등급)] 필수 선택
```
재롤 반복해도 매회 5,000G 차감.

### 5.3 전승 시스템

**전승:** 장비의 흔적 → 현재 장비의 등급 + 부가 스텟 이전

| 전승 방식 | 소모 | 효과 |
|---|---|---|
| 기본 전승 | 장비의 흔적 × 1 + 골드(M-4 미정) | 등급 + 부가 스텟 전부 이전 |
| 등급 전승권 (상점 50,000G) | 전승권 + 흔적 | 등급만 이전 |
| 부가 스텟 전승권 (상점 50,000G) | 전승권 + 흔적 | 부가 스텟만 이전 |

**장비의 흔적 아이템**

| item_id | 등급 | 획득 경로 |
|---|---|---|
| `equip_trace_broken` | 커먼 | 미감정 흔적 개봉, 필드보스 드랍 |
| `equip_trace_faded` | 레어 | 미감정 흔적(레어+ 보장), 필드보스 드랍 |
| `equip_trace_glowing` | 에픽 | 미감정 흔적(에픽+ 보장), 시즌보스 |
| `equip_trace_radiant` | 유니크 | 미감정 흔적(유니크+ 보장), 시즌보스 |
| `equip_trace_brilliant` | 레전더리 | 미감정 흔적(레전더리 확정), 균열왕 |

**고대흔적** — 미감정 흔적 개봉 시 함께 사용해 최소 등급 보장

| item_id | 보장 등급 |
|---|---|
| `ancient_trace_faded` | 레어 이상 |
| `ancient_trace_glowing` | 에픽 이상 |
| `ancient_trace_radiant` | 유니크 이상 |
| `ancient_trace_brilliant` | 레전더리 확정 |

---

## 6. 개인 영지

### 6.1 영지 작위 8단계

| 작위 | 영지명 | 승급 골드 | 추가 재료 | 저장고 용량 | 시설 슬롯 | XZ 한도 |
|---|---|---:|---|---:|---:|---|
| — | 개척지 | — | — | 500 | 4 | 50×50 |
| 기사 | 기사령 | 20,000G | 전장의파편 50 | 900 | 6 | 60×60 |
| 준남작 | 준남작령 | 35,000G | 전장의파편 80 | 1,500 | 7 | 70×70 |
| 남작 | 남작령 | 55,000G | 전장의파편 100 | 2,500 | 9 | 80×80 |
| 자작 | 자작령 | 75,000G | 전장의파편 80 + 달의흔적 3 | 3,500 | 11 | 90×90 |
| 백작 | 백작령 | 95,000G | 전장의파편 60 + 태양의흔적 2 | 5,000 | 13 | 100×100 |
| 후작 | 후작령 | 120,000G | 전장의파편 120 + 필드보스핵심재료 3 | 7,000 | 15 | 120×120 |
| 공작 | 공작령 | 150,000G | 전장의파편 200 + 필드보스핵심재료 5 | 10,000 | 18 | 150×150 |

**시설 자동 레벨업:** 남작령 달성 → 약초재배지·광물채굴기 Lv2. 백작령 달성 → Lv3.

### 6.2 시설 3종

| 시설 | item_id | 생산 주기 | 최대 레벨 |
|---|---|---|---|
| 약초 재배지 | `estate_herb_plot` | 20분 | 3 |
| 광물 채굴기 | `estate_ore_extractor` | 20분 | 3 |
| 공방 가공기 | `estate_workshop` | 즉시(대기열) | 1 |

**시설 배치:** GUI 슬롯 배정 (물리 블럭 설치 없음). DB `island_facility_slots` 저장.

**약초 재배지 산출량**

| 레벨 | 제국 약초/사이클 | 제국 정수 (레어 보너스) |
|---|---|---|
| 1 | 2~3개 | — |
| 2 | 3~4개 | 10% 확률 1개 |
| 3 | 4~6개 | 30% 확률 1개 |

**광물 채굴기 산출량**

| 레벨 | 마도철 원석/사이클 | 은 원석 (레어 보너스) |
|---|---|---|
| 1 | 2~3개 | — |
| 2 | 3~4개 | 10% 확률 1개 |
| 3 | 4~6개 | 30% 확률 1개 |

### 6.3 공방 가공기

- 1대 = 대기 슬롯 3개. 슬롯 1개당 동일 레시피 최대 8세트.
- 대기열 FIFO. 등록 즉시 재료 차감.
- 결과물 완료 시 영지 창고 자동 입금.
- 오프라인 완료 → 다음 접속 시 알림.

### 6.4 영지 방문 시스템

#### 방문 방식 3가지

| 방식 | 조건 | 처리 |
|---|---|---|
| 내 영지 이동 | 항상 가능 | IridiumSkyblock API 즉시 텔포 |
| 공개 영지 방문 | 대상 영지 `is_public=1` | 즉시 텔포 (수락 없음) |
| 방문 요청 | 대상 온라인 | 채팅 수락/거절 대기 |

#### 방문 요청 플로우

```
요청자: 채팅으로 닉네임 입력
  → PlayerService.findOnlineByName(name)
      없음        → §c없는 유저입니다.
      오프라인    → §c{name}님은 현재 오프라인입니다.
      이미 요청중 → §c이미 요청이 진행 중입니다.
      온라인      → IslandVisitRequest 생성 (TTL 30초)
                    대상 채팅 전송:
                    §e[{요청자}]님이 영지 방문을 요청했습니다. §a[수락] §c[거절]

대상: [수락] 클릭 → /empire visit accept <요청자UUID>
  → 요청자를 대상 영지 spawn으로 텔포
  → 요청자에게 §a{name}님의 영지로 이동합니다.

대상: [거절] 클릭 → /empire visit deny <요청자UUID>
  → 요청자에게 §c{name}님이 방문 요청을 거절했습니다.

30초 초과 → 양측에 §7요청 시간이 초과되었습니다.
```

#### 영지 초대 플로우

```
초대자: 채팅으로 닉네임 입력 (영지설정 GUI 슬롯 2)
  → 동일 검증 (없는 유저/오프라인)
  → IslandInvite 생성 (TTL 30초)
    대상 채팅 전송:
    §e[{초대자}]님한테서 영지 초대가 도착했습니다. §a[수락] §c[거절]

[수락] → /empire island invite accept <초대자UUID>
  → 초대자 영지 spawn 텔포
[거절] → /empire island invite deny <초대자UUID>
  → 초대자에게 §c{name}님이 영지 초대를 거절했습니다.
```

#### 내부 커맨드 (플레이어 비노출, ClickEvent 전용)

```
/empire visit accept <uuid>
/empire visit deny <uuid>
/empire island invite accept <uuid>
/empire island invite deny <uuid>
```

#### 영지 세부설정

DB `island_settings` 저장. 플러그인 적용 방식:

| 설정 | 대상 | 구현 이벤트 |
|---|---|---|
| 씨앗 보호 | 심은 씨앗이 밟혀도 뽑히지 않음 | `EntityChangeBlockEvent` — 엔티티(플레이어 포함)가 경작지(FARMLAND)를 흙으로 변환할 때 취소 |
| 시간 고정 | 낮 고정 / 밤 고정 / 자유 | `TimeSkipEvent` 취소 + 20틱 주기 스케줄러로 월드 시간 강제 설정 |
| 날씨 고정 | 맑음 고정 / 비 고정 / 자유 | `WeatherChangeEvent` 취소 + 주기 스케줄러로 날씨 강제 유지 |
| 작물 파괴 보호 | 다 자란 작물도 밟아도 파괴 안 됨 (모든 성장 단계) | `BlockBreakEvent` (플레이어 직접 파괴 취소) + `EntityChangeBlockEvent` (경작지 밟기로 인한 작물 파괴 취소) |
| 물 파괴 보호 | 흘러내리는 물이 작물을 파괴하지 않음 | `BlockPhysicsEvent` — 작물 블럭 옆·위에 물 블럭 인접 시 파괴 취소 |

> IridiumSkyblock의 Island 영역 확인 API: `IslandManager.getIslandAtLocation(location)` 활용.

---

### 6.5 영지민 권한 시스템

#### 역할 4등급

| 역할 | 설명 | 변경 권한 |
|---|---|---|
| LORD (영주) | 영지 소유자. 고정, 변경 불가 | 모든 권한 자동 |
| VICE_LORD (부영주) | 영주가 지정. 영지민 → 부영주 승격 가능 | 영주만 설정 가능 |
| RESIDENT (영지민) | 영지 초대 수락 시 기본 역할 | 영주/부영주 설정 가능 |
| VISITOR (방문자) | 멤버가 아닌 방문자 | 영주/부영주 설정 가능 |

#### 권한 11종 기본값

| 권한 | 설명 | 방문자 | 영지민 | 부영주 |
|---|---|:---:|:---:|:---:|
| `BLOCK_PLACE` | 블럭 설치 | ✗ | ✓ | ✓ |
| `BLOCK_BREAK` | 블럭 파괴 | ✗ | ✓ | ✓ |
| `CHEST_OPEN` | 상자 열기 | ✗ | ✓ | ✓ |
| `CROP_HARVEST` | 작물 수확 | ✗ | ✓ | ✓ |
| `DOOR_USE` | 문/철문 사용 | ✓ | ✓ | ✓ |
| `LEVER_BUTTON` | 레버/버튼 사용 | ✗ | ✓ | ✓ |
| `MOB_ATTACK` | 영지 내 몹 공격 | ✗ | ✓ | ✓ |
| `STORAGE_ACCESS` | 영지 저장고 접근 | ✗ | ✗ | ✓ |
| `ISLAND_INVITE` | 다른 플레이어 초대 | ✗ | ✗ | ✓ |
| `ISLAND_SETTING` | 영지 세부설정 변경 | ✗ | ✗ | ✓ |
| `MEMBER_MANAGE` | 멤버 역할 변경/추방 | ✗ | ✗ | ✗ |

> `MEMBER_MANAGE`(멤버 관리)는 부영주도 기본 비활성. 영주만 항상 허용.

#### 구현 방식

```java
// IslandPermissionService
boolean hasPermission(UUID islandOwner, UUID actor, IslandPermission permission) {
    if (actor.equals(islandOwner)) return true;  // 영주 항상 허용
    String role = getMemberRole(islandOwner, actor);  // VISITOR / RESIDENT / VICE_LORD
    return queryAllowed(islandOwner, role, permission);
}

// 권한 초기값 삽입 (영지 생성 시)
void initDefaultPermissions(UUID owner) {
    // island_role_permissions 테이블에 3역할 × 11권한 = 33행 INSERT (기본값 위 표 기준)
}
```

DB `island_role_permissions` 조회는 영지 입장 시 플레이어별 캐시 → 영주가 설정 변경 시 캐시 무효화.

---

### 6.6 파란색 색유리 엘리베이터

`world_farm` 전용. `BLUE_STAINED_GLASS` 블럭 위에서 동작.

| 입력 | 이벤트 | 동작 |
|---|---|---|
| 점프 (Space) | `PlayerJumpEvent` | 현재 위치 y 기준 +1 ~ +30 칸 내 파란색 색유리 탐색 → 그 위로 텔포 |
| 웅크리기 (Shift) | `PlayerToggleSneakEvent` (isSneaking=true) | 현재 위치 y 기준 -1 ~ -30 칸 내 파란색 색유리 탐색 → 그 위로 텔포 |

```java
// ElevatorListener
@EventHandler
void onJump(PlayerJumpEvent e) {
    Player p = e.getPlayer();
    if (!p.getWorld().getName().equals("world_farm")) return;
    Block under = p.getLocation().subtract(0,1,0).getBlock();
    if (under.getType() != Material.BLUE_STAINED_GLASS) return;

    for (int dy = 1; dy <= 30; dy++) {
        Block candidate = under.getRelative(0, dy, 0);
        if (candidate.getType() == Material.BLUE_STAINED_GLASS) {
            // candidate 위 2칸 공기 확인
            p.teleport(candidate.getLocation().add(0.5, 1, 0.5));
            return;
        }
    }
}

@EventHandler
void onSneak(PlayerToggleSneakEvent e) {
    if (!e.isSneaking()) return;
    Player p = e.getPlayer();
    if (!p.getWorld().getName().equals("world_farm")) return;
    Block under = p.getLocation().subtract(0,1,0).getBlock();
    if (under.getType() != Material.BLUE_STAINED_GLASS) return;

    for (int dy = 1; dy <= 30; dy++) {
        Block candidate = under.getRelative(0, -dy, 0);
        if (candidate.getType() == Material.BLUE_STAINED_GLASS) {
            p.teleport(candidate.getLocation().add(0.5, 1, 0.5));
            return;
        }
    }
}
```

> `PlayerJumpEvent`는 Paper 전용 이벤트. 일반 Spigot에서는 `PlayerMoveEvent` 속도 벡터로 대체 가능.

---

### 6.7 영지 저장고

- DB 기반 (`island_storage`). 물리 상자 없음.
- 좌클릭 64개 출금 / 우클릭 1개 출금.
- 전체 입금 버튼: 인벤토리 전체 입금.

### 6.8 재화 소모 공통 규칙 (전역 정책)

모든 시스템(강화·큐브·공방·상점)에서 아이템 재료 소모 순서:

1. **영지 창고** 잔량 먼저 차감
2. 부족 → **플레이어 인벤토리**에서 추가 차감
3. 합산 후에도 부족 → `§c재료가 부족합니다. (창고 + 인벤토리 합산)` 출력, 중단

> 골드는 플레이어 잔액에서만 차감 (창고 개념 없음).

### 6.9 생산 스케줄러

```java
// MachineProductionScheduler
runTaskTimerAsynchronously(plugin, 0L, 24000L); // 20분
// 전체 유저 (온라인/오프라인 모두) 처리
// last_produced_ts 기준 밀린 사이클 계산 (최대 3사이클 보상)
// 저장고 용량 초과 → 초과분 버림, 접속 시 경고
```

### 6.10 기본 광물 생성기

구성: 울타리 + 빈칸 + 물 (바닐라 설치).  
트리거: `BlockFormEvent` (물+용암 → 돌 생성 이벤트) 가로채 커스텀 드랍 처리.  
작위별 확률표:

| 아이템 | 개척지 | 기사령 | 준남작령 | 남작령 | 자작령 | 백작령 | 후작령 | 공작령 |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| 조약돌 | 45% | 38% | 32% | 26% | 20% | 15% | 7% | 5% |
| 구리 원석 | 20% | 20% | 18% | 16% | 13% | 11% | 7% | 5% |
| 철 원석 | 17% | 18% | 20% | 21% | 20% | 18% | 14% | 12% |
| 금 원석 | 5% | 7% | 9% | 12% | 15% | 18% | 22% | 20% |
| 레드스톤 | 5% | 7% | 8% | 10% | 12% | 13% | 15% | 15% |
| 청금석 | 4% | 5% | 7% | 8% | 10% | 11% | 13% | 13% |
| 다이아몬드 | 3% | 4% | 5% | 5% | 7% | 10% | 13% | 17% |
| 에메랄드 | 1% | 1% | 1% | 2% | 3% | 4% | 9% | 13% |

자작령 이상: 다이아/에메랄드 생성 시 석탄·금·철·레드스톤·청금석 중 1종 추가 (행운 레벨 비례).

### 6.11 공방 레시피 요약

**영지 제작 탭**

```
밀×192 + 감자×192 + 당근×192 → 농부의 정수 × 1
석탄블록×64 + 구리블록×64 + 철블록×32 + 금블록×32
  + 다이아블록×16 + RS블록×64 + 청금석블록×64 + 에메블록×8 → 광부의 정수 × 1
농부의 정수 + 광부의 정수 → 자연의 정수 × 1
자연의 정수 + 전장의 파편×512 → 원시 정수 × 1
원시 정수 × 1 → 미감정 흔적 × 1
광부의정수×1 + 다이아블럭×64 + 에메블럭×64 → 빛 바랜 고대흔적 × 1
광부의정수×2 + 다이아블럭×128 + 에메블럭×128 → 빛나는 고대흔적 × 1
광부의정수×3 + 다이아블럭×256 + 에메블럭×256 → 눈부신 고대흔적 × 1
광부의정수×4 + 다이아블럭×512 + 에메블럭×512 → 찬란한 고대흔적 × 1
```

**제련 탭**
```
마도철 원석×3 → 마도합금 × 1
은 원석×1 → 마도합금 × 3
```

**정제 탭**
```
제국 약초×3 → 정제 약초 × 1
제국 정수×1 → 정제 약초 × 3
```

**연금술 (치료) 탭**

| 결과물 | 재료 | HP 회복 | 보스전 횟수 |
|---|---|---:|---:|
| 치료 포션 (소) `potion_heal_small` | 정제약초×1 + 밀×64 | 30% | 3회 |
| 치료 포션 (중) `potion_heal_medium` | 정제약초×2 + 밀×64 | 40% | 4회 |
| 치료 포션 (대) `potion_heal_large` | 정제약초×3 + 밀×64 | 50% | 5회 |

**연금술 (부스트) 탭** — 농부의정수×1 + 정제약초×1 + 철블럭×16 + 금블럭×16 → 골드/강화/경험치 부스트 포션 (각 +50%, 30분). 3종 동시 적용 가능.

**요리 탭**

| 결과물 | 효과 (30분, 1종) |
|---|---|
| 전사의 만찬 | ATK +10% |
| 학살자의 만찬 | 모든 피해 +8% |
| 암살자의 만찬 | 크리 피해 +20% |
| 사냥꾼의 만찬 | 일반몹 피해 +15% (보스 미적용) |

재료: 전장의파편×64 + 정제약초×1 + 농작물×64 (만찬별 농작물 종류 다름)

---

## 7. 필드 시스템

### 7.1 필드 5개

| 필드 | 권장 평균 IL | 경고 임계 |
|---|---|---|
| 수도 외곽 평원 | IL 0~35 | IL 65+ (평균 13강+) |
| 폐광 지대 | IL 30~55 | IL 75+ (평균 15강+) |
| 오염된 수로 | IL 50~75 | IL 85+ (평균 17강+) |
| 무너진 초소 | IL 65~85 | IL 90+ (평균 18강+) |
| 고대 성벽 잔해 | IL 80~100 | 없음 |

**IL 공식:** 강화 단계 1 = IL +5 / 평균 IL = 5슬롯 평균 강화단계 × 5  
**구성:** 일반몹 2종 + 정예몹 1종 + 필드보스 1종

### 7.2 드랍 구조

| 몹 | 드랍 |
|---|---|
| 일반몹 | 골드 + 전장의 파편 + 강화석(낮은 확률) |
| 정예몹 | 골드 + 전장의 파편 + 강화석 + 큐브 조각 |
| 필드보스 | 전장의 파편 + 강화석 + 고대흔적 + 장비의 흔적 (개인 독립 확률) |

강화석 직접 드랍 확정 — `mat_stone_fragment` 파편 시스템 폐지.

### 7.3 IL 경고 시스템

```
필드 입장 시 평균 IL 계산
  → 경고 임계 초과 + 30마리마다 경고 1회
  → 3회 누적 → 임시 화이트리스트 제외 (운영자 수동 해제)
```

### 7.4 기여도 보상

- 기준: **총 피해량 3% 이상** = 기여도 충족
- 충족: 기본 보상 (확정 동일) + 희귀 보상 (개인 독립 확률)
- 미충족: 보상 없음

### 7.5 잠수 방지

- 분당 10마리 미만 처치 → 스폰 계산 제외
- 5분 이상 위치 변화 없음 → 메인 스폰으로 강제 텔포

---

## 8. 보스 시스템

### 8.1 필드보스

| 항목 | 값 |
|---|---|
| 스폰 주기 | 30분 정시 (매 :00분, :30분) |
| 자동 소멸 | 스폰 후 5분 내 미처치 |
| 타임아웃 | 첫 피해 기산 15분 |
| 알림 | 필드 채팅 5분/3분/1분 전 + Discord `#필드보스-알림` |

**필드보스 5종 드랍**

| 보스 | 필드 | 고대흔적 드랍 | 장비의 흔적 드랍 |
|---|---|---|---|
| 들판 포식자 | 수도 외곽 평원 | faded 5% | broken 20% / faded 5% |
| 폐광 골렘 | 폐광 지대 | faded 8% / glowing 1% | broken 15% / faded 10% / glowing 2% |
| 수로 군주 | 오염된 수로 | faded 12% / glowing 3% | faded 15% / glowing 5% |
| 타락 기사장 | 무너진 초소 | faded 8% / glowing 15% / radiant 2% | faded 10% / glowing 20% / radiant 3% |
| 균열 파수꾼 | 고대 성벽 잔해 | glowing 20% / radiant 5% / brilliant 0.5% | glowing 25% / radiant 8% / brilliant 1% |

드랍 전부 **개인 독립 확률** (경매 없음).

### 8.2 시즌보스

입장형/파티형. 주간 입장 횟수 제한 없음. 강화석 드랍 없음 → 무제한 클리어 허용.

| 보스 | boss_id | 목표 클리어율 | 고대흔적 (확정) | 장비의 흔적 (확률) |
|---|---|---:|---|---|
| Earth Tyrant | `season_boss_1` | ~70% | ancient_trace_glowing × 1 | equip_trace_glowing 25% |
| Steel Arbiter | `season_boss_2` | ~50% | ancient_trace_radiant × 1 | equip_trace_radiant 20% |
| Abyss Overlord | `season_boss_3` | ~40% | ancient_trace_brilliant × 1 | equip_trace_brilliant 15% |
| 균열왕 | `season_boss_final` | ~10% | ancient_trace_brilliant × 1 | equip_trace_brilliant 30% |

**클리어 보상 경매:**
```
솔로 → 즉시 지급
파티 → /bid <금액> 채팅 → 최고 입찰자 획득 → 입찰 골드 파티원 균등 분배
```

**균열왕 HP 기준:** 22강 무기 파티 3인이 15분 내 클리어 가능한 수치로 역산.

### 8.3 보스 패턴 모듈 10종

| 코드 | 이름 | 분류 |
|---|---|---|
| P-01 | 전방 강타 | 근접 공격 |
| P-02 | 직선 돌진 | 이동+공격 |
| P-03 | 원형 폭발 | 광역 공격 |
| P-04 | 부채꼴 휩쓸기 | 근접 광역 |
| P-05 | 투사체 발사 | 원거리 |
| P-06 | 산개탄 | 광역 원거리 |
| P-07 | 소환체 호출 | 소환 |
| P-08 | 페이즈 전환 | 상태 변환 |
| P-09 | 무력화 + 딜타임 | 공략 메카닉 |
| P-10 | 안전지대 | 위치 메카닉 |

전 보스 공통 타임아웃: 15분 (첫 피해 시점 기산).

---

## 9. GUI 전체

### 9.1 공통 규칙

| 규칙 | 구현 |
|---|---|
| 빈 슬롯 | `BLACK_STAINED_GLASS_PANE`, 이름 없음 |
| 모든 클릭 | `InventoryClickEvent.setCancelled(true)` 기본 |
| 전체 입금 슬롯 등 특수 버튼 | PDC `poro:gui_button` 태그로 구분 |
| 뒤로가기 | ESC 또는 슬롯 45/18 등 → 상위 GUI 재오픈 |
| 진입 | 나침반(`PlayerInteractEvent`) 또는 `/메뉴` |
| 전투 중 차단 | `CombatStateService.isInCombat()` → GUI 오픈 차단 |

**타이틀 색상 계층:**  
허브(메인) = `§6골드` / 서브허브 = `§e노랑` / 작업 GUI = `§f흰색`

### 9.2 GUI 계층

```
메인 GUI (54슬롯) ← menu_main.png
  ├── 장비 GUI (54슬롯) ← menu_equipment.png
  │       강화 / 잠재(큐브) / 각인 / 캐릭터 / 전승
  ├── 영지 GUI (54슬롯) ← menu_territory.png
  │       영지이동 / 영지상태 / 창고 / 공방 / 작물관리 / 상점 / 경매장 / 영지설정
  ├── 보스 GUI (54슬롯) ← menu_boss.png
  │       파티생성 / 파티목록 / 보스정보 / 클리어기록
  └── 필드 GUI (바닐라 아이콘)
          6필드 + 훈련장 아이콘 → 클릭 시 텔포
```

**메인 GUI 배경 글리프 (font: `poro:gui`)**

| GUI | 글리프 | PNG | ascent | height |
|---|---|---|---:|---:|
| 메인 | `` | `menu_main.png` | 116 | 141 |
| 장비 | `` | `menu_equipment.png` | 116 | 141 |
| 영지 | `` | `menu_territory.png` | 116 | 141 |
| 보스 | `` | `menu_boss.png` | 116 | 141 |

### 9.3 메인 GUI 슬롯 매핑 (54슬롯)

```
     col0  col1  col2  col3  col4  col5  col6  col7  col8
row0 [장비][장비][장비][유리][유리][영지][영지][영지][유리]
row1 [장비][장비][장비][유리][유리][영지][영지][영지][유리]
row2 [장비][장비][장비][유리][유리][영지][영지][영지][유리]
row3 [보스][보스][보스][유리][유리][필드][필드][필드][유리]
row4 [보스][보스][보스][유리][유리][필드][필드][필드][유리]
row5 [보스][보스][보스][유리][유리][필드][필드][필드][유리]
```

| 구역 | 슬롯 | 이동 |
|---|---|---|
| 장비 | 0,1,2,9,10,11,18,19,20 | 장비 GUI |
| 영지 | 5,6,7,14,15,16,23,24,25 | 영지 GUI |
| 보스 | 27,28,29,36,37,38,45,46,47 | 보스 GUI |
| 필드 | 32,33,34,41,42,43,50,51,52 | 필드 GUI |
| 유리판 | 나머지 18슬롯 | — |

### 9.4 장비 GUI 슬롯 매핑 (54슬롯)

```
     col0  col1  col2  col3  col4  col5  col6  col7  col8
row0 [강화][강화][유리][캐릭][캐릭][캐릭][유리][각인][각인]
row1 [강화][강화][유리][캐릭][캐릭][캐릭][유리][각인][각인]
row2 [유리][유리][유리][캐릭][캐릭][캐릭][유리][유리][유리]
row3 [큐브][큐브][유리][유리][유리][유리][유리][전승][전승]
row4 [큐브][큐브][유리][유리][유리][유리][유리][전승][전승]
row5 [유리×9]
```

| 구역 | 슬롯 | 이동 |
|---|---|---|
| 강화 | 0,1,9,10 | 강화 GUI |
| 캐릭터 | 3,4,5,12,13,14,21,22,23 | 캐릭터 GUI |
| 각인 | 7,8,16,17 | 각인 GUI |
| 큐브(잠재) | 27,28,36,37 | 잠재 GUI |
| 전승 | 34,35,43,44 | 전승 GUI |

### 9.5 영지 GUI 슬롯 매핑 (54슬롯)

```
     col0  col1  col2  col3  col4  col5  col6  col7  col8
row0 [영이][영이][영상][영상][유리][창고][창고][공방][공방]
row1 [영이][영이][영상][영상][유리][창고][창고][공방][공방]
row2 [유리×9]
row3 [작물][작물][상점][상점][유리][경매][경매][영설][영설]
row4 [작물][작물][상점][상점][유리][경매][경매][영설][영설]
row5 [유리×9]
```

| 구역 | 슬롯 | 이동 |
|---|---|---|
| 영지이동 | 0,1,9,10 | 영지이동 GUI (27슬롯) |
| 영지상태 | 2,3,11,12 | 영지상태 GUI (27슬롯) |
| 창고 | 5,6,14,15 | 창고 GUI (54슬롯) |
| 공방 | 7,8,16,17 | 공방 GUI (54슬롯) |
| 작물관리 | 27,28,36,37 | 작물관리 GUI (54슬롯) |
| 상점 | 29,30,38,39 | 상점 GUI (54슬롯) |
| 경매장 | 32,33,41,42 | 경매장 GUI (54슬롯) |
| 영지설정 | 34,35,43,44 | 영지설정 GUI (54슬롯) |

**영지상태 GUI (27슬롯):**

```
     col0  col1  col2  col3  col4  col5  col6  col7  col8
row0 [유리][유리][유리][유리][유리][유리][유리][유리][유리]
row1 [유리][작위][유리][시설][유리][창고요약][유리][승급][유리]
row2 [뒤로][영주][멤1][멤2][멤3][멤4][멤5][멤6][더보기]
```

| 슬롯 | 역할 |
|---|---|
| 10 | 작위 아이콘 — 현재 작위명·영지명, lore에 XZ 범위·시설슬롯 수 |
| 12 | 시설 현황 — 약초 재배지×n / 광물 채굴기×n / 공방 가공기×n |
| 14 | 창고 요약 — 주요 보유 재료 수량 (상위 5종) |
| 16 | 승급 버튼 — 조건 충족 시 클릭 → 구매 확인 서브GUI |
| 18 | ◀ 뒤로가기 |
| 19 | 영주 머리 (`PLAYER_HEAD`) |
| 20~25 | 영지민/부영주 머리 최대 6명 (부영주 먼저, 이후 영지민 joined_ts 순) |
| 26 | 멤버 7명 이상 시 `§7+{N}명 더` 유리판 |

**멤버 머리 lore 포맷:**
```
§f{닉네임}
§7역할: §6영주        ← LORD
§7역할: §b부영주      ← VICE_LORD
§7역할: §a영지민      ← RESIDENT
§8[접속 중]           ← 온라인인 경우만 추가
```

**구현 메모:**
```java
// TerritoryStatusGui.java
ItemStack buildMemberHead(UUID memberUuid, String role, boolean online) {
    ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
    SkullMeta meta = (SkullMeta) skull.getItemMeta();
    meta.setOwningPlayer(Bukkit.getOfflinePlayer(memberUuid));
    meta.displayName(Component.text(nickname, NamedTextColor.WHITE));
    List<Component> lore = new ArrayList<>();
    lore.add(Component.text("역할: " + roleLabel(role)));
    if (online) lore.add(Component.text("[접속 중]", NamedTextColor.DARK_GRAY));
    meta.lore(lore);
    skull.setItemMeta(meta);
    return skull;
}
// DB에서 island_members WHERE island_owner_uuid=? ORDER BY role DESC, joined_ts ASC
// 영주 고정(슬롯19) + 부영주 먼저 + 영지민 순
```

**영지이동 GUI (27슬롯):**

```
     col0  col1  col2  col3  col4  col5  col6  col7  col8
row0 [유리][유리][유리][유리][유리][유리][유리][유리][유리]
row1 [유리][유리][내영][유리][유리][유리][유리][유리][유리]
row2 [뒤로][유리×8]
```

| 슬롯 | 아이템 | 역할 |
|---|---|---|
| 11 | 초록 양탄자, §a내 영지로 이동, lore §7클릭하여 영지로 텔레포트 | 즉시 본인 영지 스폰 텔포 (IridiumSkyblock API) |
| 18 | ◀ 뒤로가기 | 영지 GUI로 복귀 |

**영지설정 GUI (54슬롯):**

```
     col0  col1  col2  col3  col4  col5  col6  col7  col8
row0 [공개][유리][초대][유리][세부][유리][영지민][유리][유리]
row1 [슬1 ]~[슬9 ]
row2 [슬10]~[슬18]
row3~row4 [유리×9]
row5 [뒤로][유리×8]
```

| 슬롯 | 역할 |
|---|---|
| 0 | 영지 공개 여부 토글 (§a[공개] / §c[비공개]) |
| 2 | 영지 초대 — 채팅 닉네임 입력 → 초대 전송 (§6.4 플로우) |
| 4 | 영지 세부설정 GUI 오픈 |
| 6 | 영지민 관리 GUI 오픈 |
| 9~26 | 시설 배정 슬롯 (기존 동일) |
| 45 | ◀ 뒤로가기 |

**영지민 관리 GUI (54슬롯):**

```
     col0  col1  col2  col3  col4  col5  col6  col7  col8
row0 [멤버탭][유리][방문자탭][유리][영지민탭][유리][부영주탭][유리][유리]
row1 [멤버1][멤버2][멤버3][멤버4][멤버5][멤버6][멤버7][멤버8][멤버9]
row2 [멤버10]...
row3 [유리×9]
row4 [유리×9]
row5 [뒤로][유리×8]
```

**멤버목록 탭 (슬롯 9~44: 멤버 아이콘):**

| 항목 | 내용 |
|---|---|
| 아이콘 | 플레이어 머리 아이템 |
| 이름 | §f{닉네임} §7(역할명) |
| 클릭 | 개별 멤버 관리 서브GUI (27슬롯) — 역할 변경·추방 |

**개별 멤버 관리 서브GUI (27슬롯):**

```
row0: [유리×9]
row1: [유리][머리][유리][부영주설정][유리][영지민설정][유리][추방][유리]
row2: [뒤로][유리×8]
```

| 슬롯 | 역할 |
|---|---|
| 10 | 플레이어 머리 아이콘 (닉네임/역할 표시) |
| 12 | 부영주로 변경 (영주만 가능) |
| 14 | 영지민으로 변경 |
| 16 | 추방 — 확인 서브GUI |
| 18 | ◀ 뒤로가기 (멤버목록 탭) |

**권한 탭 3개 (방문자/영지민/부영주):**

| 구성 | 내용 |
|---|---|
| 슬롯 9~19 | 권한 11종 아이콘 (ON=§a초록 유리판, OFF=§c빨간 유리판) |
| 클릭 | 해당 권한 토글 — DB `island_role_permissions` 즉시 UPDATE + 캐시 무효화 |
| 슬롯 45 | ◀ 뒤로가기 |

> 권한 탭에서 `MEMBER_MANAGE`는 방문자·영지민 탭에서 항상 비활성(회색 유리판, 클릭 불가).

**영지 세부설정 GUI (27슬롯):**

```
     col0  col1  col2  col3  col4  col5  col6  col7  col8
row0 [유리×9]
row1 [유리][씨앗][유리][시간][유리][날씨][유리][유리][유리]
row2 [뒤로][유리][작물][유리][물  ][유리][유리][유리][유리]
```

| 슬롯 | 설정 | 설명 | 기본값 |
|---|---|---|---|
| 10 | 씨앗 보호 ON/OFF | 심은 씨앗이 밟혀도 뽑히지 않음 | ON |
| 12 | 시간 설정 순환 (낮 고정 → 밤 고정 → 자유) | 영지 내 시간 고정 | 자유 |
| 14 | 날씨 설정 순환 (맑음 고정 → 비 고정 → 자유) | 영지 내 날씨 고정 | 자유 |
| 18 | ◀ 뒤로가기 | | |
| 20 | 작물 파괴 보호 ON/OFF | 다 자란 작물도 밟아도 파괴되지 않음 | ON |
| 22 | 물 파괴 보호 ON/OFF | 흘러내리는 물이 작물을 파괴하지 않음 | ON |

### 9.6 강화 GUI 슬롯 매핑 (45슬롯)

```
     col0  col1  col2  col3  col4  col5  col6  col7  col8
row0 [유리][유리][유리][유리][유리][유리][유리][유리][무기]
row1 [유리][성공%][유리][유리][유리][유리][천장][유리][투구]
row2 [유리][유리][유리][유리][미리보기][유리][유리][유리][상의]
row3 [유리][골드][유리][유리][유리][유리][강화석][유리][하의]
row4 [뒤로][유리][유리][유리][강화!][유리][유리][유리][신발]
```

| 슬롯 | 역할 |
|---|---|
| 8,17,26,35,44 | 장비 선택 (col8: 무기→투구→상의→하의→신발) |
| 10 | 성공률 표시 (동적) |
| 15 | 가호 천장 진행도 (11강 미만: 유리판) |
| 22 | 선택 장비 미리보기 |
| 28 | 골드 비용 (동적) |
| 33 | 강화석 수량 (동적) |
| 36 | ◀ 뒤로가기 |
| 40 | ▶ 강화! |

### 9.7 잠재(큐브) GUI 슬롯 매핑 (54슬롯)

```
     col0  col1  col2  col3  col4  col5  col6  col7  col8
row0 [유리][현재등급][유리][유리][유리][새등급][유리][유리][무기]
row1 [유리][현재1][유리][유리][유리][새1][유리][유리][투구]
row2 [유리][현재2][유리][미리보기][유리][새2][유리][유리][상의]
row3 [유리][현재3][유리][골드][유리][새3][유리][유리][하의]
row4 [유리][현재유지][유리][큐브!][유리][새옵선택][유리][유리][신발]
row5 [뒤로][유리×8]
```

등업 발생 시: 큐브(39)·뒤로(45) 비활성화, 현재유지(37)·새옵션(41) 강제 선택.

### 9.8 캐릭터 GUI 슬롯 매핑 (54슬롯)

```
     col0  col1  col2  col3  col4  col5  col6  col7  col8
row0 [유리][유리][유리][유리][투구][치장T][유리][유리][스탯]
row1 [유리][유리][무기][치장W][상의][치장C][유리][유리][유리]
row2 [유리][유리][유리][유리][하의][치장L][유리][유리][유리]
row3 [유리][유리][유리][유리][신발][치장B][유리][유리][유리]
row4 [유리][LC][유리][RC][유리][SRC][유리][F][유리]
row5 [뒤로][유리][일괄숨][유리][유리][유리][일괄보][유리][유리]
```

스탯 아이콘 lore: 최대HP / 공격력 / 방어력 / 치명타확률 / 치명타데미지 / 보스피해 / 쿨감 / 방어력무시 / 받는피해감소 / 이동속도 / 스킬피해 / 태그피해

### 9.9 창고 GUI 슬롯 매핑 (54슬롯)

```
     col0~col8
row0~row4: 아이템 (45칸)
row5: [전입][유리][◀][유리][유리][유리][▶][유리][뒤로]
```

슬롯 45=전체입금 / 47=◀이전 / 51=▶다음 / 53=뒤로가기

### 9.10 공방 GUI 슬롯 매핑 (54슬롯)

```
row0: [영지][제련][정제][연치][연부][요리][유리×3]
row1: [레시피 1~9]
row2: [대기열 1~9]
row3: [대기열 10~18]
row4: [유리][유리][◀대기][유리×3][▶대기][유리×2]
row5: [뒤로][유리×8]
```

탭 강조: 현재 선택 탭 = `LIME_STAINED_GLASS_PANE`

### 9.11 파티 시스템 GUI

**파티생성 GUI (54슬롯):**
- 슬롯 0=ET탭 / 2=SA탭 / 4=AO탭 / 6=균열왕탭 / 22=보스정보 / 45=뒤로 / 49=생성!
- 생성 클릭 → GUI 닫힘 → 채팅 30초 제목 입력 → 파티 현황 GUI 자동 오픈
- **영지 내에서만 생성 가능**

**파티 현황 GUI (54슬롯):**
- 슬롯 1=파티제목 / 7=보스아이콘 / 9~12=멤버(최대4명) / 45=입장!(리더만) / 53=해산(리더)/탈퇴
- ESC 또는 탈퇴 버튼 → 채팅 확인창 → 예/아니요

**파티 스코어보드:**
```
§6[파티]
§f{닉네임1} §a{현재HP}
...
```
파티 해산/탈퇴 시 제거.

### 9.12 무기 선택 GUI (54슬롯, 튜토리얼)

```
     col0~col8
row1: [유리][검][유리][유리][도끼][유리][유리][창][유리]
row4: [유리][석궁][유리][유리][낫][유리][유리][스태프][유리]
나머지: 유리판
```

| 슬롯 | 아이템 |
|---|---|
| 10 | 제국 장검 |
| 13 | 제국 전투도끼 |
| 16 | 제국 장창 |
| 37 | 제국 석궁 |
| 40 | 제국 낫 |
| 43 | 제국 스태프 |

클릭 → 재확인 서브GUI(27슬롯) → 확인 → 클래스 확정

### 9.13 필드 GUI 슬롯 매핑 (54슬롯)

```
row0: [평원][폐광][수로][초소][성벽][훈련][유리×3]
row5: [뒤로][유리×8]
나머지: 유리판
```

슬롯 0~5: 각 필드/훈련장 텔포. 슬롯 45: 뒤로가기.

---

## 10. 훈련장

위치: 제국 수도 내 구역. 필드 GUI `[훈련장]`으로 접근.

| 기능 | 설명 |
|---|---|
| 허수아비 | 사실상 무한 HP, 공격 없음, 드랍/경험치 없음 |
| 피해 홀로그램 | 타격마다 피해량 DecentHolograms으로 표시 |
| 딜 측정 | 10초/30초/60초 (채팅 입력 또는 GUI 선택) |
| 훈련 설정 | 허수아비 방어력: 필드 프리셋 / 보스 프리셋 / 직접 입력 |
| 훈련 기록 | 최근 10건 열람 |

훈련장 내: IL 경고, 잠수 방지, 기여도 산정 비적용.

---

## 11. HTTP API

포트: **8765**. 내부 Discord 봇/관리 도구 전용.

| 메서드 | 경로 | 설명 |
|---|---|---|
| POST | `/auth/pending` | 인증 코드 생성 |
| GET | `/auth/role-queue` | 역할 부여 큐 조회 |
| POST | `/auth/role-granted` | 역할 부여 완료 |
| GET | `/field-status` | 필드보스 현황 |
| GET | `/operations/admin/dashboard` | 관리자 대시보드 |
| GET | `/admin/players/{userId}` | 플레이어 상세 |
| GET | `/operations/snapshot` | Discord 카드용 스냅샷 |

---

## 12. 관리자 커맨드 (`/empire`)

권한: `empire.admin`. 형식: `/empire <category> <subcommand> [args]`

### GUI 강제 오픈

`/empire gui <player> <main|equipment|territory|boss|storage|workshop|enhance|potential|succession|estate-setting|shop>`

### 경제

| 커맨드 | 동작 |
|---|---|
| `/empire gold give|take|set|check <player> [amount]` | 골드 조작/확인 |
| `/empire item give <player> <item-id> [amount]` | 커스텀 아이템 지급 |
| `/empire item storage give <player> <item-id> <amount>` | 창고에 아이템 추가 |

### 영지

| 커맨드 | 동작 |
|---|---|
| `/empire estate rank set <player> <rank>` | 작위 설정 |
| `/empire estate slot set <player> <num> <herb|ore|workshop>` | 시설 슬롯 배정 |
| `/empire estate slot reset <player>` | 슬롯 전체 초기화 |
| `/empire estate slot list <player>` | 슬롯 현황 |
| `/empire estate produce <player>` | 즉시 생산 1사이클 (테스트) |
| `/empire estate public set <player> <true|false>` | 영지 공개 여부 강제 설정 |
| `/empire estate setting set <player> <key> <value>` | 세부설정 강제 변경 (key: seed_protect/time_mode/weather_mode/crop_protect/water_protect) |
| `/empire estate member add <owner> <member> <RESIDENT\|VICE_LORD>` | 영지민 강제 추가 |
| `/empire estate member remove <owner> <member>` | 영지민 강제 제거 |
| `/empire estate member list <owner>` | 영지민 목록 출력 |
| `/empire estate permission set <owner> <VISITOR\|RESIDENT\|VICE_LORD> <permission> <true\|false>` | 권한 강제 변경 |
| `/empire estate permission reset <owner>` | 전체 권한 기본값으로 초기화 |

**내부 커맨드 (ClickEvent 전용, 플레이어 직접 입력 불가):**

| 커맨드 | 동작 |
|---|---|
| `/empire visit accept <requesterUUID>` | 방문 요청 수락 |
| `/empire visit deny <requesterUUID>` | 방문 요청 거절 |
| `/empire island invite accept <inviterUUID>` | 영지 초대 수락 |
| `/empire island invite deny <inviterUUID>` | 영지 초대 거절 |

### 장비 성장

| 커맨드 | 동작 |
|---|---|
| `/empire enhance set <player> <slot> <level>` | 강화 수치 강제 설정 |
| `/empire potential reroll <player> <slot>` | 잠재 강제 재롤 |
| `/empire potential set <player> <slot> <grade>` | 잠재 등급 강제 설정 |
| `/empire succession give <player> <equip-trace-id>` | 장비의 흔적 지급 |

### 포션·버프

| 커맨드 | 동작 |
|---|---|
| `/empire potion reset <player>` | 보스전 포션 횟수 초기화 |
| `/empire buff clear|check <player>` | 버프 제거/확인 |

### 보스

| 커맨드 | 동작 |
|---|---|
| `/empire boss spawn <boss-id> [location]` | 보스 강제 소환 |
| `/empire boss kill <boss-id>` | 보스 즉시 처치 |
| `/empire boss clearrecord set|reset <player> <boss-id>` | 클리어 기록 조작 |
| `/empire boss drop simulate <boss-id>` | 드랍 테이블 시뮬레이션 |

### 상태 조회 / 시스템

| 커맨드 | 동작 |
|---|---|
| `/empire check <player> [estate|equipment]` | 플레이어 상태 요약 |
| `/empire reload [drops|recipes]` | 설정·레지스트리 리로드 |

---

## 13. 개발 Phase

| Phase | 핵심 작업 | 주요 클래스/모듈 |
|---|---|---|
| 1. 기반 | DB 초기화, 플레이어 JSON, 권한, 월드 구조 | CommonFoundationBootstrap, MasterRegistryBootstrap |
| 2. 디스코드 인증 | 약관 동의, 임시 화이트리스트, `/연동`, 역할 부여 | AuthService, HttpApiServer |
| 3. 전투 코어 | 6무기 스킬, 입력, 쿨타임, 자원, 각인 | CombatEngineBootstrap, CooldownManager, ResourceTracker |
| 4. 장비 성장 | T1 장비, 강화(pity), 잠재(큐브/메모리얼), 전승 | GrowthEngineBootstrap, EnhancementService, PotentialService |
| 5. 영지 | 저장고, 약초재배지, 광물채굴기, 공방 대기열 | LifeEngineBootstrap, MachineProductionScheduler |
| 6. 필드/보스 | 필드 5개, 드랍, 기여도, 필드보스 5개, 시즌보스 4개 | BossEngineBootstrap, FieldBossScheduler, BossContributionTracker |
| 7. 웹/디코봇/통계 | 필드보스 알림, 유저 조회, 관리자 통계 | OperationsQueryBootstrap, DiscordWebhookService |
| 8. 치장/리소스팩 | 치장 소유권, 무기 치장 6종, GUI/HUD 완성 | CosmeticService, HUD bitmap font |

> **도감(컬렉션): 1차 시즌 전면 제외.**

---

## 부록 A. 시스템 메시지 포맷

접두어: `§8[§e포로§8] `

| 이벤트 | 메시지 |
|---|---|
| 강화 성공 | `[포로] §a✔ {슬롯} §e{N}강 §a강화 성공!` |
| 강화 실패 | `[포로] §c✘ 강화 실패.` |
| 잠재 재설정 | `[포로] §d잠재능력이 재설정되었습니다. §7(큐브 -1)` |
| 가공 완료 | `[포로] §e{아이템명} 제작 완료. 영지 저장고에 입고되었습니다.` |
| 전투 중 차단 | `[포로] §c전투 중에는 사용할 수 없습니다.` |
| IL 경고 1~2회 | `[포로] §c경고: 적정 아이템 레벨 초과. (경고 {N}/3회)` |
| IL 경고 3회 | `[포로] §4경고 3회 누적. 관리자 확인 후 복구됩니다.` |
| 필드보스 등장 | `[포로] §c⚔ §f{보스명}§c이(가) 출현했습니다!` |
| 필드보스 처치 | `[포로] §a✔ §f{보스명}§a 처치! 기여도 달성 모험가에게 보상이 지급됩니다.` |
| 시즌보스 처치 | `[포로] §5★ {파티원 목록}이(가) {보스명}을(를) 처치했습니다!` |
| 균열왕 처치 | `[포로] §6★★ {파티원 목록}이(가) 균열왕을 처치했습니다!` |

## 부록 B. 미확정 항목 (M-tags)

| # | 항목 | 상태 |
|---|---|---|
| M-1 | 강화석 몹별 드랍률 최종 수치 | 검토 중 |
| M-2 | 강화 성공률 21~25강 재조정 | 검토 중 |
| M-4 | 전승권 비용 | 오픈 후 7~14일차 흔적 시세 확인 후 결정 |
| M-6 | 태양의 흔적 아이템 정의 (어떤 시드 파일에도 미존재) | 확정 필요 |
