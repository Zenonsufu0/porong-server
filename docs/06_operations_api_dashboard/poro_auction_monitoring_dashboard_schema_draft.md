# 경매 시세 모니터링 대시보드 스키마 v1 (시즌1 45일차 튜닝용)

> **상태**: 2026-04-19 data-schema 1차 초안. 선행: `docs/03_life_system_core/poro_estate_lv_probability_buff_economy_review_v1.md` 후속 작업 지정. 2026-04-19 결정 두 건 반영 — (1) 영지 Lv 연동 확률 버프 **축소 도입 + 보너스 귀속 O**, (2) 치장 아이템 **귀속 X + 랜덤박스·확정 구매 병행**.

---

## 개요

포로 서버 시즌1 경매 데이터를 수집·집계해, 시즌1 45일차(시즌 중반)에 "확률 버프 축소안 유지 vs 재조정" 판단 근거를 제공하는 데이터 구조를 설계한다. 동시에 디스코드 봇 `/시세` 슬래시 커맨드와 운영자 웹 대시보드를 같은 저장소에서 지탱한다.

경매 시세는 세 축으로 분기된다.

- **장비 축**: T1/T2 완성 장비 + 강화석·T2 재료(실물 유지 경계선 안쪽).
- **치장 축**: 귀속 X, 확정 구매 상품군과 랜덤박스 유입 상품군 동시 유입.
- **확률 버프 간접 영향 축**: 버프 보너스 수량 자체는 귀속 O라 경매에 안 올라오지만, **가공·연금·요리 산출물 시세**에 공급 측 압력으로 작용.

## 목표

1. 원천 경매 이벤트(등록·입찰·낙찰·유찰)를 무손실로 저장한다.
2. 일간·카테고리·치장 축을 분리한 집계 테이블로 대시보드 쿼리 지연을 1초 이하로 억제한다.
3. 45일차 판정용 핵심 지표 3~5개를 단일 SQL로 꺼낼 수 있다.
4. 디스코드 봇 `/시세 <품목>` 응답을 캐시로 200ms 이내 회신한다.
5. SQLite v0.1 기반에서 시작해 MySQL 승격 시점을 수치로 판단할 수 있게 한다.

## 핵심 규칙

- 원천 이벤트는 append-only. 정정이 필요하면 역전 이벤트(`reason='reversal'`)로 처리하고 원본 행은 보존한다.
- 집계 테이블은 재계산 가능(원천에서 재빌드 가능)을 전제로 한다. TRUNCATE → INSERT가 허용되는 구조.
- 치장 축은 장비 축과 **별도 집계 테이블**로 분리한다(확정 구매·랜덤박스 유입 구분이 치장 축에만 존재).
- 확률 버프 간접 영향 축은 별도 테이블을 **만들지 않고**, `auction_category_summary` 내 `category_path`가 `crafted/*` 또는 `alchemy/*`, `cooking/*` 하위인 행을 필터로 조회한다. 공급 측 지표는 외부 소스(`wallet_log`·`crafting_log`)와 시세를 조인해 얻는다.
- 비정상 시세 감지(24h ±50%)는 집계 테이블 위에서 파생한다. 별도 알람 테이블은 1차 범위 밖.

## 범위

- 원천 3테이블: `auction_listing`, `auction_bid_log`, `auction_transaction`.
- 집계 3테이블: `auction_daily_summary`, `auction_category_summary`, `auction_cosmetic_summary`.
- 지표 카드 7종, 45일차 튜닝 지표 4종.
- 쿼리 패턴 4종(디스코드 3 + 웹 1).

## 비범위

- 실시간 이벤트 스트림(Kafka·Redis Stream 등)은 1차 도입 안 함. 5분 배치 루프로 충분.
- 경매 부정행위 탐지(자전 거래·담합) 구체 로직은 별도 문서.
- 시즌2 이후 프레스티지 구간 지표는 본 문서 대상 아님.
- 스킨 인벤토리 동기화(치장 소유권)는 본 스키마 밖 — 본 스키마는 **거래 시점의 가격 데이터**만 책임진다.

---

## 1. 원천 테이블 (Fact)

### 1-1. `auction_listing` — 등록 내역

경매장에 올라온 모든 매물. 낙찰·유찰·진행 중을 `state`로 구분한다.

