package com.poro.rpg.gui;

import com.poro.rpg.admin.AdminTogglesService;
import com.poro.rpg.admin.AdminTogglesService.Toggle;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public final class AdminTogglesGui {
    private AdminTogglesGui() {}

    public static final int SLOT_BACK  = 18;
    public static final int SLOT_CLOSE = 26;

    public static Toggle[] open(Player admin, AdminTogglesService service) {
        Inventory inv = Bukkit.createInventory(null, 27, GuiTitles.ADMIN_TOGGLES);
        ItemStack gray = MainHubGui.icon(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < 27; i++) inv.setItem(i, gray);

        Toggle[] order = Toggle.values();
        for (int i = 0; i < order.length && i < 9; i++) {
            Toggle t = order[i];
            boolean on = service.isOn(t);
            inv.setItem(9 + i, MainHubGui.icon(
                    on ? Material.LIME_DYE : Material.GRAY_DYE,
                    (on ? "§a" : "§7") + t.displayName + (on ? " §2[ON]" : " §8[OFF]"),
                    List.of("§7──────────────",
                            "§7플래그: §f" + t.name(),
                            "§7클릭 → " + (on ? "§cOFF" : "§aON"))));
        }
        inv.setItem(SLOT_BACK,  MainHubGui.icon(Material.ARROW,   "§7뒤로", List.of("§7관리자 허브")));
        inv.setItem(SLOT_CLOSE, MainHubGui.icon(Material.BARRIER, "§c닫기", List.of()));
        admin.openInventory(inv);
        return order;
    }
}
