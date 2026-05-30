# 포로 서버 작업 현황

> 마지막 갱신: 2026-05-30 (§6-15 Phase 2 Step 2 — 운영 토글 GUI + /empire-toggle)

---

## 현재 브랜치 상태

- 브랜치: `master`
- **관리자 GUI Phase 2 Step 1~5 모두 구현 완료** — Phase 2 마감. 상세는 각 §6-NN 섹션
- 빌드: `./gradlew compileJava → BUILD SUCCESSFUL` (기존 deprecated AnvilInventory warning만 잔존)
- 동기화: 매 handoff 시 master == codex-review 유지
- 다음: Phase 2 인게임 통합 테스트 / 또는 별도 지시
- ※ **상태 드리프트 방지 정책**: task.md에 "최신 커밋 해시"·"미커밋/커밋" 상태를 박지 않는다. 최신 커밋 기준은 항상 `git log`. per-step 섹션은 완료 여부만 표기한다. (자기 자신을 포함한 커밋의 해시는 미리 알 수 없어 반드시 한 발 늦기 때문)

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

## §6-7 강화 시스템 GUI 완성 — 완료 (2026-05-27)

| 항목 | 상태 |
|---|---|
| `GrowthGuiListener.openEquipmentHub()` — 장비 슬롯 아이콘(5종) 추가, MainHubListener 버전과 레이아웃 통일 | ✅ |
| 전승 버튼 "준비 중" → "§f전승 / 클릭하여 열기" (GrowthGuiListener + MainHubListener 양쪽) | ✅ |
| `equipHubSlotIcon()` / `weaponMaterial()` 헬퍼 추가 | ✅ |
| `BUILD SUCCESSFUL` | ✅ |

## Phase 2 디스코드 봇 핵심 기능 — 완료 (2026-05-27)

| 항목 | 파일 | 상태 |
|---|---|---|
| `ROLE_미인증_ID` / `CLASS_ROLE_IDS` / `NOTIFY_ROLE_IDS` env 추가 | `bot/config.py` | ✅ |
| `on_member_join` → 미인증 역할 자동 부여 | `bot/cogs/auth.py` | ✅ |
| IMMINENT 감지 (RESPAWNING + ≤5분) → 5분 전 예고 embed | `bot/cogs/field_boss.py` | ✅ |
| `/프로필`·`/영지`·`/보스` API 분리 (3개 별도 엔드포인트) | `bot/cogs/player_commands.py` | ✅ |
| `get_island_by_nick` / `get_boss_by_nick` 추가 | `bot/api_client.py` | ✅ |
| `/클래스선택` (6버튼, 1인1클래스) + `/알림설정` (3토글) 신규 Cog | `bot/cogs/role_commands.py` | ✅ |
| `cogs.role_commands` 로드 | `bot/main.py` | ✅ |

## §6-8 경매장 직접 등록 커맨드 — 완료 (2026-05-27)

| 항목 | 상태 |
|---|---|
| `AuctionGuiListener.handleDirectRegister(player, price, qty)` — 손에 든 아이템 PDC/displayName 매칭 → territory 수량 검증 → confirmRegister 위임 | ✅ |
| `confirmRegister` qty 파라미터 추가 (GUI 경로는 qty=1 고정, 커맨드 경로는 사용자 지정) | ✅ |
| `NamespacedKey "empire_item_id"` 추가 — PDC 태그 우선 조회, 없으면 displayName→ItemMaster.itemName() fallback | ✅ |
| `PlayerCommandRouter` — `/경매장 등록 <가격> [수량]` args 파싱 추가 | ✅ |
| `BUILD SUCCESSFUL` | ✅ |

## §6-9 MM 보스룸 보스 MobType ID 교체 — 완료 (2026-05-27)

| 항목 | 상태 |
|---|---|
| `server-config/mythicmobs/mobs/season_bosses.yml` — MobType ID 9종 교체 (`SBoss1_FallenKnight` → `fallen_knight` 등) | ✅ |
| `corrupted_dyad`(Z) `~onSpawn` 소환 추가 → `corrupted_dyad_s`(S) 자동 동반 스폰 | ✅ |
| `SpiritWatcher_Mage` 소환체 ID는 유지 (보스 진입점 아님) | ✅ |
| `rift_king`도 `season_bosses.yml` 내에 존재 (`Final_RiftKing` → `rift_king`) | ✅ |