```sql
CREATE TABLE auction_listing (
    listing_id          BIGINT        PRIMARY KEY,         -- 경매 고유 ID
    seller_uuid         CHAR(36)      NOT NULL,
    item_code           VARCHAR(64)   NOT NULL,            -- 품목 고유 코드 (세트/부위/등급 조합 키)
    item_name           VARCHAR(128)  NOT NULL,            -- 사람이 읽는 이름(스냅샷)
    category_path       VARCHAR(128)  NOT NULL,            -- 예: equip/t2/weapon, cosmetic/skin/weapon, crafted/alchemy
    axis                VARCHAR(16)   NOT NULL,            -- equip | cosmetic | material
    rarity              VARCHAR(16)   NOT NULL,            -- common | rare | epic | legend
    cosmetic_source     VARCHAR(16)   NULL,                -- direct_buy | lootbox | null(치장 아닌 경우)
    quantity            INT           NOT NULL DEFAULT 1,
    start_price         BIGINT        NOT NULL,            -- 시작가(골드)
    buyout_price        BIGINT        NULL,                -- 즉시구매가(없으면 NULL)
    current_price       BIGINT        NOT NULL,            -- 마지막 입찰가 또는 시작가
    listed_at           DATETIME      NOT NULL,
    expires_at          DATETIME      NOT NULL,
    closed_at           DATETIME      NULL,                -- 낙찰·유찰 확정 시각
    state               VARCHAR(16)   NOT NULL,            -- active | sold | expired | canceled
    season_id           INT           NOT NULL,            -- 시즌1 = 1
    INDEX idx_active_state   (state, expires_at),
    INDEX idx_item_listed    (item_code, listed_at),
    INDEX idx_axis_category  (axis, category_path, listed_at),
    INDEX idx_season_closed  (season_id, closed_at)
);
```

- `axis` + `category_path` 이중 키: 치장 축이 여러 카테고리(무기/갑옷/장신구)를 포함하기 때문에, 축 단위 필터와 세부 카테고리 필터를 동시에 지원해야 한다.
- `cosmetic_source`: 확정 구매 vs 랜덤박스 유입 구분 전용. 장비·재료 행에서는 NULL.
- `state` 전이는 `active → sold | expired | canceled` 한 방향. 재업로드는 새 `listing_id`로 기록.

### 1-2. `auction_bid_log` — 입찰 이력

```sql
CREATE TABLE auction_bid_log (
    bid_id         BIGINT       PRIMARY KEY,
    listing_id     BIGINT       NOT NULL,
    bidder_uuid    CHAR(36)     NOT NULL,
    bid_price      BIGINT       NOT NULL,
    bid_at         DATETIME     NOT NULL,
    is_buyout      TINYINT(1)   NOT NULL DEFAULT 0,
    INDEX idx_listing (listing_id, bid_at),
    INDEX idx_bidder  (bidder_uuid, bid_at),
    FOREIGN KEY (listing_id) REFERENCES auction_listing(listing_id)
);
```

- `is_buyout=1`은 즉시구매 결제. 이후 `auction_transaction`에 동시 생성.
- FK는 SQLite에서는 `PRAGMA foreign_keys=ON` 하에 유지, MySQL 승격 시 `InnoDB` 전제.

### 1-3. `auction_transaction` — 낙찰 후 실제 정산

```sql
CREATE TABLE auction_transaction (
    tx_id          BIGINT       PRIMARY KEY,
    listing_id     BIGINT       NOT NULL UNIQUE,           -- 1:1 (유찰 listing은 행 없음)
    seller_uuid    CHAR(36)     NOT NULL,
    buyer_uuid     CHAR(36)     NOT NULL,
    final_price    BIGINT       NOT NULL,                  -- 최종 낙찰가
    fee_amount     BIGINT       NOT NULL,                  -- 수수료 절대값
    net_to_seller  BIGINT       NOT NULL,                  -- 판매자 수령액 (= final_price - fee_amount)
    settled_at     DATETIME     NOT NULL,
    season_id      INT          NOT NULL,
    INDEX idx_settled (settled_at),
    INDEX idx_season_fee (season_id, settled_at),
    FOREIGN KEY (listing_id) REFERENCES auction_listing(listing_id)
);
```

- 수수료·분배 지표는 이 테이블 하나로 바로 낼 수 있게 `fee_amount` 별도 컬럼 유지.
- `net_to_seller`는 계산 컬럼이지만, 읽기 빈도 대비 저장 비용이 미미해 물리 컬럼으로 보관.

---

## 2. 집계 테이블 (시세 지표용)

