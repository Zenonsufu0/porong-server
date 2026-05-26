package com.poro.empire.growth.engine;

import com.poro.empire.common.result.ErrorCode;
import com.poro.empire.common.result.Result;

import java.util.List;

public final class SuccessionService {

    public enum SuccessionType {
        BASIC,
        GRADE_ONLY,
        SUBSTAT_ONLY;

        public long goldCost() {
            return this == BASIC ? 0L : 100_000L;
        }

        public String displayName() {
            return switch (this) {
                case BASIC       -> "기본 전승 §7(무료)";
                case GRADE_ONLY  -> "등급전승권 §7(100,000G)";
                case SUBSTAT_ONLY -> "세부스탯전승권 §7(100,000G)";
            };
        }

        public SuccessionType next() {
            SuccessionType[] vals = values();
            return vals[(ordinal() + 1) % vals.length];
        }
    }

    public record SuccessionResult(
            String sourceInstanceId,
            String targetInstanceId,
            SuccessionType type,
            ItemGrade appliedGrade,
            List<PotentialLine> appliedSubstats
    ) {}

    public Result<SuccessionResult> apply(
            PlayerGrowthState state,
            String sourceInstanceId,
            String targetInstanceId,
            SuccessionType type
    ) {
        PlayerEquipmentItem source = state.inventoryItem(sourceInstanceId).orElse(null);
        if (source == null) {
            return Result.failure(ErrorCode.INVALID_ARGUMENT, "흔적 아이템을 찾을 수 없습니다.");
        }
        PlayerEquipmentItem target = state.inventoryItem(targetInstanceId).orElse(null);
        if (target == null) {
            return Result.failure(ErrorCode.INVALID_ARGUMENT, "대상 아이템을 찾을 수 없습니다.");
        }
        if (sourceInstanceId.equalsIgnoreCase(targetInstanceId)) {
            return Result.failure(ErrorCode.INVALID_ARGUMENT, "흔적과 대상이 동일한 아이템입니다.");
        }
        if (state.equippedItems().containsValue(sourceInstanceId)) {
            return Result.failure(ErrorCode.INVALID_ARGUMENT, "장착 중인 아이템은 흔적으로 사용할 수 없습니다.");
        }

        long goldCost = type.goldCost();
        if (goldCost > 0 && !state.consumeCurrency("gold", goldCost)) {
            return Result.failure(ErrorCode.INVALID_ARGUMENT, "골드가 부족합니다. (필요: " + goldCost + "G)");
        }

        ItemGrade appliedGrade = null;
        List<PotentialLine> appliedSubstats = null;

        if (type == SuccessionType.BASIC || type == SuccessionType.GRADE_ONLY) {
            appliedGrade = source.grade();
            target.setGrade(source.grade());
        }
        if (type == SuccessionType.BASIC || type == SuccessionType.SUBSTAT_ONLY) {
            appliedSubstats = source.substatLines();
            target.setSubstatLines(source.substatLines());
        }

        state.removeInventoryItem(sourceInstanceId);

        return Result.success(new SuccessionResult(
                sourceInstanceId, targetInstanceId, type, appliedGrade, appliedSubstats
        ));
    }
}
