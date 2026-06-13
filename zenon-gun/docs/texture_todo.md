# porongun-core 텍스처 필요 목록 (사용자 직접 제작용)

> 목적: AI가 임의 제작한 텍스처를 **사용자 제작 16×16 픽셀아트**로 교체.
> 현재 재료/코인은 **바닐라 텍스처로 임시 치환**해 둠(아래 표 "임시" 열). 정식 PNG가 오면
> 모델 json의 `layer0`(아이템)·`all`(블록)을 다시 `porongun:item/<id>` / `porongun:block/<id>`로 되돌린다.
>
> 저장 위치:
> - 아이템: `porongun-core/src/main/resources/assets/porongun/textures/item/<id>.png`
> - 블록 : `porongun-core/src/main/resources/assets/porongun/textures/block/<id>.png`
> - 모두 **16×16 PNG, 투명 배경(아이템)** 권장.

## 아이템 (16×16, 투명 배경)

| # | 파일명 | 한글명 | 현재 임시(바닐라) | 정식 컨셉 / 색감 |
|---|---|---|---|---|
| 1 | `tungsten_alloy.png`   | 텅스텐 합금   | iron_ingot      | 은회색 금속 잉곳. 차갑고 단단한 느낌. 소총탄·총 프레임 소재 |
| 2 | `depleted_uranium.png` | 열화우라늄    | emerald         | 어두운 금속 + 방사능(노랑/연두) 포인트나 ☢ 각인. 무겁고 위험한 톤. AP탄 소재 |
| 3 | `military_alloy.png`   | 군용 합금     | netherite_ingot | 카키/올리브 또는 다크 그레이 금속판. 군용 각인·리벳. 비파밍 게이트 재료 |
| 4 | `electronic_part.png`  | 전자 부품     | comparator      | 초록 회로기판 + 금색 단자/칩. 전자 부품 느낌. 비파밍 게이트 재료 |
| 5 | `coin.png`             | 코인(화폐)    | gold_nugget     | 금/황동 동전. 각인·테두리. 한 종(단일 액면) |

## 블록 (16×16, 6면 동일 = cube_all)

| # | 파일명 | 한글명 | 현재 | 정식 컨셉 |
|---|---|---|---|---|
| 6 | `block/core.png` | 거점 코어 | **임의 제작본 유지 중** | 거점의 심장 블록. 옵시디언급 단단함 + 발광/코어 코어다운 중앙 문양. 6면 동일 또는 (후속) 면별 분리 |

- 코어 **아이템** 아이콘은 블록 모델을 그대로 따름(`item/core.json` → `block/core`). 별도 아이템 텍스처 불필요.
- 코어를 면별(위/옆/아래)로 다르게 하고 싶으면 알려주면 모델을 `cube_all` → `cube_bottom_top` 등으로 바꿔준다(텍스처 3장 필요).

## 후속(아직 미구현, 참고용 — 지금 안 그려도 됨)
- 총/탄약 = TaCZ 에셋 사용 예정(자체 텍스처 불요).
- 거점 허브/상점 GUI = 현재 코드로 그린 패널(슬롯칸). 전용 배경 PNG를 쓰고 싶으면 별도 요청.
- 보안칸·무게 유리 레이어 = 코드 오버레이(텍스처 0).
