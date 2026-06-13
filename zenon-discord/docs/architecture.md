# 봇 아키텍처 — 모듈 구조와 도메인 격리

> 코드 구조의 단일 진실원. 실제 코드와 어긋나면 이 문서를 갱신한다.

## 계층 구조

```
zenon-discord/
  main.py                 엔트리포인트 — EXTENSIONS 목록을 모듈 네임스페이스로 로드
  core/                   봇 공통 인프라 (도메인 비종속)
    config.py             .env 로드 + 역할/채널 ID
    permissions.py        권한/역할 정책 (권한↔알림 분리, requires_permission 데코레이터)
  integrations/           외부 게임 서버 연동 (도메인별 분리)
    rpg_api.py            ZenonRPG HTTP API 클라이언트 (구현)
    zenon_mon_api.py        Zenon Mon 연동 (스텁 — 인터페이스만)
  modules/                도메인 명령어/기능 (discord.py Cog)
    common/               게임 비종속 공통 (/핑 등)
    rpg/                  RPG 전용 (auth · player_commands · field_boss · role_poll)
    roles/                역할 선택 (/클래스선택 · /알림설정)
    poromon/              Zenon Mon 전용 (스텁)
    event/                이벤트 (스텁)
    admin/                운영/관리자 (스텁 — 설계 선행)
  docs/                   설계/명세 문서
```

## 계층 책임

| 계층 | 책임 | 규칙 |
|---|---|---|
| `core/` | 설정·권한 등 봇 전역 공통 | 특정 게임 도메인을 알지 못한다 |
| `integrations/` | 외부 서버와의 통신(HTTP API 등) | 도메인당 클라이언트 1개. discord 객체를 다루지 않는다 |
| `modules/<도메인>/` | 슬래시 명령어·버튼·모달·알림 Cog | 자기 도메인 + core + 자기 도메인 integration 만 의존 |

## 도메인 격리 원칙

- **RPG 코드는 Zenon Mon 모듈/클라이언트를 import 하지 않는다. 반대도 마찬가지.**
  공통이 필요하면 `core/` 로 올린다.
- 새 도메인 추가 시: `modules/<도메인>/` + (필요 시) `integrations/<도메인>_api.py` 를 만들고
  `main.py` 의 `EXTENSIONS` 에 `modules.<도메인>.<파일>` 을 등록한다.
- 멀티 서버 정보 보유: 각 도메인 integration 클라이언트가 자기 서버 정보를 들고 온다.
  봇은 디스코드 측 단일 허브로서 이를 명령어/임베드로 노출한다.

## 확장 로딩

`main.py` 의 `EXTENSIONS` 리스트가 로드 대상이다. 활성 모듈과 스텁 모듈을
같은 메커니즘으로 로드하되, 스텁은 명령어 없는 빈 Cog 다(구조 검증용).

## 서버 구조 / 봇 관여 경계 / 통신 패턴 (DL-133)

### 토폴로지

```
┌─ 오라클 클라우드 (24h, 게임 호스팅 분리) ─┐      ┌─ 게임 호스팅 (별도) ───────────┐
│ 디스코드 봇 (Python/discord.py)            │      │ ZenonRPG (Paper, HTTP API :8765)│
│  core(config·permissions·notifier)        │      │ Zenon Mon (Cobblemon+ZenonMonCore)│
│  integrations(rpg_api·zenon_mon_api) ──HTTP──┼─────▶│  ← 게임 상태 권위               │
│  modules(rpg·poromon·roles·admin·event…)  │◀─push─┤  → 이벤트 push                 │
└──────────────┬─────────────────────────────┘      └────────────────────────────────┘
              ▼  디스코드 길드 (역할·카테고리·채널 = 봇 권위)
```

| 컴포넌트 | 권위 | 책임 |
|---|---|---|
| 디스코드 봇(오라클) | **디스코드 측 전부** | 역할·카테고리·채널 가시성, 온보딩 UI, 알림 게시, 조회 임베드, 운영명령 접수 |
| ZenonRPG(Paper) | **RPG 게임 상태** | 인증/화이트리스트·전투·영지·보스·경제 — HTTP API 노출 |
| Zenon Mon(모드) | **Zenon Mon 게임 상태** | 진행·도감·리그 — ZenonMonCore가 HTTP API 노출(§통신) |

