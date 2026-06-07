package com.poro.rpg.listener;

import com.poro.rpg.admin.AdminTogglesService;
import com.poro.rpg.boss.engine.BossRewardService;
import com.poro.rpg.field.ContributionTracker;
import com.poro.rpg.field.FieldBossRespawnScheduler;
import com.poro.rpg.growth.GrowthStateStore;
import com.poro.rpg.growth.engine.ItemGrade;
import com.poro.rpg.growth.engine.PlayerGrowthState;
import com.poro.rpg.growth.engine.PotentialLine;
import com.poro.rpg.growth.engine.TraceInstance;
import com.poro.rpg.growth.engine.TraceSubstatRoller;
import com.poro.rpg.growth.island.IslandTerritoryStateStore;
import com.poro.rpg.leveling.PlayerLevelingService;
import com.poro.rpg.scoreboard.ScoreboardService;
import com.poro.rpg.storage.PlayerDataManager;
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
    private final ScoreboardService scoreboardService;
    /** EXP_BOOST / DROP_BOOST 운영 토글 (optional — null이면 미적용). */
    private final AdminTogglesService togglesService;
    /** 흔적 인스턴스 세부스탯 롤러 (DL-129 추가#38, P2). null이면 흔적 드랍 생략. */
    private final TraceSubstatRoller traceSubstatRoller;

    public FieldDropListener(
            GrowthStateStore growthStateStore,
            IslandTerritoryStateStore islandTerritoryStateStore,
            PlayerDataManager playerDataManager,
            PlayerLevelingService playerLevelingService,
            FieldBossRespawnScheduler fieldBossScheduler,
            BossRewardService bossRewardService,
            ContributionTracker contributionTracker,
            ScoreboardService scoreboardService,
            AdminTogglesService togglesService,
            TraceSubstatRoller traceSubstatRoller
    ) {
        this.growthStateStore = growthStateStore;
        this.islandTerritoryStateStore = islandTerritoryStateStore;
        this.playerDataManager = playerDataManager;
        this.playerLevelingService = playerLevelingService;
        this.bossRewardService = bossRewardService;
        this.contributionTracker = contributionTracker;
        this.scoreboardService = scoreboardService;
        this.togglesService = togglesService;
        this.traceSubstatRoller = traceSubstatRoller;
    }

    /** EXP_BOOST 토글 시 EXP ×2. */
    private int expMultiplier() {
        return (togglesService != null && togglesService.isOn(AdminTogglesService.Toggle.EXP_BOOST)) ? 2 : 1;
    }

    /** DROP_BOOST 토글 시 드랍 수량 ×2 (확률은 유지 — 기댓값 정확히 2배). */
    private int dropMultiplier() {
        return (togglesService != null && togglesService.isOn(AdminTogglesService.Toggle.DROP_BOOST)) ? 2 : 1;
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
        int exp = profile.expAmount() * expMultiplier();
        int levelsGained = playerLevelingService.addExp(growth, exp);
        String drops = grantFieldDrops(killer, profile, growth);
        // 처치당 획득 요약 1줄 (경험치 + 재화) — /알람 OFF면 생략
        if (RewardNotify.isEnabled(killer.getUniqueId())) {
            killer.sendMessage("§8[처치] §a+" + exp + " §7경험치" + drops);
        }
        if (levelsGained > 0) {
            killer.sendMessage("§6§l[레벨업!] §eLv " + growth.playerLevel()
                    + " §7달성! 스탯 포인트 §a+" + (levelsGained * 3));
        }
        scoreboardService.refresh(killer);
    }

    /** 필드 드랍 지급 + 획득 요약 문자열 반환(채팅 알림용). */
    private String grantFieldDrops(Player player, FieldDropProfile profile, PlayerGrowthState growth) {
        int mult = dropMultiplier();
        StringBuilder sb = new StringBuilder();
        long gold = randomInclusive(profile.goldMin, profile.goldMax) * mult;
        growth.addCurrency(GOLD, gold);
        sb.append(" §6+").append(gold).append("G");
        if (roll(profile.battleShardChancePct)) {
            long shard = randomInclusive(profile.battleShardMin, profile.battleShardMax) * mult;
            islandTerritoryStateStore.getOrCreate(player.getUniqueId(), player.getName())
                    .addCustomItem(BATTLE_SHARD, shard);
            sb.append(" §e+").append(shard).append(" 전투파편");
        }
        if (roll(profile.enhancementStoneChancePct)) {
            long stone = randomInclusive(profile.enhancementStoneMin, profile.enhancementStoneMax) * mult;
            growth.addCurrency(ENHANCEMENT_STONE, stone);
            sb.append(" §b+").append(stone).append(" 강화석");
        }
        if (roll(profile.cubeFragmentChancePct)) {
            growth.addCurrency(CUBE_FRAGMENT, mult);
            sb.append(" §d+").append(mult).append(" 큐브조각");
            if (growth.currency(CUBE_FRAGMENT) >= 10) {
                growth.consumeCurrency(CUBE_FRAGMENT, 10);
                growth.addCurrency(CUBE, 1);
                sb.append(" §5(큐브 1개 완성!)");
                player.getServer().getLogger().info(
                        "[Cube] " + player.getUniqueId() + " 10 fragments -> 1 cube (wallet)");
            }
        }
        if (profile.elite && roll(profile.traceChancePct) && traceSubstatRoller != null) {
            // 인스턴스화 (DL-129 추가#38, P2): mult개의 독립 흔적 — 각자 등급·세부스탯 롤.
            var state = islandTerritoryStateStore.getOrCreate(player.getUniqueId(), player.getName());
            ItemGrade lastGrade = null;
            for (int i = 0; i < mult; i++) {
                ItemGrade grade = randomTraceGrade(profile.field());
                java.util.List<PotentialLine> substats = traceSubstatRoller.roll(grade);
                state.addTraceInstance(new TraceInstance(
                        "trace_" + UUID.randomUUID(), grade, substats));
                lastGrade = grade;
            }
            // 메시지: 1개면 등급 명시, 여러 개면 개수만 (등급 혼재 가능).
            if (mult == 1 && lastGrade != null) {
                sb.append(" §3+1 ").append(com.poro.rpg.gui.EquipmentLoreRenderer.gradeColor(lastGrade))
                        .append(lastGrade.displayName()).append(" §3흔적");
            } else {
                sb.append(" §3+").append(mult).append(" 흔적");
            }
        }
        return sb.toString();
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

    /** 필드별 흔적 등급 분포 (DL-129 추가#38 P2 — 기존 randomTraceId 분포를 ItemGrade로 환산). */
    private ItemGrade randomTraceGrade(int field) {
        double roll = ThreadLocalRandom.current().nextDouble(100.0);
        if (field == 5) {
            if (roll < 2.0)  return ItemGrade.LEGENDARY; // 2%
            if (roll < 7.0)  return ItemGrade.UNIQUE;    // 5%
            if (roll < 25.0) return ItemGrade.EPIC;      // 18%
            if (roll < 60.0) return ItemGrade.RARE;      // 35%
            return ItemGrade.COMMON;                      // 40%
        }
        if (field == 4) {
            if (roll < 0.5)  return ItemGrade.LEGENDARY; // 0.5%
            if (roll < 3.0)  return ItemGrade.UNIQUE;    // 2.5%
            if (roll < 13.0) return ItemGrade.EPIC;      // 10%
            if (roll < 48.0) return ItemGrade.RARE;      // 35%
            return ItemGrade.COMMON;                      // 52%
        }
        // 필드 1~3 기본
        if (roll < 5.0)  return ItemGrade.EPIC;  // 5%
        if (roll < 40.0) return ItemGrade.RARE;  // 35%
        return ItemGrade.COMMON;                  // 60%
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
