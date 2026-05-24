package com.poro.empire.listener;

import com.poro.empire.boss.room.BossRoomManager;
import com.poro.empire.common.registry.master.BossMasterRegistry;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.Plugin;

public final class BossRoomListener implements Listener {
    public BossRoomListener(Plugin plugin, BossRoomManager bossRoomManager, BossMasterRegistry bossMasters) {
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
    }
}
