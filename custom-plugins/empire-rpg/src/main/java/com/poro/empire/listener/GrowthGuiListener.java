package com.poro.empire.listener;

import com.poro.empire.common.registry.master.ItemMasterRegistry;
import com.poro.empire.combat.CombatStateService;
import com.poro.empire.growth.GrowthStateStore;
import com.poro.empire.growth.engine.GrowthEngineRuntime;
import com.poro.empire.scoreboard.ScoreboardService;
import com.poro.empire.storage.PlayerDataManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.plugin.Plugin;

public final class GrowthGuiListener implements Listener {
    public GrowthGuiListener(
            GrowthStateStore growthStateStore,
            GrowthEngineRuntime growthEngineRuntime,
            PlayerDataManager playerDataManager,
            ScoreboardService scoreboardService,
            ItemMasterRegistry itemMasters,
            CombatStateService combatStateService,
            Plugin plugin
    ) {
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
    }
}
