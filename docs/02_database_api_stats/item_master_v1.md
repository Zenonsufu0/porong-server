# 아이템 마스터 정의서 v1

> **[STATUS: REFERENCE]** — 전체 아이템 식별자·MC 재질·CMD 범위 기준. 코드·DB·리소스팩 공통 참조.
>
> 작성: 2026-05-25

---

## 0. 규칙

### item_id 명명 규칙

```
{카테고리}_{설명}
```

| 접두사 | 카테고리 |
|---|---|
| `equip_` | 장비 완제품 (무기·방어구) |
| `equip_trace_` | 장비의 흔적 |
| `ancient_trace_` | 고대흔적 |
| `mat_` | 재화·소재 (물리 아이템) |
| `con_` | 소비품 (포션·만찬·전승권 등) |
| `trophy_` | 트로피 (최종보스 전리품) |
| `title_` | 칭호 아이템 |

### MC 기반 재질 원칙

| 용도 | 기반 재질 |
|---|---|
| 재화·소재 일반 | `minecraft:paper` |
| 고대흔적·장비의 흔적 | `minecraft:paper` |
| 소비품 (포션) | `minecraft:potion` / `minecraft:splash_potion` |
| 소비품 (만찬) | `minecraft:cooked_beef` |
| 소비품 (전승권·변경권) | `minecraft:paper` |
| 트로피 | `minecraft:nether_star` |
| 장비 무기 6종 | 아래 장비 테이블 참조 |
| 장비 방어구 4종 | 아래 장비 테이블 참조 |

### CustomModelData (CMD) 범위 할당

| 범위 | 용도 |
|---|---|
| `100001~100499` | 치장 무기 — 검·창·도끼·석궁·낫·스태프 |
| `100500~100599` | 치장 무기 — 낫 전용 (100502 기 등록) |
| `200001~200999` | GUI 아이콘·패널 PNG 아이템 |
| `300001~300499` | 재화·소재 아이콘 (mat_*) |
| `300500~300599` | 소비품 아이콘 (con_*) |
| `400001~400099` | 장비의 흔적 (equip_trace_*) |
| `400100~400199` | 고대흔적 (ancient_trace_*) |
| `400200~400299` | 트로피·칭호 (trophy_*, title_*) |
| `500001~500099` | 코스메틱 날개 (500001 기 등록) |
| `500100~500999` | 코스메틱 기타 |

> CMD는 MC 1.21+ `minecraft:custom_model_data` strings 형식 사용.  
> 예시: `[minecraft:custom_model_data={strings:["300001"]}]`

---

## 1. 장비 완제품

> 드롭 없음. 서버 첫 접속 시 T1 일괄 지급. 강화·잠재·전승으로 성장.

| item_id | 한글명 | MC 재질 | CMD | 비고 |
|---|---|---|---|---|
| `equip_sword` | 검 | `minecraft:netherite_sword` | — | 기본 외형 |
| `equip_axe` | 도끼 | `minecraft:netherite_axe` | — | |
| `equip_spear` | 창 | `minecraft:netherite_sword` | 100101 | 창 전용 CMD |
| `equip_crossbow` | 석궁 | `minecraft:crossbow` | — | |
| `equip_scythe` | 낫 | `minecraft:netherite_hoe` | 100501 | 낫 기본 외형 |
| `equip_staff` | 스태프 | `minecraft:blaze_rod` | 100201 | 스태프 기본 외형 |
| `equip_helmet` | 투구 | `minecraft:netherite_helmet` | — | |
| `equip_chestplate` | 상의 | `minecraft:netherite_chestplate` | — | |
| `equip_leggings` | 하의 | `minecraft:netherite_leggings` | — | |
| `equip_boots` | 신발 | `minecraft:netherite_boots` | — | |

> 무기 슬롯은 클래스 선택 시 해당 `equip_{class}` 1개만 지급. 방어구 4종은 공통 지급.

---

## 2. 장비의 흔적

> 보스 드랍. 잠재 등급 포함. 전승 재료.

