package com.poro.rpg.gui;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.CustomModelDataComponent;

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
}
