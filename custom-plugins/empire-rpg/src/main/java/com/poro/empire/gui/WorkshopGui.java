package com.poro.empire.gui;

import com.poro.empire.growth.island.IslandTerritoryState;
import com.poro.empire.growth.island.WorkshopJob;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * 공방 GUI (54슬롯).
 *
 * <pre>
 * row0 (슬롯 0~7) : 탭 버튼 8종 (LIME or GRAY pane)
 * row1 (슬롯 9~17): 레시피 목록 (최대 9개)
 * row2 (슬롯 18~26): 대기열 1~9
 * row3 (슬롯 27~35): 대기열 10~18
 * row4 (슬롯 36~44): 여백
 * row5 (슬롯 45~53): 뒤로(45) / 닫기(53)
 * </pre>
 */
public final class WorkshopGui {

    public enum WorkshopTab {
        ESTATE      ("영지 제작",     Material.GRASS_BLOCK,       "영지 재료로 정수·흔적을 제작합니다."),
        TRACE       ("강화 흔적",     Material.ENCHANTED_BOOK,    "강화 성공률 보정 흔적을 제작합니다."),
        SMELT       ("제련",         Material.BLAST_FURNACE,     "원석을 마도합금으로 제련합니다."),
        REFINE      ("정제",         Material.BREWING_STAND,     "약초·정수를 정제 약초로 가공합니다."),
        ALCHEMY_HEAL("연금술 (치료)", Material.POTION,            "치료 포션을 제조합니다."),
        ALCHEMY_BOOST("연금술 (부스트)", Material.SPLASH_POTION,  "효과 부스트 포션을 제조합니다."),
        COOK        ("요리",         Material.CAMPFIRE,          "만찬 음식을 요리합니다."),
        ORE_CONVERT ("광물 변환",    Material.DIAMOND,           "광물 과잉분을 희귀 광물로 변환합니다.");

        public final String displayName;
        public final Material icon;
        public final String description;

        WorkshopTab(String displayName, Material icon, String description) {
            this.displayName = displayName;
            this.icon = icon;
            this.description = description;
        }
    }

    public static final Component TITLE_PREFIX = Component.text("공방 — ");
    public static final int SLOT_BACK  = 45;
    public static final int SLOT_CLOSE = 53;

    private static final WorkshopTab[] TABS = WorkshopTab.values();

    private WorkshopGui() {}

    // ─── 오픈 ────────────────────────────────────────────────────

    public static void open(Player player, WorkshopTab tab) {
        open(player, tab, null);
    }

    public static void open(Player player, WorkshopTab tab, IslandTerritoryState territory) {
        Component title = TITLE_PREFIX.append(Component.text(tab.displayName).color(NamedTextColor.YELLOW));
        Inventory inv = Bukkit.createInventory(null, 54, title);
        render(inv, tab, territory);
        player.openInventory(inv);
    }

    // ─── 유틸 ────────────────────────────────────────────────────

    public static boolean isTitle(Component title) {
        String plain = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
                .plainText().serialize(title);
        return plain.startsWith("공방 — ");
    }

    public static WorkshopTab tabFromTitle(Component title) {
        String plain = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
                .plainText().serialize(title);
        for (WorkshopTab t : TABS) {
            if (plain.equals("공방 — " + t.displayName)) return t;
        }
        return null;
    }

    // ─── 렌더 ────────────────────────────────────────────────────

