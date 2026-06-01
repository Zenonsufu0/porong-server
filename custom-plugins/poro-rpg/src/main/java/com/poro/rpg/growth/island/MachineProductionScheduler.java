package com.poro.rpg.growth.island;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.ThreadLocalRandom;

/**
 * 영지 시설 주기 생산 스케줄러 (오프라인 누적, DL-088).
 * <p>{@code lastProductionAt} 기준으로 경과한 20분 인터벌 수만큼 누적 생산한다. 오프라인 시간도 포함되며
 * 시설 레벨별 {@code storage_hours_cap}(Lv1 12h / Lv2 16h / Lv3 24h, estate_facility_level_rule.csv)으로 상한.
 * 영속화된 {@code lastProductionAt} 덕분에 로그아웃→재접속 사이 오프라인 누적이 다음 틱(≤20분)에 정산된다.</p>
 */
public final class MachineProductionScheduler {

    private static final long   TICK_INTERVAL_TICKS  = 20L * 60 * 20;   // 20분(틱 스케줄)
    private static final long   INTERVAL_MS          = 20L * 60 * 1000; // 생산 인터벌 20분
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
        Bukkit.getScheduler().runTaskTimer(plugin, this::tick, TICK_INTERVAL_TICKS, TICK_INTERVAL_TICKS);
    }

    private void tick() {
        long now = System.currentTimeMillis();
        for (Player player : Bukkit.getOnlinePlayers()) {
            stateStore.get(player.getUniqueId()).ifPresent(state -> accrue(state, now));
        }
    }

    /**
     * lastProductionAt 이후 경과 인터벌만큼(상한 적용) 누적 생산. 오프라인 시간 포함.
     * 최초(lastProductionAt=0)에는 소급 생산 없이 기준점만 설정한다.
     */
    public void accrue(IslandTerritoryState state, long now) {
        long last = state.lastProductionAt();
        if (last <= 0L) {
            state.setLastProductionAt(now);
            return;
        }
        int intervals = (int) ((now - last) / INTERVAL_MS);
        if (intervals <= 0) return;

        int machineLevel = machineLevel(state);
        int cap = capIntervals(machineLevel);
        int cycles = Math.min(intervals, cap);

        produceHerbs(state, machineLevel, cycles);
        produceOres(state, machineLevel, cycles);

        if (intervals > cap) {
            state.setLastProductionAt(now);                          // 상한 초과분 폐기
        } else {
            state.setLastProductionAt(last + (long) intervals * INTERVAL_MS); // 잔여 인터벌 보존
        }
    }

    /** 작위 티어 기반 기계 레벨 (island_system_design.md §2.2): BARON(3)+ → Lv2, COUNT(5)+ → Lv3. */
    private int machineLevel(IslandTerritoryState state) {
        int tier = state.rank().tier;
        return tier < 3 ? 1 : tier < 5 ? 2 : 3;
    }

    /** storage_hours_cap → 20분 인터벌 수. Lv1 12h=36 / Lv2 16h=48 / Lv3 24h=72. */
    private int capIntervals(int machineLevel) {
        int capHours = machineLevel >= 3 ? 24 : machineLevel >= 2 ? 16 : 12;
        return capHours * 60 / 20;
    }

    private void produceHerbs(IslandTerritoryState state, int machineLevel, int cycles) {
        int reapers = state.reaperCount();
        if (reapers <= 0) return;
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        long totalHerbs = 0;
        long totalEssence = 0;
        for (int c = 0; c < cycles; c++) {
            for (int i = 0; i < reapers; i++) {
                switch (machineLevel) {
                    case 3 -> { // Lv3: 4~6 약초 + 30% 정수
                        totalHerbs += rng.nextInt(4, 7);
                        if (rng.nextInt(100) < 30) totalEssence++;
                    }
                    case 2 -> { // Lv2: 3~4 약초 + 10% 정수
                        totalHerbs += rng.nextInt(3, 5);
                        if (rng.nextInt(100) < 10) totalEssence++;
                    }
                    default -> totalHerbs += rng.nextInt(2, 4); // Lv1: 2~3 약초
                }
            }
        }
        state.addCustomItem(MAT_HERB_IMPERIAL, totalHerbs);
        if (totalEssence > 0) state.addCustomItem(MAT_ESSENCE_IMPERIAL, totalEssence);
    }

    private void produceOres(IslandTerritoryState state, int machineLevel, int cycles) {
        int miners = state.minerCount();
        if (miners <= 0) return;
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        long totalOre = 0;
        long totalSilver = 0;
        for (int c = 0; c < cycles; c++) {
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
        }
        state.addCustomItem(MAT_ORE_RESONANCE, totalOre);
        if (totalSilver > 0) state.addCustomItem(MAT_SILVER_ORE, totalSilver);
    }
}
