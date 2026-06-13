package kr.zenon.rpg.combat.engine;

import kr.zenon.rpg.common.logging.DomainLogger;
import kr.zenon.rpg.common.result.ErrorCode;
import kr.zenon.rpg.common.result.Result;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

final class DefaultCombatSeedInstaller {
    private DefaultCombatSeedInstaller() {
    }

    static Result<Void> install(JavaPlugin plugin, DomainLogger logger) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(logger, "logger");
        try {
            plugin.saveResource("seeds/state_master.csv", false);
            logger.info("Default combat seed resources are ensured.");
            return Result.success();
        } catch (Exception exception) {
            return Result.failure(
                    ErrorCode.SEED_LOAD_FAILED,
                    "Failed to install combat seed resources",
                    exception
            );
        }
    }
}
