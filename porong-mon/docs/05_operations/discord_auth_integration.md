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

## 봇 측 구현(porong-discord, TODO — 이 워크트리에서 수정 금지)
- `integrations/poromon_api.py`(현재 스텁): `verify_code(code, discord_id)` = aiohttp `POST {base}/auth/verify` + `X-API-Key`. base URL/키는 `core.config`.
- `modules/poromon/commands.py`: `/인증코드 <code>`(또는 DM 핸들러) → `poromon_api.verify_code(code, ctx.author.id)` →
  - 성공: "인증 완료" 응답 + **인증 역할 부여**(roles 정책) + (선택) MC UUID 기록.
  - 실패: "코드가 올바르지 않거나 만료되었습니다" 안내.
- 보안: API 키는 봇 secret(`.env`/config)로 관리, 커밋 금지. 봇·MC 같은 호스트면 `bindAddress=127.0.0.1` 유지.

## MC 측 구현 현황(이 저장소, ✅)
- `auth/AuthManager`(코드 발급·검증·허브 감금), `auth/AuthHttpServer`(JDK HttpServer, 무의존), `auth/AuthMenu`(인증하기 GUI).
- `PlayerProgress.discordVerified/discordId`(영속). `/인증`·`/auth` 명령. 미인증 = 메뉴→AuthMenu, 텔레포트 명령 차단, 허브 반경 감금.
- 검증: 헤드리스 — ping·401·404·config 생성·에러0. ⚠️ 알파(플레이어): `/인증` 코드 발급·실제 verify 연결·감금·메뉴 잠금.
