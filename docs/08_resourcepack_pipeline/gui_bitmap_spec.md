# GUI 비트맵 배경 스펙 (Custom Font Bitmap)

> **[STATUS: REFERENCE]** — Custom Font Bitmap GUI 배경 상세 참조. 공식 리소스팩 기준은 `index.md`가 우선.

> 방식: Custom Font Bitmap — 인벤토리 타이틀에 커스텀 폰트 문자를 삽입해 PNG 이미지를 GUI 배경으로 렌더링.  
> 의존성: 리소스팩 + PoroRPG 플러그인 (Adventure Component API)

---

## 1. 동작 원리

```
[리소스팩]
  assets/poro/font/gui.json
    └─ space 프로바이더: 커서 위치 조정용 음수 advance 문자
    └─ bitmap 프로바이더: PNG 파일 → 유니코드 문자 매핑

[플러그인]
  Inventory title = Component
    .font(Key.key("poro:gui"))
    .content("")   // 오프셋 char + 배경 char
```

인벤토리 타이틀 렌더링 시 ``(음수 advance)이 커서를 좌상단으로 이동시키고, 배경 char가 PNG를 그 위치에 그린다. 슬롯 아이템이 이미지 위에 겹쳐 표시된다.

---

## 2. 파일 구조

배경 PNG 4개(menu_main/equipment/territory/boss.png)는 현행 구현에서 미사용 — 모든 허브 GUI가 색상 유리판 + 중앙 아이콘 방식으로 전환됨(DL-073). 나머지 GUI는 chest + 유리판/바닐라 아이템/lore 구성.

```
assets/source/gui/                     ← Figma 원본 소스
  ├── menu_main.png       ✅ 256×256
  ├── menu_equipment.png  ✅ 256×256
  ├── menu_territory.png  ✅ 256×256
  └── menu_boss.png       ✅ 256×256

assets/export/resourcepack/assets/poro/
├── font/
│   └── gui.json          ← 폰트 프로바이더 (4개 bitmap 등록 완료)
└── textures/gui/
    ├── menu_main.png      ✅ (176×141)
    ├── menu_equipment.png ✅ (256×256)
    ├── menu_territory.png ✅ (256×256)
    └── menu_boss.png      ✅ (256×256)
```

---

## 3. 폰트 파일 스펙 (`poro/font/gui.json`)

실제 gui.json 내용 (확정):

```json
{
  "providers": [
    {
      "type": "space",
      "advances": {
        "": -176,
        "": -1,
        "": -2,
        "": -4,
        "": -8,
        "": -16,
        "": -32,
        "": -64,
        "": -128
      }
    },
    {
      "type": "bitmap",
      "file": "poro:gui/menu_main.png",
      "ascent": 116,
      "height": 141,
      "chars": [""]
    },
    {
      "type": "bitmap",
      "file": "poro:gui/menu_equipment.png",
      "ascent": 116,
      "height": 141,
      "chars": [""]
    },
    {
      "type": "bitmap",
      "file": "poro:gui/menu_territory.png",
      "ascent": 116,
      "height": 141,
      "chars": [""]
    },
    {
      "type": "bitmap",
      "file": "poro:gui/menu_boss.png",
      "ascent": 116,
      "height": 141,
      "chars": [""]
    }
  ]
}
```

| 값 | 의미 |
|---|---|
| `height: 141` | GUI 내 렌더 세로 크기(px) |
| `ascent: 116` | 베이스라인 위로 올라가는 높이 |

> 실제 맞는 값은 인게임 테스트로 미세 조정.

---

## 4. PNG 제작 스펙

| 항목 | 값 |
|---|---|
| 캔버스 해상도 | 256 × 256 px |
| 실제 GUI 렌더 영역 | 176 × 141 px (54슬롯 기준) |
| 슬롯 크기 | 18 × 18 px |
| 첫 슬롯 시작 | x=8, y=18 |
| 알파 채널 | 필수 — 슬롯 영역 투명 처리 |

```
행 y좌표: 행1=18, 행2=36, 행3=54, 행4=72, 행5=90, 행6=108
열 x좌표: 열1=8, 열2=26, 열3=44, 열4=62, 열5=80, 열6=98, 열7=116, 열8=134, 열9=152
```

---

## 5. 문자 코드 할당표

| 문자 | 용도 | 파일 |
|---|---|---|
| `` | -176px 오프셋 (전체 폭 역행) | — |
| ``~`` | 세밀 오프셋 (-1~-128px) | — |
| `` | 메인 허브 배경 *(미사용)* | `menu_main.png` |
| `` | 장비 하위 GUI 배경 *(미사용)* | `menu_equipment.png` |
| `` | 영지 하위 GUI 배경 *(미사용)* | `menu_territory.png` |
| `` | 보스 하위 GUI 배경 *(미사용)* | `menu_boss.png` |

---

## 6. 플러그인 연동 (PoroRPG)

> **[DL-073]** 현행 구현은 PNG 배경 font character를 사용하지 않으며, 모든 허브 GUI는 순수 텍스트 타이틀만 사용한다.

```java
// 현행 구현 — 순수 텍스트 타이틀 (배경 char 없음)
Inventory inv = Bukkit.createInventory(null, 54, Component.text("제국의 거점"));
Inventory inv = Bukkit.createInventory(null, 54, Component.text("장비 관리"));
Inventory inv = Bukkit.createInventory(null, 54, Component.text("영지 관리"));
Inventory inv = Bukkit.createInventory(null, 54, Component.text("보스 도전"));
```
