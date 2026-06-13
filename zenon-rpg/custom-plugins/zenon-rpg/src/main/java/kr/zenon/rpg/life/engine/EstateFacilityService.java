package kr.zenon.rpg.life.engine;

import kr.zenon.rpg.common.result.ErrorCode;
import kr.zenon.rpg.common.result.Result;
import kr.zenon.rpg.common.time.TimeProvider;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public final class EstateFacilityService {
    private final EstateFacilityRegistry facilityRegistry;
    private final EstateFacilityLevelRegistry levelRegistry;
    private final LifeSkillProgressionService progressionService;
    private final EstateProductionAmountHook productionAmountHook;
    private final TimeProvider timeProvider;

    public EstateFacilityService(
            EstateFacilityRegistry facilityRegistry,
            EstateFacilityLevelRegistry levelRegistry,
            LifeSkillProgressionService progressionService,
            EstateProductionAmountHook productionAmountHook,
            TimeProvider timeProvider
    ) {
        this.facilityRegistry = Objects.requireNonNull(facilityRegistry, "facilityRegistry");
        this.levelRegistry = Objects.requireNonNull(levelRegistry, "levelRegistry");
        this.progressionService = Objects.requireNonNull(progressionService, "progressionService");
        this.productionAmountHook = Objects.requireNonNull(productionAmountHook, "productionAmountHook");
        this.timeProvider = Objects.requireNonNull(timeProvider, "timeProvider");
    }

    public Result<InstallResult> install(PlayerLifeState state, String facilityId, int slotNo) {
        if (state == null) {
            return Result.failure(ErrorCode.INVALID_ARGUMENT, "state is required.");
        }
        EstateState estateState = state.estateState().orElse(null);
        if (estateState == null) {
            return Result.failure(ErrorCode.INVALID_ARGUMENT, "Estate is not unlocked.");
        }
        if (!estateState.hasSlot(slotNo)) {
            return Result.failure(
                    ErrorCode.INVALID_ARGUMENT,
                    "Invalid estate slot. slot=" + slotNo + ", capacity=" + estateState.slotCapacity()
            );
        }
        if (estateState.isSlotOccupied(slotNo)) {
            return Result.failure(ErrorCode.INVALID_ARGUMENT, "Estate slot is already occupied. slot=" + slotNo);
        }

        EstateFacilityMaster facility = facilityRegistry.find(facilityId).orElse(null);
        if (facility == null) {
            return Result.failure(ErrorCode.INVALID_ARGUMENT, "Unknown facility_id: " + facilityId);
        }

        if (estateState.slotByFacilityId(facility.facilityId()).isPresent()) {
            return Result.failure(ErrorCode.INVALID_ARGUMENT, "Facility is already installed: " + facility.facilityId());
        }

        if (!state.hasBlueprint(facility.requiredBlueprintId())) {
            return Result.failure(
                    ErrorCode.INVALID_ARGUMENT,
                    "Facility blueprint is not unlocked. facility_id=" + facility.facilityId()
                            + ", blueprint_id=" + facility.requiredBlueprintId()
            );
        }

        if (levelRegistry.find(facility.facilityId(), 1).isEmpty()) {
            return Result.failure(
                    ErrorCode.INVALID_ARGUMENT,
                    "Missing level rule for install. facility_id=" + facility.facilityId() + ", level=1"
            );
        }

        EstateInstalledFacility installed = new EstateInstalledFacility(
                facility.facilityId(),
                1,
                timeProvider.nowInstant(),
                timeProvider.nowInstant()
        );
        estateState.install(slotNo, installed);

        return Result.success(new InstallResult(
                estateState.estateId(),
                slotNo,
                facility.facilityId(),
                1
        ));
    }

    public Result<UpgradeResult> upgrade(PlayerLifeState state, int slotNo) {
        if (state == null) {
            return Result.failure(ErrorCode.INVALID_ARGUMENT, "state is required.");
        }
        EstateState estateState = state.estateState().orElse(null);
        if (estateState == null) {
            return Result.failure(ErrorCode.INVALID_ARGUMENT, "Estate is not unlocked.");
        }
        EstateInstalledFacility installed = estateState.facilityInSlot(slotNo).orElse(null);
        if (installed == null) {
            return Result.failure(ErrorCode.INVALID_ARGUMENT, "No facility installed in slot=" + slotNo);
        }

        EstateFacilityMaster facility = facilityRegistry.find(installed.facilityId()).orElse(null);
        if (facility == null) {
            return Result.failure(ErrorCode.INVALID_ARGUMENT, "Unknown facility_id: " + installed.facilityId());
        }
        if (installed.level() >= facility.maxLevel()) {
            return Result.failure(
                    ErrorCode.INVALID_ARGUMENT,
                    "Facility is already at max level. facility_id=" + facility.facilityId() + ", level=" + installed.level()
            );
        }

        int targetLevel = installed.level() + 1;
        EstateFacilityLevelRule levelRule = levelRegistry.find(facility.facilityId(), targetLevel).orElse(null);
        if (levelRule == null) {
            return Result.failure(
                    ErrorCode.INVALID_ARGUMENT,
                    "Missing level rule. facility_id=" + facility.facilityId() + ", level=" + targetLevel
            );
        }

        if (state.currency("gold") < levelRule.upgradeGoldCost()) {
            return Result.failure(
                    ErrorCode.INVALID_ARGUMENT,
                    "Insufficient gold for upgrade. required=" + levelRule.upgradeGoldCost()
                            + ", current=" + state.currency("gold")
            );
        }
        if (levelRule.basicCostAmount() > 0 && state.itemAmount(levelRule.basicCostItemId()) < levelRule.basicCostAmount()) {
            return Result.failure(
                    ErrorCode.INVALID_ARGUMENT,
                    "Insufficient upgrade material. item_id=" + levelRule.basicCostItemId()
                            + ", required=" + levelRule.basicCostAmount()
                            + ", current=" + state.itemAmount(levelRule.basicCostItemId())
            );
        }
        if (levelRule.themeCostAmount() > 0 && state.itemAmount(levelRule.themeCostItemId()) < levelRule.themeCostAmount()) {
            return Result.failure(
                    ErrorCode.INVALID_ARGUMENT,
                    "Insufficient theme material. item_id=" + levelRule.themeCostItemId()
                            + ", required=" + levelRule.themeCostAmount()
                            + ", current=" + state.itemAmount(levelRule.themeCostItemId())
            );
        }

        state.consumeCurrency("gold", levelRule.upgradeGoldCost());
        if (levelRule.basicCostAmount() > 0) {
            state.consumeItem(levelRule.basicCostItemId(), levelRule.basicCostAmount());
        }
        if (levelRule.themeCostAmount() > 0) {
            state.consumeItem(levelRule.themeCostItemId(), levelRule.themeCostAmount());
        }
        installed.setLevel(targetLevel);

        Map<String, Long> consumed = new LinkedHashMap<>();
        if (levelRule.basicCostAmount() > 0) {
            consumed.put(normalize(levelRule.basicCostItemId()), (long) levelRule.basicCostAmount());
        }
        if (levelRule.themeCostAmount() > 0) {
            consumed.put(normalize(levelRule.themeCostItemId()), (long) levelRule.themeCostAmount());
        }

        return Result.success(new UpgradeResult(
                facility.facilityId(),
                installed.level() - 1,
                installed.level(),
                levelRule.upgradeGoldCost(),
                Map.copyOf(consumed)
        ));
    }

    public Result<ProductionProfile> productionProfile(PlayerLifeState state, EstateInstalledFacility installedFacility) {
        if (state == null) {
            return Result.failure(ErrorCode.INVALID_ARGUMENT, "state is required.");
        }
        if (installedFacility == null) {
            return Result.failure(ErrorCode.INVALID_ARGUMENT, "installedFacility is required.");
        }

        EstateFacilityMaster facility = facilityRegistry.find(installedFacility.facilityId()).orElse(null);
        if (facility == null) {
            return Result.failure(ErrorCode.INVALID_ARGUMENT, "Unknown facility_id: " + installedFacility.facilityId());
        }
        EstateFacilityLevelRule levelRule = levelRegistry.find(facility.facilityId(), installedFacility.level()).orElse(null);
        if (levelRule == null) {
            return Result.failure(
                    ErrorCode.INVALID_ARGUMENT,
                    "Missing level rule. facility_id=" + facility.facilityId() + ", level=" + installedFacility.level()
            );
        }

        LifeBonusProfile bonus = progressionService.bonusFor(state, facility.lifeType());
        int scaledMin = scaleAmount(levelRule.baseMin(), bonus.estateOutputBonusPct());
        int scaledMax = scaleAmount(levelRule.baseMax(), bonus.estateOutputBonusPct());
        if (scaledMax < scaledMin) {
            scaledMax = scaledMin;
        }

        scaledMin = Math.max(1, productionAmountHook.adjustBaseMin(state, facility, levelRule, scaledMin));
        scaledMax = Math.max(scaledMin, productionAmountHook.adjustBaseMax(state, facility, levelRule, scaledMax));

        double rareChance = levelRule.rareChancePercent();
        if (installedFacility.level() < 3) {
            rareChance = 0.0d;
        } else {
            rareChance = Math.max(0.0d, rareChance + bonus.rareBonusPct());
        }
        rareChance = Math.max(0.0d, productionAmountHook.adjustRareChancePercent(state, facility, levelRule, rareChance));

        return Result.success(new ProductionProfile(
                facility.facilityId(),
                facility.baseItemId(),
                facility.rareItemId(),
                facility.intervalMinutes(),
                levelRule.storageHoursCap(),
                scaledMin,
                scaledMax,
                rareChance
        ));
    }

    private int scaleAmount(int value, double pct) {
        double scaled = value * (1.0d + (pct / 100.0d));
        return Math.max(1, (int) Math.round(scaled));
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    public record InstallResult(
            String estateId,
            int slotNo,
            String facilityId,
            int level
    ) {
    }

    public record UpgradeResult(
            String facilityId,
            int beforeLevel,
            int afterLevel,
            long goldCost,
            Map<String, Long> itemCosts
    ) {
    }

    public record ProductionProfile(
            String facilityId,
            String baseItemId,
            String rareItemId,
            int intervalMinutes,
            int storageHoursCap,
            int baseMin,
            int baseMax,
            double rareChancePercent
    ) {
    }
}
