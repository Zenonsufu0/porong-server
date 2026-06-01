# 01. 플러그인 아키텍처 — CANON

> **[STATUS: CANON]** — 플러그인 경계, PoroRPG 책임 범위, 부트스트랩 순서, 운영 명령어의 공식 기준.

## 공식 기준

| 항목 | 기준 |
|---|---|
| 코어 플러그인 | PoroRPG |
| PoroRPG 책임 | 전투, 장비, 영지, 보스, 보상, DB, API, Discord/web 연동 데이터 |
| 외부 플러그인 역할 | MythicMobs는 바닐라 기반 몹/보스 껍데기, IridiumSkyblock은 개인 영지 껍데기만 담당 |
| 제거 확정 | BetonQuest, Citizens |
| 2차 확장 후보 | ModelEngine, BetterModel, FMM, ItemsAdder/Oraxen |
| 상호작용 원칙 | NPC 없음. 모든 주요 상호작용은 GUI 기반 |

## 참조 우선순위

| 문서 | 역할 |
|---|---|
| `../final_master_plan.md` §4, §11, §15 | 전체 플러그인 방향, API, `/poro` 관리자 커맨드 요약 |
| `implementation_reference.md` | DB 스키마, 이벤트 훅, 클래스 배정 등 구현 상세 |
| `admin_command_spec.md` | `/poro` 서브커맨드 상세 |
| `index.md` | 플러그인 운용 구조 개요 |
| `poro_rpg_design_intent.md` | PoroRPG 설계 의도 |
| `poro_rpg_module_design.md` | 모듈 단위 설계 참조 |

## 부트스트랩 순서

1. CommonFoundationBootstrap
2. MasterRegistryBootstrap
3. CombatEngineBootstrap
4. BossEngineBootstrap
5. GrowthEngineBootstrap
6. LifeEngineBootstrap
7. QuestAchievementBootstrap
8. OperationsQueryBootstrap

## 충돌 처리

이 폴더의 다른 문서가 Citizens 사용, NPC 기반 진행, 또는 PoroRPG 외부 플러그인이 핵심 데이터를 소유하는 구조를 전제하면 이 문서와 `../final_master_plan.md`를 우선한다.
