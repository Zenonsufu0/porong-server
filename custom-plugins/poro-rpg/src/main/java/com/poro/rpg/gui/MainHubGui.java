package com.poro.rpg.gui;

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
 * 메인 허브 GUI (54슬롯).
 * 3×2 격자 — 구역당 3×3=9슬롯, 색상 유리 채움 + 중앙 아이콘.
 *
 *  col0-2       col3-5       col6-8
 *  [장비 3×3]   [영지 3×3]   [보스 3×3]   ← row 0-2
 *  [탐험 3×3]   [PvP 3×3]    [경매장 3×3] ← row 3-5
 */
public final class MainHubGui {

    // ─── 클릭 구역 ────────────────────────────────────────────────────
    public static final Set<Integer> ZONE_EQUIP     = Set.of(0,1,2,   9,10,11,  18,19,20);
    public static final Set<Integer> ZONE_TERRITORY = Set.of(3,4,5,  12,13,14,  21,22,23);
    public static final Set<Integer> ZONE_BOSS      = Set.of(6,7,8,  15,16,17,  24,25,26);
    public static final Set<Integer> ZONE_EXPLORE   = Set.of(27,28,29, 36,37,38, 45,46,47);
    public static final Set<Integer> ZONE_PVP       = Set.of(30,31,32, 39,40,41, 48,49,50);
    public static final Set<Integer> ZONE_AUCTION   = Set.of(33,34,35, 42,43,44, 51,52,53);

    private MainHubGui() {}

    public static void open(Player player) {
        Inventory gui = Bukkit.createInventory(null, 54, GuiTitles.MAIN_HUB);

        fillZone(gui, ZONE_EQUIP,     icon(Material.LIGHT_BLUE_STAINED_GLASS_PANE, " ", List.of()));
        fillZone(gui, ZONE_TERRITORY, icon(Material.GREEN_STAINED_GLASS_PANE,       " ", List.of()));
        fillZone(gui, ZONE_BOSS,      icon(Material.RED_STAINED_GLASS_PANE,         " ", List.of()));
        fillZone(gui, ZONE_EXPLORE,   icon(Material.YELLOW_STAINED_GLASS_PANE,      " ", List.of()));
        fillZone(gui, ZONE_PVP,       icon(Material.ORANGE_STAINED_GLASS_PANE,      " ", List.of()));
        fillZone(gui, ZONE_AUCTION,   icon(Material.YELLOW_STAINED_GLASS_PANE,      " ", List.of()));

        // 중앙 아이콘 — row1(slots 9-17) + row4(slots 36-44) 의 각 구역 중심
        gui.setItem(10, icon(Material.NETHERITE_SWORD,      "§b장비 허브",  List.of("§7강화  ·  잠재  ·  전승", "§8▶ 클릭하여 열기")));
        gui.setItem(13, icon(Material.GRASS_BLOCK,           "§a영지 허브",  List.of("§7작위  ·  창고  ·  공방", "§8▶ 클릭하여 열기")));
        gui.setItem(16, icon(Material.WITHER_SKELETON_SKULL, "§c보스 허브",  List.of("§7필드보스  ·  시즌보스",  "§8▶ 클릭하여 열기")));
        gui.setItem(37, icon(Material.FILLED_MAP,            "§e탐험 허브",  List.of("§7필드 이동  ·  던전",     "§8▶ 클릭하여 열기")));
        gui.setItem(40, icon(Material.IRON_SWORD,            "§6PvP 허브",   List.of("§7전투 구역  ·  랭킹",    "§8▶ 클릭하여 열기")));
        gui.setItem(43, icon(Material.GOLD_INGOT,            "§e경매장",     List.of("§7아이템  ·  즉시구매",   "§8▶ 클릭하여 열기")));

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
