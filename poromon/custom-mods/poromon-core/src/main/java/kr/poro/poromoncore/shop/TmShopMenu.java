package kr.poro.poromoncore.shop;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.moves.Move;
import com.cobblemon.mod.common.api.moves.MoveSet;
import com.cobblemon.mod.common.api.moves.MoveTemplate;
import com.cobblemon.mod.common.api.moves.Moves;
import com.cobblemon.mod.common.pokemon.Pokemon;
import kr.poro.poromoncore.config.ConfigManager;
import kr.poro.poromoncore.config.EconomyConfig.TmShopConfig;
import kr.poro.poromoncore.data.PoroMonState;
import kr.poro.poromoncore.economy.EconomyBridge;
import kr.poro.poromoncore.item.MakeoverStone;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 마개조 기술머신 상점 (결정 033). 타입 그리드 → 타입별/검색 목록 → SimpleTMs TM 구매.
 * 가격 = 위력 자동 등급(EconomyConfig.tmShop). learnset 해제는 simpletms config(전역).
 * 구매한 TM을 원하는 포켓몬에 사용 = 그 포켓몬만 학습.
 */
public final class TmShopMenu {
    private TmShopMenu() {}

    private static final int PER_PAGE = ShopLayout.CONTENT_SLOTS.length; // 28
    private static final int SEARCH_SLOT = 53;
    private static final int STONE_SLOT = 45;     // 해금석 구매(구매 모드)
    private static final int MAKEOVER_SLOT = 47;  // 마개조 각인 진입(구매 모드)

    /** 마개조 각인 대상: player → pokemon UUID (해금석 우클릭 시 설정). 비어있으면 구매 모드. */
    private static final Map<UUID, UUID> TEACH = new HashMap<>();
    /** 풀 무브셋 교체 대기: player → 각인할 기술명. */
    private static final Map<UUID, String> PENDING = new HashMap<>();

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

    /** 해금석 우클릭 → 대상 포켓몬 마개조 각인 모드로 진입. */
    public static void openTeach(ServerPlayerEntity player, UUID pokemonUuid) {
        TEACH.put(player.getUuid(), pokemonUuid);
        ServerMenuHandler.show(player, Text.literal("마개조 — 기술 선택").formatted(Formatting.LIGHT_PURPLE),
                inv -> gridPopulate(inv, player), TmShopMenu::gridClick);
    }

    private static boolean teachMode(ServerPlayerEntity player) {
        return TEACH.containsKey(player.getUuid());
    }

    private static void gridPopulate(Inventory inv, ServerPlayerEntity player) {
        for (int i = 0; i < ServerMenuHandler.DISPLAY_SIZE; i++) inv.setStack(i, MenuIcons.pane());
        boolean teach = teachMode(player);
        inv.setStack(ShopLayout.BALANCE_SLOT, MenuIcons.icon(Items.GOLD_NUGGET,
                "§6잔액: " + EconomyBridge.getBalance(player) + " " + ConfigManager.economy().currencyDisplay,
                teach ? List.of("§d마개조 각인 모드", "§7타입/검색으로 각인할 기술 선택")
                      : List.of("§7타입을 골라 기술머신 구매", "§7마개조는 해금석으로 포켓몬에 직접 각인")));
        inv.setStack(ShopLayout.BACK_SLOT, MenuIcons.icon(Items.ARROW,
                teach ? "§c취소" : "§e← 메뉴로", List.of()));
        inv.setStack(SEARCH_SLOT, MenuIcons.icon(Items.OAK_SIGN, "§b검색",
                List.of("§7기술 이름으로 검색", "§e클릭 — 검색어 입력")));
        if (!teach) {
            TmShopConfig cfg = ConfigManager.economy().tmShop;
            inv.setStack(STONE_SLOT, MenuIcons.icon(Items.PAPER, "§d마개조 해금석",
                    List.of("§7포켓몬에 우클릭 → 그 포켓몬 영구 마개조 해제",
                            "§7가격: §6" + cfg.makeoverStonePrice + " " + ConfigManager.economy().currencyDisplay,
                            "§7배지 " + cfg.makeoverStoneBadges + "개 필요", "§e클릭 — 구매")));
            inv.setStack(MAKEOVER_SLOT, MenuIcons.icon(Items.ENDER_EYE, "§d마개조 각인",
                    List.of("§7마개조된 포켓몬에 배울 수 없는 기술 각인", "§7각인마다 골드(위력 등급가)", "§e클릭 — 포켓몬 선택")));
        }

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
        if (slot == ShopLayout.BACK_SLOT) {
            if (teachMode(player)) { TEACH.remove(player.getUuid()); player.closeHandledScreen(); }
            else MenuGuiManager.open(player);
            return;
        }
        if (slot == SEARCH_SLOT) { promptSearch(player); return; }
        if (slot == STONE_SLOT && !teachMode(player)) { buyStone(player); return; }
        if (slot == MAKEOVER_SLOT && !teachMode(player)) { openPokemonSelect(player); return; }
        int idx = ShopLayout.contentIndexOf(slot);
        if (idx < 0 || idx >= TmCatalog.TYPES.length) return;
        showList(player, "type:" + TmCatalog.TYPES[idx], 0);
    }

