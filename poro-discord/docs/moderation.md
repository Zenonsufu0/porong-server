# 모더레이션 / 운영 제재 설계 (T15)

> **[STATUS: DRAFT]** — 2026-06-06. 디스코드 측 제재(경고·타임아웃·추방·차단) + 운영/감사 로그.
> 저장 = [`data_model.md`](data_model.md) §2.4 `warnings`·§2.5 `mod_log`. 권한 = [`roles_and_permissions.md`](roles_and_permissions.md) §B. 인터페이스 단계(구현 전).

## 0. 경계
- **디스코드 측 제재만.** 게임 내 제재(인게임 밴·아이템 회수 등)는 게임서버 API(A-3) 별개 — 본 모듈 범위 아님.
- 자동 키워드/스팸 차단은 **디스코드 네이티브 AutoMod**(봇 외부, §9.2). 본 모듈은 **수동 운영 제재**만.
- 모든 제재는 `mod_log` 적재 + `#운영로그` 채널 게시(누가·언제·무엇·왜).

## 1. 명령어 (모두 `requires_permission`)

| 명령 | 입력 | 효과 | 권한 |
|---|---|---|---|
| `/경고` | `유저, 사유` | `warnings` 추가 + DM 통보 + 누적 경고수 안내 | admin·support |
| `/경고목록` | `유저` | 경고 이력(활성/철회) 조회 | admin·support |
| `/경고취소` | `경고_id` | `warnings.active=0` | admin·support |
| `/타임아웃` | `유저, 기간, 사유` | 디스코드 timeout(최대 28일) + DM | admin·support |
| `/타임아웃해제` | `유저` | timeout 해제 | admin·support |
| `/추방` | `유저, 사유` | kick + DM(사전 발송) | admin |
| `/차단` | `유저, 사유` | ban + DM(사전 발송) | admin |
| `/차단해제` | `유저_id` | unban | admin |

- Owner 는 항상 통과. 권한 역할 전부 미설정이면 보수적 차단(roles_and_permissions §B).
- 응답 ephemeral, 공개 기록은 `#운영로그`.

## 2. 경고 누적 / 에스컬레이션
- `/경고` 시 활성 경고수 집계 → 임계 도달 안내.
- **제안:** 자동 조치는 하지 않고 **운영자에게 권고만**(예 "3회 누적 — 타임아웃 검토"). 자동 에스컬레이션 도입 여부 = 미확정.
- 임계·조치 매핑(예 3=타임아웃, 5=차단)은 도입 시 `config`.

## 3. 운영 / 감사 로그
- 모든 액션(경고·타임아웃·추방·차단·서버 생애주기·XP보정 등) → `mod_log`:
  `{action, target_id, operator_id, reason, detail, created_at}`.
- 동시에 `#운영로그`(`CHANNEL_MODLOG_ID`, 신규 `.env`) 임베드 게시.
- 봇이 멤버 역할/상태 변경 시 항상 `reason` 기록(roles_and_permissions §보안메모).

## 4. DM 통보
- 제재 대상에게 사유 DM(베스트에포트). **추방/차단은 실행 전 DM 발송**(차단 후엔 공유 서버 없어 DM 불가).
- DM 차단된 유저는 무시(로깅).

## 5. 미확정
- 경고 임계·자동 에스컬레이션 수치·자동/수동 — 1차는 수동 권고.
- 제재 권한 세분(경고=support 허용 / 차단=admin 전용) 최종 확정.
- 디스코드 차단과 게임 화이트리스트 연동 여부(현재 별개 — 게임 제재는 A-3).
- DM 문구 템플릿.
</content>
