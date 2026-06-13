package com.poro.rpg.growth.engine;

import com.poro.rpg.common.registry.master.ItemMasterRegistry;
import com.poro.rpg.common.registry.master.model.ItemMaster;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class SetBonusService {
    private final ItemMasterRegistry itemMasterRegistry;
    private final SetBonusRegistry setBonusRegistry;

    public SetBonusService(ItemMasterRegistry itemMasterRegistry, SetBonusRegistry setBonusRegistry) {
        this.itemMasterRegistry = Objects.requireNonNull(itemMasterRegistry, "itemMasterRegistry");
        this.setBonusRegistry = Objects.requireNonNull(setBonusRegistry, "setBonusRegistry");
    }

    public SetBonusState calculate(PlayerGrowthState state) {
        Map<String, Integer> pieceCountBySet = new LinkedHashMap<>();
        for (String itemInstanceId : state.equippedItems().values()) {
            PlayerEquipmentItem item = state.inventoryItem(itemInstanceId).orElse(null);
            if (item == null) {
                continue;
            }
            ItemMaster master = itemMasterRegistry.find(item.itemId()).orElse(null);
            if (master == null || master.setId() == null || master.setId().isBlank()) {
                continue;
            }
            String setId = master.setId().trim().toLowerCase();
            pieceCountBySet.merge(setId, 1, Integer::sum);
        }

        Map<String, List<SetBonusRule>> activeRules = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : pieceCountBySet.entrySet()) {
            List<SetBonusRule> activated = setBonusRegistry.findBySetId(entry.getKey()).stream()
                    .filter(rule -> entry.getValue() >= rule.pieceCount())
                    .toList();
            if (!activated.isEmpty()) {
                activeRules.put(entry.getKey(), activated);
            }
        }

        return new SetBonusState(Map.copyOf(pieceCountBySet), Map.copyOf(activeRules));
    }

    public GrowthStatBlock applyToFinalStats(SetBonusState state) {
        GrowthStatBlock block = new GrowthStatBlock();
        for (List<SetBonusRule> rules : state.activeRulesBySet().values()) {
            for (SetBonusRule rule : rules) {
                if ("FLAG".equalsIgnoreCase(rule.valueType())) {
                    block.addFlag(rule.effectType());
                } else {
                    block.add(rule.effectType(), rule.valueAmount());
                }
            }
        }
        return block;
    }

    public record SetBonusState(
            Map<String, Integer> pieceCountBySet,
            Map<String, List<SetBonusRule>> activeRulesBySet
    ) {
    }
}
