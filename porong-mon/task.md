# PoroMon 작업 기록 / 다음 세션 핸드오프

> 최종 업데이트: 2026-06-07 (폴더 rename 머지 누락분 복구 — 아래 ▶ 참조)
> 이 파일은 세션 간 작업 연속성을 위한 핸드오프 노트다. 다음 세션은 이 파일부터 읽고 이어서 진행한다.
>
> **▶ 2026-06-07 rename 머지 복구(9b26ec8):** cc8c42d 머지가 feature 전용 작업물을 옛 `poromon/`에 고아로 남겼던 것을 발견·복구. `custom-mods/poromon-core`(모드 소스 114) + modpack config 27(`poromon_lm_control`·`poromon_battletower_test`·`simpletms/main.json`)을 `porong-mon/`으로 git mv(순수 rename). 이제 단일 트리 통합, sparse-checkout(`/porong-mon/`)로 정상 노출. ⚠️ **런타임 미복구**: `modpack/client/mods/*.jar`(빌드 의존, gitignore)·`.local/server`(서버 런타임)는 rename 후 사라짐 → 빌드/기동 전 재구성 필요(Windows `PoroMon 0.1 Dev/mods` 86개에서 복원 가능).
> 권위 규칙: `CLAUDE.md` · 문서 인덱스: `docs/README.md` · 결정 기록: `docs/00_project/decisions.md`(001~028)
>
> **2026-06-05 모노레포 구조 정리:** poromon 자체 폴더/파일은 **변경 없음**. 다만 레포 레벨에서 RPG 자산과 프로젝트 전역 docs가 `porong-rpg/`로 통합되고 루트 `docs/`가 폐지됨. 포로몬은 계속 `porong-mon/`(자체 `docs/`·`server/`·`custom-mods/`) 안에서 독립적으로 작업한다.

---

## 0. 현재 단계
**Phase 0(설계/문서) 완료 + Phase 1(서버 기동) 통과 + Phase 2 진행(배틀타워 핵심 + 메뉴/GUI 0.1 완성).** PoroMonCore 0.1: 빌드·로드·`/poromon` 명령 + **진행도 영속화(실증)** + **배틀타워 실배틀**(pvn 명시파티)·**AI 공격**·**속도 8배**·**NPC 메가진화**·아이템드롭 차단 **인게임 작동 확인**(→ **§4b**) + **메뉴 아이템/GUI**(리그패스 지급·복원·드롭차단·우클릭 메뉴·허브TP·config 시스템) **헤드리스 검증 완료, 클라 클릭검증 대기**(→ **§4d**). + **상점(매입/편의)·골드(EconomyBridge)·홈 시스템(결정029)** 인게임 검증 완료(→ **§4e**). 다음 과업(허브/알/조우권) **§4c**.
- 모드팩: PoroMon 0.1 Dev / MC 1.21.1 / **Fabric Loader 0.19.3** / Java 21.
- 실제 jar **85개**를 `modpack/client/mods/`에 복사 완료 + jar 내부 감사 완료.
- **서버 1차 기동 성공**(2026-06-05): `.local/server`(표준, 비추적)에서 화이트리스트 **25개** 로드, `Done` 출력, 크래시·실제 ERROR 없음. 2026-06-05 재기동 재확인(1.3s, 정상 stop).
  - 무해 로그만: cobblemon `No data fixer`, LM 리소스팩 특수문자 경로 무시, Lithium 믹스인 자동 비활성, 서버에 없는 클라 클래스 프로빙 WARN. **실제 에러 아님.**
  - ⚠️ 옛 `.local/poromon-server/`(워크트리 루트)는 stale 빈 골격 — 표준은 **`poromon/.local/server`**.

## 1. 지금까지 완료한 것
- **문서 체계 구축**: `docs/` 거의 전부 한글화 + 인덱스(`README.md`).
- **게임 설계 확정**(decisions 011~024): PvP off / 허브만 보호 / 골드 단일 화폐 / 경매장 없음 / 야생만 골드보상(레벨비례) / 상점 7구역 / 짐 8개(순차강제·초회보상·레벨캡 100) / 배틀타워 50층(진행저장) / 정규리그(점수제·lv50·실시간매칭만·8배지) / 챔피언스리그(서버 마지막날 토너먼트) / 메가만 허용(테라·다이맥스·Z off) / **LM 완전 비활성(023)** / **상점=하이브리드(매입·편의=9번 메뉴, 핵심=허브 NPC)·골드 스케일 §9 기준선(024)**.
- **조우권 체계**: 일반 5등급(희귀/하급/중급/상급/최상급) + 컨셉 특수 10종(하늘~영원). 하급·중급=조우권+2시간 필드이벤트, 상급·최상급=개인방. 레쿠쟈=하늘, 아르세우스=영원.
- **애드온 추가 확정**(decisions 015/017): SimpleTMs, Eggs Addon, Legendary Monuments.
- **서버/클라 분리**(실제 jar 기준): 서버 화이트리스트 **25개**(§1 9 + §1b LM의존 5 + §2 11) / 애매 4 / 클라 제외 56. `scripts/sync-server-mods.sh`로 `.local/server/mods` 동기화 완료. **서버에 클라 전용(Sodium/Iris/EMI/JEI/Xaero/FancyMenu/ETF/EMF/EntityCulling) 0개 검증.**
- **클라 편의성 티어 분류**(간편설치기용): `docs/01_modpack/client_mod_tiers.md` 신설 — 85개를 T0 코어(14, 강제) / T1 권장 편의(성능·뷰어·QoL·UI) / T2 선택 취향(셰이더·파티클·사운드) / L 라이브러리(자동 의존) / (S) 서버전용(클라 제외 가능)으로 분리. 설치기 3토글 모델.
- **LM 비활성 datapack 적용·검증**(결정 023): `poromon_lm_control`(OpenLoader)로 `worldgen/structure_set/*` 21종 빈 구조물 오버라이드 → `/locate` "Could not find" 확인.
- **jar 내부 감사 완료**: `docs/01_modpack/jar_feature_audit.md` — 실제 아이템 ID·한글화·충돌 확인.

## 2. jar 감사 핵심 결과 (확정 사실)
- **Mega Showdown**: `mega_showdown:keystone`(채굴 가능), `mega_bracelet`, `charizardite_x/y`, `<name>ite` 메가스톤 다수, `tera_orb`/`tera_shard`, `z_ring`/`ium_z`, dynamax_tab. **ko_kr 기본 포함.**
- **SimpleTMs**: `simpletms:tm_<move>`(1000+), `tm_blank/tr_blank`, `case_tm/case_tr/machine_tm`. TM/TR 월드 비활성 토글. **ko_kr 기본 포함.**
- **Eggs Addon**: 데이터팩형(namespace `diesse`). 풀=loot table common/rare/shiny/rides → **datapack 오버라이드로 커스텀 가능**. 상인 없음(둥지 스폰형). 알 아이템 ID 불명확.
- **Cobblemon**: ko_kr 기본 포함.
- **Legendary Monuments**: ko_kr **없음**(신규 번역 1순위). 자체 소환 시스템 방대(제단/항아리/피리/trial spawner/바이옴).

## 3. BLOCKER 상태 (2026-06-05 — 전부 해소)
1. **[해소 ✅ 결정 023] Legendary Monuments = (A) 완전 비활성.**
   - 구조물 자연 생성 차단 **적용·검증 완료**: `poromon_lm_control` datapack(OpenLoader)로 `worldgen/structure_set/*` 21종 빈 오버라이드 → `/locate structure legendarymonuments:*` "Could not find".
   - **남은 잔여(별도 패스, 이번 범위 아님)**: ① `loot_table/*` 드롭 차단 ② 소환/제단접근 **아이템 제작 레시피 무력화**(예 `arc_phone` 바닐라 재료 자가제작 경로). Fabric 조건부 레시피로 제거 가능. terrablender 바이옴은 코드 등록(현 부팅 정상).
