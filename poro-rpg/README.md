# poro-rpg

> **디스코드 유입 제한형, 45일 시즌제 Minecraft RPG 서버.**
> 바닐라 Paper 위에 자체 플러그인 **PoroRPG** 하나로 6무기 전투·장비 성장·개인 영지·필드/보스를 얹고, Discord 봇과 HTTP API로 온보딩·운영을 연동한다.

> 모노레포 전체 개요는 루트 [`../README.md`](../README.md). 설계 단일 진실 공급원은 [`docs/final_master_plan.md`](docs/final_master_plan.md), RPG 도메인 세부는 [`docs/`](docs/)의 각 `CANON.md`.

---

## 🧭 이게 뭔가요?

약 **45일** 동안만 운영하는 **시즌제** 마인크래프트 RPG 서버. 짧은 시즌 안에 **전투 → 성장 → 보스 → 생산**이 하나의 루프로 도는 것을 목표로 설계됐다.

- **누가**: 공개 테스트 없이 **디스코드 인증**한 사람만 정식 입장.
- **무엇을**: 6종 무기 중 하나로 필드 몹·필드보스·시즌보스를 잡고, 장비를 강화·세팅하며, 개인 영지에서 자원을 생산.
- **어떻게**: 핵심 게임 로직(전투·성장·영지·보스·경제·데이터)을 전부 자체 플러그인 **PoroRPG**가 소유. 몹/보스 외형은 MythicMobs, 섬 생성은 IridiumSkyblock이 "껍데기"만 담당.

---

## 🎮 게임 시스템

| 시스템 | 한 줄 설명 |
|---|---|
| **6무기 전투** | 검 / 도끼 / 창 / 석궁 / 낫 / 스태프. 각 무기 `좌클릭·우클릭·Shift+우클릭·F` 스킬 4종 + 무기 고유 **자원(스택)·만충** 메커닉. 만충 시 핵심기가 **만충 폭발(×보너스)** 로 터진다. |
| **장비 성장** | 5슬롯(무기·투구·상의·하의·신발) T1 단일 체계. **강화(0~25강)** + **잠재능력(큐브)** + **직업각인(소모형/유지형 A·B)** + 스탯 트리. |
| **개인 영지** | "스카이블럭"이 아닌 **개인 영지**. 약초 재배지 · 광물 채굴기 · 공방 가공기 3종 시설로 생산·가공(영지 저장고, 오프라인 누적 생산). |
| **필드 & 보스** | 필드 5개(일반몹 2 + 정예몹 1 + 필드보스 1). **필드보스 5종**(30분 정시 스폰), **시즌보스 6종**, **최종보스 3종**. 원샷 방지 85% 클램프. |
| **PvP** | 자유 / 정규(점수·랭킹) / 친선 대전 + 아레나 풀. 정규대전은 장비 가상 동일화(IL 보정). |
| **경제** | 골드 + 강화석 + 큐브를 **DB 가상 지갑**으로 관리(바닥 드랍 없음). 경매·판매 상점. |
| **디스코드 온보딩** | 인증 게이트 → 역할 부여, 직업 역할, 필드보스 알림 (봇은 `../poro-discord/`). |

> 설계 금지(1차 시즌): 플레이어 지속 장판, 적 표식, 받는 피해 증가/방어 감소 디버프, 파티 전체 피해 증가, 복잡한 스택 폭발.

---

## 🏗️ 아키텍처

```
   ┌──────────────┐    HTTP API (X-Api-Key, :8765)   ┌────────────────────┐
   │  Discord 봇   │ ───────────────────────────────▶ │  PoroRPG 플러그인    │
   │ (Python)     │   /auth /field-boss /player ...   │  (Paper 1.21.10)    │
   │  인증·알림·역할 │ ◀─────────────────────────────── │  전투·성장·영지·보스   │
   └──────────────┘         인증 상태/역할 큐           │  경제·데이터·API      │
                                                       └─────────┬──────────┘
                                          SQLite `poro.db` + 플레이어 JSON
   보조 셸: MythicMobs(몹/보스 외형·스코어보드 태그) · IridiumSkyblock(섬 생성·보호)
```

