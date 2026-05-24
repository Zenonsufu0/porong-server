package com.poro.empire.listener;

import com.poro.empire.growth.GrowthStateStore;
import com.poro.empire.growth.island.IslandStorageStore;
import com.poro.empire.growth.island.IslandTerritoryStateStore;
import com.poro.empire.storage.PlayerDataManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public final class TerritoryStatusGuiListener implements Listener {
    public TerritoryStatusGuiListener(
            IslandTerritoryStateStore islandTerritoryStateStore,
            IslandStorageStore islandStorageStore,
            GrowthStateStore growthStateStore,
            PlayerDataManager playerDataManager
    ) {
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
    }
}
