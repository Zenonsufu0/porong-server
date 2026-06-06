package kr.poro.poromoncore.config;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * economy.json 매핑 POJO (economy_design.md §9, 가격 단일 출처).
 * 0.1 범위: 매입가(sellPrices) + 편의 구매가(buyPrices). 야생 보상/핵심 sink 가격은 향후.
 * 기본값 = economy_design §9 앵커(검증된 실 item id). 운영자가 economy.json에서 수동 조정 → reload.
 */
public class EconomyConfig {
    public int configVersion = 1;
    public String currencyDisplay = "골드";
    public long startingBalance = 0L;

    /** 매입가: item id → 골드/개 (광물·농작물·베리). */
    public Map<String, Long> sellPrices = defaultSellPrices();

    /** 편의 구매가: item id → 골드/개 (볼·회복약). */
    public Map<String, Long> buyPrices = defaultBuyPrices();

    private static Map<String, Long> defaultSellPrices() {
        Map<String, Long> m = new LinkedHashMap<>();
        // 광물 (economy_design §9)
        m.put("minecraft:coal", 1L);
        m.put("minecraft:raw_iron", 4L);
        m.put("minecraft:iron_ingot", 5L);
        m.put("minecraft:raw_gold", 7L);
        m.put("minecraft:gold_ingot", 8L);
        m.put("minecraft:diamond", 40L);
        m.put("minecraft:emerald", 30L);
        m.put("minecraft:lapis_lazuli", 3L);
        m.put("minecraft:redstone", 2L);
        // 농작물
        m.put("minecraft:wheat", 1L);
        m.put("minecraft:potato", 1L);
        m.put("minecraft:carrot", 1L);
        m.put("minecraft:beetroot", 1L);
        m.put("minecraft:pumpkin", 2L);
        m.put("minecraft:melon_slice", 1L);
        // 베리/에이프리코트 (cobblemon 실 id 검증: <color>_apricorn)
        m.put("cobblemon:black_apricorn", 3L);
        m.put("cobblemon:blue_apricorn", 3L);
        m.put("cobblemon:green_apricorn", 3L);
        m.put("cobblemon:pink_apricorn", 3L);
        m.put("cobblemon:red_apricorn", 3L);
        m.put("cobblemon:white_apricorn", 3L);
        m.put("cobblemon:yellow_apricorn", 3L);
        return m;
    }

    private static Map<String, Long> defaultBuyPrices() {
        Map<String, Long> m = new LinkedHashMap<>();
        // 볼 계열 (shop_catalog_0.1 §3.1, 실 id 검증)
        m.put("cobblemon:poke_ball", 50L);
        m.put("cobblemon:great_ball", 120L);
        m.put("cobblemon:ultra_ball", 300L);
        m.put("cobblemon:premier_ball", 50L);
        m.put("cobblemon:heal_ball", 120L);
        m.put("cobblemon:net_ball", 120L);
        m.put("cobblemon:dive_ball", 120L);
        m.put("cobblemon:dusk_ball", 120L);
        m.put("cobblemon:quick_ball", 150L);
        m.put("cobblemon:timer_ball", 120L);
        m.put("cobblemon:repeat_ball", 120L);
        m.put("cobblemon:luxury_ball", 200L);
        // 회복약
        m.put("cobblemon:potion", 30L);
        m.put("cobblemon:super_potion", 100L);
        m.put("cobblemon:hyper_potion", 250L);
        m.put("cobblemon:max_potion", 500L);
        m.put("cobblemon:revive", 400L);
        m.put("cobblemon:full_heal", 120L);
        m.put("cobblemon:antidote", 30L);
        m.put("cobblemon:paralyze_heal", 30L);
        m.put("cobblemon:awakening", 30L);
        m.put("cobblemon:burn_heal", 30L);
        m.put("cobblemon:ice_heal", 30L);
        return m;
    }
}
