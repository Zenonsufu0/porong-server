package com.poro.rpg.gui;

import com.poro.rpg.growth.island.IslandStorage;
import com.poro.rpg.growth.island.IslandTerritoryState;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 영지 저장고 GUI (54슬롯, gui_storage.md §2).
 *
 * <pre>
 * 행 1~5 (슬롯 0~44) : 아이템 표시 (페이지당 최대 45종)
 * 슬롯 45 : 전체 입금
 * 슬롯 48 : 이전 페이지
 * 슬롯 49 : 페이지 표시
 * 슬롯 50 : 다음 페이지
 * 슬롯 52 : 뒤로 (영지 서브 허브)
 * 슬롯 53 : 닫기
 * </pre>
 */
public class StorageGui {

    public static final Component TITLE = Component.text("영지 저장고").color(NamedTextColor.DARK_GRAY);

    public static final int ITEMS_PER_PAGE = 45;
    public static final int SLOT_DEPOSIT_ALL = 45;
    public static final int SLOT_PREV        = 48;
    public static final int SLOT_PAGE_INFO   = 49;
    public static final int SLOT_NEXT        = 50;
    public static final int SLOT_BACK        = 52;
    public static final int SLOT_CLOSE       = 53;

    private static final NumberFormat FMT = NumberFormat.getNumberInstance(Locale.KOREA);

    // ─── 카테고리 정렬 (광물류 0, 농작물 1, 목재류 2, 기타 99) ──────
    private static final Map<Material, Integer> CATEGORY = buildCategoryMap();

    private static Map<Material, Integer> buildCategoryMap() {
        Map<Material, Integer> m = new HashMap<>();
        for (Material mat : List.of(
                Material.IRON_ORE, Material.DEEPSLATE_IRON_ORE, Material.RAW_IRON, Material.IRON_INGOT,
                Material.GOLD_ORE, Material.DEEPSLATE_GOLD_ORE, Material.RAW_GOLD, Material.GOLD_INGOT,
                Material.DIAMOND, Material.DIAMOND_ORE, Material.DEEPSLATE_DIAMOND_ORE,
                Material.EMERALD, Material.EMERALD_ORE, Material.DEEPSLATE_EMERALD_ORE,
                Material.COAL, Material.COAL_ORE, Material.DEEPSLATE_COAL_ORE,
                Material.REDSTONE, Material.REDSTONE_ORE, Material.DEEPSLATE_REDSTONE_ORE,
                Material.LAPIS_LAZULI, Material.LAPIS_ORE, Material.DEEPSLATE_LAPIS_ORE,
                Material.QUARTZ, Material.NETHER_QUARTZ_ORE,
                Material.COPPER_ORE, Material.DEEPSLATE_COPPER_ORE, Material.RAW_COPPER, Material.COPPER_INGOT,
                Material.NETHERITE_INGOT, Material.NETHERITE_SCRAP)) { m.put(mat, 0); }
        for (Material mat : List.of(
                Material.WHEAT, Material.WHEAT_SEEDS,
                Material.CARROT, Material.POTATO, Material.BAKED_POTATO,
                Material.BEETROOT, Material.BEETROOT_SEEDS,
                Material.SUGAR_CANE, Material.SUGAR,
                Material.PUMPKIN, Material.MELON, Material.MELON_SLICE,
                Material.COCOA_BEANS)) { m.put(mat, 1); }
        for (Material mat : List.of(
                Material.OAK_LOG, Material.BIRCH_LOG, Material.SPRUCE_LOG, Material.JUNGLE_LOG,
                Material.ACACIA_LOG, Material.DARK_OAK_LOG, Material.MANGROVE_LOG, Material.CHERRY_LOG,
                Material.OAK_PLANKS, Material.BIRCH_PLANKS, Material.SPRUCE_PLANKS, Material.JUNGLE_PLANKS,
                Material.ACACIA_PLANKS, Material.DARK_OAK_PLANKS, Material.MANGROVE_PLANKS,
                Material.BAMBOO, Material.BAMBOO_BLOCK)) { m.put(mat, 2); }
        return Map.copyOf(m);
    }

    private static int categoryOf(Material mat) {
        return CATEGORY.getOrDefault(mat, 99);
    }

