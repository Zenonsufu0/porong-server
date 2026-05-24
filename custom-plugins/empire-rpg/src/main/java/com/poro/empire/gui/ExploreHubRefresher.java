package com.poro.empire.gui;

import org.bukkit.plugin.Plugin;

public final class ExploreHubRefresher {
    private ExploreHubRefresher() {
    }

    public static void start(Plugin plugin, ExploreHubGui.FieldStateProvider provider) {
        plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (provider != null) {
                provider.status("default");
            }
        }, 20L, 20L * 30L);
    }
}
