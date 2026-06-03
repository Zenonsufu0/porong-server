# 포로 서버 작업 현황

> 마지막 갱신: 2026-06-02 (§8 전투밸런스·리브랜드·라이선스·2D 이펙트 — DL-124/125)

---

## 현재 브랜치 상태

- 브랜치: `master` (origin 동기화 완료, GitHub 푸시됨)
- **플러그인 리브랜드: `EmpireRPG`/`empire-rpg` → `PoroRPG`/`poro-rpg` 전체 완료** (DL 없음, 커밋 메시지 참조). 패키지 `com.poro.rpg`, 명령어 `/poro`·`poro-*`, 권한 `poro.*`, 봇 `PORO_API_*`, 데이터 식별자(NBT·스코어보드 태그)까지. 재배포 시 `server/plugins/EmpireRPG→PoroRPG` 폴더 이전 필요.
- **§8 진행 중** — 전투 밸런스(DL-124/125) + 2D 스킬 이펙트(무기 9종) 구현. 서버 인게임 테스트 단계.
- 빌드: `./gradlew build → BUILD SUCCESSFUL` / 서버 클린 부팅 확인(Done ~15s, PoroRPG enable)
- 동기화: 매 handoff 시 master == codex-review 유지
- **다음 (재개 지점)**: 재접속 → §7 핵심 검증(무기별 독립 성장·GUI 36칸 통합·무기 lore·창 삼지창·스코어보드 위치) → 통과 시 잔여(튜토리얼 맵·필드 일반/정예 토글·HUD 리소스팩) 진행
- ※ **상태 드리프트 방지 정책**: task.md에 "최신 커밋 해시"·"미커밋/커밋" 상태를 박지 않는다. 최신 커밋 기준은 항상 `git log`. per-step 섹션은 완료 여부만 표기한다.

---

## §9 HUD·스코어보드 정리 (2026-06-02) — 진행 중

액션바 HUD(`HealthHudFormatter`) + 사이드바(`ScoreboardService`) 인게임 반복 튜닝.

### 완료
- **정렬 근본 해결**: 액션바 중앙정렬 문제 → 맨 앞 `negSpace(LEFT_ANCHOR)`로 전체 advance 고정(무기 유무 무관) + 행마다 정확한 폭 되감기(net 0). 픽셀 advance 테이블(바107·cd바·문자6 등) + 음수/양수 스페이스 글리프 합성.
- **HP/XP 좌측 정렬**(핫바 좌단 기준), 무기 들/안 들 때 위치 불변.
- **XP 행**: 경험치 수치(`현재/다음레벨`) 추가. **바 채움 = 커스텀 진행도**(currentExp/expToNextLevel) — 바닐라 XP 억제로 `player.getExp()`=0이라 바가 안 차던 버그 수정. 스코어보드 % 동일 수정.
- **쿨타임 2x2**: 입력 라벨 **LC/RC/SRC/F**(SkillInputListener 매핑), 라벨/시간 칸 **고정폭**(`-`↔숫자 전환 시 밀림 방지), 정수 초 표기, cd바 크기·간격 튜닝.
- **스코어보드**: invisible entry(평문 `poro_eN` 노출 제거) + **텍스트 폰트 상속 버그 수정**(HUD_FONT 상속→두부 깨짐 → empty 형제로 기본폰트 렌더) + 아이콘 height 16→10 축소 + **골드/강화석/큐브 라벨** 추가.

