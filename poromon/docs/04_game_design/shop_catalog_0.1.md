# 상점 카탈로그 0.1 (Shop Catalog)  — 초안(DRAFT)

> `shop_design.md`의 카테고리별 **실제 판매 후보 표**. 화폐: 골드 단일(결정 014).
> ✅ **아이템 ID 대거 검증 완료(2026-06-05)**: 메가(키스톤·메가링·메가스톤 47·주홍/쪽빛구슬) + **Cobblemon 기본**(볼 12+마스터·회복약·진화의 돌 11·비타민 6·민트 25·특성캡슐/패치·사탕류) **실 ID+한글명 확정**(전수 jar 검증, 오타 0) → `01_modpack/jar_registry_reference.md`. 잔여 TODO: SimpleTMs 개별 기술 TM ID(동적 합성)·Eggs 알 아이템 ID. 남은 건 **판매정책/가격(설계 결정)**.
> 가격(골드)은 `economy_design.md` 앵커 기준 **예시 수치** — 운영 지표로 튜닝.

## 범례
- 판매 여부: 판매 / 보류(이벤트·후반) / 금지
- 해금: 없음 / 배지 N / 관장(타입) 클리어 / 메가팔찌 보유 / 이벤트·후반
- 출처: **base**(Cobblemon/Mega Showdown/SimpleTMs/Eggs 등 모드 아이템) / **custom**(PoroMonCore 커스텀)

---

## 3.1 일반 상점

### 볼 계열
> ✅ ID 검증(`jar_registry_reference.md` — Cobblemon 본체 lang). 한글명은 Cobblemon 공식(예: 슈퍼볼=Great, 하이퍼볼=Ultra).
| 상품 | 아이템 ID | 가격(예시) | 판매 | 해금 | 비고 |
|---|---|---|---|---|---|
| 몬스터볼 (Poké Ball) | `cobblemon:poke_ball` | 50 | 판매 | 없음 | |
| 슈퍼볼 (Great Ball) | `cobblemon:great_ball` | 120 | 판매 | 없음 | |
| 하이퍼볼 (Ultra Ball) | `cobblemon:ultra_ball` | 300 | 판매 | 없음 | |
| 프리미어볼 | `cobblemon:premier_ball` | 50 | 판매 | 없음 | |
| 힐볼 (Heal Ball) | `cobblemon:heal_ball` | 120 | 판매 | 없음 | |
| 네트볼 (Net Ball) | `cobblemon:net_ball` | 120 | 판매 | 없음 | |
| 다이브볼 (Dive Ball) | `cobblemon:dive_ball` | 120 | 판매 | 없음 | |
| 다크볼 (Dusk Ball) | `cobblemon:dusk_ball` | 120 | 판매 | 없음 | |
| 퀵볼 (Quick Ball) | `cobblemon:quick_ball` | 150 | 판매 | 없음 | |
| 타이머볼 (Timer Ball) | `cobblemon:timer_ball` | 120 | 판매 | 없음 | |
| 리피트볼 (Repeat Ball) | `cobblemon:repeat_ball` | 120 | 판매 | 없음 | |
| 럭셔리볼 (Luxury Ball) | `cobblemon:luxury_ball` | 200 | 판매 | 없음 | |
| 마스터볼 (Master Ball) | `cobblemon:master_ball` | — | **금지** | — | 비매(이벤트/특수만) |

### 회복 아이템
| 상품 | 아이템 ID | 가격(예시) | 판매 | 해금 | 비고 |
|---|---|---|---|---|---|
| 상처약 (Potion) | `cobblemon:potion` | 30 | 판매 | 없음 | |
| 좋은상처약 (Super Potion) | `cobblemon:super_potion` | 100 | 판매 | 없음 | |
| 고급상처약 (Hyper Potion) | `cobblemon:hyper_potion` | 250 | 판매 | 없음 | |
| 풀회복약 (Max Potion) | `cobblemon:max_potion` | 500 | 판매 | 없음 | |
| 회복약 (Full Restore) | `cobblemon:full_restore` | 700 | 판매 | 배지 후보 | 상태+HP 전회복 |
| 기력의조각 (Revive) | `cobblemon:revive` | 400 | 판매 | 없음 | |
| 기력의덩어리 (Max Revive) | `cobblemon:max_revive` | 800 | 판매 | 배지 후보 | |
| 만병통치제 (Full Heal) | `cobblemon:full_heal` | 120 | 판매 | 없음 | |
| 해독제 (Antidote) | `cobblemon:antidote` | 30 | 판매 | 없음 | |
| 마비치료제 (Paralyze Heal) | `cobblemon:paralyze_heal` | 30 | 판매 | 없음 | |
| 잠깨는약 (Awakening) | `cobblemon:awakening` | 30 | 판매 | 없음 | |
| 화상치료제 (Burn Heal) | `cobblemon:burn_heal` | 30 | 판매 | 없음 | |
| 얼음상태치료제 (Ice Heal) | `cobblemon:ice_heal` | 30 | 판매 | 없음 | |
| PP에이드/회복 (Ether/Elixir) | `cobblemon:ether` / `max_ether` / `elixir` / `max_elixir` | 200~ | 판매 | 없음 | PP 회복 |

