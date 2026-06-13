# Server / Client Mod Separation (실제 jar 기준)

> 대상: **PoroMon 0.1 Dev** (MC 1.21.1 / Fabric Loader 0.18.4)
> 소스: **`modpack/client/mods/` 실제 jar 80개**(CurseForge 프로필에서 복사) + `reports/mod_classification.md`(1차 자동 분류) + `reports/client_mod_jars.txt`.
> 목적: 데디케이티드 서버(`server/run/mods`)에 **서버용 후보만** 화이트리스트로 복사. **클라 전용은 제외.**
> 갱신 이력: modlist 추정 → **실제 jar 80개 기준 재작성**(Legendary Monuments 포함, 결정 017).

## 0. 분류 원칙 (자동 분류 맹신 금지)
1. 자동 보고서 `environment="client"` → **확정 클라 전용**(제외).
2. **자동 `COMMON_OR_UNKNOWN`(="*"/누락)은 서버 안전 보장이 아님.** Fabric 기본값이 `*`이라 EMI/JEI/Xaero/FancyMenu/EntityCulling 등 **클라 전용도 `*`로 선언**됨 → 도메인 지식으로 재판정.
3. 게임플레이가 서버측 연산 = 서버 필수(Cobblemon 스택).
4. 라이브러리는 **서버측 의존 모드가 있을 때만** 포함.
5. 성능/운영 = 권장. 애매한 공용은 별도 묶고 **필요 시에만**.
6. 최종 확정은 **데디 서버 부팅 테스트**(§6).

> ⚠️ 자동 파서 오류 2건(`mega_showdown`, `bwncr-fabric`)은 fabric.mod.json 제어문자 때문이며, 정체가 명확해 수동 분류함.
> ⚠️ 개수 불일치 TODO: client/mods=80, manifest CurseForge=80 + 수동 swingthrough = 기대 81. 1건 차이 → 누락 mod 또는 비-mod 리소스(resourcepack/shader) 여부 확인 필요.

---

## 1. 서버 필수 (Server Required) — 9개
Cobblemon 게임플레이 스택 + 필수 의존. 없으면 서버 미기동 또는 핵심 기능 불가.

| jar | 역할 | 확인 |
|---|---|---|
| `fabric-api-0.116.8+1.21.1.jar` | 전 모드 런타임 | — |
| `architectury-13.0.8-fabric.jar` | Cobblemon 의존 추상화 | — |
| `owo-lib-0.12.15.4+1.21.jar` | Cobblemon 의존 유틸 | depends 확인 |
| `Cobblemon-fabric-1.7.3+1.21.1.jar` | 포켓몬 엔진(코어) | — |
| `mega_showdown-fabric-1.8.4+1.7.3+1.21.1.jar` | 메가/테라 등(배틀 서버측) | 파서오류였음, 수동 |
| `SimpleTMs-fabric-2.3.3.jar` | TM/TR(기술 부여 서버측) | — |
| `eggs-cobblemon-addon-0.9.jar` | 알/부화(서버측) | — |
| `LegendaryMonuments-7.8.jar` | 전설 구조물/소환(서버측) | ⚠️ **완전 비활성 대상**(결정 023): worldgen/loot_table datapack 오버라이드로 소환 경로 차단. jar는 로드하되 자연 생성·드롭만 무력화. `legendary_encounter.md` LM 섹션 |
| `accessories-fabric-1.1.0-beta.53+1.21.1.jar` | MSD 착용 슬롯 의존 | depends 확인 |

> ⚠️ **LM 하드 의존 JIJ 검증(결정 023 §2)**: LM `fabric.mod.json` `depends`에 `chipped`·`cobblefurnies`·`terrablender`·`accessories`·`mega_showdown`이 **필수**. 공개 팩 modlist 80개에 `chipped`·`cobblefurnies`·`terrablender`는 **미포함** → LM jar 내부 **JIJ(jar-in-jar) 번들 추정**. 별도 jar가 없고 JIJ도 아니면 **서버·클라 미기동**. → §6 부팅 로그(`Missing dependency`)로 확정.

