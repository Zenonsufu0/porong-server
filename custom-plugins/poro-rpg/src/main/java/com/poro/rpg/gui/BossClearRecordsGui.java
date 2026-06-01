package com.poro.rpg.gui;

import com.poro.rpg.boss.db.BossSessionRepository;
import com.poro.rpg.growth.island.IslandTerritoryState;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 클리어 기록 GUI (27슬롯, 3×9).
 *
 * <pre>
 * row0: ░ ░ ░ ░ ░ ░ ░ ░ ░  (테두리)
 * row1: [보스1][보스2][보스3][보스4][보스5][보스6][최종1][최종2][최종3]
 * row2: [뒤로] ░ ░ ░ ░ ░ ░ ░ [트로피]
 * </pre>
 *
 * <p>접근 제한: 없음 (전투 중 포함 항시 열람 가능).
 */
public final class BossClearRecordsGui {
    private BossClearRecordsGui() {}

    public static final int SLOT_BACK    = 18;
    public static final int SLOT_TROPHY  = 26;
    public static final int TROPHY_BOSS_SLOT_START = 9;

    private static final String TROPHY_ITEM_ID = "rift_king_heart";

    /** 표시 순서: 시즌1~6 + 최종3 (BossHubGui와 동일 ID 사용). */
    private static final List<BossRow> BOSS_ROWS = List.of(
            new BossRow("fallen_knight",   "타락 기사장",     Material.NETHERITE_SWORD,  false),
            new BossRow("corrupted_lord",  "오염된 군주",     Material.POISONOUS_POTATO, false),
            new BossRow("stone_colossus",  "석조 거상",       Material.STONE,            false),
            new BossRow("storm_sorcerer",  "폭풍 술사",       Material.LIGHTNING_ROD,    false),
            new BossRow("abyss_guardian",  "심연 수호자",     Material.CRYING_OBSIDIAN,  false),
            new BossRow("void_herald",     "공허 사자",       Material.CHORUS_FRUIT,     false),
            new BossRow("rift_king",       "균열왕",          Material.NETHER_STAR,      true),
            new BossRow("corrupted_dyad",  "타락한 이중체",   Material.WITHER_ROSE,      true),
            new BossRow("spirit_watcher",  "진혼의 주시자",   Material.SOUL_LANTERN,     true)
    );

    public record BossRow(String id, String displayName, Material icon, boolean isFinal) {}

    public static void open(Player player, BossSessionRepository repo, IslandTerritoryState territory) {
        Inventory inv = Bukkit.createInventory(null, 27, GuiTitles.BOSS_CLEAR_RECORDS);
        ItemStack gray = MainHubGui.icon(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < 27; i++) inv.setItem(i, gray);

        String uuid = player.getUniqueId().toString();
        for (int i = 0; i < BOSS_ROWS.size(); i++) {
            BossRow row = BOSS_ROWS.get(i);
            int clears      = repo.countClearsByPlayer(uuid, row.id());
            Long personalBest = repo.bestClearTimeByPlayer(uuid, row.id());
            Long serverBest   = repo.serverBestClearTime(row.id());
            Map<String, Object> stats = repo.queryStatsByBossId(row.id());

            inv.setItem(TROPHY_BOSS_SLOT_START + i, bossIcon(row, clears, personalBest, serverBest, stats));
        }

        inv.setItem(SLOT_BACK,   MainHubGui.icon(Material.ARROW, "§7뒤로", List.of("§7보스 허브")));
        inv.setItem(SLOT_TROPHY, trophyIcon(territory));

        player.openInventory(inv);
    }

    private static ItemStack bossIcon(BossRow row, int clears, Long personalBest, Long serverBest,
                                      Map<String, Object> stats) {
        if (clears <= 0) {
            return MainHubGui.icon(Material.GRAY_STAINED_GLASS_PANE,
                    "§7" + row.displayName(),
                    List.of("§7──────────────",
                            "§c미클리어",
                            "§7──────────────",
                            "§7보스 선택 탭에서 도전 가능"));
        }

        String tierColor = row.isFinal() ? "§6§l" : "§6";
        List<String> lore = new ArrayList<>();
        lore.add("§7──────────────");
        lore.add("§7개인 클리어: §f" + clears + "회");
        if (personalBest != null) {
            lore.add("§7개인 최단: §a" + formatTime(personalBest));
        }

        if (stats != null) {
            Object avg = stats.get("avg_clear_seconds");
            if (avg instanceof Number num && num.longValue() > 0) {
                lore.add("§7──────────────");
                lore.add("§8서버 평균: §7" + formatTime(num.longValue()));
            }
        }
        if (serverBest != null) {
            lore.add("§8서버 최단: §7" + formatTime(serverBest));
        }

        return MainHubGui.icon(row.icon(), tierColor + row.displayName(), lore);
    }

    private static ItemStack trophyIcon(IslandTerritoryState territory) {
        long count = territory.customItemsSnapshot().getOrDefault(TROPHY_ITEM_ID, 0L);
        if (count > 0) {
            return MainHubGui.icon(Material.HEART_OF_THE_SEA, "§6§l균열왕의 심장",
                    List.of("§7──────────────",
                            "§7보유: §6" + count + "개",
                            "§7──────────────",
                            "§8시즌 최종보스 클리어 시 1개 지급"));
        }
        return MainHubGui.icon(Material.GRAY_STAINED_GLASS_PANE, "§7균열왕의 심장",
                List.of("§7──────────────",
                        "§7보유: §c0개",
                        "§7──────────────",
                        "§8시즌 최종보스 클리어로 획득"));
    }

    private static String formatTime(long seconds) {
        long min = seconds / 60;
        long sec = seconds % 60;
        if (min > 0) return min + "분 " + sec + "초";
        return sec + "초";
    }
}