### 봇 관여 경계 — 봇은 게임 상태의 "권위"가 아니라 "클라이언트"

게임에 영향을 주는 모든 동작은 **게임서버가 노출한 정의된 API 엔드포인트로만** 한다.

| 봇이 한다 (IN) | 봇이 안 한다 (OUT — 게임서버 권위) |
|---|---|
| 디스코드 역할/카테고리/채널 가시성 관리 | 게임 로직 계산(전투·드랍·경제·강화)¹ |
| 온보딩 구동(인증 UI·닉네임 수집·이모지 선택) | 게임 DB 직접 읽기/쓰기 |
| 화이트리스트 **요청**(API 호출 → 게임서버가 적용) | 게임서버 파일·화이트리스트 직접 수정 |
| 게임서버 API 조회 → 임베드 노출 | 게임 내 임의 RCON/명령 직접 실행 |
| 알림 게시(게임 상태 → 디스코드 채널) | 게임 세션/인스턴스 직접 제어 |
| 운영명령 **트리거**(admin → 게임서버 API) | |

¹ 예외: `/강화계산` 등 **CANON 고정 수치 표시**는 봇 내부 계산 허용(상태 불변·순수 표시, DL-030).

### 통신 패턴

- **봇 → 게임서버 (요청-응답, HTTP)**: 인증·조회·운영명령 트리거. `integrations/*_api.py`.
  - RPG: 구현(`/auth/*`·`/player`·`/island`·`/boss-history`). 인증 역할 부여는 `role-queue` **폴링**(현행).
  - Zenon Mon: **ZenonMonCore가 RPG와 동일한 HTTP API 패턴 노출**(DL-133). `zenon_mon_api`가 클라이언트(스텁).
- **게임서버 → 봇 (push)**: 게임 이벤트(보스·점검·이벤트)는 게임서버가 봇 **인바운드 HTTP 수신
  엔드포인트**로 push → `core/notifier`가 채널 라우팅+멘션+전송([`notifications.md`](notifications.md)).
  - 봇은 경량 HTTP 리스너(aiohttp.web 등)를 discord 루프와 함께 띄운다.
  - 인바운드는 외부 유입 → **공유 시크릿/HMAC 서명 + 방화벽/IP 허용** 필수. 시크릿은 `.env`.
  - 현행 RPG 필드보스 폴링은 유지(점진 이관). 신규 알림은 push 우선.

> 엔드포인트·페이로드 수준 명세는 [`integration_contract.md`](integration_contract.md)(DL-134).

## 배포 / 호스팅 (DL-131)

- 봇은 **24시간 상시 운영**이 전제다. 따라서 **게임 서버 호스팅과 분리된
  오라클 클라우드(Oracle Cloud) 인스턴스**에 배포한다.
  - 게임 서버(RPG/Zenon Mon) 재시작·점검이 봇 가동에 영향을 주지 않는다(독립 프로세스·독립 호스트).
  - 봇 → 게임 서버는 `integrations/*_api.py` HTTP 경유로만 통신(네트워크 도달성·방화벽은 배포 시 구성).
  - 비밀정보(`.env`·토큰·API 키)는 인스턴스에만 두고 커밋 금지([`../CLAUDE.md`] §5).
- 운영 방식(systemd 서비스/프로세스 매니저·로그·재기동 정책) 상세는 배포 작업 시 확정(→ [`task.md`](task.md) T8).

## 디스코드 채널/카테고리 구조 (DL-132)

서버를 **카테고리 단위**로 묶고, **역할 기반 가시성**으로 접근을 제어한다.

