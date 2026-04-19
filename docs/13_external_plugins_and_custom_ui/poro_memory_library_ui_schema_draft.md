# 포로 서버 "차원도서관" UI + DB + 회수 트리거 + 디스코드 연동

## 문서 목적
4막 복선 7종 회수 공통 시스템 설계. data-schema 1차 초안 기반.

## 배경
- 복선 7종 (수도 1 + 6테마, `poro_act1_theme_foreshadowing_items_draft.md`)
- **복선 칸 UI 탭 이름 = "차원도서관"** 확정
- 6종 수집 `[일곱 결전의 증인]`·7종 완수 `[봉인된 이름을 본 자]` 확정
- 명예의 전당 = **디스코드 봇 조회 + 디스코드 공지** 확정
- 월드 사운드 제거 a안 준수 (풀스크린 연출은 로컬 SFX만)

## 서사 배경
관찰자 공간의 연장이자 **"기록되지 않기를 선택한 페이지들의 책장"**.
- 관찰자는 세계의 결정적 분기를 기록하지만, 차원도서관은 그 기록 이전의 "미확정 흔적"만 담는다
- 복선 아이템은 실제 물건이 아니라 플레이어의 결정이 만들어낸 파동의 응결물
- 7개가 모이면 관찰자조차 읽지 못한 한 줄이 드러난다 — `봉인된 이름`
- UI 상단 플레이버: *"관찰자의 손이 닿지 않는 책장. 네가 남긴 흔적이 여기에 모인다."*

---

## 1. UI 설계

### 메뉴 위치 — 독립 카테고리 (권고)
Shift+좌클릭 메뉴 구조: `[캐릭터] [인벤토리] [월렛] [창고] [차원도서관] [설정]`

**독립 카테고리 근거**:
- 월렛·창고와 저장소 분리 → "지갑 하위" 혼동 유발
- 캐릭터 하위에 두면 스탯·장비 탭과 성격 달라 정보 밀도 불일치
- 복선 7종은 시즌/막 단위 메타 서사 오브젝트 → 독립 탭이 서사적으로도 맞음

### 탭 내부 구성 (7슬롯)
```
┌────────────── 차원도서관 ──────────────┐
│ "관찰자의 손이 닿지 않는 책장.          │
│  네가 남긴 흔적이 여기에 모인다."        │
│                                          │
│   ┌─ 중앙 상단 (수도 전용) ─┐             │
│   │  [ 식어버린 황성 파편 ]  │             │
│   │  3막 결전 보스 회수      │             │
│   └─────────────────────────┘             │
│                                          │
│   ───── 6테마 2×3 그리드 ─────            │
│   ┌─동부─┐ ┌─서부─┐ ┌─남부─┐             │
│   │ 눈물 │ │풍향계│ │  재  │             │
│   └──────┘ └──────┘ └──────┘             │
│   ┌─북부─┐ ┌사하르┐ ┌아르카논┐           │
│   │  숨  │ │ 인장 │ │  시계  │           │
│   └──────┘ └──────┘ └────────┘           │
│                                          │
│   진행도: 5 / 7                          │
│   [일곱 결전의 증인 잠금]                 │
└──────────────────────────────────────────┘
```

### 슬롯 상태 3단계
- **LOCKED (미획득)**: 회색 실루엣 + 툴팁에 `reveal_hint` (이름 `???` 마스킹, 테마명만)
- **OBTAINED (획득·미회수)**: 테마 컬러 틴트 + 은은한 파동 이펙트 (2~3% 알파 호흡)
- **RECLAIMED (회수됨)**: 골드 테두리 2px + 테마 엑센트, 라벨 `[테마 보스명]의 기억`

### 슬롯 클릭 상세 패널
- 이름 / 테마 / 심은 위치 / 심은 시각 / 회수 시점 / 회수 시각
- 회수 대사 인용 / 설명

