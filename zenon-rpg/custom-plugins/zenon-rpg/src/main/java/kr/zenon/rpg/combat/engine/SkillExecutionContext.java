package kr.zenon.rpg.combat.engine;

import kr.zenon.rpg.common.registry.master.SkillMasterRegistry;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class SkillExecutionContext {
    private final String skillId;
    private final CombatUnitSnapshot attacker;
    private final CombatUnitSnapshot defender;
    private final SkillMasterRegistry skillRegistry;
    private final StateRegistry stateRegistry;
    private final BuffDebuffService buffDebuffService;
    private final StateApplier stateApplier;
    private final ResourceHandler resourceHandler;
    private final CombatFormulaResolver combatFormulaResolver;
    private final TagDamageResolver tagDamageResolver;
    private final ConditionalDamageResolver conditionalDamageResolver;
    private final Map<String, Boolean> environmentFlags;
    private final Set<String> immediateStateConsumeSkillIds;
    private final Double criticalRoll;
    private final boolean pve;

    private SkillExecutionContext(Builder builder) {
        this.skillId = normalize(builder.skillId);
        this.attacker = Objects.requireNonNull(builder.attacker, "attacker");
        this.defender = Objects.requireNonNull(builder.defender, "defender");
        this.skillRegistry = Objects.requireNonNull(builder.skillRegistry, "skillRegistry");
        this.stateRegistry = Objects.requireNonNull(builder.stateRegistry, "stateRegistry");
        this.buffDebuffService = Objects.requireNonNull(builder.buffDebuffService, "buffDebuffService");
        this.stateApplier = Objects.requireNonNull(builder.stateApplier, "stateApplier");
        this.resourceHandler = Objects.requireNonNull(builder.resourceHandler, "resourceHandler");
        this.combatFormulaResolver = Objects.requireNonNull(builder.combatFormulaResolver, "combatFormulaResolver");
        this.tagDamageResolver = Objects.requireNonNull(builder.tagDamageResolver, "tagDamageResolver");
        this.conditionalDamageResolver = Objects.requireNonNull(builder.conditionalDamageResolver, "conditionalDamageResolver");
        this.environmentFlags = Map.copyOf(new LinkedHashMap<>(builder.environmentFlags));
        this.immediateStateConsumeSkillIds = Set.copyOf(builder.immediateStateConsumeSkillIds);
        this.criticalRoll = builder.criticalRoll;
        this.pve = builder.pve;
    }

    public String skillId() {
        return skillId;
    }

    public CombatUnitSnapshot attacker() {
        return attacker;
    }

    public CombatUnitSnapshot defender() {
        return defender;
    }

    public SkillMasterRegistry skillRegistry() {
        return skillRegistry;
    }

    public StateRegistry stateRegistry() {
        return stateRegistry;
    }

    public BuffDebuffService buffDebuffService() {
        return buffDebuffService;
    }

    public StateApplier stateApplier() {
        return stateApplier;
    }

    public ResourceHandler resourceHandler() {
        return resourceHandler;
    }

    public CombatFormulaResolver combatFormulaResolver() {
        return combatFormulaResolver;
    }

    public TagDamageResolver tagDamageResolver() {
        return tagDamageResolver;
    }

    public ConditionalDamageResolver conditionalDamageResolver() {
        return conditionalDamageResolver;
    }

    public boolean flagEnabled(String key) {
        return environmentFlags.getOrDefault(normalize(key), false);
    }

    public boolean allowImmediateStateConsume(String resolvedSkillId) {
        return immediateStateConsumeSkillIds.contains(normalize(resolvedSkillId));
    }

    public Optional<Double> criticalRoll() {
        return Optional.ofNullable(criticalRoll);
    }

    public boolean pve() {
        return pve;
    }

    public static Builder builder(String skillId) {
        return new Builder(skillId);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    public static final class Builder {
        private final String skillId;
        private CombatUnitSnapshot attacker;
        private CombatUnitSnapshot defender;
        private SkillMasterRegistry skillRegistry;
        private StateRegistry stateRegistry;
        private BuffDebuffService buffDebuffService;
        private StateApplier stateApplier;
        private ResourceHandler resourceHandler;
        private CombatFormulaResolver combatFormulaResolver = new CombatFormulaResolver();
        private TagDamageResolver tagDamageResolver = new TagDamageResolver();
        private ConditionalDamageResolver conditionalDamageResolver = new ConditionalDamageResolver();
        private final Map<String, Boolean> environmentFlags = new LinkedHashMap<>();
        private Set<String> immediateStateConsumeSkillIds = Set.of();
        private Double criticalRoll;
        private boolean pve = true;

        private Builder(String skillId) {
            this.skillId = skillId;
        }

        public Builder attacker(CombatUnitSnapshot value) {
            this.attacker = value;
            return this;
        }

        public Builder defender(CombatUnitSnapshot value) {
            this.defender = value;
            return this;
        }

        public Builder skillRegistry(SkillMasterRegistry value) {
            this.skillRegistry = value;
            return this;
        }

        public Builder stateRegistry(StateRegistry value) {
            this.stateRegistry = value;
            return this;
        }

        public Builder buffDebuffService(BuffDebuffService value) {
            this.buffDebuffService = value;
            return this;
        }

        public Builder stateApplier(StateApplier value) {
            this.stateApplier = value;
            return this;
        }

        public Builder resourceHandler(ResourceHandler value) {
            this.resourceHandler = value;
            return this;
        }

        public Builder combatFormulaResolver(CombatFormulaResolver value) {
            this.combatFormulaResolver = value;
            return this;
        }

        public Builder tagDamageResolver(TagDamageResolver value) {
            this.tagDamageResolver = value;
            return this;
        }

        public Builder conditionalDamageResolver(ConditionalDamageResolver value) {
            this.conditionalDamageResolver = value;
            return this;
        }

        public Builder flag(String key, boolean enabled) {
            this.environmentFlags.put(key.trim().toLowerCase(Locale.ROOT), enabled);
            return this;
        }

        public Builder immediateStateConsumeSkillIds(Set<String> value) {
            this.immediateStateConsumeSkillIds = value;
            return this;
        }

        public Builder criticalRoll(Double value) {
            this.criticalRoll = value;
            return this;
        }

        public Builder pve(boolean value) {
            this.pve = value;
            return this;
        }

        public SkillExecutionContext build() {
            return new SkillExecutionContext(this);
        }
    }
}
