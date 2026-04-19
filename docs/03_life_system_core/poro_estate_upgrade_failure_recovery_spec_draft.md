# 영지 Lv 업그레이드 실패 3단 계층 복구 구현 명세 (1차 드래프트)

## 문서 목적
2026-04-19 확정된 "영지 Lv 업그레이드 실행 중 실패 시 3단 복구" 원칙(재화 자동 재지급 → 디스코드 문의 안내 → 운영자 수동 복구)의 구현 1차 드래프트. implementation-reviewer 관점에서 작업 단위·리스크·테스트 포인트를 정리하고, 기존 플래그 저장소·DB 구조를 최대한 재활용하는 1차 버전을 제시한다.

## 목표
- 영지 Lv 업그레이드가 중간 단계에서 실패하더라도 **플레이어 재화·영지 상태가 정합을 유지**하도록 3단 안전망 구축.
- 1차 자동 재지급으로 **압도적 다수의 실패 케이스를 사람 개입 없이 해결**한다.
- 2차 디스코드 알림으로 **플레이어가 "돈만 빠지고 Lv이 안 올랐다"라는 신호를 받을 길을 보장**한다.
- 3차 운영자 수동 복구로 **극단 케이스까지 SLA 내 회수**한다.
- 영지 Lv은 영속 보존 축이므로(시즌 초기화에도 보존), 이 복구 로그 역시 영속성 보존을 전제로 설계한다.

## 선행조건
- **플레이어 플래그 저장소 v0.1 머지 완료** — 트랜잭션 로그 테이블은 플래그 저장소와 독립 테이블이지만, 동일 SQLite `DataSource`·마이그레이션 러너(`NoopMigrationEntryPoint` 교체본)를 재사용한다. 플래그 저장소 스프린트가 미완이면 본 기능 착수 불가.
- **영지 Lv 업그레이드 실행 엔드포인트(영주청·관리석 상호작용) 최소 구현** — 재화 차감 → Lv 승급 커밋의 단일 트랜잭션 경로가 존재해야 실패 탐지 훅을 꽂을 수 있다.
- **디스코드 봇 플레이어 연결 테이블(UUID ↔ Discord User ID) 선행** — 2차 알림에서 DM 경로를 쓰려면 필수. 없으면 2차는 인게임 채팅 + 관리자 채널 알림만 우선 가동하고 DM은 후속.
- **운영자 권한 체계** — 3차 명령어는 `poro.admin.estate.recover` 권한 전제.

## 작업 분해

### 1. 트랜잭션 로그 테이블 스키마
새 테이블 `estate_upgrade_txn_log`를 플래그 저장소와 동일한 SQLite 스키마에 추가한다.

| 컬럼 | 타입 | 비고 |
|---|---|---|
| `txn_id` | TEXT PK | UUID v4 문자열, 클라이언트가 업그레이드 상호작용 시 발급 |
| `player_uuid` | TEXT NOT NULL | 인덱스 |
| `previous_level` | INTEGER NOT NULL | 업그레이드 이전 영지 Lv |
| `target_level` | INTEGER NOT NULL | 목표 Lv |
| `cost_snapshot_json` | TEXT NOT NULL | 차감 재화 스냅샷 (골드·희귀재료 수량 목록, v0.1은 JSON 문자열 허용 예외) |
| `status` | TEXT NOT NULL | ENUM: `attempted` / `success` / `failed_rollback_pending` / `failed_auto_refunded` / `failed_discord_notified` / `manual_recovered` |
| `attempted_at` | INTEGER NOT NULL | epoch millis |
| `resolved_at` | INTEGER NULL | 최종 상태 확정 시점 |
| `failure_reason` | TEXT NULL | 내부 에러 코드·스택 요약 |
| `recovery_operator` | TEXT NULL | 3차 수동 복구 시 운영자 UUID |

