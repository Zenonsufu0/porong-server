# 포로 서버 음식 / 만찬 / 물약 마스터 통합 초안

## 문서 목적
음식·만찬·물약 관련 4개 초안(`poro_food_feast_potion_seed_table.md` + `poro_food_feast_potion_effect_table.md` + `poro_food_feast_potion_db_structure.md` + `poro_consumable_effect_seed_config_table.md`)을 단일 마스터 문서로 통합. 원본 4파일은 병합 직후 삭제. 제작 흐름·레시피 전용 문서 `poro_cooking_alchemy_details.md`는 별도 유지.

핵심 원칙:
- 음식·만찬·물약은 **역할이 다르므로** 아이템 분류·버프 슬롯·적용 규칙 모두 분리.
- 효과는 텍스트가 아니라 **데이터**로 관리 — 아이템 + 효과 + 적용 규칙 3축.
- 1차는 보수적 수치로 출발, 테스트 결과에 따라 조정.

## 1. 기본 적용 규칙

### 음식
- 개인 적용, 한 번에 1개
- 새 음식 먹으면 기존 제거 후 대체, 지속시간 초기화

### 만찬
- 파티 전체 적용, 한 번에 1개
- 새 만찬 먹으면 기존 제거 후 대체

### 음식 / 만찬 공통
- **통합 1슬롯** (`MEAL_SLOT`) 공유
- 음식 적용 중 만찬 먹으면 음식 제거, 반대도 동일

### 물약
- 음식/만찬 슬롯과 별개
- 즉시 사용형(회복) 또는 짧은 지속형(대응·전투 보조)
- 재사용 대기시간 필수, 같은 계열 중복 금지

## 2. 아이템 seed

### 2-1. 음식 seed (개인 장기 버프)
| consumable_id | 이름 | 타입 | 주요 효과 | 지속시간 |
|---|---|---|---|---:|
| healing_soup | 회복 수프 | FOOD | 최대 체력 +3% | 1800 |
| gatherer_lunchbox | 채집꾼의 도시락 | FOOD | 채집 효율 +8% | 1800 |
| frost_meal | 설혼 방한식 | FOOD | 냉기 저항 +10% | 1800 |
| flame_meal | 홍염 내열식 | FOOD | 화상 저항 +10% | 1800 |
| survival_meal | 생존 전투식 | FOOD | 회복 효율 +8% | 1800 |
| scout_meal | 정찰병 식사 | FOOD | 이동속도 +3% | 1800 |

### 2-2. 만찬 seed (파티 장기 버프)
| consumable_id | 이름 | 타입 | 주요 효과 | 지속시간 |
|---|---|---|---|---:|
| capital_defense_feast | 제국 수비 만찬 | FEAST | 최대 체력 +5%, 회복 효율 +6% | 2700 |
| forest_spirit_feast | 정령 숲 만찬 | FEAST | 상태이상 저항 +10%, 이동속도 +2% | 2700 |
| frost_guard_feast | 설원의 방한 만찬 | FEAST | 냉기 저항 +12%, 최대 체력 +4% | 2700 |
| flame_guard_feast | 홍염 대비 만찬 | FEAST | 화상 저항 +12%, 회복 효율 +6% | 2700 |
| explorer_feast | 탐험가의 성찬 | FEAST | 최대 체력 +4%, 상태이상 저항 +8% | 2700 |

### 2-3. 회복 물약 seed
| consumable_id | 이름 | 타입 | 등급 | 주요 효과 | 쿨다운 |
|---|---|---|---|---|---:|
| basic_healing_potion | 일반 회복 물약 | POTION_HEAL | NORMAL | 체력 15% 회복 | 30 |
| advanced_healing_potion | 고급 회복 물약 | POTION_HEAL | ADVANCED | 체력 25% 회복 | 35 |
| rare_healing_potion | 희귀 회복 물약 | POTION_HEAL | RARE | 체력 35% 회복 + 피해감소 5% 5초 | 40 |

