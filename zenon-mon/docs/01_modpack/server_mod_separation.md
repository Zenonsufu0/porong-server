# Server / Client Mod Separation (실제 jar 기준)

> 대상: **Zenon Mon 0.1 Dev** (MC 1.21.1 / Fabric Loader 0.19.3)
> 소스: **`modpack/client/mods/` 실제 jar 85개**(CurseForge 프로필에서 복사) + `reports/mod_classification.md`(1차 자동 분류) + `reports/client_mod_jars.txt`.
> 목적: 데디케이티드 서버(`.local/server/mods`)에 **서버용 후보만** 화이트리스트로 복사. **클라 전용은 제외.**
> 갱신 이력: modlist 추정 → 실제 jar 80개 기준 재작성(결정 017) → **LM 의존 5종 추가로 85개**(2026-06-05, LM 최종 포함) → **구성 갱신(결정 044, 2026-06-09)**: `eggs-cobblemon-addon` 제거(결정 032) − `ZenonMonCore` 커스텀 모드 양쪽 추가 + `complete-cobblemon-collection`(클라 전용 모델팩) 추가 → **클라 86 / 서버 25**(아래 §5 재집계).
>
> ⚠️ **2026-06-09 현재 실측**: `modpack/client/mods/*.jar` = **86개**, `.local/server/mods/*.jar` = **25개**. 본 문서 표가 열거하는 개별 jar 이름은 구버전(eggs 포함) 잔재가 있을 수 있으니 §1c·§4-3·§5의 갱신분을 우선한다.

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
| ~~`eggs-cobblemon-addon-0.9.jar`~~ | ~~알/부화(서버측)~~ | ❌ **제거됨(결정 032)** — 알 시스템 폐기(조우권 중복+리소스팩 의존). 클라/서버 양쪽 jar 삭제. |
| `LegendaryMonuments-7.8.jar` | 전설 구조물/소환(서버측) | ✅ **최종 포함**(의존 5종 확보, 2026-06-05 부팅 확인). ⚠️ 자체 소환은 ZenonMonCore 전설 통제와 충돌 → **비활성/제한 datapack·config 별도 검토**(결정 023). |
| `accessories-fabric-1.1.0-beta.53+1.21.1.jar` | MSD 착용 슬롯 의존 | depends 확인 |

### 1b. LM 하드 의존 체인 (Server Required) — 5개
LM 7.8 채택을 위해 추가. **CurseForge 프로필에 추가**(클라+서버 공통, 모두 `environment="*"`). 2026-06-05 부팅테스트로 의존 충족·정상 로드 확인.

| jar | 역할 | 의존 |
|---|---|---|
| `chipped-fabric-1.21.1-4.0.2.jar` | LM 하드 의존(블록 변형) | → athena, resourcefullib, fabric-api |
| `CobbleFurnies-fabric-1.1.jar` | LM 하드 의존(가구) | → athena, cobblemon, architectury |
| `TerraBlender-fabric-1.21.1-4.1.0.8.jar` | LM 하드 의존(바이옴) | (fabric/mc만) |
| `athena-fabric-1.21.1-4.0.6.jar` | chipped/cobblefurnies 의존 lib(env=*) | (fabric/mc만) |
| `resourcefullib-fabric-1.21-3.0.12.jar` | chipped 의존 lib(env=*) | (fabric/mc만) |

> ✅ **LM 하드 의존 결론(결정 023 §2 — 2026-06-05 확정)**: LM 7.8은 `chipped`·`cobblefurnies`·`terrablender`를 하드 의존하나 팩에 없고 JIJ 번들도 아니었음(`HARD_DEP_NO_CANDIDATE`로 1차 부팅 차단). → **3종을 CurseForge 프로필에 추가**(부수 lib `athena`·`resourcefullib` 동반, 총 5종) 후 LM **최종 포함**. (`accessories`·`mega_showdown` 의존은 기존 팩 내 충족.)
> ⚠️ **후속(별도)**: LM 자체 구조물/소환이 ZenonMonCore 전설 통제(조우권·사설룸)와 충돌하지 않도록 worldgen/loot_table 비활성 또는 제한 datapack·config 전략을 별도 검토·적용한다(이번 범위 아님).

