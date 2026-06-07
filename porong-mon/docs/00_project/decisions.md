# PoroMon 결정 기록

이 문서는 중요한 프로젝트 결정을 기록한다.

## 결정

### 001. 서버 타입

PoroMon은 메인 Paper 기반 Poro RPG 서버의 일부가 아닌, 별도의 모드 서버다.

### 002. 로더(Loader)

초기 방향은 Fabric 1.21.1.

### 003. Java 버전

Java 21 사용.

### 004. 베이스 모드팩

Cobblemon Official Modpack [Fabric] 1.7.3을 출발점으로 사용.

### 005. 메가 진화

메가 진화는 Cobblemon: Mega Showdown을 통해 포함한다.

### 006. 일반 플레이

일반 플레이는 자유롭고 가볍게 통제한다.

### 007. 통제 시스템

PoroMonCore는 전설 조우, 메가/테라 해금, 짐, 배지, 리그 진행, 시즌 시스템을 통제한다.

### 008. 전설 조우

전설 포켓몬은 공개 랜덤 스폰이 아니라 조우권(encounter ticket)과 사설 제단룸을 통해 접근한다.

### 009. 기존 포켓몬 데이터

초기 단계에서는 기존 포켓몬 타입이나 종족값을 수정하지 않는다.

### 010. 허브(Hub)

짐, 배지, 전설 제단, 메가/테라 해금, 배틀타워, 리그, 챔피언 시스템은 허브에 중앙집중한다.

### 011. PvP

플레이어 간 전투(PvP)는 허브 아레나를 포함해 전역에서 완전 비활성(`pvp=false`)이다. 허브 "아레나"는 **Cobblemon 포켓몬 배틀 전용**이며 — 바닐라 인간 PvP는 절대 켜지 않는다(포켓몬 배틀은 PvP 플래그와 무관). 리그/아레나 포켓몬 배틀 규칙은 PoroMonCore가 별도로 통제한다.

### 012. 월드 보호

스폰(허브) 건물만 보호한다(spawn-protection / 보호 영역). 그 외 월드는 초기 단계에서 claim/그리핑 방지 보호가 없으며, 서버를 가볍게 유지하기 위함이다. 공개 확장 전 재검토한다. `docs/02_server/world_policy.md` 및 `protection_policy.md` 참고.

### 013. 전설 스폰 / 관장 출현 통제

전설 포켓몬의 **자연 랜덤 스폰은 off**(전설은 조우권+사설룸으로만, 결정 008과 연계). Cobblemon 스폰 설정/데이터팩으로 전설 스폰 풀을 제거한다(구현 방식 검증 필요). 관장(gym leader)·트레이너는 **맵에 스폰하지 않으며 허브 전용 PoroMonCore NPC로만** 존재한다. `docs/02_server/world_policy.md`(스폰 통제), `04_game_design/legendary_encounter.md`, `gym_badge_design.md` 참고.

### 014. 단일 화폐(골드)

인게임 화폐는 **골드 하나만** 사용한다. 배틀포인트(BP)·전설 조각·메가 결정 등 **추가 화폐는 0.1 필수가 아니며 추후 확장 후보로만** 남긴다(도입하지 않음). 상품 차등·통제는 화폐 다변화가 아니라 **골드 비용 + 배지/진행 조건(관장 클리어)** 으로 만든다. 유저 간 거래/경매장도 없다(`04_game_design/economy_design.md`, `shop_design.md`). 골드 유통량 자체는 거래가 없어 인플레 위험이 낮으므로, 밸런싱은 "메가진화 등 목표에 적당한 노력" 기준으로 한다.

### 015. 애드온 추가 (SimpleTMs, Eggs)

모드팩에 **SimpleTMs: TMs and TRs for Cobblemon**(dragomordor)과 **Eggs - Cobblemon Addon**(Diesse)을 추가한다(manifest 77 → 79). 둘 다 Cobblemon 게임플레이 애드온으로 **클라/서버 양쪽 필요**, Cobblemon에 의존(그 외 의존성은 jar 검증). `server_mod_separation.md` §1 서버 필수에 반영. 각 모드의 실제 아이템 ID(TM/TR, 알 종류 등)는 **추측 금지** — 미확인은 `TODO: item id 확인 필요`.

### 016. 상점 통제 방식 / 리자몽나이트 분류

상점 고급 상품 통제는 **가격(고액) + 배지/관장 클리어 조건** 중심으로 한다. **주간 구매 제한은 0.1에서 기본적으로 사용하지 않는다.** 강한 제한은 **전설/레쿠쟈/메가 레쿠쟈/시즌 전용**에만 적용(일반 판매 금지·이벤트/후반). **리자몽나이트 X/Y는 별도 카테고리가 아니라 메가스톤 카테고리에 포함**하되, 고급 메가스톤으로 가격·해금 조건을 높게 책정한다. `04_game_design/shop_design.md`, `shop_catalog_0.1.md` 참고.

### 017. 애드온 추가 (Cobblemon: Legendary Monuments)

모드팩에 **Cobblemon: Legendary Monuments**(JorgaoMC)를 추가한다(manifest 79 → 80). Cobblemon 게임플레이 애드온으로 **클라/서버 양쪽 필요**, Cobblemon 의존(그 외 의존성 jar 검증). 전설 관련 구조물/소환 콘텐츠 활용에 사용하되, **전설 획득은 PoroMonCore의 통제 정책(조우권+사설룸+통제된 월드 이벤트, 결정 008/013)을 우선**한다. 이 모드가 제공하는 자연 생성 구조물/소환이 PoroMonCore 통제를 **우회할 수 있는지는 확인 필요(TODO)** — 우회 시 비활성/게이트/통합 방안 결정. `04_game_design/legendary_encounter.md`, `01_modpack/server_mod_separation.md` 참고. 실제 구조물/아이템/포켓몬 ID·config key는 추측 금지(미확인 TODO). **[해소 → 결정 023: 완전 비활성 확정]**

### 018. 전설 조우권 등급 체계 (일반 5등급)

일반 조우권을 **5등급**으로 한다: **희귀 / 하급 전설 / 중급 전설 / 상급 전설 / 최상급 전설**. (이전 "초월/하늘 균열" 명칭 폐기 — 중급 신설, 최상급으로 통합, 하늘 균열은 컨셉별 "하늘 조우권"으로 흡수.) 희귀=비전설 600족/희귀. 풀: `04_game_design/encounter_pool_design.md`.

### 019. 하급+중급 전설 2시간 필드 이벤트

