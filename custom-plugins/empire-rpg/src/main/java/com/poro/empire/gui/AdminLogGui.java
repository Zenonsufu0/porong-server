package com.poro.empire.gui;

import com.poro.empire.growth.engine.EnhancementService.EnhancementResult;
import com.poro.empire.growth.engine.InMemoryEnhancementLogHook;
import com.poro.empire.market.AuctionListing;
import com.poro.empire.market.AuctionStore;
import com.poro.empire.pvp.db.PvpMatchLogRepository;
import com.poro.empire.pvp.db.PvpMatchLogRepository.PvpMatchLogRow;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * 관리자 로그/감시 GUI (54슬롯, 읽기 전용). 3탭: 강화 / 거래 / PvP.
 * <pre>
 * row0~4 (0~44): 현재 탭의 로그 항목 (최신순, 페이지당 45건)
 * row5  (45~53): [강화][거래][PvP] [뒤로] [이전][페이지][다음] _ [닫기]
 * </pre>
 * 데이터 소스 영속성 주의:
 * - 강화 로그는 in-memory (재시작 시 소실), 거래·PvP 로그는 DB 영속.
 */
public final class AdminLogGui {
    private AdminLogGui() {}

    public enum LogTab { ENHANCE, TRADE, PVP }

    /** 소스에서 가져올 최대 건수 (페이지네이션 상한). */
    public static final int MAX_ENTRIES = 100;
    public static final int PAGE_SIZE   = 45;

    public static final int SLOT_TAB_ENHANCE = 45;
    public static final int SLOT_TAB_TRADE   = 46;
    public static final int SLOT_TAB_PVP     = 47;
    public static final int SLOT_BACK        = 48;
    public static final int SLOT_PREV        = 49;
    public static final int SLOT_PAGE        = 50;
    public static final int SLOT_NEXT        = 51;
    public static final int SLOT_CLOSE       = 53;

