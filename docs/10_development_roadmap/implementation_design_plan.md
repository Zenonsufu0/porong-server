# 구현 설계계획서

> **[STATUS: REFERENCE]** — 현재 스텁 구현을 본격 구현으로 확장하기 전 확정해야 할 설계·검증 계획.
>
> 최초 작성: 2026-05-25 / 확정 반영: 2026-05-25

---

## 0. 목적

현재 EmpireRPG 구현은 컴파일 가능한 스텁과 일부 최소 동작 보정까지 진행된 상태다.
이 문서는 추가 구현을 진행하기 전 설계 기준을 고정한다.

목표:

- 코드가 CANON·item master·드랍표와 다른 식별자를 사용하지 않게 한다.
- GUI/입력/드랍처럼 런타임에서만 드러나는 흐름을 구현 전에 표로 확정한다.
- MythicMobs, IridiumSkyblock, Paper API 경계에서 EmpireRPG가 소유할 데이터와 외부 플러그인 껍데기를 분리한다.
- 구현 완료 기준과 검증 명령을 Phase별로 명확히 둔다.

---

## 1. 설계계획서 범위

범위 내:

| 구분 | 작성 대상 | 기준 문서 |
|---|---|---|
| 아이템·재화 키 | item_id, currency key, PDC key, CMD | `docs/02_database_api_stats/item_master_v1.md` |
| 클래스 선택 | GUI slot, 지급 장비, 첫 접속 상태 전이 | `docs/02_database_api_stats/item_master_v1.md` |
| 스킬 입력 | LMB/RMB/Shift+RMB/F 매핑, 이벤트 처리 | `docs/04_combat_weapon_skills/weapon_skills_v1.md` |
| 필드 드랍 | MythicMob 태그 규칙, 보상 지급 경계 | `docs/06_fields_bosses/drop_tables_v1.md` |
| 저장·복원 | JSON schema v3, migration 정책 | `PlayerSaveData.java` |
| GUI 라우팅 | title 상수, slot 매핑, listener 연결 | `docs/08_resourcepack_pipeline/gui_hub_structure.md` |
| 검증 | compile/수동 QA 체크리스트 | 이 문서 §7 |

범위 밖:

- 리소스팩 실제 제작
- 보스 패턴 세부 수치 재산정
- 웹 대시보드 구현
- Discord 봇 구현
- 2차 확장: 도감, 세트 장비, 악세서리, 외부 보스 모델

---

## 2. 현재 임시 구현 고정 상태

| 영역 | 현재 상태 | 비고 |
|---|---|---|
| 저장소 | `PlayerSaveData` schemaVersion=3, Gson JSON | §5 참조 |
| EquipmentSlot | `WEAPON/HELMET/CHESTPLATE/LEGGINGS/BOOTS` | `ARMOR_*` 역호환: `EquipmentSlot.from()` |
| 무기 판정 | PDC `empire_rpg:weapon_type` 우선, material fallback | §3.2 참조 |
| 클래스 선택 | WeaponSelectionGuiListener, slot 10~15 | §4.1 참조 |
| 스킬 입력 | SkillInputListener RMB/F만 임시 연결 → 4종으로 수정 필요 | §4.2 참조 |
| 필드 드랍 | 이름 기반 fieldIndex (임시) → scoreboard tag로 교체 | §4.3 참조 |
| 큐브 | PotentialService memory_cube/upgrade_cube → mat_cube 교체 필요 | §3.1 참조 |

---

## 3. 식별자 확정표

### 3.1 재화·아이템 저장 기준

`PlayerGrowthState.wallet` Map<String, Long>에 저장.

| 의미 | 공식 key | 저장 위치 | 비고 |
|---|---|---|---|
| 골드 | `gold` | `PlayerGrowthState.wallet` | 주 화폐 |
| 강화석 | `mat_stone_enhance` | `PlayerGrowthState.wallet` | DB 가상재화, 실물 없음 |
| 큐브 조각 | `mat_cube_fragment` | `PlayerGrowthState.wallet` | 10개 → 큐브 1개 자동 교환 |
| 큐브 | `mat_cube` | `PlayerGrowthState.wallet` | 사용 시 500G 추가 차감 |

