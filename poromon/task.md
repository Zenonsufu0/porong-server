# PoroMon 작업 기록 / 다음 세션 핸드오프

> 최종 업데이트: 2026-06-05
> 이 파일은 세션 간 작업 연속성을 위한 핸드오프 노트다. 다음 세션은 이 파일부터 읽고 이어서 진행한다.
> 권위 규칙: `CLAUDE.md` · 문서 인덱스: `docs/README.md` · 결정 기록: `docs/00_project/decisions.md`(001~026)
>
> **2026-06-05 모노레포 구조 정리:** poromon 자체 폴더/파일은 **변경 없음**. 다만 레포 레벨에서 RPG 자산과 프로젝트 전역 docs가 `poro-rpg/`로 통합되고 루트 `docs/`가 폐지됨. 포로몬은 계속 `poromon/`(자체 `docs/`·`server/`·`custom-mods/`) 안에서 독립적으로 작업한다.

---

## 0. 현재 단계
**Phase 0(설계/문서) 완료 + Phase 1(서버 최소구성 기동) 1차 통과.** PoroMonCore **구현 코드는 아직 0줄**(Phase 2 대기).
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
- [ ] (이후) Phase 2: `custom-mods/poromon-core` Gradle 골격

## 5. 진행 중 / 미해결 TODO (요약)
- ✅ **species ID 검증 완료** → `01_modpack/jar_registry_reference.md`: 전설 71(restricted 27 / 준전설 44)·환상 23·UB 11·패러독스 20 실 ID 확정.
- ✅ **전설/환상 풀 배치 완료(결정 026)**: 조우권 적힌 전설=전수 실재(누락0). 미배치 28+마샤도 배치 — 박스전설4=최상급+컨셉, 준전설 그룹=등급/컨셉 분산, 환상13=이벤트/컨셉 분산(코스모움=폼 제외). 전설70+환상23 전수 배치 재검증.
- ✅ **legendary_pools.draft.yml 작성**: 16풀 전수에 실 `cobblemon:<id>` + 초기 가중치/enabled. 환상=이벤트 게이트(일반 티어 false, 컨셉/이벤트만 true), apex 시그니처(레쿠쟈/아르세우스/창조신) 저가중. YAML파싱·ID오타·apex동기화 검증 통과.
- ✅ **희귀 풀 확정(#3)**: 비전설 600족/인기 라인 **73종 전수 실 ID + 전원 비전설 라벨 확인**, rare_encounter_pool 주입(stage basic/middle/final). 종합 재검증(16풀 285엔트리, 오타0, 동기화0). **남은 건 가중치 운영 튜닝 + egg_pool 600족 중복정리**.
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
