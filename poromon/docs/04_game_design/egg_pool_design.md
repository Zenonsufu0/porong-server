# 알 등급별 포켓몬 풀 설계 (Egg Pool)  — 초안(DRAFT)

> 알 상점(Eggs - Cobblemon Addon, Diesse)의 등급별 **부화 포켓몬 풀**. 상점 정책: `shop_design.md` §3.5 / `shop_catalog_0.1.md` §3.5.
> 화폐: 골드 단일(결정 014). 알 = 골드 소모처 + 뽑기성/희귀 획득 루트.
> ✅ **커스텀 메커니즘 검증 완료(2026-06-05)** — §8: 부화 종 = `egg/poke/{common,rare,shiny}.mcfunction`의 `spawnpokemon <id>` 인덱스, loot_table은 난수 범위만. **mcfunction 오버라이드로 풀 완전 커스텀 가능**(가중=인덱스 중복). 아래 §1~6 설계 풀은 그 위에서 정의. **종족값/타입 미수정.**

## 0. 원칙
- 일반 알은 초반부터 저가, 희귀·특수 알은 고가/배지 조건.
- 알 풀과 희귀 조우권 풀(`encounter_pool_design.md`)은 **루트 중복 허용**(알=부화, 조우권=직접 조우). 의도적 이중 루트.
- **전설 알 판매 금지.** **색이 다른 알(Shiny)은 일반 판매 비추천**(이벤트/후반).

## 1. 일반 알 풀 (= 모드 common, §8-2)
- 성격: 입문용 — **모드 common 56종 = 전 세대 스타터 + 흔한 종**(eevee·pichu·gastly·abra·riolu 등).
- 채택: **모드 기본 common 사용**(또는 `egg/poke/common.mcfunction` 오버라이드로 가감). 판매=초반·저가·무제한.

