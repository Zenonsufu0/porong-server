package com.poro.empire.gui;

import com.poro.empire.pvp.PvpMatch;
import com.poro.empire.pvp.PvpMatchService;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 진행 중 매치 GUI (54슬롯). PvP만 노출 (보스 런은 Phase 2에서 추가).
 * <pre>
 * row0~4: 진행 중 PvP 매치 (좌클릭 = 강제 종료, 무승부 처리)
 * row5:   [뒤로]     ...     [닫기]
 * </pre>
 */
public final class AdminMatchesGui {
    private AdminMatchesGui() {}

    public static final int SLOT_BACK  = 45;
    public static final int SLOT_CLOSE = 53;
    public static final int ITEM_AREA_END = 44;

    public static List<UUID> open(Player admin, PvpMatchService matchService) {
        Inventory inv = Bukkit.createInventory(null, 54, GuiTitles.ADMIN_MATCHES);
        ItemStack gray = MainHubGui.icon(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < 54; i++) inv.setItem(i, gray);

        List<UUID> orderedMatchIds = new ArrayList<>();
        var matches = new ArrayList<>(matchService.activeMatches());
        if (matches.isEmpty()) {
            inv.setItem(22, MainHubGui.icon(Material.PAPER, "§7진행 중인 매치 없음",
                    List.of("§8대기열만 있거나 모두 종료됨")));
        } else {
            for (int i = 0; i < matches.size() && i <= ITEM_AREA_END; i++) {
                PvpMatch m = matches.get(i);
                orderedMatchIds.add(m.matchId());
                inv.setItem(i, matchIcon(m));
            }
        }

        inv.setItem(SLOT_BACK,  MainHubGui.icon(Material.ARROW,   "§7뒤로", List.of("§7관리자 허브")));
        inv.setItem(SLOT_CLOSE, MainHubGui.icon(Material.BARRIER, "§c닫기", List.of()));
        admin.openInventory(inv);
        return orderedMatchIds;
    }

    private static ItemStack matchIcon(PvpMatch m) {
        long now = System.currentTimeMillis();
        long elapsed = (now - m.startedAt()) / 1000;
        return MainHubGui.icon(Material.IRON_SWORD,
                "§c" + m.type().name() + " §7- §f" + m.nameA() + " §7vs §f" + m.nameB(),
                List.of("§7──────────────",
                        "§7경과: §e" + elapsed + "초",
                        "§7아레나 슬롯: §f" + m.arenaSlotId(),
                        "§7──────────────",
                        "§c⚠ 좌클릭 = 강제 종료 (무승부)"));
    }
}
