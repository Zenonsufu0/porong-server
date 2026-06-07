# 설계 결정 로그 (Decision Log)

> **[STATUS: DRAFT]** — PHASE 2~4 수정 작업 기록. 각 항목은 "무엇을 / 왜 / 근거 문서" 형식.

---

### DL-125 자원 충전 명중 조건 정합 — 발동 무관 충전 버그 수정 (DL-124 라이브 테스트 후속)

**배경:** DL-124 인게임 테스트 중 사용자 보고 — "모든 스킬 쓰면 명중과 무관하게 스택이 쌓인다". 정본 §4 충전 조건은 전 무기 "**명중 시** 1단계"인데, 빌더 스킬 일부가 발동만 하면(허공 헛스윙 포함) `gainStack`을 무조건 호출하고 있었다.

**조사:** `gainStack` 호출 10곳 중 **명중 판정 있음**(석궁 속사/회피사격/관통볼트 `if(hit[0])`, 낫 사신베기 `if(!targets.isEmpty())`)과 **무조건 호출**(검 섬광베기·도끼 철퇴강타·창 관통찌르기·스태프 마력탄/속성폭발/마력쇄도)이 혼재. 후자 6종이 헛스윙에도 충전 → "충전" 의미 상실.

**수정 (custom-plugins/poro-rpg, 6개 빌더):** 크로스보우·낫이 이미 쓰던 명중 판정 패턴으로 통일.
- 컬렉션 히트박스(arc/line/burst): `var targets = ...; targets.forEach(deal); if (!targets.isEmpty()) gainStack(...)`. (검 섬광베기·도끼 철퇴강타·창 관통찌르기·스태프 마력쇄도)
- Optional 히트박스(projectileRaycast): `gainStack`을 `.ifPresent` 내부로 이동. (스태프 마력탄·속성폭발)
- 돌진·이동(dashForward/dashBackward)은 명중과 무관하게 유지(이동기 기능). 충전만 명중 조건.

**검증:** 빌드 통과·재기동 무에러. 명중 시 충전·헛스윙 시 미충전은 사용자 인게임 재확인.

**스태프 충전 범위 정합 (사용자 결정 — LMB만):** 정본 §4 스태프 충전은 "**LMB 마법 명중**"만인데 코드는 마력탄(LMB)+속성폭발(특수기)+마력쇄도(이동기) 3개 모두 충전했다. → **속성폭발·마력쇄도의 `gainStack` 제거, 마력탄만 충전원으로 확정.** `dps_balance_pass_v2` 모델(스태프=마력탄 LMB로 충전, 7회/사이클)과 정확히 일치해 모델 변경 불필요. (도끼는 반대로 §4 "특수기 명중 2단계"가 코드 미구현 — 철퇴강타만 충전. 별도 후속.)

**관련:** DL-124(만충 스파이크), `combat_balance_v2 §4`(충전 조건), INBOX-013.

---

### DL-124 만충 스파이크 하이브리드 — 임계/만충 구조 재설계 + DPS 보정 (DL-123 후속, INBOX-013)

**배경:** DL-123이 "최우선"으로 남긴 "창·스태프 임계(2단계)·만충(3단계) 2단 구조 캡3 붕괴" 재설계. 조사 결과 **근본 원인 발견** — 정본 §4의 "중간 임계점 효과(창 +15%·스태프 +10% 지속)"와 "만충 효과(핵심기 +35~45%)"가 **코드에 구현된 적이 전혀 없었다**(`combat/` 전역 grep으로 임계 배율 1.15/1.10·만충 배율 1.30~1.45 부재 확인). 실제 구현은 전 무기 동일한 매끄러운 per-stack 누진(`scaledDamageWithStacks`)뿐. 따라서 `dps_balance_pass_v1`의 DPS 수치(스프레드 19.3%, "창 1위·임계 상시 ON")는 **존재하지 않는 배율을 가정한 허구**였다. 추가로 천뢰일창 쿨 18s·저격태세 20s 표기도 오기(실코드 15s/14s, 정본 §3과 일치).

**재모델링 (combat-balance, `dps_balance_pass_v2.md`):** 실코드 계수만으로 재산출한 **진짜 현재 스프레드 = 37.6%**(석궁 11,126 +14.3% ~ 스태프 8,088 −16.9%). 실제 1위 원인은 임계가 아니라 **석궁의 짧은 쿨(14s)**, 바닥은 **스태프 긴 쿨(20s) + 마력탄 per-stack 부재**.

**결정 (사용자 — 하이브리드 + DPS 보정 C-2까지):**
1. **임계 지속 버프 폐기** — 미구현이었으므로 코드 변경 없음, 정본 §4 컬럼만 삭제·정정.
2. **만충 스파이크 신규 도입** — 데미지 핵심기 4종(결전일섬·천뢰일창·저격태세·별빛쇄도)에 **소모형 만충(3스택) 소모 시에만 ×1.20 디스크리트 폭발**. per-stack을 절반으로 재배분(순증 아님)해 만충 cap3 총계수는 목표값 유지, 비만충/유지형은 배율 미적용(2→3스택 ~22% 점프 = "충전 완료 보상"). **유지형(cap6, 만충 상시)은 게이팅으로 배율 제외** → 소모형=폭발/유지형=지속 분리, 유지형 핵심 단타는 소모형 만충의 86~89%.
3. **C-2 비핵심 슬롯 보정** — 쿨 격차 직접 대응: 석궁 속사 0.75→0.65·관통볼트 2.20→2.00(↓), 스태프 마력탄 1.50→1.70·마력쇄도 2.20→2.40(↑), 검 섬광베기 1.60→1.70. 예측 스프레드 **37.6%→18.4%**.

**핵심기 만충 재배분 (base/perStack → base'/perStack'·×1.20):** 결전 `3.45,0.12`→`3.32,0.06` / 천뢰 `3.60,0.08`→`2.96,0.04` / 저격 `4.20,0.10`→`3.35,0.05` / 별빛 `4.05,0.10`→`3.89,0.05`. 도끼 거신추락(4.55,0.10)·낫 처형낫은 유틸 유지(배율 없음).

**구현 (custom-plugins/poro-rpg):**
- `BaseWeaponSkill.scaledDamageFullChargeSpike(ctx,player,base,perStack,mult)` 신규 — `!retained && stacks>=3`에서만 `×mult`. retained 판정은 기존 `gainStack`과 동일(`classEngravingId().endsWith("_retained_01")`).
- 핵심기 4종 호출 교체 + C-2 5종 계수 교체(Java).
- **CSV `skill_master.csv` base_coefficient 동기** + finisher 쿨 오기 정정(천뢰 18→15·저격 20→14) + secondary_effect_value(구 per-stack) 갱신. (CSV→CombatFormulaResolver는 부팅만, 데미지 미적용 참조 경로. 만충 ×1.20은 CSV 스키마에 컬럼 없어 미표현 — Java가 권위.)
- **GUI** `GrowthGuiListener.SKILL_INFO` 9종 표시 문자열 동기(만충 핵심기는 "base%(+perStack%/스택, 만충 ×1.20)").

**정본 반영:** `combat_balance_v2 §4`(임계 컬럼 폐기·만충=디스크리트 ×1.20 명문화)·`§5`(유지형 만충 미적용 규칙). `skill_effects_reference_v1`(계수·쿨·per-stack 스냅샷·관찰메모 갱신). `dps_balance_pass_v1` 폐기 표시 → `v2` 신규.

**검증:** `gradlew build` 통과·jar 생성. **실측 DPS·캡6 유지형 각인 고유 +%(질풍창 등) 합산 폭주는 인게임 후속(미검증).**

**미해결(후속):** ① 스태프 20s 쿨 구조적 바닥(−9.9%, 계수만으론 ±10% 미달) — 쿨 단축 검토. ② 석궁 14s 쿨이 구 "조준 ON/OFF" 근거라 스택 전환 후 낡음 — 쿨 재검토. ③ C-4 2차 미세조정(보류). ④ 유지형 각인 +% × 만충 게이팅 실 DPS. ⑤ 크리·평타 0.8 손실계수 실측.

**관련:** DL-123(폐기된 v1 패스)·DL-122(캡3 통일)·INBOX-013, `dps_balance_pass_v2.md`, `combat_balance_v2 §4·§5`, `skill_effects_reference_v1`.

---

### DL-123 무기 DPS 균형 계수 패스 (DL-122 후속, combat-balance 분석)

**배경:** DL-122 캡 3 통일 후 `combat-balance` 서브에이전트가 6무기 풀로테이션 DPS 모델링(`dps_balance_pass_v1.md`). 결과 스프레드 19.3% — **창 1위(10,167)·스태프 6위(8,520)**. 핵심: 창·스태프 동일 "임계+만충 2단" 구조인데, 창은 관통찌르기 2단계 임계 +15%가 캡3에서 상시 ON되어 손실 자력 보전(1위), 스태프는 수치가 한 단계 낮아 못 메워 최하위. → **사용자 직관("창·스태프 둘 다 ↓")과 반대로 스태프는 과너프 상태**라 미세 상향이 정답.

**결정 (사용자 — 1차+2차 전부 적용):** DPS ±10% 수렴 목표로 계수 조정. 실제 데미지 계수(Java `scaledDamage`) + 시드 `base_coefficient`(CombatFormulaResolver 참조) + GUI 표시 문자열 3곳 동기화.

**적용 계수 (현재→제안):**
- 검 결전일섬 `3.20,0.15`→`3.45,0.12` / 도끼 거신추락 `4.20`→`4.55` · 파쇄돌진 `2.80`→`3.05` / 창 관통찌르기 `1.70`→`1.60`(창만 ↓) / 석궁 저격태세 `4.00,0.12`→`4.20,0.10` / 낫 사신베기 `1.80`→`1.90` / 스태프 별빛쇄도 `3.80`→`4.05`·속성폭발 `2.40`→`2.55`·마력쇄도 `2.00`→`2.20`(스태프 ↑).
- 캡6(유지형) per-stack 폭주 차단 위해 검 0.15→0.12·석궁 0.12→0.10 동반.
- 창 천뢰일창은 유지(이중 너프 금지).

**변경 파일:** 9개 스킬 Java + `skill_master.csv`(base_coefficient 9 + jar/외부 동반 배포) + `GrowthGuiListener` SkillInfo 표시 9.

**검증:** 빌드·기동 무에러, 24엔트리 로드. 예측 스프레드 19.3%→±10% 근접. 실측 DPS는 인게임/로그 후속.

**미해결(후속):** 창·스태프 임계(2단계)·만충(3단계) 2단 구조가 캡3에서 1스택 차로 붕괴 — 임계 단계 재설계 필요(최우선). 유지형 각인 별도 +%(질풍창 등) 합산 시 캡6 폭주 재검증. 보스 DEF 적용 절대 DPS.

**관련:** `dps_balance_pass_v1.md`, `skill_effects_reference_v1.md`, DL-122, `combat_balance_v2 §2·§3`.

---

### DL-122 자원 단계 통일 — 전 무기 기본 3 / 유지형 6 (INBOX-013)

**배경:** 사용자 — "유지형이 base 5인데 6이면 어색. 전 무기 기본 3으로 통일." 기존: 창·스태프 기본 5, 나머지 3 / 유지형은 코드상 무기 공통 flat 6(`BaseWeaponSkill.gainStack` `_retained_01 ? 6`). 정본 §5는 "유지형 +1"이었으나 코드는 flat 6 — 불일치였음.

**결정:** **전 무기 기본 자원 3단계 / 유지형 6단계**로 통일. (창·스태프 5→3. 유지형 6 유지 = 사용자 결정. 정본 §5 "+1" 폐기→"flat 6".)

**변경 (custom-plugins/poro-rpg):**
- `gainStack` 5→3: StaffArcaneOrb/ArcaneRush/ElementalBurst, SpearThrust (4파일).
- finisher 소모량 5→3: `skill_master.csv` 천뢰일창·별빛쇄도 (cap 3에서 발동 가능하도록 — **필수 연동**).
- 표시 3: `HealthHudFormatter`·`GrowthGuiListener` `(SPEAR||STAFF)?5:3`→`3`. 유지형 표시는 flat 6 유지.
- 정본: `combat_balance_v2 §4`(창·스태프 5→3, 중간 임계 3단계→2단계)·`§5`(유지형 flat 6).
- 배포: jar + **외부 시드** `server/plugins/PoroRPG/seeds/skill_master.csv` 동반(외부 우선 로드).

**미완(후속 — 사용자 계수 결정 대기):** ① 창·스태프 per-stack 계수(thrust 0.05·thunder 0.08·starburst 0.10)가 5스택 기준이라 3캡에서 최대 보너스 축소(starburst +50%→+30%) — "계수 좀 내리고"는 캡으로 일부 자동 반영. ② 다른 4무기 계수 상향 "고려"(미적용). ③ 창·스태프 중간 임계(2단계)·만충(3단계) 효과 2단 구조 재설계. → `skill_effects_reference_v1.md` 기준 별도 패스.

**관련:** INBOX-013, `combat_balance_v2 §4·§5`, `skill_effects_reference_v1.md`(현행 스냅샷), DL-121.

---

### DL-121 원거리 무기 투사체 평타 (스태프·석궁) (INBOX-013)

**배경:** 라이브 검증 — 마법사(스태프)·석궁 평타가 원거리로 안 나감. 조사: 평타는 `onAttack`(`EntityDamageByEntityEvent`, 멜리 접촉)에서만 `weaponPower×0.4` 적용 → 전 무기 근접. `onSwing`(LMB 허공)은 slot1 스킬만 발동, 쿨다운 중이면 무동작.

**변경 (custom-plugins/poro-rpg, `SkillInputListener`):**
- `onSwing`에서 slot1 쿨다운 중 + **원거리 무기(STAFF·CROSSBOW)**면 `rangedBasicAttack` 발사 — `SkillHitboxHelper.projectileRaycast(player, 20, 0.6)`로 시선 첫 대상에 `weaponPower×0.4` 피해. `SkillDamageGuard.run(() -> target.damage(dmg, player))`로 killer 귀속 + onAttack 재처리 차단(BaseWeaponSkill 패턴 재사용). 발사 시 `swingFiredAt` 기록으로 근접 시 멜리 평타 중복 방지.
- 시각: 시선 파티클 빔(석궁 CRIT / 스태프 WITCH) + 사운드(ARROW_SHOOT / BLAZE_SHOOT).

**근접 무기(검·도끼·창·낫)는 기존 멜리 평타 유지.** 계수는 동일(0.4)이라 밸런스 일관.

**검증:** 빌드 통과·기동 무에러. 실제 투사체 발사·원거리 피해는 사용자 인게임 확인(스태프/석궁 장착, slot1 쿨다운 중 LMB).

**관련:** DL-113(평타 도입 0.4), INBOX-013.

---

### DL-120 동적 스폰 경계 정합 — 경계 밖 스폰 금지 + 이탈 몹 제거 (INBOX-012 후속)

**배경:** DL-119 WorldBorder(±150) 적용 후 사용자 보고 — 경계 밖에도 몹이 스폰됨. 원인: `FieldSpawnService.randomGroundAround`가 플레이어 주변 20~30블록 랜덤 스폰 시 **필드 경계(±150) 미검사** → 가장자리 플레이어 기준 스폰이 경계 밖으로 튐. 또 AI 배회로 경계를 벗어난 몹이 잔존.

**변경 (custom-plugins/poro-rpg):**
- **스폰 경계 검증** — `tick()` 스폰 루프에 `if (fieldAt(loc) != field) continue` 추가. 경계 안에서만 생성.
- **이탈 몹 제거** — `removeStrayMobs()` 신규 + 1초 주기 타이머. 추적 몹 중 `fieldAt(위치) != 배정필드`면 `remove()`. 스폰 존=WorldBorder(±150) 동일 판정.

**범위/한계:** FieldSpawnService가 추적하는 **동적 스폰 몹**만 대상. 필드보스(제자리 고정·경계 내 스폰)·수동 /mm 소환(미추적)은 무관. tick 15s는 스폰, 이탈 정리는 1s로 분리.

**검증:** 빌드 통과. 기동 로그 `경계 이탈 정리 1s`. 동적 스폰 실효는 플레이어 필드 진입 시 — 사용자 인게임 확인.

**관련:** DL-119(WorldBorder), DL-100(±150 스폰 존), INBOX-012.

---

### DL-119 필드 진입 WorldBorder 경계 (INBOX-012)

**배경:** 필드는 단일 `world`의 좌표 기반 300×300 스폰 존(`FieldSpawnService.BOUND_RADIUS=150`)일 뿐 물리·시각 경계가 전무. 맵 설계의 포탈/울타리 경계는 미구현. 사용자가 "필드 진입 시 WorldBorder로 경계 표시" 요청.

**결정:** 필드 진입 시 **개인(per-player) WorldBorder**를 띄워 빨간 경계 표시. 스폰 존(300×300)과 정확히 일치. 데미지 0(이동 통제·시각만, 이탈은 GUI 이동).

**구현 (custom-plugins/poro-rpg):**
- `FieldBorderService`(신규) — 0.5초 폴링. 플레이어가 필드 영역(중심 ±150) 진입 시 `player.setWorldBorder`(중심=필드 좌표, size 300, warningDistance 8, damage 0). 이탈 시 `setWorldBorder(null)`(월드 기본 복귀). 상태 변화 시에만 패킷(플리커 방지). per-player라 같은 월드에서도 각자 자기 필드 경계만 봄. config `fields`에서 5필드 로드.
- `PoroRPGPlugin` — FieldSpawnService 옆에서 start.

**검증:** 빌드 통과. 기동 로그 `[FieldBorder] 필드 진입 경계 활성 — 필드 5개, 크기 300×300`. per-player WorldBorder 시각(빨간 경계)은 플레이어 필드 진입 시 발현 — 사용자 인게임 확인.

**관련:** INBOX-012, DL-100(필드 ±150 스폰 존), `poro-rpg/docs/12_map_design/field_map_concepts`(포탈/울타리 경계 — 향후 빌드).

---

### DL-118 바닐라 콘텐츠 제거 — 몹/동물 자연 스폰 차단 + 바닐라 드랍 제거 (INBOX-011)

**배경:** 라이브 검증 중 사용자 보고 — (1) 필드에 커스텀 필드몹과 바닐라 몹이 섞여 혼란, (2) 동물도 막아야 함, (3) 영지(섬)에서도 몹·동물 차단, (4) 좀비 처치 시 철·썩은고기 등 바닐라 드랍. 조사: 필드=단일 오버월드 `world` 리전(자연 스폰 제어 전무). 커스텀 몹은 `PreventOtherDrops:true`+드랍테이블 EXP만이라 바닐라 드랍 없음 → (4)는 섞인 바닐라 몹의 드랍.

**결정 (사용자 선택):** 커스텀 RPG 월드에서 **바닐라 생물(몹+동물) 자연 스폰 전면 차단 + 바닐라 아이템 드랍 제거**. 대상 월드 = `world`(필드) + `world_hub`(허브) + `IridiumSkyblock`(+nether/end, 영지).

**구현 (custom-plugins/poro-rpg):**
- `VanillaContentControlListener`(신규, 4기능) — ① `CreatureSpawnEvent`: 대상 월드에서 스폰 이유가 **허용셋(CUSTOM·COMMAND·SPAWNER_EGG)** 밖인 모든 생물 취소(몹+동물). Citizens NPC(메타 `NPC`) 보호. ② `EntityDeathEvent`: 비-플레이어·비-NPC 몹 드랍 `getDrops().clear()`(전역). PoroRPG 보상은 전부 DB(`addCurrency`)라 무영향(코드 전역 `getDrops().add` 부재 확인). ③ `EntityCombustEvent`: 일광 화상만 취소(ByBlock 용암·ByEntity 화염 유지) — 커스텀 좀비/스켈레톤 일광 사망 방지. ④ 60초 주기 sweep: 패치 전 잔존 바닐라 동물 제거(passive는 디스폰 안 됨 — `Animals`/`WaterMob`/`Ambient` 타입 판정, 커스텀 몹=Monster라 안전).
- `PoroRPGPlugin` — 대상 월드셋 주입 + 등록 + sweep 시작.

**검증:** 빌드 통과. 인게임 라이브 — `world`에 Plains_Soldier(CUSTOM) **생존 + 오버라이드 atk 4.0**(커스텀 미차단). `/summon zombie`→처치 시 **아이템 0개**(드랍 제거). `/summon cow`→60초 sweep이 **테스트 소 + 기존 잔존 동물 4마리 = 5마리 정리** 실증. 일광 화상 방지·NATURAL 차단은 사용자 인게임 최종 확인.

**관련:** INBOX-011, DL-100(필드 리전), DL-117(몹스탯 — 동일 스폰 경로 공존), `FieldDropListener`(DB 보상), `VanillaExpSuppressListener`(바닐라 XP 억제 — 동일 철학).

---

### DL-117 런타임 몹 스탯 오버라이드 시스템 MVP (INBOX-010 축 A)

**배경:** 몹 HP/DEF/ATK·상점을 플러그인 재배포 없이 인게임 명령으로 핫에딧하려는 운영 요구(INBOX-010). 기획안 `poro-rpg/docs/01_plugin_architecture/runtime_admin_config_plan_v1.md` 작성 후 축 A(몹 스탯 ATK·HP) MVP 구현.

**구현 (custom-plugins/poro-rpg):**
1. **DB** — `mob_stat_override`(mob_key PK, max_hp/def/atk nullable) + `config_change_log`(감사 로그=패치노트 원천). `RuntimeConfigMigration`을 `CommonFoundationBootstrap` 마이그레이션 리스트에 등록.
2. **서비스** — `MobStatOverrideService`: 부팅 시 DB 캐시 로드 + **DL-116 정본값 시드(ATK만, 없는 키만 삽입 → 운영자 편집 보존)**. set/reset이 DB·감사로그·캐시 동기. `MobStatOverrideRepository`/`ConfigChangeLogRepository` CRUD.
3. **스폰 적용** — `MobStatOverrideSpawnListener`가 `MythicMobSpawnEvent`(reflection 격리)를 리스닝해 **전 스폰 경로 커버**(동적 필드/보스룸/MythicMobs 네이티브 — 필드보스 포함). 1틱 지연으로 MythicMobs 적용 후 어트리뷰트(`MAX_HEALTH`/`ATTACK_DAMAGE`) 덮어씀. (mythicSpawner 람다 주입 대신 채택 — 필드보스가 람다를 안 거치는 문제 해결.)
4. **명령** — `/poro-mobstat list|get|set|reset` (`poro.admin` 권한, 값 0~100000 클램프). plugin.yml 등록.

**적용 범위/제약:**
- 적용: 몹 **HP·평타 ATK**(=바닐라 `Damage:` 속성). DL-116 일반/정예/필드보스 기본공격값 시드.
- **DEF는 저장만, 미적용** — `PlayerDefenseListener` 몹별 DEF 연동은 2단계.
- **보스 스킬 패턴 데미지(강타/폭발)는 적용 불가** — MythicMobs YAML 상수(기획안 §4.1-C, C-2). 패턴 데미지는 별도 YAML 배포 영역.
- 변경은 **신규 스폰부터** 반영(기존 개체 미소급).

**검증:** `gradlew build` 통과 + **인게임 라이브 검증 통과**(2026-06-01). MythicMobs 5.11.2 jar로 reflection 시그니처 확인(`getMobType().getInternalName()`/`getEntity()`). 서버 기동 로그: 마이그레이션·시드 21건·리스너 등록 정상. 콘솔 실측: `set Plains_Soldier atk 77`→스폰 후 attack_damage **77.0** 확인 / `reset`→**4.0**(DL-116 시드) 복원 확인. 1틱 오버라이드가 MythicMobs `Damage:` 적용을 확실히 덮음(race 없음).

**남은 작업:** 축 B(상점 명령), 축 C(패치노트 디스코드/웹 피드), DEF 연동(2단계), 보스 패턴 배율 placeholder(C-1, 수요 시), 인게임 검증.

**관련:** INBOX-010, `runtime_admin_config_plan_v1.md`, DL-116(시드 원천), `combat_balance_v2 §1.5`.

---

### DL-116 몹 → 플레이어 ATK 정본표 신규 (HP 100 스케일 동반 재조정)

**배경:** DL-115에서 0강 풀세트 HP를 670→100으로 낮췄으나, 몹→플레이어 피격은 바닐라 데미지 경로(`PlayerDefenseListener`가 DEF 경감만 적용)라 몹 공격력이 HP 670 시절 임의값 그대로였다. 조사 결과 현행 MythicMobs 설정에 **내부 모순** 확인 — 보스 바닐라 평타(`Damage:` 35~120)는 신규 HP 기준 거의 즉사(F5 120 → 18강 158HP의 52%), 반면 같은 보스 스킬 패턴(`damage{a=8~22}`)은 과소(원형폭발 22 → 10%). 둘 다 HP 670 시절 따로 튜닝된 결과.

**결정:** 몹 ATK를 **티어별 "추천 강화 풀세트 최대 HP 대비 고정 비율(유효 피해)"**로 정본화. `raw ATK = (티어비율 × 추천HP) / 관통율`. raw는 DEF 경감 전 값으로 MythicMobs `Damage:`/`damage{a}`에 그대로 입력(리스너가 받는 쪽에서 `×(1−mit)`).

**티어 비율 (사용자 조정 — 필드 가볍게 / 시즌보스 무겁게):** 필드 콘텐츠(몹+필드보스)와 시즌보스의 배율을 **분리**. 같은 P-코드 패턴이라도 필드보스 강 24% ↔ 시즌보스 강 40%. 근거: 필드는 오픈월드·상시 리젠이라 "죽을 걱정"이 유저 부담이고, **회복약이 공방 제한 소모재**라 필드에서 포션을 쓰게 하면 시즌보스용이 마름. → 필드는 포션 없이 버티는 가벼움, 포션·회피는 시즌보스 전용 긴장.
- 일반몹 2.5%(~40타) / 정예몹 6%(~17타)
- **필드보스** 기본 8%(~12타) / 일반 16%(~6타) / 강 24%(~4타)
- **시즌보스** 기본 12%(~8타) / 일반 24%(~4타) / 강 40%(~2.5타)

**필드보스 ATK(기본/일반/강):** F1 12/24/37 · F2 14/29/43 · F3 16/32/48 · F4 17/34/50 · F5 18/37/55. 일반몹 4~6, 정예 9~14. 시즌보스(무겁게): 기본 22~30 / 일반 43~61 / 강 72~101, 잠정(인내·잠재 의존 큼).

**정합성:** 최고 티어 시즌보스 강 40% ≪ 85% 클램프. 필드(몹+필드보스)는 포션 없이 버티도록 설계 — 회복약은 시즌보스용. 하급 포션(HP 30%/3회)이 시즌보스 강 1대(40%)를 겨우 상쇄하는 "줄타기", 연달아 맞으면 회피 필수. 인내·받피감 잠재·포션 미반영 **보수적 하한**.

**변경 (docs only):**
- 신규 `poro-rpg/docs/06_fields_bosses/mob_attack_stats_v1.md`(field_boss_stats의 공격 측 대칭 문서, 설계 전제·티어 룰·필드/시즌 정본표·현행 gap·MythicMobs 적용 매핑).
- `combat_balance_v2.md §1.5` 신설(정본 요약 + 포인터), DL-115 HP 노트에 "정본화 완료" 갱신.
- `poro-rpg/docs/06_fields_bosses/CANON.md` 참조 우선순위에 신규 문서 추가.

**남은 작업:** (1) server/plugins/MythicMobs 실제 적용은 별도 배포 단계(사용자 승인 필요). (2) 시즌보스 강 패턴 비율(30 vs 35~40%) 실측 확정. (3) 오픈 1주차 사망 로그로 티어 비율 보정.

**관련:** DL-115(HP 100 스케일), `combat_balance_v2 §1·§2·§11`, `field_boss_stats_v1`, `mob_attack_stats_v1`.

---

### DL-115 방어구 HP+DEF 정본 구현 + HP 0강 100 하향

**배경:** DL-113 1단계는 DEF만, 그것도 combat-balance가 없는 문서(`potential_options_v1.md`)·가중배열로 추측 설계했다. 사용자 지적으로 정본(`combat_balance_v2 §1`)을 재확인 — 방어구는 **HP+DEF 둘 다**(투구 HP400·DEF0, 상의 HP150·DEF15, 하의 HP0·DEF30, 신발 HP100·DEF7), 강화는 **선형 `기본×0.04×단계`**. HP는 게임에 전혀 미반영이었고(max health 미설정), DEF 강화는 정본 선형이 아니었다.

**변경 (custom-plugins/poro-rpg):**
1. **DEF 강화 정본 선형화** — `SkillContext.defense()` = Σ 기본 DEF×(1+0.04×강화) + 인내(0.4/pt). combat-balance 가중배열 폐기.
2. **방어구 HP 신규 구현** — `SkillContext.armorMaxHealth()` + `applyMaxHealth()`(max health = 20 + 방어구 HP). 접속(`PlayerJoinListener`)·강화(`GrowthGuiListener`) 시 갱신. (기존 미구현 해소)
3. **HP 0강 100 하향(사용자 결정)** — 정본 670은 몹→플레이어 ATK 정본표 부재(바닐라 데미지) 대비 과함. 투구50/상의20/신발10 = 방어구 80 + 기본 20 = **0강 풀세트 100**, 25강 180. 비율 유지. DEF 52(20.6%)는 유지.
4. **문서:** `combat_balance_v2 §1` HP 표 갱신(670→100, 강화 표 동반).

**남은 작업:** 잠재 def%·곱산 상한(3단계), 몹→플레이어 ATK 정본표(HP 스케일과 동반 재조정). 방어구 부위별 주스탯 정합은 구현됨(투구=HP만, 하의=DEF만 — base 0 처리).

**관련:** DL-113(1단계), `combat_balance_v2 §1`.

---

### DL-114 잠재 t1 옵션 풀 보스공 편중 완화 (임시)

**배경:** 무기 잠재를 돌리면 EPIC+ 등급에서 3줄 전부 보스공만 나옴. 원인 — t1 풀의 RARE+ 등급에 `boss_damage_increase`만 정의(attack/general은 COMMON만). `buildLine`이 "라인 등급=옵션 등급 정확 일치"로 추첨해 다양성 0.

**변경:** `growth_potential_option_pool.csv` t1_weapon/armor 풀의 RARE/EPIC/UNIQUE/LEGENDARY에 attack_percent·general·core/precision 태그(무기), max_hp·damage_reduction(방어구) 추가. EPIC+ 3줄 보스공 확률 100%→0.16%.

**한계(중요):** 정본 `equipment_growth_spec §2.2`는 **슬롯별** 풀(투구=HP/받피감/쿨감, 하의=HP/방어/이속, 신발=이속/방어/방어무시)인데 현재 csv는 armor 통합. §2.1 라인 수(커먼1/레어2/에픽3)도 코드는 3줄 고정. **본 변경은 보스공 편중만 임시 완화**이며 정본 슬롯별 구조는 미준수. 추가 옵션 값은 근거 약함(combat-balance가 없는 문서 인용). 정본화는 후속.

**관련:** `equipment_growth_spec §2`, `growth_potential_option_pool.csv`.

---

### DL-113 방어구 DEF 1단계 + 전투 핵심 수정(평타·내구도·스킬 재진입)

**배경:** 라이브 검증. (1) 방어구가 강화/표시는 되는데 실제 피해 감소에 전혀 반영 안 됨(몹→플레이어 바닐라 경로). (2) "현재 스탯" 방어력이 0으로 표시(인내 보너스만, 방어구 베이스 누락). (3) 공격력 93인데 철골렘이 안 죽음 — 디버그 결과 스킬 데미지(163)가 평타(37)로 덮어써지는 재진입 버그. (4) 무기 내구도 소모.

**전투 구조 확정(조사):** 좌클릭=스킬(바닐라 취소). 스킬 데미지는 `CombatFormulaResolver`로 무기 ATK(`WeaponPowerCalculator`) 반영. 단 몹→플레이어 피격은 바닐라라 방어구 DEF 미적용. `combat_balance_v2 §1` 정본 베이스 DEF = 투구0/상의15/하의30/신발7 = **52**(item_master.csv 150은 고립값, 폐기).

**변경 (custom-plugins/poro-rpg):**
1. **방어구 DEF 1단계 (피격 경감 파이프라인)** — `SkillContext.defense()` 신규(방어구 베이스 DEF 상수맵 52 + 인내 0.4/pt). `PlayerDefenseListener`(신규) — 몹→플레이어 `EntityDamageByEntityEvent`에 `DEF/(DEF+200)` 경감(0강 20.6%). PvP·환경피해 제외. 강화 DEF 곡선·잠재 def%는 2~3단계로 분리.
2. **방어력 정본 통일** — "현재 스탯" GUI(`GrowthGuiListener:1630`)가 `skillContext.defense()` 호출 → 표시=경감 일치(방어구 베이스 포함). 단일 정본화.
3. **ATK 기반 평타** — 기본기 쿨다운(3~4초) 중 좌클릭이 바닐라 재질 데미지로 새던 것을 `weaponPower × 0.4`로 교체(`SkillInputListener`). 무기 ATK·강화·잠재 반영.
4. **무기 내구도 보호** — `WeaponItemFactory`에 `setUnbreakable(true)` + ItemFlag 숨김. (기존 손무기는 재생성 시 적용)
5. **검 기본기 적중** — `SwordFlashSlashSkill` 타격 판정을 돌진(`dashForward`) 전으로 — 붙어서 쳐도 arc(전방 120°) 빗나감 해소. (돌진 있는 건 검만)
6. **스킬 데미지 재진입 가드(핵심)** — `SkillDamageGuard`(신규, ThreadLocal). 스킬 `target.damage()`가 재발생시키는 `EntityDamageByEntityEvent`를 평타 리스너가 `setDamage`로 덮어쓰던 버그(163→37) 차단. `BaseWeaponSkill.dealDamage` 한 곳에 적용돼 전 무기 스킬 공통.

**남은 작업:** 방어구 강화 DEF 곡선(2단계, `ARMOR_DEF_COEF`), 잠재 def%·곱산 상한(3단계), 몹→플레이어 ATK 정본표 신규. 평타 계수 0.4 라이브 튜닝.

**관련:** DL-104(무기별 인스턴스), `combat_balance_v2 §1·§2`, `level_stat_system_v1 §7`.

---

### DL-112 잠재 큐브 연속 재롤 + 잠재 GUI 현재 옵션 안내 + 무기 강화스탯 lore

**배경:** 라이브 검증 요청. (1) 큐브 굴린 후 [현재 유지]/[새 옵션 선택]을 강제로 눌러야 다음 큐브 가능 → "선택 없이 계속 돌리고 싶다". (2) 잠재 GUI에서 선택한 장비의 현재 잠재 옵션이 안 보임. (3) 무기 lore에 강화 단계만 있고 그로 인한 스탯 증가가 안 보여 "강화 체감 불가".

**변경 (custom-plugins/poro-rpg):**
1. **큐브 연속 재롤** — `handlePotentialClick`에서 미확정 결과 중 `USE_CUBE` 클릭 허용. 큐브 재클릭 시 직전 후보를 `before`로 자동 복원(기존 유지 효과) 후 재굴림. [새 옵션 선택]을 눌러야만 등급/옵션 확정. `rollPotential` 종료 시 큐브 버튼 비활성→활성 유지.
2. **잠재 GUI 현재 옵션 안내** — `refreshPotentialCurrentPanel` preview에 등급 + 옵션 라인 전체 한글 나열. 미부여 시 "큐브를 돌려 부여" 안내.
3. **무기 강화스탯 lore** — `EquipmentLoreRenderer.baseLore` 무기 강화 줄에 `WeaponPowerCalculator.enhanceAtkBonus` 병기(`+N강 (공격력 +M)`). 방어구 DEF 보너스는 정본 충돌·계산 미구현으로 별도 작업(DL-113 예정)으로 분리.

**남은 작업(DL-113 예정):** 방어구 강화 DEF 보너스 — 베이스 DEF 정본 충돌(item_master 150 vs combat_balance_v2 52) 확정, `ArmorDefenseCalculator` 신규 구현, 강화 곡선(`ARMOR_DEF_COEF`) 적용, 다층 곱산 상한 정책.

**관련:** DL-110(lore 정본 통일), DL-094/111(큐브 확률).

---

### DL-111 잠재(큐브) 승급 확률 재조정 — 전설 극희귀 격리 (DL-094 후속)

**배경:** 라이브 검증 중 "큐브 24개로 전설 달성" 발생. economy-reviewer 수급량 역산 결과, 실제 원인은 확률이 아니라 **수급량 대비 목표 미스매치**로 확인. 일반 유저 45일 ~200큐브 / 선발대 ~1,300큐브 수급인데 전설 기댓값이 96큐브(DL-094)라 일반 유저도 전설 2회분 확보 → "전설=상위 0.5~1% 희귀" 의도와 정반대.

**결정 (사용자 선택 A안):** 유니크까지는 큐브 후함 철학 유지, **전설(유니크→전설)만 대폭 격리**.
- 확률: 0.25/0.12/0.06/0.015 → **0.22/0.10/0.04/0.001**
- 누적 기댓값: 레어 ~4.5 / 에픽 ~14.5 / 유니크 ~39.5 / **전설 ~1,040큐브**
- 선발대 가용(~1,300) 대비 전설 1회분 미만 도달 = 상위 0.5~1% 성립. 일반 유저 전설 도달률 사실상 0.
- 골드(큐브당 500G)·단조 상승 구조는 유지 (병목은 큐브, 단조 상승은 후함 원인 아님 — economy-reviewer 의견).

**결과:**
- 코드: `PotentialService.UPGRADE_CHANCE_BY_GRADE` 4값 교체(1곳).
- 문서: `equipment_growth_spec §1.3` 메모리얼 확률표 갱신 필요(후속).

**운영 체크포인트:** 오픈 30일 전설 도달 인원이 활성 1% 초과 시 UNIQUE 0.001→0.0007 추가 하향. 큐브 경매 시세 폭락 시 = 수급(정예몹 조각률 필드5 15%) 손볼 신호. "24개 전설"이 다수면 배포 jar 확률값/조각 중복적립 버그 점검.

**관련:** DL-094(메모리얼 승급 확정), `economy_numbers_v2 §0·§12`, `drop_tables_v1 §5·§6`.

---

### DL-110 무기별 독립 각인 전환 + 장비 lore 정본 통일 + 표시 한글화

**배경:** DL-109 후속 라이브 검증. "도끼로 바꿨는데 석궁 각인이 뜨고 영어로 표시" 버그. 근본 원인 2가지 — (1) 각인이 `classEngravingId` 단일 필드라 무기 변경 시 잔존, (2) 장비 lore가 3곳(`equipBaseLore`·`WeaponItemFactory`·`buildWeaponChangeIcon`)에 복제돼 한글화가 일부만 적용. DL-109의 "정본 통일"이 GUI 5종만 고치고 손무기/무기변경 아이콘은 누락했던 것.

**변경 (custom-plugins/poro-rpg):**
1. **무기별 독립 각인 (영속화 스키마 v5→v6)** — `PlayerGrowthState.classEngravingId`(단일 String) → `Map<classId, engravingId>`. `classEngravingId()`=현재 무기 각인, `classEngravingId(classKey)` 오버로드 추가. 강화/잠재가 무기별 인스턴스(`weapon_<타입>`)인 것과 대칭. `PlayerSaveData`에 `classEngravingByClass` 필드 + `CURRENT_VERSION 6`. `PlayerPersistenceService` v5→v6 마이그레이션(단일값→현재 classId 키 이관). 무기를 왕복해도 각 무기가 자기 각인 보유.
2. **lore 정본 통일** — `EquipmentLoreRenderer`(신규, static + enable 시 registry 주입). `equipBaseLore`·`WeaponItemFactory.buildLore`·`buildWeaponChangeIcon` 3곳이 모두 이 단일 정본에 위임. 각인 한글명(`engravingDisplayName`)·잠재 등급/옵션 한글 헬퍼를 렌더러로 이관.
3. **표시 한글화/정합** — 스코어보드 각인 한글명 + 청록(§b) 강조(DL-109 미완 해소, `ScoreboardService`). 강화 GUI 강화시도 버튼 무기명 영어(`Training Sword`) → 정본 한글(`equipDisplayName`). 잠재 GUI 등급/옵션 영어 → 한글(`potentialGradeKr`/`potentialOptionKr` 재사용).
4. **운영자 지급 명령 별칭** — `/poro-currency`에 한글/약어(골드·강화석·큐브·큐브조각) 매핑 추가(`AdminPlayerCommand.resolveCurrencyCode`).

**남은 작업(묶음 B 예정):** 강화단계별 고정스탯 lore 표시(무기 ATK 반영됨/방어구 DEF 보너스 미구현), 큐브 결과 미선택 시 연속 재롤, 잠재 GUI 현재 옵션 안내.

**관련:** DL-109(정본 통일 1차), DL-104(무기별 독립 인스턴스), DL-103(방어구 재질 변경 제거).

---

### DL-109 성장 GUI 정본 통일 + 직업변경 각인 연동 + 손무기 실시간 lore

**배경:** 라이브 검증으로 강화/잠재/각인/전승/캐릭터 GUI의 장비 표시가 제각각이고, 직업 변경이 각인과 연동 안 되던 문제 일괄 수정.

**변경 (custom-plugins/poro-rpg):**
1. **직업 변경 ↔ 각인 연동** — `PlayerGrowthState.classId` final 제거 + `setClassId()`. `ClassInitService.grantStarterEquipment`(/직업)와 `handleWeaponChangeClick`(무기 변경 GUI) 모두 classId 갱신.
2. **장비 표시 정본 통일** — 공통 헬퍼 `equipDisplayName`(무기=검/도끼…, 방어구=투구/흉갑/레깅스/부츠) + `equipBaseLore`(구분선/강화/등급/잠재/세부스탯/각인). 강화·잠재·전승·캐릭터·상세 5개 GUI 재사용.
3. **lore 한글화** — 등급/잠재 등급(커먼~전설), 잠재 옵션 한글(`potentialOptionKr` 19종), 각인 한글명(`engravingDisplayName`). 잠재 미부여 "없음", 부여 후 등급+옵션. "(N라인)" 제거.
4. **[T1] 제거 / 캐릭터 일괄버튼 제거 / 방어구 재질 변경 제거(DL-103)**.
5. **강화 미리보기 무기 연동** — PAPER·DB이름 → cosmetic 재질 + 정본 이름·lore.
6. **손무기 lore 실시간 갱신** — `refreshHeldWeapon` 헬퍼, 강화/각인/잠재 5개 지점 호출(DL-104 후속 해소).

**남은 작업(미완):** 스코어보드 각인 영어 ID(`ScoreboardService:98`) → 한글화 필요(engravingRegistry 주입).

---

### DL-108 라이브 인게임 디버깅 종합 (INBOX-007 2차 라이브 세션)

**배경:** 서버를 실제 기동하고 인게임 검증을 반복하며 발견한 버그·요청을 일괄 수정. 정적 분석으로는 못 잡는 Paper 버전 호환·플러그인 충돌·UX 문제 다수.

**코드 변경 (custom-plugins/poro-rpg — 커밋 대상):**
1. **영지명 변경 — 채팅 입력 방식 전환.** Paper 1.21.10 Anvil 3중 버그(① `createInventory(ANVIL)` ClassCastException → `MenuType.ANVIL`, ② 클릭 판정 `instanceof AnvilInventory` 실패 → `getView() AnvilView`, ③ repairCost>0로 결과슬롯 잠김 → `AnvilView.setRepairCost(0)`)를 거쳐 **최종 채팅 입력으로 전환**(버전 의존 제거). `AnvilGuiHelper`(신규) 잔존. **멤버 초대도 채팅 전환.**
2. **사망 시 keepInventory** — `DeathKeepInventoryListener`(신규). 템·경험치 유지(1차 시즌 정책). gamerule 아닌 이벤트(동적 월드 대응).
3. **영지 농작물 보호** — `IslandProtectionListener`(신규): 밟기(성장도 무관 전체)·손파괴(미성숙만, 다 자란 건 수확 허용)·물 흐름(작물 칸 유입 차단). 본인 영지 `CONV_CROP_PROTECT` 기준.
4. **접속 빨간화면(간헐)** — `HubSpawnListener`에 `PlayerSpawnLocationEvent` 추가. 마지막 위치(IS 섬 border 밖) 경유 깜빡임 제거.
5. **world_hub WorldBorder 정상화** — `HubWorldService`가 기존/신규 모두 center(0,0)·무제한 보장.
6. **영지 스폰 설정** — `TerritorySettingsGui` slot 10에 버튼(`/is sethome`). slot 9 자동입금 충돌 정정.
7. **영지 창고 입금** — `StorageGuiListener`: 인벤 아이템 좌클릭=1개/우클릭=종류 전부. **무기(PDC `poro_rpg:weapon_type`)·메뉴 나침반(COMPASS) 입금 차단**(인벤 클릭+전체입금 공통).

**런타임 설정 변경 (server/ — .gitignore, 재배포 시 재적용 필요):**
- `server.properties`: `spawn-monsters=false`
- `Multiverse-Core/worlds.yml`: 전 월드 `spawning.monster.spawn=false` (바닐라 적대몹 차단 — server.properties를 Multiverse가 오버라이드하던 문제)
- `WorldEdit/config.yml`: `navigation-wand.item: minecraft:recovery_compass` (compass 충돌 → 나침반 메뉴 "No free spot" 해소)
- `IridiumSkyblock/settings.yml`: `weather.enabled=false`, `time.enabled=false` (IS Island Time이 영지 시간 설정을 덮어쓰던 문제)
- `IridiumSkyblock/schematics.yml`: `yHome` 93→91 (새 섬 스폰 낙하 완화; 기존 섬은 `/is sethome`으로)

**검증:** 인게임 전수 확인 — 영지명·초대·시간·작물보호(밟기/손파괴/물)·창고입금·무기나침반차단·몹스폰·빨간화면·사망유지 전부 통과.

**후속/미해결:** "다른 곳에서 깨지는 것" 사용자 보고(미특정) — 별도 진단. server/ 설정 변경의 server-config 영속화 검토.

---

### DL-106 스코어보드 영지명 rename 반영 + 스키매틱 picker 검증 (INBOX-007 ③⑦)

**결정/구현:**
1. **⑦ 영지명 rename 반영** — 스코어보드 위치 표기에서 IridiumSkyblock 월드 케이스를 `player.getName() + "의 영지"`(하드코딩) → `IslandTerritoryStateStore`의 `islandName()` 조회로 교체. `setIslandName()`(영지 설정 모루 rename)이 반영된다.
   - `ScoreboardService`: 생성자에 `IslandTerritoryStateStore` 주입, `resolveLocationName`을 static→인스턴스 전환, `territoryName(player)` 헬퍼 추가(미생성·공백 시 `"{이름}의 영지"` 폴백). `PoroRPGPlugin` 생성자 호출 배선.
2. **③ 스키매틱 picker 우회 — 검증 완료(코드/설정 변경 없음)**: `server/plugins/IridiumSkyblock/schematics.yml`가 `poro` 단일 엔트리만 정의 → picker 생략 조건 충족. (schematics/ 폴더에 미등록 옛 .schem 9개 잔존하나 schematics.yml 미등록이라 무해.) 실제 picker 미표출은 인게임 재검증 몫.

**왜:**
- ⑦: 영지명 변경 기능(store·UI)은 이미 있었으나 스코어보드가 하드코딩이라 반영 안 됨 — 표시 경로만 store에 배선.
- IS API JAR 미포함이라 "현재 발 딛은 섬의 소유자" 역조회는 불가. 기존도 본인 영지를 가정했으므로 **퇴보 없이 rename만 추가 반영**.

**한계/후속:**
- ~~`islandName`은 `IslandSettingsRepository` 저장/복원 대상이 아님 → 재로그인 시 기본값으로 리셋. DB 영속화는 별도 후속.~~ → **정정 [DL-107]**: 영지명은 `PlayerSaveData.TerritorySaveData`(ownerName 슬롯)로 이미 영속화됨. 재로그인 리셋 없음 — 추가 작업 불필요.
- 남의 영지 방문 시 그 집 영지명 표시는 IS API 연동(§7+) 후속.
- ③ 옛 .schem 9개(desert/jungle/mushroom 계열) 정리는 선택(무해).

---

### DL-107 영지명 영속화 = 기존 PlayerSaveData 경로로 이미 완료 (DL-106 진단 정정)

**검증 결과:** 영지명(`islandName`)은 `IslandSettingsRepository`(island_settings DB)엔 없지만, **`PlayerSaveData.TerritorySaveData`로 이미 저장/복원**되고 있다. DL-106이 적은 "재로그인 시 리셋"은 오진.

**근거:**
- 저장: `PlayerPersistenceService.toTerritoryDto`가 `TerritorySaveData`의 첫 인자(`ownerName` 슬롯)에 `t.islandName()`을 넣는다.
- 복원: `applyTerritory`가 그 슬롯(`t.ownerName()`)을 `state.setIslandName()`으로 적용한다.
- 즉 필드 *이름*만 `ownerName`(레거시 명칭)일 뿐, 값은 영지명으로 일관되게 왕복. rank·기계수와 같은 DTO라 동일 트리거(로그아웃 save / 로그인 load)로 보장.

**조치:** 별도 DB 컬럼 추가 안 함(중복 영속화 방지). 혼란 방지로 `TerritorySaveData.ownerName` 필드 + save/load 양쪽에 "실제 islandName" 주석 추가. 필드명 리네이밍은 JSON 직렬화 키 변경 → 기존 저장 영지명 유실 위험이라 **보류**.

**후속:** ownerName→islandName 안전 리네이밍은 `@SerializedName("ownerName")` 등 JSON 키 고정 동반 시에만 별도 검토.

---

### DL-105 server-config/poro-rpg/seeds 화석 디렉토리 삭제 (DL-103 후속 리스크 해소)

**결정:** `server-config/poro-rpg/seeds/` 디렉토리를 통째로 삭제한다(추적 파일 10개). `server-config/mythicmobs/`는 보존.

**왜 (화석 확정 근거):**
- PoroRPG 시드 **정본은 jar 내장 `src/main/resources/seeds`** — 코드(`DefaultMasterSeedInstaller` 등)가 `plugin.saveResource(path, false)`로 런타임 datafolder에 추출하고 `getDataFolder()/seeds`에서 읽는다. server-config/seeds는 이 경로에 등장하지 않는다.
- server-config/seeds를 **DB로 import하는 스크립트·파이프라인이 어디에도 없다**. DL-1874가 언급한 "slot-specific(head/chest/legs/boots) 풀은 server-config DB용"은 실재하지 않는 옛 가정 — 코드는 `slot_type=armor`(generic) + `EquipmentService` 슬롯 매칭만 사용.
- server-config에서 실제 런타임으로 배포되는 건 **mythicmobs 셸뿐**(server_test_prep.md). seeds는 배포 대상이 아니다. 현재 런타임 `server/plugins/PoroRPG/seeds`가 정본과 동일 = jar 추출 상태인 것이 증거.

**해소한 리스크:** server-config/seeds는 옛 계보(item_master 등 4파일 스키마 분기 `t1_armor_head`/`t1_weapon_hammer` + 코드 미참조 분할표 4파일 + 정본 대비 21파일 누락). 누군가 이 화석을 런타임에 수동 복사하면 스타터 장비 붕괴(DL-103 경고) — 화석 제거로 함정 원천 차단.

**근거:** jar 내장 정본(`src/main/resources/seeds`, 빌드/런타임 사본 동일) 무손상. git 추적이라 필요 시 복원 가능.

---

### DL-104 무기별 독립 성장 + 무기 선택/변경 GUI 통합 (INBOX-007 2차)

**결정:**
1. **무기별 독립 성장** — 6종 무기는 각자 강화/등급/잠재/세부스탯을 **따로** 가져간다. 무기 교체 시 그 무기의 성장치가 적용된다. (기존: 모든 무기가 1개 인스턴스를 공유)
2. **무기 GUI 통합** — 튜토리얼 최초 "클래스 선택"과 캐릭터 "무기 변경"이 **같은 36칸 레이아웃**(스펙 `index.md §86`)을 재사용. 슬롯: 검10·도끼13·창16 / 석궁19·낫22·스태프25 / 뒤로27. 컨텍스트로 타이틀·뒤로가기만 분기.
3. **손에 든 무기 lore** — 무기 변경 GUI와 동일 형식(강화/등급/잠재/세부스탯/각인)으로 통일.
4. **창 아이콘 = TRIDENT** (기존 NETHERITE_SWORD 오류 정정, 선택·변경·물리 무기 전부).

**왜:**
- 무기 선택의 무게를 살리려는 사장님 설계 의도. 강화/큐브 투자가 무기별로 의미를 가진다.
- 구현이 저렴: 성장 모델(PlayerGrowthState)이 이미 "다중 인벤토리 아이템 + 슬롯별 장착"을 지원하고, 강화/큐브/잠재가 전부 `equippedItems().get(WEAPON)`을 대상으로 하므로 **교체 시 무기별 인스턴스(`weapon_<타입>`)를 WEAPON 슬롯에 장착**하기만 하면 독립화된다.

**구현:**
- `gui/WeaponGui.java`(슬롯·재질·표시명·instanceId 단일 소스), `gui/WeaponItemFactory.java`(손에 든 무기 lore).
- `ClassInitService`(스타터 = `weapon_<타입>` 인스턴스 + 36칸 GUI), `WeaponSelectionGuiListener`(공용 슬롯 매핑), `GrowthGuiListener`(무기 변경 GUI 36칸 + 무기별 인스턴스 보장·장착 + 무기별 lore).

**후속(미구현):**
- 손에 든 무기 lore 실시간 동기화(강화 직후 슬롯0 갱신) — 현재는 지급/교체 시점에만 빌드.
- 무기 변경 3분 쿨타임(CANON §55) — 현재 전투 중 차단만 구현.
- 무기별 각인 — 현재 각인은 클래스 단위 공유.

---

### DL-103 스타터 방어구 = 가상 전용 확정 (INBOX-007 ④)

**결정:** 스타터 방어구(견습 투구/흉갑/각반/신발)는 **가상 장비(growth state)로만 지급**한다. 바닐라 물리 방어구로 주지 않고, 캐릭터 메뉴 스탯(§2 DEF)으로만 적용한다. 외형/재질 선택 UI는 두지 않는다.

**왜:**
- 전투가 §2 곱연산 공식의 DEF 스탯으로 방어를 처리하므로, 바닐라 방어구를 주면 바닐라 armor point와 **이중 적용**된다.
- 무기만 물리 아이템인 것은 좌/우클릭 스킬이 손에 든 아이템(PDC 태그)으로 무기를 감지해야 하기 때문. 방어구는 그런 요구가 없다.
- 가상 장비는 표현을 GUI 아이콘으로 추상화하므로 material/외형 컬럼이 불필요 — item_master 스키마에 애초에 없다(slot_type+tier로 아이콘 결정).

**근거/현황:**
- 런타임 `item_master.csv`(src/main/resources → server/plugins/PoroRPG)에 `t1_helmet_starter`(방어 35)·`t1_chestplate_starter`(45)·`t1_leggings_starter`(40)·`t1_boots_starter`(30) 등록 확인. `ClassInitService.grantStarterEquipment`가 4슬롯 가상 장착. → ④는 설계대로 정상 동작이었고 코드 변경 없음.
- **후속(리스크)**: `server-config/poro-rpg/seeds/item_master.csv`가 다른 스키마(`t1_armor_head`, 방어 12/20/15/5)로 분기 — 오배포 시 스타터 장비 붕괴 함정. 정리 필요. → **[해소 → DL-105]** (2026-05-31, server-config/poro-rpg/seeds 화석 디렉토리 통째 삭제)

---

### DL-102 첫 접속 라우팅 + 영지(IS 섬) 자동 생성·이동 (INBOX-006 온보딩 ①)

**결정:** 첫 접속 온보딩을 "허브에서 무기 선택 → 영지(IS 섬) 자동 생성+이동"으로 연결. 튜토리얼 맵(안내 스텝)은 후속(②).

**구현:**
- `HubSpawnListener`: 접속 시 **전원 허브 이동**(첫 접속도 안전한 허브에서 무기 선택 — 필드 동적 스폰 위험 회피). `playerDataManager` 의존 제거.
- `WeaponSelectionGuiListener`: 무기 선택 완료 → 스타터 지급 후 **`is create poro`**(IridiumSkyblock, poro 스키매틱 강제로 스키매틱 GUI 회피) → IS가 비동기 생성 후 새 섬으로 자동 텔레포트.
- 흐름: 접속 → 허브 → (첫 접속) 무기 선택 GUI → 선택 → 스타터+도구 → 영지 생성·이동. 복귀 유저는 허브에서 `/필드`·`/영지` 분기.

**검증:** 부팅 클린(disable 0, world_hub OK, 에러 0). **실 온보딩 흐름(접속→허브→무기→섬)은 인게임 접속 검증 필요.**

**남은 단계 (온보딩 ②):** 첫 접속을 허브가 아닌 **튜토리얼 맵**으로 → 안내 스텝(이동·공격·메뉴) → 무기 선택 → 영지. `TutorialService`(빈 스텁) 구현. IS create 인자(`poro`)가 GUI를 여는 경우 대비(스키매틱명 직접 지정으로 회피 의도).

**영향 범위:** `HubSpawnListener`, `WeaponSelectionGuiListener`, `PoroRPGPlugin`. IS 스키매틱 키 `poro`(schematics.yml) 의존.

**관련:** INBOX-006, DL-101(허브 월드), `ClassInitService`(무기선택), IridiumSkyblock(`is create`).

---

### DL-101 허브 월드 + 접속 스폰 이동 — 온보딩 코어 (INBOX-006)

**결정:** 스폰을 **별도 평지 월드 `world_hub`**로 분리(사장님이 수도 건축). 접속 시 복귀 유저는 허브로 이동, 거기서 `/필드`·`/영지`로 분기. 단일 `world`(필드·보스·PvP)와 분리해 허브 전용 규칙(평화·세이프) 적용. 같은 월드 먼 좌표 대신 별도 월드 선택(허브가 필드 몹·규칙과 분리되도록).

**구현:**
- `HubWorldService`: 부팅 시 `world_hub` 보장(없으면 평지 생성, **표면 y=64**로 필드와 통일, PVP off·PEACEFUL·스톰 off). 스폰 (0,64,0). `PoroRPGPlugin` onEnable에서 `ensureHubWorld()`.
- `HubSpawnListener`: 접속 1틱 후(데이터 로드 완료) 무기 선택 완료(=복귀)면 허브 스폰 이동. 첫 접속(무기 NONE)은 온보딩 단계에서 분기.

**검증:** 부팅 시 world_hub 생성(폴더 2.2M, WorldGuard 적용), disable 0, 클린 부팅.

**남은 단계 (온보딩 2차):** 첫 접속 → 튜토리얼 맵(안내 스텝) → 무기선택 → 스타터 → **영지(IridiumSkyblock 섬) 자동 생성+이동**. `TutorialService`(현재 빈 스텁) 구현, IS 섬 생성 연동(현재 미연동), 첫접속 라우팅.

**영향 범위:** `HubWorldService`(신규), `HubSpawnListener`(신규), `PoroRPGPlugin`. 런타임 server/world_hub(git 밖).

**관련:** INBOX-006, DL-099(단일 평지 world), `ClassInitService`(무기선택=첫접속 감지), `final_master_plan §5`(수도).

---

### DL-100 동적 필드 스폰 시스템 — 코어 (INBOX-006, 1/2차)

**결정:** 고정 MM 스폰 대신 **플레이어 주변 웨이브 스폰**으로 필드 사냥을 구동한다(INBOX-006 확정 설계). 본 커밋은 **코어**(경계 판정 + 스케줄러 + 2단 캡 + 소유자 추적 + 기본 정리).

**구현 (`FieldSpawnService`):**
- 필드 경계 = config `fields.*` 중심 ±150(300×300). 필드 간격 500→**1000(700블록 갭)**으로 분리(config 수정). 필드 id→인덱스(plain1~ruins5).
- 스케줄러 15초 주기: 필드 내 각 플레이어 주변 20~30블록 지면에 일반몹 배치(웨이브 12). **2단 캡**: 플레이어당 40 + 필드 전체 250. mythicSpawner(reflection)로 필드별 MM 몹 소환.
- 몹↔소유자/필드 추적(`ownedMobs`/`mobField`). 매 틱 사망 몹 untrack + **오프라인 소유자 몹 디스폰**.
- 필드×모드 MM 몹 매핑 상수(일반 2종/정예 1종 × 필드5). 정예는 수 1/3.
- **일반/정예 토글은 `Predicate<UUID> eliteMode` 훅으로 분리** — 코어는 전원 일반(`uuid->false`), 2차에서 명령어·GUI 연결.

**검증:** 부팅 시 "동적 필드 스폰 활성 — 5개 필드, 주기 15s" 로그, disable 0·에러 0. (실 스폰은 필드 내 플레이어 필요 — 인게임 검증 대상.)

**남은 단계 (2차):** 일반/정예 모드 상태+명령어(`/필드 일반|정예`)+필드 GUI 토글, 랜덤 진입(타 플레이어 ≥35블록), 디스폰 정밀화(필드 이탈/원거리), 캡·웨이브 튜닝. PvP 친선사격은 이미 `PvpDamageListener`로 차단(추가 없음).

**영향 범위:** `FieldSpawnService`(신규), `PoroRPGPlugin`(등록), `config.yml`(필드 1000간격). 문서: INBOX-006.

**관련:** INBOX-006, DL-099(단일 평지 월드), `MobTagHelper`(poro_field_N), 필드 MM 셸(field_*.yml).

---

### DL-099 1차 시즌 단일 평지 월드 운영 (§5 4월드 분리 간소화)

**결정:** 1차 시즌 테스트 운영은 `final_master_plan §5`의 4월드 분리(world_main/world_boss/world_farm/world_test) 대신 **단일 평지 월드 `world`** + IridiumSkyblock 자체 영지 월드로 간소화한다.

**배경:** 필드·보스룸·PvP를 좌표로 분리(필드 스폰 근처 / 보스룸 X·Z 10000+ / PvP 20000+)하면 한 월드로 충분. 평지(superflat)는 무한 생성이라 크기 무관. 사용자 확정.

**구현:**
- `server.properties`: `level-type=minecraft:flat` + generator-settings(bedrock+stone124+dirt2+grass, **표면 y=64** → config 좌표 유지). 기존 `world` 재생성.
- `npc_spawn_seed.csv`(src+런타임): `world_main` → `world`. config.yml은 이미 전부 `world`.
- `/poro-genrooms world 10000 64 10000` → 보스룸 30개(6×5, 50³) 생성. `/poro-genarenas world 20000 64 20000` → PvP 아레나 10개 생성.

**검증:** 평지 월드 부팅·플러그인 클린(disable 0)·NPC 3명 스폰·보스룸 30/아레나 10 슬롯 로드 및 구조물 생성 완료. 클린 종료.

**한계/후속:** 필드 5종 좌표는 config 더미값(0/500/.../2000) 유지 — 평지 평면이라 텔레포트·몹 스폰은 동작하나 지형 장식 없음. season-start-epoch=0(주차 계산). 영지=IridiumSkyblock 월드 별도. 4월드 분리는 2차 시즌/확장 시 재검토 가능.

**영향 범위:** `npc_spawn_seed.csv`(src). 런타임 server/(git 밖): server.properties·config.yml·world. 문서: final_master_plan §5 노트.

**관련:** final_master_plan §5, server_test_prep.md §1, DL-098.

---

### DL-098 깨끗한 배포 부팅 검증 — 검증 과엄격 4건 해소 (런타임 부팅 통과)

**배경:** 신규 jar + src 정본 seed로 런타임을 배포(server/plugins)하니, 그간 stale 런타임 seed(`saveResource(replace=false)`로 미갱신)가 가려온 **검증 과엄격 버그 4건**이 연쇄로 표면화 — 플러그인이 부팅 중 disable. 실서버 띄워 부팅 로그로 1건씩 해소.

**해소 (전부 src 수정):**
1. **skill_master class_id required→optional** (`MasterCsvMappers.skill`): 1차 시즌은 클래스 미사용(무기 기반)이라 class_id가 빈 값인데 `required`라 파싱 실패. → `optional`.
2. **NPC sync 비fatal** (`PoroRPGPlugin`): `npc_spawn_seed.csv`가 정본 월드 `world_main` 참조 → 월드 미생성 시 fatal로 플러그인 disable. NPC는 비핵심이므로 실패해도 warn+continue(코어 정상 가동). `getNpcSyncRuntime()` 외부 호출처 0이라 null 안전.
3. **onDisable null 가드** (`PoroRPGPlugin`): 부팅 초기 실패 disable 시 `playerPersistenceService`가 null이라 onDisable이 2차 NPE로 진짜 원인 가림 → 가드.
4. **life/estate 검증 정합** (`LifeEngineBootstrap`): ①레시피·시설이 **바닐라 재료**(iron_block·diamond_block·carrot 등)를 쓰는데 커스텀 item_master로만 확인 → `Material.matchMaterial`로 바닐라 인식. ②공방 life_type(`crafting`/`workshop`)은 생활 스킬 레벨링이 없어 exp table 면제. ③`base_item_id="-"`(추상 시설=공방) 면제.

**검증 결과 (실부팅):** 8개 부트스트랩 전부 completed, disable 0, MythicMobs 감지 → 전투/보스 리스너 등록, Done. seed 로스터 정합(시즌6+최종3·강화표·잠재풀).

**남은 선행조건(코드 아님, 맵 작업):** 0개 보스룸 슬롯 로드·NPC sync 비활성 = 월드(world_main/boss) 미생성·`/poro genrooms` 미실행 때문. config 좌표·월드 + genrooms로 해소(server_test_prep.md). 운영 팁: 종료 시 `stop`으로 클린 셧다운(timeout kill은 session.lock 잔존→다음 부팅 크래시).

**영향 범위:** `MasterCsvMappers`, `PoroRPGPlugin`, `LifeEngineBootstrap`. (배포물 server/는 git 밖.)

**관련:** INBOX-005 2차, server_test_prep.md, DL-086~088(seed 정본).

---

### DL-097 보스 클리어 게이트 영속화 + 페이즈 틱 불요 확정 (INBOX-005 2차 후속 마무리)

**결정:**
- **클리어 영속화**: `BossRoomManager.clearedBosses`(in-memory, 재시작 소실 → #5 최종보스 게이트가 재시작 후 풀리던 문제)를 **boss_session에서 lazy 복원**으로 해소. 새 테이블 없이 기존 영속 데이터 재사용.
- **페이즈/패턴 진행 틱은 구현하지 않음(의도)**: MM이 패턴을 전부 구동(season_bosses.yml onTimer/onHealthPercent 117개)하고, 플러그인 `selectNextPattern`/`updateBossHp`는 런타임 호출처 0(휴면 추상화, 비-MM 미래용). 1차 MM 보스엔 구현 시 중복·충돌 → 스킵.

**구현 (클리어 영속화):**
- `BossSessionRepository.clearedBossIds(uuid)`: `result='clear'` 세션 참여 보스 id 집합 조회(boss_session_log ⋈ boss_session_player). 클리어는 이미 endRun→DbBossRunRecordHook로 boss_session에 영속(별도 write 불필요).
- `BossRoomManager`: `attachClearSource(Function)` + 게이트 첫 조회 시 `ensureClearsLoaded`로 1회 lazy 복원·캐시. `markCleared`(in-memory)는 세션 중 즉시 반영용으로 유지.
- `PoroRPGPlugin`: `bossRoomManager.attachClearSource(uuid → bossSessionRepository.clearedBossIds(...))`.

**영향 범위:** `BossSessionRepository`, `BossRoomManager`, `PoroRPGPlugin`. DDL/마이그레이션 추가 없음(기존 boss_session 재사용).

**관련:** DL-091(#5 게이트), DL-084(boss_session), INBOX-005 2차.

---

### DL-096 전투 공식 후속 — 보스 피해증가 + 치명 피해량 스탯 적용 (#7 잔여)

**결정:** DL-092(#7)에서 후속으로 미뤘던 두 계층을 중앙 데미지 적용부에 마저 배선한다.

**구현:**
- **boss_damage_increase(보스 피해증가) 적용**: 타깃이 보스일 때만 `× (1 + Σboss_damage_increase%/100)`. 보스 판정 = `MobTagHelper.isFieldBoss`(필드보스 태그 `poro_type_field_boss`) **또는** `BossDamageTracker.isTracked`(인스턴스 보스). `SkillContext`에 `BossDamageTracker` 주입.
- **치명 피해량 스탯 적용**: 고정 ×1.5 → **1.5 + critPts × 0.15%/pt**(치명 트리 부효과, `level_stat_system §2`, DL-061). 150pt 시 ×1.725. `potential_options_v1`의 stale 0.2%/pt도 0.15로 정정.

**최종 1차 피해 공식 (중앙 `BaseWeaponSkill.dealDamage`):**
`피해 = ATK(attack_percent 반영) × 계수 × (1+general%) × (보스면 1+boss%) × 치명배율(발동 시)`. DEF는 바닐라 armor 위임(커스텀 보스 DEF 시드 없음).

**영향 범위:** `SkillContext`, `BaseWeaponSkill`, `PoroRPGPlugin`(생성자), 문서 `potential_options_v1`.

**관련:** DL-092(#7 본체), DL-095(치명 확률), `level_stat_system §2`(치명 부효과 0.15%/pt), `growth_potential_option_pool.csv`(boss_damage_increase).

---

### DL-095 치명타 확률 %/pt 정합 — 0.30%/pt 확정 (문서 충돌 해소)

**결정:** 치명타 확률 스탯 계수를 **0.30%/pt**로 확정. `potential_options_v1`의 구 0.45%/pt 표기 폐기.

**배경 (DL-094 점검 중 발견):** `level_stat_system_v1 §3`(2026-05-16 재설계) = **0.30%/pt**(150pt=45%, "스탯만으로 치명 채우는 구조 방지 위해 하향") + 100% 캡 시나리오 완결. 반면 `potential_options_v1 §0-3` = 0.45%/pt + 그 전제의 100% math. → 직접 충돌. level_stat_system이 전용·최신·완결 설계라 정본.

**결과:**
- 정본 = 0.30%/pt. **#7 코드(`SkillContext.critChance` = critPts×0.003)는 이미 정합 — 코드 변경 없음.**
- `potential_options_v1` 0.45→0.30 교체 + 100% 시나리오를 level_stat_system §3과 동일하게 정정(레벨80 레전1개=102% / 65 레전+보조1=103.5% / 50 레전+보조2=105%).

**한계/후속:** 치명타 **피해량** 스탯(0.2%/pt, `potential_options §0-3`)은 #7 코드가 미적용(고정 ×1.5) — 별도 후속(boss%와 함께).

**영향 범위:** 문서 `potential_options_v1.md`. 코드 변경 없음.

**관련:** DL-092(#7 치명 0.3%/pt 사용), `level_stat_system_v1 §3`(정본), INBOX-005 2차.

---

### DL-094 잠재(큐브) 등급 모델 확정 — 메모리얼 승급 + 확률 강화 (INBOX-005 2차 #9 해소)

**결정:** 코드·문서 3원 분기였던 잠재 등급 모델을 **메모리얼 승급**으로 확정하고, drop_tables §6의 옛 플랫 분포를 폐기한다. 승급 확률은 레전더리를 "선발대 전용 희귀 달성"으로 강화.

**3원 분기 (감사 발견):**
- A `drop_tables_v1 §6`: 플랫 재롤(커먼40/레어35/에픽20/유니크5, 4등급, 등급 하락 가능).
- B `equipment_growth_spec §1.3`: 메모리얼 승급(5등급, 단조 상승) — 확률 수치 미정.
- 코드 `PotentialService`: 메모리얼 구현 + 하드코딩 확률(30/15/7.5/3%, 어느 문서에도 없음).

**확정:**
- **모델 = 메모리얼 승급**(B/코드). COMMON 시작, 큐브 1회 = 현재 등급 기준 확률로 한 칸 위 승급 시도(성공=승급, 실패=유지, 어느 쪽이든 옵션 재롤). **등급 하락 없음.** A(플랫)는 폐기.
- **승급 확률 강화** (사용자 선택, 0.30/0.15/0.075/0.03 → **0.25/0.12/0.06/0.015**): COMMON→RARE 25 / RARE→EPIC 12 / EPIC→UNIQUE 6 / UNIQUE→LEGENDARY 1.5%.
- **기대 큐브(누적):** 레어 ~4 / 에픽 ~12 / 유니크 ~29 / **레전더리 ~96**(≈48,000G, 큐브 ~960조각). `potential_options_v1 §"레전더리 = 전체 유저 0.5~1%"` 희귀성 의도와 정합. `economy_numbers_v2`의 "100회" 가정과도 수렴.

**결과:**
- 코드: `PotentialService.UPGRADE_CHANCE_BY_GRADE` 수치 교체(1곳).
- 문서: `equipment_growth_spec §1.3`(모호 표→확정 메모리얼 표), `drop_tables_v1 §6`(플랫표 폐기 표기), `economy_numbers_v2`(큐브 수요 ~96큐브로 재정합).

**한계:** 초기 등급은 GUI가 `generateInitial(COMMON)` 호출(코드 일치). 옵션 라인 이탈 확률(2라인10%/3라인5%)은 `potential_options_v1` 정본, 본 결정과 별개.

**영향 범위:** `PotentialService`. 문서: `equipment_growth_spec`, `drop_tables_v1`, `economy_numbers_v2`.

**관련:** INBOX-005 2차 #9, `equipment_growth_spec §1.3`(정본), `potential_options_v1.md`, DL-016(큐브 500G).

---

### DL-093 보스 전투 타임아웃 구동 (#10)

**결정:** 보스 런이 영영 끝나지 않아 슬롯·DB 세션이 영구 점유되던 문제(INBOX-005 2차 #10) 해소. `07_boss_pattern_modules §타임아웃`(필드·시즌1~6: 15분 / 최종3종: 10분)을 실제 구동하는 주기 스케줄러를 추가한다.

**구현:**
- `BossRunService`: `activeRunsSnapshot()` + `timeoutSecondsFor(bossId)`(최종3=600/그외=900) + `isTimedOut(run)`(enteredAt 기준). 최종보스 집합 = rift_king/corrupted_dyad/spirit_watcher.
- `BossDamageTracker.mobForRun(runId)`: 디스폰용 mob UUID 역조회(`endRun`의 finalizeShares가 매핑을 지우므로 종료 전에 캡처).
- `PoroRPGPlugin`: 10초(200틱) 주기 sync 스케줄러 — 경과 런마다 mob UUID 캡처 → `endRun(false,"timeout")`(→onRunEnded→releaseByRunId로 슬롯 회수) → 보스 mob `remove()` → 참가자 알림.

**한계:** 타임아웃 시 참가자 자동 텔레포트는 미구현(알림만 — 보스 디스폰으로 위험 제거, 수동 퇴장). 페이즈/패턴 진행 틱(`selectNextPattern`·`updateBossHp` 주기 호출)은 본 범위 밖(MM 패턴 실행과 연동 필요) — 별도. 정밀도 ±10초(스케줄러 주기).

**영향 범위:** `BossRunService`, `BossDamageTracker`, `PoroRPGPlugin`.

**관련:** INBOX-005 2차 #10, `07_boss_pattern_modules §타임아웃`, DL-091(#3 endRun 체인 선행).

---

### DL-092 전투 피해 공식 계층 배선 (#7, 경량 중앙 적용부)

**결정:** 스킬 피해가 `ATK × 계수`까지만 계산하고 잠재·치명을 무시하던 문제(INBOX-005 2차 #7)를 해소. `combat_balance_v2 §2` 공식 중 1차 시즌이 실제 쓰는 계층을 **경량 중앙 적용부**(`BaseWeaponSkill.dealDamage`)에 배선한다. 엔진(`CombatFormulaResolver`) literal 배선은 1차엔 과해(SkillMaster/스냅샷/태그·조건부 리졸버 조립 필요, 라이브 빌더 부재) 공식만 재사용.

**적용 (1차 시즌 옵션 범위):**
- **attack_percent** → `WeaponPowerCalculator`에서 ATK에 승산(`enhancedAtk × (1+ap/100)`). 기존 "모든 잠재 라인을 flat ATK로 합산"하던 버그 교정(general/boss를 ATK에 잘못 더하던 것 분리).
- **general_damage_increase(스킬피해%)** → `dealDamage`에서 `rawDamage × (1+Σgdi/100)`. 장착 5슬롯 잠재만 집계(`SkillContext.generalDamageMultiplier`, 여분 아이템 제외).
- **치명** → `critPts × 0.3%`(스탯 배분 치명 트리) 확률로 ×1.5(`CRIT_MULTIPLIER`, CANON §2 기본). 매 타격 독립 롤.

**의도적 미적용/위임:**
- **DEF/(DEF+200) 경감**: 1차 시즌은 커스텀 보스 DEF 시드가 없고(보스 능력치 MM 소관), `target.damage()`가 이미 바닐라 armor 경감을 적용하므로 추가 시 이중 경감 → **바닐라 armor에 위임**. MM이 보스 armor를 설정하면 그 값이 반영됨.
- **boss_damage_increase(조건부)**: 보스 타깃 판정 배선(인스턴스+필드보스)이 필요해 **후속**. 현재 무적용.
- 태그피해%·치명피해%: T2 옵션이라 1차 제외(애초에 미발급).

**구현:** 옛 `dealDamage(Player,LivingEntity,double)` 제거 → `dealDamage(SkillContext,Player,LivingEntity,double)`로 교체. 24개 호출부(23개 스킬)에 `ctx` 인자 일괄 추가(전부 execute 스코프라 안전). 전역 데미지-이벤트 리스너 방식은 `SkillInputListener`의 바닐라평타 취소·스킬 대체 재진입 흐름과 충돌해 배제.

**한계:** general%/치명을 매 타격 stat 재집계(다타깃 시 N회) — 경미한 비용, 필요 시 캐시. boss% 미적용 동안 잠재 boss_damage_increase는 무효. DEF는 MM armor 정합성에 의존.

**영향 범위:** `WeaponPowerCalculator`, `SkillContext`, `BaseWeaponSkill`, 23개 스킬(`combat/skills/**`).

**관련:** INBOX-005 2차 #7, `combat_balance_v2 §2`, DL-091(#6 ATK flat 테이블 선행), `level_stat_system_v1 §3`(치명 0.3%/pt).

---

### DL-091 서버 테스트 진입 전 코드 블로커 수정 (INBOX-005 2차 감사) — 진행 중

**배경:** 서버 테스트 진입 전 5도메인 병렬 코드↔기획 감사(INBOX-005 2차, 2026-05-31)에서 "엔진 로직은 있으나 이벤트 배선이 비어 있는" 블로커 다수 발견. 데이터·경제·DB·강화/흔적/EXP는 정합 양호. 사용자 결정: 코드 블로커부터 순차 수정(작은 것부터).

**완료 (이 커밋):**
- **#6 무기 ATK 선형→flat 테이블.** `WeaponPowerCalculator`가 `×(1+0.1L)` 선형(20강 240)이라 CANON flat 테이블(`combat_balance_v2 §1`, 20강 157)과 +53% 괴리 → 누적 가산 배열로 교체. 검증: 10강 103·18강 144·20강 157·25강 192 일치. (잠재 ATK 합산·DEF/치명/스킬% 계층은 #7에서 처리.)
- **#3 보스 처치→클리어 종료 브리지.** 보스 mob `EntityDeathEvent`→`endRun(true)` 리스너 부재로 잡아도 보상 0·슬롯 미회수였음. `BossInstanceDamageListener`에 사망 핸들러 추가(이미 보유한 `BossDamageTracker`의 신규 `runIdForMob`로 mob→runId 조회 후 `runService.endRun(runId,true,"")`). 보상·`markCleared`·`damage_share`는 기존 `endRun` 훅 체인(`BossRewardService.onRunEnded`, `DbBossRunRecordHook.finalizeShares`)에서 처리.
- **#5 최종보스 입장 게이트.** `AllowAllUnlockQuestChecker`(무조건 true)로 보스6 미클리어도 최종보스 입장 가능했음 → `BossClearUnlockQuestChecker` 신설(`quest_boss6_clear`=`void_herald` 클리어 시 해금, `boss_entry_rule.csv`/DL-044 일치). `BossRoomManager.hasCleared` 조회. 부트스트랩에 `bossRoomManager` 주입.
- **#4 보스 원샷 방지 85% 클램프.** 빈 스텁이던 `BossDefenseListener`를 구현 — 추적 보스(`BossDamageTracker.isTracked`) 대상 단일 타격을 maxHP×0.85로 상한 + 같은 타이밍(≈1틱) 후속 타격 0(다단 히트 우회 차단). `EventPriority.LOW`로 기여도 집계(NORMAL)보다 먼저 적용. 1차 시즌은 커스텀 보스 DEF 시드가 없어(보스 능력치 MM 소관) DEF/(DEF+200) 경감은 미적용, 원샷 방지만.

**#7 설계 결정 (구현은 후속 커밋):** `combat/engine/CombatFormulaResolver` literal 배선은 1차 시즌엔 과함 — 매 타격 `SkillMaster`+`SkillExecutionContext`+공/방 `CombatUnitSnapshot`+태그/조건부 리졸버 조립 필요, 라이브 빌더도 부재. 게다가 1차 공격 옵션은 `attack_percent`·`general_damage_increase`·`boss_damage_increase`(잠재)+`critPts`(스탯 배분, 0.3%/pt)뿐(태그피해·치명%는 T2). 결정: **엔진 공식(DEF/(DEF+200)·치명 구조)은 재사용하되 경량 중앙 적용부로 처리.** 위치는 `BaseWeaponSkill` 중앙 메서드(스킬이 `target.damage()` 직전 1회 적용) — 전역 `EntityDamageByEntityEvent` 리스너는 `SkillInputListener`가 바닐라 평타를 취소·스킬 대체하는 재진입 흐름과 충돌하므로 배제. ※부수 버그: 현재 `WeaponPowerCalculator`가 `attack_percent`(%)·general/boss 증가를 flat ATK로 잘못 합산 → #7에서 attack_percent는 ATK 승산, general/boss는 피해 승산으로 분리 교정 예정.

**한계:** #3의 보스 클리어 기록(`BossRoomManager.clearedBosses`)은 in-memory라 재시작 시 소실 → #5 게이트도 재시작 후 풀림. 영속화는 후속. #3은 단일 보스 엔티티 사망 기준이라 페이즈 전환으로 엔티티가 교체되는 보스는 조기 종료 가능(1차 시즌 바닐라 MM 셸은 동일 엔티티 HP 페이즈라 무해 추정).

**남은 블로커 (후속 커밋):** #7 피해 공식 계층(경량 중앙 적용부, 위 설계대로) + #10 보스 타임아웃·페이즈 틱 루프. 선행조건(코드 밖): config 좌표·월드명 실값, MM 셸 설치·ID 충돌.

**영향 범위:** `WeaponPowerCalculator`, `BossDamageTracker`, `BossInstanceDamageListener`, `BossClearUnlockQuestChecker`(신규), `BossEngineBootstrap`, `PoroRPGPlugin`. 문서: `idea_inbox.md`(INBOX-005 2차 감사).

**관련:** INBOX-005, DL-044(최종보스 게이트), DL-084(보스 데미지 추적), `combat_balance_v2 §1`.

---

### DL-090 강화 흔적 곱연산 전환 + 3종 토글 + 수량 재산정 (DL-089 개정)

**결정:** DL-089의 강화 흔적 보정 모델을 **가산(%p)·1종 선택 → 곱연산·3종 동시 토글**로 전환한다.
- 보정: `유효 성공률 = 기본율 × (1 + Σ 켠 흔적 보너스)`. 별 +0.15 / 달 +0.25 / 태양 +0.30. **전부 = ×1.70**.
- 강화 GUI: 별·달·태양 독립 ON/OFF 토글(슬롯 19/20/21) + 일괄 토글(18). 켠 흔적은 단계별 요구 수량을 전량 소모(all-or-nothing).
- 흔적별·단계별 요구 수량(시작값): 11~17강 별1/달1/태양1, 18~22강 별2/달1/태양1, 23~25강 별2/달2/태양1.
- 레시피 경량화(블럭 유지, 개수 조정): 별 마도6+다1+에1 / 달 마도12+다2+에2 / 태양 마도24+다4+에4.

**배경:** DL-089는 가산(+%p)·1종 모델이었으나, 사용자가 ①곱연산(기본율에 배수), ②3종 동시 사용(토글), ③보너스 15/25/30(합 70%→×1.7)으로 의도를 정정. 가산 모델은 고단계(저성공률)에서 흔적이 확률을 지배하는 문제가 있었고, 곱연산은 "기본율 비례"라 그 지배를 제거.

**경제 검토 (economy-reviewer 2라운드):**
- 곱연산에서 흔적 이득은 **1회 강화비용(골드+강화석×150)에 비례**(시도 횟수는 R에서 약분). 따라서 흔적 총비용이 1회 강화비용의 ~0.49배일 때 "흔적 포함 기대비용 ≈ 미사용의 0.875"가 성립 → 목표 1:0.85~0.9.
- 핵심 한계: 결과가 **마도합금·다이아블럭·에메랄드블럭의 골드 환산값**(추정치)에 지배적으로 민감. 블럭값이 높으면 흔적이 채산 미달, 낮으면 양호. 오픈 전 확정 불가.
- 사용자 결정: **메커니즘 확정 + 시작값 투입, 밸런스는 오픈 7일차 실측 재산정.** 수량은 `EnhancementService.traceCostForLevel`, 보너스는 `traceMultiplierBonus`, 레시피는 `workshop_crafting_spec §9` 단일 소스라 조정 용이.

**결과 (코드):**
- `EnhancementService`: `traceBonusFor`(%p) → `traceMultiplierBonus`(분수). `traceCostForLevel(traceId, currentLevel)`(흔적별 밴드). `attempt(...)` 흔적 인자 `String` → `Collection<String>`, 곱연산 적용·각 흔적 전량 소모. `EnhancementResult.traceId`는 소모 흔적 쉼표 결합 문자열(미사용 null) — `enhancement_log.trace_id`에 그대로 기록.
- `GrowthGuiListener`: 단일 순환 슬롯(38) → 4토글(18 일괄/19 별/20 달/21 태양). `enabledEnhanceTraces` 토글 집합 상태. 유효율 곱연산 표시, ON·부족 표기, 천장 미소모.

**한계/미확정:**
- 위 경제 가정(블럭·마도합금 시세) — 7일차 실측 재산정 필요. 별(+0.15) 단독 채산은 시세에 따라 경계.
- 흔적 종류 편중(태양 효율 최고) 가능 → 사용 비율 모니터링(운영 체크포인트).
- `db_event_log_spec`의 `trace_used` 전용 테이블은 미도입(enhancement_log.trace_id 통합 기록 유지).

**영향 범위:** `EnhancementService`, `EnhancementResult`, `GrowthGuiListener`. 문서: `equipment_growth_spec §3.4/§3.5`, `workshop_crafting_spec §9`, `gui_enhancement.md`. (DDL·DbEnhancementLogHook은 DL-089의 trace_id 컬럼 재사용.)

**관련:** DL-089(개정 대상), DL-024(흔적 레시피), `equipment_growth_spec §3.4`, `workshop_crafting_spec §9`.

---

### DL-089 강화 흔적 성공률 보정 연동 (INBOX-005 🟠 해소) [개정 → DL-090: 곱연산·3종 토글로 전환]

**결정:** 공방 생산만 되고 소비처가 없던 **강화 흔적 3종(별/달/태양)**을 `EnhancementService` 강화 성공률 보정에 연동한다. 강화 GUI에서 선택 → 영지 customItems에서 1개 소모하며 성공률에 가산 %p 보정(별 +20 / 달 +30 / 태양 +50). ※보정 모델(가산·1종)·수량(1/2/3)·레시피는 **DL-090에서 곱연산·3종 토글로 개정됨.** 흔적 미연동 부채 해소·10강 게이트·`enhancement_log.trace_id` 컬럼은 유효.

**배경:** `workshop_crafting_spec §9`·`equipment_growth_spec §3.4`에 흔적 효과·레시피는 확정돼 있었으나 구현이 생산 측에만 존재 — `idea_inbox.md` 감사에서 "강화 흔적 미연동(소비처 0)"으로 🟠 부채 기록. 사용자 확정: GUI까지 풀스택 연동.

**결과:**
- `EnhancementService`: `traceBonusFor(id)` + 5-arg `attempt(state, id, island, traceId, fixedRoll)`. 일반 롤 분기에서만 1개 소모(`withdrawCustomItem`), **천장(가호) 강제 성공 시 미소모**. 소모분은 `EnhancementResult.traceId`로 노출.
- **사용 조건 `현재 +10강 이상`**(`equipment_growth_spec §3.4` "10강 이상" = 아이템 현재 강화 단계 기준으로 해석). 미만에서는 선택·소모 모두 차단. → `EnhancementService.TRACE_MIN_LEVEL = 10`.
- 보정 계산: 가산(threshold + bonus/100), 1.0 클램프. 예) 25강 기본 0.05% + 태양 +50% = 50.05%.
- **요구 수량 단계별 증가 확정**: `traceCostForLevel(currentLevel)` — 목표 11~15강=1개 / 16~20강=2개 / 21~25강=3개(종류 무관). `equipment_growth_spec §3.4`의 빈 포인터("위 강화 테이블 참고" — 실제 컬럼 부재) 및 `workshop_crafting_spec §9`와의 모순을 수치 표로 정합화. 서비스는 해당 수량을 한 번에 소모, GUI는 요구 수량 미달 흔적을 선택 불가 처리.
- 강화 GUI(`GrowthGuiListener`): 흔적 선택 슬롯(38) 추가 — 미사용 → 별 → 달 → 태양 순환(요구 수량 이상 보유분만), 유효 성공률·요구/소모 수량 표시. 보유 부족/10강 미만 시 비활성.
- DB: `enhancement_log.trace_id` 컬럼 추가(`EnhancementLogDdl` CREATE + 기존 DB 멱등 ALTER via `EnhancementLogMigration` PRAGMA 체크). 흔적 사용 누적 분석 가능(`db_event_log_spec`의 `trace_used` 의도 충족). 소모 수량은 `before_level`로 역산 가능.

**수량 곡선 근거:** 흔적 보정은 flat %p라 저확률 고단계(18강 5%↓, 23강 1%↓)일수록 가치가 급상승 → 21~25강 3개로 비용 게이트. 초안 수치이며 단일 함수(`traceCostForLevel`)·`§3.4` 표로 손쉽게 튜닝 가능.

**한계:** 흔적 소모는 in-memory island 상태 차감 → `PlayerPersistenceService` 다음 저장 시 영속화(SuccessionService 전승 흔적과 동일 패턴). 별도 `trace_used` 전용 이벤트 테이블은 미도입(enhancement_log에 통합 기록).

**영향 범위:** `EnhancementService`, `EnhancementResult`, `DbEnhancementLogHook`, `EnhancementLogDdl`, `EnhancementLogMigration`, `GrowthGuiListener`. 문서: `gui_enhancement.md`.

**관련:** `poro-rpg/docs/idea_inbox.md` INBOX-005 #강화 흔적 미연동 해소. `workshop_crafting_spec §9`, `equipment_growth_spec §3.4`, DL-024(레시피).

---

### DL-088 영지 시설 오프라인 누적 생산 + 광물 채굴기 시드 (INBOX-005 🟠 해소)

**결정:** 영지 시설(약초 재배지·광물 채굴기) 생산을 온라인 전용 → **오프라인 누적**으로 전환한다. `storage_hours_cap`(Lv1 12h/Lv2 16h/Lv3 24h) 상한 적용.

**배경:** `MachineProductionScheduler`가 `Bukkit.getOnlinePlayers()`만 순회해 오프라인 생산이 없었다(INBOX-005 🟠). 그러나 `estate_facility_level_rule.csv`에 `storage_hours_cap`이 존재 — 기획 의도는 상한 있는 오프라인 누적. 사용자 확정: "오프라인 생산 구현".

**결과:**
- `IslandTerritoryState.lastProductionAt`(epoch ms) 추가 + 영속화(`PlayerSaveData.TerritorySaveData` 7번째 필드, 6-arg/5-arg legacy 호환).
- `MachineProductionScheduler.accrue(state, now)`: lastProductionAt 이후 경과 20분 인터벌 수만큼 누적 생산, 기계 레벨별 cap 상한. 영속화 덕분에 로그아웃→재접속 사이 오프라인분이 다음 틱(≤20분)에 정산 — 별도 onJoin 훅 불필요. 최초(lastProductionAt=0)는 소급 없이 기준점만 설정(기존 플레이어 안전 롤아웃).
- `estate_facility_master.csv`에 `estate_resonance_extractor`(광물 채굴기) 행 추가 — level_rule과 일관. life_type=`mining`(LifeType enum 유효값, "ore_mining"은 from() 예외).

**한계:** cap 시간(12/16/24h)을 스케줄러에 하드코딩(level_rule의 storage_hours_cap과 동일하나 런타임에 읽지 않음). 산출량·아이템도 스케줄러 하드코딩(estate_facility_master 레지스트리는 미연동 평행 시스템 — 별도 정리 대상).

**영향 범위:** `IslandTerritoryState`, `PlayerSaveData`, `PlayerPersistenceService`, `MachineProductionScheduler`, `estate_facility_master.csv`.

**관련:** `poro-rpg/docs/idea_inbox.md` INBOX-005 #영지 생산·#광물 채굴기 시드 해소. island_system_design.md §2.2.

---

### DL-087 보스 시드 정합 — boss_master를 정본(시즌6+최종3)에 동기화 (INBOX-005 🔴 해소)

**결정:** `boss_master.csv`를 정본 보스 로스터(필드5 + 시즌6 + 최종3)로 교체한다.

**배경 (감사로 발견된 모순):**
- 정본 보스 집합은 `BossRewardService.SEASON_REWARDS` + `season_bosses.yml`(MM 셸) + `boss_entry_rule.csv`가 일치: 시즌6(fallen_knight·corrupted_lord·stone_colossus·storm_sorcerer·abyss_guardian·void_herald) + 최종3(rift_king·corrupted_dyad·spirit_watcher). `final_master_plan §9`·DL-043과 일치.
- 그러나 `boss_master.csv`만 구 ID(시즌3: earth_tyrant·steel_arbiter·abyss_overlord, 최종1: rift_king)를 유지 → `BossMasterRegistry` 기반 통계/조회/`/보스` 목록이 실제 보스와 어긋남. 실제 시즌보스(fallen_knight 등)는 boss_master에 없어 표시명 누락.
- `fallen_knight`가 boss_master에서 필드보스(필드4)이자 정본 시즌보스1 → 동일 CSV 내 ID 충돌(중복 키).

**결과:**
- boss_master 시즌/최종을 정본 6+3으로 교체(표시명은 season_bosses.yml Display 기준). 구 ID 3종은 코드/설정 어디서도 참조 없음 확인 후 제거.
- 필드4 보스 ID 충돌 회피: `fallen_knight`(field) → `outpost_knight`로 분리. 필드보스는 field index(MobTagHelper) 기반 스폰·보상이라 ID는 레지스트리 메타데이터 — 안전.

**남은 서버 설정 이슈 (server-config, 본 수정 범위 밖):**
- ⚠️ **MM 셸 레벨 ID 충돌**: `field_outpost.yml`의 `Fallen_Knight` mob vs `season_bosses.yml`의 `fallen_knight` mob — 동일 이름(대소문자만 차이). MM이 대소문자 무시 시 `/보스 fallen_knight` 스폰이 필드/시즌 보스를 혼동할 위험. server-config 수정 필요(별도).
- 필드4 boss_master 표시명(초소 기사장) vs MM Display(타락 기사장) 경미한 불일치.

**영향 범위:** `boss_master.csv`(src). 코드 변경 없음.

**관련:** `poro-rpg/docs/idea_inbox.md` INBOX-005 #보스 시드(🔴) 해소. DL-043, final_master_plan §9.

---

### DL-086 강화 테이블 확정 수치 반영 + 방어구 강화석 보정 (INBOX-005 🔴 해소)

**결정:** `growth_enhancement_table.csv` T1을 `economy_numbers_v2.md`/DL-033 확정 수치로 전면 교체하고, 미구현이던 방어구 강화석 보정(`ceil(무기÷1.5)`)을 코드에 추가한다.

**배경 (감사로 발견된 불일치):**
- 기존 시드 T1: 11강 76%·20강 15%·1강 80G — DL-033(11강 25%·20강 1%·1강 2,000G) 미반영. 골드 싱크·강화석 수요·가호 천장 전부 어긋남.
- T1이 **20강까지만** 존재 → `MAX_ENHANCE_LEVEL=25`인데 T1 21강 시도 시 "rule not found" 차단.
- 방어구 강화석 `ceil(÷1.5)` 미구현 — 방어구도 무기 강화석 그대로 소모(스펙보다 1.5배 비쌈).

**결과:**
- T1 1~25강을 확정 표로 교체. 가호 천장은 코드 `ceil(200/성공률)`이 표 값과 자동 일치(11강 8회·20강 200회·25강 4,000회 검증).
- `EnhancementService`: 아이템 `slotType`이 weapon이 아니면 강화석 = `ceil(stoneCost/1.5)`. EnhancementResult·강화 로그도 실소모량 반영.
- `GrowthEngineBootstrap.validate`: "1~5강 100% 필수" → **"1~3강 100%"로 완화** (확정 표가 4강 95%·5강 90%이므로 기존 검증과 충돌, 스펙 우선).
- T2(1차 시즌 미사용)는 **유지** — validate가 전 tier 1~25강 룰을 강제하므로 제거 시 부트스트랩 실패. inert 데이터로 보존.

**한계/후속:** 배포 사본(`server-config/`, `server/plugins/PoroRPG/seeds/`)은 미수정 — 다음 배포 시 동기화 필요. T2 완전 제거는 validate를 T1 한정으로 바꿔야 하는 별도 작업.

**영향 범위:** `growth_enhancement_table.csv`(src), `EnhancementService`, `GrowthEngineBootstrap`.

**관련:** `poro-rpg/docs/idea_inbox.md` INBOX-005 #강화 테이블(🔴) 해소. economy_numbers_v2.md §강화 비용표, DL-033.

---

### DL-085 바닐라 경험치 바 억제 — 커스텀 레벨링만 노출

**결정:** 바닐라 마인크래프트 경험치(초록 XP 바)를 전면 억제한다. PoroRPG는 커스텀 레벨링(`PlayerLevelingService`, HUD 표시)을 쓰므로 바닐라 XP 바는 노출하지 않는다.

**이유:**
- 커스텀 XP 바(HUD)가 이미 있어 바닐라 XP 바와 병존하면 혼란(두 개의 XP 시스템처럼 보임). 사용자 확인: "커스텀 xp 바가 있으니 바닐라 xp 바는 안 보이는 게 맞다."
- 커스텀 레벨링은 `level_stat_system_v1.md §5`대로 필드 몹 사냥에서 수급(이미 구현·일치). 바닐라 XP는 RPG에서 용도 없음(강화는 커스텀, 바닐라 인챈트 미사용).

**결과 (코드):**
- `VanillaExpSuppressListener`(신규): `PlayerExpChangeEvent`→0(모든 획득 차단), `EntityDeathEvent.setDroppedExp(0)`(오브 미생성), `PlayerJoinEvent`에서 바닐라 level/exp 0 리셋(기존 바 비우기). MM 게이트 밖 항상 등록.
- 커스텀 레벨링(`FieldDropListener` → `addExp`)은 별개로 그대로 동작.

**한계:** 바닐라 XP 바 트로프(빈 틀)는 API로 완전 숨김 불가 — 빈 상태로 표시됨(리소스팩으로 별도 숨김 가능). 바닐라 인챈트/모루 XP 비용 기능을 향후 쓰려면 재검토 필요.

**영향 범위:** `VanillaExpSuppressListener`(신규), `PoroRPGPlugin`(등록).

---

### DL-084 보스 데미지 기여 추적 — damage_share 실측 (데이터 공백 7/7 완료)

**결정:** 인스턴스 보스(보스룸) 처치 시 참여자별 데미지 점유율(%)을 `boss_session_player.damage_share`에 기록한다.

**이유:**
- "누가 보스 처치에 얼마나 기여하는가"는 보스 밸런스·파티 기여 분석의 핵심인데, damage_share가 테이블에 없고 placeholder 0.0이었다 (INBOX-004 #5, DL-064).

**구현 (핵심 — 추적 대상 식별 메커니즘 신설):**
- **mob UUID 캡처:** `mythicSpawner` 반환을 `Boolean` → `UUID`로 변경. reflection `spawnMythicMob`이 반환하는 `Entity`의 UUID 추출(실패 시 null).
- `BossDamageTracker`(신규): 범용 `ContributionTracker` 래핑 + mob UUID ↔ runId 매핑. registerMob/recordDamage/finalizeShares.
- `BossInstanceDamageListener`(신규): `EntityDamageByEntityEvent` → 추적 대상 보스면 가해자별 누적(직접/투사체). MM 활성 시 등록.
- `BossRoomListener`: 스폰 성공 시 `registerMob(runId, mobUuid)`.
- `DbBossRunRecordHook.onRunEnded`: `finalizeShares(runId)` → 참여자별 `recordPlayerDamage(sessionId, uuid, share)`.
- `boss_session_player.damage_share` 컬럼 추가(CREATE + `BossSessionPlayerMigrationV3` ALTER, PRAGMA idempotent).

**한계:** 보스가 소환한 add(소환수)는 다른 UUID라 미집계(메인 보스 데미지만). 페이즈 전환으로 엔티티 교체 시 추적 끊김(1차 시즌 단일 엔티티 전제). reflection `spawnMythicMob` 반환 타입(Entity) 런타임 의존 — MM 버전 통합 테스트 필요. damage_total(원시값)은 미저장(share만).

**영향 범위:** `BossDamageTracker`/`BossInstanceDamageListener`/`BossSessionPlayerMigrationV3`(신규), `mythicSpawner`(plugin), `BossRoomListener`, `BossSessionDdl`, `BossSessionRepository`, `DbBossRunRecordHook`, `BossEngineBootstrap`, `PoroRPGPlugin`, `CommonFoundationBootstrap`.

**관련:** `poro-rpg/docs/idea_inbox.md` INBOX-004 #5 (PROMOTED). **데이터 수집 공백 7종 전부 해소 — INBOX-004 완료.**

---

### DL-083 성장 시계열 스냅샷 — 성장 곡선 판단

**결정:** `growth_snapshot` 테이블에 온라인 플레이어 성장(레벨·평균 IL·총 강화·골드)을 30분 주기로 일별 upsert한다.

**이유:**
- 플레이어 JSON은 현재값 덮어쓰기라 "며칠차에 평균 몇 레벨/강인지" 성장 곡선을 알 수 없었다 (INBOX-004 #7). 시즌 진행에 따른 성장 속도·정체 구간 판단 불가.

**결과 (코드 구조):**
- `growth_snapshot` (player_uuid, snapshot_date, player_level, avg_il, total_enhance, gold, captured_at), PK(player_uuid, snapshot_date). `GrowthSnapshotMigration` 등록.
- 스케줄러: 30분(36000틱)마다 메인 스레드에서 온라인 플레이어 상태 읽어 행 구성 → async DB upsert. `(uuid, date)` 충돌 시 갱신 → 하루 1행(그날 마지막).
- 읽기 API: `GET /api/v1/growth/curve?days=N`(기본 45=시즌 길이) — 일별 평균 레벨·IL·골드 + 집계 플레이어 수.

**한계:** 30분 주기 스냅샷이라 그 사이 변동은 미포착(일별 평균엔 무관). 오프라인 기간은 행 없음(그날 미접속). 개별 성장 추적이 아닌 모집단 평균 곡선용.

**영향 범위:** `GrowthSnapshotDdl`/`GrowthSnapshotMigration`/`GrowthSnapshotRepository`(신규), `PoroRPGPlugin`(스케줄러+행 구성), `GrowthApiHandler`(신규), `PoroHttpServer`, `OperationsQueryBootstrap`, `CommonFoundationBootstrap`.

**관련:** `poro-rpg/docs/idea_inbox.md` INBOX-004 #7 (PROMOTED). 남은 공백 1종(#5 보스 데미지 기여 — 런타임 데미지 추적).

---

### DL-082 PvP 매치 무기/IL 기록 — 클래스 밸런스 판단

**결정:** `pvp_match_log`에 winner_weapon·winner_il·loser_weapon·loser_il 컬럼을 추가하고 매치 종료 시 양측 무기(클래스)·실측 평균 IL을 기록한다.

**이유:**
- "어느 직업/IL이 PvP에서 강한가"는 클래스 밸런스 판단의 핵심인데 매치 로그에 무기·IL이 전혀 없어 분석 불가였다 (INBOX-004 #6).
- IL은 **랭크 가상화(12강 IL 60) 전의 실측값** 기록 — 참가자 실제 스펙 분포를 봐야 밸런스를 판단할 수 있음.

**결과 (코드 구조):**
- `PvpDdl` CREATE에 4컬럼 추가(신규 DB) + `PvpMatchLogMigrationV2`(기존 DB ALTER, PRAGMA로 idempotent).
- `PvpMatchService.endMatch`: `weaponOf(uuid)`(classId)·`ilOf(uuid)`(5슬롯 강화×5) 계산 → `record(...)`에 전달. 무승부/상태없음은 null.
- `PvpMatchLogRepository.record` 시그니처에 4파라미터 추가, `weaponWinRates()` 집계(무기별 승/패/승률) 추가.
- 읽기 API: `GET /api/v1/pvp/balance` (신규 `PvpApiHandler`) — 무기별 승률.

**한계:** 무승부·플레이어 상태 소실(quit) 시 해당 측 무기/IL은 null. 데미지량(누가 얼마 때렸는지)은 미수집(별도).

**영향 범위:** `PvpDdl`, `PvpMatchLogMigrationV2`(신규), `PvpMatchLogRepository`, `PvpMatchService`, `PvpApiHandler`(신규), `PoroHttpServer`, `OperationsQueryBootstrap`, `CommonFoundationBootstrap`.

**관련:** `poro-rpg/docs/idea_inbox.md` INBOX-004 #6 (PROMOTED). 남은 공백 2종(#5 보스 데미지 기여·#7 성장 시계열).

---

### DL-081 보스 참여자 스펙 실측 — placeholder 0/NULL 해소

**결정:** `boss_session_player`의 weapon_enhance·avg_enhance·il, `boss_session_log`의 party_avg_enhance·party_avg_il을 보스 입장 시점 실측값으로 기록한다. (기존 DL-064/§7+ 부채 해소.)

**이유:**
- "어느 강화도/IL에서 보스를 깨는가"는 보스 밸런스 판단의 핵심인데, 참여자 스펙·파티 평균이 전부 placeholder(0/NULL)라 `boss_stats_summary`의 avg_party_il/enhance가 무의미했다 (INBOX-004 #4).

**결과 (코드 구조):**
- `BossParticipantSpec`(weaponEnhance·avgEnhance·il) + `BossParticipantSpecResolver` 인터페이스(신규) — 보스 엔진을 growth에 직접 결합하지 않도록 분리. 구현(5슬롯 강화 → IL 계산, 강화 1당 IL 5)은 플러그인 와이어링(`PoroRPGPlugin.resolveBossParticipantSpec`)이 제공.
- `DbBossRunRecordHook.onRunStarted`: 참여자별 resolver로 스펙 계산 → `recordPlayerEntry(sessionId, uuid, spec)`, 누적 후 `recordPartySpec(sessionId, avgEnhance, avgIl)`로 파티 평균 UPDATE.
- `BossSessionRepository.recordPlayerEntry` 시그니처에 spec 추가, `recordPartySpec` 신규.

**한계:**
- **defense_ignore_pct / has_defense_ignore는 0 유지** — 1차 시즌 방무 메커니즘 없음(CANON 방깎 제외). `boss_stats_summary.defense_ignore_rate_pct`도 0 고정(설계상 정상).
- **damage_share / 데미지 기여는 별개(#5)** — 본 작업은 입장 시점 스펙만. 런타임 데미지 추적은 미구현.
- 본 변경 이전 종료 세션은 party_avg가 NULL로 남음(AVG 집계에서 자동 제외).

**영향 범위:** `BossParticipantSpec`/`BossParticipantSpecResolver`(신규), `BossSessionRepository`, `DbBossRunRecordHook`, `BossEngineBootstrap`, `PoroRPGPlugin`.

**관련:** `poro-rpg/docs/idea_inbox.md` INBOX-004 #4 (PROMOTED). DL-064(damage_share placeholder)는 #5로 별도. 남은 공백 3종(#5·#6·#7).

---

### DL-080 통화 흐름 로그 — 골드 인플레이션/싱크(발행량 vs 소각량) 수집

**결정:** 지갑 변동을 `economy_flow` 테이블로 기록한다. `PlayerGrowthState.addCurrency`(inflow)/`consumeCurrency`(outflow)에 `CurrencyFlowListener`를 부착하고, 로드 복원만 신규 `restoreCurrency`로 우회하여 제외한다.

**이유:**
- 45일 시즌 경제에서 골드 인플레이션 감지가 핵심인데 `addEconomyFlow` 호출처가 0건이라 "발행량 vs 소각량"을 전혀 알 수 없었다 (INBOX-004 #2, 죽은 모델).
- **계측 방식 결정 (scope):** 골드 변동 지점이 ~12곳(드랍·퀘스트·보스·강화·큐브·상점·경매·영지 등)이고 일부는 환불/롤백·플레이어간 이체가 섞여, 개별 site 계측은 침습적이고 엣지가 많다. **지갑 레벨 후킹 + 로드 우회** 방식은 환불이 inflow로 잡혀 직전 outflow와 net 상쇄, 경매 이체도 buyer/seller가 net 상쇄되어 **net(발행-소각)이 누락·중복 없이 정확**하다.

**결과 (코드 구조):**
- `economy_flow` (player_uuid, direction, currency, amount, occurred_at, flow_date) + 인덱스. `EconomyFlowMigration` 등록.
- `PlayerGrowthState`: addCurrency/consumeCurrency가 `CurrencyFlowListener.onFlow` 발화. 신규 `restoreCurrency`(무발화)를 `PlayerPersistenceService.load`가 사용. `GrowthStateStore.attachFlowListener`로 모든 상태에 주입.
- `EconomyFlowRepository`(CurrencyFlowListener 구현, write+read). 읽기 API: `GET /api/v1/economy/flow` — 통화별 발행/소각/net + 골드 30일 일별 net.

**한계 / 트레이드오프:**
- **source 미세분(어느 faucet/sink가 큰지)은 미제공.** 지갑 레벨이라 변동 원인을 모른다. net 추세(인플레이션 여부)는 정확. source별 분해는 후속(개별 site에 source 태그 주입) 시 확장.
- **gross(총 inflow)는 경매 이체분만큼 부풀려짐** — net은 정확하나 "총 발행량"으로 해석 시 transfer 노이즈 포함. 분석은 net 기준 권장.
- 전 통화(골드+재화) 기록. 드랍 등 고빈도 변동마다 INSERT — 부하 우려 시 시간버킷 집계로 배치화 가능(후속).

**영향 범위:** `CurrencyFlowListener`(신규), `PlayerGrowthState`, `GrowthStateStore`, `PlayerPersistenceService`, `EconomyFlowDdl`/`EconomyFlowMigration`/`EconomyFlowRepository`(신규), `PoroRPGPlugin`, `EconomyApiHandler`, `OperationsQueryBootstrap`, `CommonFoundationBootstrap`.

**관련:** `poro-rpg/docs/idea_inbox.md` INBOX-004 #2 (PROMOTED). 남은 공백 4종(#4~#7). 죽은 모델 `EconomyFlowRecord`(in-memory)는 본 DB 방식으로 대체 — 미사용 유지.

---

### DL-079 강화 로그 DB 영속화 — in-memory 휘발 해소, 성공률 검증 가능

**결정:** 강화 시도 로그를 `enhancement_log` 테이블로 DB 영속화한다. 기존 `InMemoryEnhancementLogHook`(관리자 GUI 최근 조회용)는 유지하고, `DbEnhancementLogHook`을 추가해 `CompositeEnhancementLogHook`으로 합성 — in-memory와 DB에 동시 기록.

**이유:**
- 강화 로그는 이미 풍부한 데이터(성공/실패·roll·골드/강화석 비용·천장 발동)를 담고 있었으나 in-memory라 재시작 시 소실, 누적 판단 불가였다 (INBOX-004 #3).
- 표기 성공률 vs 실제, 유저당 강화 부담(골드·강화석 총 소모), 천장 발동 빈도는 시즌 경제 밸런스 판단의 핵심.
- 기존 로그가 충분해 DB hook 1개 + 테이블 1개만 추가하면 되는 가장 적은 작업의 공백 해소.

**결과 (코드 구조):**
- `enhancement_log` (player_uuid, item_id, tier, before/target/final_level, success, success_rate, roll, gold_cost, stone_cost, forced_ceiling, attempted_at) + 인덱스 2종. `EnhancementLogMigration` 등록.
- `CompositeEnhancementLogHook([inMemory, db])`을 `EnhancementService`에 주입. runtime은 in-memory hook을 계속 노출(관리자 GUI `logs()` 호환).
- 읽기 API: `GET /api/v1/economy/enhancement` — 요약(표기 vs 실제 성공률·총 소모) + 티어·단계별 성공률.

**영향 범위:** `EnhancementLogDdl`, `EnhancementLogMigration`, `DbEnhancementLogHook`(신규, write+read), `CompositeEnhancementLogHook`(신규), `GrowthEngineBootstrap`, `EconomyApiHandler`(신규), `PoroHttpServer`, `OperationsQueryBootstrap`, `CommonFoundationBootstrap`.

**관련:** `poro-rpg/docs/idea_inbox.md` INBOX-004 #3 (PROMOTED). 남은 공백 5종(#2·#4~#7).

---

### DL-078 접속 세션 로그 — 리텐션·DAU·플레이타임 수집 테이블 신규

**결정:** `player_session_log` 테이블을 신규 추가해 플레이어 접속/종료 세션을 DB 영속 기록한다. 운영 판단용 활동 지표(DAU·플레이타임·리텐션)의 데이터 소스.

**이유:**
- 45일 시즌 서버에서 가장 치명적인 운영 지표는 **유저 잔존(리텐션)** 인데, 기존에는 접속 시각조차 기록하지 않아 측정 불가였다 (INBOX-004 #1 검토 결과).
- 데이터 수집 충분성 검토(2026-05-30)에서 활동 추적이 "완전 미수집"으로 분류됨.

**결과 (코드 구조):**
- `player_session_log` (id, player_uuid, player_name, joined_at, quit_at, duration_s, session_date) + 인덱스 3종. `PlayerSessionMigration`을 마이그레이션 체인에 등록.
- 쓰기: `PlayerJoinListener.onJoin` → `recordJoin`(quit_at NULL INSERT), `onQuit` → `recordQuit`(최근 열린 세션 UPDATE + duration 계산). `onDisable` → `closeOpenSessions`로 정상 종료 시 마감.
- 읽기 API: `GET /api/v1/activity/summary`(총 세션·고유 플레이어·평균 세션 길이), `GET /api/v1/activity/dau?days=N`(날짜별 DAU).
- 크래시로 quit 이벤트 누락 시 세션은 quit_at=NULL → 플레이타임 집계 제외, DAU에는 포함.

**영향 범위:** `PlayerSessionDdl`, `PlayerSessionMigration`, `PlayerSessionRepository`(신규), `PlayerJoinListener`, `PoroRPGPlugin`, `ActivityApiHandler`(신규), `PoroHttpServer`, `OperationsQueryBootstrap`, `CommonFoundationBootstrap`.

**관련:** `poro-rpg/docs/idea_inbox.md` INBOX-004 #1 (PROMOTED). 나머지 공백 6종(#2~#7)은 미구현 잔존.

---

### DL-070 스킬 이펙트 — 1차 시즌은 Bukkit Particle 직접 구현, MythicMobs 이펙트 키 사용 안 함

**결정:** 모든 무기 스킬의 시각 이펙트(파티클·사운드)는 1차 시즌 동안 Bukkit API(`World.spawnParticle`, `World.playSound`)로 직접 구현한다. `weapon_skills_v1.md`의 `effect_key: mm:xxx / dp:xxx` 컬럼은 향후 참고용으로만 남기고 현재는 사용하지 않는다.

**이유:**
- MythicMobs 이펙트 키 연동은 MythicMobs 스킬 yaml 별도 관리와 플러그인 간 이벤트 브리지가 필요해 1차 시즌 구현 범위를 초과한다.
- 바닐라 파티클만으로 낫·검 등 각 무기 색감(보라/붉은/파랑)을 충분히 표현 가능하다.
- `BaseWeaponSkill`에 `spawnParticleArc`, `spawnParticleCircle`, `spawnParticleLine`, `spawnSlashEffect`, `spawnImpactEffect`, `playSound` 헬퍼를 추가해 각 스킬에서 간단히 호출한다.

**영향 범위:** `BaseWeaponSkill.java` (헬퍼 추가), 각 스킬 구현체 (이펙트 호출). `weapon_skills_v1.md` `effect_key` 컬럼 주석 불필요 — 스펙 문서는 변경하지 않는다.

---

### DL-066 영지 작위 승급 재료 — 전장의 파편 + 골드만으로 확정

작위 승급 소비 재료를 **전장의 파편(`mat_battle_shard`) + 골드**만으로 확정한다.

- `island_system_design.md` 초안에 자작령 달의 흔적 3개, 백작령 태양의 흔적 2개가 포함되어 있었으나,
  달/태양 흔적은 공방 제작 아이템으로 획득 경로가 별도 시스템을 요구함.
- 1차 시즌 범위 내에서 WorkshopGui 레시피가 구현되기 전까지 승급 경로를 막지 않기 위해 제외.
- `island_system_design.md` 승급 테이블에서 흔적 재료 칸 제거 완료.
- `IslandRank.java` 모든 단계 upgradeMaterials에서 mat_trace_moon/sun 제거.
- mat_trace_star/moon/sun은 **강화 보정 아이템**(공방 제작)으로 존재, 승급과 무관.

---

### DL-065 보스 세션 진행 중 상태 — result='abandoned' 초기값 + ended_at IS NOT NULL 뷰 필터

**결정:**
- `boss_session_log` INSERT 시 `result='abandoned'`을 초기값으로 사용 (CANON 원안의 `result=NULL` 대신).
- `boss_stats_summary` VIEW에 `WHERE ended_at IS NOT NULL` 조건을 추가해 진행 중 세션을 통계에서 제외.
- 서버 크래시로 종료된 세션(`result='abandoned'`, `ended_at=NULL`)은 통계 제외 — 허용 가능한 트레이드오프.

**이유:** SQLite `ALTER TABLE`로 컬럼 제약을 변경하려면 테이블 전체 rebuild가 필요하다. `NULL` vs `'abandoned'` 초기값은 VIEW 필터를 통해 기능적으로 동일하므로 schema 변경 비용 대비 실익 없음.

**영향 범위:** `BossSessionDdl.CREATE_SESSION_LOG` (DEFAULT 'abandoned'), `BossSessionRepository.recordStart()`, `BossSessionDdl.CREATE_STATS_SUMMARY_VIEW` (WHERE ended_at IS NOT NULL).

**관련:** `poro-rpg/docs/02_database_api_stats/boss_clear_stats_spec.md` §5 업데이트 완료.

---

### DL-064 시즌보스 참여 기준 — 보스룸 입장 확인, damage_share 집계 1차 시즌 제외

**결정:**
- 시즌보스 참여 기준: **파티 보스룸 입장 확인** (방 입장 = 참여 게이트).
- 필드보스 기준인 "총 피해량 3%"는 오픈필드 무임승차 방지용; 1~3인 제한 보스룸에는 적용 안 함.
- `BossResultSummaryBuilder`의 `damage_share`는 1차 시즌 `0.0` placeholder 유지. 실제 집계 연결은 §7+ 보스 패턴 구현 단계로 이관.
- **CANON §6 및 drop_tables_v1.md §시즌보스 보상 구조 반영 완료** (2026-05-25).

**이유:** `BossDefenseListener` stub 상태, MythicMobs 기반 시즌보스 데미지 이벤트 연결 미완. 보스룸 입장 자체가 참여 확인으로 충분.

**근거:** 사용자 확인 (2026-05-25). CANON.md + drop_tables_v1.md 동기화 완료.

---

### DL-063 튜토리얼 완료 → 영지 자동 생성 + 섬 도구 지급 흐름 확정

**결정:** 튜토리얼 완료(무기 선택) 시점에 IridiumSkyblock 섬을 자동 생성하고, 영지로 텔레포트하면서 섬 도구 세트를 함께 지급한다.

**튜토리얼 흐름:**
```
튜토리얼 방 입장
  → 서버 규칙 읽기
  → 무기 선택 GUI (27슬롯, "클래스 선택")
    → RPG 장비 5슬롯 지급 (WEAPON + 방어구 4종, ClassInitService)
    → IridiumSkyblock 섬 자동 생성 (/is create 또는 API 호출)
    → 섬 도구 세트 지급 (아래 명세)
    → 영지(IridiumSkyblock 세계)로 텔레포트
```

**섬 도구 세트 (ClassInitService.grantIslandTools):**

| 아이템 | 인챈트 | 비고 |
|---|---|---|
| 네더라이트 곡괭이 | Efficiency V + Fortune III | `setUnbreakable(true)` |
| 네더라이트 삽 | Efficiency V + Fortune III | `setUnbreakable(true)` |
| 네더라이트 괭이 | Efficiency V + Fortune III | `setUnbreakable(true)` |
| 네더라이트 도끼 | Efficiency V + Fortune III | `setUnbreakable(true)` |

**정책:**
- 모루 사용 불가 (WorldGuard 또는 이벤트로 차단 예정)
- 인챈트 시스템(상점 인챈트북) 불필요 — 도구에 풀 인챈트 박아서 지급
- Silk Touch 곡괭이 미지급 — Fortune III 1종으로 통일
- 내구도 소모 없음 (`setUnbreakable(true)`, Mending/Unbreaking 미사용)

**근거:** 45일 시즌 서버 특성상 유저가 바닐라 도구 파밍 루프에 시간을 쏟지 않고 RPG 콘텐츠(전투·성장·영지 생산)에 바로 집중할 수 있도록 한다.

---

### DL-062 공방 광물 변환 탭 — 원석 → 주괴 정정 + 금 주괴 소모처 확정

**결정:** `tab_ore_convert`의 모든 교환 재료가 **원석(Ore)이 아닌 주괴(Ingot)**임을 확정. 문서 오기 수정.

| 교환 재료 | 기존 표기 | 확정 표기 |
|---|---|---|
| 철 변환 (다이아) | 철 원석 × 32 | **철 주괴 × 32** |
| 금 변환 (다이아) | 금 원석 × 16 | **금 주괴 × 16** |
| 철 변환 (에메) | 철 원석 × 48 | **철 주괴 × 48** |
| 금 변환 (에메) | 금 원석 × 24 | **금 주괴 × 24** |

**금 주괴 소모처 해소:** 금 주괴는 공방 광물 변환 탭이 유일 소모처. 별도 NPC 매입 불필요.

**이유:** 제련 후 주괴 형태가 인벤토리에 쌓이는 구조이므로 원석 요구는 UX상 부자연스러움. 공방은 가공된 재료 투입이 일관된 설계.

**근거 문서:** `poro-rpg/docs/05_island_farm_system/workshop_crafting_spec.md §4`, `poro-rpg/docs/02_database_api_stats/economy_numbers_v2.md §8`

---

### DL-061 스탯 트리 치명 부효과(치피) 수치 확정

**결정:** 치명 트리 부 효과 치명타 피해량% **+0.15%/pt** 확정.

| 트리 | 주 효과 | pt당 | 부 효과 | pt당 |
|---|---|---|---|---|
| 치명 | 치명타 확률% | +0.30% | 치명타 피해량% | **+0.15%** |
| 특화 | 스킬 피해% | +0.30% | 태그 피해% | +0.15% |
| 인내 | 받는 피해 감소% | +0.15% | 방어력 | +0.4 |

레벨당 스탯 포인트 지급량: **3pt** (기존 확정 유지).

**이유:** 기존 +0.20%/pt 초안 대비 하향. 치피 스텟만으로 얻는 이득을 줄여 치확 또는 잠재에 더 의존하게 만드는 설계 방향. 레벨 50 치명 all-in 시 치명 vs 특화 DPS 격차 6.3% → 설계 목표 5~10% 내 유지.

**근거 문서:** `poro-rpg/docs/04_combat_weapon_skills/level_stat_system_v1.md §2, §4`

---

### DL-060 daily_economy_snapshot 집계 방식 확정

**결정:** **메모리 누적 + 10분 주기 플러시** 방식 채택.

- 이벤트 발생 시 `Map<String, Long>` 메모리 카운터만 증가 (DB 접근 없음)
- Bukkit 비동기 스케줄러로 10분(12,000 ticks)마다 DB upsert
- 자정에 최종 flush → 카운터 초기화

**이유:** 이벤트마다 DB write 시 메인 틱 스레드 블로킹으로 틱 지연 직결. 자정 일괄 집계는 당일 실시간 조회 불가. 메모리 누적 + 주기 플러시는 서버 부하 거의 없고 웹 대시보드에서 약 10분 지연의 실시간 통계 제공 가능. 45일 시즌 서버에서 크래시 시 최대 10분 손실은 허용 범위.

**근거 문서:** `poro-rpg/docs/11_web_dashboard/db_event_log_spec.md`, `poro-rpg/docs/02_database_api_stats/CANON.md`

---

### DL-059 경매소 1차 포함 재확인 + 파티 경매 제외

**결정:**

1. **경매소 1차 포함** — `economy_numbers_v2.md §11` 2026-05-17 확정 사항 유지. 고정가 즉시 거래, 수수료 5%, 72시간 등록 기간.
2. **파티 경매 1차 제외** — 보스 드랍 후 파티원 간 입찰 분배 방식 미구현. 포로 서버는 기여도 3% 이상이면 개인 독립 확률로 드랍하는 구조이므로 파티 경매와 설계 전제가 충돌한다.

**이유:** 개인 독립 드랍 구조에서는 보스 보상이 이미 각자에게 귀속되므로 파티 경매를 위한 공용 드랍 풀이 존재하지 않는다. 구현 복잡도 대비 설계 이점 없음.

**근거 문서:** `poro-rpg/docs/02_database_api_stats/economy_numbers_v2.md §11`, `poro-rpg/docs/06_fields_bosses/CANON.md`

---

### DL-058 시즌보스 드랍 기준량 확정 + 강화석 완성품 폐지

**결정:**

1. **시즌보스 드랍 3인 기준량 (1.00×) 확정** — `drop_tables_v1.md §4` 초안 수치 그대로 공식 기준으로 채택.
2. **강화석 완성품 항목 폐지** — 강화석 DB 가상 재화 전환(M-5) 이후 남아 있던 "강화석 완성품" 행을 전 보스 드랍 테이블에서 제거.
3. **인원 배율 확정** — 방법 B 유지: 강화석·큐브 조각은 1인 0.55×/2인 0.78×/3인 1.00×. 고대흔적·장비의 흔적·치장 파편·칭호 재료·트로피는 인원 무관 동일.

**이유:** 드랍 수량이 초안으로만 남아 구현 블로커였음. 수치는 45일 시즌 경제 시뮬레이션(economy_numbers_v2.md) 기준 잉여 설계 의도와 충돌하지 않음. 강화석 완성품은 파편 시스템 시절 잔재로 DB 가상 재화 체계와 이중 설계가 됨.

**근거 문서:** `poro-rpg/docs/06_fields_bosses/drop_tables_v1.md §4`, `poro-rpg/docs/04_combat_weapon_skills/season_boss_stats_v1.md §8.1 R-5`

---

### DL-057 큐브 조각 필드보스 드랍 + 구현 미결 4종 확정

**결정:**

**큐브 조각 필드보스 기본 보상 (확정, DL-057):**

| 필드보스 | 큐브 조각 (기본 보상 확정) | 비고 |
|---|---|---|
| 들판 포식자 | 3~5개 | 기존 드랍 없음 → 신규 추가 |
| 폐광 골렘 | 5~8개 | 기존 드랍 없음 → 신규 추가 |
| 수로 군주 | 5~8개 | 기존 드랍 없음 → 신규 추가 |
| 타락한 수호기사 | 8~12개 | 기존 희귀 확률형(20%×10개) → 기본 확정으로 전환 |
| 균열 파수꾼 | 10~15개 | 기존 희귀 확률형(30%×15개) → 기본 확정으로 전환 |

**스킬타입 판별 구현:** 입력 이벤트 태깅 방식. 스킬 등록 시 `SkillType(BASIC/MOBILITY/SPECIAL/CORE)` 열거형 부여 → 피해 계산 시 해당 잠재 배율 주입. 별도 런타임 추적 없음.

**메모리얼 fallback:** 큐브 소모 확정 시 DB에 `pending_memorial` 레코드 기록. 재접속 시 pending 존재 → 선택 화면 재표시. 서버 재시작 소실 시 현재 옵션 유지. 큐브 복구 없음 (무료 롤 미리보기 악용 방지).

**이동속도% 바닐라 캡:** 레전더리 신발 최대 +13%는 바닐라 속성 캡 범위 내. 별도 처리 불필요. 구현 시 실측 확인만.

**이유:** 필드보스 큐브 조각을 확률형에서 확정형으로 전환 — 30분 보스 참여 보상의 예측 가능성 확보. 불확실한 0% 보상보다 소량 확정이 참여 유도에 효과적.  
**파일:** `poro-rpg/docs/06_fields_bosses/drop_tables_v1.md`, `poro-rpg/docs/02_database_api_stats/potential_options_v1.md`  
**근거:** 2026-05-24 확정

---

### DL-056 큐브 조각 교환 방식 확정 — 공방 제작 없음, 즉시 교환

**결정:** 큐브 조각 10개 → 큐브 1개 교환은 공방 가공기 제작 경로가 아닌 별도 즉시 교환 UI로 처리.

- 공방 가공기에 큐브 제작 탭 없음. 큐브 제작 재료 없음.
- 조각 10개 즉시 교환 UI (별도 GUI 또는 명령어 기반).
- 스코어보드에 현재 큐브 수 옆 **(큐브 조각: n개)** 표시.

**이유:** 공방 슬롯을 점유하지 않아 가공 병렬성 유지. 조각 수집 즉시 교환 가능해 수급 체감 개선.  
**파일:** `poro-rpg/docs/02_database_api_stats/potential_options_v1.md`, `poro-rpg/docs/10_development_roadmap/index.md`  
**근거:** 2026-05-24 확정

---

### DL-055 필드보스 P-10 전면 교체 + P-00 추가 (DL-048 후속)

**결정:** 필드보스 패턴 파일의 P-10 잔재(3곳) 제거 및 DL-048 기준 재정합.

| 보스 | 구 패턴 | 신 패턴 | 이유 |
|---|---|---|---|
| 폐광 골렘 Phase 2 | P-10 독 가스 장판 | P-12 연속 타격 | TPS 부하 제거, 골렘 묵직함 표현 |
| 수로 군주 Phase 2 | P-10 이동형 오염 수막 | P-11 추적 독 구체 | 이동 장판 TPS 최악, 수생 테마 유지 |
| 균열 파수꾼 Phase 3 | P-10 균열 에너지 장판 | P-13 낙하 충격 | 균열 낙하 연출로 대체 |

전 필드보스 5종에 **P-00 기본 공격** 추가. 헤더 "P-01~P-10" → "P-00~P-13" 갱신.

**파일:** `poro-rpg/docs/07_boss_pattern_modules/field_boss_patterns.md`  
**근거:** DL-048 P-10 폐기 후속 정합 (2026-05-24)

---

### DL-054 시즌최종보스 패턴 확정 — SP-84 신설·동기화 전환·1~2인 보정 없음

**결정:**

**타락한 이중체 (SP-8X)**
- **동기화 Phase 전환**: 두 개체 모두 전환 HP%에 도달해야 발동. 먼저 도달한 개체는 피해 면역 대기.
- **전투 종료**: Z·S 두 개체 모두 0% 처치 필요.
- **SP-84 합체 저지 신설** (Phase 전환 #2): Z·S가 서로를 향해 이동하며 합체 시도. 파티원 차단 + 40%p 딜로 해제. 실패 시 융합체 생성 — HP 재생 + 즉사 광역으로 실질 클리어 불가 (와이프 메카닉).
- Phase 구조 표 확정: Phase 1(100%~60%) / 전환 #1 SP-83 / Phase 2(60%~30%) / 전환 #2 SP-84 / Phase 3(30%~0%)

**진혼의 주시자 (SP-9X)**
- **Phase 전환 HP%**: Phase 2 진입 70%, Phase 3 진입 35%.
- **P-09 취약 창**: 마법사 0명 기준 → **마법사 5마리 누적 처치**마다 취약 창 +20% 30초 (초반 0명 반복 악용 방지).

**공통**
- 시즌최종보스 3종: 1~2인 입장 가능, **인원 스케일 보정 없음** (3인 기준 설계).
- SP-81~84, SP-91~92 전체 상태 초안 → 확정.

**파일:** `poro-rpg/docs/07_boss_pattern_modules/season_boss_patterns.md`  
**근거:** 사용자 확인 (2026-05-24)

---

### DL-053 보스4~6 패턴 확정 — SP-43 신설·보스5 P-12 추가·SP-62 1인 고난이도 확정

**결정:**

1. **SP-43 번개 집중포화 신설** (보스4 Phase 3 전용)  
   보스4 Phase 3에 고유 압박 패턴 부재 문제 해소. 15초 누적 피해 최다 플레이어에게 번개 3연타(각 130%, 0.8s 간격, 쿨 15s). 어그로 교체 전략 강제.

2. **보스5 P-12 연속 타격 추가** (Phase 2·3)  
   워든 계열임에도 근접 압박 패턴이 P-00·P-04만 존재했던 문제 보완. 피해 160%, 연타 2~4회, 0.5s 간격, 쿨 9s. 근거리 딜러 위험 강화.

3. **SP-62 1인 고난이도 의도 확정**  
   허상 4개·오공격 HP 회복 패널티는 1인 입장 시 그대로 유지. 보스6 1인 클리어는 최고 난이도 도전으로 의도.

**SP-41~62 전체 상태 초안 → 확정.**

**파일:** `poro-rpg/docs/07_boss_pattern_modules/season_boss_patterns.md`  
**근거:** 사용자 확인 (2026-05-24)

---

### DL-052 보스 클리어 통계 수집 스펙 확정

**결정:** 웹 운영 대시보드 및 밸런스 판단을 위한 보스 클리어 통계 DB 스키마·API 설계 확정.

수집 항목:
- 보스별 클리어율 / 타임아웃 실패율 / 패턴 실패율
- 평균 클리어 시간 (클리어 성공 세션만)
- 파티 평균 강화 수치 / 평균 IL
- 방무 옵션 보유율 (파티 내 최소 1인 보유 기준)
- 시즌 주차별 클리어율 추이

DB 테이블: `boss_session_log` (세션), `boss_session_player` (참여자 스펙)  
API: `GET /api/v1/boss/stats`, `/boss/{boss_id}/stats`, `/boss/{boss_id}/weekly`, `/boss/{boss_id}/party-spec`

버프/너프 기준: 클리어율이 설계 목표 ±15~20%p 이상 2주 지속 시 검토.

**파일:** `poro-rpg/docs/02_database_api_stats/boss_clear_stats_spec.md` (신규), `poro-rpg/docs/02_database_api_stats/CANON.md`  
**근거:** INBOX-002 → 확정 (2026-05-24)

---

### DL-051 시즌보스 1~6 HP 우상향 재설계 — 방어력 무시 필수화

**결정:** 보스1→6 HP를 우하향에서 **우상향**으로 전면 재설계.

| 보스 | 구 HP | 신 HP | 방무 없으면 |
|---|---|---|---|
| 보스1 타락 기사장 | 160,000 | **150,000** | 클리어 가능 (입문) |
| 보스2 오염된 군주 | 155,000 | **155,000** | 빡빡하지만 가능 |
| 보스3 석조 거상 | 150,000 | **158,000** | 취약창 필수 |
| 보스4 폭풍 술사 | 145,000 | **160,000** | 타임아웃 실패 |
| 보스5 심연 수호자 | 135,000 | **165,000** | 클리어 불가 |
| 보스6 공허 사자 | 125,000 | **170,000** | 레전더리 방무 필요 |

- DEF는 이미 우상향 (100→265). HP도 우상향으로 맞춰 두 수치 모두 강함이 직관적으로 올라가는 구조.
- 방어력 무시 유니크(~17%)가 보스4+ 클리어의 실질 필요 조건.
- 정확한 TTK 검증은 M-1 프리 시즌 실측 후. 방향성 확정, 수치는 조정 가능.

**파일:** `poro-rpg/docs/04_combat_weapon_skills/season_boss_stats_v1.md`
**근거:** 사용자 확인 (2026-05-24)

---

### DL-050 균열왕 분노 타이머 수정

**결정:** 분노 트리거 "25분 경과" → **"8분 경과"** 로 수정.

- 구버전(35분 타이머 시절) 설계값이 그대로 남아있던 오류.
- 확정 타이머 10분 기준으로 8분 경과 시 전 패턴 속도 +20%, 분노 구간 진입.

**파일:** `poro-rpg/docs/07_boss_pattern_modules/season_boss_patterns.md`  
**근거:** 타이머 불일치 발견 + 사용자 확인 (2026-05-24)

---

### DL-049 SP-83 결속 차단 신설 (타락한 이중체 Phase 전환 #1)

**결정:** 타락한 이중체 Phase 전환 #1 메카닉으로 SP-83 결속 차단 추가.

- Z·S 사이 결속 링크 형성 → 피해 전이율 50% (딜 상쇄)
- 파티원 1명이 두 개체 사이 위치 시 링크 차단 → 기절 4초 + 취약 창 +30%
- 실패 시 전이율 80% 강화 + 분노 복귀
- **설계 의도**: SP-81(개체 분리 강제) ↔ SP-83(집결 강제) — 전환마다 파티 배치 반전 압박

**파일:** `poro-rpg/docs/07_boss_pattern_modules/season_boss_patterns.md`  
**근거:** 사용자 확인 (2026-05-24)

---

### DL-048 P-10 장판 패턴 폐기 + 전 보스 대체

**결정:** P-10(안전지대/지속 장판) 공용 패턴 **완전 폐기**. 서버 TPS 부하 이유.

- MythicMobs 지속 장판은 매 틱 범위 검사 — 보스 전투 중 복수 장판 상시 활성 시 TPS 직접 영향.
- 교체 원칙: 지속 체크 없는 단발 폭발·낙하·추적 구체로 동일 위협 연출.

| 보스 | 구 패턴 | 교체 |
|---|---|---|
| 보스2 오염된 군주 | P-10 전자기장 | P-03 전자기 폭발 15s 주기 |
| 보스4 SP-42 실패 | 번개 장판 상시 | P-06 산개탄 8s 강제 발동 |
| 보스4 분노(12분) | 번개 장판 1개 상시 | P-06 쿨타임 50% 감소 |
| 보스5 심연 수호자 | P-10 암흑 구간 | P-13 암흑 낙하 20s 주기 |
| 보스6 공허 사자 | P-10 균열 장판 | P-11 추적 균열 구체 (착탄 시 텔레포트) |
| 균열왕 | P-10 저주 성장 장판 | P-03 저주 폭발 Phase별 반지름 증가 |

**파일:** `poro-rpg/docs/07_boss_pattern_modules/common_patterns.md`, `season_boss_patterns.md`  
**근거:** 사용자 확인 (2026-05-24)

---

### DL-047 공용 패턴 풀 개편 — P-00 신설 + P-11/12/13 추가

**결정 1 — P-00 기본 공격 신설**

- 전 보스 공통 보유. 쿨타임 1.5~2.2s, 피해 60~90%, 예고 0.3s.
- 스킬(P-01 이상) 쿨타임 사이를 채워 "보스가 항상 뭔가를 한다"는 전투 리듬 형성.
- 원거리 보스(보스2·4)는 약한 투사체 형태로 오버라이드.

**결정 2 — P-11·P-12·P-13 추가**

| 코드 | 이름 | 개요 |
|---|---|---|
| P-11 | 추적 구체 | 특정 플레이어 유도 추적. 보스 방향으로 유인하면 소멸. |
| P-12 | 연속 타격 | 2~4회 빠른 근접 연타. 1타씩 약하나 전타 합산 피해 큼. |
| P-13 | 낙하 충격 | 공중 도약 후 낙하, 착지 광역 피해. 파티클로 낙하 지점 예고. |

- P-14(독기/화염 장판)은 서버 부하 이유로 미채택 (DL-048 참조).

**파일:** `poro-rpg/docs/07_boss_pattern_modules/common_patterns.md`, `season_boss_patterns.md`  
**근거:** 사용자 확인 (2026-05-24)

---

### DL-046 석조 거상 (보스3) 패턴 전면 재설계

**결정:** SP-3X 전체 교체. 암흑/촉수 테마(구 심연의 군주) → 석조/지진 테마(아이언 골렘).

| 항목 | 구버전 | 신버전 |
|---|---|---|
| 공용 패턴 | P-07(소환체) P-08 P-09 P-10 | P-01 P-02 P-03 P-04 P-08 P-09 |
| 고유 패턴 | SP-31~35 (암흑·수정·에너지) | SP-31 지진 발걸음, SP-32 거석 팔 투척 |
| Phase 수 | 4 (15% 추가 Phase) | 3 (60%·25%) |

- SP-31 지진 발걸음: 발구름 3회 동심원 진동파 → 점프 회피 + 석판 충전으로 무적 해제
- SP-32 거석 팔 투척: 팔 오브젝트(HP 20,000) 파괴로 무적 해제. 성공 시 Phase 3 P-04 범위 -30%, 실패 시 공격력 +15% 분노

**파일:** `poro-rpg/docs/07_boss_pattern_modules/season_boss_patterns.md`  
**근거:** 사용자 확인 (2026-05-24)

---

### DL-045 시즌최종보스 칭호명 확정 + 닉네임·커스텀 칭호 변경권 보스 드랍 추가

**결정 1 — 칭호명 확정**

| 보스 | 클리어 칭호 |
|---|---|
| 균열왕 | **수호자** |
| 타락한 이중체 | **이중주** |
| 진혼의 주시자 | **낙성자** |
| 3종 모두 클리어 | **심연을 넘은 자** |

**결정 2 — 한글 닉네임 변경권 · 커스텀 칭호 변경권 시즌보스 4+ 확률 드랍 추가**

| 보스 | 닉네임 변경권 (S/A) | 커스텀 칭호 변경권 (S/A) |
|---|---|---|
| 시즌보스4 | 5% / 3% | 3% / — |
| 시즌보스5 | 8% / 5% | 5% / 2% |
| 시즌보스6 | 12% / 7% | 7% / 3% |
| 시즌최종보스 3종 | 15% / 10% | 10% / 5% |

- 두 아이템 현재 상점 미판매. 보스 드랍 단일 경로.
- 수치는 초안 — 운영 후 조정 가능.

**파일:** `poro-rpg/docs/06_fields_bosses/drop_tables_v1.md`  
**근거:** 사용자 확인 (2026-05-24)

---

### DL-044 균열왕 봉인 파편 입장 조건 폐지

**결정:** 균열왕 봉인 파편 드랍·입장권 제작 시스템 전면 삭제. 보스6 클리어 시 시즌최종보스 3종 자유 입장.

- 시즌보스 4·5·6 보상에서 균열왕 봉인 파편 행 제거
- 균열왕 입장 조건: "봉인 파편 수집 후 입장권 제작" → "보스6 클리어 후 자유 입장"
- 공방 가공기 균열왕 입장권 제작 레시피 불필요

**근거:** 입장권 수집 과정이 콘텐츠 진행 흐름을 단절시킴. 보스6 클리어 자체가 최종보스 도전 자격의 자연스러운 기준. 사용자 확인 (2026-05-24)  
**파일:** `poro-rpg/docs/06_fields_bosses/drop_tables_v1.md`, `poro-rpg/docs/decision_log.md`

---

### DL-043 시즌최종보스 3종 티어 확정

**결정:** 시즌최종보스를 균열왕 단일 최종이 아닌 3종 동급 최종 티어로 구성.

| 순번 | 이름 | 베이스 몹 | 핵심 메카닉 | 고유 칭호 |
|---|---|---|---|---|
| 최종보스1 | **균열왕** | 위더스켈레톤+말 | 분노 스택 (SP-7X) | 수호자 (DL-045) |
| 최종보스2 | **타락한 이중체** | 좀비+스켈레톤 쌍 | HP 균형 광폭화 (SP-8X) | 이중주 (DL-045) |
| 최종보스3 | **진혼의 주시자** | 위더 | 별빛 마법사 스택 (SP-9X) | 낙성자 (DL-045) |

- 3종 모두 클리어율 목표 ~10%, 우열 없음
- **입장 조건:** 보스6 클리어 후 자유 입장 (봉인 파편 폐지 — DL-044)
- **보상:** 각 고유 칭호 별도 지급 (수호자·이중주·낙성자) + 3종 모두 클리어 시 **심연을 넘은 자** 칭호 발급 (DL-045)
- 찬란한 고대흔적·강화석·큐브 등 나머지 보상은 3종 동일 구조 적용
- **타락한 이중체 핵심 설계:** 좀비 HP% · 스켈레톤 HP% 차이 >25% 시 광폭화. 각 HP 445,000 (총 890,000), DEF 270, 타이머 10분
- **진혼의 주시자 핵심 설계:** 위더가 별빛 마법사 주기 소환. 마법사 수 스택별 위더 받피감 증가: 1~2명 +15%/명, 3~4명 +20%/명 추가, 5명+ 전원 소모 → 별빛 대마법 발동. HP 890,000, DEF 280, 타이머 10분

**파일:** `poro-rpg/docs/06_fields_bosses/CANON.md`, `poro-rpg/docs/06_fields_bosses/drop_tables_v1.md`, `poro-rpg/docs/04_combat_weapon_skills/season_boss_stats_v1.md`, `poro-rpg/docs/07_boss_pattern_modules/season_boss_patterns.md`  
**근거:** 사용자 확인 (2026-05-24)

---

### DL-042 시즌보스 6종 + 균열왕 이름 확정

**결정:** 보스 이름 7종 확정.

| 보스 | 이름 | 베이스 몹 | 컨셉 |
|---|---|---|---|
| 보스1 | **타락 기사장** | 좀비 + 풀아머 | 타락한 제국 중갑 기사 |
| 보스2 | **오염된 군주** | Drowned + 아머 | 오염된 수로 군주 |
| 보스3 | **석조 거상** | 아이언 골렘 | 석조 거대 골렘 |
| 보스4 | **폭풍 술사** | 에보커 | 폭풍 소환사 (마법형) |
| 보스5 | **심연 수호자** | 워든 | 심연의 수호자 (어둠/지하) |
| 보스6 | **공허 사자** | 엔더맨 | 균열 파편 (균열 에너지) |
| 최종보스 | **균열왕** | 위더스켈레톤 + 말 | 검 든 기사왕 |

**파일:** `poro-rpg/docs/06_fields_bosses/CANON.md`, `poro-rpg/docs/06_fields_bosses/drop_tables_v1.md`, `poro-rpg/docs/04_combat_weapon_skills/season_boss_stats_v1.md`, `poro-rpg/docs/07_boss_pattern_modules/season_boss_patterns.md`  
**근거:** 사용자 확인 (2026-05-24)

---

### DL-041 시즌보스 3종 → 6종으로 확장

**결정:** 시즌보스 6종 + 균열왕 구조로 변경.

| 보스 | 클리어율 | 권장 강화 | 고대흔적 |
|---|---|---|---|
| 보스1 | 75% | 6~10강 | 빛 바랜 |
| 보스2 | 65% | 10~13강 | 빛 바랜 |
| 보스3 | 55% | 13~16강 | 빛나는 |
| 보스4 | 45% | 16~18강 | 빛나는 |
| 보스5 | 35% | 18~20강 | 눈부신 |
| 보스6 | 25% | 20~22강 | 찬란한 |
| 균열왕 | 10% | 22강+ | 찬란한 |

- 모든 보스 자유 입장 (해금 조건 없음)
- 45일 시즌 상한 22강에 맞춰 상한 수정 (기존 20강 → 22강)
- 모듈 패턴 시스템으로 추가 3보스 구현 비용 최소화
- 컨셉/이름/스탯/보상 표는 후속 작업에서 확정

**파일:** `poro-rpg/docs/06_fields_bosses/CANON.md`, `poro-rpg/docs/final_master_plan.md`  
**근거:** 사용자 확인 (2026-05-24)

---

### DL-040 균열왕 분노 스택 수치 확정

**결정:** 스택당 공격력 +10%, 최대 3스택(+30%)

| 발동 조건 | 스택 |
|---|---|
| SP-41 에너지 결절 방치 (전환 #1) | +1 |
| SP-44 분신 처치 실패 (전환 #4) | +1 |
| 25분 경과 후 무적 기믹 실패 | +1 |

3스택 도달 시 공격력 +30% → 사실상 클리어 불가 수준의 압박. "기믹 실패 = 끝" 의도된 설계.

> **보스명/컨셉 변경 가능성 있음.** "균열왕"은 현재 임시 명칭. 변경 시 수치 재검토 가능하나 스택 구조 자체는 유지.

**파일:** `poro-rpg/docs/04_combat_weapon_skills/season_boss_stats_v1.md` (M-7 해소), `poro-rpg/docs/07_boss_pattern_modules/season_boss_patterns.md` (기존 초안 수치 확정)  
**근거:** 사용자 확인 (2026-05-24)

---

### DL-039 시즌보스 핵심 재료 폐기 → 고대흔적 확률 드랍으로 교체

**결정:** 시즌보스 전용 핵심 재료 아이템 제거. 대신 티어별 고대흔적을 확률 드랍.

| 보스 | 드랍 고대흔적 | S | A | B | C |
|---|---|---|---|---|---|
| 시즌보스 1 | 빛 바랜 고대흔적 | 확정 | 70% | 40% | — |
| 시즌보스 2 | 빛나는 고대흔적 | 확정 | 확정 | 50% | — |
| 시즌보스 3 | 눈부신 고대흔적 | 확정 | 확정 | 70% | 30% |
| 균열왕 | 찬란한 고대흔적 | 확정 | 확정 | — | — |

- 고대흔적은 **주간 첫 클리어에서만 지급** (재도전 보상 미포함)
- 인원 배율 표에서 "시즌보스 핵심 재료" → "고대흔적" 희소재 보호 유지

**폐기된 아이템:** `시즌보스 1/2/3 핵심 재료`

**파일:** `poro-rpg/docs/06_fields_bosses/drop_tables_v1.md`  
**근거:** 사용자 확인 (2026-05-24)

---

### DL-038 균열왕 최소 입장 인원 제한 없음 확정

**결정:** 최소 입장 인원 제한 없음. 솔로·2인·3인 모두 입장 허용.

- 솔로 TTK 38분으로 35분 타이머 초과 → 사실상 솔로 클리어 불가이나 입장 자체는 막지 않음
- 입장 시 인원 부족 경고 메시지 출력 (`§e[경고] 현재 인원으로 클리어가 어려울 수 있습니다.`)
- 클리어 여부는 유저 책임. 구현 복잡도 절감 (인원 체크 로직 불필요)

**파일:** `poro-rpg/docs/04_combat_weapon_skills/season_boss_stats_v1.md` (R-3, M-3 해소), `poro-rpg/docs/06_fields_bosses/CANON.md`  
**근거:** 사용자 확인 (2026-05-24)

---

### DL-037 시즌보스 반복 보상 구조 확정

**결정:** 주간 첫 클리어 풀 보상 + 이후 재도전 감소 보상 (방안 A)

| 항목 | 기준 |
|---|---|
| 보상 리셋 | 매주 월요일 00:00 (서버 시간) |
| 첫 클리어 보상 | 장비의 흔적 · 핵심 재료 · 칭호 재료 포함 풀 보상 |
| 재도전 보상 | 강화석 · 큐브 조각 · 핵심 재료 소량만. 장비의 흔적 · 칭호 재료 미포함 |
| 균열왕의 심장 | 매 클리어 드랍 허용 (시즌 고유 트로피, 반복 획득 의미 있음) |
| 참여 기준 | 기여도 3% 이상 |

**45일 시즌 기준:** 6주 = 풀 보상 최대 6회 기회.

**파일:** `poro-rpg/docs/06_fields_bosses/drop_tables_v1.md`, `poro-rpg/docs/06_fields_bosses/CANON.md`  
**근거:** 사용자 확인 (2026-05-24)

---

### DL-036 공방 가공기 오프라인 완료 — 타임스탬프 기반으로 확정

**결정:**
- 공방 대기열은 사이클 캡 없는 타임스탬프 기반 완료로 처리
- `island_workshop_queue`에 `duration_minutes`, `due_ts` 컬럼 추가
- 완료 조건: `due_ts ≤ now` (= 등록 시각 + 처리 시간)
- 완료 체크 시점: 스케줄러 20분 틱 + `PlayerLoginEvent` + GUI 열람
- 완료 시 영지 창고 자동 입금 + 로그인 알림

**약초 재배지 / 광물 채굴기는 기존 3사이클 캡 유지** — 주기적 생산 재화 폭발 방지 목적.

**파일:** `poro-rpg/docs/05_island_farm_system/island_system_design.md`, `poro-rpg/docs/05_island_farm_system/CANON.md`  
**근거:** 사용자 확인 (2026-05-24)

---

### DL-035 공방 가공기 처리 시간 확정

**목표 사이클:** 사냥 전 대기 등록 → 1~2시간 사냥 후 귀환 → 완료 확인 → 재등록 → 재사냥

**처리 시간 기준:**

| 등급 | 시간 | 레시피 |
|---|---:|---|
| 즉시 소비품 | 5분 | 제련(마도합금), 정제(약초), 광물 변환 전종 |
| 단기 소비품 | 10분 | 치료 포션 3종, 만찬 4종 |
| 핵심 재료 | 15분 | 자연의 정수, 부스트 포션 3종 |
| 핵심 재료 | 20분 | 별의 흔적 |
| 핵심 재료 | 30분 | 농부의 정수, 달의 흔적, 미감정 흔적, 빛 바랜 고대흔적 |
| 핵심 재료 | 45분 | 광부의 정수, 태양의 흔적, 빛나는 고대흔적 |
| 고급 완성품 | 60분 | 눈부신 고대흔적 |
| 고급 완성품 | 90분 | 찬란한 고대흔적 (최대 1.5 사냥 세션) |

**설계 의도:** 처리 시간이 곧 "게임 내 대기 비용". 고급일수록 길어지지만 최대 90분(찬란한)을 상한으로 둬 오프라인 누적 보상(최대 3사이클)과 조합 시 비효율이 없도록 설계.

**파일:** `poro-rpg/docs/05_island_farm_system/workshop_crafting_spec.md`  
**근거:** 사용자 확인 (2026-05-24)

---

### DL-034 마도합금 용도 확정 + 레시피 재구성

**결정:**
1. `mat_mado_alloy` (마도합금) 용도: 강화 흔적 3종 + 고대흔적 4종 핵심 재료
2. 강화 흔적 / 고대흔적 레시피에서 광부의 정수 → 마도합금으로 교체
3. 부스트 포션 레시피 현행 유지 (농부의 정수 × 1 + 정제 약초 × 1 + 철블럭 × 16 + 금블럭 × 16)

**마도합금 투입량:**

| 결과물 | 마도합금 |
|---|---|
| 별의 흔적 | × 10 |
| 달의 흔적 | × 20 |
| 태양의 흔적 | × 40 |
| 빛 바랜 고대흔적 | × 20 |
| 빛나는 고대흔적 | × 40 |
| 눈부신 고대흔적 | × 60 |
| 찬란한 고대흔적 | × 80 |

**역할 분리 결과:**
- 광부의 정수: 자연의 정수 → 미감정 흔적 체인 + 부스트 포션 전용 (고비용 유지)
- 마도합금: 광물 채굴기 → 제련 → 강화 흔적/고대흔적 체인 담당

**파일:** `poro-rpg/docs/05_island_farm_system/workshop_crafting_spec.md`
**근거:** 사용자 확인 (2026-05-24)

---

### DL-033 강화석 드랍률 + 11~16강 성공률 확정

**개발 철학:** 수급량을 수학적으로 맞추려 하지 않는다. 공급을 넉넉하게 두고 성공률을 낮춰 플레이어가 버튼을 많이 누르게 하는 것이 목표.

**강화석 드랍률 (일반몹, 세트 A):**

| 필드 | 구 확률 | 신 확률 |
|---|---|---|
| 필드1 수도 외곽 평원 | 4% | **6%** |
| 필드2 폐광 지대 | 5% | **8%** |
| 필드3 오염된 수로 | 6% | **9%** |
| 필드4 무너진 초소 | 6% | **10%** |
| 필드5 고대 성벽 잔해 | 6% | **12%** |

정예몹·필드보스 드랍률 변경 없음.

**11~16강 성공률 하향 (세트 A):**

| 강화 | 구 성공률 | 신 성공률 | 신 천장 |
|---|---|---|---|
| 11강 | 50% | **25%** | 8회 |
| 12강 | 40% | **20%** | 10회 |
| 13강 | 35% | **17%** | 12회 |
| 14강 | 30% | **15%** | 14회 |
| 15강 | 25% | **12%** | 17회 |
| 16강 | 20% | **10%** | 20회 |

17~25강 성공률 현행 유지.

**파일:**
- `poro-rpg/docs/06_fields_bosses/drop_tables_v1.md`
- `poro-rpg/docs/02_database_api_stats/economy_numbers_v2.md`
- `poro-rpg/docs/final_master_plan.md` §13 미확정 항목 업데이트

**이유:** 버튼 많이 누르는 경험 > 정밀한 수급 계산. 오픈 후 3일 실측 기반 재조정 여지 유지.
**근거:** 사용자 확인 (2026-05-24)

---

### DL-032 스킬 히트박스 수치 / 이동기 거리 확정

**결정:** 전 무기 24스킬의 판정 수치(반경·각도·길이·너비·사거리·탄속) 및 이동기 6종 거리·소요시간 확정.

**스킬 데이터 스키마 추가 필드:**
- `angle` — melee_arc / cone 좌우 총 각도(°)
- `movement_distance` — 이동기 이동 거리(블럭)
- `movement_duration` — 이동 소요 시간(초)

**기준값 요약:**
- 기본 근접 reach 2.5블럭, 창·낫처럼 컨셉이 "넓거나 긴" 무기는 +0.5~1블럭
- 이동기 거리: 스태프 1.5 < 검/석궁 2~2.5 < 도끼 3 < 창 5블럭
- 각도: 표준 근접 120°, 좌우 대응 보완 스킬 150°

**파일:** `poro-rpg/docs/04_combat_weapon_skills/weapon_skills_v1.md` — 히트박스 섹션 + 스키마 필드 추가
**근거:** 사용자 확인 (2026-05-24)

---

### DL-031 스킬 이펙트 시스템 아키텍처 확정

**결정:** 방안 4 혼합 구조 — MythicMobs(파티클·사운드) + Display Entity(투사체·검기) + Bukkit Particle(fallback) 조합.

**원칙:**
1. 스킬 판정·피해·쿨타임·상태는 PoroRPG 단독 책임
2. MythicMobs는 이펙트 위임 전용, 판정 위임 없음
3. Display Entity는 0.5초 이상 보이는 투사체·검기·마법탄에만 사용
4. MythicMobs 없어도 전투 로직 정상 작동 (graceful degradation)
5. ModelEngine/BetterModel은 2차 확장 보류

**effect_key prefix 규칙:**
- `mm:xxx` — MythicMobs castSkill
- `dp:xxx` — Display Entity
- `pt:xxx` — Bukkit Particle 직접
- 빈 값 — 이펙트 없음

**구현 격리 원칙:** MythicMobs API 직접 호출은 `MythicMobsEffectHandler` 안에만 격리. PoroRPG core는 인터페이스(`EffectDispatcher`)만 참조.

**파일:**
- `poro-rpg/docs/04_combat_weapon_skills/weapon_skills_v1.md` — 이펙트 시스템 섹션 + 24개 스킬 effect_key 기준값 추가

**이유:** MythicMobs는 이미 스택에 있어 추가 의존 없음. prefix 기반 dispatch는 핸들러를 교체해도 스킬 데이터를 건드리지 않아 2차 확장 시 ModelEngine 교체 비용 최소화.
**근거:** 사용자 확인 (2026-05-24)

---

### DL-030 디스코드 봇 설계 결정 3종

**결정:**
1. `/강화계산` — 봇 내부 계산 (강화 비용 표 하드코딩). API 호출 없음.
2. `/프로필` 타인 조회 — 전체 공개. 본인과 동일 형식 출력.
3. 봇 구현 언어 — **Node.js (Discord.js)**, PoroRPG와 독립 프로세스.

**파일:**
- `poro-discord/docs/discord_bot_spec.md`

**이유:**
- `/강화계산`: 강화 비용은 CANON 고정 수치. API 왕복 불필요. 봇 내부 계산이 응답 빠르고 구현 단순. 45일 시즌 내 수치 변경 가능성 낮음.
- 타인 프로필 공개: 파티 모집·랭킹 맥락에서 플레이어 간 정보 공유가 게임플레이 경험에 유익. 숨길 민감 정보 없음.
- Node.js: Discord.js가 버튼·모달·선택메뉴 등 인터랙티브 기능을 가장 완전하게 지원. 게임 서버와 독립되어 봇 재시작이 게임에 영향 없음. 커뮤니티·예제 가장 풍부.
**근거:** 사용자 확인 (2026-05-24)

---

### DL-029 버그 제보 접수번호 체계 확정

**결정:** 디스코드 봇 `/버그제보` 접수번호는 `bug_report.id` AUTOINCREMENT를 사용하고 `BUG-{id}` 형식으로 공개한다.

**파일:**
- `poro-discord/docs/discord_bot_spec.md`
- `poro-rpg/docs/11_web_dashboard/db_event_log_spec.md` — `bug_report` 테이블 추가

**이유:** 날짜+순번 방식(`BUG-20260523-001`)은 날짜 파싱·중복 방지 로직이 추가로 필요하다. DB AUTOINCREMENT는 SQLite가 보장하므로 구현이 단순하고, 45일 시즌 내 버그 수가 수백 건 이하로 예상되므로 번호 자릿수 부담 없음.
**근거:** 사용자 확인 (2026-05-24)

---

### DL-028 운영자 웹 대시보드 도메인 신설

**결정:** `poro-rpg/docs/11_web_dashboard/` 폴더를 신설하고 운영자 전용 웹 대시보드 설계 문서 4종을 작성한다.

**변경:**
- `poro-rpg/docs/11_web_dashboard/index.md` 신규: 전체 구조 개요, 페이지 목록, 기술 스택
- `poro-rpg/docs/11_web_dashboard/web_dashboard_spec.md` 신규: 페이지별 레이아웃·필터·표시 데이터 상세
- `poro-rpg/docs/11_web_dashboard/api_endpoints.md` 신규: 대시보드 호출 API 엔드포인트 목록
- `poro-rpg/docs/11_web_dashboard/db_event_log_spec.md` 신규: 경제·전투 이벤트 로그 DB 테이블 설계
- `poro-rpg/docs/final_master_plan.md`: 공식 문서 구조 표에 도메인 11 추가, §11 데이터와 API에 웹 대시보드 설명 추가

**이유:** 45일 시즌 서버에서 경제 이상·보스 밸런스를 운영 중 감지하고 근거 있는 너프/버프 판단을 하기 위해 운영자 관제 도구가 필요하다.

**핵심 결정 내용:**
- 접근: 운영자 전용, Bearer 토큰 인증
- 데이터 원본: PoroRPG HTTP API(포트 8765) 단일 경로. DB 직접 접근 없음
- 갱신: 일별 집계(자정 스냅샷). 실시간은 현재 접속자·최근 보스 클리어만
- 1차 범위: 경제 관제, 보스 기록, 서버 현황, 아이템 발행 수
- 기존 `poro.db` 테이블 수정 없음. 이벤트 로그 테이블 10종 신규 추가만

**근거:** 사용자 요구사항 (2026-05-23)

---

### DL-026 날개 치장 1차 시즌 포함 확정

**결정:** 날개 치장(wing_volt_poro_01)을 1차 시즌 리소스팩 범위에 포함한다.

**변경:**
- `final_master_plan.md` 리소스팩 우선순위에서 "날개 치장 제외" → "날개 치장 1종 포함"으로 수정
- CMD 500xxx 범위를 코스메틱 전용으로 신설 (500001~500099: 날개)
- `paper.json` CMD 500001 = `poro:item/cosmetics/wings/wing_volt_poro_01` 등록
- `assets/source/items/cosmetics/cosmetics_registry.yml` 신규 생성
- 코스메틱 아이템 lore에 "장착 부위: XXX" 명시 + 플러그인 슬롯 차단 규칙 확정

**이유:** 모델·텍스처가 이미 완성돼 있어 추가 비용 없이 등록 가능. 1차 오픈 콘텐츠 다양성 확보.
**근거:** 사용자 확인 (2026-05-23)

---

## 2026-05-22 — PHASE 2 canon 충돌 수정

### DL-001 Citizens 플러그인 제거

**파일:** `poro-rpg/docs/01_plugin_architecture/index.md`  
**변경:** 플러그인 목록 테이블에서 `Citizens | NPC 껍데기` 행 제거  
**이유:** `final_master_plan.md`의 "플러그인 구조"에서 Citizens 제거가 확정됨. NPC 역할은 PoroRPG 자체 처리로 전환.  
**근거:** `final_master_plan.md`의 "플러그인 구조" (2026-05-20 기준)

---

### DL-002 마력 과부하 채팅 메시지 제거

**파일:** `poro-rpg/docs/04_combat_weapon_skills/index.md`  
**변경:** 채팅/알림 포맷 테이블에서 `마력 과부하`, `마력 과부하 반복` 행 제거  
**이유:** 마력 시스템(발전기·마력 소비 구조) 2026-05-19 전면 폐지 확정.  
**근거:** `final_master_plan.md`의 "개인 영지" + `economy_numbers_v2.md`의 마력 시스템 폐지 주석

---

### DL-003 무기 이름 망치 → 도끼

**파일:**  
- `poro-rpg/docs/04_combat_weapon_skills/weapon_skills_v1.md` (전체 표시명 치환)  
- `poro-rpg/docs/04_combat_weapon_skills/index.md` (무기 클래스 테이블, GUI 레이아웃, 아이콘 테이블)  
**변경:** 표시명 "망치" → "도끼" (당시 YAML 코드 식별자 `hammer`는 구현 로직으로 유지했으나, DL-025에서 `axe`로 전환 확정)
**이유:** `final_master_plan.md`의 "전투와 장비 성장"에서 무기 6종 중 "도끼"로 확정됨. `weapon_skills_v1.md`의 도끼 항목도 `minecraft:netherite_axe`를 추천 베이스 아이템으로 기재함.  
**근거:** `final_master_plan.md`의 "전투와 장비 성장" (2026-05-20 기준)

---

### DL-004 도끼 베이스 아이템 NETHERITE_PICKAXE → NETHERITE_AXE

**파일:** `poro-rpg/docs/04_combat_weapon_skills/index.md`  
**변경:** 무기 선택창 아이콘 테이블에서 도끼 베이스 아이템을 `NETHERITE_PICKAXE` → `NETHERITE_AXE`로 수정  
**이유:** `weapon_skills_v1.md`의 도끼 항목에 "추천 베이스 아이템: `minecraft:netherite_axe`"로 명시됨. index.md의 `NETHERITE_PICKAXE`는 이전 망치 기반 시절의 잔재.  
**근거:** `weapon_skills_v1.md`의 도끼 항목 + `final_master_plan.md`의 "전투와 장비 성장"

---

### DL-005 강화 비용표 전면 교체

**파일:** `poro-rpg/docs/02_database_api_stats/economy_numbers_v2.md`  
**변경:** `### 강화 비용표` 섹션 전체를 `final_master_plan.md`의 "전투와 장비 성장" 및 "미확정 항목" 확정 내용 기준으로 교체  
**상세 변경:**
- 골드 비용: 1강 180G → **2,000G**, 22강+ 25,000G → **27,000G 고정**
- 강화석 파편 시스템 폐지 → 강화석 직접 소모 (M-5 확정)
- 보조재 B/C 시스템 폐지 → 강화 흔적 3종(별/달/태양) 시스템 (M-3 확정)
- 방어구 강화석 비율: 무기의 50% → `ceil(무기 강화석 ÷ 1.5)` (약 67%, 확정 2026-05-20)
- 구 계산 테이블(강화석 파편 소모량, 보조재 소모량)은 구버전 표기로 인라인 주석 처리

**이유:** economy_numbers_v2가 2026-05-15 기준 수치이고, `final_master_plan.md`의 "전투와 장비 성장"은 2026-05-20 M-3 확정 기준. 최신 확정이 우선.  
**근거:** `final_master_plan.md`의 "전투와 장비 성장" 및 `poro-rpg/docs/02_database_api_stats/CANON.md`

---

### DL-006 마력 관련 항목 제거 (economy_numbers_v2)

**파일:** `poro-rpg/docs/02_database_api_stats/economy_numbers_v2.md`  
**변경:**
- `economy_numbers_v2.md`의 병목 구간 테이블에서 "발전기 마력 부족" 행 제거
- `economy_numbers_v2.md`의 v2 신규 체크포인트 테이블에서 "마력 과부하 발생 비율" 행 제거
- `economy_numbers_v2.md`의 영지 편의 해금 테이블에서 "발전기 효율 업그레이드 +5 MP/h" 행 제거
**이유:** 마력 시스템 2026-05-19 전면 폐지.  
**근거:** `final_master_plan.md`의 "개인 영지" + `economy_numbers_v2.md`의 마력 시스템 폐지 주석

---

## 2026-05-22 — PHASE 4 final_master_plan 재정리

### DL-007 final_master_plan.md 2,341줄 → 601줄 축소

**파일:** `poro-rpg/docs/final_master_plan.md`  
**변경:** 세부 수치·슬롯 매핑·레시피 체인·확정 완료 M-tags를 "→ 상세: X 참조" 형태로 위임.  
**제거 및 위임 목록:**
- 무기 6종 스킬 상세 → `poro-rpg/docs/04_combat_weapon_skills/weapon_skills_v1.md`
- 장비 포맷·시작 장비 → `poro-rpg/docs/01_plugin_architecture/implementation_reference.md`
- 시스템 메시지 포맷 → `poro-rpg/docs/01_plugin_architecture/CANON.md`
- 상점 상세 → `poro-rpg/docs/02_database_api_stats/CANON.md`
- 잠재 등급 상세 표 → `poro-rpg/docs/02_database_api_stats/potential_options_v1.md`
- 강화 비용표 전체 → `poro-rpg/docs/02_database_api_stats/economy_numbers_v2.md`
- 작위별 상세 표 → `poro-rpg/docs/05_island_farm_system/CANON.md`
- 공방 레시피 체인 → `poro-rpg/docs/05_island_farm_system/workshop_crafting_spec.md`
- 광물 생성기 확률표 → `poro-rpg/docs/05_island_farm_system/CANON.md`
- 엘리베이터 구현 코드 → `poro-rpg/docs/01_plugin_architecture/implementation_reference.md`
- GUI 슬롯 매핑 전체 → `poro-rpg/docs/08_resourcepack_pipeline/gui_*.md`
- 확정 완료 M-tags → 각 도메인 문서와 `decision_log.md`에 흡수
- 관리자 커맨드 상세 표 → `poro-rpg/docs/01_plugin_architecture/admin_command_spec.md`
**추가 수정:** 장비 이름 변경권 가격 300,000G → **10,000G** (DL-007 충돌 해소, M-11 확정 반영)  
**이유:** final_master_plan을 프로젝트 철학·핵심 방향성·시스템 연결 중심 문서로 전환. 세부 내용은 전용 시스템 문서(CANON.md, 시스템 스펙 문서)에서 관리.  
**근거:** `poro-rpg/docs/_archive/master_plan_content_audit.md` 분류 기준

---

## 2026-05-22 — PHASE 5 docs 리빌드 검수 반영

### DL-008 archive 구조 확정

**파일:**
- `poro-rpg/docs/_archive/README.md`
- `poro-rpg/docs/_archive/docs_restructure_plan.md`
- `poro-rpg/docs/_archive/master_plan_content_audit.md`
- `poro-rpg/docs/_archive/11_remaining_decisions/index.md`

**변경:** 활성 docs 루트에 남아 있던 리빌드 계획/감사/구 결정 문서를 `poro-rpg/docs/_archive/`로 이동하고, README에 archive 이유와 대체 문서를 기록.  
**이유:** 실행 완료된 계획 문서와 흡수된 결정 문서가 활성 기준 문서처럼 보이는 문제 제거.  
**근거:** docs 리빌드 검수 기준 — archive 폴더는 `poro-rpg/docs/_archive/`로 통일.

---

### DL-009 구현 레퍼런스 경로 확정

**파일:** `poro-rpg/docs/01_plugin_architecture/implementation_reference.md`  
**변경:** 기존 루트 `docs/final_설계_plan.md`를 플러그인 아키텍처 하위 구현 레퍼런스로 이동.  
**이유:** `final_master_plan.md`과 각 CANON에서 이미 구현 상세를 `poro-rpg/docs/01_plugin_architecture/implementation_reference.md`로 위임하고 있었으므로 실제 파일 경로를 참조와 일치시킴.  
**근거:** `poro-rpg/docs/01_plugin_architecture/CANON.md`

---

### DL-010 CANON.md 역할 정리

**파일:**
- `poro-rpg/docs/01_plugin_architecture/CANON.md`
- `poro-rpg/docs/02_database_api_stats/CANON.md`
- `poro-rpg/docs/04_combat_weapon_skills/CANON.md`
- `poro-rpg/docs/05_island_farm_system/CANON.md`
- `poro-rpg/docs/06_fields_bosses/CANON.md`

**변경:** TODO/잔존 충돌 문구를 제거하고, 공식 기준·참조 우선순위·충돌 처리 방식만 남김. 잘못된 섹션 번호와 이동 예정 문구도 정리.  
**이유:** CANON 문서가 작업 체크리스트가 아니라 현재 공식 기준 역할을 해야 함.  
**근거:** `final_master_plan.md`의 "공식 문서 구조".

---

### DL-011 Status 태그와 참조 경로 통일

**파일:** `docs/**/*.md`, `CLAUDE.md`  
**변경:** 주요 문서 상단 Status를 `[STATUS: CANON|REFERENCE|DRAFT|ARCHIVED]` 형식으로 통일. 구 GUI 경로를 `poro-rpg/docs/08_resourcepack_pipeline/`로 수정. 존재하지 않는 경제 검토 문서 참조 제거.  
**이유:** 문서 역할과 현재 docs 구조를 명확히 하고 깨진 참조를 제거.  
**근거:** docs 리빌드 검수 기준 — Status 태그 누락, CLAUDE/AGENTS 참조 경로 정합성.

---

### DL-012 final_master_plan 재축약

**파일:** `poro-rpg/docs/final_master_plan.md`  
**변경:** 구현 절차, 상세 수치표, 슬롯 배치, API 상세, 관리자 커맨드 상세를 하위 문서로 위임하고 원칙/방향성/도메인 진입점 중심으로 재구성.  
**이유:** `final_master_plan.md`가 너무 많은 세부사항을 보유하면 각 CANON.md와 하위 문서가 공식 기준으로 기능하기 어렵다.  
**근거:** `poro-rpg/docs/_archive/master_plan_content_audit.md`

---

### DL-013 AI orchestra workflow 명령어 통일

**파일:**
- `CLAUDE.md`
- `AGENTS.md`
- `scripts/orchestra.sh`

**변경:** AI 작업 루프의 공식 명령어를 `orc`로 통일하고, main → review는 `orc handoff-main` / `orc to-review`, review → main은 `orc handoff-review` / `orc to-master` 흐름으로 정리. `handoff-*` 명령은 현재 worktree 변경사항을 `add -A` 후 commit하고 대응하는 동기화 명령을 실행하는 것으로 정의. `to-review` / `to-master`는 양쪽 worktree가 dirty 상태이면 중단하는 안전 동기화 명령으로 명시.
**이유:** Claude(main/master)와 Codex(review/codex-review)의 역할을 분리하면서도 handoff 명령과 legacy alias 설명이 혼동되지 않도록 공식 workflow를 단일 명령 체계로 정리하기 위함.
**근거:** `CLAUDE.md`와 `AGENTS.md`의 작업 루프 명령어 섹션, `scripts/orchestra.sh`

---

## 2026-05-22 — 미확정 M-tag 확정 처리

### DL-014 M-6 확정 — 강화 흔적 3종 아이템 정의·수급 경로

**파일:**
- `poro-rpg/docs/final_master_plan.md` (§13 미확정 항목에서 제거)
- `poro-rpg/docs/02_database_api_stats/CANON.md` (흔적 수급 경로 추가)
- `poro-rpg/docs/05_island_farm_system/CANON.md` (공방 가공기 제작 대상 명시)

**변경:**
- 별의 흔적 / 달의 흔적 / 태양의 흔적 = **강화 성공률 보정 아이템**으로 정의
- 수급 경로: **영지 공방 가공기에서 제작 가능** (레시피 미확정 — 사용자 확정 필요)
- economy_numbers_v2.md 강화표의 "강화 흔적 (선택)" 열과 정합성 확보

**이유:** 21강 이상 강화 진행 시 흔적 소모가 필수인데 수급 경로가 미확정이면 강화 진행 자체가 막히는 블로커였음. M-2 서브 에이전트 분석에서도 "성공률 수치보다 흔적 수급이 더 큰 병목"으로 지적됨.  
**근거:** 사용자 확인 (2026-05-22)

---

### DL-015 M-4 확정 — 전승권 비용

**파일:**
- `poro-rpg/docs/final_master_plan.md` (§13 미확정 항목에서 제거)
- `poro-rpg/docs/02_database_api_stats/CANON.md` (전승권 비용 추가)

**변경:**
- 기본 전승: **0G (무료)**
- 등급전승권: **100,000G**
- 세부스탯전승권: **100,000G**

**이유:** 오픈 후 7~14일차 시세 확인 후 결정 예정이었으나 기본 전승 무료 + 등급/세부스탯 전승권 각 100,000G로 사전 확정.  
**근거:** 사용자 확인 (2026-05-22)

---

## 2026-05-22 — PHASE 7 큐브 비용 하향 및 문서 정리

### DL-016 큐브 1회 비용 5,000G → 500G

**파일:**
- `poro-rpg/docs/02_database_api_stats/CANON.md`
- `poro-rpg/docs/02_database_api_stats/economy_numbers_v2.md`

**변경:**
- 큐브 1회 사용 골드 비용: **5,000G → 500G**
- 선발대 기준 일 150회 사용 (75,000G/일 소모) 기준으로 추정치 재산정

**이유:** 큐브를 일상적인 골드 소모처로 활용. 5,000G는 선발대도 하루 0.4회 수준으로 잠재 성장 체감이 너무 낮았음.  
**근거:** 사용자 확인 (2026-05-22)

---

### DL-017 enhancement_droprate_v1.md 아카이브

**파일:**
- `poro-rpg/docs/02_database_api_stats/enhancement_droprate_v1.md` → `poro-rpg/docs/_archive/enhancement_droprate_v1.md`
- `poro-rpg/docs/_archive/README.md` (PHASE 6 항목 추가)

**변경:** enhancement_droprate_v1.md를 _archive로 이동

**이유:** 강화석 파편 시스템 기반 드랍률 계산 문서. 파편 시스템 폐지(강화석 직접 드랍/소모)로 계산 방식 전체 무효.  
**근거:** 강화석 파편 시스템 폐지 확정 (DL-005 참조)

---

### DL-018 economy_numbers_v2.md 영지·공방 섹션 제거

**파일:** `poro-rpg/docs/02_database_api_stats/economy_numbers_v2.md`

**변경:**
- §2 영지 시설 슬롯 구성, §3 공방 가공기 레시피, §4 공방 대기열 한도 전체 삭제
- 해당 내용은 `poro-rpg/docs/05_island_farm_system/island_system_design.md`, `poro-rpg/docs/05_island_farm_system/workshop_crafting_spec.md`가 권위 있는 최신 버전으로 유지
- economy_numbers_v2는 강화·큐브·경제 분석 전용 문서로 범위 축소
- §1 작위 구매 비용 재료 컬럼을 전장의 파편 기반으로 수정 (island_system_design.md 기준)
- 폐지된 마력 결정, 자동재배기, 전투 식량, 구버전 공명 추출기 잔존 참조 일괄 제거

**이유:** 같은 내용이 두 문서에 있되 economy 문서가 구버전이면 반드시 혼동 발생. AI 에이전트가 구버전을 현재 기준으로 읽는 오류 재발 방지.  
**근거:** 사용자 지시 (2026-05-22)

---

## 2026-05-22 — PHASE 1 문서 통폐합 기준 확정

### DL-019 문서 정리 기준과 폐기 설계 목록 확정

**파일:**
- `poro-rpg/docs/final_master_plan.md`
- `poro-rpg/docs/02_database_api_stats/CANON.md`
- `poro-rpg/docs/04_combat_weapon_skills/CANON.md`
- `poro-rpg/docs/05_island_farm_system/CANON.md`
- `poro-rpg/docs/06_fields_bosses/CANON.md`
- `poro-rpg/docs/08_resourcepack_pipeline/index.md`

**변경:**
- 문서 정리 기준을 `final_master_plan.md` / 각 `CANON.md` / 최신 DL 항목 우선으로 고정
- archive 문서는 과거 맥락 추적용이며 현재 구현·수치·GUI 기준으로 사용하지 않는다고 명시
- 폐기된 구버전 설계 목록을 명시: 마력/발전기, 강화석 파편, 큐브 5,000G, 전승권 5,000G/50,000G, 망치 표시명, 도감/컬렉션 1차 포함, 시즌보스 주간 입장 제한, 고퀄 외부 보스 모델 1차 적용
- archive 이동된 `enhancement_droprate_v1.md` 활성 참조를 제거하고, 드랍률 재정리 기준을 `drop_tables_v1.md`로 이동
- 리소스팩 1차 우선순위에서 도감 GUI를 제외 항목으로 정리

**이유:** 통폐합/archive 전환 전에 어떤 문서가 현재 기준이고 어떤 구버전 설계를 폐기해야 하는지 먼저 고정해야 후속 archive 이동 시 기준 충돌이 재발하지 않음.
**근거:** 2026-05-22 문서 모순 리뷰 결과 및 사용자 지시

---

## 2026-05-22 — PHASE 2 구버전 GUI 문서 archive

### DL-020 구버전 GUI 상세 문서 3종 archive

**파일:**
- `poro-rpg/docs/08_resourcepack_pipeline/gui_functional_specs.md` → `poro-rpg/docs/_archive/gui_functional_specs.md`
- `poro-rpg/docs/08_resourcepack_pipeline/gui_territory_status.md` → `poro-rpg/docs/_archive/gui_territory_status.md`
- `poro-rpg/docs/08_resourcepack_pipeline/gui_boss_info.md` → `poro-rpg/docs/_archive/gui_boss_info.md`
- `poro-rpg/docs/08_resourcepack_pipeline/index.md`
- `poro-rpg/docs/08_resourcepack_pipeline/gui_todo_list.md`
- `poro-rpg/docs/05_island_farm_system/CANON.md`
- `poro-rpg/docs/05_island_farm_system/index.md`
- `poro-rpg/docs/04_combat_weapon_skills/index.md`
- `poro-rpg/docs/_archive/README.md`

**변경:**
- 마력/발전기, 강화석 파편, 큐브 5,000G, 전승권 구가격, 구 보스 보상 기준이 섞인 GUI 상세 문서 3종을 archive로 이동
- 활성 문서의 해당 링크를 제거하고 "현행 기준 재작성 필요" 상태로 표시
- archive README에 이동 이유와 대체 기준 문서를 기록

**이유:** 구버전 GUI 문서가 현재 CANON보다 상세해서 AI 에이전트가 구버전 수치와 흐름을 구현 기준으로 오인할 위험이 큼.
**근거:** PHASE 1 폐기 설계 목록, 2026-05-22 문서 모순 리뷰 결과

---

## 2026-05-22 — PHASE 3 활성 상세 문서 통폐합

### DL-021 경제·성장·드랍·GUI 허브 활성 문서 기준 정리

**파일:**
- `poro-rpg/docs/02_database_api_stats/economy_numbers_v2.md`
- `poro-rpg/docs/02_database_api_stats/equipment_growth_spec.md`
- `poro-rpg/docs/02_database_api_stats/potential_options_v1.md`
- `poro-rpg/docs/06_fields_bosses/drop_tables_v1.md`
- `poro-rpg/docs/08_resourcepack_pipeline/gui_hub_structure.md`
- `poro-rpg/docs/08_resourcepack_pipeline/gui_shop.md`

**변경:**
- `economy_numbers_v2.md`는 강화 비용·큐브 비용·골드 경제 지표 문서로 범위를 좁히고, 잠재 확률 세부 기준은 `potential_options_v1.md`로 위임
- 구 방어구 50% 강화석 비율, 강화석 파편 경매 표현, 자동심기 300,000G 잔재를 현재 기준으로 정리
- `equipment_growth_spec.md`의 큐브 비용을 500G로, 전승권 비용을 DL-015 기준으로 정리
- `drop_tables_v1.md`의 주간 첫 클리어/반복 보상 구조를 구버전 초안으로 표시하고, 큐브 비용과 균열왕 심장 용도에서 구버전 표기를 제거
- GUI 허브/상점 문서에서 마력 잔량, 강화석 제작, 강화석 파편, 자동심기 50,000G 잔재를 현재 기준으로 정리

**이유:** archive하지 않고 유지할 활성 상세 문서가 CANON보다 오래된 수치·흐름을 포함하면 통폐합 후에도 구현 기준 혼선이 계속 발생함.
**근거:** PHASE 1 폐기 설계 목록, DL-015, DL-016, DL-018

---

## 2026-05-22 — PHASE 4 구현 레퍼런스 재축약

### DL-022 implementation_reference.md 장문 구버전 archive

**파일:**
- `poro-rpg/docs/01_plugin_architecture/implementation_reference.md` → `poro-rpg/docs/_archive/implementation_reference_legacy.md`
- `poro-rpg/docs/01_plugin_architecture/implementation_reference.md` (현재 기준 진입점으로 재작성)
- `poro-rpg/docs/_archive/README.md`

**변경:**
- 1,500줄 이상의 장문 구현 레퍼런스를 archive로 이동
- 활성 `implementation_reference.md`는 현재 CANON 기준, 구현 상세 진입점, 확정 구현 기준, 금지된 구버전 기준만 담는 얇은 문서로 재작성
- archive README에 이동 이유와 대체 기준 문서를 기록

**이유:** 기존 구현 레퍼런스에 큐브 5,000G, 전승권 구가격, M-tag 미정, 구 GUI/보상 기준이 섞여 있어 AI 에이전트가 현재 CANON보다 구버전 구현 메모를 우선할 위험이 큼.
**근거:** PHASE 1 폐기 설계 목록, PHASE 3 활성 상세 문서 정리 결과

---

## 2026-05-22 — PHASE 5 활성 문서 잔여 구버전 키워드 정리

### DL-024 강화 흔적 3종 제작 레시피 및 효과 확정

**파일:**
- `poro-rpg/docs/05_island_farm_system/workshop_crafting_spec.md` (§9 레시피 전면 교체, `tab_trace` 신설)
- `poro-rpg/docs/05_island_farm_system/CANON.md` (레시피 확정 명시)
- `poro-rpg/docs/idea_inbox.md` (INBOX-001 PROMOTED)

**변경:**
- 별의 흔적 (`mat_trace_star`): 강화 성공률 **+20%p**, 마도합금×10 + 다이아블럭×2 + 에메랄드블럭×2
- 달의 흔적 (`mat_trace_moon`): 강화 성공률 **+30%p**, 마도합금×20 + 다이아블럭×4 + 에메랄드블럭×4
- 태양의 흔적 (`mat_trace_sun`): 강화 성공률 **+50%p**, 마도합금×40 + 다이아블럭×8 + 에메랄드블럭×8
- 공방 탭 `tab_trace` 신설 (기존 §9 "공방 제작 탭 내 포함" 표현 교체)
- 마도합금(`mat_mado_alloy`) 용도: **DL-034에서 확정** — 강화 흔적 3종 + 고대흔적 4종 핵심 재료. DL-034 전 구 레시피의 광부의 정수는 현행 기준이 아니다.

**이유:** 강화 흔적 레시피가 INBOX-001로만 남아 있어 구현 블로커. 사용자 확정으로 레시피와 효과 수치 동시 확정.  
**근거:** 사용자 확인 (2026-05-22)

---

### DL-023 활성 문서 최종 스캔 반영

**파일:**
- `poro-discord/docs/index.md`
- `poro-rpg/docs/07_boss_pattern_modules/season_boss_patterns.md`
- `poro-rpg/docs/01_plugin_architecture/poro_rpg_design_intent.md`
- `poro-rpg/docs/01_plugin_architecture/poro_rpg_module_design.md`
- `poro-rpg/docs/04_combat_weapon_skills/combat_balance_v2.md`
- `poro-rpg/docs/08_resourcepack_pipeline/gui_hud_spec.md`
- `poro-rpg/docs/02_database_api_stats/index.md`

**변경:**
- Discord 알림 목록에서 폐지된 마력 과부하 제거
- 보스 패턴/HUD/모듈 설계의 사용자 노출 무기명 `망치`를 `도끼`로 정리
- 강화석 획득량 설명에서 강화석 파편 표기를 제거
- DB/API 통계 개요에서 1차 시즌 제외 대상인 컬렉션 관련 테이블/API/통계 목표 제거
- Citizens/외부 보스 모델 설명을 현재 CANON 기준으로 정리

**이유:** Phase 1~4 이후에도 활성 문서 일부가 구버전 키워드를 현재 구현 기준처럼 포함하고 있었음.
**근거:** Phase 5 활성 문서 스캔 결과

---

## 2026-05-23 — 사용자 확정 반영

### DL-025 강화석 DB 재화 및 `axe` 내부 식별자 확정

**파일:**
- `poro-rpg/docs/02_database_api_stats/CANON.md`
- `poro-rpg/docs/02_database_api_stats/economy_numbers_v2.md`
- `poro-rpg/docs/02_database_api_stats/equipment_growth_spec.md`
- `poro-rpg/docs/06_fields_bosses/CANON.md`
- `poro-rpg/docs/06_fields_bosses/drop_tables_v1.md`
- `poro-rpg/docs/final_master_plan.md`
- `poro-rpg/docs/01_plugin_architecture/implementation_reference.md`
- `poro-rpg/docs/04_combat_weapon_skills/CANON.md`
- `poro-rpg/docs/04_combat_weapon_skills/weapon_skills_v1.md`
- `poro-rpg/docs/04_combat_weapon_skills/index.md`
- `poro-rpg/docs/08_resourcepack_pipeline/index.md`
- `poro-rpg/docs/08_resourcepack_pipeline/gui_todo_list.md`
- `poro-rpg/docs/08_resourcepack_pipeline/gui_boss_info.md`

**변경:**
- 강화석은 실물 아이템 드랍이 아니라 DB 가상 재화로 처치 시 직접 적립/소모하는 기준으로 통일
- `mat_stone_enhance` 실물 드랍/소모 표현 제거
- 무기 내부 식별자 `hammer` 유지 방침을 폐기하고 `axe`로 전환
- GUI 세부 설계 상태를 장비 계열 작성완료, 영지·보스·필드 계열 재작성필요로 정리

**이유:** 사용자 확정에 따라 강화석 데이터 모델과 무기 내부 식별자 기준을 명확히 하고, 진행 중인 GUI 상세 설계 상태를 실제 작업 상태와 일치시킴.
**근거:** 사용자 확인 (2026-05-23)

---

### DL-027 GUI 완료 상태 및 공식 배경 4종 확정

**파일:**
- `poro-rpg/docs/08_resourcepack_pipeline/index.md`
- `poro-rpg/docs/08_resourcepack_pipeline/gui_todo_list.md`
- `poro-rpg/docs/08_resourcepack_pipeline/gui_bitmap_spec.md`
- `poro-rpg/docs/08_resourcepack_pipeline/gui_png_make_guide.md`
- `poro-rpg/docs/08_resourcepack_pipeline/gui_hub_structure.md`

**변경:**
- 최신 커밋 기준 모든 GUI 설정이 완료된 상태로 정리
- 공식 GUI 배경을 `menu_main.png`, `menu_equipment.png`, `menu_territory.png`, `menu_boss.png` 4개로 확정
- `menu_hub_*` 27슬롯/서브허브 전용 배경은 공식 사용 대상에서 제외
- GUI 글리프 문자를 실제 `gui.json` 기준(``, ``, ``, ``)으로 통일

**이유:** GUI 설정 완료 상태와 공식 배경 4개 사용 방침을 문서 전체에 일관되게 반영하기 위함.
**근거:** 사용자 확인 (2026-05-23)

---

### DL-067 플레이어 레벨링 경험치 곡선·몹 EXP·보스 EXP 재확정

**파일:**
- `poro-rpg/docs/04_combat_weapon_skills/level_stat_system_v1.md` (§5 전체 대체)
- `custom-plugins/poro-rpg/src/main/java/com/poro/poro/leveling/PlayerLevelingService.java` (신규)
- `custom-plugins/poro-rpg/src/main/java/com/poro/poro/listener/FieldDropListener.java` (EXP 연동)

**변경:**
- 경험치 곡선: `550 × n^1.5` (2026-05-16 초안) → `round(800 × 1.1^(n-1))` (기하급수, 2026-05-26 확정)
- 몹 EXP: F1 50/100 → 10/25, F2 70/140 → 20/50, F3 100/200 → 30/75, F4 140/275 → 45/110, F5 200/400 → 60/150
- 보스 처치 EXP: **없음** (재화·흔적만 지급). 초안의 "보스 처치 시 보너스 경험치" 항목 삭제.
- 레벨 목표: "80 사실상 불가" → 선발대(하루 8h) 83~87 목표, 일반 ~60, 활성 ~70, 상위 ~78 로 재설정

**이유:**
- 초안 멱함수(`550 × n^1.5`)는 고정 킬레이트(25/min) 기준 선발대가 45일에 100레벨 초과 → 설계 한계 역할 불가.
- 기하급수 곡선은 레벨 85+ 진입 비용을 지수 폭증시켜 선발대의 80~85 목표를 자연 차단.
- 구 문서의 몹 EXP(200/kill@F5)는 "80 사실상 불가" 주장과 내부적으로 모순됨.
- 보스 EXP는 수급 예측이 어렵고 경쟁 요소를 과도하게 자극할 수 있어 배제.

**근거:** 사용자 구두 확정 2026-05-26, Codex 리뷰(P1) 지적 → 문서 갱신으로 해소.

---

### DL-068 장비의 흔적 드랍 등급 5종 확장

**파일:**
- `custom-plugins/poro-rpg/src/main/java/com/poro/poro/listener/FieldDropListener.java`
- `custom-plugins/poro-rpg/src/main/java/com/poro/poro/boss/engine/BossRewardService.java`
- `poro-rpg/docs/06_fields_bosses/drop_tables_v1.md`
- `poro-rpg/docs/04_combat_weapon_skills/item_grade_substat_v1.md` (§2 표 "시즌보스 1~3" → "1~6")

**변경:**
- 초기 구현: 정예몹/보스 모두 broken/faded/glowing 3등급만 드랍.
- 확장 후: 필드4/5 정예몹에 radiant(유니크)·brilliant(레전더리) 추가. 필드보스 필드4/5도 동일 확장. 시즌보스 1~6 전원 5등급 분포, 최종 3보스(균열왕·이중체·주시자)는 상위 분포 별도 적용.
- 등급 분포 수치: `item_grade_substat_v1.md §2` 표 기준 (상세 확률은 `FieldDropListener.randomTraceId()`, `BossRewardService.pickTrace()` 구현 참조).

**이유:**
- 상위 필드/보스 콘텐츠에 차별화된 보상 가치가 없으면 필드4/5 진입 동기 부족.
- `item_grade_substat_v1.md §2`는 처음부터 5등급 분포 체계를 설계했으나 초기 구현에서 3등급만 반영됨 — 이번 작업에서 문서 설계 의도 구현으로 정합.

**근거:** 사용자 구두 확정 2026-05-26 (Codex 리뷰 P2 지적 → 구현 확장 선택).

---

### DL-069 PvP 대전 시스템 — 자유/정규/친선 3종 확정

**파일:**
- `poro-rpg/docs/13_pvp_system/CANON.md` (신규)
- `poro-rpg/docs/idea_inbox.md` INBOX-003 → PROMOTED

**내용:**
- 1차 시즌(45일) 포함 결정.
- 3종 대전: 자유대전(현재 장비, 보상 없음), 정규대전(IL 60 동일화, 점수 +15/−10, 랭킹), 친선대전(이름 입력 → 요청/수락).
- 모든 대전 영지에서만 최초 진입 가능.
- 아레나: 보스룸 풀 방식 (사전 생성 고정 방 배정·반납).
- 정규대전 동일화: 5슬롯 모두 12강 고정(IL=60), 무기·각인·스탯 배분은 현재 그대로 유지. 가상 컨텍스트 방식으로 실제 인벤토리 변경 없이 구현.
- 사망: 즉시 패배 + 영지 귀환.
- 타임아웃: 3분, HP 비율로 승자 결정.
- 정규 점수: 초기 100, 승 +15, 패 −10, 시즌 종료 시 초기화.

**이유:**
- 전투 시스템이 완성된 1차 시즌에 PvP를 포함시켜 플레이어 간 비교 컨텐츠 제공.
- 정규대전 IL 동일화로 과금 우위 없이 클래스·각인·스킬 숙련도 경쟁 가능.

**근거:** 사용자 확정 2026-05-28 (INBOX-003 → 설계 결정).

### DL-070 잠재 옵션 풀 slot_type 아키텍처 결정

**결정:** 플러그인 잠재 옵션 풀은 방어구 slot_type을 `armor`(generic)로 유지한다. `head/chest/legs/boots` slot-specific 풀은 server-config DB용에만 존재한다.

**이유:**
- `EquipmentSlot.java`의 `itemSlotType()`이 모든 방어구 슬롯에 `"armor"`를 반환하므로, item_master에 `legs` 등을 쓰면 장착 검증(`EquipmentService:34`)이 실패한다.
- 45일 시즌 서버에서 `EquipmentSlot` + item_master + pool 구조 전체 리팩터는 범위 초과.

**결과 (1차 시즌):**
- `t1_armor_pool`/`t2_armor_pool`에 `boss_damage_increase` 전 등급 포함 (values = 하의 기준).
- `t1_legs_pool`/`t2_legs_pool`은 플러그인 seed에서 제거 (dead code).
- 하의뿐 아니라 투구·상의·신발에도 `boss_damage_increase`가 등장할 수 있음 (doc 기준 하의 전용이나 허용).

**부채:** 2차 확장 시 slot-specific 풀 아키텍처로 전환 필요.

---

### DL-071 CombatStateService 전투 태그 8초 만료 추가

**결정:** `isInCombat()`이 마지막 피격 시각으로부터 8초 경과 시 자동 해제된다.

**이유:**
- 기존 구현은 `exitCombat()` 명시 호출 없이 영구 유지 — 영지 바닐라 몹 피격 한 번으로 모든 전투 제한 GUI 영구 차단 버그 발생.
- `getLastHitTime()` 필드는 이미 존재했으나 `isInCombat()`에서 미사용 상태였음.

**결과:** `isInCombat()` 내부에서 lazy cleanup — 별도 스케줄러 없이 호출 시점 만료 판정.

---

### DL-072 영지 설정·시설 현황 GUI 전투 중 접근 차단

**결정:** `TerritorySettingsGui`, `TerritoryFacilityGui` 진입·복귀 경로 전체에 `combatStateService.isInCombat()` 검사 추가.

**이유:**
- 설계 문서 접근 조건 "전투 중 불가" 계약 이행.
- DL-071 수정 전까지는 영지 몹 피격 시 영구 전투 태그로 인해 실질적 차단 불가 상태였음.

**적용 위치:** `MainHubListener.handleTerritoryHub` (ZONE_SETTINGS), `TerritorySettingsGuiListener.handleSettings` (SLOT_FACILITY), `TerritorySettingsGuiListener.onClick` (TERRITORY_FACILITY 뒤로).

---

### DL-073 영지 허브 구조 개편 — 시설관리 영지설정 흡수·경매장 메인 이동

**결정:**
- 영지 서브 허브 8구역 → 6구역 (3×3 격자).
- 시설 관리 구역 제거 → 영지 설정 GUI 내 "시설 현황 →" 버튼(slot 17)으로 흡수.
- 경매장 구역 → 메인 허브 6번째 구역으로 이동.

**이유:** PNG 배경 제거 후 8구역 레이아웃이 시각적으로 복잡. 6구역 3×3은 메인 허브와 동일 패턴으로 일관성 확보.

**영향:** `gui_territory_settings.md` slot 17 기존 회색 → 시설현황 버튼으로 변경 기록.

---

### DL-074 강화·전승 GUI 45슬롯 정렬 — 설계 문서 기준 복원

**결정:**
- 강화 GUI: 54슬롯 → 45슬롯, 장비 선택 col8 세로 배치, 정보 슬롯 5개 독립.
- 전승 GUI: 54슬롯 → 45슬롯, `HEIR_SLOT_BACK` 45 → 36.

**이유:** 구현 당시 설계 문서(`gui_enhancement.md`, `gui_succession.md`)와 다른 레이아웃으로 구현됨. 강화 GUI는 신발 슬롯이 row3에 낙오되는 버그 포함.

**결과:** 두 GUI 모두 `gui_enhancement.md`·`gui_succession.md` 스펙과 일치.

---

### DL-075 상점 구매 단위 — 1세트/N세트 기준 (amount 인식)

**결정:** 상점 좌클릭 = 1세트(amount개), 우클릭 = `max(1, 64 / amount)` 세트 (~64개 기준).

**이유:**
- config.yml `shop.items.<item>.amount`는 1세트당 개수를 의미하지만 기존 `gui_shop.md` 기준 "1개/64개"로 구현 시 `amount: 64` 화살 우클릭 1회에 4,096개 지급되는 문제 발생.
- amount 인식 방식으로 변경 시 모든 품목에서 우클릭 = 약 64개 지급으로 일관성 확보.

**결과:**
- 좌클릭: 1세트 = `amount`개 × `price`G
- 우클릭: `max(1, 64 / amount)` 세트 = 약 64개
- 인벤토리 풀일 시 영지 창고 자동 적재 (`IslandStorage.add`)
- `gui_shop.md` §3/§4 동기화 완료

---

### DL-076 시즌보스 보상 — 등급 폐기 + 주간 10회 보너스 모델

**결정:** S/A/B/C 등급 시스템을 1차 시즌에서 폐기. 단순 확률 기반 단일 테이블 + 주간 누적 클리어 10회 임계 기반 분기로 대체.

**이유:**
- 등급 판정 기준(`damage_share`)이 1차 시즌 미사용 (DL-064).
- 클리어 시간 기반 등급은 별도 기획 결정 필요. 시즌 오픈 우선.
- "깨기만 하면 보상"이 1차 시즌 유저 경험에 더 적합.

**결과 (코드 구조):**
- 주간 1~10회 클리어: `firstTen` 보상 (강화석/큐브 풍부 + 장비 흔적 N개 확정 + 고대흔적 확정 + 치장 파편)
- 주간 11회+ 클리어: `beyondTen` 보상 (강화석/큐브만, 적게)
- 보스별 보상량: `drop_tables_v1.md §4` S 클리어 보상(=firstTen), S 재도전 보상(=beyondTen) 값 매핑
- 인원 배율 모두 1.0 (개인 지급이라 1인/2인 차등 없음)
- 시즌 칭호 재료 제외 (1차 시즌 칭호 시스템 미구현)
- 균열왕 심장: rift_king 주간 1~10회 클리어 시 1개 확정 (11회+ 없음)
- 흔적 등급 분포: 커먼 5%/레어 25%/에픽 45%/유니크 23%/레전더리 2%

**구현:**
- `BossSessionRepository.countClearsThisWeekAcrossBosses(uuid, week)` 추가
- `BossRewardService.setSeasonContext(repo, seasonStartEpoch)` setter — BossEngineRuntime 초기화 후 주입
- `SEASON_REWARDS` Map<String, BossSeasonRewards> — 보스 9종 인라인

**부채:**
- WorldGuard 등 외부 hook 없이 in-engine만 작동
- 치장 제작 파편(`cosmetic_fragment`)은 적립만 되고 사용처(치장 시스템) 미구현

---

### DL-077 PvP 시스템 1차 시즌 압축 구현 + 미해결 항목

**결정:** PvP 시스템 (CANON `poro-rpg/docs/13_pvp_system/CANON.md`)을 다음 범위로 1차 시즌 구현.

**구현 범위:**
- PvP 허브 GUI (54슬롯, 자유/정규/친선/랭킹)
- 정규대전 점수 (in-memory + pvp_rating DB 영속화, 초기 100점/승+15/패-10)
- 자유/정규 매칭 큐 (FIFO, 2명 모이면 즉시 텔레포트)
- 친선대전 (Anvil 닉네임 입력 + 30초 응답)
- 아레나 방 풀 (5×2 = 10개, /poro-genarenas 명령으로 생성)
- 매치 진행 (3분 타임아웃, 사망=패배, 서버이탈=자동 패배, HP 비율 비교)
- 매치 로그 (pvp_match_log DDL, FREE/RANKED/FRIENDLY 구분)
- 정규대전 가상 컨텍스트 (PvpContext: 12강 IL60 보관)
- /대전 /대전랭킹 /친선 명령

**미해결:** 없음 (모든 잔여 항목 처리 완료)

**처리됨 (후속 커밋):**
- 아레나 외 영역 PvP 차단 — `PvpDamageListener`에서 매치 외 PvP 전체 cancel + 매치 페어의 아레나 외부 데미지 cancel (WorldGuard 없는 환경에서도 차단됨)
- 영지 검증 — `PvpFriendlyService.attachSafeZone(svc, enforceIsland)`로 WorldGuard 환경에서만 영지 검증 적용. 미설치 환경에서는 검증 우회로 친선전 가능 (영지 구역 정의 자체가 없는 환경)
- 정규대전 IL 비율 데미지 공식 — `PvpContext.damageScale()` (VIRTUAL_IL / actualAvgIl) + `PvpMatchService.computeAverageIl` (5슬롯 평균, 강화 1당 IL 5). `PvpDamageListener`가 공격자 ctx.damageScale() 곱하고 2.0~18.0 클램프 (커밋 `fe89283`)
- 매치 중 텔레포트/명령 회피 차단 — `PvpTeleportListener` (`PlayerTeleportEvent` non-PLUGIN 차단 + 회피 명령 차단) (커밋 `a2c87db`)
- 보스 패턴 CSV ↔ Mythic 매핑 — `boss_pattern_mythic_mapping.csv` 신규 + `BossPatternMythicMapping` Registry. 향후 scheduler 결과를 Mythic `castSkill`로 발동시키는 hook 연결 시 사용

**파일:**
- `pvp/` 신규 패키지 (PvpRatingService, PvpArenaManager/Slot, PvpMatchService, PvpMatchType, PvpMatch, PvpContext, PvpFriendlyService, PvpArenaGenerationService)
- `pvp/db/` (PvpRatingRepository, PvpMatchLogRepository)
- `common/db/PvpDdl + PvpMigration`
- `gui/PvpHubGui + PvpRankingGui`
- `listener/PvpHubListener`
- `command/PvpArenaGenCommand`

**커밋:** b284fe5 (Phase 1) → bbde535 (2a) → 83a1dc9 (2b/c/d) → a806e1b (2e) → 2f (이 커밋)

---

### DL-126 보스 파티 시스템 v2 — 설정형 파티·준비·입장·채팅확인 (설계 확정, 구현 진행 중)

**배경:** 기존 보스 파티는 생성 즉시 빈 파티(최대 3 고정)만 만들고, 보스 허브 slot10 라벨이 "파티 생성"인데 실제로는 "파티 관리"를 여는 라벨·동작 불일치 버그가 있었다(사용자 보고 "파티 생성 눌러도 파티 관리로 간다"). 파티 흐름을 전면 재설계.

**확정 설계(사용자 답변 반영):**
- **생성 플로우**: 파티 생성 → ① 대상 보스(GUI 선택) ② 최대 인원 1~3(GUI 선택, 최대 3) ③ 파티 제목(채팅 입력, 모루 금지 정책 일관). → 생성 시 파티 목록에 노출.
- **참가**: 파티 목록에서 파티 클릭 → 입장 → 파티 관리 GUI.
- **파티 관리 GUI**:
  - 파티원 머리 = 스펙 표시(직업 + 평균 IL + 캐릭터 스탯. ※ `/캐릭터`는 현재 stub이라 보유 스탯[레벨·공격력·치명타 등]으로 대체).
  - 방장: 머리 클릭 → 추방. (GUI 닫힘 → 채팅 `추방하시겠습니까? [네][아니요]` 클릭형 → 네=추방+GUI 재표시)
  - 파티원: 준비(ready) 토글.
  - 방장: 전원 준비 시 GUI "보스룸 입장" 버튼 활성 → 클릭 시 파티 전원 입장. (기존 [보스] 표지판은 솔로/대체용 유지)
- **나가기**: ESC로 파티 관리 GUI 닫으면 → 채팅 `파티에서 나가시겠습니까? [네][아니요]` 클릭형 → 30초 무응답 = 자동 탈퇴.
- **보스 허브 라벨 정정**: slot10 "파티 생성"=실제 생성 플로우 연결, slot14 "보스 도전" → "보스 정보"(이미 보스 정보 화면 오픈).

**데이터 모델 확장(PartyManager.Party)**: bossId, title, maxSize, 멤버별 ready 상태, started 플래그.

**구현 단계(phased):**
- P1: 보스 허브 라벨 정정 + Party 모델 확장 + 생성 플로우(보스/인원 GUI + 제목 채팅).
- P2: 파티 목록(보스·제목·인원 표시) + 참가.
- P3: 파티 관리(머리 스펙·준비 토글·추방 채팅확인·전원준비 입장버튼).
- P4: ESC 나가기 채팅확인 + 30초 자동탈퇴.

**미확정/대체:** 머리 "캐릭터 스펙" 정확 항목(캐릭터 GUI stub) → 보유 스탯으로 대체. 채팅 [네][아니요] = Adventure clickEvent(runCommand) 방식.

**상태:** 설계 확정. P1부터 구현.

---

## DL-127 — 보스룸 공유 부활 토큰(데스카운트) + 수도 부활 + 파티 스코어보드 패널 (2026-06-02)

**배경:** 사용자 요청 — ① 죽으면 필드에서 부활하는데 수도에서 부활하게 ② 파티 생성 시 스코어보드에 데스카운트·보스명·파티원·HP(90/100) 표시 ③ 1/2/3인 파티에 데스카운트 3/4/5 부여, 소진 전까지 보스룸에서 부활.

**확정 설계:**
- **부활 라우팅(`BossRespawnListener`, PlayerRespawnEvent)**:
  - 보스룸 사망 + 공유 부활 토큰 잔여 → 보스룸 player-spawn에 부활(토큰 1 소모, 야간투시 재적용), 같은 슬롯 파티원에게 "남은 부활 N/M" 알림.
  - 보스룸 사망 + 토큰 소진 → 파티 전멸: `endRun(false, "party_wipe")` → 기존 실패 UX(메시지·영지 귀환).
  - 그 외(필드·영지 등) 일반 사망 → 수도(`world_hub`) 스폰에서 부활.
  - 인벤토리·경험치 유지는 기존 `DeathKeepInventoryListener`(PlayerDeathEvent)가 그대로 담당(역할 분리).
- **공유 부활 토큰(데스카운트)**: `BossRoomManager`가 slotId별로 보관. 입장 시 `initDeathPool(slotId, partySize)` = `partySize + 2` (1/2/3인 → 3/4/5). 슬롯 해제 시 정리.
- **파티 스코어보드 패널(`ScoreboardService`)**: 보스룸 입장 중에만 사이드바 하단에 `보스 {이름}` / `남은 부활 N/M` / `파티원` + 멤버별 `{이름} {현재HP}/{최대HP}`(HP 비율 색상) 표시. 보스룸 안에서는 위치워처가 매 초 강제 refresh(HP·토큰 실시간 반영).

**구현 파일:** `BossRoomManager`(데스풀·runIdOf·slotById), `BossRespawnListener`(신규), `BossRoomListener`(initDeathPool·applyBossVision public), `ScoreboardService`(attachBossContext·appendBossPanel·hpLine), `PoroRPGPlugin`(와이어링).

**상태:** 구현·배포 완료. 인게임 검증 대기.

---

## DL-128 — MythicMobs 5.11 스킬 구문 마이그레이션 (보스 패턴 실동작 복구) (2026-06-02)

**배경:** 사용자 "붙었을 때 데미지 말고 패턴을 쓴다고 느껴지지 않는다." 원인 진단 결과 보스/필드 스킬이 **현 MM 5.11에 존재하지 않는 구문**을 다수 사용해 패턴의 데미지·넉백·텔레그래프가 한 번도 실행되지 않았음(리로드 시 다량 경고).

**수정 매핑(reload 무경고까지 검증):**
- **AoE 관용구**: `aoe{r=N} @EntitiesInRadius{r=M,mobs=false} =MECH` (무효) → `MECH @PlayersInRadius{r=M}` (메커닉을 타게터에 직접 적용, 플레이어만 타격).
- **메커닉명**: `mdamage`→`damage`, `thrust{v;y}`→`throw{velocity;velocityY}`, `spawn{m=}`→`summon{mob=}`, `removepotion`→`potionclear`.
- **반복 블록**: `repeat{t;i}`…`endrepeat`(무효) → 서브스킬 추출 + `skill{s=Sub} @Self repeat=N repeatInterval=M`.
- **delay**: bare `- delay N` 형식이 정식(`delay{t=N}` 아님).
- **파티클(1.21 enum)**: CRIT_MAGIC→ENCHANTED_HIT, EXPLOSION_NORMAL→POOF, EXPLOSION_LARGE→EXPLOSION, EXPLOSION_HUGE→EXPLOSION_EMITTER, SMOKE_NORMAL→SMOKE, SMOKE_LARGE→LARGE_SMOKE, SPELL_WITCH/SPELL→WITCH, FIREWORKS_SPARK→FIREWORK, WATER_SPLASH→SPLASH, WATER_BUBBLE→BUBBLE, WATER_WAKE→FISHING, TOTEM→TOTEM_OF_UNDYING, BARRIER→END_ROD, SPARK→ELECTRIC_SPARK, REDSTONE→DUST(color), BLOCK_CRACK→BLOCK(material).

**대상 파일(런타임, gitignore):** `server/plugins/MythicMobs/skills/{boss_patterns,SeasonBossSkills,FieldBossSkills,field_skills}.yml`. `.bak.particle` 백업 생성. **재배포 시 재적용 필요**(server/ 전체 gitignored).

**잔여 미해결(별개 콘텐츠 공백):** 미정의 MythicMob 소환수 4건 — `F2_CaveZombie`(Elite_Summon·MG_SummonFragments), `F4_FallenSoldier`(FC_SummonKnights), `Abyss_Tentacle`(AO_SummonTentacles)는 mobs/에 정의가 없어 summon 실패. 해당 보스의 "쫄 소환" 패턴만 미작동(메인 패턴은 정상). 사용자 테스트 보스 타락 기사장(fallen_knight)은 영향 없음.

**상태:** 스킬 구문 수정·MM reload 무경고(소환수 4건 제외) 검증 완료. 인게임 패턴 발동 검증 대기.

### DL-128 추가 (2026-06-02) — 패턴 발동 검증 + 텔레그래프 가시성 보강 + 후속 코드 수정

**패턴 발동 검증:** `mm debug 3`로 fallen_knight 스폰 시 `P_00_BasicAttack ~onTimer:40`이 실제 실행되고 damage·particles가 도는 것을 콘솔 로그로 확정. **스킬 시스템은 정상**. 사용자가 "평타만" 느낀 원인은 ① 텔레그래프(예비 파티클·사운드)가 미묘해 평타와 구분이 안 됨 ② 큰 패턴(P-01/02/03/04)은 5~8초 주기·페이즈(60%/30%) 조건이라 짧은 전투에선 체감 적음.

**텔레그래프 보강(boss_patterns.yml P_01/02/03/04):** 각 패턴 예비 동작에 `actionmessage{...}`(액션바 경고, @PlayersInRadius r=14) + 예비 파티클 대폭 증량 + 사운드 강화. "⚠ 전방 강타/돌진/원형 폭발/휩쓸기 준비" 경고로 평타와 명확히 구분.

**season_bosses.yml(mob) 잔여 파티클 보정:** SPELL_WITCH→WITCH, WATER_SPLASH→SPLASH, FLASH(무효)→END_ROD.

**후속 코드 수정(DL-127 보완):**
- 파티 스코어보드 패널에 **본인 항상 포함**(LinkedHashSet, 본인 맨 위 `§b▶` 표시) — 기존엔 누락 체감 보고.
- 보스룸 **부활 시 야간투시 유지** — 사망이 포션효과를 초기화하므로 PlayerRespawnEvent 도중이 아닌 1틱 뒤 재적용으로 변경.

**미확정(사용자 결정 대기):** 보스 표기 HP(150,000, BossHubGui)와 실제 스폰 HP 불일치 — 실제 HP는 BossMaster/스탯 경로에서 결정. 패턴 충분히 체감하려면 실효 HP·생존시간 확보 필요(별도 밸런스 결정).

### DL-128 추가#2 (2026-06-02) — 스코어보드 15줄 제한 수정 + 패턴 채팅알림 + 타락 기사장 분신 소환(설계)

**파티 패널 미표시 원인·수정:** 마인크래프트 사이드바는 **최대 15줄**만 표시. 기존 스코어보드가 ~14줄을 채워 파티원 HP 줄이 잘려나갔음(야간투시는 별개라 정상). → 보스룸 입장 중에는 재화/레벨/IL 블록을 생략하는 전투 압축 레이아웃으로 전환, 파티 패널 노출. 패널에 **본인 항상 포함**(맨 위 `§b▶`).

**패턴 채팅 알림:** P-08 전환·P-09 무적·P-09 딜타임은 기존 `speak`(채팅) 보유. P-03 원형 폭발에 `speak` 추가. 공용 P-01/02/04는 actionbar 경고 유지.

**타락 기사장 분신 소환 — 설계 패턴 구현(사용자 결정 "설계대로"):**
- 원인: fallen_knight가 `Type: EVOKER`라 바닐라 벡스("분신체")를 강제 시전. EVOKER 주문은 `AIGoalSelectors: clear`로도 억제 불가, MM 전용 억제 옵션 없음.
- 해결: **Type EVOKER→WITHER_SKELETON**(검 든 언데드 기사 — 테마 일치 + 벡스 원천 제거). 네더라이트 검/투구/흉갑 Equipment. 되돌리기 쉬움(한 줄).
- 신규 소환수 `fallen_knight_phantom`(WITHER_SKELETON, HP250, 30초 자가 소멸로 누적 방지) + 신규 스킬 `P_SummonPhantoms`(채팅+actionbar 알림 → 분신 2마리). fallen_knight에 `~onHealthPercent:60/30` + `~onTimer:400 ?health{h<60%}` 연결.
- ※ storm_sorcerer는 술사라 EVOKER 유지(벡스 적합).

**검증:** mm reload 무경고(신규 몹/스킬/summon 정상). 잔여 경고는 여전히 미정의 소환수 4건(F2_CaveZombie/F4_FallenSoldier/Abyss_Tentacle — 별개). 인게임 외형·소환 발동 검증 대기.

### DL-128 추가#3 (2026-06-02) — 보스 페이즈 조건 문법 수정(공용 패턴 미발동 근본 원인) + HP 캡 발견

**근본 원인:** 보스/필드 스킬의 페이즈 게이팅 `?health{h>60%}`(이전 hpabove/hpbelow 수정본)이 **런타임에 항상 실패** → P-01 전방강타·P-02 돌진 등 공용 패턴이 한 번도 발동 못 함(유저 "돌진 없다"). 진단:
- MM에는 `health`(절대값, 속성 `amount`)와 `healthpercent`(퍼센트, 속성 `percent`) 두 조건이 분리. `health{h>60%}`는 존재하지 않는 속성 `h`라 무효.
- 1차 교체 `healthpercent{percent>0.6}`도 실패 — `healthpercent`는 ComparisonCondition이라 값이 RangedDouble. `{percent>0.6}`는 `=`가 없어 `percent` 속성 미설정.
- **정답: `?healthpercent{percent=>0.6}`** (key=`percent`, value=`>0.6`), `?healthpercent{percent=<0.3}` 등. mm debug 3로 100% HP에서 P_01/P_02 발동 + phase2/3 미발동 확인.
- 전 스킬/몹 파일 75개 조건 일괄 교체(skills·mobs). 로드 통과 ≠ 동작 — 반드시 debug로 발동 검증.

**부수 효과:** 분신/페이즈 전환이 스폰 즉시 터지던 것도 해소(이제 실제 HP가 임계 통과할 때만). 분신 소환 채팅도 전투 중 정상 표시.

**HP 캡 발견(미해결·밸런스 결정 대기):** fallen_knight 실측 HP=**1024**. `minecraft:max_health` 속성의 바닐라 상한 1024 때문에 MM `Health:150000`이 클램프됨. BossHubGui 표기(150,000)와 불일치. 1024는 파티 보스로 낮아 패턴 페이즈가 빨리 지나감. 해결안: ① 표기를 실제로 낮춤 ② 데이터팩으로 속성 상한 상향(server/) ③ 보스 DEF로 실효 HP 확보(설계 DEF 100 활용). 사용자 결정 필요.

**유지 결정:** WITHER_SKELETON 피격 시 위더 효과 — 유저 "그냥 둘까" → 유지(언데드 기사 플레이버).

### DL-128 추가#4 (2026-06-02) — 보스 사망 시 분신 제거 + 텔레그래프를 액션바→채팅 전환

**분신 정리:** fallen_knight에 `- remove @MobsInRadius{types=fallen_knight_phantom;r=120} ~onDeath` 추가 — 보스 처치 시 잔여 분신 즉시 제거(자가 30초 소멸과 별개로 즉시).

**돌진/강타가 안 보이던 원인:** 포로 HUD가 **액션바를 점유**해 패턴의 `actionmessage`(액션바 경고)가 즉시 덮여 안 보였음("고유 패턴은 보임"은 그것들이 채팅 `speak`을 써서). → P-01 강타·P-02 돌진·P-03 폭발·P-04 휩쓸기의 액션바 텔레그래프를 모두 **채팅(`speak`, @PlayersInRadius r=40~60)**으로 전환. actionmessage 전량 제거.

**검증:** mm reload 무경고(@MobsInRadius·onDeath·remove·speak 정상). 공용 패턴 발동 자체는 DL-128#3에서 debug로 확인 완료(조건 수정). 인게임 채팅 가시성 검증 대기.

**잔여:** HP 캡 1024(DL-128#3) 미해결 — 보스가 빨리 죽으면 패턴 노출 시간 부족. 밸런스 결정 대기.

### DL-128 추가#5 (2026-06-02) — 패턴 실데미지/돌진 동작 수정 + 빈도 완화

**딜이 안 들어가던 원인:** MM `damage` 메커닉의 `amount`(=`a`)는 **고정 데미지**이고 배수 옵션이 없음(배수는 프리미엄 math 필요). 기존 `damage{a=1.2}`("120%" 의도)는 실제로 1.2 데미지(0.6칸)라 사실상 0. → 설계 %를 보스 Damage(~85) 기준 **고정값으로 변환**: P-00 40 / P-01 85 / P-02 105 / P-03 90 / P-04 75 (P-05 70, P-11 95, P-12 40×3, P-13 110). ※ 공용 패턴이라 상위 보스는 저평가 — 추후 보스별 변형 또는 프리미엄 math 필요.

**돌진이 안 움직이던 원인:** `throw{velocity=5} @self`는 자기를 원점에서 밀어내라는 것이라 방향이 없어 무효. → `leap{velocity=1.3} @target`(타겟으로 도약=돌진)로 교체.

**빈도 완화("패턴 빠르다"):** fallen_knight 타이머 ~1.6배. P-00 2.5s / P-01 8s / P-02 14s / P-04 10s / P-03 16s / 분신 30s. 채팅 텔레그래프 스팸도 함께 완화.

**검증:** mm reload 무경고(leap·damage 정상). 실제 타격량·돌진 이동은 인게임(플레이어 대상) 검증 필요. 데미지 수치는 플레이어 EHP 미확인이라 과/소하면 조정 — 사용자 피드백 대기.

**잔여:** HP 캡 1024, 공용 패턴 데미지의 보스별 스케일링, P-09 무적 30s(저HP 보스에서 다운타임 길 수 있음) — 밸런스 결정 대기.

### DL-128 추가#6 (2026-06-02) — 타락 기사장 패턴 디버그 분석 + 후속 수정

**분석 방법:** forceload로 보스 유지 + mm debug 3로 각 HP 구간에서 "Running Skill" 로그 집계(플레이어 없이도 트리거+조건 발동 확인).

**분석 결과(발동 확인):**
- Phase1(100%, 16s): P_00×9, P_01 강타×2(8s), P_02 돌진×1(14s) — 전부 정상.
- Phase2(56%, /damage 450): P_04 휩쓸기 신규 발동(percent=<0.6) 확인.
- Phase3(24%, Health 250): P_01×2·P_02×2·P_04×2·분신 주기소환×1 — 조건 패턴 전부 정상.
- 결론: **타이머+`?healthpercent` 조건 패턴은 전부 신뢰성 있게 발동.** 

**발견 이슈:**
1. **돌진 체감 약함**: `leap @target`인데 플레이어 근접 시 이동 거리≈0 → leap velocity 1.3→2.6 상향. (원거리에서 가장 잘 보임 — 근접 고정 전투에선 한계.)
2. **"60/30% 딱 아니고 그 후에 소환"**: ① `~onHealthPercent`는 임계 통과 *히트*에 발동(MM 특성, 딱 60% 불가) ② 별도 주기 소환 `~onTimer:600 ?percent<0.6`이 30s마다 발동 → 혼란. → **주기 소환 제거, 임계(60/30%) 전용으로.**
3. **P-09 무적 30s**: 해제 메카닉(SP-11/12) 미구현이라 깰 수 없는 30s 무적 = 답답. → **비활성(주석), EmpireRPG 연결 후 복구.**
4. `~onHealthPercent`는 /damage·data merge로 테스트 불가(실 전투 데미지에만 반응) — 실 전투에선 작동(유저가 소환 확인).

**수정:** 주기 소환 제거 / leap 2.6 / P-09 비활성. mm reload 무경고.

**잔여:** HP 캡 1024, 데미지 보스별 스케일, 돌진 근접 한계(원거리 유도 후 돌진은 추가 설계 필요) — 결정 대기.

### DL-128 추가#7 (2026-06-02) — 페이즈 전환 콤보화 + 돌진 "우다다" 재설계

**페이즈 전환 콤보(60/30%):** 사용자 요청 — 무적+폭발+소환을 하나로. P_08_PhaseTransition 재설계: `speak` 경고 → DAMAGE_RESISTANCE 5(60t=**3초 무적**, 전환 보호용 짧게) → 주변 폭발 `damage 75 @PIR{r=7}`+넉백 → 분신 2마리 소환. 별도 P_SummonPhantoms 트리거 제거(콤보에 통합). P-09 30s 무적은 계속 비활성(해제 메카닉 미구현).

**돌진 "우다다" 재설계:** 기존 leap(아치형 도약)은 근접 시 체감 0. → `potion{SPEED;lvl=4;50t}`(2.5초 신속 추격) + `lunge @target`(타겟으로 달려듦) ×4회(repeatInterval 8t)로 "플레이어에게 우다다 달려오는" 연출. 1틱당 damage 45(누적 ~90-135). lunge="Causes caster to lunge forward at target".

**전방강타(P-01) 사거리:** `@PlayersInRadius{r=3}` = 3블럭(근접). 필요 시 조정 가능(MM 무료판 콘 타게터 미지원이라 원형 AoE).

**검증:** mm reload 무경고(lunge/potion/repeat/summon 정상). 돌진 P_02+P_02_DashTick 발동 확인. 전환 콤보는 onHealthPercent라 실전투 검증 필요.

### DL-128 추가#8 (2026-06-03) — 페이즈 전환 콤보 신뢰성 트리거 + 유예시간

**문제:** `~onHealthPercent`가 fallen_knight에서 안 터짐("무적/폭발/분신 안됨"). 원인 — 포로 전투 데미지(`target.damage()`)가 MM의 HP% 추적과 연동되지 않아 onHealthPercent가 임계를 못 잡음(이전 "그 후에 나옴"은 제거한 주기소환이었음).

**해결(디버그 검증):** `~onHealthPercent` → **`~onTimer:20 + ?healthpercent{percent=<0.6} + ?!variableisset{var=caster.p2}`**. 타이머가 매 1초 HP%를 직접 읽어 임계 통과 감지(데미지 경로 무관), `setvariable @self`로 1회용 플래그 + 래퍼 메타스킬 `Cooldown:99999` 이중 가드 → **정확히 1회 발동**(debug: set 실행 → 다음 틱 "Condition variableIsSet failed"=차단 확인). 함정: `setvariable`는 `@self` 타게터 필수("No applicable targets" 취소됨).

**전환 콤보 + 유예시간(사용자 요청):** P_08 = 예고("분노 폭발 준비! 피하세요") + 3초 무적(연출 보호, P-09 30s 무적과 별개) → **1.75초 유예**(충전 연출) → 폭발(반경7, 75딜, 유예 동안 탈출 시 회피 가능) → 분신 2마리. 돌진 직후 60% 즉발 폭발로 즉사하던 문제 해소.

**잔여:** 타 시즌보스(corrupted_lord·stone_colossus 등)는 아직 `~onHealthPercent` 사용 — 동일 문제 가능, 추후 같은 방식으로 교체 필요(현재 fallen_knight만 테스트 중). HP 캡 1024도 미해결.

### DL-128 추가#9 (2026-06-03) — 돌진을 "직선 예고 → 전방 돌진"(비호밍)으로 재설계

**사용자 요청:** 호밍(플레이어 추적) 돌진 대신, 보스 전방 직선 위협을 띄우고 1초 후 그 방향으로 돌진(회피 가능).

**구현(P_02_LinearDash):**
- 1) 예고: `speak`("직선 돌진 준비! 옆으로 피하세요") + `P_02_DashWarn`(전방 불꽃+연기 라인 `effect:particleline @Forward{f=12}`) 1초간 반복(repeat 5/interval 4).
- 2) 돌진: `lunge{velocity=1.9} @Forward{f=10}`(타겟 추적 아닌 **전방** 돌진) + `P_02_DashHit`(0.1s마다 경로 위 플레이어 35딜+넉백, repeat 9).
- @Forward 타게터로 호밍 제거 → 옆으로 피하면 회피. mm debug로 particleline·@Forward·lunge 정상 실행 확인(무에러).

**잔여:** 윈드업 1초 동안 보스 AI가 플레이어를 향해 약간 회전 가능(완전 고정 방향은 변수에 방향 저장 필요) — 현재는 돌진 시점 facing으로 직진하므로 막판 사이드스텝으로 회피 가능. HP 캡 1024, 타 보스 onHealthPercent 잔존.

### DL-128 추가#10 (2026-06-03) — 돌진: 제자리 고정(stun) + 라인 가시성 수정

**라인 안 뜨던 원인:** `effect:particleline`의 파티클명을 소문자(`flame`/`smoke`)로 써서 미렌더(무에러 무표시). → 대문자 `FLAME`/`LARGE_SMOKE`, 밀도↑(distanceBetween 0.35), 높이 y=1.0(가슴)로 수정.

**제자리 고정(사용자 요청):** 윈드업 동안 보스가 추적/이동하던 것을, `stun{duration=20} @self`로 **1.1초 제자리 고정**(이동·회전·공격 정지). 고정 중 전방 라인 표시 → 고정 해제 후 `lunge @Forward{f=11}` 전방 돌진. 고정으로 방향도 잠겨 라인=돌진경로 일치, 옆으로 피하면 회피.

**검증:** mm debug — stun·effect:particleline(FLAME/LARGE_SMOKE)·lunge@Forward·DashHit 전부 정상 실행, reload 무경고.

### DL-128 추가#11 (2026-06-03) — 보스 HP 1024 클램프 근본 해결(spigot 캡) + 패턴 데미지 완화

**HP 1024 클램프 근본 원인:** `server/spigot.yml`의 `settings.attribute.maxHealth.max: 1024.0` — Spigot이 모든 엔티티 최대체력을 1024로 제한. MM `Health:150000`이 실효 1024로 클램프 → 보스 즉사 + (이전) HP% 깨짐의 공통 뿌리였음. → **`maxHealth.max: 1024.0 → 2048000.0`** 상향(재시작 필요, 완료). 검증: fallen_knight 스폰 시 Health=8000.0 확인.

**보스 HP:** fallen_knight `Health: 150000 → 8000`(솔로 테스트값, 원 설계 파티 150000). `~onSpawn P_FillHealth`(heal 최대치) 백업 추가. 캡 상향으로 healthpercent 페이즈 조건도 실제 %로 정상화.

**패턴 데미지 완화("너무 쌔다"):** 약 절반. P-00 20, P-01 강타 42, P-02 돌진틱 18, P-03 폭발 45, P-04 휩쓸기 38, P-08 폭발 38, P-05 18, P-11 48, P-13 55.

**주의:** spigot.yml은 server/ gitignore — 재배포 시 재적용 필요(캡 상향 반드시 포함). 타 보스도 동일 캡 혜택, HP는 각 설계값 적용됨(파티 기준이라 솔로는 길 수 있음).

### DL-128 추가#12 (2026-06-03) — 플레이어 HP 산식 상향 + 보스 데미지 역산 + 보스 타이머 표시

**배경(사장님 지적):** 보스 데미지가 강화당 HP/DEF 증가를 고려 못 해 방어구 강화 의미가 약함. +18강에도 HP ~158로 너무 낮음. → +18 기준 HP 500~600 필요, 데미지는 적정 강화대 HP/DEF 역산.

**정정:** 보스 데미지는 이미 `PlayerDefenseListener`에서 DEF/(DEF+200) 감산 적용됨(방어구 무의미 아님). 진짜 문제는 HP 산식.

**HP 산식 상향(SkillContext.java):** 기본 HP 슬롯합 80→180(투구90·상의50·다리20·신발20), **HP 전용 강화계수 신설 0.11**(DEF는 0.04 유지·분리). 결과 +0≈200 / +8≈358 / +18≈556. 강화 2.8배 스프레드로 방어구 강화 체감 확보.

**보스 데미지 역산(boss_patterns.yml, 적정 358 HP·26% 감산 기준):** 기본 28 / 강타 62 / 돌진틱 28 / 폭발 72(회피) / 휩쓸기 50 / 전환폭발 62 (효과 HP의 8~19%).

**보스 남은시간 표시:** `BossRunService.remainingSeconds(runId)` 신설 → `ScoreboardService.appendBossPanel`에 `남은시간 M:SS`(60초↓ 빨강, 1초 갱신). BossRun 타임아웃(일반 900s/최종 600s) 기준. attachBossContext에 provider 추가.

**잔여/주의:**
- applyMaxHealth는 접속·강화·장착 시 호출 → 기존 접속자는 재접속/재장착해야 새 HP 적용.
- ★ 필드몹 데미지는 옛 HP(100~158) 기준 → HP 상향으로 너무 약해질 수 있음, 재조정 필요.
- 타 시즌보스 HP는 설계 15만대 그대로(솔로 길어짐). 공용 패턴 데미지 상향은 전 보스 공유.
- HP 곡선·데미지 수치는 인게임 체감 후 조정.

### DL-128 추가#13 (2026-06-03) — 무기 ATK 강화 곡선 ↑ + 필드 몹 ATK 재밸런스

**배경(사장님):** 무기 강화 ATK가 기본 대비 너무 적음(+18에 base×1.8). 필드 몹 ATK가 필드별 차이 미미(F1 4 → F5 6)라 무기만 들고 F5 즉시 사냥 가능(저강화 게이팅 부재). HP 상향(#12)과 균형 필요.

**무기 ATK 강화(WeaponPowerCalculator):** ENHANCE_ATK_BONUS ~1.8배 상향. base 80 기준 0강 80 / 10강 121 / 18강 195(2.4배) / 25강 282(3.5배). 강화 체감 확보.

**필드 몹 ATK(MobStatOverrideService ATK_SEED):** 새 HP 곡선(200~556) 기준 필드별 가파른 스케일 — 일반 F1 8 / F2 13 / F3 19 / F4 26 / F5 34, 정예 ~2배, 필드보스 ~2.5배(F5 정예 72·보스 90). 상위 필드 = 저강화 플레이어 게이팅.
- **시드 재적용**: 기존 insertIfAbsent(기존 DB값 고정) → loadAndSeed가 ATK 시드와 다르면 upsert(HP/DEF 보존). 부팅 로그 "ATK 시드 적용/갱신 21건" + 실측 Wall_SentinelElite attack_damage=72 확인.

**필드 몹 HP:** 1024 캡 해제(#11)로 YAML값 복원(F1 300 → F5 1800, 미니보스 1500~9000). 별도 변경 없음.

**잔여/주의:**
- 운영자 /poro-mobstat ATK 변경은 재부팅 시 코드 시드로 복원(밸런스 단계 정책 — 확정 후 author 기반 보존으로 전환 예정).
- ★ 필드보스 HP 10만~66만이 캡 해제로 실제 적용 → 솔로 과다 가능, 별도 검토 필요.
- 무기 ATK·필드 ATK 수치는 인게임 체감 후 조정. 기존 접속자는 재접속해야 무기 ATK 갱신.

### DL-128 추가#14 (2026-06-03) — 보스 DEF 실데미지 적용 + 방어력무시 잠재 도입

**배경(사장님 결정):** 기존엔 플레이어→보스 스킬 피해에 DEF 경감이 코드 어디에도 없어(보스는 순수 HP 스펀지) "방어력무시" 잠재가 무의미. 보스 DEF를 실효화하여 방어력무시 스탯에 의미 부여.

**구현:**
- **보스 DEF 시드**(`MobStatOverrideService.DEF_SEED`): season_boss_stats_v1 설계값 — fallen_knight 100 / corrupted_lord 150 / stone_colossus 180 / storm_sorcerer 210 / abyss_guardian 240 / void_herald 265 / rift_king 280 / corrupted_dyad 270 / spirit_watcher 270 / 분신 40. 스폰 시 엔티티 PDC `poro_rpg:mob_def`에 기록.
- **DEF 경감**(`BaseWeaponSkill.dealDamage` + `SkillContext.defenseMitigation`): `×200/(200+유효DEF)`, 유효DEF = DEF×(1−방어력무시%/100). DEF 미기록 몹은 1.0(영향 없음). 검증: fallen_knight 스폰 PDC mob_def=100.0, 경감 0.667.
- **방어력무시 잠재**(`growth_potential_option_pool.csv` t1_weapon_pool): defense_ignore COMMON 2~5 / RARE 6~10 / EPIC 11~16 / UNIQUE 17~23 / LEGENDARY 24~32(%). `SkillContext.defenseIgnorePercent` 합산.

**CANON 명확화(금지 항목 재정의):** 1차 금지 "방어 감소"는 **적에게 거는 방어 디버프(전원 공유)**를 의미 — 유지 제외. **방어력무시(플레이어 개인 관통 스탯, 잠재)**는 디버프가 아니므로 별개로 **도입**. (CANM.md §금지 설계 주석 갱신.)

**잔여:** ① 보스 HP는 DEF 실효화로 effective HP = HP×(DEF+200)/200 (fallen_knight 8000×1.5=12000) — 여전히 2~3분 전투엔 부족, HP 상향은 후속(decision #2/#3). ② boss_session 통계의 defense_ignore_pct는 여전히 0 기록(스탯 와이어링 후속). ③ 필드 몹 DEF 미시드(0) — 필요 시 추가.

---

### DL-129 (2026-06-03) — 잠재 풀 전면 정비 1단계: CSV 정본 재작성 + 쉬운 메커니즘 배선

**배경(사장님 "A로 가야지" 확정):** 구현 잠재 풀(`growth_potential_option_pool.csv`)이 정본 `poro-rpg/docs/02_database_api_stats/potential_options_v1.md`(2026-05-24 재확정)과 전혀 안 맞았음. 폐기/금지 옵션(mark_target_damage=적표식 금지·crack_efficiency·resonance_effect_up·conditional_damage_bonus·core_tag/precision_tag·status_resistance·recovery/shield_efficiency·survival_trigger·playstyle_completion·combo_tag·resource_gain) 다수 혼재, 정본 옵션 다수 미구현. 실제 작동 옵션은 4종(attack_percent·general_damage_increase·boss_damage_increase·defense_ignore)뿐이었음.

**1단계 범위(사장님 단계 순서 1→2→3 확정):**
- **CSV 정본 재작성**: 폐기/금지 옵션 전부 제거. 무기풀 6종(attack_percent·defense_ignore·general_damage_increase=스킬피해%·boss_damage_increase·crit_chance_percent·crit_damage_percent) + 방어구 통합풀 6종(max_hp_percent·defense_percent·crit_chance_percent·general_damage_increase·boss_damage_increase·damage_reduction[유니크+]). 수치 정본 §2-1~2-4. 가중치 희귀=2·보통=3(정본 §6 1:1.5 비율 보존). T1+T2 미러.
- **소비처 신규 배선(`SkillContext`)**: critChance(+crit_chance_percent)·critDamageMultiplier(+crit_damage_percent)·defense(×defense_percent)·applyMaxHealth(×max_hp_percent)·damageReductionPercent()[신규]·applyMoveSpeed()+MOVEMENT_SPEED[신규]·applyDerivedAttributes()[통합 헬퍼].
- **받피감 적용(`PlayerDefenseListener`)**: 몹→플레이어 피격에 DEF 경감 × (1−damage_reduction%) 순차 곱산. 0~80% 클램프.
- **재적용 지점**: 접속(`PlayerJoinListener`)·강화·큐브 사용/유지/선택(`GrowthGuiListener`)에서 `applyDerivedAttributes` 호출 — HP%/이속%는 attribute 베이킹이라 명시 재적용 필요(DEF%·치명·스킬/보스피해·받피감은 데미지 시점 라이브 합산이라 불필요).
- **라벨 정본화(`EquipmentLoreRenderer.potentialOptionKr`)**: general_damage_increase→"스킬 피해", 신규 코드 한글명 추가, 폐기 코드 매핑 제거. 캐릭터 스탯 패널(치명확률·치명피해·피해감소)에 잠재 합산 표시.
- **defense_ignore 수치 교정**: DL-128#14의 임시값(C2~5·R6~10·E11~16·U17~23·L24~32)을 정본 §2-1(C1~2·R2~4·E5~8·U8~12·L13~18)으로 하향 정렬.

**검증:** `./gradlew compileJava` → BUILD SUCCESSFUL. MasterSeedValidator는 잠재 옵션 코드 미검증(부팅 안전). CSV 로더는 `#` 주석 라인 스킵 확인.

**의도적 1단계 한계(2·3단계 이월):**
- **방어구 통합풀 근사**: 머리/상의/하의/신발이 같은 t1_armor_pool 공유 → 머리가 보스피해% 롤 등 슬롯 테마 일부 혼선. EquipmentSlot enum이 4부위를 전부 `itemSlotType="armor"`로 매핑·`EquipmentService.equip`가 이를 장착검증에 사용 → 슬롯별 풀 분리는 item_master slot_type 세분(=구조변경)이 필요한 **3단계**. 옵션 정의(코드·수치·가중치)는 그대로 재사용되므로 버리는 작업 아님.
- **미배선 옵션(2단계)**: 스킬타입 4종(기본/이동/특수/핵심 피해%, dealDamage에 입력 슬롯 태그 전달 필요)·cooldown_reduction_percent(CooldownManager 훅). CSV 미포함 — 소비처 생기면 추가.
- **이속% 롤소스**: move_speed_percent 소비처(applyMoveSpeed)는 1단계 구현됐으나 정본상 신발 전용 → 롤 소스는 3단계 신발풀 분리 시 등장.
- **base 치명 5%**: critChance()는 여전히 트리+잠재만(기본 5% 미가산) — GUI 표시(5%+)와 전투 불일치, 별개 선행 이슈로 미반영.
- **PvP 받피감**: PlayerDefenseListener가 PvP 제외라 미적용(후속).

**다음 세션:** 2단계(스킬타입 4종 + 쿨감) → 3단계(슬롯별 풀 완전 분리, item_master 구조변경 — 사장님 확정).

---

### DL-129 추가#1 (2026-06-03) — 잠재 풀 2단계: 스킬타입 피해% 4종 + 쿨타임 감소%

**범위:** 1단계 미배선 옵션을 모두 작동화. 무기풀이 정본 §1-1 **11종 완성**(ATK·방무·스킬피해·보스피해·기본/이동/특수/핵심·치확·치피·쿨감).

**핵심 설계 — 입력 슬롯은 스킬 키로 결정론적:** `SkillInputListener`의 slot1~4 스위치 테이블이 곧 입력→스킬 매핑(slot1=기본기 LC / slot2=이동기 RC / slot3=특수기 SRC / slot4=핵심기 F). 즉 스킬 키만으로 타입 판별 가능. `dealDamage`는 `BaseWeaponSkill` 인스턴스 메서드라 `this.key()` 접근 → **호출부 수정 0**으로 배율 주입.

**구현:**
- **`combat/SkillType.java`[신규]**: enum BASIC/MOBILITY/SPECIAL/CORE + option_code(basic/mobility/special/core_skill_damage) + `fromKey(키)` 정적 맵(6무기×4슬롯=24키, SkillInputListener와 동기 필수). 미등록 키(평타 등)→null→배율 미적용.
- **`SkillContext`**: `skillTypeMultiplier(player, type)` = 1+해당 스킬타입 잠재%/100 / `cooldownReductionPercent(player)` = Σcooldown_reduction_percent, 0~50% 클램프.
- **`BaseWeaponSkill.dealDamage`**: 데미지 체인에 `× skillTypeMultiplier(attacker, SkillType.fromKey(key()))` 추가.
- **`SkillService.useSkill`**: applyCooldown 시 `effectiveCd = cooldown × (1−쿨감%)` 적용. applyCooldown이 totalMs를 저장하므로 HUD 진행바도 감소분 반영.
- **CSV**: weapon/armor 풀(T1+T2)에 basic/mobility/special/core_skill_damage(정본 §2-1 무기 C2~4·R5~8·E10~14·U15~19·L20~26 / §2-2~4 방어구 C1~2·R3~4·E5~8·U8~11·L12~16) + cooldown_reduction_percent(C1·R2·E3~4·U5~6·L7~9) 추가. 가중치: 무기 스킬타입·쿨감=보통(3, 정본 §6-1), 방어구 스킬타입=희귀(2)·쿨감=보통(3).

**검증:** `./gradlew compileJava` → BUILD SUCCESSFUL. CSV t1_weapon_pool=11종/등급(정본 §1-1 일치), t1_armor_pool=11종(통합풀). 컬럼수 전행 10 정상.

**한계/이월:**
- **dealDamage 우회 데미지엔 미적용**: 좌클릭 평타(BASIC_ATTACK_COEFF, 슬롯1 쿨 중 필러)·일부 직접 `target.damage()` 호출 스킬은 스킬타입 배율 비대상. 기본기 "스킬"(flash_slash 등)은 dealDamage 경유라 정상 적용.
- **방어구 통합풀 테마 혼선**(1단계와 동일): 쿨감이 정본상 하의/신발 전용이나 통합풀이라 머리/상의도 롤 가능 → 3단계 슬롯분리 시 해소.
- **SkillType 키맵 드리프트 리스크**: SkillInputListener slot 테이블 변경 시 SkillType.BY_KEY도 갱신 필요(주석 명시).

---

### DL-129 추가#2 (2026-06-03) — 잠재 풀 3단계: 슬롯별 풀 완전 분리 (item_master 구조변경)

**범위(사장님 "완전 분리" 확정):** 방어구 통합풀(1·2단계 근사)을 정본 §1-2~1-5대로 **머리/상의/하의/신발 4개 독립 풀**로 분리. 슬롯 테마 혼선(머리가 보스피해 롤 등) 완전 해소.

**핵심 안전성 — 저장 데이터가 enum 키:** `equippedItems`는 `Map<EquipmentSlot, instanceId>`(slot_type 문자열 아님) → slot_type 세분에도 **플레이어 세이브 마이그레이션 불필요**. 영향은 마스터 데이터 + 그걸 비교하는 코드에 국한. 또한 `EquipmentService.equip`(slot 검증)은 **생성만 되고 호출되지 않는 휴면 경로**(실제 장착=`equipItem` 직접 호출)라 검증 정밀화에도 회귀 0.

**구현:**
- **`item_master.csv`**: 방어구 7행 slot_type `armor`→`head`(helmet·ragnes_head) / `chest`(chestplate·ragnes_chest) / `legs`(leggings) / `feet`(boots×2).
- **`EquipmentSlot`**: HELMET("head")·CHESTPLATE("chest")·LEGGINGS("legs")·BOOTS("feet"). itemSlotType=item_master slot_type 매칭(장착검증 + resolvePoolId 풀선택). 부수효과: 장착 검증이 부위 정확(머리템→머리칸만) — 개선.
- **CSV 풀 분리**(생성기 `gen_pool.py` 결정론적 재작성, T1+T2 × weapon/head/chest/legs/feet=10풀): 무기 11종 / 머리 7종(+받피감 U+) / 상의 7종(+받피감 U+) / 하의 8종(+받피감 U+) / 신발 9종(받피감 없음). 수치 정본 §2-1~2-5, 가중치 §6 희귀2:보통3. resolvePoolId=`tier_slotType_pool`이 동적 라우팅(head 아이템→t1_head_pool). 하드코딩 구 풀 id 0건 확인.
- **`RuneService.hasEquippedSlotType`**: 룬 slot_filter="armor"(rune_survival_minor) 하위호환 shim — "armor"는 head/chest/legs/feet 어느 하나 장착 시 충족.
- **`WeaponPowerCalculator`**: attack_percent를 **장착 전 슬롯 합산**으로 변경(정본 ATK%는 무기+신발 보유). 기존엔 무기 프로필만 읽어 신발 ATK% 무시되던 것 교정.

**검증:** `./gradlew build` → BUILD SUCCESSFUL(jar 포함). item_master slot_type 분포 head2/chest2/legs1/feet2 정상. 풀별 옵션 종수 무기11·머리7·상의7·하의8·신발9(정본 §1 일치). MasterSeedValidator slot_type 화이트리스트 없음(부팅 안전). SuccessionService 부옵션은 slotType 필터로 **부위별 정확 부옵션** 산출(개선).

**한계/이월:**
- **SuccessionService**: 부옵션이 t1+t2 동일부위 풀 중복 참조(가중치 2배) — 동작엔 무해.
- **악세서리**: t2_ring_carmen(slot_type=accessory)은 t2_accessory_pool 부재 → 잠재 롤 시 전등급 fallback. 1차 시즌 악세 슬롯 미사용(정본 "장갑 슬롯 없음")이라 실경로 아님.
- **API/스냅샷 slot_type**: PlayerGrowthSnapshotBuilder가 slot_type 문자열 그대로 노출 → 웹/봇이 "armor" 기대 시 head/chest 등으로 표기 변경됨(표시용, 기능 영향 없음).
- **gen_pool.py**: 생성기는 미커밋(로컬). 수치 재조정 시 정본 §2 동기 후 재생성.

**잠재 풀 전면 정비 1~3단계 완료.** 정본 potential_options_v1과 구현 100% 정합(슬롯별 풀·옵션·수치·가중치·메커니즘). 남은 건 인게임 검증 + 운영 중 밸런스 튜닝.

---

### DL-129 추가#3 (2026-06-03) — 잠재 큐브 인게임 피드백: 등급 상승 보호 + 장비명 한글화

**인게임 검증 중 사장님 보고 3건 수정.**

1. **등급 상승 후 재굴림으로 등급이 날아가던 버그(정본 §7 "등급 보장" 위반)**: 큐브 버튼은 대기 결과가 있으면 `before`(=상승 이전, 낮은 등급)로 되돌린 뒤 재굴림했음(DL-112 연속재롤). 등급업 시엔 before가 낮은 등급이라 클릭 한 번에 상승이 소실. → `GrowthGuiListener` 큐브 핸들러에서 **대기 결과가 `success()`(등급업)면 재굴림 차단** + "[등급 적용]으로 확정하세요" 안내. `rollPotential` 렌더를 등급업/동급 분기: 등급업이면 양 패널에 상승 결과 표시 + 좌측을 "등급 상승 보호" 안내 패널 + 우측 "[등급 적용] 확인" + **큐브 버튼 잠금**(pending=true). `CUR_KEEP`/`NEW_SELECT` 핸들러도 `success()` 분기 — 등급업은 before 복원하지 않고 새 등급 확정(하락 방지).
2. **등급 확정 채팅이 영문**: `itemDisplayName`이 `ItemMaster.itemName()`(영문 "Training Helmet") 반환. → 잠재 채팅을 **`equipDisplayName(player, item)`**(신규)로 교체 — slot_type 기반 한글명(무기=WeaponGui 한글명 / head=투구·chest=갑옷·legs=각반·feet=장화). 등급 한글은 `gradeKr()`(potentialGradeKr 위임).
3. **(향후) 장비 이름 변경권 연동**: `equipDisplayName`를 장비 표시명 단일 resolve 지점으로 설계 — 이름 변경권 구현 시 이 메서드 최상단에 `item.customName()` 우선 반환만 추가하면 연동(주석 TODO 명시). idea_inbox 항목에 연동 지점 기록.

**검증:** `./gradlew build` SUCCESSFUL, jar 재배포 + 서버 재기동(Done 15.3s, 에러 0건). **한계:** equipDisplayName는 잠재 채팅에만 적용 — 다른 GUI의 itemDisplayName(영문)은 이름 변경권 구현 시 일괄 이관 예정.

---

### DL-129 추가#4 (2026-06-03) — 잠재 큐브 2차 피드백: 확률 정본화 + 천장 + GUI 풀/확률 표시 + 공방 탭

**인게임 검증 중 사장님 보고 처리 (잠재 묶음 + 탭).**

1. **등급업이 너무 잘됨 → 확률 정본화(DL-111 폐기)**: 코드 승급 확률이 정본 §5-2보다 2~4배 높았음(RARE→EPIC 0.10 vs 0.05 / EPIC→UNIQUE 0.04 vs 0.01). → `PotentialService.UPGRADE_CHANCE_BY_GRADE`를 정본값(커먼 0.25 / 레어 0.05 / 에픽 0.01 / 유니크 0.001)으로 교정. 시작→유니크 기댓값 ~124큐브(정본 §5-2 복귀).
2. **큐브 천장(사장님 "기댓값×2") 신규**: `PITY_CEILING_BY_GRADE` = 2/확률 = **커먼 8 / 레어 40 / 에픽 200 / 유니크 2000회**. 카운터(`PlayerEquipmentItem.pityCount`)가 승급 없이 +1, (천장−1) 도달 시 다음 큐브에서 **확정 승급**, 승급(자연/확정) 시 0 리셋. **영속화**: `ItemSaveData`에 `pityCount` 필드 추가(Gson — 구 세이브 누락 시 0) + restore/save 양방향 와이어. 천장은 비운의 꼬리(~10%)만 보호, 평균 거의 불변.
3. **GUI에 승급 확률·천장 진행·부위 풀 표시**: `PotentialService.upgradeChanceFor`/`pityCeilingFor`/`poolOptionCodes` 신규 노출. 잠재 GUI 미리보기 슬롯(POT_SLOT_PREVIEW) lore에 "다음 등급 승급: N%", "천장: cur/ceil", "이 부위 잠재 풀: (옵션 한글 나열)" 추가.
4. **공방 상단 탭 구분(#7)**: `WorkshopGui.buildTabItem` 비활성 탭이 배경과 같은 GRAY 유리라 식별 불가 → **항상 탭 고유 아이콘**(GRASS_BLOCK·BLAST_FURNACE 등) 표시 + 활성은 §a§l 이름·인챈트 글로우 강조.

**검증:** `./gradlew build` SUCCESSFUL. Gson 하위호환(구 세이브 pityCount=0). 서버 재배포·재기동.

**미처리(보고 후 우선순위 대기)** — 별도 서브시스템이라 이번 묶음서 제외:
- ~~**필드 정예 on/off 토글**~~ ✅ 해소 (DL-129 추가#5)
- (이하 잔여)
- **영지 시설현황 미등록**: `EstateState`(시설 설치) ↔ `IslandTerritoryState`(reaper/miner/Count) 미동기화. 공방 슬롯은 IslandRank.workshopQueueMax(작위 기반)로 이미 해금 — "가공기 등록→공방 해금" 설계 의도 재확인 필요.
- **공방 레시피 텍스처**: `WorkshopRecipe.guiIcon`이 Material만(CustomModelData 미지정). 리소스팩에 공방 모델 미발견. "다 정함" 기억 vs 코드 불일치 — 텍스처 매핑 설계 복구 필요.

---

### DL-129 추가#5 (2026-06-03) — 필드 정예 on/off 토글 (/정예)

**배경:** 필드 정예 스폰 인프라는 존재했으나(`FieldSpawnService` eliteMode Predicate·ELITE_MOB 맵·batch/3·정예 태깅) `PoroRPGPlugin`에서 `uuid->false`로 고정돼 토글 수단이 없었음(task.md §7-2 "2차 대기").

**구현:**
- `PlayerData.fieldEliteMode`(세션 메모리, 재접속 시 OFF) + `PlayerDataManager.isFieldElite/toggleFieldElite/setFieldElite`.
- `FieldSpawnService` 생성 predicate를 `playerDataManager::isFieldElite`로 교체 — 정예 ON이면 본인 주변 웨이브가 정예 몹(수 적고 강함).
- `/정예 [on|off]` 명령(`FieldEliteCommand`, 인자 없으면 토글, 권한 없음=전체 플레이어). plugin.yml `poro-field-elite` alias [정예].

**검증:** `./gradlew build` SUCCESSFUL. **한계:** 정예 모드는 세션 메모리(영속 안 함) — 재접속 시 OFF. 다음 웨이브부터 반영(즉시 아님). GUI 토글은 미추가(명령만).

**잠재 풀 정비 전체 + 인게임 피드백(등급보호·한글명·확률/천장/풀표시·탭·정예토글) 완료.** 잔여: 영지 시설현황 동기화(#5), 공방 레시피 텍스처(#6).

---

### DL-129 추가#6 (2026-06-03) — 필드 정예 GUI 토글 + 시설/텍스처 설계 검증

**1. 필드 정예 GUI 토글(#1 추가요청):** 명령(`/정예`) 외 GUI에서도 토글 — `FieldHubGui` 슬롯 22에 정예 ON/OFF 버튼(LIME/GRAY 염료), `FieldHubListener`에 PlayerDataManager 주입 + 클릭 시 toggleFieldElite + 재오픈. `FieldHubGui.open` 시그니처에 eliteOn 추가.

**2. 영지 시설현황→공방 슬롯 설계 검증(#5):** 사장님 지적이 정확 — **코드가 설계 이탈**.
- 정본: `island_system_design.md:27` 작위별 최대 시설 슬롯(개척지 4/기사 6/준남작 7/남작 9/자작 11/백작 13/후작 15/공작 18), §3.1 "물리 블럭 없음, GUI 슬롯 클릭 배정(DB island_facility_slots)", §3.4 **"공방 가공기 1대 = 대기 슬롯 3개"**. 설치 비용 없음(작위 슬롯 배분).
- 코드: `IslandRank.workshopQueueMax()=min(5+tier,12)`(작위 tier 기반, 가공기 수 무시), `IslandTerritoryState`에 workshopMachineCount 필드 부재, 시설 설치 GUI 플로우 없음(reaper/miner count만 존재).
- **필요 구현(다음 작업)**: ① IslandTerritoryState.workshopMachineCount + 영속 ② IslandRank.maxFacilitySlots(작위표) ③ workshopQueueMax=machineCount×3 ④ 시설현황 GUI 빈칸 클릭→시설 선택(약초/광물/공방) 설치, 총 슬롯≤작위한계 ⑤ 스케줄러 시설 카운트 연동.

**3. 공방 레시피 텍스처 설계 검증(#6):** 텍스처는 **추출 완료**(`assets/export/resourcepack/assets/poro/textures/item/`에 essence 3·trace 12·potion 4·feast 4 등 존재). 미비점: ① CustomModelData 번호 할당표 문서 부재 ② item_id↔텍스처 파일명 매핑 규칙 문서 부재 ③ 마도합금 경로 불일치(문서 alloy/ vs 실제 ingot/) ④ 공방 레시피 아이콘 코드가 기본 Material만(CMD 미지정). **필요 작업**: CMD 번호 배정 + 리소스팩 item 모델 JSON(CMD override) 등록 + WorkshopRecipe에 CMD 필드 + 아이콘 빌드 시 적용. 에셋 파이프라인(bb-export-register) + 코드 병행.

---

### DL-129 추가#7 (2026-06-03) — 영지 시설 설치 시스템 (#5) + 공방 텍스처 근본원인(#6)

**#5 시설 설치 시스템 — 설계대로 구현(가공기 수 = 공방 슬롯):**
- `IslandRank.maxFacilitySlots()`: 작위별 시설 슬롯(개척지 4/기사 6/준남작 7/남작 9/자작 11/백작 13/후작 15/공작 18, island_system_design §2.1). `workshopQueueMax()`는 @Deprecated.
- `IslandTerritoryState`: `workshopMachineCount` 필드 + 영속, `FacilityType{HERB,ORE,WORKSHOP}`, `facilitySlotsUsed/Max/Available`, `installFacility/removeFacility`(슬롯 한계 검증, 설치 비용 없음). **`workshopQueueMax()=workshopMachineCount×3`**(§3.4) — 기존 작위 tier 기반에서 전환.
- 영속: `PlayerSaveData.TerritorySaveData`에 workshopMachineCount(8번째 필드, Gson 구세이브 0) + save/load 와이어.
- GUI `TerritoryFacilityGui` 재작성: 슬롯 0~17 = 시설 슬롯(설치 아이콘/빈칸 클릭/잠금), 빈칸 클릭→`openSelect`(약초/광물/공방 3선택), 설치칸 Shift+클릭→철거. info 슬롯에 사용/한계·공방 대기슬롯 표시. `TerritorySettingsGuiListener.handleFacility/handleFacilitySelect` + GuiTitles.TERRITORY_FACILITY_SELECT.
- **주의(밸런스 영향)**: 기존 플레이어는 공방 가공기 미설치 시 workshopQueueMax=0 → 시설현황에서 가공기 설치해야 공방 사용 가능(설계 의도). 작위 기반 자동 슬롯 폐지.
- 검증: `./gradlew build` SUCCESSFUL.

**#6 공방 텍스처 근본원인(미구현, 에셋 파이프라인 작업):** 텍스처·모델 JSON 모두 **이미 존재**(`models/item/material/trace/trace_star.json` 등, parent=item/generated·layer0=poro:item/...). 그러나 **1.21.10 `items/` 정의(item_model 컴포넌트)가 전무**하고 코드도 `setItemModel` 미적용 → 게임 아이템에 텍스처가 연결 안 됨. **필요**: ① `assets/poro/items/<name>.json` 정의 생성(기존 모델 참조) ② WorkshopRecipe에 itemModel 키 + buildRecipeIcon에서 `setItemModel` ③ 리소스팩 zip 재패키징 + server.properties sha1 갱신(틀리면 접속 차단). **리소스팩 sha1 민감 + 메모리 에셋 규칙(bb workflow)상 별도 신중 작업으로 분리** — 무리한 재패키징 보류.

---

### DL-129 추가#8 (2026-06-03) — 공방 레시피 효과 설명 lore + 시설 생산 주기 확인

**1. 레시피 효과·사용법 lore(인게임 피드백):** 공방 레시피 아이콘에 결과물 효과를 표시 — `WorkshopGui.effectLore(resultItemId)` 신규(레시피 정의 무수정, 결과 ID→효과 맵) + buildRecipeIcon에 배선. 정본 workshop_crafting_spec.md §6~10 수치:
- 만찬 4종(공격력+10%/모든피해+8%/치명피해+20%/일반몹+15%, 30분·1종), 치료포션 3종(HP 30/40/50% 회복, 보스전 3/4/5회), 부스트 3종(골드/강화석/경험치 +50%, 30분·3종중첩), 강화흔적 3종(성공률 ×1.15/1.25/1.30, 10강↑ 선택소모·3종동시), 고대흔적 4종(미감정과 함께 레어/에픽/유니크 이상·레전더리 확정), 미감정 흔적(우클릭 개봉, 단독=랜덤/고대와=최소등급보장), 마도합금·정수류 재료 안내.

**2. 시설 생산 주기(#4 질문) — 이미 구현 확인:** `MachineProductionScheduler`가 **20분 주기**로 약초/광물 생산 + `lastProductionAt` 기반 오프라인 누적(Lv1 12h/Lv2 16h/Lv3 24h 캡), plugin:475 기동. "안 된 것처럼 보인" 원인 = #5 전엔 시설 설치 불가(count 0)라 생산 미작동. **추가**: 시설현황 info 패널에 "생산 주기 20분 / 다음 생산까지 ~N분" 표시로 가시화.

**검증(#8):** `./gradlew build` SUCCESSFUL.

---

### DL-129 추가#9 (2026-06-03) — 시설 생산 타이머 타일별 표시 + 신규 시설 windfall 방지

**인게임 피드백: "다음 생산 약 0분으로 뜸 + 각 재배기/채굴기에 시간 표시되게".**
- **타일별 타이머**: info 패널 집계 → **각 약초 재배지/광물 채굴기 타일 lore에 "다음 생산: N분 M초"**(`TerritoryFacilityGui.productionTimer`). 약초/광물 공통 20분 주기(전 시설 동시 생산)라 같은 값.
- **"0분" 버그 수정**: 재시작 직후 `lastProductionAt`이 세이브의 오래된 값이면 `now-last>20분`→0 클램프로 "약 0분" 표시되던 문제. 주기 초과=다음 틱 적립이므로 **"곧 적립(생산 대기 중)"**, lastProductionAt=0(최초)은 "약 20분 후(주기 시작)".
- **windfall 방지**: 시설 설치 시 `lastProductionAt<=0`이면 `now`로 설정. 안 하면 첫 틱에서 `intervals=(now-0)/20분`=거대→캡(12~24h)만큼 한꺼번에 생산 버그(`handleFacilitySelect`).

**운영 노트:** 이번 세션 재기동에서 **백그라운드 작업 중복으로 paper JVM 2~3개 → 포트 충돌→접속불가** 발생. 원인: 재기동 bash 작업 겹침 + 진단 `pgrep -f 'paper.jar nogui'`가 자기 명령줄 자기매칭(`[p]aper` 대괄호로도 실행 중 bash 명령문자열은 못 거름). **확정 교훈: 재기동은 단일 작업, 서버 카운트는 `ps comm==java` 필터, 부팅/종료 판정은 포트 25565 LISTEN 여부로만.**

---

### DL-129 추가#10 (2026-06-03) — 생산 타이머가 전부 "곧 적립"으로 뜨던 문제

**원인:** 생산은 플레이어별 `lastProductionAt`이 아니라 **서버 전역 20분 스케줄러 틱**에 일괄 발생. 그런데 타이머를 `lastProductionAt + 20분`으로 계산 → 세이브의 오래된 lastProductionAt이면 항상 음수 → 전부 "곧 적립". (설치 가드는 `lastProductionAt<=0`일 때만 now로 세팅하므로 이미 stale 값이 있으면 미갱신.)

**수정(임시):** `MachineProductionScheduler.nextProductionAt`(전역 틱) 노출 → GUI 카운트다운. **단, 추가#11에서 전역 틱 모델 자체를 폐기**(익스플로잇).

**검증:** `./gradlew build` SUCCESSFUL.

---

### DL-129 추가#11 (2026-06-03) — 시설별 설치시각 기반 생산 (전역 틱 익스플로잇 차단)

**사장님 지적(익스플로잇):** 전역 20분 틱이면 슬롯을 평소 공방 가공기로 쓰다가 **생산 직전 5초에 재배기/채굴기로 교체→수확→다시 가공기**로 공짜 생산 가능(설치 무료·즉시). → 생산을 **시설별 설치시각 기준 20분**으로 변경해 차단(생산 받으려면 해당 슬롯을 20분 내내 생산기로 유지해야 함 = 공방과 양립 불가). 사장님 원래 요청("각 시설에 시간 표시")과도 일치.

**구현:**
- **`IslandTerritoryState`**: `reaperCount`/`minerCount` int → **`List<Long> herbProducedAt`/`oreProducedAt`**(시설별 마지막 생산시각). 파생 getter(reaperCount=size), `installFacility`가 설치 시 now 기록(설치+20분 첫 생산, windfall 내장 차단), `removeFacility`는 마지막 항목 제거.
- **`MachineProductionScheduler`**: 전역 lastProductionAt 누적 → **시설별 누적**(`accrueProducers`: 각 시설 자기 last+20분마다 cycle 생산, cap 적용). 틱 주기 **20분→1분**(시설별 마크 제때 포착, 누적식이라 안전). 전역 `nextProductionAt` 제거. produceHerbs/Ores 통합.
- **`TerritoryFacilityGui`**: 각 타일이 자기 시설 타임스탬프로 카운트다운(`productionTimer(lastProducedAt)`). 1분 틱이라 "곧 적립"은 최대 1분.
- **영속(`TerritorySaveData`)**: `herbProducedAt`/`oreProducedAt` 리스트 추가(Gson). **구 세이브 마이그레이션**: 리스트 null이면 reaper/minerCount만큼 lastProductionAt(또는 now)으로 시드. save는 리스트 기록.

**검증:** `./gradlew compileJava`·`build` SUCCESSFUL.

---

### DL-129 추가#12 (2026-06-03) — 공방 레시피 텍스처 배선 (#6, 순수 코드)

**조사 결과 #6은 리소스팩 작업 불필요 — 코드만:** `assets/minecraft/items/paper.json`이 이미 `minecraft:select property=custom_model_data`로 공방 결과물 전체(정수 307xxx·약초/합금 302xxx·강화석 303002·흔적 308xxx·물약/만찬 400xxx)를 CMD→poro 모델로 매핑해 둠. **서빙 중 `server/resourcepack.zip`도 동일(122줄)·server.properties sha1 일치(9bdcdbb…)** → 재패키징/접속차단 위험 0. 빠진 건 코드가 CMD를 안 걸던 것뿐.

**구현:**
- **`CustomItemModel`[신규]**: item_id→CMD 매핑(30종) + `applyModel(item, cmd)`(carrier=PAPER + `CustomModelDataComponent.setStrings([cmd])` — 1.21.4+ select는 정수 아닌 strings[0] 매칭, 2D 이펙트와 동일 검증된 방식).
- **`WorkshopGui.buildRecipeIcon`**: 결과물 CMD 있으면 paper+CMD 아이콘, 없으면 기존 Material fallback.

**한계/후속:** 공방 레시피 아이콘만 우선 배선. 창고 GUI·실제 지급 아이템 등 동일 재료가 보이는 다른 GUI는 후속(같은 `CustomItemModel` 재사용 가능). 마도합금 모델은 ingot/mado_iron_ingot(302016) 경로 사용.

**검증:** `./gradlew build` SUCCESSFUL. resourcepack 무변경(sha1 동일).

---

### DL-129 추가#13 (2026-06-03) — HUD HP 정수화·XP 오버플로 + 고대흔적/K·M 에셋 진단

**인게임 피드백 3건.**

1. **HP 소수점 → 바 밀림(수정)**: 잠재 HP% 적용 후 HP가 소수(예 358.4)로 떠 `.` 폭 변동으로 액션바 정렬이 흔들림. `HealthHudFormatter.buildHp`를 `Math.round`로 정수 표기.
2. **XP 고레벨 큰 수 → 바 밀림(수정)**: 100레벨대 expToNext가 6자리+라 수치가 길어 밀림. `need >= 100,000`이면 수치 숨기고 바+Lv만 표시(바가 진행도 전달). **K/M 단축은 HUD 폰트(chars.png)에 K·M 글리프 부재로 미지원** — 폰트 추가(리소스팩) 후속.
3. **고대흔적 텍스처 깨짐(에셋 진단, 미수정)**: `textures/item/material/trace/trace_ancient_*.png`가 **16×128**(일반 흔적은 16×16). `item/generated`에 16×128=8프레임 애니 스트립인데 **.mcmeta 애니메이션 정의 부재** → 통째로 찌그러져 렌더. 모델 JSON·PNG는 정상. **필요**: ① 각 PNG에 `.png.mcmeta`(animation, 8프레임) 추가(움직이는 아이콘 의도면) 또는 ② 16×16 재크롭 + ③ 리소스팩 재패키징·sha1 갱신. 리소스팩 sha1 민감 + 에셋 규칙(bb)상 별도 신중 작업.

**검증:** `./gradlew build` SUCCESSFUL.

**잔여 리소스팩 후속(묶음 권장 — 1회 재패키징):** 고대흔적 .mcmeta(또는 재크롭) + HUD 폰트 K·M 글리프 추가. 둘 다 chars.png/텍스처 수정 + zip 재패키징 + server.properties sha1 갱신 필요. → **DL-129 추가#14에서 처리.**

---

### DL-129 추가#14 (2026-06-03) — 리소스팩 1회 재패키징: 고대흔적 애니 + HUD K·M 글리프

**고대흔적 텍스처는 8프레임 모두 distinct = 의도된 애니메이션 확정.** 두 에셋 이슈를 묶어 1회 재패키징.

1. **고대흔적 애니메이션(.mcmeta)**: `trace_ancient_{faded,glowing,radiant,brilliant}.png.mcmeta` 신규 — `{"animation":{"interpolate":false,"frametime":3}}`. 16×128(8프레임)을 애니메이션으로 정상 렌더(찌그러짐 해소).
2. **HUD 폰트 K·M 글리프**: `chars.png` 150×7(25칸)→**162×7(27칸)**, K(칸25)·M(칸26) 5×7 흰색 글리프 추가(기존 글리프 스타일 일치). `font/hud.json` 5개 chars provider에 코드포인트 2개씩 추가(E219/E21A …). 코드 `HealthHudFormatter.charIdx` K→25·M→26, `buildXp` abbrev(1000=K/1,000,000=M, 소수1자리). XP 자릿수 고정 → 바 밀림 해소.

**재패키징(in-place, 안전):** `zip -u`로 6개 파일만 교체/추가(나머지 바이트 보존). **새 sha1 `7ffcf09768c172613cd58b29128e3289779b836a`** → `server.properties resource-pack-sha1` 갱신(구 9bdcdbb…). 백업 `server/resourcepack.zip.bak.dl129`.

**★재배포 시 필수:** 리소스팩 sha1이 9bdcdbb→**7ffcf097**로 변경됨. server.properties·HTTP 서빙 zip 동기 필수(불일치 시 접속 차단). chars.png/hud.json 폰트 델타·고대흔적 mcmeta는 gitignore(로컬) — 새 환경 재적용 필요.

**검증:** `./gradlew build` SUCCESSFUL. zip in-place 업데이트(chars 346→364·hud 13283→13313·mcmeta×4). server.properties sha1=zip sha1 일치.

---

### DL-129 추가#15 (2026-06-03) — 전승 GUI 장비명 영문→한글

**인게임 피드백:** 전승 GUI 대상 표시가 영문(itemDisplayName=item_master 영문명, 예 "Training Sword"). 장비 선택칸은 한글(equipDisplayName)인데 **대상 슬롯·전승 버튼·완료 메시지** 3곳만 영문.

**수정:** `GrowthGuiListener` 전승 영역 `itemDisplayName(...)` 3곳 → `equipDisplayName(findEquipSlot(state,targetId), playerDataManager.getWeaponType(uid))`(전승 selector와 동일 한글 변환). 무기=한글 무기명·방어구=부위명(투구/흉갑/레깅스/부츠).

**검증:** `./gradlew build` SUCCESSFUL. **잔여:** 잠재 미리보기 슬롯 타이틀·강화 메시지 등 다른 GUI itemDisplayName(영문)은 이름변경권 구현 시 일괄 이관(equipDisplayName 재사용).

---

### DL-129 추가#16 (2026-06-03) — 보스정보 GUI 실스탯 동기(B) + 보스별 패턴(C) + HP 곡선(A 제안)

**배경:** 보스정보 GUI(BossHubGui)가 HP/ATK를 하드코딩(설계값)으로 표시 — 실제 보스 스탯(MobStatOverrideService 시드)과 분리. 패턴은 "3페이즈 60%·30%" 고정. 실제 보스 HP는 미시드(테스트 ~8000).

**B — GUI 실스탯 동기:** `MobStatOverrideService.seededHp/seededAtk/seededDef` 정적 접근자 노출. `BossHubGui`가 시드값 우선 표시(없으면 BossDef 설계값 fallback) + **방어(DEF) 행 표시**(기존 "—").

**C — 보스별 패턴 표시:** `BossHubGui.bossPattern(id)` — season_boss_patterns.md 기반 보스별 요약(페이즈 전환%·고유 SP 패턴명). 9보스 각각(예 균열왕 "5페이즈(75/50/25/10%)·무적딜타임×4·분신/격자폭발").

**A — 보스 HP 곡선(제안·적용):** `MobStatOverrideService.HP_SEED` 신규 + loadAndSeed에 HP 시드/적용 추가(applyOnSpawn이 MAX_HEALTH 적용, spigot 캡 2048000). balance_review_dl128 §2 "시즌보스 평탄 150~170k → 권장강화 비례 곡선(역전 해소)" 반영. **제안값(검토·조정 대상)**: 시즌1 fallen_knight 10만 / 시즌2 16만 / 시즌3 23만 / 시즌4 31만 / 시즌5 40만 / 시즌6 50만 / 최종 균열왕 85만·이중체 55만·주시자 85만. DEF 경감(×(DEF+200)/200)이 실효 HP 추가 가산. **사장님 검토 후 숫자 조정 — HP_SEED 맵 1곳 수정.**

**검증:** `./gradlew build` SUCCESSFUL. 부팅 시 mob_stat_override 시드 갱신(HP 포함). **잔여:** 필드보스 HP, 보스 ATK 곡선 미세조정, 패턴 데미지(MM YAML)는 본 레이어 밖.

**HP 곡선 1차 조정(사장님 피드백):** raw 10만 너무 높음 → DEF 경감 역산 반영. raw HP 하향: fallen_knight 30k(실효~45k) / corrupted_lord 45k / stone_colossus 70k / storm_sorcerer 100k / abyss_guardian 130k / void_herald 180k / rift_king 420k / corrupted_dyad 300k / spirit_watcher 420k. (실효 HP = raw × (DEF+200)/200.)

---

### DL-129 추가#17 (2026-06-03) — 보스 HP 실입장 인원수 스케일링

**사장님 요청:** HP를 등록 파티가 아니라 **실제 입장(온라인) 유저수** 기준으로 곱. 3인 파티라도 2명만 들어오면 2인 HP.

**핵심:** `BossRoomListener`가 memberIds 구성 시 **온라인 파티원만**(`Bukkit.getPlayer != null`) 포함(125-133행) → `memberIds.size()`가 곧 실입장 인원. 별도 추적 불필요(데스풀도 이 값 사용).

**구현:**
- `partyHpMultiplier(entered)`: 1인 ×1.0 / 2인 ×1.8 / 3인+ ×2.5.
- 스폰 후 5틱 지연(MobStatOverride applyOnSpawn 1틱 이후) → 보스 MAX_HEALTH × 배수 + setHealth. `BossRoomListener`에 Plugin 주입(스케줄러).
- `BossHubGui` 체력 표기에 "(1인 기준 · 2인 ×1.8 · 3인 ×2.5)" 추가.

**검증:** `./gradlew build` SUCCESSFUL. **참고:** 입장 후 합류/이탈은 미반영(스폰 시점 고정). 데스카운트(3/4/5)와 동일 기준(memberIds.size).

---

### DL-129 추가#18 (2026-06-03) — 보스정보 요약 아이콘 + 클릭 시 상세(페이즈·패턴·데미지)

**사장님 요청:** 보스정보 아이콘은 간단히, 클릭하면 페이즈별 패턴·패턴 데미지 상세.

**구현:**
- **요약 아이콘**(BossHubGui 그리드): 체력(1인 기준)·권장·파티만 + "클릭 — 상세". 기존 공격력/방어/패턴 라인 제거.
- **상세 GUI**(`openBossDetail`, GuiTitles.BOSS_DETAIL, 27슬롯): slot11 스탯(HP+인원배수·ATK·DEF+경감식·권장), slot15 페이즈·패턴 상세(`bossDetailLore`), slot22 뒤로.
- **`bossDetailLore(id)`**: 9보스 각각 페이즈 전환%·패턴·데미지.
- `BossHubListener.handleBossInfo`: 보스 슬롯 클릭→openBossDetail, BOSS_DETAIL 뒤로(slot22)→openBossInfo.

**★정본 정정(사장님 지적):** 처음엔 `season_boss_patterns.md`(SP-11 에너지 석판·SP-12 유도 구체 등)로 채웠으나 **이는 옛 초안이고 실제 인게임과 다름**. **정본 = MythicMobs YAML**(`season_bosses.yml`·`boss_patterns.yml`). 실제 패턴은 공용 P-00~P-13(기본×0.6/전방강타×1.2/직선돌진×1.5/원형폭발×1.3/부채꼴×1.1/투사체×1.0/산탄/소환/연속타격×0.6/낙하충격×1.6) + P-08 전환 + P-09 무적(SP 해제 메카닉 미구현→자동해제). `bossDetailLore`를 YAML 기준으로 전면 교체(데미지 = mdamage 배율 = 보스ATK×N). `season_boss_patterns.md` 헤더에 "DRAFT·일부 미구현, 정본=YAML" 명시.

**검증:** `./gradlew build` SUCCESSFUL. (bossPattern 요약 메서드는 미사용이나 잔존 — 경고만.)

---

### DL-129 추가#19 (2026-06-03) — 오염된 군주 히트박스(엘더가디언→RAVAGER) + 보스 종료 영지 텔레포트 확인

**① 히트박스(사장님: 스킬이 안 맞음):** corrupted_lord Type = **ELDER_GUARDIAN** = 원거리 빔·수중 부유 몹 → 플레이어와 거리 벌리고 떠다녀 근접/단거리 스킬(반경 ~3-4) 빗나감. 다른 보스(EVOKER·IRON_GOLEM·WARDEN=근접 지상)는 정상. → **Type ELDER_GUARDIAN → RAVAGER**(대형 근접 지상, 큰 히트박스). `season_bosses.yml` 소스+런타임 둘 다 교체, `mm reload`(118 스킬 무에러). 보스 재스폰 시 적용.

**② 보스룸→영지(사장님: 보스룸 있다가 영지 감):** `BossRewardService.onRunEnded`가 **클리어 시 10초 후·실패 시 즉시 전원 `is home`(영지 귀환)** — **의도된 동작**(채팅 "영지로 귀환합니다"). 예상치 못한 케이스는 ①과 연결: 엘더가디언이 지상 질식/원거리 이탈로 스스로/조기 종료 시 영지로 보내짐. RAVAGER 교체로 정상 전투 → 정상 종료 흐름 복원. (귀환지를 수도/hub로 바꾸려면 BossRewardService tp 대상 변경 — 후속 옵션.)

**검증:** mm reload OK. server-config·런타임 YAML 동기. 인게임 재입장 확인 대기.

---

### DL-129 추가#20 (2026-06-03) — 보스룸 이동 시 "보스 포기" 확인 게이트

**사장님:** 보스 안 깼는데 메뉴에서 영지/필드 이동이 그냥 됨 → 이동 시 "포기하겠습니까? [예][아니요]" 떠야 함. 예=파티 탈퇴+이동, 아니요=계속 전투.

**구현:**
- `BossAbandonGui`(27슬롯, 예 slot11/아니요 slot15) + GuiTitles.BOSS_ABANDON.
- `BossAbandonListener`: `promptIfInRoom(player, destCode)` — `bossRoomManager.isInBossRoom`이면 목적지 저장+확인 GUI 띄우고 true(이동 보류), 아니면 false. 예→`partyManager.leaveParty` + `bossRoomManager.exitRoom` + 야간투시 해제 + 목적지 이동("home"/"visit:<owner>"/"field:<id>"). 아니요→닫기.
- 가드 배선: `MainHubListener.handleTerritoryMove`(내 영지 is home·공개 영지 is visit) + `FieldHubListener`(필드 이동) — 이동 직전 promptIfInRoom 호출, true면 return. 세터 주입(setBossAbandonListener), PoroRPGPlugin에서 생성·등록.
- 보스 클리어/실패 시 영지 귀환(BossRewardService)은 의도된 동작 — 변경 없음.
- **재입장 버그 수정:** 포기 후 재입장 시 INVALID_ARGUMENT("이미 활성 런에 있음") — `exitRoom`이 슬롯만 정리하고 `BossRunService.activeRunByParticipant`(검증 체커가 보는 맵)는 안 지웠던 것. **`BossRunService.leaveRun(userId)`** 신규(참가자 추적 제거, 전원 떠나면 activeRuns 레코드 제거, 텔레포트/보상 없음) → BossAbandonListener에서 호출. 슬롯은 exitRoom, 보스 엔티티 despawn은 후속 부채.

**검증:** `./gradlew build` SUCCESSFUL. (보스 엔티티 despawn·HP 재조정은 추가#21에서 구현)

---

### DL-129 추가#21 (2026-06-03) — 보스 포기 시 솔로 즉시 despawn · 파티 HP 인원 비례 재조정 · 이탈자 보상 제외

**사장님:** 솔로일 땐 나가면 즉시 despawn하고 방 바로 비워라. 2~3인일 땐 한 명이라도 남으면 계속 진행하게 하되 HP를 즉시 남은 인원수에 맞춰라 — 단 이때까지 입힌 피해량은 남기고, HP만 회복시키는 게 아니라 아예 비율에 맞춰 깎아라. 나간 사람은 남은 사람이 깨도 보상 들어오면 안 됨.

**구현:**
- **이탈 추적 모델:** `BossRun.abandoned`(LinkedHashSet) + `markAbandoned`/`isAbandoned`/`activeCount`(미이탈 참가자 수). 종료 판정·HP 스케일·보상의 단일 기준.
- **`BossRunService.leaveRun`** 반환형을 `LeaveResult(runId, empty, oldActiveCount, newActiveCount)`로 변경. 호출 시 `run.markAbandoned` 후 newActiveCount≤0이면 activeRuns 제거(empty=true).
- **`BossAbandonListener`** — `BossDamageTracker` 주입. 예 처리 시 `damageTracker.mobForRun(runId)`로 보스 엔티티 조회:
  - `empty`(솔로/전원 포기) → `boss.remove()` 즉시 despawn(슬롯은 exitRoom이 해제).
  - 일부 잔류 → `ratio = partyHpMultiplier(newActiveCount)/partyHpMultiplier(oldActiveCount)`로 MAX_HEALTH base와 현재 체력을 **동시에 축소**(입힌 피해 보존, ratio<1일 때만). `BossRoomListener.partyHpMultiplier` public static화하여 입장 스케일과 동일 곡선(1인×1/2인×1.8/3인+×2.5) 재사용.
- **이탈자 보상 제외:** `BossResultSummaryBuilder.fromRun`이 `run.isAbandoned(uid)` 참가자를 participantSummary에서 skip → 남은 사람이 클리어해도 이탈자는 보상·종료 텔레포트 대상 아님.
- 추가#20에서 "보스 엔티티 despawn은 후속 부채"로 남겼던 부분 해소.

**검증:** `./gradlew build` SUCCESSFUL(31 tests; GrowthEngineSampleTest 큐브 roll을 새 RARE→EPIC 확률 0.05 경계 안 0.01로 갱신). 서버 재기동 정상(Done 19.6s, PoroRPG·boss-engine 예외 없음).

---

### DL-129 추가#22 (2026-06-03) — 보스 잔존 버그 수정(슬롯 해제 = 단일 despawn 권한) + 근접 스킬 히트박스 인지

**사장님:** ① 리타이어/타임아웃/영지이동 등 보스룸을 빠져나갈 때 보스가 즉시 despawn 안 되고 방만 비워져서, 새 입장자에게 같은 슬롯이 배정되면 새 보스 + 잔존 보스가 겹친다. ② 낫 스킬이 타락기사장엔 스택이 잘 차는데 오염된 군주엔 안 찬다 — 히트 판정 확인.

**원인 ①:** `BossRoomManager.releaseByRunId`/`exitRoom`/`releaseSlot`가 슬롯만 비우고 보스 엔티티는 월드에 방치. despawn은 타임아웃 스케줄러와 abandon 경로에만 산재 → 전멸(리타이어)·일부 경로에서 보스 잔존.
**수정 ①:** `BossRoomManager.slotToBossMob`(slotId→보스 UUID) 추가, `registerBossMob`(스폰 직후 BossRoomListener에서 등록). **`releaseSlot`이 단일 despawn 권한** — 클리어/전멸/타임아웃/포기/스폰실패 모든 경로가 releaseSlot으로 수렴하므로 여기서 `Bukkit.getEntity(mob).remove()`. 클리어 시엔 보스가 이미 사망(getEntity=null)이라 무해. abandon 경로의 명시적 despawn은 제거(exitRoom→releaseSlot이 처리), HP 비율 재조정만 유지.

**원인 ②:** `SkillHitboxHelper.arc/line/burst`가 타겟을 **발밑 중심점 한 점(`e.getLocation()`)**으로만 판정. WITHER_SKELETON(타락기사장, 폭 0.7)은 OK지만 RAVAGER(오염된 군주, 폭 1.95)·WARDEN·IRON_GOLEM 등 대형 보스는 몸통이 플레이어를 밀어내 중심이 사거리·부채꼴 축 밖으로 → 누락.
**수정 ②:** **히트박스 인지(hitbox-aware)** — `horizontalRadius(e)=max(BoundingBox 폭X,폭Z)/2`로 ⓐ 1차 탐색 반경 +2.5 여유, ⓑ arc 사거리=`dist−er≤radius`·부채꼴 허용각 `+atan2(er,dist)`, ⓒ line 폭·길이 `±er` 확장, ⓓ burst 반경 가장자리 기준. 모든 대형 보스에 일괄 적용.

**검증:** `./gradlew build` SUCCESSFUL(31 tests). 서버 재기동 정상(Done 17.4s, 예외 없음). 인게임: ① 전멸/포기/타임아웃 시 보스 즉시 소멸·슬롯 재사용 정상, ② 오염된 군주(RAVAGER) 낫 스킬 명중·스택 충전 — 확인 필요.

---

### DL-129 추가#23 (2026-06-03) — 슬롯 해제 시 방 전체 청소(소환수 포함) + 바닐라 디버프 차단

**사장님:** ① 보스 본체만 없어지는 듯 — 소환수까지 그 방에 있는 모든 애를 디스폰시켜라. ② 바닐라 위더 효과·채굴 피로 같은 디버프 빼자.

**수정 ① (방 전체 청소):** 추가#22는 추적된 보스 UUID 1개만 제거 → 소환수(add)·잔류 투사체·장판이 남았음. `BossRoomSlot.roomBox()`(방 영역 BoundingBox — 슬롯 X간격 60이라 X±28로 이웃 침범 방지, z는 player/boss 스폰 감싸도록 ±35) 추가. `releaseSlot`이 방 박스 안의 **플레이어 아닌 LivingEntity + Projectile + AreaEffectCloud**를 일괄 `remove()`. 홀로그램/데미지숫자(TextDisplay·ArmorStand·Display)는 보존. 보스 add(소환수)도 LivingEntity라 함께 정리.
- 보스룸 설정: world 10024.5~ X 60 간격 일렬, player-spawn z=10045 / boss-spawn z=10010.

**수정 ② (바닐라 디버프 차단):** `VanillaDebuffBlockListener` 신규 — `EntityPotionEffectEvent`(ADDED/CHANGED)에서 플레이어 대상이고 효과가 블랙리스트면 취소. 차단 목록 = **WITHER(위더 스켈레톤), MINING_FATIGUE(가디언/엘더가디언 채굴피로), DARKNESS(워든 어둠 — 보스전 시야방해)**. 플레이어가 직접 마실 일 없는 효과라 출처 구분 없이 전역 차단해도 안전. `Set<PotionEffectType> BLOCKED` 상수라 확장 용이.

**검증:** `./gradlew build` SUCCESSFUL(31 tests). 서버 재기동 정상(Done 21.9s, 예외 없음). 인게임 확인 필요: 방 청소(소환수 포함)·위더/채굴피로/어둠 미적용.

---

### DL-129 추가#24 (2026-06-03) — 직선 돌진 텔레그래프 방향 잠금 + 디버프 차단 확장(BLINDNESS) + 낫 스택 진단

**사장님:** ① 돌진 패턴에서 바닥 예고선과 실제 돌진 방향이 다르다(예고선 따로, 돌진은 플레이어 추적). 0.5s 추종 후 고정하고 그 방향으로 돌진하거나, 처음부터 고정해라. ② 낫 스택 여전히 안 참.

**수정 ① (돌진 방향 잠금, MM `P_02_LinearDash`):** 기존 `stun{duration=20}`은 이동만 막고 **회전은 허용** → windup 내내 보스가 플레이어를 따라 돌다가 마지막 텔레그래프(t=15)와 lunge(t=22) 사이 7틱 더 회전 → 예고선≠돌진 방향. 재설계:
- 1) 추종 0.6s: `stun{duration=12}` + `P_02_DashWarn`(불꽃선) 반복 — 이동만 막고 회전 허용해 플레이어 추적.
- 2) 고정 0.6s: `setAI{ai=false}`로 **회전까지 동결**(돌진 방향 확정) + `P_02_DashLock`(영혼불꽃 고정 강조선) 반복.
- 3) `setAI{ai=true}` 직후 **같은 틱** `lunge @Forward{f=11}` — velocity가 잠긴 yaw 기준 월드벡터로 캡처되어 이후 재회전과 무관하게 예고선 방향 직진. 무료 MM엔 yaw 고정 전용 메커닉이 없어 setAI 토글이 유일 수단.
- 필드몹도 동일 스킬 공유 → 일괄 개선. server-config tracked 사본이 라이브보다 크게 드리프트(boss_patterns 221줄·season_bosses 304줄)되어 있어 **tracked ← live 전체 동기화**로 해소(드리프트엔 #21·#22 RAVAGER 전환·HP값 등 이미 라이브 반영분 포함).

**수정 (디버프 확장):** corrupted_lord가 MM `potion{MINING_FATIGUE}`를 직접 걸던 라인(season_bosses) 제거. 추가로 일부 보스 <25% 패턴의 `BLINDNESS`(@PlayersInRadius) 발견 → `VanillaDebuffBlockListener.BLOCKED`에 BLINDNESS 추가(이제 WITHER·MINING_FATIGUE·DARKNESS·BLINDNESS). 리스너가 출처 무관 중앙 차단이므로 MM 소스 누락분도 안전망으로 흡수.

**진단 ② (낫 스택):** 히트박스 수정(#22) 후에도 미충전 보고. 정적 분석상 death_slash `arc(3.0,150)`가 RAVAGER를 잡아야 정상 → 원인 미상. `ScytheDeathSlashSkill`에 **임시 디버그**(`타겟=N 스택획득=bool 현재스택=N` 채팅 출력) 삽입해 실측 진단. 콤보=월영회전(RMB)→2s내 사신베기(LMB) 명중. 원인 확인 후 디버그 제거 예정.

**검증:** `./gradlew build` SUCCESSFUL(31 tests). MM `bosses=14` 정상 로드(P_02 신스킬 오류 없음). 서버 재기동 정상(Done 17.2s). 인게임 확인: 돌진 예고선=돌진 방향 일치, 디버그 메시지로 스택 원인 파악.

**후속(돌진·디버프 확인):** 사장님 인게임 — "디버프 안 걸리고 돌진 잘된다" 확인. setAI 잠금 돌진 정상 작동.

---

### DL-129 추가#25 (2026-06-03) — 낫 스택 LC 직접 적립 + 보스룸 파티 위임(해체 방지)

**사장님:** ① 낫 스택이 RC(월영회전)→LC(사신베기) 콤보로만 차서 불편하다. "1RC=2LC"로 LC를 늘리거나 LC에 스택이 차게 해라. ② 보스룸에서 파티가 해체된다 — 파티 해체도 포기 GUI 띄우고 영지로 보내라. 파티장이면 남은 파티원에게 위임, 파티원이면 탈퇴 후 GUI+영지.

**수정 ① (낫 스택):** `ScytheDeathSlashSkill` — 월영회전 2초 윈도우 게이트(`consumeShadowSpinWindow`) 제거, **사신베기(LC) 명중 시마다 1스택 직접 적립**(max 3, 유지각인 시 6). 월영회전은 순수 이동기로 단순화(`recordShadowSpin` 호출 제거 — 죽은 코드). 사장님이 준 두 옵션 중 "LC에 스택" 채택(가장 직관적). 콤보 의존 제거로 스택 램프 자연스러워짐.

**수정 ② (파티 위임):** `PartyManager.leaveParty`는 리더 호출 시 **무조건 파티 해체**라, BossAbandonListener가 포기 시 leaveParty(리더) 호출 → 남은 파티원 파티 소멸이 버그였음.
- **`PartyManager.leaveOrDelegate(uid)`** 신규 → `PartyLeaveOutcome(type, newLeaderId)`. 멤버=탈퇴 / 리더+멤버잔류=첫 멤버에게 위임(leaderId final이라 새 Party로 rekey, members·ready·started 이관) / 리더혼자=해체.
- `BossAbandonListener` 포기 YES → leaveParty 대신 `leaveOrDelegate`, 위임 시 새 파티장에게 알림.
- `BossHubListener` 파티 GUI 탈퇴/해산 버튼(slot 39) → 보스룸이면 `bossAbandonListener.promptIfInRoom(player,"home")`로 포기 게이트 경유(리더 위임·멤버 탈퇴 + 영지 귀환). 비보스룸(로비)은 기존 해체 유지. setter 주입(PoroRPGPlugin 와이어링).

**검증:** `./gradlew build` SUCCESSFUL(31 tests). 서버 재기동 정상(Done 19.6s, 예외 없음). 인게임 확인 필요: ① 사신베기 LC 명중마다 스택 증가, ② 보스룸에서 리더 포기 시 파티 유지·위임 / 멤버 포기 시 탈퇴, 양쪽 영지 귀환.

**후속(좌클 스택 확인):** 사장님 "좌클 스택 잘 오른다" 확인.

---

### DL-129 추가#26 (2026-06-03) — 월영회전 돌진 경로 전체 타격 + 돌진 중 무적

**사장님:** 월영회전 돌진이 시작 구간에만 데미지가 들어간다. 돌진 경로의 모든 적을 때려야 한다. 그리고 돌진 중엔 잠시 무적이 되어 경로에서 맞아 죽지 않게(생존기 겸용). (그믐참 스택 미소비도 재확인 — 이미 미소비 상태였음. consumeStacks는 사신처형만 호출.)

**원인:** 기존 `dashForward` 후 같은 틱에 `burst` 4회 → velocity는 다음 틱부터 적용되어 4회 모두 **출발 지점** 타격. "시작 구간에만 데미지" 현상.

**수정:** `ScytheShadowSpinSkill` 재구현 — `BukkitRunnable`로 8틱(0.4s) 동안 매 틱 전방 추진(tick당 0.7블록 ≈ 5.6블록)하며 그 위치에서 `burst(2.5)`로 훑고 `Set<UUID>`로 적별 1회 타격(경로 전체 커버). 시작 시 `setInvulnerable(true)`, 종료(틱 만료·오프라인·사망) 모든 분기에서 `setInvulnerable(false)` 복구. 쿨다운 5s > 돌진 0.4s라 중첩 없음. per-hit 계수 0.60 유지(유틸/이동기 — 데미지보다 커버리지·생존이 핵심).

**검증:** `./gradlew build` SUCCESSFUL(31 tests). 서버 재기동 정상(Done 16.8s). 인게임 확인 필요: 돌진 경로 적 전원 타격·돌진 중 무적(보스 패턴 피격 무효).

**후속:** 사장님 "경로상 다 때리고 무적도 된다" 확인.

---

### DL-129 추가#27 (2026-06-03) — 돌진 스킬 전체 경로타격+무적 통일 + 낫 F 회전율 개선

**사장님:** ① 월영회전식 경로타격+무적을 돌진 있는 스킬 전부에 적용. ② 낫 LC 스택은 빨리 쌓이는데 F 쿨이 길어 3스택 차도 바로 F를 못 써 답답 — F 쿨 줄이고 계수 낮추는 쪽 검토.

**수정 ① (돌진 통일):** `PluginWeaponSkill`에 공용 헬퍼 2종 추가 —
- `dashStrike(player, dir, ticks, speed, hitRadius, invuln, trail, onHit, onComplete)`: 매 틱 전방 추진하며 `burst`로 경로 적 1회씩 타격, 무적·잔상 처리, 종료 시 명중수 콜백. 모든 종료 분기에서 `setInvulnerable(false)` 복구.
- `invulnerableFor(player, ticks)`: 경로타격 없이 무적만(후방 회피기용).
- 적용: **전방 공격 돌진** = 월영회전(8t)·돌파창(9t,5.4블록)·섬광베기(5t,스택 onComplete)·파쇄돌진(6t) → 모두 경로타격+무적+잔상. 창/검/도끼는 `BaseWeaponSkill`→`PluginWeaponSkill` 전환(생성자에 plugin, PoroRPGPlugin 등록 `new …Skill(this)`).
- **후방 회피** = 석궁 회피사격 → 무적(6t,0.3s)만(뒤로 빠지는 회피라 경로타격 부적합).

**수정 ② (낫 F 튜닝):** `ScytheExecutionSkill` 쿨 16000→10000ms, 계수 2.80→1.80(처형 hp<30% 4.80→3.10). DPS 거의 중립(0.175→0.18/s)이면서 회전율↑ — LC 3스택 적립(~10s)과 F 쿨이 맞아 "차면 바로 사용" 가능. 처형 보너스 비율(≈1.72×) 보존.

**리스크/메모:** 섬광베기는 쿨 3s라 0.25s 무적이 잦음(스킬 기반 i-frame). 의도된 회피 표현이나 밸런스 모니터링 필요. 그믐참 스택 미소비는 기존 유지(변경 없음). 다른 무기 F 쿨/계수는 이번 범위 외(낫만 조정).

**검증:** `./gradlew build` SUCCESSFUL(31 tests). 서버 재기동 정상(Done 19.6s, 예외 없음). 인게임 확인 필요: 4종 전방 돌진 경로타격+무적, 석궁 회피 무적, 낫 F 회전율.

---

### DL-129 추가#28 (2026-06-03) — 전 무기 F 회전율 정렬 (스택 적립 속도 대비)

**사장님:** 낫뿐 아니라 다른 무기도 스택 쌓는 속도 대비 F 쿨이 너무 길지 않은지 검토.

**검토:** 전 무기 공통 구조 = LC(slot1) 명중마다 1스택 적립(max 3), F(slot4)가 스택 소비. LC 쿨로 3스택 적립 시간 계산 → F 쿨과 비교:
| 무기 | LC쿨 | 3스택≈ | 기존 F쿨 | 기존비율 | → 신 F쿨 | 계수배율 |
|---|---|---|---|---|---|---|
| 낫 | 3s | 9s | 10s(#27) | 1.1× | 10s | — |
| 검 | 3s | 9s | 16s | 1.8× | **10s** | ×0.625 (3.32/0.06→2.08/0.04) |
| 창 | 3s | 9s | 15s | 1.7× | **10s** | ×0.667 (2.96/0.04→1.97/0.03) |
| 석궁 | 3s | 9s | 14s | 1.6× | **10s** | ×0.714 (3.35/0.05→2.39/0.04) |
| 도끼 | 4s | 12s | 18s | 1.5× | **13s** | ×0.722 (4.55/0.10→3.29/0.07) |
| 지팡이 | 3s | 9s | 20s | 2.2× | **11s** | ×0.55 (3.89/0.05→2.14/0.03) |

**수정:** 각 F 쿨다운을 3스택 적립 시간의 ~1.1배로 정렬(도끼·지팡이는 LC속도/원거리 프리미엄 반영). 계수는 `신쿨/구쿨` 비율로 base·perStack 동반 하향(fullChargeMult 1.20·처형 임계비율 보존) → **DPS 거의 중립**, 풀스택 후 대기시간만 제거(답답함 해소). 모든 F가 scaledDamageFullChargeSpike(도끼만 scaledDamageWithStacks) 사용.

**리스크:** 지팡이 20→11s는 상대 절감폭 최대(원거리 폭딜 빈도↑). 검 F 10s + LC 섬광베기 무적(#27)으로 검 생존/딜 동시 상향 — 종합 모니터링 필요. CANON(04_combat) 일괄 반영은 안정화 후 권장.

**검증:** `./gradlew build` SUCCESSFUL(31 tests). 서버 재기동 정상(Done 19.7s, 예외 없음). 인게임 확인 필요: 무기별 3스택 시점에 F 사용 가능(대기 최소화), 체감 DPS 유지.

---

### DL-129 추가#29 (2026-06-03) — 영지 창고 통합 (생산·드랍 커스텀 재료 표시·입출금)

**사장님:** ① 자동재배기·광물생산기 생산물이 시간 지나도 창고에 안 들어옴. ② 창고 우클릭이 다른 물건을 입금.

**진단:** 이중 저장소 불일치 — 생산·필드/보스 드랍은 `IslandTerritoryState.customItems`(item_id 키)에 적립되나, 창고 GUI(`StorageGui`)는 `IslandStorage`(Material 키)만, 그것도 `materialList()`가 유효 Material만 통과시켜 커스텀 ID를 숨김. CANON §3.4("자동 창고 입금")와 불일치.

**수정 (창고 GUI 통합):**
- `StorageGui`: `Entry(customId | material, qty)` 통합 항목 모델. `entries(territory, storage)` = 커스텀 재료(생산·드랍) 먼저 + 바닐라 Material. 커스텀은 `customSlot`(PAPER+CMD+한글명)으로 렌더. open/render 시그니처에 `IslandTerritoryState` 추가.
- `CustomItemModel`: `buildStack(id, n)`(PAPER+CMD+displayName + 재입금용 `STORAGE_ID_KEY` PDC), `readStorageId`. `WorkshopRecipeRegistry.displayName`에 mat_herb_imperial/mat_essence_imperial 추가.
- `StorageGuiListener`: `IslandTerritoryStateStore` 주입. 출금=entries 인덱스로 커스텀(`withdrawCustomItem`→buildStack 64분할 지급)/바닐라 분기. 입금=① PDC id 커스텀(우클릭=같은 id 전부, **이슈② 해소**) ② 무기·메뉴·기타 CMD 커스텀 차단(Material 쓸어담기 방지) ③ 바닐라. depositAll도 동일 분기.
- 호출처 3곳(PlayerCommandRouter·MainHubListener·TerritoryStatusGuiListener) + 리스너 생성(PoroRPGPlugin) 시그니처 갱신.

**검증:** `./gradlew build` SUCCESSFUL(31 tests). 서버 재기동 정상(Done 16.9s, 예외 없음). 인게임 확인 필요: 생산물이 창고에 표시·출금, 커스텀 재료 좌(1)/우(같은 id 전부) 입금 정상.

**남은 작업:** §6 기본 광물 생성기(물-울타리)는 미구현 확인 — 트리거 메커닉 확정 후 별도 구현 (BlockFormEvent는 물+울타리만으로 발생 안 함, 구성 확인 필요). → 추가#30에서 구현.

---

### DL-129 추가#30 (2026-06-03) — 기본 광물 생성기 구현 (물-울타리, §6)

**사장님:** 물-울타리 구성으로, 재생성 쿨다운은 5초가 아니라 0.n초로 빠르게 "딱딱 캐지게".

**결정/제약:** 스펙(§6)의 `BlockFormEvent`는 물+울타리만으로는 미발생(용암 필요) → **`BlockFromToEvent`(물 흐름)로 변경**. 위치→소유자 API 부재(IslandProtectionListener 주석: 액체 흐름은 플레이어 정보 없음) → 작위는 **근처(반경 12) 플레이어 기준**(생성기는 곁에서 캐는 능동 사용 전제).

**구현:** `WaterFenceOreListener` 신규 —
- IridiumSkyblock 월드에서 물(WATER) 흐름의 도착 블록이 울타리(`Tag.FENCES`)에 6면 인접 시 트리거.
- 위치별 **0.4s 쿨다운**(흐르는 물이 계속 닿을 때마다 빠른 반복 생성).
- 근처 플레이어 영지 작위 tier(0 개척지~7 공작령)로 §6 확률표 8열 선택 → 누적 롤로 광물 1개 드랍(조약돌/구리·철·금 원석/레드스톤/청금석/다이아/에메랄드).
- 자작령+(tier≥4) 다이아/에메랄드 생성 시 보너스 1종(석탄·금·철·레드스톤·청금석) 추가. 행운 레벨은 소스 미정 → 현재 "없음=1개" 고정.
- 확률표 각 열 합 100 검증(개척지·공작령 확인).

**리스크/메모:** ① 작위=근처 플레이어 기준이라 방문자가 곁에 있으면 그 작위 적용(소유자 위치 API 도입 시 교체). ② 연속 생성은 "흐르는 물"이 울타리에 계속 닿는 구성 필요(고인 물은 이벤트 미발생). ③ 행운 레벨 소스 확정 시 보너스 수량 반영 필요.

**검증:** `./gradlew build` SUCCESSFUL(31 tests). 서버 재기동 정상(Done 17.7s, 예외 없음). 인게임 확인 필요: 영지에서 물-울타리 구성 시 광물 드랍·작위별 확률·0.4s 반복.

---

### DL-129 추가#31 (2026-06-03) — 창고 커스텀 한글명·아이콘 폴백 + 광물 생성기 블럭 방식·채굴 자동입금

**사장님:** ① 창고 커스텀 물자가 영어로 나옴. ② equip_trace_faded 텍스쳐 깨짐, cosmetic_fragment/전장의 파편/마도철 원석 텍스쳐 없음. ③ 광물 생성기는 드랍이 아니라 **블럭이 생겨 유저가 캐는** 방식, 자동입금은 캘 때 땅에 안 떨어지고 창고로.

**수정 ① (한글명):** `WorkshopRecipeRegistry.displayName`에 흔적류(equip_trace_*·ancient_trace_*·mat_trace_*)·cosmetic_fragment(치장 파편)·rift_king_heart(균열왕의 심장)·mat_stone_enhance 한글명 추가.

**수정 ② (아이콘 폴백):** CMD 모델 없는 커스텀은 PAPER 빈 종이로 보이던 것 → `CustomItemModel.iconMaterial`/`FALLBACK_ICON`로 대표 바닐라 아이콘 표시(res_ore_resonance→IRON_ORE, mat_battle_shard→PRISMARINE_SHARD, cosmetic_fragment→AMETHYST_SHARD, rift_king_heart→NETHER_STAR 등). `buildStack`이 폴백 아이콘 사용. **남은 리소스팩 작업:** equip_trace_faded는 모델(trace_equip_faded.json) 존재하나 깨짐 → paper.json CMD 매핑/resourcepack.zip 동기화 필요(별도). res_silver_ore는 raw_silver 모델 존재하나 CMD 미연결.

**수정 ③ (광물 생성기 블럭 방식):** 드랍→**블럭 생성**으로 전환. `BlockFromToEvent`에서 물이 울타리 인접 빈칸(AIR)으로 흐르면 `setCancelled`+`setType(광물블럭)`(조약돌·구리/철/금/레드스톤/청금석/다이아/에메랄드 ORE 블럭). 바닐라 물 흐름 속도(~0.25s)로 자동 재생성 → "딱딱" 채굴(쿨다운 불필요, 제거). **채굴 자동입금**: `BlockBreakEvent`에서 영지+CONV_AUTO_DEPOSIT ON이면 드랍을 창고(IslandStorage)로 입금하고 `setDropItems(false)`(토글 있었으나 핸들러 미구현이던 것 구현). 작위는 근처 플레이어 기준 유지.

**리스크/메모:** §6 자작령+ 보너스 드랍(다이아/에메랄드 시 추가)은 블럭 방식 전환으로 보류(생성 시점 플레이어 귀속 모호) — 추후 채굴 시 부여 검토. 자동입금은 영지 전체 채굴에 적용(생성기 광물 포함).

**검증:** `./gradlew build` SUCCESSFUL(31 tests). 서버 재기동 정상(Done 22.6s, 예외 없음). 인게임 확인 필요: 창고 한글명·아이콘, 물-울타리 광물 블럭 생성·재생성, 자동입금 ON 시 채굴→창고.

---

### DL-129 추가#32 (2026-06-03) — 마도철/은 원석 텍스쳐 배선·치장파편 제외·작위 승급 비용 표시·파편 요구량 ×50

**사장님:** ① 마도철 원석 텍스쳐 정하지 않았나(확인). ② 치장파편 안 하기로 함 — 제거. ③ 작위 승급에 전장의 파편 영어로 나옴. ④ 승급 조건(골드+전장의 파편) 표시. ⑤ 전장의 파편 요구량 50배(과잉). ⑥ 흔적명 "파손" 수정. ⑦ 장비/고대 흔적 텍스쳐 깨짐·동일 의혹(확인).

**조사 결과:**
- **마도철/은 원석**: paper.json에 `302014→raw_mado_iron`(마도철)·`302013→raw_silver`(은) 텍스쳐 **이미 매핑됨**. `CustomItemModel.CMD`에만 누락 → 추가(코드 수정만, 리소스팩 재패키징 불필요).
- **장비/고대 흔적**: CMD(308101~308106/308201~308204)·모델·텍스쳐 PNG·zip 모두 정상 배선. 모델은 서로 다른 텍스쳐 참조. 깨짐/동일 의혹은 **텍스쳐 아트 문제**(trace_equip_*.png) → Windows Blockbench 소스 작업 영역(메모리 규칙상 AI 텍스쳐 수정 금지) → 별도 보고.
- **전장의 파편(mat_battle_shard)**: 전용 텍스쳐 없음 → PRISMARINE_SHARD 폴백 유지.
- **치장파편**: 보상 코드에 `addCustomItem` 호출 없음 = **미지급 dead data**(record 필드만 inert). 사용자 창고분은 잔존.

**수정:**
- `CustomItemModel.CMD`에 res_ore_resonance→302014, res_silver_ore→302013 추가. FALLBACK_ICON에서 두 원석(이제 CMD有)·cosmetic_fragment 제거.
- cosmetic_fragment: `BossRewardService.COSMETIC_FRAGMENT` 상수 제거(미사용), 표시명 "치장 파편 (미사용)"으로(잔존분 가독). record cosmeticFrag* 필드는 inert 유지.
- 흔적명: equip_trace_broken "파손된"→**"낡은 흔적"**(커먼). 등급=낡은/바랜/빛나는/찬란한/눈부신 + 미감정.
- 작위 승급: 재료부족 메시지·승급 GUI lore에 raw id→**한글명**, 골드+전장의 파편 요구량 표시("추후 공개" 제거).
- **전장의 파편 요구량 ×50** (IslandRank): 50/80/100/80/60/120/200 → 2500/4000/5000/4000/3000/6000/10000.

**작위 승급 비용표 (골드 / 전장의 파편):** 개척→기사 2만/2500 · 기사→준남작 3.5만/4000 · 준남작→남작 5.5만/5000 · 남작→자작 7.5만/4000 · 자작→백작 9.5만/3000 · 백작→후작 12만/6000 · 후작→공작 15만/10000.

**남은 리소스팩 작업:** equip_trace 텍스쳐 아트 깨짐/고대와 중복 — Windows 소스 수정 필요. 전장의 파편 전용 텍스쳐(원하면).

**검증:** `./gradlew build` SUCCESSFUL(31 tests). 서버 재기동 정상(Done 22.0s, 예외 없음). 인게임 확인 필요: 마도철/은 원석 텍스쳐, 승급 한글명·비용 표시·파편 요구량.

---

### DL-129 추가#33 (2026-06-03) — 경매 등록 버그 수정·흔적 정본명·영지 시간/날씨 영속화

**사장님:** ① 경매장 등록 정상작동 안 함. ② 흔적명 "파손"(정본 확인). ③ 영지 시간 설정이 나갔다 들어오면 풀려 밤으로 나옴.

**수정 ① (경매 등록):** `AuctionGuiListener.onChat`만 구식 `AsyncPlayerChatEvent` 사용(나머지 모든 채팅 핸들러는 `AsyncChatEvent`). 모던 Paper에서 구식 이벤트 미발동 → **가격 채팅 입력이 처리 안 돼 등록 미완료**. `io.papermc.paper.event.player.AsyncChatEvent`로 교체 + `PlainTextComponentSerializer`로 메시지 추출. 근본 원인은 이벤트 타입 불일치.

**수정 ② (흔적 정본명):** item_master.csv가 정본 — equip_trace_broken="깨진 장비의 흔적"(파손 아님), faded="빛 바랜", glowing="빛나는", **radiant="눈부신"**, **brilliant="찬란한"**(추가#32에서 radiant/brilliant 뒤바뀜 교정). WorkshopRecipeRegistry를 item_master 명칭에 일치.

**수정 ③ (시간/날씨 영속):** `TerritorySettingsGuiListener`의 시간/날씨가 in-memory Map(`timeStates`/`weatherStates`, 재접속 시 소실)이라 풀렸음(섬 월드 밤 고정→밤으로 보임). `IslandTerritoryState.timeState`/`weatherState` 필드 추가 + Gson 영속(`TerritorySaveData` trailing int 2개, 구 세이브 0 기본). 토글/렌더를 in-memory→영지 상태로 전환. `PlayerJoinListener`에서 재접속 시 40틱 지연 후 `setPlayerTime`/`setPlayerWeather` 재적용.

**메모:** res_ore_resonance item_master명이 "Resonance Ore"(영어) — 경매 등 item_master.itemName 사용처에서 영어 노출 가능. 차후 item_master.csv Korean화 검토. 흔적 텍스쳐 아트 깨짐은 여전히 리소스팩 작업(추가#32).

**검증:** `./gradlew build` SUCCESSFUL(31 tests). 서버 재기동 정상(Done 16.3s, 예외 없음). 인게임 확인 필요: 경매 가격 입력→등록 완료, 흔적명, 시간 설정 재접속 유지.

---

### DL-129 추가#34 (2026-06-04) — 경매 등록 GUI 클릭 미취소·창고 입금 메시지·장비흔적 .mcmeta 누락

**사장님:** ① 경매 등록 GUI 클릭 시 이벤트 미처리·아이템이 그냥 빠져나감. ② 창고 좌클릭 입금 메시지 없음(우클릭만), 영어 표시. ③ 장비의 흔적 텍스쳐 깨짐.

**수정 ① (경매 클릭, 아이템 손실 버그):** `onClick`이 `playerMode==null`이면 취소 없이 return. 메인→등록 전환 시 `openRegister`가 mode 설정 후 `openInventory` 호출 → 이전 메인 GUI close의 `onClose(AUCTION)`이 `playerMode.remove` → 등록 GUI에서 mode=null → **클릭 미취소로 아이템 인벤 이동**. 타이틀 체크가 이미 경매 GUI를 식별하므로 `mode==null` 가드 제거(불필요·해로운 중복).

**수정 ② (창고 입금 메시지):** 좌클릭(1개) 입금에 메시지 누락 → 좌/우 모두 `sendDeposit` 호출. 바닐라 이름이 영어(`titleCase`)였던 것 → `Component.translatable(mat.translationKey())`로 한국어 클라 표시. 커스텀은 displayName(한글) 유지.

**수정 ③ (장비 흔적 텍스쳐):** `trace_equip_faded/glowing/radiant/brilliant.png`가 **16×128(8프레임)인데 `.mcmeta` 애니메이션 파일 누락** → 16×16 아이템에 세로로 짓눌려 깨짐. 고대 흔적(trace_ancient_*)은 .mcmeta 있어 정상. 이전 DL-129 작업에서 고대만 추가하고 장비는 누락한 것. **누락 .mcmeta 4개 추가**(frametime 3·interpolate false, 고대 패턴 복사) → resourcepack.zip 갱신 → sha1 `ad15c19...`로 server.properties 갱신 → 재기동. 클라 재접속 시 새 팩 다운로드. **Windows 소스에도 동일 .mcmeta 추가 필요**(재export 시 유실 방지). trace_equip_broken(16×16)은 단일 프레임이라 불필요.

**검증:** `./gradlew build` SUCCESSFUL(31 tests). 서버 재기동 정상(Done 16.8s). HTTP 팩 서버(8080) 동작·sha1 일치. 인게임 확인 필요: 경매 등록 클릭 정상·아이템 유지, 창고 좌클릭 한글 메시지, 장비 흔적 텍스쳐(재접속 후).

---

### DL-129 추가#35 (2026-06-04) — 흔적 아이템 lore + 경매 등록 재설계(창고 재료 브라우저)

**사장님:** ① 흔적류 아이템에도 lore 추가(레시피에만 있음). ② 경매 등록 선택 안 됨·풀 너무 적음 → 창고 재료 보여주고 거기서 고르게, 가격은 채팅.

**수정 ① (흔적 lore):** `CustomItemModel.buildStack`에 `loreFor(itemId)` 적용 — equip_trace_*="장비 전승(계승) 재료", ancient_trace_*="고대 전승 재료" 설명. 창고 표시·출금 실물 모두 lore 표시(전승=SuccessionService 등급별 재료).

**수정 ② (경매 등록 재설계):** 기존 27칸 GUI의 **8칸 팔레트**(보유 tradeable customItems만)라 풀이 적고 선택 체감 불량 → **54칸 페이지네이션 브라우저**로 교체. 창고의 거래가능 재료(item_master is_tradeable=true, 보유분) 최대 45/페이지 표시. 재료 클릭 → 즉시 가격 채팅 입력 → `confirmRegister`로 1개 등록(클릭→가격→등록 단순화, 선택/미리보기/확인 슬롯 제거). 가격 채팅 "취소"·빈입력 처리. 최대 등록수 사전 체크.

**메모:** 등록 수량은 현재 1개 고정 — 대량 판매(수량 선택)는 후속 검토. 인벤 직접 판매는 미지원(창고 입금 후 등록 — 통합 창고로 입금 간편). tradeable=false 재료(약초·강화석 등)는 의도적 거래 제한 유지.

**검증:** `./gradlew build` SUCCESSFUL(31 tests). 서버 재기동 정상(Done 22.1s, 예외 없음). 인게임 확인 필요: 등록 브라우저에 창고 재료 다수 표시·클릭 선택·가격 채팅→등록, 흔적 lore.

---

### DL-129 추가#36 (2026-06-04) — 흔적 lore 정본화·경매 창고 동일표시·등록 수량 입력

**사장님:** ① 흔적 설명이 이상함(문서 안 보고 붙임). ② 경매 등록이 창고 전체를 안 보여주고 텍스쳐·이름(영어)이 창고와 다름. ③ 등록 수량 = 가격 받고 다음 채팅에 수량, 보유 초과면 "그만큼 없다".

**수정 ① (흔적 정본 lore):** `item_grade_substat_v1 §1`·`gui_succession`·`gui_enhancement` 근거로 `CustomItemModel.loreFor` 재작성 — **장비의 흔적**(전투 드롭·전승 소스·경매 거래), **고대 흔적**(미감정 흔적과 공방 사용→등급 보장, faded 레어↑/glowing 에픽↑/radiant 유니크↑/brilliant 레전더리 확정), **강화 흔적**(별×1.15/달×1.25/태양×1.30 성공률). 이전 추측 lore 폐기.

**수정 ② (경매=창고 동일):** 경매 등록 브라우저가 `itemMaterial`(자체 아이콘)·`itemDisplayName`(item_master 영어명)을 써 창고와 달랐음 → ⓐ `storageIcon`=`CustomItemModel.buildStack`(CMD 텍스쳐+한글명) 사용, ⓑ `itemDisplayName`을 WorkshopRecipeRegistry(한글) 우선으로(경매 전역 한글화), ⓒ 팔레트를 **창고 전체(보유 customItems 전부)**로 확장, 거래불가 재료는 "§c경매 거래 불가" 표기 + 클릭 시 안내(거래는 item_master is_tradeable 유지).

**수정 ③ (수량 입력):** 재료 클릭 → 가격 채팅 → **수량 채팅**(보유량 안내) → 등록. 수량>보유면 "그만큼 없습니다(보유 N)" 후 재시도. `awaitingQtyInput` 단계 추가, onClose가 가격/수량 대기 중 선택 보존.

**메모:** 인벤 직접 판매는 여전히 미지원(창고 입금 후 등록). 메인/내목록 listing 아이콘은 아직 itemMaterial(이름은 한글화됨) — 필요 시 storageIcon 통일.

**검증:** `./gradlew build` SUCCESSFUL(31 tests). 서버 재기동 정상(Done 19.0s, 예외 없음). 인게임 확인 필요: 흔적 설명 정확, 경매 등록이 창고와 동일 표시·전체 재료, 가격→수량 입력·초과 거부.

---

### DL-129 추가#37 (2026-06-04) — 흔적 등급표기·고대lore간결화·경매 창고전체·큐브/강화석 등록·메시지

**사장님:** ① 장비의 흔적 이름별 등급 표시. ② 고대 흔적 lore "공방에서~" 빼고 "미감정 흔적~"부터. ③ 경매=창고 전체(거래불가 체크 임의분리 제거). ④ 큐브·강화석도 경매 등록(실체 없는 통화 → 등록 GUI 1·2번 고정, 텍스쳐 동일). ⑤ "그만큼 없습니다" 정중화.

**수정:**
- ① equip_trace lore에 등급 `[커먼/레어/에픽/유니크/레전더리]`(색상) 표기. ② ancient_trace lore "공방에서" 제거.
- ③ 경매 등록 팔레트 tradeable 필터 제거 → **창고 전체(보유 customItems 전부)** 등록 가능. 클릭 거래불가 차단 제거(거래불가 품목은 애초에 customItems에 미진입 전제).
- ④ **큐브(mat_cube)·강화석(mat_stone_enhance)은 통화(growthState)** — `AuctionStore.CURRENCY_ITEMS` + `isCurrencyItem`. 경매 등록 팔레트 1·2번 고정. 차감/지급/반환/배달을 통화↔창고 라우팅(`heldOf`/`debitItem`/`creditItem`) — confirmRegister·취소·배달(AuctionGuiListener·PlayerJoinListener) 4지점. 큐브 표시명 "큐브"·폴백아이콘 ENDER_EYE, 강화석은 CMD 303002 텍스쳐.
- ⑤ "그만큼 없습니다" → "보유 수량이 부족합니다 (보유 N개)".

**보류/확인 필요:** ⑥ **장비의 흔적 드랍 시 등급+세부스탯** — 현행 흔적은 등급만 가진 **스택형**(item_id→qty). 개별 인스턴스(등급+세부스탯)는 전승 시스템 재설계(현행 카운터형 전승과 충돌, item_grade_substat §4는 SUPERSEDED). 별도 설계 결정 필요 → 사용자 확인.

**검증:** `./gradlew build` SUCCESSFUL(31 tests). 서버 재기동 정상(Done 17.0s, 예외 없음). 인게임 확인 필요: 흔적 등급표기, 경매 창고전체, 큐브·강화석 등록/구매/배달, 메시지.

---

### DL-129 추가#38 (2026-06-04) — 장비의 흔적 "개별 인스턴스화" 설계 결정 (추가#37 ⑥ 후속)

**배경:** 추가#37 ⑥에서 보류한 "흔적 드랍 시 등급+세부스탯". 사용자 확정 방향 = 흔적을 장비처럼 **등급 + 세부스탯을 가진 개별 인스턴스**로. 드랍 시 등급+세부스탯이 롤되어 표시되고, 전승 시 그 흔적의 등급·세부스탯이 장비로 이전. 멀티-시스템 재설계(데이터 모델·드랍·전승·창고·경매·마이그레이션) = 단계별 배포·검증.

**착수 전 선행 결정 (2026-06-04 사용자 확정):**
1. **슬롯 부여 = 범용(슬롯 없음).** 드랍 시 등급만 확정, 세부스탯은 슬롯-무관 풀에서 롤. 어떤 장비에든 전승 가능(현행 유연성 유지). 옛 `item_grade_substat §3`의 슬롯별 흔적은 채택하지 않음.
2. **세부스탯 롤 규칙 = 현행 개수 규칙 그대로.** 등급별 개수 COMMON 1 / RARE 2 / EPIC·UNIQUE·LEGENDARY 3. 값은 옵션 풀 `value_min~value_max` 균등 롤. (현행 `SuccessionService.generateSubstats`의 개수·값 규칙 계승.)
3. **기존 스택 흔적 = 자동 변환.** 보유 스택 `equip_trace_*` N개를 각각 인스턴스 N개로 변환(등급별 세부스탯 롤). 폐기·환급 아님.
4. **경매 인스턴스 거래 = 포함.** 고유 세부스탯 흔적을 경매에 올릴 수 있도록 listing payload(JSON) 부활 + 경매 스키마 확장(`item_data` 또는 별도 테이블). 1차 시즌 포함.
5. **세부스탯 풀 = 슬롯무관 통합풀 신설.** ⚠ 충돌 발견 — 현행 `growth_potential_option_pool.csv`(437행)는 **완전 슬롯별 분리**(weapon/head/chest/legs/feet)이고 슬롯-무관 옵션이 0개. 게다가 슬롯이 스탯 종류 자체를 결정(ATK%=무기·신발만, 치명확률%=무기·머리만, 받피감=머리/상의/하의 전용, 신발=이동테마). 따라서 결정1(범용·드랍시 표시)을 만족하려면 **흔적 전용 슬롯-무관 세부스탯 풀을 신규 작성**해야 함(범용 스탯만: 공격%·체력%·치명·쿨감 등, 슬롯테마 스탯 제외). combat-balance 검토 대상. 결정2의 "현행 규칙"은 *개수·값롤 방식*만 계승하고 풀 자체는 신설로 해석.

**단계별 구현 계획 (각 단계 배포·검증):**
- **P1** 데이터 모델 + 영속 — `TraceInstance`(instanceId, grade, List<PotentialLine> substats) 신규. `IslandTerritoryState`에 `List<TraceInstance>` 추가 + `PlayerSaveData` Gson 직렬화(구 세이브 호환). customItems 스택과 공존(equip_trace_*만 인스턴스 이관, 나머지 재료는 스택 유지).
- **P1b** 슬롯무관 통합 세부스탯 풀 CSV + 로더/롤 서비스.
- **P2** 드랍 롤 — `FieldDropListener` 흔적 드랍 시 등급(기존 분포)+세부스탯(통합풀) 롤 → `TraceInstance` 생성·저장.
- **P3** 전승 재작성 — `SuccessionService`를 인스턴스 소스 기반으로(grade+substat → 장비 적용, 인스턴스 1개 소모). BASIC/GRADE_ONLY/SUBSTAT_ONLY 의미 유지. 전승 GUI 인스턴스 목록 선택.
- **P4** 창고/표시 — 흔적 인스턴스 항목 개별 표시(등급+세부스탯 lore), 입출금 인스턴스 단위.
- **P5** 경매 인스턴스 거래 — listing JSON payload 복원, 등록/구매/배달 인스턴스 단위.
- **P6** 마이그레이션(스택→인스턴스 자동 변환) + CANON 갱신(`item_grade_substat`·`equipment_growth_spec`) + DL 기록.

**근거 문서:** `poro-rpg/docs/02_database_api_stats/equipment_growth_spec.md §2.3`(현행 카운터형 전승, 대체 예정), `item_grade_substat_v1.md`(§3·§4 SUPERSEDED였으나 인스턴스화로 일부 회귀 → P6에서 정합), `growth_potential_option_pool.csv`(슬롯별 풀, 통합풀은 별도 신설).

---

### DL-130 (2026-06-05) — 서버 실행물의 표준 위치 = worktree `<프로젝트>/.local/server`

**무엇:** RPG·PoroMon의 서버 실행물(런타임)을 원본 합본 저장소(`poro-server`) 작업트리 밖, 각 worktree의 `<프로젝트>/.local/server`로 표준화한다.
- RPG: `poro-work-rpg/poro-rpg/.local/server`
- PoroMon: `poro-work-poromon/poromon/.local/server`
- Git 추적 대상은 **직접 만든 산출물만**: 플러그인 소스 `poro-rpg/custom-plugins/`, 커스텀 모드 소스 `poromon/custom-mods/`, 모드팩 메타/오버라이드 `poromon/modpack/`, 각 `docs/`·`scripts/`.

**왜:** worktree_policy(§1) "원본 `poro-server`에서는 서버를 실행하지 않는다"와 정합. 그러나 실제로는 `poro-server/poro-rpg/server`(508M, paper.jar·world·plugins)와 `poro-server/poromon/server`가 원본 작업트리 안에 존재해 정책 위반 상태였다. 실행물을 worktree-local `.local/`로 분리해 (a) 원본은 합본/merge 전용으로 깨끗이 유지, (b) 실행은 각 worktree에서 수행하도록 물리적으로 강제.

**조치 (2026-06-05 실행, `mv` rename, 같은 파일시스템):**
- `poro-server/poro-rpg/server` → `poro-work-rpg/poro-rpg/.local/server` (38항목, 무손상: paper.jar 54,475,623 bytes 동일)
- `poro-server/poromon/server` → `poro-work-poromon/poromon/.local/server` (run·worlds)
- Git 영향: **없음.** 두 `server/`는 원래부터 미추적(`.gitignore:35 server/`)이었고, 목적지 `.local/`도 `.gitignore:127 **/.local/`로 무시됨. `git rm` 대상 0건. master·rpg·poromon worktree 전부 `git status` clean 유지.

**미정리(후속):** `poro-rpg/CLAUDE.md`의 런타임 경로 표기(`poro-rpg/server/plugins`, `poro-rpg/server-config`)가 이번 이동으로 stale. RPG 워크트리 작업 시 `.local/server` 기준으로 갱신 필요(이번 변경 범위 외 — 루트 작업트리에서 RPG 전용 파일 수정 보류).

---

### DL-131 (2026-06-07) — in-repo 최상위 폴더명 `poro-`→`porong-` rename (브랜드 Porong 확정)

**무엇:** 브랜드명이 **Porong/포롱**으로 확정됨에 따라 monorepo in-repo 최상위 프로젝트 폴더를 `git mv`로 rename (master).
- `poro-rpg/` → `porong-rpg/`
- `poromon/` → `porong-mon/`
- `poro-discord/` → `porong-discord/`

**왜:** 디렉터리/워크트리(`porong-work-*`)·GitHub repo(`porong-server`)는 이미 Porong으로 전환됐으나(앞선 단계) in-repo 폴더만 `poro-`로 남아 SoT 경로·문서·gitignore 표기가 브랜드와 불일치했다. 폴더명을 브랜드에 정합시켜 단일 기준으로 통일.

**조치:**
- `git mv` 3건(최상위 폴더만 이동). 추적 파일만 이동되며 gitignored 런타임(`*/server/`·`*/.local/`)은 영향 없음.
- 경로 참조 갱신: `README.md`·`AGENTS.md`·`CLAUDE.md`·각 프로젝트 `CLAUDE.md`·`.gitignore`·`docs/worktree_policy.md`·SoT `final_master_plan.md`·살아있는 설계 문서의 내부 self-link(`porong-rpg/docs/...` 등).
- `porong-mon`은 식별자와 충돌하므로 **폴더 경로만** 치환하고 다음은 보존: mod id/패키지 `poromoncore`·`kr.poro.poromoncore`, 모드 디렉터리 `custom-mods/poromon-core`, 코드 모듈 `modules/poromon`·`integrations/poromon_api.py`, 명령어 `/poromon`, 문서 하위폴더 `03_poromoncore`.

**변경 안 함(명시적 범위 제외):** Java 패키지명, mod id, assets 네임스페이스, Gradle 프로젝트 내부명(`custom-plugins/poro-rpg`, `rootProject.name`), item id/config key, GitHub repo 이름(이미 `porong-server`), 로컬 worktree **디렉터리** 이름. 코드 파일(`*.py`/`*.java`/`*.kts`/`*.sh`)·`_archive/` 내부 문서 미수정.

**이력 보존(DL 정책):** 이 결정 로그(`decision_log.md`)와 `idea_inbox.md`의 **과거 경로 표기는 당시 값 그대로 보존**(일괄치환 되돌림). 폴더 rename은 본 항목으로만 기록한다(원장 무결성). 따라서 DL-130 등 이전 항목의 `poro-rpg`/`poro-server` 표기는 의도적으로 유지된 과거 기록이다.

**후속:** (1) feature 브랜치(`feature/*-dev`)는 아직 옛 폴더명 — 각 브랜치에 본 rename 머지 후 worktree sparse-checkout을 새 이름으로 재설정(worktree_policy §3 전환기 주의). (2) `porong-mon/scripts/*`·`server_runbook.md` 등의 옛 절대경로(`poro-server-poromon`)는 별도 stale 정리 대상.

**관련:** DL-130(런타임 위치 표준화), `docs/worktree_policy.md`(§2·§3·§7).

---

### DL-137 (2026-06-07) — 구상 단계 프로젝트 `porong-economy`/`porong-gun` 등록

**무엇:** 향후 후보 서버 2종을 in-repo 최상위 **구상(아이디어 보관/기획용) 폴더**로 등록.
- `porong-economy/` — 경제/거래/생산/시장 중심 후보 서버.
- `porong-gun/` — 총기/전술/생존 후보 서버(세부 컨셉 미확정).
- 각 폴더는 `README.md` + `docs/concept.md` + `docs/idea_inbox.md` **문서 3종만** 둔다.

**아닌 것(명시):** 확정된 개발 착수가 **아니다.** 아이디어·컨셉을 모으는 단계다.
- 런타임(`.local/server`)·서버 데이터(world/logs/jar)·빌드 산출물·플랫폼 템플릿(Gradle/Fabric/Paper)은 **생성하지 않았다.**
- worktree(`porong-work-economy`/`porong-work-gun`)도 아직 만들지 않았다. 착수 결정 시 worktree_policy §6 절차로 생성.

**왜:** 후보 서버 아이디어를 채팅이 아닌 추적 가능한 문서로 보관하고, README/구조에서 활성/구상 단계를 명확히 구분하기 위함.

**번호 메모:** master 기준 직전 DL은 131이나, `feature/discord-dev`가 DL-132~136을 선점(미머지)했으므로 충돌 회피로 **DL-137**을 부여.

**관련:** README.md(서버별 컨셉·단계 구분), `docs/worktree_policy.md`(§2 구조·§3 예정 worktree·§7), `porong-economy/docs/`, `porong-gun/docs/`.