```
[공통]            서버소개 · 규칙/약관인증 · 서버선택(이모지 패널)   ← 미인증/접속대기 가시
[RPG 카테고리]    RPG-약관 · 접속정보 · 잡담 · 파티모집 · …          ← RPG접근 역할에게만 가시
[Zenon Mon 카테고리] Zenon Mon-약관 · 접속정보 · …                          ← 포로몬접근 역할에게만 가시
[기타 카테고리]   (확장)                                              ← 기타접근 역할에게만 가시
```

- **카테고리별 약관 채널**을 둔다. 카테고리 가시화는 §A-2 카테고리 접근 역할(이모지)로,
  실제 화이트리스트는 그 안 약관 채널의 동의 + 인게임 인증으로(§D) 부여한다.
- 새 게임/서버 추가 = `카테고리 1개 + <도메인>접근 역할 + 선택 이모지 + (도메인 모듈)` 세트 추가.
- 채널/카테고리/이모지 ID 는 `core/config.py` 에 두고 `.env` 로 주입(하드코딩 금지).
- 역할 정의·정책은 [`roles_and_permissions.md`](roles_and_permissions.md) §A-2·§D.

## 공통 온보딩 vs 서버별 화이트리스트 (DL-131)

- **공통 디스코드 인증**(규칙/약관 동의 + 닉네임 1회)은 **도메인 비종속**이다 → 논리적으로 `core/`
  또는 공통 온보딩 모듈에 속한다. 현재는 RPG 전용 경로(`modules/rpg/auth.py`)에 있어,
  멀티서버 확장 시 공통 계층으로 분리가 필요(→ [`task.md`](task.md) T7).
- **서버별 약관동의 + 인게임 인증 + 화이트리스트 등록**은 **도메인별**이다 →
  `modules/rpg/`(구현), `modules/zenon_mon/`(TODO). 역할 정의는
  [`roles_and_permissions.md`](roles_and_permissions.md) §A(공통)·§D(서버별).

## core 내부 계약 — DB 접근 + 게이팅 (T12·T20·T21)

> 봇 영속화·게이팅을 도메인 모듈에 흩뿌리지 않기 위한 `core/` 계약. 스키마 = [`data_model.md`](data_model.md),
> 생애주기 = [`server_lifecycle.md`](server_lifecycle.md), 가시성 정책 = [`task.md`](task.md) §10. 인터페이스 단계(구현 전).

### `core/db.py` — 비동기 SQLite 접근 계층
- **`aiosqlite`** 단일 커넥션(또는 경량 풀). 봇 기동 시 1회 연결 + 마이그레이션, 종료 시 close. 동기 `sqlite3` 금지(루프 블로킹).
- 책임: 연결 수명관리 · 마이그레이션 실행 · 쿼리 헬퍼. **도메인 모듈은 raw SQL 을 직접 쓰지 않고** db 함수(또는 테이블별 리포지토리 함수) 경유.
- 인터페이스(시그니처 수준):
  - `async def init_db(path)` — 연결 + 마이그레이션까지.
  - 저수준 `execute / fetchone / fetchall`, 또는 테이블별 함수(`servers_*`, `community_*`, `mod_log_*`, `ticket_*` …).
  - 대부분 테이블이 도메인 비종속(레벨·경고·티켓·레지스트리)이라 **`core/` 집약이 자연스럽다**. 도메인 격리는 "타 도메인 *명령 모듈* 비import" 기준이지 db 공유는 위반 아님.
- **마이그레이션:** `migrations` 리스트(버전→SQL), 기동 시 `schema_meta.version` 읽고 순차 적용. **전진 전용**(롤백 없음 — 시즌제·단일 인스턴스라 단순 유지).

### 게이팅 헬퍼 — 문맥/상태 게이트 (운영권한과 별개)
기존 `requires_permission`(운영 권한 역할)과 **별개**의 두 게이트를 `core/`(permissions.py 또는 gating.py)에 둔다:
- **`requires_category(domain)`** — `interaction.channel.category_id == servers[domain].category_id` 아니면 ephemeral 거부. (가시성 2층)
- **`requires_server_active(domain)`** — `servers` 에 `domain` 의 `active` 행 없으면 거부. (상태 3층, 종료 서버 차단)
- 레지스트리는 자주 안 바뀜 → **메모리 캐시 + 상태 전이 시 무효화**(매 명령 DB 조회 회피).

