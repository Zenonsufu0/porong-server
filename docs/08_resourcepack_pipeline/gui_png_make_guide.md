# GUI PNG 수동 제작 가이드

> 작성일: 2026-05-17  
> 투명 갑옷 4종은 자동 생성 완료. 아래는 직접 그려야 하는 PNG 목록과 스펙.

---

## 공통 도구 기준

- **권장**: Figma (슬롯 그리드 정밀 배치), Aseprite (픽셀아트 아이콘)
- 저장 형식: **PNG with alpha** (투명 채널 필수)
- 소스 보관: `assets/source/gui/`
- export 복사: `assets/export/resourcepack/assets/poro/textures/gui/` 또는 `item/gui/`

---

## 1. 배경 PNG — 54슬롯 (8개)

**캔버스: 256 × 256 px**

| 파일명 | export 경로 | 용도 |
|---|---|---|
| `menu_main.png` | `textures/gui/` | 메인 허브 (4분할 배경) |
| `menu_enhance.png` | `textures/gui/` | 강화 메뉴 |
| `menu_potential.png` | `textures/gui/` | 잠재능력 큐브 메뉴 |
| `menu_engrave.png` | `textures/gui/` | 각인/스킬 선택 메뉴 |
| `menu_territory.png` | `textures/gui/` | 영지 GUI |
| `menu_boss.png` | `textures/gui/` | 보스 방 설정·목록 |
| `menu_inherit.png` | `textures/gui/` | 전승 메뉴 |
| `menu_equipment.png` | `textures/gui/` | 장비 패널 |

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

## 2. 배경 PNG — 27슬롯 서브 허브 (4개)

**캔버스: 256 × 256 px**  
> ascent/height는 인게임 확인 후 확정. 현재 추정치: `ascent:52 / height:59`

| 파일명 | export 경로 | 용도 |
|---|---|---|
| `menu_hub_equipment.png` | `textures/gui/` | 장비 서브 허브 |
| `menu_hub_territory.png` | `textures/gui/` | 영지 서브 허브 |
| `menu_hub_boss.png` | `textures/gui/` | 보스 서브 허브 |
| `menu_hub_explore.png` | `textures/gui/` | 탐험 서브 허브 |

### 슬롯 그리드 규칙 (27슬롯 기준)

```
실제 GUI 렌더 영역: 176×77 px (3행)
첫 슬롯 시작점: x=8, y=18
행: 3행 (y = 18, 36, 54)
열: 9열 (x = 8, 26, 44, 62, 80, 98, 116, 134, 152)
```

> 54슬롯 배경과 동일 캔버스(256×256)를 쓰되, 실제 GUI 높이가 절반이므로
> 배경 그래픽을 위쪽 절반에만 그리면 됨. 아래는 투명.

---

## 3. 아이콘 PNG — 외형 토글 + 핫바 (4개)

**캔버스: 16 × 16 px** (일반 아이템 텍스처 크기)  
**export 경로**: `assets/export/resourcepack/assets/poro/textures/item/gui/`  
**CMD 등록 필요** — 각 아이콘은 별도 CustomModelData 번호로 등록.

| 파일명 | 내용 |
|---|---|
| `toggle_cosmetic.png` | 치장 외형 표시 상태 아이콘 (예: 옷 실루엣, 색상 강조) |
| `toggle_base.png` | 원본 장비 외형 표시 상태 아이콘 (예: 갑옷 실루엣, 중립 톤) |
| `toggle_hidden.png` | 안보이기 상태 아이콘 (예: 눈에 X, 반투명 실루엣) |
| `hotbar_slot.png` | 핫바 슬롯 선택 표시 아이콘 (선택된 슬롯 하이라이트 테두리 등) |

> 픽셀아트 스타일 권장 (Minecraft 텍스처와 통일감).  
> toggle 3종은 같은 베이스 아이콘에 색상/표시만 바꿔 세트로 제작 권장.

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
