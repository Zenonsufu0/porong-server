# 봇 ↔ 게임서버 통신 계약 (Integration Contract)

> **[STATUS: ACTIVE]** — 봇과 게임서버(RPG·포로몬) 사이 통신 계약의 단일 진실원.
> DL-133(통신 패턴)을 엔드포인트/페이로드 수준으로 구체화. 구조는 [`architecture.md`],
> 알림 라우팅은 [`notifications.md`], 역할/온보딩은 [`roles_and_permissions.md`].
> 상태: 🟢 구현 · 🟡 설계(미구현) · 🔴 미정.
>
> ⚠ 게임서버 측 API의 권위는 각 게임 프로젝트(ZenonRPG / PoroMonCore)에 있다. 이 문서는
> **봇이 기대·호출하는 계약(봇 관점)**을 명세한다. 게임서버 코드는 여기서 수정하지 않는다.

## 0. 두 방향

| 방향 | 용도 | 트리거 | 코드 |
|---|---|---|---|
| **A. 봇 → 게임서버** | 인증·조회·운영명령 | 봇이 요청(요청-응답) | `integrations/*_api.py` |
| **B. 게임서버 → 봇** | 이벤트 알림 | 게임서버가 push | `core/`(인바운드 수신) → `core/notifier` |

핵심 원칙(DL-133): **봇은 게임 상태의 권위가 아니다.** 모든 상태 변경은 게임서버가 적용하고,
봇은 정의된 엔드포인트로 **요청/트리거**만 한다. DB·파일·임의 RCON 직접 접근 금지.

---

## A. 봇 → 게임서버 (요청-응답 API)

### A-0. 공통 규약
- **전송:** JSON over HTTP(S). 도메인별 베이스 URL — RPG `PORONG_API_URL`, 포로몬 `POROMON_API_URL`(신규).
- **인증:** `X-Api-Key: <서버별 키>` 헤더(`.env`). `Content-Type: application/json`.
- **에러:** HTTP status + `{error, message}`. 4xx → 사용자에게 정중한 메시지(ephemeral), 5xx → 재시도/실패무시 후 로깅.
- **상태변경 멱등성:** 운영 명령(골드지급·화이트리스트 등)은 재요청 안전성(idempotency key) 고려.

### A-1. 인증 / 온보딩 — RPG 🟢 (구현)
클라이언트: `integrations/rpg_api.py`.

| 메서드 | 엔드포인트 | 요청 | 응답 |
|---|---|---|---|
| `verify_code` 🟢 | `POST /auth/verify` | `{code, discordId}` (헤더 `X-Api-Key`) | 200 `{uuid, name}` / 404 / 429 / 401 |

> 🟢 **신방향(DL-138 코드방향 통일, 2026-06-09 봇측 구현): `verify_code`** — 인게임 `/인증` 발급 코드를
> 봇이 검증. 봇 클라이언트 = `rpg_api.verify_code`(포로몬과 동일 계약). 온보딩 `_verifiers["rpg"]` 등록.
> RPG 게임서버 `/auth/verify` = DL-138로 준비됨. ⚠ 필드명(`discordId` camelCase)은 레퍼런스 기준 — RPG
> AuthApiHandler 실계약과 다르면 맞출 것(RPG worktree 확인).
> ❌ **구방향 제거됨(2026-06-09, DL-138):** `create_pending`·`get_auth_status`·`poll_role_queue`·
> `acknowledge_role_granted`(`POST /auth/pending`·`GET /auth/status`·`GET /auth/role-queue`·`POST /auth/role-granted`)
> + `modules/rpg/auth.py`(닉 모달·약관 메시지)·`modules/rpg/role_poll.py`(role-queue 폴러) 삭제. 온보딩은
> `modules/onboarding`(약관동의 버튼·인증 모달·verify)로 통일. 화이트리스트는 인게임 `/인증` 성공 시 게임서버가 확정.

### A-2. 조회 — RPG 🟢 (구현)

| 메서드 | 엔드포인트 | 응답 |
|---|---|---|
| `get_field_boss_status` | `GET /field-boss/status` | `[{field_id, status, respawn_minutes, player_count}, …]` |
| `get_player_by_nick` | `GET /player/by-nick/{nick}` | `DiscordCardResponse` |
| `get_island_by_nick` | `GET /island/by-nick/{nick}` | `DiscordCardResponse` |
| `get_boss_by_nick` | `GET /boss-history/by-nick/{nick}` | `DiscordCardResponse` |

> `DiscordCardResponse` = 봇이 임베드로 그대로 렌더하는 카드 페이로드(게임서버가 표시용으로 구성).

### A-2b. 버그제보 — RPG 🟡 (계약 추가, 미구현)
`support.md` §1(T16)이 의존하는 게임서버 측 계약. 현재 `rpg_api.py`에 메서드 없음 → 신규.

| 메서드 | 엔드포인트 | 요청 | 응답 |
|---|---|---|---|
| `create_bug_report` | `POST /bug-report` | `{discord_user_id, title, steps, expected_actual, severity}` | `{id}` (접수번호 `BUG-{id}`) |
| `update_bug_status` | `POST /bug-report/{id}/status` | `{status, operator}` | — |

> 게임 버그 데이터는 **게임 DB**(`bug_report` 테이블)에 저장, 봇 DB 비복제. 포로몬은 `poromon_api` 동형(API 확정 선행).
> ⚠ 게임서버 측 엔드포인트 실제 구현은 RPG 워크스페이스 협의 필요(여기선 봇이 기대하는 계약).