### ⚠ 리소스팩 폰트 변경 = 로컬 전용(gitignored, §8-3 정책)
`assets/export/resourcepack`의 `font/hud.json`·`textures/hud/chars.png`는 비추적. 재클론·재배포 시 **유실됨 — 별도 보관 필요**:
- `chars.png`: 24→25칸 확장, **'C' 글리프 추가**(라벨 LC/RC/SRC용).
- `hud.json`: HUD 행 ascent +3(위로), cd바 height 8·ascent 조정, 아이콘(E034~36) height 10·ascent 8, chars provider 5개에 C 코드포인트.
- 현재 sha1: **`7ffcf097…`**(DL-129 추가#14에서 갱신, server.properties와 동기). 구 9bdcdbb. 새 환경에선 이 폰트 델타 재적용 + zip 재패키징 필요.
- **chars.png 25→27칸 확장**(K·M 글리프 추가, XP K/M 단축표기용) + 고대흔적 4종 `.png.mcmeta` 애니메이션 추가(16×128 8프레임). 백업 `server/resourcepack.zip.bak.dl129`.

### 아이템 이름 가림 — 해소 (2026-06-02)
- 블럭/도구로 슬롯 변경 시 바닐라 아이템명이 액션바 HUD(XP바)에 가려 안 보이던 문제.
- `HealthHudListener.onItemHeld`: 새 아이템이 비무기(`WeaponTypeResolver.resolve==NONE`)면 2초간 HUD 억제 → 그 동안 **빈 액션바 송신**(`Component.empty()`)으로 HUD 레이어만 비움 → 클라 native 아이템 이름(toolHighlight, 별개 레이어)만 단독 노출.
- **제약 기록**: 클라 native 아이템명은 서버에서 못 끈다. 우리가 이름을 직접 보내면 native와 겹쳐 "두번" 보임 → 빈 액션바 방식 채택(사용자 확정).
- 무기/빈손 전환 시 즉시 억제 해제(cd/스택 HUD 우선).

---

## §8 전투밸런스·리브랜드·라이선스·2D 이펙트 (2026-06-01~02)

### §8-1 전투 밸런스 — 만충 스파이크 하이브리드 (DL-124) — 완료
- 조사: 정본 §4 "임계/만충" 배율이 코드에 **미구현**이었음 확인(per-stack 누진만). v1 DPS 모델(스프레드 19.3%) 허구 → 실측 37.6%(석궁 쿨14s 1위·스태프 20s 바닥).
- 임계 폐기 + **만충 ×1.20 디스크리트 스파이크**(검·창·석궁·스태프 핵심기, 소모형 만충 한정·유지형 게이팅). per-stack 절반 재배분. `scaledDamageFullChargeSpike`.
- C-2 비핵심 슬롯 보정(석궁 속사↓·스태프 마력탄↑ 등). 예측 스프레드 18.4%.
- Java + skill_master.csv + GrowthGuiListener 3중 동기. 정본 `combat_balance_v2 §4·§5`, `dps_balance_pass_v2.md`(v1 폐기), `skill_effects_reference_v1`.

### §8-2 자원 충전 명중 조건 정합 (DL-125) — 완료
- 빌더 6종이 명중 무관 발동만으로 스택 충전하던 버그 → 명중 판정 통일.
- 스태프 충전 LMB(마력탄) 한정(속성폭발·마력쇄도 gainStack 제거). 정본 §4 일치.

### §8-3 라이선스/보안 정리 — 완료
- 외부 제3자 에셋(asset.zip·Carnivoret·Vanquisher·Wuthering refs 등) **전 git 히스토리 제거**(git filter-repo + force push). `.gitignore` 강화.
- **assets/ 전체 비배포**(외부 유래 텍스처 혼재) — filter-repo + force push, 로컬 디스크 유지. `.git` 121M→3.5M.
- 비밀정보 스캔 클린(노출 0). **사유 라이선스 `LICENSE`**(2차배포·수정 금지) + README 전면 보강.
- 백업: `~/poro-backup-20260601-220033/`, `~/poro-assets-backup-20260601-222103/`.

### §8-4 2D 스킬 이펙트 — 무기 9종 (skill_effect_2d_integration_v1) — 완료
- 배경 제거 도구 `assets/source/effect_bg_remove.py`(13종 RGBA 변환).
- ItemDisplay 통합, 오리엔테이션 3패턴: `spawnSlash`(빌보드+3D비행), `spawnGroundTravel`(바닥평면+조준추종), `spawnDecal`(바닥장판).
- 무기 9개 효과(검·창·석궁·스태프×2·낫×2·도끼) 인게임 확정. carrier `paper` cmd 400101~108, 텍스처 `poro:item/effect/`, 코너 0-16 모델.
- 운영 토글 `NO_SKILL_COOLDOWN`(테스트 쿨0초). 스태프 마력쇄도 넉백 제거.
- 함정 기록: 중앙정렬 모델 스케일 오프셋·텍스처 경로 캐시·firework_star 특수렌더(메모리 `project_2d_effect_system`).

### §8 잔여
- **보스 텔레그래프 4종(400201~204)**: `BossPatternScheduler` 런타임 미배선이라 보류 — 보스 패턴 시스템 배선과 묶어 진행.
- ~~**공용 임팩트링(400901)**: 흰 코어 40% 과다~~ ✅ 텍스처 재작업 완료 (2026-06-02). 불투명 흰 코어(near-white 40%)를 알파 연산으로 투명화 + 잔여 백열 amber 틴트 → near-white 0%, 중앙 투명. `assets/source/effects_clean/effect_impact_ring_amber.png`(1254) + export 256 재생성, 원본 `.bak.whitecore` 보관. **배선 완료**: `AxeColossalDropSkill`(거신추락)에 충격파 고리로 배선 — 기존 균열 decal(400106 scale11) 위에 임팩트링(400901 scale13, 10틱, +0.02블록 z-fight 방지)을 감싸듯 팝. `spawnDecal` 직접 호출(주변 코드 패턴). 균열·임팩트링 모델이 동일 기하(코너 0-16 quad)라 방향 리스크 0. `compileJava` ✅. **인게임 확인 완료(2026-06-02)**: 링이 균열을 적절히 감싸고 z-fight 없이 깔끔 — scale 13·10틱·+0.02y 그대로 확정. 리소스팩 재패키징(sha1 `7c2a3957…`) + 새 JAR 배포 후 실서버 검수.
- 리소스팩 자산은 `assets/`(gitignored) 로컬+HTTP 서버 — 새 환경 클론 시 별도 배포 필요.
- 보스 50블록 빔 등 일부 수치 미세조정 여지.

---

## §7 서버 테스트 준비 + 온보딩 + 무기 시스템 (2026-05-30~31)

기준: INBOX-006/007, DL-099~104. 라이브 인게임 테스트 기반 반복 수정.

### §7-1 서버 가동 기반 — 완료
| 항목 | 상태 |
|---|---|
| 배포 트랩 해소 — 런타임 시드/config가 `saveResource(replace=false)`로 안 덮이던 문제 인지, jar만 교체하는 배포 흐름 확립 | ✅ |
| 단일 평지 월드 `world` (level-type=flat, surface y=64) + 보스룸 X10000대·PvP X20000대·필드 X0~4000 배치 (DL-099) | ✅ |
| 부팅 블로커 해소 — skill_master class_id optional, NPC sync 비-fatal, life/facility validation 완화, 중복 jar·session.lock 정리 | ✅ |

### §7-2 동적 필드 스폰 (INBOX-006, DL-100) — 코어 완료 / 2차 대기
| 항목 | 상태 |
|---|---|
| `FieldSpawnService` — 플레이어 주변 웨이브(~15s, 10~15마리), 2단 캡(플레이어 40/필드 250), 소유자 추적, 오프라인·사망 정리, 1000간격 5필드 | ✅ |
| 일반/정예 토글(명령어+GUI)·랜덤 진입(≥35블록)·디스폰 정밀화 | ⏳ 2차 |

### §7-3 허브 온보딩 (INBOX-006, DL-101/102) — 코어 완료
| 항목 | 상태 |
|---|---|
| `HubWorldService` — 별도 평지 월드 `world_hub`(수도) 생성, PVP off·PEACEFUL (DL-101) | ✅ |
| `HubSpawnListener` — 접속 시 전원 허브 이동(1틱 지연) (DL-102) | ✅ |
| 첫 접속 무기 선택 → IS 섬 자동 생성·이동 (`is create poro`) (DL-102) | ✅ |
| IS 스키매틱 picker 우회 — `schematics.yml` poro 1개만 (백업 .bak.4schem) | ✅ (인게임 재검증 필요) |
| 튜토리얼 맵(안내 스텝) — 첫 접속을 허브 대신 튜토리얼로 | ⏳ 후속 |

### §7-4 온보딩 라이브 피드백 (INBOX-007) — 완료
| 항목 | 상태 |
|---|---|
| 스타터 방어구 = 가상 전용 확정 (물리 미지급, §2 DEF 이중적용 방지) (DL-103) | ✅ |
| 낫 우클릭(월영회전) 시선 방향 dashForward (getVelocity 서버측 ~0 버그) | ✅ |
| 스코어보드 world_hub→"수도", IridiumSkyblock→"[플레이어]의 영지" | ✅ |

### §7-5 무기 시스템 — 무기별 독립 성장 + GUI 통합 (INBOX-007 2차, DL-104) — 완료
| 항목 | 파일 | 상태 |
|---|---|---|
| 무기별 독립 성장 — `weapon_<타입>` 인스턴스, 교체 시 장착 → 강화/큐브가 현재 무기에만 적용 | `GrowthGuiListener`, `ClassInitService` | ✅ |
| 무기 선택/변경 GUI 스펙 36칸 통합 (검10·도끼13·창16/석궁19·낫22·스태프25/뒤로27) | `gui/WeaponGui.java`(신규) | ✅ |
| 손에 든 무기 lore = 무기변경 형식(강화/등급/잠재/세부스탯/각인) | `gui/WeaponItemFactory.java`(신규) | ✅ |
| 창 아이콘 NETHERITE_SWORD→TRIDENT (선택·변경·물리 무기) | `WeaponGui`, `GrowthGuiListener`, `ClassInitService` | ✅ |
| 스코어보드 위치 변경 감시 태스크(1초 폴링·변경 시만 refresh) + 좌표 기반 구역명(필드5/보스룸/PvP) | `ScoreboardService`, `PoroRPGPlugin` | ✅ |
| `BUILD SUCCESSFUL` + 배포 + 서버 클린 부팅 | — | ✅ |

### §7-5 잔여 (DL-104 후속)
- 손에 든 무기 lore 실시간 동기화 — 강화 직후 슬롯0 갱신 안 됨(GUI엔 즉시 반영). 현재 지급/교체 시점에만 빌드
- 무기 변경 3분 쿨타임(CANON §55) 미구현 — 전투 중 차단만
- 무기별 각인 — 현재 각인은 클래스 단위 공유
- 스코어보드 구역명이 config 좌표와 결합 — 좌표 변경 시 `resolveWorldArea` 상수도 수정 필요
- 영지명 rename 반영 — 현재 "[플레이어]의 영지" 고정(territory store 연동 후속)
- item_master.csv 스키마 분기 — `server-config`판(`t1_armor_head`) vs runtime판(`t1_helmet_starter`) 정리 필요

### §7 인게임 재검증 포인트 (재개 시 우선)
1. **무기별 독립** — 검 +N강 후 낫 교체 시 낫 +0강, 검 복귀 시 +N 유지 (핵심)
2. GUI 36칸 통합 (첫 진입 = 무기 변경 동일 배치)
3. 손에 든 무기 lore 형식 / 창 삼지창
4. 스코어보드 위치 — 영지→필드(평원/광산/하수도/전초기지/폐허)·보스룸 전환
5. IS 스키매틱 picker 미출현 (③)

---

## §6 PoroRPG 플러그인 코어 구현 — 완료

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
| `BossRoomGenerationService` + `/poro-genrooms` 커맨드 | `7be9897` | ✅ |
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
| `PoroHttpServer` (포트 8765, JDK 내장 HttpServer) | ✅ |
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
| `NamespacedKey "poro_item_id"` 추가 — PDC 태그 우선 조회, 없으면 displayName→ItemMaster.itemName() fallback | ✅ |
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
| `BUILD SUCCESSFUL` + `server/plugins/PoroRPG.jar` 재배포 | — | ✅ |

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
| JAR 재빌드 + `server/plugins/PoroRPG.jar` 재배포 | — | ✅ |

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
| `/poro-admin` 허브 + Phase 1 GUI (인스펙트·진행매치·통계·슬롯해제) | `AdminHubGui`, `AdminInspectGui`, `AdminMatchesGui`, `AdminStatsGui`, `AdminGuiListener`, `AdminHubCommand` | ✅ |
| Anvil 닉네임 → `AdminInspectGui` 27슬롯 (직업·레벨·영지·PvP·재화·장비 5슬롯·평균 IL) | `AdminInspectGui` | ✅ |
| 운영자 단건 변경 8종 (`poro-give`, `poro-currency`, `poro-rank`, `poro-enhance`, `poro-level`, `poro-pvp-score`, `poro-cleanse`, `poro-island-reset`) | `AdminPlayerCommand` (신규) | ✅ |
| `PlayerEquipmentItem.setEnhanceLevel` 가시성 public 승격 (관리자 명령 접근용) | `PlayerEquipmentItem` | ✅ |
| `PvpMatchService.activeMatches()` / `adminForceEnd(matchId, reason)` 추가 | `PvpMatchService` | ✅ |
| `PvpRatingService.adminAdjustScore(uuid, name, delta)` 추가 | `PvpRatingService` | ✅ |
| 권한 분리 (sub-permissions) + 명령어 네이밍 일관화 | `plugin.yml` | ✅ |

## §6-15 관리자 Phase 2 Step 2 — 운영 토글 — 완료 (2026-05-30, 커밋 `ff46573`+`58a38e0`)

기준: 사용자 지시 "C 방식 — 모든 동작은 명령어로도 존재해야 하고 운영자용 편의성/기능성 명령어도 검토"

| 항목 | 파일 | 상태 |
|---|---|---|
| `AdminTogglesService` — 5개 플래그 (`BOSS_SPAWN_PAUSE`/`ENHANCE_BOOST`/`EXP_BOOST`/`DROP_BOOST`/`PVP_QUEUE_PAUSE`), in-memory `LinkedHashMap`, isOn/setOn/setOff/toggle/all | `admin/AdminTogglesService.java` (신규) | ✅ |
| `/poro-toggle <flag> [on|off]` / `/poro-toggle list` | `command/AdminTogglesCommand.java` (신규) | ✅ |
| `AdminTogglesGui` — 27슬롯, Toggle 당 LIME/GRAY 다이 아이콘, `open()` 반환 `Toggle[]`로 슬롯 매핑 | `gui/AdminTogglesGui.java` (신규) | ✅ |
| `GuiTitles.ADMIN_TOGGLES` 추가 | `gui/GuiTitles.java` | ✅ |
| `AdminHubGui` slot 31 stub → 활성 LEVER 아이콘 | `gui/AdminHubGui.java` | ✅ |
| `AdminGuiListener` — togglesService 주입, `ADMIN_TOGGLES` 클릭 핸들러, `toggleSlotMapping` | `listener/AdminGuiListener.java` | ✅ |
| `PoroRPGPlugin` — `AdminTogglesService` 생성 + `AdminTogglesCommand` 등록 | `PoroRPGPlugin.java` | ✅ |
| `plugin.yml` — `poro-toggle` 명령 등록 (`poro.admin`) | `plugin.yml` | ✅ |
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
| 신규 명령 없음 — 액션은 기존 `/poro-rank`·`/poro-island-reset`와 동등 (C 방식) | — | ✅ |
| `BUILD SUCCESSFUL` | — | ✅ |

### §6-18 잔여
- 멤버 개별 강제 제거 GUI — 현재는 전체 초기화만. 후속
- 영지 customItem(자원) 직접 지급/회수 GUI — `/poro-currency`·`/poro-give` 명령 존재, GUI는 후속
- 작위 변경 즉시 영속화 — `/poro-rank`와 동일하게 `setRank`만 (기존 저장 경로 위임). 별도 persist 미호출

---

## §6-17 관리자 Phase 2 Step 4 — 보스 디버그 GUI + /poro-boss-list·/poro-boss-end — 완료 (2026-05-30, 커밋 `7c02972`)

기준: `docs/10_development_roadmap/admin_gui_phase2.md` §보스 디버그 (slot 24)

| 항목 | 파일 | 상태 |
|---|---|---|
| `AdminBossGui` — 54슬롯 진행 중 보스 런 목록, 좌클릭=강제 종료 | `gui/AdminBossGui.java` (신규) | ✅ |
| `/poro-boss-list` + `/poro-boss-end <runId>` (접두어 매칭, 한 핸들러 라벨 분기) | `command/AdminBossCommand.java` (신규) | ✅ |
| 강제 종료 = `BossRunService.endRun(runId, false, "admin_force")` → onRunEnded → `releaseByRunId` (슬롯 해제 검증됨, BossRewardService:115) | — | ✅ |
| 조회 = `BossRunService.activeRuns()` (도메인 변경 없음, 기존 API 재사용) | — | ✅ |
| `GuiTitles.ADMIN_BOSS` + `AdminHubGui` slot 24 활성 | `gui/*` | ✅ |
| `AdminGuiListener` — `bossRunService` 주입 + `ADMIN_BOSS` 핸들러 + `bossRunMapping` | `listener/AdminGuiListener.java` | ✅ |
| `PoroRPGPlugin` — `bossEngineRuntime.runService()` 주입 + 두 명령 등록 | `PoroRPGPlugin.java` | ✅ |
| `plugin.yml` — `poro-boss-list`·`poro-boss-end` 등록 (`poro.admin`) | `plugin.yml` | ✅ |
| `BUILD SUCCESSFUL` | — | ✅ |

### §6-17 잔여
- 강제 페이즈 트리거 (`BossPatternScheduler.enqueueForced`) — Step 4+ 보류
- MM 보스 엔티티 잔존 — 강제 종료는 슬롯만 해제. 엔티티 despawn은 후속

---

## §6-16 관리자 Phase 2 Step 3 — 로그/감시 GUI + /poro-log — 완료 (2026-05-30, 커밋 `ef31e9e`)

기준: `docs/10_development_roadmap/admin_gui_phase2.md` §로그/감시 (slot 33)

| 항목 | 파일 | 상태 |
|---|---|---|
| `AdminLogGui` — 54슬롯 3탭(강화/거래/PvP) 페이지네이션 45/page, 읽기 전용 | `gui/AdminLogGui.java` (신규) | ✅ |
| `/poro-log [enhance\|trade\|pvp]` 텍스트 출력 (콘솔 가능, 탭별 최근 10건) | `command/AdminLogCommand.java` (신규) | ✅ |
| `AuctionStore.recentSold(limit)` — `status='sold' ORDER BY sold_at DESC` | `market/AuctionStore.java` | ✅ |
| `PvpMatchLogRepository.recentMatches(limit)` + `PvpMatchLogRow` record | `pvp/db/PvpMatchLogRepository.java` | ✅ |
| 강화 로그 소스 = `InMemoryEnhancementLogHook.logs()` (in-memory, 휘발성) | — | ✅ |
| `GuiTitles.ADMIN_LOGS` + `AdminHubGui` slot 33 stub→활성 | `gui/*` | ✅ |
| `AdminGuiListener` — 로그 소스 3종 주입 + `ADMIN_LOGS` 탭/페이지 핸들러 + `LogView` 상태 | `listener/AdminGuiListener.java` | ✅ |
| `PoroRPGPlugin` — `pvpMatchLogRepo` 필드 승격 + 주입 + `/poro-log` 등록 | `PoroRPGPlugin.java` | ✅ |
| `plugin.yml` — `poro-log` 등록 (`poro.admin`) | `plugin.yml` | ✅ |
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

- 와이어링: `PoroRPGPlugin` — `adminTogglesService` 필드 승격, `pvpMatchService.attachToggles()` + `enhancementService().setEnhanceBoostSupplier()` + 두 리스너 생성자 주입
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
| `/직업 <플레이어> <검\|도끼\|창\|석궁\|낫\|스태프>` 운용자 명령어 (`poro.admin`) — `ClassInitService.grantStarterEquipment()` 호출 | `ClassAdminCommand.java` (신규) | ✅ |
| `plugin.yml` — `/직업` 명령어 등록 (`poro.admin` 권한) | `plugin.yml` | ✅ |
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
| 중간 | 코드↔기획 불일치 후속 (INBOX-005) | 해소: 🔴 강화테이블(DL-086)·보스시드(DL-087), 🟠 영지 오프라인생산+채굴기시드(DL-088). **잔여 🟠**: 강화 흔적 미연동·필드보스 스폰 스케줄러 stub. 🟡: 금지설계 시드정리·장비 이름변경권. **server-config**: MM fallen_knight 셸 충돌, 배포 사본 동기화 |
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

---

# 🔻 다음 세션 인계 — 잠재 풀 전면 정비 (A) + 밸런스 후속 (2026-06-03 작성)

## 이번 세션 완료 (DL-128#11~14, 모두 라이브 적용·재시작 반영)
- **#11 보스 HP 캡 해제**: `server/spigot.yml` `attribute.maxHealth.max 1024→2048000`. 1024 클램프가 모든 보스/필드 큰몹 HP를 1024로 묶던 근본 원인. **★재배포 시 재적용 필수**(gitignore).
- **#12 플레이어 HP 산식 상향**: `combat/SkillContext.java` 기본HP합 80→180, HP 전용 계수 0.11(DEF 0.04와 분리). +0≈200/+8≈358/+18≈556. 보스 패턴 데미지 역산 재조정. fallen_knight HP 8000(테스트값, 설계 파티 150k).
- **#13 무기 ATK 강화 곡선↑** `combat/WeaponPowerCalculator.java`(+18 195=2.4배) + **필드 몹 ATK 재밸런스** `admin/config/MobStatOverrideService.java`(F1 일반8→F5 34, loadAndSeed upsert로 재적용).
- **#14 보스 DEF 실데미지 적용 + 방어력무시**: `SkillContext.defenseMitigation`(×200/(200+유효DEF)), `BaseWeaponSkill.dealDamage` 적용, 보스 DEF 시드(`MobStatOverrideService.DEF_SEED` 100~280)→스폰 시 PDC `poro_rpg:mob_def` 기록. 방어력무시 잠재 옵션 정본값으로 추가.

## 밸런스 분석 결과 (서브에이전트 3종) → `docs/04_combat_weapon_skills/balance_review_dl128.md`
- 보스 HP 권장: fallen_knight 8k→30~35k(DEF무효 가정, DEF 살아있으니 ×1.5 반영해 ~20k), 시즌보스 HP가 평탄(150~170k)한데 권장강화는 6→22강 = **상위보스 역전** → 권장강화 비례 곡선 필요.
- 필드 45마리/분: **F1만 스폰 병목**(15초·12마리 리필, 7초 노는 공백), F2~F5는 화력 부족(스폰 대기 아님). 스폰반경 코드 5~10 vs 주석 20~30 불일치.
- 골드: 보스 미지급(의도). 인입=필드몹/영지 재료 판매(사장님 확인).

## ▶ 메인 작업: 잠재 풀 전면 정비 (사장님 "A로 가야지" 확정)
**문제**: 구현 잠재 풀(`custom-plugins/poro-rpg/src/main/resources/seeds/growth_potential_option_pool.csv`)이 **정본 `docs/02_database_api_stats/potential_options_v1.md`(2026-05-24 재확정)과 전혀 안 맞음**. 구현엔 폐기/금지 옵션(mark_target_damage=적표식 금지, crack_efficiency, resonance_effect_up, conditional_damage_bonus) 섞여있고, 정본 옵션 다수 미구현.
**현재 실제 작동 옵션 4종뿐**: attack_percent, general_damage_increase, boss_damage_increase, defense_ignore(#14 추가). 나머지는 CSV에 있어도 소비 코드 없음.

### 정본 잠재 풀 (potential_options_v1.md §1~2 — 슬롯별 옵션 + 등급값 전부 명시됨)
- **무기 11종**: ATK%/방어력무시%/스킬피해%/보스피해%/기본기·이동기·특수기·핵심기 피해%/치명타확률%/치명타데미지%/쿨타임감소%
- **머리 7(+유니크 받피감)**: HP%/DEF%/치명타확률%/스킬타입4종 + (유니크+)받피감%
- **상의 7(+1)**: HP%/DEF%/스킬피해%/스킬타입4종 + 받피감%
- **하의 8(+1)**: HP%/DEF%/쿨감%/보스피해%/스킬타입4종 + 받피감%
- **신발 9**: 이속%/HP%/방어력무시%/쿨감%/ATK%/스킬타입4종 (받피감 없음)
- 등급값 표: 정본 §2-1~2-5 (커먼~레전더리, 정수). 라인 적용: 1라인=현등급/2·3라인=한단계아래(이탈 2라인10%·3라인5%는 현등급).
- 방어력무시 메카닉: 유효DEF=DEF×(1−방무%). **이미 #14에서 구현됨.**

### 미구현 메커니즘 (정본 정비 = 아래 신규 구현 필요)
| 옵션 | 난이도 | 구현처 |
|---|---|---|
| 스킬 피해%(skill_damage) | 쉬움 | general_damage_increase 정렬/개명, dealDamage 전체 적용 |
| 치명타 확률%·데미지% | 쉬움 | SkillContext.critChance/critDamageMultiplier 합산 |
| DEF%·HP% | 쉬움 | SkillContext.defense()·armorMaxHealth 합산(현재 잠재 미반영) |
| 받는 피해 감소% | 쉬움 | listener/PlayerDefenseListener |
| 이동 속도% | 쉬움 | MOVEMENT_SPEED 속성 |
| 기본기/이동기/특수기/핵심기 피해% 4종 | 중간 | **입력 구분 이미 있음**: `listener/SkillInputListener`(slot1 LC=기본기/slot2 RC=이동기/slot3 SRC=특수기/slot4 F=핵심기). 스킬→slot 태그를 dealDamage에 전달해 승수 적용 |
| 쿨타임 감소% | 중간 | CooldownManager 훅 |
| 슬롯별 풀 차등 | 구조변경 | **현 item slot_type="armor" 단일**(머리/상의/하의/신발 구분 없음). `PotentialService.resolvePoolId`=`tier_slotType_pool`. 정본대로 4풀 나누려면 item_master.csv slot_type 세분 또는 resolvePoolId가 item_id/EquipmentSlot에서 sub-slot 도출 |

### 3단계 계획 (사장님 확정: 순서 1→2→3 / 3단계=완전 분리)
1. ~~**CSV 정본 재작성 + 쉬운 메커니즘**(스킬피해/치명확률·데미지/DEF%/HP%/받피감/이속)~~ ✅ **완료 (DL-129, 2026-06-03)** — `compileJava` 통과. 인게임 검증 대기.
2. ~~스킬타입 피해% 4종(SkillInputListener slot 연동) + 쿨타임 감소%~~ ✅ **완료 (DL-129 추가#1, 2026-06-03)** — SkillType.fromKey(키→타입) + dealDamage 배율 + SkillService 쿨감. 무기풀 11종 정본 완성. 인게임 검증 대기.
3. ~~슬롯별 풀 차등(머리/상의/하의/신발) — 완전 분리(item_master slot_type 세분)~~ ✅ **완료 (DL-129 추가#2, 2026-06-03)** — item_master armor→head/chest/legs/feet, EquipmentSlot itemSlotType 세분, CSV 10풀(생성기), RuneService armor shim, WeaponPowerCalculator 신발 ATK% 합산. build SUCCESSFUL. **잠재 풀 정비 1~3단계 전체 완료 = 정본 100% 정합.**

### 인게임 피드백 반영 (DL-129 추가#3~#5, 2026-06-03)
- 등급업 보호(재굴림 차단·확정 버튼), 잠재 채팅 한글 장비명(equipDisplayName, 이름변경권 연동지점)
- 등급업 확률 정본화(레어→에픽 5%·에픽→유니크 1%) + 큐브 천장(기댓값×2=8/40/200/2000, pityCount 영속) + GUI에 확률·천장·부위풀 표시
- 공방 상단 탭 고유 아이콘+글로우(유리 구분불가 해소)
- **신규 운영 명령**: `/골드`·`/강화석`·`/큐브`·`/큐브조각 <플레이어> [수량]`(poro.admin) / **`/정예 [on|off]`**(전체, 필드 정예 토글·세션메모리)
- **#1 추가**: 필드 정예 GUI 토글(FieldHubGui 슬롯22)
- **#5 완료(DL-129 추가#7)**: 영지 시설 설치 시스템 — 작위별 슬롯 한계(4~18), 빈칸 클릭→시설(약초/광물/공방) 설치, **공방 대기슬롯=가공기×3**(설계 정합), workshopMachineCount 영속. ⚠기존 플레이어는 가공기 설치해야 공방 사용 가능
- **#6 보류(에셋 파이프라인)**: 공방 텍스처 — 텍스처·모델 JSON은 존재하나 1.21.10 `items/` 정의·`setItemModel`·pack 재패키징(sha1)이 통째로 누락. 리소스팩 접속차단 위험 + bb 워크플로우 필요로 별도 신중 작업

### 잠재 풀 정비 — 인게임 검증 체크리스트 (배포 후)
- 부위별 옵션 분리: 머리=치명확률/HP/DEF/스킬타입, 상의=스킬피해/HP/DEF, 하의=쿨감/보스피해/HP/DEF, 신발=이속/ATK/방무/쿨감 — 큐브로 굴려 각 부위가 자기 풀 옵션만 뜨는지
- 받피감: 머리/상의/하의 유니크+에서만, 신발엔 안 뜸
- 신발 ATK% → 무기 공격력(스탯창 ATK) 증가 반영
- 스킬타입: 핵심기 잠재 → F스킬만↑, 쿨감 → 쿨타임·HUD 단축
- 강화/큐브 직후 HP·이속 즉시 반영
- ⚠ **재배포 시**: item_master.csv·growth_potential_option_pool.csv 모두 jar에 포함(seeds) — saveResource replace=false 트랩 주의(기존 서버는 시드 갱신 안 될 수 있음, 런타임 시드 교체 확인)

**1·2단계 인게임 검증 포인트(재개 시)**: ① 잠재 GUI 큐브 사용 시 폐기옵션 안 뜨고 정본 옵션만 → 한글 라벨(스킬피해/치명타확률·데미지/방어력/최대체력/받는피해감소/기본기·이동기·특수기·핵심기 피해/쿨타임 감소) ② 무기 치명확률·치명데미지 잠재 → 캐릭터 스탯창 수치 반영 ③ HP% 잠재 장착 → 최대체력 증가, 큐브 사용 직후 즉시 반영 ④ 이속% → 실제 이동 빨라짐(현재 롤소스는 통합풀 미포함, 임시 테스트 시 CSV에 1줄 추가 필요) ⑤ 받피감(유니크+) → 몹 피격 데미지 감소 ⑥ **핵심기 피해% 무기 잠재 → 핵심기(F) 스킬만 데미지↑, 기본기(LC)는 불변**(스킬타입 분리 확인) ⑦ **쿨감% 잠재 → 스킬 쿨타임·HUD 진행바 단축**.

### 핵심 파일
- 정본: `docs/02_database_api_stats/potential_options_v1.md`
- CSV: `custom-plugins/poro-rpg/src/main/resources/seeds/growth_potential_option_pool.csv`
- 롤/풀선택: `growth/engine/PotentialService.java`(resolvePoolId·filteredOptions·generateProfile)
- 소비처: `combat/SkillContext.java`(generalDamageMultiplier·bossDamageMultiplier·critChance·critDamageMultiplier·defense·armorMaxHealth·defenseIgnorePercent·defenseMitigation), `combat/WeaponPowerCalculator.java`(attack_percent), `combat/skills/BaseWeaponSkill.java`(dealDamage)
- 입력유형: `listener/SkillInputListener.java`(slot1~4)
- 코드 stale 주석: `boss/db/BossParticipantSpec.java`("1차 방무 없음" — 정본과 모순, 정비 시 갱신)

## 기타 밸런스 후속 (분석 기반, 우선순위 사장님 결정)
- 보스 HP 재튜닝(DEF 반영): fallen_knight 8k→? / 시즌보스 HP 곡선 역전 해소
- 필드 F1 스폰 주기 단축(15→10초 또는 batch 12→16), 스폰반경 5~10 의도 확인
- 필드보스 HP 10만~66만 솔로 적정성
- 기존 접속자는 재접속해야 HP/ATK 산식 갱신 적용
