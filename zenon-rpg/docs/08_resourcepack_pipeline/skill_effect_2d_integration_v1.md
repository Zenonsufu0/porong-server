# 2D 스킬/보스 이펙트 — 리소스팩 통합 설계 v1

> **[STATUS: 부분 구현 — 무기 9종 완료 / 보스·임팩트 보류]** — 배경 제거된 RGBA 이펙트 PNG를 인게임에 ItemDisplay로 통합. 무기 9개 효과 인게임 확정(커밋 d939a41→7e9f0f1). 보스 텔레그래프 4종은 보스 패턴 런타임 미배선으로 보류. 공용 임팩트링은 흰 코어 과다로 보류.

> **구현 결과 (인게임 확정):**
> - **오리엔테이션 3패턴** (`EffectDisplayService`): `spawnSlash`(빌보드 정면+3D비행, 검), `spawnGroundTravel`(바닥 평면 비행, `followPitch`로 조준 상하 추종 — 창·낫좌클릭·석궁), `spawnDecal`(바닥 고정 장판 — 도끼균열·스태프원·낫처형표식).
> - **모델 = 코너 0-16 quad** (중앙정렬은 ItemDisplay 센터링과 충돌해 스케일배수 오프셋 발생 → 코너로 확정). 텍스처 `poro:item/effect/` 경로(`textures/effect/`는 클라 캐시 함정), 256px 패딩, carrier=`paper` cmd 400101~108.
> - **운영 토글** `NO_SKILL_COOLDOWN`(테스트 쿨 0초) 추가.
> - **보류**: 보스 텔레그래프 400201~204(런타임 미배선), 공용 임팩트링 400901(흰색 40%).
> - 리소스팩 자산은 `assets/`(gitignored) 로컬+HTTP 서버. 플러그인 cmd 참조만 git 추적.

> 기준일: 2026-06-01 | 서버 Paper 1.21.10 / pack_format 34 (1.21.4+ 아이템 정의)

---

## 0. 목표 / 범위

- 입력: `assets/source/effects_clean/` 13개 RGBA PNG(진짜 alpha 투명).
- 목표: 모드 없이 **바닐라 Paper + 리소스팩**만으로 2D 이펙트를 월드에 띄운다(ModelEngine/BetterModel은 1차 시즌 보류 — `final_master_plan §6`).
- 비범위: 신규 이펙트 아트 제작, 파티클 시스템 전면 교체.

---

## 1. 핵심 결정 — ItemDisplay 빌보드 + 평면 모델 + custom_model_data

마인크래프트에서 임의 PNG를 월드에 띄우는 바닐라 경로는 사실상 **ItemDisplay(아이템 디스플레이 엔티티)** 뿐이다(커스텀 파티클은 바닐라 미지원).

**채택안:**
1. 각 이펙트 PNG → **평면 `item/generated` 모델**(아이템 아이콘처럼 1장 텍스처 평면).
2. 기존 무기 관례 그대로 **`custom_model_data` select 아이템 정의**로 등록(아래 §4).
3. 플러그인이 스킬/보스 발동 시 **ItemDisplay**를 좌표에 스폰 → 운반 아이템(cmd 부여) 장착 → 빌보드/방향/스케일 지정 → N틱 후 제거.

**근거:** 모드 불필요, 기존 리소스팩 시스템(1.21.4+ cmd select) 재사용, 파티클과 병행 가능, 보스 경고 장판처럼 "정확한 범위 시각화"에 평면 텍스처가 파티클보다 명확.

---

## 2. PNG → 용도 매핑 + custom_model_data 배정

기존 대역(무기 100xxx / 200xxx / 300xxx)과 겹치지 않게 **이펙트 = 400xxx 신규 대역**. 대역 규칙: `4001xx`=무기, `4002xx`=보스 텔레그래프, `4009xx`=공용.

설계 의도: **무기 6종 각각 "핵심기" 시그니처 1개** + 스태프·낫만 **두 번째 효과** + 공용 임팩트 1 + 보스 텔레그래프 4.