### A-3. 운영 명령 — 🟡 (설계, 미구현)
admin 명령의 게임서버 측 계약. 모두 상태 변경 → **API 경유 + 운영로그 + 권한 보호**(`@requires_permission`).
실구현은 사용자 명시 요청 시(DL-130 ⑤).

| 용도 | 엔드포인트(제안) | 요청 | 권한 | 근거 |
|---|---|---|---|---|
| 골드 지급 | `POST /admin/grant-gold` | `{nick, amount, reason, operator}` | rpg_manager | DL-130, admin.md |
| 닉네임 변경 | `POST /admin/relink` | `{discord_user_id, old_nick, new_nick, reason}` | admin | DL-132 |
| 화이트리스트 정합 | `POST /admin/whitelist` | `{nick, action: add\|remove, reason}` | admin | DL-132 |
| 유저 상세 조회 | `GET /admin/player/{nick}` | — | admin | admin.md |

### A-4. 포로몬 — 🟡 (동일 패턴, 미구현)
`integrations/poromon_api.py`(스텁). **PoroMonCore가 RPG와 동일한 HTTP API 패턴 제공**(DL-133).
인증/조회/운영은 위 A-1~A-3과 동형으로 설계. 구체 엔드포인트·스키마는 PoroMonCore 설계 확정 선행.

| 용도(예시) | 엔드포인트(제안) | 응답 |
|---|---|---|
| 서버 상태 | `GET /server/status` | `{online, tps, …}` |
| 플레이어 요약 | `GET /player/{nick}` | 표시용 카드 |
| 인증/온보딩 | `/auth/*` (동형) | RPG와 동일 계약 |

---

## B. 게임서버 → 봇 (push 이벤트)

### B-0. 인바운드 엔드포인트 규약 — 🟡 (설계)
봇은 경량 HTTP 리스너(aiohttp.web 등)를 discord 루프와 함께 띄운다(DL-133).

- **엔드포인트:** `POST {BOT_INBOUND_BASE}/events` (단일 진입, 이벤트 타입은 봉투로 구분).
- **인증(외부 유입 → 강하게):**
  - `X-Signature: hex(HMAC-SHA256(body, INBOUND_SECRET))` — 본문 서명 검증.
  - `X-Timestamp: <epoch>` — 허용 오차(±N초) 밖이면 거부(리플레이 방지).
  - 추가로 방화벽/보안그룹 **IP 허용**(게임 호스팅 IP만). 시크릿은 `.env`.
- **봉투(envelope) 스키마:**
  ```json
  {
    "domain": "rpg | poromon | common",
    "kind":   "<이벤트 종류>",
    "ts":     1733400000,
    "idempotency_key": "<uuid>",
    "data":   { /* kind별 상세 */ }
  }
  ```
- **응답:** `200 {"ok": true}`. `idempotency_key` 중복 수신은 무시(dedup). 검증 실패 `401`, 스키마 오류 `400`.

### B-1. 이벤트 카탈로그 — 🟡 (설계)
수신 후 `(domain, kind)` → 채널/멘션 라우팅은 [`notifications.md`](notifications.md).

| domain.kind | 멘션 역할 | `data` 스키마 | 비고 |
|---|---|---|---|
| `rpg.field_boss_pre` | `@필드보스알림` | `{field_id, field_name, minutes}` | 등장 N분 전 |
| `rpg.field_boss_spawn` | `@필드보스알림` | `{field_id, field_name}` | 등장 |
| `rpg.season_boss_recruit` | `@시즌보스알림` | `{boss_name, when, info}` | 시즌보스 모집/공지 |
| `common.world_boss` | `@월드보스알림` | `{boss_name, when}` | 서버 공통 |
| `common.maintenance` | `@점검알림` | `{start, eta, reason}` | 점검 안내 |
| `common.update` | `@업데이트알림` | `{version, summary}` | 패치/업데이트 |
| `common.event_start` / `event_end` | `@이벤트알림` | `{event_name, info}` | 이벤트 시작/종료 |
| `poromon.event` | `@포로몬알림` | `{title, info}` | 포로몬 이벤트/공지 |

> **현행 RPG 필드보스는 폴링**(`A-2 /field-boss/status`)으로 구현돼 있다. push 구조 완성 후
> `rpg.field_boss_*` 이벤트로 점진 이관(DL-133). 그때까지 폴링·push 병행 가능.

---

## C. 도메인 적용 현황

| 항목 | RPG | 포로몬 |
|---|---|---|
| 봇→서버 인증/온보딩 (A-1) | 🟢 구현 | 🟡 설계(동형) |
| 봇→서버 조회 (A-2) | 🟢 구현 | 🟡 설계 |
| 봇→서버 운영 (A-3) | 🟡 설계 | 🟡 설계 |
| 서버→봇 push (B) | 🟡 설계(현행 필드보스는 폴링) | 🟡 설계 |

## D. 환경변수 추가 예정 (`.env` / `.env.example` placeholder)
- 포로몬: `POROMON_API_URL`, `POROMON_API_KEY`
- 인바운드 push: `BOT_INBOUND_BASE`(또는 PORT), `INBOUND_SECRET`, `INBOUND_ALLOW_IPS`(선택)
- 실제 추가는 인바운드/포로몬 연동 구현 시(→ [`task.md`](task.md) T1·T4).

## 미확정
- 포로몬 엔드포인트·스키마 구체화 (PoroMonCore `../../poromon/docs/03_poromoncore/` 선행).
- 필드보스 폴링 → push 이관 시점.
- 운영 API(A-3) 실구현 시점·게임서버 측 엔드포인트 합의.
- ✅ 인바운드 인증 = **HMAC-SHA256 + timestamp 확정**(2026-06-06). 검증 순서·`.env` = [`notifications.md`](notifications.md) ①.