### 적용 매트릭스 — 어느 게이트를 어디에
| 명령 유형 | requires_permission | requires_category | requires_server_active |
|---|---|---|---|
| 운영(서버시작/종료·제재) | ✅ | (운영로그 채널 한정 검토) | ✖ — 종료 서버도 운영·조회는 가능해야 함, 명령 내부에서 상태 검증 |
| 도메인 플레이어 명령(/프로필 등) | ✖ | ✅ | ✅ |
| 알림 토글(도메인) | ✖ | (해당 카테고리 한정 검토) | ✅ — 종료 서버 알림 토글 차단 |
| 공통(/핑·레벨·출석) | ✖ | ✖ | ✖ — 도메인 무관 |

> 핵심: `requires_server_active` 는 **유저 대상 도메인 명령에만**. 운영 명령엔 걸지 않는다(종료 서버 정리·조회 필요) — 대신 명령 내부에서 상태를 직접 검증.

## 신규 설정 키 집계 (구현 시 `core/config.py` + `.env.example` placeholder)

> 이번 설계 패스(T12~T21)가 도입한 신규 키 모음. **실제 값은 `.env`(gitignored), `.env.example`엔 placeholder만.**
> 채널/역할 ID·시크릿 하드코딩 금지. 구현 착수 시 이 목록을 config에 일괄 반영한다.

| 키 | 용도 | 출처 docs |
|---|---|---|
| `BOT_DB_PATH` | SQLite 파일 경로 | data_model §1 |
| `BOT_INBOUND_BASE`(또는 PORT) · `INBOUND_SECRET` · `INBOUND_ALLOW_IPS` | push 인바운드 수신·HMAC | notifications ① |
| `CHANNEL_NOTICE_ID` | 공지/시즌보스/월드보스/점검/업데이트/이벤트 알림 | notifications ③ |
| `CHANNEL_ZENON_MON_NOTICE_ID` | Zenon Mon 알림 | notifications ③ |
| `CHANNEL_LEVELUP_ID` | 레벨업·칭호 획득 공지 | community_level §3.3 |
| `CHANNEL_MODLOG_ID` | **운영/감사 로그(=#운영로그, 정식 키)** | moderation §3 |
| 임시음성 허브(카테고리별 다중 — 템플릿 생성 `➕ 음성방 만들기`) · `AFK_CHANNEL_ID` · `XP_EXCLUDE_CHANNEL_IDS` | 임시음성 허브·음성 XP 제외 | community_level §1·§8 |
| XP 튜닝(`CHAT_XP_PER_MSG`·`CHAT_XP_COOLDOWN_SEC`·`VOICE_XP_PER_TICK`·`VOICE_TICK_SEC`·곡선 계수) | 레벨 밸런스 | community_level §6 |
| `ZENON_MON_API_URL` · `ZENON_MON_API_KEY` | Zenon Mon 연동 | integration_contract §D |
| 접근역할(`ROLE_RPG접근_ID`·`ROLE_포로몬접근_ID`…) | 카테고리 가시성 | roles §A-2(T10) |

> `#운영로그`는 문서마다 이름으로 불렸으나 **정식 키 = `CHANNEL_MODLOG_ID`** 로 통일. server_lifecycle·admin의 "#운영로그"는 이 키를 가리킨다.

## 미설계/공백 (TODO)

- `core/notifier.py` — 도메인 무관 알림 디스패처(채널 라우팅 + 멘션 + 트리거 인터페이스).
  현재 알림 로직이 `modules/rpg/field_boss.py` 에 도메인 종속으로 박혀 있음.
  Zenon Mon/이벤트/월드보스/점검 알림을 일관되게 붙이려면 이 계층이 필요.
- 공통 온보딩 계층 분리(공통 디스코드 인증을 `core/`로) — DL-131.
- `integrations/zenon_mon_api.py` 실구현(현재 스텁).
- `modules/admin` · `modules/event` · `modules/zenon_mon` 실구현(현재 스텁, 설계 선행).
