package com.poro.rpg.listener;

import com.poro.rpg.combat.CombatStateService;
import com.poro.rpg.gui.ExploreHubGui;
import com.poro.rpg.hotbar.HotbarService;
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
