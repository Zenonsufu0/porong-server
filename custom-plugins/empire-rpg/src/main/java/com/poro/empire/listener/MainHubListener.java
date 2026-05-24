package com.poro.empire.listener;

import com.poro.empire.combat.CombatStateService;
import com.poro.empire.field.FieldTeleportService;
import com.poro.empire.growth.GrowthStateStore;
import com.poro.empire.growth.engine.GrowthEngineRuntime;
import com.poro.empire.growth.island.IslandStorageStore;
import com.poro.empire.growth.island.IslandTerritoryStateStore;
import com.poro.empire.gui.ExploreHubGui;
import com.poro.empire.market.AuctionStore;
import com.poro.empire.scoreboard.ScoreboardService;
import com.poro.empire.storage.PlayerDataManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.Plugin;

public final class MainHubListener implements Listener {
    public MainHubListener(
            Plugin plugin,
            ExploreHubGui.FieldStateProvider fieldStateProvider,
            GrowthStateStore growthStateStore,
            GrowthEngineRuntime growthEngineRuntime,
            ScoreboardService scoreboardService,
            PlayerDataManager playerDataManager,
            IslandStorageStore islandStorageStore,
            IslandTerritoryStateStore islandTerritoryStateStore,
            FieldTeleportService fieldTeleportService,
            CombatStateService combatStateService,
            AuctionStore auctionStore,
            BossRoomListener bossRoomListener
    ) {
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
    }
}