> **DP-001 확정**: 큐브/큐브 조각은 wallet 가상재화로 통일한다. 10개 자동 교환 및 사용 처리를 wallet 수치 조작만으로 처리해 인벤토리 이벤트 의존을 제거한다.

**PotentialService 수정 필요 항목:**

| 현재 (구버전) | 교체 후 (공식) |
|---|---|
| `MATERIAL_MEMORY_CUBE = "memory_cube"` | `MATERIAL_CUBE = "mat_cube"` |
| `MATERIAL_UPGRADE_CUBE = "upgrade_cube"` | (삭제, mat_cube로 통합) |

CANON 기준: 큐브 1회 → 전 라인 재롤 + 등업 시도 동시 진행, 500G 차감.
큐브/큐브 조각은 스코어보드 HUD에 잔량 표시한다 (wallet 수치 조회).

### 3.2 PDC Key

| PDC Key (NamespacedKey) | 값 형식 | 용도 |
|---|---|---|
| `empire_rpg:weapon_type` | `SWORD/AXE/SPEAR/CROSSBOW/SCYTHE/STAFF` | 무기 클래스 판정 |
| `empire_rpg:item_id` | `equip_spear`, `mat_cube` 등 item_master key | 아이템 종류 식별 |
| `empire_rpg:instance_id` | UUID 또는 `starter_{slot}` 형식 | 장비 인스턴스 고유 식별 |

적용 규칙:

| 아이템 종류 | item_id PDC | instance_id PDC |
|---|---|---|
| 장비 완제품 (`equip_*`) | ✓ 필수 | ✓ 필수 |
| 소재/소비품 (`mat_*`, `con_*`) | ✓ 필수 | 없음 |
| PDC 없는 아이템 | 드랍/스킬 미처리, 오류 없음 | — |

---

## 4. 도메인별 설계 확정

### 4.1 클래스 선택·초기 지급

**GUI 구성:**

| 항목 | 확정 내용 |
|---|---|
| 인벤토리 크기 | 27슬롯 (3행×9열) |
| GUI title 상수 | `GuiTitles.WEAPON_SELECTION = Component.text("클래스 선택")` |
| 재선택 정책 | 1차 시즌 재선택 불가. 관리자 `/empire setclass <player> <type>`만 변경 가능 |
| 저장 시점 | 선택 즉시 동기 save (firstJoin이므로 지연 없음) |

**슬롯 매핑 (WeaponSelectionGuiListener):**

```
row0: [0] [1] [2] [3] [4] [5] [6] [7] [8]   ← 배경/타이틀
row1: [9] [검=10] [도끼=11] [창=12] [석궁=13] [낫=14] [스태프=15] [16] [17]
row2: [18] [19] [20] [21] [22] [23] [24] [25] [26]  ← 설명
```

**초기 지급 장비 (ClassInitService 구현 기준):**

| 슬롯 | item_id | instance_id |
|---|---|---|
| WEAPON | `equip_{class}` (선택한 무기) | `starter_weapon` |
| HELMET | `equip_helmet` | `starter_helmet` |
| CHESTPLATE | `equip_chestplate` | `starter_chestplate` |
| LEGGINGS | `equip_leggings` | `starter_leggings` |
| BOOTS | `equip_boots` | `starter_boots` |

장착 상태로 PlayerGrowthState에 추가 후 즉시 save.

### 4.2 스킬 입력 매핑 (DP-002 확정: 4종 모두 구현)

| 슬롯 | 입력 | 이벤트 | 조건 | 스킬 역할 |
|---|---|---|---|---|
| 1 (기본기) | LMB 공격 | `EntityDamageByEntityEvent` (damager=player) | — | 자원 생성, 기본 딜 |
| 2 (이동기) | RMB | `PlayerInteractEvent` RIGHT_CLICK | `!player.isSneaking()` | 이동기/보조기 |
| 3 (특수기) | Shift+RMB | `PlayerInteractEvent` RIGHT_CLICK | `player.isSneaking()` | 특수기/제어기 |
| 4 (핵심기) | F키 | `PlayerSwapHandItemsEvent` | — | 자원 소모, 핵심 딜 |

