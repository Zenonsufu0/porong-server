# 지원 / 문의 설계 (T16)

> **[STATUS: DRAFT]** — 2026-06-06. 버그제보(서버 선택형)·티켓(1:1 문의)·FAQ(→문의 폴백).
> 저장 = [`data_model.md`](data_model.md) §2.6 `tickets`·§2.9 `faq`. LLM 미사용(§9.2). 인터페이스 단계(구현 전).

## 1. 버그제보 — 서버 선택형 (rpg.md §4 일반화) 🟢 (2026-06-10 구현)

> 🟢 **구현: `modules/support/bug_report.py` + `core/config.CHANNEL_BUGREPORT_ID`.** `/버그제보 대상(RPG/포로몬/기타·봇) 심각도(낮음~심각)` → 모달(제목·재현·기대·실제) → 공통 버그채널 임베드 게시(`BUG · reporter:{id}` 푸터) + 제보자 DM + 운영진 상태 버튼(확인중/완료/기각, `member_has_permission(admin·support·rpg_manager·poromon_manager)`). 상태 변경 시 임베드 색/필드 갱신 + 제보자 DM. **봇 DB 미저장**(채널 임베드가 유일 기록 — data_model §0). RPG/포로몬도 현재는 동일 채널 baseline 접수(임베드에 "게임 DB 이관 예정" 노트) — `rpg_api.create_bug_report`/`poromon_api` 게임 DB 라우팅은 게임 API 확정 후 연동. `CHANNEL_BUGREPORT_ID` 미설정 시 graceful 비활성(문의 안내).

기존 RPG 전용 `/버그제보`([`domains/rpg.md`](domains/rpg.md) §4)를 **대상 서버 선택**으로 일반화한다.

```
/버그제보 → 대상 선택(RPG / 포로몬 / 기타·봇) → 모달(제목·재현·기대/실제·심각도)
```

- **라우팅(경계 유지 — 게임 버그는 게임 DB):**
  - RPG: `rpg_api.create_bug_report`([`integration_contract.md`](integration_contract.md) §A-2b, 신규 계약) → 게임 DB + RPG `#버그제보` 채널 embed `BUG-{id}`.
  - 포로몬: `poromon_api`(T4) 동형 — 포로몬 게임 DB + 포로몬 채널. **API 확정 선행**.
  - 기타·봇: 게임 API 없음 → **공통 `#버그제보` 채널 게시**(접수번호는 채널 메시지 기준). 봇 DB에 별도 저장 안 함.
- 상태 버튼(접수/확인중/완료/기각) + 제보자 DM = 기존 rpg.md §4-3 재사용. 게임측 저장분 상태는 게임 API, 봇/기타분은 채널 임베드 갱신.
- **경계:** 게임 버그 데이터는 **게임 DB**(API 경유), 봇 DB에 복제하지 않는다(data_model §0).

## 2. 티켓 — 1:1 문의

- `/문의` 또는 패널 버튼 → **비공개 채널 생성**(개설자 + Support·admin 가시) → `tickets` INSERT(`open`).
- 채널 내 운영진과 대화. `/티켓종료`(개설자 또는 운영자) → 채널 정리 + `state=closed, closed_at, closed_by`.
- **유저당 동시 오픈 1개 제한**(중복 방지) — 미확정(아래 §4).
- 모든 개설/종료 `mod_log` 기록(선택) + 운영 가시.
- 🟢 **구현(2026-06-09): `modules/support/tickets.py` + db v10 `tickets` + `core/tickets.py`.** `/문의`(동시 1개 제한, @everyone 숨김+개설자/admin/support 가시) · `/티켓종료`·[종료] 영구버튼(개설자/운영) · 종료=**잠금·아카이브**(채널 삭제 X, `[종료]` 프리픽스 + 개설자 접근 해제 — §5 삭제vs아카이브 = 아카이브 채택, 생애주기 정합). 생성 카테고리 = `CATEGORY_티켓_ID`(.env, 미설정 시 무카테고리). mod_log(ticket_open/close).

## 3. FAQ 🟢 (2026-06-10 구현 — 조회방식 = **패널형(B안)** 확정)

- **조회 = 패널 선택**: `/faq` → 등록된 질문 **Select 메뉴**(공통 + 현재 active 도메인, ≤25개)에서 골라 답변(ephemeral). 키워드 텍스트 매칭은 폐기(LLM 미사용 → 정확 단어 요구로 실효성 낮음, 운영자 큐레이션 목록을 직접 고르는 편이 현실적).
- **폴백**: 패널 [운영진 문의] 버튼 → 티켓 생성(§2 연결, `TicketCog.open_ticket_for`). FAQ 0건이어도 버튼 노출.
- 노출 필터 = `domain IS NULL`(공통) + active 서버 도메인(전역 단일 active, task.md §5). >25개면 절단 + 안내 문구.
- 운영 관리: `/faq추가`(대상=공통/RPG/포로몬, 모달)·`/faq수정 번호`(모달 프리필)·`/faq삭제 번호`(admin·support) → `faq` CRUD. 번호 = 자동완성(`#id [도메인] 질문`). 전부 `mod_log`(faq_add/update/delete).
- 구현: db v11 `faq`, `core/faq.py`, `modules/support/faq.py`.

## 4. 권한 / 게이트

| 명령 | 권한 |
|---|---|
| `/버그제보` `/문의` `/faq` | 공통(유저) |
| `/티켓종료`(타인 티켓) · `/faq추가·수정·삭제` | `requires_permission`(admin·support) |

- 대상 선택형이라 **카테고리 게이트 불필요**(공통 채널에서 호출, 대상은 모달 선택).

## 5. 미확정
- 유저당 동시 티켓 수 제한값.
- 티켓 종료 = 채널 **삭제 vs 아카이브 보존**(생애주기 정합 검토).
- 봇/기타 버그 저장 방식(채널 기록만 vs 경량 테이블).
- 포로몬 버그 API 스키마(PoroMonCore 선행).
</content>
