package kr.zenon.rpg.life.engine;

import kr.zenon.rpg.common.time.TimeProvider;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class EstateSnapshotBuilder {
    private final EstateHarvestService harvestService;
    private final TimeProvider timeProvider;

    public EstateSnapshotBuilder(EstateHarvestService harvestService, TimeProvider timeProvider) {
        this.harvestService = Objects.requireNonNull(harvestService, "harvestService");
        this.timeProvider = Objects.requireNonNull(timeProvider, "timeProvider");
    }

    public EstateSnapshot build(PlayerLifeState state) {
        Instant now = timeProvider.nowInstant();
        EstateState estateState = state == null ? null : state.estateState().orElse(null);
        if (estateState == null) {
            return new EstateSnapshot("", now, false, List.of(), null);
        }

        List<FacilitySnapshot> facilities = new ArrayList<>();
        for (Map.Entry<Integer, EstateInstalledFacility> entry : estateState.installedFacilities().entrySet()) {
            int slotNo = entry.getKey();
            EstateInstalledFacility installed = entry.getValue();

            Map<String, Long> currentStored = new LinkedHashMap<>();
            long storableCycles = 0L;
            EstateHarvestService.FacilityStoragePreview preview = harvestService
                    .previewStoredAmount(state, installed, now)
                    .orElse(null);
            if (preview != null) {
                storableCycles = preview.storableCycles();
                currentStored.putAll(preview.expectedStoredItems());
            }

            facilities.add(new FacilitySnapshot(
                    slotNo,
                    installed.facilityId(),
                    installed.level(),
                    installed.lastHarvestAt(),
                    storableCycles,
                    Map.copyOf(currentStored)
            ));
        }

        RecentHarvestSnapshot recent = null;
        if (estateState.recentHarvestSummary() != null) {
            EstateHarvestSummary summary = estateState.recentHarvestSummary();
            recent = new RecentHarvestSnapshot(
                    summary.harvestedAt(),
                    summary.facilityCount(),
                    summary.itemTotals()
            );
        }

        return new EstateSnapshot(
                estateState.estateId(),
                estateState.unlockedAt(),
                true,
                List.copyOf(facilities),
                recent
        );
    }

    public record FacilitySnapshot(
            int slotNo,
            String facilityId,
            int level,
            Instant lastHarvestAt,
            long storableCycles,
            Map<String, Long> currentStoredItems
    ) {
    }

    public record RecentHarvestSnapshot(
            Instant harvestedAt,
            int facilityCount,
            Map<String, Long> harvestedItems
    ) {
    }

    public record EstateSnapshot(
            String estateId,
            Instant unlockedAt,
            boolean unlocked,
            List<FacilitySnapshot> facilities,
            RecentHarvestSnapshot recentHarvest
    ) {
    }
}