**이벤트 처리 원칙:**

- RMB에서 블록 상호작용 충돌 방지: `event.setUseInteractedBlock(Event.Result.DENY)` 호출
- 석궁 장전(CROSSBOW) RMB 충돌: WeaponType이 CROSSBOW면 slot2 스킬로 우선 처리하고 장전 이벤트 취소
- 쿨다운 actionbar 형식: `§e{스킬명} §c{N.N}s` (CooldownManager.formatSeconds 사용)
- 전투 불가 구역(수도 내부 등): 별도 `SafeZoneService` 또는 WorldGuard adapter를 정의한 뒤 스킬 차단. `CombatStateService`에는 safe-zone 책임을 추가하지 않는다.

**SkillInputListener 현재 버그 (수정 필요):**

| 현재 (틀림) | 수정 후 |
|---|---|
| RMB → slot1 (기본기) | RMB (not sneaking) → slot2 (이동기) |
| F키 → specialSkillKey (일부만) | F키 → slot4 (핵심기) |
| LMB 미처리 | LMB → EntityDamageByEntityEvent, slot1 (기본기) |
| Shift+RMB 미처리 | Shift+RMB → slot3 (특수기) |

**slot key 헬퍼 (SkillInputListener에 추가):**

```java
private String slot1Key(WeaponType t) {
    return switch (t) {
        case SWORD     -> "sword:flash_slash";
        case AXE       -> "axe:smash";
        case SPEAR     -> "spear:thrust";
        case CROSSBOW  -> "crossbow:rapid_fire";
        case SCYTHE    -> "scythe:death_slash";
        case STAFF     -> "staff:arcane_orb";
        default        -> null;
    };
}
private String slot2Key(WeaponType t) { /* RMB */ }
private String slot3Key(WeaponType t) { /* Shift+RMB */ }
private String slot4Key(WeaponType t) { /* F키 */ }
```

slot2~4 전체 key 표:

| 무기 | slot1 (LMB) | slot2 (RMB) | slot3 (Shift+RMB) | slot4 (F) |
|---|---|---|---|---|
| 검 | `sword:flash_slash` | `sword:triple_strike` | `sword:guard_counter` | `sword:final_strike` |
| 도끼 | `axe:smash` | `axe:crush_charge` | `axe:unyielding` | `axe:colossal_drop` |
| 창 | `spear:thrust` | `spear:crescent` | `spear:charge` | `spear:thunderstrike` |
| 석궁 | `crossbow:rapid_fire` | `crossbow:evade_fire` | `crossbow:pierce_bolt` | `crossbow:sniper` |
| 낫 | `scythe:death_slash` | `scythe:shadow_spin` | `scythe:grim_strike` | `scythe:execution` |
| 스태프 | `staff:arcane_orb` | `staff:elemental_burst` | `staff:arcane_rush` | `staff:starburst` |

### 4.3 필드 드랍·MythicMobs 태그 (DP-003 확정: scoreboard tag)

**MythicMobs scoreboard tag 규칙:**

MythicMob YAML의 `Options.Scoreboard` 항목에 아래 태그를 부여한다.

| 태그 이름 | 값 예시 | 용도 |
|---|---|---|
| `empire_field` | `empire_field_1` ~ `empire_field_5` | 드랍표 필드 번호 선택 |
| `empire_rank` | `empire_rank_normal`, `empire_rank_elite` | 일반/정예 분기 |
| `empire_type` | `empire_type_field_boss` | 필드보스 판정 (기여도 보상) |

적용 예시 (MythicMobs YAML):

```yaml
Mobs:
  PrairiWolf:
    DisplayName: "<gray>초원 늑대"
    Type: WOLF
    Options:
      Scoreboard:
        - empire_field_1
        - empire_rank_normal
```

**FieldDropListener 처리 원칙:**

