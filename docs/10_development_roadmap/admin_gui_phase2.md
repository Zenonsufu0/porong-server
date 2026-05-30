# 관리자 GUI — Phase 2 잔여 작업

> **[STATUS: DRAFT]** — Phase 1은 `/empire-admin` 명령으로 구현 완료. Phase 2 항목은 1차 시즌 운영 피드백 후 우선순위 재조정.

> 작성일: 2026-05-30  
> Phase 1 커밋: (이번 세션) — 인스펙트·진행 매치·통계·슬롯 해제

---

## Phase 1 완료 (활성)

| 항목 | 진입 | 구현 |
|---|---|---|
| 플레이어 인스펙트 | AdminHubGui slot 11 | Anvil 닉네임 → 27슬롯 상세 (직업/레벨/영지/PvP/재화/장비 5슬롯/평균 IL) |
| 진행 중 매치 | AdminHubGui slot 13 | PvP 매치 목록 + 좌클릭 강제 종료 (무승부) |
| 서버 통계 | AdminHubGui slot 15 | 시즌 주차·일수 / 아레나·보스룸 점유 / 온라인 |
| 슬롯 강제 해제 | AdminHubGui slot 22 | Shift+클릭 → 모든 아레나·보스룸 슬롯 강제 해제 |

---

## Phase 2 — 미구현 (stub 슬롯 위치 확정)

### 영지 관리 (slot 29) — **Step 5 구현 완료 (2026-05-30)**

| 기능 | 상태 |
|---|---|
| 영지 목록 GUI | ✅ `AdminTerritoryGui` — `snapshot()` 작위 내림차순 페이지네이션(45/page), 작위·시설 표시 |
| 영지 초기화 | ✅ Shift+우클릭 → `IslandTerritoryStateStore.resetSocialSettings` (멤버·권한·방문모드 reset, 작위·시설 보존) |
| 작위 강제 승급/강등 | ✅ 좌클릭=▲ / 우클릭=▼ (ordinal ±1 clamp). 명령 `/empire-rank`도 존재 |
| 멤버 강제 제거 | ⏳ 보류 — 초기화로 전체 멤버 제거는 가능, 개별 제거 GUI는 후속 |
| 자원 강제 지급/회수 | ⏳ 보류 — `/empire-currency`·`/empire-give` 명령 존재 (영지 customItem 직접 조작 GUI는 후속) |

- GUI: `AdminTerritoryGui` (54슬롯, 목록+클릭 액션). 진입: `/empire-admin` → slot 29.
- 명령어: 신규 없음 — 액션은 기존 `/empire-rank`(작위)·`/empire-island-reset`(소셜 초기화)와 동일 (C 방식: GUI 액션 ↔ 명령 동등). 초기화 로직은 `resetSocialSettings`로 공통화(DRY).
- 확인 단계: 초기화는 Shift+우클릭(오클릭 방지). 별도 확인 GUI 없음.

### 운영 토글 (slot 31) — **Step 2 GUI+명령어 완료, Step 2b hook 적용 완료 (2026-05-30)**

| 기능 | 플래그 | hook 상태 |
|---|---|---|
| 보스 스폰 일시정지 | `BOSS_SPAWN_PAUSE` | ✅ `BossRoomListener.onInteract` — MM 가드 직후 `assignRoom` 전 차단 |
| 강화 확률 부스트(2배) | `ENHANCE_BOOST`   | ✅ `EnhancementService` 성공 임계값 ×2 (1.0 클램프, `BooleanSupplier` 주입) |
| EXP 2배              | `EXP_BOOST`       | ✅ `FieldDropListener` 필드몹 처치 EXP ×2 |
| 필드 드랍 2배         | `DROP_BOOST`      | ✅ `FieldDropListener.grantFieldDrops` 드랍 수량 ×2 (확률 유지) |
| PvP 큐 일시정지       | `PVP_QUEUE_PAUSE` | ✅ `PvpMatchService.enqueue` 진입 즉시 reject |

