package kr.zenon.rpg.boss.engine;

import kr.zenon.rpg.common.result.ErrorCode;
import kr.zenon.rpg.common.result.Result;

import java.util.Objects;

public final class BossStaggerResolver {
    private final BossPatternRegistry patternRegistry;

    public BossStaggerResolver(BossPatternRegistry patternRegistry) {
        this.patternRegistry = Objects.requireNonNull(patternRegistry, "patternRegistry");
    }

    public Result<StaggerResolution> resolve(BossRun run, String triggerPatternId, boolean success) {
        if (run == null || triggerPatternId == null || triggerPatternId.isBlank()) {
            return Result.failure(ErrorCode.INVALID_ARGUMENT, "run and trigger_pattern_id are required");
        }

        BossPattern triggerPattern = patternRegistry.find(run.bossId(), triggerPatternId)
                .orElse(null);
        if (triggerPattern == null) {
            return Result.failure(
                    ErrorCode.INVALID_ARGUMENT,
                    "Stagger trigger pattern not found. boss_id=" + run.bossId() + ", pattern_id=" + triggerPatternId
            );
        }

        String branchPatternId = success
                ? triggerPattern.successBranchPatternId()
                : triggerPattern.failureBranchPatternId();
        if (branchPatternId == null || branchPatternId.isBlank() || "-".equals(branchPatternId)) {
            return Result.success(new StaggerResolution(success, "", false, "", "no_branch"));
        }

        boolean terminal = triggerPattern.isTerminalBranchCode(branchPatternId);
        String terminalResultCode = terminal ? branchPatternId : "";
        return Result.success(new StaggerResolution(
                success,
                branchPatternId,
                terminal,
                terminalResultCode,
                success ? "stagger_success_branch" : "stagger_failure_branch"
        ));
    }

    public record StaggerResolution(
            boolean success,
            String branchPatternId,
            boolean terminal,
            String terminalResultCode,
            String reason
    ) {
    }
}
