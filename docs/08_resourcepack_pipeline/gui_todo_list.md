# GUI 제작 전체 체크리스트

> **[STATUS: DRAFT]** — GUI 제작 작업 체크리스트. 공식 리소스팩 기준은 `index.md`와 각 GUI 상세 문서가 우선.

> 작성일: 2026-05-16  
> 목적: 배경 PNG, 아이콘 PNG, 슬롯 배치 설계 상태를 한 곳에서 추적  
> 설계 상세는 각 링크 문서를 참고.

---

## 1. 배경 PNG 제작 목록

> 배경 PNG는 4개뿐. 나머지 GUI는 모두 chest + 유리판/아이콘 구성.  
> 소스: `assets/source/gui/` → export: `assets/export/resourcepack/assets/poro/textures/gui/`

### 54슬롯 배경 — 256×256 PNG, `ascent:116 / height:141`

| 상태 | 파일명 | 유니코드 | 용도 |
|:---:|---|---|---|
| [완료] | `menu_main.png` | `` | 메인 허브 (장비/필드/영지/보스 선택) |
| [완료] | `menu_equipment.png` | `` | 장비 하위 GUI |
| [완료] | `menu_territory.png` | `` | 영지 하위 GUI |
| [완료] | `menu_boss.png` | `` | 보스 하위 GUI |


---

## 2. 아이콘 PNG 제작 목록

> `assets/export/resourcepack/assets/poro/textures/item/gui/`

| 상태 | 파일명 | 용도 | CMD 등록 필요 |
|:---:|---|---|:---:|
| [제외] | `toggle_cosmetic.png` | 바닐라 아이템으로 대체 — `ARMOR_STAND` | — |
| [제외] | `toggle_base.png` | 바닐라 아이템으로 대체 — `IRON_CHESTPLATE` | — |
| [제외] | `toggle_hidden.png` | 바닐라 아이템으로 대체 — `BARRIER` | — |
| [제외] | `hotbar_slot.png` | 바닐라 아이템으로 대체 — `ITEM_FRAME` | — |

> `assets/export/resourcepack/assets/poro/textures/item/armor/`

| 상태 | 파일명 | 용도 |
|:---:|---|---|
| [ ] | `transparent_helmet.png` | 투구 안보이기용 투명 텍스처 |
| [ ] | `transparent_chestplate.png` | 흉갑 안보이기 |
| [ ] | `transparent_leggings.png` | 바지 안보이기 |
| [ ] | `transparent_boots.png` | 신발 안보이기 |

---

## 3. GUI 화면 목록 및 설계 상태

### 3-1. 허브 계층

| 상태 | 화면 | 슬롯 | 설계문서 | 비고 |
|:---:|---|:---:|---|---|
| [설계완료] | 메인 허브 | 54 | `gui_hub_structure.md §2` | 4분할 (장비/영지/보스/탐험) |
| [설계완료] | 장비 서브 허브 | 54 | `gui_hub_structure.md §3` | 공식 배경 `menu_equipment.png` |
| [설계완료] | 영지 서브 허브 | 54 | `gui_hub_structure.md §4` | 공식 배경 `menu_territory.png` |
| [설계완료] | 보스 서브 허브 | 54 | `gui_hub_structure.md §5` | 공식 배경 `menu_boss.png` |
| [설계완료] | 탐험 서브 허브 | 27 | `gui_hub_structure.md §6` | |

### 3-2. 장비 계열

| 상태 | 화면 | 슬롯 | 설계문서 | 비고 |
|:---:|---|:---:|---|---|
| [설계완료] | 장비 패널 (캐릭터) | 54 | `gui_equipment_panel.md` | 장착+치장+토글+스탯배분 |
| [설계완료] | 장비 세부 GUI (외형·재질) | 27 | `gui_equipment_detail.md` | 개별 재질선택·숨김/보이기, 일괄재질선택 포함 |
| [설계완료] | 스탯 배분 GUI | 54 | `gui_stat_allocation.md` | 3트리(치명/특화/인내) -10/-1/+1/+10 버튼, 전체초기화 |
| [설계완료] | 강화 GUI | 45 | `gui_enhancement.md` | 슬롯 배치·흐름 확정. 수치는 economy_numbers_v2.md 참조 |
| [설계완료] | 잠재능력 큐브 GUI | 54 | `gui_potential.md` | 500G 반영. 큐브 동작 방식은 potential_options_v1.md 참조 |
| [설계완료] | 각인 GUI | 27 | `gui_engraving.md` | 무기 전용 각인 2종 선택 구조 확정 |
| [설계완료] | 전승 GUI | 45 | `gui_succession.md` | 기본 0G, 등급/세부스탯전승권 100,000G 반영 (DL-015) |

