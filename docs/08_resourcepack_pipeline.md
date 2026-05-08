# 08. 리소스팩 / 치장 제작 파이프라인

## 1차 스타일

깔끔한 RPG풍. 다크판타지, 고어, 공포 톤은 지양한다.

## 우선순위

1. 재료 아이콘
2. GUI 아이콘
3. 영지/가공기 GUI
4. 마인팜 기계 외형
5. 무기 6종 치장
6. 컬렉션북 GUI

## 제외

- 날개 치장
- 고퀄 보스 모델
- 외부 몬스터 모델

## 역할 분담

- Claude/Claude Design: 구조 설계/검수
- Figma: GUI/아이콘/패널
- GPT 이미지: 컨셉/텍스처 초안
- Blockbench: 모델/UV/display 확인
- Codex: json, 폴더, CustomModelData, registry 연결

## 원칙

무기/기계는 2D~2.5D 중심. 보스 모델은 1차에서 바닐라 강화형으로 처리한다.
