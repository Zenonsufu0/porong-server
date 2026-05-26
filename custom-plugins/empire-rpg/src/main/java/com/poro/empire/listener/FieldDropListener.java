package com.poro.empire.listener;

import com.poro.empire.boss.engine.BossRewardService;
import com.poro.empire.field.ContributionTracker;
import com.poro.empire.field.FieldBossRespawnScheduler;
import com.poro.empire.growth.GrowthStateStore;
import com.poro.empire.growth.engine.PlayerGrowthState;
import com.poro.empire.growth.island.IslandTerritoryStateStore;
import com.poro.empire.leveling.PlayerLevelingService;
import com.poro.empire.storage.PlayerDataManager;
import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.Locale;
import java.util.UUID;
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
    private final PlayerLevelingService playerLevelingService;
    private final BossRewardService bossRewardService;
    private final ContributionTracker contributionTracker;

    public FieldDropListener(
            GrowthStateStore growthStateStore,
            IslandTerritoryStateStore islandTerritoryStateStore,
            PlayerDataManager playerDataManager,
            PlayerLevelingService playerLevelingService,
            FieldBossRespawnScheduler fieldBossScheduler,
            BossRewardService bossRewardService,
            ContributionTracker contributionTracker
    ) {
        this.growthStateStore = growthStateStore;
        this.islandTerritoryStateStore = islandTerritoryStateStore;
        this.playerDataManager = playerDataManager;
        this.playerLevelingService = playerLevelingService;
        this.bossRewardService = bossRewardService;
        this.contributionTracker = contributionTracker;
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!MobTagHelper.isFieldBoss(event.getEntity())) return;
        double finalDamage = event.getFinalDamage();
        if (finalDamage <= 0) return;
        Player player = resolvePlayer(event.getDamager());
        if (player == null) return;
        contributionTracker.recordDamage(
                event.getEntity().getUniqueId(),
                player.getUniqueId(),
                finalDamage);
    }

    @EventHandler
    public void onEntityRemove(EntityRemoveFromWorldEvent event) {
        if (!MobTagHelper.isFieldBoss(event.getEntity())) return;
        contributionTracker.evict(event.getEntity().getUniqueId());
    }

    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        if (MobTagHelper.isFieldBoss(event.getEntity())) {
            int field = MobTagHelper.fieldIndex(event.getEntity());
            if (field > 0) {
                UUID entityId = event.getEntity().getUniqueId();
                java.util.Map<UUID, Double> shares = contributionTracker.finalizeShares(entityId);
                if (shares.isEmpty()) {
                    event.getEntity().getServer().getLogger().warning(
                            "[ContributionTracker] field boss " + entityId + " (field=" + field
                                    + ") died with no tracked damage — no reward granted");
                } else {
                    shares.forEach((uuid, share) -> {
                        if (share >= 3.0) bossRewardService.grantFieldBossReward(uuid, field);
                    });
                }
            }
            return;
        }
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;
        FieldDropProfile profile = profileFor(event.getEntity());
        if (profile == null) return;
        String classId = playerDataManager.getWeaponType(killer.getUniqueId()).name().toLowerCase(Locale.ROOT);
        PlayerGrowthState growth = growthStateStore.getOrCreate(killer.getUniqueId(), classId);
        int levelsGained = playerLevelingService.addExp(growth, profile.expAmount());
        if (levelsGained > 0) {
            killer.sendMessage("§6§l[레벨업!] §eLv " + growth.playerLevel()
                    + " §7달성! 스탯 포인트 §a+" + (levelsGained * 3));
        }
        grantFieldDrops(killer, profile, growth);
    }

    private void grantFieldDrops(Player player, FieldDropProfile profile, PlayerGrowthState growth) {
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
                    .addCustomItem(randomTraceId(profile.field()), 1);
        }
    }

    private Player resolvePlayer(Entity damager) {
        if (damager instanceof Player p) return p;
        if (damager instanceof Projectile proj && proj.getShooter() instanceof Player p) return p;
        return null;
    }

    private FieldDropProfile profileFor(Entity entity) {
        if (MobTagHelper.isFieldBoss(entity)) return null;
        int field = MobTagHelper.fieldIndex(entity);
        if (field == 0) return null;
        boolean elite = MobTagHelper.isElite(entity);
        if (elite) {
            return switch (field) {
                case 2 -> new FieldDropProfile(true, 30, 50, 80, 2, 3, 15, 1, 1, 7,  5,  50,  2);
                case 3 -> new FieldDropProfile(true, 35, 57, 75, 2, 3, 15, 1, 2, 10, 8,  75,  3);
                case 4 -> new FieldDropProfile(true, 38, 65, 75, 2, 3, 20, 1, 2, 12, 10, 110, 4);
                case 5 -> new FieldDropProfile(true, 45, 75, 75, 2, 3, 20, 1, 2, 15, 12, 150, 5);
                default -> new FieldDropProfile(true, 23, 42, 80, 2, 3, 10, 1, 1, 5,  5,  25,  1);
            };
        }
        return switch (field) {
            case 2 -> new FieldDropProfile(false, 8,  14, 65, 1, 2, 8,  1, 1, 1.5, 0, 20, 2);
            case 3 -> new FieldDropProfile(false, 9,  15, 60, 1, 2, 9,  1, 1, 2,   0, 30, 3);
            case 4 -> new FieldDropProfile(false, 11, 17, 60, 1, 2, 10, 1, 1, 2.5, 0, 45, 4);
            case 5 -> new FieldDropProfile(false, 12, 21, 55, 1, 2, 12, 1, 1, 3,   0, 60, 5);
            default -> new FieldDropProfile(false, 6,  12, 70, 1, 2, 6,  1, 1, 1,   0, 10, 1);
        };
    }

    private String randomTraceId(int field) {
        double roll = ThreadLocalRandom.current().nextDouble(100.0);
        if (field == 5) {
            if (roll < 2.0)  return "equip_trace_brilliant"; // 레전더리 2%
            if (roll < 7.0)  return "equip_trace_radiant";   // 유니크 5%
            if (roll < 25.0) return "equip_trace_glowing";   // 에픽 18%
            if (roll < 60.0) return "equip_trace_faded";     // 레어 35%
            return "equip_trace_broken";                      // 커먼 40%
        }
        if (field == 4) {
            if (roll < 0.5)  return "equip_trace_brilliant"; // 레전더리 0.5%
            if (roll < 3.0)  return "equip_trace_radiant";   // 유니크 2.5%
            if (roll < 13.0) return "equip_trace_glowing";   // 에픽 10%
            if (roll < 48.0) return "equip_trace_faded";     // 레어 35%
            return "equip_trace_broken";                      // 커먼 52%
        }
        // 필드 1~3 기본
        if (roll < 5.0)  return "equip_trace_glowing";   // 에픽 5%
        if (roll < 40.0) return "equip_trace_faded";     // 레어 35%
        return "equip_trace_broken";                      // 커먼 60%
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
            double traceChancePct,
            int expAmount,
            int field
    ) {}
}
