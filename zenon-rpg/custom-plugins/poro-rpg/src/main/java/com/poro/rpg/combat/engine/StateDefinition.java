package com.poro.rpg.combat.engine;

import java.util.Locale;
import java.util.Objects;

public record StateDefinition(
        String stateCode,
        String stateGroup,
        String displayName,
        boolean stackable,
        int maxStack,
        double durationDefaultSeconds,
        String notes
) {
    public StateDefinition {
        Objects.requireNonNull(stateCode, "stateCode");
        Objects.requireNonNull(stateGroup, "stateGroup");
        Objects.requireNonNull(displayName, "displayName");
        stateCode = normalize(stateCode);
        stateGroup = normalize(stateGroup).toUpperCase(Locale.ROOT);
        notes = notes == null ? "" : notes;
    }

    public boolean isMarkOrControl() {
        return "DEBUFF_MARK".equals(stateGroup) || "DEBUFF_CONTROL".equals(stateGroup);
    }

    public boolean isStatus() {
        return "DEBUFF_STATUS".equals(stateGroup);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