2. **[해소 ✅] chipped / cobblefurnies / terrablender** (LM 하드 의존): JIJ 아님 확인 → **CurseForge 프로필에 3종 + lib(athena·resourcefullib) 추가**, 서버 기동 로그 `Missing dependency` 경고 없음. LM 최종 포함 25개 정상 로드.
3. **[해소 ✅] 개수**: client/mods = **85개**(80 + LM 의존 5종 추가)로 확정. server_mod_separation.md 갱신됨.

## 4. 다음 액션
**Phase 1 완료분 (✅)**
- [x] §3-1 LM 완전 비활성(결정 023) + 구조물 차단 datapack 적용·`/locate` 검증
- [x] §3-2 chipped/cobblefurnies/terrablender = CurseForge 프로필 추가로 해소, 부팅 로그 경고 없음
- [x] `.local/server` 기동 파일: `eula.txt`, `server.properties`(pvp=false), `start.sh`(Java21, -Xms2G -Xmx6G)
- [x] `sync-server-mods.sh`로 서버 mods 25개 복사 + 클라 전용 0개 검증
- [x] 서버 1차 기동 → Done, Cobblemon/MSD/SimpleTMs/Eggs/LM 로드, 실제 ERROR 없음. 재기동 재확인
- [x] 클라 편의성 티어 분류(`client_mod_tiers.md`) — 간편설치기 기반

**Phase 1 잔여 / 다음**
- [ ] LM 소환 통제 잔여(별도 패스): `loot_table/*` 차단 + `arc_phone` 등 소환아이템 제작 레시피 무력화(Fabric 조건부 레시피)
- [ ] 실제 클라 접속 테스트(클라 인스턴스로 서버 접속 — 모드 불일치 거부 없음, 스폰/배틀/TPS)
- [ ] 간편설치기 스펙 확정: T1/T2 토글 단위·의존성 자동해소(CurseForge/Modrinth 메타 재검증)

## 4b. Phase 2 — PoroMonCore 구현 (2026-06-06 세션, ✅ 배틀타워 핵심 완성)
**위치**: `custom-mods/poromon-core/` (Fabric 1.21.1 / Java21 / 베이스 `kr.poro.poromoncore`). 빌드: `./gradlew build`(캐시 후 ~8초). 배포: `build/libs/poromon-core-0.1.0.jar` → `.local/server/mods/`. **서버 포트 25566**(RPG=25565).
**의존**(compileOnly, 런타임은 서버 모드 제공): Cobblemon `com.cobblemon:fabric:1.7.3+1.21.1`(impactdev maven) + fabric-language-kotlin `1.13.6+kotlin.2.2.20` + **MSD 로컬 jar**(`modpack/client/mods/mega_showdown-*.jar`).

- ✅ **스캐폴드**: `PoroMonCore`(ModInitializer)·`PlayerProgress`·`PoroMonState`(PersistentState 월드부속 NBT)·`PoroMonCommand`. BUILD SUCCESSFUL·로드 확인.
- ✅ **진행도 영속화 실증**: `/poromon admin tower set <p> <floor>` → save-all → **재시작 → 디스크에서 복원 확인**(`world/data/poromoncore_progress.dat`). database_schema §1 PersistentState.
- ✅ **배틀타워 실배틀**(`BattleTowerService`): 층 파티를 `PokemonProperties`로 코드 빌드 → NPC 생성(플레이어 3칸 앞·마주보게=NaN 회피) → **`BattleBuilder.INSTANCE.pvn(player, npc, …, party)` 명시 파티 주입**(getPartyForChallenge 우회). `/poromon admin tower start <p> <floor>`. **근본원인**: /summon NPC는 배틀 파티 미초기화→pvn NoPartyError(battle_tower §4-N).
- ✅ **AI**: NPC `skill=1`(spawn 후). StrongBattleAI `checkSwitchOutSkill` 임계 skill5=1.0(무한 스위칭!)→skill1≈0 → 공격 위주(skill 5가 오히려 멍청한 역설).
- ✅ **배틀 속도 8배**: `WaitDispatchMixin`(mixin) — `WaitDispatch(float)` 대기를 SCALE 0.12. 생성자 HEAD 주입이라 **static 핸들러** 필수. (교체 recall/send-out **모션**은 클라 애니=서버 불가.)
- ✅ **NPC 메가진화**: tick(20틱)마다 NPC 활성 포켓몬이 메가스톤 보유 시 **`MegaGimmick.megaEvolveInBattle(p, bp)` 강제 발동**(MSD가 NPC 자동메가 미지원). `MEGA_DONE` 추적으로 무한 메가 애니 루프 방지. 레쿠쟈 제외 메가스톤 47종 범용. (NPC는 모션 없이 즉시 메가 = 의도대로 OK. 플레이어 메가는 MSD 기본 연출 정상.)
- ✅ **아이템 드롭 차단**: tick에서 양측 활성 포켓몬 `setCanDropHeldItem$common(false)`.

**커밋**: e5c6ee2(스캐폴드)·662da3f→ad5b306(검증ID)·8f9d8ab(pvn 배틀)·b82c6d2(속도/AI)·3405356(메가).

## 4c. 다음 세션 과업 (우선순위)
1. ~~**메뉴 아이템/바**(9번 슬롯 리그패스 + 우클릭 GUI)~~ → **✅ 0.1 구현 완료(2026-06-06), §4d 참조.**
2. **허브 구성** — `hub_design.md`. 허브 텔레포트(`/poromon hub`) + 구역(짐/제단/메가연구소/마켓/배틀타워 입구). ⚠️ TP 골격은 §4d에서 완성(core.json hub.spawn) — 남은 건 **실제 허브 빌드 + 구역 좌표 확정 후 core.json 값 채우기** + HubInteractionManager(NPC 상호작용).
3. **상점 구조** — `shop_design.md`·`shop_catalog_0.1.md`(검증 ID 보유). 하이브리드(9번메뉴 매입/편의 + 허브 NPC 통제). EconomyBridge 골드.
4. ~~**알 구조**~~ → ⛔ **폐기(결정 032, 2026-06-07)**: Eggs Addon/알 시스템 제거(조우권 중복 + 리소스팩 의존). 모드 jar·OpenLoader egg 팩·메뉴 슬롯40 제거. egg_pool_design/shop §3.5 폐기.
5. **조우권 생성·구조** — `encounter_pool_design.md`·`legendary_pools.draft.yml`(검증 ID). EncounterTicket 커스텀아이템 + InstanceRoom + pvn으로 전설 소환(배틀타워와 동일 패턴 재사용 가능). ★ **이로치 조우권/확정권(idea_inbox IB-001)도 이 작업과 함께 구현 결정**(`Pokemon.setShiny`·`shiny=true` 검증됨).
> 배틀타워 잔여(저우선): 메가 연출(클라모드 필요), 다른 메가 47종 검증, 보상 지급(§3-R) 실제 연동(RewardManager), 진행도 ↔ 배틀 승리 연동(현재 set 명령만).

## 4d. Phase 2 — 메뉴 아이템/GUI 0.1 (2026-06-06 세션, ✅ 구현 완료)
**위치**: `custom-mods/poromon-core/`. 빌드: `./gradlew build`. 배포: `build/libs/poromon-core-0.1.0.jar` → `.local/server/mods/`. 설계: `docs/03_poromoncore/menu_design.md §7`.

