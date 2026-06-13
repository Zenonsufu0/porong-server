package kr.zenon.gun.shop;

import java.util.List;

/**
 * 상점 품목 목록 (M-Currency).
 *
 * 앵커: 🧨화약 1개 = 5코인(economy #2). 고급 총·고급탄·비파밍 재료는 **구매 불가**(게이트),
 * 잉여 재료는 **저가 매입**(환금 밸브). 의료(First Aid)·총(TaCZ)은 해당 모드 로드 시 ID로 추가.
 *
 * ⚠️ MVP는 하드코딩 샘플. economy 전체 가격표는 **config(toml/json)** 로 이전 예정(impl_plan §4).
 */
public final class ShopCatalog {

    public static final List<ShopEntry> ENTRIES = List.of(
            // 소모재 — 구매·매입 (economy 식량 5)
            ShopEntry.of("minecraft:bread", 5, 1),
            ShopEntry.of("minecraft:cooked_beef", 8, 2),
            ShopEntry.of("minecraft:arrow", 2, 0),
            // 곡괭이 레이드 도구 받침(바닐라 제작 본체는 매입만)
            ShopEntry.of("minecraft:iron_pickaxe", 40, 4),
            // 커스텀 재료 — 비파밍은 구매 불가(-1), 매입은 저가(잉여 환금)
            ShopEntry.of("zenongun:tungsten_alloy", 30, 6),   // 파밍 재료=구매 가능
            ShopEntry.of("zenongun:depleted_uranium", -1, 20),
            ShopEntry.of("zenongun:military_alloy", -1, 60),  // 비파밍 게이트=구매 불가
            ShopEntry.of("zenongun:electronic_part", -1, 80)  // 비파밍 게이트=구매 불가
    );

    private ShopCatalog() {}
}
