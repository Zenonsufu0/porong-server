package com.poro.empire.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Set;

/**
 * 영지 서브 허브 GUI (54슬롯).
 * 3×2 격자 — 구역당 3×3=9슬롯, 색상 유리 채움 + 중앙 아이콘.
 *
 *  col0-2        col3-5        col6-8
 *  [이동 3×3]    [상태 3×3]    [창고 3×3]    ← row 0-2
 *  [공방 3×3]    [상점 3×3]    [설정 3×3]    ← row 3-5
 */
public final class TerritoryHubGui {

    // ─── 클릭 구역 (MainHubGui와 동일한 3×3 패턴) ────────────────────
    public static final Set<Integer> ZONE_MOVE     = Set.of(0,1,2,   9,10,11,  18,19,20);
    public static final Set<Integer> ZONE_STATUS   = Set.of(3,4,5,  12,13,14,  21,22,23);
    public static final Set<Integer> ZONE_STORAGE  = Set.of(6,7,8,  15,16,17,  24,25,26);
    public static final Set<Integer> ZONE_WORKSHOP = Set.of(27,28,29, 36,37,38, 45,46,47);
    public static final Set<Integer> ZONE_SHOP     = Set.of(30,31,32, 39,40,41, 48,49,50);
    public static final Set<Integer> ZONE_SETTINGS = Set.of(33,34,35, 42,43,44, 51,52,53);

    private TerritoryHubGui() {}

    public static void open(Player player) {
        Inventory gui = Bukkit.createInventory(null, 54, GuiTitles.TERRITORY_HUB);

        // 준비 중 구역 — 회색
        ItemStack gray = MainHubGui.icon(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
        fillZone(gui, ZONE_MOVE,  gray);
        fillZone(gui, ZONE_SHOP,  gray);
        fillZone(gui, ZONE_SETTINGS, gray);

        // 활성 구역 — 색상 채움
        fillZone(gui, ZONE_STATUS,   MainHubGui.icon(Material.GREEN_STAINED_GLASS_PANE,      " ", List.of()));
        fillZone(gui, ZONE_STORAGE,  MainHubGui.icon(Material.ORANGE_STAINED_GLASS_PANE,     " ", List.of()));
        fillZone(gui, ZONE_WORKSHOP, MainHubGui.icon(Material.LIGHT_BLUE_STAINED_GLASS_PANE, " ", List.of()));

        // 중앙 아이콘
        gui.setItem(10, MainHubGui.icon(Material.ENDER_PEARL,    "§a영지 이동",  List.of("§7내 영지 / 공개 영지 목록", "§8▶ 클릭하여 열기")));
        gui.setItem(13, MainHubGui.icon(Material.TOTEM_OF_UNDYING, "§a영지 상태", List.of("§7작위  ·  기계  ·  편의 설정", "§8▶ 클릭하여 열기")));
        gui.setItem(16, MainHubGui.icon(Material.CHEST,           "§6창고",      List.of("§7보관 재료 확인 및 입금",       "§8▶ 클릭하여 열기")));
        gui.setItem(37, MainHubGui.icon(Material.BLAST_FURNACE,   "§f공방",      List.of("§7재료 가공  ·  정제  ·  제련",  "§8▶ 클릭하여 열기")));
        gui.setItem(40, MainHubGui.icon(Material.EMERALD,         "§a상점",      List.of("§7재료·블럭·치장·특수 구매", "§8▶ 클릭하여 열기")));
        gui.setItem(43, MainHubGui.icon(Material.COMPARATOR,      "§7영지 설정", List.of("§7시설 현황 포함", "§8준비 중")));

        player.openInventory(gui);
    }

    private static void fillZone(Inventory gui, Set<Integer> slots, ItemStack item) {
        for (int slot : slots) gui.setItem(slot, item);
    }
}
