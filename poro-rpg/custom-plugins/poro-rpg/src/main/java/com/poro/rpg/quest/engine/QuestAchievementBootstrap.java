package com.poro.rpg.quest.engine;

import com.poro.rpg.common.config.FoundationContext;
import com.poro.rpg.common.logging.DomainLogger;
import com.poro.rpg.common.registry.RegistryBootstrapper;
import com.poro.rpg.common.registry.master.MasterRegistryContext;
import com.poro.rpg.common.registry.master.model.AchievementMaster;
import com.poro.rpg.common.registry.master.model.QuestMaster;
import com.poro.rpg.common.result.ErrorCode;
import com.poro.rpg.common.result.Result;
import com.poro.rpg.common.seed.CsvSeedLoader;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class QuestAchievementBootstrap {
    private QuestAchievementBootstrap() {
    }

    public static Result<QuestAchievementRuntime> bootstrap(
            JavaPlugin plugin,
            FoundationContext foundationContext,
            MasterRegistryContext masterRegistryContext
    ) {
        DomainLogger logger = foundationContext.logger().domain("quest-achievement-engine");

        Result<Void> installResult = DefaultQuestAchievementSeedInstaller.install(plugin, logger);
        if (installResult.isFailure()) {
            return Result.failure(installResult.errorCode(), installResult.message(), installResult.cause());
        }

        QuestRewardRegistry rewardRegistry = new QuestRewardRegistry();
        QuestChainBranchRegistry chainBranchRegistry = new QuestChainBranchRegistry();
        Path seedPath = foundationContext.config().seedPath();
        Result<Void> loadResult = foundationContext.registryBootstrapper().bootstrap(List.of(
                RegistryBootstrapper.task(
                        "quest_reward_seed",
                        new CsvSeedLoader<>("quest_reward_seed", seedPath.resolve("quest_reward_seed.csv"), QuestSeedMappers::rewardEntry),
                        list -> list.forEach(rewardRegistry::register)
                ),
                RegistryBootstrapper.task(
                        "quest_chain_branch",
                        new CsvSeedLoader<>("quest_chain_branch", seedPath.resolve("quest_chain_branch.csv"), QuestSeedMappers::chainBranchRule),
                        list -> list.forEach(chainBranchRegistry::register)
                )
        ));
        if (loadResult.isFailure()) {
            return Result.failure(loadResult.errorCode(), loadResult.message(), loadResult.cause());
        }

        Result<Void> validation = validate(masterRegistryContext, rewardRegistry, chainBranchRegistry, logger);
        if (validation.isFailure()) {
            return Result.failure(validation.errorCode(), validation.message(), validation.cause());
        }

        QuestObjectiveEvaluator objectiveEvaluator = new QuestObjectiveEvaluator();
        InMemoryQuestRecordRewardHook recordRewardHook = new InMemoryQuestRecordRewardHook();
        InMemoryHallOfFameHook hallOfFameHook = new InMemoryHallOfFameHook();
        InMemorySymbolicHonorHook symbolicHonorHook = new InMemorySymbolicHonorHook();
        QuestRewardResolver rewardResolver = new QuestRewardResolver(rewardRegistry, recordRewardHook, foundationContext.timeProvider());
        QuestChainService chainService = new QuestChainService(masterRegistryContext.questMasters(), chainBranchRegistry);
        AchievementRewardService achievementRewardService = new AchievementRewardService(
                hallOfFameHook,
                symbolicHonorHook,
                foundationContext.timeProvider()
        );
        AchievementTracker achievementTracker = new AchievementTracker(
                masterRegistryContext.achievementMasters(),
                achievementRewardService,
                foundationContext.timeProvider()
        );
        QuestStateService questStateService = new QuestStateService(
                masterRegistryContext.questMasters(),
                objectiveEvaluator,
                rewardResolver,
                chainService,
                new NoopNextQuestUnlockHook(),
                achievementTracker,
                foundationContext.timeProvider()
        );
        PlayerQuestAndHonorSnapshotBuilder snapshotBuilder = new PlayerQuestAndHonorSnapshotBuilder(
                masterRegistryContext.questMasters(),
                masterRegistryContext.achievementMasters(),
                foundationContext.timeProvider()
        );

        logger.info("Quest/achievement engine bootstrap completed. quest_rewards=" + rewardRegistry.size()
                + ", chain_branches=" + chainBranchRegistry.size()
                + ", quest_masters=" + masterRegistryContext.questMasters().size()
                + ", achievement_masters=" + masterRegistryContext.achievementMasters().size());

        return Result.success(new QuestAchievementRuntime(
                questStateService,
                objectiveEvaluator,
                rewardResolver,
                chainService,
                achievementTracker,
                achievementRewardService,
                snapshotBuilder,
                rewardRegistry,
                chainBranchRegistry,
                recordRewardHook,
                hallOfFameHook,
                symbolicHonorHook
        ));
    }

    private static Result<Void> validate(
            MasterRegistryContext masterRegistryContext,
            QuestRewardRegistry rewardRegistry,
            QuestChainBranchRegistry chainBranchRegistry,
            DomainLogger logger
    ) {
        List<String> blockingErrors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        for (QuestMaster quest : masterRegistryContext.questMasters().all().values()) {
            try {
                QuestObjectiveType.from(quest.objectiveType());
            } catch (Exception exception) {
                blockingErrors.add("Unknown objective_type in quest_master: quest_id=" + quest.questId()
                        + ", objective_type=" + quest.objectiveType());
            }

            String nextQuestId = normalize(quest.nextQuestId());
            if (!nextQuestId.isBlank() && !"none".equals(nextQuestId) && !"-".equals(nextQuestId)
                    && masterRegistryContext.questMasters().find(nextQuestId).isEmpty()) {
                warnings.add("next_quest_id references missing quest: quest_id=" + quest.questId()
                        + ", next_quest_id=" + quest.nextQuestId());
            }
        }

        for (Map.Entry<String, List<QuestRewardEntry>> entry : rewardRegistry.all().entrySet()) {
            if (masterRegistryContext.questMasters().find(entry.getKey()).isEmpty()) {
                blockingErrors.add("quest_reward_seed references unknown quest_id: " + entry.getKey());
                continue;
            }
            for (QuestRewardEntry reward : entry.getValue()) {
                if (reward.rewardAmount() < 0L) {
                    blockingErrors.add("quest_reward_seed reward_amount must be >= 0. quest_id=" + reward.questId()
                            + ", order=" + reward.rewardOrder());
                }
            }
        }

        for (List<QuestChainBranchRule> rules : chainBranchRegistry.all().values()) {
            for (QuestChainBranchRule rule : rules) {
                if (masterRegistryContext.questMasters().find(rule.parentQuestId()).isEmpty()) {
                    blockingErrors.add("quest_chain_branch parent quest not found: " + rule.parentQuestId());
                }
                if (masterRegistryContext.questMasters().find(rule.branchQuestId()).isEmpty()) {
                    blockingErrors.add("quest_chain_branch branch quest not found: " + rule.branchQuestId());
                }
            }
        }

        for (AchievementMaster achievement : masterRegistryContext.achievementMasters().all().values()) {
            try {
                AchievementConditionType.from(achievement.conditionType());
            } catch (Exception exception) {
                warnings.add("Unknown achievement condition_type: achievement_id=" + achievement.achievementId()
                        + ", condition_type=" + achievement.conditionType());
            }
            try {
                QuestRewardType.from(achievement.rewardType());
            } catch (Exception exception) {
                warnings.add("Unknown achievement reward_type: achievement_id=" + achievement.achievementId()
                        + ", reward_type=" + achievement.rewardType());
            }
        }

        warnings.forEach(logger::warn);
        if (!blockingErrors.isEmpty()) {
            blockingErrors.forEach(logger::error);
            return Result.failure(
                    ErrorCode.MASTER_SEED_VALIDATION_FAILED,
                    "Quest/achievement seed validation failed with " + blockingErrors.size() + " blocking errors."
            );
        }
        return Result.success();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
