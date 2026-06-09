# 포로서버 디스코드 봇 — 작업 목록 (task.md)

> **[STATUS: ACTIVE]** — poro-discord 구현 작업 트래커.
> 구조 정본은 [`architecture.md`], 통신 계약은 [`integration_contract.md`], 결정 근거는 `../../poro-rpg/docs/decision_log.md` **DL-130~134**.
> 상태 표기: 🟢 구현 · 🟡 스텁/부분 · 🔴 미설계 · ✅ 완료 · ⬜ 미착수.
> 연동/운영 명령어는 **사용자 명시 요청 시에만** 실구현(DL-130 ⑤). 그 외는 설계/인터페이스까지만.

## 0. 우선순위 요약

| # | 작업 | 영역 | 선행 | 상태 |
|---|---|---|---|---|
| T1 | 알림 인바운드 수신(push) + `core/notifier.py` 디스패처 | core | — | 🔴 |
| T2 | `rpg.md §8` Node.js 표기 → Python 정합 | docs | — | ⬜ |
| T3 | `modules/admin` 명령어 설계 확정 | admin | 권한정책 | 🟡 |
| T4 | `integrations/poromon_api.py` 인터페이스 확정 | poromon | 포로몬 설계 | 🟡 |
| T5 | 알림 후보(시즌/월드보스·점검·업데이트) 배선 | notify | T1 | 🟡 |
| T6 | `modules/event` 실구현 설계 | event | — | 🟡 |
| T7 | 공통 디스코드 인증을 `core/`로 분리 (멀티서버 온보딩) | core/onboarding | — | 🟡 |
| T8 | 오라클 서버 배포(24h 상시) 구성 문서/스크립트 | infra | — | ⬜ |
| T9 | 포로몬 서버별 약관동의+화이트리스트 온보딩 (봇측 구현) | poromon | T7, T4 | 🟡 |
| T10 | 이모지 서버선택 → 카테고리 접근 역할 → 역할기반 가시성 | roles/onboarding | — | 🔴 |
| T11 | 운영자 닉네임 변경 명령(`/닉네임변경`·`/닉네임재입력`) | admin | API | 🟡 |
| T12 | SQLite 저장 계층 도입(봇 영속 DB) — community/moderation/support 공통 선행 | core | — | 🟢 |
| T13 | community 모듈(레벨·칭호·리더보드·임시음성) | community | T12 | 🟡 |
| T14 | community 확장(출석·일일보상·임시역할 자동만료) | community | T13 | 🟡 |
| T15 | moderation 모듈(제재·추방·경고 + 운영/감사로그) | moderation | T12 | 🟢 (경고·타임아웃·추방·차단 + mod_log) |
| T16 | support 모듈(버그제보·티켓·FAQ→운영진문의) | support | T12 | 🟡 |
| T17 | admin 확장(서버 on/off·카테고리 템플릿 신설) | admin | 봇 길드권한 | 🟢 (템플릿 자동전개 — `/서버신설`) |
| T18 | common 확장(`/도움말`·`/서버상태`) | common | T4(서버상태) | 🟡 |
| T19 | `/클래스선택`을 `modules/roles/`→`modules/rpg/`로 이관(도메인 격리). 클래스 매핑도 RPG로 | rpg | — | ⬜ |
| T20 | 도메인 명령어 가시성 정책 — 접근역할 제한(1차) + 카테고리 가드 헬퍼(2차) | core | T10 | 🟡 |
| T21 | 서버 레지스트리 + 생애주기(준비→활성→종료) 상태머신 + `/서버시작`·`/서버종료`·`/서버목록` | core/admin | T12 | 🟢 |

> **T21 구현(2026-06-08, `modules/server_lifecycle/commands.py` + `core/servers.py` + `core/gating.py`):** 🟢 `/서버목록`·`/서버정보`(읽기), `/서버신설`(prep 행 + 카테고리/역할 선택 연결), `/서버시작`(prep→active)·`/서버종료`(active→ended) 상태전이 + 카테고리 가시성(공개/[종료] 아카이브). domain당 active 1 = 부분유니크 인덱스 강제. 게이팅 3층 헬퍼 `core/gating.py`(순수 판정 + `requires_category`/`requires_server_active` 데코레이터 — 도메인 명령 부착 대기). ✅ mod_log 적재+#운영로그(§2) 배선 완료(2026-06-09). ⬜ 후속: 카테고리/역할 자동생성(T17), 이모지 서버선택 패널(§3).

> T12~T18·T20·T21 = **🟡 설계 완료·구현 전**(상세 docs 작성됨). T19 = ⬜ 코드 이관(설계 불필요). 실구현은 사용자 명시 요청 시.

> T12~T18 은 §9 후보 기능 백로그에서 승격(2026-06-06). 상세 명세·근거는 §9. Tier3(초대추적·건의투표)는 §9에 보류 유지.
> T19(클래스 RPG 이관)·T20(명령어 가시성)은 도메인 격리 후속 결정(2026-06-06). T20 설계 노트는 §10.
> **T12 SQLite 스키마 1차안 = [`data_model.md`](data_model.md)** (servers 레지스트리 포함 — T21·T13~T16 공통 기반).
> **T12 기반 구현(2026-06-08): `core/db.py`** = aiosqlite 단일커넥션 + 쓰기 lock + `schema_meta` 증분 마이그레이션 러너. v1 = `servers` 테이블(§2.1, domain당 active 1 부분유니크). `BOT_DB_PATH`(.env). 나머지 테이블(community_xp·warnings·tickets…)은 각 기능 구현 시 v2+ 마이그레이션으로 추가. main.py 가 기동 시 연결→`bot.db`.
> **T13 커뮤니티 레벨 상세 = [`community_level.md`](community_level.md)**, T21 생애주기 상세 = [`server_lifecycle.md`](server_lifecycle.md).
> **T15 모더레이션 상세 = [`moderation.md`](moderation.md)**, **T16 지원/문의 상세 = [`support.md`](support.md)**.

## 1. core (공통 인프라)

- 🔴 **T1. 알림 인바운드 수신(push) + 디스패처** — 통신 방향 = 게임서버 → 봇 push 확정(DL-133).
  - ⬜ ① 인바운드 HTTP 리스너(`core/`, aiohttp.web 등)를 discord 루프와 함께 기동.
  - ⬜ 인바운드 보안: 공유 시크릿/HMAC 서명 검증 + 방화벽/IP 허용. 시크릿 `.env`(`INBOUND_SECRET` 등).
  - ⬜ ② `core/notifier.py` 디스패처 — `(domain, kind, embed, mention_role_key)` → 채널/멘션 라우팅·전송·실패무시.
  - ⬜ 각 도메인 모듈은 이벤트 의미 해석(embed)만, 전송은 notifier 위임.
  - ⬜ 현행 RPG 필드보스 폴링은 유지, push 구조 완성 후 점진 이관.
  - → 설계: [`notifications.md`] "통합 알림 구조" 절. 실구현 전 인터페이스 단계.