### 2-1. `auction_daily_summary` — 품목별 일간 집계

```sql
CREATE TABLE auction_daily_summary (
    summary_date     DATE         NOT NULL,
    item_code        VARCHAR(64)  NOT NULL,
    axis             VARCHAR(16)  NOT NULL,
    category_path    VARCHAR(128) NOT NULL,
    listed_count     INT          NOT NULL,
    sold_count       INT          NOT NULL,
    expired_count    INT          NOT NULL,
    avg_price        BIGINT       NULL,
    min_price        BIGINT       NULL,
    max_price        BIGINT       NULL,
    median_price     BIGINT       NULL,
    total_volume     BIGINT       NOT NULL,   -- 낙찰 건의 final_price 합산
    fail_rate        DECIMAL(5,4) NOT NULL,   -- expired / listed
    season_id        INT          NOT NULL,
    PRIMARY KEY (summary_date, item_code),
    INDEX idx_axis_date (axis, summary_date),
    INDEX idx_category_date (category_path, summary_date)
);
```

### 2-2. `auction_category_summary` — 카테고리별 집계

장비/치장/재료/소비품 축을 한 테이블에서 분리 조회 가능하게.

```sql
CREATE TABLE auction_category_summary (
    summary_date     DATE         NOT NULL,
    axis             VARCHAR(16)  NOT NULL,
    category_path    VARCHAR(128) NOT NULL,
    listed_count     INT          NOT NULL,
    sold_count       INT          NOT NULL,
    total_volume     BIGINT       NOT NULL,
    avg_price        BIGINT       NULL,
    fail_rate        DECIMAL(5,4) NOT NULL,
    unique_items     INT          NOT NULL,   -- 해당 카테고리 내 distinct item_code 수
    season_id        INT          NOT NULL,
    PRIMARY KEY (summary_date, axis, category_path)
);
```

### 2-3. `auction_cosmetic_summary` — 치장 전용 축

확정 구매 vs 랜덤박스 유입을 구분. 치장 귀속 X 결정에 따라 경매 유통량이 다른 축보다 빠르게 쌓일 수 있어 전용 테이블로 분리한다.

```sql
CREATE TABLE auction_cosmetic_summary (
    summary_date      DATE         NOT NULL,
    category_path     VARCHAR(128) NOT NULL,   -- cosmetic/skin/weapon 등
    rarity            VARCHAR(16)  NOT NULL,
    cosmetic_source   VARCHAR(16)  NOT NULL,   -- direct_buy | lootbox
    listed_count      INT          NOT NULL,
    sold_count        INT          NOT NULL,
    avg_price         BIGINT       NULL,
    min_price         BIGINT       NULL,
    max_price         BIGINT       NULL,
    total_volume      BIGINT       NOT NULL,
    source_ratio      DECIMAL(5,4) NOT NULL,   -- 같은 category+rarity 내 해당 source 비율
    season_id         INT          NOT NULL,
    PRIMARY KEY (summary_date, category_path, rarity, cosmetic_source)
);
```

### 집계 주기 선택 근거

- **일간 집계 (`*_daily_summary`, 두 카테고리 테이블)**: 운영 판단·시즌 리포트 기본 단위. 자정 1회 배치로 충분. 1초 이하 응답 필요.
- **1시간 집계**: 1차에서는 불채택. 품목별 시세가 짧은 주기로 크게 출렁이는 신호는 **비정상 감지 쿼리(원천 2-3 테이블 실시간 조회)로 대체**해, 집계 테이블 수를 늘리지 않는다.
- **실시간**: 디스코드 `/시세`는 "최근 24h 평균 + 최저/최고"를 원천 테이블에서 직접 뽑고 5분 캐시. 집계 테이블을 통하지 않는다(일간 집계 직전 시점에 오답 위험).

즉 기본 집계 주기는 **일 1회 + 5분 캐시 레이어**. 1시간 버킷은 승격 시점(MySQL 전환) 이후로 미룬다.

---

## 3. 지표 (대시보드 카드)

### 3-1. 시즌 누적 골드 유통량
`SUM(final_price)` from `auction_transaction` where `season_id = 1`. 낙찰 기준. 7일 이동 평균과 함께 표시.

### 3-2. T1/T2 강화석·재료 시세 추이 (일간)
`auction_daily_summary` 에서 `category_path LIKE 'material/enhance/%'` 필터. T1/T2 2개 라인 동시 그래프. 45일차 판정용 핵심 카드.