### 1c. ZenonMonCore 커스텀 모드 (Server Required + Client Required) — 1개
서버 규칙 엔진. CurseForge/Modrinth에 없는 **자체 빌드 모드**(`custom-mods/zenon-mon-core/`). 배포 산출물 `zenon-mon-core-0.1.0.jar`.

| jar | 역할 | 배치 |
|---|---|---|
| `zenon-mon-core-0.1.0.jar` | 진행도·메뉴·상점·조우·리그·인증 등 서버 규칙 엔진 | **서버 필수** + **클라 필수** |

> ⚠️ **양쪽 배포 필수**: 서버 로직은 물론, **클라에도 동일 jar 필요**(배지·조우권·정수 커스텀 텍스처/모델이 jar `assets/`에 내장 — CustomModelData 모델 분기를 클라가 렌더하려면 jar가 있어야 함, task.md §4f·§4j). translatable 텍스트는 네트워크 전송이라 클라 lang으로 자동 렌더되지만, **CustomModelData 텍스처는 클라 jar 없으면 종이로 보임**.
> ⚠️ **빌드 변경 시 서버+클라 동시 재배포**(해시 일치 확인). 간편설치기는 이 jar를 CurseForge 메타로 해소 불가 → **overrides에 직접 번들**해야 한다(`client_pack_policy.md` §간편설치기 참조).

## 2. 서버 권장 (Server Recommended) — 11개
성능/운영 + 그 의존 라이브러리. (※ `prickle`은 권장이 아니라 OpenLoader **하드 의존**이라 사실상 필수.)

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
| `OpenLoader-fabric-1.21.1-21.1.5.jar` | 외부 datapack/resource 로더(서버측 data) | dep prickle |
| `prickle-fabric-1.21.1-21.1.11.jar` | Darkhax lib | **OpenLoader 하드 의존**(`prickle>=21.1.8`). §3→서버 필수 재분류(2026-06-05 부팅테스트) |

> **서버 화이트리스트 = §1(9) + §1b(5) + §2(11) = 25개**(LM 최종 포함) → `scripts/sync-server-mods.sh`.
> 스크립트는 화이트리스트로 DEST를 미러링(prune)하므로, 화이트리스트에 없는 stale jar는 재동기화 시 자동 제거된다.

## 3. 애매 / 공용 후보 (Conditional) — 4개
양쪽 동작 가능, 서버 필수 아님. **기본 제외**, 부팅 시 `Missing dependency` 뜨거나 동작 필요 시 개별 추가.
(※ `prickle`은 OpenLoader 하드 의존으로 §2 서버 필수 승격 → 이 목록에서 제외.)

| jar | 판단 |
|---|---|
| `appleskin-fabric-mc1.21-3.0.6.jar` | 서버 넣으면 포만도/채도 동기화 정확. 없어도 클라 동작 |
| `craftingtweaks-fabric-1.21.1-21.1.7.jar` | 서버 카운터파트 존재(QoL). dep Balm |
| `cloth-config-15.0.140-fabric.jar` | 설정 lib. 서버측 모드가 요구하면 포함 |
| `bookshelf-fabric-1.21.1-21.1.80.jar` | Darkhax lib. 이 팩 의존처가 대부분 클라(enchdesc 등) → 부팅 경고 시만 |

## 4. 클라이언트 전용 — 서버 제외 (56개)
> (LM 의존 5종은 §1b 서버 필수이므로 제외 목록 아님. 클라 제외 수는 56개 유지.)
**`.local/server/mods`에 넣지 않음.** (★ = 서버 진입점 크래시 위험 큰 순수 클라)

