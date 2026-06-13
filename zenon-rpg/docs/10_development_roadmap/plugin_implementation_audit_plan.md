# 플러그인 구현상태 전수검사 계획서

> **[STATUS: REFERENCE]** — 현재 ZenonRPG 코드가 기획서·CANON 기준과 얼마나 일치하는지 점검하고, 실행되지 않거나 화면에 보이지 않는 기능의 원인을 정리하는 구현 계획서.
>
> 작성일: 2026-05-28

---

## 0. 목적

현재 `custom-plugins/zenon-rpg`에는 여러 도메인 클래스와 GUI, 보스, 보상, 저장 로직이 존재하지만 실제 서버에서 실행되지 않거나 사용자에게 보이지 않는 기능이 많다.

이 문서는 코드 수정을 시작하기 전 아래 항목을 고정한다.

- 기획서 대비 구현 차이점
- CANON과 충돌하는 모순점
- 구현은 있으나 명령어·GUI·부트스트랩 연결이 없어 보이지 않는 기능
- 우선순위별 수정 계획

---

## 1. 기준 문서

판단 우선순위는 루트 `AGENTS.md`의 문서 계층을 따른다.

| 우선순위 | 문서 | 역할 |
|---|---|---|
| 1 | `../final_master_plan.md` | 프로젝트 전체 방향과 1차 시즌 범위 |
| 2 | `../01_plugin_architecture/CANON.md` | 플러그인 경계, 제거 플러그인, 부트스트랩 순서 |
| 3 | 각 도메인 `CANON.md` | 성장, 전투, 영지, 필드/보스, GUI 공식 기준 |
| 4 | `implementation_design_plan.md` | 본격 구현 전 확정한 DP-001~DP-006 및 구현 순서 |
| 5 | 도메인 상세 문서 | 수치·GUI·드랍·보상 세부 기준 |

`zenon-rpg/docs/_archive/`는 현재 기준으로 사용하지 않는다.

---

## 2. 핵심 결론

현재 구현은 `implementation_design_plan.md`의 일부 항목을 반영했지만, 실제 체감 기능은 제한적이다.

주요 원인:

| 원인 | 설명 |
|---|---|
| CANON 충돌 | 제거 확정인 Citizens/BetonQuest/NPC 모듈이 `plugin.yml`과 부트스트랩에 남아 있다. |
| 진입점 미연결 | 일부 GUI 클래스는 있으나 `/메뉴`, `/영지`, `/상점`, `/보스정보`, `/클리어` 등 플레이어 명령에서 stub 응답만 한다. |
| 상태 provider stub | 필드보스 상태 표시가 실제 스폰/리스폰 상태와 연결되지 않고 고정값을 반환한다. |
| 보상 로직 축약 | 시즌보스 S/A/B/C, 주간 첫 클리어, 재도전, 인원 배율, 고대흔적 지급이 문서 기준보다 단순하다. |
| 패턴 seed 미완성 | 보스3 이후 및 최종보스 패턴 seed에 placeholder가 남아 있다. |
| 문서-코드 버전 차이 | 저장 schema는 기획서 v3 기준보다 앞선 v4로 구현되어 있으나 문서 반영이 부족하다. |

---

## 3. 차이점과 모순점

### 3.1 아키텍처 / 부트스트랩

| 항목 | 문서 기준 | 현재 구현 상태 | 판정 |
|---|---|---|---|
| Citizens | 제거 확정. NPC 기반 진행 없음. | `plugin.yml` softdepend에 `Citizens` 존재. `npc/citizens` 패키지와 `NpcSyncBootstrap` 존재. | CANON 모순 |
| BetonQuest | 제거 확정. | `plugin.yml` softdepend에 `BetonQuest` 존재. Citizens-BetonQuest bridge 코드 존재. | CANON 모순 |
| 주요 상호작용 | GUI 기반. | NPC sync seed와 trait binder가 남아 있다. | 설계 충돌 |
| 부트스트랩 순서 | Common → Master → Combat → Boss → Growth → Life → Quest → Operations | Master 이후 NPC sync가 먼저 실행된다. | 순서 불일치 |

### 3.2 저장 / 데이터

| 항목 | 문서 기준 | 현재 구현 상태 | 판정 |
|---|---|---|---|
| JSON 위치 | `plugins/ZenonRPG/playerdata/{uuid}.json` | 동일 | 일치 |
| schemaVersion | `implementation_design_plan.md` 기준 v3 | `PlayerSaveData.CURRENT_VERSION = 4` | 문서 미갱신 |
| legacy wallet migration | `enhancement_stone` → `mat_stone_enhance` | 구현됨 | 일치 |
| 종료 저장 | Quit 즉시 save + cache 제거, onDisable 전원 저장 | 구현됨 | 일치 |
| 자동 저장 | 5분 주기 snapshot 후 async I/O | 구현됨 | 일치 |

