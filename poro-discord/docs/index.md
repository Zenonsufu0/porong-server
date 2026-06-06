# 포로서버 중앙제어 디스코드 봇 — 문서 인덱스

> **[STATUS: ACTIVE]** — poro-discord 봇 설계/운영 문서의 진입점.
> 봇 작업 규칙은 [`../CLAUDE.md`], 모노레포 전역 SoT 는 `../../poro-rpg/docs/final_master_plan.md`.

## 이 봇은 무엇인가

하나의 디스코드 서버에서 동작하는 **포로서버 전체 중앙제어 봇**이다.
여러 게임 서버(RPG·포로몬)의 정보를 통합 보유하고, 역할 부여와 알림을 담당한다.

- 실제 게임 로직은 RPG 플러그인 / 포로몬 서버가 처리한다.
- 봇은 **명령어 · 권한 · 알림 · 조회 · 운영자 UI** 만 담당한다.
- RPG/포로몬은 코드 공유가 거의 없으므로 **봇 내부에서도 도메인을 격리**한다.

스택: Python 3.12 / discord.py 2.3.

## 문서 지도

| 문서 | 내용 |
|---|---|
| [`architecture.md`](architecture.md) | 모듈 구조(core/integrations/modules)·도메인 격리·서버구조·봇 관여 경계 |
| [`integration_contract.md`](integration_contract.md) | **봇↔게임서버 통신 계약** — 요청 API + push 이벤트 스키마(DL-133/134) |
| [`data_model.md`](data_model.md) | **봇 SQLite 스키마 SoT** — 디스코드 측 상태 저장(레벨·칭호·경고·티켓·서버 레지스트리), T12 |
| [`server_lifecycle.md`](server_lifecycle.md) | **서버 생애주기 SoT** — 준비→활성→종료 상태머신·운영명령·게이팅 3층, T21 |
| [`community_level.md`](community_level.md) | **커뮤니티 레벨 설계** — 채팅·음성 XP·곡선·칭호·리더보드·어뷰징 방지, T13 |
| [`moderation.md`](moderation.md) | **모더레이션 설계** — 경고·타임아웃·추방·차단·운영/감사로그, T15 |
| [`task.md`](task.md) | **작업 트래커** — 구현 작업 목록·우선순위(DL-130 연동) |
| [`roles_and_permissions.md`](roles_and_permissions.md) | **역할 체계 SoT** — 온보딩·권한(수동)·알림(자동) 분리 |
| [`notifications.md`](notifications.md) | 알림 시스템 — 채널·멘션·트리거(도메인별) |
| [`domains/rpg.md`](domains/rpg.md) | RPG 도메인 상세 명세(온보딩·명령어·버그제보 등) |
| [`domains/poromon.md`](domains/poromon.md) | 포로몬 도메인 (스텁/TODO) |
| [`domains/common.md`](domains/common.md) | 공통 명령어 |
| [`domains/admin.md`](domains/admin.md) | 운영/관리자 명령어 (설계 선행) |

## 구현 현황 요약

| 영역 | 상태 |
|---|---|
| RPG 연동(프로필·영지·보스·필드보스 알림·인증/역할) | 🟢 구현 |
| 역할 부여 프레임워크(온보딩 자동승급·알림 토글·운영권한 데코레이터) | 🟢 구현 |
| 멀티서버 온보딩(공통 디스코드 인증 → 서버별 약관동의 → 서버별 화이트리스트, DL-131) | 🟡 RPG만 구현, 공통 분리·포로몬 설계 |
| 배포(오라클 상시 운영, 게임 호스팅 분리, DL-131) | 🔴 미구성 |
| 포로몬 연동·명령어 | 🟡 스텁(인터페이스/TODO) |
| 이벤트·월드보스·점검 알림 | 🟡 TODO(알림 역할만 정의됨) |
| 운영/관리자 명령어(공지·점검·역할부여·골드지급) | 🟡 스텁(설계 선행) |
| 통합 알림 디스패처(`core/notifier`) | 🔴 미설계 |

상태 표기: 🟢 구현 · 🟡 스텁/부분 · 🔴 미설계.
