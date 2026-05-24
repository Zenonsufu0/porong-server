package com.poro.empire.listener;

import com.poro.empire.growth.GrowthStateStore;
import com.poro.empire.market.AuctionStore;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.plugin.Plugin;

public final class AuctionGuiListener implements Listener {
    public AuctionGuiListener(Plugin plugin, GrowthStateStore growthStateStore, AuctionStore auctionStore) {
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
    }
}
