# 웹 대시보드 API 엔드포인트 목록

> **[STATUS: DRAFT]** — 대시보드에서 호출하는 EmpireRPG HTTP API 엔드포인트 설계.
>
> 기준일: 2026-05-23
>
> 기반: EmpireRPG HTTP API, 포트 8765. 모든 엔드포인트는 Bearer 토큰 인증 필요.

---

## 공통 규칙

| 항목 | 규칙 |
|---|---|
| 인증 | `Authorization: Bearer <token>` 헤더 필수 |
| 응답 형식 | JSON |
| 에러 구조 | `{"error": "message", "code": N}` |
| 날짜 형식 | ISO 8601 (`2026-05-23`) |
| 타임스탬프 | Unix epoch (초 단위) |
| 기본 Base URL | `http://localhost:8765` |

---

## 1. 서버 현황 (`/api/dashboard/status`)

### `GET /api/dashboard/status`

현재 접속자 수, 서버 업타임, 당일 요약 수치.

**응답**

```json
{
  "online_count": 12,
  "uptime_seconds": 86400,
  "today": {
    "gold_created": 1240000,
    "gold_consumed": 980000,
    "gold_net": 260000,
    "items_issued": {
      "rare": 45,
      "epic": 12,
      "unique": 3,
      "legendary": 0
    }
  },
  "recent_boss_clears": [
    {
      "timestamp": 1748000000,
      "boss_id": "season_boss_1",
      "boss_name": "균열의 수문장",
      "clear_time_seconds": 423,
      "party_size": 4,
      "party_weapons": ["sword", "axe", "crossbow", "staff"]
    }
  ]
}
```

---

## 2. 경제 — 골드 흐름 (`/api/economy/gold`)

### `GET /api/economy/gold/daily`

일별 골드 생성/소모/총량 시계열.

**파라미터**

| 파라미터 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `from` | string | 필수 | 시작일 (`2026-05-01`) |
| `to` | string | 필수 | 종료일 (`2026-05-23`) |

**응답**

```json
{
  "days": [
    {
      "date": "2026-05-01",
      "gold_total": 5200000,
      "gold_created": 350000,
      "gold_consumed": 280000,
      "gold_net": 70000,
      "breakdown_created": {
        "mob_kill": 210000,
        "boss_kill": 48000,
        "farm_sell": 62000,
        "auction_sell": 28000,
        "admin_grant": 2000
      },
      "breakdown_consumed": {
        "enhancement": 120000,
        "cube_use": 95000,
        "title_purchase": 40000,
        "convenience_unlock": 0,
        "auction_fee": 18000,
        "other": 7000
      }
    }
  ]
}
```

### `GET /api/economy/gold/anomaly`

경제 이상 감지 지표 현재값.

**응답**

```json
{
  "season_day": 12,
  "daily_net_rate": 0.032,
  "top5_share": 0.41,
  "cube_consume_ratio": 0.62,
  "stone_consume_ratio": 0.55,
  "legendary_today": 1,
  "auction_volume_ratio": 1.2,
  "metric_status": {
    "daily_net_rate":     { "active": true,  "active_since_day": 5  },
    "top5_share":         { "active": true,  "active_since_day": 7  },
    "cube_consume_ratio": { "active": true,  "active_since_day": 5  },
    "stone_consume_ratio":{ "active": true,  "active_since_day": 5  },
    "legendary_today":    { "active": true,  "active_since_day": 1  },
    "auction_volume_ratio":{ "active": false, "active_since_day": 8  }
  },
  "alerts": [
    {
      "metric": "daily_net_rate",
      "level": "warning",
      "message": "일별 골드 순증가율 3.2% — 기준 5% 미만"
    }
  ]
}
```

`season_day`: 서버 오픈일 기준 경과 일수. 클라이언트가 `metric_status[*].active_since_day`와 비교해 비활성 지표를 회색 표시.

`alerts` 배열은 `active: true`인 지표 중 경보 조건 충족 항목만 포함한다. 정상 또는 비활성이면 해당 지표는 배열에 없다.

`level` 값: `"warning"` (황색) / `"danger"` (적색)

### `GET /api/economy/gold/players`

플레이어별 골드 보유/생성/소모 목록.

**파라미터**

