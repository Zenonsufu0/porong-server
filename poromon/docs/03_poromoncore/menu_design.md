# 메뉴 아이템 / GUI 설계 (Menu & GUI Design) — 초안(DRAFT)

> 9번 슬롯 **리그 패스(League Pass)** 아이템 정책 + 우클릭 메뉴 GUI 레이아웃.
> 담당 모듈: `MenuItemManager`(item) · `MenuGuiManager`(menu/client) — `module_structure.md`.
> 설정: `config_structure.md §3 core.json`(`menuItem`/`hub`) · 명령: `commands.md`(`/poromon menu`, `admin pass`).
> 상점 접근 정책: **하이브리드(결정 024)** — 매입·편의=메뉴 GUI(어디서나), 핵심 sink=허브 NPC.
> ⚠️ `TODO`/`후보`는 검증 대상. 좌표·아이템 ID는 추측 금지(미확인 TODO).

---

## 1. 책임 / 원칙
- 메뉴는 **자유 플레이를 방해하지 않는 "허브로 가는 입구 + 자주 쓰는 환금/편의"**다. 핵심 통제 상점은 메뉴에 넣지 않고 허브로 보낸다(결정 024, 설계 철학 = 허브 회귀 유도).
- **모든 상태 변경(골드 차감/지급, 아이템 매매)은 서버 권위 트랜잭션.** 클라 GUI는 표시·입력만, 신뢰하지 않는다(§5).
- 밸런스·가격·레이아웃 토글은 하드코딩 금지 → `core.json`/`economy.json`.

---

## 2. 리그 패스 아이템 정책 (`MenuItemManager`)

`core.json §menuItem` 기준(현 값):

| 항목 | 값 | 설명 |
|---|---|---|
| `enabled` | `true` | 메뉴 아이템 기능 on/off |
| `itemId` | `minecraft:clock` (임시) | 베이스 아이템 — **커스텀 모델/한글명 적용 전 임시**(TODO: 전용 모델·CustomModelData) |
| `displayName` | `리그 패스` | 표시명(한국어). en_us fallback `PoroMon League Pass` |
| `hotbarSlot` | `8` | 0-based → **핫바 9번째 칸** |
| `giveOnFirstJoin` | `true` | 최초 접속 시 지급 |
| `restoreOnJoin` / `restoreOnRespawn` | `true` | 접속·리스폰 시 없으면 복원 |
| `preventDrop` | `true` | 드롭(Q)·죽음 드롭 방지 |
| `lockSlot` | `false` | true면 슬롯 이동 잠금(고정) |

**식별:** 아이템 NBT/Component에 PoroMonCore 태그(예: `poromon:league_pass=1`)로 식별 → 일반 시계와 구분, 복제/위조 방지. 인벤토리 정리 시 1개 초과분은 회수.

**복원 흐름(이벤트 훅):** `ServerPlayConnectionEvents.JOIN` / `ServerPlayerEvents.AFTER_RESPAWN` → 9번 칸에 정품 패스 없으면 재지급(`MenuItemManager.restore()`).

**열기 방법:**
- 패스 **우클릭**(`use` 이벤트) → 메뉴 GUI 오픈.
- 명령 대안 `/poromon menu`(패스 분실/비활성 대비) — `commands.md`.
- 관리 `/poromon admin pass <player>` 재지급.

**잠금 모드:** `lockSlot=true` 시 9번 칸에서 이동·교체 불가(슬롯 클릭 취소). 인벤토리 닫기·핫바 스왑으로 우회되지 않도록 서버 검증.

---

## 3. 메인 메뉴 GUI 레이아웃

컨테이너: **6행 × 9열(54칸) 셰스트형**. 테두리(슬롯 0–8, 45–53 일부)는 회색 유리판 placeholder. 기능 슬롯만 아래 표.

```
        col0    col1    col2    col3    col4     col5    col6    col7    col8
row0  [▓]     [▓]     [▓]     [▓]     (플레이어)[▓]     [▓]     [▓]     [▓]
row1  [▓] [진행도][ 배지 ][짐가이드][리그/챔피언][서버가이드][▓]   [▓]     [▓]
row2  [▓] [허브 TP][야생귀환][▓]    [▓]      [▓]     [▓]     [▓]     [▓]
row3  [▓] [매입소][편의상점][▓]     [▓]      [▓]     [▓]     [▓]     [▓]
row4  [▓] [전설제단][메가연구소][기술머신][알 상점][육성 상점][▓]   [▓]     [▓]
row5  [▓]     [▓]     [▓]     [▓]     [ 닫기 ] [▓]     [▓]     [▓]     [▓]
```