- ⬜ **봇 관여 경계 가드(DL-133)** — 게임 상태 변경은 `integrations/*_api.py` 경유로만. DB/파일/임의 RCON 직접접근 금지를 코드 리뷰 체크포인트로.
- ⬜ 권한 데코레이터(`requires_permission`) 단위 검증 보강(운영 명령어 도입 전).

## 2. integrations (외부 서버 연동)

- 🟢 `rpg_api.py` — PoroRPG HTTP API 클라이언트(구현됨). 신규 엔드포인트는 RPG API 확정 시 추가.
- 🟡 **T4. `poromon_api.py`** — 현재 인터페이스 스텁. 연동 방식 = HTTP API(PoroMonCore, RPG와 동일 패턴, DL-133).
  - ⬜ API 포트·인증 시크릿·엔드포인트 계약(인증/조회/이벤트 push 스키마) 확정 — `../../poromon/docs/03_poromoncore/` 선행.
  - ⬜ `get_server_status`(접속/TPS), 도감 조회 인터페이스 확정.
  - ⬜ 이벤트 알림 = 포로몬 서버 → 봇 push(T1 인바운드 수신 공용).
  - ⚠ 실 API 연동은 사용자 명시 요청 시에만(DL-130 ⑤).

## 3. modules (도메인 Cog)

### rpg/ 🟢 (auth · player_commands · field_boss · role_poll)
- 🟢 온보딩 인증·역할 승급, 프로필/영지/보스 조회, 필드보스 알림 구현.
- ⬜ 필드보스 알림 로직을 T1(notifier) 완성 후 위임 구조로 이관.

### roles/ 🟢 (/클래스선택 · /알림설정)
- 🟢 알림 역할 토글·클래스 역할 선택. 신규 알림 역할 추가 시 `config` + 선택지 동기.

### admin/ 🟡 (스텁 — 설계 선행, 보안 영향 큼)
- 🟡 **T3. 명령어 설계 확정** (모두 `@requires_permission` 보호, [`domains/admin.md`]):
  - ⬜ `/공지` `/점검` `/보스알림` (admin / event_manager)
  - ⬜ `/역할부여` `/유저조회` (admin)
  - ⬜ `/골드지급` (rpg_manager, API 경유 + 운영로그 기록)
  - ⬜ `/이벤트등록` (event_manager)
  - ⬜ 운영 명령어 전용 채널(`#운영로그`) 제한 검토.
  - ⚠ 상태 변경 명령은 게임 서버 API 경유 + 사용자 명시 요청 시에만 실구현.

### poromon/ 🟡 (스텁)
- ⬜ `/포로몬현황` `/포로몬도감` — `poromon_api` 실구현(T4) 선행.

### event/ 🟡 (스텁)
- ⬜ **T6.** 이벤트 등록/시작/종료 알림 흐름 설계 — admin `/이벤트등록`·notifier(T1)와 연계.

### common/ 🟢
- 🟢 게임 비종속 공통(/핑 등). 추가 공통 명령은 여기.

## 4. 알림 배선 (→ T1 선행, [`notifications.md`])

- 🟢 RPG 필드보스 5분전·등장.
- 🟡 **T5.** 시즌보스 / 월드보스 / 포로몬 이벤트 / 이벤트 시작·종료 / 점검 / 업데이트
  — 역할만 정의됨, 트리거·전송 미구현. notifier 완성 후 도메인별 배선.

## 5. 멀티서버 온보딩 / 배포 (DL-131)

### 공통 온보딩 흐름 (전 서버 동일 — 2026-06-08 사용자 방향)
> **[STATUS: DRAFT]** — 추가되는 모든 서버에 동일 적용 전제. 확정 시 DL 동기화(RPG worktree, 여긴 읽기전용).

표준 흐름 (서버별 3단계 역할 상태머신 — 채널 가시성 = 역할 권한 오버라이트):
1. 포로 디스코드 서버 가입
2. 플레이할 서버 선택(서버선택 패널, T10) → **`<서버>_접근`(임시역할)** 부여 → **약관 채널만** 보임(읽기전용)
3. 약관 채널의 **약관동의 버튼** 클릭 → 임시역할을 **`<서버>_인증전`로 변환** → **인증 채널만** 보임(약관 채널 숨김)
4. 인증 채널의 **🔑 인증코드 입력 버튼 → 모달(코드 입력칸)** → 봇이 verify 호출 (인게임 `/인증`이 발급한 코드)
5. 인증 성공 → **`<서버>_인증전` → `<서버>_플레이어`(정식)** + 서버별 화이트리스트 → 전체 채팅/음성 + 인게임 활동
> 기존 `미인증→접속대기→인증유저`의 **서버별 버전**. 단일서버 역할을 서버별/템플릿화 필요(T21 레지스트리·`api_env_key` 연계).

**✅ 확정 (2026-06-08): 코드 방향 = 인게임 발급 → 봇 `/인증코드` 검증 으로 전 서버 통일.**
이유 = 신원 양측 권위화(서버=로그인된 MC uuid, 봇=interaction discord_id) → 유저 닉 받아쓰기(오타·사칭 여지) 제거, 단계 축소. 기존 RPG(봇 발급→인게임 `/연동`)도 이 방향으로 이관.
> ✅ DL 동기화 완료 — RPG `decision_log.md` **DL-138**(2026-06-08, "코드방향 통일 + RPG 측 구현"). RPG가 `/인증` 명령 + `POST /auth/verify` 핸들러까지 실구현.

**기술 제약 / 챙길 점 (확정 방향 전제):**
1. **미인증 인게임 접속 허용**: `/인증`을 치려면 접속은 돼 있어야 함 → 바닐라 화이트리스트로 *접속 자체*를 막지 말 것. 미인증자는 제한 로비/대기 영역까지 진입해 `/인증`만 가능, 화이트리스트는 *플레이 권한*으로 분리. (RPG=Paper·포로몬=모드 동일 패턴)
2. **봇 라우팅**: `/인증코드` 가 어느 서버 verify 엔드포인트로 갈지 = 카테고리 가드(§10 T20)로 문맥 결정. RPG·포로몬 동시 접근자 모호성도 이걸로 해소.
3. **코드 보안**: 코드는 mc_uuid 에만 바인드(먼저 친 사람이 링크 차지) → 짧은 TTL + 1회용 + 충분한 엔트로피 + verify rate-limit. API 계약에 명시.
4. **마이그레이션 순서**: `core/` 공통 온보딩 먼저 → 포로몬 흡수 → RPG 마지막 이관(기존 동작 보호). RPG 측 = 인게임 `/인증`(코드 발급) + 봇→RPG verify 엔드포인트 신설, `create_pending`/role-queue 폴링 제거(RPG worktree 협의). 봇 `auth.py` = 닉 모달 제거, 약관동의 버튼은 동의 상태/역할 부여만.