### 3-3. 치장 시세 추이 (등급별, 확정 구매 vs 랜덤박스 유입 비율)
`auction_cosmetic_summary`의 `rarity` × `cosmetic_source` 조합. 누적 라인 + 스택 바 조합 추천. 확정 구매 풀이 랜덤박스 풀보다 낮은 시세를 유지하는 건 정상 신호, 반대 현상이 3일 이상 지속되면 랜덤박스 드롭표 이상 의심.

### 3-4. 확률 버프 예상 영향 지표 (가공·연금·요리 산출물 시세)
`auction_daily_summary` 에서 `category_path IN ('crafted/alchemy', 'crafted/cooking', 'crafted/processing')` 필터. 버프 보너스 수량은 귀속이라 경매에 없지만, **플레이어가 자신이 제작한 수량(보너스 포함)을 경매에 올리는 것은 허용**되므로 공급 측 압력이 간접 반영된다. 22일차 기준선 대비 40% 이상 하락 시 버프 영향 유력 시그널.

### 3-5. 경매 수수료 누적·분배
`SUM(fee_amount)` from `auction_transaction`. 수수료는 골드 싱크로 작용. 시즌 목표 싱크량(운영자 설정값)과 비교 배너.

### 3-6. 비정상 시세 감지 (24h 내 ±50% 변동 알림)
원천 테이블에서 24h 중앙값 vs 48h~24h 중앙값 비교. 품목별 리스트 카드. 임계값은 ±50% 기본, 치장 축은 랜덤박스 로테이션 영향 커서 ±80%로 완화.

### 3-7. 축별 유찰률 (Fail Rate)
`auction_category_summary.fail_rate`. 장비 축 유찰률이 40%를 지속 초과하면 장비 공급 과잉 or 수요 저하 신호. 치장 축 유찰률이 60%를 지속 초과하면 치장 가격 기대치와 시세 괴리.

> 카드 합계 7종. 시즌 초기 2주차까지는 3-6(비정상 감지)·3-7(유찰률)이 주로 UI 주의선을 당기고, 30일차 이후 3-2·3-4 라인이 튜닝 신호로 전환된다.

---

## 4. 시즌1 45일차 튜닝 기준 지표

"확률 버프 축소안 유지 vs 재조정" 판단용 핵심 지표 4종. 각 판정 기준선을 문서에 고정해 운영자가 재량 해석하지 않도록 한다.

### 4-1. 가공 중간재 시세 변동 (vs 22일차 기준선)
대상: `crafted/processing/*`, `crafted/alchemy/intermediate/*`, `crafted/cooking/intermediate/*`.
- **유지 신호**: 22일차 기준선 대비 -20% 이내.
- **하향 재조정 신호**: -30% 초과.
- **폐기 신호**: -50% 초과 또는 유찰률 60% 초과 동반.

### 4-2. T1 강화석 경매 최저가 추이
대상: `material/enhance/t1/stone`.
- **유지 신호**: 30일차 대비 -15% 이내.
- **하향 신호**: -25% 초과 (Lv5 영지 상위권의 공급 압력 의심).

### 4-3. 치장 시세 확정 구매 vs 랜덤박스 비율
대상: `auction_cosmetic_summary.source_ratio` 중 랜덤박스 축.
- **정상**: 동일 등급 기준 랜덤박스 평균가 ≥ 확정 구매 평균가.
- **경고**: 랜덤박스 평균가 < 확정 구매 평균가 인 상태가 7일 이상 지속. 랜덤박스 드롭표 이상 또는 공급 과잉 의심.

### 4-4. 수수료 누적 vs 골드 유통량 비율
`SUM(fee_amount) / SUM(final_price)`. 시즌 누적 기준.
- **정상**: 설정 수수료율(예: 5%) 부근 ±0.5%p.
- **경고**: -1%p 이하 (저가 위주 거래 편중) 또는 +1%p 이상 (대형 거래 편중, 소수 독점 의심).

> 지표 4-1이 주지표. 4-2는 영지 Lv5 상위권 편중 확인용, 4-3은 치장 축 건강도, 4-4는 전반적 거래 분포 확인용.

---

## 5. 디스코드 봇·웹 대시보드 쿼리 패턴

### 5-1. 디스코드 `/시세 <품목>` — 24h 요약

