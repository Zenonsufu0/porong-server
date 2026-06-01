package com.poro.rpg.gui;

import com.poro.rpg.growth.island.IslandTerritoryState;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * 시설 현황 GUI (45슬롯, 5×9).
 *
 * row0-1: 약초 재배기 슬롯 (slot 0~17, reaperCount만큼 채움)
 * row2-3: 광물 채굴기 슬롯 (slot 18~35, 현재 데이터 미지원 → gray)
 * row4:   뒤로(slot36) + gray×8
 */
public final class TerritoryFacilityGui {
    private TerritoryFacilityGui() {}

    public static final int SLOT_BACK = 36;

    public static void open(Player player, IslandTerritoryState territory) {
        Inventory inv = Bukkit.createInventory(null, 45, GuiTitles.TERRITORY_FACILITY);
        ItemStack gray = MainHubGui.icon(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < 45; i++) inv.setItem(i, gray);

        int reapers = territory.reaperCount();
        int miners  = territory.minerCount();
        int lv      = machineLevel(territory);

        // row0-1: 약초 재배기
        for (int i = 0; i < Math.min(reapers, 18); i++) {
            inv.setItem(i, MainHubGui.icon(Material.WHEAT,
                    "§a약초 재배기 §fLv." + lv,
                    List.of("§720분 주기 생산", "§7제국 약초·정수 자동 적립")));
        }
        if (reapers == 0) {
            inv.setItem(8, MainHubGui.icon(Material.GRAY_STAINED_GLASS_PANE,
                    "§7약초 재배기 §8[미설치]",
                    List.of("§7작위 승급 후 설치 가능")));
        }

        // row2-3: 광물 채굴기 (slot 18~35)
        for (int i = 0; i < Math.min(miners, 18); i++) {
            inv.setItem(18 + i, MainHubGui.icon(Material.IRON_PICKAXE,
                    "§b광물 채굴기 §fLv." + lv,
                    List.of("§720분 주기 생산", "§7마도철 원석·은 원석 자동 적립")));
        }
        if (miners == 0) {
            inv.setItem(26, MainHubGui.icon(Material.GRAY_STAINED_GLASS_PANE,
                    "§7광물 채굴기 §8[미설치]",
                    List.of("§7작위 승급 후 설치 가능")));
        }

        inv.setItem(SLOT_BACK, MainHubGui.icon(Material.ARROW, "§7뒤로", List.of("§7영지 설정")));

        player.openInventory(inv);
    }

    private static int machineLevel(IslandTerritoryState t) {
        int tier = t.rank().tier;
        return tier < 3 ? 1 : tier < 5 ? 2 : 3;
    }
}