## §6-11 HUD 5레이어·스코어보드·스킬·커맨드 구현 — 완료 (2026-05-28)

기준 문서: `docs/10_development_roadmap/implementation_plan_v2.md`

| 항목 | 파일 | 상태 |
|---|---|---|
| Phase A-1: HealthHudListener → 1틱 repeating task + override map | `HealthHudListener.java` | ✅ |
| Phase A-2: HealthHudFormatter → 5레이어 글리프 Component 빌더 (HP/XP/CD/Stack) | `HealthHudFormatter.java` | ✅ |
| Phase A-2: CooldownManager `long[]` 저장 (expireAt + totalMs) → CD 진행률 계산 가능 | `CooldownManager.java` | ✅ |
| Phase A-2: SkillService 쿨타임 action bar 텍스트 제거 (HUD로 통합) | `SkillService.java` | ✅ |
| Phase B-1: ScoreboardService.refresh() 완전 구현 (골드·강화석·큐브·레벨·IL·위치) | `ScoreboardService.java` | ✅ |
| Phase C-1: BaseWeaponSkill.dashInInputDirection() 추가 | `BaseWeaponSkill.java` | ✅ |
| Phase C-1: 낫 월영회전 dashSideways → dashInInputDirection (키 입력 방향) | `ScytheShadowSpinSkill.java` | ✅ |
| Phase D-1: /메뉴 → MainHubGui.open(player) 연결 | `PlayerCommandRouter.java` | ✅ |
| `BUILD SUCCESSFUL` + `server/plugins/EmpireRPG.jar` 재배포 | — | ✅ |

### 잔여 미구현 (구현 계획서 v2 기준)
| 항목 | 비고 |
|---|---|
| Phase A-3: 스택 최대값 각인 연동 | 현재 max=3 하드코딩 |
| Phase A-4: 강화 성공/실패 알림 HUD 오버라이드 연결 | `showAlert()` API 준비됨, GrowthGuiListener에서 호출 필요 |
| Phase B-2: 재화 변경 시점 refresh() 호출 연결 | join 외 이벤트 구독 필요 |
| Phase C-2: 낫 스킬 쿨타임 수치 정정 | weapon_skills_v1.md 대조 필요 |
| Phase D-2: /영지·/작물·/상점 stub → GUI 연결 | |
| Phase E-1: IridiumSkyblock 섬 크기 확장 | API JAR 미포함 |
| Phase F-1: 보스 인스턴스 파티 스코어보드 | |

---

## §6-10 인게임 버그 수정 + 경매장 마이그레이션 — 완료 (2026-05-28)

| 항목 | 커밋 | 상태 |
|---|---|---|
| `GrowthGuiListener` — WeaponType.NONE guard (`isNoneClass()` 헬퍼) 4개 공개 진입점 전체 적용 | `a897c5f`→fix | ✅ |
| `MainHubListener.openEquipmentHub()` — WeaponType.NONE guard 동일 적용 | `a897c5f` | ✅ |
| `GrowthGuiListener.openGrowthPotential()` — slot 53 닫기 버튼 추가 | `a897c5f` | ✅ |
| `GrowthGuiListener.openGrowthHeirloom()` — 45→54 슬롯 확장, HEIR_SLOT_BACK=45·HEIR_SLOT_CLOSE=53 추가 | `a897c5f` | ✅ |
| `AuctionMigration` — 구 스키마(`item_data` 컬럼) 감지 → 비어 있으면 DROP·재생성, 데이터 있으면 abort+안내 | `cea6aeb` | ✅ |
| JAR 재빌드 + `server/plugins/EmpireRPG.jar` 재배포 | — | ✅ |

