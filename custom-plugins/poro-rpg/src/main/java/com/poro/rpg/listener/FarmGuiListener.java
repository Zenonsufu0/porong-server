package com.poro.rpg.listener;

import com.poro.rpg.growth.island.IslandStorageStore;
import com.poro.rpg.growth.island.IslandTerritoryStateStore;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public final class FarmGuiListener implements Listener {
    public FarmGuiListener(IslandTerritoryStateStore islandTerritoryStateStore, IslandStorageStore islandStorageStore) {
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
    }
}