- 복합 인덱스: `(player_uuid, attempted_at DESC)`, `(status)` — 미해결 실패 건 스캔용.
- `cost_snapshot_json`은 플래그 저장소 v0.1의 "JSON은 v0.2" 규약에 대한 **국소 예외**(재화 품목 배열을 로그에 남기는 용도이므로 검색 대상 아님). 플래그 저장소 본체에는 영향 없음.
- Lv 상한 하드코딩 없음 (Lv6+ 확장 대비, `INTEGER` 허용 범위).

### 2. 실패 탐지 훅
업그레이드 실행 시퀀스를 아래 단계로 고정하고 각 단계에 훅을 건다.

1. **시도 로그 기록**: `txn_id` 발급, `attempted` 상태로 INSERT. 이 시점에 실패하면 재화 차감 자체가 안 일어나므로 별도 복구 불필요.
2. **재화 차감**: 플레이어 인벤토리·골드에서 비용 스냅샷 차감. 차감 직후 `status=failed_rollback_pending`로 UPDATE. (이 상태를 거쳐야 1차 재지급 대상이 된다.)
3. **Lv 승급 커밋**: 영지 Lv·해금 기기·크기·전력 상한 갱신. 단일 DB 트랜잭션으로 묶고 성공 시 `status=success`.
4. **실패 탐지 조건**:
   - 재화 차감 후 Lv 승급 SQL 예외 / 런타임 예외
   - 서버 크래시 후 재기동 시 `status=failed_rollback_pending`로 남은 로그 스캔 (기동 시 스윕 잡)
   - DB 커넥션 단절 / 트랜잭션 커밋 타임아웃
   - 동시 승급 시도 충돌 (동일 UUID로 `attempted`가 둘 이상) — 사용자 단일 세션 가정이지만 안전장치로 UUID 단위 애플리케이션 락

### 3. 1차 — 재화 자동 재지급
- `failed_rollback_pending` 상태 로그에 대해 재지급 잡 실행.
- 재지급 방식: `cost_snapshot_json`을 역으로 읽어 골드·아이템을 플레이어 인벤토리·우편함에 반환.
  - 인벤토리 공간 부족 시 **우편함 폴백** (우편 시스템이 없다면 1차 버전은 "다음 접속 시 지급 대기 큐" 단순 테이블).
- 재지급 확정 시점: 재지급 트랜잭션 커밋 후 `status=failed_auto_refunded`, `resolved_at` 기록.
- 재지급 실패 판정: 재지급 자체가 DB 예외·우편함 적재 실패·3회 재시도 소진으로 끝나면 2차로 에스컬레이션.

### 4. 2차 — 디스코드·인게임 알림
- **인게임 채팅 템플릿**: "영지 업그레이드 처리가 지연되었습니다. 거래 번호: `<txn_id>`. 디스코드 `#문의` 채널로 번호와 함께 문의해 주세요."
- **디스코드 DM** (봇 연결 UUID 보유 시): 동일 문구 + `/estate-recover` 안내 링크.
- **운영자 관리자 채널 동시 알림**: `#ops-estate-recovery` 채널에 `txn_id`·UUID·이전/목표 Lv·실패 사유 요약 임베드 전송.
- 전송 성공 시 `status=failed_discord_notified`로 승격. 인게임 채팅은 전송 보장이 쉬우므로 "인게임 채팅 성공 OR 관리자 채널 전송 성공" 중 하나라도 되면 2차 성공 판정.
- 디스코드 봇 다운타임 시: 재시도 큐에 넣고(최대 N분), 그래도 실패하면 `status`는 `failed_rollback_pending` 유지 → 다음 봇 기동 시 스윕 재처리.

### 5. 3차 — 운영자 수동 복구
- 명령어: `/estate-recover <player> <txn_id>`
- 권한: `poro.admin.estate.recover`
- 동작:
  - `txn_id` 유효성·대상 플레이어 일치 검증
  - 로그 상태가 `manual_recovered`·`success`·`failed_auto_refunded`면 **이중 실행 거절** (재화 인플레이션 방지)
  - 수동 재지급 실행 (우편함 경로 선호) → 성공 시 `status=manual_recovered`, `recovery_operator`·`resolved_at` 기록
