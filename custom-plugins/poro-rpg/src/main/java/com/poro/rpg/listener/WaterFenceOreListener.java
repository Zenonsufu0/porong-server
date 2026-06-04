package com.poro.rpg.listener;

import com.poro.rpg.growth.island.IslandRank;
import com.poro.rpg.growth.island.IslandStorage;
import com.poro.rpg.growth.island.IslandStorageStore;
import com.poro.rpg.growth.island.IslandTerritoryState;
import com.poro.rpg.growth.island.IslandTerritoryStateStore;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.inventory.ItemStack;

import java.util.concurrent.ThreadLocalRandom;

/**
 * 기본 광물 생성기 (island_system_design.md §6, DL-129 추가#30·#31).
 * 영지(IridiumSkyblock 월드)에서 물이 울타리에 인접한 빈칸으로 흐르면 그 자리에 작위별 확률로 **광물 블럭**을 생성한다.
 * 드랍이 아니라 블럭이라 유저가 직접 캔다(바닐라 물 흐름 속도 ~0.25s로 자동 재생성 = "딱딱" 채굴).
 * 채굴 자동입금(CONV_AUTO_DEPOSIT) 켜져 있으면 캘 때 땅에 안 떨어지고 창고로 바로 입금.
 * 작위는 위치→소유자 API 부재로 근처(반경 12) 플레이어 기준.
 */
public final class WaterFenceOreListener implements Listener {

    private static final String ISLAND_WORLD = "IridiumSkyblock";
    private static final double NEAR_RADIUS  = 12.0;

    private final IslandTerritoryStateStore territoryStore;
    private final IslandStorageStore storageStore;

    public WaterFenceOreListener(IslandTerritoryStateStore territoryStore, IslandStorageStore storageStore) {
        this.territoryStore = territoryStore;
        this.storageStore = storageStore;
    }

    // 작위 tier(0 개척지 ~ 7 공작령)별 생성 광물 블럭 (행: §6 확률표 순서)
    private static final Material[] ORE_BLOCKS = {
            Material.COBBLESTONE, Material.COPPER_ORE, Material.IRON_ORE, Material.GOLD_ORE,
            Material.REDSTONE_ORE, Material.LAPIS_ORE, Material.DIAMOND_ORE, Material.EMERALD_ORE
    };
    private static final int[][] PROB = {
            // 개척 기사 준남 남작 자작 백작 후작 공작
            { 45, 38, 32, 26, 20, 15,  7,  5 }, // 조약돌
            { 20, 20, 18, 16, 13, 11,  7,  5 }, // 구리 원석
            { 17, 18, 20, 21, 20, 18, 14, 12 }, // 철 원석
            {  5,  7,  9, 12, 15, 18, 22, 20 }, // 금 원석
            {  5,  7,  8, 10, 12, 13, 15, 15 }, // 레드스톤
            {  4,  5,  7,  8, 10, 11, 13, 13 }, // 청금석
            {  3,  4,  5,  5,  7, 10, 13, 17 }, // 다이아몬드
            {  1,  1,  1,  2,  3,  4,  9, 13 }, // 에메랄드
    };

    /** 물이 울타리 인접 빈칸으로 흐를 때 → 광물 블럭 생성(드랍 아님). */
    @EventHandler(ignoreCancelled = true)
    public void onFlow(BlockFromToEvent event) {
        if (event.getBlock().getType() != Material.WATER) return;
        Block to = event.getToBlock();
        if (!ISLAND_WORLD.equals(to.getWorld().getName())) return;
        if (to.getType() != Material.AIR) return;          // 빈칸에만 생성
        if (!touchesFence(to)) return;                     // 울타리 인접

        int tier = nearestRankTier(to);
        if (tier < 0) return;                              // 근처 소유자 없음 → 일반 물 흐름

        event.setCancelled(true);                          // 물 대신 광물 블럭
        to.setType(rollOreBlock(tier, ThreadLocalRandom.current()));
    }

    /** 채굴 자동입금 — CONV_AUTO_DEPOSIT 켜진 플레이어가 영지에서 캐면 드랍을 창고로(땅에 안 떨어짐). */
    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Player p = event.getPlayer();
        if (!ISLAND_WORLD.equals(p.getWorld().getName())) return;
        if (p.getGameMode() == GameMode.CREATIVE) return;
        boolean auto = territoryStore.get(p.getUniqueId())
                .map(s -> s.hasConvenience(IslandTerritoryState.CONV_AUTO_DEPOSIT)).orElse(false);
        if (!auto) return;

        var drops = event.getBlock().getDrops(p.getInventory().getItemInMainHand(), p);
        if (drops.isEmpty()) return;
        IslandStorage storage = storageStore.getOrCreate(p.getUniqueId());
        for (ItemStack d : drops) {
            if (d != null && !d.getType().isAir()) storage.add(d.getType(), d.getAmount());
        }
        event.setDropItems(false); // 땅 드랍 억제 — 창고로 귀속
    }

    private static boolean touchesFence(Block to) {
        for (BlockFace f : new BlockFace[]{BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST,
                BlockFace.WEST, BlockFace.UP, BlockFace.DOWN}) {
            if (Tag.FENCES.isTagged(to.getRelative(f).getType())) return true;
        }
        return false;
    }

    private int nearestRankTier(Block to) {
        org.bukkit.Location loc = to.getLocation();
        Player nearest = null;
        double best = NEAR_RADIUS * NEAR_RADIUS;
        for (Player p : to.getWorld().getPlayers()) {
            double d = p.getLocation().distanceSquared(loc);
            if (d <= best) { best = d; nearest = p; }
        }
        if (nearest == null) return -1;
        IslandRank rank = territoryStore.get(nearest.getUniqueId())
                .map(IslandTerritoryState::rank).orElse(IslandRank.FRONTIER);
        return rank.ordinal();
    }

    private static Material rollOreBlock(int tier, ThreadLocalRandom rng) {
        int roll = rng.nextInt(100);
        int cum = 0;
        for (int i = 0; i < ORE_BLOCKS.length; i++) {
            cum += PROB[i][tier];
            if (roll < cum) return ORE_BLOCKS[i];
        }
        return Material.COBBLESTONE;
    }
}
