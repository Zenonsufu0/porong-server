# 구현 설계계획서

> **[STATUS: REFERENCE]** — 현재 스텁 구현을 본격 구현으로 확장하기 전 확정해야 할 설계·검증 계획.
>
> 작성: 2026-05-25

---

## 0. 목적

현재 EmpireRPG 구현은 컴파일 가능한 스텁과 일부 최소 동작 보정까지 진행된 상태다.
이 문서는 추가 구현을 바로 진행하지 않고, 설계 기준을 먼저 고정하기 위한 계획서다.

목표는 다음 네 가지다.

- 코드가 CANON·item master·드랍표와 다른 식별자를 사용하지 않게 한다.
- GUI/입력/드랍처럼 런타임에서만 드러나는 흐름을 구현 전에 표로 확정한다.
- MythicMobs, IridiumSkyblock, Paper API 경계에서 EmpireRPG가 소유할 데이터와 외부 플러그인 껍데기를 분리한다.
- 구현 완료 기준과 검증 명령을 Phase별로 명확히 둔다.

---

## 1. 설계계획서 작성 범위

| 구분 | 작성 대상 | 기준 문서 |
|---|---|---|
| 아이템·재화 키 | item_id, currency key, PDC key, CMD | `docs/02_database_api_stats/item_master_v1.md` |
| 클래스 선택 | GUI slot, 지급 장비, 첫 접속 상태 전이 | `docs/02_database_api_stats/item_master_v1.md`, `docs/08_resourcepack_pipeline/gui_hub_structure.md` |
| 스킬 입력 | 우클릭/F키/쿨다운/무기 판정/PDC | `docs/04_combat_weapon_skills/weapon_skills_v1.md` |
| 필드 드랍 | MythicMob 식별자, 필드/정예 판정, 보상 지급처 | `docs/06_fields_bosses/drop_tables_v1.md` |
| 저장·복원 | JSON schema, slot 호환, autosave, quit save | `docs/01_plugin_architecture/empire_rpg_module_design.md` |
| GUI 라우팅 | 메인 허브, 성장, 영지, 보스, 경매장 | `docs/08_resourcepack_pipeline/gui_hub_structure.md` |
| 검증 | compile/test/manual QA/server smoke | 이 문서 §7 |

범위 밖 항목:

- 리소스팩 실제 제작
- 보스 패턴 세부 수치 재산정
- 웹 대시보드 구현
- Discord 봇 구현
- 2차 확장 기능: 도감, 외부 고퀄 보스 모델, 세트 장비, 악세서리

---

## 2. 현재 임시 구현 고정 상태

| 영역 | 현재 상태 | 설계 확정 전 주의 |
|---|---|---|
| 저장소 | 플레이어 JSON load/save 최소 구현 | schema migration 규칙 필요 |
| EquipmentSlot | `WEAPON/HELMET/CHESTPLATE/LEGGINGS/BOOTS` 기준으로 정리 | 기존 `ARMOR_*` 저장값 호환 유지 |
| 무기 판정 | PDC `empire_rpg:weapon_type` 우선, material fallback | 리소스팩 CMD와 PDC 동시 부여 규칙 필요 |
| 클래스 선택 | 명령어와 GUI listener에서 최소 지급 처리 | GUI title/slot 공식화 필요 |
| 스킬 입력 | 우클릭/F키를 기본/특수 스킬에 임시 연결 | 최종 입력 매핑표 필요 |
| 필드 드랍 | 태그/이름 기반 필드 추정, 실패 시 미지급 | MythicMob internal name 또는 scoreboard tag 확정 필요 |
| 강화석 | `mat_stone_enhance`로 currency key 통일 | 기존 저장 파일의 `enhancement_stone` migration 필요 여부 검토 |

---

## 3. 확정해야 할 식별자 표

### 3.1 Currency Key

| 의미 | 공식 key | 저장 위치 | 비고 |
|---|---|---|---|
| 골드 | `gold` | `PlayerGrowthState.wallet` | DB/API에서도 동일 key 사용 |
| 강화석 | `mat_stone_enhance` | `PlayerGrowthState.wallet` | 실물 아이템 아님 |
| 큐브 조각 | `mat_cube_fragment` | `IslandTerritoryState.customItems` 또는 별도 wallet 확정 필요 | 10개 자동 교환 |
| 큐브 | `mat_cube` | `IslandTerritoryState.customItems` 또는 별도 wallet 확정 필요 | 사용 시 500G |

결정 필요:

