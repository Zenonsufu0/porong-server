# Jar Feature Audit (실제 jar 기준 기능 검토)  — 초안(DRAFT)

> 소스: `reports/jar_inspection/`(Cobblemon/LegendaryMonuments/SimpleTMs/Eggs/MegaShowdown/LetMeDespawn 6개 jar 내부 덤프) + `reports/jar_inspection_summary.md` + `modpack/client/mods/`(80 jar).
> 목적: 설계한 기능(상점/조우권/알/메가/한글화/서버분리)이 **실제 모드팩에서 가능한지** jar 근거로 검증.
> 표기: **✅ jar 확인** = jar 내부에서 직접 확인 / **⚠️ 추정** = 설계 추정 / **TODO** = 미확인. *species/아이템 ID는 확인된 것만 적고 나머지는 TODO.*

---

## 1. Legendary Monuments (legendarymonuments 7.8)

### ✅ jar 확인
- environment `*`(양쪽), entrypoints에 **main/client/fabric-datagen/terrablender** 모두 존재 → **월드 생성(바이옴/구조물) 능동 추가**.
- **신규 하드 의존성**: `cobblemon>=1.7.0, mega_showdown, accessories>=1.1.0, fabric-api, **chipped, cobblefurnies, terrablender**`.
  - ⚠️ **chipped / cobblefurnies / terrablender 는 `modpack/client/mods` 80개 목록에 별도 jar로 없음** → **JIJ(jar-in-jar) 번들 추정**(클라 프로필이 동작 중이므로). **TODO: 서버에서도 JIJ로 함께 로드되는지 확인**(아니면 서버 미기동).
- **자체 전설 획득 시스템 다수 확인**(블록/아이템 lang 기준):
  - **제단(pedestal)**: dialga/palkia/giratina/lugia/ho_oh/raikou/entei/suicune/latias/heatran/kyurem/reshiram/zekrom/mew/zacian/zamazenta/hoopa.
  - **잠금(lock)+열쇠(key)**: temple/lugia/victini/regi(rock/ice/steel)/regigigas/elekidrago. Liberty Pass(victini), Lugia Key, Temple Key 등.
  - **항아리(urn)**: "Defeat Pokémon of matching type to charge → Right-click to summon a legendary"(타입별 충전식 소환). galarian urn 포함.
  - **피리/도구**: Azure Flute("enter the Hall of Origin"→**아르세우스**), Fullmoon Whistle→**크레세리아**, Newmoon Whistle→**다크라이**, Curry of Justice→**케르디오**, Old Sea Map→**뮤**, GS Ball, Silver Wing→루기아 보정, Vortex Stone(삼신조→루기아 도전권), Proof of Conquest(M/A/U)→**미스피/아그놈/유크시**, Truth/Ideals Bottle→**큐레무**, Light/Dark Stone→**레시라무/제크로무**, globes(space/time/antimatter)→펄기아/디아루가/기라티나, Cosmic Catalyst, Red Chain.
  - **Pokémon Trial Spawner**(전용 trial spawner 블록) + **Eternatus Cocoon**(에터너스).
- **자체 한글화 없음**: lang = en/es/ja/ru/zh(cn/hk/tw)만. **ko_kr 없음.**

### ⚠️ ZenonMonCore 조우권/필드 이벤트와 충돌 (확정적)
- 이 모드는 **조우권/사설룸 없이도** 제단·항아리·피리·trial spawner·구조물로 전설/환상(아르세우스 포함)을 **누구나 월드에서 획득**하게 한다. → **ZenonMonCore 통제(조우권+사설룸+통제 필드 이벤트)를 정면 우회**(결정 017 우려가 jar로 확정됨).

### TODO (제어 가능 여부 — 미확인)
1. **구조물/바이옴 자동 생성 비활성** 가능 여부: terrablender 바이옴 + 구조물 datapack을 끌 수 있는지(config 파일 jar 내 미발견 → TODO). 구조물 `placed_feature`/`structure_set` datapack 오버라이드로 비활성 가능성 검토.
2. **소환 아이템/블록 비활성**: 제단·항아리·피리류를 레시피 제거(datapack) 또는 획득 차단으로 막을 수 있는지.
3. chipped/cobblefurnies/terrablender JIJ 여부 및 서버 로드.
4. 대안: 끌 수 없다면 ZenonMonCore 전설 정책을 **재정의**(예: LM 구조물을 "통제된 후반 콘텐츠"로 수용 + 조우권은 LM 미포함 전설/연출용으로 분리). → **설계 결정 필요.**