하급·중급 전설은 **조우권(개인방) + 2시간 주기 필드 이벤트** 두 경로를 병행한다. PoroMonCore가 2시간마다 1회, 하급+중급 통합 풀에서 1마리를 통제 스폰(기본 가중 하급 70/중급 30 후보). 좌표 비공개·힌트 공지·제한시간 디스폰·전투 유예·동시 1마리·재시작 직후 차단·최소 인원 조건(후보). **상급/최상급은 필드 이벤트 미포함(개인방 중심).** `legendary_encounter.md`, `config_structure.md`(legendary_events.yml).

### 020. 컨셉별 특수 조우권 10종 / 레쿠쟈·아르세우스 특수 취급

중급/상급/최상급 전설을 섞은 **테마 조우권 10종**을 둔다: 하늘·심해·대지·시간·공간·반전·빛·용왕·수호자·영원. **레쿠쟈는 하늘 조우권 중심**(일반 하급/중급/상급 풀 미포함), **아르세우스는 영원 조우권 중심**(기본 enabled: false, 모드팩 미존재 시 잠금/TODO). 전설 알은 판매하지 않는다. 풀·정책: `encounter_pool_design.md` §8. **모든 species ID는 추측 금지(TODO).**

### 021. 희귀 조우권 보강 (진화 단계 가중)

희귀 조우권 풀을 600족/준전설급 라인 + 실전·인기 비전설 라인으로 보강한다(`encounter_pool_design.md` §1). **전설/환상은 포함하지 않는다.** 진화 단계별 가중치 후보 **기본형 70 / 중간진화 20 / 최종진화체 10** — 기본형을 많이, 최종체를 드물게. 최종체가 과강하면 기본/중간 중심으로 조정. species ID·존재 여부는 확인(TODO).

### 022. 컨셉권 가격대 / 최상급권 전용 / 풀 동기화

- **가격 순서: 희귀 < 하급 < 중급 < 상급 < 컨셉별 특수권 < 최상급.**
- **컨셉별 특수권**은 중급/상급/최상급 **혼합 풀**(가중 후보 중급 55 / 상급 35 / 최상급 10) — "컨셉 안에서 최상급 대박을 노린다".
- **컨셉권에서 나오는 최상급 라인은 최상급 전설 조우권 풀에도 반드시 포함**한다(동기화, 불일치 방지).
- **최상급 전설 조우권은 최상급 풀에서만** 등장(중급/상급 미등장) — "핵심 전설 하나를 뽑는" 최고가 상품.
- `encounter_pool_design.md` §0/§7, `config_structure.md` §6.

### 023. Legendary Monuments 처리 = 완전 비활성 (결정 017 해소)

jar 감사 결과(`01_modpack/jar_feature_audit.md`, `reports/jar_inspection/LegendaryMonuments-7.8.jar.md`) LM의 전설 소환이 PoroMonCore 통제(조우권+사설룸+통제 이벤트, 결정 008/013)를 **확정적으로 우회**한다: 자체 항아리(urn)·피리(flute)·호루라기(whistle)·열쇠(key)·구체(globe)·treat **아이템 우클릭 소환** + 제단(pedestal)·trial spawner·shrine·lock **구조물 소환** + terrablender **바이옴**. 감사 기준 **in-game config 토글 파일 부재**.

→ **방향 (A) 완전 비활성 확정.** LM의 worldgen(구조물·구조물셋·feature·광석)과 관련 **루트테이블을 datapack 오버라이드로 비활성**해 소환 아이템의 **자연 획득 경로를 차단**한다. 아이템 우클릭 소환 로직 자체는 자바 하드코딩이라 제거 불가하나, 소환 아이템을 야생에서 얻지 못하면 사실상 무력화된다. 전설 획득은 PoroMonCore 조우권/사설룸으로 단일화(018~022 유지). LM은 블록/장식 자산만 잔존 허용.

구현 범위(서버 mods 배치 후 실제 jar의 worldgen 경로 확인 필요 = **TODO**):
- `data/legendarymonuments/worldgen/{structure,structure_set,placed_feature,configured_feature}/*` 비활성(빈 오버라이드 또는 structure_set placement 제거).
- `data/legendarymonuments/loot_table/*` 비활성(소환·진행 아이템 드롭 차단). cobblemon_drops 주입분도 점검.
- terrablender 바이옴은 **코드 등록**이라 datapack 제거 난도 높음 → 서버 기동 로그로 생성 여부 확인, 필요 시 바이옴 weight 0/스폰 풀 제거로 게이트.
- 검증: 서버 기동 후 신규 청크에 LM 구조물 미생성 + 소환 아이템 미획득(크리에이티브 외) 확인. (Phase 1 `task.md` §4 "LM 우회 동작 점검"으로 연계.)

**구현 현황 (2026-06-05 — 서버 mods 배치 후 실제 적용·검증):**
- ✅ **구조물 자연 생성 차단 적용** — datapack `poromon_lm_control`(OpenLoader `config/openloader/packs/`)로 LM `worldgen/structure_set/*` **21종을 빈 구조물(`"structures": []`)로 오버라이드**. 추적 소스: `modpack/overrides/config/openloader/packs/poromon_lm_control/`(클라/서버 공통). 런타임 배포: `.local/server/config/openloader/packs/`(비추적).
  - 검증(2026-06-05 부팅): OpenLoader가 pack 자동 로드(`/datapack list` 활성 포함), `/locate structure legendarymonuments:{spear_pillar,southern_island,hall_of_origin}` 전부 **"Could not find ... nearby"** = 자연 생성 차단 확인. LM 본체·Cobblemon/MSD/SimpleTMs/Eggs 정상 유지.
- 🚧 **잔여(미적용, 별도 패스)**: ① `loot_table/*` 비활성(소환·진행 아이템 드롭) ② **소환/제단접근 아이템 제작 레시피 무력화** — 예: `arc_phone`은 바닐라 재료(end_crystal+철+금+포켓덱스 태그)로 제작 가능 → 구조물 차단만으로는 자가 제작 경로 잔존. Fabric 조건부 레시피(`fabric:load_conditions` never-true)로 제거 가능(사용자 선택으로 이번 범위 제외).
- ℹ️ terrablender 바이옴은 코드 등록이라 datapack 제거 난도 높음(현 부팅은 정상). 아이템 우클릭 소환 로직은 자바 하드코딩(제거 불가) — 획득 경로 차단으로 무력화하는 전략 유지.

`legendary_encounter.md`(LM 섹션), `encounter_pool_design.md`(전설 단서/충돌 처리), `01_modpack/server_mod_separation.md` 반영. 실제 경로/ID·config key는 추측 금지(미확인 TODO).

### 024. 상점 구현 방식 = 하이브리드 / 골드 스케일 현 예시 기준선