### 3.3 클래스 / 스킬 입력

| 항목 | 문서 기준 | 현재 구현 상태 | 판정 |
|---|---|---|---|
| 클래스 선택 GUI | 27슬롯, 10~15번 무기 선택 | 구현됨 | 일치 |
| 초기 지급 장비 | WEAPON/HELMET/CHESTPLATE/LEGGINGS/BOOTS 5슬롯 | 구현됨 | 일치 |
| 스킬 입력 | LMB/RMB/Shift+RMB/F 4종 | 구현됨 | 일치 |
| 안전구역 차단 | SafeZoneService 또는 WorldGuard adapter | 구현됨. WorldGuard 없으면 Noop. | 부분 일치 |

### 3.4 GUI / 명령어

| 명령어/기능 | 기대 상태 | 현재 상태 | 판정 |
|---|---|---|---|
| `/메뉴` | 메인 허브 오픈 | 준비 중 stub | 보이지 않음 |
| `/장비` | 장비 허브 오픈 | 연결됨 | 일치 |
| `/강화` | 강화 GUI 오픈 | 연결됨 | 일치 |
| `/잠재` | 잠재 GUI 오픈 | 연결됨 | 일치 |
| `/전승` | 전승 GUI 오픈 | 연결됨 | 일치 |
| `/영지` | 영지 허브 오픈 | 준비 중 stub | 보이지 않음 |
| `/영지이동` | 영지 이동 GUI 오픈 | 준비 중 stub | 보이지 않음 |
| `/작물` | 작물 관리 GUI 오픈 | 준비 중 stub | 보이지 않음 |
| `/상점` | 상점 GUI 오픈 | 준비 중 stub | 보이지 않음 |
| `/보스` | 보스 허브 오픈 | 연결됨 | 일치 |
| `/파티` | 파티 허브 오픈 | 연결됨 | 일치 |
| `/파티목록` | 파티 목록 오픈 | 연결됨 | 일치 |
| `/보스정보` | 보스 정보 GUI 오픈 | 준비 중 stub | 보이지 않음 |
| `/클리어` | 클리어 기록 GUI 오픈 | 준비 중 stub | 보이지 않음 |
| `/필드` | 필드 허브 오픈 | 연결됨 | 일치 |

### 3.5 필드 / 드랍 / 보상

| 항목 | 문서 기준 | 현재 구현 상태 | 판정 |
|---|---|---|---|
| 필드몹 식별 | scoreboard tag 기반 | `MobTagHelper` 구현됨 | 일치 |
| customName 판정 | 제거 | 제거된 것으로 보임 | 일치 |
| 일반/정예몹 드랍 | `drop_tables_v1.md` §1~2 | 하드코딩 profile로 구현됨. 수치 재검증 필요 | 부분 일치 |
| 필드보스 기여도 | 총 피해 3% 이상 | `ContributionTracker` + `BossRewardService.grantFieldBossReward` 구현 | 부분 일치 |
| 필드보스 상태 표시 | 실제 스폰/리스폰 상태 표시 | field state provider가 `RESPAWNING` 고정 stub | 보이지 않음 |
| 큐브 조각 자동 전환 | wallet 기준 10개 → 큐브 1개 | 구현됨 | 일치 |

### 3.6 시즌보스 / 패턴

| 항목 | 문서 기준 | 현재 구현 상태 | 판정 |
|---|---|---|---|
| 시즌보스 보상 등급 | S/A/B/C별 차등 | 코드상 단순 table 중심 | 미완성 |
| 주간 첫 클리어 | 고대흔적 지급 | 명확한 주간 첫 클리어 분기 미확인 | 미완성 |
| 재도전 보상 | 별도 낮은 보상 | 미확인 또는 미구현 | 미완성 |
| 인원 배율 | 1인/2인 소모성 재화 배율 적용 | 명확한 반영 미확인 | 미완성 |
| 보스3 이후 패턴 | 공용 패턴 + 고유 패턴 조합 | `boss_pattern_seed.csv`에 placeholder 다수 | 미완성 |
| P-10 | 폐기 | 현재 seed에서 P-10 직접 사용은 확인되지 않음 | 유지 필요 |

---

## 4. 실행되지 않거나 보이지 않는 기능 목록

