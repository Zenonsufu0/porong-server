# CLAUDE.md

## Project Name

PoroMon / 포로몬

## Project Summary

PoroMon is a Cobblemon-based Minecraft mod server project.

The server direction is:

- Free survival-style Pokémon exploration
- Player housing, farming, mining, trading, and natural village usage
- Central hub for gym battles, badges, legendary encounters, Mega/Tera unlocks, league systems, and seasonal endgame content
- Cobblemon provides the Pokémon engine
- Mega Showdown provides Mega Evolution and related battle gimmicks
- PoroMonCore controls server-specific progression, rewards, legendary encounter rooms, unlock rules, and menu systems

This is not the main Poro RPG server.  
It is a separate modded server line under the Poro brand.

---

## 작업 영역 / 워크스페이스 규칙 (모노레포)

이 저장소는 Zenon 서버 모노레포다. **이 작업 영역(`zenon-mon/`)은 포로몬 모드서버/모드팩만 담당한다.**

### 수정 가능
- `zenon-mon/` 내부 전체 (`modpack/`, `docs/`, `scripts/`, `reports/`, `CLAUDE.md`, `task.md`)

### 읽기만 (수정 금지)
- `zenon-rpg/`, `zenon-discord/` — 다른 프로젝트. **절대 수정하지 않는다.**
- 루트 `.gitignore`, 루트 `README.md`, 루트 `CLAUDE.md`, `infra/`, 공통 docs 구조

### 런타임 파일 커밋 금지
다음은 Git에 올리지 않는다 (루트 `.gitignore`가 커버하지만 항상 확인):
- 모드 jar / 서버 jar (`**/*.jar`, 단 `gradle/wrapper/gradle-wrapper.jar`는 예외)
- `server/`, `world/`, `world_nether/`, `world_the_end/`
- `logs/`, `crash-reports/`, `cache/`, `libraries/`, `versions/`, `backups/`

> ⚠️ `modpack/overrides/config`·`datapack`은 **실제 모드팩 구성 파일**이다.
> 경로/이름에 `world` 같은 문자열이 들어가도 런타임 산출물로 오인해 삭제하지 않는다.

### 관리 중심
모드팩 `overrides/`·`config/`, `scripts/`, `docs/`, `reports/` 중심으로 관리한다.
RPG Paper 플러그인 구조에 억지로 맞추지 않고, `poro-common` 같은 공통 모듈도 억지로 만들지 않는다.

### 커밋 전 필수 검사
```
git status --short
git diff --stat
git diff --check
git ls-files | grep -E '\.jar$' | grep -v 'gradle/wrapper/gradle-wrapper.jar'
git ls-files | grep -E '(^|/)(logs|crash-reports|world|world_nether|world_the_end|backups|cache|libraries|versions)(/|$)|(^|/)server\.jar$'
```
금지 파일이 추적 중이면 바로 삭제하지 말고 먼저 보고한다.

### 임의 수정 금지 (요청 시에만)
모드 목록 / 조우권 데이터 / 포로몬 밸런스(종족값·타입·기술) / 서버 설정값. RPG·디스코드봇 코드.

---

## Current Technical Baseline

- Minecraft: 1.21.1
- Mod Loader: Fabric
- Java: JDK 21
- Modpack Source: Cobblemon Official Modpack [Fabric] 1.7.3
- Added Core Addon: Cobblemon: Mega Showdown
- Added Addons: SimpleTMs (TMs/TRs for Cobblemon), Cobblemon: Legendary Monuments
  - (~~Eggs - Cobblemon Addon~~ 제거 — 결정 032: 조우권과 중복 + 리소스팩 의존. 알 시스템 폐기.)
  - All are Cobblemon gameplay addons → required on **both client and server**. Item/Pokémon IDs, structures, and config keys must be verified from the actual jars/registry (do not guess).
  - **Legendary Monuments**: jar 감사 결과 자체 소환(아이템/구조물/바이옴)이 PoroMonCore 통제를 우회 확정 → **결정 023: 완전 비활성**(worldgen + loot_table datapack 오버라이드로 비활성, 소환 아이템 자연 획득 차단). 전설은 조우권/사설룸으로만. 실제 비활성 datapack은 서버 mods 배치 후 jar worldgen 경로 확인 필요(TODO).
