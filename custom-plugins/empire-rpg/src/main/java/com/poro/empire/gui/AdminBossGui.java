package com.poro.empire.gui;

import com.poro.empire.boss.engine.BossRun;
import com.poro.empire.boss.engine.BossRunService;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 관리자 보스 디버그 GUI (54슬롯). 진행 중 보스 런 목록 + 강제 종료.
 * <pre>
 * row0~4 (0~44): 진행 중 보스 런 (좌클릭 = 강제 종료 → 슬롯/참가자 해제)
 * row5:   [뒤로]     ...     [닫기]
 * </pre>
 * 강제 종료는 {@code endRun(runId, false, "admin_force")} → onRunEnded → releaseByRunId 체인으로
 * 보스룸 슬롯을 해제한다. (MM 보스 엔티티 잔존 가능 — 슬롯 stuck 해소가 1차 목적)
 */
public final class AdminBossGui {
    private AdminBossGui() {}

    public static final int SLOT_BACK     = 45;
    public static final int SLOT_CLOSE    = 53;
    public static final int ITEM_AREA_END = 44;

    /** @return 슬롯 인덱스 → runId 매핑 (리스너가 클릭 → 강제 종료에 사용) */
    public static List<String> open(Player admin, BossRunService runService) {
        Inventory inv = Bukkit.createInventory(null, 54, GuiTitles.ADMIN_BOSS);
        ItemStack gray = MainHubGui.icon(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < 54; i++) inv.setItem(i, gray);

        List<String> orderedRunIds = new ArrayList<>();
        List<BossRun> runs = new ArrayList<>(runService.activeRuns().values());
        if (runs.isEmpty()) {
            inv.setItem(22, MainHubGui.icon(Material.PAPER, "§7진행 중인 보스 런 없음",
                    List.of("§8현재 활성 보스 인스턴스가 없습니다")));
        } else {
            for (int i = 0; i < runs.size() && i <= ITEM_AREA_END; i++) {
                BossRun r = runs.get(i);
                orderedRunIds.add(r.runId());
                inv.setItem(i, runIcon(r));
            }
        }

        inv.setItem(SLOT_BACK,  MainHubGui.icon(Material.ARROW,   "§7뒤로", List.of("§7관리자 허브")));
        inv.setItem(SLOT_CLOSE, MainHubGui.icon(Material.BARRIER, "§c닫기", List.of()));
        admin.openInventory(inv);
        return orderedRunIds;
    }

    private static ItemStack runIcon(BossRun r) {
        long elapsed = r.enteredAt() != null
                ? Duration.between(r.enteredAt(), Instant.now()).toSeconds() : 0;
        return MainHubGui.icon(Material.WITHER_SKELETON_SKULL,
                "§c" + r.bossId() + " §7- §f" + nameOf(r.leaderUserId()),
                List.of("§7──────────────",
                        "§7런 ID: §8" + shortId(r.runId()),
                        "§7파티: §f" + r.partySize() + "명 §7| 페이즈: §e" + r.currentPhase() + "§7/" + r.phaseReached(),
                        "§7보스 HP: §c" + String.format("%.0f%%", r.bossHpPercent()),
                        "§7경과: §e" + elapsed + "초",
                        "§7──────────────",
                        "§c⚠ 좌클릭 = 강제 종료 (슬롯 해제)"));
    }

    private static String shortId(String runId) {
        if (runId == null) return "?";
        return runId.length() > 8 ? runId.substring(0, 8) : runId;
    }

    private static String nameOf(String uuidStr) {
        if (uuidStr == null || uuidStr.isBlank()) return "?";
        try {
            String name = Bukkit.getOfflinePlayer(UUID.fromString(uuidStr)).getName();
            return name != null ? name : shortId(uuidStr);
        } catch (IllegalArgumentException e) {
            return uuidStr;
        }
    }
}
