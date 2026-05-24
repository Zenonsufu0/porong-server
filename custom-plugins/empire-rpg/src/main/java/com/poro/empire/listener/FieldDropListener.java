package com.poro.empire.listener;

import com.poro.empire.field.FieldBossRespawnScheduler;
import com.poro.empire.growth.GrowthStateStore;
import com.poro.empire.growth.island.IslandTerritoryStateStore;
import com.poro.empire.leveling.LevelingService;
import com.poro.empire.storage.PlayerDataManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

public final class FieldDropListener implements Listener {
    private final LevelingService levelingService;

    public FieldDropListener(
            GrowthStateStore growthStateStore,
            IslandTerritoryStateStore islandTerritoryStateStore,
            PlayerDataManager playerDataManager,
            LevelingService levelingService,
            FieldBossRespawnScheduler fieldBossScheduler
    ) {
        this.levelingService = levelingService;
    }

    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer != null) {
            levelingService.addExp(killer.getUniqueId(), 10L);
        }
    }
}