> species ID는 cobblemon 네임스페이스(예: `cobblemon:dialga`) 추정이나 본 jar에서 직접 노출 안 됨 → **TODO**. 단 **구현 전설 목록**(위 제단/소환 대상)은 사실상 확정.

---

## 2. Eggs - Cobblemon Addon (mr_eggs_cobblemonaddon 0.9)

### ✅ jar 확인
- **순수 데이터팩형**(assets 없음, environment `*`, depends `fabric-resource-loader-v0`만). animated_java로 알 연출.
- **알 풀 = 루트 테이블 4종**: `data/diesse/loot_table/common.json` · `rare.json` · `rides.json` · `shiny.json`.
- `data/diesse/predicate/spawn_nest.json` + `data/minecraft/tags/block/hatch.json` + load/tick functions → **둥지(nest) 스폰 + 부화** 방식(상인/trade 아님).

### 검토 결론
- **알 풀 커스텀: ✅ 가능** — `data/diesse/loot_table/{common,rare,shiny,rides}.json`를 **datapack 오버라이드**로 교체하면 부화 풀 변경 가능(설계의 등급별 풀을 여기에 매핑).
- **등급 구조**: 모드 기본 = common / rare / shiny / **rides**(탈것). ⚠️ 설계의 "스타팅/화석/드래곤/타입별 알"은 **모드가 기본 제공하지 않음** → 그 등급들은 **ZenonMonCore 커스텀 또는 추가 loot table 정의** 필요.
- **랜덤 알 상인/villager/trade 비활성화**: 이 모드는 **상인/trade를 추가하지 않음**(둥지 스폰 기반). → "상인 비활성화" 대상 없음. 대신 **둥지 자연 스폰**이 통제 대상 → load/tick function·spawn_nest predicate 비활성/조정으로 막을 수 있는지 **TODO(함수 내용 미확인)**.
- **알 아이템 ID: TODO** — assets/lang 없음. 알이 표준 등록 아이템인지(block/entity/animated_java 디스플레이인지) 불명확 → ID 확인 불가, **추측 금지**.

### ⚠️ 대안 (커스텀 알)
- 둥지 스폰을 끄기 어렵거나 알 아이템 ID가 불명확하면, **ZenonMonCore 커스텀 알 아이템 + 자체 부화 풀**로 상점 알을 구현(모드 알과 분리). 모드 알 풀은 datapack로 별도 운영.

---

## 3. SimpleTMs (simpletms 2.3.3)

### ✅ jar 확인
- environment `*`, depends `fabric-api, architectury 13.x, cobblemon>=1.7.1`.
- **아이템(모델 경로 기준)**:
  - `simpletms:tm_blank`(빈 TM), `simpletms:tr_blank`(빈 TR)
  - **무브별 TM 아이템 `simpletms:tm_<move>`** (예: `tm_flamethrower`, `tm_icebeam`, `tm_earthquake`, `tm_dragonclaw` …) — 모델 1000+개 확인(전 기술 망라). ⚠️ 무브별 **TR 아이템 `simpletms:tr_<move>`**도 존재 추정(itemGroup `tr_items` + tr_blank). → 정확한 등록 ID는 모델 경로와 일치 추정, **최종 레지스트리 확인 권장**.
  - **`simpletms:case_tm`(TM Case), `simpletms:case_tr`(TR Case)** ✅
  - **`simpletms:machine_tm`(TM Machine, 블록)** ✅
  - 아이템 그룹: `tm_items / tr_items / custom_tm_items / custom_tr_items / tm_storage_items`.
- **TM/TR 월드 단위 비활성 토글 존재** ✅ (lang: "TM's have been disabled in this world" / blank TM·TR 개별) → 서버 config/gamerule로 제어 가능 추정.
- **한국어 lang 기본 포함** ✅ `assets/simpletms/lang/ko_kr.json` (UI/타입/카테고리까지 번역됨).

