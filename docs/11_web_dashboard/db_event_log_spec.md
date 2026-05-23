# 경제·전투 이벤트 로그 DB 테이블 설계

> **[STATUS: DRAFT]** — `empire.db`에 추가할 이벤트 로그 테이블 설계.
>
> 기준일: 2026-05-23
>
> 전제: 기존 `empire.db` 구조와 충돌하지 않도록 신규 테이블만 추가한다.

---

## 0. 설계 원칙

| 원칙 | 내용 |
|---|---|
| 충돌 방지 | 기존 테이블을 수정하지 않는다. 신규 테이블만 추가한다 |
| 불변 로그 | 이벤트 로그는 INSERT 전용이다. UPDATE/DELETE 없음 |
| 집계 분리 | 원시 로그(event log)와 일별 집계 스냅샷(daily snapshot)을 분리한다 |
| SQLite 적합 | 복잡한 JSON 컬럼은 피한다. 정규화 우선 |
| 타임스탬프 | 모든 테이블의 `created_at`은 Unix epoch 정수 (초) |

---

## 1. 골드 이벤트 로그 — `gold_event_log`

골드 생성·소모가 발생할 때마다 한 행을 INSERT한다.

```sql
CREATE TABLE gold_event_log (
    id             INTEGER PRIMARY KEY AUTOINCREMENT,
    created_at     INTEGER NOT NULL,
    player_uuid    TEXT    NOT NULL,
    event_type     TEXT    NOT NULL,
    amount         INTEGER NOT NULL,
    balance_after  INTEGER NOT NULL
);

CREATE INDEX idx_gold_event_log_created_at   ON gold_event_log(created_at);
CREATE INDEX idx_gold_event_log_player_uuid  ON gold_event_log(player_uuid);
CREATE INDEX idx_gold_event_log_event_type   ON gold_event_log(event_type);
```

### `event_type` 값 목록

| event_type | 방향 | 설명 |
|---|---|---|
| `mob_kill` | 생성 (+) | 몹 처치 골드 드랍 |
| `boss_kill` | 생성 (+) | 보스 기여도 보상 |
| `farm_sell` | 생성 (+) | NPC 작물 판매 |
| `auction_sell` | 생성 (+) | 경매소 판매 수입 |
| `enhancement` | 소모 (-) | 강화 시도 골드 비용 |
| `cube_use` | 소모 (-) | 큐브 사용 500G |
| `title_purchase` | 소모 (-) | 작위 구매 |
| `convenience_unlock` | 소모 (-) | 영지 편의 해금 |
| `auction_fee` | 소모 (-) | 경매소 수수료 5% |
| `transfer_ticket` | 소모 (-) | 전승권 구매 |
| `item_rename` | 소모 (-) | 장비 이름 변경권 |
| `admin_grant` | 생성 (+) | 운영자 지급 |
| `admin_deduct` | 소모 (-) | 운영자 차감 |
| `other` | 양방향 | 분류되지 않은 기타 |

`amount`는 항상 양수. 방향은 `event_type`으로 판단한다.

---

## 2. 강화석 이벤트 로그 — `enhancement_stone_log`

강화석은 DB 가상 재화이므로 변동이 발생할 때마다 기록한다.

```sql
CREATE TABLE enhancement_stone_log (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    created_at      INTEGER NOT NULL,
    player_uuid     TEXT    NOT NULL,
    stone_type      TEXT    NOT NULL,
    event_type      TEXT    NOT NULL,
    amount          INTEGER NOT NULL,
    balance_after   INTEGER NOT NULL
);

CREATE INDEX idx_es_log_created_at   ON enhancement_stone_log(created_at);
CREATE INDEX idx_es_log_player_uuid  ON enhancement_stone_log(player_uuid);
```

### `stone_type` 값

| stone_type | 설명 |
|---|---|
| `weapon` | 무기 강화석 |
| `armor` | 방어구 강화석 |

### `event_type` 값

| event_type | 방향 | 설명 |
|---|---|---|
| `mob_kill` | 생성 (+) | 몹 처치 시 직접 적립 |
| `boss_kill` | 생성 (+) | 보스 기여도 보상 |
| `enhancement_attempt` | 소모 (-) | 강화 시도 시 소모 |
| `auction_sell` | 소모 (-) | 경매소 강화석 판매 (판매자 차감) |
| `auction_buy` | 생성 (+) | 경매소 강화석 구매 (구매자 적립) |
| `admin_grant` | 생성 (+) | 운영자 지급 |
| `admin_deduct` | 소모 (-) | 운영자 차감 |

`auction_sell` / `auction_buy`는 항상 쌍으로 발생한다. 서버 전체 강화석 총량은 변하지 않고 플레이어 간 이동만 발생한다.

