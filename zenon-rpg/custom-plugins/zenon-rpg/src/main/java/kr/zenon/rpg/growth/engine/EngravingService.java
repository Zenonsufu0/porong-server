package kr.zenon.rpg.growth.engine;

import kr.zenon.rpg.common.result.ErrorCode;
import kr.zenon.rpg.common.result.Result;

import java.util.Locale;
import java.util.Objects;

public final class EngravingService {
    private static final int MIN_COMMON_SLOT = 1;
    private static final int MAX_COMMON_SLOT = 3;

    private final EngravingRegistry engravingRegistry;

    public EngravingService(EngravingRegistry engravingRegistry) {
        this.engravingRegistry = Objects.requireNonNull(engravingRegistry, "engravingRegistry");
    }

    public Result<Void> equipClassEngraving(PlayerGrowthState state, String engravingId) {
        EngravingMaster engraving = engravingRegistry.find(engravingId).orElse(null);
        if (engraving == null) {
            return Result.failure(ErrorCode.INVALID_ARGUMENT, "Unknown class engraving: " + engravingId);
        }
        if (!engraving.classType()) {
            return Result.failure(ErrorCode.INVALID_ARGUMENT, "Engraving is not class type: " + engravingId);
        }
        if (!engraving.classFilter().isBlank() && !"all".equals(engraving.classFilter())
                && !engraving.classFilter().equals(normalized(state.classId()))) {
            return Result.failure(
                    ErrorCode.INVALID_ARGUMENT,
                    "Class engraving does not match class. required=" + engraving.classFilter() + ", actual=" + state.classId()
            );
        }

        state.setClassEngravingId(engraving.engravingId());
        return Result.success();
    }

    public Result<Void> equipCommonEngraving(PlayerGrowthState state, int slotNo, String engravingId, int level) {
        if (slotNo < MIN_COMMON_SLOT || slotNo > MAX_COMMON_SLOT) {
            return Result.failure(ErrorCode.INVALID_ARGUMENT, "Common engraving slot must be between 1 and 3.");
        }
        EngravingMaster engraving = engravingRegistry.find(engravingId).orElse(null);
        if (engraving == null) {
            return Result.failure(ErrorCode.INVALID_ARGUMENT, "Unknown common engraving: " + engravingId);
        }
        if (!engraving.commonType()) {
            return Result.failure(ErrorCode.INVALID_ARGUMENT, "Engraving is not common type: " + engravingId);
        }
        int validatedLevel = Math.max(1, level);
        if (validatedLevel > engraving.maxLevel()) {
            return Result.failure(
                    ErrorCode.INVALID_ARGUMENT,
                    "Requested common engraving level exceeds max. requested=" + validatedLevel + ", max=" + engraving.maxLevel()
            );
        }

        state.equipCommonEngraving(slotNo, new PlayerGrowthState.EquippedCommonEngraving(engraving.engravingId(), validatedLevel));
        return Result.success();
    }

    public Result<Void> unequipCommonEngraving(PlayerGrowthState state, int slotNo) {
        if (slotNo < MIN_COMMON_SLOT || slotNo > MAX_COMMON_SLOT) {
            return Result.failure(ErrorCode.INVALID_ARGUMENT, "Common engraving slot must be between 1 and 3.");
        }
        state.unequipCommonEngraving(slotNo);
        return Result.success();
    }

    public GrowthStatBlock buildStatBlock(PlayerGrowthState state) {
        GrowthStatBlock block = new GrowthStatBlock();

        if (!state.classEngravingId().isBlank()) {
            EngravingMaster classEngraving = engravingRegistry.find(state.classEngravingId()).orElse(null);
            if (classEngraving != null) {
                apply(block, classEngraving, 1);
            }
        }

        for (PlayerGrowthState.EquippedCommonEngraving equipped : state.commonEngravings().values()) {
            EngravingMaster commonEngraving = engravingRegistry.find(equipped.engravingId()).orElse(null);
            if (commonEngraving == null) {
                continue;
            }
            apply(block, commonEngraving, equipped.level());
        }
        return block;
    }

    private void apply(GrowthStatBlock block, EngravingMaster engraving, int level) {
        double value = engraving.valuePerLevel() * Math.max(1, level);
        if ("FLAG".equalsIgnoreCase(engraving.valueType())) {
            block.addFlag(engraving.effectType());
            return;
        }
        block.add(engraving.effectType(), value);
    }

    private String normalized(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
