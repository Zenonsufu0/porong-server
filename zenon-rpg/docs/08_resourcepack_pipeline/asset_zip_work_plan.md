# asset.zip 에셋 작업 계획 (내일 이어 작업용)

> **[STATUS: DRAFT]** — asset.zip 작업 계획. 공식 리소스팩 기준은 `index.md`와 관련 GUI 문서가 우선.

> 작성일: 2026-05-19  
> 기준: asset.zip 검수 결과 (임시 압축 해제 경로: `~/dev/poro-server/tmp/asset_inspect/extracted/`)  
> 원본 asset.zip은 절대 수정하지 않는다.

---

## 작업 상태 체크리스트

- [x] 바로 복사 (이름 유지) — 2026-05-19 완료
- [x] 이름 바꿔서 복사 — 2026-05-19 완료
- [x] 3D 모델 JSON namespace 교체 + 복사 — 2026-05-19 완료
- [x] 인게임 item_model 등록 (paper.json CMD 등록 + 모델 JSON 생성) — 2026-05-19 완료
- [ ] 복사 후 리소스팩 반영 (rsync to Windows .minecraft)
- [ ] F3+T 인게임 테스트

---

## 임시 폴더 경로 빠른 참조

```
TMP=~/dev/poro-server/tmp/asset_inspect/extracted

potion:      $TMP/potion1/en/itemsadder-type/.../resourcepack/potion1/textures/
grass:       $TMP/grass1/en/itemsadder-type/.../resourcepack/grass1/textures/items/
pack1_items: $TMP/pack1/en/itemsadder-type/.../resourcepack/pack1/textures/items/
pack1_block: $TMP/pack1/en/itemsadder-type/.../resourcepack/pack1/textures/blocks/
dessert:     $TMP/dessert1/en/itemsadder-type/.../resourcepack/dessert1/textures/items/
stone:       $TMP/stone1/en/nexo-type/.../pack/assets/stone1/textures/
wood:        $TMP/wood1/en/nexo-type/.../pack/assets/wood1/textures/
key:         $TMP/key1/en/itemsadder-type/.../resourcepack/key1/textures/items/
barrel:      $TMP/barrelknight/ItemsAdder/.../resourcepack/assets/cosmetics/
chests:      $TMP/chests_rar2/Chests Pack/assets/chests_pack/
```

대상 리소스팩 루트: `~/dev/poro-server/assets/export/resourcepack/assets/poro/`

---

## A. 바로 넣을 것 (이름 그대로 복사)

파일 내용을 확인했고 이름 변경 없이 경로만 이동하면 된다.

| # | 원본 파일 | 대상 경로 | 설명 | 용도 |
|---|---|---|---|---|
| 1 | `ruby.png` | `textures/item/material/gem/` | 붉은 보석, 16×16 | 반지·장신구 재료, 보스 드랍 |
| 2 | `aquamarine.png` | `textures/item/material/gem/` | 청록색 보석, 16×16 | `mat_crystal_magic` 아이콘 후보 |
| 3 | `topaz.png` | `textures/item/material/gem/` | 황색 보석, 16×16 | 중급 보석 재료 |
| 4 | `raw_silver.png` | `textures/item/material/ore/` | 은 원석, 16×16 | 광산 채굴 드랍 |
| 5 | `silver_ingot.png` | — | ~~은 주괴~~ **폐기** | 은 원석→마도합금 직환으로 대체됨 |
| 6 | `ruby_ore.png` | `textures/block/ore/` | 루비 광석 블럭, 16×16 | 광산 필드 채굴 블럭 |
| 7 | `silver_ore.png` | `textures/block/ore/` | 은 광석 블럭, 16×16 | 광산 주 채굴 블럭 |
| 8 | `topaz_ore.png` | `textures/block/ore/` | 황옥 광석 블럭, 16×16 | 중층 광산 블럭 |
| 9 | `aquamarine_ore.png` | `textures/block/ore/` | 아쿠아마린 광석 블럭, 16×16 | 하수구/지하 필드 블럭 |
| 10 | `raw_flatfish.png` | `textures/item/consumable/food/fish/` | 생 넙치, 16×16 | 강/평야 낚시 드랍 |
| 11 | `stone_fragments.png` | `textures/item/material/stone/` | 돌 파편, 16×16 | 필드 채집 부산물, 제작 기본 재료 |
| 12 | `stone_piece.png` | `textures/item/material/stone/` | 돌 조각, 16×16 | 제작 기본 재료 |
| 13 | `stone_bricks.png` | `textures/item/material/stone/` | 벽돌 조각, 16×16 | 영지 건축 재료 |
| 14 | `iron_key1.png` | `textures/item/dungeon/key/` | 철제 열쇠, 16×16 | 일반 던전 입장 열쇠 |
| 15 | `golden_key1.png` | `textures/item/dungeon/key/` | 황금 열쇠, 16×16 | 고급 던전 입장 열쇠 |

