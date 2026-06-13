package kr.zenon.rpg.common.config;

import java.nio.file.Path;
import java.time.ZoneId;
import java.util.Objects;

public record CommonConfig(
        Path sqlitePath,
        Path seedPath,
        ZoneId defaultZoneId,
        String apiBind,
        String apiSecretKey,
        long seasonStartEpoch
) {
    public CommonConfig {
        Objects.requireNonNull(sqlitePath, "sqlitePath");
        Objects.requireNonNull(seedPath, "seedPath");
        Objects.requireNonNull(defaultZoneId, "defaultZoneId");
        apiBind = (apiBind == null || apiBind.isBlank()) ? "127.0.0.1" : apiBind;
        apiSecretKey = (apiSecretKey == null) ? "" : apiSecretKey;
        if (seasonStartEpoch < 0) seasonStartEpoch = 0;
    }

    public String sqliteJdbcUrl() {
        return "jdbc:sqlite:" + sqlitePath.toAbsolutePath();
    }
}
