package com.poro.rpg.gui;

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
        // 시즌보스 6종 — row1·2, 컬럼 2·4·6 (여백 2·1·1·2 가운데 정렬)
        SLOT_MAP.put(11, new BossDef("fallen_knight",  "타락 기사장",   "시즌1", Material.NETHERITE_SWORD,   "6~10강",  false));
        SLOT_MAP.put(13, new BossDef("corrupted_lord", "오염된 군주",   "시즌2", Material.POISONOUS_POTATO,  "10~13강", false));
        SLOT_MAP.put(15, new BossDef("stone_colossus", "석조 거상",     "시즌3", Material.STONE,              "13~16강", false));
        SLOT_MAP.put(20, new BossDef("storm_sorcerer", "폭풍 술사",     "시즌4", Material.LIGHTNING_ROD,      "16~18강", false));
        SLOT_MAP.put(22, new BossDef("abyss_guardian", "심연 수호자",   "시즌5", Material.CRYING_OBSIDIAN,    "18~20강", false));
        SLOT_MAP.put(24, new BossDef("void_herald",    "공허 사자",     "시즌6", Material.CHORUS_FRUIT,       "20~22강", false));
        // 최종보스 3종 — row3, 컬럼 2·4·6 (시즌과 세로 정렬)
        SLOT_MAP.put(29, new BossDef("rift_king",      "균열왕",        "최종",  Material.NETHER_STAR,        "22강+",   true));
        SLOT_MAP.put(31, new BossDef("corrupted_dyad", "타락한 이중체", "최종",  Material.WITHER_ROSE,        "22강+",   true));
        SLOT_MAP.put(33, new BossDef("spirit_watcher", "진혼의 주시자", "최종",  Material.SOUL_LANTERN,       "22강+",   true));
    }

    // 27슬롯 중간 허브 — 파티생성/관리·목록·보스정보·클리어기록
    public static void open(Player player, boolean inParty) {
        Inventory inv = Bukkit.createInventory(null, 27, GuiTitles.BOSS_HUB);
        ItemStack blk = MainHubGui.icon(Material.BLACK_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < 27; i++) inv.setItem(i, blk);

        // slot10: 파티 유무로 생성/관리 분기
        if (inParty) {
            inv.setItem(10, MainHubGui.icon(Material.CHEST, "§a파티 관리",
                    List.of("§7내 파티 보기·준비·입장", "§8▶ 클릭하여 열기")));
        } else {
            inv.setItem(10, MainHubGui.icon(Material.GREEN_WOOL, "§a파티 생성",
                    List.of("§7보스·인원·제목을 정해 생성", "§8▶ 클릭하여 열기")));
        }
        inv.setItem(12, MainHubGui.icon(Material.BOOK,                  "§f파티 목록",   List.of("§7열린 파티에 참가",     "§8▶ 클릭하여 열기")));
        inv.setItem(14, MainHubGui.icon(Material.WITHER_SKELETON_SKULL, "§c보스 정보",   List.of("§7보스 정보·권장 스펙 확인", "§8▶ 클릭하여 열기")));
        inv.setItem(16, MainHubGui.icon(Material.NETHER_STAR,           "§e클리어 기록", List.of("§7보스별 개인·서버 기록", "§8▶ 클릭하여 열기")));

        inv.setItem(18, MainHubGui.icon(Material.ARROW, "§7뒤로", List.of("§7메인 메뉴")));
        player.openInventory(inv);
    }

    /** 파티 생성 — 보스 선택. */
    public static void openPartyCreateBoss(Player player) {
        renderBossGrid(player, GuiTitles.PARTY_CREATE_BOSS, "§a클릭하여 이 보스로 파티 생성");
    }

    /** 보스 정보/선택 — 시즌 6(좌측 세로 컬럼) + 최종 3(우측 세로 컬럼). */
    public static void openBossInfo(Player player) {
        renderBossGrid(player, GuiTitles.BOSS_INFO, "§a클릭하여 입장 준비");
    }

    private static void renderBossGrid(Player player, net.kyori.adventure.text.Component title, String action) {
        Inventory inv = Bukkit.createInventory(null, 54, title);
        ItemStack bluPane = MainHubGui.icon(Material.BLUE_STAINED_GLASS_PANE,   " ", List.of());
        for (int i = 0; i <= 26; i++) inv.setItem(i, bluPane);
        ItemStack purPane = MainHubGui.icon(Material.PURPLE_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 27; i <= 44; i++) inv.setItem(i, purPane);
        ItemStack blkPane = MainHubGui.icon(Material.BLACK_STAINED_GLASS_PANE,  " ", List.of());
        for (int i = 45; i <= 53; i++) inv.setItem(i, blkPane);
        inv.setItem(4,  MainHubGui.icon(Material.BLUE_STAINED_GLASS_PANE,   "§b시즌 보스 §8(6 ~ 22강)", List.of()));
        inv.setItem(27, MainHubGui.icon(Material.PURPLE_STAINED_GLASS_PANE, "§5최종 보스", List.of("§8공허 사자(시즌6) 클리어 필요")));

        for (Map.Entry<Integer, BossDef> e : SLOT_MAP.entrySet()) {
            BossDef b = e.getValue();
            java.util.List<String> lore = new java.util.ArrayList<>();
            lore.add("§7──────────────");
            lore.add("§7권장: §e" + b.recLevel());
            lore.add("§7파티: §f1~3인");
            if (b.needsUnlock()) lore.add("§c공허 사자 클리어 필요");
            lore.add("");
            lore.add(action);
            inv.setItem(e.getKey(), MainHubGui.icon(b.icon(), "§f[" + b.tier() + "] " + b.name(), lore));
        }
        inv.setItem(45, MainHubGui.icon(Material.ARROW, "§7뒤로", List.of("§7보스 허브")));
        player.openInventory(inv);
    }

    public static String  bossIdAt(int slot)          { BossDef d = SLOT_MAP.get(slot); return d == null ? null : d.id(); }
    public static String  bossNameAt(int slot)         { BossDef d = SLOT_MAP.get(slot); return d == null ? null : d.name(); }

    /** 보스 id → 표시명. 미존재 시 id 그대로. */
    public static String  bossNameById(String id) {
        for (BossDef d : SLOT_MAP.values()) if (d.id().equals(id)) return d.name();
        return id;
    }
    public static boolean bossNeedsUnlockAt(int slot)  { BossDef d = SLOT_MAP.get(slot); return d != null && d.needsUnlock(); }
}
