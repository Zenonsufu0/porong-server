package com.poro.empire.listener;

import com.poro.empire.combat.SkillService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;

public final class SkillInputListener implements Listener {
    private final SkillService skillService;

    public SkillInputListener(SkillService skillService) {
        this.skillService = skillService;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
    }

    @EventHandler
    public void onSwap(PlayerSwapHandItemsEvent event) {
    }
}
