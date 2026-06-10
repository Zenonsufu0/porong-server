# porong-economy 데이터 스키마 (구상)

> **[STATUS: 구상 — 엔티티 맵 단계]** 경제 시스템의 영속 데이터 모델. 개요 [`concept.md`](concept.md) · 시스템 상세 [`design.md`](design.md)(§N) · 결정 [`decision_log.md`](decision_log.md)(`DL-E###`).
> 필드는 1차 후보 — 구체 타입·인덱스·제약은 구현 단계. 밸런싱 수치는 P1.

## 아키텍처 (확정 · DL-E035)

```
CORE 모드(NeoForge) ──write──> 영속 SQL DB (Postgres/MySQL 후보)
                                   │
                    ┌──────────────┼──────────────┐
                read│           read│           read│
              인게임 커스텀 GUI   웹 대시보드     디스코드 봇
              (실시간 push)      (심화 분석)     (조회·알림)
```

- **영속 SQL 서버 DB** — 재무·지분·거래가 구조적 관계 데이터라 관계형. 멀티 소비자(웹/디스코드)라 임베디드(SQLite)보다 서버 DB(Postgres/MySQL).
- **CORE 모드가 단일 writer**, 인게임 GUI(DL-E032)·웹 대시보드·디스코드 봇이 reader.
- **시계열 데이터**(주가 캔들·지표 추이)는 스냅샷 테이블로 분리 — 차트·웹 분석용.
- 실시간 인게임 GUI는 DB 폴링이 아니라 CORE가 메모리 상태를 패킷 push, DB는 영속·조회·웹용(쓰기 주기는 구현 시).

## 엔티티 맵 (핵심)

| 엔티티 | 핵심 필드(후보) | 관계 | 근거 |
|---|---|---|---|
| **player** | 개인 지갑(cash)·총자산 | → shareholding, vote | DL-E029 |
| **company** | 자본·현금·상장여부·발행주식수·주가·산재이력 | → industry, financials, shareholding, employment, market_listing | DL-E029·E033 |
| **industry**(업종 마스터) | 오염계수·산재위험도·부가가치·노동집약도·경기민감도 | ← company | DL-E010 |
| **financials**(재무제표) | 손익(매출·비용·이익)·재무상태(자산·부채·자본)·현금흐름, 기간 | ← company | DL-E029 |
| **population_stratum**(주민 계층) | 인구·평균학력·임금·소비성향(MPC)·욕구충족·저축·출산기여·고용/실업 | → employment, shareholding(주민풀) | DL-E003 |
| **employment**(고용) | employer_type(company/public)·employer_id·인원·임금 | (company OR 공공) ↔ stratum | DL-E017·E046 |
| **item**(품목) | 필수/사치 tier·기본 속성 | → market_state, market_listing | DL-E013 |
| **market_listing**(출하) | 물량·호가·품질 | company → item | DL-E026·E027 |
| **market_state**(시세) | 공급·수요·시세·품질분포·독점도(HHI) | ← item | DL-E014 |
| **shareholding**(지분) | 보유 주식수·holder종류(player/주민풀) | company ↔ player/주민풀 | DL-E033·E034 |
| **stock_price_history** | 시각·OHLC(캔들) | ← company (시계열) | DL-E033 |
| **contract**(계약) | 공급레이트·개당대금·기간·위약금·상태 | company ↔ company | DL-E028 |
| **techtree_node**(기술) | 선행노드·시대·해금상태(서버 공통)·연구진척 | 서버 공통 | DL-E020·E031 |
| **treasury**(재정/중앙은행) | 국고잔액·부채·이자·통화량·세율(정책 상태값들) | 단일 | DL-E012·E023 |
| **agenda**(안건) | 카테고리·트리거지표·효과·상태·발의시각 | → vote | DL-E025, agenda_catalog |
| **vote**(투표) | 선택(찬/반)·시각 | agenda ↔ player | DL-E025 |
| **indicator_snapshot**(지표 시계열) | 시각 + 오염·범죄율·CPI·인플레·실업률·GDP·지니·행복도… | 대시보드/웹 차트 | DL-E009~E017 |

## 다음
- 엔티티별로 필드·타입·관계를 상세화한다(아래에 §로 추가).
- IPO 상장 재무 요건·자본 건전성 임계는 financials 상세 시 정의(DL-E034 이월).

## 엔티티 상세

### company (회사) + 위성

**`company`**
| 필드 | 타입(후보) | 설명 |
|---|---|---|
| id | PK | |
| name | text | |
| industry_id | FK→industry | 업종(§1-A) |
| founder_player_id | FK→player | 창업자(기록) |
| controller_player_id | FK→player (캐시) | 현재 대표 = 최대 유저 지분(DL-E034), 지분 변동 시 재계산 |
| stable_control | bool | 51%+ 단독 지배(적대적 인수 면역) |
| cash | decimal | **회사 현금 — 유저 지갑과 분리(유한책임)** |
| land_plot_id | FK→land | 마을 토지 구획(§9) |
| automation_tier | int | 자동화 수준(기술 게이팅, §2·DL-E031) |
| safety_level | enum | 노동안전(감독관·안전규칙 → 산재, DL-E004) |
| accident_count | int | 산재 이력 |
| listed | bool | 상장 여부(DL-E033) |
| shares_outstanding | bigint | 총 발행 주식수 |
| share_price | decimal (캐시) | 현재가(stock_price_history 최신 파생) |
| dividend_policy | decimal | 배당 vs 재투자 비율(DL-E033) |
| status | enum | 운영/도산/상장폐지 |
| founded_at | timestamp | |