골드는 PoroMonCore 내부 잔액(`EconomyBridge`)이라 **바닐라 주민 거래(에메랄드 기반) 사용 불가** → 거래 실체는 어느 방식이든 **서버 검증 GUI 트랜잭션**이다. 그 GUI를 여는 방식을 **하이브리드로 확정**한다:

- **어디서나(9번 슬롯 메뉴 = 리그 패스 GUI)**: 단순 매입(광물·농작물·베리 → 골드) + 일반 편의(볼·회복약). 진행도·배지 조회·허브 텔레포트 포함. 자주 쓰는 환금/소모는 접근성 우선.
- **허브 NPC/판매대 우클릭**: 핵심 통제 상점 — 전설 제단(조우권), 메가 연구소(메가팔찌·스톤 해금), 기술머신(TM 타입 게이트), 성장·실전 육성, 알. **배지·관장 클리어 게이트 판정은 허브에서.** 통제·해금 상품은 허브 회귀 유도(설계 철학)와 위치 몰입.
- 거래는 서버 검증 트랜잭션(잔액 확인→차감→지급, 실패 롤백). 가격은 `economy.json` 단일 출처.

**골드 스케일**: `economy_design.md §9` 예시를 **0.1 기준선으로 채택**(메가팔찌 20,000 앵커, 매입가 다이아 40·금 8 등, 야생 처치 레벨×2). 인플레 위험 낮음(거래 없음) → 운영 후 §6 텔레메트리로 튜닝. `economy_design.md §5/§10`, `shop_design.md §5`, `hub_design.md §2`, `CLAUDE.md` 메뉴 설계 반영.

### 025. 클라이언트 모드 티어링 (간편설치기 기반)

서버/클라 분리(`server_mod_separation.md`)는 "서버에 무엇을 넣을지" 관점이었다. **클라이언트 설치를 간편설치기로 돕기 위해**, 클라 85개를 클라이언트 관점에서 **4구분으로 티어링**한다. 분류는 `01_modpack/client_mod_tiers.md`(단일 출처).

- **T0 코어(14, 강제·서버 일치)**: 게임플레이 + 레지스트리 추가 모드와 그 하드 의존(Cobblemon·MSD·SimpleTMs·Eggs·LM + chipped/cobblefurnies/terrablender + accessories/athena/resourcefullib + fabric-api/architectury/owo). 서버 화이트리스트 §1+§1b(14)와 동일 집합. 설치기 토글 불가.
- **T1 권장 편의(기본 ON)**: 성능(Sodium 패밀리·ferritecore·entityculling·lithium·krypton) + 아이템/정보 뷰어(EMI·JEI·appleskin·enchdesc 등) + 조작/인벤 QoL + UI(modmenu·fancymenu). 끄면 성능·편의만 손해.
- **T2 선택 취향(기본 OFF)**: 셰이더(iris)·ETF/EMF·파티클/사운드/분위기(particlerain·visuality·lambdynamiclights·PresenceFootsteps 등)·CraftPresence. 순수 비주얼/취향.
- **L 라이브러리**: 선택 모드 의존성으로 **자동 포함**(cloth-config·YACL·balm·konkrete·melody·bookshelf·UniLib 등).
- **(S) 서버전용(클라 제외 가능, 무해)**: OpenLoader·letmedespawn·netherportalfix·bwncr·Clumps. 클라 게임플레이 미사용.

설치기 모델 = **3토글(T0 강제 / T1 기본 ON / T2 기본 OFF) + 라이브러리 자동 의존 해소**. 최소 설치(저사양)는 T0+성능만으로 정상 플레이. **의존 관계는 각 모드 `fabric.mod.json depends` / CurseForge·Modrinth 메타로 재검증 후 설치기 로직 반영(추측 금지)**. 토글 단위(개별 vs 묶음)·정확 개수는 설치기 스펙 확정 시 재고정(TODO). `01_modpack/client_mod_tiers.md`, `server_mod_separation.md`, `CLAUDE.md` 모드팩 정책 참고.

### 026. 전설/환상 미배치분 풀 배치 (jar 검증 기반)

`jar_registry_reference.md` 추출로 모드 실재 전설 71·환상 23이 확정되자, 조우권 설계(`encounter_pool_design.md`)에 **미배치였던 28종(전설 16 + 환상 12) + 프로즈에만 있던 마샤도**를 배치했다. (조우권에 적힌 전설은 전수 모드 실재 확인 — 누락 0.)

- **신규 박스전설 4종(restricted=apex)**: 코라이돈·미라이돈·테라파고스·버드렉스 → **§5 최상급 + 컨셉**(코라이돈/미라이돈=용왕, 테라파고스=빛, 버드렉스=수호자, 코라이돈 추가로 대지). 컨셉 최상급 라인 = §5 동기화(결정 022 규칙 준수).
- **준전설 그룹**: 러브로스→상급+하늘(영물 사천왕 정합), 오거폰·치고마/우라오스→상급+수호자, 블리자포스/레이스포스→상급(버드렉스 기수, 레이스포스=반전), 로열3(조타구/이야후/기로치)·타입:널/실버디→중급.
- **환상 13종(마샤도 포함)**: 컨셉/이벤트로 분산(공간=테오키스, 시간=게노세크트/마기아나/멜탄/멜메탈, 대지=디안시/볼케니온/자루도, 빛=메로엣타/제라오라, 반전=복숭악동/마샤도, 영원=비크티니). **환상 공통 정책 = 이벤트 게이트(일반 판매 비추천 — 컨셉권/이벤트/리그 보상으로만).**
- **제외**: 코스모움(cosmoem)=코스모그→솔가레오/루나아라 중간 폼이라 별도 조우 타깃 아님(코스모그는 배치됨).

배치는 **등급/풀 위치만 확정**(설계). 가중치 수치·`enabled`는 `legendary_pools.yml` 구현 시 확정. species ID는 `cobblemon:<id>`(레퍼런스 §1)로 매핑. 종족값/타입 미수정.

### 027. Eggs Addon 동작 검증 + 통합·통제 방식

jar 전수 검증(`egg_pool_design.md §8`)으로 Eggs Addon(`diesse`)의 실제 동작이 확정됐다. **기존 문서의 "loot table 오버라이드로 부화 종 변경" 설명은 부정확**했고 정정한다.

