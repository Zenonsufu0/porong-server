# Client Mod Tiers (클라이언트 편의성 분류 — 간편설치기용)

> 대상: **PoroMon 0.1 Dev** 클라이언트 모드 **85개** (`modpack/client/mods/` 실제 jar 기준, MC 1.21.1 / Fabric Loader 0.19.3)
> 목적: 서버/클라 분리(`server_mod_separation.md`)는 "서버에 무엇을 넣을지"의 관점. **이 문서는 클라이언트 관점** — 85개를 **코어(강제) / 권장 편의 / 선택 취향 / 서버전용(클라 불요)**로 나눠 **추후 간편설치기**가 프로필을 구성·토글·의존성 해소할 수 있게 한다.
> 분류 기준일: 2026-06-05. 서버 1차 기동(25개) 정상 확인 후 작성. **실제 ID/namespace는 추측 금지** — 역할 분류는 모드 정체 기준.

---

## 0. 설치기 모델 (3토글 + 자동 라이브러리)

간편설치기는 아래 4구분으로 클라 프로필을 만든다.

| 구분 | 설치기 동작 | 끄면 |
|---|---|---|
| **T0 코어** | 항상 설치, **토글 불가** | 서버 접속 불가/레지스트리 불일치 |
| **T1 권장 편의** | 기본 **체크 ON**, 끌 수 있음 | 성능↓ 또는 편의 기능 사라짐(게임은 정상) |
| **T2 선택 취향** | 기본 **체크 OFF**, 켤 수 있음 | 순수 비주얼/취향 — 없어도 무관 |
| **L 라이브러리** | 선택 모드의 **의존성으로 자동 포함** | 해당 모드 활성 시 자동 |
| **(S) 서버전용** | 클라 프로필에서 **제외 가능**(무해) | 클라에서 미사용 |

> 원칙: **T0는 서버 화이트리스트의 게임플레이/레지스트리 모드와 1:1** (양쪽 일치 필수). T1/T2는 순수 클라이언트. 라이브러리는 "무엇을 켰는가"에 따라 따라온다.

---

## 1. T0 — 코어 필수 (14, 강제·서버 일치) — 결정 044 갱신(2026-06-09)

게임플레이 콘텐츠 + 그 하드 의존 라이브러리. **레지스트리(블록·아이템·엔티티·바이옴)를 추가하므로 서버와 반드시 동일.** 설치기에서 끌 수 없다.

| jar | 역할 | 비고 |
|---|---|---|
| `fabric-api-*` | 전 모드 런타임 기반 | 모든 Fabric 모드 의존 |
| `architectury-*` | Cobblemon 추상화 의존 | lib |
| `owo-lib-*` | Cobblemon 의존 유틸 | lib |
| `accessories-*` | Mega Showdown 착용 슬롯 의존 | lib(게임플레이) |
| `Cobblemon-*` | 포켓몬 엔진(코어) | ★게임플레이 |
| `mega_showdown-*` | 메가/테라/배틀 기믹 | ★게임플레이 |
| `SimpleTMs-*` | TM/TR | ★게임플레이 |
| **`poromon-core-*`** | **PoroMonCore 서버 규칙 엔진 + 커스텀 텍스처/모델(배지·조우권·정수)** | ★**커스텀(자체 빌드)**. CurseForge 메타 해소 불가 → overrides 직접 번들. 결정 044 |
| ~~`eggs-cobblemon-addon-*`~~ | ~~알/부화~~ | ❌ **제거(결정 032)** — 알 시스템 폐기 |
| `LegendaryMonuments-*` | 전설 구조물/소환(비활성됐어도 **레지스트리 일치 필요**) | ★게임플레이·결정 023 |
| `chipped-*` | LM 하드 의존(블록 변형, 레지스트리) | LM 체인 |
| `CobbleFurnies-*` | LM 하드 의존(가구, 레지스트리) | LM 체인 |
| `TerraBlender-*` | LM 하드 의존(바이옴, 레지스트리) | LM 체인 |
| `athena-*` | chipped/cobblefurnies 의존 lib | LM 체인 lib |
| `resourcefullib-*` | chipped 의존 lib | LM 체인 lib |

