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
        // 재배기 1대당 제국 약초 1개 (Lv1 기준; 레벨별 증산은 추후 연동)
        state.addCustomItem(MAT_HERB_IMPERIAL, reapers);
        // 재배기 3대 이상 보유 시 에센스 추가 (Lv3 재배기 판단 임시 기준)
        if (reapers >= 3) {
            state.addCustomItem(MAT_ESSENCE_IMPERIAL, reapers / 3);
        }
    }
}
