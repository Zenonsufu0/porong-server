package kr.zenon.rpg.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.CustomModelDataComponent;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.Map;

/**
 * 커스텀 아이템 텍스처 연결 — item_id → CustomModelData(CMD) 매핑 (DL-129 추가#12).
 *
 * <p>리소스팩 {@code assets/minecraft/items/paper.json}이 {@code minecraft:select property=custom_model_data}로
 * CMD 문자열 → poro 모델을 이미 매핑해 둠. 코드는 carrier=PAPER + {@code CustomModelDataComponent.setStrings([cmd])}만
 * 걸면 됨(2D 이펙트와 동일 메커니즘). 리소스팩 재패키징 불필요.</p>
 */
public final class CustomItemModel {
    private CustomItemModel() {}

    public static final Material CARRIER = Material.PAPER;

    /** item_id → paper.json CMD 값. 미등록 item은 매핑 없음(기본 Material 사용). */
    private static final Map<String, Integer> CMD = Map.ofEntries(
            // 정수류
            Map.entry("mat_essence_farmer", 307003),
            Map.entry("mat_essence_miner", 307002),
            Map.entry("mat_essence_nature", 307001),
            Map.entry("mat_essence_imperial", 302002),
            // 약초/광물/합금/강화석
            Map.entry("mat_herb_imperial", 302001),
            Map.entry("mat_refined_herb", 302003),
            Map.entry("res_silver_ore", 302013),       // 은 원석 (raw_silver, DL-129#32)
            Map.entry("res_ore_resonance", 302014),    // 마도철 원석 (raw_mado_iron, DL-129#32)
            Map.entry("mat_mado_alloy", 302016),
            Map.entry("mat_stone_enhance", 303002),
            // 강화 흔적
            Map.entry("mat_trace_star", 308001),
            Map.entry("mat_trace_moon", 308002),
            Map.entry("mat_trace_sun", 308003),
            // 미감정·장비의 흔적
            Map.entry("equip_trace_unidentified", 308101),
            Map.entry("equip_trace_broken", 308102),
            Map.entry("equip_trace_faded", 308103),
            Map.entry("equip_trace_glowing", 308104),
            Map.entry("equip_trace_radiant", 308105),
            Map.entry("equip_trace_brilliant", 308106),
            // 고대흔적
            Map.entry("ancient_trace_faded", 308201),
            Map.entry("ancient_trace_glowing", 308202),
            Map.entry("ancient_trace_radiant", 308203),
            Map.entry("ancient_trace_brilliant", 308204),
            // 치료 포션
            Map.entry("con_heal_minor", 400001),
            Map.entry("con_heal_mid", 400002),
            Map.entry("con_heal_major", 400003),
            // 부스트 포션
            Map.entry("con_potion_gold", 400004),
            Map.entry("con_potion_enhance", 400005),
            Map.entry("con_potion_exp", 400006),
            // 만찬
            Map.entry("con_feast_warrior", 400007),
            Map.entry("con_feast_slayer", 400008),
            Map.entry("con_feast_assassin", 400009),
            Map.entry("con_feast_hunter", 400010)
    );

    /** item_id의 CMD 값. 없으면 null. */
    public static Integer cmd(String itemId) {
        return itemId == null ? null : CMD.get(itemId);
    }

    /** CMD 모델이 없는 커스텀 재료의 표시용 바닐라 아이콘 (DL-129 추가#31). 미등록은 PAPER. */
    private static final Map<String, Material> FALLBACK_ICON = Map.ofEntries(
            Map.entry("mat_battle_shard",  Material.PRISMARINE_SHARD),// 전장의 파편 (텍스쳐 미정 — 폴백)
            Map.entry("mat_cube",          Material.ENDER_EYE),      // 큐브 (통화 — 텍스쳐 미정, 폴백)
            Map.entry("mat_cube_fragment", Material.PRISMARINE_CRYSTALS), // 큐브 조각
            Map.entry("rift_king_heart",   Material.NETHER_STAR),    // 균열왕의 심장
            Map.entry("res_herb_imperial", Material.FERN),           // 제국 약초(가공품)
            Map.entry("res_essence_imperial", Material.GLOW_BERRIES) // 제국 정수(가공품)
    );

    /** 표시용 아이콘 Material — CMD 모델 있으면 CARRIER(PAPER+CMD), 없으면 폴백 또는 PAPER. */
    public static Material iconMaterial(String itemId) {
        if (cmd(itemId) != null) return CARRIER;
        return FALLBACK_ICON.getOrDefault(itemId, CARRIER);
    }