- ✅ **config 시스템 착수**: `config/CoreConfig`(POJO) + `config/ConfigManager`(Gson). `config/poromoncore/core.json` 자동 생성·로드·`/poromon admin reload` 실동작. **부팅 시 core.json 기본값 생성 검증**(menuItem/hub/logging, config_structure.md §3 스키마 일치).
- ✅ **리그 패스 아이템**(`item/MenuItemManager`): `minecraft:clock` 베이스 + `poromon_league_pass` CUSTOM_DATA 태그로 식별(위조/복제 방지) + 한글명/lore. `ensure()` = 중복 1개로 회수(안티-듀프) + 없으면 핫바 9번칸(또는 빈칸) 지급.
- ✅ **지급/복원/보호 훅**(`PoroMonCore`): JOIN(최초=giveOnFirstJoin / 재접속=restoreOnJoin) · AFTER_RESPAWN(restoreOnRespawn) · `mixin/ServerPlayerEntityMixin`로 `dropSelectedItem` Q-드롭 차단(preventDrop). `leaguePassGiven` 진행도 연동.
- ✅ **메인 메뉴 GUI**(`menu/MenuGuiManager` + `menu/LeaguePassMenuHandler`): 6×9 셰스트형(GENERIC_9X6). **서버 권위 읽기전용** — `onSlotClick` super 미호출 + `syncState()`로 모든 아이템 이동/추출 차단(듀프 방지), 디스플레이 클릭만 액션 라우팅. menu_design.md §3 슬롯 배치. **0.1 활성**: 19=허브TP · 10=진행도 채팅 · 49=닫기 · 4=플레이어 정보(읽기). 나머지는 "준비 중" placeholder.
- ✅ **허브 TP**(`hub/HubManager`): core.json `hub.spawn`(월드/좌표/yaw·pitch)로 `player.teleport`. teleportCommandEnabled 게이트.
- ✅ **명령**: `/poromon menu`(GUI) · `/poromon hub`(TP) · `/poromon admin pass <player>`(지급/복원) · `/poromon admin reload`(config 실리로드).
- ✅ **헤드리스 부팅 검증**: Done(1.38s) · `[PoroMonCore] 0.1 초기화` · core.json 생성 · `poromon`/`admin reload` 콘솔 동작 · mixin 적용 오류 없음 · poromoncore ERROR/WARN 0 · 정상 stop.
- ⚠️ **미검증(클라 접속 필요)**: 우클릭 메뉴 오픈/슬롯 클릭/허브 TP 실동작/드롭 차단/접속·리스폰 지급. 다음 클라 테스트 시 확인.
- ⚠️ **잔여**: core.json hub.spawn 기본값=(0.5,64,0.5) 임시 → 허브 빌드 후 실좌표. 인벤 화면 밖 버리기·죽음 드롭(현재 복원으로 보완). lockSlot 미구현(0.3). 전용 패스 모델 CustomModelData(menu_design §8-1).

## 4e. Phase 2 — 상점(매입/편의) + 홈 시스템 (2026-06-06 세션, ✅ 구현·인게임 검증)
**위치**: `custom-mods/poromon-core/`. 설계: `menu_design.md`·`economy_design.md`·`shop_catalog_0.1.md`·decisions **029**. **인게임 실테스트 통과**(zenonsufu0, 포트 25566).