| 슬롯 | 항목 | 종류 | 아이콘 후보 | 동작 |
|---|---|---|---|---|
| 4 | 플레이어 정보 | 조회 | 플레이어 머리 | lore에 이름·**골드 잔액**·배지 수·해금 요약(읽기 전용) |
| 10 | 내 진행도 | 조회 | 책 | `/poromon progress` 내용 GUI 표시(배지/해금/티켓) |
| 11 | 배지 | 조회 | 8종 배지 아이콘 | 짐별 클리어 현황(8칸 서브 GUI 후보) |
| 12 | 짐 가이드 | 조회 | 표지판 | 짐 순서·타입·레벨캡 안내 |
| 13 | 리그/챔피언 정보 | 조회 | 금 사과(후보) | 정규리그·챔피언스리그 일정/규칙 |
| 14 | 서버 가이드 | 조회 | 지도 | 서버 규칙·핵심 정책 안내 |
| 19 | 허브 텔레포트 | 액션 | 엔더펄(후보) | `HubInteractionManager` → `hub.spawn`(core.json) |
| 20 | 야생 귀환 | 액션 | 나침반 | 직전 위치/랜덤 야생 복귀(후보, `teleportCommandEnabled` 게이트) |
| 28 | **매입소** | 상점(메뉴) | 모루/상자 | §4.1 매입 GUI(광물·농작물·베리 → 골드) |
| 29 | **편의 상점** | 상점(메뉴) | 몬스터볼(후보) | §4.2 편의 GUI(볼·회복약 구매) |
| 37 | 전설 제단 안내 | 허브 안내 | 보라 유리 | §4.3 — "허브 전설 제단으로" + 위치/TP |
| 38 | 메가 연구소 안내 | 허브 안내 | 메가 팔찌(후보) | §4.3 |
| 39 | 기술머신 안내 | 허브 안내 | TM 케이스(후보) | §4.3 |
| 40 | 알 상점 안내 | 허브 안내 | 알(후보) | §4.3 |
| 41 | 육성 상점 안내 | 허브 안내 | 경험치 병 | §4.3(성장·실전 육성) |
| 49 | 닫기 | 액션 | 바리어/레드 | GUI 닫기 |

> 핵심 sink 상점(37–41)은 **메뉴에서 직접 거래하지 않는다.** 결정 024에 따라 허브 NPC에서만 거래 → 메뉴는 위치 안내·텔레포트만.

---

## 4. 하위 GUI

### 4.1 매입소 GUI (광물·농작물·베리 → 골드)
- 방식(후보): **인벤토리 일괄 매입** — 플레이어 인벤에서 판매 가능 아이템을 인식, "전체 팔기/카테고리별 팔기" 버튼. 또는 슬롯에 올려 개별 판매.
- 가격: `economy.json §sellPrices`(단일 출처). 판매 불가 아이템은 회색 처리.
- 흐름: 클릭 → 서버에 판매 요청(아이템·수량) → 서버가 인벤 보유 검증 → 차감/골드 지급 → `EconomyBridge`(출처 태그 `sell:ore/crop/berry`, §텔레메트리).

### 4.2 편의 상점 GUI (몬스터볼·회복약 구매)
- 품목: `shop_catalog_0.1.md §3.1`(볼 계열·회복약). 제작 가능 → 편의·저비중(결정/`economy §4`).
- 흐름: 아이템 클릭(수량 선택 후보: shift=묶음) → 서버가 잔액 검증 → 골드 차감 → 아이템 지급(인벤 가득 시 거절/드롭 정책 명시).

### 4.3 허브 시설 안내 (핵심 sink — 거래는 허브 NPC)
- 각 시설(전설 제단·메가 연구소·기술머신·알·육성)을 클릭하면 **안내 패널**: 무엇을 파는지 요약 + **해당 허브 구역으로 텔레포트 버튼**(또는 좌표 안내).
- 실제 매매·해금 판정은 허브 NPC GUI에서(배지·관장 게이트). 메뉴는 거래 트랜잭션을 열지 않는다.

### 4.4 조회 화면 (진행도·배지·가이드)
- 읽기 전용. 서버가 `PlayerProgress`를 조회해 lore/아이템으로 렌더(클라 계산 금지).

---

## 5. 인터랙션 / 네트워크 / 보안
- **서버 권위:** GUI는 `ScreenHandler`(서버측) + 클라 표시. 모든 클릭은 서버 핸들러에서 검증 — 잔액/배지/조건/아이템 보유.
- **거래 트랜잭션:** 잔액 확인 → 차감 → 지급, 중간 실패 시 롤백(`economy_design.md §5`).
- **안티-듀프:** 셰스트 GUI의 플레이어 인벤 클릭/shift-클릭/드래그/hopper 상호작용 차단·검증. placeholder 슬롯 클릭은 취소.
- **레이트 제한:** 매입/구매 연타 디바운스, 비정상 패킷 무시.
- **로깅:** 큰 금액 거래·관리 지급 = `AuditLog`(`logging.*`).

