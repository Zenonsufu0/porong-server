# PoroMon 작업 기록 / 다음 세션 핸드오프

> 최종 업데이트: 2026-06-05
> 이 파일은 세션 간 작업 연속성을 위한 핸드오프 노트다. 다음 세션은 이 파일부터 읽고 이어서 진행한다.
> 권위 규칙: `CLAUDE.md` · 문서 인덱스: `docs/README.md` · 결정 기록: `docs/00_project/decisions.md`(001~028)
>
> **2026-06-05 모노레포 구조 정리:** poromon 자체 폴더/파일은 **변경 없음**. 다만 레포 레벨에서 RPG 자산과 프로젝트 전역 docs가 `poro-rpg/`로 통합되고 루트 `docs/`가 폐지됨. 포로몬은 계속 `poromon/`(자체 `docs/`·`server/`·`custom-mods/`) 안에서 독립적으로 작업한다.

---

## 0. 현재 단계
**Phase 0(설계/문서) 완료 + Phase 1(서버 기동) 통과 + Phase 2 진행(배틀타워 핵심 완성).** PoroMonCore 0.1: 빌드·로드·`/poromon` 명령 + **진행도 영속화(실증)** + **배틀타워 실배틀**(pvn 명시파티)·**AI 공격**·**속도 8배**·**NPC 메가진화**·아이템드롭 차단 전부 **인게임 작동 확인**. → 상세 **§4b**. 다음 과업(메뉴/허브/상점/알/조우권) **§4c**.
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
1. **메뉴 아이템/바**(9번 슬롯 리그패스 + 우클릭 GUI) — `menu_design.md`. MenuItemManager/MenuGuiManager.
2. **허브 구성** — `hub_design.md`. 허브 텔레포트(`/poromon hub`) + 구역(짐/제단/메가연구소/마켓/배틀타워 입구).
3. **상점 구조** — `shop_design.md`·`shop_catalog_0.1.md`(검증 ID 보유). 하이브리드(9번메뉴 매입/편의 + 허브 NPC 통제). EconomyBridge 골드.
4. **알 구조** — `egg_pool_design.md`(결정 027). `egg/give/<등급>` 연동, 방랑상인 비활성(적용됨), 커스텀 알(드래곤/화석/타입별) mcfunction.
5. **조우권 생성·구조** — `encounter_pool_design.md`·`legendary_pools.draft.yml`(검증 ID). EncounterTicket 커스텀아이템 + InstanceRoom + pvn으로 전설 소환(배틀타워와 동일 패턴 재사용 가능).
> 배틀타워 잔여(저우선): 메가 연출(클라모드 필요), 다른 메가 47종 검증, 보상 지급(§3-R) 실제 연동(RewardManager), 진행도 ↔ 배틀 승리 연동(현재 set 명령만).

## 5. 진행 중 / 미해결 TODO (요약)
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