- **메커니즘**: 부화 종족 = `data/diesse/function/egg/poke/{common,rare,shiny}.mcfunction`의 `spawnpokemon <cobblemon_id>` 인덱스 매핑. `loot_table/*`은 **난수 범위만**(rolls max) 제공(종 목록 아님). → **풀 커스텀 = mcfunction 오버라이드 + loot rolls.max 일치**, 가중치 = **인덱스 중복**.
- **기본 풀**: common 56(전 세대 스타터+흔한 종)·rare 26(유사전설/강종, 희귀 조우권과 중복)·shiny 81(common+rare 이로치)·rides 소수. 화석/드래곤/타입별은 모드 미제공 → 커스텀 추가.
- **알 아이템**: `minecraft:armor_stand`+컴포넌트(`custom_model_data` 1/2/3, tag `egg.<등급>.placed`, animated_java). 표준 아이템 아님. **지급 = `function diesse:egg/give/<등급>`**.
- **통제(결정 + 적용)**: 모드 자체 **방랑상인**(`egg/villager_spawn`)이 알을 **바닐라 화폐**(금괴/다이아/네더라이트)로 판매 → **PoroMon 골드 단일 경제(014/024) 우회**. ⇒ **방랑상인 비활성 적용·검증(2026-06-05)**: OpenLoader 팩 `poromon_egg_control`로 `diesse:egg/villager_spawn`을 빈 함수 오버라이드(소스 `modpack/overrides/config/openloader/packs/poromon_egg_control/`). 서버 기동 시 `datapack list` 활성 + eggs 모드보다 뒤 로드(우선순위 우위) 확인. 알 판매는 **PoroMonCore 상점이 골드 차감 후 `egg/give/<등급>` 호출**로 단일화. 둥지 자연 스폰(`egg/nest/all` + `predicate spawn_nest`)은 **미통제(별도 결정 필요)** — 현재 야생 둥지는 그대로. (LM 우회 차단(023)과 동일 OpenLoader 패턴.)
- 전설 알 금지·Shiny 일반 판매 비추천 유지(`egg_pool_design.md §7`). 스타팅 알은 common 중복이라 기본 별도 등급 안 둠(필요 시 분리).

`egg_pool_design.md`, `shop_catalog_0.1.md §3.5`, `01_modpack/jar_feature_audit.md §2` 반영.

### 028. 배틀타워 입장 조건 8관장 상향 + 50층 초안 편입·검증

배틀타워 50층 상세 설계 초안(`battle_tower_design.md`, 외부 작성분 편입)을 검토·검증하며 두 가지를 확정한다.

- **입장 조건 = 관장 8명(8배지) 전원 클리어로 상향**(기존 "배지 4개 이상"에서). 50층·Lv100·전설/메가 포함 엔드콘텐츠라 후반 게이트가 맞음. 반영: `league_season_design.md §2/§3`, `gym_badge_design.md §7`, `hub_design.md §6`, `battle_tower_design.md §0`.
- **데이터 검증 통과(2026-06-05, jar 기준)**: 전 50층 파싱 — **종족 119종·기술 172종 매칭 실패 0**, 메가스톤 10·전용 아이템 전수·시그니처기 18종(dragonascent/psystrike/behemothblade·bash/precipiceblades/originpulse/thousandarrows 등) **전부 실재**. → "species/move/item 존재" 검증 완료.
- **남은 검증 = 동작(실배틀 테스트)**: NPC 메가진화 발동 / NPC held item 전투 반영 / AI의 셋업·해저드·상태이상 운용. MSD에 npc·trainer 문자열 없음 = 코드/전투 시점 동작이라 정적 확인 불가. **최대 리스크 = NPC 메가 지원 여부**(20층부터 의존).
- **레벨 설계(명확화 — 충돌 아님)**: 배틀타워 "Lv.100 고정"은 **NPC(상대) 파티 레벨**을 뜻한다. 플레이어는 **실제 레벨 그대로(정규화 없음)** — 엔드콘텐츠라 ~100까지 직접 육성해 도전하는 의도. `league_season §35`("정규화 안 함")와 일치(TBD 해소·확정). **정규리그(§4)만 Lv50 정규화**(레벨 무관 공평 대전) — 의도적으로 대비되는 별개 콘텐츠.

`battle_tower_design.md`(신설), `league_season_design.md`, `gym_badge_design.md`, `hub_design.md` 반영.

### 029. 메뉴 "야생 귀환" → 홈 등록/이동 시스템으로 대체

메인 메뉴 슬롯20의 "야생 귀환(직전 위치 복귀)"을 폐기하고 **홈 등록/이동 시스템**으로 대체한다(인게임 제안·확정).

- **슬롯 5칸**: 기본 1칸 개방(`freeSlots`), 나머지 4칸은 **골드 점진 해금**. 비용(표준 곡선, 메가팔찌 20k 앵커 대비): **2번째 10,000 / 3번째 30,000 / 4번째 70,000 / 5번째 150,000**(합계 260,000). 순차 해금만(이전 슬롯 먼저).
- **등록/이동**: 각 칸에 현재 위치(차원+좌표+시선) 등록(덮어쓰기). 좌클릭=이동, 우클릭=재등록. 차원 간 이동 허용(네더/엔드 홈 가능).
- **악용 방지 = 쿨다운 + 웜업(채널링)**: 이동 요청 시 `warmupSeconds`(기본 3초) 대기 후 이동, 대기 중 **이동(약 0.3블록↑) 또는 피격 시 취소**. 이동 후 `cooldownSeconds`(기본 30초) 재사용 대기.
- **저장**: `PlayerProgress.homesUnlocked`(기본 1) + `homes[5]`(차원/좌표 NBT). **설정**: `core.json §home`(enabled/maxSlots/freeSlots/unlockCosts/warmup/cooldown/cancelOnMove/cancelOnDamage) — 값 하드코딩 금지.
- **구현(2026-06-06)**: `home/HomeManager`(해금/등록/이동 웜업·쿨다운 매 틱 점검) + `home/HomeMenu`(5칸 GUI) + `data/Home`. 메뉴 슬롯20 = 홈, `/poromon home`. 골드는 `EconomyBridge`.

`menu_design.md §3`(슬롯20 = 야생 귀환 → 홈), `config_structure.md §3 core.json`(home 섹션) 반영. 직전 위치 복귀형 "야생 귀환"은 폐기(필요 시 향후 재검토).

### 030. 허브 역할 단순화 — 모든 상호작용 메뉴 통합 (결정 024 대체)

1인 개발 현실(맵에 NPC·판매대·제단을 일일이 배치/관리하기 어려움)을 반영해, **결정 024의 하이브리드(핵심 sink=허브 NPC) 방식을 폐기**하고 **모든 거래·해금·조우를 9번 슬롯 메뉴(리그 패스) GUI로 통합**한다.