### 4-1. 자동 보고서 `environment=client` (확정 클라, 32개)
AmbientEnvironment · BHMenu · BetterPingDisplay · BetterThirdPerson · CraftPresence · InvMove · InvMoveCompats · UniLib · cherishedworlds · ★citresewn · enchdesc · enhanced_attack_indicator · ★entity_model_features · ★entity_texture_features · fallingleaves · ★iris · lambdynamiclights · language-reload · make_bubbles_pop · modmenu · ★particlerain · ★particular · ★reeses-sodium-options · ★sodium-extra · ★sodium · ★sodium-shadowy-path-blocks · ★sodiumextras · ★sodiumoptionsapi · tipsmod · tooltipfix · ★visuality · ★wakes

### 4-2. 자동 `*`이나 **실제 클라 전용**(도메인 판정, 24개)
- 아이템뷰어/레시피(★): `emi` · `emi_enchanting` · `emi_ores` · `EMIProfessions` · `jei` · `jeed` · `AdvancedLootInfo`(ali)
- 지도: `xaerominimap` · `xaeroworldmap`
- 메뉴/HUD: `fancymenu` · `konkrete`(FancyMenu lib) · `melody`(FancyMenu lib) · `betteradvancements` · `dynamiccrosshair` · `shulkerboxtooltip` · `notenoughanimations` · `stendhal` · `enhanced_attack_indicator`(중복 제외)
- 렌더 최적화(★): `entityculling` · `sodiumleafculling`
- 기타 클라: `monsters-in-the-closet`(검증) · `yet_another_config_lib`(YACL) · `yosbr` · `swingthrough`(수동 동봉, 클라 입력)

> 특히 **Sodium 패밀리 전부 · Iris · ETF/EMF · EMI/JEI(+애드온) · Xaero · FancyMenu/Konkrete/Melody** 는 절대 서버 금지(작업 지시 명시).

### 4-3. 추가 클라 전용(2026-06-09, 결정 044) — 1개
| jar | 역할 | 서버 |
|---|---|---|
| `complete-cobblemon-collection-myths-and-legends-compat-2.0.1.jar` | Cobblemon 1.7.3 미구현 전설/환상 **모델·렌더 보충**(조우권 설계 278종 전수 렌더 커버) | **제외** — 서버는 렌더 무관(스폰은 Cobblemon 종 데이터로 처리). 클라만 필요. EMF(`entity_model_features`)와 함께 동작. (task.md §4h) |

> **클라 전용 합계 = 56 + 1(collection) = 57.** (eggs 제거로 양쪽에서 −1, 하지만 eggs는 §1 서버필수였지 §4 클라전용 목록은 아니었으므로 §4 카운트엔 영향 없음.)

---

## 5. 집계 (결정 044 갱신, 2026-06-09)

### 5-1. 현행(실측 기준)
| 구분 | 수 | 비고 |
|---|---|---|
| 서버 필수(§1) | **8** | LM 포함, **eggs 제거(−1)** |
| ZenonMonCore 커스텀(§1c) | **1** | `zenon-mon-core` (양쪽 필수) |
| LM 의존 체인(§1b) | 5 | chipped·cobblefurnies·terrablender + athena·resourcefullib |
| 서버 권장(§2) | 11 | prickle 승격 포함 |
| **서버 화이트리스트 합** | **25** | 8 + 1 + 5 + 11 |
| 애매/공용(§3) | 4 | 기본 제외 |
| 클라 전용 제외(§4) | **57** | 56 + complete-cobblemon-collection(§4-3) |
| **총 클라 jar** | **86** | (이전 85 → eggs −1, zenon-mon-core +1, collection +1) |

> **서버 25** = §1(8) + §1c(1, ZenonMonCore) + §1b(5) + §2(11). eggs 빠진 자리를 ZenonMonCore가 채워 합계는 25로 동일하나 **구성이 다름**.
> **클라 86** = 서버 공통 게임플레이/lib + 클라 전용(57) + ZenonMonCore + 애매(클라엔 일부 동봉).
> ⚠️ 데이터팩 통제(LM·메가)는 **서버측 OpenLoader `config/openloader/data/`**로만 적용(결정 043). 클라는 worldgen 서버권한이라 미적용 — 클라 jar 구성과 무관.

