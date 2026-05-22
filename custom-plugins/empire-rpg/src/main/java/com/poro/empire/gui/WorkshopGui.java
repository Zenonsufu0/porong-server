package com.poro.empire.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

/**
 * 공방 GUI (54슬롯).
 *
 * <pre>
 * row0 (슬롯 0~5) : 탭 버튼 — 선택 탭은 LIME_STAINED_GLASS_PANE, 비선택은 GRAY_STAINED_GLASS_PANE
 * row1 (슬롯 9~17): 레시피 목록
 * row2 (슬롯 18~26): 대기열 1~9
 * row3 (슬롯 27~35): 대기열 10~18
 * row4 (슬롯 36~44): 대기열 내비게이션
 * row5 (슬롯 45~53): 뒤로/닫기
 * </pre>
 */
public final class WorkshopGui {

    public enum WorkshopTab {
        ESTATE("영지 제작", Material.GRASS_BLOCK, "영지 재료를 소모해 제국 재화를 만듭니다."),
        SMELT("제련", Material.BLAST_FURNACE, "광석을 합금으로 제련합니다."),
        REFINE("정제", Material.BREWING_STAND, "약초를 정제합니다."),
        ENHANCE_COSMETIC("연마 (치장)", Material.IRON_INGOT, "치장 아이템을 연마합니다."),
        ENCHANT("연마 (부여)", Material.ENCHANTING_TABLE, "부여 아이템을 연마합니다."),
        COOK("요리", Material.CAMPFIRE, "만찬 음식을 요리합니다.");

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

    public static void open(Player player, WorkshopTab tab) {
        Component title = TITLE_PREFIX.append(Component.text(tab.displayName).color(NamedTextColor.YELLOW));
        Inventory inv = Bukkit.createInventory(null, 54, title);
        render(inv, tab);
        player.openInventory(inv);
    }

    public static boolean isTitle(Component title) {
        String plain = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
                .plainText().serialize(title);
        return plain.startsWith("공방 — ");
    }

    public static @org.jetbrains.annotations.Nullable WorkshopTab tabFromTitle(Component title) {
        String plain = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
                .plainText().serialize(title);
        for (WorkshopTab t : TABS) {
            if (plain.equals("공방 — " + t.displayName)) return t;
        }
        return null;
    }

    public static void render(Inventory inv, WorkshopTab activeTab) {
        ItemStack filler = icon(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < 54; i++) inv.setItem(i, filler);

        // row0 탭 버튼 (슬롯 0~5)
        for (int i = 0; i < TABS.length; i++) {
            WorkshopTab tab = TABS[i];
            boolean active = tab == activeTab;
            ItemStack tabItem = buildTabItem(tab, active);
            inv.setItem(i, tabItem);
        }

        // 뒤로 / 닫기
        inv.setItem(SLOT_BACK,  icon(Material.DARK_OAK_DOOR, "§7◀ 뒤로", List.of("§7영지 메뉴")));
        inv.setItem(SLOT_CLOSE, icon(Material.BARRIER, "§c닫기", List.of()));
    }

    // ─── internal ────────────────────────────────────────────────

    private static ItemStack buildTabItem(WorkshopTab tab, boolean active) {
        Material pane = active ? Material.LIME_STAINED_GLASS_PANE : Material.GRAY_STAINED_GLASS_PANE;
        String nameColor = active ? "§a" : "§7";
        List<String> lore = active
                ? List.of("§7" + tab.description, "§7──────────", "§a선택된 탭")
                : List.of("§7" + tab.description, "§7──────────", "§7클릭하여 선택");

        // 탭 아이콘 위에 실제 재료 이미지 표시 (베이스는 원래 아이콘)
        ItemStack item = new ItemStack(tab.icon);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(nameColor + tab.displayName));
        meta.lore(lore.stream().map(Component::text).toList());
        item.setItemMeta(meta);

        // 활성 탭은 유리판 아이콘으로 덮어 강조
        if (!active) {
            ItemStack glass = new ItemStack(pane);
            ItemMeta glassMeta = glass.getItemMeta();
            glassMeta.displayName(Component.text(nameColor + tab.displayName));
            glassMeta.lore(lore.stream().map(Component::text).toList());
            glass.setItemMeta(glassMeta);
            return glass;
        }
        return item;
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
