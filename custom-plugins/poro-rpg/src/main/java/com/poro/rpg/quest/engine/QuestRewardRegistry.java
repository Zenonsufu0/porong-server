package com.poro.rpg.quest.engine;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public final class QuestRewardRegistry {
    private final Map<String, List<QuestRewardEntry>> rewardsByQuest = new LinkedHashMap<>();

    public void register(QuestRewardEntry entry) {
        String questId = normalize(entry.questId());
        List<QuestRewardEntry> updated = rewardsByQuest.getOrDefault(questId, List.of()).stream()
                .filter(existing -> existing.rewardOrder() != entry.rewardOrder())
                .collect(Collectors.toCollection(java.util.ArrayList::new));
        updated.add(entry);
        updated.sort(Comparator.comparingInt(QuestRewardEntry::rewardOrder));
        rewardsByQuest.put(questId, List.copyOf(updated));
    }

    public List<QuestRewardEntry> rewards(String questId) {
        return rewardsByQuest.getOrDefault(normalize(questId), List.of());
    }

    public Map<String, List<QuestRewardEntry>> all() {
        Map<String, List<QuestRewardEntry>> copy = new LinkedHashMap<>();
        rewardsByQuest.forEach((questId, entries) -> copy.put(questId, List.copyOf(entries)));
        return Map.copyOf(copy);
    }

    public int size() {
        return rewardsByQuest.values().stream().mapToInt(List::size).sum();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
