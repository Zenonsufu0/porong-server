package com.poro.rpg.boss.engine;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

public record BossEntryRule(
        String bossId,
        String requiresUnlockQuest,
        int requiresPartySizeMin,
        int requiresPartySizeMax,
        String requiresKeyItem,
        Integer dailyLimit,
        Integer weeklyLimit,
        String notes
) {
    public BossEntryRule {
        Objects.requireNonNull(bossId, "bossId");
        bossId = normalize(bossId);
        requiresUnlockQuest = normalize(requiresUnlockQuest);
        requiresKeyItem = normalize(requiresKeyItem);
        notes = notes == null ? "" : notes.trim();
    }

    public Optional<Integer> dailyLimitOpt() {
        return Optional.ofNullable(dailyLimit);
    }

    public Optional<Integer> weeklyLimitOpt() {
        return Optional.ofNullable(weeklyLimit);
    }

    public boolean hasUnlockQuest() {
        return !requiresUnlockQuest.isBlank() && !"none".equals(requiresUnlockQuest);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