### 2-4. 상태 대응 물약 seed
| consumable_id | 이름 | 타입 | 주요 효과 | 지속시간 |
|---|---|---|---|---:|
| frost_relief_potion | 냉기 완화 물약 | POTION_RESIST | 냉기 대응 +15% | 90 |
| flame_resist_potion | 화상 저항 물약 | POTION_RESIST | 화상 저항 +15% | 90 |
| poison_relief_potion | 독 완화 물약 | POTION_RESIST | 독 지속 완화 +20% | 90 |
| bleed_relief_potion | 출혈 완화 물약 | POTION_RESIST | 출혈 피해 완화 +15% | 90 |
| erosion_relief_agent | 침식 완화제 | POTION_RESIST | 침식 대응 +15% | 90 |

### 2-5. 전투 보조 물약 seed
| consumable_id | 이름 | 타입 | 주요 효과 | 지속시간 |
|---|---|---|---|---:|
| survival_tonic | 생존 강화 약제 | POTION_COMBAT | 받는 피해 8% 감소 | 30 |
| focus_extract | 집중 추출액 | POTION_COMBAT | 상태이상 저항 +10%, 이동속도 +3% | 20 |
| emergency_regen_tonic | 응급 재생 약제 | POTION_COMBAT | 2초마다 체력 3% 회복 | 10 |
| mobility_tonic | 긴급 기동 약제 | POTION_COMBAT | 이동속도 +8% | 15 |

### 2-6. 강화 보조 물약 seed (2026-04-19 신규 — 영지 연금술, 배율 수식 확정)
"25강 최대 + 파괴/하락 없음" 구조에서 강화 실패 시 재료만 소비되는 포로 체계상, 성공률 증가 = 재료 절약 레버. 각인 시약 제도 삭제 후 도입된 **유일한 강화 보조 소비품**. 수식은 **고정 %p가 아닌 원래 성공률 대비 배율(%) 증가** — 고강 구간 자연 감쇠로 페이싱 보호.

| consumable_id | 이름 | 타입 | 등급 | 주요 효과 | 지속 / 쿨다운 |
|---|---|---|---|---|---|
| craftsman_focus_lesser | 장인의 집중제 | POTION_ENHANCE | NORMAL | **다음 1회 강화 성공률 × 1.05** (원래 대비 +5%) | 1회 소모 / 쿨다운 없음 |
| craftsman_focus_greater | 숙련된 집중제 | POTION_ENHANCE | ADVANCED | **다음 1회 강화 성공률 × 1.10** (원래 대비 +10%) | 1회 소모 / 쿨다운 없음 |
| craftsman_focus_masters | 장인의 영약 | POTION_ENHANCE | RARE | **다음 1회 강화 성공률 × 1.20** (원래 대비 +20%) | 1회 소모 / 쿨다운 없음 |

수식: `실제 성공률 = min(기본 R × 배율, 0.95)`

운영 규칙:
- **1회 소모형**: 물약 사용 → 다음 1회 강화에 효과 적용 → 성공·실패 무관 소모.
- **중복 불가**: 동시에 1종만 활성. 이미 버프 보유 중 다른 등급 사용 시 "교체" 팝업 → 기존 효과 제거 후 새 효과 적용.
- **파괴/하락 없음** 원칙 유지 — 실패 시 강화석·파편·조각 재료만 소비, 장비 단계는 변하지 않음. 성공률 증가 = 재료 절약 효과로만 작동.
- **상한 클램프**: 기본 성공률 × 배율이 95%를 초과하지 않도록 클램프(저강 구간 95% 물약 무의미 — 의도된 설계).
- **귀속 여부**: NORMAL/ADVANCED는 거래 가능, RARE는 귀속 고려(economy-reviewer 2026-04-19 판정: 안전, 대신 영지 합금 드랍량이 자연 상한 역할).
- **획득 경로**: 영지 연금 제작이 주 루트. 보스 드랍은 RARE만 저확률로 허용.
- **페이싱 특성**: 배율 수식 덕에 고강(18강 이하 기본 14~22%)에서 RARE 적용 시 절대 증가 폭이 +2.8~+4.4%p로 자연 감쇠. **RARE ROI 피크는 중강 50~68% 구간** — 플레이어는 고강 몰빵보다 중강 졸업 가속용으로 학습.

## 3. 효과 상세표

