# 포로 서버 1차 시즌 — Final Master Plan

> **[STATUS: CANON]** — 프로젝트 전체 방향성과 도메인 간 우선순위의 최상위 기준. 세부 구현·수치·표는 각 `CANON.md`와 하위 참조 문서가 담당하며, 충돌 시 이 문서가 우선한다.

> 기준일: 2026-05-20 → 재정리: 2026-05-22 | 서버: Paper 1.21.10 | 운영 기간: 약 45일

---

## 0. 한 줄 정의

포로 서버 1차는 **디스코드 유입 제한형, 45일 시즌제 RPG 프로젝트 서버**다.

목표는 6무기 전투, T1 장비 성장, 필드/보스, 개인 영지 생산이 하나의 짧은 시즌 루프로 안정적으로 돌아가는 것이다.

---

## 1. 운영 기본값

| 항목 | 기준 |
|---|---|
| 운영 기간 | 약 45일, 종료 예정일 2026-09-01 |
| 오픈 방식 | 디스코드 선입장 인증 후 정식 오픈. 공개 테스트 없음 |
| 서버 버전 | Paper 1.21.10 / Java 21 |
| 데이터 저장 | SQLite `empire.db` + 플레이어 JSON |
| 후원 | 성장형 유료 상품 없음. 보상 없는 자발 후원만 허용 |

버그, 비정상 루트 재화 유입, 특정 루트 과효율, 골드 총량 과잉 증가 시 운영 개입이 가능하다.

---

## 2. 공식 문서 구조

| 도메인 | 공식 기준 |
|---|---|
| 플러그인/구현 경계 | `docs/01_plugin_architecture/CANON.md` |
| DB/API/경제/통계 | `docs/02_database_api_stats/CANON.md` |
| 디스코드 온보딩 | `docs/03_discord_onboarding_bot/index.md` |
| 전투/무기/스킬 | `docs/04_combat_weapon_skills/CANON.md` |
| 영지/농사/공방 | `docs/05_island_farm_system/CANON.md` |
| 필드/보스/드랍 | `docs/06_fields_bosses/CANON.md` |
| 보스 패턴 | `docs/07_boss_pattern_modules/index.md` |
| GUI/리소스팩 | `docs/08_resourcepack_pipeline/index.md` |
| 약관/운영정책 | `docs/09_terms_and_policy/index.md` |
| 개발 로드맵 | `docs/10_development_roadmap/index.md` |
| 맵 디자인 | `docs/12_map_design/` |
| Archive | `docs/_archive/README.md` |

---

## 3. 디스코드 온보딩

디스코드 입장 → 약관 동의 → 마크 닉네임 입력 → 임시 화이트리스트 → 마크 접속 → `/연동` → 인증 유저 역할 부여.

접속정보 채널은 인증 유저에게만 열람 가능하다. 필드보스 알림은 단일 채널 `#필드보스-알림`에 통합한다.

상세: `docs/03_discord_onboarding_bot/index.md`

---

## 4. 플러그인 구조

EmpireRPG가 전투, 장비, 영지, 보스, 보상, DB, API, Discord/web 연동 데이터를 소유한다.

| 플러그인 | 역할 |
|---|---|
| EmpireRPG | 코어 로직과 데이터 소유 |
| MythicMobs | 바닐라 기반 몹/보스 껍데기와 간단한 연출 |
| IridiumSkyblock | 개인 영지 생성/보호/방문 껍데기 |
| LuckPerms / Vault | 권한/경제 연동 |
| EssentialsX, WorldEdit, WorldGuard, Multiverse-Core | 운영, 월드, 보호 |
| PlaceholderAPI, DecentHolograms, Chunky, spark | 표시, 프리젠, 성능 측정 |

제거 확정: BetonQuest, Citizens. NPC 기반 진행 없음. 주요 상호작용은 GUI 기반이다.

상세: `docs/01_plugin_architecture/CANON.md`, `docs/01_plugin_architecture/implementation_reference.md`

---

## 5. 월드 구조

| 월드 | 용도 |
|---|---|
| `world_main` | 제국 수도, 필드 5개, 훈련장 |
| `world_farm` | 개인 영지 |
| `world_boss` | 시즌보스/최종보스 인스턴스 |
| `world_test` | 개발/리소스/보스 테스트 |

---

## 6. 전투와 장비 성장

1차 시즌은 T1 단일 체계다. T1.5, T2, 세트 장비, 세트 효과, 공용각인, 악세서리는 제외한다.

무기 6종은 검 / 도끼 / 창 / 석궁 / 낫 / 스태프다. 각 무기는 LC, RC, Shift+RC, F 입력 기반의 스킬 4개와 A/B 직업각인 2종을 가진다.

설계 금지 항목: 플레이어 지속 장판, 적 표식, 받는 피해 증가, 방어 감소, 파티 전체 피해 증가, 복잡한 스택 폭발.

상세:

- 스킬과 전투 기준: `docs/04_combat_weapon_skills/CANON.md`
- 스킬 계수/쿨타임: `docs/04_combat_weapon_skills/weapon_skills_v1.md`
- 강화/잠재/경제: `docs/02_database_api_stats/CANON.md`

---

## 7. 튜토리얼, 핫바, 상점

최초 접속자는 튜토리얼, 무기 선택, 기본 장비 지급, 영지 생성 순서로 시즌 루프에 진입한다.