    public static void open(Player player, IslandTerritoryState territory, IslandStorage storage, int page) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE);
        fill(inv);
        render(inv, territory, storage, page);
        player.openInventory(inv);
    }

    /** 창고 표시 항목 — 커스텀 재료(customId)·바닐라(material)·흔적 인스턴스(trace) 중 하나. */
    public record Entry(String customId, Material material, long qty,
                        com.poro.rpg.growth.engine.TraceInstance trace) {
        public boolean isCustom() { return customId != null; }
        public boolean isTrace()  { return trace != null; }
    }

    /**
     * 정렬된 표시 항목 (DL-129 추가#29·#38): 흔적 인스턴스(등급↓) → 커스텀 재료(customItems) → 바닐라 Material.
     * 생산물이 창고에 보이지 않던 이중 저장소 불일치를 통합 뷰로 해소 + 흔적 인스턴스 개별 표시.
     */
    public static List<Entry> entries(IslandTerritoryState territory, IslandStorage storage) {
        List<Entry> list = new ArrayList<>();
        if (territory != null) {
            // 흔적 인스턴스 — 등급 내림차순(고등급 우선) 개별 표시 (DL-129 추가#38).
            territory.traceInstancesSnapshot().stream()
                    .sorted((a, b) -> b.grade().ordinal() - a.grade().ordinal())
                    .forEach(t -> list.add(new Entry(null, null, 1, t)));
            territory.customItemsSnapshot().entrySet().stream()
                    .filter(e -> e.getValue() != null && e.getValue() > 0)
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(e -> list.add(new Entry(e.getKey(), null, e.getValue(), null)));
        }
        var materials = new ArrayList<>(storage.materialList());
        materials.sort(Comparator.comparingInt(StorageGui::categoryOf)
                .thenComparingLong((Material m) -> -storage.getAmount(m)));
        for (Material m : materials) list.add(new Entry(null, m, storage.getAmount(m), null));
        return list;
    }

    public static boolean isTitle(Component title) {
        return TITLE.equals(title);
    }

    /** 현재 페이지 번호를 GUI 타이틀에서 추출. -1이면 파싱 실패. */
    public static int pageFromTitle(Component title) {
        // 타이틀에 인코딩하지 않고 PDC나 별도 Map에서 추적하는 방식 사용 → 항상 0으로 fallback
        return 0;
    }

    /** 인벤토리 전체 재렌더 (페이지 변경·입금 후 호출). */
    public static void render(Inventory inv, IslandTerritoryState territory, IslandStorage storage, int page) {
        // 아이템 슬롯 초기화
        var filler = MainHubGui.icon(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < ITEMS_PER_PAGE; i++) inv.setItem(i, filler);

        // 커스텀 재료(생산·드랍) + 바닐라 통합 목록
        List<Entry> entries = entries(territory, storage);
        int totalPages = Math.max(1, (int) Math.ceil((double) entries.size() / ITEMS_PER_PAGE));
        int clampedPage = Math.max(0, Math.min(page, totalPages - 1));

        int start = clampedPage * ITEMS_PER_PAGE;
        int end   = Math.min(start + ITEMS_PER_PAGE, entries.size());
        for (int i = start; i < end; i++) {
            Entry e = entries.get(i);
            ItemStack icon = e.isTrace() ? traceSlot(e.trace())
                    : e.isCustom() ? customSlot(e.customId(), e.qty())
                    : itemSlot(e.material(), e.qty());
            inv.setItem(i - start, icon);
        }
        int materialsCount = entries.size();

        // 이전/다음 페이지
        if (clampedPage > 0) {
            inv.setItem(SLOT_PREV, MainHubGui.icon(Material.ARROW,
                    "§f◀ 이전 페이지", List.of("§7" + clampedPage + "페이지 / " + totalPages + "페이지")));
        } else {
            inv.setItem(SLOT_PREV, filler);
        }
        if (clampedPage < totalPages - 1) {
            inv.setItem(SLOT_NEXT, MainHubGui.icon(Material.ARROW,
                    "§f다음 페이지 ▶", List.of("§7" + (clampedPage + 2) + "페이지 / " + totalPages + "페이지")));
        } else {
            inv.setItem(SLOT_NEXT, filler);
        }

        // 페이지 정보
        inv.setItem(SLOT_PAGE_INFO, MainHubGui.icon(Material.PAPER,
                "§f" + (clampedPage + 1) + " / " + totalPages + " 페이지",
                List.of("§7총 §f" + materialsCount + "§7종 보관 중")));

        // 전체 입금
        inv.setItem(SLOT_DEPOSIT_ALL, MainHubGui.icon(Material.CHEST,
                "§e전체 입금",
                List.of("§7인벤토리의 모든 아이템을", "§7창고에 입금합니다.", "§7", "§f[클릭]")));

        inv.setItem(SLOT_BACK, MainHubGui.icon(Material.DARK_OAK_DOOR, "§7◀ 뒤로", List.of("§7영지 메뉴")));
    }

    // ─── internal ────────────────────────────────────────────────

    /** 커스텀 재료 슬롯 — CMD 모델 또는 폴백 바닐라 아이콘 + 한글명 + 보유량/출금 안내 (DL-129 추가#29·#31). */
    private static ItemStack customSlot(String itemId, long qty) {
        ItemStack item = CustomItemModel.buildStack(itemId, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.lore(List.of(
                    Component.text("§7보유량: §f" + FMT.format(qty) + "개"),
                    Component.text("§7──────────"),
                    Component.text("§8생산·드랍 재료 — 공방·상점·경매장에서 사용"),
                    Component.text("§7──────────"),
                    Component.text("§7좌클릭  §f64개 출금"),
                    Component.text("§7우클릭  §f1개 출금"),
                    Component.text("§7Shift+클릭  §f256개 출금")
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    /** 흔적 인스턴스 슬롯 — 등급 색상명 + 세부스탯 lore. 표시 전용(전승/경매에서 사용, 물리 출금 없음). DL-129 추가#38. */
    private static ItemStack traceSlot(com.poro.rpg.growth.engine.TraceInstance trace) {
        ItemStack item = CustomItemModel.buildStack("equip_trace_unidentified", 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String color = EquipmentLoreRenderer.gradeColor(trace.grade());
            meta.displayName(Component.text(color + trace.grade().displayName() + " 장비의 흔적")
                    .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("§7등급: " + color + trace.grade().displayName()));
            if (trace.substats().isEmpty()) {
                lore.add(Component.text("§8세부스탯: 없음"));
            } else {
                lore.add(Component.text("§7세부스탯:"));
                trace.substats().forEach(l -> lore.add(Component.text(
                        "§7  " + EquipmentLoreRenderer.potentialOptionKr(l.optionCode())
                                + " §e+" + String.format("%.1f", l.value()))));
            }
            lore.add(Component.text("§7──────────"));
            lore.add(Component.text("§8전승 GUI에서 장비에 적용 / 경매장에서 거래"));
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack itemSlot(Material mat, long qty) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        // 번역키 기반 — 클라 로케일(한국어)로 자연스럽게 표시. italic 제거.
        meta.displayName(Component.translatable(mat.translationKey())
                .color(net.kyori.adventure.text.format.NamedTextColor.WHITE)
                .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));
        meta.lore(List.of(
                Component.text("§7보유량: §f" + FMT.format(qty) + "개"),
                Component.text("§7──────────"),
                Component.text("§8공방·상점·경매장에서 직접 사용 가능"),
                Component.text("§7──────────"),
                Component.text("§7좌클릭  §f64개 출금"),
                Component.text("§7우클릭  §f1개 출금"),
                Component.text("§7Shift+클릭  §f256개 출금")
        ));
        item.setItemMeta(meta);
        return item;
    }

    private static String formatName(Material mat) {
        String raw = mat.name().replace('_', ' ');
        StringBuilder sb = new StringBuilder(raw.length());
        boolean cap = true;
        for (char c : raw.toCharArray()) {
            sb.append(cap ? Character.toUpperCase(c) : Character.toLowerCase(c));
            cap = c == ' ';
        }
        return sb.toString();
    }

    private static void fill(Inventory inv) {
        var filler = MainHubGui.icon(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 45; i < 54; i++) inv.setItem(i, filler);
    }
}