- Development Environment: WSL Ubuntu
- Repo Layout: Zenon 서버 모노레포. 포로몬 작업 영역은 모노레포 루트 하위 `zenon-mon/`
  - 포로몬 전용 worktree: `/home/zenonsufu1/dev/zenon-work-mon/zenon-mon` (브랜치 `feature/poromon-dev`)
  - 모노레포 루트(master worktree): `/home/zenonsufu1/dev/zenon-server`
  - ⚠️ 옛 경로 `/home/zenonsufu1/dev/poro-server-poromon`·`porong-work-mon`·`porong-server`는 모노레포/브랜드 이전 표기 — 사용 금지
- IDE: VS Code with Claude Code
- Custom Mod Path: `zenon-mon/custom-mods/poromon-core` (포로몬 작업 영역 기준 `custom-mods/poromon-core`)

---

## Important Project Rule

Do not assume this is a Paper plugin server.

This project is a Fabric modded Minecraft server.

Use the following mental model:

- Paper server = plugins
- PoroMon server = Fabric mods, server-side mods, configs, datapacks, and custom Fabric mod development

Do not design core systems as Bukkit/Paper plugins unless explicitly requested.

---

## Core Philosophy

General gameplay should remain free and natural.

Players should be able to:

- Explore freely
- Catch Pokémon naturally
- Build houses
- Use naturally spawned villages
- Mine, farm, trade, and play casually

PoroMonCore should only control high-impact progression and server-lifetime systems:

- Legendary Pokémon encounters
- Mega Evolution unlocks
- Tera/Dynamax/Z-Move policies if enabled
- Gym clear records
- Badges
- League and Champion systems
- Seasonal progression
- Hub menus
- Encounter tickets
- Personal legendary rooms

Avoid over-controlling basic gameplay.

---

## PoroMonCore Role

PoroMonCore is the central rules engine of the server.

Cobblemon is the Pokémon engine.  
Mega Showdown is the battle gimmick expansion.  
PoroMonCore is responsible for PoroMon-specific progression and server rules.

PoroMonCore should eventually include:

- MenuItemManager
- MenuGuiManager
- PlayerProgressManager
- EconomyBridge
- HubInteractionManager
- InstanceRoomManager
- EncounterTicketManager
- LegendaryEncounterManager
- MegaUnlockManager
- GymBadgeManager
- RewardManager
- SeasonManager
- AdminCommandManager
- ConfigManager
- AuditLogManager

---

## Initial PoroMonCore 0.1 Scope

The first implementation should be small and stable.

Target features:

1. `/poromon` base command
2. 9th hotbar slot menu item, tentatively called League Pass
3. Right-click GUI menu
4. Hub teleport button
5. Player progress storage scaffold
6. Encounter ticket data model
7. Legendary room data model
8. Config loading scaffold
9. Admin reload command
10. Basic logging

Do not implement all endgame systems at once.

---

## Menu Item Design

> 상세 정책·GUI 레이아웃: `docs/03_poromoncore/menu_design.md`.

The player should receive a fixed menu item in hotbar slot 9.

Suggested item name:

- 리그 패스
- PoroMon League Pass

Behavior:

- Right-click opens PoroMon GUI
- Should not be dropped
- Should not be moved if locked mode is enabled
- Should be restored on join/respawn if missing
- Should have a command alternative: `/poromon menu`

GUI should eventually include:

- Hub teleport
- My progress
- Badges
- Encounter tickets
- Legendary altar info
- Mega/Tera unlock status
- Gym guide
- League/Champion info
- Server guide