### 3-1. 음식 효과표
| 분류 | 이름 예시 | 지속 | 방향 | 권장 수치 |
|---|---|---:|---|---|
| 일반 음식 | 회복 수프 | 30분 | 생존 보조 | 최대 체력 +3% |
| 일반 음식 | 채집꾼의 도시락 | 30분 | 생활 효율 | 채집 효율 +8% |
| 일반 음식 | 설혼 방한식 | 30분 | 냉기 대응 | 냉기 저항 +10% |
| 일반 음식 | 홍염 내열식 | 30분 | 화상 대응 | 화상 저항 +10% |
| 일반 음식 | 생존 전투식 | 30분 | 생존 보조 | 회복 효율 +8% |
| 일반 음식 | 정찰병 식사 | 30분 | 이동 보조 | 이동속도 +3% |

설계 메모:
- 일반 음식은 공격 버프보다 **생존·저항·편의** 쪽이 안전.
- 테마 대응 음식은 북부·남부처럼 상태이상 테마가 강한 곳에서 의미 큼.

### 3-2. 만찬 효과표
| 분류 | 이름 예시 | 지속 | 방향 | 권장 수치 |
|---|---|---:|---|---|
| 만찬 | 제국 수비 만찬 | 45분 | 생존 안정성 | 최대 체력 +5%, 회복 효율 +6% |
| 만찬 | 정령 숲 만찬 | 45분 | 저항·안정 | 상태이상 저항 +10%, 이동속도 +2% |
| 만찬 | 설원의 방한 만찬 | 45분 | 북부 대응 | 냉기 저항 +12%, 최대 체력 +4% |
| 만찬 | 홍염 대비 만찬 | 45분 | 남부 대응 | 화상 저항 +12%, 회복 효율 +6% |
| 만찬 | 탐험가의 성찬 | 45분 | 범용 준비용 | 최대 체력 +4%, 상태이상 저항 +8% |

설계 메모:
- 만찬은 일반 음식보다 조금 더 강하거나 복합 효과.
- 공격 계열보다 생존·저항 쪽이 포로 철학에 부합.
- 파티 전체 적용이라 너무 강하면 필수 숙제가 됨 → 조심.

### 3-3. 회복 물약 효과표
| 등급 | 이름 | 쿨다운 | 방향 | 권장 수치 |
|---|---|---:|---|---|
| 일반 | 일반 회복 물약 | 30초 | 즉시 회복 | 최대 체력의 15% |
| 고급 | 고급 회복 물약 | 35초 | 즉시 회복 | 최대 체력의 25% |
| 희귀 | 희귀 회복 물약 | 40초 | 즉시 회복 + 부가 | 최대 체력의 35% + 5초간 받는 피해 5% 감소 |

설계 메모:
- 희귀 물약은 회복량만 올리기보다 작은 부가효과 추가로 고급 소비품 느낌.
- 1차는 보수적 출발 권장(보스 설계 붕괴 방지).

### 3-4. 상태 대응 물약 효과표
| 이름 | 지속 | 방향 | 권장 수치 |
|---|---:|---|---|
| 냉기 완화 물약 | 90초 | 북부 대응 | 냉기 축적/효과 완화 +15% |
| 화상 저항 물약 | 90초 | 남부 대응 | 화상 저항 +15% |
| 독 완화 물약 | 90초 | 독 대응 | 독 지속시간 완화 +20% |
| 출혈 완화 물약 | 90초 | 출혈 대응 | 출혈 피해 완화 +15% |
| 침식 완화제 | 90초 | 특수 대응 | 침식류 누적 완화 +15% |

설계 메모:
- "대비하면 편해지는 정도"가 적정선. 없다고 진행 불가가 되면 안 됨.
- 저항/지속 완화/축적 완화 중 하나로 단순화.

### 3-5. 전투 보조 물약 효과표
| 이름 | 지속 | 방향 | 권장 수치 |
|---|---:|---|---|
| 생존 강화 약제 | 30초 | 짧은 생존 | 받는 피해 8% 감소 |
| 집중 추출액 | 20초 | 짧은 전투 집중 | 상태이상 저항 +10%, 이동속도 +3% |
| 응급 재생 약제 | 10초 | 짧은 회복 | 2초마다 체력 3% 회복 |
| 긴급 기동 약제 | 15초 | 회피/이동 | 이동속도 +8% |