- 위 scoreboard tag가 없으면 드랍을 지급하지 않고 조용히 skip (WARN 로그 없음)
- customName 기반 판정은 완전 제거
- 필드보스(`empire_type_field_boss` 태그) 처치 → §4.6 기여도 서비스로 위임

**태그 파싱 유틸 (`MobTagHelper`):**

```java
public static int fieldIndex(Entity entity) {
    for (String tag : entity.getScoreboardTags()) {
        if (tag.startsWith("empire_field_")) {
            try { return Integer.parseInt(tag.substring(13)); } catch (...) {}
        }
    }
    return 0; // 태그 없음
}
public static boolean isElite(Entity entity) {
    return entity.getScoreboardTags().contains("empire_rank_elite");
}
public static boolean isFieldBoss(Entity entity) {
    return entity.getScoreboardTags().contains("empire_type_field_boss");
}
```

### 4.4 저장·복원 (DP-005 확정: schemaVersion 기반 migration)

**JSON 파일 위치:** `plugins/EmpireRPG/playerdata/{uuid}.json`

**PlayerSaveData 현재 schema v3:**

```json
{
  "schemaVersion": 3,
  "weaponType": "SWORD",
  "classId": "sword",
  "classEngravingId": "",
  "wallet": {
    "gold": 0,
    "mat_stone_enhance": 0
  },
  "equippedSlots": {
    "WEAPON": "starter_weapon",
    "HELMET": "starter_helmet",
    "CHESTPLATE": "starter_chestplate",
    "LEGGINGS": "starter_leggings",
    "BOOTS": "starter_boots"
  },
  "inventory": [],
  "territory": {
    "ownerName": "...",
    "rankName": "FRONTIER",
    "convenienceUnlocks": 0,
    "reaperCount": 0,
    "storageCount": 0
  },
  "playerLevel": 1,
  "unspentPts": 0,
  "critPts": 0,
  "specPts": 0,
  "endurPts": 0,
  "currentExp": 0,
  "ceilingCounters": {},
  "ilWarningCount": 0,
  "mobIlHitCount": 0,
  "catalystBonusPct": 0
}
```

**migration 정책:**

| schemaVersion | 처리 |
|---|---|
| 없음 (null/0) | v1 간주 → wallet의 `enhancement_stone` 키를 `mat_stone_enhance`로 rename |
| 1 | wallet 키 정규화 후 v2로 승격 |
| 2 | equippedSlots 키 정규화 (`ARMOR_*` → EquipmentSlot.from() 처리) 후 v3으로 승격 |
| 3 | 그대로 사용 |

migration은 `PlayerPersistenceService.load()` 내부에서 버전별 분기로 처리.
저장 실패 시: `SEVERE` 레벨 로그 기록 + 재시도 없음 (데이터 손실보다 로그가 낫다).

**자동 저장/종료 저장:**

| 시점 | 방식 |
|---|---|
| 5분 주기 | 메인 스레드에서 UUID snapshot → async I/O |
| 플레이어 퇴장 | PlayerQuitEvent에서 즉시 save + cache 제거 |
| 서버 종료 | onDisable 온라인 전원 동기 save |

영지 저장고(`island_storage`)는 `docs/05_island_farm_system/island_system_design.md` 기준 DB 가상 저장고로 분리한다. Player JSON의 `storage` 필드는 제거하거나 migration 전용 legacy 필드로만 취급한다.

### 4.5 GUI title 상수 (DP-004 확정: GuiTitles 클래스)

`com.poro.empire.gui.GuiTitles` 클래스에 static final Component 상수 정의.

