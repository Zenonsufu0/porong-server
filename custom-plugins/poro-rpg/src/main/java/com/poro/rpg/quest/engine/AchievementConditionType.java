package com.poro.rpg.quest.engine;

import java.util.Locale;

public enum AchievementConditionType {
    BOSS_CLEAR,
    NO_DEATH_CLEAR,
    FIRST_CRAFT,
    CRAFT_ANY,
    ESTATE_UNLOCK,
    REGION_DISCOVER,
    EXTREME_CLEAR,
    QUEST_CLEAR;

    public static AchievementConditionType from(String value) {
        String normalized = normalize(value).replace('-', '_');
        for (AchievementConditionType type : values()) {
            if (type.name().equals(normalized)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown achievement condition type: " + value);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }
}