설계 메모:
- 직접 딜 증가보다 생존·이동·대응 쪽이 안전.
- 극상위·결전에서 "준비 잘한 파티의 안정성" 용도.

### 3-6. 강화 보조 물약 효과표 (배율 수식 확정)
| 등급 | 이름 | 소모 | 배율 | 95% 상한 |
|---|---|---|---|---|
| 일반 | 장인의 집중제 | 1회 | × 1.05 | 적용 |
| 고급 | 숙련된 집중제 | 1회 | × 1.10 | 적용 |
| 희귀 | 장인의 영약 | 1회 | × 1.20 | 적용 |

구간별 실제 성공률 예시 (기본 R → RARE 적용):

| 기본 R | NORMAL ×1.05 | ADVANCED ×1.10 | RARE ×1.20 | RARE 절대 +Δ |
|---|---|---|---|---|
| 95% | 95.0% (클램프) | 95.0% (클램프) | 95.0% (클램프) | +0.0%p |
| 85% | 89.25% | 93.5% | 95.0% (클램프) | +10.0%p |
| 68% | 71.4% | 74.8% | 81.6% | +13.6%p |
| 50% | 52.5% | 55.0% | 60.0% | +10.0%p |
| 32% | 33.6% | 35.2% | 38.4% | +6.4%p |
| 22% | 23.1% | 24.2% | 26.4% | +4.4%p |
| 14% | 14.7% | 15.4% | 16.8% | +2.8%p |
| 6% | 6.3% | 6.6% | 7.2% | +1.2%p |

설계 메모 (economy-reviewer 2026-04-19 판정):
- 배율 수식 덕에 **고강 페이싱 자연 보호**. 기본 14% 이하 구간에서 RARE 적용 시 절대 증가 폭 +2.8%p 이하.
- **RARE ROI 피크는 중강 50~68% 구간** (+10~+13.6%p). 플레이어는 중강 졸업 가속용으로 학습.
- NORMAL은 95% 클램프 탓에 저강 구간 무효화 — 사실상 연금 Lv 7 튜토리얼·소모 사이클 형성용.
- 완전 100% 성공은 의도적으로 금지 (95% 상한 클램프).
- 영지 합금·별가루 제작 제약이 자연 상한 역할. 연금 Lv 15(RARE) 도달 시점 자체가 1막 후반이라 고강 초반엔 NORMAL·ADVANCED만 사용 가능.

## 4. 효과 카테고리 / 중복 규칙
| 카테고리 | 설명 | 중복 규칙 |
|---|---|---|
| 음식 | 개인 장기 버프 | `MEAL_SLOT` 1개 |
| 만찬 | 파티 장기 버프 | `MEAL_SLOT` 1개 (음식과 공유) |
| 회복 물약 | 즉시 회복 | 개별 쿨다운 |
| 상태 대응 물약 | 특정 상태 대응 | `RESIST_POTION_SLOT` 1개 |
| 전투 보조 물약 | 짧은 생존/이동/집중 | `COMBAT_POTION_SLOT` 1개 |
| 강화 보조 물약 | 다음 1회 강화 성공률 증가 | `ENHANCE_SLOT` 1개 (1회 소모) |

## 5. consumable_effect seed 추천 필드 (짧은 config용)
- `consumable_id`
- `effect_order`
- `effect_type`
- `target_type`
- `buff_slot_type`
- `duration_seconds`
- `cooldown_seconds`
- `value_type`
- `value_amount`
- `secondary_effect_type`
- `secondary_value_amount`
- `replace_existing`

예시 — 제국 수비 만찬:
| consumable_id | order | effect_type | target | slot | duration | value |
|---|---:|---|---|---|---:|---:|
| capital_defense_feast | 1 | MAX_HP_PERCENT | PARTY | MEAL_SLOT | 2700 | 5 |
| capital_defense_feast | 2 | RECOVERY_EFFICIENCY | PARTY | MEAL_SLOT | 2700 | 6 |

예시 — 희귀 회복 물약:
| consumable_id | order | effect_type | target | slot | cooldown | value |
|---|---:|---|---|---|---:|---:|
| rare_healing_potion | 1 | HEAL_PERCENT | SELF | NONE | 40 | 35 |
| rare_healing_potion | 2 | DAMAGE_REDUCTION | SELF | NONE | 40 | 5 |

