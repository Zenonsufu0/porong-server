package com.poro.rpg.life.engine;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;

public final class EstateInstalledFacility {
    private final String facilityId;
    private final Instant installedAt;
    private int level;
    private Instant lastHarvestAt;

    public EstateInstalledFacility(String facilityId, int level, Instant installedAt, Instant lastHarvestAt) {
        this.facilityId = normalize(facilityId);
        this.level = Math.max(1, level);
        this.installedAt = Objects.requireNonNull(installedAt, "installedAt");
        this.lastHarvestAt = Objects.requireNonNull(lastHarvestAt, "lastHarvestAt");
    }

    public String facilityId() {
        return facilityId;
    }

    public int level() {
        return level;
    }

    public void setLevel(int level) {
        this.level = Math.max(1, level);
    }

    public Instant installedAt() {
        return installedAt;
    }

    public Instant lastHarvestAt() {
        return lastHarvestAt;
    }

    public void setLastHarvestAt(Instant lastHarvestAt) {
        this.lastHarvestAt = Objects.requireNonNull(lastHarvestAt, "lastHarvestAt");
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
