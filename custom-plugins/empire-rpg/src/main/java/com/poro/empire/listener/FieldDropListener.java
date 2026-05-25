package com.poro.empire.listener;

import com.poro.empire.field.FieldBossRespawnScheduler;
import com.poro.empire.growth.GrowthStateStore;
import com.poro.empire.growth.engine.PlayerGrowthState;
import com.poro.empire.growth.island.IslandTerritoryStateStore;
import com.poro.empire.leveling.LevelingService;
import com.poro.empire.storage.PlayerDataManager;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

public final class FieldDropListener implements Listener {
    private static final String GOLD = "gold";
    private static final String ENHANCEMENT_STONE = "mat_stone_enhance";
    private static final String BATTLE_SHARD = "mat_battle_shard";
    private static final String CUBE_FRAGMENT = "mat_cube_fragment";
    private static final String CUBE = "mat_cube";

    private final GrowthStateStore growthStateStore;
    private final IslandTerritoryStateStore islandTerritoryStateStore;
    private final PlayerDataManager playerDataManager;
    private final LevelingService levelingService;

    public FieldDropListener(
            GrowthStateStore growthStateStore,
            IslandTerritoryStateStore islandTerritoryStateStore,
            PlayerDataManager playerDataManager,
            LevelingService levelingService,
            FieldBossRespawnScheduler fieldBossScheduler
    ) {
        this.growthStateStore = growthStateStore;
        this.islandTerritoryStateStore = islandTerritoryStateStore;
        this.playerDataManager = playerDataManager;
        this.levelingService = levelingService;
    }

    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer != null) {
            levelingService.addExp(killer.getUniqueId(), 10L);
            if (MobTagHelper.isFieldBoss(event.getEntity())) {
                grantFieldBossDrops(killer, event.getEntity());
            } else {
                grantFieldDrops(killer, event.getEntity());
            }
        }
    }

    private void grantFieldBossDrops(Player player, Entity entity) {
        int field = MobTagHelper.fieldIndex(entity);
        if (field == 0) return;

        String classId = playerDataManager.getWeaponType(player.getUniqueId()).name().toLowerCase(java.util.Locale.ROOT);
        PlayerGrowthState growth = growthStateStore.getOrCreate(player.getUniqueId(), classId);

        long stone;
        long cube;
        double tracePct;
        switch (field) {
            case 2 -> { stone = randomInclusive(4, 6);  cube = randomInclusive(5, 8);  tracePct = 45; }
            case 3 -> { stone = randomInclusive(6, 9);  cube = randomInclusive(5, 8);  tracePct = 55; }
            case 4 -> { stone = randomInclusive(7, 10); cube = randomInclusive(8, 12); tracePct = 65; }
            case 5 -> { stone = randomInclusive(9, 13); cube = randomInclusive(10, 15); tracePct = 75; }
            default -> { stone = randomInclusive(2, 4); cube = randomInclusive(3, 5);  tracePct = 35; }
        }
        growth.addCurrency(ENHANCEMENT_STONE, stone);
        growth.addCurrency(CUBE_FRAGMENT, cube);
        if (roll(tracePct)) {
            islandTerritoryStateStore.getOrCreate(player.getUniqueId(), player.getName())
                    .addCustomItem(randomTraceId(), 1);
        }
    }

    private void grantFieldDrops(Player player, Entity entity) {
        FieldDropProfile profile = profileFor(entity);
        if (profile == null) {
            return;
        }
        String classId = playerDataManager.getWeaponType(player.getUniqueId()).name().toLowerCase(Locale.ROOT);
        PlayerGrowthState growth = growthStateStore.getOrCreate(player.getUniqueId(), classId);

        growth.addCurrency(GOLD, randomInclusive(profile.goldMin, profile.goldMax));
        if (roll(profile.battleShardChancePct)) {
            islandTerritoryStateStore.getOrCreate(player.getUniqueId(), player.getName())
                    .addCustomItem(BATTLE_SHARD, randomInclusive(profile.battleShardMin, profile.battleShardMax));
        }
        if (roll(profile.enhancementStoneChancePct)) {
            growth.addCurrency(ENHANCEMENT_STONE, randomInclusive(profile.enhancementStoneMin, profile.enhancementStoneMax));
        }
        if (roll(profile.cubeFragmentChancePct)) {
            growth.addCurrency(CUBE_FRAGMENT, 1);
            if (growth.currency(CUBE_FRAGMENT) >= 10) {
                growth.consumeCurrency(CUBE_FRAGMENT, 10);
                growth.addCurrency(CUBE, 1);
                player.getServer().getLogger().info(
                        "[Cube] " + player.getUniqueId() + " 10 fragments -> 1 cube (wallet)");
            }
        }
        if (profile.elite && roll(profile.traceChancePct)) {
            islandTerritoryStateStore.getOrCreate(player.getUniqueId(), player.getName())
                    .addCustomItem(randomTraceId(), 1);
        }
    }

    private FieldDropProfile profileFor(Entity entity) {
        if (MobTagHelper.isFieldBoss(entity)) return null;
        int field = MobTagHelper.fieldIndex(entity);
        if (field == 0) return null;
        boolean elite = MobTagHelper.isElite(entity);
        if (elite) {
            return switch (field) {
                case 2 -> new FieldDropProfile(true, 30, 50, 80, 2, 3, 15, 1, 1, 7, 5);
                case 3 -> new FieldDropProfile(true, 35, 57, 75, 2, 3, 15, 1, 2, 10, 8);
                case 4 -> new FieldDropProfile(true, 38, 65, 75, 2, 3, 20, 1, 2, 12, 10);
                case 5 -> new FieldDropProfile(true, 45, 75, 75, 2, 3, 20, 1, 2, 15, 12);
                default -> new FieldDropProfile(true, 23, 42, 80, 2, 3, 10, 1, 1, 5, 5);
            };
        }
        return switch (field) {
            case 2 -> new FieldDropProfile(false, 8, 14, 65, 1, 2, 8, 1, 1, 1.5, 0);
            case 3 -> new FieldDropProfile(false, 9, 15, 60, 1, 2, 9, 1, 1, 2, 0);
            case 4 -> new FieldDropProfile(false, 11, 17, 60, 1, 2, 10, 1, 1, 2.5, 0);
            case 5 -> new FieldDropProfile(false, 12, 21, 55, 1, 2, 12, 1, 1, 3, 0);
            default -> new FieldDropProfile(false, 6, 12, 70, 1, 2, 6, 1, 1, 1, 0);
        };
    }

    private String randomTraceId() {
        double roll = ThreadLocalRandom.current().nextDouble(100.0);
        if (roll < 5.0) {
            return "equip_trace_glowing";
        }
        if (roll < 40.0) {
            return "equip_trace_faded";
        }
        return "equip_trace_broken";
    }

    private boolean roll(double chancePct) {
        return ThreadLocalRandom.current().nextDouble(100.0) < chancePct;
    }

    private long randomInclusive(int min, int max) {
        return ThreadLocalRandom.current().nextLong(min, max + 1L);
    }

    private record FieldDropProfile(
            boolean elite,
            int goldMin,
            int goldMax,
            double battleShardChancePct,
            int battleShardMin,
            int battleShardMax,
            double enhancementStoneChancePct,
            int enhancementStoneMin,
            int enhancementStoneMax,
            double cubeFragmentChancePct,
            double traceChancePct
    ) {}
}
