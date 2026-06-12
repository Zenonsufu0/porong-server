package com.poro.rpg.common.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.file.Path;
import java.time.ZoneId;

public final class CommonConfigLoader {
    private CommonConfigLoader() {
    }

    public static CommonConfig load(JavaPlugin plugin) {
        Path dataFolder = plugin.getDataFolder().toPath();
        FileConfiguration config = plugin.getConfig();

        String sqlitePathRaw = config.getString("common.sqlite-path", "storage/poro.sqlite");
        String seedPathRaw = config.getString("common.seed-path", "seeds");
        String timezoneRaw = config.getString("common.timezone", "UTC");
        String apiBind = config.getString("common.api-bind-host", "127.0.0.1");
        String apiSecretKey = config.getString("common.api-secret-key", "");
        long seasonStartEpoch = config.getLong("common.season-start-epoch", 0L);

        Path sqlitePath = resolvePath(dataFolder, sqlitePathRaw);
        Path seedPath = resolvePath(dataFolder, seedPathRaw);
        ZoneId zoneId = safeZoneId(timezoneRaw);

        return new CommonConfig(sqlitePath, seedPath, zoneId, apiBind, apiSecretKey, seasonStartEpoch);
    }

    private static Path resolvePath(Path base, String value) {
        Path path = Path.of(value == null || value.isBlank() ? "." : value);
        return path.isAbsolute() ? path.normalize() : base.resolve(path).normalize();
    }

    private static ZoneId safeZoneId(String value) {
        try {
            return ZoneId.of(value == null || value.isBlank() ? "UTC" : value);
        } catch (Exception ignored) {
            return ZoneId.of("UTC");
        }
    }
}
