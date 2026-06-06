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

    /** 야생 포켓몬 보상 (economy_design §3): 야생만, 레벨 비례. */
    public PokemonRewards pokemonRewards = new PokemonRewards();

    public static class PokemonRewards {
        public boolean enabled = true;
        public int defeatPerLevel = 2;   // 처치: 레벨 × 2
        public int capturePerLevel = 1;  // 포획: 레벨 × 1
    }

    /** 매입가: item id → 골드/개 (광물·농작물·베리). */
    public Map<String, Long> sellPrices = defaultSellPrices();

    /** 편의 구매가: item id → 골드/개 (볼·회복약). */
    public Map<String, Long> buyPrices = defaultBuyPrices();

    /** 카테고리 상점 품목: 가격 + 배지 게이트(minBadges). */
    public static class ShopEntry {
        public long price;
        public int minBadges = 0;
        public ShopEntry() {}
        public ShopEntry(long price, int minBadges) { this.price = price; this.minBadges = minBadges; }
    }

    /** 성장 상점(이상한사탕·경험사탕·행복알·진화돌·비타민). shop_catalog_0.1 §3.2/3.3. */
    public Map<String, ShopEntry> growthShop = defaultGrowthShop();

    private static Map<String, ShopEntry> defaultGrowthShop() {
        Map<String, ShopEntry> m = new LinkedHashMap<>();
        // 레벨/경험치
        m.put("cobblemon:rare_candy", new ShopEntry(800, 0));
        m.put("cobblemon:exp_candy_xs", new ShopEntry(100, 0));
        m.put("cobblemon:exp_candy_s", new ShopEntry(300, 0));
        m.put("cobblemon:exp_candy_m", new ShopEntry(800, 0));
        m.put("cobblemon:exp_candy_l", new ShopEntry(1500, 0));
        m.put("cobblemon:exp_candy_xl", new ShopEntry(3000, 0));
        m.put("cobblemon:lucky_egg", new ShopEntry(6000, 1));
        // 진화의 돌 (기본/희귀 배지 게이트)
        m.put("cobblemon:fire_stone", new ShopEntry(2000, 0));
        m.put("cobblemon:water_stone", new ShopEntry(2000, 0));
        m.put("cobblemon:thunder_stone", new ShopEntry(2000, 0));
        m.put("cobblemon:leaf_stone", new ShopEntry(2000, 0));
        m.put("cobblemon:ice_stone", new ShopEntry(2500, 2));
        m.put("cobblemon:moon_stone", new ShopEntry(2500, 2));
        m.put("cobblemon:sun_stone", new ShopEntry(2500, 2));
        m.put("cobblemon:shiny_stone", new ShopEntry(3000, 2));
        m.put("cobblemon:dusk_stone", new ShopEntry(3000, 2));
        m.put("cobblemon:dawn_stone", new ShopEntry(3000, 2));
        // EV 비타민
        m.put("cobblemon:hp_up", new ShopEntry(2000, 0));
        m.put("cobblemon:protein", new ShopEntry(2000, 0));
        m.put("cobblemon:iron", new ShopEntry(2000, 0));
        m.put("cobblemon:calcium", new ShopEntry(2000, 0));
        m.put("cobblemon:zinc", new ShopEntry(2000, 0));
        m.put("cobblemon:carbos", new ShopEntry(2000, 0));
        return m;
    }

    /**
     * 전설 제단 해금(등급 tier 1회) — 해당 등급 조우권 사용 선행조건 (결정 031).
     * tier = 풀 type(rare/basic/intermediate/advanced/theme/apex). 큰 골드 + 배지 게이트.
     */
    public Map<String, ShopEntry> altarUnlock = defaultAltarUnlock();

    /** 조우권 사용가(등급 tier, 반복·저가). 게이트는 제단 해금이 담당. */
    public Map<String, Long> ticketUse = defaultTicketUse();

    /** 조우권 사용 시 이로치(샤이니) 출현 확률 % (결정 031). */
    public int shinyChancePercent = 5;

    private static Map<String, ShopEntry> defaultAltarUnlock() {
        Map<String, ShopEntry> m = new LinkedHashMap<>();
        // 일반 등급 = tier 키 (1회 해금)
        m.put("rare", new ShopEntry(5000, 0));
        m.put("basic", new ShopEntry(30000, 4));
        m.put("intermediate", new ShopEntry(50000, 6));
        m.put("advanced", new ShopEntry(80000, 8));
        m.put("apex", new ShopEntry(120000, 8));
        // 컨셉 = poolId 키 (개별 해금, 균일 60k·배지8 — 결정031/A안)
        for (String t : new String[]{"sky", "deep_sea", "earth", "time", "space",
                "reverse", "light", "dragon_king", "guardian", "eternity"}) {
            m.put("theme_" + t + "_pool", new ShopEntry(60000, 8));
        }
        return m;
    }

    private static Map<String, Long> defaultTicketUse() {
        Map<String, Long> m = new LinkedHashMap<>();
        m.put("rare", 500L);
        m.put("basic", 1500L);
        m.put("intermediate", 2500L);
        m.put("theme", 3500L);
        m.put("advanced", 4000L);
        m.put("apex", 6000L);
        return m;
    }

    /** 실전 육성 상점(성격 민트 21 + 특성 캡슐/패치). shop_catalog_0.1 §3.3. */
    public Map<String, ShopEntry> trainingShop = defaultTrainingShop();

    private static Map<String, ShopEntry> defaultTrainingShop() {
        Map<String, ShopEntry> m = new LinkedHashMap<>();
        String[] natures = {"adamant", "bold", "brave", "calm", "careful", "gentle", "hasty",
                "impish", "jolly", "lax", "lonely", "mild", "modest", "naive", "naughty",
                "quiet", "rash", "relaxed", "sassy", "serious", "timid"};
        for (String n : natures) m.put("cobblemon:" + n + "_mint", new ShopEntry(3000, 0));
        m.put("cobblemon:ability_capsule", new ShopEntry(8000, 4));
        m.put("cobblemon:ability_patch", new ShopEntry(15000, 6));
        return m;
    }

    /**
     * 기술머신 상점(마개조): SimpleTMs 전체 TM을 타입 분류 + 검색으로 판매. 결정 033.
     * 가격은 기술 위력으로 자동 등급화(수작업 632줄 불필요). learnset 해제는 simpletms config.
     */
    public TmShopConfig tmShop = new TmShopConfig();

    public static class TmShopConfig {
        public int minBadges = 0;          // 구매 배지 게이트
        public long makeoverStonePrice = 10000; // 마개조 해금석 가격(off-learnset 1회 각인)
        public int makeoverStoneBadges = 4;      // 해금석 구매 배지 게이트
        public long priceStatus = 1000;    // 위력 0 이하(변화기)
        public long priceWeak = 1500;      // 위력 ≤ 60
        public long priceMedium = 2500;    // 위력 61~90
        public long priceStrong = 4000;    // 위력 91~110
        public long pricePremium = 6000;   // 위력 111+

        /** 위력 → 가격 등급. */
        public long priceFor(double power) {
            if (power <= 0) return priceStatus;
            if (power <= 60) return priceWeak;
            if (power <= 90) return priceMedium;
            if (power <= 110) return priceStrong;
            return pricePremium;
        }
    }

    /** 메가 연구소(메가팔찌 + 메가스톤 47). shop_catalog_0.1 §3.6 / mega_tera_unlock. */
    public Map<String, ShopEntry> megaShop = defaultMegaShop();

    private static Map<String, ShopEntry> defaultMegaShop() {
        Map<String, ShopEntry> m = new LinkedHashMap<>();
        m.put("mega_showdown:mega_bracelet", new ShopEntry(20000, 4)); // 앵커
        // 기본 메가스톤 43종 (배지4 게이트, 8,000)
        String[] basic = {
                "abomasite", "absolite", "aerodactylite", "aggronite", "alakazite", "altarianite",
                "ampharosite", "audinite", "banettite", "beedrillite", "blastoisinite", "blazikenite",
                "cameruptite", "diancite", "galladite", "garchompite", "gardevoirite", "gengarite",
                "glalitite", "gyaradosite", "heracronite", "houndoominite", "kangaskhanite", "latiasite",
                "latiosite", "lopunnite", "lucarionite", "manectite", "mawilite", "medichamite",
                "metagrossite", "pidgeotite", "pinsirite", "sablenite", "salamencite", "scizorite",
                "sceptilite", "sharpedonite", "slowbronite", "steelixite", "swampertite", "tyranitarite",
                "venusaurite"
        };
        for (String s : basic) m.put("mega_showdown:" + s, new ShopEntry(8000, 4));
        // 고급 메가스톤(X/Y, 배지6 게이트, 25,000)
        String[] advanced = {"charizardite_x", "charizardite_y", "mewtwonite_x", "mewtwonite_y"};
        for (String s : advanced) m.put("mega_showdown:" + s, new ShopEntry(25000, 6));
        return m;
    }

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