| 파라미터 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `sort` | string | 선택 | `balance` / `created` / `consumed`. 기본 `balance` |
| `order` | string | 선택 | `desc` / `asc`. 기본 `desc` |
| `limit` | int | 선택 | 기본 50, 최대 200 |
| `offset` | int | 선택 | 기본 0 |
| `search` | string | 선택 | 플레이어명 부분 검색 |

**응답**

```json
{
  "total": 87,
  "players": [
    {
      "uuid": "...",
      "name": "플레이어명",
      "gold_balance": 450000,
      "gold_created_total": 2100000,
      "gold_consumed_total": 1650000,
      "gold_net_7d": 38000
    }
  ]
}
```

---

## 3. 경제 — 큐브 흐름 (`/api/economy/cube`)

### `GET /api/economy/cube/daily`

일별 큐브 생성(조각→큐브 변환)과 사용 수.

**파라미터**

| 파라미터 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `from` | string | 필수 | 시작일 |
| `to` | string | 필수 | 종료일 |

**응답**

```json
{
  "days": [
    {
      "date": "2026-05-01",
      "cube_fragment_converted": 340,
      "cube_used": 210,
      "result_distribution": {
        "rare": 105,
        "epic": 72,
        "unique": 28,
        "legendary": 5
      }
    }
  ]
}
```

---

## 4. 경제 — 강화석 흐름 (`/api/economy/enhancement-stone`)

### `GET /api/economy/enhancement-stone/daily`

일별 강화석 적립(처치)/소모(강화 시도) 수.

**파라미터**

| 파라미터 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `from` | string | 필수 | 시작일 |
| `to` | string | 필수 | 종료일 |

**응답**

```json
{
  "days": [
    {
      "date": "2026-05-01",
      "weapon_stone_total": 142000,
      "weapon_stone_gained": 8400,
      "weapon_stone_consumed": 6200,
      "weapon_stone_auction_sold": 1800,
      "armor_stone_total": 94000,
      "armor_stone_gained": 5600,
      "armor_stone_consumed": 4100,
      "armor_stone_auction_sold": 920
    }
  ]
}
```

강화석은 DB 가상 재화다. `gained`는 몹/보스 처치 시 적립, `consumed`는 강화 시도 시 소모, `auction_sold`는 경매소를 통해 타 플레이어에게 이전된 수량.

`total`은 해당 일 자정 기준 서버 전체 플레이어 보유량 합계 스냅샷이다.

---

## 5. 아이템 발행 (`/api/items/issued`)

### `GET /api/items/issued/summary`

기간 내 등급별 잠재 확정 아이템 누적 발행 수.

**파라미터**

| 파라미터 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `from` | string | 필수 | 시작일 |
| `to` | string | 필수 | 종료일 |

**응답**

```json
{
  "period": { "from": "2026-05-01", "to": "2026-05-23" },
  "potential_confirmed": {
    "rare": 1240,
    "epic": 380,
    "unique": 95,
    "legendary": 8
  },
  "transfer_tickets_used": {
    "basic": 34,
    "grade": 12,
    "substat": 7
  },
  "enhancement_trace_used": {
    "star": 4200,
    "moon": 1800,
    "sun": 620
  }
}
```

### `GET /api/items/issued/daily`

일별 발행 수 시계열. 응답 구조는 `summary`와 동일하나 `days` 배열로 감싼다.

---

## 6. 보스 기록 (`/api/boss`)

### `GET /api/boss/clears`

보스 클리어 목록.

**파라미터**

| 파라미터 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `boss_id` | string | 선택 | 특정 보스 필터. 없으면 전체 |
| `from` | string | 선택 | 시작일 |
| `to` | string | 선택 | 종료일 |
| `limit` | int | 선택 | 기본 50 |
| `offset` | int | 선택 | 기본 0 |

**응답**

```json
{
  "total": 312,
  "clears": [
    {
      "clear_id": "...",
      "timestamp": 1748000000,
      "boss_id": "season_boss_1",
      "boss_name": "균열의 수문장",
      "result": "clear",
      "clear_time_seconds": 423,
      "party_size": 4,
      "party_weapons": ["sword", "axe", "crossbow", "staff"],
      "party_total_dps": 84200,
      "members": [
        {
          "uuid": "...",
          "name": "플레이어명",
          "weapon": "sword",
          "total_damage": 12800000,
          "dps": 30260,
          "max_single_hit": 180000
        }
      ]
    }
  ]
}
```

`result`: `"clear"` / `"timeout"` / `"wipe"`

### `GET /api/boss/summary`

