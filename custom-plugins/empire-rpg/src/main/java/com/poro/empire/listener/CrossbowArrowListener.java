package com.poro.empire.listener;

import com.poro.empire.combat.ResourceTracker;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileLaunchEvent;

public final class CrossbowArrowListener implements Listener {
    private final ResourceTracker resourceTracker;

    public CrossbowArrowListener(ResourceTracker resourceTracker) {
        this.resourceTracker = resourceTracker;
    }

    @EventHandler
    public void onLaunch(ProjectileLaunchEvent event) {
        Projectile projectile = event.getEntity();
        if (projectile.getShooter() instanceof org.bukkit.entity.Player player) {
            resourceTracker.incrementStack(player.getUniqueId(), 5);
        }
    }
}
