package kr.zenon.rpg.boss.engine;

import java.util.List;
import java.util.Objects;

public final class BossBerserkService {
    private final BossPatternRegistry patternRegistry;

    public BossBerserkService(BossPatternRegistry patternRegistry) {
        this.patternRegistry = Objects.requireNonNull(patternRegistry, "patternRegistry");
    }

    public BerserkTransition enterIfNeeded(BossRun run) {
        if (run.berserkEntered() || run.bossHpPercent() > 0.0d) {
            return new BerserkTransition(false, List.of());
        }
        run.markBerserkEntered();
        List<String> forcedPatternIds = patternRegistry.forcedByHpZero(run.bossId()).stream()
                .map(BossPattern::patternId)
                .toList();
        return new BerserkTransition(true, forcedPatternIds);
    }

    public BerserkOutcome resolveOutcome(BossRun run, boolean success) {
        BossPattern berserkPattern = patternRegistry.forcedByHpZero(run.bossId()).stream()
                .findFirst()
                .orElse(null);

        if (success) {
            String branch = berserkPattern == null ? "boss_clear" : emptyToDefault(berserkPattern.successBranchPatternId(), "boss_clear");
            return new BerserkOutcome(true, branch, "");
        }
        String branch = berserkPattern == null ? "battle_fail" : emptyToDefault(berserkPattern.failureBranchPatternId(), "battle_fail");
        String failureCode = "battle_fail".equals(branch) ? "fail_berserk_timeout" : branch;
        return new BerserkOutcome(false, branch, failureCode);
    }

    private String emptyToDefault(String value, String fallback) {
        if (value == null || value.isBlank() || "-".equals(value)) {
            return fallback;
        }
        return value;
    }

    public record BerserkTransition(
            boolean entered,
            List<String> forcedPatternIds
    ) {
    }

    public record BerserkOutcome(
            boolean clearSuccess,
            String branchCode,
            String failureReasonCode
    ) {
    }
}
