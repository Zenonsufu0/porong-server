package kr.zenon.rpg.npc.citizens;

import kr.zenon.rpg.common.logging.DomainLogger;
import kr.zenon.rpg.common.result.ErrorCode;
import kr.zenon.rpg.common.result.Result;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Objects;

final class DefaultNpcSyncSeedInstaller {
    private static final List<String> RESOURCE_PATHS = List.of(
            "seeds/npc_spawn_seed.csv"
    );

    private DefaultNpcSyncSeedInstaller() {
    }

    static Result<Void> install(JavaPlugin plugin, DomainLogger logger) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(logger, "logger");

        try {
            for (String resourcePath : RESOURCE_PATHS) {
                plugin.saveResource(resourcePath, false);
            }
            logger.info("Default Citizens NPC seed resource is ensured.");
            return Result.success();
        } catch (Exception exception) {
            return Result.failure(
                    ErrorCode.SEED_LOAD_FAILED,
                    "Failed to install Citizens NPC seed resource",
                    exception
            );
        }
    }
}

