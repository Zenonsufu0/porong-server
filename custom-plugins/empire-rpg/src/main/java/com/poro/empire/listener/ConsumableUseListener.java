package com.poro.empire.listener;

import com.poro.empire.boss.room.BossRoomManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.Plugin;

public final class ConsumableUseListener implements Listener {
    public ConsumableUseListener(BossRoomManager bossRoomManager, Plugin plugin) {
    }

    @EventHandler
    public void onUse(PlayerInteractEvent event) {
    }
}
