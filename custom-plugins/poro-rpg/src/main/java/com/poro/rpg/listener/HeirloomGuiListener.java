package com.poro.rpg.listener;

import com.poro.rpg.growth.GrowthStateStore;
import com.poro.rpg.growth.island.IslandTerritoryStateStore;
import com.poro.rpg.storage.PlayerDataManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public final class HeirloomGuiListener implements Listener {
    public HeirloomGuiListener(
            IslandTerritoryStateStore islandTerritoryStateStore,
            GrowthStateStore growthStateStore,
            PlayerDataManager playerDataManager
    ) {
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
    }
}
