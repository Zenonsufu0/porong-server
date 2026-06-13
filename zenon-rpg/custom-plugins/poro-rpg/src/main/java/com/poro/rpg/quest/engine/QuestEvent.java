package com.poro.rpg.quest.engine;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public record QuestEvent(
        ProgressEventType eventType,
        String targetId,
        long amount,
        Map<String, String> metadata
) {
    public QuestEvent {
        targetId = normalize(targetId);
        amount = Math.max(0L, amount);
        metadata = metadata == null ? Map.of() : Map.copyOf(new LinkedHashMap<>(metadata));
    }

    public static QuestEvent of(ProgressEventType eventType, String targetId, long amount) {
        return new QuestEvent(eventType, targetId, amount, Map.of());
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
