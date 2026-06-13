package com.poro.rpg.common.ids;

import java.util.Locale;
import java.util.UUID;

public final class PrefixUuidIdGenerator implements IdGenerator {
    @Override
    public String newId(String prefix) {
        String safePrefix = prefix == null ? "" : prefix.trim().toLowerCase(Locale.ROOT);
        if (safePrefix.isBlank()) {
            throw new IllegalArgumentException("prefix must not be blank");
        }
        return safePrefix + "_" + UUID.randomUUID();
    }
}
