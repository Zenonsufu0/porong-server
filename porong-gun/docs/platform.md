# porong-gun 플랫폼 / 기술 스택

> 비전 SoT: [`concept.md`](concept.md) · 설계 문서 맵은 concept.md 「문서 구조」 참조.
> **상태:** 구상 — 로더·모드 스택은 *잠정 확정*, 최종은 착수 0순위 통합 검증 게이트에서.

## 로더 — 1.20.1 Forge (잠정 확정)
> **⚠️ 로더 방향 전환(2026-06-08, INBOX-005 뒤집음):** ~~Fabric 1.21.1~~ → **1.20.1 Forge.** 이유 = 우리 정체성의 간판인 **스캐브([TACZ] NPCs)·전술이동(Tactical Movement Renewed)이 1.21.1엔 존재하지 않음**(Fabric/NeoForge 무관, 둘 다 1.20.1 Forge/NeoForge 전용). "기존 모드 최대 활용" 원칙상 생태계가 사는 버전으로 내려간다.

- **로더: 1.20.1 Forge / Java 17.** 1.20.1 Forge = 모딩 역사상 최다 지원·최고 성숙 버전 → TaCZ 원조·애드온·의료·POI 거의 전부 네이티브.
  - **대가:** porong-mon의 Fabric 1.21.1 환경 재사용(loom·빌드·노하우) 포기. 단 porong-gun은 **완전 독립**이라 손실 작음. 구버전(Java 17)이나 생존/총겜엔 1.20.1이 외려 표준.
  - 1.20.1엔 NeoForge도 있으나 **Forge가 최대 공통분모**(모든 핵심 애드온이 Forge 지원). 장기 정출 시 신버전/NeoForge는 그때.

## 모드 스택 (1.20.1 Forge)
범례: ✅ 존재 확인(웹) / 🔲 버전 핀·선택 필요 / ⭐ 핵심.

| 역할 | 모드 | 상태 | 비고 |
|---|---|---|---|
| ⭐ 총기 | **Timeless and Classics Zero (TaCZ 원조)** | ✅ | Forge판(Refabricated/NeoForge 포트 아님). 백업 = Vic's Point Blank(총만). GPLv3 — 내부 Mixin 지양·공개 API만. |
| ⭐ 스캐브 | **[TACZ] NPCs** (1.2.0) | ✅ | 랭크별 Rookie→Expert 총 쏘는 인간형. 보조 = TaCZ: Simple Enemy / Insurgents. **← 1.20.1 전환 핵심 이유.** |
| ⭐ 전술 이동 | **Tactical Movement Renewed** | ✅ **클라 전용** | 린·프론·슬라이딩·3인칭 장전 애니. **← 동일 전환 이유.** ⚠️ **데디 서버 X**(LocalPlayer 크래시) → 플레이어가 *클라에만* 설치(`.local/client-mods/`). |
| ⭐ 부위 데미지·의료 | **First Aid (원조)** | ✅ | 부위별 8영역 HP·자연재생 제거·붕대/부목/모르핀·다리 둔화. 부위 데미지 모드의 원전. |
| ⭐ 방어구 | **[TaCZ] Tactical 3D Armor** | ✅ | Light/Medium/Heavy 방탄 등급. 스캐브 약탈·무게·First Aid 부위 보호와 연동. |
| ⭐ 무기 청사진 | **TaCZ Weapon Blueprints** (**1.0.3-beta7**) | ✅ | 루트 드랍 해금·레시피 영구 학습·`/gg clearRecipes`(시즌 리셋). *총만* 해금(탈출 청사진은 커스텀). ⚠️ **1.0.2는 TaCZ 1.1.8과 Mixin 충돌 → beta7 필수.** |
| ⭐ POI/구조물 | **The Lost Cities** (+ **Underground Bunkers**) | ✅ | 폐도시·건물 자동 생성(POI 본체). rarity 조정해 드문 폐도시로. 고티어 지정 = 우리 루트 티어링. **← ❌ ChaosZPack 드랍**(Create 등 8모드 강제 의존 → 경제·톤 충돌, 아래). |
| 적 보강(선택) | **ChaosZProject: Bandits!** | ✅ | TaCZ 총 든 약탈자(독립 애드온, ChaosZPack 의존 X) = 스캐브 외 적 다양화. |
| 좀비 블록 파괴 | **Zombies Break & Build** (forge-1.20.1-1.5.0) | ✅ **핀** | 타겟까지 경로 블록 파괴(히트박스 기반) = 우리 기지 습격 설계와 정확히 일치. (Improved Mobs는 과해서 선택지로만.) |
| 호드 나이트 | **Undead Nights** (2.0.4) | ✅ **핀** | 설정 풍부(규모·캡·간격). **Demolition Zombies(TNT로 방벽 폭파) + Elite Zombies** 내장 = 결전 특수/강화 좀비에 매핑. porongun-core가 config/명령으로 상시 야간+결전 오케스트레이션. |
| 클레임·파티 | **Open Parties and Claims (OPaC)** | ✅ **핀** | **API**(porongun-core가 보호 룰 hook) + **파티 = 정식 연합(권한 그룹)에 매핑.** 보호 *기하*(코어 반경·Y30·다중방벽·레벨)는 porongun-core 커스텀. |
| 폭약(관통) | **porongun-core 커스텀** | — **확정** | "방벽만 특수 폭약에 뚫림"은 우리 룰이라 자체 제작(레퍼런스 = Mekanism Breaching Explosive). 제3자 폭약 모드(More Explosives 등)는 일반 폭발물 flavor용 선택. |