| cmd | 파일(effects_clean) | 매핑(스킬) | 발동 클래스 | 방향 모드 |
|---|---|---|---|---|
| 400101 | effect_sword_final_slash_white | 검 **결전일섬**(핵심기) 직선 베기 | `SwordFinalStrikeSkill` | DIRECTIONAL(시전 정렬) |
| 400102 | effect_spear_thunder_line | 창 **천뢰일창**(핵심기) 직선 | `SpearThunderstrikeSkill` | DIRECTIONAL |
| 400103 | effect_crossbow_sniper_bolt_green | 석궁 **저격태세**(핵심기) 빔 | `CrossbowSniperSkill` | DIRECTIONAL(시선 빔) |
| 400104 | effect_staff_starlight_beam | 스태프 **별빛쇄도**(핵심기) 빔 | `StaffStarburstSkill` | DIRECTIONAL(시선 빔) |
| 400105 | effect_staff_arcane_circle | 스태프 **마력쇄도**(이동기/광역) 비전 원 | `StaffArcaneRushSkill` | GROUND_FLAT(자기중심 바닥) |
| 400106 | effect_axe_giant_fall_crack | 도끼 **거신추락**(핵심기) 착탄 균열 | `AxeColossalDropSkill` | GROUND_FLAT(착탄점) |
| 400107 | effect_scythe_crescent_slash | 낫 **사신베기**(기본기) 광역 호 베기 | `ScytheDeathSlashSkill` | DIRECTIONAL |
| 400108 | effect_scythe_execution_mark | 낫 **처형낫**(핵심기) 대상 표식 | `ScytheExecutionSkill` | GROUND_FLAT(대상 발밑) |
| 400901 | effect_impact_ring_amber | **공용** 임팩트 링(만충 폭발/강타 착탄) | 만충 스파이크·착탄 공용 | GROUND_FLAT |
| 400201 | effect_boss_warning_circle_red | 보스 **원형 장판** 경고 | 보스 패턴(server-config) | GROUND_FLAT |
| 400202 | effect_boss_warning_line_red | 보스 **직선** 경고 | 보스 패턴 | GROUND_FLAT(방향) |
| 400203 | effect_boss_warning_cone_red | 보스 **부채꼴** 경고 | 보스 패턴 | GROUND_FLAT(방향) |
| 400204 | effect_boss_safezone_blue | 보스 **안전지대** | 보스 패턴 | GROUND_FLAT |

**판단 근거(둘만 비자명했음):**
- `staff_arcane_circle` → **마력쇄도**: 스태프 핵심기는 빔(starlight)으로 이미 배정. 남은 원형 룬은 마력쇄도(원형 4블럭 광역 노바)의 모양과 일치. `별빛쇄도`(빔)와 역할 분리.
- `scythe_crescent_slash` → **사신베기**(기본기): 처형낫(핵심기)은 표식(execution_mark)으로 배정. 초승달 호 베기는 낫 기본기 사신베기(전방 광역 호 150°)와 일치. 그믐참(cone 60°)은 별도 효과 없음 → 사신베기 효과 재사용 가능.

> 무기당 핵심기 1개 원칙이라, 핵심기 외 스킬(이동기/특수기 등)은 1차에서 기존 파티클 유지. 스태프·낫만 2번째 효과를 받는 비대칭은 의도된 시그니처 강조.

---

## 3. 리소스팩 구조 (경로)

> `assets/`는 현재 gitignored(로컬 전용, 배포는 별도 HTTP 서버). 아래는 로컬 작업본 경로.

```
assets/export/resourcepack/assets/
  poro/
    textures/effect/effect_*.png          ← effects_clean 13개 복사
    models/effect/effect_*.json           ← 평면 generated 모델 13개
  minecraft/items/<carrier>.json          ← custom_model_data select 추가
```

- **운반 아이템(carrier) = `minecraft:firework_star` (확정)**: `paper`는 이미 cmd `400001~400010`(소모품)·490/491/492/500xxx를 써서 400xxx 충돌 → 전용 미사용 아이템으로 격리. `items/firework_star.json`에 이펙트 cmd 케이스만 둠(다른 게임 아이템과 무충돌). 플레이어 미지급.

---

## 4. 모델 / 아이템 정의 형식

**평면 모델** `poro/models/effect/effect_sword_final_slash_white.json`:
```json
{
  "parent": "minecraft:item/generated",
  "textures": { "layer0": "poro:effect/effect_sword_final_slash_white" }
}
```
> `item/generated`는 알파를 픽셀 단위로 평면화한다. 페더된 반투명 글로우가 살아있는지 인게임 확인 필요(§6).

**아이템 정의** `minecraft/items/paper.json`(기존 select에 case 추가):
```json
{ "when": "400101", "model": { "type": "minecraft:model", "model": "poro:effect/effect_sword_final_slash_white" } }
```

---

## 5. 플러그인 설계 — EffectDisplayService

신규 서비스 1개로 캡슐화(`kr.zenon.rpg.combat.effect.EffectDisplayService`).

