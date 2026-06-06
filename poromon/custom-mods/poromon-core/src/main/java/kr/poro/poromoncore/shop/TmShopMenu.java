package kr.poro.poromoncore.shop;

import kr.poro.poromoncore.config.ConfigManager;
import kr.poro.poromoncore.config.EconomyConfig.TmShopConfig;
import kr.poro.poromoncore.data.PoroMonState;
import kr.poro.poromoncore.economy.EconomyBridge;
import kr.poro.poromoncore.menu.MenuGuiManager;
import kr.poro.poromoncore.menu.MenuIcons;
import kr.poro.poromoncore.menu.ServerMenuHandler;
import kr.poro.poromoncore.util.ChatInputManager;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.Map;

/**
 * 마개조 기술머신 상점 (결정 033). 타입 그리드 → 타입별/검색 목록 → SimpleTMs TM 구매.
 * 가격 = 위력 자동 등급(EconomyConfig.tmShop). learnset 해제는 simpletms config(전역).
 * 구매한 TM을 원하는 포켓몬에 사용 = 그 포켓몬만 학습.
 */
public final class TmShopMenu {
    private TmShopMenu() {}

    private static final int PER_PAGE = ShopLayout.CONTENT_SLOTS.length; // 28
    private static final int SEARCH_SLOT = 53;

    private static final Map<String, String> TYPE_KO = Map.ofEntries(
            Map.entry("normal", "노말"), Map.entry("fire", "불꽃"), Map.entry("water", "물"),
            Map.entry("electric", "전기"), Map.entry("grass", "풀"), Map.entry("ice", "얼음"),
            Map.entry("fighting", "격투"), Map.entry("poison", "독"), Map.entry("ground", "땅"),
            Map.entry("flying", "비행"), Map.entry("psychic", "에스퍼"), Map.entry("bug", "벌레"),
            Map.entry("rock", "바위"), Map.entry("ghost", "고스트"), Map.entry("dragon", "드래곤"),
            Map.entry("dark", "악"), Map.entry("steel", "강철"), Map.entry("fairy", "페어리"));

    // ===== 타입 그리드 =====
    public static void open(ServerPlayerEntity player) {
        ServerMenuHandler.show(player, Text.literal("기술머신 — 타입 선택").formatted(Formatting.AQUA),
                inv -> gridPopulate(inv, player), TmShopMenu::gridClick);
    }

    private static void gridPopulate(Inventory inv, ServerPlayerEntity player) {
        for (int i = 0; i < ServerMenuHandler.DISPLAY_SIZE; i++) inv.setStack(i, MenuIcons.pane());
        inv.setStack(ShopLayout.BALANCE_SLOT, MenuIcons.icon(Items.GOLD_NUGGET,
                "§6잔액: " + EconomyBridge.getBalance(player) + " " + ConfigManager.economy().currencyDisplay,
                List.of("§7타입을 골라 기술머신 구매", "§7TM을 포켓몬에 사용 = 그 포켓몬만 학습")));
        inv.setStack(ShopLayout.BACK_SLOT, MenuIcons.icon(Items.ARROW, "§e← 메뉴로", List.of()));
        inv.setStack(SEARCH_SLOT, MenuIcons.icon(Items.OAK_SIGN, "§b검색",
                List.of("§7기술 이름으로 검색", "§e클릭 — 검색어 입력")));

        for (int i = 0; i < TmCatalog.TYPES.length && i < ShopLayout.CONTENT_SLOTS.length; i++) {
            String type = TmCatalog.TYPES[i];
            int count = TmCatalog.ofType(type).size();
            String ko = TYPE_KO.getOrDefault(type, type);
            inv.setStack(ShopLayout.CONTENT_SLOTS[i], MenuIcons.icon(typeIcon(type),
                    "§b" + ko + " §f타입",
                    List.of("§7" + ko + " 타입 기술머신이 모여 있습니다", "§7기술 §f" + count + "종", "§e클릭 — 목록 열기")));
        }
    }

