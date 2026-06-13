# zenon-rpg — RPG 서버 규칙

> 모노레포 전역 규칙은 루트 [`../CLAUDE.md`](../CLAUDE.md). 이 파일은 zenon-rpg 프로젝트 안에서만 적용되는 RPG 전용 규칙이다.

- 프로젝트 전역 SoT는 `docs/final_master_plan.md`(= `zenon-rpg/docs/`). RPG 도메인 세부는 `zenon-rpg/docs/`의 각 `CANON.md`. 충돌 시 `final_master_plan.md`가 우선.

## 도메인별 참조

| 영역 | 문서 |
|---|---|
| Plugin/system | `docs/01_plugin_architecture/CANON.md` |
| DB/API/statistics | `docs/02_database_api_stats/CANON.md` |
| Combat/skills | `docs/04_combat_weapon_skills/CANON.md` |
| Island/farm | `docs/05_island_farm_system/CANON.md` |
| Fields/bosses/drops | `docs/06_fields_bosses/CANON.md` |
| Boss patterns | `docs/07_boss_pattern_modules/index.md` |
| Resource pack/assets | `docs/08_resourcepack_pipeline/index.md` |
| Terms/policy | `docs/09_terms_and_policy/index.md` |
| Roadmap/issues | `docs/10_development_roadmap/index.md` |
| Map design | `docs/12_map_design/` |
| PvP | `docs/13_pvp_system/CANON.md` |
| Discord/onboarding | `../zenon-discord/docs/index.md` (zenon-discord) |

> 위 경로는 `zenon-rpg/docs/` 기준. 디스코드 봇 docs만 `zenon-discord/docs/`에 있다.

## 설계 단일 진실 (1차 시즌 확정)

- 1차 시즌은 45일 시즌제 프로젝트 서버이며 영구 서버가 아니다.
- 오픈 모델: 디스코드 인증 게이트 공식 오픈, 공개 테스트 없음.
- 핵심 플러그인 PoroRPG가 전투·장비·영지/농장·보스 보상·DB·API·디스코드/웹 연동 데이터를 소유한다. (도감/컬렉션은 1차 시즌 제외)
- MythicMobs는 바닐라 기반 몹/보스 셸과 단순 시각 스킬 담당.
- IridiumSkyblock은 개인 섬 셸만 담당.
- 1차 시즌 보스는 바닐라 강화형. ModelEngine/BetterModel/FMM은 후속 확장으로 연기.
- 공용 각인은 1차 시즌에서 제거.
- 플레이어 스킬 설계 제외: 지속 장판, 적 표식, 방어 감소, 받는 피해 증가 디버프.
- 영지/농장은 영지 저장고만 사용. 마력 시스템 제거. 시설: 약초 재배지 + 광물 채굴기 + 공방 가공기 (2026-05-19 확정). 호퍼/케이블 물류 없음.

## 작업 영역 / 가드레일

- 이 작업 영역은 **RPG Paper 플러그인 서버** 담당이다.
- **포로몬(`../zenon-mon/`)·디스코드봇(`../zenon-discord/`)은 수정 금지.** 읽기만 한다.
- 루트 공통 파일(`../CLAUDE.md`, `../README.md`, `../.gitignore`, 루트 docs 구조)은 수정하지 않는다.
- 게임 로직(전투·스킬 수치·보스·퀘스트·밸런스)을 바꾸기 전, **구조·빌드 영향을 먼저 확인**한다.
- 커밋 전 반드시 `git status`로 변경 범위를 확인하고, 작고 리뷰 가능한 단위로 나눈다.
- 런타임 파일·빌드 산출물·로그·JAR는 커밋하지 않는다(`.local/server/`, `build/`, `*.jar`, 로그 등 — 단 `gradle/wrapper/gradle-wrapper.jar`는 예외 추적). 런타임 실행 폴더는 `poro-rpg/.local/server`(DL-130).

## 빌드

- 빌드 루트: `zenon-rpg/custom-plugins/poro-rpg`. 빌드: `./gradlew build` (→ `BUILD SUCCESSFUL`).

## 코드/서버 작업 범위

- 플러그인 개발 소스는 `zenon-rpg/custom-plugins/poro-rpg` (기존 PoroRPG 경로).
- 런타임 서버 실행 폴더는 `zenon-rpg/.local/server`, 플러그인 배치 위치는 `zenon-rpg/.local/server/plugins` (DL-130). `paper.jar`·`world`·`logs`·`plugins/*.jar`·`cache`·`versions` 등은 Git 추적 금지(`.local/` 전체 gitignored).
- 런타임 서버 파일은 명시적 요청 시에만 `zenon-rpg/.local/server/plugins` 편집. MythicMobs YAML 소스(추적 대상)는 `zenon-rpg/server-config`에서 편집 후 런타임에 배포한다.
- `zenon-rpg/.local/server/`, `zenon-rpg/custom-plugins/`는 명시적 요청 없이 수정하지 않는다.

## 에셋

- 에셋 작업: `zenon-rpg/assets/source`. export 산출물: `zenon-rpg/assets/export/resourcepack`.
- Blockbench 작업은 `zenon-rpg/assets/source/CLAUDE.md`도 함께 따른다.
