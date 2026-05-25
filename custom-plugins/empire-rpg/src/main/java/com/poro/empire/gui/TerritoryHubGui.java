package com.poro.empire.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * 영지 서브 허브 GUI (27슬롯).
 *
 * <pre>
 * 슬롯 10 : 영지 상태
 * 슬롯 13 : 저장고
 * 슬롯 16 : 공방
 * 슬롯 18 : 뒤로 (메인 허브)
 * 슬롯 26 : 닫기
 * </pre>
 */
public final class TerritoryHubGui {

    public static final int SLOT_STATUS   = 10;
    public static final int SLOT_STORAGE  = 13;
    public static final int SLOT_WORKSHOP = 16;
    public static final int SLOT_BACK     = 18;
    public static final int SLOT_CLOSE    = 26;

    private TerritoryHubGui() {}

    public static void open(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, GuiTitles.TERRITORY_HUB);
        ItemStack filler = MainHubGui.icon(Material.BLACK_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < 27; i++) gui.setItem(i, filler);

        gui.setItem(SLOT_STATUS,   MainHubGui.icon(Material.TOTEM_OF_UNDYING, "§f영지 상태",
                List.of("§7──────────────", "§7작위·기계·편의 설정")));
        gui.setItem(SLOT_STORAGE,  MainHubGui.icon(Material.CHEST, "§f저장고",
                List.of("§7──────────────", "§7보관 재료 확인 및 입금")));
        gui.setItem(SLOT_WORKSHOP, MainHubGui.icon(Material.BLAST_FURNACE, "§f공방",
                List.of("§7──────────────", "§7재료 가공·정제·제련")));
        gui.setItem(SLOT_BACK,     MainHubGui.icon(Material.ARROW, "§7뒤로", List.of("§7메인 메뉴")));
        gui.setItem(SLOT_CLOSE,    MainHubGui.icon(Material.BARRIER, "§c닫기", List.of()));

        player.openInventory(gui);
    }
}