    /** 타입별 양털 아이콘(타입 색감). TM 아이템을 쓰면 SimpleTMs 기술 설명이 딸려와 사용 안 함. */
    private static Item typeIcon(String type) {
        return switch (type) {
            case "fire" -> Items.RED_WOOL;
            case "water" -> Items.BLUE_WOOL;
            case "electric" -> Items.YELLOW_WOOL;
            case "grass" -> Items.LIME_WOOL;
            case "ice" -> Items.LIGHT_BLUE_WOOL;
            case "fighting" -> Items.ORANGE_WOOL;
            case "poison" -> Items.PURPLE_WOOL;
            case "ground" -> Items.BROWN_WOOL;
            case "flying" -> Items.LIGHT_GRAY_WOOL;
            case "psychic" -> Items.PINK_WOOL;
            case "bug" -> Items.GREEN_WOOL;
            case "rock" -> Items.GRAY_WOOL;
            case "ghost" -> Items.MAGENTA_WOOL;
            case "dragon" -> Items.CYAN_WOOL;
            case "dark" -> Items.BLACK_WOOL;
            case "steel" -> Items.IRON_BLOCK;
            case "fairy" -> Items.PINK_WOOL;
            default -> Items.WHITE_WOOL; // normal
        };
    }

    private static void gridClick(ServerPlayerEntity player, int slot, int button, boolean shift) {
        if (slot == ShopLayout.BACK_SLOT) { MenuGuiManager.open(player); return; }
        if (slot == SEARCH_SLOT) { promptSearch(player); return; }
        int idx = ShopLayout.contentIndexOf(slot);
        if (idx < 0 || idx >= TmCatalog.TYPES.length) return;
        showList(player, "type:" + TmCatalog.TYPES[idx], 0);
    }

    private static void promptSearch(ServerPlayerEntity player) {
        player.closeHandledScreen();
        player.sendMessage(Text.literal("§b[기술머신] 검색할 기술 이름을 채팅에 입력하세요. §7(취소: '취소')"), false);
        ChatInputManager.await(player, msg -> {
            if (msg.equals("취소") || msg.isBlank()) { open(player); return; }
            showList(player, "q:" + msg.trim(), 0);
        });
    }

    // ===== 목록(타입 / 검색) =====
    private static List<TmCatalog.Entry> entriesFor(String source) {
        if (source.startsWith("type:")) return TmCatalog.ofType(source.substring(5));
        if (source.startsWith("q:")) return TmCatalog.search(source.substring(2));
        return List.of();
    }

    private static String titleFor(String source) {
        if (source.startsWith("type:")) return TYPE_KO.getOrDefault(source.substring(5), source.substring(5)) + " 타입 기술머신";
        if (source.startsWith("q:")) return "검색: " + source.substring(2);
        return "기술머신";
    }

    private static void showList(ServerPlayerEntity player, String source, int page) {
        ServerMenuHandler.show(player, Text.literal(titleFor(source)).formatted(Formatting.AQUA),
                inv -> listPopulate(inv, player, source, page),
                (p, slot, button, shift) -> listClick(p, slot, source, page));
    }

    private static int badgeCount(ServerPlayerEntity player) {
        return PoroMonState.get(player.getServer()).getOrCreate(player.getUuid()).badges.size();
    }