- 큐브 조각/큐브를 `customItems`에 둘지 `wallet`에 둘지 하나로 고정한다.
- `PotentialService.MATERIAL_MEMORY_CUBE`, `MATERIAL_UPGRADE_CUBE`는 현 item master와 충돌하므로 유지/삭제/alias 중 하나로 결정한다.

### 3.2 PDC Key

| key | 용도 | 값 |
|---|---|---|
| `empire_rpg:weapon_type` | 무기 클래스 판정 | `SWORD/AXE/SPEAR/CROSSBOW/SCYTHE/STAFF` |
| `empire_rpg:item_id` | item master 식별 | `equip_spear`, `mat_cube` 등 |
| `empire_rpg:instance_id` | 장비 인스턴스 식별 | UUID 또는 `starter_*` |

결정 필요:

- 장비 완제품은 반드시 `item_id`와 `instance_id`를 함께 가진다.
- 소재·소비품은 `item_id`만 가진다.
- PDC 누락 아이템을 허용할지, 관리자 복구 명령으로만 변환할지 결정한다.

---

## 4. 도메인별 설계 작성 작업

### 4.1 클래스 선택·초기 지급

작성할 표:

| 항목 | 확정할 내용 |
|---|---|
| GUI title | 클래스 선택 인벤토리 title |
| slot map | 검/도끼/창/석궁/낫/스태프 슬롯 |
| 지급 장비 | 무기 1개 + 방어구 4개 |
| 재선택 정책 | 1차 시즌 재선택 불가 / 관리자만 변경 |
| 저장 시점 | 선택 즉시 save 예약 또는 즉시 save |

완료 기준:

- `/empire class <type>`와 GUI 선택이 같은 서비스 메서드를 호출한다.
- 선택 후 `PlayerGrowthState`에 5슬롯 장비가 존재하고 장착 상태다.
- 창은 `minecraft:netherite_sword`여도 PDC로 `SPEAR` 판정된다.

### 4.2 스킬 입력

작성할 표:

| 입력 | 의미 | 1차 구현 |
|---|---|---|
| 우클릭 | 기본 스킬 | 무기별 1번 스킬 |
| F키 | 특수 스킬 | 무기별 4번 스킬 |
| Shift+우클릭 | 보류 | 설계 확정 전 미구현 |
| 숫자키/핫바 | 보류 | GUI/핫바 설계 후 결정 |

결정 필요:

- 4개 스킬 전체를 어떤 입력에 배치할지 확정한다.
- 전투 중 GUI 열기, 블록 상호작용, 석궁 장전과 충돌하는 이벤트 우선순위를 정한다.
- 쿨다운 actionbar 메시지 형식을 고정한다.

### 4.3 필드 드랍·MythicMobs 태그

필수 태그 규칙:

| 태그 | 값 예시 | 용도 |
|---|---|---|
| `empire_field` | `1~5` | 필드 드랍표 선택 |
| `empire_mob_rank` | `normal/elite/boss` | 일반/정예/보스 드랍 분기 |
| `empire_mob_id` | `field1_wolf` | 운영 로그와 디버그 |

구현 원칙:

- 위 태그가 없으면 드랍을 지급하지 않는다.
- customName 기반 판정은 디버그 fallback으로만 사용하고 운영 기준으로 쓰지 않는다.
- 필드보스와 시즌보스 보상은 일반 `EntityDeathEvent`가 아니라 보스 기여도 서비스에서 지급한다.

### 4.4 저장·복원

작성할 표:

| 항목 | 정책 |
|---|---|
| 저장 파일 | `plugins/EmpireRPG/playerdata/{uuid}.json` |
| 자동 저장 | 5분 주기, 메인 스레드 UUID snapshot 후 async file I/O |
| 퇴장 저장 | quit event에서 즉시 save 후 cache 제거 |
| 종료 저장 | onDisable에서 온라인 전원 save |
| migration | `schemaVersion`별 변환 메서드 |

결정 필요:

- `enhancement_stone` → `mat_stone_enhance` 기존 저장값 migration 여부.
- `ARMOR_*` → 5슬롯 명칭 변환은 `EquipmentSlot.from()`으로만 처리한다.
- JSON 저장 실패 시 운영 로그/재시도 정책.

### 4.5 GUI 라우팅

작성할 표:

| GUI | title | open 경로 | listener |
|---|---|---|---|
| 메인 허브 | 확정 필요 | `/메뉴`, 핫바 메뉴 | `MainHubListener` |
| 클래스 선택 | 확정 필요 | 첫 접속/튜토리얼 | `WeaponSelectionGuiListener` |
| 성장 | 확정 필요 | `/장비`, `/강화` | `GrowthGuiListener` |
| 영지 | 확정 필요 | `/영지` | `TerritoryStatusGuiListener` |
| 보스 | 확정 필요 | `/보스` | `BossRoomListener` |

