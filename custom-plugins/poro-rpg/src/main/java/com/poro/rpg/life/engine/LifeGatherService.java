package com.poro.rpg.life.engine;

import com.poro.rpg.common.result.ErrorCode;
import com.poro.rpg.common.result.Result;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public final class LifeGatherService {
    private final LifeGatherNodeRegistry gatherNodeRegistry;
    private final LifeSkillProgressionService progressionService;
    private final RandomProvider randomProvider;

    public LifeGatherService(
            LifeGatherNodeRegistry gatherNodeRegistry,
            LifeSkillProgressionService progressionService,
            RandomProvider randomProvider
    ) {
        this.gatherNodeRegistry = Objects.requireNonNull(gatherNodeRegistry, "gatherNodeRegistry");
        this.progressionService = Objects.requireNonNull(progressionService, "progressionService");
        this.randomProvider = Objects.requireNonNull(randomProvider, "randomProvider");
    }

    public Result<GatherResult> gatherField(PlayerLifeState state, String gatherId) {
        if (state == null) {
            return Result.failure(ErrorCode.INVALID_ARGUMENT, "state is required.");
        }

        LifeGatherNode node = gatherNodeRegistry.find(gatherId).orElse(null);
        if (node == null) {
            return Result.failure(ErrorCode.INVALID_ARGUMENT, "Unknown gather_id: " + gatherId);
        }
        if (node.sourceType() != LifeSourceType.FIELD) {
            return Result.failure(ErrorCode.INVALID_ARGUMENT, "gather_id is not field source: " + gatherId);
        }

        LifeBonusProfile bonus = progressionService.bonusFor(state, node.lifeType());
        int rolledBaseAmount = rollRange(node.baseMin(), node.baseMax());
        int adjustedBaseAmount = Math.max(1, scaleAmount(rolledBaseAmount, bonus.yieldBonusPct()));

        double rolledRareChance = rollChanceRange(node.rareChanceMin(), node.rareChanceMax());
        double finalRareChance = Math.max(0.0d, rolledRareChance + bonus.rareBonusPct());

        boolean rareDropped = false;
        int rareAmount = 0;
        int rareExp = 0;
        if (!node.rareItemId().isBlank() && !"-".equals(node.rareItemId())) {
            rareDropped = (randomProvider.nextDouble() * 100.0d) < finalRareChance;
            if (rareDropped) {
                rareAmount = 1;
                rareExp = rollRange(node.rareExpBonusMin(), node.rareExpBonusMax());
            }
        }

        Map<String, Long> grantedItems = new LinkedHashMap<>();
        grantItem(state, grantedItems, node.baseItemId(), adjustedBaseAmount, LifeSourceType.FIELD);
        if (rareDropped) {
            grantItem(state, grantedItems, node.rareItemId(), rareAmount, LifeSourceType.FIELD);
        }

        long gainedExp = Math.max(0L, node.expGain() + rareExp);
        return Result.success(new GatherResult(
                node.gatherId(),
                node.lifeType(),
                LifeSourceType.FIELD,
                rolledBaseAmount,
                adjustedBaseAmount,
                rolledRareChance,
                finalRareChance,
                rareDropped,
                rareAmount,
                gainedExp,
                Map.copyOf(grantedItems)
        ));
    }

    private void grantItem(
            PlayerLifeState state,
            Map<String, Long> grantedItems,
            String itemId,
            long amount,
            LifeSourceType sourceType
    ) {
        String normalizedItemId = normalize(itemId);
        if (normalizedItemId.isBlank() || "-".equals(normalizedItemId) || amount <= 0L) {
            return;
        }
        state.addItem(normalizedItemId, amount);
        state.recordAcquisition(normalizedItemId, amount, sourceType);
        grantedItems.merge(normalizedItemId, amount, Long::sum);
    }

    private int scaleAmount(int amount, double bonusPct) {
        double scaled = amount * (1.0d + (bonusPct / 100.0d));
        return (int) Math.round(scaled);
    }

    private int rollRange(int minInclusive, int maxInclusive) {
        if (maxInclusive <= minInclusive) {
            return minInclusive;
        }
        int bound = (maxInclusive - minInclusive) + 1;
        return minInclusive + randomProvider.nextInt(bound);
    }

    private double rollChanceRange(double minInclusive, double maxInclusive) {
        if (maxInclusive <= minInclusive) {
            return minInclusive;
        }
        return minInclusive + (maxInclusive - minInclusive) * randomProvider.nextDouble();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    public record GatherResult(
            String gatherId,
            LifeType lifeType,
            LifeSourceType sourceType,
            int rolledBaseAmount,
            int finalBaseAmount,
            double rolledRareChancePct,
            double finalRareChancePct,
            boolean rareDropped,
            int rareAmount,
            long gainedExp,
            Map<String, Long> grantedItems
    ) {
    }
}
