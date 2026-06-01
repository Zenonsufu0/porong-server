# Poro Server (포로 서버)

> **디스코드 유입 제한형, 45일 시즌제 Minecraft RPG 프로젝트 서버.**
> 바닐라 Paper 위에 자체 플러그인 **PoroRPG** 하나로 6무기 전투·장비 성장·개인 영지·필드/보스를 얹고, Discord 봇과 HTTP API로 온보딩·운영을 연동한다.

---

## 🧭 이게 뭔가요?

**Poro Server**는 약 **45일** 동안만 운영하는 **시즌제** 마인크래프트 RPG 서버입니다. 일반적인 "오래 키우는 서버"가 아니라, 짧은 시즌 안에 **전투 → 성장 → 보스 → 생산**이 하나의 루프로 깔끔하게 도는 것을 목표로 설계됐습니다.

- **누가 들어오나**: 공개 테스트 없이, **디스코드에서 먼저 인증**한 사람만 정식 입장합니다.
- **무엇을 하나**: 6종 무기 중 하나를 골라 필드 몹·필드보스·시즌보스를 잡고, 장비를 강화·세팅하며, 개인 영지에서 자원을 생산합니다.
- **어떻게 만들었나**: 핵심 게임 로직(전투·성장·영지·보스·경제·데이터)을 전부 **자체 플러그인 PoroRPG**가 소유하고, 몹/보스 외형은 MythicMobs, 섬 생성은 IridiumSkyblock이 "껍데기"만 담당합니다.

> 📌 설계의 **단일 진실 공급원(source of truth)** 은 [`docs/final_master_plan.md`](docs/final_master_plan.md) 입니다. 세부 수치·규칙은 도메인별 `docs/NN_*/CANON.md` 가 가집니다.

---

## 🎮 무엇을 할 수 있나 (게임 시스템)

| 시스템 | 한 줄 설명 |
|---|---|
| **6무기 전투** | 검 / 도끼 / 창 / 석궁 / 낫 / 스태프. 각 무기는 `좌클릭·우클릭·Shift+우클릭·F` 스킬 4종 + 무기 고유 **자원(스택)·만충** 메커닉. 충전을 끝까지 채우면 핵심기가 **만충 폭발(디스크리트 ×보너스)** 로 터진다. |
| **장비 성장** | 5슬롯(무기·투구·상의·하의·신발) T1 단일 체계. **강화(0~25강)** + **잠재능력(큐브)** + **직업각인(소모형/유지형 A·B)** + 스탯 트리(치명 등). |
| **개인 영지** | "스카이블럭"이 아니라 **개인 영지**로 노출. 약초 재배지 · 광물 채굴기 · 공방 가공기 3종 시설로 자원을 생산·가공(영지 저장고 사용, 오프라인 누적 생산 지원). |
| **필드 & 보스** | 필드 5개(일반몹 2 + 정예몹 1 + 필드보스 1). **필드보스 5종**(30분 정시 스폰), **시즌보스 6종**, **최종보스 3종**(균열왕·타락한 이중체·진혼의 주시자). 원샷 방지 85% 클램프. |
| **PvP** | 자유 / 정규(점수·랭킹) / 친선 대전 + 아레나 풀. 정규대전은 장비 가상 동일화(IL 보정)로 무기·각인 실력 비교. |
| **경제** | 골드 + 강화석 + 큐브를 **DB 가상 지갑**으로 관리(바닥 드랍 없음). 경매(시세 재구성), 농작물·광물 판매 상점. |
| **디스코드 온보딩** | 인증 게이트 → 역할 부여, 직업 역할, 필드보스 알림을 Discord 봇이 처리. |

> 설계 금지 항목(1차 시즌): 플레이어 지속 장판, 적 표식, 받는 피해 증가/방어 감소 디버프, 파티 전체 피해 증가, 복잡한 스택 폭발. (단순하고 읽기 쉬운 전투 지향)

---

## 🏗️ 아키텍처 (시스템 구조)

```
   ┌──────────────┐    HTTP API (X-Api-Key, :8765)   ┌────────────────────┐
   │  Discord 봇   │ ───────────────────────────────▶ │  PoroRPG 플러그인    │
   │ (Python)     │   /auth /field-boss /player ...   │  (Paper 1.21.10)    │
   │  인증·알림·역할 │ ◀─────────────────────────────── │  전투·성장·영지·보스   │
   └──────────────┘         인증 상태/역할 큐           │  경제·데이터·API      │
                                                       └─────────┬──────────┘
   ┌──────────────┐    /admin/* 대시보드 API                     │ 소유/영속
   │ 운영 웹 대시보드 │ ◀─────────────────────────────────────────┤
   └──────────────┘                                              ▼
                                          SQLite `poro.db` + 플레이어 JSON
   보조 셸: MythicMobs(몹/보스 외형·스코어보드 태그) · IridiumSkyblock(섬 생성·보호)
```