예시 — 침식 완화제:
| consumable_id | order | effect_type | target | slot | duration | value |
|---|---:|---|---|---|---:|---:|
| erosion_relief_agent | 1 | STATUS_ACCUMULATION_REDUCTION | SELF | RESIST_POTION_SLOT | 90 | 15 |

## 6. DB 스키마

### 6-1. `consumable_items` (음식/만찬/물약 마스터)
- `consumable_id` TEXT PK
- `consumable_name` TEXT
- `consumable_type` TEXT (`FOOD` / `FEAST` / `POTION_HEAL` / `POTION_RESIST` / `POTION_COMBAT` / `POTION_ENHANCE`)
- `grade` TEXT NULL (`NORMAL` / `ADVANCED` / `RARE`)
- `item_rarity` TEXT NULL
- `required_life_skill_type` TEXT
- `required_life_skill_level` INTEGER
- `recipe_id` TEXT NULL
- `is_tradeable` INTEGER
- `is_active` INTEGER

### 6-2. `consumable_effects` (효과 마스터)
- `effect_id` TEXT PK
- `consumable_id` TEXT
- `effect_type` TEXT (MAX_HP_PERCENT / HEAL_PERCENT / STATUS_RESIST / MOVE_SPEED_PERCENT / RECOVERY_EFFICIENCY / DAMAGE_REDUCTION / DOT_REDUCTION / STATUS_ACCUMULATION_REDUCTION / ENHANCE_SUCCESS_RATE)
- `target_type` TEXT (SELF / PARTY)
- `buff_slot_type` TEXT NULL (MEAL_SLOT / RESIST_POTION_SLOT / COMBAT_POTION_SLOT / ENHANCE_SLOT)
- `duration_seconds` INTEGER NULL
- `cooldown_seconds` INTEGER NULL
- `value_type` TEXT (PERCENT / FLAT / SECONDS)
- `value_amount` REAL
- `secondary_effect_type` TEXT NULL
- `secondary_value_amount` REAL NULL
- `is_replace_existing` INTEGER

### 6-3. `consumable_cooldowns` (유저 개별 쿨타임)
- `user_id` TEXT
- `consumable_type` TEXT
- `cooldown_ends_at` DATETIME
- PK: `(user_id, consumable_type)`

### 6-4. `user_active_consumable_effects` (현재 활성 버프)
- `user_id` TEXT
- `buff_slot_type` TEXT
- `consumable_id` TEXT
- `applied_at` DATETIME
- `expires_at` DATETIME
- `source_user_id` TEXT NULL
- `source_context` TEXT NULL (`self_food` / `party_feast` / `self_potion`)
- PK: `(user_id, buff_slot_type)`

### 6-5. `consumable_use_logs`
- `use_log_id` TEXT PK
- `user_id` TEXT
- `consumable_id` TEXT
- `consumable_type` TEXT
- `target_type` TEXT
- `used_at` DATETIME
- `applied_count` INTEGER

### 6-6. `feast_party_apply_logs` (옵션)
- `apply_log_id` TEXT PK
- `source_user_id` TEXT
- `target_user_id` TEXT
- `consumable_id` TEXT
- `applied_at` DATETIME
- `expires_at` DATETIME

## 7. 구조 예시 (효과 데이터 세트)

### 7-1. 음식 예시 — 회복 수프
- consumable_id: `healing_soup`, type: `FOOD`
- 효과: MAX_HP_PERCENT / SELF / MEAL_SLOT / 1800s / PERCENT 3
- 규칙: 기존 MEAL_SLOT 제거 후 새 음식 적용, 30분 지속

### 7-2. 만찬 예시 — 제국 수비 만찬
- consumable_id: `capital_defense_feast`, type: `FEAST`
- 효과1: MAX_HP_PERCENT / PARTY / MEAL_SLOT / 2700s / PERCENT 5
- 효과2: RECOVERY_EFFICIENCY / PARTY / MEAL_SLOT / 2700s / PERCENT 6
- 규칙: 파티원 전원에게 적용, 각자 MEAL_SLOT 대체