```java
// 빌보드/방향 모드
enum EffectOrientation { BILLBOARD_VERTICAL, GROUND_FLAT, DIRECTIONAL, FIXED }

ItemDisplay spawn(EffectId id, Location loc, EffectOrientation orient,
                  double scale, int lifeTicks);
void scalePop(ItemDisplay d, double from, double to, int ticks);  // 등장 연출
```

구현 골자:
- `world.spawn(loc, ItemDisplay.class)` → `setItemStack(carrierWith(cmd))`.
- `setBillboard(...)`: 경고/표식 = `VERTICAL`(플레이어 향함), 장판/원형 = `FIXED`+바닥 수평 회전(GROUND_FLAT), 베기/직선 = `FIXED`+시전 방향 정렬(DIRECTIONAL).
- `setBrightness(new Brightness(15,15))`: 풀밝기(글로우).
- `setTransformation(...)`: scale·rotation. 등장 시 `setInterpolationDuration` + 다음 틱 `setTransformation`으로 스케일-팝.
- `setViewRange`, `setTeleportDuration`(이동 빔용).
- 수명: `Bukkit` 스케줄러로 `lifeTicks` 후 `remove()`. (만일을 위해 스폰 시 PDC 태그 + 청소 스윕.)

**훅 지점:**
- 무기 핵심기 `execute()`에서 파티클과 **병행** 호출(파티클 유지 + 디스플레이 추가).
- 보스 경고: server-config MythicMobs 패턴 → 플러그인 패턴 스케줄러 hook(`boss_pattern_mythic_mapping`)에서 장판/직선/부채꼴 경고를 GROUND_FLAT으로 스폰.

---

## 6. 렌더링 주의 (인게임 검증 항목)

| 항목 | 내용 |
|---|---|
| 반투명 글로우 | `item/generated` 월드 렌더가 페더 alpha를 블렌딩하는지(가장자리 부드러움 유지). 깨지면 모델 렌더타입/별도 셰이더 검토. |
| 밝기 | `brightness 15/15`로 야간·동굴에서도 풀밝기. 미설정 시 주변광 따라 어두워짐. |
| 방향 정렬 | 베기/직선은 시전 yaw/pitch 정렬 필요(빌보드면 항상 정면이라 직선감 상실). FIXED + 수동 rotation. |
| 성능 | 엔티티 스폰/제거 비용. 동시 다발 스킬 시 상한·짧은 수명(5~12틱) 권장. 누수 방지 청소 스윕. |
| Z-파이팅/지면 | 장판은 바닥 +0.02 띄워 깜빡임 방지. |
| view_range | 광역 보스 경고는 충분한 view_range로 멀리서도 보이게. |

---

## 7. 파티클 정책

- 1차: 디스플레이를 **추가**(파티클 유지). 디스플레이가 핵심 비주얼, 파티클이 보조 잔광.
- 검증 후 무기별로 파티클 과다분만 정리(완전 대체는 비권장 — 파티클은 저비용 잔광에 유리).

---

## 8. 단계 계획

1. **PoC (1무기)**: 검 결전일섬 1종 — 텍스처/모델/아이템정의 + EffectDisplayService 최소 구현 + 핵심기 hook. 인게임 §6 검증.
2. **무기 8종 확장**: 핵심기/주요기 매핑 적용.
3. **보스 경고 4종**: 패턴 스케줄러 hook(GROUND_FLAT 장판/직선/부채꼴/안전지대). 원샷 방지·텔레그래프와 연계.
4. **공용 임팩트**: 만충/착탄 공용 링.

---

## 9. 미해결 / 결정 필요

- ~~PNG↔무기 매핑 확정~~ ✅ §2 확정(핵심기 6 + 스태프/낫 2번째 + 공용 + 보스 4, 방향 모드 포함).
- ~~carrier 아이템 선택~~ ✅ `firework_star` 전용 확정(paper 400xxx 충돌 회피).
- 반투명/렌더타입 인게임 실측(generated가 페더 글로우를 살리는지).
- 보스 경고를 디스플레이로 할지, server-config MythicMobs 자체 연출로 둘지(패턴 모듈과 책임 분담).
- 애니메이션 깊이(스케일-팝만 vs 프레임 시퀀스 — 후자는 PNG 시트 필요).

---

## 관련

- 배경 제거 도구: `assets/source/effect_bg_remove.py`(로컬), 산출 `effects_clean/`.
- 전투 연출 현행(파티클): `BaseWeaponSkill`(`spawnParticle`/`spawnBeam`/`spawnSlashEffect`).
- 아이템 모델 관례: `assets/export/resourcepack/assets/minecraft/items/*.json`(cmd select).
- 보스 패턴 hook: `boss_pattern_mythic_mapping`, `zenon-rpg/docs/07_boss_pattern_modules/`.