- **PoroRPG**가 모든 게임 상태의 주인이다. 데이터는 SQLite(`poro.db`) + 플레이어 JSON에 영속화한다.
- 플러그인은 로컬 **HTTP API**(`:8765`, `X-Api-Key` 인증)를 열어 `/auth/*`, `/api/v1/*`, `/admin/*`를 제공한다.
- **Discord 봇**은 이 API를 호출해 디스코드 인증 ↔ 인게임 닉네임을 연결하고 역할·알림을 처리한다.
- **MythicMobs**는 몹/보스 셸을 제공하며, 스폰 시 `poro_field_*`·`poro_rank_*`·`poro_type_*` **스코어보드 태그**를 붙여 플러그인이 몹 종류를 식별한다.

---

## 🧱 기술 스택

| 영역 | 사용 |
|---|---|
| 서버 | Paper **1.21.10** / Java **21** |
| 핵심 플러그인 | PoroRPG (Gradle Kotlin DSL 빌드) |
| 보조 플러그인 | MythicMobs **5.11.x**, IridiumSkyblock |
| 봇 | Python **3.12+**, discord.py |
| 데이터 | SQLite (`poro.db`) + 플레이어 JSON |

---

## 📂 저장소 구조

| 경로 | 설명 |
|---|---|
| `custom-plugins/poro-rpg/` | **PoroRPG** 핵심 플러그인 (Java, Gradle). 전투·성장·영지·보스·DB·API. |
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
> - **제3자 플러그인**(MythicMobs, IridiumSkyblock 등) — 자체 플러그인(`poro-rpg`)만 포함하며, 제3자 jar는 각 배포처에서 받아 `server/plugins/`에 설치한다.

---

## 🚀 빠른 시작

> ⚠️ 사전 준비: JDK 21, Python 3.12+, Paper 1.21.10 서버. 실제 운영 값은 `.env`로 주입(아래 [설정](#-설정--비밀정보) 참조).

### 1) 플러그인 빌드
```bash
cd custom-plugins/poro-rpg
./gradlew build
# 산출물: build/libs/poro-rpg-0.1.0.jar  → server/plugins/ 에 배치
```

### 2) Discord 봇 실행
```bash
cd bot
python3 -m pip install -r requirements.txt
cp .env.example .env   # 값 채우기 (봇 토큰 / 길드·채널·역할 ID / PORO_API_KEY)
python3 main.py
```

### 3) 서버 기동 / 로컬 배포
```bash
scripts/deploy-local.sh   # 빌드된 jar + 설정 배포 (세부는 scripts/ 참조)
scripts/start.sh          # 서버 기동
```
> 플러그인 데이터 폴더는 `server/plugins/PoroRPG/`, 시드는 `server/plugins/PoroRPG/seeds/`(런타임 우선 로드). 운영자 명령은 `/poro`, `/poro-*` 계열, 권한 노드는 `poro.*`.

---

## 🔁 개발 워크플로우

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

## 🔐 설정 · 비밀정보

- 모든 비밀값(Discord 봇 토큰, `PORO_API_KEY`, DB 자격증명)은 **환경변수**로만 주입한다.
- 템플릿: `bot/.env.example`, `env/application.example.env`, `env/database.example.env`.
- **실제 `.env`/키/토큰은 절대 커밋 금지** (`.gitignore`로 차단됨). 노출 시 즉시 재발급.

---

## 📜 라이선스

**사유 라이선스 (Proprietary, All Rights Reserved)** — 전문은 [`LICENSE`](LICENSE) 참조.

- **재배포(2차 배포) 금지**, **수정·파생물 작성 금지** (저작권자 사전 서면 허가 시 예외).
- 열람·개인 참고만 허용.
- 제3자 구성요소(Paper/MythicMobs/IridiumSkyblock/discord.py 등)와 외부 에셋은 각 원저작자 라이선스를 따른다(외부 에셋은 저장소 비포함).
- 본 프로젝트는 비공식 팬/시즌 서버이며 Mojang/Microsoft와 무관하다.

---

## 📚 문서

- 마스터 플랜(진실 공급원): [`docs/final_master_plan.md`](docs/final_master_plan.md)
- 도메인별 CANON: `docs/01_plugin_architecture` … `docs/13_pvp_system` 등 각 `CANON.md`
- 결정 로그: [`docs/decision_log.md`](docs/decision_log.md)
- 아이디어 인박스: [`docs/idea_inbox.md`](docs/idea_inbox.md)

<!-- TODO: 스크린샷 / 데모 영상 / 운영 가이드 / CONTRIBUTING 추가 -->
