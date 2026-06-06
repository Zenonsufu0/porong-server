# 조우권 텍스처 — 제작 사양 (등급 5 + 컨셉 10)

배지처럼 `paper` + `CustomModelData`로 표시. 여기에 PNG를 넣으면 제가 AltarMenu(전설 제단)에 배선한다.
(텍스처는 Windows에서 제작 = source of truth, WSL은 mirror. 16×16 권장, 투명 배경, 외곽 1px 여백.)

## 공통
- 베이스 형태 **통일**(예: 부적/티켓 또는 룬 오브) 권장.
- 등급 = **테두리 색 + 별 개수**로 위계, 컨셉 = **중앙 문양 + 색**으로 구분.
- 색약 고려해 형태로도 구분되게.

## 등급 5종 (별 개수로 위계)
| 파일명 | 등급 | 색감 | 모티프 | CMD |
|---|---|---|---|---|
| `ticket_rare.png`         | 희귀   | 은회색        | 별 없음/단순 부적 | 82001 |
| `ticket_basic.png`        | 하급   | 청동·청록     | ★                | 82002 |
| `ticket_intermediate.png` | 중급   | 은빛·하늘     | ★★               | 82003 |
| `ticket_advanced.png`     | 상급   | 금빛·보라     | ★★★              | 82004 |
| `ticket_apex.png`         | 최상급 | 홍금·무지개오라 | ★★★★/왕관       | 82005 |

## 컨셉 10종 (중앙 문양 + 색)
| 파일명 | 컨셉 | 색감 | 모티프 | CMD |
|---|---|---|---|---|
| `ticket_sky.png`         | 하늘   | 하늘색+흰  | 날개/구름        | 82011 |
| `ticket_deep_sea.png`    | 심해   | 짙은 파랑  | 물방울/조개      | 82012 |
| `ticket_earth.png`       | 대지   | 황토·갈색  | 산/바위          | 82013 |
| `ticket_time.png`        | 시간   | 남보라     | 모래시계/시계    | 82014 |
| `ticket_space.png`       | 공간   | 검정+별    | 은하/차원문      | 82015 |
| `ticket_reverse.png`     | 반전   | 흑·자주    | 뒤집힌 문양/그림자 | 82016 |
| `ticket_light.png`       | 빛     | 흰·노랑    | 광채/태양        | 82017 |
| `ticket_dragon_king.png` | 용왕   | 진홍·금    | 용비늘/뿔        | 82018 |
| `ticket_guardian.png`    | 수호자 | 청록·금    | 방패/검          | 82019 |
| `ticket_eternity.png`    | 영원   | 오팔·무지개 | 무한대(∞)/알     | 82020 |

## 동작 원리 (배선 예정)
- 베이스 = `minecraft:paper` + `CustomModelData` 위 번호.
- `assets/minecraft/models/item/paper.json`에 override 추가(배지 81001~와 공존).
- `assets/poromon/models/item/ticket_<key>.json` + 위 PNG(`poromon:item/ticket_<key>`).
- AltarMenu 아이콘을 임시 바닐라 → `MenuIcons.iconModel(paper, CMD)`로 전환.

## 넣은 뒤 (알려주시면 처리)
1. paper.json override + ticket 모델 JSON 10+5 생성
2. AltarMenu 아이콘 paper+CMD 전환
3. 서버 + 클라(PoroMon 0.1 Dev) 양쪽 배포 (커스텀 모델은 클라 jar 필요)