### 3-3. 영지 계열

| 상태 | 화면 | 슬롯 | 설계문서 | 비고 |
|:---:|---|:---:|---|---|
| [설계완료] | 영지 상태 GUI | 36 | `gui_territory_status.md` | 작위·저장고·시설 18슬롯·승급 버튼 |
| [설계완료] | 창고 GUI | 54 | `gui_storage.md` | 아이템 입출고 |
| [설계완료] | 공방 GUI | 45 | `gui_workshop.md` | 탭(row0)·레시피(row1)·대기열3개(row2)·가공기 페이지네이션 |
| [설계완료] | 시설 관리 GUI | 45 | `gui_crop_management.md` | 재배지(row0-1)·채굴기(row2-3) 2행씩 / 타이머만 표시 |
| [설계완료] | 상점 GUI | 54 | `gui_shop.md` | row0 탭 4종, 판매 서브 GUI 포함, 특수상점 5만G 3종 |
| [설계완료] | 경매장 GUI | 54+27 | `gui_auction.md` | 즉시구매, 3일 평균시세 lore |
| [설계완료] | 영지 이동 GUI | 27 | `gui_territory_move.md` | 내 영지 고정 + 공개 영지 목록·페이지네이션 |
| [설계완료] | 영지 설정 GUI | 36 | `gui_territory_settings.md` | 영지명·방문설정·자동입금·멤버 8명·권한 등급 시스템 |

### 3-4. 보스 계열

| 상태 | 화면 | 슬롯 | 설계문서 | 비고 |
|:---:|---|:---:|---|---|
| [설계완료] | 보스 방 만들기 — Anvil | — | `gui_boss_party.md §1` | 텍스트 입력(제목) |
| [설계완료] | 보스 방 설정 GUI | 54 | `gui_boss_party.md §2` | 보스선택·최소강화·최대인원·개설 |
| [설계완료] | 보스 방 목록 GUI | 54 | `gui_boss_party.md §3` | lore: 제목·보스·인원·최소강화 |
| [설계완료] | 보스 방 내부 GUI | 27 | `gui_boss_party.md §4` | 파티원 상태·강퇴·준비 확인 |
| [설계완료] | 보스 정보 GUI | 54 | `gui_boss_info.md` | CANON 기준 반영 완료 |
| [설계완료] | 클리어 기록 GUI | 27 | `gui_clear_records.md` | 클리어 횟수·최단기록·트로피 |

---

## 4. 슬롯 배치 빠른 참조

### 메인 허브 (54슬롯)
```
행\열  1   2   3   4   5   6   7   8   9
 1    ░   ░   ░   ░   ░   ░   ░   ░   ░
 2    ░  [장비] ░   ░   ░  [영지] ░   ░   ░
 3    ░   ░   ░   ░   ░   ░   ░   ░   ░
 4    ░   ░   ░   ░   ░   ░   ░   ░   ░
 5    ░  [보스] ░   ░   ░  [탐험] ░   ░   ░
 6    ░   ░   ░   ░   ░   ░   ░   ░  [닫기]
슬롯: 장비=10, 영지=14, 보스=37, 탐험=41, 닫기=53
```

### 장비 서브 허브 (54슬롯, menu_equipment.png)
```
     col0  col1  col2  col3  col4  col5  col6  col7  col8
row0 [강화] [강화] [강화] [캐릭] [캐릭] [캐릭] [각인] [각인] [각인]
row1 [강화] [강화] [강화] [캐릭] [캐릭] [캐릭] [각인] [각인] [각인]
row2 [강화] [강화] [강화] [캐릭] [캐릭] [캐릭] [각인] [각인] [각인]
row3 [큐브] [큐브] [큐브] [캐릭] [캐릭] [캐릭] [전승] [전승] [전승]
row4 [큐브] [큐브] [큐브] [캐릭] [캐릭] [캐릭] [전승] [전승] [전승]
row5 [큐브] [큐브] [큐브] [캐릭] [캐릭] [캐릭] [전승] [전승] [전승]
```

