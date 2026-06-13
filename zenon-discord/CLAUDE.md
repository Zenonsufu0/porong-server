# zenon-discord — Zenon 서버 중앙제어 디스코드 봇 작업 지침

> 이 문서는 `zenon-discord/` 작업 영역 전용 지침이다.
> 루트 `../CLAUDE.md`(모노레포 전역 규칙)가 상위 우선이며, 이 문서는 그것을 보완한다.
> 루트 CLAUDE.md / README.md / .gitignore 및 공통 docs 구조는 **수정하지 않는다.**

## 0. 이 작업 영역의 책임

이 영역은 **Zenon 서버 전체 중앙제어 디스코드 봇** 담당이다.

- 봇은 RPG 전용 봇이 아니라 **Zenon 서버 전체 운영 허브**다.
- 실제 게임 로직은 RPG 플러그인(`../zenon-rpg/`) 또는 포로몬 서버(`../zenon-mon/`)에서 처리한다.
- 봇은 **명령어 · 권한 · 알림 · 조회 · 운영자 패널(UI)** 만 담당한다.
- RPG 와 포로몬은 코드 공유가 거의 없으므로 **봇 내부에서도 도메인 모듈을 분리**한다.

### 구현 스택
- 언어: **Python 3.12 / discord.py 2.3** (실제 코드 기준).
  - ⚠️ `docs/domains/rpg.md` §8 DL-030 은 Node.js(Discord.js)로 기재돼 있으나 실제 구현은 Python 이다.
    충돌 시 실제 코드를 기준으로 하고, 마스터 docs 변경은 별도 합의 후 진행한다.

문서 지도(상세는 `docs/index.md`): `docs/architecture.md`(구조) ·
`docs/roles_and_permissions.md`(역할 정책 SoT) · `docs/notifications.md`(알림) ·
`docs/domains/{rpg,poromon,common,admin}.md`(도메인별).

## 1. 수정 범위

수정 가능:
- `zenon-discord/` 내부 전체 (이 폴더)
- (분리돼 있다면) 디스코드봇 전용 문서

읽기만 가능 (수정 금지):
- `../zenon-rpg/`, `../custom-plugins/zenon-rpg/`, `../zenon-mon/`
- 루트 `.gitignore` / `README.md` / `CLAUDE.md`, 공통 docs 구조, `../infra/`
- RPG/포로몬 실제 게임 로직 파일

## 2. 디렉터리 구조 (모듈 분리)

```
zenon-discord/
  main.py                 # 엔트리포인트 — EXTENSIONS 목록을 모듈 네임스페이스로 로드
  core/                   # 봇 공통 인프라
    config.py             # .env 로드 + 역할/채널 ID
    permissions.py        # 권한/역할 정책 (권한↔알림 분리, 권한 데코레이터)
  integrations/           # 외부 서버 연동 (도메인별 분리)
    rpg_api.py            # PorongRPG HTTP API 클라이언트 (구현됨)
    poromon_api.py        # 포로몬 연동 (스텁 — 인터페이스만)
  modules/                # 도메인 명령어/기능 모듈 (Cog)
    common/               # 게임 비종속 공통 (/핑 등)
    rpg/                  # RPG 전용 (auth/player/field_boss/role_poll)
    roles/                # 역할 선택 (/클래스선택, /알림설정)
    poromon/              # 포로몬 전용 (스텁)
    event/                # 이벤트 (스텁)
    admin/                # 운영/관리자 (스텁 — 설계 선행)
  docs/                   # 봇 설계/명세 문서
```

원칙:
- 도메인이 늘면 `modules/<도메인>/` + (필요 시) `integrations/<도메인>_api.py` 를 추가한다.
- 새 Cog 는 `main.py` 의 `EXTENSIONS` 목록에 `modules.<도메인>.<파일>` 형태로 등록한다.
- RPG 코드는 포로몬 모듈을, 포로몬 코드는 RPG 모듈을 직접 import 하지 않는다(도메인 격리).

## 3. 역할 / 권한 정책 (핵심)

`core/permissions.py` + `core/config.py` 에 정의. **권한 역할과 알림 역할을 분리한다.**

