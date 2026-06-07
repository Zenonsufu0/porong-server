package com.poro.rpg.boss.engine;

import com.poro.rpg.common.logging.DomainLogger;
import com.poro.rpg.common.registry.master.BossMasterRegistry;
import com.poro.rpg.common.result.ErrorCode;
import com.poro.rpg.common.result.Result;

import java.util.Objects;
import java.util.function.Predicate;

public final class BossEntryValidator {
    private final BossMasterRegistry bossMasterRegistry;
    private final BossEntryRuleRegistry entryRuleRegistry;
    private final UnlockQuestChecker unlockQuestChecker;
    private final DomainLogger logger;

    public BossEntryValidator(
            BossMasterRegistry bossMasterRegistry,
            BossEntryRuleRegistry entryRuleRegistry,
            UnlockQuestChecker unlockQuestChecker,
            DomainLogger logger
    ) {
        this.bossMasterRegistry = Objects.requireNonNull(bossMasterRegistry, "bossMasterRegistry");
        this.entryRuleRegistry = Objects.requireNonNull(entryRuleRegistry, "entryRuleRegistry");
        this.unlockQuestChecker = Objects.requireNonNull(unlockQuestChecker, "unlockQuestChecker");
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    public Result<BossEntryRule> validate(BossEntryRequest request, Predicate<String> activeRunParticipantChecker) {
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(activeRunParticipantChecker, "activeRunParticipantChecker");

        if (request.bossId().isBlank()) {
            return Result.failure(ErrorCode.INVALID_ARGUMENT, "boss_id must not be blank");
        }
        if (request.leaderUserId().isBlank()) {
            return Result.failure(ErrorCode.INVALID_ARGUMENT, "leader_user_id must not be blank");
        }
        if (request.partyMemberIds().isEmpty()) {
            return Result.failure(ErrorCode.INVALID_ARGUMENT, "party_member_ids must not be empty");
        }
        if (!request.partyMemberIds().contains(request.leaderUserId())) {
            return Result.failure(ErrorCode.INVALID_ARGUMENT, "Leader must be included in party_member_ids");
        }
        if (request.uniquePartyMemberIds().size() != request.partyMemberIds().size()) {
            return Result.failure(ErrorCode.INVALID_ARGUMENT, "Duplicated party member ids are not allowed");
        }

        if (bossMasterRegistry.find(request.bossId()).isEmpty()) {
            return Result.failure(ErrorCode.INVALID_ARGUMENT, "Unknown boss_id: " + request.bossId());
        }

        BossEntryRule entryRule = entryRuleRegistry.find(request.bossId())
                .orElseGet(() -> new BossEntryRule(request.bossId(), "", 1, 3, "", null, null, "fallback_default"));

        if (request.partySize() < entryRule.requiresPartySizeMin() || request.partySize() > entryRule.requiresPartySizeMax()) {
            return Result.failure(
                    ErrorCode.INVALID_ARGUMENT,
                    "Party size is out of allowed range. boss_id=" + request.bossId()
                            + ", allowed=" + entryRule.requiresPartySizeMin() + "~" + entryRule.requiresPartySizeMax()
                            + ", actual=" + request.partySize()
            );
        }

        for (String userId : request.partyMemberIds()) {
            if (activeRunParticipantChecker.test(userId)) {
                return Result.failure(
                        ErrorCode.INVALID_ARGUMENT,
                        "Party member is already in another active boss run: " + userId
                );
            }
        }

        if (entryRule.hasUnlockQuest()) {
            for (String userId : request.partyMemberIds()) {
                if (!unlockQuestChecker.hasUnlocked(userId, entryRule.requiresUnlockQuest())) {
                    return Result.failure(
                            ErrorCode.INVALID_ARGUMENT,
                            "Unlock quest not completed. user_id=" + userId + ", required=" + entryRule.requiresUnlockQuest()
                    );
                }
            }
        }

        if (entryRule.dailyLimitOpt().isPresent() || entryRule.weeklyLimitOpt().isPresent()) {
            logger.warn("daily_limit/weekly_limit are configured but currently not enforced. boss_id=" + entryRule.bossId());
        }

        return Result.success(entryRule);
    }
}
