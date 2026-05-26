package com.poro.empire.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class BossHubGui {
    private BossHubGui() {}

    public record BossDef(String id, String name, String tier, Material icon,
                          String recLevel, boolean needsUnlock) {}

    private static final Map<Integer, BossDef> SLOT_MAP = new HashMap<>();

    static {
        // 시즌보스 6종 — row 1 & 2
        SLOT_MAP.put(10, new BossDef("fallen_knight",  "타락 기사장",   "시즌1", Material.NETHERITE_SWORD,   "6~10강",  false));
        SLOT_MAP.put(12, new BossDef("corrupted_lord", "오염된 군주",   "시즌2", Material.POISONOUS_POTATO,  "10~13강", false));
        SLOT_MAP.put(14, new BossDef("stone_colossus", "석조 거상",     "시즌3", Material.STONE,              "13~16강", false));
        SLOT_MAP.put(19, new BossDef("storm_sorcerer", "폭풍 술사",     "시즌4", Material.LIGHTNING_ROD,      "16~18강", false));
        SLOT_MAP.put(21, new BossDef("abyss_guardian", "심연 수호자",   "시즌5", Material.CRYING_OBSIDIAN,    "18~20강", false));
        SLOT_MAP.put(23, new BossDef("void_herald",    "공허 사자",     "시즌6", Material.CHORUS_FRUIT,       "20~22강", false));
        // 최종보스 3종 — row 3 (공허 사자 클리어 필요)
        SLOT_MAP.put(29, new BossDef("rift_king",      "균열왕",        "최종",  Material.NETHER_STAR,        "22강+",   true));
        SLOT_MAP.put(31, new BossDef("corrupted_dyad", "타락한 이중체", "최종",  Material.WITHER_ROSE,        "22강+",   true));
        SLOT_MAP.put(33, new BossDef("spirit_watcher", "진혼의 주시자", "최종",  Material.SOUL_LANTERN,       "22강+",   true));
    }

    public static void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, GuiTitles.BOSS_HUB);
        ItemStack pane = MainHubGui.icon(Material.BLACK_STAINED_GLASS_PANE, " ", List.of());
        ItemStack sepPane = MainHubGui.icon(Material.PURPLE_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < 54; i++) inv.setItem(i, pane);
        // 최종보스 구간 구분 (row 3)
        for (int s = 27; s <= 35; s++) inv.setItem(s, sepPane);

        for (Map.Entry<Integer, BossDef> e : SLOT_MAP.entrySet()) {
            BossDef b = e.getValue();
            List<String> lore = b.needsUnlock()
                    ? List.of("§7──────────────",
                              "§7권장: §e" + b.recLevel(),
                              "§7파티: §f1~3인",
                              "§c공허 사자 클리어 필요",
                              "",
                              "§a클릭하여 입장 준비")
                    : List.of("§7──────────────",
                              "§7권장: §e" + b.recLevel(),
                              "§7파티: §f1~3인",
                              "",
                              "§a클릭하여 입장 준비");
            inv.setItem(e.getKey(), MainHubGui.icon(b.icon(),
                    "§f[" + b.tier() + "] " + b.name(), lore));
        }

        inv.setItem(45, MainHubGui.icon(Material.ARROW,   "§7뒤로", List.of("§7메인 메뉴")));
        inv.setItem(49, MainHubGui.icon(Material.BARRIER, "§c닫기", List.of()));
        player.openInventory(inv);
    }

    public static String  bossIdAt(int slot)          { BossDef d = SLOT_MAP.get(slot); return d == null ? null : d.id(); }
    public static String  bossNameAt(int slot)         { BossDef d = SLOT_MAP.get(slot); return d == null ? null : d.name(); }
    public static boolean bossNeedsUnlockAt(int slot)  { BossDef d = SLOT_MAP.get(slot); return d != null && d.needsUnlock(); }
}
