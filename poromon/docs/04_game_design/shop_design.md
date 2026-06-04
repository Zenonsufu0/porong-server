# 상점 설계 (Shop Design)  — 초안(DRAFT)

> 허브 상점의 설계 방향·정책·카테고리. **상세 판매 품목/아이템 ID 표는 `shop_catalog_0.1.md`.**
> 연동: `RewardManager`/`EconomyBridge`/`EncounterTicketManager`/`MegaUnlockManager`/`GymBadgeManager`.
> 화폐: **골드 단일제(결정 014)**. 추가 애드온: SimpleTMs / Eggs(결정 015). 리자몽나이트 X/Y = 메가스톤 카테고리(결정 016).
> ⚠️ `TODO`/`후보`는 검증 대상. **아이템 ID는 추측 금지** — 미확인은 `TODO: item id 확인 필요`.

---

## 1. 설계 방향
- 자유 플레이 보조 + 서버 수명에 영향을 주는 핵심 아이템은 단계적으로 통제.
- 제작 가능한 아이템은 편의 판매(제작보다 비싸게). 못 만드는/통제 필요 아이템이 핵심 상품.
- 전설/메가/실전 육성 아이템은 진행도(배지·관장 클리어)와 연결.
- 목표: 자유롭게 놀다가 자연스럽게 허브로 돌아와 성장 목표를 확인하게.

## 2. 골드 단일 재화 정책 (0.1)
- **화폐는 골드 하나.** 배틀포인트·메가 결정·전설 조각은 **0.1 필수 아님 → 추후 확장 후보로만** 남김.
- 고급 상품 통제 수단:
  - **비싼 골드 가격**
  - **배지 조건 / 관장 클리어 조건**
  - **이벤트·후반 콘텐츠 제한**(일부 상품)
- **주간 구매 제한은 기본적으로 사용하지 않는다.** (이상한 사탕 등도 주간 제한 대신 *가격*으로 조절)
- **강한 제한은 전설/레쿠쟈/메가 레쿠쟈/시즌 전용 상품에만** — 일반 판매하지 않음(이벤트/후반).
- 전설 조우권은 **PoroMonCore 커스텀 아이템**(기본 모드 아이템 아님).

## 3. 상점 카테고리 (7구역)
요약만 — 상세 품목·ID·해금은 `shop_catalog_0.1.md`.

### 3.1 일반 상점 (기본 편의)
- 역할: 초보·일반 플레이 편의, 기본 소모품.
- 판매: 볼 계열(Poké/Great/Ultra/Premier/Heal/Net/Dive/Dusk/Quick/Timer/Repeat/Luxury), 회복(Potion~Max Potion/Revive/Full Heal/상태이상 치료제 류).
- 정책: 초반부터 골드 구매, 무제한. 제작 가능품은 제작보다 비싸게. 과도한 제한 X.