보스별 클리어/실패/타임아웃 요약 통계.

**파라미터**

| 파라미터 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `from` | string | 선택 | 시작일 |
| `to` | string | 선택 | 종료일 |

**응답**

```json
{
  "bosses": [
    {
      "boss_id": "season_boss_1",
      "boss_name": "균열의 수문장",
      "total_attempts": 45,
      "clears": 38,
      "timeouts": 5,
      "wipes": 2,
      "avg_clear_time_seconds": 487,
      "min_clear_time_seconds": 312,
      "avg_party_dps": 76400
    }
  ]
}
```

---

## 7. DPS 통계 (`/api/dps`)

### `GET /api/dps/by-weapon`

무기 클래스별 평균 DPS 통계.

**파라미터**

| 파라미터 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `boss_id` | string | 선택 | 특정 보스 대상. 없으면 전체 보스 합산 |
| `min_enhancement` | int | 선택 | 최소 평균 IL 필터. 없으면 전체 |
| `from` | string | 선택 | 시작일 |
| `to` | string | 선택 | 종료일 |

**응답**

```json
{
  "weapons": [
    {
      "weapon": "sword",
      "sample_count": 128,
      "avg_dps": 28400,
      "stddev_dps": 4200,
      "median_dps": 27900
    }
  ]
}
```

`sample_count`가 10 미만인 항목은 `"low_sample": true` 플래그를 추가한다.

### `GET /api/dps/by-potential`

잠재 등급별 평균 DPS 통계.

**파라미터**: `by-weapon`과 동일.

**응답**

```json
{
  "potential_grades": [
    {
      "grade": "unique",
      "weapon": "sword",
      "sample_count": 34,
      "avg_dps": 31200,
      "stddev_dps": 3800
    }
  ]
}
```

---

## 8. 플레이어 조회 (`/api/players`)

### `GET /api/players`

플레이어 목록. 파라미터와 응답은 `/api/economy/gold/players`와 동일하며, 추가로 장비 정보 포함.

**응답 추가 필드**

```json
{
  "players": [
    {
      "uuid": "...",
      "name": "플레이어명",
      "weapon_class": "sword",
      "avg_il": 85,
      "highest_potential_grade": "unique",
      "gold_balance": 450000,
      "gold_created_total": 2100000,
      "gold_consumed_total": 1650000
    }
  ]
}
```

### `GET /api/players/:uuid`

특정 플레이어 상세.

**응답**

```json
{
  "uuid": "...",
  "name": "플레이어명",
  "weapon_class": "sword",
  "engraving": "A",
  "gold_balance": 450000,
  "gold_created_total": 2100000,
  "gold_consumed_total": 1650000,
  "equipment": {
    "weapon": { "enhancement": 18, "potential_grade": "unique", "il": 95 },
    "helmet": { "enhancement": 15, "potential_grade": "epic", "il": 80 },
    "chest": { "enhancement": 16, "potential_grade": "unique", "il": 85 },
    "leggings": { "enhancement": 15, "potential_grade": "rare", "il": 80 },
    "boots": { "enhancement": 14, "potential_grade": "epic", "il": 75 }
  },
  "avg_il": 83,
  "recent_boss_clears": [],
  "recent_gold_events": []
}
```

`recent_gold_events`는 1차 버전에서 빈 배열로 반환해도 된다.

---

## 9. 서버 퍼포먼스 (`/api/server/perf`)

### `GET /api/server/perf/current`

현재 TPS·평균 핑·접속자 수 스냅샷.

**응답**

```json
{
  "timestamp": 1748000000,
  "tps": 19.8,
  "avg_ping_ms": 42,
  "online_count": 14
}
```

### `GET /api/server/perf/hourly`

시간대별 평균 동접·TPS·핑 집계. 히트맵과 추이 그래프 원본.

**파라미터**

| 파라미터 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `from` | string | 필수 | 시작일 (`2026-05-01`) |
| `to` | string | 필수 | 종료일 (`2026-05-23`) |

**응답**

```json
{
  "rows": [
    {
      "date": "2026-05-01",
      "hour": 20,
      "avg_online": 18.4,
      "peak_online": 24,
      "avg_tps": 19.6,
      "min_tps": 17.2,
      "avg_ping_ms": 48
    }
  ]
}
```

`hour`: 0~23 (서버 로컬 시간 기준).