### 7-3. 일반 회복 물약 예시
- consumable_id: `basic_healing_potion`, type: `POTION_HEAL`, grade: `NORMAL`
- 효과: HEAL_PERCENT / SELF / duration NULL / cooldown 30s / PERCENT 15
- 규칙: 즉시 체력 15% 회복, 회복 물약 쿨다운 30초

### 7-4. 상태 대응 물약 예시 — 냉기 완화
- consumable_id: `frost_relief_potion`, type: `POTION_RESIST`
- 효과: STATUS_ACCUMULATION_REDUCTION / SELF / RESIST_POTION_SLOT / 90s / PERCENT 15
- 규칙: 기존 RESIST_POTION_SLOT 대체, 90초 냉기 대응

### 7-5. 전투 보조 물약 예시 — 생존 강화
- consumable_id: `survival_tonic`, type: `POTION_COMBAT`
- 효과: DAMAGE_REDUCTION / SELF / COMBAT_POTION_SLOT / 30s / PERCENT 8
- 규칙: 기존 COMBAT_POTION_SLOT 대체, 30초 받는 피해 8% 감소

## 8. recipe 연결

### 8-1. `life_crafting_recipes` 연동
- `craft_category`: COOKING / FEAST / ALCHEMY
- `result_item_id`: consumable item code
- `required_skill_type`: COOKING 또는 ALCHEMY

### 8-2. 제작 분류
- `COOKING` — 개인 음식
- `FEAST` — 만찬
- `ALCHEMY_HEAL` — 회복 물약
- `ALCHEMY_RESIST` — 상태 대응 물약
- `ALCHEMY_COMBAT` — 전투 보조 물약

## 9. 서비스 구조
- `ConsumableUseService` — 아이템 사용 진입점, 타입 분기
- `MealBuffService` — 음식/만찬 대체, MEAL_SLOT 관리
- `FeastApplyService` — 파티 대상 탐색 + 만찬 일괄 적용
- `PotionCooldownService` — 회복/전투 물약 쿨타임 검사·등록
- `ConsumableEffectResolver` — 효과 수치 조회·복합 조합
- `ConsumableLogService` — 사용 로그 기록

## 10. 등급 구조
- 회복 물약: NORMAL 15% / ADVANCED 25% / RARE 35%
- 음식 / 만찬: 1차는 등급 분리 안 함. 필요 시 BASIC / SPECIAL / FEAST 3단. 초기엔 레시피·카테고리 기준 분리가 더 낫다.

## 11. 1차 구현 추천 범위
- 음식 4~6종
- 만찬 2~4종
- 회복 물약 3등급
- 상태 대응 물약 4~5종 (냉기·화상·독·출혈·침식)
- 전투 보조 물약 2~3종 (생존·집중·기동)

## 12. 운영자 대시보드 경고 예시
- 특정 만찬 사용률 과도 → 필수 숙제화 징조
- 회복 물약 사용량 과다 → 보스 회복 흐름 재점검
- 특정 상태 대응 물약 필수품화 → 테마 밸런스 조정 신호

## 13. Codex 구현 지시 포인트
- A: consumable item/effect master schema
- B: type enum + DTO
- C: MEAL_SLOT / RESIST_POTION_SLOT / COMBAT_POTION_SLOT 기반 버프 서비스
- D: 회복 물약 쿨다운 서비스
- E: recipe 결과물을 consumable item과 연결하는 제작 로직

## 14. 한 줄 요약
**음식·만찬·물약은 `consumable_items` + `consumable_effects` + `user_active_consumable_effects` + `consumable_cooldowns` + `consumable_use_logs` 5테이블로 분리하고, 음식·만찬은 `MEAL_SLOT` 1개를 공유, 물약은 대응·전투 슬롯 별도 + 회복은 즉시 쿨다운 구조로 운영하는 것이 포로 서버에 가장 적합하다.**

## 상위 문서 참조
- 마스터플래닝: `../poro_master_planning.md` 생활·소비품 섹션
- 제작 흐름·레시피 상세: `poro_cooking_alchemy_details.md` (별도 유지)
- 올인원 조합대: `../03_life_system_core/poro_allinone_workbench_draft.md`
- 상태·제어·표식: `../10_seed_and_config_tables/poro_status_control_mark_master_table.md`
