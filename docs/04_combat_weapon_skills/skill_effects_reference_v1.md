# 스킬 이펙트·파티클 레퍼런스 v1

> **[STATUS: REFERENCE]** — 6무기 24스킬의 현재 시각/음향 연출 + 전투 수치(계수·쿨·자원) 현행 스냅샷.
> 코드 출처: `combat/skills/<weapon>/*.java` + `seeds/skill_master.csv`. 작성 2026-06-01.

> 용도: 연출 일관성 점검 / 밸런스 패스 기준값 / 리소스팩·치장 연동 참고.

---

## 0. 공통 연출 헬퍼 (`BaseWeaponSkill`)

| 헬퍼 | 용도 |
|---|---|
| `spawnParticleArc(p, data, radius, angle, count)` | 부채꼴 호 (베기/휩쓸기) |
| `spawnParticleCircle(p, data, radius, count)` | 원형 (방어/광역/소환진) |
| `spawnParticleLine(p, data, length, count)` | 직선 (찌르기/낙하) |
| `spawnBeam(p, data, range, step)` | 시선 빔 (투사체/원거리) |
| `spawnImpactEffect(loc, data, count)` | 착탄/폭발 점 |
| `playSound(p, sound, vol, pitch)` | 효과음 |
| `Particle.DUST` + `DustOptions(Color, size)` | 무기별 고유 색 파티클 |

**무기별 시그니처 색 (DustOptions RGB):**

| 무기 | 색 계열 | 주요 RGB |
|---|---|---|
| 검 | 백색·연청 | FLASH(225,240,255) / STEEL(200,220,235) / GUARD(120,200,255) / FINAL(255,248,200) |
| 도끼 | 호박·금색 | AMBER(220,140,40) / GOLD(255,200,70) |
| 창 | 청록·전격 | TEAL(90,220,210) / ELECTRIC(150,210,255) |
| 석궁 | 녹색 볼트 | BOLT(120,255,130) |
| 낫 | 보라·진홍 | PURPLE(160,0,200) / CRIMSON(180,0,60) / VIOLET(100,0,180) |
| 스태프 | 비전 보라 | ARCANE(160,110,255) / STAR(190,160,255) |

---

## 1. 검 (Sword) — 검세 / 백색·연청

| 슬롯 | 스킬 | 계수 | 쿨 | 자원 | 파티클 | 사운드 |
|---|---|---:|---:|---|---|---|
| 1 기본기 | 섬광베기 | 160%(+8%/스택) | 3s | +1 (DASH 전진2) | DUST FLASH 호2.5/1.3·SWEEP_ATTACK·END_ROD | ATTACK_SWEEP(0.9,1.5) |
| 2 이동기 | 연속참 | 70%×3타 | 6s | — (MULTI_HIT3) | DUST STEEL 호2.5/1.8/1.1·SWEEP·CRIT | ATTACK_SWEEP(1.0,1.1) |
| 3 특수기 | 수호반격 | 120% | 10s | BUFF_RESIST3 | DUST GUARD 원1.5·ENCHANTED_HIT·SWEEP | SHIELD_BLOCK·ATTACK_SWEEP |
| 4 핵심기 | 결전일섬 | 345%(+12%/스택) | 16s | 소모3 | DUST FINAL 선·END_ROD·착탄 FINAL+CRIT | ATTACK_CRIT(0.8)·TRIDENT_THROW |

## 2. 도끼 (Axe) — 충격 / 호박·금색

| 슬롯 | 스킬 | 계수 | 쿨 | 자원 | 파티클 | 사운드 |
|---|---|---:|---:|---|---|---|
| 1 기본기 | 철퇴강타 | 210%(+8%/스택) | 4s | +1 | DUST AMBER 호2.5·CRIT | ATTACK_CRIT(0.8) |
| 2 이동기 | 파쇄돌진 | 305% | 5s | DASH 전진3 | DUST AMBER 선4·CLOUD | ATTACK_KNOCKBACK(0.8) |
| 3 특수기 | 불굴자세 | — | 12s | BUFF_RESIST3 | DUST GOLD 원1.3·ENCHANTED_HIT | NETHERITE_EQUIP·ANVIL_LAND |
| 4 핵심기 | 거신추락 | 455%(+10%/스택) | 18s | 소모3 | DUST AMBER 원4.5/2.8·EXPLOSION·CRIT | GENERIC_EXPLODE·IRON_GOLEM_ATTACK |

## 3. 창 (Spear) — 압박 / 청록·전격

| 슬롯 | 스킬 | 계수 | 쿨 | 자원 | 파티클 | 사운드 |
|---|---|---:|---:|---|---|---|
| 1 기본기 | 관통찌르기 | 160%(+5%/스택) | 4s | +1 | DUST TEAL 선5·CRIT | ATTACK_STRONG(0.9,1.3) |
| 2 이동기 | 반월창 | 190% | 6s | — | DUST TEAL/WHITE 호3.0/1.8·SWEEP | ATTACK_SWEEP(1.0) |
| 3 특수기 | 돌파창 | 260% | 8s | DASH 전진5 | DUST TEAL 선5·CLOUD | ATTACK_KNOCKBACK(0.9) |
| 4 핵심기 | 천뢰일창 | 360%(+8%/스택) | 18s | 소모3 | DUST ELECTRIC 선9·ELECTRIC_SPARK | TRIDENT_THUNDER·LIGHTNING_IMPACT |

