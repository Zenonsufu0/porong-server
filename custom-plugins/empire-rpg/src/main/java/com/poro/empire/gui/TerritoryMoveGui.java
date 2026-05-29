package com.poro.empire.gui;

import com.poro.empire.growth.island.IslandTerritoryState;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import net.kyori.adventure.text.Component;

import java.util.List;

/**
 * 영지 이동 GUI (27슬롯, 3×9).
 *
 * <pre>
 * row0: [내영지] ░ ░ ░ ░ ░ ░ ░ ░
 * row1: [공개1~9]                                  (현재 데이터 없음 — gray)
 * row2: [뒤로]   ░ ░ [◀] [페이지] [▶] ░ ░ ░
 * </pre>
 *
 * <p>공개 영지 목록은 island_settings 데이터 수집 후 활성화 예정.
 */
public final class TerritoryMoveGui {
    private TerritoryMoveGui() {}

    public static final int SLOT_MY_ISLAND = 0;
    public static final int SLOT_PUBLIC_START = 9;
    public static final int SLOT_PUBLIC_END   = 17;
    public static final int SLOT_BACK         = 18;
    public static final int SLOT_PREV_PAGE    = 21;
    public static final int SLOT_PAGE_INFO    = 22;
    public static final int SLOT_NEXT_PAGE    = 23;

    public static void open(Player player, IslandTerritoryState territory) {
        Inventory inv = Bukkit.createInventory(null, 27, GuiTitles.TERRITORY_MOVE);
        ItemStack gray = MainHubGui.icon(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < 27; i++) inv.setItem(i, gray);

        // 내 영지 슬롯
        inv.setItem(SLOT_MY_ISLAND, myIslandIcon(player, territory));

        // 공개 영지 목록 (현재 데이터 없음 — 안내 메시지만)
        inv.setItem(13, MainHubGui.icon(Material.GRAY_STAINED_GLASS_PANE,
                "§8공개된 영지가 없습니다.",
                List.of("§7──────────────",
                        "§7다른 플레이어가 영지 설정에서",
                        "§7방문 공개를 활성화하면 표시됩니다.")));

        // 페이지 네비게이션 (1페이지 고정)
        inv.setItem(SLOT_PREV_PAGE, MainHubGui.icon(Material.GRAY_DYE, "§8◀ 이전 페이지",
                List.of("§8첫 페이지입니다.")));
        inv.setItem(SLOT_PAGE_INFO, MainHubGui.icon(Material.PAPER, "§f1 / 1", List.of()));
        inv.setItem(SLOT_NEXT_PAGE, MainHubGui.icon(Material.GRAY_DYE, "§8다음 페이지 ▶",
                List.of("§8마지막 페이지입니다.")));

        inv.setItem(SLOT_BACK, MainHubGui.icon(Material.ARROW, "§7뒤로", List.of("§7영지 관리")));

        player.openInventory(inv);
    }

    private static ItemStack myIslandIcon(Player player, IslandTerritoryState territory) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        meta.setOwningPlayer(player);
        meta.displayName(Component.text("§6[내 영지]"));
        meta.lore(List.of(
                Component.text("§7──────────────"),
                Component.text("§7§e" + territory.islandName()),
                Component.text("§7작위: §f" + territory.rank().displayName),
                Component.text("§7──────────────"),
                Component.text("§7클릭 → 내 영지 스폰으로 이동")
        ));
        head.setItemMeta(meta);
        return head;
    }
}