    private static void listPopulate(Inventory inv, ServerPlayerEntity player, String source, int page) {
        for (int i = 0; i < ServerMenuHandler.DISPLAY_SIZE; i++) inv.setStack(i, MenuIcons.pane());
        String unit = ConfigManager.economy().currencyDisplay;
        TmShopConfig cfg = ConfigManager.economy().tmShop;
        int badges = badgeCount(player);
        List<TmCatalog.Entry> list = entriesFor(source);
        int pages = Math.max(1, (list.size() + PER_PAGE - 1) / PER_PAGE);
        page = Math.max(0, Math.min(page, pages - 1));

        inv.setStack(ShopLayout.BALANCE_SLOT, MenuIcons.icon(Items.GOLD_NUGGET,
                "§6잔액: " + EconomyBridge.getBalance(player) + " " + unit,
                List.of("§7기술 §f" + list.size() + "종", "§7보유 배지: §f" + badges,
                        "§7페이지 §f" + (page + 1) + " / " + pages)));
        inv.setStack(ShopLayout.BACK_SLOT, MenuIcons.icon(Items.ARROW, "§e← 타입 선택", List.of()));
        if (page > 0) inv.setStack(ShopLayout.PREV_SLOT, MenuIcons.icon(Items.SPECTRAL_ARROW, "§e◀ 이전", List.of()));
        if (page < pages - 1) inv.setStack(ShopLayout.NEXT_SLOT, MenuIcons.icon(Items.SPECTRAL_ARROW, "§e다음 ▶", List.of()));

        int start = page * PER_PAGE;
        for (int i = 0; i < PER_PAGE && start + i < list.size(); i++) {
            TmCatalog.Entry e = list.get(start + i);
            Item item = resolve(e.itemId());
            if (item == null) continue;
            long price = cfg.priceFor(e.power());
            String powTxt = e.power() <= 0 ? "변화" : String.valueOf((int) e.power());
            if (badges < cfg.minBadges) {
                inv.setStack(ShopLayout.CONTENT_SLOTS[i], MenuIcons.icon(Items.IRON_BARS,
                        "§8" + e.displayName() + " §7(잠김)",
                        List.of("§7위력: §f" + powTxt, "§7가격: §6" + price + " " + unit,
                                "§c배지 " + cfg.minBadges + "개 필요")));
            } else {
                inv.setStack(ShopLayout.CONTENT_SLOTS[i], MenuIcons.icon(item,
                        "§f" + e.displayName() + " §7TM",
                        List.of("§7위력: §f" + powTxt, "§7가격: §6" + price + " " + unit,
                                "§a클릭 — 구매")));
            }
        }
    }

    private static void listClick(ServerPlayerEntity player, int slot, String source, int page) {
        if (slot == ShopLayout.BACK_SLOT) { open(player); return; }
        if (slot == ShopLayout.PREV_SLOT) { showList(player, source, page - 1); return; }
        if (slot == ShopLayout.NEXT_SLOT) { showList(player, source, page + 1); return; }
        int idx = ShopLayout.contentIndexOf(slot);
        if (idx < 0) return;
        List<TmCatalog.Entry> list = entriesFor(source);
        int globalIdx = page * PER_PAGE + idx;
        if (globalIdx >= list.size()) return;
        TmCatalog.Entry e = list.get(globalIdx);
        TmShopConfig cfg = ConfigManager.economy().tmShop;
        if (badgeCount(player) < cfg.minBadges) {
            player.sendMessage(Text.literal("§c[기술머신] 배지 " + cfg.minBadges + "개가 필요합니다."), true);
            return;
        }
        buy(player, e);
        showList(player, source, page);
    }

    private static void buy(ServerPlayerEntity player, TmCatalog.Entry e) {
        Item item = resolve(e.itemId());
        if (item == null) { player.sendMessage(Text.literal("§c[기술머신] 알 수 없는 TM."), true); return; }
        long price = ConfigManager.economy().tmShop.priceFor(e.power());
        if (!EconomyBridge.withdraw(player, price, "tm:" + e.moveName())) {
            player.sendMessage(Text.literal("§c[기술머신] 골드가 부족합니다 (필요 " + price + ")."), true);
            return;
        }
        ItemStack stack = new ItemStack(item, 1);
        player.getInventory().insertStack(stack);
        if (!stack.isEmpty()) {
            EconomyBridge.deposit(player, price, "tm_refund:" + e.moveName());
            player.sendMessage(Text.literal("§e[기술머신] 인벤토리 공간 부족 — 환불."), true);
            return;
        }
        player.sendMessage(Text.literal("§a[기술머신] " + e.displayName() + " TM 구매 (-" + price + ")."), true);
    }

    private static Item resolve(String itemId) {
        Identifier id = Identifier.tryParse(itemId);
        if (id == null || !Registries.ITEM.containsId(id)) return null;
        return Registries.ITEM.get(id);
    }
}
