package com.poro.empire.field;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public final class FieldTeleportService {
    private final Plugin plugin;

    public FieldTeleportService(Plugin plugin) {
        this.plugin = plugin;
    }

    public void teleportToField(Player player, String fieldId) {
        player.sendMessage("[필드] " + fieldId + " 이동은 아직 구현 전입니다.");
    }
}
