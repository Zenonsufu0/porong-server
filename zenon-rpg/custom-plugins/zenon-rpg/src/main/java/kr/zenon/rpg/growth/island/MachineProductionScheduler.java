package kr.zenon.rpg.growth.island;

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

    private static final long   TICK_INTERVAL_TICKS  = 60L * 20;        // 1분마다 점검(시설별 20분 마크 제때 포착)
    private static final long   INTERVAL_MS          = 20L * 60 * 1000; // 생산 인터벌 20분(시설별)
    private static final String MAT_HERB_IMPERIAL     = "mat_herb_imperial";
    private static final String MAT_ESSENCE_IMPERIAL  = "mat_essence_imperial";
    private static final String MAT_ORE_RESONANCE     = "res_ore_resonance"; // 마도철 원석
    private static final String MAT_SILVER_ORE        = "res_silver_ore";    // 은 원석 (레어)

    private final JavaPlugin                plugin;
    private final IslandTerritoryStateStore stateStore;

    /** 생산 인터벌(ms) — GUI 시설별 카운트다운 표시용 공개. */
    public static long intervalMs() { return INTERVAL_MS; }

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
     * 시설별 설치시각 기준 누적 생산 (DL-129 추가#11). 각 약초/광물 시설이 자기 마지막 생산시각+20분마다 독립 생산.
     * 오프라인 시간 포함(상한 cap). 전역 틱 익스플로잇(생산 직전 슬롯 교체)을 구조적으로 차단.
     */
    public void accrue(IslandTerritoryState state, long now) {
        int lv  = machineLevel(state);
        int cap = capIntervals(lv);
        ThreadLocalRandom rng = ThreadLocalRandom.current();

        long herbs = accrueProducers(state.herbProducedAt(), now, cap, lv, rng);
        if (herbs > 0) state.addCustomItem(MAT_HERB_IMPERIAL, herbs & 0xFFFFFFFFL);
        long herbEssence = herbs >>> 32;
        if (herbEssence > 0) state.addCustomItem(MAT_ESSENCE_IMPERIAL, herbEssence);

        long ores = accrueProducers(state.oreProducedAt(), now, cap, lv, rng);
        if ((ores & 0xFFFFFFFFL) > 0) state.addCustomItem(MAT_ORE_RESONANCE, ores & 0xFFFFFFFFL);
        long oreRare = ores >>> 32;
        if (oreRare > 0) state.addCustomItem(MAT_SILVER_ORE, oreRare);
    }

    /**
     * 생산기 리스트를 시설별로 누적 처리하고 각 시설의 마지막 생산시각을 갱신.
     * 반환: 하위32비트=기본 산출물 총량, 상위32비트=레어 보너스 총량 (약초=정수 / 광물=은원석).
     */
    private long accrueProducers(java.util.List<Long> producedAt, long now, int cap, int lv, ThreadLocalRandom rng) {
        long base = 0, rare = 0;
        for (int i = 0; i < producedAt.size(); i++) {
            long last = producedAt.get(i);
            if (last <= 0L) { producedAt.set(i, now); continue; } // 설치 직후 기준점만
            int intervals = (int) ((now - last) / INTERVAL_MS);
            if (intervals <= 0) continue;
            int cycles = Math.min(intervals, cap);
            for (int c = 0; c < cycles; c++) {
                switch (lv) {
                    case 3 -> { base += rng.nextInt(4, 7); if (rng.nextInt(100) < 30) rare++; }
                    case 2 -> { base += rng.nextInt(3, 5); if (rng.nextInt(100) < 10) rare++; }
                    default -> base += rng.nextInt(2, 4);
                }
            }
            producedAt.set(i, intervals > cap ? now : last + (long) intervals * INTERVAL_MS);
        }
        return (rare << 32) | (base & 0xFFFFFFFFL);
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

}