### 재배포 후 재확인 필요 항목
| 증상 | 판단 |
|---|---|
| 성공률 10000 표기 | 코드상 `String.format("%.1f%%", rate)` 정상 — 신 JAR 로딩 후 재확인 |
| 각 UI 구버전처럼 보임 | 신 JAR 재배포 후 재확인 |

### 외부 플러그인 / 리소스팩 이슈 (코드 범위 외)
| 증상 | 원인 | 조치 |
|---|---|---|
| 메뉴 PNG 안뜸 | 리소스팩 미적용 | 리소스팩 배포 확인 |
| 물약 PNG 미적용 | 리소스팩 | 동일 |
| 영지 평화로움 안됨 | WorldGuard 구역 설정 | WorldGuard 구역에 PVP off 설정 필요 |
| 새 영지 생성 시 IridiumSkyblock GUI | IridiumSkyblock 기본 동작 | IS `configuration.yml` island-create GUI 비활성화 검토 |
| 작물관리에 마력 | IridiumSkyblock Crystals 표시 추정 | IS `bankitems.yml` crystals 항목 비활성화 검토 |
| 캐릭터 창 세부 UI | stub 미구현 | §7+ 예정 |
| 보스정보 상세 | stub 미구현 | §7+ 예정 |
| 영지설정 | stub 미구현 | §7+ 예정 |

## §6-13 PvP 시스템 (CANON §13) — 완료 (2026-05-29~30)

기준 문서: `docs/04_combat_weapon_skills/CANON.md §13`, DL-077

| 항목 | 파일 | 상태 |
|---|---|---|
| `PvpArenaSlot` / `PvpArenaManager` — 슬롯 풀 + `tryOccupy`/`releaseByMatchId`, `isInArena` | 신규 | ✅ |
| `PvpMatch` / `PvpMatchService` — enqueue, 매치 시작·종료·AUTO_RETURN_TICKS·강제종료 | 신규 | ✅ |
| `PvpRatingService` — Elo 유사 점수, repository 연결, `adminAdjustScore` | 신규 | ✅ |
| `PvpRatingRepository` (DB 저장) + 통합 마이그레이션 | 신규 | ✅ |
| RANKED IL 60 데미지 클램프 (scale 무조건 적용) | `PvpMatchService` | ✅ |
| `PvpTeleportListener` — 매치 중 텔레포트/명령 차단 (`internalTeleport` 마커 + IS `home` 처리 순서) | 신규 | ✅ |
| Codex 리뷰 P1/P2/P3 사이클 — `nextValidCandidate` 헬퍼, 슬롯 해제 타이밍, playerToMatch 정리 시점 | 다회 커밋 | ✅ |
| `BUILD SUCCESSFUL` | — | ✅ |

## §6-14 관리자 GUI Phase 1 + 운영자 명령어 (Step 1) — 완료 (2026-05-30)

기준 문서: `docs/10_development_roadmap/admin_gui_phase2.md`, 커밋 `627e163`

| 항목 | 파일 | 상태 |
|---|---|---|
| `/empire-admin` 허브 + Phase 1 GUI (인스펙트·진행매치·통계·슬롯해제) | `AdminHubGui`, `AdminInspectGui`, `AdminMatchesGui`, `AdminStatsGui`, `AdminGuiListener`, `AdminHubCommand` | ✅ |
| Anvil 닉네임 → `AdminInspectGui` 27슬롯 (직업·레벨·영지·PvP·재화·장비 5슬롯·평균 IL) | `AdminInspectGui` | ✅ |
| 운영자 단건 변경 8종 (`empire-give`, `empire-currency`, `empire-rank`, `empire-enhance`, `empire-level`, `empire-pvp-score`, `empire-cleanse`, `empire-island-reset`) | `AdminPlayerCommand` (신규) | ✅ |
| `PlayerEquipmentItem.setEnhanceLevel` 가시성 public 승격 (관리자 명령 접근용) | `PlayerEquipmentItem` | ✅ |
| `PvpMatchService.activeMatches()` / `adminForceEnd(matchId, reason)` 추가 | `PvpMatchService` | ✅ |
| `PvpRatingService.adminAdjustScore(uuid, name, delta)` 추가 | `PvpRatingService` | ✅ |
| 권한 분리 (sub-permissions) + 명령어 네이밍 일관화 | `plugin.yml` | ✅ |

