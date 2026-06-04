# poro-rpg — RPG 서버 규칙

> 모노레포 전역 규칙은 루트 [`../CLAUDE.md`](../CLAUDE.md). 이 파일은 poro-rpg 프로젝트 안에서만 적용되는 RPG 전용 규칙이다.

- 프로젝트 전역 SoT는 `docs/final_master_plan.md`(= `poro-rpg/docs/`). RPG 도메인 세부는 `poro-rpg/docs/`의 각 `CANON.md`. 충돌 시 `final_master_plan.md`가 우선.

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
| Discord/onboarding | `../poro-discord/docs/index.md` (poro-discord) |

> 위 경로는 `poro-rpg/docs/` 기준. 디스코드 봇 docs만 `poro-discord/docs/`에 있다.

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

## 코드/서버 작업 범위

- 플러그인 구현은 `poro-rpg/custom-plugins/poro-rpg` (기존 PoroRPG 경로).
- 런타임 서버 파일은 명시적 요청 시에만 `poro-rpg/server/plugins` 또는 `poro-rpg/server-config` 편집.
- `poro-rpg/server/`, `poro-rpg/custom-plugins/`는 명시적 요청 없이 수정하지 않는다.

## 에셋

- 에셋 작업: `poro-rpg/assets/source`. export 산출물: `poro-rpg/assets/export/resourcepack`.
- Blockbench 작업은 `poro-rpg/assets/source/CLAUDE.md`도 함께 따른다.
