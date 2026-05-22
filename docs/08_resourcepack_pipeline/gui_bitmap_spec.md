# GUI 비트맵 배경 스펙 (Custom Font Bitmap)

> 작성일: 2026-05-16
> 방식: Custom Font Bitmap — 인벤토리 타이틀에 커스텀 폰트 문자를 삽입해 PNG 이미지를 GUI 배경으로 렌더링.
> 의존성: 리소스팩 + EmpireRPG 플러그인 (Adventure Component API)

---

## 1. 동작 원리

```
[리소스팩]
  assets/poro/font/gui.json
    └─ bitmap 프로바이더: PNG 파일 → 유니코드 문자 매핑
    └─ space 프로바이더: 커서 위치 조정용 음수 advance 문자

[플러그인]
  Inventory title = Component
    .font(Key.key("poro:gui"))
    .content("")   // 오프셋 char + 배경 char
```

인벤토리 타이틀 텍스트가 렌더링될 때, ``(음수 advance)이 커서를 인벤토리 좌상단으로 이동시키고, ``(배경 char)이 PNG를 그 위치에 그린다. 실제 슬롯 아이템들이 이미지 위에 겹쳐 렌더링되어 버튼처럼 동작한다.

---

## 2. 파일 구조

```
assets/
├── poro/
│   ├── font/
│   │   └── gui.json            ← 폰트 프로바이더 정의
│   └── textures/
│       └── gui/
│           ├── menu_main.png   ← 메인 허브 배경
│           ├── menu_enhance.png
│           ├── menu_potential.png
│           ├── menu_engrave.png
│           ├── menu_territory.png
│           ├── menu_boss.png
│           └── menu_inherit.png

assets/source/gui/              ← Figma 작업 소스 보관 (export 전 원본)
  ├── menu_main.fig / .psd / .png
  └── ...
```

---

## 3. 폰트 파일 스펙 (`poro/font/gui.json`)

```json
{
  "providers": [
    {
      "type": "space",
      "advances": {
        "": -176,
        "": -1,
        "": -2,
        "": -4,
        "": -8,
        "": -16,
        "": -32,
        "": -64,
        "": -128
      }
    },
    {
      "type": "bitmap",
      "file": "poro:textures/gui/menu_main.png",
      "ascent": 116,
      "height": 141,
      "chars": [""]
    },
    {
      "type": "bitmap",
      "file": "poro:textures/gui/menu_enhance.png",
      "ascent": 116,
      "height": 141,
      "chars": [""]
    },
    {
      "type": "bitmap",
      "file": "poro:textures/gui/menu_potential.png",
      "ascent": 116,
      "height": 141,
      "chars": [""]
    },
    {
      "type": "bitmap",
      "file": "poro:textures/gui/menu_engrave.png",
      "ascent": 116,
      "height": 141,
      "chars": [""]
    },
    {
      "type": "bitmap",
      "file": "poro:textures/gui/menu_territory.png",
      "ascent": 116,
      "height": 141,
      "chars": [""]
    },
    {
      "type": "bitmap",
      "file": "poro:textures/gui/menu_boss.png",
      "ascent": 116,
      "height": 141,
      "chars": [""]
    },
    {
      "type": "bitmap",
      "file": "poro:textures/gui/menu_inherit.png",
      "ascent": 116,
      "height": 141,
      "chars": [""]
    }
  ]
}
```

### ascent / height 조정 기준

모든 메뉴는 6행(54슬롯) 인벤토리를 기준으로 설계한다.

| 값 | 의미 | 조정 방법 |
|---|---|---|
| `height: 141` | 이미지가 게임 내 표시될 세로 크기(GUI 픽셀) | 이미지가 너무 크거나 작으면 조정 |
| `ascent: 116` | 베이스라인 위로 올라가는 높이 | 이미지가 위/아래로 밀리면 조정 |

> ⚠ 실제 맞는 값은 인게임 테스트 필수. 위 값은 MC 1.21 54슬롯 기준 참고값이며 ±5px 오차 있음.

---

## 4. PNG 제작 스펙

### 캔버스 크기

| 메뉴 타입 | 권장 소스 해상도 | 실제 렌더 크기(GUI px) | 슬롯 수 |
|---|---|---|---|
| 메인/서브 메뉴 전체 | **256 × 256** | 176 × 141 | 54 (6×9) |

- PNG는 해상도에 무관하게 `height` 값 기준으로 스케일되어 렌더링된다.
- 256×256 PNG를 쓰면 약 1.5× 오버샘플링으로 픽셀 선명도가 유지된다.
- 알파 채널 필수. 슬롯 영역은 투명하게 비워두면 아이템이 그 위에 표시된다.

### 슬롯 그리드 레퍼런스 (GUI 픽셀 기준)

```
인벤토리 내부 패널 기준 좌표 (margin 제외):
  슬롯 크기: 18 × 18 px
  첫 번째 슬롯 시작: x=8, y=18 (상단 타이틀 영역 17px 아래)
  
  행 y좌표: 행1=18, 행2=36, 행3=54, 행4=72, 행5=90, 행6=108
  열 x좌표: 열1=8, 열2=26, 열3=44, 열4=62, 열5=80, 열6=98, 열7=116, 열8=134, 열9=152
```