| 상수명 | 표시 문자열 | 인벤토리 크기 | listener |
|---|---|---|---|
| `WEAPON_SELECTION` | `클래스 선택` | 27 | `WeaponSelectionGuiListener` |
| `MAIN_HUB` | `제국의 거점` | 54 | `MainHubListener` |
| `EQUIPMENT_HUB` | `장비 관리` | 54 | `GrowthGuiListener` |
| `TERRITORY_HUB` | `영지 관리` | 54 | `TerritoryStatusGuiListener` |
| `BOSS_HUB` | `보스 도전` | 54 | `BossRoomListener` |
| `EXPLORE_HUB` | `탐험 지도` | 27 | `MainHubListener` 내 탐험 탭 |
| `STORAGE` | `영지 저장고` | 54 | `StorageGuiListener` (이미 존재) |
| `TERRITORY_STATUS` | `영지 상태` | 54 | `TerritoryStatusGuiListener` (이미 존재) |
| `WORKSHOP` | `공방` | 54 | `WorkshopGuiListener` (이미 존재) |
| `GROWTH_ENHANCE` | `강화` | 54 | `GrowthGuiListener` |
| `GROWTH_POTENTIAL` | `잠재능력` | 54 | `GrowthGuiListener` |
| `GROWTH_HEIRLOOM` | `전승` | 54 | `HeirloomGuiListener` |
| `AUCTION` | `경매장` | 54 | `AuctionGuiListener` |

listener는 title 문자열을 직접 문자열 비교하지 않고 `GuiTitles.WEAPON_SELECTION.equals(view.title())` 형식으로만 사용한다.

### 4.6 필드보스 보상 지급 (DP-006 확정: 기여도 서비스)

**기여도 기준 (DL-064):**

| 보스 종류 | 참여 판정 기준 |
|---|---|
| 필드보스 | 총 피해량 3% 이상 (ContributionTracker — double 누적) |
| 시즌보스 | 파티 보스룸 입장 확인 (방 입장 = 참여 게이트; 1차 시즌 damage_share 집계 없음) |

| 몹 종류 | 보상 지급 방식 |
|---|---|
| 일반 필드 몹 | EntityDeathEvent + MobTagHelper, 마지막 히터 단독 지급 |
| 필드보스 (`empire_type_field_boss`) | BossRewardService가 기여도 3% 이상 대상에게 지급 |
| 시즌보스 | BossRewardService가 보스룸 입장 참여자 전원에게 지급 (클리어/재도전/인원 스케일 규칙) |

책임 경계:

- `BossEngineRuntime`: 보스 세션, 피해량 누적, 클리어/실패 이벤트 발행.
- `BossRewardService`: `BossEngineRuntime`의 결과 이벤트를 받아 보상 대상과 보상 수량을 계산·지급.
- `FieldDropListener`: 일반/정예 필드몹만 처리. 필드보스/시즌보스 보상 지급에 관여하지 않는다.

---

## 5. 구현 중단선

설계계획서가 확정된 지금부터 구현 우선순위 외 항목은 차단한다.

허용:
- 컴파일 실패/식별자 오탈자 수정
- §6 작성 순서에 따른 코드 구현
- CANON 반영 필요 항목만 decision_log 기록

차단:
- 신규 스킬 효과 상세 구현 (§4.2 수정 전)
- 신규 GUI 화면 확장 (GuiTitles 상수 추가 전)
- 경매장/상점 경제 처리
- 웹/API 엔드포인트
- MythicMobs 실제 스폰 설정 변경

---

## 6. 구현 순서

| 순서 | 작업 | 영향 파일 |
|---|---|---|
| 1 | PotentialService cube key 정렬: memory_cube/upgrade_cube → 인벤토리 `mat_cube` 소비 | `PotentialService.java` |
| 2 | SkillInputListener 4종 매핑 수정 (LMB/RMB/Shift+RMB/F) | `SkillInputListener.java` |
| 3 | MobTagHelper 신규 작성 + FieldDropListener scoreboard tag 방식으로 교체 | `MobTagHelper.java`, `FieldDropListener.java` |
| 4 | GuiTitles 상수 클래스 작성 | `GuiTitles.java` |
| 5 | PlayerPersistenceService migration (v1→v2→v3) 추가 | `PlayerPersistenceService.java` |
| 6 | WeaponSelectionGuiListener GuiTitles 상수 교체 | `WeaponSelectionGuiListener.java` |
| 7 | PlayerQuitEvent save (PlayerJoinListener 또는 신규 listener) | `PlayerJoinListener.java` |
| 8 | ClassInitService 신규 작성 (초기 5슬롯 장비 지급) | `ClassInitService.java` |
| 9 | GuiTitles 기반 메인 허브 / 장비 허브 최소 구현 | `MainHubListener.java`, `GrowthGuiListener.java` |
| 10 | BossRewardService 신규 작성 + BossEngineRuntime 결과 이벤트 연동 | `BossRewardService.java`, `BossEngineRuntime` |
| 11 | SafeZoneService 또는 WorldGuard adapter 정의 후 SkillInputListener에 연결 | `SafeZoneService.java`, `SkillInputListener.java` |

