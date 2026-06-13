# 01. 플러그인 운용 구조

> **[STATUS: DRAFT]** — Citizens 제거 완료 (DL-001). `CANON.md` 참조.

## 핵심 원칙

ZenonRPG가 서버의 핵심 데이터와 로직을 소유한다. 다른 플러그인은 껍데기, 보조, 표시, 월드 관리만 담당한다.

## 유지/추가 플러그인

| 플러그인 | 역할 |
|---|---|
| ZenonRPG | 전투, 장비, 영지, 보스, 보상, DB, API |
| MythicMobs | 몹/보스 스폰, 바닐라 강화형 외형, 기본 연출 |
| IridiumSkyblock | 개인 영지 생성/멤버/보호/방문 |
| Vault | 경제/권한 연동 보조 |
| LuckPerms | 권한 관리 |
| EssentialsX | 운영 편의, 기능 제한 |
| WorldEdit/WorldGuard | 건축/보호 |
| Multiverse-Core | 월드 분리 |
| PlaceholderAPI | 정보 표시 연동 |
| DecentHolograms | 안내 홀로그램 |
| Chunky | 오픈 전 청크 프리젠 |
| spark | 성능 측정 |

## 제거/보류

- BetonQuest 제거
- ItemsAdder/Oraxen/Nexo 보류
- ModelEngine/BetterModel/FMM은 2차 확장 후보
- MMOCore/AuraSkills/RPGItems/Quests 보류

## ZenonRPG 모듈

```text
core, player, combat, item, field, boss, island, collection, gui, integration
```

## 데이터 소유권

DB 원본은 ZenonRPG가 가진다. 웹/디코봇은 API를 통해 조회한다.
