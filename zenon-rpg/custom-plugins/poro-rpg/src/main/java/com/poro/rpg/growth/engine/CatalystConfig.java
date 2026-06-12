package com.poro.rpg.growth.engine;

import org.bukkit.configuration.file.FileConfiguration;

public final class CatalystConfig {
    private static int catalystBonusPercent = 10;

    private CatalystConfig() {
    }

    public static void reload(FileConfiguration config) {
        catalystBonusPercent = config.getInt("growth.catalyst.bonus-percent", 10);
    }

    public static int catalystBonusPercent() {
        return catalystBonusPercent;
    }
}