**`shareholding` (지분)** — 대표·소유 계산 핵심
| company_id FK · holder_type(player/주민풀) · holder_id · shares |
- controller = 유저 holder 중 max(shares). 합산으로 51% 판정(stable_control).

**`financials` (재무제표)** — 기간별 시계열
| company_id FK · period · 손익(매출·비용[인건비·원료·전기·세금·위약금·산재]·이익) · 재무상태(자산[현금·토지·설비·재고]·부채·자본) · 현금흐름(영업·투자·재무) |
- 상장요건·안건 트리거(도산율·수익성)·법인세 세수의 입력값.

**`inventory` (재고)**
| company_id FK · item_id · qty · avg_quality |
- 원료 재고 + 미판매 산출물. `market_listing`(출하)은 이 중 박스에 올린 판매분.

**모델링 포인트:** ① `cash`는 회사≠유저 지갑(유한책임). ② `controller`는 derived지만 권한 체크용 캐시(지분 변동 시 재계산). ③ `financials`는 기간별 시계열(히스토리+상장 평가+트리거+세수).

### population_stratum (주민 계층 — 상태)

> **상태(state)** 만 여기. 행동/동학(소비·노동·출산·투자 함수, 창발 지표)은 [`population_model.md`](population_model.md)로 분리.

**`population_stratum`** — 학력 tier 기준
| 필드 | 타입(후보) | 설명 |
|---|---|---|
| id | PK | |
| education_level | enum | 무학/기초/중등/고학력 (계층 키) |
| population | int | 인구수 |
| savings | decimal | 누적 저축 → 주식 투자 풀(DL-E033) |
| avg_wage | decimal (캐시) | 평균 임금(employment 집계) |
| employed_count / unemployed_count | int (캐시) | 고용/실업 |
| needs_essential / needs_luxury | float (캐시) | 욕구충족도(필수/사치, DL-E013) |

- **state(영속)** = population·education·savings / **derived(캐시)** = wage·고용·욕구충족 (동학 엔진이 매틱 계산 → indicator_snapshot 기록).
- **학력 tier 키 이유:** 학력이 가능 직업·소득·연구 가능 여부를 가르는 1차 변수 → 노동·소비·연구가 한 키로 엮임. 소득은 고용에서 파생이라 키 아님.
- **소득 불평등(지니)** 은 계층 간+내 소득 분포에서 계산(저장 X). 고용 실제값은 `employment`(company↔stratum), 실업 = population − 고용.

### player / industry

**`player`** | id · name · cash(개인 지갑) · (지분=shareholding·예금=deposit·채권=bond 보유) — DL-E029
**`industry`** (업종 마스터) | id · name · 오염계수 · 산재위험도 · 부가가치 · 노동집약도 · 경기민감도 · **요구 학력 tier**(DL-E047) · 자동화 가능 tier — DL-E010

### 시장 (item / market_listing / market_state / stock_price_history)

**`item`** | id · name · category(필수/일반 — 사치=고품질 파생) · base_industry — DL-E013
**`market_listing`** (출하·호가) | company_id · item_id · qty · ask_price · quality — DL-E026·E027
**`market_state`** (시세) | item_id · 총공급 · 총수요 · 시세 · 품질분포 · HHI독점도 — DL-E014
**`stock_price_history`** (캔들) | company_id · time · open·high·low·close · volume — DL-E033

### 금융 (deposit / loan / bond) — 중앙은행 monobank (DL-E039)

**`deposit`** | holder_type(stratum/player) · holder_id · balance · 적용금리 — 주민·유저 예금→중앙은행
**`loan`** | borrower_type(company/player) · borrower_id · principal · 금리 · 잔액 — 중앙은행 대출
**`bond`** | issuer_type(treasury/company) · issuer_id · holder_type(stratum/player) · holder_id · 액면 · coupon · 만기 — 국채/회사채(DL-E038)
*(주식 지분 = `shareholding`, company 섹션)*

### 거래 (contract) — DL-E028

**`contract`** | id · supplier_company_id · buyer_company_id · item_id · supply_rate(분/시간당) · unit_price · period · penalty · status

### 정부·통화 (treasury) — 단일 상태 행 (DL-E012·E023·E039)

**`treasury`** | 국고잔액 · 부채 · 부채이자율 · 통화량 · 기준금리 · 신용(대출)기준 · 세율{법인·소득(+누진성)·거래·재산} · 공공지출 배분{인프라·교육·복지·치안·환경·주민회사}

### 기술 (techtree_node) — 서버 공통 단일 트리 (DL-E020·E031)

**`techtree_node`** | id · name · era(수공업/증기/전기/자동화) · prereq_node_ids · unlocked(bool) · research_progress

### 정치 (agenda / vote / party)

**`agenda`** | id · category · trigger_indicator · threshold · effect(JSON) · prereq_tech · status · proposed_at — DL-E025, agenda_catalog
**`vote`** | agenda_id · player_id · choice(찬/반) · time — DL-E025
**`party`** (무권한 소셜) | id · name · 가치축(성장자유/분배환경) · member player_ids — DL-E007

### 토지 (land_plot) — §9·DL-E022

**`land_plot`** | id · location · owner_type(company/마을) · owner_id · price · zoning(공업/상업/주거) · buildable(그린벨트 제외)

### 지표 시계열 (indicator_snapshot) — DL-E049

**`indicator_snapshot`** | time · 오염 · 범죄율 · CPI · 인플레 · 실업률 · GDP · 성장률 · 지니 · 행복도 · 빈곤율 · 경기국면 · 주가지수 · 부채-세수비율 · … → 대시보드·웹 차트 + 안건 트리거

> **엔티티 1차 완료.** 구체 타입·인덱스·제약·관계 FK는 구현 단계. IPO 상장 재무 요건·계수는 P1.


