package com.poro.empire.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * 관리자 메인 허브 GUI (54슬롯). Phase 1만 활성, Phase 2는 stub.
 */
public final class AdminHubGui {
    private AdminHubGui() {}

    // Phase 1 활성
    public static final int SLOT_INSPECT  = 11;
    public static final int SLOT_MATCHES  = 13;
    public static final int SLOT_STATS    = 15;
    public static final int SLOT_RELEASE  = 22;
    // Phase 2 stub
    public static final int SLOT_TERRITORY = 29;
    public static final int SLOT_TOGGLES   = 31;
    public static final int SLOT_LOGS      = 33;

    public static final int SLOT_CLOSE   = 49;

    public static void open(Player admin) {
        Inventory inv = Bukkit.createInventory(null, 54, GuiTitles.ADMIN_HUB);
        ItemStack black = MainHubGui.icon(Material.BLACK_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < 54; i++) inv.setItem(i, black);

        // Phase 1
        inv.setItem(SLOT_INSPECT, MainHubGui.icon(Material.PLAYER_HEAD, "§e플레이어 인스펙트",
                List.of("§7닉네임 입력 → 직업·강화·재화·영지·PvP 상세",
                        "§a▶ 클릭하여 열기")));
        inv.setItem(SLOT_MATCHES, MainHubGui.icon(Material.IRON_SWORD, "§c진행 중 매치",
                List.of("§7PvP 매치 / 보스 런 강제 종료",
                        "§a▶ 클릭하여 열기")));
        inv.setItem(SLOT_STATS, MainHubGui.icon(Material.PAPER, "§b서버 통계",
                List.of("§7시즌 주차 / 클리어 총수 / 아레나·보스룸 점유",
                        "§a▶ 클릭하여 열기")));
        inv.setItem(SLOT_RELEASE, MainHubGui.icon(Material.REDSTONE_BLOCK, "§4슬롯 강제 해제",
                List.of("§7아레나/보스룸 모든 점유 슬롯 강제 해제",
                        "§c⚠ 진행 중 매치/보스 모두 종료됨",
                        "§e▶ Shift+클릭으로 확인")));

        // Phase 2
        inv.setItem(SLOT_TERRITORY, stub("§8영지 관리"));
        inv.setItem(SLOT_TOGGLES, MainHubGui.icon(Material.LEVER, "§e운영 토글",
                List.of("§7보스 스폰·강화/EXP/드랍 부스트·PvP 큐",
                        "§a▶ 클릭하여 열기")));
        inv.setItem(SLOT_LOGS, MainHubGui.icon(Material.WRITABLE_BOOK, "§e로그/감시",
                List.of("§7최근 강화·거래·PvP 로그 조회",
                        "§a▶ 클릭하여 열기")));

        inv.setItem(SLOT_CLOSE, MainHubGui.icon(Material.BARRIER, "§c닫기", List.of()));
        admin.openInventory(inv);
    }

    private static ItemStack stub(String name) {
        return MainHubGui.icon(Material.GRAY_STAINED_GLASS_PANE, name + " §8[Phase 2]",
                List.of("§8후속 작업"));
    }
}