## 2. 희귀 알 풀 (= 모드 rare, §8-2)
- 성격: 비전설 희귀/고가치 — **모드 rare 26종 = 유사전설·강종**(dratini·larvitar·gible·jangmoo·honedge·zorua·lapras·ditto 등).
- ⚠️ **희귀 조우권 풀(#3, `encounter_pool_design.md §1`)과 다수 중복** — 의도적 이중 루트(§0). 판매=고가, 배지 조건 후보.

## 3. 스타팅 알 풀 (⚠️ 모드 common에 이미 포함)
- **모드 common이 이미 전 세대 스타터를 전부 포함**(§8-2) → 별도 "스타팅 알" 등급은 **중복**. 선택지:
  - (A) 별도 등급 안 둠 — 스타터는 일반 알에서 나옴(단순).
  - (B) 스타터를 common에서 빼고 **전용 스타팅 알**(고가)로 분리 — `common.mcfunction`에서 스타터 제거 + 신규 `egg/poke/starter` 추가. (스타터 지급 정책과 정합 시)
  - 결정 필요(설계). 기본 권장 = (A).

## 4. 화석 알 풀
- 성격: 화석 포켓몬.
- 풀(예시): 암나이트/투구/프테라/리리라/아노프스/암라르고/타이르 등.
- 판매/해금: 고가, 후반 후보. (Cobblemon 화석 부활 시스템과 중복/정합 확인)

## 5. 드래곤 알 풀 (커스텀 — 모드 미제공)
- 성격: 드래곤 계열. 풀(예시): 미뇽(dratini)·딥상어동(gible)·모노두(deino)·미끄메라(goomy)·짜랑꼬(jangmoo)·드라꼰(dreepy) 등 드래곤 라인.
- 판매/해금: 고가, 배지 조건 후보. **신규 `egg/poke/dragon` + loot table 커스텀 필요**(§8-2).

## 6. 타입별 알 풀
- 성격: 타입별 포켓몬 묶음(불꽃/물/풀/전기/얼음/고스트/드래곤 등).
- 풀: 각 타입 대표 종(예시) — Eggs Addon이 타입별 알을 제공하는지 확인 후 확정.
- 판매/해금: **배지 조건 후보**(해당 타입 관장 클리어 연계 가능).

## 7. 색이 다른 알(Shiny) / 전설 알 정책
- **Shiny Egg(색이 다른 알)**: **일반 판매 비추천.** 이벤트/후반/초고가 후보로만. (이로치 가치 보호)
- **전설 알**: **판매 금지.** 전설은 조우권+사설룸 루트로만(결정 008/013). 알로 전설 획득 불가.

## 8. Eggs Addon 실제 동작 — jar 전수 검증 (2026-06-05)
데이터팩형 모드(namespace `diesse`, **animated_java + mcfunction 기반**, 자바 아이템 모드 아님). 실제 구조:

### 8-1. 부화 종족 = mcfunction (★ 핵심 — 문서 기존 "loot table" 설명은 부정확했음)
- **종족 리스트는 `data/diesse/function/egg/poke/{common,rare,shiny}.mcfunction`에 있다.** 형식:
  ```
  execute store result score &common dsegg.random run loot spawn ~ ~ ~ loot diesse:common
  execute at @s if score &common dsegg.random matches 0 run spawnpokemon squirtle
  execute at @s if score &common dsegg.random matches 1 run spawnpokemon bulbasaur
  ...
  ```
- **loot_table(`diesse:common` 등)은 "종족 목록"이 아니라 난수 발생기**다. `rolls {min:0,max:N}` + `stone count:0` 트릭으로 **0..N 균등 난수만** 뽑고, 그 숫자를 `matches N → spawnpokemon <id>`로 종에 매핑. (그래서 loot table만 바꿔선 종이 안 바뀜.)
- **커스텀 방법(확정)**: ① `egg/poke/<등급>.mcfunction`의 `spawnpokemon <id>` 라인 datapack 오버라이드 + ② 해당 `loot_table/<등급>.json`의 `rolls.max`를 엔트리 수에 맞춤. **가중치 = 같은 종을 여러 인덱스에 중복**(예 rare는 `matches 8/9 → zorua`로 zorua 2배).
- `spawnpokemon <id>`의 `<id>` = **Cobblemon 종족명(네임스페이스 없이)** — `jar_registry_reference.md §1` ID와 동일.

### 8-2. 기본 풀 (모드 제공 — 검증된 실제 종)
- **common (57엔트리/56종)**: 전 세대 스타터 전부(squirtle·bulbasaur·charmander … sprigatito·fuecoco·quaxly) + 흔한 종(eevee·pichu·gastly·abra·riolu·ralts 등). → **스타터가 이미 common에 포함**(설계 §3 스타팅 알과 중복).
- **rare (27엔트리/26종)**: 유사전설·강종 — dratini·larvitar·gible·jangmoo·honedge·zorua(×2)·lapras·ditto·spiritomb·armarouge·ceruledge·krookodile 등. → **희귀 조우권 풀(#3)과 다수 중복**(의도적 이중 루트, §0).
- **shiny (84엔트리/81종)**: common+rare 합집합의 **이로치판**.
- **rides**: 별도(`rolls max:1`) — 탈것용 소수.
- **모드 미제공**: 화석/드래곤/타입별 전용 등급 → 신규 `egg/poke/*` + loot table **커스텀 추가** 필요(PoroMonCore 관리 datapack).

### 8-3. 알 아이템 & 획득 (★ 골드 경제 정합 핵심)
- **알 아이템 = `minecraft:armor_stand` + 컴포넌트**(`custom_model_data` common=1/rare=2/shiny=3, `entity_data` tag `egg.<등급>.placed`, animated_java 디스플레이). 표준 등록 아이템 아님 → **TODO(알 아이템 ID) 해소.**
- **지급 함수 존재**: `function diesse:egg/give/{common,rare,shiny}` (또는 `egg/give/all`) → 플레이어에게 알 armor_stand 지급. **PoroMonCore 상점이 이 함수 호출로 알 지급 가능**(골드 차감 후).
- **⚠️ 모드 자체 판매 = 골드 경제 우회**: `egg/villager_spawn.mcfunction`이 **방랑상인**을 스폰해 알을 **바닐라 화폐로 판매**(일반=금괴10, 희귀=다이아5, 색違=네더라이트1). PoroMon은 골드 단일 내부잔액(결정 014/024)이라 우회.
  - ✅ **비활성 적용·검증(2026-06-05, 결정 027)**: OpenLoader 팩 `poromon_egg_control`이 `data/diesse/function/egg/villager_spawn.mcfunction`을 **빈 함수로 오버라이드** → `egg/main`이 24000틱마다 호출해도 상인 미스폰. 서버 기동 검증: `datapack list`에 활성 + **eggs 모드보다 뒤 로드(우선순위 우위)**, `function diesse:egg/villager_spawn` 실행 시 빈 동작. 소스: `modpack/overrides/config/openloader/packs/poromon_egg_control/`(클라/서버 공통). → 알은 "야생 구매" 불가, 판매는 PoroMonCore 골드 상점 `egg/give/<등급>`만.
- **둥지 자연 스폰**: `egg/nest/all.mcfunction` — `predicate diesse:spawn_nest` + 야생 cobblemon 근처 조건으로 둥지 자연 생성. 빈도/비활성은 해당 함수/predicate 오버라이드로 조정.

## 9. 등급별 정책 요약
| 알 풀 | 판매 | 해금 | 가격 | 비고 |
|---|---|---|---|---|
| 일반 알 | 판매 | 없음 | 저가 | 무제한 |
| 희귀 알 | 판매 | 배지 후보 | 고가 | 비전설 희귀 |
| 스타팅 알 | 판매 | 후반 후보 | 고가 | 스타터 |
| 화석 알 | 판매 | 후반 후보 | 고가 | 화석 종 |
| 드래곤 알 | 판매 | 배지 후보 | 고가 | 드래곤 계열 |
| 타입별 알 | 판매 | 타입 관장 클리어 후보 | 중~고가 | 타입 묶음 |
| 색이 다른 알(Shiny) | **보류** | 이벤트/후반 | 초고가 | 일반 판매 비추천 |
| 전설 알 | **금지** | — | — | 판매하지 않음 |

> 통제는 가격·배지 중심(주간 제한 0.1 미사용, 결정 016).

## 10. 관련 문서
- 상점: `shop_design.md` §3.5 / `shop_catalog_0.1.md` §3.5 · 조우권 풀: `encounter_pool_design.md`
- 결정: `../00_project/decisions.md`(014/015/016) · 한글화: `../05_operations/localization_policy.md`
- 코어: `../03_poromoncore/module_structure.md` / `config_structure.md`
