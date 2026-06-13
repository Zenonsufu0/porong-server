package kr.zenon.rpg.quest.engine;

import kr.zenon.rpg.common.logging.DomainLogger;
import kr.zenon.rpg.common.result.ErrorCode;
import kr.zenon.rpg.common.result.Result;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Objects;

final class DefaultQuestAchievementSeedInstaller {
    private static final List<String> RESOURCE_PATHS = List.of(
            "seeds/quest_reward_seed.csv",
            "seeds/quest_chain_branch.csv"
    );

    private DefaultQuestAchievementSeedInstaller() {
    }

    static Result<Void> install(JavaPlugin plugin, DomainLogger logger) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(logger, "logger");
        try {
            for (String resourcePath : RESOURCE_PATHS) {
                plugin.saveResource(resourcePath, false);
            }
            logger.info("Default quest/achievement seed resources are ensured.");
            return Result.success();
        } catch (Exception exception) {
            return Result.failure(
                    ErrorCode.SEED_LOAD_FAILED,
                    "Failed to install quest/achievement seed resources",
                    exception
            );
        }
    }
}
