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

    // 강화 흔적 — 성공률 %p 보정 (선택, workshop_crafting_spec §9 / DL-089). 영지 customItems에서 소모.
    public static final String TRACE_STAR = "mat_trace_star"; // 별의 흔적 +20%p
    public static final String TRACE_MOON = "mat_trace_moon"; // 달의 흔적 +30%p
    public static final String TRACE_SUN  = "mat_trace_sun";  // 태양의 흔적 +50%p

    /** 강화 흔적 사용 최소 강화 단계 — 현재 +10강 이상에서만 적용 (equipment_growth_spec §3.4 / DL-089). */
    public static final int TRACE_MIN_LEVEL = 10;

    /** 강화 흔적 id → 성공률 보정(%p). 미지정/미인식이면 0. */
    public static double traceBonusFor(String traceId) {
        if (traceId == null) return 0.0;
        return switch (traceId) {
            case TRACE_STAR -> 20.0;
            case TRACE_MOON -> 30.0;
            case TRACE_SUN  -> 50.0;
            default -> 0.0;
        };
    }

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
        return attempt(state, itemInstanceId, null, null, null);
    }

    public Result<EnhancementResult> attempt(PlayerGrowthState state, String itemInstanceId, Double fixedRoll) {
        return attempt(state, itemInstanceId, null, null, fixedRoll);
    }

    /**
     * 강화 시도 (강화 흔적 선택 가능). traceId가 주어지고 영지에 보유 시 롤 분기에서 1개 소모하고
     * 성공률에 %p 보정(별 +20 / 달 +30 / 태양 +50). 천장 강제 성공 시에는 소모하지 않는다.
     *
     * @param island  강화 흔적 소비처 (영지 customItems). null이면 흔적 미사용.
     * @param traceId mat_trace_star/moon/sun. null이면 흔적 미사용.
     */
    public Result<EnhancementResult> attempt(PlayerGrowthState state, String itemInstanceId,
                                             com.poro.empire.growth.island.IslandTerritoryState island,
                                             String traceId, Double fixedRoll) {
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

        // 강화석: 시드값은 무기 기준. 방어구는 ceil(무기 ÷ 1.5) (economy_numbers_v2 / DL-033).
        long stoneCost = rule.stoneCost();
        if (!"weapon".equalsIgnoreCase(itemMaster.slotType())) {
            stoneCost = (long) Math.ceil(stoneCost / 1.5);
        }

        if (!state.consumeCurrency(CURRENCY_GOLD, rule.goldCost())) {
            return Result.failure(
                    ErrorCode.INVALID_ARGUMENT,
                    "Not enough gold. required=" + rule.goldCost() + ", current=" + state.currency(CURRENCY_GOLD)
            );
        }
        if (!state.consumeCurrency(MATERIAL_ENHANCE_STONE, stoneCost)) {
            state.addCurrency(CURRENCY_GOLD, rule.goldCost());
            return Result.failure(
                    ErrorCode.INVALID_ARGUMENT,
                    "Not enough enhancement stone. required=" + stoneCost + ", current=" + state.currency(MATERIAL_ENHANCE_STONE)
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
        String consumedTrace = null; // 실제 소모된 강화 흔적 id (천장 강제 성공 시 null)

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
            // 강화 흔적 — 현재 +10강 이상에서만, 보유 시 1개 소모하고 성공률 %p 보정 (선택, DL-089)
            double traceBonus = traceBonusFor(traceId);
            if (traceBonus > 0 && currentLevel >= TRACE_MIN_LEVEL
                    && island != null && island.withdrawCustomItem(traceId, 1)) {
                threshold = Math.min(threshold + traceBonus / 100.0, 1.0);
                consumedTrace = traceId;
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
                stoneCost,
                ceilingCountAfter,
                ceilingCap,
                forcedByCeiling,
                consumedTrace
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
            boolean forcedByCeiling,
            String traceId
    ) {
    }
}