## 4. 석궁 (Crossbow) — 명중 / 녹색 볼트 (전 스킬 원거리 빔)

| 슬롯 | 스킬 | 계수 | 쿨 | 자원 | 파티클 | 사운드 |
|---|---|---:|---:|---|---|---|
| 1 기본기 | 속사 | 75%×3발 | 3s | +1 (MULTI_SHOT3) | beam BOLT 20·CRIT | CROSSBOW_SHOOT(1.5) |
| 2 이동기 | 회피사격 | 170% | 6s | +1 (DASH 후방2) | beam BOLT 25 | ATTACK_SWEEP·CROSSBOW_SHOOT |
| 3 특수기 | 관통볼트 | 220% | 10s | +1 | beam BOLT 30·ELECTRIC_SPARK | CROSSBOW_SHOOT(0.9) |
| 4 핵심기 | 저격태세 | 420%(+10%/스택) | 20s | 소모3 | beam BOLT 50·END_ROD | CROSSBOW_SHOOT(0.7)·ARROW_HIT |

## 5. 낫 (Scythe) — 그림자 흐름 / 보라·진홍

| 슬롯 | 스킬 | 계수 | 쿨 | 자원 | 파티클 | 사운드 |
|---|---|---:|---:|---|---|---|
| 1 기본기 | 사신베기 | 190%(+5%/스택) | 4s | — | DUST PURPLE/CRIMSON 호2.5/1.5·SWEEP | ATTACK_SWEEP(0.75) |
| 2 이동기 | 월영회전 | 60%×4타 | 5s | +1 (DASH·MULTI_HIT4) | DUST VIOLET 원2.0·SWEEP | ATTACK_SWEEP(1.4) |
| 3 특수기 | 그믐참 | 240%(+8%/스택) | 8s | LIFESTEAL6 | DUST PURPLE/CRIMSON 호3.5/2.0·WITCH | WITHER_SHOOT(1.3) |
| 4 핵심기 | 처형낫 | 280% | 16s | 소모3 (EXECUTION200) | DUST PURPLE 선·SOUL_FIRE_FLAME·SWEEP | WITHER_HURT·ATTACK_CRIT(0.6) |

## 6. 스태프 (Staff) — 마력 충전 / 비전 보라 (전 스킬 원거리/광역)

| 슬롯 | 스킬 | 계수 | 쿨 | 자원 | 파티클 | 사운드 |
|---|---|---:|---:|---|---|---|
| 1 기본기 | 마력탄 | 150% | 3s | +1 | beam ARCANE 20·WITCH | EVOKER_CAST(1.4) |
| 2 이동기 | 속성폭발 | 255% | 8s | +1 (BURST_ON_HIT25, AoE2.5) | beam ARCANE 18·착탄 ARCANE+WITCH | EVOKER_CAST(1.0) |
| 3 특수기 | 마력쇄도 | 220% | 10s | +1 (DASH 후방, 원4) | 원 ARCANE 4.0·WITCH | EVOKER_CAST(0.8)·AMETHYST_CHIME |
| 4 핵심기 | 별빛쇄도 | 405%(+10%/스택) | 20s | 소모3 | beam STAR 22·END_ROD | EVOKER_CAST(1.2)·AMETHYST_CHIME |

---

## 7. 관찰 / 정합 메모

- **자원 단계 (DL-122 적용):** 전 무기 기본 **3단계** / 유지형 각인(`_retained_01`) **6단계**. (구: 창·스태프 5 → 3 통일.) finisher 소모 천뢰일창·별빛쇄도 5→3 동반. 계수는 DL-123 균형 패스 반영. **잔여:** 창·스태프 임계(2단계)·만충(3단계) 2단 구조가 캡3에서 1스택 차로 붕괴 — 재설계 필요(`dps_balance_pass_v1.md`).
- **스택 스케일 계수(`scaledDamageWithStacks`):** 검 0.08 / 도끼 0.08·0.10 / 창 0.05·0.08 / 석궁 0.12 / 낫 0.05·0.08 / 스태프 0.10(별빛쇄도). 검 결전일섬·석궁 저격태세는 캡6 폭주 차단 위해 0.15→0.12·0.12→0.10 하향(DL-123).
- **원거리/근접:** 석궁·스태프는 전 스킬 원거리(beam/projectile). 평타는 별도(DL-121 — 원거리 무기 투사체 평타 추가). 검·도끼·창·낫은 근접 호/선.
- **연출 일관성:** 무기별 시그니처 색 통일됨(검=백청, 도끼=호박, 창=청록, 석궁=녹색, 낫=보라, 스태프=비전보라). 핵심기는 공통적으로 END_ROD/EXPLOSION/SOUL_FIRE 등 강조 파티클 + 2중 사운드.

> 본 문서는 현행 스냅샷. 밸런스 변경(자원 단계·계수) 적용 시 이 표를 갱신한다.
</content>