### 자동 지급 가능 (알림 역할 — `NOTIFY_ROLE_IDS`)
플레이어가 `/알림설정` 등으로 자유롭게 토글. 어떤 운영 권한도 부여하지 않는다.
- RPG/필드보스 알림, 시즌보스 알림, 월드보스 알림
- 포로몬 알림, 이벤트 알림, 점검/공지(점검·업데이트) 알림

### 수동 지급 전용 (권한 역할 — `PERMISSION_ROLE_IDS`)
운영진이 디스코드에서 **직접** 부여. **봇은 버튼·이모지·자동 로직으로 절대 지급하지 않는다.**
- Owner, Admin
- RPG Manager, Poromon Manager, Event Manager
- Support

### 온보딩 권한 역할 (시스템 자동 승급)
인증 플로우에 따라 봇이 자동 승급 (미인증 → 접속대기 → 인증유저). 운영 권한과 무관.

운영 명령어는 반드시 `permissions.requires_permission("admin", ...)` 로 보호한다.

## 4. 구현 범위 제한 (중요)

- RPG/포로몬 서버와 **직접 연결하는 API 구현은 사용자가 명시할 때만** 한다.
  그 외에는 인터페이스/TODO 수준으로만 남긴다 (`integrations/poromon_api.py` 참조).
- **계정 연동 · 역할 지급 · 관리자 명령어**는 보안 영향이 크다.
  실제 구현 전 **설계 문서 또는 인터페이스부터 제안**한다 (`modules/admin/` 은 현재 스텁).

## 5. 보안 / 비밀정보

- **봇 토큰 · `.env` · 비밀키 · 웹훅 URL · API 키 · DB 자격증명은 절대 커밋하지 않는다.**
- 설정값은 `.env`(gitignored)에서 로드한다. 새 설정은 `.env.example` 에 placeholder 로만 추가한다.
- 코드/문서에 실제 토큰·ID·URL 을 하드코딩하지 않는다.

### 봇이 요구하는 환경변수 (`.env`)
필수: `DISCORD_TOKEN`, `PORONG_API_KEY`, `GUILD_ID`, `CHANNEL_FIELD_BOSS_ID`
DEPRECATED(구 RPG 단일서버 온보딩 — DL-138 폐기, 미참조·optional): `CHANNEL_AUTH_ID`,
`ROLE_접속대기_ID`, `ROLE_인증유저_ID`, `ROLE_미인증_ID`, `TERMS_MESSAGE_ID`
선택(미설정 시 0 → 기능 비활성): `PORONG_API_URL`,
클래스 역할(`ROLE_검사_ID` …), 알림 역할(`ROLE_필드보스알림_ID`, `ROLE_월드보스알림_ID`,
`ROLE_포로몬알림_ID`, `ROLE_이벤트알림_ID`, `ROLE_점검알림_ID`, `ROLE_업데이트알림_ID`),
운영 권한 역할(`ROLE_OWNER_ID`, `ROLE_ADMIN_ID`, `ROLE_RPG_MANAGER_ID`,
`ROLE_POROMON_MANAGER_ID`, `ROLE_EVENT_MANAGER_ID`, `ROLE_SUPPORT_ID`)

## 6. 작업 / 커밋 규칙

- 작고 리뷰 가능한 단위로 작업한다. 변경 전 대상 파일을, 변경 후 변경 내용을 요약한다.
- 커밋 전 점검:
  - `git status --short`
  - `git diff --check`
  - `git diff | grep -Ei 'token|secret|password|webhook|api_key|apikey|discord_token'` → 누출 없음 확인
  - 구문 검증: `python3 -m compileall main.py core integrations modules`
    (discord.py 미설치 환경에서는 import 실행 불가 → `compileall` 로 구문만 검증)
- 커밋 메시지 예: `refactor(discord): …`, `feat(discord): …`, `docs(discord): …`, `chore(discord): …`
- `commit · merge · push 는 사용자 승인 후에만` 실행한다.
- master/main 으로 바로 merge 하지 않는다. merge 전 `git fetch origin` →
  `git diff --name-only HEAD origin/master` 로 충돌 가능성을 먼저 보고한다.
