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