- **허브 = 서버 중심 건축물 + 스폰/텔레포트 지점**으로만 사용. 허브 안에 NPC·상점 판매대·전설 제단·메가 연구소 등을 **물리적으로 두지 않는다.**
- **모든 상점·해금·조우권·알·육성·기술머신·전설 제단 = 메뉴 GUI에서 직접 처리**(서버 검증 트랜잭션). 배지/관장 클리어 게이트도 메뉴에서 판정.
- 메뉴 기존 "허브 시설 안내(37–41)" 슬롯(전설 제단·메가 연구소·기술머신·알·육성) = "허브로 가세요" 안내가 아니라 **해당 카테고리 상점 GUI를 직접 여는 입구**로 전환.
- 허브 맵은 schematic 하나(예: `factionsspawn` 302×289)를 붙여 쓰고, 좌표만 `core.json hub.spawn`/`setworldspawn`으로 잡는다. NPC 배치 작업 불필요.
- **영향(문서 갱신):** `shop_design.md §5`·`economy_design.md §5`의 하이브리드 서술 → 메뉴 통합으로 정정. `hub_design.md` 허브 시설(상점/제단/연구소 물리 배치) → 건축물/스폰 역할로 축소. `menu_design.md §3/§4.3` 안내 패널 → 직접 상점 GUI.
- 유지: 골드 단일 화폐(014), 가격/풀 정책, 배지 게이트 수치 — 접근 경로만 메뉴로 일원화(어디서 거래하느냐만 바뀜).

> 결정 024는 [폐기 → DL-030]. 야생 이동(허브 밖 랜덤)·경계는 새 맵 확정 후 별도 진행.

### 031. 전설 제단 = 등급별 선행 해금 + 조우권 저가 반복 + 이로치 5%

조우권 접근 구조를 정교화한다(결정 018~022/030 보완).

- **전설 제단 = 등급(tier)별 1회 해금(선행조건).** 해당 등급 조우권을 사용하려면 그 등급 제단을 **골드(+배지 게이트)로 먼저 해금**해야 한다. 해금은 영구(`PlayerProgress.altarsUnlocked`). tier = 풀 type(rare/basic/intermediate/advanced/theme/apex). **컨셉(theme) 제단 1개 해금 = 컨셉 10종 전부 사용.**
- **조우권 = 반복 사용·저가.** 메인 골드 sink는 제단 해금(큰 금액), 조우권 사용은 저렴. 기본값(economy.json): 해금 희귀5k/하급30k·배지4/중급50k·6/컨셉70k·8/상급80k·8/최상급120k·8, 사용 500/1500/2500/3500/4000/6000.
- **이로치(샤이니) = 조우권 사용 시 확률 출현.** 별도 이로치 조우권/확정권 대신 **사용마다 `shinyChancePercent`(기본 5%)** 로 샤이니 전설 출현. (IB-001 보유변환 확정권은 보류/대체.)
- **모델**: Cobblemon 1.7.3 미구현 전설은 모드팩 `complete-cobblemon-collection`(+EMF, 클라)로 모델 보충. 풀 enabled = (Cobblemon implemented ∪ 컬렉션 커버) 게이트.
- 구현: `encounter/{ArenaManager,EncounterService,AltarMenu}` + `EconomyConfig.altarUnlock/ticketUse/shinyChancePercent` + `PlayerProgress.altarsUnlocked`. 메뉴 슬롯37(가상 제단).

`encounter_pool_design.md`(가격/게이트는 economy.json로 이관), `menu_design.md`(슬롯37) 반영. 조우권 커스텀 텍스처(등급별 paper+CMD 82001~)는 추후.

### 032. Eggs Addon(알 시스템) 제거 — 결정 027 폐기

알 시스템(Eggs - Cobblemon Addon, `mr_eggs_cobblemonaddon`)을 **모드팩에서 제거**한다. 결정 015(추가)·027(통합/통제)는 **폐기**.

- **이유**: ① 조우권/제단으로 희귀·전설 획득 루트가 충분(알의 뽑기 역할과 중복) ② 알 모델은 별도 **리소스팩**(Animated Java export) 필요한데 mod jar에 assets 0개라 미설치 시 **맨 아머 스탠드**로 표시됨(리소스팩 의존 부담) ③ 야생 둥지 자연 스폰 통제 등 부가 관리 비용.
- **제거 범위**: `eggs-cobblemon-addon-0.9.jar`(서버/모드팩/클라 인스턴스) + OpenLoader `poromon_egg_control` 팩(방랑상인·둥지 비활성 오버라이드, 이제 불필요) + 메뉴 슬롯40(알 상점 placeholder) 제거.
- **잔재**: 기존 월드 level.dat의 datapack 참조로 `Missing data pack mr_eggs_cobblemonaddon` WARN(무해, 바닐라 무시). 신규 월드/정리 시 사라짐.
- **폐기 문서**: `egg_pool_design.md`(전체 폐기), `shop_design.md §3.5`(알 상점 폐기), `shop_catalog_0.1.md §3.5`, `server_mod_separation.md`(모드 수 -1). 향후 알 재도입 시 리소스팩 확보 선행.

`poromon/CLAUDE.md` 모드 목록에서 Eggs 제거.

### 033. 마개조 기술머신 — learnset 해제 + 타입분류/검색 TM 상점

기술머신을 "마개조"(자유 기술 부여)로 확장.

- **learnset 해제**: SimpleTMs `anyMovesLearnableTMs=true`(+TRs). TM을 **사서 보관** → 원하는 포켓몬에 사용 = **그 포켓몬만** 그 기술 학습(다른 포켓몬 무관). 전역 토글이지만 적용은 사용한 포켓몬 단위. (포켓몬별 게이트 없음 = 구매 골드가 게이트.)
- **TM 상점 재구성**(`shop/TmShopMenu`+`TmCatalog`): SimpleTMs 전체 TM(632)을 **18타입 카테고리 + 검색**(채팅 입력)으로 판매. TM 목록은 레지스트리에서 자동 수집(`simpletms:tm_*`), 타입/위력/표시명은 Cobblemon `Moves.getByName` 런타임 조회. 메뉴 슬롯39.
- **가격 = 위력 자동 등급**(`economy.json tmShop`): 변화기1k/≤60 1.5k/≤90 2.5k/≤110 4k/111+ 6k. 632줄 수작업 불필요. (배지 게이트 minBadges=0 기본.)
- **텍스처**: SimpleTMs TM은 타입색 디스크 모델/텍스처를 jar에 보유 → 정상 렌더(Eggs와 달리 추가 리소스팩 불필요).
- 결정 032에서 선별 40종(고정 map)이었던 것을 전체+타입/검색으로 대체.

`anyMovesLearnableTMs`는 `.local/server/config/simpletms/main.json` + `modpack/overrides/config/simpletms/main.json`(배포). CLAUDE.md/shop_design TM 항목 갱신.

### 033-a. 마개조 = 해금석 게이트로 변경 (033 보완)

