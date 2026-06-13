# 관장 배지 텍스처 — 제작 사양

여기에 **16×16 PNG** 8장을 아래 파일명 그대로 넣으면 인게임 배지로 표시됩니다.
(메모리 규칙: 텍스처는 Windows에서 제작 = source of truth, WSL은 mirror. UV 0–16, atlas 금지, 재질별 분리.)

## 필요한 파일 (이 디렉터리에 저장)

| 파일명 | 관장 | CustomModelData | 권장 색감 |
|---|---|---|---|
| `badge_bug.png`      | 1. 벌레   | 81001 | 연두 |
| `badge_rock.png`     | 2. 바위   | 81002 | 갈색 |
| `badge_electric.png` | 3. 전기   | 81003 | 노랑 |
| `badge_grass.png`    | 4. 풀     | 81004 | 초록 |
| `badge_water.png`    | 5. 물     | 81005 | 하늘 |
| `badge_fire.png`     | 6. 불꽃   | 81006 | 주황 |
| `badge_psychic.png`  | 7. 에스퍼 | 81007 | 분홍 |
| `badge_dragon.png`   | 8. 드래곤 | 81008 | 보라 |

## 규격
- 크기: **16×16** (필요시 32×32도 가능하나 16 권장), PNG, 투명 배경 권장.
- 외곽 1px 여백 두면 인벤토리에서 깔끔.
- 색약 고려해 형태(모양)로도 구분되게.

## 동작 원리 (제가 배선한 부분)
- 베이스 아이템 = `minecraft:paper` + `CustomModelData` 81001~81008.
- `assets/minecraft/models/item/paper.json` 가 CMD별로 `poromon:item/badge_<type>` 모델로 분기.
- `assets/poromon/models/item/badge_<type>.json` 가 위 PNG(`poromon:item/badge_<type>`)를 사용.

## 텍스처 넣은 뒤 남은 작업 (알려주시면 제가 처리)
1. 메뉴 코드(BadgeMenu)를 유리판 → `paper` + CMD 로 전환.
2. 리소스팩 **배포 방식 확정**(클라 인스턴스 자동적용 vs 서버 푸시).
