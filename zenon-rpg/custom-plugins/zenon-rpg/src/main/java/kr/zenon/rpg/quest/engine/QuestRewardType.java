package kr.zenon.rpg.quest.engine;

import java.util.Locale;

public enum QuestRewardType {
    GOLD("gold"),
    ITEM("item"),
    LIFE_EXP("life_exp"),
    UNLOCK("unlock"),
    TITLE("title"),
    BADGE("badge"),
    RECORD("record");

    private final String code;

    QuestRewardType(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    public static QuestRewardType from(String value) {
        String normalized = normalize(value);
        for (QuestRewardType type : values()) {
            if (type.code.equals(normalized)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown quest reward type: " + value);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
