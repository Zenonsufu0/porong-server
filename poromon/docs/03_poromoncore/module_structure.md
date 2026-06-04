# PoroMonCore Module Structure (모듈 구조)

> PoroMonCore = PoroMon 서버의 **규칙 엔진**(Cobblemon=포켓몬 엔진, MSD=배틀 기믹).
> 상위: `poromoncore_spec.md`(역할·스코프·매니저 목록). 본 문서는 **패키지/모듈/의존/초기화 순서**를 고정한다.
> 베이스 패키지: **`kr.poro.poromoncore`** (CLAUDE.md). Java 21 / Fabric 1.21.1.
> ⚠️ 아직 코드 미생성. 본 문서는 구현 전 설계 기준.

## 1. 설계 원칙
- **서버 권위(server-authoritative).** 진행/티켓/룸/보상/해금 등 모든 상태 결정·검증은 **서버측**. 클라는 표시·입력만.
- **모듈 = 매니저 1책임.** 한 클래스에 큰 시스템 몰지 않음(CLAUDE.md). 매니저는 인터페이스 + 구현 분리 지향.
- **밸런스/규칙은 config.** 티켓·룸·보상·짐·해금 값 하드코딩 금지 → `ConfigManager` 경유 (`config_structure.md`).
- **클라/서버 패키지 분리.** 클라 전용 UI 코드는 `client` 패키지 + 별도 entrypoint. 서버 로직과 혼재 금지.
- **중요 행위 로깅.** 티켓 사용·룸 배정·보상 지급·관리자 명령 → `AuditLogManager`.

## 2. 패키지 레이아웃

```
kr.poro.poromoncore
├─ PoroMonCore.java          # ModInitializer (서버/공통 진입점, 매니저 부트스트랩)
├─ PoroMonCoreClient.java    # ClientModInitializer (클라 전용; src/client)  ※ 0.1에선 최소
│
├─ config/      ConfigManager — config 로드/리로드, 기본값, 검증
├─ data/        PlayerProgress 등 영속 데이터 모델 + 저장/로드(PersistentState/JSON)
├─ item/        MenuItemManager — 9번 슬롯 League Pass 아이템 등록/지급/복원/보호
├─ menu/        MenuGuiManager — 우클릭 GUI(화면 구성/네트워크 패킷)
├─ hub/         HubInteractionManager — 허브 텔레포트/상호작용/시설 보호 훅
├─ room/        InstanceRoomManager — 사설 인카운터 룸 배정/정리
├─ encounter/   EncounterTicket/LegendaryEncounter/EncounterPool/LegendaryEventSpawn Manager — 티켓·풀·개인방 소환·필드 이벤트
├─ gym/         GymBadgeManager — 짐 클리어 기록/배지
├─ mega/        MegaUnlockManager — 메가/테라/다이맥스 해금 정책(MSD 연동)
├─ reward/      RewardManager — 보상 지급(아이템/재화/배지) + EconomyBridge
├─ season/      SeasonManager — 시즌/리그/챔피언 기록
├─ command/     AdminCommandManager + /poromon 커맨드 트리
├─ util/        공통 유틸(좌표/텍스트/권한/로깅) + i18n/LocalizationManager(한국어 텍스트)
└─ client/      PoroMonCoreClient 하위 — 화면/렌더/키바인드 (클라 전용)
```

> `data/`는 저장 메커니즘(`database_schema.md`)과 짝. `EconomyBridge`는 `reward/` 내부 또는 별도 `economy/`로 둘 수 있음(초기엔 `reward/`에 동거, 외부 경제 모드 붙으면 분리).

## 3. 모듈 책임 / 분류

| 모듈(매니저) | 패키지 | 측 | 0.1 | 책임 요약 |
|---|---|---|:--:|---|
| ConfigManager | config | 서버 | ✅(스캐폴드) | config 로드/리로드/검증, 기본값 생성 |
| PlayerProgressManager | data | 서버 | ✅(스캐폴드) | 플레이어별 진행 상태 영속화 |
| MenuItemManager | item | 서버(+클라 표시) | ✅ | League Pass 아이템 등록/지급/복원/이동·드롭 방지 |
| MenuGuiManager | menu/client | 클라 UI + 서버 검증 | ✅(기본) | 우클릭 메뉴 GUI, 버튼→서버 액션 |
| HubInteractionManager | hub | 서버 | ✅(텔레포트만) | 허브 텔레포트, 허브 보호/상호작용 훅 |
| EncounterTicketManager | encounter | 서버 | ✅(데이터모델) | 티켓 발급/소모/검증 |
| InstanceRoomManager | room | 서버 | ✅(데이터모델) | 사설 룸 풀 배정/입장/정리 |
| AdminCommandManager | command | 서버 | ✅(reload) | `/poromon` 관리자 하위명령 |
| AuditLogManager | util/log | 서버 | ✅(기본 로깅) | 중요 행위 감사 로그 |
| LegendaryEncounterManager | encounter | 서버 | ❌ 향후 | 개인방 전설 소환/배틀/정리(조우권 사용) |
| EncounterPoolManager | encounter | 서버 | ❌ 향후 | `legendary_pools.yml` 로드 — 등급/필드/컨셉 10종 풀·가중치 제공 |
| LegendaryEventSpawnManager | encounter | 서버 | ❌ 향후 | 2시간 필드 이벤트 스케줄·종 선택·힌트 공지·디스폰·전투 유예 (`legendary_events.yml`) |
| LocalizationManager | util/i18n | 서버 | ⬜ 후보 | 유저 표시 텍스트 한국어 제공(GUI/공지/lore), ko_kr 우선·en fallback |
| MegaUnlockManager | mega | 서버 | ❌ 향후 | 메가/테라/다이맥스 해금 조건·게이트(MSD) |
| GymBadgeManager | gym | 서버 | ❌ 향후 | 짐 클리어 기록/배지 |
| RewardManager | reward | 서버 | ❌ 향후 | 보상 지급 |
| EconomyBridge | reward/economy | 서버 | ❌ 향후 | 재화 연동(내부/외부 경제) |
| SeasonManager | season | 서버 | ❌ 향후 | 시즌/리그/챔피언 |

