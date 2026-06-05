# 봇 아키텍처 — 모듈 구조와 도메인 격리

> 코드 구조의 단일 진실원. 실제 코드와 어긋나면 이 문서를 갱신한다.

## 계층 구조

```
poro-discord/
  main.py                 엔트리포인트 — EXTENSIONS 목록을 모듈 네임스페이스로 로드
  core/                   봇 공통 인프라 (도메인 비종속)
    config.py             .env 로드 + 역할/채널 ID
    permissions.py        권한/역할 정책 (권한↔알림 분리, requires_permission 데코레이터)
  integrations/           외부 게임 서버 연동 (도메인별 분리)
    rpg_api.py            PoroRPG HTTP API 클라이언트 (구현)
    poromon_api.py        포로몬 연동 (스텁 — 인터페이스만)
  modules/                도메인 명령어/기능 (discord.py Cog)
    common/               게임 비종속 공통 (/핑 등)
    rpg/                  RPG 전용 (auth · player_commands · field_boss · role_poll)
    roles/                역할 선택 (/클래스선택 · /알림설정)
    poromon/              포로몬 전용 (스텁)
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

- **RPG 코드는 포로몬 모듈/클라이언트를 import 하지 않는다. 반대도 마찬가지.**
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
│ 디스코드 봇 (Python/discord.py)            │      │ PoroRPG (Paper, HTTP API :8765)│
│  core(config·permissions·notifier)        │      │ PoroMon (Cobblemon+PoroMonCore)│
│  integrations(rpg_api·poromon_api) ──HTTP──┼─────▶│  ← 게임 상태 권위               │
│  modules(rpg·poromon·roles·admin·event…)  │◀─push─┤  → 이벤트 push                 │
└──────────────┬─────────────────────────────┘      └────────────────────────────────┘
              ▼  디스코드 길드 (역할·카테고리·채널 = 봇 권위)
```

| 컴포넌트 | 권위 | 책임 |
|---|---|---|
| 디스코드 봇(오라클) | **디스코드 측 전부** | 역할·카테고리·채널 가시성, 온보딩 UI, 알림 게시, 조회 임베드, 운영명령 접수 |
| PoroRPG(Paper) | **RPG 게임 상태** | 인증/화이트리스트·전투·영지·보스·경제 — HTTP API 노출 |
| PoroMon(모드) | **포로몬 게임 상태** | 진행·도감·리그 — PoroMonCore가 HTTP API 노출(§통신) |

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
  - 포로몬: **PoroMonCore가 RPG와 동일한 HTTP API 패턴 노출**(DL-133). `poromon_api`가 클라이언트(스텁).
- **게임서버 → 봇 (push)**: 게임 이벤트(보스·점검·이벤트)는 게임서버가 봇 **인바운드 HTTP 수신
  엔드포인트**로 push → `core/notifier`가 채널 라우팅+멘션+전송([`notifications.md`](notifications.md)).
  - 봇은 경량 HTTP 리스너(aiohttp.web 등)를 discord 루프와 함께 띄운다.
  - 인바운드는 외부 유입 → **공유 시크릿/HMAC 서명 + 방화벽/IP 허용** 필수. 시크릿은 `.env`.
  - 현행 RPG 필드보스 폴링은 유지(점진 이관). 신규 알림은 push 우선.

> 엔드포인트·페이로드 수준 명세는 [`integration_contract.md`](integration_contract.md)(DL-134).

## 배포 / 호스팅 (DL-131)

- 봇은 **24시간 상시 운영**이 전제다. 따라서 **게임 서버 호스팅과 분리된
  오라클 클라우드(Oracle Cloud) 인스턴스**에 배포한다.
  - 게임 서버(RPG/포로몬) 재시작·점검이 봇 가동에 영향을 주지 않는다(독립 프로세스·독립 호스트).
  - 봇 → 게임 서버는 `integrations/*_api.py` HTTP 경유로만 통신(네트워크 도달성·방화벽은 배포 시 구성).
  - 비밀정보(`.env`·토큰·API 키)는 인스턴스에만 두고 커밋 금지([`../CLAUDE.md`] §5).
- 운영 방식(systemd 서비스/프로세스 매니저·로그·재기동 정책) 상세는 배포 작업 시 확정(→ [`task.md`](task.md) T8).

## 디스코드 채널/카테고리 구조 (DL-132)

서버를 **카테고리 단위**로 묶고, **역할 기반 가시성**으로 접근을 제어한다.

```
[공통]            서버소개 · 규칙/약관인증 · 서버선택(이모지 패널)   ← 미인증/접속대기 가시
[RPG 카테고리]    RPG-약관 · 접속정보 · 잡담 · 파티모집 · …          ← RPG접근 역할에게만 가시
[포로몬 카테고리] 포로몬-약관 · 접속정보 · …                          ← 포로몬접근 역할에게만 가시
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
  `modules/rpg/`(구현), `modules/poromon/`(TODO). 역할 정의는
  [`roles_and_permissions.md`](roles_and_permissions.md) §A(공통)·§D(서버별).

## 미설계/공백 (TODO)

- `core/notifier.py` — 도메인 무관 알림 디스패처(채널 라우팅 + 멘션 + 트리거 인터페이스).
  현재 알림 로직이 `modules/rpg/field_boss.py` 에 도메인 종속으로 박혀 있음.
  포로몬/이벤트/월드보스/점검 알림을 일관되게 붙이려면 이 계층이 필요.
- 공통 온보딩 계층 분리(공통 디스코드 인증을 `core/`로) — DL-131.
- `integrations/poromon_api.py` 실구현(현재 스텁).
- `modules/admin` · `modules/event` · `modules/poromon` 실구현(현재 스텁, 설계 선행).
