package com.poro.rpg.combat.engine;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public final class CombatUnitSnapshot {
    private final String id;
    private final double weaponPower;
    private final double equipmentPower;
    private final double generalDamageIncrease;
    private final double defense;
    private final double criticalChance;
    private final double criticalDamageMultiplier;
    private final double damageReduction;
    private final Map<String, Double> tagDamageIncreases;
    private final Map<String, Boolean> flags;
    private final Map<String, Integer> resources = new ConcurrentHashMap<>();

    private CombatUnitSnapshot(Builder builder) {
        this.id = Objects.requireNonNull(builder.id, "id");
        this.weaponPower = builder.weaponPower;
        this.equipmentPower = builder.equipmentPower;
        this.generalDamageIncrease = builder.generalDamageIncrease;
        this.defense = builder.defense;
        this.criticalChance = builder.criticalChance;
        this.criticalDamageMultiplier = builder.criticalDamageMultiplier;
        this.damageReduction = builder.damageReduction;
        this.tagDamageIncreases = Map.copyOf(new LinkedHashMap<>(builder.tagDamageIncreases));
        this.flags = Map.copyOf(new LinkedHashMap<>(builder.flags));
        this.resources.putAll(builder.initialResources);
    }

    public String id() {
        return id;
    }

    public double weaponPower() {
        return weaponPower;
    }

    public double equipmentPower() {
        return equipmentPower;
    }

    public double generalDamageIncrease() {
        return generalDamageIncrease;
    }

    public double defense() {
        return defense;
    }

    public double criticalChance() {
        return criticalChance;
    }

    public double criticalDamageMultiplier() {
        return criticalDamageMultiplier;
    }

    public double damageReduction() {
        return damageReduction;
    }

    public double tagDamageIncrease(String tagCode) {
        if (tagCode == null || tagCode.isBlank()) {
            return 0.0d;
        }
        return tagDamageIncreases.getOrDefault(tagCode.trim().toLowerCase(), 0.0d);
    }

    public boolean flagEnabled(String flagKey) {
        if (flagKey == null || flagKey.isBlank()) {
            return false;
        }
        return flags.getOrDefault(flagKey.trim().toLowerCase(), false);
    }

    public int resource(String resourceType) {
        if (resourceType == null || resourceType.isBlank()) {
            return 0;
        }
        return resources.getOrDefault(resourceType.trim().toLowerCase(), 0);
    }

    public int addResource(String resourceType, int amount, int maxValue) {
        if (amount <= 0) {
            return resource(resourceType);
        }
        String key = resourceType.trim().toLowerCase();
        return resources.merge(key, amount, (oldValue, gain) -> Math.min(maxValue, oldValue + gain));
    }

    public int consumeResource(String resourceType, int amount) {
        if (amount <= 0) {
            return 0;
        }
        String key = resourceType.trim().toLowerCase();
        int current = resources.getOrDefault(key, 0);
        int consumed = Math.min(current, amount);
        resources.put(key, Math.max(0, current - consumed));
        return consumed;
    }

    public Map<String, Integer> resourceSnapshot() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(resources));
    }

    public static Builder builder(String id) {
        return new Builder(id);
    }

    public static final class Builder {
        private final String id;
        private double weaponPower;
        private double equipmentPower;
        private double generalDamageIncrease;
        private double defense;
        private double criticalChance;
        private double criticalDamageMultiplier = 1.5d;
        private double damageReduction;
        private final Map<String, Double> tagDamageIncreases = new LinkedHashMap<>();
        private final Map<String, Boolean> flags = new LinkedHashMap<>();
        private final Map<String, Integer> initialResources = new LinkedHashMap<>();

        private Builder(String id) {
            this.id = id;
        }

        public Builder weaponPower(double value) {
            this.weaponPower = value;
            return this;
        }

        public Builder equipmentPower(double value) {
            this.equipmentPower = value;
            return this;
        }

        public Builder generalDamageIncrease(double value) {
            this.generalDamageIncrease = value;
            return this;
        }

        public Builder defense(double value) {
            this.defense = value;
            return this;
        }

        public Builder criticalChance(double value) {
            this.criticalChance = value;
            return this;
        }

        public Builder criticalDamageMultiplier(double value) {
            this.criticalDamageMultiplier = value;
            return this;
        }

        public Builder damageReduction(double value) {
            this.damageReduction = value;
            return this;
        }

        public Builder tagDamageIncrease(String tagCode, double value) {
            this.tagDamageIncreases.put(tagCode.trim().toLowerCase(), value);
            return this;
        }

        public Builder flag(String flagKey, boolean enabled) {
            this.flags.put(flagKey.trim().toLowerCase(), enabled);
            return this;
        }

        public Builder initialResource(String resourceType, int amount) {
            this.initialResources.put(resourceType.trim().toLowerCase(), Math.max(0, amount));
            return this;
        }

        public CombatUnitSnapshot build() {
            return new CombatUnitSnapshot(this);
        }
    }
}