전역 무료 해제(anyMovesLearnableTMs=true) 대신 **포켓몬별 골드 게이트**로 확정.
- `anyMovesLearnableTMs=false`(되돌림) → 일반 SimpleTMs TM은 정규 learnset만 (밸런스 보존).
- **마개조 해금석**(`item/MakeoverStone`, paper+태그, 신규 등록 불필요): TM 상점에서 골드 구매(기본 10k·배지4) → **포켓몬에 우클릭** → 기술 선택(타입/검색, teach 모드) → 그 포켓몬에 off-learnset 기술 1개 각인(`Pokemon.getMoveSet().setMove`, MoveSet 가득 시 교체 슬롯 선택) → 해금석 소모.
- learnset 무시는 SimpleTMs가 아닌 **PoroMonCore가 MoveSet 직접 조작**으로 처리(전역 토글 무관). `PoroMonCore` UseEntityCallback로 우클릭 감지.
- TM 상점: 일반 TM 구매(learnset용) + 해금석 구매(마개조용) 공존.

### 033-b. 기술머신 / 포로공학 분리 + 워딩

기술머신(정규)과 마개조(off-learnset)를 별도 상점으로 분리. "마개조" → **"포로공학"** 으로 사용자 대면 워딩 변경.
- **기술머신 상점**(메뉴39): 선별 40종 TM 아이템 판매(`economy.json tmShop`, CategoryShopMenu). 정규 learnset 기술용(SimpleTMs, anyMovesLearnableTMs=false).
- **포로공학**(메뉴42, `shop/EngineeringMenu`): 해금석 구매 + off-learnset 기술 각인. 해금석 우클릭→포켓몬 영구 해제, 메뉴에서 해제된 포켓몬에 기술(타입/검색) 각인(각인마다 골드, `economy.json engineering`). 전체 632 기술 대상.
- 해금석 표시명 "포로공학 해금석"(태그 키·내부 식별자 makeover/MakeoverService는 유지).

### 034. 포로공학 정수 2종 분리(기술머신/특성) + 특성 완전 마개조

포로공학 정수를 트랙별로 분리하고, 특성 마개조(임의 특성 강제 부여)를 신설. 특성 변경은 종족값 변형급 사기성이라 기술과 **별도 정수·별도 해제 트랙**으로 격리.
- **정수·기술머신**(기존 "포로공학 정수" 리네임, CMD 82030, 태그 `poromon_makeover_stone`): 기술 각인 트랙. lore "포로공학의 정수이다 / 모든 기술머신을 배울 수 있게 된다". 가격 30만·배지4.
- **정수·특성**(신규, CMD 82031, 태그 `poromon_ability_stone`): 특성 마개조 트랙. 우클릭→포켓몬 영구 해제(`PlayerProgress.abilityMakeoverPokemon`) → 메뉴서 해제 포켓몬 선택 → 특성 목록/검색 → **`Pokemon.setAbility$common(new Ability(tpl, forced=true, NORMAL))`** 로 종족 무관 임의 특성 강제 부여(`Abilities.get`). 가격 50만·배지6, 변경 1회 5만(`economy.json engineering.abilityStonePrice/abilityStoneBadges/abilityChangePrice`).
- 특성 목록·한글검색: `shop/AbilityCatalog`(Cobblemon `ko_kr.json cobblemon.ability.<name>` 로드 → `Abilities.get` 유효분만, 표시는 `Text.translatable`로 ko_kr 자동). 무제한 변경.
- 포로공학 메뉴(42): 기술(정수 구매 29 / 각인 33) + 특성(정수 구매 38 / 변경 42) 2트랙.
- 특성 정수 텍스처는 임시(기술 정수 색조 변형) — 전용 원본 추후 교체(CMD 82031).

### 035. 조우 풀 2단계 가중(stage/tier) + B 고정 cap + 심해/빛/용왕 보강

조우권 풀의 가중을 설계 의도대로 코드/데이터에 반영(이전엔 후보 weight 직접 추첨 = stage/tier 미적용).
- **2단계 가중**: `P(후보) = (stage 비중 / Σstage 비중) × (후보 weight / Σ동일 stage weight)`. 단일 진실 공급원 = `EncounterConfig.probabilities()` (추첨 `EncounterService` + 표시 `PoolInfoMenu` 양쪽이 사용 → 표시=실제 일치).
- **stage 비중**: 희귀 풀 = 기본형70/중간20/최종10(evo stage). 컨셉 10풀 = 중급(intermediate)55/상급(advanced)35/최상급(apex)10.
- **B 고정 cap**: 후보 없는 stage 비중은 "비중이 가장 큰 존재 stage"가 흡수 → **최상급 항상 ≤10%**(희소성 일관). 예: 반전(중급 0) = 상급90/최상10. (희귀는 전 stage 상시 존재라 재분배 없음.)
- **tier 분류 권위** = 일반 등급 풀 멤버십: 최상급=apex풀 · 상급=advanced풀 · 중급=intermediate/하급전설풀 · 환상(미분류)=상급.
- **풀 내 weight**: 중급·상급 균등 10. 최상급 = **시그니처 apex★ 20 / 부수(타 컨셉 교차등재) 5** → 도메인 대표 부각 + 중복 차별화. ★=레쿠쟈(하늘)·가이오가(심해)·그란돈(대지)·디아루가(시간)·펄기아(공간)·기라티나(반전)·테라파고스(빛)·레시라무/제크로무/큐레무/코라이돈/미라이돈(용왕)·자시안/자마젠타/버드렉스(수호자)·아르세우스(영원).
- **후보 보강**(단독 독식 해소): 심해 +4(케르디오·무쇠보따리·굽이치는물결·볼케니온), 빛 +4(디안시·비크티니·코스모그·마기아나), 용왕 +5(라티아스·라티오스·날뛰는우레·꿰뚫는화염·포효의달 — 레지드래고 90%→18%). 신규 종은 미배치 패러독스 활용. 용왕은 최상급 多 의도 유지(B cap=최상급 10% 합).
- ✅ **필드이벤트 70/30(2026-06-07)**: `field_event_legendary_pool` stageWeight {low:70, mid:30} + 후보 stage(하급12/중급16, basic/intermediate 풀 멤버십 기준). 결정 019 반영.
- ✅ **apex 운영 토글(2026-06-07)**: 컨셉 최상급 가중 ×2 이벤트. `PoroMonState.apexBoost`(영속) + `EventManager.apexMultiplier()` + 운영자 GUI(AdminMenu 슬롯40) + `EncounterConfig.probabilities(pool, apexMult)`. 추첨·제단 확률표시 양쪽 반영(레쿠쟈 10%→18.2%). 일반 apex 티켓 풀은 stageWeight 없어 무영향.
- **within-tier 세부 weight = 균등 유지(2026-06-07 결정)**: 같은 tier 내 후보는 균등 가중(중급·상급 10). 환상 희소화 안은 검토 후 **보류**(예측 가능성·단순성 우선). 필요 시 재검토.
- 연관 기완료(별개 세션): 조우권 텍스처 15종(§4j, 일반5+컨셉10), 야생 처치/포획 골드 보상(§4i `WildRewardService`). → 밸런스 패스로 추가 작업 없음.
- 설계 표: `04_game_design/encounter_balance_proposal_v1.md`. 구현: `EncounterConfig`·`EncounterService`·`PoolInfoMenu`·`EventManager`·`PoroMonState`·`AdminMenu` + `legendary_pools.json`.