---

## B. 이름만 바꿔서 넣을 것

파일 내용은 그대로이고 Zenon 서버 아이템명으로 파일명을 바꿔서 복사한다.

### B-1. 소비 물약 (potion1.zip)

> Zenon 서버 플러그인의 `MachineItem` 열거형 ID와 1:1로 매핑한다.

| 원본 파일 | 변경 후 파일명 | 대상 경로 | 설명 | 용도 / 플러그인 ID |
|---|---|---|---|---|
| `potion1_red_small.png` | `lesser_healing_potion.png` | `textures/item/consumable/potion/` | 소형 빨간 물약, 16×16 | 하급 회복약 / `con_healing_minor` |
| `potion1_red_middle.png` | `healing_potion.png` | 동일 | 중형 빨간 물약, 16×16 | 중급 회복약 / `con_healing_mid` |
| `potion1_red_large.png` | `greater_healing_potion.png` | 동일 | 대형 빨간 물약, 16×16 | 상급 회복약 / `con_healing_major` |
| `potion1_blue_small.png` | `battle_elixir.png` | 동일 | 소형 파란 물약, 16×16 | 배틀 엘릭서 / `con_battle_elixir` |
| `potion1_blue_large.png` | `lesser_focus_potion.png` | 동일 | 대형 파란 물약, 16×16 | 미래 확장 집중 물약 (1차 예비 확보) |

> **참고**: potion2/potion3 계열(다른 병 디자인 × 18종 추가)은 확장용으로 보류.

### B-2. 약초 (grass1.zip)

> `MachineItem`의 `mat_herb_basic`, `mat_petal_vitality` 외 필드 채집용 약초 아이콘.

| 원본 파일 | 변경 후 파일명 | 대상 경로 | 설명 | 용도 |
|---|---|---|---|---|
| `grass1.png` | `clearleaf_herb.png` | `textures/item/material/herb/` | 녹색 기본 약초, 16×16 | 초원 채집 / `mat_herb_basic` 아이콘 |
| `grass2.png` | `field_medicinal_herb.png` | 동일 | 들판 약초, 16×16 | 평야 채집 재료 |
| `grass5.png` | `life_sprout.png` | 동일 | 생명 새싹, 16×16 | `mat_petal_vitality` 아이콘 후보 |
| `leaf1.png` | `dried_herb_leaf.png` | 동일 | 건조 허브 잎, 16×16 | 공방 제작 부재료 |
| `four_leaf_clover.png` | `lucky_clover.png` | 동일 | 네잎 클로버, 16×16 | 퀘스트 납품 / 행운 아이템 |

> **참고**: grass3/4, leaf2/3은 확장용 보류. 1차는 위 5종만 사용.

### B-3. 광물 — 이름 변경 필수 (리네이밍 주의)

> `lithium` 계열은 현대적 느낌이므로 반드시 판타지 이름으로 교체한다.

| 원본 파일 | 변경 후 파일명 | 대상 경로 | 한글명 | 용도 |
|---|---|---|---|---|
| `lithium_ore.png` (블럭) | `mado_iron_ore.png` | `textures/block/ore/` | 마도철 광석 | 고급 광산 채굴 블럭 |
| `raw_lithium.png` | `raw_mado_iron.png` | `textures/item/material/ore/` | 마도철 원석 | 광물 추출기 생산 / 공방 제련 투입재 |
| `lithium_ingot.png` | `mado_alloy.png` | `textures/item/material/alloy/` | 마도합금 | 공방 제련 결과물 / `mat_mado_alloy` 아이콘 |

### B-4. 음식 — 생선류 (pack1.zip)

