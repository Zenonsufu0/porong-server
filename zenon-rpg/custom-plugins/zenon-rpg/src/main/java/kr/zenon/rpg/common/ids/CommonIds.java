package kr.zenon.rpg.common.ids;

public final class CommonIds {
    public static final String USER_ID_PREFIX = "user";
    public static final String RUN_ID_PREFIX = "run";
    public static final String USER_ITEM_ID_PREFIX = "user_item";
    public static final String QUEST_ID_PREFIX = "quest";

    private static final IdGenerator DEFAULT_GENERATOR = new PrefixUuidIdGenerator();

    private CommonIds() {
    }

    public static String newUserId() {
        return DEFAULT_GENERATOR.newId(USER_ID_PREFIX);
    }

    public static String newRunId() {
        return DEFAULT_GENERATOR.newId(RUN_ID_PREFIX);
    }

    public static String newUserItemId() {
        return DEFAULT_GENERATOR.newId(USER_ITEM_ID_PREFIX);
    }

    public static String newQuestId() {
        return DEFAULT_GENERATOR.newId(QUEST_ID_PREFIX);
    }

    public static boolean matchesPrefix(String id, String prefix) {
        if (id == null || prefix == null) {
            return false;
        }
        return id.startsWith(prefix + "_");
    }
}