## §6-15 관리자 Phase 2 Step 2 — 운영 토글 — 완료 (2026-05-30, 커밋 `ff46573`+`58a38e0`)

기준: 사용자 지시 "C 방식 — 모든 동작은 명령어로도 존재해야 하고 운영자용 편의성/기능성 명령어도 검토"

| 항목 | 파일 | 상태 |
|---|---|---|
| `AdminTogglesService` — 5개 플래그 (`BOSS_SPAWN_PAUSE`/`ENHANCE_BOOST`/`EXP_BOOST`/`DROP_BOOST`/`PVP_QUEUE_PAUSE`), in-memory `LinkedHashMap`, isOn/setOn/setOff/toggle/all | `admin/AdminTogglesService.java` (신규) | ✅ |
| `/empire-toggle <flag> [on|off]` / `/empire-toggle list` | `command/AdminTogglesCommand.java` (신규) | ✅ |
| `AdminTogglesGui` — 27슬롯, Toggle 당 LIME/GRAY 다이 아이콘, `open()` 반환 `Toggle[]`로 슬롯 매핑 | `gui/AdminTogglesGui.java` (신규) | ✅ |
| `GuiTitles.ADMIN_TOGGLES` 추가 | `gui/GuiTitles.java` | ✅ |
| `AdminHubGui` slot 31 stub → 활성 LEVER 아이콘 | `gui/AdminHubGui.java` | ✅ |
| `AdminGuiListener` — togglesService 주입, `ADMIN_TOGGLES` 클릭 핸들러, `toggleSlotMapping` | `listener/AdminGuiListener.java` | ✅ |
| `EmpireRPGPlugin` — `AdminTogglesService` 생성 + `AdminTogglesCommand` 등록 | `EmpireRPGPlugin.java` | ✅ |
| `plugin.yml` — `empire-toggle` 명령 등록 (`empire.admin`) | `plugin.yml` | ✅ |
| `docs/10_development_roadmap/admin_gui_phase2.md` — Step 2 완료/Step 2b hook 명시 | docs | ✅ |
| `BUILD SUCCESSFUL` | — | ✅ |

## §6-18 관리자 Phase 2 Step 5 — 영지 관리 GUI (slot 29) — 완료 (2026-05-30)

기준: `docs/10_development_roadmap/admin_gui_phase2.md` §영지 관리 (slot 29). **Phase 2 마지막 단계.**

| 항목 | 파일 | 상태 |
|---|---|---|
| `AdminTerritoryGui` — 전체 영지 목록(작위 내림차순 페이지네이션 45/page) + 클릭 액션 | `gui/AdminTerritoryGui.java` (신규) | ✅ |
| 작위 강제 변경: 좌클릭=▲ / 우클릭=▼ (ordinal ±1 clamp) | `listener/AdminGuiListener.java` | ✅ |
| 소셜 초기화: Shift+우클릭 → `resetSocialSettings` (멤버·권한·방문모드, 작위·시설 보존) | — | ✅ |
| `IslandTerritoryStateStore.resetSocialSettings(uuid)` 공통 추출 (명령/GUI DRY) | `growth/island/IslandTerritoryStateStore.java` | ✅ |
| `AdminPlayerCommand.handleIslandReset` → 공통 헬퍼 호출로 리팩터 (중복 제거) | `command/AdminPlayerCommand.java` | ✅ |
| `GuiTitles.ADMIN_TERRITORY` + `AdminHubGui` slot 29 stub→활성 | `gui/*` | ✅ |
| `AdminGuiListener` — `ADMIN_TERRITORY` 핸들러 + `territoryView` 상태 + `shiftRank` | `listener/AdminGuiListener.java` | ✅ |
| 신규 명령 없음 — 액션은 기존 `/empire-rank`·`/empire-island-reset`와 동등 (C 방식) | — | ✅ |
| `BUILD SUCCESSFUL` | — | ✅ |