---

## 3. 큐브 이벤트 로그 — `cube_event_log`

큐브 조각 변환 및 큐브 사용을 기록한다.

```sql
CREATE TABLE cube_event_log (
    id                   INTEGER PRIMARY KEY AUTOINCREMENT,
    created_at           INTEGER NOT NULL,
    player_uuid          TEXT    NOT NULL,
    event_type           TEXT    NOT NULL,
    cube_fragment_delta  INTEGER NOT NULL DEFAULT 0,
    cube_delta           INTEGER NOT NULL DEFAULT 0,
    result_grade         TEXT,
    gold_spent           INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX idx_cube_log_created_at   ON cube_event_log(created_at);
CREATE INDEX idx_cube_log_player_uuid  ON cube_event_log(player_uuid);
```

### `event_type` 값

| event_type | 설명 |
|---|---|
| `fragment_drop` | 큐브 조각 드랍 (mob/boss) |
| `fragment_to_cube` | 조각 10개 → 큐브 1개 자동 변환 |
| `cube_use` | 큐브 사용. `result_grade`와 `gold_spent` 채움 |

`result_grade`: `rare` / `epic` / `unique` / `legendary` / `null` (큐브 사용이 아닌 경우)

---

## 4. 아이템 발행 로그 — `item_issue_log`

잠재 확정 및 전승권 사용을 기록한다.

```sql
CREATE TABLE item_issue_log (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    created_at      INTEGER NOT NULL,
    player_uuid     TEXT    NOT NULL,
    event_type      TEXT    NOT NULL,
    item_slot       TEXT,
    potential_grade TEXT,
    gold_spent      INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX idx_item_issue_log_created_at   ON item_issue_log(created_at);
CREATE INDEX idx_item_issue_log_player_uuid  ON item_issue_log(player_uuid);
CREATE INDEX idx_item_issue_log_event_type   ON item_issue_log(event_type);
```

### `event_type` 값

| event_type | 설명 |
|---|---|
| `potential_confirmed` | 큐브 사용으로 잠재 등급 확정. `potential_grade` 채움 |
| `transfer_basic` | 기본 전승 (0G) |
| `transfer_grade` | 등급전승권 사용 (100,000G) |
| `transfer_substat` | 세부스탯전승권 사용 (100,000G) |
| `trace_used` | 강화 흔적 사용 (별/달/태양은 `item_slot`에 기록) |

`item_slot`: `weapon` / `helmet` / `chest` / `leggings` / `boots`

---

## 5. 보스 클리어 로그 — `boss_clear_log`

보스 전투 결과를 기록한다.

```sql
CREATE TABLE boss_clear_log (
    id                   INTEGER PRIMARY KEY AUTOINCREMENT,
    created_at           INTEGER NOT NULL,
    boss_id              TEXT    NOT NULL,
    result               TEXT    NOT NULL,
    clear_time_seconds   INTEGER,
    party_size           INTEGER NOT NULL,
    party_total_damage   INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX idx_boss_clear_log_created_at ON boss_clear_log(created_at);
CREATE INDEX idx_boss_clear_log_boss_id    ON boss_clear_log(boss_id);
```

`result`: `clear` / `timeout` / `wipe`

`clear_time_seconds`: `result = 'clear'`일 때만 값 있음. 나머지 NULL.

---

## 6. 보스 전투 참여자 로그 — `boss_combat_log`

보스 전투의 개인별 DPS를 기록한다.

```sql
CREATE TABLE boss_combat_log (
    id                 INTEGER PRIMARY KEY AUTOINCREMENT,
    clear_log_id       INTEGER NOT NULL REFERENCES boss_clear_log(id),
    player_uuid        TEXT    NOT NULL,
    weapon_class       TEXT    NOT NULL,
    engraving          TEXT,
    total_damage       INTEGER NOT NULL,
    dps                INTEGER NOT NULL,
    max_single_hit     INTEGER NOT NULL DEFAULT 0,
    avg_il             INTEGER
);

CREATE INDEX idx_boss_combat_log_clear_log_id ON boss_combat_log(clear_log_id);
CREATE INDEX idx_boss_combat_log_player_uuid  ON boss_combat_log(player_uuid);
CREATE INDEX idx_boss_combat_log_weapon_class ON boss_combat_log(weapon_class);
```

`weapon_class`: `sword` / `axe` / `spear` / `crossbow` / `scythe` / `staff`

`engraving`: `A` / `B` / NULL (각인 미장착)

`avg_il`: 전투 시점의 플레이어 장비 평균 IL. 스냅샷으로 기록.

---

## 7. 일별 경제 스냅샷 — `daily_economy_snapshot`