각 시간대 행은 해당 시간 내 1분 단위 `server_perf_log` 행의 AVG/MIN 집계값이다.

### `GET /api/server/perf/daily`

일별 평균·피크 동접, 평균 TPS, 평균 핑 집계.

**파라미터**: `hourly`와 동일.

**응답**

```json
{
  "days": [
    {
      "date": "2026-05-01",
      "avg_online": 11.2,
      "peak_online": 24,
      "avg_tps": 19.5,
      "min_tps": 16.8,
      "avg_ping_ms": 45,
      "low_tps_minutes": 3
    }
  ]
}
```

`low_tps_minutes`: TPS < 18 구간의 누적 분 수. 0이면 당일 TPS 저하 없음.

### `GET /api/server/perf/peak-hours`

요일×시간 히트맵용 평균 동접 집계.

**파라미터**: `from` / `to` 필수.

**응답**

```json
{
  "heatmap": [
    { "weekday": 1, "hour": 20, "avg_online": 21.3 },
    { "weekday": 1, "hour": 21, "avg_online": 19.8 }
  ],
  "peak_top3": [
    { "weekday": 5, "hour": 21, "avg_online": 23.1 },
    { "weekday": 6, "hour": 20, "avg_online": 22.4 },
    { "weekday": 6, "hour": 21, "avg_online": 21.9 }
  ]
}
```

`weekday`: 1(월)~7(일). `peak_top3`는 평균 동접 상위 3 슬롯.

---

## 10. 필드 활동 (`/api/field`)

### `GET /api/field/summary`

기간 내 필드별 자원 생산·몬스터 처치 합계.

**파라미터**

| 파라미터 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `from` | string | 필수 | 시작일 |
| `to` | string | 필수 | 종료일 |

**응답**

```json
{
  "period": { "from": "2026-05-01", "to": "2026-05-23" },
  "fields": [
    {
      "field_id": "field_grassland",
      "field_name": "초원 사냥터",
      "mob_kill_count": 84200,
      "gold_earned": 12600000,
      "stone_earned": 338000,
      "cube_piece_earned": 8400,
      "unique_players": 28
    }
  ]
}
```

### `GET /api/field/daily`

일별 필드 자원 생산 시계열.

**파라미터**

| 파라미터 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `field_id` | string | 선택 | 특정 필드 필터. 없으면 전체 |
| `from` | string | 필수 | 시작일 |
| `to` | string | 필수 | 종료일 |

**응답**

```json
{
  "days": [
    {
      "date": "2026-05-01",
      "field_id": "field_grassland",
      "mob_kill_count": 3800,
      "gold_earned": 570000,
      "stone_earned": 15200,
      "cube_piece_earned": 380
    }
  ]
}
```

### `GET /api/field/density`

필드별 밀도 이상 감지 지표. 대시보드 필드 탭의 🔴🟢🟡 표 원본.

**파라미터**: `from` / `to` 필수.

**응답**

```json
{
  "fields": [
    {
      "field_id": "field_grassland",
      "field_name": "초원 사냥터",
      "kills_per_player_hour": 42.3,
      "gold_per_kill": 150,
      "avoid_index": 0.12,
      "status": "normal",
      "flags": []
    },
    {
      "field_id": "field_mine",
      "field_name": "광산 사냥터",
      "kills_per_player_hour": 8.1,
      "gold_per_kill": 310,
      "avoid_index": 0.67,
      "status": "warning",
      "flags": ["low_traffic", "high_avoid"]
    }
  ]
}
```

`status`: `"normal"` / `"warning"` / `"danger"`

`avoid_index`: 0.0~1.0. 해당 필드를 기피하는 비율 지수. 0.5 이상이면 경보.

`flags` 가능한 값: `"low_traffic"` / `"high_traffic"` / `"high_avoid"` / `"gold_spike"` / `"stone_spike"`

---

## 11. 미구현 / 2차 예정 엔드포인트

| 엔드포인트 | 설명 | 이유 |
|---|---|---|
| `GET /api/players/:uuid/gold-events` | 개별 골드 이벤트 상세 | 이벤트 로그 적재 확인 후 |
| `GET /api/economy/auction/daily` | 경매소 일별 거래량 | 경매소 거래 로그 적재 필요 |
| `GET /api/dps/by-engraving` | 직업각인별 DPS | 샘플 수 확보 후 |
| `GET /api/economy/gold/distribution` | 골드 보유 분포 히스토그램 | 2차 |
