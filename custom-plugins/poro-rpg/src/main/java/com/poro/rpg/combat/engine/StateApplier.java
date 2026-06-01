package com.poro.rpg.combat.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class StateApplier {
    private final StateRegistry stateRegistry;
    private final BuffDebuffService buffDebuffService;

    public StateApplier(StateRegistry stateRegistry, BuffDebuffService buffDebuffService) {
        this.stateRegistry = stateRegistry;
        this.buffDebuffService = buffDebuffService;
    }

    public List<String> applyMarkOrControl(String targetUnitId, String stateCode) {
        return applyByGroup(targetUnitId, stateCode, true);
    }

    public List<String> applyStatus(String targetUnitId, String stateCode) {
        return applyByGroup(targetUnitId, stateCode, false);
    }

    public List<String> applyAny(String targetUnitId, String stateCode) {
        List<String> events = new ArrayList<>();
        Optional<StateDefinition> definition = stateRegistry.find(stateCode);
        if (definition.isEmpty()) {
            return events;
        }
        int stack = buffDebuffService.apply(targetUnitId, definition.get(), 1);
        events.add("state_applied:" + definition.get().stateCode() + " stack=" + stack);
        return events;
    }

    private List<String> applyByGroup(String targetUnitId, String stateCode, boolean markOrControl) {
        List<String> events = new ArrayList<>();
        Optional<StateDefinition> definition = stateRegistry.find(stateCode);
        if (definition.isEmpty()) {
            return events;
        }

        StateDefinition stateDefinition = definition.get();
        String group = stateDefinition.stateGroup().toUpperCase(Locale.ROOT);
        boolean matched = markOrControl
                ? ("DEBUFF_MARK".equals(group) || "DEBUFF_CONTROL".equals(group))
                : "DEBUFF_STATUS".equals(group);
        if (!matched) {
            return events;
        }

        int stack = buffDebuffService.apply(targetUnitId, stateDefinition, 1);
        events.add("state_applied:" + stateDefinition.stateCode() + " stack=" + stack);
        return events;
    }
}
