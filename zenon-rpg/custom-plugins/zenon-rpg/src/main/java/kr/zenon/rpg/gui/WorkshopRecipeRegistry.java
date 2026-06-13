package kr.zenon.rpg.gui;

import kr.zenon.rpg.gui.WorkshopGui.WorkshopTab;
import kr.zenon.rpg.gui.WorkshopRecipe.RecipeMaterial;
import org.bukkit.Material;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** 공방 탭별 레시피 정적 레지스트리 (workshop_crafting_spec.md 기준). */
public final class WorkshopRecipeRegistry {

    private static final Map<WorkshopTab, List<WorkshopRecipe>> RECIPES = new EnumMap<>(WorkshopTab.class);

    static {
        // ─── 영지 제작 ─────────────────────────────────────────────
        RECIPES.put(WorkshopTab.ESTATE, List.of(
            recipe("estate_essence_farmer", "농부의 정수", Material.WHEAT_SEEDS,
                "mat_essence_farmer", 1, 30,
                mat("WHEAT", 192), mat("POTATO", 192), mat("CARROT", 192)),

            recipe("estate_essence_miner", "광부의 정수", Material.RAW_IRON,
                "mat_essence_miner", 1, 45,
                mat("COAL_BLOCK", 64), mat("COPPER_BLOCK", 64), mat("IRON_BLOCK", 32),
                mat("GOLD_BLOCK", 32), mat("DIAMOND_BLOCK", 16),
                mat("REDSTONE_BLOCK", 64), mat("LAPIS_BLOCK", 64), mat("EMERALD_BLOCK", 8)),

            recipe("estate_essence_nature", "자연의 정수", Material.OAK_SAPLING,
                "mat_essence_nature", 1, 15,
                mat("mat_essence_farmer", 1), mat("mat_essence_miner", 1)),

            recipe("estate_trace_unidentified", "미감정 흔적", Material.PAPER,
                "equip_trace_unidentified", 1, 30,
                mat("mat_essence_nature", 1), mat("mat_battle_shard", 512)),

            recipe("estate_ancient_faded", "빛 바랜 고대흔적", Material.FLINT,
                "ancient_trace_faded", 1, 30,
                mat("mat_mado_alloy", 20), mat("DIAMOND_BLOCK", 64), mat("EMERALD_BLOCK", 64)),

            recipe("estate_ancient_glowing", "빛나는 고대흔적", Material.GLOWSTONE_DUST,
                "ancient_trace_glowing", 1, 45,
                mat("mat_mado_alloy", 40), mat("DIAMOND_BLOCK", 128), mat("EMERALD_BLOCK", 128)),

            recipe("estate_ancient_radiant", "눈부신 고대흔적", Material.BLAZE_POWDER,
                "ancient_trace_radiant", 1, 60,
                mat("mat_mado_alloy", 60), mat("DIAMOND_BLOCK", 256), mat("EMERALD_BLOCK", 256)),

            recipe("estate_ancient_brilliant", "찬란한 고대흔적", Material.NETHER_STAR,
                "ancient_trace_brilliant", 1, 90,
                mat("mat_mado_alloy", 80), mat("DIAMOND_BLOCK", 512), mat("EMERALD_BLOCK", 512))
        ));

        // ─── 강화 흔적 ─────────────────────────────────────────────
        RECIPES.put(WorkshopTab.TRACE, List.of(
            recipe("trace_star", "별의 흔적", Material.GOLD_NUGGET,
                "mat_trace_star", 1, 20,
                mat("mat_mado_alloy", 10), mat("DIAMOND_BLOCK", 2), mat("EMERALD_BLOCK", 2)),

            recipe("trace_moon", "달의 흔적", Material.ENDER_PEARL,
                "mat_trace_moon", 1, 30,
                mat("mat_mado_alloy", 20), mat("DIAMOND_BLOCK", 4), mat("EMERALD_BLOCK", 4)),

            recipe("trace_sun", "태양의 흔적", Material.BLAZE_ROD,
                "mat_trace_sun", 1, 45,
                mat("mat_mado_alloy", 40), mat("DIAMOND_BLOCK", 8), mat("EMERALD_BLOCK", 8))
        ));

        // ─── 제련 ──────────────────────────────────────────────────
        RECIPES.put(WorkshopTab.SMELT, List.of(
            recipe("smelt_ore_alloy", "마도합금 (원석)", Material.IRON_INGOT,
                "mat_mado_alloy", 1, 5,
                mat("res_ore_resonance", 3)),

            recipe("smelt_silver_alloy", "마도합금 (은 원석)", Material.GOLD_INGOT,
                "mat_mado_alloy", 3, 5,
                mat("res_silver_ore", 1))
        ));

        // ─── 정제 ──────────────────────────────────────────────────
        RECIPES.put(WorkshopTab.REFINE, List.of(
            recipe("refine_herb", "정제 약초 (약초)", Material.LIME_DYE,
                "mat_refined_herb", 1, 5,
                mat("res_herb_imperial", 3)),

            recipe("refine_herb_essence", "정제 약초 (정수)", Material.GREEN_DYE,
                "mat_refined_herb", 3, 5,
                mat("res_essence_imperial", 1))
        ));

        // ─── 연금술 (치료) ─────────────────────────────────────────
        RECIPES.put(WorkshopTab.ALCHEMY_HEAL, List.of(
            recipe("potion_heal_minor", "치료 포션 (소)", Material.POTION,
                "con_heal_minor", 1, 10,
                mat("mat_refined_herb", 1), mat("WHEAT", 64)),

            recipe("potion_heal_mid", "치료 포션 (중)", Material.POTION,
                "con_heal_mid", 1, 10,
                mat("mat_refined_herb", 2), mat("WHEAT", 64)),

            recipe("potion_heal_major", "치료 포션 (대)", Material.POTION,
                "con_heal_major", 1, 10,
                mat("mat_refined_herb", 3), mat("WHEAT", 64))
        ));

        // ─── 연금술 (부스트) ───────────────────────────────────────
        RECIPES.put(WorkshopTab.ALCHEMY_BOOST, List.of(
            recipe("potion_gold", "골드 부스트 포션", Material.SPLASH_POTION,
                "con_potion_gold", 1, 15,
                mat("mat_essence_farmer", 1), mat("mat_refined_herb", 1),
                mat("IRON_BLOCK", 16), mat("GOLD_BLOCK", 16)),

            recipe("potion_enhance", "강화 부스트 포션", Material.SPLASH_POTION,
                "con_potion_enhance", 1, 15,
                mat("mat_essence_farmer", 1), mat("mat_refined_herb", 1),
                mat("IRON_BLOCK", 16), mat("GOLD_BLOCK", 16)),

            recipe("potion_exp", "경험치 부스트 포션", Material.SPLASH_POTION,
                "con_potion_exp", 1, 15,
                mat("mat_essence_farmer", 1), mat("mat_refined_herb", 1),
                mat("IRON_BLOCK", 16), mat("GOLD_BLOCK", 16))
        ));

        // ─── 요리 ──────────────────────────────────────────────────
        RECIPES.put(WorkshopTab.COOK, List.of(
            recipe("feast_warrior", "전사의 만찬", Material.COOKED_BEEF,
                "con_feast_warrior", 1, 10,
                mat("mat_battle_shard", 64), mat("mat_refined_herb", 1), mat("CARROT", 64)),

            recipe("feast_slayer", "학살자의 만찬", Material.COOKED_PORKCHOP,
                "con_feast_slayer", 1, 10,
                mat("mat_battle_shard", 64), mat("mat_refined_herb", 1), mat("POTATO", 64)),

            recipe("feast_assassin", "암살자의 만찬", Material.BREAD,
                "con_feast_assassin", 1, 10,
                mat("mat_battle_shard", 64), mat("mat_refined_herb", 1), mat("WHEAT", 64)),

            recipe("feast_hunter", "사냥꾼의 만찬", Material.GOLDEN_CARROT,
                "con_feast_hunter", 1, 10,
                mat("mat_battle_shard", 64), mat("mat_refined_herb", 1), mat("CARROT", 64))
        ));

        // ─── 광물 변환 ─────────────────────────────────────────────
        RECIPES.put(WorkshopTab.ORE_CONVERT, List.of(
            recipe("ore_coal_diamond", "다이아몬드 (석탄)", Material.DIAMOND,
                "DIAMOND", 1, 5, mat("COAL", 64)),
            recipe("ore_iron_diamond", "다이아몬드 (철)", Material.DIAMOND,
                "DIAMOND", 1, 5, mat("IRON_INGOT", 32)),
            recipe("ore_gold_diamond", "다이아몬드 (금)", Material.DIAMOND,
                "DIAMOND", 1, 5, mat("GOLD_INGOT", 16)),
            recipe("ore_red_diamond", "다이아몬드 (레드스톤)", Material.DIAMOND,
                "DIAMOND", 1, 5, mat("REDSTONE", 32)),
            recipe("ore_lapis_diamond", "다이아몬드 (청금석)", Material.DIAMOND,
                "DIAMOND", 1, 5, mat("LAPIS_LAZULI", 32)),
            recipe("ore_iron_emerald", "에메랄드 (철)", Material.EMERALD,
                "EMERALD", 1, 5, mat("IRON_INGOT", 48)),
            recipe("ore_gold_emerald", "에메랄드 (금)", Material.EMERALD,
                "EMERALD", 1, 5, mat("GOLD_INGOT", 24)),
            recipe("ore_red_emerald", "에메랄드 (레드스톤)", Material.EMERALD,
                "EMERALD", 1, 5, mat("REDSTONE", 48)),
            recipe("ore_lapis_emerald", "에메랄드 (청금석)", Material.EMERALD,
                "EMERALD", 1, 5, mat("LAPIS_LAZULI", 48))
        ));
    }

