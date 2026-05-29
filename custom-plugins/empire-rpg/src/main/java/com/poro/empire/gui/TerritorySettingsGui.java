package com.poro.empire.gui;

import com.poro.empire.growth.island.IslandTerritoryState;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * 영지 설정 GUI (36슬롯, 4×9).
 *
 * row0: 설정 버튼 9개 (영지명·방문·채굴·농사·날씨·시간·작물보호·물보호·권한)
 * row1: 자동입금(slot9) + gray×7 + 시설현황(slot17)
 * row2: 멤버 슬롯 8개 (slot18~25)
 * row3: 뒤로(slot27) + gray×8
 */
public final class TerritorySettingsGui {
    private TerritorySettingsGui() {}

    public static final int SLOT_NAME     = 0;
    public static final int SLOT_WEATHER  = 4;
    public static final int SLOT_TIME     = 5;
    public static final int SLOT_FACILITY = 17;
    public static final int SLOT_BACK     = 27;

    /** weatherState: 0=서버기본, 1=맑음, 2=비 */
    public static final int WEATHER_DEFAULT = 0;
    public static final int WEATHER_CLEAR   = 1;
    public static final int WEATHER_RAIN    = 2;

    /** timeState: 0=서버기본, 1=낮, 2=밤 */
    public static final int TIME_DEFAULT = 0;
    public static final int TIME_DAY     = 1;
    public static final int TIME_NIGHT   = 2;

    public static void open(Player player, IslandTerritoryState territory) {
        open(player, territory, WEATHER_DEFAULT, TIME_DEFAULT);
    }

    public static void open(Player player, IslandTerritoryState territory,
                            int weatherState, int timeState) {
        Inventory inv = Bukkit.createInventory(null, 36, GuiTitles.TERRITORY_SETTINGS);
        ItemStack gray = MainHubGui.icon(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < 36; i++) inv.setItem(i, gray);

        // row0 — 설정 버튼
        inv.setItem(SLOT_NAME, MainHubGui.icon(Material.NAME_TAG, "§f영지명 변경",
                List.of("§7현재: §e" + territory.islandName(), "§8▶ 클릭하여 변경")));
        inv.setItem(1, stub(Material.LIME_STAINED_GLASS_PANE, "§f방문 설정"));
        inv.setItem(2, stub(Material.IRON_PICKAXE,            "§f방문자 채굴 허용"));
        inv.setItem(3, stub(Material.WHEAT,                    "§f방문자 농사 허용"));
        inv.setItem(SLOT_WEATHER, weatherIcon(weatherState));
        inv.setItem(SLOT_TIME,    timeIcon(timeState));
        inv.setItem(6, stub(Material.WHEAT,                    "§f농작물 보호"));
        inv.setItem(7, stub(Material.WATER_BUCKET,             "§f물 파괴 보호"));
        inv.setItem(8, stub(Material.BOOK,                     "§f권한 설정"));

        // row1 — 자동입금 상태 표시 + 시설현황
        inv.setItem(9,  autoDepositIcon(territory));
        inv.setItem(17, MainHubGui.icon(Material.PISTON, "§f시설 현황",
                List.of("§7약초 재배기 현황 확인", "§8▶ 클릭하여 열기")));

        // row2 — 멤버 슬롯
        for (int i = 18; i <= 25; i++) {
            inv.setItem(i, MainHubGui.icon(Material.WHITE_STAINED_GLASS_PANE,
                    "§7플레이어 초대", List.of("§8준비 중")));
        }

        // row3 — 네비게이션
        inv.setItem(SLOT_BACK, MainHubGui.icon(Material.ARROW, "§7뒤로", List.of("§7영지 관리")));

        player.openInventory(inv);
    }

    public static ItemStack weatherIcon(int state) {
        return switch (state) {
            case WEATHER_CLEAR -> MainHubGui.icon(Material.SUNFLOWER, "§a날씨 설정 §f[맑음 고정]",
                    List.of("§7클릭 → §b비 고정"));
            case WEATHER_RAIN  -> MainHubGui.icon(Material.WATER_BUCKET, "§b날씨 설정 §f[비 고정]",
                    List.of("§7클릭 → §7서버 기본"));
            default            -> MainHubGui.icon(Material.CLOCK, "§7날씨 설정 §f[서버 기본]",
                    List.of("§7클릭 → §a맑음 고정"));
        };
    }

    public static ItemStack timeIcon(int state) {
        return switch (state) {
            case TIME_DAY   -> MainHubGui.icon(Material.SUNFLOWER, "§e시간 설정 §f[낮 고정]",
                    List.of("§7클릭 → §9밤 고정"));
            case TIME_NIGHT -> MainHubGui.icon(Material.INK_SAC,   "§9시간 설정 §f[밤 고정]",
                    List.of("§7클릭 → §7서버 기본"));
            default         -> MainHubGui.icon(Material.CLOCK,     "§7시간 설정 §f[서버 기본]",
                    List.of("§7클릭 → §e낮 고정"));
        };
    }

    private static ItemStack autoDepositIcon(IslandTerritoryState t) {
        boolean on = t.hasConvenience(IslandTerritoryState.CONV_AUTO_DEPOSIT);
        return on
                ? MainHubGui.icon(Material.HOPPER, "§a자동 입금 §2[활성화]",
                        List.of("§7채굴·수확 산출물이 창고에 자동 적재", "§8영지 상태 GUI에서 설정"))
                : MainHubGui.icon(Material.HOPPER, "§7자동 입금 §8[비활성화]",
                        List.of("§7채굴·수확 산출물이 인벤토리로 이동", "§8영지 상태 GUI에서 설정"));
    }

    private static ItemStack stub(Material mat, String name) {
        return MainHubGui.icon(mat, name, List.of("§8준비 중"));
    }
}