### §6-18 잔여
- 멤버 개별 강제 제거 GUI — 현재는 전체 초기화만. 후속
- 영지 customItem(자원) 직접 지급/회수 GUI — `/empire-currency`·`/empire-give` 명령 존재, GUI는 후속
- 작위 변경 즉시 영속화 — `/empire-rank`와 동일하게 `setRank`만 (기존 저장 경로 위임). 별도 persist 미호출

---

## §6-17 관리자 Phase 2 Step 4 — 보스 디버그 GUI + /empire-boss-list·/empire-boss-end — 완료 (2026-05-30, 커밋 `7c02972`)

기준: `docs/10_development_roadmap/admin_gui_phase2.md` §보스 디버그 (slot 24)

| 항목 | 파일 | 상태 |
|---|---|---|
| `AdminBossGui` — 54슬롯 진행 중 보스 런 목록, 좌클릭=강제 종료 | `gui/AdminBossGui.java` (신규) | ✅ |
| `/empire-boss-list` + `/empire-boss-end <runId>` (접두어 매칭, 한 핸들러 라벨 분기) | `command/AdminBossCommand.java` (신규) | ✅ |
| 강제 종료 = `BossRunService.endRun(runId, false, "admin_force")` → onRunEnded → `releaseByRunId` (슬롯 해제 검증됨, BossRewardService:115) | — | ✅ |
| 조회 = `BossRunService.activeRuns()` (도메인 변경 없음, 기존 API 재사용) | — | ✅ |
| `GuiTitles.ADMIN_BOSS` + `AdminHubGui` slot 24 활성 | `gui/*` | ✅ |
| `AdminGuiListener` — `bossRunService` 주입 + `ADMIN_BOSS` 핸들러 + `bossRunMapping` | `listener/AdminGuiListener.java` | ✅ |
| `EmpireRPGPlugin` — `bossEngineRuntime.runService()` 주입 + 두 명령 등록 | `EmpireRPGPlugin.java` | ✅ |
| `plugin.yml` — `empire-boss-list`·`empire-boss-end` 등록 (`empire.admin`) | `plugin.yml` | ✅ |
| `BUILD SUCCESSFUL` | — | ✅ |

### §6-17 잔여
- 강제 페이즈 트리거 (`BossPatternScheduler.enqueueForced`) — Step 4+ 보류
- MM 보스 엔티티 잔존 — 강제 종료는 슬롯만 해제. 엔티티 despawn은 후속

---

## §6-16 관리자 Phase 2 Step 3 — 로그/감시 GUI + /empire-log — 완료 (2026-05-30, 커밋 `ef31e9e`)

기준: `docs/10_development_roadmap/admin_gui_phase2.md` §로그/감시 (slot 33)

| 항목 | 파일 | 상태 |
|---|---|---|
| `AdminLogGui` — 54슬롯 3탭(강화/거래/PvP) 페이지네이션 45/page, 읽기 전용 | `gui/AdminLogGui.java` (신규) | ✅ |
| `/empire-log [enhance\|trade\|pvp]` 텍스트 출력 (콘솔 가능, 탭별 최근 10건) | `command/AdminLogCommand.java` (신규) | ✅ |
| `AuctionStore.recentSold(limit)` — `status='sold' ORDER BY sold_at DESC` | `market/AuctionStore.java` | ✅ |
| `PvpMatchLogRepository.recentMatches(limit)` + `PvpMatchLogRow` record | `pvp/db/PvpMatchLogRepository.java` | ✅ |
| 강화 로그 소스 = `InMemoryEnhancementLogHook.logs()` (in-memory, 휘발성) | — | ✅ |
| `GuiTitles.ADMIN_LOGS` + `AdminHubGui` slot 33 stub→활성 | `gui/*` | ✅ |
| `AdminGuiListener` — 로그 소스 3종 주입 + `ADMIN_LOGS` 탭/페이지 핸들러 + `LogView` 상태 | `listener/AdminGuiListener.java` | ✅ |
| `EmpireRPGPlugin` — `pvpMatchLogRepo` 필드 승격 + 주입 + `/empire-log` 등록 | `EmpireRPGPlugin.java` | ✅ |
| `plugin.yml` — `empire-log` 등록 (`empire.admin`) | `plugin.yml` | ✅ |
| `BUILD SUCCESSFUL` | — | ✅ |