### 7종 완수 풀 스크린 연출
- **트리거**: 마지막 복선 RECLAIMED 후 **플레이어가 차원도서관 탭을 처음 열었을 때** (보스 처치 직후 아님 — 플레이어가 혼자 마주하도록)
- 시퀀스:
  1. 배경 암전 (85% 알파, 2초 페이드인)
  2. 7슬롯이 중앙 회전하며 모이는 파티클 (3초)
  3. 중앙 채팅 서사 박스 (UI 전용, 월드 채팅 아님, 3~4줄 칭호 부여 서사)
  4. 하단 골드 텍스트: `[봉인된 이름을 본 자]` 칭호 획득
  5. 로컬 SFX만 (월드 사운드 제거 준수), BGM은 낮춤만
- 1회성, 이후 입장 시 스킵

---

## 2. DB 스키마

### `foreshadow_item_master` (정적, 7행 고정)
| 컬럼 | 타입 | 설명 |
|---|---|---|
| `item_id` | SMALLINT UNSIGNED PK | 1~7 |
| `item_code` | VARCHAR(48) UNIQUE | 예: `FRAGMENT_CAPITAL_ASHEN` |
| `theme` | ENUM | CAPITAL/EAST/WEST/SOUTH/NORTH/SAHAR/ARCANON |
| `boss_id` | VARCHAR(64) | 회수 트리거 보스 ID |
| `name_kr` | VARCHAR(64) | 표시 이름 |
| `description` | VARCHAR(512) | 설명 |
| `seeded_location` | VARCHAR(128) | 심은 위치 |
| `reclaim_boss_id` | VARCHAR(64) | 회수 보스 (대부분 `boss_id`와 동일) |
| `reclaim_dialogue_key` | VARCHAR(128) | 회수 대사 i18n 키 |
| `is_key_item` | TINYINT(1) | 수도 파편만 1 |
| `grade_coefficient` | DECIMAL(4,2) | 칭호 가중치 |
| `reveal_hint` | VARCHAR(256) | 미획득 힌트 |
| `display_order` | TINYINT | 정렬 순서 |

인덱스: `idx_theme`, `idx_boss_id`, `idx_reclaim_boss_id`. 제약: `is_key_item=1` 정확히 1행.

### `player_foreshadow_state` (런타임)
| 컬럼 | 타입 | 설명 |
|---|---|---|
| `player_uuid` | BINARY(16) PK | |
| `item_id` | SMALLINT FK PK | |
| `state` | ENUM | LOCKED/OBTAINED/RECLAIMED |
| `obtained_at` | DATETIME | 복선 획득 시각 |
| `reclaimed_at` | DATETIME | 회수 시각 |
| `obtained_context` | JSON | 심은 시점 플래그 스냅샷 |
| `reclaimed_context` | JSON | 회수 시점 컨텍스트 |
| `source_quest_id` | VARCHAR(64) | 심은 퀘스트 ID |
| `version` | INT | 낙관적 락 |

인덱스: `idx_player_state (player_uuid, state)`, `idx_reclaimed_at`. 상태 역행 금지 (애플리케이션 레이어).

### `foreshadow_collection_title` (칭호 조건)
| 컬럼 | 타입 | 설명 |
|---|---|---|
| `title_id` | SMALLINT PK | |
| `title_code` | VARCHAR(48) UNIQUE | 예: `WITNESS_OF_SEVEN_BATTLES` |
| `title_name` | VARCHAR(64) | |
| `required_item_count` | TINYINT | 6 또는 7 |
| `required_items` | JSON | 필수 item_code 배열 |
| `rank_tier` | TINYINT | 정렬·상위호환 |
| `announce_discord` | TINYINT(1) | 디스코드 공지 대상 |

초기 2행:
- `[일곱 결전의 증인]` 6종·rank_tier=1·announce=0
- `[봉인된 이름을 본 자]` 7종·rank_tier=2·announce=1

### `player_title_award` (칭호 수여 로그, 기존 시스템 재활용)
`player_uuid + title_id` PK, `awarded_at`, `awarded_reason`

---

## 3. 회수 트리거 로직