| 원본 파일 | 변경 후 파일명 | 대상 경로 | 설명 | 용도 |
|---|---|---|---|---|
| `raw_mullet.png` | `raw_river_fish.png` | `textures/item/consumable/food/fish/` | 생 숭어, 16×16 | 낚시/채집 재료 아이템 |
| `mullet.png` | `grilled_river_fish.png` | 동일 | 구운 숭어, 16×16 | 초반 회복 음식 / 마을 상점 판매 |
| `flatfish.png` | `grilled_flatfish.png` | 동일 | 구운 넙치, 16×16 | 지역 특산 요리 / 마을 상점 판매 |
| `raw_eel.png` | `raw_eel.png` | 동일 | 생 장어 *(이름 유지 가능)*, 16×16 | 하수구 지역 채집 재료 |
| `eel.png` | `grilled_eel.png` | 동일 | 구운 장어, 16×16 | 던전 준비 음식 (체력 회복) |

> **참고**: squid/yellowtail(오징어/방어) 계열 4종은 확장용 보류.

### B-5. 음식 — 디저트 (dessert1.zip)

> 1차에는 서버 톤이 가벼워지지 않도록 **1종만** 넣는 것을 권장한다.

| 원본 파일 | 변경 후 파일명 | 대상 경로 | 설명 | 용도 |
|---|---|---|---|---|
| `chocolate1.png` | `dark_chocolate.png` | `textures/item/consumable/food/dessert/` | 다크 초콜릿, 16×16 | 마을 상점 판매 / 퀘스트 납품 아이템 |

> **참고**: cake1/2, candy1/2, donut, icecream1/2는 확장용 보류.

### B-6. 제작 재료 — 목재 (wood1.zip)

| 원본 파일 | 변경 후 파일명 | 대상 경로 | 설명 | 용도 |
|---|---|---|---|---|
| `wood.png` | `timber.png` | `textures/item/material/wood/` | 가공 목재, 16×16 | `mat_timber_refined` 아이콘 |
| `stump.png` | `small_stump.png` | 동일 | 나무 그루터기, 16×16 | 영지 소품 / 퀘스트 오브젝트 |
| `twisted_wood.png` | `twisted_wood.png` | 동일 | 뒤틀린 목재 *(이름 유지)*, 16×16 | 지하/오염 지역 특산 목재 |
| `stick1.png` | `dry_branch.png` | 동일 | 마른 나뭇가지, 16×16 | 제작 부재료 |
| `sticks.png` | `branch_bundle.png` | 동일 | 나뭇가지 묶음, 16×16 | 효율 재료 묶음 아이템 |

### B-7. 제작 재료 — 석재 (stone1.zip)

| 원본 파일 | 변경 후 파일명 | 대상 경로 | 설명 | 용도 |
|---|---|---|---|---|
| `slate.png` | `slate_piece.png` | `textures/item/material/stone/` | 슬레이트 조각, 16×16 | 중급 제작 재료 |
| `rocks1.png` | `field_rock.png` | 동일 | 들판 돌, 16×16 | 채집 부산물 |

> **참고**: stone1.zip은 Nexo 구조만 있다(ItemsAdder 없음). 복사 경로가 다르다.  
> `$TMP/stone1/en/nexo-type/plugins/Nexo/pack/assets/stone1/textures/`

### B-8. 던전 상자 텍스처 (Chests_Pack RAR)

> RAR 추출 시 `unrar`로만 정상 복구된다. `7z`로 추출하면 `normal_chest.png`가 0바이트가 됨.  
> 복사 전 반드시 `$TMP/chests_rar2/` 경로에서 가져온다.

| 원본 파일 | 변경 후 파일명 | 대상 경로 | 설명 | 용도 |
|---|---|---|---|---|
| `normal_chest.png` (128×128) | `old_supply_chest.png` | `textures/item/dungeon/chest/` | 낡은 나무 상자 | 일반 던전 보상 상자 |
| `medium_chest.png` (128×128) | `sealed_supply_chest.png` | 동일 | 봉인된 상자 | 보스 클리어 보상 상자 |
| `premium_chest.png` (128×128) | `royal_supply_chest.png` | 동일 | 왕실 장식 상자 | 월드보스/레이드 보상 상자 |
| `normal_key.png` (32×32) | `old_key.png` | 동일 | 낡은 열쇠 | 낡은 상자 열쇠 |
| `medium_key.png` (32×32) | `sealed_key.png` | 동일 | 봉인 열쇠 | 봉인된 상자 열쇠 |
| `premium_key.png` (32×32) | `royal_key.png` | 동일 | 왕실 열쇠 | 왕실 상자 열쇠 |

---

## C. 3D 모델 — 블럭화하면 좋은 것