### 상점 접근 = 하이브리드 (결정 024)

골드는 PoroMonCore 내부 잔액(EconomyBridge)이라 바닐라 주민 거래(에메랄드) 불가 → 모든 거래는 서버 검증 GUI 트랜잭션. 접근만 둘로 나눈다:

- **9번 슬롯 메뉴(리그 패스) GUI — 어디서나**: 단순 매입(광물·농작물·베리 → 골드) + 일반 편의(볼·회복약). 위 진행도·배지 조회·허브 텔레포트와 같은 메뉴.
- **허브 NPC/판매대 우클릭**: 핵심 통제 상점(성장·실전 육성·기술머신·알·메가 연구소·전설 제단). 배지·관장 클리어 게이트 판정은 허브에서 → 통제·해금 상품은 허브 회귀 유도.

상세: `docs/04_game_design/economy_design.md §5`, `shop_design.md §5`, `hub_design.md §2`.

---

## Legendary Encounter Design

Legendary Pokémon should not be freely random-spawned in the wild.

Preferred structure:

1. Player obtains encounter ticket through in-game currency, gym rewards, quests, or events
2. Player uses hub altar
3. PoroMonCore assigns an empty private encounter room
4. Player enters the room alone
5. Ticket is consumed
6. Weighted random table selects the Pokémon
7. Pokémon spawns in the room
8. Player battles/catches it
9. Result is logged
10. Room is cleaned up

Important:

- Do not allow other players to steal the legendary encounter
- Do not spawn legendaries freely in public hub spaces
- Weighted random tables should be configurable
- Rayquaza should be treated as a special late-game or season-end encounter

### Encounter Tier Structure (latest)

- **General tickets (5):** 희귀 / 하급 전설 / 중급 전설 / 상급 전설 / 최상급 전설 (decision 018).
- **Price order:** 희귀 < 하급 < 중급 < 상급 < **concept tickets** < 최상급 (decision 022).
- **희귀 (Rare):** non-legendary high-value Pokémon (600-line + popular). No legendaries/mythicals. Evolution-stage weight 기본70/중간20/최종10 (decision 021).
- **하급·중급:** ticket (private room) **+ PoroMonCore 2-hour field event** (combined low+mid pool 70/30, hidden coords + chat hint, despawn timer, battle grace, one at a time) (decision 019).
- **상급:** ticket / private room only (no field event).
- **최상급 (Apex):** apex-pool ONLY (no mid/advanced) — "pick one core legendary", highest price.
- **Concept tickets (10):** 하늘·심해·대지·시간·공간·반전·빛·용왕·수호자·영원 — mixed 중급/상급/최상급 (weight 55/35/10). **Any apex line in a concept pool MUST also be in the apex pool** (decision 020/022).
- **레쿠쟈 = 하늘 조우권 중심**; **아르세우스 = 영원 조우권 중심** (default enabled:false; lock/TODO if not in modpack).
- No legendary eggs. User-facing names/notices in Korean.
- **Legendary Monuments는 완전 비활성** (decision 023) — worldgen/loot_table datapack 오버라이드로 소환 경로 차단. 전설은 PoroMonCore 통제만.
- Do not guess species IDs — mark `TODO`. Pools/policy: `docs/04_game_design/encounter_pool_design.md`.

---

## Mega / Tera Policy

Mega Evolution is allowed as a feature, but should be progression-gated.

Mega Showdown may add multiple mechanics such as Mega Evolution, Z-Moves, Tera, Dynamax, Ultra Burst, and fusion-related features.

PoroMonCore should control unlock rules and server policy.

Initial policy:

- Mega Evolution: planned
- Charizardite X/Y: late-game unlock
- Rayquaza/Mega Rayquaza: special endgame content
- Tera/Dynamax/Z-Moves: not automatically free; decide and gate later
- PvP usage should be separately controlled by league rules