> 14개 = `server_mod_separation.md` §1(8, eggs 제거) + §1c(1, PoroMonCore) + §1b(5)와 동일 집합. **클라·서버 공통 코어.** (eggs −1, poromon-core +1 → 합계 14 유지.)
> ⚠️ **PoroMonCore는 CustomModelData 텍스처를 클라가 렌더해야 하므로 T0**(끌 수 없음). 빌드 변경 시 서버+클라 동시 재배포. CurseForge/Modrinth에 없어 **설치기가 overrides로 직접 동봉**한다.

---

## 2. T1 — 권장 편의 (기본 ON, 끌 수 있음)

### 2-1. 성능 (없으면 프레임/메모리 손해)
| jar | 역할 | 의존 |
|---|---|---|
| `sodium-fabric-*` | 렌더 최적화(핵심) | — |
| `sodium-extra-*` | Sodium 추가 옵션 | YACL |
| `reeses-sodium-options-*` | Sodium 옵션 UI | sodium |
| `sodiumoptionsapi-*` / `sodiumextras-*` | Sodium 옵션 확장 | sodium |
| `sodium-shadowy-path-blocks-*` | 흙길 렌더 보정 | sodium |
| `sodiumleafculling-*` | 나뭇잎 컬링(성능) | sodium |
| `entityculling-*` | 시야 밖 엔티티 컬링(포켓몬 다수 환경 유리) | — |
| `ferritecore-*` | 메모리 절감 | (서버에도 포함) |
| `lithium-fabric-*` | 틱/AI 최적화 | (서버에도 포함, 클라도 이득) |
| `krypton-*` | 네트워크 최적화 | (서버에도 포함, 접속 이득) |

### 2-1b. 전설 모델 보충 (강권장 — 결정 044)
| jar | 역할 | 의존 |
|---|---|---|
| `complete-cobblemon-collection-myths-and-legends-compat-*` | Cobblemon 1.7.3 **미구현 전설/환상 모델·렌더 보충**(없으면 다수 전설이 substitute 인형으로 보임) | EMF(`entity_model_features`) 권장 동반 |

> ⚠️ 레지스트리를 추가하지 않아 **접속엔 불필요(T0 아님)**하지만, 조우권 콘텐츠가 핵심이라 **기본 ON 강권장**. 끄면 전설 조우 시 모델이 인형으로 표시(게임 동작은 정상). EMF/ETF는 §3 T2이나 이 모델팩과 함께 쓰면 렌더 품질↑.

### 2-2. 정보/아이템 뷰어 (플레이 편의 큼)
| jar | 역할 | 의존 |
|---|---|---|
| `emi-*` | 레시피/아이템 뷰어(메인) | — |
| `emi_enchanting-*` | EMI 인챈트 뷰 | emi |
| `emi_ores-*` | EMI 광석 정보 | emi |
| `EMIProfessions-*` | EMI 주민 거래 정보 | emi |
| `jei-*` | 레시피 뷰어(EMI와 중복 — 운영자 택1 검토) | — |
| `jeed-*` | 효과(포션) 정보 | jei |
| `AdvancedLootInfo-*` | 루트테이블 정보 | — |
| `appleskin-*` | 포만도/채도 표시 | (서버 넣으면 동기화 정확) |
| `enchdesc-*` | 인챈트 설명 | bookshelf |
| `shulkerboxtooltip-*` | 셔커상자 내용 미리보기 | — |
| `tooltipfix-*` | 긴 툴팁 표시 보정 | — |
| `BetterPingDisplay-*` | 탭 핑 표시 | — |
| `stendhal-*` | 채팅/표지판 서식 | — |
| `betteradvancements-*` | 발전과제 화면 개선 | — |
| `tipsmod-*` | 로딩 팁 | — |
| `xaerominimap-*` | 미니맵 (탐험 편의) | — |
| `xaeroworldmap-*` | 월드맵 (탐험 편의) | — |

> ⚠️ **보강(2026-06-12)**: `xaerominimap`/`xaeroworldmap` 2종이 이전 분류표에서 누락돼 있었음(`gen-pack-json.py` 전수 분류 시 발견). 미니맵/월드맵은 탐험 편의라 **T1 기본 ON**. → §6 집계 T1 39→**40** 정정.