핫바는 장착 무기, 자유 슬롯, 탐험 나침반, 메뉴 아이콘 중심으로 구성한다. 슬롯 7/8의 기능 아이템은 버릴 수 없고 로그인 시 재지급 체크한다.

상점 기준: 장비 이름 변경권은 10,000G. 농작물·광물 판매 상점과 블럭 상점 상세는 경제 문서에서 관리한다.

상세:

- 구현 레퍼런스: `docs/01_plugin_architecture/implementation_reference.md`
- 경제 기준: `docs/02_database_api_stats/CANON.md`
- GUI 기준: `docs/08_resourcepack_pipeline/index.md`

---

## 8. 개인 영지

사용자에게는 “스카이블럭”이 아니라 “개인 영지”로 노출한다.

IridiumSkyblock은 생성/보호/방문 껍데기만 담당하고, EmpireRPG가 저장고, 시설, 가공, 작위, 생산 로직을 소유한다.

시설은 약초 재배지, 광물 채굴기, 공방 가공기 3종이다. 마력 시스템, 발전기, 호퍼/케이블 물류는 1차 시즌에서 제외한다.

상세: `docs/05_island_farm_system/CANON.md`

---

## 9. 필드, 보스, 드랍

필드는 5개이며 각 필드는 일반몹 2종, 정예몹 1종, 필드보스 1종으로 구성한다.

필드 재료는 `전장의 파편` 1종으로 통합한다. 강화석 파편 시스템은 폐지하고 강화석을 직접 드랍/소모한다.

필드보스는 5종, 30분 정시 스폰, 개인 독립 확률 보상 기준이다. 시즌보스는 3종 + 균열왕이며, 주간 입장 제한은 없다.

보상 기여도 기준은 총 피해량 3% 이상이다. 원샷 방지는 단일 타격 85% 클램프와 후속 공격 0 피해 규칙을 사용한다.

상세:

- 필드/보스/드랍 기준: `docs/06_fields_bosses/CANON.md`
- 드랍 상세: `docs/06_fields_bosses/drop_tables_v1.md`
- 보스 패턴: `docs/07_boss_pattern_modules/index.md`

---

## 10. GUI와 리소스팩

메인 진입은 나침반 우클릭 또는 `/메뉴` 커맨드다. 메인 GUI는 장비, 영지, 보스, 필드 흐름으로 분기한다.

모든 시스템에서 재료 소모는 영지 창고를 먼저 차감하고, 부족분을 플레이어 인벤토리에서 차감한다. 골드는 플레이어 골드 잔액에서만 차감한다.

리소스팩 1차 우선순위는 재료 아이콘, GUI 아이콘, 영지/가공기 GUI, 영지 시설 외형, 무기 6종 치장이다. 날개 치장, 고퀄 보스 모델, 도감 GUI, 외부 몬스터 모델은 제외한다.

상세: `docs/08_resourcepack_pipeline/index.md`

---

## 11. 데이터와 API

EmpireRPG가 데이터 원본을 소유한다. Discord 봇과 웹은 HTTP API를 통해 조회한다.

API 범위는 인증 연동, 역할 큐, 필드보스 현황, 운영 대시보드, 플레이어 조회, Discord 카드용 스냅샷이다.

상세: `docs/02_database_api_stats/CANON.md`, `docs/01_plugin_architecture/implementation_reference.md`

---

## 12. 개발 로드맵

Phase는 기반, 디스코드 인증, 전투 코어, 장비 성장, 영지, 필드/보스, 웹/디코봇/통계, 치장/리소스팩 순서로 진행한다.

도감(컬렉션)은 1차 시즌에서 제외한다.

상세: `docs/10_development_roadmap/index.md`

---

## 13. 미확정 항목

| 항목 | 상태 | 담당 문서 |
|---|---|---|
| 강화석 드랍률 최종 수치 | 검토 중 | `docs/02_database_api_stats/enhancement_droprate_v1.md` |
| 21~25강 강화 성공률 재조정 | 검토 중 | `docs/02_database_api_stats/economy_numbers_v2.md` |
| 전승권 비용 | 오픈 후 7~14일차 흔적 시세 확인 후 결정 | `docs/08_resourcepack_pipeline/gui_functional_specs.md` |
| 태양의 흔적 아이템 정의 및 수급 경로 | 미확정 | `docs/02_database_api_stats/economy_numbers_v2.md` |

확정 완료된 M-tags는 각 도메인 문서와 `docs/decision_log.md`에 흡수한다.

---

## 14. 용어 확정

| 기능 | 표시 용어 |
|---|---|
| 서버명 | 포로 서버 |
| 수도 | 제국 수도 |
| 개인섬 | 개인 영지 |
| Island Storage | 영지 저장고 |
| 자동 재배기 | 약초 재배지 |
| 광물 채굴기 | 광물 채굴기 |
| 올인원 가공기 | 공방 가공기 |
| 시즌 최종보스 | 균열왕 |
| 무기각인 | 직업각인 |
| 잠재 | 잠재능력 |
| 전투 파편 | 전장의 파편 |

---

## 15. 운영 명령어

관리자 명령어는 `/empire <category> <subcommand> [args]` 구조를 사용한다. 권한 노드는 `empire.admin`이다.

상세: `docs/01_plugin_architecture/admin_command_spec.md`
