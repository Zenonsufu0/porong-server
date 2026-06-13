package kr.zenon.rpg.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;

public class HungerLockListener implements Listener {

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        applyFoodLock(event.getPlayer());
    }

    @EventHandler
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        event.setCancelled(true);
        applyFoodLock(player);
    }

    private void applyFoodLock(Player player) {
        player.setFoodLevel(20);
        player.setSaturation(20.0f);
        player.setExhaustion(0.0f);
    }
}
