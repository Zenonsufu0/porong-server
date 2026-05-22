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
  - Unconfirmed ideas / inbox: `docs/idea_inbox.md`

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

## 작업 루프 명령어

공식 명령어는 `orc` (`scripts/orchestra.sh`)로 통일한다.  
legacy alias (`aistatus`, `toreview`, `tomaster`)는 호환용으로 남아 있지만 공식 문서에서는 `orc` 사용을 권장한다.

| 명령어 | 역할 |
|---|---|
| `orc status` | 양쪽 worktree 상태 확인 |
| `orc diff-main` | main worktree diff stat (staged + unstaged) |
| `orc handoff-main "요약"` | main 변경사항을 `add -A` 후 commit하고 `orc to-review` 실행 |
| `orc to-review` | main/master의 커밋된 변경사항을 review/codex-review로 동기화하고 Codex 리뷰 준비 |

`orc to-review`는 양쪽 worktree가 dirty 상태이면 실행을 막는 안전 명령이다.
`orc handoff-main`은 모든 변경사항을 `add -A` 하므로 작업이 섞였으면 먼저 `orc diff-main`으로 확인한다.

commit · merge · push는 사용자 승인 후 실행한다. `orc handoff-main`은 제안만 하고 자동 실행하지 않는다.

---

## 아이디어 문서 반영 프로토콜

사용자가 작업 중 새 아이디어·설정 변경·기획 방향·구현 방향을 말하면 채팅에만 남기지 않는다.  
반드시 아래 기준으로 분류하고 해당 문서에 반영하거나, 반영하지 않는다면 이유를 보고한다.

| 분류 | 조건 | 반영 위치 |
|---|---|---|
| CANON 반영 | 확정된 공식 기준 변경 | 관련 `docs/NN_*/CANON.md` + `decision_log.md` |
| 결정 기록 | 설계 결정 (이유가 있는 확정) | `docs/decision_log.md` (DL-NNN 형식) |
| DRAFT 보관 | 아직 미확정·검토 필요 | `docs/idea_inbox.md` |
| 폐기 기록 | 대체·폐기된 내용 | `docs/decision_log.md` 또는 `docs/_archive/README.md`에 이유 기록 |
| 미반영 | 위 어디에도 해당 안 됨 | 보고 시 "미반영 이유" 명시 |

`docs/idea_inbox.md` 운용 규칙:
- 아이디어 확정 시: 관련 CANON.md에 반영 + decision_log에 DL-NNN 기록 + inbox 항목에 `[PROMOTED → DL-NNN]` 표시
- 폐기 시: 항목에 `[폐기 — 이유]` 표시
- 비워두지 않는다: 불확실하면 inbox에라도 기록한다

---

## 작업 완료 보고 형식

모든 작업 완료 후 반드시 아래 형식으로 보고한다.  
항목이 해당 없으면 "해당 없음"으로 명시한다. 생략하지 않는다.

```
[작업 완료 보고]
- 변경 요약:
- 수정/생성한 파일:
- 반영한 사용자 아이디어:
- 문서 반영 상태:
  - CANON 반영:
  - DRAFT/보류 (idea_inbox):
  - decision_log 기록:
  - 미반영 (이유):
- 검증:
  - orc status:
  - orc diff-main:
  - git diff --check:
- 남은 위험/미확정:
- 다음 단계:
  - 맞으면 실행할 명령어: orc handoff-main "요약"
  - 아니면 추가 지시할 내용:
```

commit · merge · push는 자동으로 하지 않는다. 다음 단계 명령어는 제안만 한다.
