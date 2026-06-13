package com.poro.rpg.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * 보스룸에서 영지/필드로 이동 시 "보스 도전을 포기하시겠습니까?" 확인 GUI (DL-129 추가#20).
 * 예 → 파티 탈퇴 + 보스룸 퇴장 + 이동 / 아니요 → 계속 전투.
 */
public final class BossAbandonGui {
    private BossAbandonGui() {}

    public static final int SLOT_YES = 11;
    public static final int SLOT_NO  = 15;

    public static void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, GuiTitles.BOSS_ABANDON);
        ItemStack pane = MainHubGui.icon(Material.BLACK_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < 27; i++) inv.setItem(i, pane);

        inv.setItem(4, MainHubGui.icon(Material.WITHER_SKELETON_SKULL, "§c§l보스 도전을 포기하시겠습니까?",
                List.of("§7현재 보스룸에서 전투 중입니다.",
                        "§7이동하면 보스 도전이 종료됩니다.")));
        inv.setItem(SLOT_YES, MainHubGui.icon(Material.LIME_STAINED_GLASS, "§a§l예 — 포기하고 이동",
                List.of("§7──────────────",
                        "§7파티에서 나가고 보스룸을 떠납니다",
                        "§c보상 없음",
                        "§7──────────────",
                        "§a클릭")));
        inv.setItem(SLOT_NO, MainHubGui.icon(Material.RED_STAINED_GLASS, "§c아니요 — 계속 전투",
                List.of("§7──────────────",
                        "§7보스룸에 남아 전투를 계속합니다",
                        "§7──────────────",
                        "§c클릭")));
        player.openInventory(inv);
    }
}