### 2-3. 조작/인벤 QoL
| jar | 역할 | 의존 |
|---|---|---|
| `craftingtweaks-*` | 제작 편의(정렬/이동) | balm |
| `InvMove-*` / `InvMoveCompats-*` | 인벤 열고 이동 | — |
| `notenoughanimations-*` | 1인칭 손 동작 | — |
| `dynamiccrosshair-*` | 상황별 십자선 | — |
| `swingthrough-*` | 풀 통과 공격 | — |
| `BetterThirdPerson-*` | 3인칭 카메라 제어 | — |
| `enhanced_attack_indicator-*` | 공격 인디케이터 | — |

### 2-4. UI/메뉴
| jar | 역할 | 의존 |
|---|---|---|
| `modmenu-*` | 모드 설정 메뉴 | — |
| `fancymenu_fabric_*` | 타이틀/메뉴 커스터마이즈(서버 브랜딩 유용) | konkrete, melody |
| `language-reload-*` | 언어 로딩 가속 | — |

> **권장 라이브러리(2-1~2-4 의존)**: `cloth-config`, `yet_another_config_lib_v3`(YACL), `balm`, `konkrete`, `melody`, `bookshelf`. → §4.

---

## 3. T2 — 선택 취향 (기본 OFF, 순수 비주얼/취향)

없어도 게임·접속 무관. 무거운 비주얼 또는 개인 취향. 설치기 기본 해제, 사양 여유/원하면 켜기.

| jar | 분류 | 비고 |
|---|---|---|
| `iris-*` | 셰이더 | 셰이더팩 사용 시만. 무거움 |
| `entity_texture_features-*`(ETF) | 커스텀 엔티티 텍스처 | 리소스팩 의존, 무거움 |
| `entity_model_features-*`(EMF) | 커스텀 엔티티 모델 | 리소스팩 의존 |
| `lambdynamiclights-*` | 동적 조명(들고 있는 횃불 등) | 인기 QoL이나 성능 비용 |
| `particlerain-*` | 날씨 파티클 | 비주얼 |
| `particular-*` | 환경 파티클 | 비주얼 |
| `visuality-*` | 추가 파티클 | 비주얼 |
| `fallingleaves-*` | 낙엽 파티클 | 비주얼 |
| `make_bubbles_pop-*` | 물거품 효과 | 비주얼 |
| `wakes-*` | 물 항적 효과 | 비주얼 |
| `AmbientEnvironment-*` | 환경 사운드/분위기 | 취향 |
| `PresenceFootsteps-*` | 발소리 사운드 | 취향 |
| `citresewn-*`(CIT Resewn) | 커스텀 아이템 텍스처 | 리소스팩 의존 |
| `CraftPresence-*` | Discord Rich Presence | UniLib 의존, 취향 |
| `BHMenu-*` | 메인 메뉴 변경 | 취향 |
| `cherishedworlds-*` | 싱글 월드 즐겨찾기 | 멀티 전용 서버엔 거의 무용 |
| `monsters-in-the-closet-*` | 분위기/사운드(검증) | 취향 |
| `yosbr-*` | 옵션 기본값 유지(Your Options Shall Be Respected) | 취향 |

> **선택 라이브러리(T2 의존)**: `UniLib`(CraftPresence). → §4.

---

## 4. L — 라이브러리 (자동 의존성, 단독 의미 없음)

설치기는 **선택된 모드에 따라** 아래 lib를 자동 포함한다(직접 토글 노출 불필요).

| lib | 누가 요구 | 구분 |
|---|---|---|
| `cloth-config-*` | 여러 설정 화면 모드 | T1 의존 |
| `yet_another_config_lib_v3-*`(YACL) | sodium-extra, iris 등 | T1/T2 의존 |
| `balm-*` | craftingtweaks(T1), netherportalfix(S) | 공통 의존 |
| `konkrete_fabric_*` | fancymenu | T1 의존 |
| `melody_fabric_*` | fancymenu | T1 의존 |
| `bookshelf-fabric-*` | enchdesc | T1 의존 |
| `UniLib-*` | CraftPresence | T2 의존 |
| `Almanac-*` | letmedespawn(S) | 서버측 의존 |
| `prickle-fabric-*` | OpenLoader(S) | 서버측 의존 |

