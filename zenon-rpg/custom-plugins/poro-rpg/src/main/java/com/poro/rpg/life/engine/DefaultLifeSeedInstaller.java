package com.poro.rpg.life.engine;

import com.poro.rpg.common.logging.DomainLogger;
import com.poro.rpg.common.result.ErrorCode;
import com.poro.rpg.common.result.Result;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Objects;

final class DefaultLifeSeedInstaller {
    private static final List<String> RESOURCE_PATHS = List.of(
            "seeds/life_gather_node.csv",
            "seeds/life_recipe_master.csv",
            "seeds/life_skill_exp_table.csv",
            "seeds/estate_unlock_rule.csv",
            "seeds/estate_facility_master.csv",
            "seeds/estate_facility_level_rule.csv"
    );

    private DefaultLifeSeedInstaller() {
    }

    static Result<Void> install(JavaPlugin plugin, DomainLogger logger) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(logger, "logger");
        try {
            for (String resourcePath : RESOURCE_PATHS) {
                plugin.saveResource(resourcePath, false);
            }
            logger.info("Default life/estate seed resources are ensured.");
            return Result.success();
        } catch (Exception exception) {
            return Result.failure(
                    ErrorCode.SEED_LOAD_FAILED,
                    "Failed to install life/estate seed resources",
                    exception
            );
        }
    }
}