### 이벤트 흐름
```
BossKillEvent
  ↓
ForeshadowReclaimListener.onBossKill(event)
  ↓
foreshadow_item_master WHERE reclaim_boss_id = event.bossId 조회
  ↓ (0건 종료)
처치 기여자 플레이어 리스트 산출 (파티 + 유효 기여치)
  ↓
각 플레이어별 독립 처리 (병렬):
  ├─ player_foreshadow_state 조회
  ├─ state = OBTAINED → RECLAIMED 전환 + reclaimed_at + reclaimed_context
  ├─ state = LOCKED → 스킵 (복선 미심기)
  └─ state = RECLAIMED → 중복 스킵
  ↓
회수 성공 시:
  ├─ UI 토스트 이벤트 (해당 플레이어 한정)
  ├─ 칭호 달성 체크 (6종/7종)
  └─ 달성 시 TitleAwardEvent + 디스코드 공지 큐 + 풀스크린 연출 펜딩
```

### 상태 전이 규칙
- LOCKED → OBTAINED: 복선 심기 로직 (본 문서 범위 외)
- OBTAINED → RECLAIMED: 보스 처치 이벤트에서만 허용
- 역행 금지 (RECLAIMED 이후 어드민만 되돌림)
- 동일 보스 재처치 시 RECLAIMED 상태면 무동작

### 멀티 플레이어 처리
- 파티 전원 각자 독립 회수 판정, `player_foreshadow_state` row 완전 독립
- 복선 미심기 플레이어는 LOCKED 상태 자동 스킵
- 기여치는 기존 보스 처치 이벤트 유효 참여자 기준 재사용

---

## 4. 디스코드 연동

### 7종 완수자 자동 공지 — 타 유저 조회 전역 거부 정책 반영
**사용자 결정 (전역)**: 타 유저 조회·노출 **전역 거부**. 7종 완수자 공지도 개인 식별 정보(플레이어명) 직접 노출은 **금지**. 익명화 또는 본인 동의 시만 공개로 조정.

**옵션 A — 익명 + 통계 공지 (권장, 프라이버시 기본 준수)**
```
🏆 **[봉인된 이름을 본 자]** 칭호 발생

한 명의 조사자가 4막 복선 7종을 모두 회수하였습니다.
시즌 누적 완수자: <N>명

> "관찰자의 손이 닿지 않는 책장에 한 줄이 더 새겨졌다."
```
- 플레이어명 노출 없음, 누적 통계만 제공

**옵션 B — 본인 동의 시만 공개 공지**
- 7종 회수 직후 **인게임 모달**: "이 성취를 디스코드 공지에 공개하시겠습니까?" 수락/거절
- 수락 시 플레이어명 포함 공지, 거절 시 옵션 A(익명) 공지
- 기본값은 거절 (Opt-in)

- **채널**: `#포로-서사` (서버 운영 공지)
- 중복 방지: `player_title_award` 기록 이후 발행, 동일 플레이어 재발행 금지
- 실패 시 재시도 큐: `discord_announcement_queue`
- **사용자 최종 결정 대기**: A(익명 기본) vs B(본인 동의 시 공개) 중 택일

### `/차원도서관 [<플레이어>]` 봇 명령 (타 유저 조회 부활, 약관 동의 기반)
**사용자 정책 번복**: 이전 "전역 거부" 철회. 약관 동의 하에 **타 유저 조회 허용**. 인자 생략 시 본인 조회, 지정 시 타 유저 조회.
```
📚 차원도서관 — <본인>
진행도: 5 / 7

[수도]   식어버린 황성 파편        🟡 획득 (미회수)
[동부]   침묵한 정령의 눈물        ✅ 회수  2026-04-14 23:02
[서부]   부서진 풍향계 조각        ✅ 회수  2026-04-10 20:44
[남부]   식지 않는 재 한 줌        ✅ 회수  2026-04-12 22:15
[북부]   얼어붙은 숨 한 조각       ✅ 회수  2026-04-13 19:30
[사하르] 이름이 지워진 계약 인장   ⬜ 미획득
[아르카논] 울리지 않는 공명 시계   ✅ 회수  2026-04-15 01:08

칭호: [일곱 결전의 증인] 보유 중
```
- 범례: ⬜ LOCKED / 🟡 OBTAINED / ✅ RECLAIMED
- **본인 Discord 계정과 게임 계정 연동** 필수 (연동 안 된 계정은 본인 조회도 불가)
- 약관 동의 완료 플레이어만 조회 대상으로 노출됨 (약관 미동의자는 조회 결과에 "비공개"로 표시)