| item_id | 한글명 | 등급 | MC 재질 | CMD |
|---|---|---|---|---|
| `equip_trace_broken` | 깨진 장비의 흔적 | 커먼 | `minecraft:paper` | 400001 |
| `equip_trace_faded` | 빛 바랜 장비의 흔적 | 레어 | `minecraft:paper` | 400002 |
| `equip_trace_glowing` | 빛나는 장비의 흔적 | 에픽 | `minecraft:paper` | 400003 |
| `equip_trace_radiant` | 눈부신 장비의 흔적 | 유니크 | `minecraft:paper` | 400004 |
| `equip_trace_brilliant` | 찬란한 장비의 흔적 | 레전더리 | `minecraft:paper` | 400005 |
| `equip_trace_unidentified` | 미감정 흔적 | — | `minecraft:paper` | 400000 |

---

## 3. 고대흔적

> 공방 가공기 제작. 잠재 전승 재료.

| item_id | 한글명 | 등급 | MC 재질 | CMD |
|---|---|---|---|---|
| `ancient_trace_faded` | 빛 바랜 고대흔적 | 레어 | `minecraft:paper` | 400101 |
| `ancient_trace_glowing` | 빛나는 고대흔적 | 에픽 | `minecraft:paper` | 400102 |
| `ancient_trace_radiant` | 눈부신 고대흔적 | 유니크 | `minecraft:paper` | 400103 |
| `ancient_trace_brilliant` | 찬란한 고대흔적 | 레전더리 | `minecraft:paper` | 400104 |

---

## 4. 강화 관련 재화

| item_id | 한글명 | 물리 여부 | MC 재질 | CMD | 비고 |
|---|---|---|---|---|---|
| `mat_stone_enhance` | 강화석 | **DB 가상재화** | — | — | 처치 시 직접 적립 (M-5) |
| `mat_trace_star` | 별의 흔적 | 물리 | `minecraft:paper` | 300101 | 강화 성공률 +20%p |
| `mat_trace_moon` | 달의 흔적 | 물리 | `minecraft:paper` | 300102 | +30%p |
| `mat_trace_sun` | 태양의 흔적 | 물리 | `minecraft:paper` | 300103 | +50%p |

---

## 5. 큐브 관련 재화

| item_id | 한글명 | 물리 여부 | MC 재질 | CMD | 비고 |
|---|---|---|---|---|---|
| `mat_cube_fragment` | 큐브 조각 | 물리 | `minecraft:paper` | 300201 | 10개 → 큐브 1개 자동 교환 |
| `mat_cube` | 큐브 | 물리 | `minecraft:paper` | 300202 | 사용 비용 500G |

---

## 6. 필드·영지 재화

| item_id | 한글명 | 물리 여부 | MC 재질 | CMD | 수급처 |
|---|---|---|---|---|---|
| `mat_battle_shard` | 전장의 파편 | 물리 | `minecraft:paper` | 300001 | 필드 몹·보스 |
| `mat_herb_basic` | 기본 약초 | 물리 | `minecraft:paper` | 300301 | 약초 재배지 |
| `mat_herb_refined` | 정제 약초 | 물리 | `minecraft:paper` | 300302 | 공방 정제 |
| `mat_mado_alloy` | 마도합금 | 물리 | `minecraft:paper` | 300303 | 공방 제련 |
| `mat_essence_farmer` | 농부의 정수 | 물리 | `minecraft:paper` | 300304 | 약초 재배지 Lv3 |
| `mat_essence_miner` | 광부의 정수 | 물리 | `minecraft:paper` | 300305 | 광물 채굴기 Lv3 |
| `mat_essence_nature` | 자연의 정수 | 물리 | `minecraft:paper` | 300306 | 고위 작위 해금 |
| `mat_essence_imperial` | 제국 정수 | 물리 | `minecraft:paper` | 300307 | 고위 작위 해금 |
| `mat_cosmetic_shard` | 치장 파편 | 물리 | `minecraft:paper` | 300401 | 시즌보스 드랍 |
| `mat_title_shard` | 칭호 재료 | 물리 | `minecraft:paper` | 300402 | 시즌보스 드랍 |