- **PoroRPG**가 모든 게임 상태의 주인. 데이터는 SQLite(`poro.db`) + 플레이어 JSON에 영속화.
- 플러그인은 로컬 **HTTP API**(`:8765`, `X-Api-Key`)로 `/auth/*`·`/api/v1/*`·`/admin/*` 제공.
- **Discord 봇**이 이 API를 호출해 디스코드 인증 ↔ 인게임 닉네임 연결, 역할·알림 처리.
- **MythicMobs**는 몹/보스 셸 제공. 스폰 시 `poro_field_*`·`poro_rank_*`·`poro_type_*` 스코어보드 태그로 플러그인이 몹 종류 식별.

## 🧱 기술 스택

| 영역 | 사용 |
|---|---|
| 서버 | Paper **1.21.10** / Java **21** |
| 핵심 플러그인 | PoroRPG (Gradle Kotlin DSL) |
| 보조 플러그인 | MythicMobs **5.11.x**, IridiumSkyblock |
| 봇 | Python **3.12+**, discord.py (`../poro-discord/`) |
| 데이터 | SQLite (`poro.db`) + 플레이어 JSON |

---

## 📂 폴더 구조

| 경로 | 설명 |
|---|---|
| `custom-plugins/poro-rpg/` | **PoroRPG** 핵심 플러그인 (Java, Gradle). 전투·성장·영지·보스·DB·API. |
| `server-config/` | MythicMobs 설정(필드 몹·시즌 보스·스킬·드랍 테이블) 등 서버 측 YAML. |
| `docs/` | RPG 설계 도메인 문서. 번호 도메인별 `CANON.md`. |
| `tools/` | 보조 파이썬 툴 (명령 레지스트리 린트, 무기 팩토리 등). |
| `scripts/` | RPG 전용 스크립트 (`setup-worlds.sh` 등). |
| `env/` | 환경변수 **예시** 템플릿 (실제 값은 커밋 금지). |
| `assets/` | 리소스팩/텍스처 원본 — 외부 유래 에셋 혼재로 **저장소 비배포**(gitignored). 리소스팩은 별도 HTTP 서버로 서빙. |
| `server/` | 런타임 서버 데이터(월드·플러그인 설치본·로그). gitignored. |

> 제3자 플러그인(MythicMobs, IridiumSkyblock 등)은 자체 플러그인만 저장소에 포함하며, 제3자 jar는 각 배포처에서 받아 `server/plugins/`에 설치한다.

---

## 🚀 빠른 시작

> 사전 준비: JDK 21, Paper 1.21.10 서버. (Discord 봇은 `../poro-discord/` 참조.)

```bash
# 플러그인 빌드
cd custom-plugins/poro-rpg
./gradlew build
# 산출물: build/libs/poro-rpg-0.1.0.jar  → server/plugins/ 에 배치
```

> 플러그인 데이터 폴더는 `server/plugins/PoroRPG/`, 시드는 `server/plugins/PoroRPG/seeds/`(런타임 우선 로드). 운영자 명령은 `/poro`, `/poro-*` 계열, 권한 노드는 `poro.*`.
> 월드 초기 생성: `scripts/setup-worlds.sh` 참조.

## 🔐 설정 · 비밀정보

- 모든 비밀값(`PORO_API_KEY`, DB 자격증명 등)은 **환경변수**로만 주입.
- 템플릿: `env/application.example.env`, `env/database.example.env` (봇은 `../poro-discord/.env.example`).
- **실제 `.env`/키/토큰은 절대 커밋 금지** (`.gitignore` 차단). 노출 시 즉시 재발급.

---

## 📚 문서

- 마스터 플랜(SoT): [`docs/final_master_plan.md`](docs/final_master_plan.md)
- 도메인별 CANON: [`docs/01_plugin_architecture`](docs/01_plugin_architecture) … [`docs/13_pvp_system`](docs/13_pvp_system) 등 각 `CANON.md`
- 결정 로그: [`docs/decision_log.md`](docs/decision_log.md) · 아이디어 인박스: [`docs/idea_inbox.md`](docs/idea_inbox.md)
