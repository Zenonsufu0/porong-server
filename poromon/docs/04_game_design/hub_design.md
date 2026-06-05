# Hub Design (허브 설계)

허브는 PoroMon 서버의 중앙 도시이자 **리그 본부 / 엔드게임 서비스 구역**이다.
자유 탐험을 대체하지 않는다 — 짐·해금·레전드·리그 등 *통제 시스템*만 허브에 모은다(`../README.md` §1).

> 보호: 허브 건물은 보호 대상(`../02_server/protection_policy.md`, 결정 012). **월드 스폰 = 중앙 광장**으로 고정.
> 연동: 짐 기록·레전드 룸·메가/테라 해금·텔레포트는 PoroMonCore가 담당(`../03_poromoncore/module_structure.md`).

## 구역 개요
중앙 광장 · 시장 · 체육관 · 연구소 · 전설 제단 · 리그 — 6개 구역.

---

## 1. 중앙 광장 (Central Plaza)
- 최초 스폰 (= 월드 스폰, spawn-protection 중심)
- 포켓몬센터 (Cobblemon 회복)
- 초보 안내 NPC
- 서버 규칙 / 디스코드 안내
- **야생 랜덤 텔레포트 NPC** — 필드로 무작위 송출(탐험 진입점)
- 리그 패스 / 메뉴 안내

**PoroMonCore 연동:** 야생 랜덤 텔레포트·허브 귀환 = `HubInteractionManager`. League Pass 안내 = `MenuItemManager`/`MenuGuiManager`.

## 2. 시장 구역 (Market District)

**상점 접근 = 하이브리드(결정 024).** 골드가 내부 잔액이라 모든 거래는 서버 GUI 트랜잭션.

- **9번 슬롯 메뉴(리그 패스)에서 어디서나** — 물리 판매대 불필요(랜드마크로만 선택 배치):
  - 광물 판매소 / 농작물 판매소 / 베리 판매소 (매입)
  - 몬스터볼 상점 / 회복약 상점 (편의)
- **허브 NPC/판매대 우클릭 (핵심 통제 상점)** — 각 구역에 물리 배치:
  - 성장·실전 육성 상점, 기술머신(SimpleTMs), 알(Eggs) → 시장/연구 구역
  - 메가 연구소(§5 연구소 구역), 전설 제단 매표(§? 전설 제단 구역)
  - 배지·관장 클리어 게이트 판정은 NPC에서.
- **유저 거래소/경매장: 도입 안 함(결정 014).** 직거래는 바닐라/Cobblemon 트레이드로 충분.

**연동:** 매매는 `RewardManager`/`EconomyBridge`(향후) + 9번 메뉴 GUI(매입·편의) / 허브 NPC(핵심 sink). 상세 가격/품목·접근 매핑: `economy_design.md §5/§9`, `shop_design.md §5`.

## 3. 체육관 구역 (Gym District)
- 8개 관장 입장구
- 관장별 타입 문양
- 배지 보상 NPC
- 재도전 NPC
- **관장 클리어 기록은 PoroMonCore가 저장**

**연동:** `GymBadgeManager`(클리어 기록/배지), 저장은 `PlayerProgress.badges`(`../03_poromoncore/database_schema.md`). 배지 → 메가/테라 해금·티켓 구매권 등 진행 게이트. 상세는 `gym_badge_design.md`(TODO).

## 4. 연구소 구역 (Research District)
- 메가팔찌 해금
- 메가스톤 상점
- 테라스탈 해금
- 포켓몬 정보 / 도감 보상 NPC
- 리자몽나이트 X/Y 같은 **고급 메가스톤 해금 관리**

**연동:** `MegaUnlockManager`(해금 게이트, MSD 연동), 저장은 `PlayerProgress.unlocks`. 메가팔찌/스톤은 진행 기반 해금(결정·정책: `mega_tera_unlock.md`). 고급 스톤(리자몽나이트 X/Y)은 후반 해금.

## 5. 전설 제단 구역 (Legendary Altar)
- 허브 지하 또는 별도 신전 느낌의 공간
- 전설 조우권(티켓) 사용 지점
- 클릭 시 **개인 조우방 입장**
- 여러 방 중 **빈 방에 1명만** 입장
- 방 안에서 조우권 사용 시 랜덤 전설 소환
- **다른 유저의 횡취 방지**

**연동:** 핵심 통제 시스템.
- `EncounterTicketManager` — 티켓 검증/소모
- `InstanceRoomManager` — `rooms.json` 풀에서 빈 방 1인 배정, 세션 추적, 만료/정리(`RoomSession`)
- `LegendaryEncounterManager` — 가중 테이블(`legendary.json`)로 랜덤 전설 소환
- 흐름·정합·롤백 주의: `legendary_encounter.md`, `database_schema.md` §5(티켓→룸 트랜잭션). 레이쿠자는 시즌말/특수 취급.

## 6. 리그 구역 (League District)
- 배틀타워 (탑 형식 PvE 도전, **관장 8명/8배지 전원** — 결정 028)
- **배틀 아레나 (친선 포켓몬 대전 전용)** ※ 인간 PvP 아님 — 아래 확정 참고
- 정규리그장 (점수제 래더, 레벨50)
- 챔피언스리그 경기장 (서버 마지막날 토너먼트)
- 챔피언 홀 / 우승자 동상

**연동:** `SeasonManager`(시즌/리그/챔피언 기록), 우승자 동상·챔피언 홀은 시즌 기록 기반. 상세는 `league_season_design.md`(TODO).

> ✅ **확정(결정 011): 아레나는 포켓몬 대전만 허용.** 서버 전역 `pvp=false`이며, "아레나"는 **플레이어 간 Cobblemon 포켓몬 배틀** 전용이다. 바닐라 인간 PvP는 아레나를 포함해 **허브 어디서도 켜지 않는다**(Cobblemon 배틀은 pvp 플래그와 무관). → 용어 혼동 방지를 위해 표지판/문서는 "배틀 아레나(포켓몬 대전)"로 표기하고, "PvP 아레나" 명칭은 지양.

---

## 구역 ↔ PoroMonCore 모듈 매핑(요약)

| 구역 | 주요 모듈 |
|---|---|
| 중앙 광장 | HubInteractionManager, MenuItem/MenuGui |
| 시장 | RewardManager/EconomyBridge(향후) |
| 체육관 | GymBadgeManager |
| 연구소 | MegaUnlockManager |
| 전설 제단 | EncounterTicket / InstanceRoom / LegendaryEncounter |
| 리그 | SeasonManager |

## 미정(TBD)
- 허브 좌표/스폰 위치 확정(→ `config_structure.md` core.json `hub`)
- 거래소/경매장 도입 여부·방식
- 제단 룸 개수·크기(→ rooms.json `instances`)
- 구역별 NPC 구현 방식(Cobblemon NPC / 엔티티 / 표지판+상호작용)

## 관련 문서
- 컨셉: `../README.md` · 결정: `../00_project/decisions.md`(011 PvP, 012 보호)
- 코어 모듈: `../03_poromoncore/module_structure.md` / 설정: `config_structure.md` / 데이터: `database_schema.md`
- 레전드: `legendary_encounter.md` · 메가/테라: `mega_tera_unlock.md`
- 보호: `../02_server/protection_policy.md`