자정에 집계하여 INSERT한다. 원시 로그의 집계 결과를 저장한다.

```sql
CREATE TABLE daily_economy_snapshot (
    id                          INTEGER PRIMARY KEY AUTOINCREMENT,
    snapshot_date               TEXT    NOT NULL UNIQUE,

    -- 골드 총량 및 흐름
    gold_total                  INTEGER NOT NULL,
    gold_created                INTEGER NOT NULL,
    gold_consumed               INTEGER NOT NULL,

    -- 골드 수입 세부 분류
    gold_created_mob_kill       INTEGER NOT NULL DEFAULT 0,
    gold_created_boss_kill      INTEGER NOT NULL DEFAULT 0,
    gold_created_farm_sell      INTEGER NOT NULL DEFAULT 0,
    gold_created_auction_sell   INTEGER NOT NULL DEFAULT 0,
    gold_created_admin_grant    INTEGER NOT NULL DEFAULT 0,

    -- 골드 소모 세부 분류
    gold_consumed_enhancement   INTEGER NOT NULL DEFAULT 0,
    gold_consumed_cube          INTEGER NOT NULL DEFAULT 0,
    gold_consumed_title         INTEGER NOT NULL DEFAULT 0,
    gold_consumed_convenience   INTEGER NOT NULL DEFAULT 0,
    gold_consumed_auction_fee   INTEGER NOT NULL DEFAULT 0,
    gold_consumed_other         INTEGER NOT NULL DEFAULT 0,

    -- 큐브
    cube_fragment_converted     INTEGER NOT NULL DEFAULT 0,
    cube_used                   INTEGER NOT NULL DEFAULT 0,
    cube_result_rare            INTEGER NOT NULL DEFAULT 0,
    cube_result_epic            INTEGER NOT NULL DEFAULT 0,
    cube_result_unique          INTEGER NOT NULL DEFAULT 0,
    cube_result_legendary       INTEGER NOT NULL DEFAULT 0,

    -- 강화석 일별 흐름
    weapon_stone_gained         INTEGER NOT NULL DEFAULT 0,
    weapon_stone_consumed       INTEGER NOT NULL DEFAULT 0,
    weapon_stone_auction_sold   INTEGER NOT NULL DEFAULT 0,
    armor_stone_gained          INTEGER NOT NULL DEFAULT 0,
    armor_stone_consumed        INTEGER NOT NULL DEFAULT 0,
    armor_stone_auction_sold    INTEGER NOT NULL DEFAULT 0,

    -- 강화석 서버 전체 보유량 스냅샷 (balance_after 최신값 집계)
    weapon_stone_total          INTEGER NOT NULL DEFAULT 0,
    armor_stone_total           INTEGER NOT NULL DEFAULT 0,

    -- 아이템 발행
    items_rare                  INTEGER NOT NULL DEFAULT 0,
    items_epic                  INTEGER NOT NULL DEFAULT 0,
    items_unique                INTEGER NOT NULL DEFAULT 0,
    items_legendary             INTEGER NOT NULL DEFAULT 0,

    created_at                  INTEGER NOT NULL
);
```

`snapshot_date` 형식: `"2026-05-23"` (YYYY-MM-DD)

이 테이블은 API가 일별 그래프를 반환할 때 원시 로그 대신 참조한다. 집계 성능을 확보하기 위한 캐싱 역할.

---

## 8. 테이블 목록 요약

| 테이블 | 역할 | INSERT 주체 |
|---|---|---|
| `gold_event_log` | 골드 생성·소모 원시 로그 | EmpireRPG 이벤트 발생 시 |
| `enhancement_stone_log` | 강화석 적립·소모 원시 로그 | EmpireRPG 이벤트 발생 시 |
| `cube_event_log` | 큐브 조각·큐브 흐름 원시 로그 | EmpireRPG 이벤트 발생 시 |
| `item_issue_log` | 잠재 확정·전승권 원시 로그 | EmpireRPG 이벤트 발생 시 |
| `boss_clear_log` | 보스 전투 결과 | EmpireRPG 보스 종료 시 |
| `boss_combat_log` | 보스 전투 개인 DPS | EmpireRPG 보스 종료 시 |
| `server_perf_log` | TPS·핑·동접 수 시간별 스냅샷 | EmpireRPG 1분 주기 스케줄러 |
| `field_activity_log` | 필드별 처치 수·자원 생산 원시 로그 | EmpireRPG 몹 처치 이벤트 발생 시 |
| `daily_economy_snapshot` | 일별 집계 스냅샷 | EmpireRPG 자정 스케줄러 |
| `bug_report` | 디스코드 버그 제보 원시 로그 | 디스코드 봇 `/버그제보` 접수 시 |