---

## 3.2 성장 상점

### 레벨/경험치
| 상품 | 아이템 ID | 가격(예시) | 판매 | 해금 | 비고 |
|---|---|---|---|---|---|
| 이상한사탕 (Rare Candy) | `cobblemon:rare_candy` | 800 | 판매 | 없음 | 비싸게(주간 제한 X) |
| 경험사탕 XS~XL | `cobblemon:exp_candy_{xs,s,m,l,xl}` | 100~3,000 | 판매 | 없음 | 등급별 경험치 |
| 행복의알 (Lucky Egg) | `cobblemon:lucky_egg` | 6,000 | 판매 | 배지 1~2 | 경험치 증가 |
| ~~Exp. Share / 학습장치~~ | ❌ `experience_share` 미존재 | — | — | — | Cobblemon은 파티 자동 분배 → 해당 아이템 없음 |

### 진화의 돌
| 상품 | 아이템 ID | 가격(예시) | 판매 | 해금 | 비고 |
|---|---|---|---|---|---|
| 불꽃의돌 (Fire Stone) | `cobblemon:fire_stone` | 2,000 | 판매 | 없음 | 기본 |
| 물의돌 (Water Stone) | `cobblemon:water_stone` | 2,000 | 판매 | 없음 | 기본 |
| 천둥의돌 (Thunder Stone) | `cobblemon:thunder_stone` | 2,000 | 판매 | 없음 | 기본 |
| 리프의돌 (Leaf Stone) | `cobblemon:leaf_stone` | 2,000 | 판매 | 없음 | 기본 |
| 얼음의돌 (Ice Stone) | `cobblemon:ice_stone` | 2,500 | 판매 | 배지 2~4 | 희귀 후보 |
| 달의돌 (Moon Stone) | `cobblemon:moon_stone` | 2,500 | 판매 | 배지 2~4 | 희귀 후보 |
| 태양의돌 (Sun Stone) | `cobblemon:sun_stone` | 2,500 | 판매 | 배지 2~4 | 희귀 후보 |
| 빛의돌 (Shiny Stone) | `cobblemon:shiny_stone` | 3,000 | 판매 | 배지 2~4 | 희귀 후보 |
| 어둠의돌 (Dusk Stone) | `cobblemon:dusk_stone` | 3,000 | 판매 | 배지 2~4 | 희귀 후보 |
| 각성의돌 (Dawn Stone) | `cobblemon:dawn_stone` | 3,000 | 판매 | 배지 2~4 | 희귀 후보 |

### 진화 아이템 (실제 존재하는 것만 유지 — 전부 확인 필요)
| 상품 | 아이템 ID | 판매 | 비고 |
|---|---|---|---|
| Linking Cord / King's Rock / Metal Coat / Dragon Scale / Upgrade / Dubious Disc / Protector / Electirizer / Magmarizer / Reaper Cloth / Prism Scale / Razor Claw / Razor Fang / Sachet / Whipped Dream | `TODO: item id 및 존재 여부 확인 필요` | 후보 | Cobblemon 구현 여부 항목별 확인 |

---

## 3.3 실전 육성 상점

### EV 비타민
| 상품 | 아이템 ID | 판매 | 해금 | 비고 |
|---|---|---|---|---|
| 맥스업(HP)·타우린(공)·사포닌(방)·리보플라빈(특공)·키토산(특방)·알칼로이드(스피드) | `cobblemon:{hp_up,protein,iron,calcium,zinc,carbos}` | 판매 | 없음 | EV, 적극 판매 |
| 포인트업 / 포인트맥스 | `cobblemon:pp_up` / `cobblemon:pp_max` | 판매 | 배지 후보 | PP 증가 |