## 2. 서버 권장 (Server Recommended) — 10개
성능/운영 + 그 의존 라이브러리. 빠져도 크래시는 아니나 권장.

| jar | 역할 | 비고 |
|---|---|---|
| `lithium-fabric-0.15.1+mc1.21.1.jar` | 틱/AI 최적화 | |
| `krypton-0.2.8.jar` | 네트워크 최적화 | |
| `ferritecore-7.0.3-fabric.jar` | 메모리 절감 | |
| `Clumps-fabric-1.21.1-19.0.0.1.jar` | XP 오브 병합 | |
| `letmedespawn-1.21.x-fabric-1.5.0.jar` | 디스폰 제어(서버) | dep Almanac |
| `Almanac-1.21.1-2-fabric-1.5.2.jar` | letmedespawn 의존 라이브러리 | |
| `netherportalfix-fabric-1.21.1-21.1.3.jar` | 포털 링크 보정 | dep Balm |
| `balm-fabric-1.21.1-21.0.56.jar` | netherportalfix/craftingtweaks 의존 lib | |
| `bwncr-fabric-1.21.1-3.20.3.jar` | 보스 브로드캐스트 억제(서버) | 파서오류였음, 수동 |
| `OpenLoader-fabric-1.21.1-21.1.5.jar` | 외부 datapack/resource 로더(서버측 data) | |

> **§1 + §2 = 서버 화이트리스트 19개** → `scripts/sync-server-mods.sh`.

## 3. 애매 / 공용 후보 (Conditional) — 5개
양쪽 동작 가능, 서버 필수 아님. **기본 제외**, 부팅 시 `Missing dependency` 뜨거나 동작 필요 시 개별 추가.

| jar | 판단 |
|---|---|
| `appleskin-fabric-mc1.21-3.0.6.jar` | 서버 넣으면 포만도/채도 동기화 정확. 없어도 클라 동작 |
| `craftingtweaks-fabric-1.21.1-21.1.7.jar` | 서버 카운터파트 존재(QoL). dep Balm |
| `cloth-config-15.0.140-fabric.jar` | 설정 lib. 서버측 모드가 요구하면 포함 |
| `bookshelf-fabric-1.21.1-21.1.80.jar` | Darkhax lib. 이 팩 의존처가 대부분 클라(enchdesc 등) → 부팅 경고 시만 |
| `prickle-fabric-1.21.1-21.1.11.jar` | Darkhax lib. 동일 |

## 4. 클라이언트 전용 — 서버 제외 (56개)
**`server/run/mods`에 넣지 않음.** (★ = 서버 진입점 크래시 위험 큰 순수 클라)

### 4-1. 자동 보고서 `environment=client` (확정 클라, 32개)
AmbientEnvironment · BHMenu · BetterPingDisplay · BetterThirdPerson · CraftPresence · InvMove · InvMoveCompats · UniLib · cherishedworlds · ★citresewn · enchdesc · enhanced_attack_indicator · ★entity_model_features · ★entity_texture_features · fallingleaves · ★iris · lambdynamiclights · language-reload · make_bubbles_pop · modmenu · ★particlerain · ★particular · ★reeses-sodium-options · ★sodium-extra · ★sodium · ★sodium-shadowy-path-blocks · ★sodiumextras · ★sodiumoptionsapi · tipsmod · tooltipfix · ★visuality · ★wakes

