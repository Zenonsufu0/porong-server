# Poro Server (포로 서버) — 모노레포

> 포로 서버 프로젝트의 통합 저장소. **서로 거의 독립적인 3개 프로젝트**를 한 레포에서 관리한다.

---

## 📦 프로젝트

| 프로젝트 | 폴더 | 한 줄 설명 | 상세 |
|---|---|---|---|
| **poro-rpg** | [`poro-rpg/`](poro-rpg/) | 디스코드 유입 제한형, 45일 시즌제 **Paper 플러그인 RPG 서버**. 6무기 전투·장비 성장·개인 영지·필드/보스를 자체 플러그인 PoroRPG가 소유. | [`poro-rpg/README.md`](poro-rpg/README.md) |
| **poromon** | [`poromon/`](poromon/) | **모드 서버/모드팩** 관리 프로젝트 (Forge/Fabric 계열 모드 + 모드팩 + 서버 운영). | [`poromon/`](poromon/) |
| **poro-discord** | [`poro-discord/`](poro-discord/) | 디스코드 **온보딩/운영 봇** (Python, discord.py). 인증 게이트·역할·알림. | [`poro-discord/README.md`](poro-discord/README.md) |

> 두 게임 프로젝트(poro-rpg, poromon)는 코드를 공유하지 않는다. 공통 모듈을 억지로 만들지 않는다.

---

## 📂 저장소 구조 (최상위)

| 경로 | 설명 |
|---|---|
| `poro-rpg/` | RPG 서버 + **프로젝트 전역 문서**: `custom-plugins/`(PoroRPG 플러그인) · `server-config/`(MythicMobs YAML) · `tools/` · `scripts/` · `env/` · `assets/`·`server/`(gitignored 런타임) · `docs/`(RPG 설계 도메인 + `final_master_plan.md` SoT · `decision_log.md` · `idea_inbox.md` · `_archive/`) |
| `poromon/` | 모드 서버: `custom-mods/` · `modpack/` · `server/` · `docs/`(자체 번호 체계) · `scripts/` · `reports/` |
| `poro-discord/` | 디스코드 봇: `cogs/` · `api_client.py` · `main.py` · `docs/` · `.env.example` |
| `scripts/` | 레포 전역 보조 스크립트 (서버 기동/배포 스텁 등). |
| `CLAUDE.md` / `AGENTS.md` | 에이전트/기여자용 전역 작업 규칙. 프로젝트별 규칙은 각 폴더의 `CLAUDE.md`. |

> **저장소에 없는 것 (의도적, gitignored):** 런타임 서버 데이터(`*/server/`, world·logs·설치된 jar), 리소스팩/텍스처 원본(`poro-rpg/assets/`), 빌드 산출물, 비밀값(`.env`). 제3자 플러그인/모드 jar는 각 배포처에서 받아 설치한다.

---

## 🧭 전역 규칙 (요약)

- **설계 단일 진실 공급원(SoT):** [`poro-rpg/docs/final_master_plan.md`](poro-rpg/docs/final_master_plan.md). 충돌 시 최우선. 도메인 세부는 각 프로젝트 docs의 `CANON.md`.
- **결정 기록:** 설계/방향 결정은 채팅에 남기지 않고 [`poro-rpg/docs/decision_log.md`](poro-rpg/docs/decision_log.md)(DL-NNN) 또는 [`poro-rpg/docs/idea_inbox.md`](poro-rpg/docs/idea_inbox.md)에 반영한다.
- **비밀정보:** 토큰·키·DB 자격증명은 절대 커밋 금지(`.gitignore` 차단). 노출 시 즉시 재발급.
- **commit · merge · push:** 사용자 승인 후 실행.
- 자세한 규칙: 루트 [`CLAUDE.md`](CLAUDE.md) + 각 프로젝트 `CLAUDE.md`.

---

## 📜 라이선스

**사유 라이선스 (Proprietary, All Rights Reserved)** — 전문은 [`LICENSE`](LICENSE) 참조.

- **재배포·수정·파생물 작성 금지** (저작권자 사전 서면 허가 시 예외). 열람·개인 참고만 허용.
- 제3자 구성요소(Paper/MythicMobs/IridiumSkyblock/Forge/Fabric/discord.py 등)와 외부 에셋은 각 원저작자 라이선스를 따른다(외부 에셋은 저장소 비포함).
- 본 프로젝트는 비공식 팬/시즌 서버이며 Mojang/Microsoft와 무관하다.
