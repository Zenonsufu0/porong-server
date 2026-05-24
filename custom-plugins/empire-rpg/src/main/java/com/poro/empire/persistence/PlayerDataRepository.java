package com.poro.empire.persistence;

import org.bukkit.plugin.Plugin;

import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

public final class PlayerDataRepository {
    private final Path dataDirectory;

    public PlayerDataRepository(Plugin plugin) {
        this.dataDirectory = plugin.getDataFolder().toPath().resolve("playerdata");
    }

    public Optional<PlayerSaveData> load(UUID uuid) {
        return Optional.empty();
    }

    public void save(UUID uuid, PlayerSaveData data) {
    }

    public Path dataDirectory() {
        return dataDirectory;
    }
}
