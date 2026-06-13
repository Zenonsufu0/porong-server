# 역할 / 권한 정책 (SoT)

> 봇의 역할·권한 정책 단일 진실원. 코드: `core/config.py`, `core/permissions.py`,
> `modules/roles/role_commands.py`, `modules/rpg/auth.py`, `modules/rpg/role_poll.py`.
> 도메인 문서(RPG 등)의 역할 서술과 충돌하면 **이 문서가 우선**한다.

## 대원칙

1. **권한 역할(운영 제어)과 알림 역할(플레이어 선택)을 분리한다.**
2. **운영 권한 역할은 수동 지급 전용이다.** 봇은 버튼·이모지·자동 로직으로
   운영 권한 역할을 절대 지급하지 않는다.
3. **알림 역할만 자동 지급(토글)을 허용한다.** 알림 역할에는 어떤 운영 권한도 없다.

## 역할 3분류

### A. 온보딩 권한 역할 (시스템 자동 승급)

인증 플로우에 따라 봇이 자동 승급. `config.ROLE_*_ID`.

| 역할 | 부여 시점 | 처리 코드 |
|---|---|---|
| `미인증` | 디스코드 입장 직후 | `modules/rpg/auth.py` (on_member_join) |
| `접속대기` | 약관 동의 + 닉네임 입력 | `modules/rpg/auth.py` (모달 제출) |
| `인증유저` | 인게임 `/연동` 코드 인증 완료 | `modules/rpg/role_poll.py` (역할 큐 폴링) |

> 온보딩은 현재 RPG 인증 플로우 기준이다. 포로몬 전용 온보딩이 필요해지면
> 별도 설계 후 이 표를 확장한다.

### B. 운영 권한 역할 (수동 지급 전용 — 봇 자동 지급 금지)

`config.PERMISSION_ROLE_IDS`. 운영진이 디스코드에서 직접 부여한다.

| 키 | 역할 | 비고 |
|---|---|---|
| `owner` | Owner | 슈퍼유저 — 모든 권한 통과 |
| `admin` | Admin | 관리자급 운영 명령 |
| `rpg_manager` | RPG Manager | RPG 운영(골드 지급 등) |
| `poromon_manager` | Poromon Manager | 포로몬 운영 |
| `event_manager` | Event Manager | 이벤트 등록/관리 |
| `support` | Support | 문의/지원 |

권한 체크: `core/permissions.py`
- `member_has_permission(member, *keys)` — 보유 여부(Owner 항상 통과)
- `is_admin(member)` — owner/admin 여부
- `@requires_permission("admin", ...)` — 슬래시 커맨드 데코레이터.
  권한 역할이 전부 미설정(0)이면 보수적으로 **차단**한다(무방비 노출 방지).

### C. 알림 역할 (자동 지급 가능 — 플레이어 토글)

`config.NOTIFY_ROLE_IDS`. `/알림설정` 버튼으로 자유롭게 추가/제거. 미설정(0)이면 버튼 숨김.

| 키 | 표시명 | 알림 대상 |
|---|---|---|
| `필드보스알림` | 필드보스 알림 | RPG 필드보스 5분 전·등장 |
| `시즌보스알림` | 시즌보스 알림 | RPG 시즌보스 모집·공지 |
| `월드보스알림` | 월드보스 알림 | 월드보스(서버 공통) |
| `포로몬알림` | 포로몬 알림 | 포로몬 이벤트/공지 |
| `이벤트알림` | 이벤트 알림 | 서버 이벤트 시작/종료 |
| `점검알림` | 점검 알림 | 점검 안내 |
| `업데이트알림` | 업데이트 알림 | 패치/업데이트 공지 |

> 클래스 역할(`config.CLASS_ROLE_IDS`: 검사/도끼전사/…)은 RPG 도메인 전용 선택 역할이다.
> 1인 1클래스, `/클래스선택`. 상세는 [`domains/rpg.md`](domains/rpg.md).

## 자동 지급 가능 여부 판정

`permissions.is_auto_grantable(role_key)`:
- 권한 역할 키(B) → **항상 False**
- 알림 역할 키(C) → True
- 운영 권한을 알림처럼 자동 지급하려는 시도를 코드 레벨에서 차단하는 가드.

## 보안 메모

- 운영 권한 역할 ID 는 `.env` 에서 로드한다(코드 하드코딩 금지).
- 봇이 멤버 역할을 변경할 때는 항상 reason 을 남긴다(감사 로그).
- 운영 명령어(역할부여·골드지급 등)는 실제 구현 전 설계/승인을 선행한다
  ([`domains/admin.md`](domains/admin.md)).
