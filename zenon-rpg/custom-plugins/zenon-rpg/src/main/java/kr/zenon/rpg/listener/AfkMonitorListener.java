package kr.zenon.rpg.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.Plugin;

public final class AfkMonitorListener implements Listener {
    public AfkMonitorListener(Plugin plugin) {
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
    }
}
