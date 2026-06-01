package com.poro.rpg.quest.engine;

import java.util.Locale;

public enum QuestObjectiveType {
    TALK,
    KILL,
    COLLECT,
    GATHER,
    CRAFT,
    DELIVER,
    INTERACT,
    ENTER_REGION,
    ENTER_DUNGEON,
    CLEAR_BOSS,
    VIEW_UI,
    INSTALL_FACILITY,
    HARVEST,
    EQUIP_ITEM,
    ENHANCE_ITEM;

    public static QuestObjectiveType from(String value) {
        String normalized = normalize(value).replace('-', '_');
        for (QuestObjectiveType type : values()) {
            if (type.name().equals(normalized)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown quest objective type: " + value);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }
}
