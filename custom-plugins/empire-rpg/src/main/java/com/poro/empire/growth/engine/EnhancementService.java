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

    // 강화 흔적 — 성공률 곱연산 보정 (선택, workshop_crafting_spec §9 / DL-090). 영지 customItems에서 소모.
    // 3종 동시 사용 가능, 보너스 합산 후 한 번 곱: 유효율 = 기본율 × (1 + Σ보너스). 전부 = ×1.70.
    public static final String TRACE_STAR = "mat_trace_star"; // 별의 흔적 ×(1+0.15)
    public static final String TRACE_MOON = "mat_trace_moon"; // 달의 흔적 ×(1+0.25)
    public static final String TRACE_SUN  = "mat_trace_sun";  // 태양의 흔적 ×(1+0.30)

    /** 강화 흔적 사용 최소 강화 단계 — 현재 +10강 이상에서만 적용 (equipment_growth_spec §3.4 / DL-090). */
    public static final int TRACE_MIN_LEVEL = 10;

    /** 강화 흔적 순환/표시 순서 (별→달→태양). */
    public static final java.util.List<String> TRACE_ORDER = java.util.List.of(TRACE_STAR, TRACE_MOON, TRACE_SUN);

    /**
     * 강화 흔적 id → 곱연산 보너스(분수). 별 0.15 / 달 0.25 / 태양 0.30. 미지정/미인식이면 0.
     * 적용: threshold ×= (1 + Σ 선택 흔적 보너스). 전부 사용 시 ×1.70.
     */
    public static double traceMultiplierBonus(String traceId) {
        if (traceId == null) return 0.0;
        return switch (traceId) {
            case TRACE_STAR -> 0.15;
            case TRACE_MOON -> 0.25;
            case TRACE_SUN  -> 0.30;
            default -> 0.0;
        };
    }

    /**
     * 강화 흔적 요구 수량 — 흔적별 × 목표 강화 단계별 (equipment_growth_spec §3.4 / DL-090).
     * 완만한 곡선(시작값, 밸런스 7일차 재산정 대상). 토글 ON 시 해당 수량 전량 소모(all-or-nothing).
     * <pre>
     *   목표 11~17강 : 별1 / 달1 / 태양1
     *   목표 18~22강 : 별2 / 달1 / 태양1
     *   목표 23~25강 : 별2 / 달2 / 태양1
     * </pre>
     *
     * @param traceId      흔적 id
     * @param currentLevel 강화 전 단계 (목표 = currentLevel+1)
     */
    public static int traceCostForLevel(String traceId, int currentLevel) {
        if (traceMultiplierBonus(traceId) <= 0) return 0;
        int target = currentLevel + 1;
        int band = target <= 17 ? 0 : (target <= 22 ? 1 : 2);
        return switch (traceId) {
            case TRACE_STAR -> new int[]{1, 2, 2}[band];
            case TRACE_MOON -> new int[]{1, 1, 2}[band];
            case TRACE_SUN  -> new int[]{1, 1, 1}[band];
            default -> 0;
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
     * 강화 시도 (강화 흔적 선택 가능, DL-090). traceIds의 각 흔적은 현재 +10강 이상 & 단계별 요구 수량
     * 이상 보유 시 롤 분기에서 그 수량을 전량 소모하고, 보너스를 합산해 성공 임계값에 곱연산 적용한다
     * (유효율 = 기본율 × (1 + Σ보너스), 전부 = ×1.70). 천장 강제 성공 시에는 소모하지 않는다.
     *
     * @param island   강화 흔적 소비처 (영지 customItems). null이면 흔적 미사용.
     * @param traceIds mat_trace_star/moon/sun 집합. null/빈값이면 흔적 미사용. 중복은 1회만 처리.
     */
    public Result<EnhancementResult> attempt(PlayerGrowthState state, String itemInstanceId,
                                             com.poro.empire.growth.island.IslandTerritoryState island,
                                             java.util.Collection<String> traceIds, Double fixedRoll) {
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
        String consumedTrace = null; // 소모된 강화 흔적 id 목록(쉼표 구분), 천장 강제 성공 시 null

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
            // 강화 흔적 — 현재 +10강 이상에서만, 토글된 흔적별 요구 수량 전량 소모하고 보너스 합산해 곱연산 (선택, DL-090)
            if (currentLevel >= TRACE_MIN_LEVEL && island != null && traceIds != null && !traceIds.isEmpty()) {
                double bonusSum = 0.0;
                java.util.List<String> consumed = new java.util.ArrayList<>();
                for (String tid : new java.util.LinkedHashSet<>(traceIds)) { // 중복 제거
                    double b = traceMultiplierBonus(tid);
                    if (b <= 0) continue;
                    int cost = traceCostForLevel(tid, currentLevel);
                    if (cost > 0 && island.getCustomItem(tid) >= cost && island.withdrawCustomItem(tid, cost)) {
                        bonusSum += b;
                        consumed.add(tid);
                    }
                }
                if (bonusSum > 0) {
                    threshold = Math.min(threshold * (1.0 + bonusSum), 1.0);
                    consumedTrace = String.join(",", consumed);
                }
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
