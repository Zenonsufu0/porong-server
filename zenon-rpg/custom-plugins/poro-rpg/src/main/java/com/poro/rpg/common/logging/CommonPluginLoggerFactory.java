package com.poro.rpg.common.logging;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class CommonPluginLoggerFactory {
    private CommonPluginLoggerFactory() {
    }

    public static CommonPluginLogger fromPlugin(JavaPlugin plugin) {
        Objects.requireNonNull(plugin, "plugin");
        return new CommonPluginLogger(plugin.getLogger(), plugin.getName());
    }
}