### 파워 아이템
| 상품 | 아이템 ID | 판매 | 비고 |
|---|---|---|---|
| Power Weight / Bracer / Belt / Lens / Band / Anklet | TODO | 판매 | EV 트레이닝 |

### EV 감소 열매
| 상품 | 아이템 ID | 판매 | 비고 |
|---|---|---|---|
| Pomeg / Kelpsy / Qualot / Hondew / Grepa / Tamato Berry | TODO | 판매 | 베리(제작/재배 가능성 확인) |

### 성격 민트
| 상품 | 아이템 ID | 가격(예시) | 판매 | 비고 |
|---|---|---|---|---|
| 민트류 전체(25종) | `cobblemon:<nature>_mint` (예 `adamant_mint`=고집·`jolly_mint`=명랑·`timid_mint`=겁쟁이·`modest_mint`=조심…) | 중고가 | 후보 | 25 성격 전수 존재 ✅ |

### 특성
| 상품 | 아이템 ID | 판매 | 비고 |
|---|---|---|---|
| 특성캡슐 (Ability Capsule) | `cobblemon:ability_capsule` ✅ | 후보 | 일반↔숨은특성 외 전환, 밸런스 검토 |
| 특성패치 (Ability Patch) | `cobblemon:ability_patch` ✅ | 보류 | 숨은특성 부여 — 존재 확인됨, 밸런스 검토 |

### IV / 개체값
| 상품 | 아이템 ID | 판매 | 해금 | 비고 |
|---|---|---|---|---|
| Hyper Training Candy 계열(타입/스탯별, 실제 종류 확인) | `TODO: item id 및 종류 확인 필요` | 후보 | 배지 6~8 | 고가, 효과 확인 필요 |

---

## 3.4 기술머신 상점 (SimpleTMs)
> ✅ jar 확인(`jar_feature_audit.md` §3): namespace `simpletms`. 무브별 TM 아이템 `simpletms:tm_<move>` 1000+종, TR `simpletms:tr_<move>` 추정. (등록 ID는 모델 경로와 일치 추정 — 최종 레지스트리 확인 권장)

| 등급/품목 | 아이템 ID(확인) | 해금 조건 | 비고 |
|---|---|---|---|
| 빈 TM / 빈 TR | `simpletms:tm_blank` / `simpletms:tr_blank` ✅ | 없음 | 추출용 |
| TM Case / TR Case | `simpletms:case_tm` / `simpletms:case_tr` ✅ | 없음 | 보관 |
| TM Machine(블록) | `simpletms:machine_tm` ✅ | 없음 | 기술머신 |
| 초급 무브 TM | `simpletms:tm_<move>` (무브 선정 TODO) | 없음 | 저~중위력/유틸 |
| 중급 무브 TM | `simpletms:tm_<move>` (선정 TODO) | 배지 2~4 / 타입 관장 클리어 | 타입별 |
| 고급 무브 TM | `simpletms:tm_<move>` (선정 TODO) | 배지 6~8 | 실전급(예 `tm_earthquake`/`tm_icebeam`/`tm_flamethrower` 등) |

> ✅ 초급/중급/고급 분리 가능(무브 단위). **타입별 관장 클리어 시 해당 타입 TM 해금** 가능(타입 메타 존재). 전 무브 판매 X — 선별. 무브 등급 배정 = TODO.
> ✅ SimpleTMs는 **TM/TR 월드 비활성 토글** 보유(서버 config 확인).

---

## 3.5 알 상점 (Eggs - Cobblemon Addon)
> ✅ jar 확인(`jar_feature_audit.md` §2): **데이터팩형**(namespace `diesse`). 기본 풀 = loot table **common/rare/shiny/rides** 4종. assets/lang 없음 → **알 아이템 ID 불명확(둥지/animated_java) = TODO, 추측 금지.** 상인/trade 없음(둥지 스폰형).

| 알 종류(설계) | 모드 제공 | 아이템 ID | 판매 | 해금 | 비고 |
|---|---|---|---|---|---|
| 일반 알 | ✅ common loot table | `TODO: item id 확인 필요` | 판매 | 없음 | 기본 |
| 희귀 알 | ✅ rare loot table | TODO | 판매 | 배지 후보 | 고가 |
| 색이 다른 알(Shiny) | ✅ shiny loot table | TODO | 보류 | 이벤트/후반 | 일반 판매 비추천 |
| 탈것 알(rides) | ✅ rides loot table | TODO | 후보 | 후보 | 모드 기본 등급 |
| 스타팅/화석/드래곤/타입별 알 | ❌ 모드 미제공 | — | 후보 | — | **PoroMonCore 커스텀 또는 loot table 추가 필요** |
| 전설 알 | — | — | **금지** | — | 판매하지 않음 |