### 4-2. 자동 `*`이나 **실제 클라 전용**(도메인 판정, 24개)
- 아이템뷰어/레시피(★): `emi` · `emi_enchanting` · `emi_ores` · `EMIProfessions` · `jei` · `jeed` · `AdvancedLootInfo`(ali)
- 지도: `xaerominimap` · `xaeroworldmap`
- 메뉴/HUD: `fancymenu` · `konkrete`(FancyMenu lib) · `melody`(FancyMenu lib) · `betteradvancements` · `dynamiccrosshair` · `shulkerboxtooltip` · `notenoughanimations` · `stendhal` · `enhanced_attack_indicator`(중복 제외)
- 렌더 최적화(★): `entityculling` · `sodiumleafculling`
- 기타 클라: `monsters-in-the-closet`(검증) · `yet_another_config_lib`(YACL) · `yosbr` · `swingthrough`(수동 동봉, 클라 입력)

> 특히 **Sodium 패밀리 전부 · Iris · ETF/EMF · EMI/JEI(+애드온) · Xaero · FancyMenu/Konkrete/Melody** 는 절대 서버 금지(작업 지시 명시).

---

## 5. 집계
| 구분 | 수 |
|---|---|
| 서버 필수(§1) | 9 |
| 서버 권장(§2) | 10 |
| **서버 화이트리스트 합** | **19** |
| 애매/공용(§3) | 5 |
| 클라 전용 제외(§4) | 56 |
| **총 jar** | **80** |

---

## 6. 서버 구동 테스트 전 체크리스트
### 6-1. 준비
- [ ] Java 21 (`java -version`)
- [ ] Fabric **server** (Loader 0.18.4 / MC 1.21.1) 설치
- [ ] `server/run/eula.txt` = `eula=true`
- [ ] `server.properties`: `pvp=false`, `spawn-protection=<허브>`(결정 011/012)
- [ ] `scripts/sync-server-mods.sh`로 **화이트리스트 19개만** 복사됨(클라 0개)

### 6-2. 분리 검증
- [ ] `server/run/mods`에 Sodium/Iris/EMI/JEI/Xaero/FancyMenu/ETF/EMF/EntityCulling **0개** 확인
- [ ] 클라 인스턴스(`modpack/client/mods`)는 그대로(서버 복사로 변경 X)

### 6-3. 1차 기동(필수 9개만 권장)
- [ ] `Done (..)! For help` 출력, 크래시 없음
- [ ] Cobblemon 1.7.3 + Fabric Language Kotlin(JIJ) 로드 로그
- [ ] `Missing dependency`/`requires` 경고 없음 → 뜨면 §3에서 해당 lib만 추가(우선 owo/accessories/balm/almanac/bookshelf/prickle 순 점검)

### 6-4. 애드온 로드 확인
- [ ] Mega Showdown / SimpleTMs / Eggs / Legendary Monuments 로드
- [ ] **LM 하드 의존 해소 확인(결정 023 §2)**: `chipped`/`cobblefurnies`/`terrablender`가 JIJ로 로드되어 `Missing dependency` 경고 없음. 뜨면 해당 jar를 별도 확보·추가(아니면 LM 제외 재검토).
- [ ] **LM 완전 비활성 검증(결정 023)**: 신규 청크에 LM 구조물(제단/trial spawner/shrine 등) **미생성**, 소환 아이템(항아리/피리/열쇠/구체 등) 야생/드롭 **미획득**. 실제 jar의 `data/legendarymonuments/worldgen/*`·`loot_table/*` 경로를 추출해 `overrides` datapack으로 비활성 후 재확인.

### 6-5. 클라 접속 / 안정성
- [ ] PoroMon 클라 모드팩 접속 성공(서버가 클라 전용 모드 요구 안 함)
- [ ] 스폰/배틀/동기화 정상, 5~10분 TPS 안정
- [ ] `server/run/logs/latest.log` ERROR 없음, `stop` 시 월드 정상 저장

## 7. 관련 문서
- 동기화 스크립트: `../../scripts/sync-server-mods.sh`
- 서버 셋업/런북: `../02_server/server_setup.md` · `server_runbook.md`
- 보고서: `../../reports/mod_classification.md` · `client_mod_jars.txt`
