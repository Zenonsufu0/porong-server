package kr.zenon.rpg.listener;

import kr.zenon.rpg.combat.CombatStateService;
import kr.zenon.rpg.gui.ExploreHubGui;
import kr.zenon.rpg.hotbar.HotbarService;
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
