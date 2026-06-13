package com.poro.rpg.common.registry.master;

import com.poro.rpg.common.logging.DomainLogger;
import com.poro.rpg.common.result.ErrorCode;
import com.poro.rpg.common.result.Result;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Objects;

final class DefaultMasterSeedInstaller {
    private static final List<String> RESOURCE_PATHS = List.of(
            "seeds/item_master.csv",
            "seeds/skill_master.csv",
            "seeds/boss_master.csv",
            "seeds/quest_master.csv",
            "seeds/achievement_master.csv",
            "seeds/region_master.csv",
            "seeds/town_master.csv",
            "seeds/npc_master.csv"
    );

    private DefaultMasterSeedInstaller() {
    }

    static Result<Void> install(JavaPlugin plugin, DomainLogger logger) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(logger, "logger");

        try {
            for (String resourcePath : RESOURCE_PATHS) {
                plugin.saveResource(resourcePath, false);
            }
            logger.info("Default master seed resources are ensured.");
            return Result.success();
        } catch (Exception exception) {
            return Result.failure(
                    ErrorCode.SEED_LOAD_FAILED,
                    "Failed to install default master seed resources",
                    exception
            );
        }
    }
}
