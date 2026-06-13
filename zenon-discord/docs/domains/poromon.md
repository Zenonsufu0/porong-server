# Zenon Mon 도메인 — 명세 (스텁)

> **[도메인: Zenon Mon]** **[STATUS: STUB/TODO]** — 골격만 존재. 실제 명령어·연동 미구현.
> 코드: `modules/poromon/commands.py`(빈 Cog), `integrations/zenon_mon_api.py`(인터페이스 스텁).

## 원칙

- RPG 도메인과 **완전 분리**한다. RPG 코드/클라이언트를 import 하지 않는다.
- Zenon Mon 서버 연동은 `integrations/zenon_mon_api.py` 를 통해서만 한다.
- 실제 API 연동은 사용자가 명시적으로 요청할 때만 구현한다(현재 인터페이스/TODO).

## 연동 방식 (DL-133)

- **ZenonMonCore(커스텀 모드)가 RPG와 동일한 HTTP API 패턴을 노출**한다. `zenon_mon_api` 가 그 클라이언트.
- 봇 관여 경계는 RPG와 동일 — 게임 상태 권위는 Zenon Mon 서버, 봇은 API 클라이언트(DL-133).
- 이벤트 알림은 **Zenon Mon 서버 → 봇 push**(공유 시크릿/HMAC 보호) → `core/notifier` 게시.
- 온보딩은 멀티서버 모델을 따른다 — Zenon Mon 카테고리(이모지 `포로몬접근`) → Zenon Mon 약관동의 →
  Zenon Mon `/연동` → Zenon Mon 화이트리스트 + `포로몬플레이어`([`../roles_and_permissions.md`] §A-2·§D).

## 명령어 후보 (TODO)

| 명령어 | 설명 | 선행 조건 |
|---|---|---|
| `/포로몬현황` | 모드 서버 상태(접속 인원·TPS) | `zenon_mon_api.get_server_status` 구현 |
| `/포로몬도감` | 도감/보유 현황 조회 | `zenon_mon_api` + Zenon Mon DB/API 확정 |

## 알림 후보 (TODO)

- Zenon Mon 이벤트/공지 → `@포로몬알림` (→ [`../notifications.md`]).

## 미확정

- 연결 방식은 HTTP API로 확정(DL-135). 남은 것: **API 포트·인증 시크릿·엔드포인트 계약**
  (인증/조회/이벤트 push 스키마)과 조회 가능 데이터 범위.
  ZenonMonCore(`../../zenon-mon/docs/03_zenonmoncore/`) 설계 확정과 함께 결정.
