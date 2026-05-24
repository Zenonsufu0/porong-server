package com.poro.empire.listener;

import org.bukkit.entity.Entity;

public final class MobTagHelper {
    private MobTagHelper() {}

    public static int fieldIndex(Entity entity) {
        for (String tag : entity.getScoreboardTags()) {
            if (tag.startsWith("empire_field_")) {
                try {
                    return Integer.parseInt(tag.substring(13));
                } catch (NumberFormatException ignored) {}
            }
        }
        return 0;
    }

    public static boolean isElite(Entity entity) {
        return entity.getScoreboardTags().contains("empire_rank_elite");
    }

    public static boolean isFieldBoss(Entity entity) {
        return entity.getScoreboardTags().contains("empire_type_field_boss");
    }
}