---

## 8. 서버 퍼포먼스 로그 — `server_perf_log`

TPS·평균 핑·동접 수를 1분 단위로 기록한다. 피크 시간대 분석과 TPS 저하 감지에 사용한다.

```sql
CREATE TABLE server_perf_log (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    created_at    INTEGER NOT NULL,
    tps           REAL    NOT NULL,
    avg_ping_ms   INTEGER NOT NULL,
    online_count  INTEGER NOT NULL
);

CREATE INDEX idx_server_perf_log_created_at ON server_perf_log(created_at);
```

| 컬럼 | 설명 |
|---|---|
| `tps` | 해당 시각 TPS (0~20.0) |
| `avg_ping_ms` | 접속 중인 플레이어 평균 핑 (ms) |
| `online_count` | 해당 시각 접속자 수 |

> 1분 1행. 45일 기준 약 64,800행. SQLite 부담 없음.

---

## 9. 필드 활동 로그 — `field_activity_log`

몹 처치 시마다 필드 ID와 생산 자원을 기록한다.

```sql
CREATE TABLE field_activity_log (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    created_at      INTEGER NOT NULL,
    field_id        TEXT    NOT NULL,
    player_uuid     TEXT    NOT NULL,
    mob_type        TEXT    NOT NULL,
    gold_earned     INTEGER NOT NULL DEFAULT 0,
    stone_earned    INTEGER NOT NULL DEFAULT 0,
    cube_piece_earned INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX idx_field_activity_created_at ON field_activity_log(created_at);
CREATE INDEX idx_field_activity_field_id   ON field_activity_log(field_id);
CREATE INDEX idx_field_activity_player     ON field_activity_log(player_uuid);
```

| 컬럼 | 설명 |
|---|---|
| `field_id` | 필드 식별자 (예: `field_grassland`, `field_mine`) |
| `mob_type` | 몹 종류 (예: `wolf_alpha`, `stone_golem`) |
| `gold_earned` | 해당 처치로 생성된 골드 |
| `stone_earned` | 해당 처치로 적립된 강화석 |
| `cube_piece_earned` | 해당 처치로 드랍된 큐브 조각 수 |

> 집계 단위: 일별 스냅샷에 `field_id` 기준 SUM으로 집계. 원시 로그는 쿼리 성능상 7일 이후 아카이브 검토.

---

## 10. 버그 제보 로그 — `bug_report`

디스코드 봇 `/버그제보` 명령어 접수 시 INSERT된다. `id` AUTOINCREMENT가 공개 접수번호(`BUG-{id}`)로 사용된다.

```sql
CREATE TABLE bug_report (
    id                INTEGER PRIMARY KEY AUTOINCREMENT,
    created_at        INTEGER NOT NULL,
    discord_user_id   TEXT    NOT NULL,
    discord_username  TEXT    NOT NULL,
    player_uuid       TEXT,
    title             TEXT    NOT NULL,
    reproduce_steps   TEXT    NOT NULL,
    expected_result   TEXT,
    actual_result     TEXT,
    severity          TEXT    NOT NULL,
    status            TEXT    NOT NULL DEFAULT 'open',
    resolved_at       INTEGER,
    resolver_id       TEXT
);

CREATE INDEX idx_bug_report_created_at ON bug_report(created_at);
CREATE INDEX idx_bug_report_status     ON bug_report(status);
```

### `severity` 값

| severity | 설명 |
|---|---|
| `crash` | 서버·클라이언트 크래시 |
| `functional` | 기능 오류 |
| `display` | 표시·UI 버그 |
| `other` | 기타 |

### `status` 값

| status | 설명 |
|---|---|
| `open` | 접수됨 (초기) |
| `in_review` | 확인 중 |
| `resolved` | 수정 완료 |
| `dismissed` | 중복·기각 |

`player_uuid`: 인증 유저면 연동된 UUID 기록. 미인증 제보자는 NULL.

`resolver_id`: 상태를 변경한 운영진 디스코드 ID.

---

## 11. 오픈 질문

- `gold_event_log`의 일별 행 수 추정: 활성 유저 50명 × kills/min 45 × 60분 × 시간당 평균 골드 드랍 이벤트 수. 45일 누적 시 수백만 행 가능. SQLite 한계 내에 있는지 사전 검토 필요.
- 원시 로그 보존 기간 정책이 없다. 시즌 종료 후 아카이브 또는 삭제 기준을 확정해야 한다.
- `daily_economy_snapshot`은 자정 스케줄러가 원시 로그를 집계하는 방식인지, EmpireRPG가 실시간으로 누적 업데이트하는 방식인지 구현 방향 미확정.
