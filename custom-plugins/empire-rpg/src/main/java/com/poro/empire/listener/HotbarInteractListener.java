package com.poro.empire.listener;

import com.poro.empire.combat.CombatStateService;
import com.poro.empire.gui.ExploreHubGui;
import com.poro.empire.hotbar.HotbarService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;

public final class HotbarInteractListener implements Listener {
    public HotbarInteractListener(
            HotbarService hotbarService,
            ExploreHubGui.FieldStateProvider fieldStateProvider,
            CombatStateService combatStateService
    ) {
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
    }
}