## 4. 의존 / 초기화 순서

부트스트랩(서버 시작 시 `PoroMonCore.onInitialize`):
```
1. ConfigManager.load()          # 가장 먼저 — 다른 매니저가 설정 참조
2. AuditLogManager.init()        # 로깅 준비
3. PlayerProgressManager.init()  # 저장소 연결(서버 시작/월드 로드 이벤트에 hook)
4. MenuItemManager.register()    # 아이템 레지스트리 등록(레지스트리 freeze 전)
5. EncounterTicketManager / InstanceRoomManager.init()
6. HubInteractionManager.init()  # 텔레포트 타깃 config 참조
7. AdminCommandManager.register()# CommandRegistrationCallback
8. (이벤트 훅) join/respawn → MenuItemManager.restore()
```
의존 방향(상위→하위, 단방향 유지):
- 모든 매니저 → **ConfigManager**, **AuditLogManager**, **util**
- menu → item, hub, (향후) encounter/gym/season 데이터 조회
- encounter(Legendary) → room, reward, data
- gym/mega/season → reward, data
- reward → EconomyBridge, data
- **역참조 금지**(예: config가 menu를 모름). 순환 의존 방지.

## 5. 클라 / 서버 분리 (entrypoints)

`fabric.mod.json`:
```jsonc
"entrypoints": {
  "main":   ["kr.poro.poromoncore.PoroMonCore"],          // 서버+공통
  "client": ["kr.poro.poromoncore.client.PoroMonCoreClient"]
}
```
- **서버에만 존재**: 모든 매니저 로직, 검증, 데이터 저장, 명령 처리.
- **클라에만 존재**: GUI 화면 클래스, 렌더/키바인드 → `client` 패키지. 서버에 없어도 동작해야 함.
- **공통**: 아이템/패킷 ID 정의, 네트워크 페이로드 레코드(서버 송신·클라 수신 양쪽 참조).
- 데디 서버에 `client` 클래스가 로드되지 않도록 `@Environment(CLIENT)` 준수.

## 6. 외부 모드 연동 지점 (compile/runtime)

| 대상 | 모듈 | 용도 | 비고 |
|---|---|---|---|
| Fabric API | 전역 | 이벤트(join/respawn/command), 네트워킹, 레지스트리 | 필수 |
| Cobblemon API | data/encounter/gym | 포켓몬·배틀·스폰 이벤트(`CobblemonEvents`), 종족/폼 | 핵심 연동 |
| Mega Showdown | mega | 메가/테라 상태 훅(공개 API 유무 확인) | 없으면 데이터/이벤트 우회 |
| Accessories | mega/item | 메가링/키스톤 슬롯 연동(필요 시) | depends 확인 |
| (외부 경제 모드) | reward/economy | 재화 — 미정 | EconomyBridge 추상화로 흡수 |

> Cobblemon/MSD 의존은 `build.gradle`에서 `modCompileOnly`/`modImplementation` 또는 로컬 jar — `server_mod_separation.md` 검증 결과와 동기화.

## 7. 0.1 최소 모듈 세트 (스펙 0.1 ↔ 모듈 매핑)

| 0.1 기능(spec) | 담당 모듈 |
|---|---|
| `/poromon` 기본 명령 | command(AdminCommandManager) |
| 9번 슬롯 League Pass 아이템 | item(MenuItemManager) |
| 우클릭 GUI | menu(MenuGuiManager) + client |
| 허브 텔레포트 | hub(HubInteractionManager) |
| 진행 저장 스캐폴드 | data(PlayerProgressManager) |
| 티켓 데이터 모델 | encounter(EncounterTicketManager) |
| 레전드 룸 데이터 모델 | room(InstanceRoomManager) |
| config 로딩 스캐폴드 | config(ConfigManager) |
| 어드민 reload | command |
| 기본 로깅 | util/AuditLogManager |

→ **0.1에선 위 10개만 스캐폴드.** mega/gym/reward/season/legendary-spawn 로직은 인터페이스/데이터모델만 두고 비워둠(향후).

## 8. 관련 문서
- 스펙: `poromoncore_spec.md`
- 설정 파일 구조: `config_structure.md` (TODO) — ConfigManager가 읽을 파일/키
- 데이터 저장: `database_schema.md` (TODO) — PlayerProgress/티켓/룸 영속 포맷
- 명령: `commands.md` (TODO) — `/poromon` 트리
- 서버 분리: `../01_modpack/server_mod_separation.md` / 셋업: `../02_server/server_setup.md`
