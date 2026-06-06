# 알림 시스템

> 봇이 보내는 알림(채널·멘션·트리거)의 정리. 알림 역할 정의는
> [`roles_and_permissions.md`](roles_and_permissions.md) §C.

## 구성 요소

- **알림 채널**: `config.CHANNEL_*_ID` (예: `CHANNEL_FIELD_BOSS_ID`).
- **멘션 역할**: `config.NOTIFY_ROLE_IDS` 의 역할을 `<@&id>` 로 멘션.
- **트리거**: 게임 서버 상태를 폴링하거나(현재 방식), 서버가 봇 API 를 호출(향후).

## 현재 구현

| 알림 | 트리거 | 채널 | 멘션 | 코드 | 상태 |
|---|---|---|---|---|---|
| RPG 필드보스 5분 전 | `rpg_api` 폴링(30s), respawn 0<m≤5 | `CHANNEL_FIELD_BOSS_ID` | `@필드보스알림` | `modules/rpg/field_boss.py` | 🟢 |
| RPG 필드보스 등장 | 상태 RESPAWNING→ALIVE 전환 | 동일 | `@필드보스알림` | 동일 | 🟢 |

폴링 방식: `tasks.loop` 으로 주기 조회 후 이전 상태와 비교해 전환 시점에만 1회 발송.

## 미구현 (TODO)

| 알림 | 멘션 | 비고 |
|---|---|---|
| 시즌보스 모집/공지 | `@시즌보스알림` | RPG 또는 운영 `/보스알림` |
| 월드보스 | `@월드보스알림` | 서버 공통 |
| 포로몬 이벤트/공지 | `@포로몬알림` | `poromon_api` 실구현 선행 |
| 이벤트 시작/종료 | `@이벤트알림` | `modules/event` 실구현 선행 |
| 점검 안내 | `@점검알림` | 운영 `/점검` |
| 업데이트/패치 | `@업데이트알림` | 운영 `/공지` |

## 통합 알림 구조: push 수신 + 디스패처 (T1, DL-133)

현재 알림 로직은 `modules/rpg/field_boss.py` 에 RPG 종속·폴링으로 박혀 있다.
도메인이 늘면 채널 라우팅·멘션·전송이 모듈마다 중복된다. 통신 방향은
**게임서버 → 봇 push**로 확정(DL-133). 알림 구조를 2부로 나눈다.
봉투 스키마·이벤트 카탈로그 = [`integration_contract.md`](integration_contract.md) B. 인터페이스 단계(구현 전).

### ① 인바운드 수신 엔드포인트 (`core/inbound.py` — 경량 HTTP 리스너)
- 게임서버가 이벤트를 봇으로 push. 봇은 `aiohttp.web` 앱을 discord 루프와 **함께** 띄운다(같은 프로세스).
- **엔드포인트:** `POST {BOT_INBOUND_BASE}/events` (단일 진입, 봉투의 `kind`로 분기).
- **검증 순서(외부 유입 → 강하게):**
  1. **IP 허용** — `INBOUND_ALLOW_IPS`(게임 호스팅 IP)만. 방화벽/보안그룹과 이중.
  2. **`X-Timestamp`** — `±N초`(예 300) 밖이면 `401`(리플레이 방지).
  3. **`X-Signature`** — `hex(HMAC-SHA256(raw_body, INBOUND_SECRET))` 불일치 시 `401`.
  4. **idempotency** — `idempotency_key` 기 수신이면 무시(dedup). 저장소 = 메모리 LRU(재시작 시 유실 허용) 또는 DB.
- 통과 → 봉투 `{domain, kind, data}` 파싱 → `notifier.dispatch(domain, kind, data)`. 응답 `200 {"ok":true}` / 스키마 오류 `400`.
- **인증 방식 결정:** **HMAC-SHA256 + timestamp**(권장안 채택). 단순 공유키 대비 본문 위변조·리플레이 방어. `.env`: `INBOUND_SECRET`.
- `.env` 추가: `BOT_INBOUND_BASE`(또는 PORT) · `INBOUND_SECRET` · `INBOUND_ALLOW_IPS`(선택). placeholder만 `.env.example`.

### ② 디스패처 `core/notifier.py`
- **진입점:** `async def dispatch(domain, kind, data)` — 라우팅 테이블 lookup → embed 빌드 → 채널 전송 + 멘션.
- **라우팅 테이블:** `(domain, kind) → {channel_id_key, mention_role_key}`. 채널/역할 ID는 `core/config.py`+`.env`(하드코딩 금지).
- **embed 빌더 레지스트리(도메인 격리 유지):** core/notifier 는 도메인 코드를 import 하지 않는다.
  각 도메인 모듈이 로드 시 `(domain, kind) → builder(data)→Embed` 를 **등록**하고, notifier 는 등록된 빌더만 조회.
- **전송:** best-effort. 채널 없음/권한 없음/HTTP 오류 → 로깅 후 무시(알림 1건 실패가 봇을 막지 않음).
- 각 도메인 모듈은 **이벤트 의미 해석(embed 구성)만**, 전송·라우팅은 notifier 위임.

### ③ 라우팅 테이블 초안 (이벤트 카탈로그 × 채널/멘션)
| domain.kind | 채널(config 키) | 멘션역할키 |
|---|---|---|
| `rpg.field_boss_pre` / `field_boss_spawn` | `CHANNEL_FIELD_BOSS_ID` | `필드보스알림` |
| `rpg.season_boss_recruit` | `CHANNEL_NOTICE_ID`(신규) | `시즌보스알림` |
| `common.world_boss` | `CHANNEL_NOTICE_ID` | `월드보스알림` |
| `common.maintenance` | `CHANNEL_NOTICE_ID` | `점검알림` |
| `common.update` | `CHANNEL_NOTICE_ID` | `업데이트알림` |
| `common.event_start` / `event_end` | `CHANNEL_NOTICE_ID` | `이벤트알림` |
| `poromon.event` | `CHANNEL_POROMON_NOTICE_ID`(신규) | `포로몬알림` |

> 신규 채널 ID(`CHANNEL_NOTICE_ID`·`CHANNEL_POROMON_NOTICE_ID` 등)는 `config.py`+`.env` 추가. 다수 공지를 단일 공지채널로 묶을지 분리할지는 운영 채널구조에 맞춰 확정.

### ④ 폴링 → push 이관
- 현행 RPG 필드보스 **폴링**(`field_boss.py`)은 유지. 게임서버가 `rpg.field_boss_*` push 구현 시 동일 embed 경로로 이관.
- **이관 중 중복 방지:** 한 소스만 활성(폴링 XOR push) — 플래그로 제어. 양쪽 동시 활성 금지.

> 실제 구현은 사용자 요청 시 진행(현재는 설계 확정·인터페이스 수준). → [`task.md`](task.md) T1.