**✅ 확정 추가 (2026-06-08):**
- **인증 UI = 버튼+모달** (`/인증코드` 슬래시 아님). 인증 채널에 영구 버튼 → 모달 코드 입력칸 → verify. 채팅에 메시지 안 남김(깔끔). RPG `auth.py`의 `약관동의 버튼→NicknameModal` 패턴 재사용. 현 포로몬 `/인증코드` 슬래시·DM은 폴백(또는 제거).
- **채널 가시성 = 읽기전용 + 봇 게이트 2중**: 약관·인증 채널은 임시/인증전 역할에 대해 `send_messages=False` **+ `use_application_commands=False`**(슬래시까지 차단). 버튼/모달은 읽기전용에서도 작동. 봇 측 약관동의 역할 체크는 우회 방어로 병행.
- **약관동의 게이트 = 역할 기반(1차)**: 동의 시 `<서버>_인증전` 역할 부여 → 인증 버튼/모달이 그 역할 확인. DB 없이 디스코드 역할만으로 동작. (DB 기록(b)은 T12 도입 시 얹기)
- **닉네임 1회 입력 폐기**: 인게임 발급 방향 → verify 응답이 `uuid`+**`name`** 반환 → 봇이 신원 확보. 유저 닉 타이핑 단계 제거. RPG **DL-138**로 베이스라인 닉 입력 단계(final_master_plan §3 등) SUPERSEDE 확정(2026-06-08). ※ RPG 원장 DL-131은 폴더 rename으로 무관.
  > ⚠ 매핑은 봇 DB에 **저장 안 함**(권위=게임서버, `data_model.md` §3). `name`은 표시/로깅용. API 계약 추가: 게임서버 verify 응답에 `name` 포함 필요(현 `verify_code`는 uuid+name 모두 읽음).

**남은 열린 결정:**
- **공통화(T7)**: 버튼+모달·약관동의·게이팅·역할전이를 도메인 모듈 중복 대신 `core/` 공통 온보딩으로 추출. 현재 포로몬 전용 구현(`modules/poromon/commands.py`)이 그 시드.
- **약관동의 DB 기록(b)** 도입 시점: 1차는 역할 기반, 감사/이력 필요 시 T12 테이블.

### 온보딩 — 공통 인증 ↔ 서버별 화이트리스트 분리
- 🟡 **T7. 공통 온보딩 모듈 = `modules/onboarding/panels.py` (1차 구현).**
  - 🟢 도메인 비종속 약관 게이트 + 인증 버튼/모달 패널 + 3단계 역할 전이(접근→인증전→플레이어) 구현. 서버 레지스트리 = `core/config.ONBOARDING_SERVERS`(코드 기반, DB 전).
  - 🟢 verify 라우팅(도메인→API)·약관 게이트(인증전 역할 확인)·운영자 `/온보딩패널 <서버>` 게시 명령.
  - ⬜ RPG 이관(아래) 후 RPG 도 레지스트리 등록 → `auth.py` 공통부 흡수.
  - ✅ 매핑 비저장 확정: verify 응답 `uuid`+`name`은 표시/로깅용, 봇 DB 미저장(data_model §3). 영속 상태=부여된 디스코드 역할.
- 🟢 RPG 서버별 단계: 약관동의 + 인게임 `/연동` 코드 → RPG 화이트리스트 + `인증유저`. **(현행 — 구 코드방향, 이관 대상)**
- 🟡 **T9. 포로몬 서버별 단계:** 인게임 `/인증` 발급 → 봇 인증 버튼/모달 verify → `포로몬플레이어` 승급.
  - 🟢 봇 측 = `poromon_api.verify_code` + 공통 온보딩 패널로 구현. env(`ROLE_포로몬접근/인증전/플레이어_ID`, `CHANNEL_포로몬약관/인증_ID`) 추가.
  - ⬜ 포로몬 게임서버 측 = 인게임 `/인증` 코드 발급 + `/auth/verify` 엔드포인트(`name` 반환) 실구현 — `../../porong-mon/` 영역(읽기전용, 협의 필요).
- ⚠ 본인인증 코드는 서버별로 유지 — 소유권 검증·사칭 방지(DL-131).
- ⚠ **미인증 인게임 접속 허용 전제**: 화이트리스트로 *접속 자체*를 막으면 `/인증` 불가 → 제한 로비 진입 허용 + 화이트리스트는 *플레이 권한*으로 분리.

### 채널/카테고리 구조 + 서버 선택 (DL-132)
- 🔴 **T10. 이모지 서버선택 → 카테고리 접근 역할 → 역할기반 가시성.**
  - ⬜ 카테고리 구조 확정(공통 / RPG / 포로몬 / 기타) + 카테고리별 약관 채널 배치.
  - ⬜ 서버선택 이모지 패널(reaction) → `RPG접근`·`포로몬접근`·`기타접근` 역할 자동 부여/해제.
  - ⬜ 카테고리 채널 권한 = 접근 역할에게만 가시(디스코드 권한 설정 + 봇 검증).
  - ⬜ 카테고리/역할/이모지 ID `core/config.py` + `.env`(`ROLE_RPG접근_ID` 등) 추가.
  - ⚠ 카테고리 접근 ≠ 화이트리스트 — 게임 접속은 §D 약관동의+인게임 인증 필요(DL-132 대원칙⑤).

### 운영자 닉네임 변경 (DL-132)
- 🟡 **T11. `/닉네임변경 <유저> <새닉네임> <사유>` · `/닉네임재입력 <유저>`(토글)** — admin 한정.
  - ⬜ 플레이어 셀프 변경 금지, 운영자 명령으로만 처리.
  - ⬜ 닉네임 교체 시 화이트리스트 정합(구 닉네임 해제/신 닉네임 재인증) + 운영로그 기록.
  - ⚠ 게임 서버 API 경유 상태 변경 → 사용자 명시 요청 시에만 실구현.

### 배포 — 오라클 상시 운영
- ⬜ **T8. 오라클 클라우드 인스턴스 배포(24h 상시, 게임 호스팅과 분리, DL-131).**
  - ⬜ 인스턴스 프로비저닝 + Python 3.12 런타임.
  - ⬜ 프로세스 관리(systemd 서비스 등) + 로그/재기동 정책.
  - ⬜ 봇→게임 서버 API 도달성(방화벽/네트워크) 구성.
  - ⬜ `.env`(토큰·키)는 인스턴스에만 — 커밋 금지. `.env.example`은 placeholder 유지.

