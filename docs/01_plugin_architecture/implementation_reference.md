# EmpireRPG 구현 레퍼런스

> **[STATUS: REFERENCE]** — 현재 CANON 기준의 구현 진입점. 구버전 장문 구현 메모는 `docs/_archive/implementation_reference_legacy.md`로 이동됨.

## 기준 우선순위

구현 판단은 아래 순서로 한다.

| 우선순위 | 문서 | 역할 |
|---|---|---|
| 1 | `../final_master_plan.md` | 1차 시즌 범위, 시스템 경계, 폐기 기준 |
| 2 | `CANON.md` | 플러그인 경계, EmpireRPG 책임, 부트스트랩 순서 |
| 3 | 각 도메인 `CANON.md` | 성장/전투/영지/필드/GUI 도메인별 공식 기준 |
| 4 | 활성 상세 문서 | 구현 상세. CANON과 충돌 시 상세 문서를 정리한다 |
| 5 | `../_archive/` | 과거 맥락 전용. 현재 구현 기준으로 사용하지 않는다 |

## EmpireRPG 책임

EmpireRPG는 전투, 장비, 영지, 보스, 보상, DB, API, Discord/web 연동 데이터를 소유한다.

외부 플러그인은 껍데기 또는 보조 역할만 담당한다.

| 외부 플러그인 | 역할 |
|---|---|
| MythicMobs | 바닐라 기반 몹/보스 껍데기와 간단한 연출 |
| IridiumSkyblock | 개인 영지 생성/보호/방문 껍데기 |
| LuckPerms / Vault | 권한·경제 연동 |
| WorldEdit / WorldGuard / Multiverse-Core | 월드·보호·운영 보조 |

제거 확정: BetonQuest, Citizens. NPC 기반 진행 없음.

## 부트스트랩 순서

1. `CommonFoundationBootstrap`
2. `MasterRegistryBootstrap`
3. `CombatEngineBootstrap`
4. `BossEngineBootstrap`
5. `GrowthEngineBootstrap`
6. `LifeEngineBootstrap`
7. `QuestAchievementBootstrap`
8. `OperationsQueryBootstrap`

## 구현 상세 진입점

| 영역 | 현재 기준 문서 |
|---|---|
| 관리자 명령어 | `admin_command_spec.md` |
| DB/API/경제/강화/큐브 | `../02_database_api_stats/CANON.md`, `../02_database_api_stats/economy_numbers_v2.md`, `../02_database_api_stats/equipment_growth_spec.md`, `../02_database_api_stats/potential_options_v1.md` |
| 전투/무기/스킬 | `../04_combat_weapon_skills/CANON.md`, `../04_combat_weapon_skills/weapon_skills_v1.md`, `../04_combat_weapon_skills/combat_balance_v2.md` |
| 영지/공방 | `../05_island_farm_system/CANON.md`, `../05_island_farm_system/island_system_design.md`, `../05_island_farm_system/workshop_crafting_spec.md` |
| 필드/보스/드랍 | `../06_fields_bosses/CANON.md`, `../06_fields_bosses/drop_tables_v1.md`, `../07_boss_pattern_modules/index.md` |
| GUI/리소스팩 | `../08_resourcepack_pipeline/index.md`, `../08_resourcepack_pipeline/gui_hub_structure.md`, `../08_resourcepack_pipeline/gui_equipment_panel.md` |
| 운영 로드맵 | `../10_development_roadmap/index.md` |

## 현재 확정 구현 기준

- 장비는 T1 단일 체계다. 무기 / 투구 / 상의 / 하의 / 신발 5슬롯만 사용한다.
- 무기 6종은 검 / 도끼 / 창 / 석궁 / 낫 / 스태프다. 내부 식별자도 `axe`를 사용하며 `hammer` 표기는 구버전이다.
- 공용각인, 악세서리, 세트 장비, 도감/컬렉션은 1차 시즌에서 제외한다.
- 강화석 파편은 폐지됐다. 강화석은 실물 아이템이 아니라 DB 가상 재화로 처치 시 직접 적립/소모한다.
- 방어구 강화석 소모는 `ceil(무기 강화석 ÷ 1.5)`다.
- 큐브 사용 비용은 1회 500G다.
- 기본 전승은 0G, 등급전승권과 세부스탯전승권은 각 100,000G다.
- 개인 영지는 약초 재배지 / 광물 채굴기 / 공방 가공기 3종 시설 기준이다.
- 마력 시스템, 발전기, 호퍼/케이블 물류, 자동채굴기 기반 흐름은 폐지됐다.
- 필드보스는 5종, 30분 정시 스폰, 기여도 3% 이상 보상 기준이다.
- 시즌보스는 6종이며 주간 입장 제한은 없다. 시즌 최종보스는 균열왕·타락한 이중체·진혼의 주시자 3종 별도 (DL-042, DL-043).
- 1차 보스 외형은 바닐라 강화형이다. ModelEngine/BetterModel/FMM은 2차 확장 후보로만 둔다.

## 금지된 구버전 구현 기준

아래 내용은 archive 문서에 남아 있더라도 현재 구현 기준으로 사용하지 않는다.

- 큐브 5,000G
- 전승권 5,000G 또는 50,000G
- 강화석 파편, 강화석 파편 상점, 강화석 파편 경매 지표
- 방어구 강화석 50% 비율
- 마력 잔량, 발전기, 마력 과부하, 마력 소비 기계
- `hammer` 내부 식별자 및 망치 사용자 노출명
- 도감/컬렉션 1차 시즌 포함
- 시즌보스 주간 입장 제한
- 고퀄 외부 보스 모델 1차 적용

## 구현 메모 관리 규칙

- 새 구현 상세를 추가할 때는 이 문서를 다시 장문화하지 않는다.
- 도메인별 상세는 해당 도메인 폴더의 활성 REFERENCE 문서에 둔다.
- 구버전 상세를 참고해야 하면 `docs/_archive/implementation_reference_legacy.md`를 읽되, 현재 기준과 비교한 뒤 필요한 내용만 별도 문서로 승격한다.
- 현재 CANON과 충돌하는 상세는 수정하거나 archive로 이동한다.
