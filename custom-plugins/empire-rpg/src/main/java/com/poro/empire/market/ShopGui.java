package com.poro.empire.market;

import com.poro.empire.gui.GuiTitles;
import com.poro.empire.gui.MainHubGui;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

/**
 * 상점 GUI (54슬롯, 4탭 구조).
 *
 * <pre>
 * row0: [재료][블럭][치장][특수] ░ ░ ░ ░ ░
 * row1~4: 아이템 슬롯 (페이지당 최대 36종)
 * row5: [판매] ░ ░ [◀] [페이지] [▶] ░ ░ [뒤로]
 * </pre>
 *
 * <p>1차 시즌 압축 구현: 모든 아이템은 config.yml `shop.items`에서 단일 카테고리로 로드.
 * 탭 4종 중 재료(MATERIAL)만 활성. 나머지 3종은 "준비 중". 판매·페이지네이션은 stub.
 */
public final class ShopGui {
    private ShopGui() {}

    public static final int SLOT_TAB_MATERIAL = 0;
    public static final int SLOT_TAB_BLOCK    = 1;
    public static final int SLOT_TAB_COSMETIC = 2;
    public static final int SLOT_TAB_SPECIAL  = 3;
    public static final int SLOT_SELL_BUTTON  = 45;
    public static final int SLOT_PREV_PAGE    = 48;
    public static final int SLOT_PAGE_INFO    = 49;
    public static final int SLOT_NEXT_PAGE    = 50;
    public static final int SLOT_BACK         = 53;

    public static final int ITEM_AREA_START = 9;
    public static final int ITEM_AREA_END   = 44;

    public enum Tab { MATERIAL, BLOCK, COSMETIC, SPECIAL }

    public record ShopItem(String key, Material material, int amount,
                           String displayName, long price, List<String> lore) {}

    private static final List<ShopItem> MATERIAL_ITEMS = new ArrayList<>();

    /** config.yml의 shop.items 섹션에서 아이템 로드. */
    public static void reloadItems(Plugin plugin) {
        MATERIAL_ITEMS.clear();
        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("shop.items");
        if (sec == null) {
            plugin.getLogger().warning("[Shop] config.yml shop.items 섹션이 없습니다.");
            return;
        }
        for (String key : sec.getKeys(false)) {
            ConfigurationSection itemSec = sec.getConfigurationSection(key);
            if (itemSec == null) continue;
            try {
                Material mat   = Material.valueOf(itemSec.getString("material", "STONE"));
                int    amount  = itemSec.getInt("amount", 1);
                String name    = itemSec.getString("display-name", key);
                long   price   = itemSec.getLong("price", 0);
                List<String> lore = itemSec.getStringList("lore");
                MATERIAL_ITEMS.add(new ShopItem(key, mat, amount, name, price, lore));
            } catch (Exception e) {
                plugin.getLogger().warning("[Shop] " + key + " 로드 실패: " + e.getMessage());
            }
        }
        plugin.getLogger().info("[Shop] 아이템 " + MATERIAL_ITEMS.size() + "종 로드 완료.");
    }

    public static List<ShopItem> items(Tab tab) {
        return tab == Tab.MATERIAL ? MATERIAL_ITEMS : List.of();
    }

    public static void open(Player player, Tab activeTab) {
        Inventory inv = Bukkit.createInventory(null, 54, GuiTitles.SHOP);
        ItemStack gray = MainHubGui.icon(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
        ItemStack black = MainHubGui.icon(Material.BLACK_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < 54; i++) inv.setItem(i, i >= ITEM_AREA_START && i <= ITEM_AREA_END ? gray : black);

        // 탭 버튼
        inv.setItem(SLOT_TAB_MATERIAL, tabIcon(Tab.MATERIAL, activeTab, Material.CRAFTING_TABLE, "재료"));
        inv.setItem(SLOT_TAB_BLOCK,    tabIcon(Tab.BLOCK,    activeTab, Material.BRICKS,         "블럭"));
        inv.setItem(SLOT_TAB_COSMETIC, tabIcon(Tab.COSMETIC, activeTab, Material.DIAMOND,         "치장"));
        inv.setItem(SLOT_TAB_SPECIAL,  tabIcon(Tab.SPECIAL,  activeTab, Material.NETHER_STAR,     "특수"));

        // 아이템 영역
        List<ShopItem> items = items(activeTab);
        if (items.isEmpty()) {
            inv.setItem(22, MainHubGui.icon(Material.BARRIER, "§7" + tabName(activeTab) + " 탭 §8[준비 중]",
                    List.of("§7아이템이 등록되지 않았습니다.")));
        } else {
            for (int i = 0; i < items.size() && i < 36; i++) {
                ShopItem si = items.get(i);
                inv.setItem(ITEM_AREA_START + i, displayItem(si));
            }
        }

        // 액션·네비
        inv.setItem(SLOT_SELL_BUTTON, MainHubGui.icon(Material.GOLD_INGOT, "§7판매 §8[준비 중]",
                List.of("§7판매 기능은 §c준비 중§7입니다.")));
        inv.setItem(SLOT_PREV_PAGE, MainHubGui.icon(Material.GRAY_DYE, "§8◀ 이전 페이지", List.of("§8첫 페이지입니다.")));
        inv.setItem(SLOT_PAGE_INFO, MainHubGui.icon(Material.PAPER,    "§f1 / 1", List.of()));
        inv.setItem(SLOT_NEXT_PAGE, MainHubGui.icon(Material.GRAY_DYE, "§8다음 페이지 ▶", List.of("§8마지막 페이지입니다.")));
        inv.setItem(SLOT_BACK,      MainHubGui.icon(Material.ARROW, "§7뒤로", List.of("§7영지 관리")));

        player.openInventory(inv);
    }

    public static ShopItem itemAt(Tab tab, int rawSlot) {
        if (rawSlot < ITEM_AREA_START || rawSlot > ITEM_AREA_END) return null;
        int idx = rawSlot - ITEM_AREA_START;
        List<ShopItem> items = items(tab);
        return idx < items.size() ? items.get(idx) : null;
    }

    private static ItemStack tabIcon(Tab tab, Tab active, Material mat, String name) {
        boolean isActive = (tab == active);
        Material display = isActive ? Material.LIME_STAINED_GLASS_PANE : mat;
        String   label   = isActive ? "§a" + name + " §8← 현재" : "§f" + name;
        List<String> lore = isActive
                ? List.of("§8현재 탭")
                : List.of("§7클릭 → 탭 전환");
        return MainHubGui.icon(display, label, lore);
    }

    private static ItemStack displayItem(ShopItem si) {
        ItemStack stack = new ItemStack(si.material(), Math.max(1, Math.min(si.amount(), 64)));
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(Component.text(si.displayName()));
        List<Component> lore = new ArrayList<>();
        si.lore().forEach(l -> lore.add(Component.text(l)));
        lore.add(Component.text("§7──────────────"));
        lore.add(Component.text("§e구매: §f" + si.price() + "G §7(" + si.amount() + "개)"));
        lore.add(Component.text("§7좌클릭  §f1세트 구매"));
        lore.add(Component.text("§7우클릭  §f64세트 구매"));
        meta.lore(lore);
        stack.setItemMeta(meta);
        return stack;
    }

    private static String tabName(Tab tab) {
        return switch (tab) {
            case MATERIAL -> "재료";
            case BLOCK    -> "블럭";
            case COSMETIC -> "치장";
            case SPECIAL  -> "특수";
        };
    }
}
