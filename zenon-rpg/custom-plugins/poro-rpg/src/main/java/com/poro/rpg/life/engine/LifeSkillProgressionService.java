package com.poro.rpg.life.engine;

import com.poro.rpg.common.result.ErrorCode;
import com.poro.rpg.common.result.Result;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class LifeSkillProgressionService {
    private final LifeSkillExpRegistry expRegistry;
    private final LifeLevelUnlockHook levelUnlockHook;

    public LifeSkillProgressionService(
            LifeSkillExpRegistry expRegistry,
            LifeLevelUnlockHook levelUnlockHook
    ) {
        this.expRegistry = Objects.requireNonNull(expRegistry, "expRegistry");
        this.levelUnlockHook = Objects.requireNonNull(levelUnlockHook, "levelUnlockHook");
    }

    public Result<ProgressionUpdate> gainExp(PlayerLifeState state, LifeType lifeType, long amount) {
        if (state == null) {
            return Result.failure(ErrorCode.INVALID_ARGUMENT, "state is required.");
        }
        if (lifeType == null) {
            return Result.failure(ErrorCode.INVALID_ARGUMENT, "lifeType is required.");
        }
        if (amount < 0L) {
            return Result.failure(ErrorCode.INVALID_ARGUMENT, "amount must be >= 0.");
        }
        if (expRegistry.rules(lifeType).isEmpty()) {
            return Result.failure(ErrorCode.INVALID_ARGUMENT, "No exp table registered for life_type=" + lifeType.code());
        }

        PlayerLifeState.LifeSkillProfile before = state.lifeProfile(lifeType);
        long afterExp = safeAdd(before.exp(), amount);
        int beforeLevel = before.level();
        int afterLevel = expRegistry.resolveLevel(lifeType, afterExp);
        state.setLifeProfile(lifeType, new PlayerLifeState.LifeSkillProfile(afterLevel, afterExp));

        List<String> unlockedCodes = new ArrayList<>();
        if (afterLevel > beforeLevel) {
            for (int level = beforeLevel + 1; level <= afterLevel; level++) {
                LifeSkillExpRule rule = expRegistry.find(lifeType, level).orElse(null);
                if (rule == null) {
                    continue;
                }
                String unlockCode = normalize(rule.unlockCode());
                if (!unlockCode.isBlank() && !"none".equals(unlockCode) && !"-".equals(unlockCode)) {
                    unlockedCodes.add(unlockCode);
                    levelUnlockHook.onLevelUnlocked(state, lifeType, level, unlockCode);
                }
            }
        }

        return Result.success(new ProgressionUpdate(
                lifeType,
                beforeLevel,
                afterLevel,
                before.exp(),
                afterExp,
                List.copyOf(unlockedCodes)
        ));
    }

    public LifeBonusProfile bonusFor(PlayerLifeState state, LifeType lifeType) {
        if (state == null || lifeType == null) {
            return LifeBonusProfile.ZERO;
        }
        PlayerLifeState.LifeSkillProfile profile = state.lifeProfile(lifeType);
        return expRegistry.resolveRuleByExp(lifeType, profile.exp())
                .map(rule -> new LifeBonusProfile(
                        rule.gatherSpeedBonusPct(),
                        rule.yieldBonusPct(),
                        rule.rareBonusPct(),
                        rule.estateOutputBonusPct()
                ))
                .orElse(LifeBonusProfile.ZERO);
    }

    private long safeAdd(long current, long amount) {
        try {
            return Math.addExact(current, amount);
        } catch (ArithmeticException overflow) {
            return Long.MAX_VALUE;
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    public record ProgressionUpdate(
            LifeType lifeType,
            int beforeLevel,
            int afterLevel,
            long beforeExp,
            long afterExp,
            List<String> unlockedCodes
    ) {
    }
}
