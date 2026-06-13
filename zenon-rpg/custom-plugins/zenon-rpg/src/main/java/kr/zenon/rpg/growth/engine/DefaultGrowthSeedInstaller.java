package kr.zenon.rpg.growth.engine;

import kr.zenon.rpg.common.logging.DomainLogger;
import kr.zenon.rpg.common.result.ErrorCode;
import kr.zenon.rpg.common.result.Result;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Objects;

final class DefaultGrowthSeedInstaller {
    private static final List<String> RESOURCE_PATHS = List.of(
            "seeds/growth_enhancement_table.csv",
            "seeds/growth_potential_option_pool.csv",
            "seeds/growth_trace_substat_pool.csv",
            "seeds/growth_set_bonus.csv",
            "seeds/growth_rune_master.csv",
            "seeds/growth_engraving_master.csv"
    );

    private DefaultGrowthSeedInstaller() {
    }

    static Result<Void> install(JavaPlugin plugin, DomainLogger logger) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(logger, "logger");
        try {
            for (String resourcePath : RESOURCE_PATHS) {
                plugin.saveResource(resourcePath, false);
            }
            logger.info("Default growth seed resources are ensured.");
            return Result.success();
        } catch (Exception exception) {
            return Result.failure(
                    ErrorCode.SEED_LOAD_FAILED,
                    "Failed to install growth seed resources",
                    exception
            );
        }
    }
}
