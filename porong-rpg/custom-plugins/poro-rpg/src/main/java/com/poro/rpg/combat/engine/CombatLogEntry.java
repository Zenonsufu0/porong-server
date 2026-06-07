package com.poro.rpg.combat.engine;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class CombatLogEntry {
    private final Instant executedAt;
    private final String skillId;
    private final String attackerId;
    private final String defenderId;
    private final double step1Coefficient;
    private final double step2BaseDamage;
    private final double step3GeneralMultiplier;
    private final double step4TagMultiplier;
    private final double step5ConditionalMultiplier;
    private final double step6DefenseMultiplier;
    private final double step7CriticalMultiplier;
    private final double step8TargetDamageReductionMultiplier;
    private final double finalDamage;
    private final boolean critical;
    private final String conditionalReason;
    private final Map<String, Double> tagBreakdown;
    private final Map<String, Integer> attackerResourcesBefore;
    private final Map<String, Integer> attackerResourcesAfter;
    private final Map<String, Integer> defenderStatesAfter;
    private final List<String> stateEvents;
    private final List<String> resourceEvents;
    private final List<String> warnings;

    private CombatLogEntry(Builder builder) {
        this.executedAt = builder.executedAt;
        this.skillId = builder.skillId;
        this.attackerId = builder.attackerId;
        this.defenderId = builder.defenderId;
        this.step1Coefficient = builder.step1Coefficient;
        this.step2BaseDamage = builder.step2BaseDamage;
        this.step3GeneralMultiplier = builder.step3GeneralMultiplier;
        this.step4TagMultiplier = builder.step4TagMultiplier;
        this.step5ConditionalMultiplier = builder.step5ConditionalMultiplier;
        this.step6DefenseMultiplier = builder.step6DefenseMultiplier;
        this.step7CriticalMultiplier = builder.step7CriticalMultiplier;
        this.step8TargetDamageReductionMultiplier = builder.step8TargetDamageReductionMultiplier;
        this.finalDamage = builder.finalDamage;
        this.critical = builder.critical;
        this.conditionalReason = builder.conditionalReason;
        this.tagBreakdown = Map.copyOf(new LinkedHashMap<>(builder.tagBreakdown));
        this.attackerResourcesBefore = Map.copyOf(new LinkedHashMap<>(builder.attackerResourcesBefore));
        this.attackerResourcesAfter = Map.copyOf(new LinkedHashMap<>(builder.attackerResourcesAfter));
        this.defenderStatesAfter = Map.copyOf(new LinkedHashMap<>(builder.defenderStatesAfter));
        this.stateEvents = List.copyOf(new ArrayList<>(builder.stateEvents));
        this.resourceEvents = List.copyOf(new ArrayList<>(builder.resourceEvents));
        this.warnings = List.copyOf(new ArrayList<>(builder.warnings));
    }

    public Instant executedAt() {
        return executedAt;
    }

    public String skillId() {
        return skillId;
    }

    public String attackerId() {
        return attackerId;
    }

    public String defenderId() {
        return defenderId;
    }

    public double finalDamage() {
        return finalDamage;
    }

    public boolean critical() {
        return critical;
    }

    public String conditionalReason() {
        return conditionalReason;
    }

    public Map<String, Double> tagBreakdown() {
        return tagBreakdown;
    }

    public List<String> stateEvents() {
        return stateEvents;
    }

    public List<String> resourceEvents() {
        return resourceEvents;
    }

    public List<String> warnings() {
        return warnings;
    }

    @Override
    public String toString() {
        return "CombatLogEntry{" +
                "skillId='" + skillId + '\'' +
                ", attackerId='" + attackerId + '\'' +
                ", defenderId='" + defenderId + '\'' +
                ", finalDamage=" + String.format("%.2f", finalDamage) +
                ", critical=" + critical +
                ", stateEvents=" + stateEvents +
                ", resourceEvents=" + resourceEvents +
                ", warnings=" + warnings +
                '}';
    }

    public static final class Builder {
        private Instant executedAt = Instant.now();
        private String skillId = "";
        private String attackerId = "";
        private String defenderId = "";
        private double step1Coefficient;
        private double step2BaseDamage;
        private double step3GeneralMultiplier;
        private double step4TagMultiplier;
        private double step5ConditionalMultiplier;
        private double step6DefenseMultiplier;
        private double step7CriticalMultiplier;
        private double step8TargetDamageReductionMultiplier;
        private double finalDamage;
        private boolean critical;
        private String conditionalReason = "";
        private Map<String, Double> tagBreakdown = Map.of();
        private Map<String, Integer> attackerResourcesBefore = Map.of();
        private Map<String, Integer> attackerResourcesAfter = Map.of();
        private Map<String, Integer> defenderStatesAfter = Map.of();
        private final List<String> stateEvents = new ArrayList<>();
        private final List<String> resourceEvents = new ArrayList<>();
        private final List<String> warnings = new ArrayList<>();

        public Builder executedAt(Instant value) {
            this.executedAt = value;
            return this;
        }

        public Builder identifiers(String skillId, String attackerId, String defenderId) {
            this.skillId = skillId;
            this.attackerId = attackerId;
            this.defenderId = defenderId;
            return this;
        }

        public Builder damageSteps(
                double step1Coefficient,
                double step2BaseDamage,
                double step3GeneralMultiplier,
                double step4TagMultiplier,
                double step5ConditionalMultiplier,
                double step6DefenseMultiplier,
                double step7CriticalMultiplier,
                double step8TargetDamageReductionMultiplier,
                double finalDamage,
                boolean critical
        ) {
            this.step1Coefficient = step1Coefficient;
            this.step2BaseDamage = step2BaseDamage;
            this.step3GeneralMultiplier = step3GeneralMultiplier;
            this.step4TagMultiplier = step4TagMultiplier;
            this.step5ConditionalMultiplier = step5ConditionalMultiplier;
            this.step6DefenseMultiplier = step6DefenseMultiplier;
            this.step7CriticalMultiplier = step7CriticalMultiplier;
            this.step8TargetDamageReductionMultiplier = step8TargetDamageReductionMultiplier;
            this.finalDamage = finalDamage;
            this.critical = critical;
            return this;
        }

        public Builder conditionalReason(String value) {
            this.conditionalReason = value;
            return this;
        }

        public Builder tagBreakdown(Map<String, Double> value) {
            this.tagBreakdown = value;
            return this;
        }

        public Builder attackerResourcesBefore(Map<String, Integer> value) {
            this.attackerResourcesBefore = value;
            return this;
        }

        public Builder attackerResourcesAfter(Map<String, Integer> value) {
            this.attackerResourcesAfter = value;
            return this;
        }

        public Builder defenderStatesAfter(Map<String, Integer> value) {
            this.defenderStatesAfter = value;
            return this;
        }

        public Builder addStateEvents(List<String> values) {
            this.stateEvents.addAll(values);
            return this;
        }

        public Builder addResourceEvents(List<String> values) {
            this.resourceEvents.addAll(values);
            return this;
        }

        public Builder addWarnings(List<String> values) {
            this.warnings.addAll(values);
            return this;
        }

        public Builder addWarning(String value) {
            this.warnings.add(value);
            return this;
        }

        public CombatLogEntry build() {
            return new CombatLogEntry(this);
        }
    }
}
