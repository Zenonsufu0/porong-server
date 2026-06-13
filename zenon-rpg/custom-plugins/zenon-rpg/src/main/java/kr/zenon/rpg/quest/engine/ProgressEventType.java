package kr.zenon.rpg.quest.engine;

import java.util.Locale;

public enum ProgressEventType {
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
    ENHANCE_ITEM,
    QUEST_CLEAR,
    BOSS_CLEAR,
    NO_DEATH_CLEAR,
    ESTATE_UNLOCK,
    REGION_DISCOVER,
    EXTREME_CLEAR;

    public static ProgressEventType from(String value) {
        String normalized = normalize(value).replace('-', '_');
        for (ProgressEventType type : values()) {
            if (type.name().equals(normalized)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown progress event type: " + value);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }
}