원칙:

- listener가 title 문자열을 임의 추론하지 않도록 GUI별 상수 title을 둔다.
- slot 번호는 문서 표와 코드 상수를 1:1로 맞춘다.
- GUI 클릭은 항상 `event.setCancelled(true)`를 기본값으로 둔다.

---

## 5. 구현 중단선

설계계획서가 확정되기 전에는 아래 구현을 추가하지 않는다.

- 신규 스킬 효과 상세 구현
- 신규 GUI 화면 확장
- 보스 보상 지급 상세 구현
- 경매장/상점 경제 처리
- 웹/API 엔드포인트 구현
- MythicMobs 실제 스폰 설정 변경

허용되는 작업:

- 컴파일 실패 수정
- 명백한 식별자 오탈자 수정
- 문서와 코드의 명칭 표 대조
- 설계계획서 작성 및 리뷰

---

## 6. 작성 순서

1. item_id/currency/PDC 식별자 표 확정
2. 클래스 선택·초기 지급 상태 전이표 작성
3. 스킬 입력 매핑표 작성
4. MythicMobs 태그 규칙과 드랍 지급 경계 작성
5. 저장·복원 schema/migration 작성
6. GUI title/slot/listener 표 작성
7. 검증 체크리스트 작성
8. CANON 반영 필요 여부 판정
9. decision_log 기록 필요 여부 판정

---

## 7. 검증 계획

### 7.1 정적 검증

| 검증 | 명령 |
|---|---|
| 컴파일 | `./gradlew compileJava` |
| 공백 오류 | `git diff --check` |
| 문서 상태 | `orc status` |
| review diff | `orc diff-review` |

### 7.2 수동 QA

| 시나리오 | 기대 결과 |
|---|---|
| 신규 유저 접속 → 클래스 선택 | 무기 1개 + 방어구 4개 지급, JSON 저장 |
| 창 선택 후 우클릭/F키 | 창 스킬로 판정 |
| 태그 없는 엔티티 처치 | 골드/재료 미지급 |
| `empire_field=3`, `empire_mob_rank=elite` 처치 | 필드3 정예 드랍표 적용 |
| 재접속 | weaponType, 장비, wallet, storage 복원 |
| 10 큐브 조각 획득 | 큐브 1개 자동 교환 |

### 7.3 운영 로그

필수 로그:

- 클래스 선택 완료
- 필드 드랍 지급
- 저장 실패
- JSON migration 수행
- 태그 누락 몹 드랍 skip

---

## 8. 산출물

| 산출물 | 위치 |
|---|---|
| 최종 구현 설계계획서 | 이 문서 |
| 식별자 표 보강 | `docs/02_database_api_stats/item_master_v1.md` 필요 시 |
| GUI title/slot 확정 | `docs/08_resourcepack_pipeline/gui_hub_structure.md` 또는 신규 GUI 문서 |
| MythicMobs 태그 규칙 | `docs/06_fields_bosses/` 하위 신규 문서 |
| 저장 schema/migration | `docs/01_plugin_architecture/` 하위 신규 문서 |
| 결정 로그 | `docs/decision_log.md` 필요 시 |

---

## 9. 다음 결정 필요 항목

| ID | 항목 | 선택지 | 권장 |
|---|---|---|---|
| DP-001 | 큐브/큐브 조각 저장 위치 | `wallet` / `customItems` | `wallet`로 통일 |
| DP-002 | 스킬 4종 입력 배치 | 우클릭/F/Shift/핫바 | 우클릭+F만 1차, 나머지 보류 |
| DP-003 | MythicMob 필드 식별 | 이름 추론 / scoreboard tag / PDC | scoreboard tag |
| DP-004 | 클래스 선택 GUI title | 임의 문자열 / 상수화 | 코드 상수 + 문서 표 |
| DP-005 | 저장 migration | 미지원 / schema별 변환 | schema별 변환 |
| DP-006 | 필드보스 보상 지급 | death event / boss contribution service | boss contribution service |

---

## 10. 완료 기준

이 설계계획서는 아래 조건을 만족하면 작성 완료로 본다.

- §9 결정 필요 항목이 모두 확정 또는 보류 사유와 함께 정리된다.
- CANON 변경이 필요한 항목과 상세 구현 문서에 둘 항목이 분리된다.
- 구현자가 코드 수정 없이도 GUI title, slot, key, tag, 저장 schema를 확인할 수 있다.
- 수동 QA 시나리오가 실제 서버 테스트 순서로 실행 가능하다.