## 6. 문서 정합

- ⬜ **T2.** `docs/domains/rpg.md §8`의 봇 언어 Node.js 표기 → Python 3.12/discord.py 정합(DL-030 SUPERSEDE by DL-130). 마스터 docs 변경은 합의 후.
- ⬜ 구조/상태 변경 시 [`architecture.md`]·[`index.md`] 구현 현황표 동기.

## 7. 작업 규칙(요약, 상세는 `../CLAUDE.md`)

- 작고 리뷰 가능한 단위. 변경 전 대상 파일 요약 → 변경 후 내용 요약.
- 구문 검증: `python3 -m compileall main.py core integrations modules`.
- 비밀정보(토큰·키·웹훅·DB 자격증명) 커밋 금지. 새 설정은 `.env.example`에 placeholder만.
- commit · merge · push는 사용자 승인 후에만. master 직접 merge 금지.

## 8. 세션 핸드오프 (다음 세션 이어가기)

### 이번 세션 완료 (2026-06-09, feature/discord-dev) — moderation T15 (경고계 + 제재)
mod_log 인프라 위에 경고계 + 상태변경 제재까지 구현(제재는 사용자 명시 승인하 진행).

**구현 산출:**
- **v3 마이그레이션** `core/db.py` = `warnings` 테이블(§2.4) + `idx_warnings_user`.
- **`core/warnings.py`** = add_warning/list_warnings/count_active/get_warning/revoke_warning(철회=active=0, 행 보존).
- **`permissions.permission_rank()`** = 대상 보호 서열(owner100·admin80·매니저50·support40·0). §1b.
- **`modules/moderation/commands.py`**:
  - 경고계: `/경고`(warnings+DM+활성수)·`/경고목록`(이력 임베드)·`/경고취소`(철회).
  - 제재: `/타임아웃`(분/시간/일 단위, ≤28일)·`/타임아웃해제`·`/추방`·`/차단`·`/차단해제`.
  - 공통 가드 `_guard()` = `_target_reject_reason`(봇·자기자신·서버소유자·동급이상) + `_bot_hierarchy_reject`(봇 위계). `discord.Forbidden`/`HTTPException` graceful. 추방/차단=길드 제거 전 DM.
  - 권한: 경고·타임아웃=admin·support / 추방·차단·차단해제=admin. 전부 `mod_log.record()` 적재. `main.py` 등록.

**검증:** stdlib sqlite3로 v3 + 경고 추가/집계/철회 검증. compileall 통과. discord.py 2.3 API 시그니처(timeout/kick/ban/unban) 확인. (실제 멤버 상태변경·DM·임베드 e2e는 스테이징.)

**주의:**
- 봇 길드 권한(**Moderate/Kick/Ban Members**)은 **배포 T8에서 부여 필요** — 미부여 시 Forbidden 안내로 안전 실패.
- 경고 임계 자동 에스컬레이션 = 미도입(1차 수동 권고, moderation.md §2·§5).

**다음 세션 착수 후보:**
1. ✅ **T17 템플릿 신설 구현 완료(2026-06-09)** — `/서버신설 자동생성`이 카테고리+채널+접근역할 전개(§11·server_lifecycle §3). 후속: 생성된 약관/인증 채널을 온보딩 패널에 배선(레지스트리에 채널ID 영속화 필요).
2. **RPG auth 이관**(DL-138) — RPG를 `ONBOARDING_SERVERS` 등록 + `modules/rpg/auth.py` 구 흐름 정리.
3. **B: master 동기화** — 합본 원본(`porong-server`)에서 수행(디스코드 워크트리 불가).

### 이번 세션 완료 (2026-06-09, feature/discord-dev) — mod_log 운영로그 인프라
moderation/admin/lifecycle 공통 선행(§12.2)인 운영/감사 로그 인프라를 구현. 붙은 효과: `/서버신설`·`/서버시작`·`/서버종료` 전이가 mod_log + `#운영로그`에 남음.

**구현 산출:**
- **v2 마이그레이션** `core/db.py` `_MIGRATIONS[1]` = `mod_log` 테이블(§2.5) + 인덱스(target/action/created). append-only 규약 준수(v1 미변경).
- **`core/mod_log.py`** = `record(bot, *, action, operator_id, target_id, reason, detail)` — ① mod_log 적재(보장) ② `#운영로그`(`CHANNEL_MODLOG_ID`) 임베드 게시(best-effort, 미설정/실패여도 적재). `_ACTION_META` action 라벨 매핑. `created_at`=`strftime('%s','now')`(DB측 시각).
- **배선** `modules/server_lifecycle/commands.py` — 신설(`server_create`)·시작(`server_start`)·종료(`server_end`, reason 포함) 전이에 `record()` 호출.
- **설정** `core/config.CHANNEL_MODLOG_ID`(0=게시 생략) + `.env.example`.

**검증:** stdlib sqlite3로 v1·v2 executescript + mod_log INSERT(strftime) + 인덱스 생성 확인. `compileall` 통과. (aiosqlite 미설치 환경이라 봇 e2e는 스테이징 대상.)

**주의:** `warnings`(§2.4)는 T15 모더레이션 모듈과 함께 **v3**로 추가 예정(이번엔 mod_log만). moderation/admin 명령은 raw INSERT 금지, `mod_log.record()`만 호출.

**다음 세션 착수 후보:**
1. **moderation(T15) 본체** — v3(`warnings`) + `/경고`·`/경고목록`·`/경고취소`·`/타임아웃`·`/추방`·`/차단`. 대상 보호 가드(§1b) + `mod_log.record()` 재사용.
2. **RPG auth 이관**(DL-138) — RPG를 `ONBOARDING_SERVERS` 등록 + `modules/rpg/auth.py` 구 흐름 정리.
3. **B: master 동기화** — 합본 원본(`porong-server`)에서 수행(디스코드 워크트리 불가).

### 이번 세션 완료 (2026-06-08~09, feature/discord-dev) — 인증·온보딩·DB·서버레지스트리 구현 패스
설계만이던 영역을 **실코드로 구현**. 상태변경 명령은 사용자 명시 요청받아 진행. 핵심 흐름:
포로몬 인증(verify) → 공통 온보딩(약관게이트+버튼/모달) → SQLite 계층(core/db.py) → 서버 레지스트리/생애주기(T21).

