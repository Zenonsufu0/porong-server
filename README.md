# Zenon Server

> **Zenon Server**는 **Zenon Project**의 마인크래프트 서버 모노레포로, 서로 독립적인 여러 게임 서버와 운영 도구를 한 레포에서 관리하는 멀티-서버 프로젝트다.
> 각 서버는 자기만의 컨셉·플랫폼·플레이루프를 가지며, 코드는 거의 공유하지 않는다.

> 🤍 **YUKI-01 / 유키** — Zenon Project의 공식 마스코트이자 Discord 비서봇 캐릭터(흰색+하늘색+눈송이 테마의 SD 메이드 안드로이드 비서). 운영 봇(`porong-discord`)의 페르소나로 사용한다.

GitHub: [`Zenonsufu0/zenon-server`](https://github.com/Zenonsufu0/zenon-server)

---

## 🎯 프로젝트 방향

- **여러 컨셉의 서버를 한 브랜드(Zenon Project)로** 묶어, 각각 독립적으로 설계·개발·운영한다.
- 서버 간에는 **억지 공통 모듈을 만들지 않는다.** 공유는 운영 도구(디스코드 봇)와 문서 규약 수준.
- 모든 설계 결정은 문서로 남긴다(SoT·CANON·decision_log). 채팅에만 남기지 않는다.
- 현재는 **RPG가 주력 개발**, `porong-mon`/Discord 봇이 병행, 나머지는 **구상 단계** 후보 서버다.

---

## 🧩 서버 / 모듈별 컨셉

### ✅ 활성 개발

| 프로젝트 | 폴더 | 컨셉 |
|---|---|---|
| **porong-rpg** | [`porong-rpg/`](porong-rpg/) | **오픈월드 RPG.** 액션 전투, 직업, 월드보스, 던전, 퀘스트 중심. 자체 Paper 플러그인 PoroRPG가 전투·성장·영지·필드/보스를 소유. |
| **porong-mon** | [`porong-mon/`](porong-mon/) | **Cobblemon 기반 수집/육성 서버.** 포켓몬 수집·육성, 관장(짐) 도전, 배틀타워, 전설 조우권 중심. Fabric 모드팩 + 자체 모드(PoroMonCore). |
| **porong-discord** | [`porong-discord/`](porong-discord/) | **중앙제어 Discord 봇.** 전체 Zenon 서버 운영을 보조 — 온보딩·인증 게이트·역할·알림·운영자 패널. 봇 페르소나는 공식 마스코트 **YUKI-01 / 유키**. (Python, discord.py) |

### 🧪 구상 단계 (개발 미착수 — 문서만)

| 프로젝트 | 폴더 | 컨셉 |
|---|---|---|
| **porong-economy** | [`porong-economy/`](porong-economy/) | **경제 특화 후보 서버.** 유저 간 거래·시장·생산·경제 밸런스가 핵심 루프. 플랫폼·규모 미확정. |
| **porong-gun** | [`porong-gun/`](porong-gun/) | **총기/전술/생존 후보 서버.** 후보 방향: 타르코프식 익스트랙션 · 배그식 배틀로얄 · 좀비 서바이벌 · 총기 PvE/PvP. **세부 컨셉 미확정.** |

> 구상 단계 프로젝트는 폴더 안에 **문서만**(README·`docs/concept.md`·`docs/idea_inbox.md`) 존재한다.
> 서버 런타임·빌드 산출물·플랫폼 템플릿(Gradle/Fabric/Paper)은 아직 없다. 착수 결정 시 worktree를 만들고 설계를 확장한다.

---

## 🗂️ 개발 단계 구분

| 단계 | 프로젝트 | 의미 |
|---|---|---|
| 활성 개발 | porong-rpg, porong-mon, porong-discord | feature 브랜치 + 전용 worktree에서 실제 개발/실행 중 |
| 구상 | porong-economy, porong-gun | 컨셉/아이디어 수집 단계. 코드·런타임 없음, 문서만 추적 |

---

## 🧭 전역 규칙 (요약)

- **설계 단일 진실 공급원(SoT):** [`porong-rpg/docs/final_master_plan.md`](porong-rpg/docs/final_master_plan.md). 충돌 시 최우선. 도메인 세부는 각 프로젝트 docs의 `CANON.md`.
- **결정 기록:** 설계/방향 결정은 [`porong-rpg/docs/decision_log.md`](porong-rpg/docs/decision_log.md)(DL-NNN), 미확정은 각 프로젝트 `docs/idea_inbox.md`.
- **비밀정보:** 토큰·키·DB 자격증명은 절대 커밋 금지(`.gitignore` 차단).
- **런타임 비추적(gitignored):** `*/.local/server`·world·logs·설치된 jar·빌드 산출물·`.env`. 제3자 플러그인/모드 jar는 각 배포처에서 받아 설치한다.
- **commit · merge · push:** 사용자 승인 후 실행.
- 자세한 규칙: 루트 [`CLAUDE.md`](CLAUDE.md) · [`AGENTS.md`](AGENTS.md) + 각 프로젝트 `CLAUDE.md`.

---

## 🌳 Worktree 운영

> 합본 저장소(이 폴더, `zenon-server`)는 **merge/기록/전체 구조 확인용**이며, 여기서는 서버를 실행하지 않는다.
> 프로젝트별 개발/실행은 각 worktree에서 자기 폴더만 sparse-checkout해서 한다. 전체 기준은 [`docs/worktree_policy.md`](docs/worktree_policy.md).

| worktree 디렉토리 | 브랜치 | sparse-checkout | 상태 |
|---|---|---|---|
| `zenon-server` (이 폴더) | `master` | (전체) | 합본 · merge · 기록 |
| `zenon-work-rpg` | `feature/rpg-dev` | `porong-rpg` | 활성 |
| `zenon-work-mon` | `feature/poromon-dev` | `porong-mon` | 활성 |
| `zenon-work-discord` | `feature/discord-dev` | `porong-discord` `porong-rpg/docs` `porong-mon/docs` | 활성 |
| `zenon-work-economy` *(예정)* | `feature/economy-dev` | `porong-economy` | 구상 — 착수 시 생성 |
| `zenon-work-gun` *(예정)* | `feature/gun-dev` | `porong-gun` | 구상 — 착수 시 생성 |

- **in-repo 폴더 이름(`porong-rpg/` 등)은 이번 1차 브랜드 전환에서 그대로 유지한다.** SoT 경로·문서·`.gitignore`가 참조하므로 함부로 rename하지 않는다. worktree **디렉토리** 이름(`zenon-work-*`)은 별개 레이어다.
- 새 서버/하위 프로젝트는 새 폴더 + 새 worktree + 해당 폴더만 sparse-checkout. 절차는 정책 문서 §6 참고.

---

## 📜 라이선스

**사유 라이선스 (Proprietary, All Rights Reserved)** — 전문은 [`LICENSE`](LICENSE) 참조.

- **재배포·수정·파생물 작성 금지** (저작권자 사전 서면 허가 시 예외). 열람·개인 참고만 허용.
- 제3자 구성요소(Paper/MythicMobs/IridiumSkyblock/Forge/Fabric/Cobblemon/discord.py 등)와 외부 에셋은 각 원저작자 라이선스를 따른다(외부 에셋은 저장소 비포함).
- 본 프로젝트는 비공식 팬/시즌 서버이며 Mojang/Microsoft와 무관하다.