### 영지 서브 허브 (54슬롯, 6구역 3×3) — DL-073
```
     col0-2       col3-5       col6-8
row0-2: [이동 3×3]  [상태 3×3]   [창고 3×3]
row3-5: [공방 3×3]  [상점 3×3]   [설정 3×3]
이동=0~2,9~11,18~20 / 상태=3~5,12~14,21~23 / 창고=6~8,15~17,24~26
공방=27~29,36~38,45~47 / 상점=30~32,39~41,48~50 / 설정=33~35,42~44,51~53
```
※ 경매장 → 메인 허브 6번째 구역 / 시설 관리 → 영지 설정 GUI slot17 흡수

### 보스 서브 허브 (54슬롯, menu_boss.png)
```
     col0  col1  col2  col3  [col4]  col5  col6  col7  col8
row1 [파티생성] ×4cols  [-]  [파티목록] ×3cols   ░
row2 [파티생성] ×4cols  [-]  [파티목록] ×3cols   ░
row3 [보스정보] ×4cols  [-]  [클리어기록] ×3cols  ░
row4 [보스정보] ×4cols  [-]  [클리어기록] ×3cols  ░
파티생성=9~12,18~21 / 파티목록=14~16,23~25
보스정보=27~30,36~39 / 클리어기록=32~34,41~43
```

### 탐험 서브 허브 (27슬롯)
```
행\열  1     2     3     4     5     6~9
 2   [필드1][필드2][필드3][필드4][필드5]  ░
 3   [뒤로]
슬롯: 수도외곽=9, 폐광=10, 오염수로=11, 초소=12, 고대성벽=13, 뒤로=18
버튼 lore: 권장 강화구간 / 현재 접속자 수 / 필드 보스 리스폰 타이머
```

### 장비 패널 (54슬롯) — 상세는 `gui_equipment_panel.md`
```
행\열  1    2    3    4    5       6     7   8    9
 2   [무기][치장][토글] ░  [ATK __][기여][-][+] [잔여포인트]
 3   [투구][치장][토글] ░  [DEF __][기여][-][+]   ░
 4   [흉갑][치장][토글] ░  [HP  __][기여][-][+]   ░
 5   [바지][치장][토글] ░  [치명타%%]  ░   ░   ░    ░
 6   [신발][치장][토글][핫바][치명피해%%][보스피해%%][피감%%] ░ [닫기]
```

---

## 5. 접근 제한 정책 요약

| 메뉴 | 전투 중 | 필드 내 | 영지 내 |
|---|:---:|:---:|:---:|
| 강화·잠재·각인·캐릭터·전승 | ❌ | ❌ | ✅ |
| 영지 상태·창고·공방·작물·설정 | ❌ | ❌ | ✅ |
| 영지 이동 | ❌ | ✅ | ✅ |
| 경매장·상점 | ❌ | ✅ | ✅ |
| 탐험 (필드 이동) | ❌ | ✅ | ✅ |
| 보스 매칭·방 이동 | ❌ | ✅ | ✅ |
| 보스 정보·클리어 기록 | ✅ | ✅ | ✅ |

허용 구역 판정: `WorldGuard` 리전 태그 `poro:menu_allowed` (각 영지 내부 적용)

---

## 6. 플러그인 구현 참조 (Java)

```java
// 인벤토리 타이틀에 배경 char 삽입
private Component buildTitle(char backgroundChar) {
    return Component.text()
        .font(Key.key("poro", "gui"))
        .content("" + backgroundChar)   //  역행 오프셋 + 배경 char
        .color(NamedTextColor.WHITE)
        .decoration(TextDecoration.ITALIC, false)
        .build();
}

// 예시: 메인 허브 열기
Inventory inv = Bukkit.createInventory(null, 54, buildTitle(''));
// 장비 서브 허브
Inventory inv = Bukkit.createInventory(null, 27, buildTitle(''));
```

> 전체 스펙은 `gui_bitmap_spec.md §7`, 장비 패널 이벤트 처리는 `gui_equipment_panel.md §6` 참조.
