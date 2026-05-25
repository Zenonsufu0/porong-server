# 포로 서버 작업 현황

> 마지막 갱신: 2026-05-25 (Phase 5 영지/농장 핵심 리스너 구현 완료)

---

## 현재 브랜치 상태

- 브랜치: `master` = `codex-review` (동기화 완료)
- 최근 작업: §6 플러그인 코어 구현 목록 1~11 전체 완료

---

## §6 EmpireRPG 플러그인 코어 구현 — 완료

기준 문서: `docs/10_development_roadmap/implementation_design_plan.md`

| 순서 | 작업 | 상태 |
|---|---|---|
| 1 | PotentialService `useCube()` 단일 통합 | ✅ |
| 2 | SkillInputListener 4종 매핑 (LMB/RMB/Shift+RMB/F) + SafeZone 연결 | ✅ |
| 3 | MobTagHelper 신규 + FieldDropListener scoreboard tag 교체 | ✅ |
| 4 | GuiTitles 상수 클래스 | ✅ |
| 5 | PlayerPersistenceService migration v1→v2→v3 | ✅ |
| 6 | WeaponSelectionGuiListener GuiTitles 교체 | ✅ |
| 7 | PlayerQuitEvent save | ✅ |
| 8 | ClassInitService 신규 (장비 지급 + 영지 도구) | ✅ |
| 8b | poro_island.schem + schematics.yml (server/plugins/IridiumSkyblock/ — review worktree 미포함 경로, main에 존재) | ✅ |
| 9 | 메인 허브 / 장비 허브 최소 구현 | ✅ |
| 10 | BossRewardService + BossEngineRuntime 연동, ContributionTracker | ✅ |
| 11 | SafeZoneService (WorldGuardSafeZoneService / Noop) | ✅ |

---

## 빌드 상태

```
./gradlew compileJava → BUILD SUCCESSFUL
```

---

## Phase 6 잔여 — 완료

| 항목 | 상태 |
|---|---|
| scoreboard tag 전략: FieldMobTagListener 삭제 → YAML `addScoreboardTag ~onSpawn` 방식으로 교체, compileJava 정상 | ✅ |
| `boss_patterns.yml` (P-00~P-13) 신규 생성 | ✅ |
| 5개 필드보스 YAML Phase 구조 + BossBar 업데이트 | ✅ |
| `season_bosses.yml` 9보스 셸 신규 생성 | ✅ |

---

## Phase 7 선행 — 완료

| 항목 | 상태 |
|---|---|
| `boss_session_log` + `boss_session_player` DDL + migration | ✅ |
| `boss_stats_summary` VIEW | ✅ |
| `CommonFoundationBootstrap` migration chain 활성화 (Noop 교체 + `initialize()` 호출) | ✅ |
| `BossSessionRepository` (read/write) | ✅ |
| `EmpireHttpServer` (포트 8765, JDK 내장 HttpServer) | ✅ |
| `/api/v1/boss/stats` `/boss/{id}/stats` `/boss/{id}/weekly` `/boss/{id}/party-spec` | ✅ |
| `onDisable` HTTP 서버 stop 연결 | ✅ |
| `DbBossRunRecordHook` + `CompositeBossRunRecordHook` (세션 시작/종료 DB 기록) | ✅ |

---

## Phase 5 영지/농장 시스템 — 부분 완료

| 항목 | 상태 |
|---|---|
| `TerritoryStatusGuiListener` 전체 구현 (작위 승급, 편의 토글, 네비게이션) | ✅ |
| 작위 승급 재료 검증·차감 (customItems 기준) + IridiumSkyblock stub + 시설 자동레벨업 알림 | ✅ |
| `IslandRank` 골드 비용 스펙 정정 (20k~150k) + 업그레이드 재료 정의 | ✅ |
| `MachineProductionScheduler` 랜덤 산출량 스펙 반영 (Lv1=2~3, Lv2=3~4+10%, Lv3=4~6+30%) | ✅ |
| `item_master.csv` 재료 4종 추가/정정 (mat_battle_shard, mat_trace_star/moon/sun) + DL-066 전장의파편+골드 확정 | ✅ |
| `WorkshopGuiListener` 뒤로 버튼 → TerritoryHubGui.open 연결 | ✅ |
| `MachineProductionScheduler` 레벨별 증산 (Lv2=BARON+, Lv3=COUNT+) | ✅ |
| `WorkshopGui` 8탭 34레시피 + 대기열 enqueue + 완료 정산 (스키마 v4 영속화) | ✅ |
| `TerritoryHubGui` 54슬롯 8패널 (gui_hub_structure.md §4) — 상태·창고·공방 활성, 5개 패널 준비 중 | ✅ |

---

## 다음 작업 후보 (병렬 Phase)

| 우선도 | 항목 | 관련 문서 |
|---|---|---|
| ~~높음~~ | ~~보스 세션 DB 쓰기 연결~~ | ✅ 완료 |
| 중간 | 디스코드 인증봇 (Phase 2) | `docs/03_discord_onboarding_bot/index.md` |
| ~~중간~~ | ~~스킬 전체 구현 (Phase 3)~~ | ✅ 완료 — 24종 히트박스/대미지 실구현 |
| 중간 | 영지/농장 시스템 (Phase 5) | `docs/05_island_farm_system/CANON.md` |
| 낮음 | 리소스팩 파이프라인 (Phase 8) | `docs/08_resourcepack_pipeline/index.md` |

---

## 알려진 기술 부채

| 항목 | 내용 |
|---|---|
| compileTestJava | GrowthEngineSampleTest legacy `EquipmentSlot` 참조 (§6 범위 외) |
| MythicMobs/IridiumSkyblock | `../../server/plugins/` 로컬 JAR 경로 — review worktree 미지원 (pre-existing). Java 코드에서 `io.lumine.*` 임포트 없으므로 `compileOnly` JAR 불필요 → review worktree `compileJava` 정상 |
| BossSessionRepository 쓰기 경로 | ✅ 연결 완료 — DbBossRunRecordHook + CompositeBossRunRecordHook. 참여자 실수치(damage_share, il 등)는 §7+ 예정 (placeholder 0.0 유지) |
| 시즌보스 damage_share | `BossResultSummaryBuilder` placeholder 0.0 유지 — §7+ 구현 (DL-064) |
| AllowAllUnlockQuestChecker | 보스6 클리어 조건 stub — 퀘스트 시스템 구현 후 연결 |
| boss_pattern_seed.csv | 7개 보스 placeholder 패턴만 있음 — 실제 패턴 설계 필요 |
| 스킬 자원 스택 최대값 | 각 스킬 파일에 max=3 또는 5 하드코딩 — CANON "유지형 자원 최대 6스택"은 각인(유지형/소모형 분기) 기반이며 1차 시즌 각인 제외로 해당 없음. 현재 값은 스킬 스펙(weapon_skills_v1.md) 기본값 |