    /**
     * 로그 GUI를 연다.
     * @return 해당 탭의 총 페이지 수 (리스너가 page 클램프에 사용)
     */
    public static int open(Player admin, LogTab tab, int page,
                           InMemoryEnhancementLogHook enhanceLog,
                           AuctionStore auctionStore,
                           PvpMatchLogRepository pvpLog) {
        List<ItemStack> entries = switch (tab) {
            case ENHANCE -> enhanceIcons(enhanceLog);
            case TRADE   -> tradeIcons(auctionStore);
            case PVP     -> pvpIcons(pvpLog);
        };

        int totalPages = Math.max(1, (entries.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        int safePage   = Math.max(0, Math.min(page, totalPages - 1));

        Inventory inv = Bukkit.createInventory(null, 54, GuiTitles.ADMIN_LOGS);
        ItemStack gray = MainHubGui.icon(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < 54; i++) inv.setItem(i, gray);

        int from = safePage * PAGE_SIZE;
        int to   = Math.min(from + PAGE_SIZE, entries.size());
        if (entries.isEmpty()) {
            inv.setItem(22, MainHubGui.icon(Material.PAPER, "§7기록 없음",
                    List.of("§8" + tabName(tab) + " 로그가 비어 있습니다")));
        } else {
            for (int i = from; i < to; i++) inv.setItem(i - from, entries.get(i));
        }

        inv.setItem(SLOT_TAB_ENHANCE, tabButton(Material.ANVIL,        "강화 로그",  tab == LogTab.ENHANCE));
        inv.setItem(SLOT_TAB_TRADE,   tabButton(Material.GOLD_INGOT,   "거래 로그",  tab == LogTab.TRADE));
        inv.setItem(SLOT_TAB_PVP,     tabButton(Material.IRON_SWORD,   "PvP 로그",   tab == LogTab.PVP));
        inv.setItem(SLOT_BACK,  MainHubGui.icon(Material.ARROW,   "§7뒤로", List.of("§7관리자 허브")));
        inv.setItem(SLOT_PREV,  safePage > 0
                ? MainHubGui.icon(Material.SPECTRAL_ARROW, "§e◀ 이전 페이지", List.of())
                : gray);
        inv.setItem(SLOT_PAGE,  MainHubGui.icon(Material.MAP,
                "§f페이지 " + (safePage + 1) + " §7/ " + totalPages,
                List.of("§8총 " + entries.size() + "건")));
        inv.setItem(SLOT_NEXT,  safePage < totalPages - 1
                ? MainHubGui.icon(Material.SPECTRAL_ARROW, "§e다음 페이지 ▶", List.of())
                : gray);
        inv.setItem(SLOT_CLOSE, MainHubGui.icon(Material.BARRIER, "§c닫기", List.of()));

        admin.openInventory(inv);
        return totalPages;
    }

    // ─── 탭별 아이콘 빌더 (최신순) ──────────────────────────────────────

    private static List<ItemStack> enhanceIcons(InMemoryEnhancementLogHook log) {
        List<EnhancementResult> all = new ArrayList<>(log.logs());
        Collections.reverse(all); // 최신순
        List<ItemStack> icons = new ArrayList<>();
        for (int i = 0; i < all.size() && i < MAX_ENTRIES; i++) {
            EnhancementResult r = all.get(i);
            String result = r.forcedByCeiling() ? "§6천장 보정 성공"
                    : (r.success() ? "§a성공" : "§c실패");
            Material mat = r.success() ? (r.forcedByCeiling() ? Material.GOLD_INGOT : Material.LIME_DYE)
                    : Material.GRAY_DYE;
            icons.add(MainHubGui.icon(mat,
                    "§f" + nameOf(r.userId()) + " §7- §e" + r.itemId(),
                    List.of("§7──────────────",
                            "§7결과: " + result,
                            "§7강화: §f+" + r.beforeLevel() + " §8→ §f+" + r.finalLevel()
                                    + " §8(목표 +" + r.targetLevel() + ")",
                            "§7등급: §f" + r.tier() + " §7| 성공률: §f" + String.format("%.1f%%", r.successRate()),
                            "§7천장: §f" + r.ceilingCount() + "§7/§f" + r.ceilingCap(),
                            "§7비용: §6" + r.goldCost() + "G §7+ §b강화석 " + r.stoneCost())));
        }
        return icons;
    }

    private static List<ItemStack> tradeIcons(AuctionStore store) {
        List<ItemStack> icons = new ArrayList<>();
        for (AuctionListing l : store.recentSold(MAX_ENTRIES)) {
            icons.add(MainHubGui.icon(Material.GOLD_INGOT,
                    "§e" + l.itemId() + " §7×" + l.quantity(),
                    List.of("§7──────────────",
                            "§7판매자: §f" + l.sellerName(),
                            "§7가격: §6" + l.price() + "G",
                            "§7판매: §f" + ago(l.soldAt() == null ? 0L : l.soldAt()))));
        }
        return icons;
    }

    private static List<ItemStack> pvpIcons(PvpMatchLogRepository pvpLog) {
        List<ItemStack> icons = new ArrayList<>();
        for (PvpMatchLogRow r : pvpLog.recentMatches(MAX_ENTRIES)) {
            String headline = r.draw()
                    ? "§7무승부"
                    : "§a" + nameOf(r.winnerUuid()) + " §7▶ §c" + nameOf(r.loserUuid());
            icons.add(MainHubGui.icon(r.draw() ? Material.IRON_SWORD : Material.DIAMOND_SWORD,
                    "§f" + r.matchType() + " §7- " + headline,
                    List.of("§7──────────────",
                            "§7지속: §e" + r.durationS() + "초",
                            "§7사유: §f" + (r.reason() == null ? "-" : r.reason()),
                            "§7시각: §f" + ago(r.playedAt()))));
        }
        return icons;
    }

    // ─── 헬퍼 ──────────────────────────────────────────────────────────

    private static ItemStack tabButton(Material mat, String label, boolean active) {
        return MainHubGui.icon(active ? Material.LIME_STAINED_GLASS_PANE : mat,
                (active ? "§a§l▶ " : "§7") + label,
                List.of(active ? "§8현재 보는 중" : "§7클릭하여 전환"));
    }

    private static String nameOf(String uuidStr) {
        if (uuidStr == null || uuidStr.isBlank()) return "?";
        try {
            String name = Bukkit.getOfflinePlayer(UUID.fromString(uuidStr)).getName();
            return name != null ? name : uuidStr.substring(0, 8);
        } catch (IllegalArgumentException e) {
            return uuidStr;
        }
    }

    private static String tabName(LogTab tab) {
        return switch (tab) {
            case ENHANCE -> "강화";
            case TRADE   -> "거래";
            case PVP     -> "PvP";
        };
    }

    /** epoch millis → "N초/분/시간/일 전" 상대 표기. */
    private static String ago(long ts) {
        if (ts <= 0) return "-";
        long diff = System.currentTimeMillis() - ts;
        if (diff < 0) return "방금";
        long sec = diff / 1000;
        if (sec < 60)   return sec + "초 전";
        long min = sec / 60;
        if (min < 60)   return min + "분 전";
        long hour = min / 60;
        if (hour < 24)  return hour + "시간 전";
        return (hour / 24) + "일 전";
    }
}
