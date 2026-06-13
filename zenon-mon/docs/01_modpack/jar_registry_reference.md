# Jar Registry Reference (실 jar 추출 — 검증된 ID/한글명)

> 출처: `modpack/client/mods/` 실제 jar 내부 추출(2026-06-05). **추측 아님 — 실 레지스트리/lang 기준.**
> 대상 jar: `Cobblemon-fabric-1.7.3+1.21.1`, `mega_showdown-fabric-1.8.4`, `SimpleTMs-fabric-2.3.3`, `eggs-cobblemon-addon-0.9`.
> 역할: 그동안 "추측 금지 → TODO"로 비워둔 **종족 ID·아이템 ID·한글 표시명**의 단일 검증 출처. 설계 문서(`encounter_pool_design.md`·`shop_catalog_0.1.md`·`egg_pool_design.md`)는 이 표를 **인용**한다.
> ⚠️ **이 문서는 "무엇이 존재하는가"(사실)만 확정한다. "어느 등급/가격에 넣는가"(설계 배치)는 각 설계 문서 소관.**
> 추출 방법: `unzip -p <jar> data/cobblemon/species/**/*.json` 의 `labels`/`nationalPokedexNumber`, `assets/<mod>/lang/ko_kr.json`. 재검증 시 동일 명령.

---

## 1. Cobblemon 종족 분류 (1025 종족 파일 기준)

`data/cobblemon/species/generationN/<id>.json`의 `labels` 배열로 분류. 종족 ID 네임스페이스 = `cobblemon:<id>`.

### 1-1. 전설 (legendary) — 71종
그중 **`restricted`(박스/표지 전설) = 27종**은 게임 내 "1마리 제한"급 핵심 전설 → **최상급(apex)·컨셉권 핵심 후보**. 나머지 44종은 준전설(트리오·사천왕급).

**restricted 27종 (apex 후보):**

| dex | id | dex | id | dex | id |
|---|---|---|---|---|---|
| 150 | mewtwo | 484 | palkia | 800 | necrozma |
| 249 | lugia | 487 | giratina | 888 | zacian |
| 250 | hooh | 643 | reshiram | 889 | zamazenta |
| 382 | kyogre | 644 | zekrom | 890 | eternatus |
| 383 | groudon | 646 | kyurem | 898 | calyrex |
| 384 | rayquaza | 716 | xerneas | 1007 | koraidon |
| 483 | dialga | 717 | yveltal | 1008 | miraidon |
| | | 718 | zygarde | 1024 | terapagos |
| | | 789 | cosmog | | |
| | | 790 | cosmoem | | |
| | | 791 | solgaleo | | |
| | | 792 | lunala | | |

**준전설 44종 (legendary, non-restricted):**
`articuno(144) zapdos(145) moltres(146) raikou(243) entei(244) suicune(245) regirock(377) regice(378) registeel(379) latias(380) latios(381) uxie(480) mesprit(481) azelf(482) heatran(485) regigigas(486) cresselia(488) cobalion(638) terrakion(639) virizion(640) tornadus(641) thundurus(642) landorus(645) typenull(772) silvally(773) tapukoko(785) tapulele(786) tapubulu(787) tapufini(788) zacian외... kubfu(891) urshifu(892) regieleki(894) regidrago(895) glastrier(896) spectrier(897) enamorus(905) wochien(1001) chienpao(1002) tinglu(1003) chiyu(1004) okidogi(1014) munkidori(1015) fezandipiti(1016) ogerpon(1017)`

> ※ `restricted`=27 + 위 준전설 = 71. (레쿠쟈 384=restricted → CANON "레쿠쟈=하늘 컨셉/apex"와 정합.)

### 1-2. 환상 (mythical) — 23종
`mew(151) celebi(251) jirachi(385) deoxys(386) phione(489) manaphy(490) darkrai(491) shaymin(492) arceus(493) victini(494) keldeo(647) meloetta(648) genesect(649) diancie(719) hoopa(720) volcanion(721) magearna(801) marshadow(802) zeraora(807) meltan(808) melmetal(809) zarude(893) pecharunt(1025)`

> ※ **arceus(493)=mythical** → CANON "아르세우스=영원 컨셉권" 정합. hoopa(720)=mythical(컨셉권 후보). No legendary/mythical **eggs**(CANON 유지).

### 1-3. Ultra Beast — 11종
`nihilego naganadel kartana pheromosa guzzlord blacephalon xurkitree buzzwole poipole stakataka celesteela`

### 1-4. Paradox — 20종
`greattusk ironboulder ironcrown screamtail sandyshocks ironbundle ironjugulis walkingwake ironvaliant ragingbolt ironleaves ironhands slitherwing brutebonnet ironmoth roaringmoon gougingfire irontreads fluttermane ironthorns`

> UB·패러독스는 전설/환상 라벨과 별개. 컨셉권/특수 풀 후보로 활용 가능(설계 소관).

---

## 2. Mega Showdown — 배틀 기믹 아이템 (검증 ID → 한글)

네임스페이스 `mega_showdown:<id>`. 한글명은 MSD `ko_kr.json` 기본 포함(번역 불필요).

