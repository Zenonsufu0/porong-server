package com.poro.rpg.quest.engine;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public final class QuestChainBranchRegistry {
    private final Map<String, List<QuestChainBranchRule>> branchRulesByParent = new LinkedHashMap<>();

    public void register(QuestChainBranchRule rule) {
        String parent = normalize(rule.parentQuestId());
        List<QuestChainBranchRule> updated = branchRulesByParent.getOrDefault(parent, List.of()).stream()
                .filter(existing -> !existing.branchQuestId().equals(rule.branchQuestId()))
                .collect(Collectors.toCollection(java.util.ArrayList::new));
        updated.add(rule);
        branchRulesByParent.put(parent, List.copyOf(updated));
    }

    public List<QuestChainBranchRule> branches(String parentQuestId) {
        return branchRulesByParent.getOrDefault(normalize(parentQuestId), List.of());
    }

    public Map<String, List<QuestChainBranchRule>> all() {
        Map<String, List<QuestChainBranchRule>> copy = new LinkedHashMap<>();
        branchRulesByParent.forEach((questId, rules) -> copy.put(questId, List.copyOf(rules)));
        return Map.copyOf(copy);
    }

    public int size() {
        return branchRulesByParent.values().stream().mapToInt(List::size).sum();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