텍스처 + JSON 모델이 모두 있다. JSON에 `"parent"` 없이 `"elements"` 직접 정의된 독립 모델이므로  
**namespace 교체(sed) 후 바로 `item_model`로 등록 가능**하다.

### C-1. BarrelKnight (공통 작업 사항)

> 모든 JSON에서 `"cosmetics:XXX"` → `"poro:item/furniture/XXX"` 로 교체해야 한다.

| 원본 JSON | 원본 텍스처 | 변경 후 모델명 | 변경 후 텍스처명 | 텍스처 크기 | 설명 | 용도 | 비고 |
|---|---|---|---|---|---|---|---|
| `barrel.json` | `barrel.png` | `supply_barrel.json` | `barrel.png` | 64×64 | 나무 통, 18 elements 3D 모델 | 영지 재료 보관통 / 보급 오브젝트 / 상호작용 블럭 | BBmodel 원본 파일도 있어서 Blockbench 수정 가능 |
| `wooden_bucket.json` | `wooden_bucket.png` | `herb_wash_barrel.json` | `wooden_bucket.png` | 32×32 | 나무 양동이, 8 elements | 약초 세척통 비주얼 / 영지 가공기 장식 | 자동재배기 주변 장식 오브젝트로 활용 가능 |
| `wooden_wheel.json` | `wooden_wheel.png` | `wooden_wheel.json` | `wooden_wheel.png` | 64×64 | 수레바퀴, 7 elements | 제분기 장식 / 마인팜 풍차 소품 | `wooden_wheel_blocking.json` 변형도 있음 |
| `pitchfork.json` | `pitchfork.png` | `pitchfork.json` | `pitchfork.png` | — | 쇠스랑, 5 elements | NPC 소품 / 농장 장식 | 기능 블럭보다 장식 아이템 적합 |

**namespace 교체 sed 명령어:**
```bash
DEST=~/dev/poro-server/assets/export/resourcepack/assets/poro

sed -i 's|"cosmetics:barrel"|"poro:item/furniture/barrel"|g'              $DEST/models/item/furniture/supply_barrel.json
sed -i 's|"cosmetics:wooden_bucket"|"poro:item/furniture/wooden_bucket"|g' $DEST/models/item/furniture/herb_wash_barrel.json
sed -i 's|"cosmetics:wooden_wheel"|"poro:item/furniture/wooden_wheel"|g'   $DEST/models/item/furniture/wooden_wheel.json
sed -i 's|"cosmetics:pitchfork"|"poro:item/furniture/pitchfork"|g'         $DEST/models/item/furniture/pitchfork.json
```

### C-2. Chests Pack (공통 작업 사항)

> 모든 JSON에서 `"chests_pack:XXX"` → `"poro:item/dungeon/chest/XXX"` 로 교체해야 한다.  
> **주의**: `normal_chest.png`는 unrar 추출본(`chests_rar2/`)만 정상. 7z 추출본은 0바이트.

| 원본 JSON | 원본 텍스처 | 변경 후 모델명 | 변경 후 텍스처명 | 텍스처 크기 | 설명 | 용도 |
|---|---|---|---|---|---|---|
| `normal_chest.json` | `normal_chest.png` | `old_supply_chest.json` | `old_supply_chest.png` | 128×128 | 낡은 나무 상자 3D 모델 | 일반 던전 보상 상자 / 필드 보급 상자 |
| `medium_chest.json` | `medium_chest.png` | `sealed_supply_chest.json` | `sealed_supply_chest.png` | 128×128 | 봉인된 금속 상자 3D 모델 | 보스 클리어 보상 상자 |
| `premium_chest.json` | `premium_chest.png` | `royal_supply_chest.json` | `royal_supply_chest.png` | 128×128 | 왕실 장식 상자 3D 모델 | 월드보스/레이드 보상 상자 |
| `normal_key.json` | `normal_key.png` | `old_key.json` | `old_key.png` | 32×32 | 낡은 열쇠 3D 모델 | 낡은 상자 열쇠 |
| `medium_key.json` | `medium_key.png` | `sealed_key.json` | `sealed_key.png` | 32×32 | 봉인 열쇠 3D 모델 | 봉인된 상자 열쇠 |
| `premium_key.json` | `premium_key.png` | `royal_key.json` | `royal_key.png` | 32×32 | 왕실 열쇠 3D 모델 | 왕실 상자 열쇠 |