    // ─── 조회 API ─────────────────────────────────────────────────

    public static List<WorkshopRecipe> getRecipes(WorkshopTab tab) {
        return RECIPES.getOrDefault(tab, List.of());
    }

    public static Optional<WorkshopRecipe> getById(String recipeId) {
        return RECIPES.values().stream()
                .flatMap(List::stream)
                .filter(r -> r.recipeId().equals(recipeId))
                .findFirst();
    }

    /** 아이템 ID → 한국어 표시명 */
    public static String displayName(String itemId) {
        return switch (itemId.toUpperCase()) {
            case "WHEAT"          -> "밀";
            case "POTATO"         -> "감자";
            case "CARROT"         -> "당근";
            case "COAL"           -> "석탄";
            case "IRON_INGOT"     -> "철 주괴";
            case "GOLD_INGOT"     -> "금 주괴";
            case "REDSTONE"       -> "레드스톤";
            case "LAPIS_LAZULI"   -> "청금석";
            case "DIAMOND"        -> "다이아몬드";
            case "EMERALD"        -> "에메랄드";
            case "COAL_BLOCK"     -> "석탄 블럭";
            case "COPPER_BLOCK"   -> "구리 블럭";
            case "IRON_BLOCK"     -> "철 블럭";
            case "GOLD_BLOCK"     -> "금 블럭";
            case "DIAMOND_BLOCK"  -> "다이아 블럭";
            case "EMERALD_BLOCK"  -> "에메랄드 블럭";
            case "REDSTONE_BLOCK" -> "레드스톤 블럭";
            case "LAPIS_BLOCK"    -> "청금석 블럭";
            default -> switch (itemId.toLowerCase()) {
                case "res_herb_imperial"     -> "제국 약초";
                case "mat_herb_imperial"     -> "제국 약초";
                case "res_essence_imperial"  -> "제국 정수";
                case "mat_essence_imperial"  -> "제국 정수";
                case "res_ore_resonance"     -> "마도철 원석";
                case "res_silver_ore"        -> "은 원석";
                case "mat_battle_shard"      -> "전장의 파편";
                case "mat_refined_herb"      -> "정제 약초";
                case "mat_mado_alloy"        -> "마도합금";
                case "mat_essence_farmer"    -> "농부의 정수";
                case "mat_essence_miner"     -> "광부의 정수";
                case "mat_essence_nature"    -> "자연의 정수";
                case "rift_king_heart"       -> "균열왕의 심장";
                case "mat_stone_enhance"     -> "강화석";
                case "mat_cube"              -> "큐브";
                case "mat_cube_fragment"     -> "큐브 조각";
                case "cosmetic_fragment"     -> "치장 파편 (미사용)"; // 1차 시즌 제외 — 잔존분 표시용
                // 무기 강화 흔적 (item_master 정본명, 커먼→레전더리)
                case "equip_trace_unidentified" -> "미감정 흔적";
                case "equip_trace_broken"    -> "깨진 장비의 흔적";
                case "equip_trace_faded"     -> "빛 바랜 장비의 흔적";
                case "equip_trace_glowing"   -> "빛나는 장비의 흔적";
                case "equip_trace_radiant"   -> "눈부신 장비의 흔적";
                case "equip_trace_brilliant" -> "찬란한 장비의 흔적";
                // 고대 흔적
                case "ancient_trace_faded"     -> "바랜 고대 흔적";
                case "ancient_trace_glowing"   -> "빛나는 고대 흔적";
                case "ancient_trace_radiant"   -> "찬란한 고대 흔적";
                case "ancient_trace_brilliant" -> "눈부신 고대 흔적";
                // 잠재능력 흔적
                case "mat_trace_star"        -> "별의 흔적";
                case "mat_trace_moon"        -> "달의 흔적";
                case "mat_trace_sun"         -> "태양의 흔적";
                default -> itemId;
            };
        };
    }

    // ─── 빌더 헬퍼 ───────────────────────────────────────────────

    @SafeVarargs
    private static WorkshopRecipe recipe(String id, String name, Material icon,
                                          String resultId, int amount, int duration,
                                          RecipeMaterial... materials) {
        return new WorkshopRecipe(id, name, icon, resultId, amount, duration, List.of(materials));
    }

    private static RecipeMaterial mat(String itemId, long amount) {
        return new RecipeMaterial(itemId, amount);
    }

    private WorkshopRecipeRegistry() {}
}
