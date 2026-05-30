package com.poro.empire.growth.engine;

import com.poro.empire.common.registry.master.ItemMasterRegistry;
import com.poro.empire.common.registry.master.model.ItemMaster;
import com.poro.empire.common.result.ErrorCode;
import com.poro.empire.common.result.Result;

import java.util.Map;
import java.util.Objects;
import java.util.function.BooleanSupplier;

public final class EnhancementService {
    public static final int MAX_ENHANCE_LEVEL = 25;
    public static final String CURRENCY_GOLD = "gold";
    public static final String MATERIAL_ENHANCE_STONE = "mat_stone_enhance";

    private final ItemMasterRegistry itemMasterRegistry;
    private final EnhancementRuleRegistry enhancementRuleRegistry;
    private final EnhancementLogHook enhancementLogHook;
    private final RandomProvider randomProvider;
    /**
     * 운영자 강화 부스트 토글 연동 (Step 2b). 도메인↔운영 역결합을 피하기 위해 BooleanSupplier로 주입.
     * true면 성공 임계값 ×2 (1.0 클램프). 기본값은 항상 false.
     */
    private BooleanSupplier enhanceBoostSupplier = () -> false;

    public EnhancementService(
            ItemMasterRegistry itemMasterRegistry,
            EnhancementRuleRegistry enhancementRuleRegistry,
            EnhancementLogHook enhancementLogHook,
            RandomProvider randomProvider
    ) {
        this.itemMasterRegistry = Objects.requireNonNull(itemMasterRegistry, "itemMasterRegistry");
        this.enhancementRuleRegistry = Objects.requireNonNull(enhancementRuleRegistry, "enhancementRuleRegistry");
        this.enhancementLogHook = Objects.requireNonNull(enhancementLogHook, "enhancementLogHook");
        this.randomProvider = Objects.requireNonNull(randomProvider, "randomProvider");
    }

    /** 운영자 강화 부스트 토글 공급자 주입 (EmpireRPGPlugin 와이어링). */
    public void setEnhanceBoostSupplier(BooleanSupplier enhanceBoostSupplier) {
        this.enhanceBoostSupplier = Objects.requireNonNull(enhanceBoostSupplier, "enhanceBoostSupplier");
    }

    public Result<EnhancementResult> attempt(PlayerGrowthState state, String itemInstanceId) {
        return attempt(state, itemInstanceId, null);
    }

    public Result<EnhancementResult> attempt(PlayerGrowthState state, String itemInstanceId, Double fixedRoll) {
        Objects.requireNonNull(state, "state");
        PlayerEquipmentItem item = state.inventoryItem(itemInstanceId).orElse(null);
        if (item == null) {
            return Result.failure(ErrorCode.INVALID_ARGUMENT, "Item instance not found: " + itemInstanceId);
        }

        ItemMaster itemMaster = itemMasterRegistry.find(item.itemId()).orElse(null);
        if (itemMaster == null) {
            return Result.failure(ErrorCode.INVALID_ARGUMENT, "Unknown item master for enhancement: " + item.itemId());
        }

        int currentLevel = item.enhanceLevel();
        if (currentLevel >= MAX_ENHANCE_LEVEL) {
            return Result.failure(ErrorCode.INVALID_ARGUMENT, "Enhancement is already at max level: " + currentLevel);
        }

        int targetLevel = currentLevel + 1;
        GrowthTier tier = GrowthTier.from(itemMaster.tier());
        EnhancementRule rule = enhancementRuleRegistry.find(tier, targetLevel).orElse(null);
        if (rule == null) {
            return Result.failure(
                    ErrorCode.INVALID_ARGUMENT,
                    "Enhancement rule not found. tier=" + tier + ", level=" + targetLevel
            );
        }

        if (!state.consumeCurrency(CURRENCY_GOLD, rule.goldCost())) {
            return Result.failure(
                    ErrorCode.INVALID_ARGUMENT,
                    "Not enough gold. required=" + rule.goldCost() + ", current=" + state.currency(CURRENCY_GOLD)
            );
        }
        if (!state.consumeCurrency(MATERIAL_ENHANCE_STONE, rule.stoneCost())) {
            state.addCurrency(CURRENCY_GOLD, rule.goldCost());
            return Result.failure(
                    ErrorCode.INVALID_ARGUMENT,
                    "Not enough enhancement stone. required=" + rule.stoneCost() + ", current=" + state.currency(MATERIAL_ENHANCE_STONE)
            );
        }

        // 천장 카운터 조회 (11강 이상, 장착 슬롯 아이템만 적용)
        String counterKey = null;
        int ceilingCap = 0;
        int ceilingCount = 0;
        if (targetLevel >= 11) {
            for (Map.Entry<EquipmentSlot, String> e : state.equippedItems().entrySet()) {
                if (e.getValue().equalsIgnoreCase(itemInstanceId)) {
                    counterKey = e.getKey().name().toLowerCase() + "_" + targetLevel;
                    break;
                }
            }
            if (counterKey != null) {
                ceilingCap = (int) Math.ceil(200.0 / rule.successRate());
                ceilingCount = state.getCeilingCounter(counterKey);
            }
        }

        double roll;
        boolean success;
        boolean forcedByCeiling = false;
        int finalLevel = currentLevel;

        if (counterKey != null && ceilingCount >= ceilingCap) {
            roll = -1.0; // 천장 강제 성공 마킹
            success = true;
            forcedByCeiling = true;
            state.resetCeilingCounter(counterKey);
        } else {
            roll = fixedRoll == null ? randomProvider.nextDouble() : fixedRoll;
            double threshold = rule.successRate() / 100.0d;
            int catalystBonus = state.drainCatalystBonus();
            if (catalystBonus > 0) {
                threshold = Math.min(threshold + catalystBonus / 100.0, 1.0);
            }
            // 운영자 강화 부스트 토글 (ENHANCE_BOOST): 성공 임계값 ×2, 1.0 클램프.
            if (enhanceBoostSupplier.getAsBoolean()) {
                threshold = Math.min(threshold * 2.0, 1.0);
            }
            success = roll <= threshold;
            if (counterKey != null) {
                if (success) state.resetCeilingCounter(counterKey);
                else state.incrementCeilingCounter(counterKey);
            }
        }

        if (success) {
            finalLevel = targetLevel;
            item.setEnhanceLevel(finalLevel);
        }

        int ceilingCountAfter = counterKey != null ? state.getCeilingCounter(counterKey) : 0;

        EnhancementResult result = new EnhancementResult(
                state.userId(),
                item.itemInstanceId(),
                item.itemId(),
                tier.name(),
                currentLevel,
                targetLevel,
                finalLevel,
                success,
                rule.successRate(),
                roll,
                rule.goldCost(),
                rule.stoneCost(),
                ceilingCountAfter,
                ceilingCap,
                forcedByCeiling
        );
        enhancementLogHook.onAttempt(result);
        return Result.success(result);
    }

    public record EnhancementResult(
            String userId,
            String itemInstanceId,
            String itemId,
            String tier,
            int beforeLevel,
            int targetLevel,
            int finalLevel,
            boolean success,
            double successRate,
            double roll,
            long goldCost,
            long stoneCost,
            int ceilingCount,
            int ceilingCap,
            boolean forcedByCeiling
    ) {
    }
}
