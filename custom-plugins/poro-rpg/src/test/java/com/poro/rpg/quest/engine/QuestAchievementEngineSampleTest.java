package com.poro.rpg.quest.engine;

import com.poro.rpg.common.registry.master.AchievementMasterRegistry;
import com.poro.rpg.common.registry.master.QuestMasterRegistry;
import com.poro.rpg.common.registry.master.model.AchievementMaster;
import com.poro.rpg.common.registry.master.model.QuestMaster;
import com.poro.rpg.common.seed.CsvRow;
import com.poro.rpg.common.seed.CsvSeedLoader;
import com.poro.rpg.common.time.TimeProvider;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestAchievementEngineSampleTest {
    @Test
    void shouldRunQuestAndAchievementFlowSample() {
        QuestMasterRegistry questRegistry = loadQuestRegistry();
        AchievementMasterRegistry achievementRegistry = loadAchievementRegistry();
        QuestRewardRegistry rewardRegistry = loadQuestRewardRegistry();
        QuestChainBranchRegistry branchRegistry = loadQuestChainRegistry();

        FixedTimeProvider timeProvider = new FixedTimeProvider(Instant.parse("2026-01-01T00:00:00Z"));
        InMemoryQuestRecordRewardHook questRecordHook = new InMemoryQuestRecordRewardHook();
        InMemoryHallOfFameHook hallOfFameHook = new InMemoryHallOfFameHook();
        InMemorySymbolicHonorHook symbolicHonorHook = new InMemorySymbolicHonorHook();

        QuestObjectiveEvaluator objectiveEvaluator = new QuestObjectiveEvaluator();
        QuestRewardResolver questRewardResolver = new QuestRewardResolver(rewardRegistry, questRecordHook, timeProvider);
        AchievementRewardService achievementRewardService = new AchievementRewardService(hallOfFameHook, symbolicHonorHook, timeProvider);
        AchievementTracker achievementTracker = new AchievementTracker(achievementRegistry, achievementRewardService, timeProvider);
        QuestChainService chainService = new QuestChainService(questRegistry, branchRegistry);
        QuestStateService questStateService = new QuestStateService(
                questRegistry,
                objectiveEvaluator,
                questRewardResolver,
                chainService,
                new NoopNextQuestUnlockHook(),
                achievementTracker,
                timeProvider
        );
        PlayerQuestAndHonorSnapshotBuilder snapshotBuilder = new PlayerQuestAndHonorSnapshotBuilder(
                questRegistry,
                achievementRegistry,
                timeProvider
        );

        PlayerQuestHonorState state = new PlayerQuestHonorState("user_quest_01");

        questStateService.startQuest(state, "q_spawn_intro_01").orElseThrow();
        QuestStateService.ProgressUpdateBatch introProgress = questStateService
                .updateProgress(state, QuestEvent.of(ProgressEventType.TALK, "npc_capital_leonid", 1))
                .orElseThrow();
        assertEquals(1, introProgress.completedQuests().size());
        assertEquals(300L, state.currency("gold"));

        questStateService.startQuest(state, "q_spawn_intro_02").orElseThrow();
        QuestStateService.ProgressUpdateBatch mainProgress = questStateService
                .updateProgress(state, QuestEvent.of(ProgressEventType.KILL, "training_dummy", 3))
                .orElseThrow();
        assertEquals(1, mainProgress.completedQuests().size());
        assertTrue(state.unlockedQuestSnapshot().contains("q_life_intro_01"));
        assertTrue(state.unlockedQuestSnapshot().contains("q_estate_unlock_01"));
        assertTrue(state.unlockedQuestSnapshot().contains("q_daily_capital_01"));

        state.addItem("res_herb_imperial", 10);
        questStateService.startQuest(state, "q_life_intro_01").orElseThrow();
        QuestStateService.ProgressUpdateBatch lifeProgress = questStateService
                .updateProgress(state, QuestEvent.of(ProgressEventType.GATHER, "res_herb_imperial", 3))
                .orElseThrow();
        assertEquals(1, lifeProgress.completedQuests().size());
        assertTrue(state.itemAmount("mat_refined_herb") >= 1);
        assertTrue(state.lifeExpSnapshot().getOrDefault("herb_gathering", 0L) >= 120L);

        questStateService.startQuest(state, "q_estate_unlock_01").orElseThrow();
        QuestStateService.ProgressUpdateBatch estateProgress = questStateService
                .updateProgress(state, QuestEvent.of(ProgressEventType.COLLECT, "res_herb_imperial", 5))
                .orElseThrow();
        assertEquals(1, estateProgress.completedQuests().size());
        assertTrue(state.hasUnlockFlag("estate_access"));
        assertTrue(state.itemAmount("bp_resonance_extractor") >= 1);

        questStateService.startQuest(state, "q_daily_capital_01").orElseThrow();
        QuestStateService.ProgressUpdateBatch dailyProgress = questStateService
                .updateProgress(state, QuestEvent.of(ProgressEventType.KILL, "training_dummy", 5))
                .orElseThrow();
        assertEquals(1, dailyProgress.completedQuests().size());

        questStateService.updateProgress(state, QuestEvent.of(ProgressEventType.CRAFT, "con_basic_healing_potion", 1)).orElseThrow();
        questStateService.updateProgress(state, QuestEvent.of(ProgressEventType.ESTATE_UNLOCK, "estate_access", 1)).orElseThrow();
        questStateService.updateProgress(state, QuestEvent.of(ProgressEventType.ENTER_REGION, "capital", 1)).orElseThrow();
        questStateService.updateProgress(state, QuestEvent.of(ProgressEventType.BOSS_CLEAR, "ragnes", 1)).orElseThrow();
        questStateService.updateProgress(state, QuestEvent.of(ProgressEventType.NO_DEATH_CLEAR, "ragnes", 1)).orElseThrow();
        questStateService.updateProgress(state, QuestEvent.of(ProgressEventType.EXTREME_CLEAR, "carmen", 1)).orElseThrow();

        assertTrue(state.isAchievementCompleted("ach_first_craft"));
        assertTrue(state.isAchievementCompleted("ach_estate_unlock"));
        assertTrue(state.isAchievementCompleted("ach_region_capital_discover"));
        assertTrue(state.isAchievementCompleted("ach_clear_ragnes"));
        assertTrue(state.isAchievementCompleted("ach_extreme_carmen"));

        achievementRewardService.equipTitle(state, "title_ragnes_clear").orElseThrow();
        achievementRewardService.equipBadge(state, "badge_first_craft").orElseThrow();
        assertEquals("title_ragnes_clear", state.equippedTitleId());
        assertEquals("badge_first_craft", state.equippedBadgeId());

        PlayerQuestAndHonorSnapshotBuilder.Snapshot snapshot = snapshotBuilder.build(state);
        assertTrue(snapshot.completedQuestCount() >= 5);
        assertTrue(snapshot.featuredAchievements().size() >= 1);

        System.out.println("objective_eval_example=talk,kill,gather,collect were processed by QuestObjectiveEvaluator.");
        System.out.println("quest_chain_example=q_spawn_intro_02 complete -> unlocked " + state.unlockedQuestSnapshot());
        System.out.println("quest_reward_example=q_estate_unlock_01 rewards unlock=" + state.hasUnlockFlag("estate_access")
                + ", blueprint=" + state.itemAmount("bp_resonance_extractor"));
        System.out.println("achievement_unlock_example=title=" + state.equippedTitleId()
                + ", badge=" + state.equippedBadgeId()
                + ", hall_of_fame_entries=" + hallOfFameHook.entries().size());
        System.out.println("snapshot_example=active=" + snapshot.activeQuestSummaries().size()
                + ", completed=" + snapshot.completedQuestCount()
                + ", featured=" + snapshot.featuredAchievements().stream().map(PlayerQuestAndHonorSnapshotBuilder.FeaturedAchievementSummary::achievementId).toList());
    }

    private QuestMasterRegistry loadQuestRegistry() {
        Path path = Path.of("src/main/resources/seeds/quest_master.csv");
        CsvSeedLoader<QuestMaster> loader = new CsvSeedLoader<>("quest_master_test", path, this::mapQuest);
        QuestMasterRegistry registry = new QuestMasterRegistry();
        loader.load().orElseThrow().forEach(registry::register);
        return registry;
    }

    private AchievementMasterRegistry loadAchievementRegistry() {
        Path path = Path.of("src/main/resources/seeds/achievement_master.csv");
        CsvSeedLoader<AchievementMaster> loader = new CsvSeedLoader<>("achievement_master_test", path, this::mapAchievement);
        AchievementMasterRegistry registry = new AchievementMasterRegistry();
        loader.load().orElseThrow().forEach(registry::register);
        return registry;
    }

    private QuestRewardRegistry loadQuestRewardRegistry() {
        Path path = Path.of("src/main/resources/seeds/quest_reward_seed.csv");
        CsvSeedLoader<QuestRewardEntry> loader = new CsvSeedLoader<>("quest_reward_seed_test", path, QuestSeedMappers::rewardEntry);
        QuestRewardRegistry registry = new QuestRewardRegistry();
        loader.load().orElseThrow().forEach(registry::register);
        return registry;
    }

    private QuestChainBranchRegistry loadQuestChainRegistry() {
        Path path = Path.of("src/main/resources/seeds/quest_chain_branch.csv");
        CsvSeedLoader<QuestChainBranchRule> loader = new CsvSeedLoader<>("quest_chain_branch_test", path, QuestSeedMappers::chainBranchRule);
        QuestChainBranchRegistry registry = new QuestChainBranchRegistry();
        loader.load().orElseThrow().forEach(registry::register);
        return registry;
    }

    private QuestMaster mapQuest(CsvRow row) {
        return new QuestMaster(
                row.required("quest_id"),
                row.required("quest_type"),
                row.required("quest_name"),
                row.optional("region_code"),
                row.optional("giver_npc_id"),
                row.optional("unlock_condition_type"),
                row.optional("unlock_condition_value"),
                row.optional("objective_type"),
                row.optional("objective_target_id"),
                row.optionalInt("objective_amount", 0),
                row.optional("reward_type"),
                row.optional("reward_target_id"),
                row.optionalInt("reward_amount", 0),
                row.optional("next_quest_id")
        );
    }

    private AchievementMaster mapAchievement(CsvRow row) {
        return new AchievementMaster(
                row.required("achievement_id"),
                row.required("category"),
                row.required("achievement_name"),
                row.required("condition_type"),
                row.optional("condition_target_id"),
                row.optionalInt("condition_amount", 0),
                row.optional("reward_type"),
                row.optional("reward_target_id"),
                row.optionalInt("reward_amount", 0),
                row.optionalBoolean("is_hidden", false),
                row.optionalBoolean("is_repeatable", false),
                row.optional("notes")
        );
    }

    private static final class FixedTimeProvider implements TimeProvider {
        private Instant cursor;

        private FixedTimeProvider(Instant cursor) {
            this.cursor = cursor;
        }

        @Override
        public Instant nowInstant() {
            return cursor;
        }
    }
}
