package com.poro.rpg.life.engine;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class EstateFacilityRegistry {
    private final Map<String, EstateFacilityMaster> values = new LinkedHashMap<>();

    public void register(EstateFacilityMaster master) {
        values.put(normalize(master.facilityId()), master);
    }

    public Optional<EstateFacilityMaster> find(String facilityId) {
        return Optional.ofNullable(values.get(normalize(facilityId)));
    }

    public Map<String, EstateFacilityMaster> all() {
        return Map.copyOf(new LinkedHashMap<>(values));
    }

    public int size() {
        return values.size();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
