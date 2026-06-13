package kr.zenon.rpg.gui;

import kr.zenon.rpg.growth.island.IslandTerritoryState;
import kr.zenon.rpg.growth.island.IslandTerritoryState.FacilityType;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * 시설 현황 GUI (45슬롯) — 작위별 시설 슬롯 한계 내에서 약초/광물/공방 시설을 배분 설치.
 *
 * <p>설계: island_system_design.md §2.1(작위별 시설 슬롯)·§3(시설 3종, GUI 슬롯 클릭 배정)·§3.4(공방 1대=대기 3슬롯).
 * 슬롯 0~17 = 시설 슬롯(설치/빈칸/잠금), 22 = 요약, 36 = 뒤로. 빈칸 클릭 → 시설 선택, 설치칸 Shift+클릭 → 제거(DL-129 추가#7).</p>
 */
public final class TerritoryFacilityGui {
    private TerritoryFacilityGui() {}

    public static final int SLOT_BACK = 36;
    public static final int SLOT_INFO = 22;
    private static final int FACILITY_ROWS = 18; // 슬롯 0~17 (최대 시설 슬롯 18과 일치)

    public static void open(Player player, IslandTerritoryState t) {
        Inventory inv = Bukkit.createInventory(null, 45, GuiTitles.TERRITORY_FACILITY);
        ItemStack gray = MainHubGui.icon(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < 45; i++) inv.setItem(i, gray);

        int reapers = t.reaperCount();
        int miners  = t.minerCount();
        int machines = t.workshopMachineCount();
        int used    = t.facilitySlotsUsed();
        int max     = t.facilitySlotsMax();
        int lv      = machineLevel(t);

        for (int i = 0; i < FACILITY_ROWS; i++) {
            if (i < reapers) {
                inv.setItem(i, MainHubGui.icon(Material.WHEAT, "§a약초 재배지 §fLv." + lv,
                        List.of("§720분 주기 — 제국 약초·정수",
                                "§7다음 생산: " + productionTimer(t.herbProducedAt().get(i)),
                                "§8Shift+클릭: 철거")));
            } else if (i < reapers + miners) {
                inv.setItem(i, MainHubGui.icon(Material.IRON_PICKAXE, "§b광물 채굴기 §fLv." + lv,
                        List.of("§720분 주기 — 마도철·은 원석",
                                "§7다음 생산: " + productionTimer(t.oreProducedAt().get(i - reapers)),
                                "§8Shift+클릭: 철거")));
            } else if (i < used) {
                inv.setItem(i, MainHubGui.icon(Material.SMITHING_TABLE, "§6공방 가공기",
                        List.of("§7대기 슬롯 §f+3§7 제공", "§8Shift+클릭: 철거")));
            } else if (i < max) {
                inv.setItem(i, MainHubGui.icon(Material.LIME_STAINED_GLASS_PANE, "§a빈 시설 슬롯",
                        List.of("§7클릭하여 시설을 설치합니다",
                                "§8(약초 재배지 / 광물 채굴기 / 공방 가공기)")));
            } else {
                inv.setItem(i, MainHubGui.icon(Material.RED_STAINED_GLASS_PANE, "§c잠긴 슬롯",
                        List.of("§7작위 승급 시 해금됩니다")));
            }
        }

        inv.setItem(SLOT_INFO, MainHubGui.icon(Material.BOOK, "§e시설 현황",
                List.of("§7──────────────",
                        "§7작위: §f" + t.rank().displayName,
                        "§7시설 슬롯: §f" + used + " §7/ §f" + max,
                        "§a약초 재배지: §f" + reapers,
                        "§b광물 채굴기: §f" + miners,
                        "§6공방 가공기: §f" + machines + " §7(공방 대기 슬롯 §f" + (machines * 3) + "§7)",
                        "§7──────────────",
                        "§7생산 주기: §f20분 §8(오프라인 누적, 창고 자동 입금)",
                        "§8각 재배기/채굴기에 다음 생산 시각 표시",
                        "§7──────────────",
                        "§8빈 슬롯 클릭=설치 / 설치칸 Shift+클릭=철거")));

        inv.setItem(SLOT_BACK, MainHubGui.icon(Material.ARROW, "§7뒤로", List.of("§7영지 설정")));
        player.openInventory(inv);
    }

    /** 빈 슬롯 클릭 시 뜨는 시설 선택 화면 (27슬롯). */
    public static void openSelect(Player player, IslandTerritoryState t) {
        Inventory inv = Bukkit.createInventory(null, 27, GuiTitles.TERRITORY_FACILITY_SELECT);
        ItemStack gray = MainHubGui.icon(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < 27; i++) inv.setItem(i, gray);

        inv.setItem(SEL_HERB, MainHubGui.icon(Material.WHEAT, "§a약초 재배지",
                List.of("§720분 주기 — 제국 약초·정수", "", "§a클릭하여 설치")));
        inv.setItem(SEL_ORE, MainHubGui.icon(Material.IRON_PICKAXE, "§b광물 채굴기",
                List.of("§720분 주기 — 마도철·은 원석", "", "§a클릭하여 설치")));
        inv.setItem(SEL_WORKSHOP, MainHubGui.icon(Material.SMITHING_TABLE, "§6공방 가공기",
                List.of("§7공방 대기 슬롯 §f+3§7 제공", "", "§a클릭하여 설치")));

        int used = t.facilitySlotsUsed();
        int max  = t.facilitySlotsMax();
        inv.setItem(4, MainHubGui.icon(Material.BOOK, "§e남은 시설 슬롯: §f" + (max - used) + " §7/ §f" + max,
                List.of("§7설치할 시설을 선택하세요")));
        inv.setItem(SLOT_BACK_SELECT, MainHubGui.icon(Material.ARROW, "§7뒤로", List.of("§7시설 현황")));
        player.openInventory(inv);
    }

    public static final int SEL_HERB = 11;
    public static final int SEL_ORE = 13;
    public static final int SEL_WORKSHOP = 15;
    public static final int SLOT_BACK_SELECT = 22;

    /** 선택 화면 슬롯 → 시설 종류. 해당 없으면 null. */
    public static FacilityType selectedType(int slot) {
        return switch (slot) {
            case SEL_HERB -> FacilityType.HERB;
            case SEL_ORE -> FacilityType.ORE;
            case SEL_WORKSHOP -> FacilityType.WORKSHOP;
            default -> null;
        };
    }

    /** 시설현황 슬롯 인덱스(0~17)에 설치된 시설 종류 반환 — 철거용. 빈칸/잠금이면 null. */
    public static FacilityType installedTypeAt(IslandTerritoryState t, int slot) {
        if (slot < 0 || slot >= FACILITY_ROWS) return null;
        int reapers = t.reaperCount();
        int miners = t.minerCount();
        int used = t.facilitySlotsUsed();
        if (slot < reapers) return FacilityType.HERB;
        if (slot < reapers + miners) return FacilityType.ORE;
        if (slot < used) return FacilityType.WORKSHOP;
        return null;
    }

    private static int machineLevel(IslandTerritoryState t) {
        int tier = t.rank().tier;
        return tier < 3 ? 1 : tier < 5 ? 2 : 3;
    }

    /**
     * 시설 1대의 다음 생산까지 남은 시간 — 그 시설의 마지막 생산시각 + 20분 기준(DL-129 추가#11).
     * 스케줄러가 1분마다 점검하므로 "곧 적립"은 최대 1분만 표시.
     */
    private static String productionTimer(long lastProducedAt) {
        long next = lastProducedAt + kr.zenon.rpg.growth.island.MachineProductionScheduler.intervalMs();
        long remain = next - System.currentTimeMillis();
        if (remain <= 0L) return "§a곧 적립";
        long min = remain / 60000;
        long sec = (remain % 60000) / 1000;
        return "§f" + (min > 0 ? min + "분 " + sec + "초" : sec + "초");
    }
}
