# ZenonMonCore 스펙

ZenonMonCore는 Zenon Mon 고유의 서버 시스템을 통제하는 커스텀 Fabric 모드다.

## 역할

Cobblemon은 포켓몬 시스템을 담당한다.
Mega Showdown은 메가 진화와 특수 배틀 메커니즘을 담당한다.
ZenonMonCore는 서버 진행, 보상, 제한, 허브 상호작용을 담당한다.

## 0.1 스코프

- 기본 `/zenonmon` 명령
- 9번 핫바 슬롯 League Pass 아이템
- 우클릭 메뉴 GUI
- 허브 텔레포트
- config 로딩 스캐폴드
- 플레이어 진행 저장 스캐폴드
- 인카운터 티켓 데이터 모델
- 전설 룸 데이터 모델
- 어드민 reload 명령
- 기본 로깅

## 향후 스코프

- 전설 제단
- 사설 인카운터 룸
- 가중 전설 조우 테이블
- 메가/테라 해금 조건
- 짐 클리어 기록
- 배지 시스템
- 리그·챔피언 시스템
- 시즌 기록
- 보상 매니저(Reward Manager)
- 경제 브릿지(Economy Bridge)

## 제안 모듈

- MenuItemManager
- MenuGuiManager
- PlayerProgressManager
- EconomyBridge
- HubInteractionManager
- InstanceRoomManager
- EncounterTicketManager
- LegendaryEncounterManager
- MegaUnlockManager
- GymBadgeManager
- RewardManager
- SeasonManager
- AdminCommandManager
- ConfigManager
- AuditLogManager