PNG 제작 시 슬롯 위치를 시각적으로 확인하려면:
1. 176×141 px 기준 가이드라인 생성
2. 각 슬롯 영역(18×18)을 투명 처리 또는 약하게 표시
3. 배경 디자인 후 256×256으로 업스케일 export

---

## 5. 문자 코드 할당표

| 문자 | 용도 | 파일 |
|---|---|---|
| `` | -176px 오프셋 (전체 폭 역행) | — |
| ``~`` | 세밀 오프셋 (-1~-128px) | — |
| `` | 메인 허브 메뉴 배경 | `menu_main.png` |
| `` | 강화 메뉴 배경 | `menu_enhance.png` |
| `` | 잠재능력 큐브 메뉴 배경 | `menu_potential.png` |
| `` | 각인/스킬 선택 메뉴 배경 | `menu_engrave.png` |
| `` | 영지/공방 메뉴 배경 | `menu_territory.png` |
| `` | 보스 입장 메뉴 배경 | `menu_boss.png` |
| `` | 전승 메뉴 배경 | `menu_inherit.png` |
| `` | 장비 패널 배경 | `menu_equipment.png` |

`` 이후는 서브 패널, 팝업, 확인 다이얼로그용으로 예약한다.

---

## 6. 메인 허브 GUI 슬롯 배치

인벤토리: 54슬롯 (6×9), 배경 char: ``

```
행 \ 열   1    2    3    4    5    6    7    8    9
 1        ░    ░    ░    ░    ░    ░    ░    ░    ░    ← 장식 (검은 유리창)
 2        ░    ░    [강화] [잠재] [각인] ░    ░    ░    ░
 3        ░    ░    ░    ░    ░    ░    ░    ░    ░
 4        ░    ░    [영지] [보스] [전승] ░    ░    ░    ░
 5        ░    ░    ░    ░    ░    ░    ░    ░    ░
 6        ░    ░    ░    ░    ░    ░    ░    ░   [닫기]
```

| 슬롯 번호 | 역할 | 아이템 |
|---|---|---|
| 20 | 강화 메뉴 진입 | custom CMD 아이콘 (투명 느낌 + 이름) |
| 21 | 잠재능력 큐브 진입 | custom CMD 아이콘 |
| 22 | 각인/스킬 선택 진입 | custom CMD 아이콘 |
| 38 | 영지/공방 진입 | custom CMD 아이콘 |
| 39 | 보스 입장 진입 | custom CMD 아이콘 |
| 40 | 전승 진입 | custom CMD 아이콘 |
| 53 | 닫기 | custom CMD 아이콘 |
| 나머지 | 장식 | `minecraft:black_stained_glass_pane` (이름 `" "`) |

---

## 7. 플러그인 연동 (EmpireRPG)

```java
// 인벤토리 타이틀에 커스텀 폰트 배경 char 삽입
private Component buildTitle(char backgroundChar) {
    return Component.text()
        .font(Key.key("poro", "gui"))
        //  = -176px 역행, 배경 char가 인벤토리 상단에 정렬됨
        .content("" + backgroundChar)
        .color(NamedTextColor.WHITE)
        .decoration(TextDecoration.ITALIC, false)
        .build();
}

// 메인 허브 열기
Inventory inv = Bukkit.createInventory(null, 54, buildTitle(''));
```

그림자 제거(`shadow: false`)가 필요하면 Adventure의 `ShadowColor.NONE`을 추가한다.

---

## 8. 작업 분담

| 단계 | 담당 | 도구 |
|---|---|---|
| GUI 배경 PNG 디자인 | 사용자 | Figma |
| PNG → source 폴더 저장 | 사용자 | — |
| `gui.json` 폰트 파일 작성 | Claude/Codex | — |
| PNG → export/resourcepack 복사 | Claude/Codex | bash cp |
| 플러그인 인벤토리 열기 코드 | Claude/Codex | Java (EmpireRPG) |
| 인게임 위치 미세 조정 | 사용자 | `ascent` 값 조절 후 재배포 |

---

## 9. 제작 순서

1. **Figma에서 `menu_main.png` 배경 디자인**
   - 176×141 기준 가이드 위에 배경 그래픽 제작
   - 슬롯 클릭 영역(20·21·22·38·39·40)은 투명 또는 살짝 어두운 박스
   - 각 카테고리 레이블/아이콘 이미지 포함
   - 256×256 PNG로 export → `assets/source/gui/menu_main.png` 저장

2. **나머지 서브메뉴 배경 순서대로 제작** (강화 → 잠재 → 각인 → 영지 → 보스 → 전승)

3. **PNG를 export 경로에 복사**
   ```bash
   cp assets/source/gui/menu_main.png \
      assets/export/resourcepack/assets/poro/textures/gui/menu_main.png
   ```

4. **`poro/font/gui.json` 파일 생성** (§3 내용 그대로)

5. **리소스팩 재압축 후 인게임 위치 확인** → `ascent` 미세 조정

6. **플러그인 GUI 핸들러 연결**
