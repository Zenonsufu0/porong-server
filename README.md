# Poro Server

> 45일 시즌제 Minecraft RPG 서버 — 1차 시즌 프로젝트.
> 커스텀 전투/성장/보스/영지 시스템을 자체 플러그인(EmpireRPG)과 Discord 봇으로 운영한다.

<!-- TODO: 배지(빌드 상태/라이선스/디스코드) 필요 시 추가 -->

---

## 개요

- **장르:** Paper 기반 시즌제 RPG 서버 (오픈월드 필드 + 시즌 보스 + 개인 영지).
- **운영 모델:** Discord 인증 게이트 기반 공식 오픈 (공개 테스트 없음).
- **핵심 플러그인:** `EmpireRPG` — 전투·장비·강화·영지/농장·보스 보상·DB·HTTP API·Discord/웹 연동 데이터를 모두 소유.
- **보조:** MythicMobs(바닐라 강화 몹/보스 셸), IridiumSkyblock(개인 섬 셸).
- **기본 언어:** 한국어.

> 설계의 단일 진실 공급원(source of truth)은 **[`docs/final_master_plan.md`](docs/final_master_plan.md)** 이다. 도메인별 세부는 각 `docs/NN_*/CANON.md` 참조.

**기술 스택:** Paper **1.21.10** · Java **21** · MythicMobs **5.11.x** · IridiumSkyblock · Python **3.12+**(Discord 봇).

---

## 저장소 구조

| 경로 | 설명 |
|---|---|
| `custom-plugins/empire-rpg/` | **EmpireRPG** 핵심 플러그인 (Java, Gradle). 전투·성장·영지·보스·DB·API. |
| `bot/` | Discord 온보딩/운영 봇 (Python, discord.py cogs). |
| `server-config/` | MythicMobs 설정(필드 몹·시즌 보스·스킬·드랍 테이블) 등 서버 측 YAML. |
| `docs/` | 기획·설계 문서. 번호 도메인별 `CANON.md` + `decision_log.md`(DL-NNN). |
| `tools/` | 보조 파이썬 툴 (명령 레지스트리 린트, 무기 팩토리 등). |
| `scripts/` | 운영/개발 스크립트 (`orchestra.sh`=`orc`, 서버 기동/배포/백업 등). |
| `env/`, `bot/.env.example` | 환경변수 **예시** 템플릿 (실제 값은 커밋 금지). |
| `CLAUDE.md` / `AGENTS.md` | 에이전트/기여자용 작업 규칙. |

> **저장소에 없는 것 (의도적):**
> - `assets/` — 리소스팩/텍스처 원본. 외부 유래 에셋 혼재로 **저장소 비배포**(gitignored). 리소스팩은 별도 HTTP 서버로 서빙.
> - `server/` — 런타임 서버 데이터(월드·플러그인 설치본·로그). gitignored.
> - **제3자 플러그인**(MythicMobs, IridiumSkyblock 등) — 자체 플러그인(`poro-rpg`)만 저장소에 포함하며, 제3자 플러그인 jar는 배포하지 않는다(각 배포처에서 받아 `server/plugins/`에 설치).
> 자세한 배경: [`docs/decision_log.md`](docs/decision_log.md) 보안/라이선스 정리 항목.

---

## 핵심 구성요소

### EmpireRPG 플러그인 (`custom-plugins/empire-rpg/`)
- Paper 플러그인. 전투(무기별 스킬/자원/만충), 장비·강화·잠재, 영지/농장(약초·광물·공방), 필드/시즌 보스, 경제, DB 영속화, HTTP API(`/api/v1/...`).
- 빌드: Gradle (`build.gradle.kts`). 산출물 `build/libs/empire-rpg-*.jar`.

### Discord 봇 (`bot/`)
- `discord.py` 기반. cogs: 인증(`auth`), 필드보스 알림(`field_boss`), 플레이어 명령(`player_commands`), 역할(`role_commands`/`role_poll`).
- 설정은 전부 환경변수(`bot/config.py` → `os.environ`). 진입점 `bot/main.py`, 의존성 `bot/requirements.txt`.

### 서버 설정 (`server-config/`)
- MythicMobs: 필드 몹(평원/수로/광산/전초/고대벽), 시즌 보스, 보스 패턴 스킬, 필드 드랍 테이블.

---

## 빠른 시작

> ⚠️ 사전: JDK 21, Python 3.12+, Paper 1.21.10 서버(별도 준비). 실제 운영 값은 `.env`로 주입(아래 설정 참조).

### 1) 플러그인 빌드
```bash
cd custom-plugins/empire-rpg
./gradlew build
# 산출물: build/libs/empire-rpg-0.1.0.jar
```

### 2) Discord 봇 실행
```bash
cd bot
python3 -m pip install -r requirements.txt
cp .env.example .env   # 값 채우기 (토큰/길드/채널/역할 ID)
python3 main.py
```

### 3) 서버 기동 / 로컬 배포
<!-- TODO: 실제 서버 기동 절차 확정 (scripts/start.sh, deploy-local.sh 활용) -->
```bash
# 예: 로컬 배포 스크립트 / 기동 (세부는 scripts/ 참조)
scripts/deploy-local.sh   # 빌드된 jar + 설정 배포
scripts/start.sh          # 서버 기동
```

---

## 개발 워크플로우

- 작업 루프 공식 명령: **`orc`** (`scripts/orchestra.sh`).
  | 명령 | 역할 |
  |---|---|
  | `orc status` | 양쪽 worktree 상태 |
  | `orc diff-main` | main worktree diff |
  | `orc handoff-main "요약"` | main 변경 커밋 + 리뷰 동기화 |
  | `orc to-review` | 커밋을 codex-review로 동기화 |
- 설계/결정은 채팅에만 남기지 않고 **CANON 반영 + `decision_log.md`(DL-NNN)** 또는 `docs/idea_inbox.md`에 기록한다.
- commit · merge · push 는 **승인 후** 실행.

<!-- TODO: 브랜치 전략 / PR 규칙 / 코드 스타일 가이드 보강 -->

---

## 설정 · 비밀정보

- 모든 비밀값(Discord 봇 토큰, API 키, DB 자격증명)은 **환경변수**로만 주입한다.
- 템플릿: `bot/.env.example`, `env/application.example.env`, `env/database.example.env`.
- **실제 `.env`/키/토큰은 절대 커밋 금지** (`.gitignore`로 차단됨). 노출 시 즉시 재발급.

---

## 라이선스

**사유 라이선스 (Proprietary, All Rights Reserved)** — 전문은 [`LICENSE`](LICENSE) 참조.

- **재배포(2차 배포) 금지**, **수정·파생물 작성 금지** (저작권자 사전 서면 허가 시 예외).
- 열람·개인 참고만 허용.
- 제3자 구성요소(Paper/MythicMobs/IridiumSkyblock/discord.py 등)와 외부 에셋은 각 원저작자 라이선스를 따른다(외부 에셋은 저장소 비포함).
- 본 프로젝트는 비공식 팬/시즌 서버이며 Mojang/Microsoft와 무관하다.

---

## 문서

- 마스터 플랜(진실 공급원): [`docs/final_master_plan.md`](docs/final_master_plan.md)
- 도메인별 CANON: `docs/01_plugin_architecture` … `docs/13_pvp_system` 등 각 `CANON.md`
- 결정 로그: [`docs/decision_log.md`](docs/decision_log.md)
- 아이디어 인박스: [`docs/idea_inbox.md`](docs/idea_inbox.md)

<!-- TODO: 스크린샷 / 데모 / 운영 가이드 / 기여 안내(CONTRIBUTING) 추가 -->
