# 봇 아키텍처 — 모듈 구조와 도메인 격리

> 코드 구조의 단일 진실원. 실제 코드와 어긋나면 이 문서를 갱신한다.

## 계층 구조

```
porong-discord/
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

## 미설계/공백 (TODO)

- `core/notifier.py` — 도메인 무관 알림 디스패처(채널 라우팅 + 멘션 + 트리거 인터페이스).
  현재 알림 로직이 `modules/rpg/field_boss.py` 에 도메인 종속으로 박혀 있음.
  포로몬/이벤트/월드보스/점검 알림을 일관되게 붙이려면 이 계층이 필요.
- `integrations/poromon_api.py` 실구현(현재 스텁).
- `modules/admin` · `modules/event` · `modules/poromon` 실구현(현재 스텁, 설계 선행).