### 검토 결론
- **상점 후보**: tm_blank/tr_blank, case_tm/case_tr, machine_tm, 그리고 무브별 tm_*/tr_*. → **초급/중급/고급 분리 가능 ✅**(무브 단위로 풀 구성). 타입 검색 메타가 있어 **타입별/관장 클리어 연계 해금**도 가능.
- ⚠️ 무브가 매우 많으므로 상점엔 **선별 목록**만(전부 X). 등급 분류 = 무브 위력/유틸 기준(설계 표는 카탈로그에서 무브 선정 후 ID 확정).

---

## 4. Mega Showdown (mega_showdown 1.8.4)

> fabric.mod.json 파서 오류(제어문자)였으나 assets/lang로 정체·아이템 확인. assets: cobblemon/mega_showdown, data: accessories/cobblemon/mega_showdown/minecraft/sodiumdynamiclights.

### ✅ jar 확인 — 아이템 ID (mega_showdown 네임스페이스)
- **메가 시작 장비**: `mega_showdown:keystone`(키스톤, "Used to create Mega Bracelet") + `keystone_ore`/`keystone_block`(채굴 경로), **`mega_showdown:mega_bracelet`** (+ 색상 변형 `mega_bracelet_red/_pink/_yellow/_green/_blue/_black`). 트레이너 캐릭터 장비: korrina_glove, lysandre_ring, brendan_mega_cuff, lisia_mega_tiara, maxie_glasses 등.
- **메가스톤 `mega_showdown:<name>ite`**: abomasite, absolite, aerodactylite, aggronite, alakazite, altarianite, ampharosite, audinite, banettite, beedrillite, blastoisinite, blazikenite, cameruptite, **charizardite_x, charizardite_y**, diancite, galladite, garchompite, gardevoirite, gengarite, glalitite, gyaradosite, heracronite, houndoominite, kangaskhanite, latiasite, latiosite, lopunnite, lucarionite, manectite, mawilite, medichamite … (다수).
- **원시회귀/특수**: `blue_orb`(가이오가), red_orb(추정), `archie_anchor`, `dna_splicer`(큐레무 합체), `reins_of_unity`(버드렉스).
- **테라스탈**: `mega_showdown:tera_orb`(테라 오브) + 타입별 `<type>_tera_shard`(bug/dark/dragon/electric/fairy/fighting/fire/flying/ghost/grass/ground/ice…).
- **Z기술**: `mega_showdown:z_ring`(+색상) + 타입별 `<type>ium_z`(firium_z/electrium_z/dragonium_z…) + 전용 z(eevium_z/decidium_z/incinium_z/lunalium_z/marshadium_z…), `blank_z`.
- **다이맥스**: creativeTab `dynamax_tab` + accessories 슬롯 `dynamax_slot` 존재 ✅(다이맥스 시스템 있음). ⚠️ 구체 다이맥스 아이템(밴드/맥스버섯) ID는 이 추출에서 미캡처 → **TODO 확인**.
- **한국어 lang 기본 포함** ✅ `assets/mega_showdown/lang/ko_kr.json`(z_ring/tera_orb 등 한국어). ⚠️ 메가스톤 전체가 ko_kr에 번역됐는지는 **커버리지 확인 TODO**.

### 검토 결론
- 설계의 **메가팔찌/메가스톤/Charizardite X·Y = 실제 존재 ✅** → 상점 카탈로그 ID 확정 가능.
- **테라/다이맥스/Z 전부 아이템 존재** → 결정 011/리그 룰셋("메가만 허용, 테라·다이맥스·Z off")은 **아이템 판매를 막는 방식**으로 통제(상점 미판매 + 리그 룰셋). 기능 자체 비활성은 MSD config 확인 TODO.
- 메가팔찌가 **keystone_ore 채굴 경로**를 가지므로, "상점 골드 게이트"와 **채굴 획득이 병존** → 통제 정책에 반영 필요(상점만으론 독점 불가). **TODO: keystone_ore 월드 생성 비활성 가능 여부.**

---

## 5. 한글화 (실측)