- **PvE 강화(선택):** Improved Mobs / Apocalypse — 좀비 외 몹 강화. 스캐브와 역할 충돌 주의(좀비에 갑옷·총은 지양).

### 🔫 무기 확장 (gun pack — 선택)
- **본판 TaCZ에 기본 gun pack 포함** → 없어도 총 나옴. MVP엔 **base pack만**으로 충분(총 종류 통제 = 청사진/티어 설계 단순).
- 더 원하면 테마 맞는 **1개**만: **Apocalypse Gun Pack**(포스트아포칼립스 정합) / Modern Warfare / **LesRaisins Append Pack**(총+갑옷+전술템, 3.8M DL — 단 이건 코드 모드라 mods/에). Perseus·ARIPS·Weapons and Tactics 등 다수.
- ⚠️ gun pack은 대개 **TaCZ 팩(데이터)** = mods/ 아닌 TaCZ 팩 폴더로 들어감(CurseForge 분류 'Addons/Customization'). LesRaisins처럼 'mc-mods'면 코드 모드. 종류 구분해서 배치.
- **QoL(선택):** `TaCZ addon`(MerrySnow, 5.9M DL) — 부착물·탄약 호환 표시·필터.

### ⚠️ 버전 정합 / 의존성 (통합 검증서 확인됨)
- **애드온은 TaCZ 본판 버전에 묶임.** 본판 `tacz-1.20.1-1.1.8` → **Weapon Blueprints 1.0.2는 Mixin 충돌(GunSmithTableRecipe), 1.0.3-beta7로 해결.**
- **❌ ChaosZPack은 Create·Furniture·Horror·MineTraps·Survival Instinct·Doomsday·Immersive Weathering를 *필수 의존*으로 강제** → Create 자동화가 우리 경제(희소성·노출 리스크)를 무력화 + 톤 충돌 → **드랍.** POI는 Lost Cities(+Underground Bunkers)로 충분.

## 중심 모드 — `porongun-core` (Forge, group `kr.porong.gun` 예정)
총·좀비AI·습격·갑옷·총 청사진은 기존 모드가 담당. 우리는 **서바이벌 시스템 + 룰 + 오케스트레이션**을 만든다.
- **"오케스트레이션만"은 과소평가** — 실제 커스텀 표면이 묵직: 무게 점유칸+유리 GUI · 상자 식별 GUI · 조건부 기지 보호(OPaC hook+관통 폭약) · Y25 부분 리셋 · 화폐+상점·메뉴 GUI · 신규 보호막 · 결전 오케스트레이션 · 무게/부위 합산 · 탈출 청사진 테크.
- **📌 포트폴리오 관점:** 이 프로젝트는 포트폴리오 성격도 있어 묵직한 porongun-core(커스텀 GUI·시스템)는 *부담이 아니라 실력 증명 자산.* 리스크는 "무겁다"가 아니라 **"1인이 한 번에 다 못 푸니 MVP 순서가 필요"**일 뿐 → 단계적 출시로 해소(concept 「스코프 원칙」).

## ✅ 통합 검증 결과 (착수 0순위 — 통과)
**2026-06-08, dev 서버(`.local/server/`)에 실제로 올려 부팅 검증 — `Done (9.3s)`.** 로더 **1.20.1 Forge 정식 확정.**
- **최종 스택 = 서버 19 모드 + 클라 1(Tactical Movement).** (모드팩 "Escape From Porong 0.1.0" → ChaosZPack 클러스터 15개 드랍 후 린 스택.)
- **서버 핵심:** TaCZ 1.1.8 · TACZ NPCs · TaCZ Weapon Blueprints(beta7) · Tactical 3D Armor · TaCZ addon · First Aid · Lost Cities · LC²H · Underground Bunkers · Undead Nights · Zombies Break & Build · OPaC · Bandits · Vic's Point Blank · (libs: GeckoLib·Kotlin·SmartBrainLib·Fzzy Config·Quantified).
- **확인된 작동:** Lost Cities POI 프로파일 생성 / Undead Nights `demolition_zombie`(6%)·`elite_zombie`(3%) = 결전 좀비 / Blueprints·Bandits init.
- **남은 검증 포인트(런타임서):** Tactical 3D Armor × First Aid 부위 보호 연동, OPaC API로 관통 폭약 hook, Undead Nights config로 결전 구동, Vic's Point Blank를 TaCZ와 병행할지(둘 다 총 모드 — 거취 미정).