### §6-16 잔여
- 의심 활동 알림(짧은 시간 다량 거래·반복 패배 등) — 집계/임계 로직 별도 설계 필요, Step 3+ 보류
- 강화 로그 영속화 — 현재 in-memory. DB 테이블 필요 시 별도 작업

---

### §6-15 Step 2b — 토글 hook 실제 적용 — 완료 (2026-05-30)

| 플래그 | 적용 위치 | 상태 |
|---|---|---|
| `BOSS_SPAWN_PAUSE` | `BossRoomListener.onInteract` — MM 가드 직후, `assignRoom` 전 차단 | ✅ |
| `ENHANCE_BOOST` | `EnhancementService` 성공 임계값 ×2 (1.0 클램프). `BooleanSupplier` 주입으로 도메인↔운영 역결합 회피 | ✅ |
| `EXP_BOOST` | `FieldDropListener` 필드몹 처치 EXP ×2 (`expMultiplier()`) | ✅ |
| `DROP_BOOST` | `FieldDropListener.grantFieldDrops` 드랍 **수량** ×2 (`dropMultiplier()`, 확률 유지 → 기댓값 정확히 2배) | ✅ |
| `PVP_QUEUE_PAUSE` | `PvpMatchService.enqueue` 진입 즉시 reject 메시지 | ✅ |

- 와이어링: `EmpireRPGPlugin` — `adminTogglesService` 필드 승격, `pvpMatchService.attachToggles()` + `enhancementService().setEnhanceBoostSupplier()` + 두 리스너 생성자 주입
- `BUILD SUCCESSFUL` (기존 AnvilInventory deprecation 경고만 잔존)
- **범위 주의:** EXP/DROP 부스트는 `FieldDropListener`(필드몹) 한정. 보스 보상(`BossRewardService`) 경로는 미적용 — 필요 시 확장 검토

### Phase 2 — 전체 완료 (Step 1~5)
- Step 1~5 모두 구현 완료. 후속 확장(멤버 개별 제거·자원 GUI·강제 페이즈 트리거·의심활동 알림)은 각 §6-NN 잔여 항목 참조.

---

## §6-12 HUD·스코어보드·GUI·명령어 개선 — 완료 (2026-05-29)

| 항목 | 파일 | 상태 |
|---|---|---|
| `GuiTitles` — MAIN/EQUIPMENT/TERRITORY/BOSS_HUB 4종 `poro:gui` 글리프 제거, 순수 텍스트로 대체 | `GuiTitles.java` | ✅ |
| `HealthHudFormatter.build()` — 5레이어 HUD를 수평 나열 → `` (-176px) rewind overlay 기법으로 수직 적층 (좌우 퍼짐 수정) | `HealthHudFormatter.java` | ✅ |
| `ScoreboardService` — 골드/강화석/큐브 행에 `poro:hud` PNG 아이콘 (U+E034~E036) 추가 (`Team.prefix(Component)` 방식) | `ScoreboardService.java` | ✅ |
| `/직업 <플레이어> <검\|도끼\|창\|석궁\|낫\|스태프>` 운용자 명령어 (`empire.admin`) — `ClassInitService.grantStarterEquipment()` 호출 | `ClassAdminCommand.java` (신규) | ✅ |
| `plugin.yml` — `/직업` 명령어 등록 (`empire.admin` 권한) | `plugin.yml` | ✅ |
| `BUILD SUCCESSFUL` | — | ✅ |

### §6-12 남은 확인 항목
| 항목 | 비고 |
|---|---|
| HUD 행 X 정렬 미세조정 | rewind -176px 고정이라 행 너비 차이만큼 X 시작점 최대 ~30px 어긋날 수 있음. in-game 확인 후 행별 패딩 추가 결정 |
| 스코어보드 아이콘 크기 | U+E034~E036 height=16 — 아이콘 행이 텍스트 행보다 2배 높게 보일 수 있음 |
| 리소스팩 재배포 | 현재 SHA1 `5ba9751...` 변경 없음. 신 JAR 배포 후 `/resource-pack reload` 또는 서버 재시작 필요 |

