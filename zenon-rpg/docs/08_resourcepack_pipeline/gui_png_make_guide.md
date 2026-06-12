# GUI PNG 수동 제작 가이드

> **[STATUS: REFERENCE]** — GUI PNG 제작 절차 참조. 공식 리소스팩 기준은 `index.md`가 우선.

> 작성일: 2026-05-17  
> 투명 갑옷 4종은 자동 생성 완료. 아래는 직접 그려야 하는 PNG 목록과 스펙.

---

## 공통 도구 기준

- **권장**: Figma (슬롯 그리드 정밀 배치), Aseprite (픽셀아트 아이콘)
- 저장 형식: **PNG with alpha** (투명 채널 필수)
- 소스 보관: `assets/source/gui/`
- export 복사: `assets/export/resourcepack/assets/poro/textures/gui/` 또는 `item/gui/`

---

## 1. 배경 PNG — 54슬롯 (4개)

**캔버스: 256 × 256 px**

| 파일명 | export 경로 | 용도 |
|---|---|---|
| `menu_main.png` | `textures/gui/` | 메인 허브 (현행 미사용 — 색상 유리로 대체, DL-073) |
| `menu_equipment.png` | `textures/gui/` | 장비 하위 GUI (현행 미사용 — 색상 유리로 대체, DL-073) |
| `menu_territory.png` | `textures/gui/` | 영지 하위 GUI (현행 미사용 — 색상 유리로 대체, DL-073) |
| `menu_boss.png` | `textures/gui/` | 보스 하위 GUI (현행 미사용 — 색상 유리로 대체, DL-073) |

### 슬롯 그리드 규칙 (54슬롯 기준)

```
캔버스 256×256 중, 실제 GUI 렌더 영역: 176×141 px
첫 슬롯 시작점: x=8, y=18
슬롯 크기: 18×18 px (슬롯 내부 아이콘 영역: 16×16, 테두리 1px 포함)
열 간격: 18px (x = 8, 26, 44, 62, 80, 98, 116, 134, 152)
행 간격: 18px (y = 18, 36, 54, 72, 90, 108)
```

**슬롯 영역은 반드시 투명하게 비울 것** — 바닐라 인벤토리 슬롯이 위에 렌더됨.

### 폰트 등록 파라미터

| 구분 | 값 |
|---|---|
| ascent | 116 |
| height | 141 |
| 캔버스 | 256×256 |

---

## 2. 27슬롯 서브 허브 배경

27슬롯 전용 배경 PNG는 공식 사용하지 않는다. 탐험 서브 허브와 기타 27슬롯 GUI는 chest + 유리판/바닐라 아이템/lore로 구성한다.

---

## 3. 아이콘 PNG — 외형 토글 + 핫바

**캔버스: 16 × 16 px** (일반 아이템 텍스처 크기)  
**export 경로**: `assets/export/resourcepack/assets/poro/textures/item/gui/`  
외형 토글과 핫바 표시는 별도 PNG/CMD를 만들지 않고 바닐라 아이템으로 대체한다.

| 항목 | 바닐라 대체 |
|---|---|
| 치장 외형 표시 | `ARMOR_STAND` |
| 원본 장비 외형 표시 | `IRON_CHESTPLATE` |
| 안보이기 | `BARRIER` |
| 핫바 슬롯 선택 | `ITEM_FRAME` |

---

## 4. 완료 체크

제작 후 `gui_todo_list.md` 의 `[ ]` 를 `[x]` 로 변경.

### 자동 생성 완료 (건드리지 않아도 됨)

| 파일 | 경로 | 크기 |
|---|---|---|
| `transparent_helmet.png` | `textures/item/armor/` | 16×16 투명 |
| `transparent_chestplate.png` | `textures/item/armor/` | 16×16 투명 |
| `transparent_leggings.png` | `textures/item/armor/` | 16×16 투명 |
| `transparent_boots.png` | `textures/item/armor/` | 16×16 투명 |