### 시즌 종료 랭킹 공지 — 타 유저 조회 거부 정책 반영
**사용자 결정**: Top 10 등 랭킹에 플레이어명 직접 노출 금지. 익명화·통계 위주로 조정.
```
🏁 시즌 <N> 종료 — 차원도서관 집계
7종 완수자: <N>명 / 6종 수집자: <N>명
최초 완수 시각: <YYYY-MM-DD HH:mm:ss KST>
평균 완수 시각: <YYYY-MM-DD>

> "이번 시즌의 책장은 조용히 덮인다."
```
- 개인 랭킹(Top 10) **삭제**, 통계 집계만 공지
- 본인 완수 시각은 `/차원도서관` 명령에서 본인만 확인 가능
- 본인 동의 시 공개 공지(옵션 B) 누적 리스트는 별도 운영(본인 Opt-in 한정)

### 운영 API
- `GET /admin/dashboard/foreshadow_progress` (전체 집계)
- `GET /admin/dashboard/foreshadow_progress/{player_uuid}` (개별)
- `GET /admin/foreshadow/leaderboard?season=<N>&limit=10`
- `POST /admin/foreshadow/override` (감사 로그 필수)

---

## 5. 기존 시스템과의 통합
- **월렛 분리**: 복선은 월렛 스키마에 없음, 인벤/창고/우편 노출 안 함
- **창고 분리**: 창고 스캔 제외, 순수 DB 상태 오브젝트
- **플래그 저장소 v0.1과의 역할 분리**: 복선은 3상태+타임스탬프+JSON 컨텍스트 전용 테이블 유지. 단, 복선 심기 트리거는 `player_flag` 참조 가능 (예: `FLAG_EAST_CHOICE_SPIRIT=true` 시 심기)

## 6. 운영 포인트
- 일 1회 `state` 제약 일관성 배치 검증
- 어드민 도구: 플레이어별 수동 조정 UI (버그 복구), 감사 로그 필수
- **시즌 전환 시 복선/칭호 영구 보존 vs 리셋**: 오픈 질문
- 성능: 플레이어당 최대 7행, 부하 없음. 랭킹 쿼리는 `reclaimed_at` 인덱스 필수
- 로컬라이제이션: `reclaim_dialogue_key`로 언어별 대사 테이블 분리

## 7. 오픈 질문
1. **수도 파편 회수 트리거 보스**: 1~3막 어느 보스와 연결할지 (현재 4막 진입 키만 확정)
2. **`/차원도서관 <타플레이어>` 조회 허용 여부**: 본인만 / 길드원만 / 전체 공개
3. **시즌 리셋 정책**: 복선·칭호 영구 보존 vs 리셋 (명예의 전당 구조와 연계)
4. **7종 완수 풀스크린 연출 파티클 리소스**: 기존 에셋 재활용 여부 (아트·이펙트 담당 협의)
5. **6종 칭호 "6테마 세트" 정의**: 수도 제외 6테마 고정 vs 수도 포함 임의 6종 허용 (현재: 수도 제외 6테마 고정)
6. **복선 심기 실패 시 복구 정책**: 해당 시점 퀘스트 놓친 플레이어 영원히 LOCKED vs 추후 보상 루트
7. **월드 사운드 제거 하 로컬 SFX 허용 범위**: 풀스크린 연출 SFX가 정책 범위 내인지 재확인

## 8. 다음 추천 작업
- 복선 **심기(seed) 트리거** 설계 — 언제·어디서·어떤 플래그 조건으로 LOCKED → OBTAINED 전환되는지 테마별 상세화
- 4막 최종 보스 입장 게이트 `is_key_item=1` 보유 체크 로직 위치 결정
- 디스코드 봇 명령 권한 모델 (길드 역할 기반) 설계

## 상위 문서 참조
- 복선 7종 전체: `../08_story_and_quests/poro_act1_theme_foreshadowing_items_draft.md`
- 동·북 서브 퀘스트: `../08_story_and_quests/act1_capital_main/poro_act1_theme_sub_foreshadowing_quests_draft.md`
- 분기 B 메인 13 (수도 파편 심기): `../08_story_and_quests/act1_capital_main/poro_act1_branch_b_hidden_m8_15_scene_cards.md`
- 마스터플래닝: `../poro_master_planning.md`
