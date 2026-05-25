package com.poro.empire.growth.island;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * 영지 자동재배기 주기 생산 스케줄러.
 * 20분(24000틱)마다 온라인 플레이어의 재배기 대수에 따라 제국 약초를 customItems에 추가한다.
 */
public final class MachineProductionScheduler {

    private static final long   TICK_INTERVAL        = 20L * 60 * 20; // 20분
    private static final String MAT_HERB_IMPERIAL     = "mat_herb_imperial";
    private static final String MAT_ESSENCE_IMPERIAL  = "mat_essence_imperial";

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
            stateStore.get(player.getUniqueId()).ifPresent(this::produceHerbs);
        }
    }

    private void produceHerbs(IslandTerritoryState state) {
        int reapers = state.reaperCount();
        if (reapers <= 0) return;

        // 기계 레벨 = 작위 티어 기반 (BARON+ → Lv2, COUNT+ → Lv3)
        int tier = state.rank().tier;
        int machineLevel = tier < 3 ? 1 : tier < 5 ? 2 : 3;

        int herbPerReaper = machineLevel;          // Lv1=1, Lv2=2, Lv3=3
        state.addCustomItem(MAT_HERB_IMPERIAL, reapers * herbPerReaper);

        // 에센스: Lv1=3대당 1개, Lv2=2대당 1개, Lv3=1대당 1개
        int essenceDivisor = switch (machineLevel) { case 3 -> 1; case 2 -> 2; default -> 3; };
        int essence = reapers / essenceDivisor;
        if (essence > 0) state.addCustomItem(MAT_ESSENCE_IMPERIAL, essence);
    }
}