> Step 2b 범위 주의: EXP/DROP 부스트는 `FieldDropListener`(필드몹) 한정. 보스 보상(`BossRewardService`) 경로는 미적용 — 필요 시 확장.

진입 경로:
- GUI: `/empire-admin` → slot 31 (운영 토글)
- 명령어: `/empire-toggle <flag> [on|off]`, `/empire-toggle list`

### 로그/감시 (slot 33) — **Step 3 구현 완료 (2026-05-30)**

| 기능 | 상태 |
|---|---|
| 최근 강화 시도 로그 | ✅ 100건, 성공/실패/천장 보정 표시 (`InMemoryEnhancementLogHook.logs()` — in-memory, 재시작 시 소실) |
| 최근 거래 로그 | ✅ 경매장 판매 100건 (`AuctionStore.recentSold` — `auction_listings status='sold'`) |
| 최근 PvP 매치 로그 | ✅ `PvpMatchLogRepository.recentMatches` — `pvp_match_log` DB |
| 의심 활동 알림 | ⏳ Step 3+ 보류 (집계/임계 로직 별도 설계 필요) |

- GUI: `AdminLogGui` (54슬롯, 3탭 강화/거래/PvP, 페이지네이션 45/page, 읽기 전용). 진입: `/empire-admin` → slot 33.
- 명령어: `/empire-log [enhance|trade|pvp]` (콘솔 가능, 탭별 최근 10건 텍스트).
- 영속성 주의: 강화 로그만 in-memory (휘발성) — GUI lore·명령어 헤더에 명시.

### 보스 디버그 (slot 24) — **Step 4 구현 완료 (2026-05-30)**

| 기능 | 상태 |
|---|---|
| 진행 중 보스 런 목록 | ✅ `AdminBossGui` (별도 GUI). `BossRunService.activeRuns()` — bossId·리더·파티·페이즈·HP·경과 표시 |
| 강제 종료 | ✅ 좌클릭/`/empire-boss-end` → `endRun(runId, false, "admin_force")` → onRunEnded → `releaseByRunId` (슬롯+참가자 해제) |
| 강제 페이즈 트리거 | ⏳ 보류 — `BossPatternScheduler.enqueueForced` 연동은 Step 4+ |

- GUI: `AdminBossGui` (54슬롯, 진행 중 런 목록, 좌클릭=강제 종료). 진입: `/empire-admin` → slot 24.
- 명령어: `/empire-boss-list` (목록), `/empire-boss-end <runId 앞 8자리>` (강제 종료, 접두어 매칭). 한 핸들러 `AdminBossCommand`가 라벨 분기.
- 한계: MM 보스 엔티티는 잔존 가능 (슬롯 stuck 해소가 1차 목적). 강제 처치/페이즈 트리거는 후속.

### 플레이어 강제 변경 (인스펙트 확장)

| 기능 | 비고 |
|---|---|
| 직업 변경 | `/empire-setclass` 이미 있음 — GUI 통합 검토 |
| 강화 단계 강제 변경 | 슬롯별 enhance 값 ± |
| 재화 지급/회수 | 골드/강화석/큐브 직접 조작 |
| 영지 작위 변경 | 영지 관리 섹션과 중복 가능 |
| 매치/큐 강제 추출 | 현재는 인스펙트 후 외부 명령 사용 |

---

## 우선순위 (제안)

1. **영지 관리** — 1차 시즌 중 영지 분쟁 해결 빈도 가장 높을 듯
2. **로그/감시** — DB에 이미 들어있는 정보를 GUI로 노출만
3. **보스 디버그** — 보스 룬 stuck 처리용
4. **운영 토글** — 이벤트성, 필요 시 빠르게 추가

---

## 비고

- Phase 1 GUI는 PlayerCommandRouter 패턴 외부에서 직접 등록 (`AdminHubCommand` + `AdminGuiListener`)
- `/empire-admin` 권한: `empire.admin` (모든 하위 권한 포함)
- 통계 GUI는 in-memory 카운터만 사용. 실시간 정확도 보장. DB join은 별도 비동기 작업으로 처리