### 036. 정규리그 코어 구현 (league_season §4)

정규리그(점수제 래더 + 실시간 큐 매칭 + lvl50 정규화)를 구현. 챔피언스리그/시즌 운영은 후속(Phase 6).
- **§7 레벨50 정규화 블로커 해소(옵션 A 채택)**: Cobblemon `BattleFormat.adjustLevel` 확인 → GEN_9_SINGLES 복제 후 adjustLevel=50. `BattleBuilder.pvp1v1`로 플레이어 간 대전(pvn 패턴과 동일).
- **점수제**: 시작 1000, 승 +10 / 패 −7, 하한 0. `PlayerProgress.rankedInit/rankedScore/rankedWins/rankedLosses`(NBT). 첫 큐 참가 시 startScore로 초기화.
- **실시간 큐 매칭(AI 없음)**: `LeagueManager` 큐 + 1초 틱 매칭. 윈도우 ±50 시작, 초당 +2.5 확대, 상한 ±400. 동일 상대 재대전 쿨다운 600초(파밍 방지). 매칭 성사 → pvp1v1(lvl50).
- **승리 처리**: `BATTLE_VICTORY` 구독 — 양측 모두 PlayerBattleActor이고 ACTIVE 등록쌍일 때만 리그로 인정(pvn/관장/타워/친선 오인 없음). 점수 반영 + 쿨다운 기록.
- **UI/명령**: 메뉴 슬롯13(리그/챔피언) → `LeagueMenu`(큐 참가·취소·내 전적·순위표 Top5·챔피언스리그 안내). `/poromon league [queue|leave]`. 자격 = 배지 8.
- **config**: `seasons.json`(`SeasonConfig.rankedLeague`) — 하드코딩 금지. 접속종료 = 큐/진행 정리(노카운트).
- **0.1 미구현(TBD)**: 짜고지기 방지, 무효배틀 세부 처리(현재 노카운트), 룰셋 기믹 게이트(메가만 허용=테라/다이맥스/Z off 강제), 챔피언스리그 토너먼트, 시즌 리셋.
- 구현: `LeagueManager`·`LeagueMenu`·`SeasonConfig`·`PlayerProgress`(ranked)·`PoroMonState.all()` + 코어/명령/메뉴 배선. 검증: 빌드 + 헤드리스(seasons.json 생성·로드, 에러0). **인게임 미검증(2인 필요)**: 매칭·lvl50 정규화·점수.

#### 036-a. 정규리그 동적 아레나 (방/이동 로직 추가)
매칭 성사 시 제자리 배틀이 아니라 **동적 아레나로 격리**(조우 `ArenaManager` 재사용). 매칭 → 셀 할당·방 생성 → 양쪽 원위치(차원+좌표) 저장 → 11×11 방 양 끝(z+2/z+8, x중앙)에 **마주보게 텔레포트** → pvp1v1. 종료(`onVictory`)·강제해제(`forceEnd`, releaseAll 연동)·접속종료(`onDisconnect`) 시 **양쪽 원위치 복귀 + 방 철거·셀 반납**. 대전 중 끊긴 플레이어는 `PENDING`에 원위치 저장 → **재접속 시 복귀**(허공 로그인 방지). 동시 다중 매치는 셀이 격리. 빌드+헤드리스 검증(에러0). 인게임 미검증(2인): 텔레포트·마주봄·복귀.

### 037. 서버 전역 메가-온리 기믹 게이트 (룰셋)

기믹 정책을 **서버 전역 메가 전용**으로 확정. 테라스탈·(거)다이맥스·Z무브·울트라버스트 **off**. 리그뿐 아니라 야생·관장 등 전 배틀 공통(MSD 기믹 토글이 전역 static이라 per-battle 분리 불가 + 설계 정책이 본래 "메가만 허용").
- 구현 = **MSD config**(`config/mega_showdown/config.json`, 코드 불필요): `mega:true` / `teralization:false` / `dynamax:false` / `zMoves:false` / `outSideUltraBurst:false` / `teraShardDropRate:0` / `stellarShardDropRate:0`. `outSideMega:true`(메가는 필드에서도 허용) 유지.
- 소스: `modpack/overrides/config/mega_showdown/config.json`(추적). 런타임: `.local/server/config/`(비추적, 동기화).
- 검증: 헤드리스 부팅 — MSD가 값 로드·유지(되쓰기 없음), 에러0.
- 정합: 메가 연구소 상점은 메가 장비만 판매(테라오브/다이맥스밴드/Z링 미판매)와 일치. 정규/챔피언스 리그 룰셋(league §11) 자동 충족.
- 잔여: 이미 보유한 테라오브 등 아이템은 기믹 off로 무효(아이템 회수/정리는 운영 판단).

### 038. 전설 필드 이벤트 구현 + 자연스폰 차단 검증 (결정 019/013)

**① 하급·중급 필드 이벤트 구현** (`encounter/FieldEventManager`):
- 주기(기본 2시간)마다 `field_event_legendary_pool`(하급70/중급30)에서 1마리를 **월드보더 안 야생 지표에 통제 스폰** → **채팅 공지(종명 + 좌표 공개) → 30분 후 디스폰**. 동시 1마리. 포획/처치/제거 시 종료 후 다음 주기 대기.
- **결정 019 갱신**: 좌표 **비공개+힌트** → **좌표 공개**(사용자 결정 2026-06-07). 제한시간 30분 확정.
- 위치 = WildManager 패턴 재사용(경계 안 랜덤 + 비동기 청크 로드 + 안전 지표 검사). 자연 디스폰은 **no-op Despawner**(`setDespawner`)로 차단(30분은 매니저가 관리, Cobblemon 기본 3분 디스폰 무력화).
- config = `core.json fieldEvent`(enabled/intervalMinutes120/durationMinutes30/poolId/level60/edgeMargin/minSurfaceY/maxAttempts). 운영자 `/poromon admin fieldevent`로 즉시 발생(테스트).
- 검증: 헤드리스 — 아그놈(중급) Lv60 출현·좌표 공지·중복 거부·에러0. ⚠️ 알파: 30분 디스폰·영속(3분 초과 생존)·포획 종료.

