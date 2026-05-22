# Poro Server global rules

- Default response language is Korean.
- This repository is for the Poro Server 45-day seasonal project server.
- **Always read `docs/final_master_plan.md` first.** This is the project-level source of truth. Domain-level details live in each `CANON.md`; on conflict, `final_master_plan.md` wins.
- For deeper reference on specific areas:
  - Plugin/system work: `docs/01_plugin_architecture/CANON.md`
  - DB/API/statistics: `docs/02_database_api_stats/CANON.md`
  - Discord/onboarding: `docs/03_discord_onboarding_bot/index.md`
  - Combat/skills: `docs/04_combat_weapon_skills/CANON.md`
  - Island/farm: `docs/05_island_farm_system/CANON.md`
  - Fields/bosses/drops: `docs/06_fields_bosses/CANON.md`
  - Boss patterns: `docs/07_boss_pattern_modules/index.md`
  - Resource pack/assets: `docs/08_resourcepack_pipeline/index.md`
  - Terms/policy: `docs/09_terms_and_policy/index.md`
  - Roadmap/issues: `docs/10_development_roadmap/index.md`
  - Archived rebuild/audit docs: `docs/_archive/README.md`
  - Map design: `docs/12_map_design/`

## Safety and scope

- Do not modify unrelated project areas.
- Do not touch `server/`, `security/`, `ops/`, `tests/`, `scripts/`, `custom-plugins/`, `.github/` unless the user explicitly asks for implementation, server configuration, tests, scripts, plugin code, or CI work.
- When asked for planning or documentation, edit only `docs/` unless explicitly told otherwise.
- When asked for plugin implementation, prefer `custom-plugins/EmpireRPG` or the existing EmpireRPG plugin path.
- When asked for live server plugin files, only edit `server/plugins` or `server-config` if explicitly requested.
- Never commit secrets, tokens, IPs, passwords, Discord bot tokens, API keys, or database credentials.

## Final design source of truth

- 1st season is a 45-day seasonal project server, not a long-term permanent server.
- Opening model: Discord-gated official opening, no public test.
- Core plugin: EmpireRPG owns combat, equipment, island/farm, boss rewards, DB, API, Discord/web integration data. (도감/컬렉션은 1차 시즌 제외)
- MythicMobs is for vanilla-based mobs/boss shells and simple visual skills.
- IridiumSkyblock is only the personal island shell.
- 1st season bosses are vanilla-enhanced; ModelEngine/BetterModel/FMM are deferred to a later expansion.
- Common engravings are removed from 1st season.
- Player skill design excludes persistent player AoE zones, target marks, defense reduction, and received-damage-increase debuffs.
- Island/farm uses island storage only. Magic power system is removed. Island facilities: 약초 재배지 + 광물 채굴기 + 공방 가공기 (confirmed 2026-05-19). No hopper/cable logistics.

## Assets

- Asset work lives under `assets/source`.
- Export outputs belong under `assets/export/resourcepack`.
- For Blockbench work, also follow:
  - `assets/source/CLAUDE.md`

## Response/output expectations

- Be direct and practical.
- Before changing files, summarize the intended files to change.
- After changing files, summarize changed files and what was changed.
- Prefer small, reviewable changes over broad rewrites.