```sql
-- 파라미터: :item_code, :now (UTC)
SELECT
    COUNT(*)                                  AS sold_count_24h,
    AVG(t.final_price)                        AS avg_price_24h,
    MIN(t.final_price)                        AS min_price_24h,
    MAX(t.final_price)                        AS max_price_24h
FROM auction_transaction t
JOIN auction_listing l ON l.listing_id = t.listing_id
WHERE l.item_code = :item_code
  AND t.settled_at >= DATETIME(:now, '-24 hours');
```
- 캐시 TTL 5분, 키 = `price:{item_code}`. 디스코드 봇 첫 호출 시 채움, 이후 히트.

### 5-2. 디스코드 `/시세 추이 <품목>` — 7일 일별

```sql
SELECT summary_date, avg_price, min_price, sold_count
FROM auction_daily_summary
WHERE item_code = :item_code
  AND summary_date >= DATE(:now, '-7 days')
ORDER BY summary_date;
```
- 캐시 TTL 1시간. 자정 배치 직후 스태일 윈도우만 5분 TTL로 일시 덮어쓰기.

### 5-3. 웹 대시보드 OLAP — 축별 유찰률 & 평균가 (45일차 리포트용)

```sql
SELECT
    axis,
    category_path,
    AVG(avg_price)        AS avg_price_window,
    AVG(fail_rate)        AS fail_rate_window,
    SUM(total_volume)     AS volume_window
FROM auction_category_summary
WHERE summary_date BETWEEN :window_start AND :window_end
  AND season_id = :season
GROUP BY axis, category_path
ORDER BY volume_window DESC;
```
- `:window_start` = 30일차, `:window_end` = 45일차 운영 기본값. 리프레시 주기 = 매일 00:10 배치 종료 후 캐시 무효화.

### 5-4. 웹 대시보드 OLAP — 확률 버프 영향 조인

```sql
SELECT
    d.summary_date,
    d.category_path,
    d.avg_price,
    d.total_volume,
    w.bonus_qty_total            -- wallet_log에서 reason='estate_lv_bonus' 합계
FROM auction_daily_summary d
LEFT JOIN (
    SELECT DATE(created_at) AS d, SUM(delta) AS bonus_qty_total
    FROM wallet_log
    WHERE reason = 'estate_lv_bonus'
    GROUP BY DATE(created_at)
) w ON w.d = d.summary_date
WHERE d.category_path LIKE 'crafted/%'
  AND d.summary_date BETWEEN :window_start AND :window_end
ORDER BY d.summary_date;
```
- 버프 수량은 귀속이지만 `wallet_log`에 `reason='estate_lv_bonus'`로 집계 남는 것을 전제. 상관 분석용.

### 리프레시 주기·캐시 TTL 요약

- 원천 테이블: 실시간 append.
- 집계 테이블: 자정 배치 (+ 테스트 기간은 매 30분 재빌드).
- 디스코드 봇 24h 요약: 5분 TTL.
- 디스코드 7일 추이: 1시간 TTL.
- 웹 OLAP 위젯: 10분 TTL (관리자 세션은 우회 옵션 제공).

---

## 6. DB 선택 (SQLite vs MySQL)

### 현 시점 판단 — **SQLite로 시작, 30일차에 재평가**

- **유리 근거**:
  - v0.1 플래그 저장소가 이미 SQLite. 운영 인프라 단일화.
  - 쓰기 피크가 경매 로그 수준(일 1만~5만 건 예상). SQLite WAL 모드로 충분.
  - 집계는 배치 1회/일이라 읽기 잠금 경쟁 낮음.
  - 로컬 파일 백업이 간단. 초기 튜닝 반복이 빠름.

- **MySQL 승격 트리거 (수치 기준)**:
  - 일 경매 이벤트 발생량 `> 20만 건 / day`.
  - 원천 3테이블 합계 크기 `> 5 GB`.
  - 동시 쓰기 충돌(WAL 대기 100ms 초과)이 하루 1회 이상.
  - 디스코드 봇 캐시 미스 시 응답이 500ms 초과.
- 위 트리거 중 2개 이상 30일차 시점에 관측되면 MySQL(InnoDB, Row-based Replication) 전환을 승인.
- 스키마 자체는 MySQL 호환 형태로 작성(예: `BIGINT`, `DATETIME`, `VARCHAR` 명시)해 승격 비용을 최소화했다. SQLite에서는 `VARCHAR`가 TEXT로 치환되지만 쿼리 호환성에는 영향 없음.

---

## 구현 포인트