**② Cobblemon 기본 전설 자연스폰 차단(결정 013) = 기본값 충족 확인**:
- jar 검증: Cobblemon 1.7.3 `spawn_pool_world`에 전설 71·환상 23·UB 11·패러독스 20 **전부 스폰 데이터 0종** → 바닐라가 전설을 자연 스폰하지 않음. LM은 결정 023으로 비활성. ⇒ **차단 datapack 불필요**(결정 013 자동 충족). 전설 입수 = 조우권(개인 아레나) + 필드 이벤트(①)로 단일화.

#### 038-a. 운영자 토글: 전설 출현 주기 ×2 단축
운영자 GUI(AdminMenu 슬롯34)에 **전설 필드 이벤트 주기 절반** 토글 추가(xp/gold/apex 부스트와 동일 패턴). `PoroMonState.fieldEventFast`(영속) + `EventManager.isFieldEventFast/toggleFieldEventFast`. `FieldEventManager`는 기준점(idleSinceTick) + 동적 interval로 리팩터 → 토글 즉시 반영(현재 주기: 기본 120분, ON 시 60분). 헤드리스 검증(core.json fieldEvent 생성, 에러0).

### 039. 네더 / 엔드 차원 정책

**네더:**
- 월드보더 **지름 5000**(오버월드와 동일, ÷8 비적용 — ÷8은 너무 좁음).
- **고정 네더 허브**: 모든 오버월드→네더 포탈이 **단일 고정 허브 좌표**로 도착(포탈 위치 무관). 허브 = 요새 인접 네더랙 지형(블레이즈 막대·네더라이트 확보 용이). → 상세 설계 = IB-005.
- **플레이어별 오버월드 진입 좌표 기억** → 네더→오버월드 복귀 시 그 자리로. (vanilla 1:8 포탈 매핑 대체.)
- **블레이즈 스포너 파괴 불가**(허브 블레이즈 팜 영속).
- 전설 필드 이벤트(결정 038)는 **오버월드 한정**(네더/엔드 미적용) 유지.

**엔드:**
- **엔더 드래곤전 없음** + **엔드시티(바깥섬) 탐험 가능**. 입장은 허용하되 드래곤 보스전은 제거/무력화, 바깥섬(엘리트라·셸커·엔드 전용 포켓몬) 콘텐츠는 접근 가능.
  - 구현 후보(TBD): 드래곤을 사전 처치 상태로 프리셋(출구 게이트웨이 생성 → 바깥섬 도달) / 드래곤 스폰 datapack 제거 + 게이트웨이 대체. 기술 검증 필요.
- 엔드 월드보더: TBD(오버월드 준용 또는 별도).

**LM 커스텀 차원(`legendarymonuments:hall_of_origin_world`·`distortion_world`):**
- **접근 차단 확정**(결정 023 정합). LM 입장 아이템(arc_phone 등) 자연 획득 차단으로 사실상 봉쇄 → 잔여 획득 경로(제작 레시피) 점검·차단 검증 필요(TODO).

> 차원 경계는 차원별 독립(MC) → PoroMonCore 시작 시 차원별 보더 설정(config 구동) 또는 운영 명령. 구현 = IB-005/별도 패스.

#### 039-a. 네더 정책 구현 (1·2·4단계)
- ✅ **네더 월드보더**(`NetherManager.applyBorder`, SERVER_STARTED): 중심(0,0) 지름 5000. core.json `nether` 구동.
- ✅ **포탈 리다이렉트**(`ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD`): 오버월드→네더 = 고정 허브 도착(좌표 미안전 시 안전 지표 자동 탐색) + 진입 좌표를 `PlayerProgress.netherReturn`에 저장 / 네더→오버월드 = 그 좌표로 복귀. 오버월드 위치는 매 틱 추적.
- ✅ **허브 보호**(`PlayerBlockBreakEvents.BEFORE`): 네더 허브 중심 반경(기본10=21×21) 내 블록 파괴 차단(포탈·블레이즈 스포너). op(권한2) 우회 가능.
- ⚠️ **미완(3단계 월드빌드)**: 고정 허브 좌표(core.json hubX/Y/Z)는 임시(0.5/64/0.5) — **요새 인접 네더랙 허브 + 블레이즈 스포너 물리 건설 후 실좌표 설정** 필요. 그 전엔 리다이렉트가 (0,0) 근처 안전지표로 보냄.
- 검증: 빌드 + 헤드리스(보더 적용 로그·nether config 생성·에러0). ⚠️ 알파(포탈 통과 필요): 허브 도착·복귀 좌표·블록파괴 차단.

#### 039-b. 네더 허브 물리 건설 (3단계)
- ✅ **`/poromon admin netherhub`**(`NetherHubBuilder`): 운영자가 네더에서 실행 → 현재 위치에 **21×21 블랙스톤 플랫폼(난간 포함) + 귀환 네더 포탈(흑요석 프레임+포탈블록) + 블레이즈 스포너** 건설 → 허브 좌표를 core.json(`nether.hubX/Y/Z/Yaw`)에 저장(`ConfigManager.saveCore`) → 도착지로 TP.
- 자연 요새 의존 없이 블레이즈 스포너 직접 배치(막대 확보) + 주변 네더 지형(네더라이트). 포탈·스포너는 허브 보호 반경(기본10=21×21) 안 → 파괴 불가.
- 귀환: 허브 포탈 진입 → 네더→오버월드 → 저장된 진입 좌표 복귀(039-a).
- 검증: 빌드 + 헤드리스(명령 등록·콘솔 비플레이어 graceful·에러0). ⚠️ 알파(네더 플레이어 실행): 플랫폼/포탈/스포너 생성·도착·귀환·보호.

#### 039-c. 네더 허브 자동 위치 + 보호 강화
- ✅ **자동 위치**(`buildHubAuto`): `/poromon admin netherhub` = 보더 중심(0,0) 근처 **안전한 공동 바닥 자동 탐색**(8칸 공기+주변 무용암) 후 건설 → **운영자가 위치 선정 불필요**(콘솔/어디서나 실행). `/poromon admin netherhub here` = 플레이어 위치 수동.
- ✅ **허브 안전 강화**: 빌더가 21×21 플랫폼 + **둘레 벽(블랙스톤)+천장**(용암/몹 유입 차단) + **-Z 3×3 출입구**(네더 탐험 진출) + 귀환 포탈 + 블레이즈 스포너. 난간→벽으로 대체.
- 검증: 헤드리스 콘솔 `/poromon admin netherhub` → (0,64,0) 자동 건설·좌표 저장·에러0.