### 2-1. 기믹 키 아이템
| id | 한글 | 용도 |
|---|---|---|
| `keystone` | 키스톤 | 메가진화 트리거(키스톤) |
| `mega_bracelet` / `mega_ring` | 메가링 | 메가 착용구(색상 변형: red/pink/yellow/green/blue/black) |
| `z_ring` | Z-링 | Z무브(색상 변형 6종) — **CANON: Z 미사용**(보류) |
| `tera_orb` | 테라스탈 오브 | 테라스탈 — **CANON: 테라 미사용**(보류) |
| `dynamax_band` | 다이맥스밴드 | 다이맥스 — **CANON: 다이맥스 미사용**(보류) |
| `dynamax_candy` | 다이맥스사탕 | 다이맥스 보조 |
| `red_orb` | 주홍구슬 | 그란돈 원시회귀(프라이멀) |
| `blue_orb` | 쪽빛구슬 | 가이오가 원시회귀(프라이멀) |

> CANON(결정/메가정책): **메가만 허용, Z·테라·다이맥스 게이트/보류.** → 상점은 키스톤·메가링·메가스톤 중심. Z-링/테라오브/다이맥스밴드는 **판매 제외 또는 후반 게이트**(설계 소관).

### 2-2. 메가스톤 — 47종 (`<species>ite` / `_x`/`_y`)
CANON: **리자몽나이트 X/Y = 메가스톤 카테고리 내 고급가**, 메가 연구소 판매.

`abomasite(눈설왕) absolite(앱솔) aerodactylite(프테라) aggronite(보스로라) alakazite(후딘) altarianite(파비코리) ampharosite(전룡) audinite(다부니) banettite(다크펫) beedrillite(독침붕) blastoisinite(거북왕) blazikenite(번치코) cameruptite(폭타) charizardite_x(리자몽X) charizardite_y(리자몽Y) diancite(디안시) galladite(엘레이드) garchompite(한카리아스) gardevoirite(가디안) gengarite(팬텀) glalitite(얼음귀신) gyaradosite(갸라도스) heracronite(헤라크로스) houndoominite(헬가) kangaskhanite(캥카) latiasite(라티아스) latiosite(라티오스) lopunnite(이어롭) lucarionite(루카리오) manectite(썬더볼트) mawilite(입치트) medichamite(요가램) metagrossite(메타그로스) mewtwonite_x(뮤츠X) mewtwonite_y(뮤츠Y) pidgeotite(피죤투) pinsirite(쁘사이저) sablenite(깜까미) salamencite(보만다) sceptilite(나무킹) scizorite(핫삼) sharpedonite(샤크니아) slowbronite(야도란) steelixite(강철톤) swampertite(대짱이) tyranitarite(마기라스) venusaurite(이상해꽃)`

> ⚠️ 메가 레쿠쟈는 별도 스톤 없음(게임 원작대로 기술 `governing`/전용 처리 추정) → MSD lang에 `rayquazite` 미발견. 레쿠쟈 메가 해금 방식은 jar 추가 확인 필요(TODO).
> ⚠️ `tera_pouch_*`는 메가스톤 아님(테라 파우치). 정규식 `ite` 매칭 시 `white` 오검출 주의.

---

## 3. SimpleTMs (`simpletms:<id>`)
- 컨테이너/베이스 아이템 lang 확인: `tm_blank`(빈 TM) · `tr_blank`(빈 TR) · `case_tm`(기술머신 케이스) · `case_tr`(기술레코드 케이스) · `machine_tm`.
- 개별 기술 TM은 **lang 키 1:1 부재** → TM 이름은 **런타임 동적 합성**(TM + 기술명) 추정. 상점에서 "특정 기술 TM" 판매 시 아이템 ID/컴포넌트(기술 지정 방식)를 jar/레지스트리로 추가 확인 필요(TODO).
- 한글: `ko_kr.json` 기본 포함.

## 4. Eggs Addon (`diesse` 네임스페이스)
- loot_table 4종 확정: `data/diesse/loot_table/{common,rare,rides,shiny}.json` → **datapack 오버라이드로 부화 풀 커스텀 가능**(CANON egg_pool 전제 충족).
- 상인 없음(둥지 스폰형). 알 아이템 ID는 추가 확인 필요(TODO).

---

## 5. 이 레퍼런스가 푸는 설계 TODO

| 설계 문서 | 기존 TODO | 본 레퍼런스로 확정 가능 |
|---|---|---|
| `encounter_pool_design.md` | species ID 전부 TODO | 전설 71(restricted 27=apex / 준전설 44)·환상 23·UB 11·패러독스 20 **실 ID 확정**. 등급 배치만 설계로 결정 |
| `shop_catalog_0.1.md` | 아이템 ID TODO | 키스톤·메가링·메가스톤 47·주홍/쪽빛구슬 **ID·한글명 확정**. Z/테라/다이맥스 아이템 존재 확인(판매정책은 설계) |
| `egg_pool_design.md` | 커스텀 가능 여부 미확인 | loot_table 4종으로 **datapack 커스텀 가능 확정** |
| `localization_policy.md` | mod 한글 커버리지 | Cobblemon·MSD·SimpleTMs **ko_kr 기본 포함 확인**(LM만 신규 번역 필요) |

> **다음 설계 액션**: encounter_pool/shop_catalog에서 위 실 ID로 TODO 치환 + 등급/가격 배치(별도 패스). restricted 27 → 최상급+컨셉권, 준전설 44 → 중·상급, UB/패러독스 → 컨셉/특수가 자연스러운 출발점(설계 확정 필요).

## 6. 관련 문서
- 기능 감사: `jar_feature_audit.md` (기능/충돌 관점) · 본 문서(검증 ID 관점)
- 풀/상점 설계: `../04_game_design/{encounter_pool_design, shop_catalog_0.1, egg_pool_design}.md`
- 한글화: `../05_operations/localization_policy.md`