- SLA 목표: **플레이어 문의 수령 후 24시간 이내 1차 응답·복구 처리**. SLA 자체는 운영 규약이고 코드 강제는 하지 않지만, `#ops-estate-recovery` 채널에 24h 경과 미해결 건 데일리 리포트 잡을 1차 버전 이후에 추가.
- 복구 완료 로그는 `estate_upgrade_txn_log`의 `manual_recovered` 상태 + 운영자 UUID로 충분. 별도 감사 테이블은 v0.2.

### 6. PR 분해 (5~7 PR, 약 8~11 man-day)

**PR1 — 스키마 DDL + 마이그레이션 등록 (1 man-day)**
- `estate_upgrade_txn_log` 테이블·인덱스 DDL
- 플래그 저장소와 동일한 마이그레이션 러너에 등록
- 테스트: DDL idempotent·PK 중복 거절·인덱스 확인

**PR2 — 트랜잭션 로그 Repository + 상태 전이 규칙 (1.5 man-day)**
- `EstateUpgradeTxnLogRepository`: insert / updateStatus / findByStatus / findById
- 상태 전이 정합성 검증 (`attempted → failed_rollback_pending → failed_auto_refunded → failed_discord_notified → manual_recovered`, `attempted → success` 단축 경로 허용)
- 테스트: 허용 전이·금지 전이·Optional.empty

**PR3 — 실패 탐지 훅 + 기동 시 스윕 잡 (1.5~2 man-day)**
- 업그레이드 실행 파이프라인에 훅 삽입
- 서버 기동 시 `failed_rollback_pending` 로그 스캔 후 1차 재지급 잡에 투입
- 테스트: 재화 차감 직후 크래시 시나리오, 기동 스윕, 중복 실행 방지

**PR4 — 1차 자동 재지급 잡 + 우편함 폴백 (2 man-day)**
- 재지급 실행기, 3회 재시도, 인벤토리 부족 시 우편함/지급 대기 큐
- 테스트: 인벤토리 공간 충분/부족, 재시도 소진, 커넥션 단절

**PR5 — 2차 디스코드·인게임 알림 (1.5~2 man-day)**
- 인게임 채팅 송출 + 봇 DM + 관리자 채널 임베드
- 봇 다운타임 시 재시도 큐
- 테스트: 봇 연결 UUID 유/무, 봇 다운 시나리오, 관리자 채널 전송 실패

**PR6 — 3차 운영자 명령어 `/estate-recover` (1 man-day)**
- 권한 검사, 이중 실행 방지, 상태 전이 확정
- 테스트: 비운영자 거절, 잘못된 txn_id, 이미 복구된 건 재실행 거절

**PR7 — (옵션) 24h 미해결 데일리 리포트 (0.5~1 man-day)**
- `#ops-estate-recovery`에 미해결 건 집계 임베드
- 1차 버전에서 미뤄도 무방

**머지 순서**: PR1 → PR2 → PR3 → (PR4 ‖ PR5) → PR6 → (PR7)

## 리스크
- **동시성 · 이중 재지급**: 플래그 저장소 v0.1과 동일하게 플레이어 단위 단일 스레드 가정. 그래도 기동 스윕 + 실시간 훅이 같은 로그를 동시에 집으면 이중 재지급 위험 → `status` 전이를 DB UPDATE의 `WHERE status=<이전상태>` 조건부 업데이트로 구현해 **낙관적 경합 차단**.
- **재화 인플레이션**: 1차 재지급 성공 후 로그 상태 반영 실패 시 재기동 스윕이 또 재지급할 수 있음 → 위 조건부 UPDATE가 실패하면 재지급을 수행하지 않도록 순서를 "상태 전이 시도 → 성공 시에만 재지급 실행"으로 뒤집는다. (재지급 후 상태 전이 실패보다 재지급 전 상태 선점이 안전)
- **디스코드 봇 다운타임**: 2차 전체 경로가 봇에 의존하면 복구가 봇 가용성에 종속. 인게임 채팅은 봇 독립 경로로 유지하고, 관리자 채널 전송 실패 시 내부 로그에만 남기고 봇 복구 후 재전송.
- **플래그 저장소 미구축 의존성**: v0.1 머지 전 착수 금지. 억지로 착수하면 마이그레이션 러너 스텁을 중복 만드는 비용 발생.
- **우편함 시스템 부재**: 1차 버전에서 우편함이 없다면 `pending_refund_queue` 단순 테이블로 대체(다음 접속 시 수령). 우편함 도입 후 마이그레이션.
- **Lv6+ 확장**: Lv 상한 하드코딩 금지 규약을 지켜, `previous_level`·`target_level` 정수 범위만 유지.
- **관리자 명령어 오남용**: `/estate-recover`는 txn_id 필수화해 "아무 플레이어에게 임의 재화 지급" 경로로 변질되지 않게 강제.