**namespace 교체 sed 명령어:**
```bash
DEST=~/dev/poro-server/assets/export/resourcepack/assets/poro

sed -i 's|"chests_pack:normal_chest"|"poro:item/dungeon/chest/old_supply_chest"|g'    $DEST/models/item/dungeon/chest/old_supply_chest.json
sed -i 's|"chests_pack:medium_chest"|"poro:item/dungeon/chest/sealed_supply_chest"|g' $DEST/models/item/dungeon/chest/sealed_supply_chest.json
sed -i 's|"chests_pack:premium_chest"|"poro:item/dungeon/chest/royal_supply_chest"|g' $DEST/models/item/dungeon/chest/royal_supply_chest.json
sed -i 's|"chests_pack:normal_key"|"poro:item/dungeon/chest/old_key"|g'               $DEST/models/item/dungeon/chest/old_key.json
sed -i 's|"chests_pack:medium_key"|"poro:item/dungeon/chest/sealed_key"|g'            $DEST/models/item/dungeon/chest/sealed_key.json
sed -i 's|"chests_pack:premium_key"|"poro:item/dungeon/chest/royal_key"|g'            $DEST/models/item/dungeon/chest/royal_key.json
```

---

## D. 확장용 보류 (나중에 쓸 것)

| 파일 | 종류 | 설명 | 쓸 수 있는 시점 |
|---|---|---|---|
| `potion2_*/potion3_*` (12종) | 물약 텍스처 | 다른 병 디자인의 물약 | 소비 아이템 종류 확장 시 |
| `grass3/4.png`, `leaf2/3.png` | 약초 텍스처 | 추가 약초 디자인 | 2차 지역 약초 추가 시 |
| `raw_squid/squid.png`, `raw_yellowtail/yellowtail.png` | 음식 텍스처 | 오징어/방어 계열 | 해안 지역 콘텐츠 추가 시 |
| `cake1/2, donut, icecream1/2, candy1/2` (dessert1) | 디저트 텍스처 | 고급 음식 | 생활 콘텐츠 추가 시 |
| `deepslate_*_ore.png` (5종) | 블럭 텍스처 | 심층암 변형 광석 | 심층암 지하 던전 추가 시 |
| `fire/thunder/water_book.png`, `*_element.png` (9종) | 마법 아이템 | 원소 마법서·원소 재료 | 원소 속성 시스템 추가 시 |
| `scroll1~4.png` | 스크롤 텍스처 | 스킬/퀘스트 스크롤 | 스크롤 시스템 추가 시 |
| `*_ring.png` (5종: ruby/aquamarine/topaz/emerald/ring) | 반지 텍스처 | 보석 반지 | 악세서리 슬롯 추가 시 |
| `fire/ice/mythic_key1.png` (key1) | 열쇠 텍스처 | 원소/신화 열쇠 | 원소 던전 및 레이드 추가 시 |
| `reforge1` 36종 | 강화석 텍스처 | 색상×크기×단계 강화석 아이콘 | 강화 시스템 비주얼 등록 시 |
| `rune1` 11색 (16×128 애니메이션) | 룬 텍스처 | 애니메이션 스프라이트 (.mcmeta 포함) | 2차 룬 시스템 추가 시 |
| `newyears1` coin_*/fan_*/ceramic_* | 이벤트 텍스처 | 동전·부채·도자기 이벤트 아이콘 | 시즌 이벤트 추가 시 |
| `stick2/3.png`, `woods1/2.png` (wood1) | 목재 텍스처 | 나뭇가지/목재 추가 디자인 | 목재 제작 시스템 확장 시 |
| `stone_brick1/2.png`, `rocks2.png` (stone1) | 석재 텍스처 | 추가 석재 디자인 | 석재 제작 시스템 확장 시 |
| `pitchfork.json` (BarrelKnight) | 3D 모델 | 쇠스랑 장식 소품 | NPC 소품/농장 장식 추가 시 |

---

## E. 비추천 (사용 금지)

| 파일 | 이유 |
|---|---|
| `TechnoPack-lmg2ie.zip` | SF/사이버펑크 톤, 판타지 RPG 분위기와 완전 불일치 |
| `Toffys-Zelda-Pack-ph2tmh.rar` | 젤다 IP 직접 차용, 공개 서버 저작권 리스크 |
| `pack1.zip` 내 `battery1/2, syringe, can1/2, band, bandage, bolak` | 현대/의료 아이콘, 판타지 RPG에 사용 불가 |
| `x64 Spawn Points [FREE].zip` | Axiom Blueprint 파일 (.bp)만 포함, 리소스팩 에셋 아님 |
| `Cudex-1.53.zip` | 큐브형 캐릭터 코스메틱 팩, 1차 생산/아이콘 용도와 무관 |

