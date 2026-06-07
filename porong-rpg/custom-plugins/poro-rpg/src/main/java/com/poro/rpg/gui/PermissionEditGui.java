package com.poro.rpg.gui;

import com.poro.rpg.growth.island.IslandTerritoryState;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * 권한 편집 GUI (27슬롯, gui_territory_settings.md §12).
 *
 * <pre>
 * row0: ░ ░ ░ ░ ░ ░ ░ ░ ░
 * row1: [창고][공방][채굴][농사][물접근][초대][강퇴][블록][설정]
 * row2: [뒤로] ░ ░ ░ ░ ░ ░ ░ ░
 * </pre>
 */
public final class PermissionEditGui {
    private PermissionEditGui() {}

    public static final int SLOT_BACK = 18;

    /** GUI 슬롯 → Permission 매핑 (row1: 9~17). */
    public static final IslandTerritoryState.Permission[] SLOT_TO_PERM = {
            null,null,null,null,null,null,null,null,null,                // row0
            IslandTerritoryState.Permission.STORAGE_DEPOSIT,  // 9
            IslandTerritoryState.Permission.WORKSHOP_USE,     // 10
            IslandTerritoryState.Permission.MINE,             // 11
            IslandTerritoryState.Permission.FARM,             // 12
            IslandTerritoryState.Permission.WATER_MODIFY,     // 13
            IslandTerritoryState.Permission.MEMBER_INVITE,    // 14
            IslandTerritoryState.Permission.MEMBER_KICK,      // 15
            IslandTerritoryState.Permission.BLOCK_MODIFY,     // 16
            IslandTerritoryState.Permission.SETTINGS_EDIT,    // 17
            null,null,null,null,null,null,null,null,null                 // row2
    };

    private static final String[] PERM_NAMES = {
            "창고 입출금", "공방 가공기", "채굴", "농사",
            "물 제거·배치", "멤버 초대", "멤버 강퇴", "블록 파괴·배치", "영지 설정 변경"
    };

    public static void open(Player player, IslandTerritoryState territory, IslandTerritoryState.Role role) {
        Inventory inv = Bukkit.createInventory(null, 27, GuiTitles.PERMISSION_EDIT);
        ItemStack gray = MainHubGui.icon(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < 27; i++) inv.setItem(i, gray);

        String roleName = switch (role) {
            case VICE_LORD -> "§b부영주";
            case RESIDENT  -> "§e영지민";
            case VISITOR   -> "§7방문자";
        };
        for (int s = 9; s <= 17; s++) {
            IslandTerritoryState.Permission perm = SLOT_TO_PERM[s];
            inv.setItem(s, permIcon(territory, role, perm, PERM_NAMES[s - 9]));
        }
        inv.setItem(SLOT_BACK, MainHubGui.icon(Material.ARROW,
                "§7뒤로 §8(" + roleName + "§8)", List.of("§7등급 선택")));

        player.openInventory(inv);
    }

    public static ItemStack permIcon(IslandTerritoryState territory, IslandTerritoryState.Role role,
                                     IslandTerritoryState.Permission perm, String name) {
        boolean on = territory.hasPermission(role, perm);
        return on
                ? MainHubGui.icon(Material.LIME_DYE, "§a" + name + ": §2[허용]",
                        List.of("§7클릭 → §c비허용"))
                : MainHubGui.icon(Material.GRAY_DYE, "§7" + name + ": §8[비허용]",
                        List.of("§7클릭 → §a허용"));
    }
}
