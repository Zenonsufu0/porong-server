package kr.zenon.rpg.listener;

import org.bukkit.entity.Entity;

public final class MobTagHelper {
    private MobTagHelper() {}

    public static final String FIELD_TAG_PREFIX = "poro_field_";
    public static final String ELITE_TAG        = "poro_rank_elite";

    public static int fieldIndex(Entity entity) {
        for (String tag : entity.getScoreboardTags()) {
            if (tag.startsWith(FIELD_TAG_PREFIX)) {
                try {
                    return Integer.parseInt(tag.substring(FIELD_TAG_PREFIX.length()));
                } catch (NumberFormatException ignored) {}
            }
        }
        return 0;
    }

    /** 필드몹에 필드 인덱스(+정예) 스코어보드 태그 부여. */
    public static void tagFieldMob(Entity entity, int field, boolean elite) {
        entity.addScoreboardTag(FIELD_TAG_PREFIX + field);
        if (elite) entity.addScoreboardTag(ELITE_TAG);
    }

    public static boolean isElite(Entity entity) {
        return entity.getScoreboardTags().contains(ELITE_TAG);
    }

    public static boolean isFieldBoss(Entity entity) {
        return entity.getScoreboardTags().contains("poro_type_field_boss");
    }
}
