# 보스 클리어 통계 수집 스펙

> **[STATUS: CANON]** — 웹 운영 대시보드 및 밸런스 판단을 위한 보스 클리어 통계 DB 스키마·API 설계.  
> 확정: DL-052 (2026-05-24)

---

## 1. 목적

시즌 운영 중 HP·DEF 버프/너프 판단을 위한 실측 데이터 수집.  
웹 대시보드에서 보스별 클리어율, 클리어 시간, 파티 스펙, 방무 보유율을 확인한다.

---

## 2. DB 스키마

### 2.1 `boss_session_log` — 개별 도전 세션

```sql
CREATE TABLE boss_session_log (
    id                    INTEGER PRIMARY KEY AUTOINCREMENT,
    boss_id               TEXT    NOT NULL,   -- fallen_knight | corrupted_lord | stone_colossus
                                              -- storm_sorcerer | abyss_guardian | void_herald
                                              -- rift_king | fallen_twin | soul_watcher
    season_week           INTEGER NOT NULL,   -- 시즌 경과 주차 (1~6+)
    started_at            INTEGER NOT NULL,   -- UNIX timestamp
    ended_at              INTEGER,            -- UNIX timestamp (NULL = 진행 중)
    result                TEXT    NOT NULL,   -- 'clear' | 'timeout' | 'pattern_fail' | 'abandoned'
    clear_time_seconds    INTEGER,            -- result='clear'일 때만
    party_size            INTEGER NOT NULL,
    party_avg_enhance     REAL,               -- 파티 평균 강화 수치
    party_avg_il          REAL,               -- 파티 평균 IL
    max_defense_ignore_pct REAL DEFAULT 0.0,  -- 파티 최고 방무% (0.0~1.0)
    has_defense_ignore    INTEGER DEFAULT 0   -- 방무 옵션 보유자 있음 (0/1)
);

CREATE INDEX idx_bsl_boss_week ON boss_session_log (boss_id, season_week);
CREATE INDEX idx_bsl_result    ON boss_session_log (boss_id, result);
```

### 2.2 `boss_session_player` — 세션 참여자 스펙

```sql
CREATE TABLE boss_session_player (
    id                  INTEGER PRIMARY KEY AUTOINCREMENT,
    session_id          INTEGER NOT NULL REFERENCES boss_session_log(id),
    player_uuid         TEXT    NOT NULL,
    weapon_enhance      INTEGER DEFAULT 0,
    avg_enhance         REAL    DEFAULT 0.0,
    il                  REAL    DEFAULT 0.0,
    defense_ignore_pct  REAL    DEFAULT 0.0   -- 무기 방무% (없으면 0)
);

CREATE INDEX idx_bsp_session ON boss_session_player (session_id);
```

> `boss_session_log` 집계 컬럼(`party_avg_enhance`, `max_defense_ignore_pct`)은 세션 종료 시  
> `boss_session_player` 레코드에서 계산해 저장한다 (이후 플레이어 레코드 변경과 독립).

---

## 3. 집계 뷰

### 3.1 전체 보스 요약 뷰

```sql
CREATE VIEW boss_stats_summary AS
SELECT
    boss_id,
    COUNT(*) AS total_attempts,
    SUM(CASE WHEN result = 'clear'   THEN 1 ELSE 0 END) AS total_clears,
    ROUND(100.0 * SUM(CASE WHEN result = 'clear' THEN 1 ELSE 0 END) / COUNT(*), 1)
        AS clear_rate_pct,
    ROUND(AVG(CASE WHEN result = 'clear' THEN clear_time_seconds END), 0)
        AS avg_clear_seconds,
    ROUND(100.0 * SUM(CASE WHEN result = 'timeout' THEN 1 ELSE 0 END) / COUNT(*), 1)
        AS timeout_rate_pct,
    ROUND(100.0 * SUM(has_defense_ignore) / COUNT(*), 1)
        AS defense_ignore_rate_pct,
    ROUND(AVG(party_avg_enhance), 1) AS avg_party_enhance,
    ROUND(AVG(party_avg_il), 1)      AS avg_party_il
FROM boss_session_log
GROUP BY boss_id;
```

### 3.2 주간 클리어율 추이 쿼리

```sql
-- 특정 보스의 주차별 클리어율
SELECT
    season_week,
    COUNT(*) AS attempts,
    SUM(CASE WHEN result = 'clear' THEN 1 ELSE 0 END) AS clears,
    ROUND(100.0 * SUM(CASE WHEN result = 'clear' THEN 1 ELSE 0 END) / COUNT(*), 1) AS clear_rate_pct
FROM boss_session_log
WHERE boss_id = :boss_id
GROUP BY season_week
ORDER BY season_week;
```

