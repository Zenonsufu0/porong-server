package com.poro.rpg.boss.engine;

import com.poro.rpg.common.result.ErrorCode;
import com.poro.rpg.common.result.Result;

public final class BossDeathCountService {
    public int initialDeathCount(int partySize) {
        if (partySize <= 1) {
            return 3;
        }
        if (partySize == 2) {
            return 4;
        }
        return 5;
    }

    public Result<DeathCountUpdate> registerDeath(BossRun run, String userId, boolean partyWipe) {
        if (run == null || userId == null || userId.isBlank()) {
            return Result.failure(ErrorCode.INVALID_ARGUMENT, "run and user_id are required");
        }
        if (!run.participants().contains(userId)) {
            return Result.failure(ErrorCode.INVALID_ARGUMENT, "Unknown participant in this run: " + userId);
        }

        run.registerDeath(userId);

        boolean battleFailed = false;
        String failureReasonCode = "";
        if (partyWipe) {
            battleFailed = true;
            failureReasonCode = "fail_party_wipe";
        } else if (run.remainingDeathCount() <= 0) {
            battleFailed = true;
            failureReasonCode = "fail_deathcount_zero";
        }

        boolean revivePossible = !battleFailed && run.remainingDeathCount() > 0;
        return Result.success(new DeathCountUpdate(
                run.remainingDeathCount(),
                revivePossible,
                battleFailed,
                failureReasonCode
        ));
    }

    public record DeathCountUpdate(
            int remainingDeathCount,
            boolean revivePossible,
            boolean battleFailed,
            String failureReasonCode
    ) {
    }
}
