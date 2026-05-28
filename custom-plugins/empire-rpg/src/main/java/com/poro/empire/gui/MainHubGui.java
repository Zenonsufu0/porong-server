package com.poro.empire.gui;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.Set;

/**
 * 메인 허브 GUI (54슬롯, gui_hub_structure.md §2).
 *
 * 배경 menu_main.png — 4분할 프레임.
 * 구역 내 어느 슬롯 클릭해도 해당 하위 GUI로 이동.
 *
 *      col0  col1  col2  col3  [col4]  col5  col6  col7  col8
 * row0 [장비]×4                [div]         [영지]×4
 * row1 [장비]×4                [div]         [영지]×4
 * row2 [장비]×4                [div]         [영지]×4
 * row3 [보스]×4                [div]         [탐험]×4
 * row4 [보스]×4                [div]         [탐험]×4
 * row5 [보스]×4                [div]         [탐험]×4
 */
public final class MainHubGui {

    // 클릭 구역 (MainHubListener에서 사용)
    public static final Set<Integer> ZONE_EQUIP    = Set.of(
            0,  1,  2,  3,
            9, 10, 11, 12,
            18, 19, 20, 21);
    public static final Set<Integer> ZONE_TERRITORY = Set.of(
            5,  6,  7,  8,
            14, 15, 16, 17,
            23, 24, 25, 26);
    public static final Set<Integer> ZONE_BOSS     = Set.of(
            27, 28, 29, 30,
            36, 37, 38, 39,
            45, 46, 47, 48);
    public static final Set<Integer> ZONE_EXPLORE  = Set.of(
            32, 33, 34, 35,
            41, 42, 43, 44,
            50, 51, 52, 53);

    private MainHubGui() {}

    public static void open(Player player) {
        Inventory gui = Bukkit.createInventory(null, 54, GuiTitles.MAIN_HUB);

        // col4 = PNG 분할선 위치만 명시. 나머지 구역 슬롯은 AIR로 두어 PNG 배경이 보이도록 함.
        // 클릭은 모두 MainHubListener에서 취소하므로 빈 슬롯에 아이템이 이동하지 않는다.
        ItemStack divider = icon(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
        for (int div : new int[]{4, 13, 22, 31, 40, 49}) gui.setItem(div, divider);

        player.openInventory(gui);
    }

    public static void fillZone(Inventory gui, Set<Integer> slots, ItemStack item) {
        for (int slot : slots) gui.setItem(slot, item);
    }

    public static ItemStack icon(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name));
        meta.lore(lore.stream().map(Component::text).toList());
        item.setItemMeta(meta);
        return item;
    }
}