> 풀 커스텀 = `data/diesse/loot_table/{common,rare,shiny,rides}.json` datapack 오버라이드로 가능(`egg_pool_design.md` §8).

---

## 3.6 메가 연구소 상점 (Mega Showdown)
> ✅ jar 확인(`jar_feature_audit.md` §4): namespace `mega_showdown`. Charizardite X/Y = 메가스톤 카테고리(결정 016).

| 상품명 | 아이템 ID(확인) | 등급 | 해금 조건 | 비고 |
|---|---|---|---|---|
| 키스톤 | `mega_showdown:keystone` ✅ | 재료 | 채굴 가능(keystone_ore) | 메가팔찌 재료 |
| 메가팔찌 (Mega Bracelet) | `mega_showdown:mega_bracelet` ✅ (+색상 변형) | 기본 | 배지 4 + 고가 | 메가진화 시작 |
| Charizardite X | `mega_showdown:charizardite_x` ✅ | 고급 | 배지 6~8 후보 | 메가스톤 카테고리 |
| Charizardite Y | `mega_showdown:charizardite_y` ✅ | 고급 | 배지 6~8 후보 | 메가스톤 카테고리 |
| 일반/기타 메가스톤 (47종 전체) | `mega_showdown:<name>ite`·`_x`/`_y` ✅ — **전수 확인**(§ 메가스톤 목록) | 일반~고급 | 메가팔찌 보유 | 라티/거북왕/이상해꽃/한카리아스/루카리오 등 |
| 원시회귀 구슬 | `mega_showdown:red_orb`(주홍구슬=그란돈) ✅ / `mega_showdown:blue_orb`(쪽빛구슬=가이오가) ✅ | 후보 | 후반 | 원시회귀(Primal) |
| 테라 오브 / 테라 조각 | `mega_showdown:tera_orb` / `<type>_tera_shard` ✅ | — | **상점 미판매(리그 off)** | 결정 011 |
| Z-링 / Z 크리스탈 | `mega_showdown:z_ring` / `<type>ium_z` ✅ | — | **상점 미판매(off)** | 결정 011 |
| 다이맥스 관련 | `dynamax_tab` 존재, 구체 ID **TODO** | — | **상점 미판매(off)** | 결정 011 |
| Mega Rayquaza 관련 | — | — | **일반 판매 금지** | 하늘 조우권 연결 |

> ⚠️ 메가팔찌는 keystone_ore **채굴 경로**도 존재 → 상점 골드 독점 불가. keystone_ore 월드 생성 비활성 가능 여부 TODO.

#### 메가스톤 47종 전체 (`mega_showdown:` / 검증 — `jar_registry_reference.md` §2)
> 판매: 전부 "메가팔찌 보유" 해금. 등급(일반/고급)·가격은 종에 따라 설계 배분(리자몽/뮤츠/라티 X·Y류=고급).

| ID | 한글 | ID | 한글 | ID | 한글 |
|---|---|---|---|---|---|
| abomasite | 눈설왕나이트 | gardevoirite | 가디안나이트 | pinsirite | 쁘사이저나이트 |
| absolite | 앱솔나이트 | gengarite | 팬텀나이트 | sablenite | 깜까미나이트 |
| aerodactylite | 프테라나이트 | glalitite | 얼음귀신나이트 | salamencite | 보만다나이트 |
| aggronite | 보스로라나이트 | gyaradosite | 갸라도스나이트 | sceptilite | 나무킹나이트 |
| alakazite | 후딘나이트 | heracronite | 헤라크로스나이트 | scizorite | 핫삼나이트 |
| altarianite | 파비코리나이트 | houndoominite | 헬가나이트 | sharpedonite | 샤크니아나이트 |
| ampharosite | 전룡나이트 | kangaskhanite | 캥카나이트 | slowbronite | 야도란나이트 |
| audinite | 다부니나이트 | latiasite | 라티아스나이트 | steelixite | 강철톤나이트 |
| banettite | 다크펫나이트 | latiosite | 라티오스나이트 | swampertite | 대짱이나이트 |
| beedrillite | 독침붕나이트 | lopunnite | 이어롭나이트 | tyranitarite | 마기라스나이트 |
| blastoisinite | 거북왕나이트 | lucarionite | 루카리오나이트 | venusaurite | 이상해꽃나이트 |
| blazikenite | 번치코나이트 | manectite | 썬더볼트나이트 | charizardite_x | 리자몽나이트 X |
| cameruptite | 폭타나이트 | mawilite | 입치트나이트 | charizardite_y | 리자몽나이트 Y |
| diancite | 디안시나이트 | medichamite | 요가램나이트 | mewtwonite_x | 뮤츠나이트 X |
| galladite | 엘레이드나이트 | metagrossite | 메타그로스나이트 | mewtwonite_y | 뮤츠나이트 Y |
| garchompite | 한카리아스나이트 | pidgeotite | 피죤투나이트 | | |

