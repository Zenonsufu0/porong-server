package kr.zenon.rpg.boss.engine;

import kr.zenon.rpg.common.logging.DomainLogger;
import kr.zenon.rpg.common.result.ErrorCode;
import kr.zenon.rpg.common.result.Result;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Objects;

final class DefaultBossSeedInstaller {
    private static final List<String> RESOURCE_PATHS = List.of(
            "seeds/boss_entry_rule.csv",
            "seeds/boss_pattern_seed.csv"
    );

    private DefaultBossSeedInstaller() {
    }

    static Result<Void> install(JavaPlugin plugin, DomainLogger logger) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(logger, "logger");
        try {
            for (String resourcePath : RESOURCE_PATHS) {
                plugin.saveResource(resourcePath, false);
            }
            logger.info("Default boss seed resources are ensured.");
            return Result.success();
        } catch (Exception exception) {
            return Result.failure(
                    ErrorCode.SEED_LOAD_FAILED,
                    "Failed to install boss engine seed resources",
                    exception
            );
        }
    }
}
