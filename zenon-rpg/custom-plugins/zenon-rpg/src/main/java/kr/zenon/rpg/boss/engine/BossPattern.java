package kr.zenon.rpg.boss.engine;

import java.util.Locale;
import java.util.Objects;

public record BossPattern(
        String bossId,
        int phaseNo,
        String patternId,
        String patternGroup,
        String priority,
        double unlockHpThreshold,
        int cooldownSeconds,
        int maxConsecutiveUse,
        boolean forced,
        String conditionType,
        String conditionValue,
        String successBranchPatternId,
        String failureBranchPatternId,
        String notes
) {
    public BossPattern {
        Objects.requireNonNull(bossId, "bossId");
        Objects.requireNonNull(patternId, "patternId");
        bossId = normalize(bossId);
        patternId = normalize(patternId);
        patternGroup = normalize(patternGroup).toUpperCase(Locale.ROOT);
        priority = normalize(priority).toUpperCase(Locale.ROOT);
        conditionType = normalize(conditionType).toUpperCase(Locale.ROOT);
        conditionValue = conditionValue == null ? "" : conditionValue.trim();
        successBranchPatternId = normalize(successBranchPatternId);
        failureBranchPatternId = normalize(failureBranchPatternId);
        notes = notes == null ? "" : notes.trim();
    }

    public String compositeKey() {
        return bossId + ":" + patternId;
    }

    public int priorityScore() {
        String lowered = normalize(priority);
        try {
            return Integer.parseInt(lowered);
        } catch (NumberFormatException ignored) {
            return switch (lowered) {
                case "forced" -> 100;
                case "high" -> 80;
                case "mid", "medium" -> 50;
                case "low" -> 20;
                default -> 10;
            };
        }
    }

    public boolean isBerserkPattern() {
        return phaseNo == 0 || "BERSERK".equals(patternGroup);
    }

    public boolean isStaggerPattern() {
        return "STAGGER".equals(patternGroup);
    }

    public boolean hasSuccessBranch() {
        return !successBranchPatternId.isBlank() && !"-".equals(successBranchPatternId);
    }

    public boolean hasFailureBranch() {
        return !failureBranchPatternId.isBlank() && !"-".equals(failureBranchPatternId);
    }

    public boolean isTerminalBranchCode(String patternCode) {
        String normalized = normalize(patternCode);
        return "boss_clear".equals(normalized) || "battle_fail".equals(normalized);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
