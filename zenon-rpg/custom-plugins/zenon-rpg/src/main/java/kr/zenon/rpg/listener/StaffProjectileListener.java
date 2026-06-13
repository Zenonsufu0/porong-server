package kr.zenon.rpg.listener;

import kr.zenon.rpg.combat.ResourceTracker;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;

public final class StaffProjectileListener implements Listener {
    private final ResourceTracker resourceTracker;

    public StaffProjectileListener(ResourceTracker resourceTracker) {
        this.resourceTracker = resourceTracker;
    }

    @EventHandler
    public void onHit(ProjectileHitEvent event) {
        if (event.getEntity().getShooter() instanceof org.bukkit.entity.Player player) {
            resourceTracker.resetStack(player.getUniqueId());
        }
    }
}
