package com.poro.rpg.life.engine;

import java.util.Locale;

public record EstateUnlockRule(
        String estateId,
        String unlockQuestId,
        int initialSlotCapacity,
        String firstInstallFacilityId
) {
    public EstateUnlockRule {
        estateId = normalize(estateId);
        unlockQuestId = normalize(unlockQuestId);
        initialSlotCapacity = Math.max(1, initialSlotCapacity);
        firstInstallFacilityId = normalize(firstInstallFacilityId);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