**구현 산출:**
- **포로몬 인증** `integrations/poromon_api.py` `verify_code`(POST /auth/verify, X-Api-Key, {code,discordId} → 200{ok,uuid,name}/404/429/401).
- **공통 온보딩** `modules/onboarding/panels.py` — 약관동의 버튼(접근→인증전 역할) + 인증 버튼/모달 → verify → 플레이어 승급. 서버 레지스트리 `core/config.ONBOARDING_SERVERS`(포로몬 등록). `main.py` 전역 `on_app_command_error` 추가.
- **DB 계층(T12)** `core/db.py` — aiosqlite 단일커넥션 + 쓰기 lock + `schema_meta` 증분 마이그레이션. v1 = `servers` 테이블. `BOT_DB_PATH`. `bot.db` 배선.
- **서버 레지스트리/생애주기(T21)** `core/servers.py` + `core/gating.py` + `modules/server_lifecycle/commands.py` — `/서버목록`·`/서버정보`·`/서버신설`·`/서버시작`(prep→active)·`/서버종료`(active→ended) + 카테고리 가시성 + 게이팅 3층 헬퍼.

**확정/정정:**
- 코드방향 통일(인게임 발급→봇 검증) = RPG **DL-138** 동기화 완료. RPG가 `/인증`+`/auth/verify` 실구현(레퍼런스 계약). 닉입력 폐기, name 반환.
- **매핑 비저장** 확정(권위=게임서버, data_model §3) — uuid/name 은 표시/로깅용, 봇 DB 미저장.
- DL 번호 132→**138** 정정(브랜치 선점 충돌, 전 브랜치 확인 후 부여).

**주의/함정 기록:**
- `.gitignore` `server/`(런타임)에 걸려 모듈 폴더는 `modules/server_lifecycle/`로 명명(루트 .gitignore 수정 불가).
- master 머지는 **합본 원본**(`porong-server`)에서(worktree_policy). 디스코드 워크트리에서 강행 시 루트 CLAUDE.md가 rpg용으로 덮어써짐 → **B(master 동기화) 미완**, 합본 원본에서 수행 필요.

**다음 세션 착수 후보:**
1. **mod_log 인프라**(§12.2 순서) — v2 마이그레이션 + `#운영로그` 게시 헬퍼. 붙이면 `/서버시작`·`종료`·`신설` 등 전이가 운영로그에 남음(moderation/admin 공통 선행).
2. **RPG auth 이관**(DL-138 지정) — RPG를 `ONBOARDING_SERVERS`에 등록(verify 엔드포인트 준비됨) + `modules/rpg/auth.py` 구 흐름(닉 모달·create_pending·role-queue 폴링) 정리.
3. **B: master 동기화** — 합본 원본에서 `feature/discord-dev`에 origin/master 머지(decision_log 충돌=theirs, 루트 CLAUDE.md 디스코드용 정리).
4. **남은 열린 결정**: 약관동의 DB기록(b) 시점, T17 카테고리 자동생성, 이모지 서버선택 패널, XP 곡선·티켓·FAQ.

> 미push 주의: 세션 종료 시점에 T21 상태전이 2커밋(`ee170d3`·`0919642`)이 origin 미반영일 수 있음 — 다음 세션 시작 시 `git log origin/feature/discord-dev..HEAD` 확인.

### 이번 세션 완료 (2026-06-06~07, feature/discord-dev) — 공통 확장 설계 패스 + 검토
모두 **docs/설계만, 코드 변경 없음**(상태변경 구현은 DL-130 ⑤ 사용자 명시 요청 시). 10커밋:
- `26017b7` 백로그 승격(T12~T21) + data_model.md + server_lifecycle.md
- `ca924df` architecture.md core 계약(db·게이팅 헬퍼) · `f9d06b3` notifications.md T1(인바운드 HMAC·notifier)
- `66292ce` community_level.md(T13, 칭호=코스메틱) · `c64ad8f` moderation.md(T15) · `6df1340` support.md(T16)
- `91f25bb` 남은 설계(T14·T17·T18) + 트래커 🟡 · `a07d00a` 내 검토 4건 수정
- `b4bf9c7` 독립검토(서브에이전트) 반영 — 권한구멍 3건 + 구현가이드 §12

**산출 docs:** data_model · server_lifecycle · community_level · moderation · support (신규) + architecture·notifications·integration_contract·admin·common (확장). 설계 SoT = 각 docs, 트래커 = §0/§9~§12.

**확정 추가 결정:** SQLite(디스코드 측만) · message_content 인텐트 미사용 · LLM/자동언어감지 제외 · 칭호=누적보유+장착1개(역할 아님) · 게이팅 3층(접근역할→카테고리→서버상태) · 인바운드 HMAC+timestamp · 서버 생애주기(종료=완전비활성·아카이브 보존, 새게임=코드/새시즌=데이터, domain당 active 1).

**다음 세션 착수 후보:**
1. ✅ **DL 동기화 완료** — RPG `decision_log.md` DL-138(2026-06-08)로 동기화됨. RPG가 `/인증`+`/auth/verify`까지 실구현(레퍼런스 계약 확정).
2. **구현 착수**(명시 요청 시) — §12.2 순서: 전역 에러 핸들러 → `core/db.py`(T12) → `mod_log` 헬퍼 → 읽기 명령(`/서버목록`·`/레벨`·`/리더보드`)부터. 상태변경 명령은 인터페이스까지.
3. **남은 열린 결정**: XP 곡선 수치, 티켓 동시수/종료방식, FAQ 매칭 전략, 닉prefix 도입여부(1차 보류).

### 이전 세션 완료 (2026-06-05, feature/discord-dev)
설계 라인 정본화 — 모두 **docs/DL만, 코드 변경 없음**. 커밋 단위:
- `c6d4124` DL-130~132 — 봇 구조 정본화(스택 Python, DL-030 SUPERSEDE) · task.md 신설 · 멀티서버 온보딩(공통 디스코드 인증 → 서버별 약관동의 → 서버별 화이트리스트) · 오라클 상시호스팅 · 카테고리 채널구조 + 이모지 서버선택(역할기반 가시성) · 운영자 전용 닉네임 변경.
- `a17f9a2` DL-133 — 서버 구조 토폴로지 · 봇 관여 경계(봇=API 클라이언트, 게임상태 권위 아님) · 포로몬=HTTP API · 알림=게임서버→봇 push.
- (다음 커밋) DL-134 — 봇↔게임서버 통신 계약 명세([`integration_contract.md`]).

### 확정된 핵심 결정 (요약)
- 봇 = 포로서버 중앙제어 허브, Python 3.12/discord.py, 오라클 24h 상시(게임 호스팅 분리).
- 온보딩 = ①공통 디스코드 인증(규칙+닉네임 1회) → ②서버별 약관동의 + 인게임 `/연동` → 서버별 화이트리스트.
- 채널 = 카테고리(공통/RPG/포로몬/기타), 이모지로 서버선택 → 카테고리 접근 역할(가시성). 접근 ≠ 화이트리스트.
- 봇 관여 = API 경유만(DB/파일/임의 RCON 금지). 알림 = push(인바운드 HMAC+timestamp+IP).
- 닉네임 변경 = 운영자 전용(`/닉네임변경`·`/닉네임재입력`).

