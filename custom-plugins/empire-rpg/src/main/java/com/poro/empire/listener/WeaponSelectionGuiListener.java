package com.poro.empire.listener;

import com.poro.empire.combat.CombatStateService;
import com.poro.empire.combat.CooldownManager;
import com.poro.empire.growth.GrowthStateStore;
import com.poro.empire.scoreboard.ScoreboardService;
import com.poro.empire.storage.PlayerDataManager;
import com.poro.empire.tutorial.TutorialService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public final class WeaponSelectionGuiListener implements Listener {
    public WeaponSelectionGuiListener(
            PlayerDataManager playerDataManager,
            TutorialService tutorialService,
            ScoreboardService scoreboardService,
            GrowthStateStore growthStateStore,
            CooldownManager cooldownManager,
            CombatStateService combatStateService
    ) {
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
    }
}