---

## F. 실제 복사 명령어 (전체 한번에 실행)

> 아래를 복사해서 bash에 붙여넣으면 된다.  
> **전제**: `unrar`로 `chests_rar2/` 폴더가 이미 생성되어 있어야 한다.

```bash
#!/bin/bash
TMP=~/dev/poro-server/tmp/asset_inspect/extracted
DEST=~/dev/poro-server/assets/export/resourcepack/assets/poro

POTION="$TMP/potion1/en/itemsadder-type/plugins/ItemsAdder/contents/weekly_dot/resourcepack/potion1/textures"
GRASS="$TMP/grass1/en/itemsadder-type/plugins/ItemsAdder/contents/weekly_dot/resourcepack/grass1/textures/items"
P1I="$TMP/pack1/en/itemsadder-type/plugins/ItemsAdder/contents/weekly_dot/resourcepack/pack1/textures/items"
P1B="$TMP/pack1/en/itemsadder-type/plugins/ItemsAdder/contents/weekly_dot/resourcepack/pack1/textures/blocks"
DST="$TMP/dessert1/en/itemsadder-type/plugins/ItemsAdder/contents/weekly_dot/resourcepack/dessert1/textures/items"
STN="$TMP/stone1/en/nexo-type/plugins/Nexo/pack/assets/stone1/textures"
WD="$TMP/wood1/en/nexo-type/plugins/Nexo/pack/assets/wood1/textures"
KEY="$TMP/key1/en/itemsadder-type/plugins/ItemsAdder/contents/weekly_dot/resourcepack/key1/textures/items"
BRL="$TMP/barrelknight/ItemsAdder/contents/barrel_knight/resourcepack/assets/cosmetics"
CHT="$TMP/chests_rar2/Chests Pack/assets/chests_pack"

mkdir -p "$DEST/textures/item/consumable/potion"
mkdir -p "$DEST/textures/item/consumable/food/fish"
mkdir -p "$DEST/textures/item/consumable/food/dessert"
mkdir -p "$DEST/textures/item/material/herb"
mkdir -p "$DEST/textures/item/material/ore"
mkdir -p "$DEST/textures/item/material/gem"
mkdir -p "$DEST/textures/item/material/ingot"
mkdir -p "$DEST/textures/item/material/alloy"
mkdir -p "$DEST/textures/item/material/wood"
mkdir -p "$DEST/textures/item/material/stone"
mkdir -p "$DEST/textures/item/dungeon/chest"
mkdir -p "$DEST/textures/item/dungeon/key"
mkdir -p "$DEST/textures/item/furniture"
mkdir -p "$DEST/textures/block/ore"
mkdir -p "$DEST/models/item/furniture"
mkdir -p "$DEST/models/item/dungeon/chest"

# A. 바로 복사
cp "$P1I/ruby.png"          "$DEST/textures/item/material/gem/ruby.png"
cp "$P1I/aquamarine.png"    "$DEST/textures/item/material/gem/aquamarine.png"
cp "$P1I/topaz.png"         "$DEST/textures/item/material/gem/topaz.png"
cp "$P1I/raw_silver.png"    "$DEST/textures/item/material/ore/raw_silver.png"
# silver_ingot.png — 폐기 (은 원석→마도합금 직환으로 대체)
cp "$P1B/ruby_ore.png"      "$DEST/textures/block/ore/ruby_ore.png"
cp "$P1B/silver_ore.png"    "$DEST/textures/block/ore/silver_ore.png"
cp "$P1B/topaz_ore.png"     "$DEST/textures/block/ore/topaz_ore.png"
cp "$P1B/aquamarine_ore.png" "$DEST/textures/block/ore/aquamarine_ore.png"
cp "$P1I/raw_flatfish.png"  "$DEST/textures/item/consumable/food/fish/raw_flatfish.png"
cp "$STN/stone_fragments.png" "$DEST/textures/item/material/stone/stone_fragments.png"
cp "$STN/stone_piece.png"   "$DEST/textures/item/material/stone/stone_piece.png"
cp "$STN/stone_bricks.png"  "$DEST/textures/item/material/stone/stone_bricks.png"
cp "$KEY/iron_key1.png"     "$DEST/textures/item/dungeon/key/iron_dungeon_key.png"
cp "$KEY/golden_key1.png"   "$DEST/textures/item/dungeon/key/golden_dungeon_key.png"

# B. 이름 바꿔서 복사
cp "$POTION/potion1_red_small.png"  "$DEST/textures/item/consumable/potion/lesser_healing_potion.png"
cp "$POTION/potion1_red_middle.png" "$DEST/textures/item/consumable/potion/healing_potion.png"
cp "$POTION/potion1_red_large.png"  "$DEST/textures/item/consumable/potion/greater_healing_potion.png"
cp "$POTION/potion1_blue_small.png" "$DEST/textures/item/consumable/potion/battle_elixir.png"
cp "$POTION/potion1_blue_large.png" "$DEST/textures/item/consumable/potion/lesser_focus_potion.png"

cp "$GRASS/grass1.png"           "$DEST/textures/item/material/herb/clearleaf_herb.png"
cp "$GRASS/grass2.png"           "$DEST/textures/item/material/herb/field_medicinal_herb.png"
cp "$GRASS/grass5.png"           "$DEST/textures/item/material/herb/life_sprout.png"
cp "$GRASS/leaf1.png"            "$DEST/textures/item/material/herb/dried_herb_leaf.png"
cp "$GRASS/four_leaf_clover.png" "$DEST/textures/item/material/herb/lucky_clover.png"

cp "$P1B/lithium_ore.png"   "$DEST/textures/block/ore/mado_iron_ore.png"
cp "$P1I/raw_lithium.png"   "$DEST/textures/item/material/ore/raw_mado_iron.png"
cp "$P1I/lithium_ingot.png" "$DEST/textures/item/material/alloy/mado_alloy.png"

cp "$P1I/raw_mullet.png"   "$DEST/textures/item/consumable/food/fish/raw_river_fish.png"
cp "$P1I/mullet.png"       "$DEST/textures/item/consumable/food/fish/grilled_river_fish.png"
cp "$P1I/flatfish.png"     "$DEST/textures/item/consumable/food/fish/grilled_flatfish.png"
cp "$P1I/raw_eel.png"      "$DEST/textures/item/consumable/food/fish/raw_eel.png"
cp "$P1I/eel.png"          "$DEST/textures/item/consumable/food/fish/grilled_eel.png"

cp "$DST/chocolate1.png"   "$DEST/textures/item/consumable/food/dessert/dark_chocolate.png"

cp "$WD/wood.png"          "$DEST/textures/item/material/wood/timber.png"
cp "$WD/stump.png"         "$DEST/textures/item/material/wood/small_stump.png"
cp "$WD/twisted_wood.png"  "$DEST/textures/item/material/wood/twisted_wood.png"
cp "$WD/stick1.png"        "$DEST/textures/item/material/wood/dry_branch.png"
cp "$WD/sticks.png"        "$DEST/textures/item/material/wood/branch_bundle.png"

cp "$STN/slate.png"        "$DEST/textures/item/material/stone/slate_piece.png"
cp "$STN/rocks1.png"       "$DEST/textures/item/material/stone/field_rock.png"

cp "$CHT/textures/normal_chest.png"  "$DEST/textures/item/dungeon/chest/old_supply_chest.png"
cp "$CHT/textures/medium_chest.png"  "$DEST/textures/item/dungeon/chest/sealed_supply_chest.png"
cp "$CHT/textures/premium_chest.png" "$DEST/textures/item/dungeon/chest/royal_supply_chest.png"
cp "$CHT/textures/normal_key.png"    "$DEST/textures/item/dungeon/chest/old_key.png"
cp "$CHT/textures/medium_key.png"    "$DEST/textures/item/dungeon/chest/sealed_key.png"
cp "$CHT/textures/premium_key.png"   "$DEST/textures/item/dungeon/chest/royal_key.png"

# C. 3D 모델 텍스처 복사
cp "$BRL/textures/barrel.png"        "$DEST/textures/item/furniture/barrel.png"
cp "$BRL/textures/wooden_bucket.png" "$DEST/textures/item/furniture/wooden_bucket.png"
cp "$BRL/textures/wooden_wheel.png"  "$DEST/textures/item/furniture/wooden_wheel.png"
cp "$BRL/textures/pitchfork.png"     "$DEST/textures/item/furniture/pitchfork.png"

# C. 3D 모델 JSON 복사
cp "$BRL/models/barrel.json"        "$DEST/models/item/furniture/supply_barrel.json"
cp "$BRL/models/wooden_bucket.json" "$DEST/models/item/furniture/herb_wash_barrel.json"
cp "$BRL/models/wooden_wheel.json"  "$DEST/models/item/furniture/wooden_wheel.json"
cp "$BRL/models/pitchfork.json"     "$DEST/models/item/furniture/pitchfork.json"

cp "$CHT/models/normal_chest.json"  "$DEST/models/item/dungeon/chest/old_supply_chest.json"
cp "$CHT/models/medium_chest.json"  "$DEST/models/item/dungeon/chest/sealed_supply_chest.json"
cp "$CHT/models/premium_chest.json" "$DEST/models/item/dungeon/chest/royal_supply_chest.json"
cp "$CHT/models/normal_key.json"    "$DEST/models/item/dungeon/chest/old_key.json"
cp "$CHT/models/medium_key.json"    "$DEST/models/item/dungeon/chest/sealed_key.json"
cp "$CHT/models/premium_key.json"   "$DEST/models/item/dungeon/chest/royal_key.json"

# C. namespace 교체
sed -i 's|"cosmetics:barrel"|"poro:item/furniture/barrel"|g'              "$DEST/models/item/furniture/supply_barrel.json"
sed -i 's|"cosmetics:wooden_bucket"|"poro:item/furniture/wooden_bucket"|g' "$DEST/models/item/furniture/herb_wash_barrel.json"
sed -i 's|"cosmetics:wooden_wheel"|"poro:item/furniture/wooden_wheel"|g'   "$DEST/models/item/furniture/wooden_wheel.json"
sed -i 's|"cosmetics:pitchfork"|"poro:item/furniture/pitchfork"|g'         "$DEST/models/item/furniture/pitchfork.json"

sed -i 's|"chests_pack:normal_chest"|"poro:item/dungeon/chest/old_supply_chest"|g'    "$DEST/models/item/dungeon/chest/old_supply_chest.json"
sed -i 's|"chests_pack:medium_chest"|"poro:item/dungeon/chest/sealed_supply_chest"|g' "$DEST/models/item/dungeon/chest/sealed_supply_chest.json"
sed -i 's|"chests_pack:premium_chest"|"poro:item/dungeon/chest/royal_supply_chest"|g' "$DEST/models/item/dungeon/chest/royal_supply_chest.json"
sed -i 's|"chests_pack:normal_key"|"poro:item/dungeon/chest/old_key"|g'               "$DEST/models/item/dungeon/chest/old_key.json"
sed -i 's|"chests_pack:medium_key"|"poro:item/dungeon/chest/sealed_key"|g'            "$DEST/models/item/dungeon/chest/sealed_key.json"
sed -i 's|"chests_pack:premium_key"|"poro:item/dungeon/chest/royal_key"|g'            "$DEST/models/item/dungeon/chest/royal_key.json"

echo "✓ 복사 및 namespace 교체 완료"
```

