package kr.zenon.rpg.persistence;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.bukkit.plugin.Plugin;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class PlayerDataRepository {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path dataDirectory;
    private final Logger logger;

    public PlayerDataRepository(Plugin plugin) {
        this.dataDirectory = plugin.getDataFolder().toPath().resolve("playerdata");
        this.logger = plugin.getLogger();
    }

    public Optional<PlayerSaveData> load(UUID uuid) {
        Path file = file(uuid);
        if (!Files.isRegularFile(file)) {
            return Optional.empty();
        }
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            return Optional.ofNullable(GSON.fromJson(reader, PlayerSaveData.class));
        } catch (IOException | RuntimeException e) {
            logger.log(Level.WARNING, "[Persistence] 플레이어 데이터 로드 실패: " + uuid, e);
            return Optional.empty();
        }
    }

    public void save(UUID uuid, PlayerSaveData data) {
        try {
            Files.createDirectories(dataDirectory);
            try (Writer writer = Files.newBufferedWriter(file(uuid), StandardCharsets.UTF_8)) {
                GSON.toJson(data, writer);
            }
        } catch (IOException | RuntimeException e) {
            logger.log(Level.WARNING, "[Persistence] 플레이어 데이터 저장 실패: " + uuid, e);
        }
    }

    public Path dataDirectory() {
        return dataDirectory;
    }

    private Path file(UUID uuid) {
        return dataDirectory.resolve(uuid + ".json");
    }
}