| 분류 | 기능 | 원인 |
|---|---|---|
| 메인 진입점 | `/메뉴` | 명령 라우터에서 stub 처리 |
| 영지 허브 | `/영지`, `/영지이동`, `/작물`, `/영지설정` | 명령 라우터에서 stub 처리 |
| 상점 | `/상점` | `ShopGui`는 stub reload 수준, 명령 라우터도 stub |
| 보스 정보 | `/보스정보` | 명령 라우터에서 stub 처리 |
| 클리어 기록 | `/클리어` | 명령 라우터에서 stub 처리 |
| 필드보스 상태 | 필드 허브의 실제 상태 | provider가 고정 `RESPAWNING` 반환 |
| 시즌보스 보상 체감 | 등급별 보상, 첫 클리어, 재도전 | 보상 로직이 문서 대비 축약됨 |
| 보스3~최종보스 패턴 | 실제 패턴 다양성 | seed placeholder |
| NPC 관련 기능 | NPC sync/대화 | CANON상 제거 대상이므로 보이지 않아야 맞지만 코드에는 남아 있음 |

---

## 5. 구현 계획

### P0. CANON 충돌 제거

목표: 제거 확정 플러그인과 NPC 기반 진행을 런타임 경로에서 제거한다.

작업:

| 순서 | 작업 | 비고 |
|---|---|---|
| 1 | `plugin.yml`에서 `Citizens`, `BetonQuest` softdepend 제거 계획 확정 | 코드 수정 전 사용자 승인 필요 |
| 2 | `ZenonRPGPlugin`의 `NpcSyncBootstrap` 호출 제거 또는 비활성화 | 부트스트랩 순서 정합 |
| 3 | `npc/citizens` 패키지 처리 방침 결정 | 삭제는 명시 승인 필요 |
| 4 | `npc_spawn_seed.csv` 처리 방침 결정 | 삭제 또는 archive 이동은 명시 승인 필요 |
| 5 | 관련 테스트 제거/대체 방침 결정 | `tests/` 경로는 명시 요청 전 수정 금지 |

완료 기준:

- 서버 기동 시 Citizens/BetonQuest 관련 로그가 나오지 않는다.
- CANON 부트스트랩 순서와 실제 코드 순서가 일치한다.
- NPC 기반 진행이 기능 진입점에 남아 있지 않다.

### P0. 사용자 진입점 복구

목표: 구현되어 있는데도 보이지 않는 기능을 명령어와 허브에 연결한다.

작업:

| 순서 | 작업 | 대상 |
|---|---|---|
| 1 | `/메뉴`를 메인 허브로 연결 | `MainHubListener` 또는 `MainHubGui` |
| 2 | `/영지`를 영지 허브로 연결 | `TerritoryHubGui` |
| 3 | `/상점`을 실제 상점 정책에 맞게 연결 또는 1차 제외 확정 | `ShopGui`, 경제 문서 |
| 4 | `/보스정보`를 보스 정보 GUI로 연결 또는 1차 제외 확정 | `gui_boss_info.md` 기준 |
| 5 | `/클리어`를 클리어 기록 GUI/API로 연결 또는 1차 제외 확정 | `gui_clear_records.md`, boss session DB |

완료 기준:

- `plugin.yml`에 등록된 플레이어 명령이 stub로 끝나는지 목록화된다.
- 1차 포함 기능은 실제 GUI가 열린다.
- 1차 제외 기능은 문서와 사용자 메시지가 일치한다.

### P1. 필드 상태 표시 연결

목표: 필드 허브에서 실제 필드보스 상태가 보이게 한다.

작업:

| 순서 | 작업 | 비고 |
|---|---|---|
| 1 | 필드보스 상태 소유자 결정 | `FieldBossRespawnScheduler` 우선 검토 |
| 2 | `ExploreHubGui.FieldStateProvider` stub 제거 계획 수립 | 현재 `RESPAWNING` 고정 |
| 3 | alive / respawning / available 상태 정의 | GUI 표시와 일치 필요 |
| 4 | 수동 QA 항목 작성 | 보스 처치 후 리스폰 표시 |

완료 기준:

- 필드 허브가 실제 상태를 표시한다.
- 필드보스 처치 후 리스폰 타이머가 갱신된다.

### P1. 시즌보스 보상 정합화

목표: `drop_tables_v1.md` §4, §7.3 기준으로 시즌보스 보상을 구현한다.

작업:

| 순서 | 작업 | 비고 |
|---|---|---|
| 1 | 보상 등급 S/A/B/C 산정 기준 확정 | 현재 코드에는 등급 분기 부족 |
| 2 | 주간 첫 클리어 상태 저장 위치 확정 | DB 또는 Player JSON |
| 3 | 재도전 보상 분리 | 장비의 흔적·칭호·심장 제외 |
| 4 | 1인/2인 인원 배율 적용 | 소모성 재화만 적용 |
| 5 | 고대흔적 지급 구현 | 주간 첫 클리어 한정 |
| 6 | 보상 테이블 하드코딩 축소 | seed/registry 또는 명확한 enum/table로 이동 |