---

## G. 리소스팩 반영 & 인게임 테스트

```bash
# 리소스팩 Windows .minecraft에 반영 (rsync)
cd ~/dev/poro-server/assets/export/resourcepack
DEST_DEV="/mnt/c/Users/User/AppData/Roaming/.minecraft/resourcepacks/poro-resourcepack-dev"
rsync -av --delete pack.mcmeta assets "$DEST_DEV/"
```

**인게임 등록 순서:**
1. `assets/poro/items/<아이템명>.json` 파일 생성 (item_model 방식)
2. 예시: `lesser_healing_potion.json`
   ```json
   {
     "model": {
       "type": "minecraft:model",
       "model": "poro:item/consumable/potion/lesser_healing_potion"
     }
   }
   ```
3. 인게임 테스트 지급:
   ```
   /minecraft:give @p minecraft:potion[item_model="poro:item/consumable/potion/lesser_healing_potion"]
   ```
4. 리소스팩 재로드: `F3+T`
5. 텍스처 누락 시: 로그에서 `Missing texture` 확인 → namespace/경로 오타 점검

**3D 모델 (barrel/chest) 추가 확인 사항:**
- `display` → `"gui"` / `"ground"` / `"thirdperson_righthand"` 설정이 JSON에 있는지 확인
- 없으면 Blockbench에서 열어서 추가 (BBmodel 원본 파일이 `$TMP/barrelknight/BBcosmetics/` 에 있음)
