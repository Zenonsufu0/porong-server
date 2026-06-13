package com.poro.rpg.gui;

import com.poro.rpg.pvp.PvpRatingService;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.List;

/**
 * 정규대전 랭킹 GUI (54슬롯). 상위 45명 표시 (한 페이지).
 */
public final class PvpRankingGui {
    private PvpRankingGui() {}

    public static final int SLOT_BACK = 49;

    public static void open(Player player, PvpRatingService ratingService) {
        Inventory inv = Bukkit.createInventory(null, 54, GuiTitles.PVP_RANKING);
        ItemStack gray = MainHubGui.icon(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < 54; i++) inv.setItem(i, gray);

        List<PvpRatingService.Rating> rankings = ratingService.rankings();
        if (rankings.isEmpty()) {
            inv.setItem(22, MainHubGui.icon(Material.PAPER, "§7기록 없음",
                    List.of("§7아직 정규대전 기록이 없습니다.")));
        } else {
            for (int i = 0; i < Math.min(rankings.size(), 45); i++) {
                inv.setItem(i, rankingIcon(i + 1, rankings.get(i)));
            }
        }

        inv.setItem(SLOT_BACK, MainHubGui.icon(Material.ARROW, "§7뒤로", List.of("§7PvP 허브")));
        player.openInventory(inv);
    }

    private static ItemStack rankingIcon(int rank, PvpRatingService.Rating r) {
        OfflinePlayer op = Bukkit.getOfflinePlayer(r.uuid());
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        meta.setOwningPlayer(op);
        String medal = switch (rank) {
            case 1 -> "§6§l#1 ";
            case 2 -> "§f§l#2 ";
            case 3 -> "§e§l#3 ";
            default -> "§7#" + rank + " ";
        };
        meta.displayName(Component.text(medal + "§f" + r.name()));
        meta.lore(List.of(
                Component.text("§7──────────────"),
                Component.text("§7점수: §e" + r.score()),
                Component.text("§a승: §f" + r.wins() + " §c패: §f" + r.losses()),
                Component.text("§7승률: §f" + winRatePct(r))
        ));
        head.setItemMeta(meta);
        return head;
    }

    private static String winRatePct(PvpRatingService.Rating r) {
        int total = r.wins() + r.losses();
        if (total <= 0) return "—";
        return String.format("%.1f%%", 100.0 * r.wins() / total);
    }
}
