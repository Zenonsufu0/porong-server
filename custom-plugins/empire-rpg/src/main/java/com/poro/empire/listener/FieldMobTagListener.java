package com.poro.empire.listener;

import io.lumine.mythic.bukkit.events.MythicMobSpawnEvent;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public final class FieldMobTagListener implements Listener {

    @EventHandler
    public void onSpawn(MythicMobSpawnEvent event) {
        String type = event.getMobType().getInternalName();
        Entity entity = event.getEntity();
        tag(entity, type);
    }

    private void tag(Entity entity, String type) {
        switch (type) {
            // Field 1 — 수도 외곽 평원
            case "Plains_Soldier", "Plains_Wildling"
                    -> entity.addScoreboardTag("empire_field_1");
            case "Plains_StalkerElite" -> {
                entity.addScoreboardTag("empire_field_1");
                entity.addScoreboardTag("empire_rank_elite");
            }
            case "Plains_Predator" -> {
                entity.addScoreboardTag("empire_field_1");
                entity.addScoreboardTag("empire_type_field_boss");
            }
            // Field 2 — 폐광 지대
            case "Mine_Crawler", "Mine_Husk"
                    -> entity.addScoreboardTag("empire_field_2");
            case "Mine_WatcherElite" -> {
                entity.addScoreboardTag("empire_field_2");
                entity.addScoreboardTag("empire_rank_elite");
            }
            case "Mine_Golem" -> {
                entity.addScoreboardTag("empire_field_2");
                entity.addScoreboardTag("empire_type_field_boss");
            }
            // Field 3 — 오염된 수로
            case "Waterway_Drowned", "Waterway_Guardian"
                    -> entity.addScoreboardTag("empire_field_3");
            case "Waterway_LurkerElite" -> {
                entity.addScoreboardTag("empire_field_3");
                entity.addScoreboardTag("empire_rank_elite");
            }
            case "Waterway_Lord" -> {
                entity.addScoreboardTag("empire_field_3");
                entity.addScoreboardTag("empire_type_field_boss");
            }
            // Field 4 — 무너진 초소
            case "Outpost_Pillager", "Outpost_Vindicator"
                    -> entity.addScoreboardTag("empire_field_4");
            case "Outpost_CaptainElite" -> {
                entity.addScoreboardTag("empire_field_4");
                entity.addScoreboardTag("empire_rank_elite");
            }
            case "Fallen_Knight" -> {
                entity.addScoreboardTag("empire_field_4");
                entity.addScoreboardTag("empire_type_field_boss");
            }
            // Field 5 — 고대 성벽 잔해
            case "Wall_Enderman", "Wall_Shade"
                    -> entity.addScoreboardTag("empire_field_5");
            case "Wall_SentinelElite" -> {
                entity.addScoreboardTag("empire_field_5");
                entity.addScoreboardTag("empire_rank_elite");
            }
            case "Rift_Watcher" -> {
                entity.addScoreboardTag("empire_field_5");
                entity.addScoreboardTag("empire_type_field_boss");
            }
        }
    }
}
