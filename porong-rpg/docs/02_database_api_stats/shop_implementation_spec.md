# 상점 구현 스펙 (확정 — 2026-06-06)

> **[STATUS: READY-TO-BUILD]** — 사장님 결정 전부 반영. 다음 작업 = 이 스펙대로 코드/ config 구현.
> 가격 근거: `shop_pricing_proposal_draft.md`(경제 에이전트). 프레임워크: `market/ShopGui.java` + `listener/ShopGuiListener.java`(매수·판매서브GUI·전량판매·인벤→창고차감·골드지급 이미 구현됨).

## 0. 확정 결정 (사장님)
- **판매전용 + 고정가**: 구매가 없이 판매가만 표시, 30% 자동 폐기 → config 고정 `sell-price`(골드 상황 따라 버프/너프 조정용).
- **판매 모드 품목별 차등**:
  - **농작물 = set(64) 판매만** (낱개 불가).
  - **광물 = 좌클릭 낱개 / 우클릭 set(64)**.
  - **전장의 파편(재화 `mat_battle_shard`) = 상점에서 판매**(별도 UI 신설 X), 낱개 단위.
- **씨앗 구매 = 좌클릭 낱개 / 우클릭 set(64)** (기존 매수 메커닉 그대로).
- 보스 골드 0 유지.

## 1. config 스키마 (신설 필드 3개)
`shop.<tab>.<key>`에 추가: `sell-price`(고정 판매단가), `sell-mode`(`unit`|`set`, 기본 unit), `buyable`(기본 price>0), `currency`(재화 id, 있으면 인벤 대신 재화지갑 차감).

```yaml
shop:
  material:
    # ── 씨앗(구매 좌1/우64) ──
    wheat_seeds: { material: WHEAT_SEEDS, price: 10, display-name: "밀 씨앗" }
    beetroot_seeds: { material: BEETROOT_SEEDS, price: 20, display-name: "비트 씨앗" }
    melon_seeds: { material: MELON_SEEDS, price: 30, display-name: "수박 씨앗" }
    pumpkin_seeds: { material: PUMPKIN_SEEDS, price: 30, display-name: "호박 씨앗" }
    # 감자·당근은 씨앗=작물 동일 아이템 → 구매(좌1/우64) + 판매(set) 겸용
    potato: { material: POTATO, price: 15, sell-price: 25, sell-mode: set, display-name: "감자" }
    carrot: { material: CARROT, price: 15, sell-price: 25, sell-mode: set, display-name: "당근" }
    # ── 농작물(set 판매전용) ──
    wheat: { material: WHEAT, sell-price: 25, sell-mode: set, buyable: false, display-name: "밀" }
    melon: { material: MELON_SLICE, sell-price: 20, sell-mode: set, buyable: false, display-name: "수박" }
    pumpkin: { material: PUMPKIN, sell-price: 20, sell-mode: set, buyable: false, display-name: "호박" }
    # (비트루트 = 매입 불가, economy 에이전트 권고 → 미등록)
    # ── 광물(낱개+set 판매전용) ──
    coal: { material: COAL, sell-price: 2, sell-mode: unit, buyable: false, display-name: "석탄" }
    copper: { material: COPPER_INGOT, sell-price: 3, sell-mode: unit, buyable: false, display-name: "구리 주괴" }
    redstone: { material: REDSTONE, sell-price: 4, sell-mode: unit, buyable: false, display-name: "레드스톤" }
    iron: { material: IRON_INGOT, sell-price: 5, sell-mode: unit, buyable: false, display-name: "철 주괴" }
    lapis: { material: LAPIS_LAZULI, sell-price: 6, sell-mode: unit, buyable: false, display-name: "청금석" }
    gold: { material: GOLD_INGOT, sell-price: 10, sell-mode: unit, buyable: false, display-name: "금 주괴" }
    diamond: { material: DIAMOND, sell-price: 18, sell-mode: unit, buyable: false, display-name: "다이아몬드" }   # 보수 시작값
    emerald: { material: EMERALD, sell-price: 22, sell-mode: unit, buyable: false, display-name: "에메랄드" }    # 보수 시작값
    # ── 전장의 파편(재화 판매, 낱개) ──
    battle_shard: { material: PRISMARINE_SHARD, sell-price: 3, sell-mode: unit, currency: mat_battle_shard, buyable: false, display-name: "전장의 파편" }
```
> 블럭 42종(gui_shop §7)·치장·특수권한은 2차 슬라이스(이 스펙엔 미포함).

## 2. 코드 변경 (12지점)
### `market/ShopGui.java`
1. `ShopItem` 레코드에 `long sellPrice, int sellBundle, boolean buyable, String currencyId` 추가.
   - `sellBundleSize() = max(1, sellBundle)` (unit=1 / set=64).
   - `bundleSellPrice() = sellPrice`(>0). `sellable() = sellPrice>0`. `isBuyable() = buyable && price>0`. `isCurrency() = currencyId 비어있지 않음`.
   - **30% 폴백 제거**(unitSellPrice 폐기).
2. `loadInto`: `sell-price`(0)·`sell-mode`("unit"→1/"set"→64)·`buyable`(기본 price>0)·`currency`(null) 읽어 생성.
3. `buyables(tab)`/`sellables(tab)` 필터 메서드 추가.
4. `open()` 메인 그리드 = `buyables(tab)` 렌더(판매전용 작물·광물·파편은 매수 그리드에서 숨김). 빈 탭 "준비 중" 유지.
5. `itemAt(tab,slot)` = `buyables(tab)` 인덱싱(매수 클릭용).
6. `displayItem` = 매수 lore만(판매가 줄 제거 — 판매는 판매GUI에서).
7. `sellDisplayItem` = 모드별 lore: set는 "64개=NG / 보유 X세트", unit는 "1개=NG". `currency`면 보유=재화지갑, 인벤/창고 라벨 대신 "보유: N".
### `listener/ShopGuiListener.java`
8. `renderSellGui` = `sellables(tab)`. 보유량: `currency`면 `growth.currency(id)`, 아니면 인벤+창고. 슬롯↔품목 매핑 sellables 기준.
9. `handleSellClick`: sellables 인덱싱. **수량 = 모드별**:
   - set: 좌클릭 = 64(1set), 우클릭 = 보유 전체 set(64배수 내림), Shift = 동일. 64 미만 잔량 판매 불가.
   - unit: 좌클릭 = 1, 우클릭 = min(64,보유), Shift = 전량.
10. `attemptSell`: `currency`면 재화지갑에서 차감(`min(qty, wallet)` → addCurrency(id, -n)), 아니면 인벤→창고(현행). 골드 = (판매수량 / sellBundleSize) × bundleSellPrice. (unit이면 수량×sellPrice, set이면 set수×sellPrice.)
11. `executeSellAll`: 모드/currency 반영.
12. `handleShopClick` Shift+클릭 즉시판매: 메인 그리드가 이제 buyable 전용이라 → 제거 또는 무시.

## 3. 검증
- 빌드 `BUILD SUCCESSFUL`.
- 인게임: 씨앗 좌1/우64 구매 · 광물 좌1/우64 판매 · 농작물 64단위만 판매 · 전장의 파편 판매 시 재화 차감+골드 · 판매전용 품목이 매수 그리드에 안 뜸.

## 4. 2차 슬라이스(후속)
블럭 42종 + 페이지네이션 · 치장 탭(adventurer/carnivoret/vanquisher/wing_volt, `setCosmeticMaterial` 핸들러) · 특수 탭 권한(island_settings, 50,000G) · "준비 중" 라벨 정리.
