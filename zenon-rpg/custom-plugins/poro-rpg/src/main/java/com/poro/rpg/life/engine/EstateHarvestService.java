package com.poro.rpg.life.engine;

import com.poro.rpg.common.result.ErrorCode;
import com.poro.rpg.common.result.Result;
import com.poro.rpg.common.time.TimeProvider;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public final class EstateHarvestService {
    private final EstateFacilityService facilityService;
    private final EstateHarvestLogHook harvestLogHook;
    private final TimeProvider timeProvider;
    private final RandomProvider randomProvider;

    public EstateHarvestService(
            EstateFacilityService facilityService,
            EstateHarvestLogHook harvestLogHook,
            TimeProvider timeProvider,
            RandomProvider randomProvider
    ) {
        this.facilityService = Objects.requireNonNull(facilityService, "facilityService");
        this.harvestLogHook = Objects.requireNonNull(harvestLogHook, "harvestLogHook");
        this.timeProvider = Objects.requireNonNull(timeProvider, "timeProvider");
        this.randomProvider = Objects.requireNonNull(randomProvider, "randomProvider");
    }

    public Result<HarvestResult> harvestAll(PlayerLifeState state) {
        if (state == null) {
            return Result.failure(ErrorCode.INVALID_ARGUMENT, "state is required.");
        }
        EstateState estateState = state.estateState().orElse(null);
        if (estateState == null) {
            return Result.failure(ErrorCode.INVALID_ARGUMENT, "Estate is not unlocked.");
        }

        Instant now = timeProvider.nowInstant();
        Map<String, Long> totalHarvested = new LinkedHashMap<>();
        List<FacilityHarvestResult> facilityResults = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        for (Map.Entry<Integer, EstateInstalledFacility> entry : estateState.installedFacilities().entrySet()) {
            int slotNo = entry.getKey();
            EstateInstalledFacility installed = entry.getValue();

            Result<EstateFacilityService.ProductionProfile> profileResult = facilityService.productionProfile(state, installed);
            if (profileResult.isFailure()) {
                warnings.add(profileResult.message());
                continue;
            }
            EstateFacilityService.ProductionProfile profile = profileResult.value();

            CycleWindow cycleWindow = resolveCycleWindow(installed.lastHarvestAt(), now, profile.intervalMinutes(), profile.storageHoursCap());
            if (cycleWindow.harvestableCycles() <= 0) {
                facilityResults.add(new FacilityHarvestResult(
                        slotNo,
                        installed.facilityId(),
                        installed.level(),
                        0,
                        Map.of()
                ));
                continue;
            }

            Map<String, Long> facilityHarvested = new LinkedHashMap<>();
            for (long cycle = 0; cycle < cycleWindow.harvestableCycles(); cycle++) {
                int baseAmount = rollRange(profile.baseMin(), profile.baseMax());
                mergeAmount(facilityHarvested, profile.baseItemId(), baseAmount);

                if (!profile.rareItemId().isBlank() && !"-".equals(profile.rareItemId())) {
                    boolean rareDropped = (randomProvider.nextDouble() * 100.0d) < profile.rareChancePercent();
                    if (rareDropped) {
                        mergeAmount(facilityHarvested, profile.rareItemId(), 1L);
                    }
                }
            }

            for (Map.Entry<String, Long> harvested : facilityHarvested.entrySet()) {
                state.addItem(harvested.getKey(), harvested.getValue());
                state.recordAcquisition(harvested.getKey(), harvested.getValue(), LifeSourceType.ESTATE);
                mergeAmount(totalHarvested, harvested.getKey(), harvested.getValue());
            }

            installed.setLastHarvestAt(cycleWindow.nextLastHarvestAt());
            facilityResults.add(new FacilityHarvestResult(
                    slotNo,
                    installed.facilityId(),
                    installed.level(),
                    cycleWindow.harvestableCycles(),
                    Map.copyOf(facilityHarvested)
            ));
        }

        EstateHarvestSummary summary = new EstateHarvestSummary(now, totalHarvested, facilityResults.size());
        estateState.setRecentHarvestSummary(summary);

        harvestLogHook.onHarvested(new EstateHarvestLogEntry(
                now,
                state.userId(),
                estateState.estateId(),
                facilityResults.size(),
                totalHarvested
        ));

        return Result.success(new HarvestResult(
                estateState.estateId(),
                now,
                Map.copyOf(totalHarvested),
                List.copyOf(facilityResults),
                List.copyOf(warnings)
        ));
    }

    public Result<FacilityStoragePreview> previewStoredAmount(
            PlayerLifeState state,
            EstateInstalledFacility installed,
            Instant asOf
    ) {
        if (state == null) {
            return Result.failure(ErrorCode.INVALID_ARGUMENT, "state is required.");
        }
        if (installed == null) {
            return Result.failure(ErrorCode.INVALID_ARGUMENT, "installed facility is required.");
        }
        Instant safeAsOf = asOf == null ? timeProvider.nowInstant() : asOf;

        Result<EstateFacilityService.ProductionProfile> profileResult = facilityService.productionProfile(state, installed);
        if (profileResult.isFailure()) {
            return Result.failure(profileResult.errorCode(), profileResult.message(), profileResult.cause());
        }
        EstateFacilityService.ProductionProfile profile = profileResult.value();
        CycleWindow cycleWindow = resolveCycleWindow(installed.lastHarvestAt(), safeAsOf, profile.intervalMinutes(), profile.storageHoursCap());

        Map<String, Long> expectedStored = new LinkedHashMap<>();
        if (cycleWindow.harvestableCycles() > 0) {
            double avgBase = (profile.baseMin() + profile.baseMax()) / 2.0d;
            long baseAmount = Math.round(avgBase * cycleWindow.harvestableCycles());
            mergeAmount(expectedStored, profile.baseItemId(), baseAmount);

            if (!profile.rareItemId().isBlank() && !"-".equals(profile.rareItemId()) && profile.rareChancePercent() > 0.0d) {
                long expectedRare = Math.round(cycleWindow.harvestableCycles() * (profile.rareChancePercent() / 100.0d));
                mergeAmount(expectedStored, profile.rareItemId(), expectedRare);
            }
        }

        return Result.success(new FacilityStoragePreview(
                installed.facilityId(),
                installed.level(),
                cycleWindow.harvestableCycles(),
                Map.copyOf(expectedStored)
        ));
    }

    private CycleWindow resolveCycleWindow(
            Instant lastHarvestAt,
            Instant now,
            int intervalMinutes,
            int storageHoursCap
    ) {
        long elapsedSeconds = Math.max(0L, Duration.between(lastHarvestAt, now).getSeconds());
        long intervalSeconds = Math.max(1L, intervalMinutes * 60L);
        long totalCycles = elapsedSeconds / intervalSeconds;
        long capCycles = Math.max(1L, (storageHoursCap * 3600L) / intervalSeconds);
        long harvestableCycles = Math.min(totalCycles, capCycles);

        Instant nextLastHarvestAt;
        if (totalCycles > capCycles) {
            // Overflow beyond cap is intentionally discarded after harvest.
            nextLastHarvestAt = now;
        } else {
            long remainder = elapsedSeconds % intervalSeconds;
            nextLastHarvestAt = now.minusSeconds(remainder);
        }

        return new CycleWindow(totalCycles, harvestableCycles, nextLastHarvestAt);
    }

    private int rollRange(int minInclusive, int maxInclusive) {
        if (maxInclusive <= minInclusive) {
            return minInclusive;
        }
        int bound = (maxInclusive - minInclusive) + 1;
        return minInclusive + randomProvider.nextInt(bound);
    }

    private void mergeAmount(Map<String, Long> target, String itemId, long amount) {
        String normalized = normalize(itemId);
        if (normalized.isBlank() || "-".equals(normalized) || amount <= 0L) {
            return;
        }
        target.merge(normalized, amount, Long::sum);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private record CycleWindow(
            long totalCycles,
            long harvestableCycles,
            Instant nextLastHarvestAt
    ) {
    }

    public record FacilityHarvestResult(
            int slotNo,
            String facilityId,
            int level,
            long cyclesProcessed,
            Map<String, Long> harvestedItems
    ) {
    }

    public record HarvestResult(
            String estateId,
            Instant harvestedAt,
            Map<String, Long> harvestedItems,
            List<FacilityHarvestResult> facilityResults,
            List<String> warnings
    ) {
    }

    public record FacilityStoragePreview(
            String facilityId,
            int level,
            long storableCycles,
            Map<String, Long> expectedStoredItems
    ) {
    }
}
