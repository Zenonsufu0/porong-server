package kr.zenon.rpg.quest.engine;

import kr.zenon.rpg.common.registry.master.model.QuestMaster;
import kr.zenon.rpg.common.result.ErrorCode;
import kr.zenon.rpg.common.result.Result;
import kr.zenon.rpg.common.time.TimeProvider;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public final class QuestRewardResolver {
    private final QuestRewardRegistry rewardRegistry;
    private final QuestRecordRewardHook recordRewardHook;
    private final TimeProvider timeProvider;

    public QuestRewardResolver(
            QuestRewardRegistry rewardRegistry,
            QuestRecordRewardHook recordRewardHook,
            TimeProvider timeProvider
    ) {
        this.rewardRegistry = Objects.requireNonNull(rewardRegistry, "rewardRegistry");
        this.recordRewardHook = Objects.requireNonNull(recordRewardHook, "recordRewardHook");
        this.timeProvider = Objects.requireNonNull(timeProvider, "timeProvider");
    }

    public Result<RewardResolution> resolve(PlayerQuestHonorState state, QuestMaster quest) {
        if (state == null || quest == null) {
            return Result.failure(ErrorCode.INVALID_ARGUMENT, "state and quest are required.");
        }

        List<QuestRewardEntry> rewards = new ArrayList<>();
        List<QuestRewardEntry> seededRewards = rewardRegistry.rewards(quest.questId());
        if (seededRewards.isEmpty()) {
            QuestRewardEntry inlineReward = inlineReward(quest);
            if (inlineReward != null) {
                rewards.add(inlineReward);
            }
        } else {
            rewards.addAll(seededRewards);
        }

        List<GrantedReward> granted = new ArrayList<>();
        List<SkippedReward> skipped = new ArrayList<>();

        for (QuestRewardEntry reward : rewards) {
            String rewardKey = reward.rewardKey();
            if (reward.firstClearOnly() && state.hasClaimedQuestReward(rewardKey)) {
                skipped.add(new SkippedReward(reward.rewardType(), reward.rewardTargetId(), reward.rewardAmount(), "first_clear_only_already_claimed"));
                continue;
            }

            applyReward(state, quest.questId(), reward, granted);
            if (reward.firstClearOnly()) {
                state.markQuestRewardClaimed(rewardKey);
            }
        }

        return Result.success(new RewardResolution(
                quest.questId(),
                timeProvider.nowInstant(),
                List.copyOf(granted),
                List.copyOf(skipped)
        ));
    }

    private void applyReward(
            PlayerQuestHonorState state,
            String questId,
            QuestRewardEntry reward,
            List<GrantedReward> granted
    ) {
        switch (reward.rewardType()) {
            case GOLD -> {
                state.addCurrency("gold", reward.rewardAmount());
                granted.add(new GrantedReward(reward.rewardType(), "gold", reward.rewardAmount(), Map.of()));
            }
            case ITEM -> {
                state.addItem(reward.rewardTargetId(), reward.rewardAmount());
                granted.add(new GrantedReward(reward.rewardType(), reward.rewardTargetId(), reward.rewardAmount(), Map.of()));
            }
            case LIFE_EXP -> {
                state.addLifeExp(reward.rewardTargetId(), reward.rewardAmount());
                granted.add(new GrantedReward(reward.rewardType(), reward.rewardTargetId(), reward.rewardAmount(), Map.of()));
            }
            case UNLOCK -> {
                state.unlockFlag(reward.rewardTargetId());
                granted.add(new GrantedReward(reward.rewardType(), reward.rewardTargetId(), reward.rewardAmount(), Map.of()));
            }
            case TITLE -> {
                state.unlockTitle(reward.rewardTargetId());
                granted.add(new GrantedReward(reward.rewardType(), reward.rewardTargetId(), reward.rewardAmount(), Map.of("title_unlocked", "true")));
            }
            case BADGE -> {
                state.unlockBadge(reward.rewardTargetId());
                granted.add(new GrantedReward(reward.rewardType(), reward.rewardTargetId(), reward.rewardAmount(), Map.of("badge_unlocked", "true")));
            }
            case RECORD -> {
                state.addRecordStat(reward.rewardTargetId(), reward.rewardAmount());
                recordRewardHook.onQuestRecordReward(state, normalize(questId), reward.rewardTargetId(), reward.rewardAmount());
                granted.add(new GrantedReward(reward.rewardType(), reward.rewardTargetId(), reward.rewardAmount(), Map.of("record_hook", "called")));
            }
        }
    }

    private QuestRewardEntry inlineReward(QuestMaster quest) {
        String rewardTypeRaw = normalize(quest.rewardType());
        if (rewardTypeRaw.isBlank() || "none".equals(rewardTypeRaw) || "-".equals(rewardTypeRaw)) {
            return null;
        }
        QuestRewardType rewardType;
        try {
            rewardType = QuestRewardType.from(rewardTypeRaw);
        } catch (Exception ignored) {
            return null;
        }
        long amount = Math.max(0, quest.rewardAmount());
        if (amount <= 0L && rewardType != QuestRewardType.UNLOCK && rewardType != QuestRewardType.TITLE && rewardType != QuestRewardType.BADGE) {
            return null;
        }
        return new QuestRewardEntry(
                quest.questId(),
                0,
                rewardType,
                quest.rewardTargetId(),
                Math.max(1L, amount),
                false,
                "inline_reward"
        );
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    public record RewardResolution(
            String questId,
            Instant resolvedAt,
            List<GrantedReward> grantedRewards,
            List<SkippedReward> skippedRewards
    ) {
    }

    public record GrantedReward(
            QuestRewardType rewardType,
            String rewardTargetId,
            long rewardAmount,
            Map<String, String> metadata
    ) {
        public GrantedReward {
            metadata = metadata == null ? Map.of() : Map.copyOf(new LinkedHashMap<>(metadata));
        }
    }

    public record SkippedReward(
            QuestRewardType rewardType,
            String rewardTargetId,
            long rewardAmount,
            String reason
    ) {
    }
}
