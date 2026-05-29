package com.poro.empire.gui;

import com.poro.empire.pvp.PvpRatingService;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * PvP 허브 GUI (54슬롯, docs/13_pvp_system/CANON.md).
 *
 * <pre>
 *      col0  col1  col2  col3  col4  col5  col6  col7  col8
 * row0: ░  ░  ░  ░ [내정보] ░  ░  ░  ░
 * row1: ░  ░  ░  ░  ░  ░  ░  ░  ░
 * row2: ░ [자유대전] ░ [정규대전] ░ [친선대전] ░ [랭킹] ░
 * row3-4: ░ ...
 * row5: [뒤로] ░ ░ ░ ░ ░ ░ ░ ░
 * </pre>
 */
public final class PvpHubGui {
    private PvpHubGui() {}

    public static final int SLOT_MY_RATING = 4;
    public static final int SLOT_FREE      = 19;
    public static final int SLOT_RANKED    = 21;
    public static final int SLOT_FRIENDLY  = 23;
    public static final int SLOT_RANKING   = 25;
    public static final int SLOT_BACK      = 45;

    public static void open(Player player, PvpRatingService ratingService) {
        Inventory inv = Bukkit.createInventory(null, 54, GuiTitles.PVP_HUB);
        ItemStack gray = MainHubGui.icon(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < 54; i++) inv.setItem(i, gray);

        PvpRatingService.Rating myRating = ratingService.getOrInit(player.getUniqueId(), player.getName());
        inv.setItem(SLOT_MY_RATING, MainHubGui.icon(Material.NETHER_STAR, "§e내 정보",
                List.of("§7──────────────",
                        "§7정규대전 점수: §f" + myRating.score(),
                        "§a승: §f" + myRating.wins() + " §c패: §f" + myRating.losses(),
                        "§7──────────────",
                        "§8시즌 종료 시 초기화")));

        inv.setItem(SLOT_FREE, MainHubGui.icon(Material.IRON_SWORD, "§f자유대전",
                List.of("§7──────────────",
                        "§7현재 장비 그대로 / 보상·점수 없음",
                        "§8준비 중 (Phase 2)")));
        inv.setItem(SLOT_RANKED, MainHubGui.icon(Material.GOLDEN_SWORD, "§6정규대전",
                List.of("§7──────────────",
                        "§7IL 60 동일화 / 점수 변동",
                        "§7승 +15  /  패 -10",
                        "§8준비 중 (Phase 2)")));
        inv.setItem(SLOT_FRIENDLY, MainHubGui.icon(Material.DIAMOND_SWORD, "§b친선대전",
                List.of("§7──────────────",
                        "§7닉네임 지정 / 보상·점수 없음",
                        "§7상대 수락 필요",
                        "§8준비 중 (Phase 2)")));
        inv.setItem(SLOT_RANKING, MainHubGui.icon(Material.NETHER_STAR, "§e정규대전 랭킹",
                List.of("§7──────────────", "§7전체 플레이어 점수 순위", "§a▶ 클릭하여 열기")));

        inv.setItem(SLOT_BACK, MainHubGui.icon(Material.ARROW, "§7뒤로", List.of("§7메인 메뉴")));
        player.openInventory(inv);
    }
}
