package kr.zenon.rpg.life.engine;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class EstateState {
    private final String estateId;
    private final int slotCapacity;
    private final Instant unlockedAt;
    private final Map<Integer, EstateInstalledFacility> installedFacilitiesBySlot = new LinkedHashMap<>();
    private EstateHarvestSummary recentHarvestSummary;

    public EstateState(String estateId, int slotCapacity, Instant unlockedAt) {
        this.estateId = estateId;
        this.slotCapacity = Math.max(1, slotCapacity);
        this.unlockedAt = unlockedAt;
    }

    public String estateId() {
        return estateId;
    }

    public int slotCapacity() {
        return slotCapacity;
    }

    public Instant unlockedAt() {
        return unlockedAt;
    }

    public boolean hasSlot(int slotNo) {
        return slotNo >= 1 && slotNo <= slotCapacity;
    }

    public boolean isSlotOccupied(int slotNo) {
        return installedFacilitiesBySlot.containsKey(slotNo);
    }

    public Optional<EstateInstalledFacility> facilityInSlot(int slotNo) {
        return Optional.ofNullable(installedFacilitiesBySlot.get(slotNo));
    }

    public Optional<Integer> slotByFacilityId(String facilityId) {
        return installedFacilitiesBySlot.entrySet().stream()
                .filter(entry -> entry.getValue().facilityId().equalsIgnoreCase(facilityId))
                .map(Map.Entry::getKey)
                .findFirst();
    }

    public void install(int slotNo, EstateInstalledFacility facility) {
        installedFacilitiesBySlot.put(slotNo, facility);
    }

    public Map<Integer, EstateInstalledFacility> installedFacilities() {
        return Map.copyOf(new LinkedHashMap<>(installedFacilitiesBySlot));
    }

    public int installedCount() {
        return installedFacilitiesBySlot.size();
    }

    public EstateHarvestSummary recentHarvestSummary() {
        return recentHarvestSummary;
    }

    public void setRecentHarvestSummary(EstateHarvestSummary recentHarvestSummary) {
        this.recentHarvestSummary = recentHarvestSummary;
    }
}