완료 기준:

- 시즌보스별 S/A/B/C 보상이 문서 표와 일치한다.
- 첫 클리어와 재도전 보상이 분리된다.
- 인원 배율이 강화석/큐브 조각에만 적용된다.

### P1. 보스 패턴 seed 완성

목표: placeholder 패턴을 제거하고 문서 기준 패턴 조합으로 채운다.

작업:

| 순서 | 작업 | 기준 |
|---|---|---|
| 1 | `boss_pattern_seed.csv` placeholder 목록 확정 | 보스3~최종보스 |
| 2 | 공용 패턴 P-00~P-09, P-11~P-13 매핑 | `../07_boss_pattern_modules/index.md` |
| 3 | 시즌보스 고유 패턴 반영 | `season_boss_patterns.md` |
| 4 | P-10 미사용 검증 | DL-048 기준 |

완료 기준:

- seed에 placeholder notes가 남지 않는다.
- P-10은 사용되지 않는다.
- 보스별 phase/forced/stagger/berserk 흐름이 문서와 맞는다.

### P2. 문서 정합화

목표: 코드가 앞서간 부분을 문서에 반영해 다음 구현자가 같은 충돌을 반복하지 않게 한다.

작업:

| 순서 | 작업 | 대상 |
|---|---|---|
| 1 | schema v4 반영 여부 결정 | `implementation_design_plan.md`, `decision_log.md` |
| 2 | 이미 완료된 DP 항목 상태 표시 | DP-001~DP-006 |
| 3 | stub 명령어 목록 문서화 | 이 문서 또는 별도 QA 문서 |
| 4 | CANON 충돌 해결 후 decision_log 기록 | DL-NNN |

완료 기준:

- `implementation_design_plan.md`가 현재 코드 상태와 충돌하지 않는다.
- decision_log에 무엇을 왜 바꿨는지 남는다.

---

## 6. 수동 QA 계획

| 순서 | 시나리오 | 기대 결과 |
|---|---|---|
| 1 | 신규 유저 접속 | 클래스 선택 GUI 표시 |
| 2 | 클래스 선택 | 무기 1개 + 방어구 4슬롯 저장 |
| 3 | `/메뉴` | 메인 허브 표시 |
| 4 | `/영지` | 영지 허브 표시 또는 명확한 1차 제외 메시지 |
| 5 | `/필드` | 필드 허브에서 실제 보스 상태 표시 |
| 6 | scoreboard tag 없는 몹 처치 | 보상 없음, 오류 없음 |
| 7 | `poro_field_1` 일반몹 처치 | 필드1 일반몹 보상 |
| 8 | `poro_field_1`, `poro_rank_elite` 정예몹 처치 | 정예 보상과 흔적 확률 적용 |
| 9 | 필드보스 피해 3% 이상 후 처치 | 필드보스 보상 지급 |
| 10 | 시즌보스 첫 클리어 | 등급별 첫 클리어 보상 지급 |
| 11 | 같은 주 시즌보스 재도전 | 재도전 보상만 지급 |
| 12 | 재접속 | wallet, 장비, 영지, 저장고 복원 |

---

## 7. 검증 명령

| 검증 | 명령 |
|---|---|
| 작업 상태 | `git status` |
| diff | `git diff` |
| 공백 오류 | `git diff --check` |
| 컴파일 | `cd custom-plugins/zenon-rpg && bash ./gradlew compileJava` |

---

## 8. 우선순위 요약

| 우선순위 | 작업 | 이유 |
|---|---|---|
| P0 | Citizens/BetonQuest/NPC 부트스트랩 제거 계획 확정 | CANON 정면 충돌 |
| P0 | 명령어 stub 목록 정리 및 실제 GUI 연결 | 실행되지 않거나 보이지 않는 직접 원인 |
| P1 | `/메뉴` 메인 허브 연결 | 모든 기능의 사용자 진입점 |
| P1 | 필드보스 상태 provider stub 제거 | 필드 콘텐츠 체감 문제 |
| P1 | 시즌보스 보상 로직 재설계 | 경제·성장 정합성 핵심 |
| P1 | 보스 패턴 seed placeholder 제거 | 보스 콘텐츠 완성도 |
| P2 | schema v4 및 완료 DP 문서 반영 | 문서-코드 충돌 제거 |

