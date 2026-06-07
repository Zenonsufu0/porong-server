package com.poro.rpg.market;

import com.poro.rpg.gui.GuiTitles;
import com.poro.rpg.gui.MainHubGui;
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
                           String displayName, long price, List<String> lore,
                           long sellPrice, int sellBundle, boolean buyable, String currencyId) {
        /** 판매 묶음 크기(1=낱개 광물 / 64=set 농작물). */
        public int sellBundleSize() { return Math.max(1, sellBundle); }
        /** 판매 1묶음당 골드(고정가 — config sell-price 그대로). */
        public long bundleSellPrice() { return sellPrice; }
        /** 표시용 1개당 환산가(낱개=sellPrice, set=묶음가/64 내림). */
        public long unitSellPrice() { return sellPrice / sellBundleSize(); }
        public boolean sellable()  { return sellPrice > 0; }
        public boolean isBuyable() { return buyable && price > 0; }
        public boolean isCurrency() { return currencyId != null && !currencyId.isBlank(); }
    }

    private static final List<ShopItem> MATERIAL_ITEMS = new ArrayList<>();
    private static final List<ShopItem> BLOCK_ITEMS    = new ArrayList<>();

    /** config.yml의 shop.<category> 섹션에서 카테고리별 아이템 로드. */
    public static void reloadItems(Plugin plugin) {
        loadCategory(plugin, "material", MATERIAL_ITEMS);
        loadCategory(plugin, "block",    BLOCK_ITEMS);

        // legacy: shop.items (구버전 단일 섹션) — 존재하면 material에 병합
        ConfigurationSection legacy = plugin.getConfig().getConfigurationSection("shop.items");
        if (legacy != null) {
            int before = MATERIAL_ITEMS.size();
            loadInto(plugin, legacy, MATERIAL_ITEMS);
            if (MATERIAL_ITEMS.size() > before) {
                plugin.getLogger().warning("[Shop] shop.items (legacy) 섹션을 material로 병합 — config을 shop.material로 마이그레이션하세요.");
            }
        }
    }

    private static void loadCategory(Plugin plugin, String category, List<ShopItem> bucket) {
        bucket.clear();
        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("shop." + category);
        if (sec == null) return;
        loadInto(plugin, sec, bucket);
        plugin.getLogger().info("[Shop] " + category + " 탭 " + bucket.size() + "종 로드 완료.");
    }

    private static void loadInto(Plugin plugin, ConfigurationSection sec, List<ShopItem> bucket) {
        for (String key : sec.getKeys(false)) {
            ConfigurationSection itemSec = sec.getConfigurationSection(key);
            if (itemSec == null) continue;
            try {
                Material mat   = Material.valueOf(itemSec.getString("material", "STONE"));
                int    amount  = itemSec.getInt("amount", 1);
                String name    = itemSec.getString("display-name", key);
                long   price   = itemSec.getLong("price", 0);
                List<String> lore = itemSec.getStringList("lore");
                long   sellPrice  = itemSec.getLong("sell-price", 0);
                int    sellBundle = "set".equalsIgnoreCase(itemSec.getString("sell-mode", "unit")) ? 64 : 1;
                boolean buyable   = itemSec.getBoolean("buyable", price > 0);
                String currency   = itemSec.getString("currency", null);
                bucket.add(new ShopItem(key, mat, amount, name, price, lore, sellPrice, sellBundle, buyable, currency));
            } catch (Exception e) {
                plugin.getLogger().warning("[Shop] " + key + " 로드 실패: " + e.getMessage());
            }
        }
    }

    public static List<ShopItem> items(Tab tab) {
        return switch (tab) {
            case MATERIAL -> MATERIAL_ITEMS;
            case BLOCK    -> BLOCK_ITEMS;
            default       -> List.of();
        };
    }

    /** 매수 그리드용 — 구매 가능 품목만(판매전용 작물·광물·재화 숨김). */
    public static List<ShopItem> buyables(Tab tab) {
        return items(tab).stream().filter(ShopItem::isBuyable).toList();
    }

    /** 판매 GUI용 — 고정 판매가가 있는 품목만(농작물·광물·전장의 파편). */
    public static List<ShopItem> sellables(Tab tab) {
        return items(tab).stream().filter(ShopItem::sellable).toList();
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

        // 아이템 영역 — 매수 가능 품목만(판매전용은 판매 GUI에서)
        List<ShopItem> items = buyables(activeTab);
        if (items.isEmpty()) {
            inv.setItem(22, MainHubGui.icon(Material.BARRIER, "§7" + tabName(activeTab) + " 탭 §8[준비 중]",
                    List.of("§7구매 가능한 아이템이 없습니다.")));
        } else {
            for (int i = 0; i < items.size() && i < 36; i++) {
                ShopItem si = items.get(i);
                inv.setItem(ITEM_AREA_START + i, displayItem(si));
            }
        }

        // 액션·네비
        inv.setItem(SLOT_SELL_BUTTON, MainHubGui.icon(Material.GOLD_INGOT, "§e판매",
                List.of("§7농작물·광물·전장의 파편 판매", "§7클릭 → 판매 GUI")));
        inv.setItem(SLOT_PREV_PAGE, MainHubGui.icon(Material.GRAY_DYE, "§8◀ 이전 페이지", List.of("§8첫 페이지입니다.")));
        inv.setItem(SLOT_PAGE_INFO, MainHubGui.icon(Material.PAPER,    "§f1 / 1", List.of()));
        inv.setItem(SLOT_NEXT_PAGE, MainHubGui.icon(Material.GRAY_DYE, "§8다음 페이지 ▶", List.of("§8마지막 페이지입니다.")));
        inv.setItem(SLOT_BACK,      MainHubGui.icon(Material.ARROW, "§7뒤로", List.of("§7영지 관리")));

        player.openInventory(inv);
    }

    public static ShopItem itemAt(Tab tab, int rawSlot) {
        if (rawSlot < ITEM_AREA_START || rawSlot > ITEM_AREA_END) return null;
        int idx = rawSlot - ITEM_AREA_START;
        List<ShopItem> items = buyables(tab);   // 매수 그리드와 동일 순서
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
        int rightSets = Math.max(1, 64 / Math.max(1, si.amount()));
        int rightTotalItems = rightSets * si.amount();
        long rightTotalCost = (long) rightSets * si.price();

        ItemStack stack = new ItemStack(si.material(), Math.max(1, Math.min(si.amount(), 64)));
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(Component.text(si.displayName()));
        List<Component> lore = new ArrayList<>();
        si.lore().forEach(l -> lore.add(Component.text(l)));
        lore.add(Component.text("§7──────────────"));
        lore.add(Component.text("§e1세트: §f" + si.price() + "G §7(" + si.amount() + "개)"));
        lore.add(Component.text("§7좌클릭  §f1세트 구매 §7(" + si.amount() + "개 / " + si.price() + "G)"));
        lore.add(Component.text("§7우클릭  §f" + rightSets + "세트 구매 §7(" + rightTotalItems + "개 / " + rightTotalCost + "G)"));
        if (si.sellable()) {  // 구매·판매 겸용(감자·당근 등)
            lore.add(si.sellBundleSize() > 1
                    ? Component.text("§2판매: §f" + si.bundleSellPrice() + "G §7/ " + si.sellBundleSize() + "개 §8(판매 GUI)")
                    : Component.text("§2판매: §f" + si.unitSellPrice() + "G §71개 §8(판매 GUI)"));
        }
        meta.lore(lore);
        stack.setItemMeta(meta);
        return stack;
    }

    // ═══════════════════════════════════════════════════════════════════
    // 판매 서브 GUI (54슬롯)
    // ═══════════════════════════════════════════════════════════════════
    public static final int SELL_ITEM_AREA_START = 0;
    public static final int SELL_ITEM_AREA_END   = 44;
    public static final int SLOT_SELL_ALL        = 45;
    public static final int SLOT_SELL_BACK       = 53;

    /** 판매 GUI 슬롯에 표시할 ItemStack 빌더. storageCount/invCount는 caller가 계산해서 전달
     *  (재화 품목이면 invCount=재화지갑 보유, storageCount=0으로 넘겨줌). 차감 순서 = 창고→인벤. */
    public static ItemStack sellDisplayItem(ShopItem si, long invCount, long storageCount) {
        long total = invCount + storageCount;
        int bundle = si.sellBundleSize();
        boolean setMode = bundle > 1;
        long sets = total / bundle;                 // set 모드: 판매 가능한 묶음 수
        long sellableQty = setMode ? sets * bundle : total;
        long totalGold = setMode ? sets * si.bundleSellPrice() : total * si.unitSellPrice();

        ItemStack stack = new ItemStack(si.material(), Math.max(1, (int) Math.min(total, 64)));
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(Component.text(si.displayName()));
        List<Component> lore = new ArrayList<>();
        lore.add(setMode
                ? Component.text("§2판매가: §f" + si.bundleSellPrice() + "G §7/ " + bundle + "개(1세트)")
                : Component.text("§2판매가: §f" + si.unitSellPrice() + "G §71개"));
        lore.add(Component.text("§7──────────────"));
        if (si.isCurrency()) {
            lore.add(Component.text("§7보유(재화): §f" + invCount + "개"));
        } else {
            lore.add(Component.text("§7창고: §f" + storageCount + "개"));
            lore.add(Component.text("§7인벤토리: §f" + invCount + "개"));
        }
        if (setMode) {
            lore.add(Component.text("§7판매가능: §f" + sets + "세트 §7(" + sellableQty + "개)  →  §e" + totalGold + "G"));
            lore.add(Component.text("§7──────────────"));
            lore.add(Component.text("§7좌클릭    §f1세트(" + bundle + "개) 판매"));
            lore.add(Component.text("§7우클릭/Shift  §f전량(세트 단위) 판매"));
            lore.add(Component.text("§8※ " + bundle + "개 미만 잔량은 판매 불가"));
        } else {
            lore.add(Component.text("§7합계: §f" + total + "개  →  §e" + totalGold + "G"));
            lore.add(Component.text("§7──────────────"));
            lore.add(Component.text("§7좌클릭    §f1개 판매"));
            lore.add(Component.text("§7우클릭    §f64개 판매"));
            lore.add(Component.text("§7Shift+클릭  §f전량 판매"));
        }
        meta.lore(lore);
        stack.setItemMeta(meta);
        return stack;
    }

    public static Inventory createSellInventory(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, GuiTitles.SHOP_SELL);
        ItemStack gray = MainHubGui.icon(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < 54; i++) inv.setItem(i, gray);
        return inv;
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
