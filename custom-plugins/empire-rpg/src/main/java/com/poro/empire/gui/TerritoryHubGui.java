package com.poro.empire.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Set;

/**
 * 영지 서브 허브 GUI (54슬롯, gui_hub_structure.md §4).
 *
 * <pre>
 * 배경: menu_territory.png — 2행×4열 8패널.
 * 패널 내 어느 슬롯 클릭해도 해당 GUI로 진입.
 *
 *      col0  col1  col2  col3  col4  col5  col6  col7  col8
 * row0  ░    ░    ░    ░    ░    ░    ░    ░    ░   (타이틀 장식)
 * row1 [이동] [이동] [상태] [상태] [창고] [창고] [공방] [공방]  ░
 * row2 [이동] [이동] [상태] [상태] [창고] [창고] [공방] [공방]  ░
 * row3 [시설] [시설] [상점] [상점] [경매] [경매] [설정] [설정]  ░
 * row4 [시설] [시설] [상점] [상점] [경매] [경매] [설정] [설정]  ░
 * row5  ░    ░    ░    ░    ░    ░    ░    ░    ░   (하단 장식)
 * </pre>
 */
public final class TerritoryHubGui {

    // 활성 구역
    public static final Set<Integer> ZONE_STATUS   = Set.of(11, 12, 20, 21);
    public static final Set<Integer> ZONE_STORAGE  = Set.of(13, 14, 22, 23);
    public static final Set<Integer> ZONE_WORKSHOP = Set.of(15, 16, 24, 25);

    // 미구현 구역 (1차 시즌 준비 중)
    public static final Set<Integer> ZONE_MOVE     = Set.of(9,  10, 18, 19);
    public static final Set<Integer> ZONE_FACILITY = Set.of(27, 28, 36, 37);
    public static final Set<Integer> ZONE_SHOP     = Set.of(29, 30, 38, 39);
    public static final Set<Integer> ZONE_AUCTION  = Set.of(31, 32, 40, 41);
    public static final Set<Integer> ZONE_SETTINGS = Set.of(33, 34, 42, 43);

    private TerritoryHubGui() {}

    public static void open(Player player) {
        Inventory gui = Bukkit.createInventory(null, 54, GuiTitles.TERRITORY_HUB);
        ItemStack filler = MainHubGui.icon(Material.BLACK_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < 54; i++) gui.setItem(i, filler);

        // ─ 활성 패널 ─────────────────────────────────────────────
        fillZone(gui, ZONE_STATUS, MainHubGui.icon(Material.TOTEM_OF_UNDYING, "§f영지 상태",
                List.of("§7──────────────", "§7작위·기계·편의 설정", "§7──────────────", "§e클릭하여 열기")));

        fillZone(gui, ZONE_STORAGE, MainHubGui.icon(Material.CHEST, "§f저장고",
                List.of("§7──────────────", "§7보관 재료 확인 및 입금", "§7──────────────", "§e클릭하여 열기")));

        fillZone(gui, ZONE_WORKSHOP, MainHubGui.icon(Material.BLAST_FURNACE, "§f공방",
                List.of("§7──────────────", "§7재료 가공·정제·제련", "§7──────────────", "§e클릭하여 열기")));

        // ─ 미구현 패널 ───────────────────────────────────────────
        fillZone(gui, ZONE_MOVE, MainHubGui.icon(Material.ENDER_PEARL, "§7영지 이동",
                List.of("§7──────────────", "§8준비 중")));

        fillZone(gui, ZONE_FACILITY, MainHubGui.icon(Material.PISTON, "§7시설 관리",
                List.of("§7──────────────", "§8준비 중")));

        fillZone(gui, ZONE_SHOP, MainHubGui.icon(Material.EMERALD, "§7상점",
                List.of("§7──────────────", "§8준비 중")));

        fillZone(gui, ZONE_AUCTION, MainHubGui.icon(Material.GOLD_INGOT, "§7경매장",
                List.of("§7──────────────", "§8준비 중")));

        fillZone(gui, ZONE_SETTINGS, MainHubGui.icon(Material.COMPARATOR, "§7영지 설정",
                List.of("§7──────────────", "§8준비 중")));

        player.openInventory(gui);
    }

    private static void fillZone(Inventory gui, Set<Integer> slots, ItemStack item) {
        for (int slot : slots) gui.setItem(slot, item);
    }
}
