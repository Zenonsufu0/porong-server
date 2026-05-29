package com.poro.empire.growth.island;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.ThreadLocalRandom;

/**
 * 영지 자동재배기 주기 생산 스케줄러.
 * 20분(24000틱)마다 온라인 플레이어의 재배기 대수에 따라 제국 약초를 customItems에 추가한다.
 */
public final class MachineProductionScheduler {

    private static final long   TICK_INTERVAL        = 20L * 60 * 20; // 20분
    private static final String MAT_HERB_IMPERIAL     = "mat_herb_imperial";
    private static final String MAT_ESSENCE_IMPERIAL  = "mat_essence_imperial";
    private static final String MAT_ORE_RESONANCE     = "res_ore_resonance"; // 마도철 원석
    private static final String MAT_SILVER_ORE        = "res_silver_ore";    // 은 원석 (레어)

    private final JavaPlugin                plugin;
    private final IslandTerritoryStateStore stateStore;

    public MachineProductionScheduler(JavaPlugin plugin, IslandTerritoryStateStore stateStore) {
        this.plugin     = plugin;
        this.stateStore = stateStore;
    }

    public void start() {
        Bukkit.getScheduler().runTaskTimer(plugin, this::tick, TICK_INTERVAL, TICK_INTERVAL);
    }

    private void tick() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            stateStore.get(player.getUniqueId()).ifPresent(state -> {
                produceHerbs(state);
                produceOres(state);
            });
        }
    }

    private void produceHerbs(IslandTerritoryState state) {
        int reapers = state.reaperCount();
        if (reapers <= 0) return;

        // 기계 레벨 = 작위 티어 기반 (island_system_design.md §2.2)
        // BARON(tier 3)+ → Lv2, COUNT(tier 5)+ → Lv3
        int tier = state.rank().tier;
        int machineLevel = tier < 3 ? 1 : tier < 5 ? 2 : 3;

        ThreadLocalRandom rng = ThreadLocalRandom.current();
        long totalHerbs = 0;
        long totalEssence = 0;

        for (int i = 0; i < reapers; i++) {
            switch (machineLevel) {
                case 3 -> {
                    // Lv3: 4~6 약초 + 30% 확률 정수 1개
                    totalHerbs += rng.nextInt(4, 7);
                    if (rng.nextInt(100) < 30) totalEssence++;
                }
                case 2 -> {
                    // Lv2: 3~4 약초 + 10% 확률 정수 1개
                    totalHerbs += rng.nextInt(3, 5);
                    if (rng.nextInt(100) < 10) totalEssence++;
                }
                default -> {
                    // Lv1: 2~3 약초, 정수 없음
                    totalHerbs += rng.nextInt(2, 4);
                }
            }
        }

        state.addCustomItem(MAT_HERB_IMPERIAL, totalHerbs);
        if (totalEssence > 0) state.addCustomItem(MAT_ESSENCE_IMPERIAL, totalEssence);
    }

    private void produceOres(IslandTerritoryState state) {
        int miners = state.minerCount();
        if (miners <= 0) return;

        int tier = state.rank().tier;
        int machineLevel = tier < 3 ? 1 : tier < 5 ? 2 : 3;

        ThreadLocalRandom rng = ThreadLocalRandom.current();
        long totalOre = 0;
        long totalSilver = 0;

        for (int i = 0; i < miners; i++) {
            switch (machineLevel) {
                case 3 -> {
                    totalOre += rng.nextInt(4, 7);
                    if (rng.nextInt(100) < 30) totalSilver++;
                }
                case 2 -> {
                    totalOre += rng.nextInt(3, 5);
                    if (rng.nextInt(100) < 10) totalSilver++;
                }
                default -> totalOre += rng.nextInt(2, 4);
            }
        }

        state.addCustomItem(MAT_ORE_RESONANCE, totalOre);
        if (totalSilver > 0) state.addCustomItem(MAT_SILVER_ORE, totalSilver);
    }
}