---

## 다음 작업 후보

| 우선도 | 항목 | 비고 |
|---|---|---|
| 🔴 높음 | 코드↔기획 불일치 수정 (INBOX-005) | 강화 테이블 수치(DL-033 미반영)·보스 시드 모순(boss_master vs boss_entry_rule) 2건 최우선. 실제 시드 재확인 후 |
| 높음 | 관리자 GUI Phase 2 인게임 통합 테스트 | Step 1~5 GUI 클릭·명령·DB 표시 실서버 검증 |
| 중간 | EXP — 바닐라 XP 억제 인게임 확인 (DL-085) | `VanillaExpSuppressListener` — 접속 시 바 0, 몹 처치 시 오브 미생성, 커스텀 레벨링 정상 확인 |
| 높음 | JAR 재빌드 + 서버 배포 + in-game HUD/스코어보드 확인 | §6-12 수정 검증 — HUD 행 정렬, 아이콘 크기 확인 |
| 높음 | 서버 통합 테스트 — `/보스` 선택 → `[보스]` 표지판 → MM 스폰 런타임 확인 | `season_bosses.yml` 로드 + bossId 매칭 검증 |
| 중간 | HUD 행 X 정렬 패딩 (행별 advance 패딩으로 정확한 overlay) | 확인 후 필요 시 |
| 중간 | 서버 통합 테스트 — 봇 `/영지`·`/보스`·`/프로필` → Java API → 응답 확인 | 닉네임 기반 조회 실제 동작 검증 |
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
| BossSessionRepository 쓰기 경로 | ✅ 완료 — IL/강화·파티 평균 실측(DL-081), damage_share 실측(DL-084). `BossResultSummary`의 participantSummaryPlaceholder(damage_total·stagger)는 미사용(damage_share는 별도 boss_session_player 경로로 기록) |
| AllowAllUnlockQuestChecker | 보스6 클리어 조건 stub — 퀘스트 시스템 구현 후 연결 |
| questSnapshot 미적재 | `upsertQuestSnapshot` 호출 경로 없음 — `/player/by-nick` 응답의 `완료 퀘스트` 항목은 0 고정. 퀘스트 시스템 구현(§8+) 후 PlayerJoinListener에 연결 |
| boss_pattern_seed.csv | 7개 보스 placeholder 패턴만 있음 — 실제 패턴 설계 필요 |
| 스킬 자원 스택 최대값 | 각 스킬 파일에 max=3 또는 5 하드코딩 — CANON "유지형 자원 최대 6스택"은 각인(유지형/소모형 분기) 기반이며 1차 시즌 각인 제외로 해당 없음. 현재 값은 스킬 스펙(weapon_skills_v1.md) 기본값 |
| ~~스킬 이펙트 22종 미구현 (DL-070)~~ ✅ 해소 (2026-05-30) | **24/24종 전체 이펙트 구현 완료.** 검·창·도끼·낫·석궁·스태프 전 스킬 `pt:` 파티클+사운드 보유. 원거리 8종은 신규 `BaseWeaponSkill.spawnBeam`(시선 빔) 헬퍼 사용. 무기별 커밋: 검→창→도끼→낫→석궁·스태프. F는 전부 논타겟 유지 |
| ~~데이터 수집 공백 7종~~ ✅ 전체 해소 (2026-05-30) | INBOX-004 7종 모두 완료. #1 리텐션(DL-078)·#2 골드 흐름(DL-080)·#3 강화 로그(DL-079)·#4 보스 파티 스펙(DL-081)·#5 보스 데미지 기여(DL-084)·#6 PvP 무기/IL(DL-082)·#7 성장 시계열(DL-083). API: `/api/v1/activity·economy·pvp·growth`. 한계: 골드 source 미세분, 보스 damage add 미집계, MM spawn 반환 reflection 런타임 의존. 죽은 모델 `MarketPricePoint`·`LifeResourceSupplyRecord`(미사용 유지) |
