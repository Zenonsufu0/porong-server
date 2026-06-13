package kr.zenon.rpg.listener;

import kr.zenon.rpg.growth.GrowthStateStore;
import kr.zenon.rpg.growth.island.IslandTerritoryStateStore;
import kr.zenon.rpg.storage.PlayerDataManager;
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