### 3.2 성장 상점 (육성 편의)
- 역할: 레벨업·진화·기본 육성.
- 판매: 학습장치(Exp. Share)·이상한 사탕(Rare Candy)·행운의알(후보), 진화의 돌(Fire/Water/Thunder/Leaf/Ice/Moon/Sun/Shiny/Dusk/Dawn), 진화 아이템(Linking Cord/King's Rock/Metal Coat/Dragon Scale/Upgrade/Dubious Disc/Protector/Electirizer/Magmarizer/Reaper Cloth/Prism Scale/Razor Claw/Razor Fang/Sachet/Whipped Dream 등 — **존재 확인 후 유지**).
- 정책: 학습장치 초중반. 기본 진화의 돌 초반 / 희귀 돌 배지 조건. 이상한 사탕 **비싸게(주간 제한 X)**. 진화 아이템은 **실제 존재하는 것만** 후보 유지.

### 3.3 실전 육성 상점 (노력치/성격/특성/개체값)
- 역할: 실전작 편의.
- 판매: EV 비타민(HP Up/Protein/Iron/Calcium/Zinc/Carbos/PP Up/PP Max), 파워 아이템(Power Weight/Bracer/Belt/Lens/Band/Anklet), EV 감소 열매(Pomeg/Kelpsy/Qualot/Hondew/Grepa/Tamato), 성격 민트(Adamant/Modest/Jolly/Timid/Bold/Calm/Careful/Impish… 민트류 전체 후보), 특성(Ability Capsule / Ability Patch 후보), 개체값(Hyper Training Candy 계열 — 실제 종류·ID 확인 필요).
- 정책: EV 아이템 적극 판매. 민트·Ability Capsule 중고가. **Hyper Training Candy는 고가 + 배지 6~8 이후 해금 후보.** 주간 제한 대신 가격·배지로 조절.
- ⚠️ 성격/특성/개체값 아이템은 밸런스 영향 큼 → 도입 전 별도 검토.

### 3.4 기술머신 상점 (SimpleTMs)
- 역할: SimpleTMs 기반 TM/TR 판매, 실전 기술작, 관장/배지 진행 연결.
- 판매: 기본 TM(초반 유틸/저~중위력), 타입별 TM(관장 타입과 연결), 고성능 TM(주력 공격/랭크업/상태이상/보조/커버리지), TR/일회용(SimpleTMs가 TR 제공 시 별도 구분), 기타(TM Case/TR Case 등 — 존재 확인).
- 정책: **실제 SimpleTMs 제공 목록 기준**. 초급/중급/고급 3단계. **타입별 관장 클리어 후 해당 타입 TM 해금** 구조 추천. 고성능 TM은 배지 4~8 이후 + 고가. TR/강력 기술은 고가 또는 TR로.
- 상세 표(초급/중급/고급): `shop_catalog_0.1.md` §기술머신.

### 3.5 알 상점 (Eggs - Cobblemon Addon)
- 역할: 포켓몬 알 판매, 골드 소모처, 뽑기성/희귀 획득 루트.
- 판매: 기본 알(일반), 희귀 알, 타입별 알(후보), 특수 알(스타팅/화석/드래곤/Shiny 후보).
- 정책: **실제 Eggs Addon 제공 알 종류 기준.** 일반 알 초반 판매 / 희귀 알 고가 / 타입별 알 배지 조건 후보 / 스타팅·화석·드래곤 알 고가. **Shiny Egg(색이 다른 알)는 일반 판매 비추천 → 이벤트/후반 후보.** **전설 알 판매 금지.**
- **알 등급별 포켓몬 풀·정책: `egg_pool_design.md` 참조.** (풀 커스텀 가능 여부는 거기 §8 확인 항목)
- 상세 표(아이템 ID): `shop_catalog_0.1.md` §3.5.

### 3.6 메가 연구소 상점 (Mega Showdown)
- 역할: 메가진화 해금, 메가팔찌·메가스톤 판매.
- 판매: 메가팔찌(Mega Bracelet / 유사 장비 후보), 메가스톤 전체(일반/고급, **Charizardite X/Y 포함 — 별도 카테고리 X, 고급 메가스톤으로**), 원시회귀 아이템(후보).
- 정책: 메가팔찌 배지 4+ 고가. 일반 메가스톤 = 메가팔찌 보유 후. 고급 메가스톤(리자몽나이트 X/Y 등) 배지 6~8 + 고가. **메가 레쿠쟈 관련 일반 판매 금지**(전설/하늘 균열 연결).
- 상세 표: `shop_catalog_0.1.md` §메가.

### 3.7 전설 제단 상점 (PoroMonCore 커스텀)
- 역할: 전설 조우권 판매/사용, 개인 조우방 연동.
- 판매: 일반 5등급(희귀/하급/중급/상급/최상급) + 컨셉별 특수 10종(하늘·심해·대지·시간·공간·반전·빛·용왕·수호자·영원).
- 정책: 조우권 = 포켓몬 직접 지급 X, 제단에서 개인 조우방 입장 후 사용 → PoroMonCore가 확률표 굴려 소환. 하급 배지 4 / 중급 배지 6 후보 / 상급 배지 8 / 최상급 후반·극고가. **하급·중급은 2시간 필드 이벤트 병행**(상급/최상급은 개인방 중심). 영원(아르세우스) 기본 잠금. **전설 알 판매 금지.** 1인 개인방으로 횡취 방지.
- **조우권 등급별/특수 10종 소환 풀·정책: `encounter_pool_design.md` 참조.** (레쿠쟈 = 하늘 조우권 중심, 아르세우스 = 영원 조우권 중심)
- 상세 표(아이템): `shop_catalog_0.1.md` §3.7.

## 4. 판매 제한 정책
| 등급 | 품목 | 통제 |
|---|---|---|
| 무제한 판매 | 볼·회복약·상태이상 치료제·기본 음식·기본 진화의 돌·제작 가능 편의품 | 골드 |
| 가격/배지 제한 | 이상한 사탕·학습장치·희귀 진화 아이템·EV/민트/Ability Capsule·일반~중급 TM·일반 메가스톤·일반/희귀 알 | 고가 + 배지/관장 클리어 |
| 강한 제한 | 고성능 TM·고급 메가스톤(리자몽나이트 X/Y)·Hyper Training Candy·상급/최상급 전설 조우권 | 고가 + 배지 6~8 |
| 일반 판매 금지(후보) | 메가 레쿠쟈 관련·하늘/영원 등 특수 조우권 일부(레쿠쟈/아르세우스)·Shiny Egg·전설 알·시즌/왕중왕전 전용 보상 | 이벤트/후반/금지 |

> **주간 제한은 0.1 기본 미사용.** 위 통제는 전부 가격·배지·이벤트로 처리.

## 5. PoroMonCore 연동 필요 기능
- 배지/관장 클리어 조건 확인, 메가팔찌·메가스톤 구매 가능 여부, 조우권 구매 조건·사용 쿨타임
- 전설 조우 기록, 보상 지급 기록, 구매/사용 로그(`AuditLog`)
- 전설 조우권 = PoroMonCore 커스텀 아이템 등록·관리

추천 분리:
- 외부 상점/경제 모드(미도입): 일반 상점, 광물/농작물 판매, 성장 상점 일부 — 도입 시 서버 분리 재검증
- PoroMonCore 권장: 실전 육성·기술머신·알·메가 연구소 상점(해금 조건 판정)
- PoroMonCore 필수: 전설 제단 상점

## 6. 초기 구현 범위
- **0.1(문서/스캐폴드)**: 일반·성장 상점 일부 / 기술머신·알·메가·전설 조우권 **구조 문서화**(본 문서 + 카탈로그) / 전설 조우권 커스텀 아이템 설계
- **0.2**: 배지 조건 기반 성장·기술머신 상점, 메가팔찌 해금·일반 메가스톤, 알 상점
- **0.3**: 전설 제단 상점, 하급/중급/상급 조우권 + 하급·중급 2시간 필드 이벤트, 개인 조우방 연동
- **0.4+**: 리자몽나이트 X/Y·최상급 조우권·컨셉별 특수 10종(하늘/영원 등)·레쿠쟈/아르세우스 특수·시즌 보상 상점
> `roadmap.md` Phase 4~6 / `poromoncore_spec.md` 0.1(데이터 모델·명령)과 매핑.

## 7. 관련 문서
- 카탈로그(품목·ID 표): `shop_catalog_0.1.md`
- **조우권 풀: `encounter_pool_design.md` · 알 풀: `egg_pool_design.md`**
- **한글화(상점명/아이템명/설명): `../05_operations/localization_policy.md`**
- 경제(가격·골드): `economy_design.md` · 허브: `hub_design.md`
- 전설: `legendary_encounter.md` · 메가/테라: `mega_tera_unlock.md` · 짐: `gym_badge_design.md` · 리그: `league_season_design.md`
- 모드 분리: `../01_modpack/server_mod_separation.md` · 결정: `../00_project/decisions.md`(014/015/016)
- 코어: `../03_poromoncore/module_structure.md` / `config_structure.md` / `database_schema.md`
