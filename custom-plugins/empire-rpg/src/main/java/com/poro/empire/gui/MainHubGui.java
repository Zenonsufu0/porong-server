package com.poro.empire.gui;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public final class MainHubGui {
    private MainHubGui() {
    }

    public static void open(Player player) {
        Inventory gui = Bukkit.createInventory(null, 54, GuiTitles.MAIN_HUB);
        ItemStack pane = icon(Material.BLACK_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < 54; i++) gui.setItem(i, pane);
        gui.setItem(20, icon(Material.GOLDEN_SWORD,  "§f장비 관리",   List.of("§7──────────────", "§7강화 / 잠재능력 / 전승")));
        gui.setItem(22, icon(Material.GRASS_BLOCK,   "§f영지 관리",   List.of("§7──────────────", "§7영지 상태 / 저장고 / 공방")));
        gui.setItem(24, icon(Material.NETHER_STAR,   "§f보스 도전",   List.of("§7──────────────", "§7시즌보스 / 최종보스")));
        gui.setItem(26, icon(Material.EMERALD,       "§f경매장",      List.of("§7──────────────", "§7아이템 거래")));
        gui.setItem(31, icon(Material.COMPASS,       "§f필드 이동",   List.of("§7──────────────", "§7필드 5개 이동")));
        gui.setItem(49, icon(Material.BARRIER,       "§c닫기",        List.of()));
        player.openInventory(gui);
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
