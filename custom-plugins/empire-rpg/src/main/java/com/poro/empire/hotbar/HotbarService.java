package com.poro.empire.hotbar;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

public final class HotbarService {
    private final Plugin plugin;

    public HotbarService(Plugin plugin) {
        this.plugin = plugin;
    }

    public void setWeaponSlot(Player player, ItemStack itemStack) {
        player.getInventory().setItem(0, itemStack);
    }

    public void updateHotbar(Player player) {
        player.updateInventory();
    }

    public Plugin plugin() {
        return plugin;
    }
}