    public static void render(Inventory inv, WorkshopTab activeTab, IslandTerritoryState territory) {
        ItemStack filler = icon(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < 54; i++) inv.setItem(i, filler);

        // row0: 탭 버튼 (슬롯 0~7)
        for (int i = 0; i < TABS.length; i++) {
            inv.setItem(i, buildTabItem(TABS[i], TABS[i] == activeTab));
        }

        // row1: 레시피 목록 (슬롯 9~17)
        List<WorkshopRecipe> recipes = WorkshopRecipeRegistry.getRecipes(activeTab);
        for (int i = 0; i < 9; i++) {
            if (i < recipes.size()) {
                inv.setItem(9 + i, buildRecipeIcon(recipes.get(i)));
            }
        }

        // row2-3: 대기열 (슬롯 18~35)
        if (territory != null) {
            int maxSlots = Math.min(territory.workshopQueueMax(), 18);
            List<WorkshopJob> jobs = territory.workshopJobs();
            for (int i = 0; i < maxSlots; i++) {
                if (i < jobs.size()) {
                    inv.setItem(18 + i, buildJobIcon(jobs.get(i)));
                } else {
                    inv.setItem(18 + i, buildEmptyQueueSlot());
                }
            }
        }

        // row5: 뒤로 / 닫기
        inv.setItem(SLOT_BACK,  icon(Material.DARK_OAK_DOOR, "§7◀ 뒤로", List.of("§7영지 메뉴")));
        inv.setItem(SLOT_CLOSE, icon(Material.BARRIER, "§c닫기", List.of()));
    }

    // ─── 아이템 빌더 ─────────────────────────────────────────────

    private static ItemStack buildTabItem(WorkshopTab tab, boolean active) {
        String nameColor = active ? "§a" : "§7";
        List<String> lore = List.of(
                "§7" + tab.description,
                "§7──────────",
                active ? "§a▶ 선택된 탭" : "§7클릭하여 선택");
        ItemStack item = new ItemStack(active ? tab.icon : Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(nameColor + tab.displayName));
        meta.lore(lore.stream().map(Component::text).toList());
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack buildRecipeIcon(WorkshopRecipe recipe) {
        List<String> lore = new ArrayList<>();
        lore.add("§7──────────────");
        lore.add("§7재료:");
        for (var mat : recipe.materials()) {
            lore.add("  §7- §f" + WorkshopRecipeRegistry.displayName(mat.itemId())
                    + " §7×" + mat.amount());
        }
        lore.add("§7──────────────");
        lore.add("§7제작 시간: §e" + recipe.durationMinutes() + "분");
        lore.add("§7결과물: §e" + recipe.displayName() + " §7×" + recipe.resultAmount());
        lore.add("§7──────────────");
        lore.add("§e좌클릭 §7제작 등록");
        return icon(recipe.guiIcon(), "§f" + recipe.displayName(), lore);
    }

    private static ItemStack buildJobIcon(WorkshopJob job) {
        long now = System.currentTimeMillis();
        long remaining = Math.max(0, job.completeAt() - now);
        boolean done = remaining == 0;

        String status = done ? "§a완료 — 다음 접속 시 자동 입금" : "§e제작 중";
        String timeStr = done ? "" : formatMs(remaining);

        List<String> lore = new ArrayList<>();
        lore.add("§7──────────────");
        lore.add("§7레시피: §f" + job.recipeId());
        lore.add("§7상태: " + status);
        if (!done) lore.add("§7완료까지: §f" + timeStr);

        Material icon = done ? Material.LIME_STAINED_GLASS_PANE : Material.ORANGE_STAINED_GLASS_PANE;
        String title = done ? "§a[완료] " : "§e[제작 중] ";
        return icon(icon, title + job.recipeId(), lore);
    }

    private static ItemStack buildEmptyQueueSlot() {
        return icon(Material.WHITE_STAINED_GLASS_PANE, "§8[빈 슬롯]",
                List.of("§7레시피를 클릭하면 여기에 등록됩니다."));
    }

    private static String formatMs(long ms) {
        long totalSec = ms / 1000;
        long min = totalSec / 60;
        long sec = totalSec % 60;
        return min > 0 ? min + "분 " + sec + "초" : sec + "초";
    }

    static ItemStack icon(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name));
        meta.lore(lore.stream().map(Component::text).toList());
        item.setItemMeta(meta);
        return item;
    }
}