---

## 6. 설정 / 명령 연동
- `core.json §menuItem`(아이템 정책) · `§hub`(텔레포트 목적지) · `§logging`.
- (확장 후보) `core.json §menuGui`: `wildReturnEnabled`, 안내 시설 목록/좌표, 레이아웃 토글 — **0.1 필수 아님**, 필요 시 추가.
- 가격: `economy.json`(매입/편의) 단일 출처 — 메뉴는 값을 직접 들고 있지 않고 참조.
- 명령: `/poromon menu`(오픈) · `/poromon hub` · `/poromon progress` · `/poromon admin pass <player>`(`commands.md`).

---

## 7. 구현 단계
- **0.1 ✅ 구현 완료(2026-06-06)**: 리그 패스 지급/복원/보호 + 메인 메뉴 GUI(허브 텔레포트·진행도 조회). 헤드리스 부팅 검증(로드·core.json 생성·명령 등록·reload·mixin 적용·크래시 없음). **인게임 클릭/우클릭/드롭 검증은 클라 접속 필요(미실시).**
  - 구현물: `config/{CoreConfig,ConfigManager}`(core.json 로드/생성/reload) · `item/MenuItemManager`(생성/식별`poromon_league_pass` NBT/`ensure` 중복회수+지급) · `menu/{LeaguePassMenuHandler,MenuGuiManager}`(6×9 읽기전용 GUI, 클릭 라우팅) · `hub/HubManager`(core.json hub.spawn TP) · `mixin/ServerPlayerEntityMixin`(`dropSelectedItem` Q-드롭 차단) · `PoroMonCore`(JOIN 지급/복원·AFTER_RESPAWN 복원·UseItemCallback 오픈) · `command`(`menu`/`hub`/`admin pass`/`reload` 실동작).
  - 0.1 활성 슬롯: 4(플레이어 정보) · 10(진행도 채팅) · 19(허브 TP) · 49(닫기). 나머지(배지/가이드/상점/시설 안내)는 "준비 중" placeholder.
  - 인게임 검증 완료(2026-06-06, zenonsufu0): 우클릭 메뉴·허브 TP·진행 조회·드롭/이동 잠금.
- **0.2 ✅ 구현 완료(2026-06-06)**: 매입소(28)·편의 상점(29) GUI + `EconomyBridge`(골드) + `economy.json`(매입/구매가 단일 출처). 매입=인벤 판매가능 자동진열·클릭 전부판매·전부팔기 / 편의=볼·회복약 좌1·우8 구매(환불 안전). 범용 `ServerMenuHandler.show()`로 **메뉴 전환 커서 점프 제거**(재오픈 없이 내용 교체). `/poromon admin economy give|set|balance`. 인게임 검증 완료.
- **슬롯20 = 홈(결정 029, 0.1 구현 완료)**: "야생 귀환"(직전 위치) 폐기 → 홈 등록/이동. 5칸(1 무료 + 4 골드 점진 해금 10k/30k/70k/150k), 좌클릭 이동/우클릭 재등록/쉬프트 이름변경, 쿨다운30s+웜업3s 채널링(이동·피격 취소)+카운트다운. `home/{HomeManager,HomeMenu}`·`data/Home`·`util/ChatInputManager`. `/poromon home`.
- **리그 패스 보호 완성**: Q-드롭(`ServerPlayerEntityMixin`)·인벤 이동/드롭(`ScreenHandlerMixin`)·매 틱 슬롯 고정(`enforce`), 취소 후 `syncState()` 재동기화. lockSlot 기본 true.
- **0.3+**: 허브 시설 안내(37–41) + 텔레포트, 배지/짐 가이드 조회, 커스텀 패스 모델(CustomModelData).

---

## 8. 미정 / TODO
1. 리그 패스 **전용 아이템 모델**(CustomModelData) — 임시 `minecraft:clock` 대체.
2. 매입소 방식 확정(인벤 일괄 vs 슬롯 개별) + 판매 가능 아이템 인식 규칙.
3. "야생 귀환" 정확한 목적지 규칙(직전 위치 vs 랜덤) + 악용 방지.
4. 허브 시설 좌표/텔레포트 버튼 — `hub_design.md` 구역 좌표 확정 후.
5. 아이콘용 바닐라/모드 아이템 ID(후보 전부 확인 필요, 추측 금지).

---

## 9. 관련 문서
- 모듈: `module_structure.md`(MenuItemManager/MenuGuiManager) · 설정: `config_structure.md` · 명령: `commands.md`
- 경제/상점: `../04_game_design/economy_design.md §5` · `shop_design.md §5` · `shop_catalog_0.1.md`
- 허브: `../04_game_design/hub_design.md` · 결정: `../00_project/decisions.md`(024) · 한글화: `../05_operations/localization_policy.md`
