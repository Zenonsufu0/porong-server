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

    public static final int SLOT_FACILITY = 17;
    public static final int SLOT_BACK     = 27;

    public static void open(Player player, IslandTerritoryState territory) {
        Inventory inv = Bukkit.createInventory(null, 36, GuiTitles.TERRITORY_SETTINGS);
        ItemStack gray = MainHubGui.icon(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < 36; i++) inv.setItem(i, gray);

        // row0 — 설정 버튼
        inv.setItem(0, MainHubGui.icon(Material.NAME_TAG, "§f영지명 변경",
                List.of("§7현재: §e" + territory.islandName(), "§8준비 중")));
        inv.setItem(1, stub(Material.LIME_STAINED_GLASS_PANE, "§f방문 설정"));
        inv.setItem(2, stub(Material.IRON_PICKAXE,            "§f방문자 채굴 허용"));
        inv.setItem(3, stub(Material.WHEAT,                    "§f방문자 농사 허용"));
        inv.setItem(4, stub(Material.CLOCK,                    "§f날씨 설정"));
        inv.setItem(5, stub(Material.CLOCK,                    "§f시간 설정"));
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
