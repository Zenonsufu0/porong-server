package kr.zenon.rpg.life.engine;

import kr.zenon.rpg.common.result.ErrorCode;
import kr.zenon.rpg.common.result.Result;
import kr.zenon.rpg.common.time.TimeProvider;

import java.util.Objects;

public final class EstateUnlockService {
    private final EstateUnlockRuleRegistry unlockRuleRegistry;
    private final EstateUnlockQuestHook unlockQuestHook;
    private final TimeProvider timeProvider;

    public EstateUnlockService(
            EstateUnlockRuleRegistry unlockRuleRegistry,
            EstateUnlockQuestHook unlockQuestHook,
            TimeProvider timeProvider
    ) {
        this.unlockRuleRegistry = Objects.requireNonNull(unlockRuleRegistry, "unlockRuleRegistry");
        this.unlockQuestHook = Objects.requireNonNull(unlockQuestHook, "unlockQuestHook");
        this.timeProvider = Objects.requireNonNull(timeProvider, "timeProvider");
    }

    public Result<UnlockResult> unlock(PlayerLifeState state, String estateId) {
        if (state == null) {
            return Result.failure(ErrorCode.INVALID_ARGUMENT, "state is required.");
        }
        if (state.estateState().isPresent()) {
            return Result.failure(ErrorCode.INVALID_ARGUMENT, "Estate is already unlocked for user=" + state.userId());
        }

        EstateUnlockRule rule = unlockRuleRegistry.find(estateId).orElse(null);
        if (rule == null) {
            return Result.failure(ErrorCode.INVALID_ARGUMENT, "Unknown estate_id: " + estateId);
        }

        if (!unlockQuestHook.canUnlock(state, rule.unlockQuestId())) {
            return Result.failure(
                    ErrorCode.INVALID_ARGUMENT,
                    "Estate unlock quest is not completed. required_quest_id=" + rule.unlockQuestId()
            );
        }

        EstateState estateState = new EstateState(
                rule.estateId(),
                rule.initialSlotCapacity(),
                timeProvider.nowInstant()
        );
        state.setEstateState(estateState);

        return Result.success(new UnlockResult(
                estateState.estateId(),
                rule.unlockQuestId(),
                estateState.slotCapacity(),
                estateState.slotCapacity() >= 1,
                rule.firstInstallFacilityId()
        ));
    }

    public record UnlockResult(
            String estateId,
            String unlockQuestId,
            int slotCapacity,
            boolean firstFacilityInstallReady,
            String suggestedFirstFacilityId
    ) {
    }
}