---

## Shop / Economy Policy

Initial (0.1) economy uses a **single currency: Gold**.

- Battle Points, Mega Crystals (메가 결정), and Legendary Shards (전설 조각) are **NOT** 0.1 currencies — they are future-expansion candidates only.
- High-value items are controlled by: high gold price + badge requirement + gym-clear requirement, and event/late-game restriction.
- **Weekly purchase limits are NOT used by default** in 0.1; price + badge conditions do the gating.
- Only Legendary / Rayquaza / Mega Rayquaza / season-exclusive items are strongly restricted (not sold normally).
- Charizardite X/Y belong to the **Mega Stone category** (not a separate category), priced as high-grade mega stones.
- Legendary encounter tickets are **PoroMonCore custom items**, not base-mod items; they open a private encounter room rather than granting a Pokémon directly.
- Shop categories: 일반 / 성장 / 실전 육성 / 기술머신(SimpleTMs) / 알(Eggs) / 메가 연구소 / 전설 제단.
- Legendary encounter ticket tiers and Pokémon pools: see `encounter_pool_design.md`. Rayquaza = Sky Rift / late-game only. Legendary natural spawn is off (decision 013).
- Egg tiers and pools: see `egg_pool_design.md`. **No legendary eggs.** Shiny eggs are event/late-game only (not normal sale). Whether the Eggs Addon allows custom hatch pools is **unverified** — confirm from jar/datapack before committing.
- Do not guess item IDs / namespaces / translation keys. Mark unverified as `TODO`.

See `docs/04_game_design/shop_design.md`, `shop_catalog_0.1.md`, `encounter_pool_design.md`, `egg_pool_design.md`.

## Localization Policy

PoroMon is a Korean-first server. **User-facing text must be in Korean as much as possible** — shop names, item names/descriptions (lore), shop GUI, command feedback.

- PoroMonCore custom items (e.g., encounter tickets): localize via `ko_kr.json` + Korean lore (owned directly).
- Mod items (SimpleTMs / Eggs / Mega Showdown / Cobblemon): localize via a bundled Korean resource pack (`ko_kr.json` override). Verify each mod's namespace/translation key from the jar; mark unknown as `TODO`.
- Keep internal IDs/keys in English; translate only display text. en_us as fallback.

See `docs/05_operations/localization_policy.md`.

## Gym / Badge Design

Natural villages can remain usable.

Gyms and badges should be centralized in the hub.

The hub should contain:

- Gym area
- Badge reward NPC or GUI
- Battle tower
- League arena
- Champion hall
- Legendary altar
- Mega/Tera research area
- Market and sell shops

Gym clear records should be stored by PoroMonCore.

Gym rewards can unlock:

- Badges
- Gold
- Battle points
- Mega/Tera progression
- Encounter ticket purchase rights
- League participation rights

---

## Modpack Policy

The modpack is based on Cobblemon Official Modpack [Fabric] 1.7.3.

Added:

- Cobblemon: Mega Showdown
- SimpleTMs: TMs and TRs for Cobblemon (dragomordor)
- (Eggs - Cobblemon Addon 제거됨 — 결정 032)
- Cobblemon: Legendary Monuments (JorgaoMC) — 자체 소환이 통제 우회 확정 → **완전 비활성**(decision 023): worldgen/loot_table datapack 오버라이드로 차단, 전설은 조우권/사설룸만

The modpack should be distributed as a single official PoroMon pack.

Users should not be asked to install mods manually.

Supported installation path should be simple:

1. Install CurseForge or Modrinth App
2. Install PoroMon Official Pack
3. Press Play
4. Join the server

Manual mods-folder support should not be the default user guide.

---

## Server / Client Separation

Be careful when separating server mods from client mods.

Some mods are client-only and must not be placed into the server run mods folder.

Before building the server pack, analyze:

- `modpack/base/manifest/manifest.json`
- `modpack/base/mods-list/modlist.html`
- `modpack/overrides/`

Identify:

- Client-only mods
- Server-required mods
- Common mods
- Optional visual mods
- Mods that need both client and server

Do not blindly copy every client mod into `server/run/mods`.

---

## Documentation Rules

Keep docs updated when decisions change.

Primary docs:

- `docs/00_project/overview.md`
- `docs/00_project/decisions.md`
- `docs/01_modpack/modpack_list.md`
- `docs/01_modpack/server_mod_separation.md`
- `docs/03_poromoncore/poromoncore_spec.md`
- `docs/04_game_design/hub_design.md`
- `docs/04_game_design/legendary_encounter.md`
- `docs/04_game_design/mega_tera_unlock.md`

When implementing a feature, update the related document first or immediately after implementation.

---

## Coding Guidelines

- Use Java
- Target Java 21
- Use Fabric 1.21.1
- Keep systems modular
- Avoid hardcoding balance values
- Use config files for tickets, rooms, rewards, gyms, and unlocks
- Prefer server-side validation
- Log important actions such as legendary ticket use, room assignment, reward grants, and admin commands
- Do not implement large systems in one class
- Do not mix client-only UI code with server logic without clear package separation

Suggested base package:

`kr.poro.poromoncore`

Suggested package structure:

- `config`
- `data`
- `item`
- `menu`
- `hub`
- `room`
- `encounter`
- `gym`
- `mega`
- `reward`
- `season`
- `command`
- `util`
- `client`

---

## Do Not Do

Do not:

- Modify existing Pokémon types or base stats unless explicitly requested
- Add many custom Pokémon at the beginning
- Force all gameplay through the hub
- Let legendary Pokémon be freely stolen by other players
- Make Rayquaza a normal random spawn
- Add too many heavy mods before server testing
- Treat this like a Bukkit plugin project
- Copy every client mod into the server folder blindly
- Implement the entire league/season system before the basic core works

---

## Current Next Steps

> 세션 간 상세 핸드오프는 `task.md`가 단일 기준이다. 이 목록은 큰 그림 요약.

**현재 단계: Phase 0(설계/문서) 거의 완료 → Phase 1(서버 최소구성 기동) 직전. 구현 코드 0줄.**

완료 (✔):
- ✔ PoroMon 0.1 Dev 모드팩 분석 + 실제 jar 80개 감사 (`docs/01_modpack/jar_feature_audit.md`)
- ✔ 클라/서버 모드 분리 (서버 화이트리스트 19 / 애매 5 / 클라 제외 56)
- ✔ 게임 설계 확정 (decisions 011~022), 조우권·상점·짐·리그 체계
- ✔ 문서 체계 한글화 + 인덱스 (`docs/README.md`)

다음 (Phase 1):
1. **[BLOCKER] Legendary Monuments 처리 방향 결정** — 자체 전설 소환이 PoroMonCore 통제를
   우회함(jar 확인). 비활성 vs 통제된 후반 콘텐츠 수용. (task.md §3)
2. chipped / cobblefurnies / terrablender 서버 로드 검증 (LM 하드 의존, 별도 jar 없음)
3. `server/run` 기동 파일 작성 (`eula.txt`, `server.properties` pvp=false, `start.sh` Java21)
4. `DRY_RUN=0 ./scripts/sync-server-mods.sh` 로 서버 mods 19개 복사 (§3 결정 후)
5. 서버 1차 기동 → Cobblemon/MSD/SimpleTMs/Eggs/LM 로드·의존성 경고 확인 → 클라 접속 테스트

이후 (Phase 2+):
6. `custom-mods/poromon-core` Fabric 1.21.1 Gradle 골격
7. `/poromon` 명령 → 9번 슬롯 리그 패스 아이템 → 우클릭 메뉴 → config·진행도 저장 스캐폴드
