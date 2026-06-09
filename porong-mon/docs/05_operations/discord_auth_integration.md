# 디스코드 인증/화이트리스트 연동 (결정 041)

> 포로몬 모드 서버(PoroMonCore) ↔ 포롱 디스코드 봇(`porong-discord`) 인증 연동 규약.
> MC 측은 PoroMonCore가 구현(이 저장소). **봇 측은 `porong-discord` 워크트리에서 별도 구현**(이 문서가 계약).

## 흐름
1. 플레이어가 MC 접속(포롱 디스코드 멤버 전제). **미인증 상태** = 허브 감금 + 메뉴는 "인증하기"만.
2. 플레이어 `/인증`(또는 `/auth`, 또는 메뉴 "인증하기") → **6자리 코드** 발급(예: `AB12CD`, 기본 10분 유효).
3. 플레이어가 그 코드를 **디스코드 봇**에게 입력(명령/DM).
4. 봇이 MC 인증 HTTP API `POST /auth/verify` 호출(코드 + 디스코드 ID).
5. MC가 코드 검증 → 해당 MC UUID를 **인증 완료**(UUID↔디스코드 ID 연결, 영속) → 봇에 200 응답.
6. MC 내 해당 플레이어(온라인 시) 자동 안내 "인증 완료", 허브 감금/메뉴 잠금 해제.

## MC 인증 HTTP API (PoroMonCore 제공)
- 설정: `config/poromoncore/core.json` → `discordAuth`
  - `enabled`, `bindAddress`(기본 127.0.0.1), `httpPort`(기본 25580), `apiKey`(**반드시 변경**), `codeExpiryMinutes`(10), `confine`(허브 감금), `confineRadius`(100).
- 인증 헤더: `X-API-Key: <apiKey>` (모든 요청).

### `GET /auth/ping`
- 헬스체크 → `200 {"ok":true,"service":"poromon-auth"}`.

### `POST /auth/verify`
- 헤더: `X-API-Key`, `Content-Type: application/json`
- 바디: `{"code":"AB12CD","discordId":"<디스코드 유저 ID>"}`
- 응답:
  - `200 {"ok":true,"uuid":"<MC UUID>"}` — 인증 성공(코드의 MC 계정과 디스코드 ID 연결됨)
  - `404 {"ok":false,"error":"invalid or expired code"}` — 코드 없음/만료
  - `401 {"ok":false,"error":"unauthorized"}` — API 키 불일치
  - `400` — 바디 누락 / `405` — POST 아님

## 봇 측 구현 계획 (porong-discord, TODO — ⚠️ 이 워크트리에서 수정 금지)

> 봇 코드는 **별도 워크트리 `porong-work-discord`(브랜치 `feature/discord-dev`)** 에 있다. 아래는 그 워크트리에서 진행할 **구현 체크리스트**(이 문서가 계약·MC측은 완료).
> 진입 전: `cd /home/zenonsufu1/dev/porong-work-discord` 후 봇 코드베이스(파일/명령 프레임워크: discord.py vs nextcord 등)를 먼저 파악하고 아래 경로명을 실제 구조에 맞춘다(추측 금지).

### B1. 설정/시크릿 (.env)
- [ ] `POROMON_API_BASE=http://127.0.0.1:25580` (봇·MC 동일 호스트 전제. 다른 호스트면 MC `bindAddress`를 해당 IP로 + 방화벽)
- [ ] `POROMON_API_KEY=<MC core.json discordAuth.apiKey와 동일값>` (⚠️ **커밋 금지**, MC측은 런타임 core.json에 이미 주입 — 비커밋)
- [ ] `POROMON_VERIFIED_ROLE_ID=<인증 역할 ID>`
- [ ] (선택) `POROMON_API_TIMEOUT=5`(초)

### B2. API 클라이언트 (`integrations/poromon_api.py` — 현재 스텁)
- [ ] `async verify_code(code: str, discord_id: str) -> dict`:
  - aiohttp `POST {base}/auth/verify`, 헤더 `X-API-Key`, `Content-Type: application/json`, 바디 `{"code","discordId"}`.
  - 응답 매핑: `200`→성공(`uuid` 포함), `404`→코드 무효/만료, `401`→키 불일치(운영 알림), `400/405`→요청 오류. 네트워크/타임아웃→사용자엔 "서버 점검 중" + 로그.
- [ ] (선택) `async ping() -> bool`: `GET /auth/ping` 헬스체크(봇 기동 시 1회 점검).
- [ ] base URL/키는 `core.config`/`.env`에서 로드.

### B3. 명령 (`modules/poromon/commands.py`)
- [ ] `/인증코드 <code>`(슬래시) 또는 DM 코드 핸들러 → `poromon_api.verify_code(code, ctx.author.id)`:
  - **성공**: "✅ 인증 완료" 응답(ephemeral 권장) + **인증 역할 부여**(`POROMON_VERIFIED_ROLE_ID`) + (선택) `uuid` ↔ discord_id 기록.
  - **실패(404)**: "코드가 올바르지 않거나 만료되었습니다. MC에서 `/인증`으로 새 코드를 받아 주세요."
  - **실패(401/기타)**: 사용자엔 일반 오류, **운영 채널/로그엔 상세**(키 불일치는 설정 사고이므로 즉시 알림).
- [ ] 코드 형식 가벼운 검증(6자리 영숫자)으로 오타 즉시 안내(서버 왕복 절약, 선택).

### B4. 보안/운영
- [ ] API 키는 `.env`/secret 관리, 커밋 금지. 봇·MC 동일 호스트면 `bindAddress=127.0.0.1` 유지(외부 노출 0).
- [ ] 다른 호스트 분리 시: MC `bindAddress`=내부 IP + 방화벽으로 봇 호스트만 허용 + (권장) 역방향 프록시 TLS.
- [ ] verify 호출은 **봇만** 가능해야 함(키가 그 보증). 키 유출 시 즉시 교체(MC core.json + 봇 .env 동시).

### B5. 봇측 검증 체크리스트
- [ ] `GET /auth/ping` 200 (봇 호스트에서)
- [ ] 올바른 코드 → 200 + 역할 부여 + MC 플레이어 "인증 완료"·감금 해제(MC측 자동)
- [ ] 만료/오타 코드 → 404 안내
- [ ] 잘못된 키 → 401(운영 알림 동작)
- [ ] 전체 흐름 1회: MC `/인증` 코드 발급 → 디스코드 `/인증코드` → 접속 해제까지(알파)

> MC측 현황(이 저장소, ✅ 헤드리스): ping·401·404·config 생성·에러0. **남은 건 봇측 구현 + 2자 연결 실증(알파).**

## MC 측 구현 현황(이 저장소, ✅)
- `auth/AuthManager`(코드 발급·검증·허브 감금), `auth/AuthHttpServer`(JDK HttpServer, 무의존), `auth/AuthMenu`(인증하기 GUI).
- `PlayerProgress.discordVerified/discordId`(영속). `/인증`·`/auth` 명령. 미인증 = 메뉴→AuthMenu, 텔레포트 명령 차단, 허브 반경 감금.
- 검증: 헤드리스 — ping·401·404·config 생성·에러0. ⚠️ 알파(플레이어): `/인증` 코드 발급·실제 verify 연결·감금·메뉴 잠금.