    private static void buyStone(ServerPlayerEntity player) {
        TmShopConfig cfg = ConfigManager.economy().tmShop;
        if (badgeCount(player) < cfg.makeoverStoneBadges) {
            player.sendMessage(Text.literal("§c[기술머신] 해금석은 배지 " + cfg.makeoverStoneBadges + "개가 필요합니다."), true);
            return;
        }
        if (!EconomyBridge.withdraw(player, cfg.makeoverStonePrice, "makeover_stone")) {
            player.sendMessage(Text.literal("§c[기술머신] 골드가 부족합니다 (필요 " + cfg.makeoverStonePrice + ")."), true);
            return;
        }
        ItemStack stone = MakeoverStone.create();
        player.getInventory().insertStack(stone);
        if (!stone.isEmpty()) {
            EconomyBridge.deposit(player, cfg.makeoverStonePrice, "makeover_stone_refund");
            player.sendMessage(Text.literal("§e[기술머신] 인벤토리 공간 부족 — 환불."), true);
            return;
        }
        player.sendMessage(Text.literal("§a[기술머신] 마개조 해금석 구매 (-" + cfg.makeoverStonePrice + "). 포켓몬에 우클릭하세요."), true);
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

        boolean teach = teachMode(player);
        int start = page * PER_PAGE;
        for (int i = 0; i < PER_PAGE && start + i < list.size(); i++) {
            TmCatalog.Entry e = list.get(start + i);
            Item item = resolve(e.itemId());
            if (item == null) continue;
            long price = cfg.priceFor(e.power());
            String powTxt = e.power() <= 0 ? "변화" : String.valueOf((int) e.power());
            if (teach) {
                inv.setStack(ShopLayout.CONTENT_SLOTS[i], MenuIcons.icon(item,
                        "§d" + e.displayName(), List.of("§7위력: §f" + powTxt, "§a클릭 — 이 기술 각인")));
            } else if (badges < cfg.minBadges) {
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
        if (slot == ShopLayout.BACK_SLOT) {
            if (teachMode(player)) openTeach(player, TEACH.get(player.getUuid())); else open(player);
            return;
        }
        if (slot == ShopLayout.PREV_SLOT) { showList(player, source, page - 1); return; }
        if (slot == ShopLayout.NEXT_SLOT) { showList(player, source, page + 1); return; }
        int idx = ShopLayout.contentIndexOf(slot);
        if (idx < 0) return;
        List<TmCatalog.Entry> list = entriesFor(source);
        int globalIdx = page * PER_PAGE + idx;
        if (globalIdx >= list.size()) return;
        TmCatalog.Entry e = list.get(globalIdx);
        if (teachMode(player)) { teachMove(player, e.moveName(), e.displayName()); return; }
        TmShopConfig cfg = ConfigManager.economy().tmShop;
        if (badgeCount(player) < cfg.minBadges) {
            player.sendMessage(Text.literal("§c[기술머신] 배지 " + cfg.minBadges + "개가 필요합니다."), true);
            return;
        }
        buy(player, e);
        showList(player, source, page);
    }

    // ===== 마개조 각인(메뉴 진입) =====
    /** 마개조 각인: 파티 포켓몬 선택(마개조된 것만 클릭 가능). */
    public static void openPokemonSelect(ServerPlayerEntity player) {
        ServerMenuHandler.show(player, Text.literal("마개조 각인 — 포켓몬 선택").formatted(Formatting.LIGHT_PURPLE),
                inv -> {
                    for (int i = 0; i < ServerMenuHandler.DISPLAY_SIZE; i++) inv.setStack(i, MenuIcons.pane());
                    inv.setStack(ShopLayout.BALANCE_SLOT, MenuIcons.icon(Items.PAPER, "§d마개조 각인",
                            List.of("§7해금석으로 마개조된 포켓몬만 선택 가능", "§7각인마다 골드(위력 등급가)")));
                    inv.setStack(ShopLayout.BACK_SLOT, MenuIcons.icon(Items.ARROW, "§e← 기술머신", List.of()));
                    int[] slots = {11, 12, 13, 14, 15, 16};
                    int i = 0;
                    for (Pokemon pk : Cobblemon.INSTANCE.getStorage().getParty(player)) {
                        if (pk == null || i >= slots.length) { i++; continue; }
                        boolean mo = MakeoverService.isMakeover(player, pk);
                        inv.setStack(slots[i], MenuIcons.icon(mo ? Items.ENDER_EYE : Items.IRON_BARS,
                                (mo ? "§d" : "§8") + pk.getSpecies().getName() + " §7Lv." + pk.getLevel(),
                                mo ? List.of("§a클릭 — 기술 각인") : List.of("§c미해제 §7(해금석 필요)")));
                        i++;
                    }
                },
                (p, slot, button, shift) -> {
                    if (slot == ShopLayout.BACK_SLOT) { open(p); return; }
                    int[] slots = {11, 12, 13, 14, 15, 16};
                    int idx = -1;
                    for (int k = 0; k < slots.length; k++) if (slots[k] == slot) { idx = k; break; }
                    if (idx < 0) return;
                    int i = 0;
                    for (Pokemon pk : Cobblemon.INSTANCE.getStorage().getParty(p)) {
                        if (i == idx) {
                            if (pk != null && MakeoverService.isMakeover(p, pk)) openTeach(p, pk.getUuid());
                            else p.sendMessage(Text.literal("§c[마개조] 해금석으로 먼저 해제하세요."), true);
                            return;
                        }
                        i++;
                    }
                });
    }

    private static void teachMove(ServerPlayerEntity player, String moveName, String displayName) {
        UUID pid = TEACH.get(player.getUuid());
        Pokemon pk = MakeoverService.findPartyPokemon(player, pid);
        if (pk == null) {
            player.sendMessage(Text.literal("§c[마개조] 대상 포켓몬을 파티에서 찾을 수 없습니다."), true);
            TEACH.remove(player.getUuid()); player.closeHandledScreen(); return;
        }
        if (!MakeoverService.isMakeover(player, pk)) {
            player.sendMessage(Text.literal("§c[마개조] 해제되지 않은 포켓몬입니다."), true);
            TEACH.remove(player.getUuid()); player.closeHandledScreen(); return;
        }
        MoveTemplate tpl = Moves.getByName(moveName);
        if (tpl == null) { player.sendMessage(Text.literal("§c[마개조] 알 수 없는 기술."), true); return; }
        MoveSet ms = pk.getMoveSet();
        if (ms.hasSpace()) {
            long price = ConfigManager.economy().tmShop.priceFor(tpl.getPower());
            if (!EconomyBridge.withdraw(player, price, "makeover:" + moveName)) {
                player.sendMessage(Text.literal("§c[마개조] 골드가 부족합니다 (필요 " + price + ")."), true);
                return;
            }
            List<Move> withNulls = ms.getMovesWithNulls();
            int free = -1;
            for (int i = 0; i < withNulls.size(); i++) if (withNulls.get(i) == null) { free = i; break; }
            if (free < 0) free = 0;
            ms.setMove(free, tpl.create());
            finishTeach(player, pk, displayName, price);
        } else {
            PENDING.put(player.getUuid(), moveName);
            openSlotSelect(player, displayName);
        }
    }

    /** 무브셋 가득 → 교체할 슬롯 선택(확정 시 골드 차감). */
    private static void openSlotSelect(ServerPlayerEntity player, String displayName) {
        ServerMenuHandler.show(player, Text.literal("마개조 — 교체할 기술").formatted(Formatting.LIGHT_PURPLE),
                inv -> {
                    for (int i = 0; i < ServerMenuHandler.DISPLAY_SIZE; i++) inv.setStack(i, MenuIcons.pane());
                    inv.setStack(ShopLayout.BALANCE_SLOT, MenuIcons.icon(Items.PAPER,
                            "§d각인: " + displayName, List.of("§7교체할 기존 기술을 클릭하세요")));
                    inv.setStack(ShopLayout.BACK_SLOT, MenuIcons.icon(Items.ARROW, "§c취소", List.of()));
                    Pokemon pk = MakeoverService.findPartyPokemon(player, TEACH.get(player.getUuid()));
                    if (pk != null) {
                        List<Move> moves = pk.getMoveSet().getMoves();
                        int[] slots = {20, 21, 22, 23};
                        for (int i = 0; i < moves.size() && i < 4; i++) {
                            Move mv = moves.get(i);
                            inv.setStack(slots[i], MenuIcons.icon(Items.PAPER,
                                    "§f" + mv.getTemplate().getDisplayName().getString(),
                                    List.of("§7클릭 — 이 기술을 교체")));
                        }
                    }
                },
                (p, slot, button, shift) -> {
                    if (slot == ShopLayout.BACK_SLOT) { PENDING.remove(p.getUuid()); openTeach(p, TEACH.get(p.getUuid())); return; }
                    int[] slots = {20, 21, 22, 23};
                    int idx = -1;
                    for (int i = 0; i < slots.length; i++) if (slots[i] == slot) { idx = i; break; }
                    if (idx < 0) return;
                    String mn = PENDING.get(p.getUuid());
                    Pokemon pk = MakeoverService.findPartyPokemon(p, TEACH.get(p.getUuid()));
                    MoveTemplate tpl = mn == null ? null : Moves.getByName(mn);
                    if (pk == null || tpl == null) { p.closeHandledScreen(); return; }
                    if (idx >= pk.getMoveSet().getMoves().size()) return;
                    long price = ConfigManager.economy().tmShop.priceFor(tpl.getPower());
                    if (!EconomyBridge.withdraw(p, price, "makeover:" + mn)) {
                        p.sendMessage(Text.literal("§c[마개조] 골드가 부족합니다 (필요 " + price + ")."), true);
                        return;
                    }
                    pk.getMoveSet().setMove(idx, tpl.create());
                    PENDING.remove(p.getUuid());
                    finishTeach(p, pk, displayName, price);
                });
    }

    private static void finishTeach(ServerPlayerEntity player, Pokemon pk, String displayName, long price) {
        UUID pid = TEACH.get(player.getUuid());
        player.sendMessage(Text.literal("§a[마개조] " + pk.getSpecies().getName()
                + " 에게 " + displayName + " 각인 완료 (-" + price + ")."), false);
        if (pid != null) openTeach(player, pid); // 같은 포켓몬에 계속 각인 가능
        else player.closeHandledScreen();
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
