package com.poro.empire.gui;

import com.poro.empire.boss.room.BossRoomManager;
import com.poro.empire.pvp.PvpArenaManager;
import com.poro.empire.pvp.PvpMatchService;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * 서버 통계 GUI (27슬롯). 시즌·아레나·보스룸·온라인 요약.
 */
public final class AdminStatsGui {
    private AdminStatsGui() {}

    public static final int SLOT_BACK  = 18;
    public static final int SLOT_CLOSE = 26;

    public static void open(Player admin,
                            long seasonStartEpochSeconds,
                            PvpArenaManager arenaManager,
                            BossRoomManager bossRoomManager,
                            PvpMatchService matchService) {
        Inventory inv = Bukkit.createInventory(null, 27, GuiTitles.ADMIN_STATS);
        ItemStack gray = MainHubGui.icon(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < 27; i++) inv.setItem(i, gray);

        long now = System.currentTimeMillis() / 1000;
        long days = seasonStartEpochSeconds > 0 ? Math.max(0, (now - seasonStartEpochSeconds) / 86400) : 0;
        int week = seasonStartEpochSeconds > 0 ? (int) ((now - seasonStartEpochSeconds) / (7L * 86400)) + 1 : 1;

        inv.setItem(10, MainHubGui.icon(Material.CLOCK, "§e시즌 진행",
                List.of("§7주차: §f" + week,
                        "§7경과 일수: §f" + days + "일",
                        "§7총 45일")));

        inv.setItem(11, MainHubGui.icon(Material.IRON_SWORD, "§cPvP 아레나",
                List.of("§7점유: §c" + (arenaManager.totalCount() - arenaManager.freeCount())
                        + " §7/ §f" + arenaManager.totalCount(),
                        "§7빈 슬롯: §a" + arenaManager.freeCount(),
                        "§7활성 매치: §c" + matchService.activeMatches().size())));

        inv.setItem(12, MainHubGui.icon(Material.WITHER_SKELETON_SKULL, "§c보스룸",
                List.of("§7점유: §c" + (bossRoomManager.totalCount() - bossRoomManager.freeCount())
                        + " §7/ §f" + bossRoomManager.totalCount(),
                        "§7빈 슬롯: §a" + bossRoomManager.freeCount())));

        inv.setItem(13, MainHubGui.icon(Material.PLAYER_HEAD, "§b온라인 플레이어",
                List.of("§7현재: §f" + Bukkit.getOnlinePlayers().size() + "명",
                        "§7최대: §f" + Bukkit.getMaxPlayers() + "명")));

        inv.setItem(SLOT_BACK,  MainHubGui.icon(Material.ARROW,   "§7뒤로", List.of("§7관리자 허브")));
        inv.setItem(SLOT_CLOSE, MainHubGui.icon(Material.BARRIER, "§c닫기", List.of()));
        admin.openInventory(inv);
    }
}