---

## 7. 검증 계획

### 7.1 정적 검증

| 검증 | 명령 |
|---|---|
| 컴파일 | `./gradlew compileJava` |
| 공백 오류 | `git diff --check` |
| 문서 상태 | `orc status` |

### 7.2 수동 QA 시나리오

| 순서 | 시나리오 | 기대 결과 |
|---|---|---|
| 1 | 신규 유저 접속 | 클래스 선택 GUI(27슬롯) 자동 오픈 |
| 2 | 클래스 선택(창) | 창 무기 1개+방어구 4개 지급, PDC weapon_type=SPEAR, JSON 저장 |
| 3 | 우클릭(공기/블록) | 창 slot2 스킬 발동 (관통찌르기 아님, 반월창) |
| 4 | F키 | 창 slot4 스킬 발동 (천뢰일창) |
| 5 | Shift+우클릭 | 창 slot3 스킬 발동 (돌파창) |
| 6 | LMB 몹 공격 | 창 slot1 스킬 발동 (관통찌르기) + 자원 1스택 |
| 7 | empire_field_1 태그 몹 처치 | 필드1 드랍표 적용, gold/mat_stone_enhance 지급 |
| 8 | 태그 없는 몹 처치 | 드랍 없음, 오류 없음 |
| 9 | 재접속 | weaponType, 장비, wallet, territory 복원 |
| 10 | mat_cube_fragment 10개 획득 | wallet의 mat_cube_fragment 10 소모 → mat_cube 1 적립, 스코어보드 갱신 |

### 7.3 필수 운영 로그

| 이벤트 | 로그 형식 |
|---|---|
| 클래스 선택 완료 | `[Class] {uuid} selected {weaponType}` |
| 필드 드랍 지급 | `[Drop] {uuid} field={N} rank={normal/elite} gold={G} stone={S}` |
| 저장 실패 | SEVERE `[Persistence] 플레이어 데이터 저장 실패: {uuid}` |
| JSON migration 수행 | `[Migration] {uuid} v{old} → v{new}` |
| 태그 누락 몹 skip | (로그 없음, 조용히 skip) |
| 큐브 조각 자동 교환 | `[Cube] {uuid} 10 fragments → 1 cube (wallet)` |

---

## 8. DP 결정 요약

| ID | 항목 | 확정 내용 |
|---|---|---|
| DP-001 | 큐브/큐브 조각 저장 | wallet 가상재화로 통일 (mat_cube, mat_cube_fragment) |
| DP-002 | 스킬 4종 입력 배치 | LMB=slot1, RMB=slot2, Shift+RMB=slot3, F=slot4 (4종 모두 1차 구현) |
| DP-003 | MythicMob 필드 식별 | scoreboard tag (empire_field_N, empire_rank_*, empire_type_*) |
| DP-004 | GUI title | GuiTitles 상수 클래스, Component 비교 |
| DP-005 | 저장 migration | schemaVersion v1→v2→v3 단계별 변환 |
| DP-006 | 필드보스 보상 지급 | BossRewardService가 전담, 일반 몹은 FieldDropListener 마지막 히터 단독 |

---

## 9. 완료 기준

- 이 문서의 모든 DP 항목이 확정됨 ✓
- §6 구현 순서의 1~8이 완료되고 `./gradlew compileJava` 통과
- §7.2 수동 QA 시나리오 1~10이 실제 서버에서 통과
- CANON과 충돌하는 구버전 식별자(`enhancement_stone`, `memory_cube`, `upgrade_cube`)가 코드에서 제거됨