| 모드 | namespace | ko_kr 기본 | 한글화 작업 |
|---|---|---|---|
| Cobblemon | cobblemon | ✅ 있음(`assets/cobblemon/lang/ko_kr.json`) | 거의 불필요(누락만 보완) |
| Mega Showdown | mega_showdown | ✅ 있음 | 메가스톤 커버리지 확인 후 누락만 |
| SimpleTMs | simpletms | ✅ 있음(UI/타입까지) | 거의 불필요 |
| Legendary Monuments | legendarymonuments | ❌ **없음**(en/es/ja/ru/zh만) | **ko_kr 신규 작성 1순위**(아이템/블록/툴팁 다수) |
| Eggs Addon | (diesse, data만) | ❌ lang 없음 | 표시 텍스트 거의 없음(둥지/loot) — 대상 적음 |
| ZenonMonCore 커스텀(조우권 등) | zenonmoncore | — | 직접 ko_kr + lore |

### ko_kr.json 생성 대상(우선순위)
1. **legendarymonuments** — `assets/legendarymonuments/lang/en_us.json` 키 전체 번역(제단/항아리/피리/열쇠/툴팁). **Zenon Mon-Korean-Pack 리소스팩**으로 오버라이드.
2. **mega_showdown** 누락분 보완(메가스톤 명칭 등 — ko_kr 커버리지 확인 후).
3. **ZenonMonCore 커스텀 아이템**(조우권 15종 등) — jar 자체 ko_kr.
- Cobblemon/SimpleTMs는 기본 ko_kr 양호 → 보완만.

> 리소스팩 오버라이드 우선순위·적용 검증 TODO. namespace는 위 표로 확정(추측 아님).

---

## 6. 서버 / 클라이언트 재분류 (jar 확인 반영)

- §1~4 모두 environment `*` = 양쪽 로드. **Cobblemon/MegaShowdown/SimpleTMs/Eggs/LegendaryMonuments = 서버 필수**(게임플레이 서버측). → `server_mod_separation.md` §1 현행 유지.
- **변경/추가 필요**:
  1. **LegendaryMonuments 의존 chipped/cobblefurnies/terrablender** — 별도 jar 없음(JIJ 추정). 서버에서 누락 시 미기동 → **부팅 검증 항목 추가**.
  2. LegendaryMonuments는 **mega_showdown·accessories 하드 의존** → 이 둘이 서버에 반드시 함께(이미 §1 포함, 정합 OK).
  3. Eggs Addon = 데이터팩형이지만 jar 모드로 로드되므로 서버 포함(부화 로직 서버측). 현행 §1 유지.
- **결론**: `server_mod_separation.md` 화이트리스트(19개)는 유지하되, **위 1번(전이 의존 JIJ 확인)·LegendaryMonuments 우회 검증**을 §6 체크리스트에 보강 필요(TODO 반영).

---

## 7. 종합 / 다음 액션
- **가장 큰 이슈**: Legendary Monuments가 전설 통제를 **확정적으로 우회**. 끌 수 있는지(구조물/소환/바이옴) 미확인 → **설계 결정 필요**(비활성 vs 통제 수용).
- **호재**: Mega Showdown·SimpleTMs **아이템 ID 다수 확정** + **세 코어 모드 ko_kr 기본 포함** → 상점 카탈로그·한글화 작업량 대폭 감소.
- **Eggs**: 풀 커스텀은 loot table 오버라이드로 가능, 단 등급 다양화(스타팅/화석/타입별)는 커스텀 필요, 아이템 ID 불명확.

### 갱신 후보 (별도 반영)
- `shop_catalog_0.1.md`: 메가(keystone/mega_bracelet/charizardite_x·y/…ite) + SimpleTMs(tm_blank/tr_blank/case_tm/case_tr/machine_tm/tm_<move>) **실제 ID로 교체**. Eggs 아이템 ID = TODO 유지.
- `encounter_pool_design.md`: LM 구현 전설 목록 반영 + LM 우회 경고 + species ID는 cobblemon 네임스페이스 TODO 유지.
- `egg_pool_design.md`: §8을 loot table(common/rare/shiny/rides)·datapack 오버라이드·상인 없음(둥지형)·커스텀 등급 필요로 갱신.
- `localization_policy.md`: namespace 확정 + 3코어 ko_kr 기본 포함 + LM은 ko_kr 신규 1순위.

## 8. 관련 문서
- 보고서: `../../reports/jar_inspection/` · `jar_inspection_summary.md`
- 분리: `server_mod_separation.md` · 결정: `../00_project/decisions.md`(017)
- 설계: `../04_game_design/{encounter_pool_design,egg_pool_design,shop_catalog_0.1,shop_design}.md` · `../05_operations/localization_policy.md`