### 5-2. 이전(2026-06-05, 참고)
| 구분 | 수 |
|---|---|
| 서버 필수(§1) | 9 (eggs 포함, LM 포함) |
| LM 의존 체인(§1b) | 5 |
| 서버 권장(§2) | 11 |
| **서버 화이트리스트 합** | **25** |
| 클라 전용 제외(§4) | 56 |
| **총 jar** | **85** |

> 화이트리스트 25개는 2026-06-05 부팅테스트로 정상 로드 확인. 이후 eggs↔ZenonMonCore 교체는 헤드리스 재기동(2026-06-08~09)으로 확인(task.md §4k 등).

---

## 6. 서버 구동 테스트 전 체크리스트
### 6-1. 준비
- [ ] Java 21 (`java -version`)
- [ ] Fabric **server** (Loader 0.19.3 / MC 1.21.1) 설치
- [ ] `.local/server/eula.txt` = `eula=true`
- [ ] `server.properties`: `pvp=false`, `spawn-protection=<허브>`(결정 011/012)
- [ ] `scripts/sync-server-mods.sh`로 **화이트리스트 25개만** 복사됨(클라 0개)

### 6-2. 분리 검증
- [ ] `.local/server/mods`에 Sodium/Iris/EMI/JEI/Xaero/FancyMenu/ETF/EMF/EntityCulling **0개** 확인
- [ ] 클라 인스턴스(`modpack/client/mods`)는 그대로(서버 복사로 변경 X)

### 6-3. 1차 기동
- [x] `Done (..)! For help` 출력, 크래시 없음 (2026-06-05 확인, LM 포함 25개)
- [x] Cobblemon 1.7.3 + Fabric Language Kotlin(JIJ) 로드 로그
- [ ] `Missing dependency`/`requires` 경고 없음 → 뜨면 §3에서 해당 lib만 추가(우선 owo/accessories/balm/almanac/bookshelf/prickle 순 점검)

### 6-4. 애드온 로드 확인
- [x] Mega Showdown / SimpleTMs / Eggs / **Legendary Monuments** 로드 (2026-06-05 확인)
- [x] **LM 하드 의존 해소 확인(결정 023 §2)**: `chipped`/`cobblefurnies`/`terrablender`(+lib athena/resourcefullib) **§1b로 명시 추가**해 `Missing dependency` 경고 없음.
- [x] **LM 구조물 자연 생성 차단(결정 023)**: datapack `zenonmon_lm_control`(OpenLoader)로 `worldgen/structure_set/*` 21종을 빈 구조물 오버라이드. 2026-06-05 검증 — `/locate structure legendarymonuments:*` "Could not find". 추적: `modpack/overrides/config/openloader/packs/zenonmon_lm_control/`.
- [ ] **LM 소환 통제 잔여(결정 023, 별도 패스)**: `loot_table/*` 드롭 차단 + 소환/제단접근 **아이템 제작 레시피 무력화**(예 `arc_phone` 바닐라 재료 제작). Fabric 조건부 레시피로 제거 가능. → ZenonMonCore 전설 통제 정합. **이번 범위 제외(사용자 선택).**

### 6-5. 클라 접속 / 안정성
- [ ] Zenon Mon 클라 모드팩 접속 성공(서버가 클라 전용 모드 요구 안 함)
- [ ] 스폰/배틀/동기화 정상, 5~10분 TPS 안정
- [ ] `.local/server/logs/latest.log` ERROR 없음, `stop` 시 월드 정상 저장

## 7. 관련 문서
- **클라이언트 관점 티어링(간편설치기용): `client_mod_tiers.md`** ← 본 문서가 "서버에 무엇을 넣을지"라면, 그쪽은 "클라에 무엇을 권장/선택으로 둘지"(T0 코어/T1 권장/T2 선택). 결정 025.
- 동기화 스크립트: `../../scripts/sync-server-mods.sh`
- 서버 셋업/런북: `../02_server/server_setup.md` · `server_runbook.md`
- 보고서: `../../reports/mod_classification.md` · `client_mod_jars.txt`