### 다음 세션 착수 후보 (택1)
1. **`core/notifier` 인터페이스 초안** — 통신 계약 B(push)를 소비. 인바운드 수신 → `(domain,kind)` 라우팅 테이블 → 전송. (T1)
2. **온보딩 상세 시퀀스** — 공통 인증 `core/` 분리(T7) + 이모지 서버선택(T10) + `/연동`→화이트리스트 시퀀스 다이어그램.
3. **운영(admin) 명령 상세** — A-3 운영 API 계약과 짝지어 권한·입력·운영로그 설계. (T3·T11)

### 착수 전 열린 결정 (정해지면 진행 빨라짐)
- ✅ 인바운드 인증: **HMAC+timestamp 확정**(2026-06-06). 상세 = notifications.md ①, T1 설계 반영.
- 포로몬 API 포트·엔드포인트 계약: PoroMonCore(`../../poromon/docs/03_poromoncore/`) 설계 선행 — poromon 워크스페이스 영역.
- 운영 API(A-3) 게임서버 측 엔드포인트: RPG 워크스페이스 협의 필요(여기선 읽기전용).
- 필드보스 폴링 → push 이관 시점.

### 재진입 메모
- 작업 worktree: `poro-work-discord`(디스코드 전용). 브랜치 `feature/discord-dev`.
- 문서 진입점: [`index.md`] → architecture / integration_contract / roles_and_permissions / notifications / domains/*.
- DL 로그: `../../poro-rpg/docs/decision_log.md` DL-130~134.
- **공통 확장 후보는 §9 백로그(DRAFT)** — 승격 시 §0 표로 이동 + DL 동기화(RPG worktree).

## 9. 후보 기능 백로그 (DRAFT — 공통/운영 확장)

> **[STATUS: PROMOTED]** — 2026-06-06 브레인스토밍으로 추린 공통(도메인 비종속) 확장 후보.
> **§0 우선순위 표로 승격 완료** → Tier1/2 = T12~T18. Tier3(초대추적·건의투표)는 아래 **보류** 유지.
> 이 섹션은 승격 항목의 **상세 명세·근거** 보관처. DL(`../../poro-rpg/docs/decision_log.md`) 기록은 RPG worktree에서 동기화 예정(여기선 읽기전용).
> 모듈↔태스크 매핑: community=T13·T14 / moderation=T15 / support=T16 / admin=T17 / common=T18 / 저장계층=T12.

### 9.1 횡단 결정 (이 백로그 전제)
- ✅ **봇 측 영속 저장소 = SQLite** 도입 확정(오라클 인스턴스 로컬). 레벨·칭호·경고·티켓·출석 등 *디스코드 측 상태* 보관. DL-133 경계 위반 아님(봇=디스코드 측 권위).
- ✅ **`message_content` 특권 인텐트 미사용 정책** — 모든 후보를 명령어/패널/이벤트 메타(작성자·채널·voice state)만으로 설계. 현재 봇은 `members`(온보딩, 기존)만 특권 사용, `voice_states`(비특권) 기존 ON.
- ✅ **LLM 미사용** — 챗봇 제외. FAQ 미매칭 시 운영진 문의(티켓)로 폴백.

### 9.2 제외 확정
- ❌ **부적절 언어 자동감지**(현금거래·욕설) — 봇 기능에서 제외. 필요 시 **디스코드 네이티브 AutoMod**(서버 측 차단, 봇 인텐트 불필요)로 운영 설정만. → 운영 문서화 대상, 코드 아님.
- ❌ **LLM 챗봇** — API 비용/환각 부담으로 제외. FAQ→운영진 문의로 대체.

### 9.3 기능 목록 (모듈별 · Tier = 착수 우선순위)

상태: ⬜ 미착수 · 🔴 미설계 · DB = SQLite 사용 · intent0 = 특권 인텐트 추가 없음.

**`modules/community/`** (신설 · DB · intent0)
| 기능 | 설명 | Tier |
|---|---|---|
| 커뮤니티 레벨 | 채팅·음성 활동 XP → 레벨업(내용 미열람, 메시지/voice 이벤트만 집계 + 어뷰징 쿨다운) | 1 |
| 칭호 | 레벨 임계 도달 시 칭호 역할 자동 부여(레벨 연동) | 1 |
| 리더보드 | 커뮤니티 레벨 랭킹 임베드(레벨 테이블 재사용) | 1 |
| 임시 음성채널 | 허브 입장 시 같은 카테고리에 개인 음성방 자동 생성·비면 삭제. 허브=카테고리별 다중(T17 템플릿이 `➕ 음성방 만들기` 생성, 2026-06-09 결정 — community_level §8) | 1 |
| 출석/일일보상 | `/출석` 스트릭 + XP/보상(레벨 연동) | 2 |
| 임시역할 자동만료 | 이벤트 한정 역할 만료 시각 도달 시 자동 회수 | 2 |

**`modules/moderation/`** (신설 · DB · intent0)
| 기능 | 설명 | Tier |
|---|---|---|
| 제재/추방/경고 | 수동 운영명령(타임아웃·킥·밴·경고). `@requires_permission` 보호 | 1 |
| 운영/감사 로그 | 모든 제재 액션 → `#운영로그` 채널 + DB 적재(누가·언제·왜). 제재의 완성 조건 | 1 |

**`modules/support/`** (신설 · intent0 · LLM X)
| 기능 | 설명 | Tier |
|---|---|---|
| 버그제보 | 서버(RPG/포로몬) 선택 → 모달 → 제보 채널/스레드 라우팅 | 1 |
| 티켓/1:1 문의 | 버튼/명령 → 운영진만 보이는 비공개 채널 생성, 종료·아카이브 | 1 |
| FAQ | `/문의`·패널형 자주묻는질문. 미매칭 → "운영진 문의"(티켓 연결) | 1 |

**`modules/admin/`** (기존 스텁 확장 · 운영)
| 기능 | 설명 | Tier |
|---|---|---|
| 서버 on/off | 카테고리 활성/비활성 토글(역할 가시성 일괄 제어) | 1 |
| 서버 템플릿 신설 | `/서버신설` → 카테고리+접근역할+채널+이모지 세트 자동 생성(architecture.md "새 게임 추가 세트" 자동화) | 1 |

**`modules/common/`** (기존 · 확장 후보)
| 기능 | 설명 | Tier |
|---|---|---|
| `/도움말` | 명령어 안내(도메인 그룹) | 2 |
| `/서버상태` | RPG·포로몬 통합 온라인/인원(포로몬 연동 선행) | 2 |
| 접속정보 게시 | 각 게임 카테고리 `접속정보` 채널에 서버 IP·실시간 핑·on/off 상태를 봇이 임베드로 게시·주기 갱신(2026-06-09, T17 템플릿 자리 확보). 게임서버 상태조회(rpg_api/poromon_api `get_server_status`) 선행. IP는 config/.env에서 로드(커밋 금지) | 2 |

**보류(Tier 3)**
| 기능 | 보류 이유 |
|---|---|
| 초대 추적 | members 인텐트는 기존이라 공짜지만 초대 캐시 diff·재시작 레이스 처리 복잡. 추천 보상 확정 시 |
| 건의·투표 게시판 | 가치 중간, 핵심 아님. 여력 시 |

### 9.4 승격 시 선행/주의
- community·moderation·support·admin(템플릿/onoff)은 모두 봇 단독 권위(디스코드 측) → **외부 프로젝트 의존 없음**, SQLite 스키마만 확정하면 상세설계 가능.
- 봇 권한 요구: 템플릿 신설·서버 on/off·제재는 디스코드 길드 권한(Manage Channels/Roles/Members) 필요 → 배포(T8) 시 봇 역할 권한 구성에 반영.
- SQLite 스키마 설계는 data-schema 영역과 연계(레벨·칭호·경고·티켓·출석 테이블).

## 10. 도메인 명령어 가시성 정책 (T20 설계 노트)

> **[STATUS: DRAFT]** — 2026-06-06. "RPG 카테고리에선 포로몬 명령 안 보임" 요구의 디스코드 현실과 해법.

### 디스코드 한계 (확인된 사실)
- 슬래시 명령 가시성 기준 = **역할 / 권한 / 채널**. **카테고리 네이티브 기준 없음.**
- 진짜 "안 보이게"는 **봇 토큰으로 동적 제어 불가** — 운영자 Integrations 설정 또는 OAuth(Manage Guild) 필요.
- `default_member_permissions`는 **권한 비트만** 게이팅(임의 역할 불가), 전역(채널/카테고리 단위 아님).
- 현행 봇: 모든 명령 전원 노출 + `requires_permission` **런타임 거부**만(가시성 제어 없음).

### 채택 모델 — 2층 조합
- **1차(가시성) 접근역할 제한:** 도메인 명령(예 포로몬)을 `포로몬접근` 역할에게만 노출.
  운영자가 Integrations에서 1회 설정(또는 가능 범위에서 default_permissions). → 접근 안 한 유저는 목록에서 안 보임.
- **2차(문맥) 카테고리 가드:** 명령 실행 시 `interaction.channel.category_id` 검사 → 엉뚱한 카테고리면
  ephemeral 거부 안내. `core/`에 공통 헬퍼(`requires_category(domain)`)로 구현. 봇 동적·카테고리 단위.
- 도메인 격리: 각 도메인 명령은 자기 모듈에. T19(클래스 RPG 이관)와 같은 맥락.

### 남은 엣지케이스 / 열린 결정
- **RPG·포로몬 둘 다 접근한 유저**: 양쪽 명령이 다 보임 → "RPG 채널에서 포로몬 명령 안 보임"은
  per-channel 명령권한 **수동 설정** 없이는 불가.
  - ✅ **결정(2026-06-06): 2차 카테고리 가드(못 쓰게)로 충분.** per-channel 수동설정은 도입 안 함.
    둘 다 접근한 유저가 엉뚱한 카테고리에서 호출하면 ephemeral 거부 안내로 처리.

## 11. 서버 생애주기 / 레지스트리 (T21 설계 노트)

> **[STATUS: DRAFT]** — 2026-06-06. 전제: 서버 증가 100% + **시즌제 운영 → 서버 종료도 100%**.
> 운영자 토글로 시작/종료, 종료 서버는 명령·토글 사용불가.
> **상세 설계(상태머신·운영명령·전이액션·게이팅·런북) = [`server_lifecycle.md`](server_lifecycle.md).** 아래는 요약.

### 핵심 구분 — 추가 방식 두 축
- **새 게임 종류**(RPG/포로몬/신규): 명령어 = 코드 → **모듈 추가(코드 업데이트) 불가피.** 그때마다 추가가 맞음.
- **같은 게임 새 시즌**(RPG 시즌2·3…): 명령 동일, 메타만 변경 → **데이터(레지스트리)로 토글.** 시즌마다 코드 수정 금지(운영부담 누적).

### 서버 레지스트리 (T12 SQLite 포함)
```
servers: { id, domain, season_no, display_name, state(준비|활성|종료),
           category_id, access_role_id, api_env_key, created_at, ended_at }
```

### 상태머신
- `준비(prep)` → `활성(active)` → `종료(ended)`. 운영자 명령/토글로 전이.
- 운영 명령(admin, `requires_permission`): `/서버시작` `/서버종료` `/서버목록`.
- 종료 동작: 카테고리 숨김/아카이브 + 해당 서버 명령·토글 **차단**. (시즌 역할 처리 = 회수 vs 보존, 결정 필요)

### 게이팅 3층 (§10 T20 확장)
1. 접근역할(가시성) · 2. 카테고리 가드(문맥) · 3. **서버 상태** — `종료`면 1·2 통과해도 전부 거부.

### 연계
- T17(서버 on/off·템플릿 신설)은 이 생애주기의 일부로 흡수 — on/off = 상태 전이, 템플릿 신설 = `준비` 행 생성 + 카테고리 생성.
- 새 시즌 = 레지스트리 행 추가 + 카테고리 템플릿(T17) + `/서버시작`. 코드 변경 0.

### T17 템플릿 신설 — 디스코드 역량 조사 + 방향 (2026-06-09, 사용자 아이디어 [DRAFT])
> 사용자 질문: "봇에 디스코드 서버 템플릿을 저장해두고 '서버생성'을 누르면 자동 생성되는가?"

- ❌ **통째 새 디스코드 길드(서버) 생성 = 비권장.** Discord API에 길드 생성(`POST /guilds`)·길드 템플릿 적용(`/guilds/templates/{code}`)이 있으나 **봇이 10개 미만 길드에 있을 때만** 허용(Discord 제약). 봇 생성 길드는 유저 재모집·초대 흐름 재구축 부담 → **단일 허브 길드 + 게임별 카테고리** 구조와 불일치.
- ✅ **기존 길드 내 "게임/시즌 세트" 자동 생성 = 가능 (채택 방향 = T17).** 카테고리 + 채널들 + 접근역할(+온보딩 약관/인증 채널)을 봇이 `create_category`/`create_text_channel`/`create_role` 로 한 번에 생성(`Manage Channels/Roles` 권한). "봇에 저장한 템플릿" = 코드/설정에 채널·역할 구조를 **템플릿 정의**해두고 `/서버신설` 시 그대로 전개.
- **구현 그림:** 현 `/서버신설`(레지스트리 prep 행만 생성) → T17에서 템플릿 인자(또는 도메인 기본 템플릿) 받아 카테고리/채널/역할 세트 생성 후 그 ID들을 servers 행(category_id·access_role_id)에 기록. 생성도 `mod_log.record(action="server_create", detail=...)`에 남김. 멱등성(중복 생성 방지)·실패 롤백(부분 생성 정리) 주의.
- 분류: 기존 T17을 **사전정의 템플릿 방식으로 구체화**.
- 🟢 **구현 완료(2026-06-09): `modules/server_lifecycle/templates.py` `provision()`/`apply_visibility()`/`cleanup()` + `/서버신설 자동생성`.** 상세 = server_lifecycle.md §3.
- ✅ **구조 결정(2026-06-09): B안 프리픽스 카테고리 4그룹 + 온보딩 3역할 자동 생성.** 디스코드 카테고리 중첩 불가 → `<표시명> · 온보딩/정보/커뮤니티/지원·음성` 4개가 나란히. **중첩 시즌/중첩 서버 미운영**(domain당 active 1, 기존 결정과 정합) 전제라 프리픽스 충돌 없음. v4 마이그레이션(`pending_role_id`·`player_role_id` + `server_categories`), 게이팅 2층 집합 매칭(`get_active_category_ids`)으로 확장. 가시성=약관→접근/인증→인증전/그외→플레이어.
- 후속: 약관/인증 채널 온보딩 패널 배선(역할 전이 흐름 T7/T9 — 역할은 생성됨), 접속정보 라이브 상태(T18), FAQ/문의 지원(T16).
- DL 동기화: 디스코드 워크트리 읽기전용 → RPG worktree에서 후속(기존 T17 설계의 구현이므로 새 DL 불필요 판단).

### 확정 결정 (2026-06-06)
- ✅ **종료 서버 = 완전 비활성.** 조회성 명령 포함 모든 명령·토글 차단(과거 시즌 기록 조회도 봇 명령으론 불가).
- ✅ **시즌 자산 = 아카이브 보존.** 시즌 카테고리·역할은 삭제하지 않고 아카이브(숨김) 처리해 기록·구조 보존.
  - 양립: 디스코드 카테고리/역할은 남되(채팅 히스토리·구조 보존), 봇 명령·토글은 완전 차단.
  - 함의: 종료 시즌이 누적됨 → 아카이브 카테고리 정리 정책(예: N시즌 경과분 수동 정리)은 운영 가이드에서 별도 결정.

## 12. 설계 검토 결과 / 구현 가이드 (2026-06-07, implementation-reviewer)

> 설계 패스(T12~T21) 독립 검토. 🔴 = 설계 반영 완료(아래 docs), 🟡 = 구현 시 선행/주의.

### 12.1 설계 반영 완료 (🔴)
- **`/역할부여` 권한상승 차단** — `PERMISSION_ROLE_IDS` 소속 역할 부여 거부([`domains/admin.md`](domains/admin.md) 보안원칙).
- **제재 대상 보호** — operator 이상 권한자·Owner·봇상위 멤버 제재 거부([`moderation.md`](moderation.md) §1b).
- **domain당 active 1개 강제** + 1차 멀티시즌 미사용([`data_model.md`](data_model.md) §2.1).

### 12.2 구현 선행 / 주의 (🟡)
- **전역 app command 에러 핸들러 선행**: 현행 `main.py`에 `on_app_command_error` 부재 → `requires_permission`·`requires_category` 거부가 유저에게 안내 안 됨. moderation/admin 착수 전 필수.
- ✅ **`mod_log`+`#운영로그` 게시 헬퍼** 구현 완료(2026-06-09): v2 마이그레이션(`mod_log` 테이블) + `core/mod_log.py` `record()`(DB 적재 보장 + `#운영로그` best-effort 게시) + `CHANNEL_MODLOG_ID`. 서버 생애주기(신설·시작·종료) 배선됨. moderation/admin 명령은 이 헬퍼만 호출.
- **영구 View 영속화**: 신규 패널(서버선택·`/칭호`·티켓·FAQ)은 `auth.py` `TermsView`처럼 stable custom_id로 `add_view` 재등록 필수. 페이지네이션(`/리더보드`·`/도움말`)은 ephemeral timeout View로 영속화 회피.
- **인바운드 aiohttp**: `web.run_app()` 금지(루프 충돌) → `AppRunner`+`TCPSite`를 봇 루프 task로 기동.
- **명령 sync**: 단일 길드는 `tree.sync(guild=...)`로 즉시 반영(전역 sync는 최대 1h).
- **aiosqlite 쓰기 직렬화**: 단일 커넥션에 여러 코루틴(on_message XP·voice tick·temp_roles tick·명령) 동시 쓰기 → statement autocommit 또는 쓰기 lock. on_message XP는 쿨다운 메모리 1차 판정 후에만 DB write(핫패스 방어).
- **인바운드 계약 갭**(notifications ① 보강): 봇 다운 중 push 유실 = **게임서버 재시도/실패허용 책임**. 미등록 `(domain,kind)`는 graceful(로깅+200). per-domain 시크릿은 추후 고려.
- **운영명령 상태검증 강제**: 매트릭스가 운영명령에 `requires_server_active`를 안 거니, `/서버시작`을 ended 행에 호출 등은 공용 `assert_state` 헬퍼로 명시 검증.

### 12.3 1차 버전 권장
- **닉 `[LV.nn]` prefix 보류** — 레이트리밋·상위역할 skip·base 파싱 리스크. 1차는 임베드/카드 표시만.
- **서버선택 = reaction 대신 Select/Button(영구 View)** — 리액션 롤 중복/레이스 회피, 가시성 흐름과 정합.
- **멀티시즌 동시운영 미사용** — domain당 active 1 강제. `api_env_key` 동적 베이스URL은 시즌 중첩 필요 시.
- **게이팅 판정부 순수 함수화** `(category_id, registry_snapshot)->bool` — discord 없이 단위 테스트.
- **상태변경 명령은 인터페이스까지**(DL-130 ⑤), 읽기 명령(`/서버목록`·`/레벨`·`/리더보드`·`/유저조회`) 먼저 구현·검증.

### 12.4 stdlib 단위 테스트 가능(실길드 불필요)
- 마이그레이션 러너 · XP→레벨 곡선 · HMAC+timestamp+idempotency · 게이팅 판정부 · 라우팅 lookup · 권한/역할부여 가드. 그 외(역할·닉·채널·View·voice·제재·인바운드 e2e)는 실길드/스테이징.
