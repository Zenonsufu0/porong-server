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

이 저장소는 포로 서버 모노레포다. **이 작업 영역(`poromon/`)은 포로몬 모드서버/모드팩만 담당한다.**

### 수정 가능
- `poromon/` 내부 전체 (`modpack/`, `docs/`, `scripts/`, `reports/`, `CLAUDE.md`, `task.md`)

### 읽기만 (수정 금지)
- `poro-rpg/`, `poro-discord/` — 다른 프로젝트. **절대 수정하지 않는다.**
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
- Added Addons: SimpleTMs (TMs/TRs for Cobblemon), Eggs - Cobblemon Addon, Cobblemon: Legendary Monuments
  - All are Cobblemon gameplay addons → required on **both client and server**. Item/Pokémon IDs, structures, and config keys must be verified from the actual jars/registry (do not guess).
  - **Legendary Monuments**: used for legendary-themed structures/encounters, BUT **PoroMonCore's legendary control (tickets + private rooms + controlled world events) takes precedence**. Verify whether its natural structures/summoning bypass that control; if so, disable/gate it (decision 017).
- Development Environment: WSL Ubuntu
- Main Project Path: `/home/zenonsufu1/dev/poro-server-poromon`
- IDE: VS Code with Claude Code
- Custom Mod Path: `custom-mods/poromon-core`

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
- **Legendary Monuments must not bypass this control** (decision 017) — verify and gate if needed.
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
- Eggs - Cobblemon Addon (Diesse)
- Cobblemon: Legendary Monuments (JorgaoMC) — PoroMonCore legendary control takes precedence; verify it doesn't bypass it

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

1. Analyze exported PoroMon 0.1 Dev modpack
2. Separate client-only and server-required mods
3. Prepare server test folder under `server/run`
4. Create Fabric 1.21.1 Gradle project under `custom-mods/poromon-core`
5. Implement minimal `/poromon` command
6. Implement 9th-slot League Pass item
7. Implement basic right-click menu
8. Add config scaffold
9. Add player progress storage scaffold
10. Test with Cobblemon and Mega Showdown installed