---

## 4. API 엔드포인트

기본 경로: `http://localhost:8765/api/v1`

| 메서드 | 경로 | 설명 |
|---|---|---|
| GET | `/boss/stats` | 전 보스 요약 (clear_rate, avg_clear_time, defense_ignore_rate) |
| GET | `/boss/{boss_id}/stats` | 특정 보스 상세 통계 |
| GET | `/boss/{boss_id}/weekly` | 시즌 주차별 클리어율 추이 |
| GET | `/boss/{boss_id}/party-spec` | 클리어 파티 스펙 분포 (강화 분포, 방무 분포) |

### 4.1 응답 예시 — `GET /boss/stats`

```json
[
  {
    "boss_id": "fallen_knight",
    "boss_name": "타락 기사장",
    "total_attempts": 412,
    "clear_rate_pct": 74.3,
    "avg_clear_seconds": 523,
    "timeout_rate_pct": 8.2,
    "defense_ignore_rate_pct": 31.5,
    "avg_party_enhance": 9.2,
    "avg_party_il": 46.0
  },
  {
    "boss_id": "void_herald",
    "boss_name": "공허 사자",
    "total_attempts": 88,
    "clear_rate_pct": 25.0,
    "avg_clear_seconds": 841,
    "timeout_rate_pct": 61.4,
    "defense_ignore_rate_pct": 89.7,
    "avg_party_enhance": 21.1,
    "avg_party_il": 105.5
  }
]
```

### 4.2 응답 예시 — `GET /boss/{boss_id}/weekly`

```json
{
  "boss_id": "storm_sorcerer",
  "boss_name": "폭풍 술사",
  "weekly": [
    { "season_week": 1, "attempts": 31, "clears": 6,  "clear_rate_pct": 19.4 },
    { "season_week": 2, "attempts": 48, "clears": 14, "clear_rate_pct": 29.2 },
    { "season_week": 3, "attempts": 59, "clears": 24, "clear_rate_pct": 40.7 }
  ]
}
```

---

## 5. 수집 시점

| 이벤트 | 기록 내용 |
|---|---|
| 보스 레이드 시작 | `boss_session_log` INSERT (`result='abandoned'` 초기값, `ended_at=NULL`) |
| 파티원 입장 | `boss_session_player` INSERT (파티 전원, 시작 시 일괄 기록) |
| 보스 처치 | result='clear', ended_at·clear_time_seconds UPDATE |
| 제한 시간 초과 | result='timeout', ended_at UPDATE |
| 패턴 실패 (무적 해제 실패) | result='pattern_fail', ended_at UPDATE |
| 전원 사망/이탈 | result='abandoned', ended_at UPDATE |
| 서버 크래시/플러그인 재로드 | result='abandoned', ended_at=NULL 유지 (통계 제외) |

> **구현 결정 (DL-065):** SQLite `NOT NULL` 제약 때문에 진행 중 세션의 초기값으로 `NULL` 대신 `'abandoned'`을 사용한다.
> `boss_stats_summary` 뷰는 `WHERE ended_at IS NOT NULL` 조건으로 진행 중 세션을 통계에서 제외한다.
> 크래시 세션(`result='abandoned'`, `ended_at=NULL`)도 통계 제외 — 허용 가능한 트레이드오프.

---

## 6. 운영 활용 기준

| 지표 | 버프 기준 | 너프 기준 |
|---|---|---|
| 클리어율 | < 설계 목표 −15%p 이상 지속 2주 | > 설계 목표 +20%p 이상 지속 2주 |
| 타임아웃 실패율 | 단순 클리어율과 교차 확인 | — |
| 방무 보유율 | 보스4 < 30% (방무 확보 경로 문제 가능성) | — |
| 주간 클리어율 추이 | 하락세 지속 시 패턴 복잡도 재검토 | 급상승 시 HP·DEF 너프 검토 |

> 설계 목표 클리어율: 보스1 75% / 보스2 65% / 보스3 55% / 보스4 45% / 보스5 35% / 보스6 25%

---

## 7. 관련 문서

- `CANON.md` — DB 저장 구조 (SQLite `empire.db`)
- `../04_combat_weapon_skills/season_boss_stats_v1.md` — 보스 스탯 기준 (방무 수치 설계 근거)
- `../10_development_roadmap/index.md` — 웹 대시보드 구현 일정