---

## 7. 소비품 — 포션

> 공방 또는 NPC 구매. 전투 중 사용 가능.

| item_id | 한글명 | MC 재질 | CMD | 효과 |
|---|---|---|---|---|
| `con_heal_minor` | 치료 포션 (소) | `minecraft:potion` | 300501 | HP 회복 소량 |
| `con_heal_mid` | 치료 포션 (중) | `minecraft:potion` | 300502 | HP 회복 중량 |
| `con_heal_major` | 치료 포션 (대) | `minecraft:potion` | 300503 | HP 회복 대량 |
| `con_potion_gold` | 골드 부스트 포션 | `minecraft:potion` | 300511 | 골드 획득 +% |
| `con_potion_enhance` | 강화 부스트 포션 | `minecraft:potion` | 300512 | 강화 성공률 보조 |
| `con_potion_exp` | 경험치 부스트 포션 | `minecraft:potion` | 300513 | 경험치 획득 +% |
| `con_battle_elixir` | 배틀 엘릭서 | `minecraft:potion` | 300514 | 전투력 강화 |

---

## 8. 소비품 — 만찬

> 공방 제작. 전투 전 사용. 일정 시간 버프.

| item_id | 한글명 | MC 재질 | CMD | 효과 |
|---|---|---|---|---|
| `con_feast_warrior` | 전사의 만찬 | `minecraft:cooked_beef` | 300521 | ATK 버프 |
| `con_feast_slayer` | 학살자의 만찬 | `minecraft:cooked_beef` | 300522 | 크리 버프 |
| `con_feast_assassin` | 암살자의 만찬 | `minecraft:cooked_beef` | 300523 | 스킬 피해 버프 |
| `con_feast_hunter` | 사냥꾼의 만찬 | `minecraft:cooked_beef` | 300524 | 보스 피해 버프 |

---

## 9. 소비품 — 전승권·변경권

| item_id | 한글명 | MC 재질 | CMD | 비고 |
|---|---|---|---|---|
| `con_succession_grade` | 등급전승권 | `minecraft:paper` | 300531 | 잠재 등급만 전승, 100,000G |
| `con_succession_stat` | 세부스탯전승권 | `minecraft:paper` | 300532 | 부가 스텟만 전승, 100,000G |
| `con_rename_korean` | 닉네임 변경권 | `minecraft:paper` | 300541 | 한글 닉네임 1회 변경 |
| `con_title_custom` | 커스텀 칭호 변경권 | `minecraft:paper` | 300542 | 커스텀 칭호 1회 변경 |

---

## 10. 트로피 · 칭호

> 최종보스 처치 기념 아이템. 거래 불가.

| item_id | 한글명 | MC 재질 | CMD | 출처 |
|---|---|---|---|---|
| `trophy_rift_heart` | 균열왕의 심장 | `minecraft:nether_star` | 400201 | 균열왕 |
| `trophy_duality_stone` | 이중체의 쌍혼석 | `minecraft:nether_star` | 400202 | 타락한 이중체 |
| `trophy_requiem_crystal` | 진혼의 별빛 결정 | `minecraft:nether_star` | 400203 | 진혼의 주시자 |
| `title_season_final` | 시즌 최종 칭호 | `minecraft:paper` | 400204 | 3종 모두 보유 시 |

---

## 11. DB 가상 재화 (물리 아이템 없음)

> DB `player_resource` 테이블에만 존재. 인벤토리에 들어오지 않음.

| item_id | 한글명 | 단위 | 비고 |
|---|---|---|---|
| `mat_stone_enhance` | 강화석 | 정수 | 처치 시 직접 적립, 스코어보드 표시 |
| `gold` | 골드 (G) | 정수 | 서버 내 주 화폐 |

---

## 12. 변경 이력

| 날짜 | 내용 |
|---|---|
| 2026-05-25 | 초안 작성 — 전체 item_id 정의, CMD 범위 할당 |
