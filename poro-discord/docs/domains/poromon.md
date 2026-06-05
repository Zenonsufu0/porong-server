# 포로몬 도메인 — 명세 (스텁)

> **[도메인: 포로몬]** **[STATUS: STUB/TODO]** — 골격만 존재. 실제 명령어·연동 미구현.
> 코드: `modules/poromon/commands.py`(빈 Cog), `integrations/poromon_api.py`(인터페이스 스텁).

## 원칙

- RPG 도메인과 **완전 분리**한다. RPG 코드/클라이언트를 import 하지 않는다.
- 포로몬 서버 연동은 `integrations/poromon_api.py` 를 통해서만 한다.
- 실제 API 연동은 사용자가 명시적으로 요청할 때만 구현한다(현재 인터페이스/TODO).

## 명령어 후보 (TODO)

| 명령어 | 설명 | 선행 조건 |
|---|---|---|
| `/포로몬현황` | 모드 서버 상태(접속 인원·TPS) | `poromon_api.get_server_status` 구현 |
| `/포로몬도감` | 도감/보유 현황 조회 | `poromon_api` + 포로몬 DB/API 확정 |

## 알림 후보 (TODO)

- 포로몬 이벤트/공지 → `@포로몬알림` (→ [`../notifications.md`]).

## 미확정

- 포로몬 서버 연결 방식(포트·인증), 조회 가능한 데이터 범위.
  포로몬 프로젝트(`../../poromon/`) 설계 확정 후 결정.
