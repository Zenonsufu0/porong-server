# 모드 번들 재배포 라이선스 점검 (결정 046 §9-1)

> 점검일: 2026-06-12. 대상: `modpack/client/mods` **84 jar**(xaero 2 제외 후, 결정 047). 도구: `scripts/check-mod-licenses.py`(jar 내 `fabric.mod.json`/`quilt.mod.json` license 필드 추출·분류).
> 배경: **결정 046 = 모드 전부 exe 번들**. 번들 = 우리가 jar 를 **직접 재배포** → 모드별 라이선스가 적용된다. (CF manifest 방식은 "CF→유저 다운로드 위임"이라 재배포 아님 = 라이선스 무관이었으나, 번들로 전환하며 숙제가 생김.)

## 요약

| 분류 | 수 | 의미 |
|---|---|---|
| **OK** | 63 | MIT/Apache/BSD/ISC/MPL/LGPL/CC0/Unlicense 등 — 번들 재배포 허용 명확 |
| **COPYLEFT** | 1 | GPLv3(wakes) — 번들 가능하나 소스공개 의무·배포물 영향 |
| **CHECK** | 20 | ARR/커스텀/필드없음 — **개별 확인 필요**(번들 막힐 수 있음) |

> (총 84. xaero 미니맵/월드맵 2종은 ARR로 **제외 결정**됨 — 결정 047. CHECK 22→20.)

> ✅ 핵심 다수가 OK: Cobblemon(MPL-2.0)·Fabric API(Apache-2.0)·SimpleTMs(MIT)·LegendaryMonuments(MPL-2.0)·architectury·owo·sodium 옵션류·EMI/JEI·iris(LGPL) 등.

## CHECK 22개 세부 (번들 전 확인 대상)

| jar | 티어 | license | 메모 / 조치 |
|---|---|---|---|
| `zenon-mon-core` | T0 | ARR | **우리 모드 → 무관**(저작자=우리). 번들 자유 |
| `mega_showdown` | **T0 필수** | (필드 없음) | ★ 핵심. 소스레포/CF "modpack permission" 확인 필수. 빠질 수 없음 |
| `chipped` | **T0 필수** | Terrarium Licence | ★ LM 의존(필수). Terrarium 약관 확인 |
| `complete-cobblemon-collection` | T1 강권장 | ARR | 모델 보충(끌 수 있음). ARR → permission 확인 / 불가 시 선택 제외·다운로드 분리 |
| `balm` | L(필수 의존) | ARR | craftingtweaks/netherportalfix 의존 lib. permission 확인 |
| `netherportalfix` | (S) | ARR | 서버전용 → 클라 번들 제외됨(영향 적음). 서버 배포만 |
| `bwncr` | (S) | (필드 없음) | 서버전용 → 클라 번들 제외. 서버 배포만 |
| ~~`xaerominimap`~~ | — | ARR | ⛔ **제외됨(결정 047)** — TP로 보완, 번들 미포함 |
| ~~`xaeroworldmap`~~ | — | ARR | ⛔ **제외됨(결정 047)** |
| `craftingtweaks` | T1 | ARR | permission 확인 |
| `stendhal` | T1 | ARR | permission 확인 |
| `BetterThirdPerson` | T1 | ARR | permission 확인 |
| `BHMenu` | T2 | ARR | permission 확인 |
| `BetterAdvancements` | T1 | "Don't Be a Jerk" 비상업 | **비영리 서버면 OK**, 수익화/후원 시 주의 |
| `sodium` | T1 | Polyform-Shield | 비경쟁 용도 자유(모드팩 OK 일반적). 약관 확인 |
| `sodiumleafculling` | T1 | ARR | permission 확인 |
| `entityculling` | T1 | tr7zw Protective | tr7zw 약관(모드팩 보통 허용) 확인 |
| `notenoughanimations` | T1 | tr7zw Protective | 〃 |
| `fancymenu` | T1 | DSMSLv3 | 커스텀. 약관 확인 |
| `lambdynamiclights` | T2 | Lambda License | 커스텀(보통 모드팩 허용) 확인 |
| `make_bubbles_pop` | T2 | Custom | 약관 확인 |
| `jeed` | T1 | GLP3(=GPL3?) | 표기 모호 → COPYLEFT 가능. 소스 확인 |

## 권고 (번들 전 액션)

1. **우리 모드·서버전용·제외분 빼기**: zenon-mon-core(우리꺼), netherportalfix·bwncr(클라 번들 미포함), xaero 2(결정 047 제외) → 실질 CHECK = **클라 번들에 들어가는 ~16개**.
2. **핵심 필수 우선 확인**: `mega_showdown`·`chipped`·`balm`(의존) — 빠질 수 없으므로 저작자/CF **modpack permission** 또는 소스 라이선스 전문 확인. 대부분 "공개 모드팩 허용"이나 **자체 exe 번들**은 회색지대 → 필요 시 저작자 문의.
3. **순수 편의 ARR**(xaero·craftingtweaks·stendhal·BetterThirdPerson·sodiumleafculling 등): permission 확인. 불가 모드는 ① 선택(T2)로 내려 기본 OFF ② **그것만 다운로드 분리**(자체 호스팅) ③ 대체 모드로 교체.
4. **비상업 조건**(`BetterAdvancements`): 서버 수익화 계획 시 재검토.
5. **커스텀 보호 라이선스**(tr7zw·Polyform·DSMSLv3·Lambda): 대개 "모드팩 포함 허용·리버스/재판매 금지"라 **번들 OK 가능성 높음** — 각 약관 1줄 확인.

> ⚠️ 본 분류는 **license 필드 기계 추출**이다. 최종 판단은 각 모드의 라이선스 전문 + CF/Modrinth 페이지 "Allowed in modpacks?" 확인. ARR이라도 modpack permission 명시면 공개 배포 가능한 경우가 많다.

## 재실행

```bash
python3 scripts/check-mod-licenses.py
```

모드팩 업데이트(jar 추가/버전 변경) 시 재실행해 CHECK 변동 확인.
