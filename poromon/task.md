# PoroMon 작업 기록 / 다음 세션 핸드오프

> 최종 업데이트: 2026-06-05
> 이 파일은 세션 간 작업 연속성을 위한 핸드오프 노트다. 다음 세션은 이 파일부터 읽고 이어서 진행한다.
> 권위 규칙: `CLAUDE.md` · 문서 인덱스: `docs/README.md` · 결정 기록: `docs/00_project/decisions.md`(001~022)
>
> **2026-06-05 모노레포 구조 정리:** poromon 자체 폴더/파일은 **변경 없음**. 다만 레포 레벨에서 RPG 자산과 프로젝트 전역 docs가 `poro-rpg/`로 통합되고 루트 `docs/`가 폐지됨. 포로몬은 계속 `poromon/`(자체 `docs/`·`server/`·`custom-mods/`) 안에서 독립적으로 작업한다.

---

## 0. 현재 단계
**Phase 0(설계/문서) 거의 완료 → Phase 1(서버 최소구성 기동) 직전.** 아직 **구현 코드 0줄**, 서버 미기동.
- 모드팩: PoroMon 0.1 Dev / MC 1.21.1 / Fabric Loader 0.18.4 / Java 21.
- 실제 jar 80개를 `modpack/client/mods/`에 복사 완료 + jar 내부 감사 완료.

## 1. 지금까지 완료한 것
- **문서 체계 구축**: `docs/` 거의 전부 한글화 + 인덱스(`README.md`).
- **게임 설계 확정**(decisions 011~024): PvP off / 허브만 보호 / 골드 단일 화폐 / 경매장 없음 / 야생만 골드보상(레벨비례) / 상점 7구역 / 짐 8개(순차강제·초회보상·레벨캡 100) / 배틀타워 50층(진행저장) / 정규리그(점수제·lv50·실시간매칭만·8배지) / 챔피언스리그(서버 마지막날 토너먼트) / 메가만 허용(테라·다이맥스·Z off) / **LM 완전 비활성(023)** / **상점=하이브리드(매입·편의=9번 메뉴, 핵심=허브 NPC)·골드 스케일 §9 기준선(024)**.
- **조우권 체계**: 일반 5등급(희귀/하급/중급/상급/최상급) + 컨셉 특수 10종(하늘~영원). 하급·중급=조우권+2시간 필드이벤트, 상급·최상급=개인방. 레쿠쟈=하늘, 아르세우스=영원.
- **애드온 추가 확정**(decisions 015/017): SimpleTMs, Eggs Addon, Legendary Monuments.
- **서버/클라 분리**(실제 jar 기준): 서버 화이트리스트 19개 / 애매 5 / 클라 제외 56. `scripts/sync-server-mods.sh` 초안(DRY_RUN, 미실행).
- **jar 내부 감사 완료**: `docs/01_modpack/jar_feature_audit.md` — 실제 아이템 ID·한글화·충돌 확인.

## 2. jar 감사 핵심 결과 (확정 사실)
- **Mega Showdown**: `mega_showdown:keystone`(채굴 가능), `mega_bracelet`, `charizardite_x/y`, `<name>ite` 메가스톤 다수, `tera_orb`/`tera_shard`, `z_ring`/`ium_z`, dynamax_tab. **ko_kr 기본 포함.**
- **SimpleTMs**: `simpletms:tm_<move>`(1000+), `tm_blank/tr_blank`, `case_tm/case_tr/machine_tm`. TM/TR 월드 비활성 토글. **ko_kr 기본 포함.**
- **Eggs Addon**: 데이터팩형(namespace `diesse`). 풀=loot table common/rare/shiny/rides → **datapack 오버라이드로 커스텀 가능**. 상인 없음(둥지 스폰형). 알 아이템 ID 불명확.
- **Cobblemon**: ko_kr 기본 포함.
- **Legendary Monuments**: ko_kr **없음**(신규 번역 1순위). 자체 소환 시스템 방대(제단/항아리/피리/trial spawner/바이옴).

## 3. ⚠️ BLOCKER 상태 (2026-06-05 갱신)
1. **[해소 → 결정 023] Legendary Monuments 처리 = (A) 완전 비활성 확정.**
   - LM worldgen(구조물·feature·광석) + 루트테이블을 datapack 오버라이드로 비활성 → 소환 아이템 자연 획득 차단(아이템 우클릭 소환 로직은 자바 하드코딩이라 제거 불가, 획득 차단으로 무력화).
   - 문서 반영 완료: `decisions.md`(023), `legendary_encounter.md`(LM 섹션), `encounter_pool_design.md`(§10).
   - **남은 구현 TODO**: 서버 mods 배치 후 실제 jar의 `data/legendarymonuments/worldgen/*`·`loot_table/*` 경로 확인 → overrides datapack으로 비활성 작성. terrablender 바이옴은 코드 등록이라 로그로 확인 후 게이트.