> ⚠️ 메가 레쿠쟈는 전용 스톤 없음(MSD에 `rayquazite` 미발견 — 기술/전용 처리 추정, 추가 확인 TODO). 디안시나이트(diancite)는 환상 디안시용(메가) — 디안시 자체는 이벤트 게이트(`encounter_pool_design.md`).

---

## 3.7 전설 제단 상점 (PoroMonCore 커스텀 아이템)
> 조우권은 기본 모드 아이템이 아니라 **PoroMonCore 커스텀**. 포켓몬 직접 지급 X — 개인 조우방 도전 기회.

| 조우권 | 구현 방식 | 판매 여부 | 해금 조건 | 비고 |
|---|---|---|---|---|
| 희귀 조우권 | PoroMonCore(custom) | 판매 | 없음 또는 배지 2 | 비전설 600족/희귀 |
| 하급 전설 조우권 | PoroMonCore(custom) | 판매 | 배지 4 | 고가 · 필드 이벤트 병행 |
| 중급 전설 조우권 | PoroMonCore(custom) | 판매 | 배지 6 후보 | 고가 · 필드 이벤트 병행 |
| 상급 전설 조우권 | PoroMonCore(custom) | 판매 | 배지 8 | 매우 고가 · 개인방 |
| 최상급 전설 조우권 | PoroMonCore(custom) | 보류 | 후반/이벤트 | 극고가 · 개인방 |
| 컨셉별 특수 10종(하늘~영원) | PoroMonCore(custom) | 테마별 상이 | 테마별 | 하늘=레쿠쟈 / 영원=아르세우스(기본 잠금) |
| 전설 재도전권 | PoroMonCore(custom) | 후보 | TODO | 설계 미정 |
| 전설 알 | — | **금지** | — | 판매하지 않음 |

> 조우권 등급별/특수 10종 풀 = `encounter_pool_design.md` + `config_structure.md`(legendary_pools.yml/legendary_events.yml, 향후). species ID = TODO.

---

## 검증 TODO 요약
1. ✅ **Mega Showdown 해소**: 키스톤·메가팔찌·메가스톤 47종(리자몽/뮤츠 X·Y 포함)·주홍/쪽빛구슬 실 ID 확정. 레쿠쟈 메가 전용스톤 없음(추가 확인).
2. ✅ **Cobblemon 기본 해소**: 볼(몬스터~럭셔리·마스터=비매)·회복약·진화의 돌 11·비타민 6·PP·민트 25·특성캡슐/패치·이상한사탕/경험사탕/행복의알 실 ID 확정. Exp Share=미존재(자동분배).
3. ⚠️ **SimpleTMs 잔여**: 컨테이너(tm_blank/tr_blank/case/machine) 확정. **개별 기술 TM ID는 동적 합성 추정** → 특정 기술 판매 시 아이템 ID/컴포넌트 방식 레지스트리 확인 필요.
4. ⚠️ **Eggs 잔여**: loot_table 4종 확정(커스텀 가능). **알 아이템 ID 불명확**(둥지 스폰형, lang 없음) — 추가 확인.
5. 가격은 운영 지표로 튜닝(현재 예시). 진화 아이템(킹스록 등)·하이퍼트레이닝 개체값 아이템은 종별 존재 확인 잔여.

## 관련
- 설계/정책: `shop_design.md` · 가격 앵커: `economy_design.md`
- 전설: `legendary_encounter.md` · 메가: `mega_tera_unlock.md` · 결정: `../00_project/decisions.md`(014/015/016)