> `athena`·`resourcefullib`는 chipped(T0 코어) 의존이라 **T0에 귀속**(§1). `fabric-api`·`architectury`·`owo`도 코어 lib로 §1.

---

## 5. (S) — 서버전용 (클라 프로필에서 제외 가능, 무해)

클라 폴더(`modpack/client/mods/`)에 동봉돼 있으나 **클라이언트 게임플레이엔 미사용**. 로드돼도 무해(서버 로직 모드라 클라에선 거의 무동작). 간편설치기는 클라 슬림화를 위해 **제외해도 무방**, 또는 클라/서버 패리티를 위해 둬도 됨.

| jar | 역할 | 클라에서 |
|---|---|---|
| `OpenLoader-*`(+`prickle`) | 서버 datapack/resource 로더 | 미사용 |
| `letmedespawn-*`(+`Almanac`) | 디스폰 제어(서버 연산) | 미사용 |
| `netherportalfix-*`(+`balm`) | 포털 링크 보정(서버) | 미사용 |
| `bwncr-*` | 보스 브로드캐스트 억제(서버) | 미사용 |
| `Clumps-*` | XP 오브 병합(서버) | 미사용 |

> ※ `lithium`·`krypton`·`ferritecore`는 서버 화이트리스트지만 **클라에도 성능 이득** → 본 문서는 T1(권장 성능)으로 둔다(서버전용 아님).
> ※ `balm`은 클라측 `craftingtweaks`도 의존 → 클라 유지(서버전용 단독 아님).

---

## 6. 집계

| 구분 | 수 | 설치기 |
|---|---|---|
| T0 코어(강제) | 14 | 항상 (eggs −1, **poromon-core +1**) |
| T1 권장 편의 | 40 | 기본 ON (collection +1, **xaero 2 보강 2026-06-12**) |
| T2 선택 취향 | 18 | 기본 OFF |
| L 라이브러리(자동) | 9 | 의존 해소 (항상 포함) |
| (S) 서버전용(클라 제외 후보) | 5 | 선택 |

> ✅ **전수 정합(2026-06-12)**: 14 + 40 + 18 + 9 + 5 = **86** (실측 일치). `gen-pack-json.py`가 prefix 매핑으로 전수 분류·검증(미분류 0). pack.json = required(14) + optional(58=T1 40 ON/T2 18 OFF) + libraries(9) = **클라 번들 81**, 서버전용 5 제외.

> 합계는 라이브러리 중복 귀속 때문에 단순 합 ≠ 86(실측 클라 jar). **분류 1차 초안** — 간편설치기 스펙 확정 시 토글 단위(개별 vs 묶음)와 정확 개수 재고정 필요(TODO). 의존성 자동 해소는 CurseForge/Modrinth 메타로 검증.
> **결정 044(2026-06-09) 반영**: 실측 클라 86개 = 이전 85 − eggs + poromon-core + complete-cobblemon-collection. T0에 PoroMonCore(커스텀, 끌 수 없음·overrides 번들), T1에 collection(강권장) 추가.

---

## 7. 간편설치기 연계 (추후)

- 배포 채널: CurseForge/Modrinth **PoroMon Official Pack**(`CLAUDE.md` 모드팩 정책). 설치기는 그 위에서 **T1/T2 토글 프로필**을 얹는 형태가 1안.
- 최소 설치(저사양): **T0 + 성능(2-1)**만으로도 정상 플레이.
- 본 분류의 **의존 관계는 각 모드 메타(fabric.mod.json `depends`)로 재검증** 후 설치기 로직에 반영(추측 금지).
- 서버 화이트리스트(25)와의 정합: T0(14) = 서버 게임플레이 코어. 서버 권장 성능/운영(lithium·krypton·ferritecore·letmedespawn·openloader 등)은 **서버측 결정**이며 클라 티어와 독립.

## 8. 관련 문서
- 서버/클라 분리: `server_mod_separation.md`
- 모드팩 목록: `modpack_list.md` · jar 감사: `jar_feature_audit.md`
- 동기화 스크립트: `../../scripts/sync-server-mods.sh`