- ✅ **EconomyBridge + economy.json**: 골드 잔액 API(입금/출금/설정, 출처 태그 로깅) — `economy/EconomyBridge`. `config/EconomyConfig`(sellPrices/buyPrices 단일 출처, 실 item id 검증: 광물·농작물 바닐라 + `<color>_apricorn` + 볼12·회복약11). `ConfigManager` 다중 파일(core+economy)로 일반화. `/poromon admin economy give|set|balance`.
- ✅ **매입소(SellShopMenu)**: 인벤 판매가능 품목 자동 진열 → 클릭=그 종류 전부 판매 / 전부 팔기. **편의 상점(BuyShopMenu)**: 볼·회복약 좌클릭1/우클릭8 구매(잔액부족·인벤가득 안전·환불). `shop/ShopLayout` 공용 레이아웃.
- ✅ **범용 메뉴 핸들러(ServerMenuHandler)**: LeaguePass 핸들러 일반화(읽기전용·안티듀프). **`show()` = 재오픈 없이 내용/콜백 교체 → 메뉴 전환 커서 점프 제거**(사용자 피드백 반영). 클릭 콜백에 shift 전달. `menu/MenuIcons` 공용 아이콘.
- ✅ **홈 시스템(결정 029)**: `data/Home`(이름+차원+좌표) + `PlayerProgress.homesUnlocked/homes[5]`(NBT). `home/HomeManager`(해금 10k/30k/70k/150k·등록·이동: **쿨다운30s + 웜업3s 채널링**, 이동/피격 시 취소, **매 초 카운트다운 액션바**) + `home/HomeMenu`(5칸 GUI: 좌클릭 이동/우클릭 재등록/**쉬프트 이름변경**). 이름변경=`util/ChatInputManager`(다음 채팅 1줄 가로채기, `ServerMessageEvents.ALLOW_CHAT_MESSAGE`). 메뉴 슬롯20 "야생 귀환"→**홈**, `/poromon home`.
- ✅ **리그 패스 보호 완성**: Q-드롭 차단(`ServerPlayerEntityMixin.dropSelectedItem`)이 서버는 막지만 클라 예측 잔상 → **취소 후 `syncState()` 재동기화로 해결**. 인벤 화면 이동/드롭/숫자키스왑 차단(`ScreenHandlerMixin.onSlotClick`) + 매 틱 슬롯 고정(`MenuItemManager.enforce`). lockSlot 기본 true.
- ✅ **인게임 검증(zenonsufu0)**: 패스 우클릭 메뉴·드롭/이동 잠금·구매/판매/전부팔기·잔액 실시간 갱신·홈 등록/이동/해금/이름변경/카운트다운·커서 점프 제거 전부 정상.
- ⚠️ **잔여**: 허브/홈 좌표 운영값(허브 빌드 후). 이로치 조우권/확정권 = §4c 5번과 함께(idea_inbox IB-001). 상점 가격은 economy.json 운영 튜닝(§6 지표).

## 4f. Phase 2 — 메뉴 조회 + 허브 단순화 결정 (2026-06-06 세션)
- ✅ **메뉴 조회 구현**: 배지(11)=컬렉션 뷰(4×2, 타입색/회색유리) · 관장 가이드(12)=정보 보드(4×2, 순서·타입·레벨캡·획득·순차게이트) · 리그/챔피언(13)·서버 가이드(14)=채팅 안내. "짐"→"관장" 표기. `gym/{GymInfo,BadgeMenu,GymBoardMenu}`, `PlayerProgress.badges`(NBT), `/poromon admin badge give|clear`. 인게임 검증.
- ✅ **결정 030(허브 단순화)**: 허브=건축물+스폰/TP만, 모든 거래·해금·조우=메뉴 GUI 통합(결정 024 하이브리드 폐기). 메뉴 37–41=상점 GUI 직접 입구(구현 추후). 문서 정정: decisions 030 + menu/hub/shop/economy 배너.
- ✅ **허브 맵 교체**: `factionsspawn`(302×289×85, 구형 MCEdit Alpha 포맷) 채택. 허브 좌표=현 위치(`core.json hub.spawn` -15.5/95/755.5 + setworldspawn). (이전 aetherfall 1001²는 과대로 폐기.)
- ✅ **경계/야생/TPA 구현**: 월드보더(중심 -15.5/755.5, 지름 5000) + **야생 랜덤이동**(메뉴 슬롯21, 웜업3s+쿨다운30s, **비동기 청크 로드**로 멈춤 없이 안전 착지) + **TPA**(슬롯22, 닉 입력→대상에게 클릭 [수락][거절]→요청자 이동). `wild/WildManager`·`tpa/TpaManager`, `/poromon wild|tpa accept|deny`, core.json §wild. 인게임 검증.
- ✅ **관장 실배틀**(`gym/GymBattleService`): 관장 보드(12)에서 도전 클릭 → 관장 NPC pvn(배틀타워 패턴) → **승리 감지(Cobblemon BATTLE_VICTORY 구독)** → 최초 승리 시 배지+골드(order×500) 자동 지급, 재도전 무보상. 순차 강제. 인게임 검증.
  - ✅ **관장 파티 강화**: 8관장 = 초반 4~5/후반 6마리, 실전 기술4·특성·성격, **에이스 메가스톤→관장 메가진화**(tick 발동, X/Y 스톤 인식 수정). 1번 관장 인게임 확인. 후반은 알파테스트 예정.
- ✅ **pvn 파티 버그 수정(중요)**: `BattleBuilder.pvn(...,party)`의 마지막 `PartyStore`는 **플레이어** 파티(바이트코드 확인). NPC 파티를 거기 넘겨서 플레이어가 상대 팀으로 싸우던 버그 → **플레이어 본인 파티(`Cobblemon.storage.getParty`)** 전달로 수정. **관장+배틀타워 양쪽** 적용.
- ✅ **배지 커스텀 텍스처 기반(모드 내장 B안)**: 결정 — 리소스팩/http 불필요, 모드 jar `assets/`에 내장. `paper`+CustomModelData(81001~81008) 모델 분기(`paper.json` override + `badge_*.json` 8). `MenuIcons.iconModel`. ⚠️ **루트 .gitignore `assets/` 광역무시 → 모드 .gitignore 예외 추가**(추적 복구). 텍스처 8장(`assets/poromon/textures/item/badge_*.png`) **적용 완료**: 사용자 원본(1254² RGB 흰배경) → **64×64 리사이즈 + 모서리 흰배경 flood-fill 투명화** + Zone.Identifier 찌꺼기 제거 → BadgeMenu `iconModel(paper, 81000+order)` 전환. **서버+클라(PoroMon 0.1 Dev) 양쪽 배포·인게임 확인**. 원본은 `custom-mods/poromon-core/badge_originals_1254/`(gitignore). ⚠️ **커스텀 모델은 클라에도 PoroMonCore jar 필요** — 빌드 변경 시 서버+클라 동시 배포.

## 4g. Phase 2 — sink 상점(성장·메가) (2026-06-06 세션)
- ✅ **공용 `shop/CategoryShopMenu`**: 가격+배지게이트(minBadges)+**페이지네이션**(28/페이지, ◀▶) 구매 상점. 좌1/우8·환불·in-place 갱신. (제목·가격맵·태그)만 바꿔 재사용. `EconomyConfig.ShopEntry{price,minBadges}`.
- ✅ **성장 상점**(메뉴 41): 이상한사탕·경험사탕XS~XL·행복알(배지1)·진화돌10·비타민6 = 23품목. `economy.json §growthShop`.
- ✅ **메가 연구소**(메뉴 38): 메가팔찌(20k/배지4)+메가스톤47(기본43=8k/배지4, 고급 X/Y 4=25k/배지6) = 48품목 2페이지. `economy.json §megaShop`(jar에서 실 *ite ID 47 추출).
- 둘 다 서버측 GUI(바닐라 cobblemon/MSD 아이템) → 서버만 배포. 인게임 확인.
- ✅ **기술머신 / 포로공학 분리**(결정033/033-a/033-b):
  - **기술머신 상점**(메뉴39): 선별 40종 TM 판매(`economy.json tmShop`, CategoryShopMenu). 정규 learnset(SimpleTMs anyMovesLearnableTMs=false).
  - **포로공학**(메뉴42, `shop/EngineeringMenu`+`TmCatalog`+`MakeoverService`): 해금석(`MakeoverStone`, paper+태그, 10k·배지4) 구매→포켓몬 우클릭(UseEntityCallback) 영구 해제(`PlayerProgress.makeoverPokemon`)→메뉴서 해제 포켓몬 선택→기술(18타입/검색, Cobblemon Moves 런타임)→`MoveSet.setMove` off-learnset 각인(각인마다 골드 위력등급 1k~6k, 가득 시 슬롯교체). 무제한 각인.
- ✅ **실전 육성 상점**(메뉴40, 알 슬롯 폐기 자리, `trainingShop`): 성격 민트 21 + 특성캡슐(배지4)/패치(배지6). CategoryShopMenu 재사용.
- ⚠️ **잔여**: 모드 아이템명 영어(클라 ko_kr 일부만) → 향후 `assets/<ns>/lang/ko_kr.json` 오버라이드 한글화 패스. 실전육성(민트/특성캡슐)·TM(SimpleTMs ID TODO)·알(Eggs ID TODO)·전설 제단(조우권)·메가팔찌 보유 게이트는 미구현.

## 4h. Phase 2 — 조우권+이로치 (2026-06-06~ 진행 중, 큰 작업)
**결정(2026-06-06)**: 개인 조우방=**동적 아레나 자동생성** / 등급=**전부(5등급+컨셉10=16풀)** / 이로치=**조우권(shiny 옵션)+확정권(보유변환 IB-001)**. 조우권=메뉴 즉시구매·사용(결정030). 소환=야생 스폰(포획).
- ✅ **1단계 풀 데이터 파이프라인**: `legendary_pools.draft.yml`(16풀 285후보) → `legendary_pools.json` 변환(camelCase, jar 번들 `resources/poromoncore/`). `EncounterConfig` POJO + `ConfigManager.loadOrCreateResource`(config 없으면 jar 번들 복사→로드). 부팅 검증: pools 16 로드·config 생성·reload OK.
- ✅ **2단계 동적 아레나**(`encounter/ArenaManager`): 오버월드 공중(y250) grid 셀 배정→방(바닥 잔디+배리어벽/천장) 생성→TP, 종료 시 철거+셀 반납. **유저별 다른 방 격리**. 인게임 검증(방 생성·복귀).
- ✅ **3단계 소환**(`encounter/EncounterService`): 풀 가중추첨→아레나 격리→야생 전설 스폰(shiny 옵션, 포획 가능)→3분/포획/도주 시 정리·복귀. 등급별 레벨. `/poromon admin encounter <pool> [shiny]`(테스트). 인게임 검증(소환·포획·복귀·이로치).
  - ⚠️ **모델 이슈 해결**: Cobblemon 1.7.3은 다수 전설 모델 미구현(`implemented:false`→substitute 인형). **모드팩에 `complete-cobblemon-collection`(+EMF) 추가**(클라)로 모델 보충 → 설계 278종 전수 커버 확인. 풀 enabled=원설계 복원(서버는 렌더 무관, 클라만 컬렉션 필요). `legendary_pools.json` = (Cobblemon implemented ∪ 컬렉션 커버) 게이트로 생성(현재 전수 통과).
- ✅ **4단계 전설 제단(`encounter/AltarMenu`, 슬롯37) + 결정031 재설계**: **등급(tier) 제단 1회 해금(선행)** → 그 등급 조우권 반복 사용(저가). `PlayerProgress.altarsUnlocked`. economy.json `altarUnlock`(해금 5k~120k·배지게이트)·`ticketUse`(사용 500~6000)·`shinyChancePercent`(이로치 5%). 컨셉 제단1=컨셉10. 소환 실패 환불. 아이콘 임시 바닐라(텍스처 추후 paper+CMD 82001~).
- ✅ **이로치 = 조우권 사용 시 5% 확률 샤이니**(결정031, 별도 확정권 대체). (IB-001 보유변환 확정권은 보류.)
- ✅ **조우 확률 표시**(`encounter/PoolInfoMenu`): 제단 풀 **우클릭 → 후보 한글명+출현 확률%+가중치**(페이지네이션). 좌클릭=해금/사용. 확률=weight/Σweight(0.1 stage_weight 미적용).
- ✅ **밸런스 패스 1차(2026-06-07, 결정 035)**: 2단계 가중(stage/tier)+B 고정 cap(최상급≤10%) 코드/데이터 반영. 희귀 70/20/10·컨셉 55/35/10. 최상급 시그니처★20/부수5. 심해·빛·용왕 후보 보강(레지드래고 90%→18%). 단일 진실=`EncounterConfig.probabilities()`(추첨·표시 일치). 빌드+헤드리스 검증(pools 16, 에러0). 표=`encounter_balance_proposal_v1.md`.
- ✅ **밸런스 패스 B(2026-06-07)**: 필드이벤트 70/30(low/mid stage) + **apex 운영 토글**(컨셉 최상급×2 이벤트, `PoroMonState.apexBoost`+AdminMenu 슬롯40, 추첨·제단표시 반영). 빌드+헤드리스 재검증.
- ✅ **밸런스 패스 C(2026-06-07)**: within-tier 세부 weight = **균등 유지 결정**(환상 희소화 안 보류, 예측가능성 우선). 코드/데이터 변경 없음. → 밸런스 패스 A·B·C 완료. (조우권 텍스처 §4j·야생 골드 보상 §4i는 **이미 완료** — 밸런스 패스 추가 작업 없음.)

## 4n. Phase 2 — 정규리그 코어 (2026-06-07 세션, 결정 036)
- ✅ **정규리그 구현**(league §4): 점수제 래더(시작1000·승+10/패−7·하한0) + **실시간 큐 매칭**(윈도우 ±50→초당+2.5→±400, 재대전 쿨다운600s) + **lvl50 정규화**(§7 해소: `BattleFormat.adjustLevel=50` + `BattleBuilder.pvp1v1`). 자격 배지8.
  - 모듈: `league/{LeagueManager,LeagueMenu}` · `config/SeasonConfig`+`seasons.json` · `PlayerProgress`(rankedInit/Score/Wins/Losses NBT) · `PoroMonState.all()` · 메뉴 슬롯13→LeagueMenu(큐/전적/순위표Top5/챔피언 안내) · `/poromon league [queue|leave]` · 접속종료=노카운트 정리.
  - 승리감지 `BATTLE_VICTORY`: 양측 PlayerBattleActor+대전쌍일 때만 리그 인정(pvn/관장/타워 오인 없음).
  - ✅ **동적 아레나(결정 036-a)**: 매칭→`ArenaManager` 방 생성→양쪽 원위치 저장→11×11 방 양끝 마주보게 텔레포트→pvp1v1. 종료/강제해제/접속종료 시 원위치 복귀+철거. 끊긴 자는 PENDING→재접속 복귀. (조우방과 동일 격리 패턴.)
  - 빌드+헤드리스 검증(seasons.json 생성·로드 season v1, 에러0). ⚠️ **인게임 미검증(2인 필요)**: 매칭·lvl50 정규화·점수 반영.
  - **잔여(TBD)**: 짜고지기 방지, 무효배틀 세부, 챔피언스리그 토너먼트(IB-004), 시즌 리셋.
  - ✅ **기믹 게이트(결정 037)**: 서버 전역 메가-온리 — MSD `config/mega_showdown/config.json`(teralization/dynamax/zMoves/outSideUltraBurst=false, 테라샤드 드롭0). 리그 룰셋 자동 충족. 헤드리스 검증(되쓰기 없음, 에러0).
- **남은 잔여(리그 외) = 인게임 클라 검증뿐.**

## 4i. Phase 2 — 운영자 GUI + 야생 보상 (2026-06-07 세션)
- ✅ **운영자 GUI**(`/poromon admin gui`, `admin/{AdminMenu,PlayerAdminMenu,PlayerActionMenu}`): 이벤트 부스트(경험치×2·골드×2 토글, `event/EventManager`+PoroMonState 영속)·갇힘 강제해제(전체/개별)·플레이어 관리(골드/배지/패스/진행)·공지(채팅→방송). 훅: 경험치 EXPERIENCE_GAINED_EVENT_PRE, 매입가 ×배수.
- ✅ **야생 골드 보상**(`economy/WildRewardService`, economy §3): 야생만(BATTLE_VICTORY loser=WILD 처치 레벨합×2 / POKEMON_CAPTURED 포획 레벨×1), 골드 부스트 반영. `economy.json pokemonRewards`. → 골드 부스트 완성.
- ✅ **운영자 GUI 잔여**: 조우 강제소환(`admin/EncounterAdminMenu`, 풀 선택→본인 무료 소환, 우클릭 이로치) + 경제 모니터(`admin/EconomyMonitorMenu`, 출처 그룹별 골드 유입/유출 누적 — PoroMonState goldIn/goldOut, EconomyBridge 훅, economy §6 텔레메트리).
- ⏳ **잔여**: 컨셉 최상급 확률×2(apex 플래깅=밸런스 패스).

## 4j. Phase 2 — 상점/포로공학/텍스처/한글화계획 (2026-06-07 세션 마감)
- ✅ **기술머신 상점**(메뉴39): 선별 40종 TM(CategoryShopMenu, `economy.tmShop`) — 정규 learnset.
- ✅ **포로공학**(메뉴42, 결정033/a/b, `shop/EngineeringMenu`+`TmCatalog`+`MakeoverService`): 정수 구매(30만·배지4)→포켓몬 우클릭 영구 해제(`PlayerProgress.makeoverPokemon`)→메뉴서 해제 포켓몬 선택→18타입/검색→off-learnset 각인(각인마다 골드 위력등급 1k~6k, 가득 시 슬롯교체). 일반 TM은 정규 learnset(전역 OFF).
- ✅ **알 시스템 제거**(결정032): Eggs Addon jar·OpenLoader egg팩·메뉴 슬롯40(알→실전육성으로 재사용). 사유=조우권 중복+리소스팩 의존. (재도입 시 Eggs 리소스팩 선행, IB.)
- ✅ **조우권/정수 텍스처 배선**: 티켓15(`ticket/ticket_<key>`, CMD 82001~82020) + 포로공학 정수(`engineering_essence`, CMD 82030). 64×64 리사이즈·정수 투명화. paper.json override + 모델16. AltarMenu 해금 아이콘=티켓텍스처, MakeoverStone=정수텍스처. 배지 텍스처 badge/ 하위 이동 대응. **서버+클라 배포**(클라 재시작 시 렌더). 원본=texture_originals/(gitignore).
- ✅ **한글화 계획 수립**(`localization_policy.md §8`, 구현 전): 클라 ko_kr+모드 ko_kr 내장→대부분 자동. 핵심 잔여=GUI `.getString()` 영어 박제 **9곳**(CategoryShop3·BuyShop2·SellShop2·Engineering1·TmCatalog1)→`MenuIcons` Text 오버로드+translatable 전환. 누락분만 자체 ko_kr override(출처 Weblate/Bulbapedia/사용자제공).

## 4k. Phase 2 — 한글화 구현(GUI translatable 전환) (2026-06-07 세션, ✅ 구현·헤드리스 검증)
**근거**: `localization_policy.md §8.4`. 서버 GUI가 모드 아이템/기술/종족명을 `getName().getString()`(서버 locale=영어)로 **문자열 박제** → 클라가 ko_kr여도 우리 메뉴만 영어. translatable `Text`를 그대로 넘기면 클라(ko_kr)가 자동 한글 렌더 → **추가 번역 데이터·클라 jar 불필요**.
- ✅ **`MenuIcons` Text 오버로드 추가**: `icon(Item, Text, lore)` + `iconCount(Item, int, Text, lore)` + `named(Formatting, Text)`(빈 베이스에 색 부여→번역키 보존하며 자식 색 상속). 기존 String 오버로드는 Text 위임으로 통합.
- ✅ **9곳 + 추가 박제 전환**: CategoryShop(잠김/판매 아이콘+구매채팅)·BuyShop(아이콘+채팅)·SellShop(아이콘+판매채팅)=`item.getName()` translatable. EngineeringMenu(교체기술 아이콘=`MoveTemplate.getDisplayName()`, 포켓몬 선택/각인완료=`Species.getTranslatedName()`, 각인 displayText 스레딩). PoroMonCore 포로공학 해제 채팅=`getTranslatedName()`.
- ✅ **`TmCatalog.Entry`에 `displayText`(Text) 추가**: 정렬·검색은 기존 `displayName`(String) 유지, GUI 렌더만 translatable. EngineeringMenu 기술목록 아이콘+각인 메시지에 사용.
- ✅ **잔여 박제 = 서버 로그 2곳뿐**(GymBattleService/BattleTowerService `LOGGER.info` NPC 메가 — 유저 비노출, 영어 유지 OK).
- ✅ **빌드 BUILD SUCCESSFUL + 헤드리스 부팅 검증**: jar 서버 배포(.local/server/mods) → Done(1.342s)·`[PoroMonCore] 0.1 초기화`·poromoncore ERROR 0·정상 stop. (무해 WARN: 클라 클래스 프로빙·LM 리소스팩 경로·cobblemon README — 기존 동일.)
- ⚠️ **미검증(클라 접속 필요)**: 실제 ko_kr 렌더 캡처(상점/포로공학 메뉴에서 영어 잔존 0 확인). **클라 jar 재배포 불필요**(translatable은 네트워크 전송→클라 lang 렌더).
- ⚠️ **잔여**: 인게임 캡처로 모드 ko_kr 누락분 발견 시 §8.3 `PoroMon-Korean-Pack` ko_kr override(현재 미작성).

## 4l. Phase 2 — 인게임 1차 피드백 반영 (2026-06-07 세션, 클라 검증분)
사용자 클라 접속(PoroMon 0.1 Dev) 1차 검증 결과 반영:
- ✅ **티켓 텍스처 렌더 확인**(82001~82020). 배지도 OK.
- ✅ **기술머신 상점(메뉴39) 이름 한글화**: SimpleTMs `tm_<move>` 아이템은 **개별 번역키 없음**(영어/raw 키로 박제) → `CategoryShopMenu.shopName(item,itemId)` 헬퍼 추가: `simpletms:tm_` prefix면 Cobblemon `Moves.getByName(move).getDisplayName()`(translatable, ko_kr 자동)로 대체. 아이콘(정상/잠김)+구매채팅 적용. 포로공학(EngineeringMenu)과 표기 통일.
- ✅ **포로공학 검색 한글 부분일치 지원**: 서버 locale=영어라 `getDisplayName().getString()`이 영어 → 검색 불가였음. `TmCatalog.loadKoMoveNames()`로 **Cobblemon `assets/cobblemon/lang/ko_kr.json` 직접 로드**(`cobblemon.move.<move>` 키, `.desc`/`.category.*` 제외) → `Entry.koName` 추가 → `search()`가 영문키·영문명·**한글명** 모두 부분일치. 로드 실패 graceful(빈 맵 + WARN).
- ✅ **마스터볼 편의 상점 추가**: 바닐라 제작 가능 → `cobblemon:master_ball` **5000골드** (`EconomyConfig.defaultBuyPrices` luxury_ball 뒤 + 런타임 `economy.json buyPrices`). BuyShopMenu(편의)에 자동 진열.
- ✅ **빌드 + 서버/클라 양쪽 jar 재배포**(해시 일치) + 서버 재기동(Done 1.389s). **포로공학 정수(82030) 종이 렌더 = 클라 jar 구버전 원인 → 최신 jar 클라 배포로 해결, 클라 재시작 후 렌더 확인 필요.**
- ⚠️ **재검증 필요(클라 재시작)**: ① 포로공학 정수 텍스처 렌더 ② 기술머신 상점 기술명 한글 ③ 포로공학 한글 검색 동작 ④ 마스터볼 편의상점 노출/구매.

## 4m. Phase 2 — 정수 텍스처/메뉴아이콘 + 특성 마개조 신설 (2026-06-07 세션)
- ✅ **정수 메뉴 아이콘 버그 수정**: 포로공학 메뉴 정수 슬롯이 `icon()`(CMD 미부여)을 써서 종이로 보였음(티켓 AltarMenu는 `iconModel()` 사용). `iconModel(PAPER, 82030, …)`로 수정 → 메뉴 아이콘 렌더 정상.
- ✅ **정수 텍스처 배경 투명화 재처리**: 흰 배경 잔여(가장자리 alpha 255/78)로 노이즈 → 원본(texture_originals/, 1254²)에서 **가장자리 flood-fill 투명화 + premultiplied LANCZOS 리사이즈**(흰 fringe 제거) → 64² 재생성. 코너/가장자리 alpha 0 확인.
- ✅ **특성 마개조 신설(결정 034)**: 포로공학 정수 2종 분리.
  - `MakeoverStone`: `Kind{TECH(82030, poromon_makeover_stone), ABILITY(82031, poromon_ability_stone)}` enum화. 기존 "포로공학 정수"→**"포로공학 정수 · 기술머신"**(lore 변경), 신규 **"포로공학 정수 · 특성"**.
  - `PlayerProgress.abilityMakeoverPokemon`(NBT) + `MakeoverService.unlockAbility/isAbilityMakeover`. `PoroMonCore` 우클릭 = `kindOf`로 기술/특성 분기 해제.
  - `shop/AbilityCatalog`(신규): Cobblemon `ko_kr.json cobblemon.ability.<name>` 로드 → `Abilities.get` 유효분만, 표시=`Text.translatable`(ko_kr 자동), 한글/영문 검색.
  - `EngineeringMenu`: 특성 트랙 추가(정수 구매38 / 변경42 → 포켓몬 선택 → 목록·검색·페이지 → **`Pokemon.setAbility$common(new Ability(tpl, forced=true, Priority.NORMAL))`** 임의 강제 부여). 무제한 변경.
  - 가격: 특성 정수 50만·배지6, 변경 1회 5만(`EconomyConfig.engineering.abilityStonePrice/abilityStoneBadges/abilityChangePrice` + 런타임 economy.json).
  - **정수 전용 텍스처 적용**(사용자 원본): 기술머신=`engineering_tm_core`(시안 회로형, 82030) / 특성=`engineering_ability_core`(보라·핑크 DNA형, 82031). 가장자리 flood-fill 투명화+premult 리사이즈. 기존 `engineering_essence`는 **보관**(미참조, 나중 재사용). 임시 hue변형본 제거.
  - **포로공학 메뉴 슬롯 한 행 상향**: 기술(정수20/각인24=행2) · 특성(정수29/변경33=행3).
- ✅ **텍스처 원본 폴더 통일**: 배지 원본을 `badge_originals_1254/`(별도) → `texture_originals/item/badge/`로 이동. 이제 원본은 `texture_originals/item/{badge,ticket,poro_engineering}/` 한곳. (모드 사용 텍스처는 `src/.../textures/item/{badge/,...}` 그대로, git 추적.) Windows Zone.Identifier 찌꺼기 정리.
- ✅ **이번 세션 커밋**: `d850b01` "feat(poromon): 상점/포로공학 한글화 + 특성 마개조 신설 + 마스터볼" (21파일 +543/-67). working tree clean. **origin 대비 56커밋 ahead — push 미실행.**
- ⚠️ **클라 검증 미완(다음 세션 1순위)**: ① 정수(기술=시안/특성=DNA) 텍스처 렌더 ② 특성 정수 우클릭 해제 ③ 특성 변경 메뉴(목록/검색/부여) 동작·한글 ④ **forced 특성 실제 전투 반영** ⑤ 마스터볼 편의상점 노출/구매 ⑥ 기술머신 한글명·한글검색.
- ⚠️ **잔여**: AbilityCatalog는 ko_kr에 있는 특성만 노출(레지스트리 전수 아님 — 표시 한글 우선). 특성 변경 메뉴에 "현재 특성" 표시 미구현(lore 빈 줄).

---

## ◎ 세션 마감 (2026-06-07) — 폴더명 변경 예정
**여기까지 커밋 완료(d850b01), 서버 정상 정지.** 사용자가 **워크트리 폴더명을 변경할 예정** → 다음 세션 재진입 시:
- ⚠️ WSL worktree 경로(`/home/zenonsufu1/dev/poro-work-poromon/...`)가 **바뀔 수 있음**. 다음 세션 시작 시 실제 경로 재확인(pwd/git rev-parse). CLAUDE.md·메모리의 옛 경로 표기 주의.
- 클라 배포 경로 `/mnt/c/Users/User/curseforge/.../PoroMon 0.1 Dev/mods/`는 **Windows쪽이라 폴더명 변경과 무관**(그대로).
- `.local/server`(런타임, 비추적)도 worktree 내부라 함께 이동됨 — 재기동만 하면 됨.

### ▶ 다음 세션 과업 (우선순위)
1. **특성 마개조 + 텍스처 클라 검증**(클라 재시작): 위 ⚠️ ①~⑥ 전수 확인. 이상 시 수정.
2. **밸런스 패스**: 조우 stage_weight 적용·컨셉 차등/apex 중복 차별화·관장 후반 메가 검증.
3. **IB-003 커스텀 메뉴 GUI 화면(배경)**: 바닐라 컨테이너 텍스처 투박 → 방식 검토(B 타이틀 이미지-폰트 트릭 우선 / A 리소스팩 generic_54 / C 클라 Screen). `idea_inbox IB-003`.
4. (선택) push(origin 56커밋 ahead) · 특성 정수 "현재 특성" 표시 · 운영자 GUI apex 확률×2.

## ★ 알파 테스트 검증 과제 (2인/인게임 — 헤드리스로 불가)
> 코드/데이터·헤드리스는 통과했으나 **실제 클라(특히 2인) 접속이 있어야 검증되는 항목**. 알파 테스트 때 일괄 확인.

**정규리그 (2인 필수, 결정 036/036-a):**
- [ ] 큐 매칭 성사(점수 근접·윈도우 확대·재대전 쿨다운 600s)
- [ ] **lvl50 정규화 실제 적용**(BattleFormat.adjustLevel — 양측 50으로 싸우는지)
- [ ] 승/패 점수 ±반영(승+10/패−7, 하한0)·순위표 갱신
- [ ] **동적 아레나**: 매칭 시 방 TP·마주봄·종료 후 원위치 복귀·방 철거
- [ ] 동시 다중 매치 격리(셀 충돌 없음)
- [ ] 대전 중 접속종료 → 상대 복귀 + 끊긴 자 재접속 시 원위치 복귀(PENDING)

**기믹 게이트 (1인 가능, 결정 037):**
- [ ] 배틀에서 테라/다이맥스/Z 버튼 미노출 + 메가만 작동(메가-온리 체감)

**네더 정책 (결정 039-a, 1인 가능):**
- [ ] 오버월드→네더 포탈 = 고정 허브 도착 / 네더→오버월드 = 진입 좌표 복귀
- [ ] 네더 허브 반경 내 블록 파괴 차단(포탈·스포너) + op 우회
- [ ] 네더 월드보더 5000 적용 체감
- ⚠️ 선행: **3단계 허브 물리 건설**(요새 인접+블레이즈 스포너) 후 core.json hubX/Y/Z 실좌표 설정

**조우 (1인, 결정 035):**
- [ ] 제단 풀 우클릭 확률 표시 = 실제 분포(2단계 가중)·apex 토글 시 배너/확률 증가

**관장 (1인):**
- [ ] 후반 관장(5~8) 메가 발동(초반 1번만 확인됨)

**특성 마개조/텍스처/한글화 (§세션마감 ①~⑥):**
- [ ] 정수 텍스처(기술=시안/특성=DNA) 렌더 · 특성 정수 우클릭 해제 · 특성 변경 메뉴(목록/검색/부여)·**forced 특성 전투 반영** · 마스터볼 편의상점 · 기술머신 한글명/한글검색

## 5. 진행 중 / 미해결 TODO (요약)
- ✅ **전설 스폰 정리(2026-06-07, 결정 038)**: ① **하급·중급 필드 이벤트 구현** — `encounter/FieldEventManager`(2시간 주기·30분 디스폰·좌표 공개 채팅 공지·월드보더 안·동시 1마리·no-op Despawner로 영속). `/poromon admin fieldevent` 테스트. 헤드리스 검증(아그놈 Lv60 출현·공지·중복거부·에러0). **운영자 토글(038-a)**: AdminMenu 슬롯34 "전설 출현 주기 ×2 단축"(120→60분, PoroMonState.fieldEventFast 영속, 즉시 반영). ② **Cobblemon 기본 전설 자연스폰 차단(결정 013) = 기본값으로 이미 충족** — jar 검증 결과 Cobblemon 1.7.3은 전설/환상/UB/패러독스 spawn_pool_world **0종**(자연 스폰 안 함) + LM 비활성 → 차단 datapack 불필요. (이전 "바닐라 전설이 뜰 수 있음" 기록은 오판이었음.)
- ✅ **species ID 검증 완료** → `01_modpack/jar_registry_reference.md`: 전설 71(restricted 27 / 준전설 44)·환상 23·UB 11·패러독스 20 실 ID 확정.
- ✅ **전설/환상 풀 배치 완료(결정 026)**: 조우권 적힌 전설=전수 실재(누락0). 미배치 28+마샤도 배치 — 박스전설4=최상급+컨셉, 준전설 그룹=등급/컨셉 분산, 환상13=이벤트/컨셉 분산(코스모움=폼 제외). 전설70+환상23 전수 배치 재검증.
- ✅ **legendary_pools.draft.yml 작성**: 16풀 전수에 실 `cobblemon:<id>` + 초기 가중치/enabled. 환상=이벤트 게이트(일반 티어 false, 컨셉/이벤트만 true), apex 시그니처(레쿠쟈/아르세우스/창조신) 저가중. YAML파싱·ID오타·apex동기화 검증 통과.
- ✅ **희귀 풀 확정(#3)**: 비전설 600족/인기 라인 **73종 전수 실 ID + 전원 비전설 라벨 확인**, rare_encounter_pool 주입(stage basic/middle/final). 종합 재검증(16풀 285엔트리, 오타0, 동기화0).
- ✅ **Eggs Addon 동작 검증(결정 027)**: 부화종=`egg/poke/*.mcfunction`의 `spawnpokemon`(loot table은 난수범위만 — 기존 문서 부정확 정정). 기본 풀 common56(스타터+흔한)·rare26(희귀조우권 중복)·shiny81. 알=armor_stand+컴포넌트, 지급=`egg/give/<등급>`. 커스텀=mcfunction 오버라이드+loot rolls.max, 가중=인덱스중복. **커스텀 알 현재 0개**(모드 기본만).
- ✅ **알 방랑상인 비활성 적용(결정 027)**: OpenLoader 팩 `poromon_egg_control`로 `diesse:egg/villager_spawn` 빈 함수 오버라이드. 서버 기동 검증(datapack list 활성+eggs 모드보다 뒤 로드). 야생 둥지 자연 스폰은 미통제(별도 결정).
- ✅ **배틀타워 50층 초안 편입·검증(결정 028)**: 외부 초안→`battle_tower_design.md`. 입장 조건 **8관장 상향**(CANON 4곳 동기화: league §2/§3·gym_badge·hub). 검증: 종족 119·기술 172·메가스톤10·시그니처기18 **전수 실재(매칭0)**. 남은=동작(NPC메가/AI 실배틀). 레벨 명확화: **NPC Lv100 고정 / 플레이어 실레벨(정규화X, 엔드콘텐츠 육성 전제)** — 정규리그 Lv50 정규화와 대비(충돌 아님). **보상 설계 완료(§3-R)**: 층당 골드+10층 체크포인트+50층 특별(칭호·조우권·왕중왕전 예선), 1회 완주 ~95,000골드 예시.
- ✅ **NPC 구현 경로 검증(§4-N)**: Cobblemon 네이티브 트레이너 NPC(`cobblemon:npc`+class 프리셋) 소환 성공. 파티 `level=100/held_item/moves/ability/nature/IV·EV/skill(AI)` 전부 지원 → **타워 50층 파티가 NPC로 1:1 표현**. ⇒ 구현 = NPC 프리셋 50개 + PoroMonCore 오케스트레이션(진행저장/소환/보상). 진행도 영속화는 PoroMonCore 필요(현 0줄). **남은 실배틀 검증=NPC 메가 발동(클라 필요)**.
- ✅ **NPC 배틀 무동작 근본원인 규명(javap 바이트코드 추적)**: `start_battle`→`BattleBuilder.pvn`→`getPartyForChallenge`→`SimplePartyProvider.provide`. /summon NPC는 배틀 파티 미초기화 → pvn `noParty` 에러, start_battle은 `ifSuccessful`만 처리해 조용히 무동작(stock NPC 동일). **구현경로 확정**: `pvn` 마지막 인자=`PartyStore` 명시주입 → PoroMonCore가 층 파티를 직접 빌드해 `pvn(player, npc, …, 층파티)` 호출하면 배틀 확정 오픈(메가도 여기서 확인). battle_tower §4-N.
- ✅ **PoroMonCore 0.1 스캐폴드 생성·빌드·로드(Phase 2 착수)**: `custom-mods/poromon-core/`(Fabric 1.21.1/Java21, 순수 Fabric). `PoroMonCore`(ModInitializer)·`PlayerProgress`·`PoroMonState`(PersistentState 월드부속 NBT)·`PoroMonCommand`. **BUILD SUCCESSFUL** → jar 서버 배포 → `poromoncore 0.1.0` 로드 + 초기화 로그 + `/poromon` 명령 동작 확인. `/poromon admin tower set <player> <floor>`로 진행도 영속화 테스트 가능(클라 접속 시). **다음: Cobblemon 의존 추가 + 배틀타워 pvn 오케스트레이션.**
- ✅ **MSD 핵심 아이템 ID 검증**: 키스톤·메가링·메가스톤 47·주홍/쪽빛구슬·Z링·테라오브·다이맥스밴드 실 ID+한글명 확정. ko_kr 기본 포함.
- ✅ **상점 카탈로그 ID 채움(#2)**: §3.1~3.3·3.6 TODO→실 ID. Cobblemon 볼12+마스터·회복약·진화돌11·비타민6·민트25·특성캡슐/패치·사탕류 + 메가스톤47 전수(오타0 검증). Exp Share=미존재 확인. 잔여=SimpleTMs 개별TM·Eggs 알 ID.
- ⚠️ 잔여: **SimpleTMs 개별 기술 TM ID**(lang 1:1 부재 → 동적 합성 추정, 컴포넌트 방식 확인 필요), **Eggs 알 아이템 ID**·둥지 스폰 비활성 여부.
- ⚠️ 레쿠쟈 메가 해금 방식(MSD에 `rayquazite` 미발견 — 기술/전용처리 추정, 추가 확인).
- keystone/메가스톤 월드 획득 경로(메가팔찌 골드 독점 관련) 확인.
- config 포맷: 0.1 JSON vs legendary_pools/events YAML 혼용 결정.

## 6. 파일/디렉터리 맵
- 규칙/핸드오프: `CLAUDE.md`, `task.md`(이 파일)
- 문서: `docs/README.md`(인덱스) / `docs/00_project/decisions.md`(결정 001~022) / `docs/00_project/roadmap.md`(Phase 0~7)
- 모드팩 분석: `docs/01_modpack/{server_mod_separation.md, client_mod_tiers.md(설치기용 티어), jar_feature_audit.md, modpack_list.md}`
- 서버: `docs/02_server/{server_setup, world_policy, protection_policy, server_runbook}.md`
- 코어 설계: `docs/03_poromoncore/{poromoncore_spec, module_structure, config_structure, database_schema, commands, menu_design}.md`
- 게임 설계: `docs/04_game_design/{hub, economy, shop_design, shop_catalog_0.1, gym_badge, league_season, legendary_encounter, encounter_pool, egg_pool, mega_tera_unlock}_*.md`
- 운영: `docs/05_operations/localization_policy.md`
- 실제 jar: `modpack/client/mods/`(80) · 감사 보고서: `reports/jar_inspection/`(6) + `reports/{jar_inspection_summary,mod_classification,client_mod_jars}`
- 스크립트: `scripts/{sync-server-mods.sh(초안), extract-curseforge-pack.sh, run-server.sh(빈), backup-server.sh(빈)}`
- 서버 런타임(**기동 검증됨**, 비추적): `.local/server/{mods(25), config, world, logs, eula.txt, start.sh, server.properties}` ← 표준. (옛 `.local/poromon-server/`는 stale)
- 커스텀 모드(빈 디렉터리): `custom-mods/poromon-core/`

## 7. 주의 원칙 (계속 유지)
- 구현 코드는 아직 작성 안 함(설계/문서 단계).
- **실제 ID/namespace/config key는 추측 금지 → 미확인은 TODO.** jar 확인 사실 vs 설계 추정 구분.
- 클라 모드 통째로 server/run/mods 복사 금지(화이트리스트만). 파일 삭제 신중.
- 종족값/타입/기술 밸런스 미수정. 전설 알 판매 금지. 유저 표시 텍스트 한국어.