## 테스트 포인트
- 재화 차감 직후 서버 강제 종료 → 재기동 후 스윕 잡이 1차 재지급 수행 → `failed_auto_refunded` 상태.
- DB 커넥션 단절 상태에서 업그레이드 시도 → 재시도 3회 소진 후 2차 에스컬레이션.
- 디스코드 봇 오프라인 상태에서 2차 진입 → 인게임 채팅만 전송, 봇 복구 시 DM·관리자 채널 지연 전송.
- `/estate-recover` 이미 `manual_recovered` 건에 재실행 → 거절, 재화 변동 없음.
- 동일 `txn_id`에 동시에 기동 스윕 + 실시간 훅 진입 → 조건부 UPDATE로 한쪽만 재지급 수행.
- 인벤토리 만석 상태에서 1차 재지급 → 우편함(또는 대기 큐) 폴백 성공.
- Lv5 → Lv6 가상 업그레이드 시도에서도 상한 하드코딩으로 거절되지 않는지 (기능 외 스키마 검증).

## 1차 버전 권장안 (MVP)
- PR1~PR6만 머지. PR7(데일리 리포트)·우편함 정식 연동은 후속.
- 2차 알림은 "인게임 채팅 + 관리자 채널" 두 경로만 필수, 디스코드 DM은 봇 연결 UUID 테이블이 생기면 추가.
- 재지급 폴백은 `pending_refund_queue` 단순 테이블로 시작 (우편함 도입 시 교체).
- `cost_snapshot_json`은 플래그 저장소 v0.1의 JSON 금지 규약의 국소 예외로 명시 문서화, 본체 플래그는 그대로 BOOL/LONG/STRING 유지.

## 나중으로 미뤄도 되는 것
- 미해결 24h 경과 데일리 리포트 잡 (PR7)
- 디스코드 DM 경로 (봇 연결 UUID 테이블 선행 필요)
- 감사 전용 별도 테이블 (v0.2)
- 복구 시도 분당 레이트리밋 / 어뷰즈 방어
- 플레이어용 `/estate-txn-status <txn_id>` 조회 명령어
- 웹 대시보드에서 미해결 건 조회 UI
- 우편함 시스템 정식 연동 (도입 후 대기 큐 → 우편함 마이그레이션)

## 오픈 질문
- 우편함 시스템의 1차 도입 일정 — 없다면 `pending_refund_queue` 임시 테이블로 계속 갈지, 우편함 도입까지 대기할지.
- 디스코드 봇 플레이어 연결 테이블(UUID ↔ Discord User ID)의 선행 시점.
- SLA 24h가 운영 규약 측면에서 적정한지(더 짧게/길게).
- `#ops-estate-recovery` 채널 신설 여부와 기존 운영 채널 통합 여부.
- 동시성 가정을 "플레이어 단위 단일 세션"으로 유지할지, 멀티 세션 방어까지 코드 강제할지.

## 상위 문서 참조
- 영지 Lv 3축 매트릭스: `poro_estate_level_triaxial_matrix_draft.md`
- 마스터플래닝 변경 로그: `../poro_master_planning.md` 2026-04-19 영지 결정 묶음
- 플래그 저장소 v0.1 스프린트: `../00_index_and_execution/poro_flag_store_v01_sprint_plan.md`
