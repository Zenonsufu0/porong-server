# 포로 서버 작업 현황

> 마지막 갱신: 2026-05-27 (§6-6 필드 드랍 완성 — 전장의 파편/고대흔적/큐브 자동전환 추가, 시즌보스 큐브 자동전환 수정)

---

## 현재 브랜치 상태

- 브랜치: `master` = `codex-review` (동기화 완료)
- 최근 커밋: `4dbc8c3 §6-6 fix — 시즌보스 grantRewards() 큐브 자동전환 추가, task.md 메타데이터 갱신`
- 빌드: `./gradlew compileJava → BUILD SUCCESSFUL`
- §6-6 필드 드랍 완성 **완료 · 리뷰 통과 (시즌보스 큐브 자동전환 fix 포함)**

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

## §6-5 보스룸 풀 시스템 — 완료 (2026-05-27)

30개 물리 방(50×50×50 석재벽돌, 6×5 격자, `GAP=60`) 사전 생성 + 파티 단위 슬롯 배정.

| 항목 | 커밋 | 상태 |
|---|---|---|
| `BossRoomSlot` (synchronized tryOccupy/release) | `7be9897` | ✅ |
| `BossRoomManager` — `assignRoom`, `registerRun`, `releaseByRunId`, `exitRoom` 자동해제 | `9a899a4` | ✅ |
| `BossRoomGenerationService` + `/empire-genrooms` 커맨드 | `7be9897` | ✅ |
| `config.yml` boss-room-slots 30슬롯 + fields 5종 좌표 | `7be9897` | ✅ |
| `BossRoomListener` — 표지판 입장 / `startRun()` / 슬롯 실패 cleanup | `342954f` | ✅ |
| MythicMobs reflection 격리 (`build.gradle.kts` compileOnly 제거) | `eee0548` | ✅ |
| 스폰 실패 → `endRun(false, "spawn_failed")` → `releaseByRunId` 체인 | `eee0548` | ✅ |
| MM 비활성화 → `assignRoom`/`startRun` 전 조기 차단 | `874f89c` | ✅ |
| `BossRewardService.onRunEnded` — `releaseByRunId` 를 clearSuccess 분기 앞으로 (항상 실행) | `342954f` | ✅ |
| `PlayerJoinListener.onQuit` — `bossRoomManager.exitRoom(uuid)` 연결 | `9a899a4` | ✅ |

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

## §6-6 필드 드랍 완성 — 완료 (2026-05-27)

| 항목 | 상태 |
|---|---|
| `FieldDropListener` — 일반몹/정예몹 전체 드랍 (골드·전장의 파편·강화석·큐브조각·장비의 흔적) | ✅ 기존 완료 |
| `BossRewardService.grantFieldBossReward()` — 전장의 파편 추가 (F1:3~5 ~ F5:5~9) | ✅ |
| `BossRewardService.grantFieldBossReward()` — 큐브 조각 → 큐브 자동 전환 (10조각=1큐브) | ✅ |
| equip_trace 등급 분포 CANON §2 기준으로 수정 (F1~3: 커먼60/레어35/에픽5, F4/F5 상위 등급 상향) | ✅ |
| 고대흔적(ancient_trace_*) 독립 드랍 — CANON §7.2 필드보스별 각 등급 독립 확률 | ✅ |
| `BUILD SUCCESSFUL` | ✅ |

## 다음 작업 후보

| 우선도 | 항목 | 비고 |
|---|---|---|
| 높음 | 서버 통합 테스트 — `/보스` 선택 → `[보스]` 표지판 → MM 스폰 런타임 확인 | MythicMobs mobId 매칭 검증 |
| 중간 | 디스코드 인증봇 (Phase 2) | `docs/03_discord_onboarding_bot/index.md` |
| 중간 | 강화 시스템 GUI (GrowthGuiListener 플레이스홀더 → 실구현) | `docs/04_combat_weapon_skills/CANON.md` |
| 낮음 | 리소스팩 파이프라인 (Phase 8) | `docs/08_resourcepack_pipeline/index.md` |

## Phase 5 잔여 기술 부채

| 항목 | 내용 |
|---|---|
| IridiumSkyblock XZ 확장 | `TerritoryStatusGuiListener` 작위 승급 시 섬 크기 확장 — API JAR 미포함, §7+ 연동 예정 (TODO 스텁 유지) |
| TerritoryHubGui 미구현 패널 | 이동·시설·상점·경매·설정 5패널 — 1차 시즌 범위 외, `§8준비 중` 표시 유지 |

---

## 알려진 기술 부채

| 항목 | 내용 |
|---|---|
| compileTestJava | GrowthEngineSampleTest legacy `EquipmentSlot` 참조 (§6 범위 외) |
| MythicMobs | §6-5에서 `compileOnly` JAR 선언 완전 제거. 스폰 호출은 `Class.forName` reflection으로 격리 — 컴파일/리뷰 환경에 JAR 불필요. 런타임 mobId 매칭은 서버 통합 테스트 필요 |
| IridiumSkyblock | `../../server/plugins/` 로컬 JAR 경로 — review worktree 미지원 (pre-existing). Java 코드에서 `io.lumine.*` 임포트 없으므로 현재 `compileJava` 정상 |
| BossSessionRepository 쓰기 경로 | ✅ 연결 완료 — DbBossRunRecordHook + CompositeBossRunRecordHook. 참여자 실수치(damage_share, il 등)는 §7+ 예정 (placeholder 0.0 유지) |
| 시즌보스 damage_share | `BossResultSummaryBuilder` placeholder 0.0 유지 — §7+ 구현 (DL-064) |
| AllowAllUnlockQuestChecker | 보스6 클리어 조건 stub — 퀘스트 시스템 구현 후 연결 |
| boss_pattern_seed.csv | 7개 보스 placeholder 패턴만 있음 — 실제 패턴 설계 필요 |
| 스킬 자원 스택 최대값 | 각 스킬 파일에 max=3 또는 5 하드코딩 — CANON "유지형 자원 최대 6스택"은 각인(유지형/소모형 분기) 기반이며 1차 시즌 각인 제외로 해당 없음. 현재 값은 스킬 스펙(weapon_skills_v1.md) 기본값 |
