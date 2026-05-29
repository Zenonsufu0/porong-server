package com.poro.empire.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * 권한 등급 선택 GUI (27슬롯, gui_territory_settings.md §12).
 *
 * <pre>
 * row0: ░ ░ ░ ░ ░ ░ ░ ░ ░
 * row1: [부영주] [영지민] [방문자] ░ ░ ░ ░ ░ ░
 * row2: [뒤로] ░ ░ ░ ░ ░ ░ ░ ░
 * </pre>
 */
public final class PermissionRolesGui {
    private PermissionRolesGui() {}

    public static final int SLOT_VICE_LORD = 9;
    public static final int SLOT_RESIDENT  = 10;
    public static final int SLOT_VISITOR   = 11;
    public static final int SLOT_BACK      = 18;

    public static void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, GuiTitles.PERMISSION_ROLES);
        ItemStack gray = MainHubGui.icon(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < 27; i++) inv.setItem(i, gray);

        inv.setItem(SLOT_VICE_LORD, MainHubGui.icon(Material.DIAMOND, "§b부영주 권한 편집",
                List.of("§7부영주 등급의 9가지 권한", "§8▶ 클릭하여 편집")));
        inv.setItem(SLOT_RESIDENT,  MainHubGui.icon(Material.EMERALD, "§e영지민 권한 편집",
                List.of("§7영지민 등급의 9가지 권한", "§8▶ 클릭하여 편집")));
        inv.setItem(SLOT_VISITOR,   MainHubGui.icon(Material.IRON_INGOT, "§7방문자 권한 편집",
                List.of("§7방문자(미초대) 등급의 9가지 권한", "§8▶ 클릭하여 편집")));
        inv.setItem(SLOT_BACK,      MainHubGui.icon(Material.ARROW, "§7뒤로", List.of("§7영지 설정")));

        player.openInventory(inv);
    }
}