2. **chipped / cobblefurnies / terrablender** (LM 하드 의존): 공개 팩 modlist 80개에 **미포함** 확인 → **LM jar 내부 JIJ 추정**(accessories·mega_showdown은 modlist에 있음). 확정 검증은 실제 jar 내부 또는 **서버 기동 로그**(`Missing dependency` 경고 유무)로 — Phase 1에서 확인.
3. **client/mods=80 vs 기대 81**: `server_mod_separation.md:17` 기설명 — manifest CurseForge 80 + 수동 swingthrough 기대 = 81, **1건 차이 = swingthrough 미반영**. 신규 미스터리 아님(추적만).

## 4. 다음 액션 (Phase 1)
- [x] §3-1 LM 처리 방향 결정 → 완전 비활성(결정 023). [ ] 실제 비활성 datapack 작성(서버 mods 배치 후 jar worldgen 경로 확인)
- [ ] §3-2 chipped/cobblefurnies/terrablender JIJ 여부 = 서버 기동 로그로 검증
- [ ] `server/run` 기동 파일 작성: `eula.txt`, `server.properties`(pvp=false, spawn-protection), `start.sh`(Java21, JVM)
- [ ] `DRY_RUN=0 ./scripts/sync-server-mods.sh` 로 서버 mods 19개 복사(실행 전 §3 확인)
- [ ] 서버 1차 기동 → Cobblemon/MSD/SimpleTMs/Eggs/LM 로드, 의존성 경고 확인(애매 5개는 경고 시 추가)
- [ ] LM 구조물/소환 우회 실제 동작 점검
- [ ] 클라 접속 테스트
- [ ] (이후) Phase 2: `custom-mods/poromon-core` Gradle 골격

## 5. 진행 중 / 미해결 TODO (요약)
- species ID 전부 TODO(cobblemon 네임스페이스 추정, jar 직접 노출 X) — Cobblemon 레지스트리 확인 필요.
- Eggs 알 아이템 ID, 둥지 스폰 비활성 가능 여부.
- MSD 다이맥스 구체 아이템 ID, 메가스톤 ko_kr 커버리지.
- keystone_ore 월드 생성 비활성 가능 여부(메가팔찌 골드 독점 관련).
- config 포맷: 0.1 JSON vs legendary_pools/events YAML 혼용 결정.

## 6. 파일/디렉터리 맵
- 규칙/핸드오프: `CLAUDE.md`, `task.md`(이 파일)
- 문서: `docs/README.md`(인덱스) / `docs/00_project/decisions.md`(결정 001~022) / `docs/00_project/roadmap.md`(Phase 0~7)
- 모드팩 분석: `docs/01_modpack/{server_mod_separation.md, jar_feature_audit.md, modpack_list.md}`
- 서버: `docs/02_server/{server_setup, world_policy, protection_policy, server_runbook}.md`
- 코어 설계: `docs/03_poromoncore/{poromoncore_spec, module_structure, config_structure, database_schema, commands}.md`
- 게임 설계: `docs/04_game_design/{hub, economy, shop_design, shop_catalog_0.1, gym_badge, league_season, legendary_encounter, encounter_pool, egg_pool, mega_tera_unlock}_*.md`
- 운영: `docs/05_operations/localization_policy.md`
- 실제 jar: `modpack/client/mods/`(80) · 감사 보고서: `reports/jar_inspection/`(6) + `reports/{jar_inspection_summary,mod_classification,client_mod_jars}`
- 스크립트: `scripts/{sync-server-mods.sh(초안), extract-curseforge-pack.sh, run-server.sh(빈), backup-server.sh(빈)}`
- 서버 런타임(준비됨, 비어있음): `server/run/{mods, config(openloader만), defaultconfigs, world, logs}`
- 커스텀 모드(빈 디렉터리): `custom-mods/poromon-core/`

## 7. 주의 원칙 (계속 유지)
- 구현 코드는 아직 작성 안 함(설계/문서 단계).
- **실제 ID/namespace/config key는 추측 금지 → 미확인은 TODO.** jar 확인 사실 vs 설계 추정 구분.
- 클라 모드 통째로 server/run/mods 복사 금지(화이트리스트만). 파일 삭제 신중.
- 종족값/타입/기술 밸런스 미수정. 전설 알 판매 금지. 유저 표시 텍스트 한국어.
