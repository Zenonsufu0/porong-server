package com.poro.empire.listener;

import com.poro.empire.combat.CombatStateService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public final class CombatStateListener implements Listener {
    private final CombatStateService combatStateService;

    public CombatStateListener(CombatStateService combatStateService) {
        this.combatStateService = combatStateService;
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player) {
            combatStateService.enterCombat(player.getUniqueId());
        }
        if (event.getEntity() instanceof Player player) {
            combatStateService.enterCombat(player.getUniqueId());
        }
    }
}