- 원천 3테이블 append 파이프라인: 경매 플러그인 훅 4개(`onListingCreate`, `onBidPlaced`, `onListingClose`, `onTxSettled`)에서 각각 1 INSERT.
- 집계 배치 스크립트: 자정 00:05 실행. 트랜잭션 내 `DELETE WHERE summary_date = :today` → `INSERT SELECT ...` 순서. 재실행 안전(idempotent) 보장.
- 비정상 감지(3-6): 배치 종료 후 동일 스크립트가 이어서 실행. 임계 초과 품목을 `ops_alert` 테이블(별도, 본 문서 밖)에 push.
- 디스코드 봇 캐시: Caffeine (플러그인 단) or Redis(이후). 1차는 Caffeine.

## 테스트 포인트

- 원천 테이블 append-only 보장: 정정 테스트(낙찰 취소) 시 역전 `reason='reversal'` 행이 추가되는지, `final_price` 원본이 변경되지 않는지.
- 집계 재실행 일관성: 같은 날짜로 배치를 2번 실행해도 결과가 같은지.
- 치장 축 `cosmetic_source` NULL 누락: 장비·재료 행에 NULL이 아닌 값이 들어가면 집계에서 치장 축으로 잘못 편입되므로 INSERT 시 체크.
- 비정상 감지 오탐률: 경매 오픈 직후 3일은 표본 적어 오탐 잦음 — 경계 완화 설정(표본 < 20건이면 알람 억제) 검증.
- SQLite 동시성 스트레스: 입찰 피크(분당 500건) 시뮬 후 WAL 대기 측정.

## 1차 버전 범위

- 원천 3테이블 생성 + 경매 플러그인 훅 4개 연결.
- 집계 3테이블 + 자정 배치 1종.
- 지표 카드 3-1, 3-2, 3-4, 3-5, 3-7 우선(5종). 3-3·3-6은 2차.
- 45일차 튜닝 지표 4-1·4-2 우선(2종). 4-3·4-4은 2차.
- 디스코드 쿼리 5-1·5-2 우선. 웹 OLAP 5-3·5-4은 2차.

---

## 오픈 질문

1. 치장 랜덤박스가 서버에서 직접 열리는지 클라이언트에서 열리는지에 따라 `cosmetic_source='lootbox'` 결정 시점이 달라진다(개봉 당시 스냅샷을 원천에 기록할 수 있어야 축 분리가 유효함). 개봉 로그 테이블 유무 확인 필요.
2. 장비 축 경매의 **강화 레벨·잠재 상태**를 `item_code`에 인코딩할지, 별도 컬럼으로 뺄지. 별도 컬럼이면 `auction_listing`에 `enhance_lv`, `potential_grade` 2개 추가 필요. 1차는 `item_code` 단일로 두고, 2차에서 분리 제안.
3. 확률 버프 보너스 수량 로깅을 `wallet_log.reason='estate_lv_bonus'` 단일 문자열에 의존할지, 별도 `crafting_bonus_log` 테이블로 분리할지. 분리 시 쿼리 5-4가 더 간결해진다.
4. 시즌 리셋 시 원천 테이블 보존 정책(콜드 스토리지 이전 vs 동일 DB 내 `season_id` 격리). 1차는 `season_id` 격리로 충분, 시즌3~ 데이터 누적 시 재검토.
5. 수수료율 자체의 시즌 중 변경 가능 여부. 변경 시 `auction_transaction.fee_amount`만으로는 역산이 어려워 `fee_rate_snapshot` 컬럼 추가 필요할 수 있음.

## 상위 문서 참조

- `../poro_master_planning.md` — 경매·월렛·강화 재화 섹션
- `../03_life_system_core/poro_estate_lv_probability_buff_economy_review_v1.md` — 확률 버프 축소 도입 근거
- `../03_life_system_core/poro_allinone_workbench_draft.md` — 올인원 제작 산출물 구조
- `../14_world_and_maps/poro_resourcepack_custom_ui_weapon_cosmetic_draft.md` §4 — 치장 아이템 섹션

## 다음 추천 작업

- 경매 플러그인 훅 4종 실제 시그니처 확정 후 INSERT 경로 코드 스켈레톤 작성(implementation-reviewer 위임 후보).
- 22일차 기준선 계산용 스냅샷 쿼리 정의 (45일차 비교 기준선 확정).
- `ops_alert` 테이블(비정상 감지 저장소) 별도 스키마 초안.
- 오픈 질문 2번(강화 레벨 인코딩)에 대한 사용자 결정.