    public static boolean has(String itemId) {
        return cmd(itemId) != null;
    }

    /** ItemStack에 paper.json 매칭용 CMD 문자열 component를 적용(2D 이펙트와 동일 방식). */
    public static void applyModel(ItemStack item, int cmd) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        CustomModelDataComponent comp = meta.getCustomModelDataComponent();
        comp.setStrings(List.of(String.valueOf(cmd)));
        meta.setCustomModelDataComponent(comp);
        item.setItemMeta(meta);
    }

    /** 창고 출금 시 재입금 식별용 item_id PDC 키 (DL-129 추가#29). */
    public static final NamespacedKey STORAGE_ID_KEY = NamespacedKey.fromString("poro:storage_item_id");

    /**
     * 커스텀 재료 item_id를 실물 ItemStack으로 — PAPER + CMD(있으면) + 한글명 + 재입금용 PDC 태그.
     * 창고에서 커스텀 아이템 출금에 사용 (DL-129 추가#29).
     */
    public static ItemStack buildStack(String itemId, int amount) {
        ItemStack item = new ItemStack(iconMaterial(itemId), Math.max(1, amount));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(WorkshopRecipeRegistry.displayName(itemId))
                    .color(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
            java.util.List<String> lore = loreFor(itemId);
            if (!lore.isEmpty()) {
                meta.lore(lore.stream().map(s -> (net.kyori.adventure.text.Component)
                        net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().deserialize(s)
                        .decoration(TextDecoration.ITALIC, false)).toList());
            }
            meta.getPersistentDataContainer().set(STORAGE_ID_KEY, PersistentDataType.STRING, itemId);
            item.setItemMeta(meta);
        }
        Integer cmd = cmd(itemId);
        if (cmd != null) applyModel(item, cmd);
        return item;
    }

    /** 커스텀 재료 용도 설명 lore (정본 — item_grade_substat_v1 §1·gui_succession·gui_enhancement, DL-129#36). */
    public static java.util.List<String> loreFor(String itemId) {
        if (itemId == null) return java.util.List.of();
        // 장비의 흔적 — 전투 드롭, 전승 소스(강화된 장비에 등급·옵션 계승, 소모), 경매 거래
        if (itemId.startsWith("equip_trace_")) {
            String grade = switch (itemId) {
                case "equip_trace_broken"    -> "§f커먼";
                case "equip_trace_faded"     -> "§9레어";
                case "equip_trace_glowing"   -> "§5에픽";
                case "equip_trace_radiant"   -> "§6유니크";
                case "equip_trace_brilliant" -> "§a레전더리";
                default -> "§7-";
            };
            return java.util.List.of("§7──────────", "§f장비의 흔적 §7[" + grade + "§7]",
                    "§7전투에서 드롭되는 장비 조각.",
                    "§7강화한 장비에 이 흔적의 등급·옵션을",
                    "§7전승할 수 있습니다. §8(전승 시 소모)",
                    "§7경매장 거래 가능.");
        }
        // 고대 흔적 — 미감정 흔적과 함께 써 결과 등급 보장
        if (itemId.startsWith("ancient_trace_")) {
            String guar = switch (itemId) {
                case "ancient_trace_faded"     -> "§9레어 이상";
                case "ancient_trace_glowing"   -> "§5에픽 이상";
                case "ancient_trace_radiant"   -> "§6유니크 이상";
                case "ancient_trace_brilliant" -> "§a레전더리 확정";
                default -> "§f상위 등급";
            };
            return java.util.List.of("§7──────────", "§f고대 흔적 §7(감정 보장)",
                    "§d미감정 흔적§7과 함께 사용해",
                    "§7결과 등급을 " + guar + " §7으로 보장합니다.");
        }
        // 강화 흔적 — 10강 이상 강화 시 성공률 보정(소모)
        if (itemId.startsWith("mat_trace_")) {
            String b = switch (itemId) {
                case "mat_trace_star" -> "성공률 ×1.15";
                case "mat_trace_moon" -> "성공률 ×1.25";
                case "mat_trace_sun"  -> "성공률 ×1.30";
                default -> "성공률 상승";
            };
            return java.util.List.of("§7──────────", "§f강화 흔적",
                    "§710강 이상 강화 시 켜서 사용. §f" + b,
                    "§7요구 수량 전량 보유 시 적용·소모.");
        }
        return java.util.List.of();
    }

    /** ItemStack에 박힌 창고 item_id 태그 읽기 (없으면 null). */
    public static String readStorageId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer()
                .get(STORAGE_ID_KEY, PersistentDataType.STRING);
    }
}
