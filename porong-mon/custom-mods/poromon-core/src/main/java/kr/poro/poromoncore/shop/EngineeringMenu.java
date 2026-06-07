package kr.poro.poromoncore.shop;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.abilities.Abilities;
import com.cobblemon.mod.common.api.abilities.Ability;
import com.cobblemon.mod.common.api.abilities.AbilityTemplate;
import com.cobblemon.mod.common.api.moves.Move;
import com.cobblemon.mod.common.api.moves.MoveSet;
import com.cobblemon.mod.common.api.moves.MoveTemplate;
import com.cobblemon.mod.common.api.moves.Moves;
import com.cobblemon.mod.common.pokemon.Pokemon;
import kr.poro.poromoncore.config.ConfigManager;
import kr.poro.poromoncore.config.EconomyConfig.EngineeringConfig;
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
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 포로공학 (결정 033-a). 정수 구매 + off-learnset 기술 각인.
 * 정수를 포켓몬에 우클릭 → 영구 해제(PoroMonCore). 여기선 해제된 포켓몬에 기술 각인(각인마다 골드).
 * 기술 선택은 18타입 카테고리 + 검색. 일반 learnset 기술은 별도 '기술머신 상점'.
 */
public final class EngineeringMenu {
    private EngineeringMenu() {}

    private static final int PER_PAGE = ShopLayout.CONTENT_SLOTS.length;
    private static final int SEARCH_SLOT = 53;
    private static final int STONE_SLOT = 20;          // 정수·기술머신 구매
    private static final int ENGRAVE_SLOT = 24;        // 기술 각인
    private static final int ABILITY_STONE_SLOT = 29;  // 정수·특성 구매 (결정 034)
    private static final int ABILITY_SLOT = 33;        // 특성 변경 (결정 034)

    /** 각인 대상: player → pokemon UUID. */
    private static final Map<UUID, UUID> TARGET = new HashMap<>();
    /** 풀 무브셋 교체 대기: player → 각인할 기술명. */
    private static final Map<UUID, String> PENDING = new HashMap<>();

    private static final Map<String, String> TYPE_KO = Map.ofEntries(
            Map.entry("normal", "노말"), Map.entry("fire", "불꽃"), Map.entry("water", "물"),
            Map.entry("electric", "전기"), Map.entry("grass", "풀"), Map.entry("ice", "얼음"),
            Map.entry("fighting", "격투"), Map.entry("poison", "독"), Map.entry("ground", "땅"),
            Map.entry("flying", "비행"), Map.entry("psychic", "에스퍼"), Map.entry("bug", "벌레"),
            Map.entry("rock", "바위"), Map.entry("ghost", "고스트"), Map.entry("dragon", "드래곤"),
            Map.entry("dark", "악"), Map.entry("steel", "강철"), Map.entry("fairy", "페어리"));

    private static int badgeCount(ServerPlayerEntity p) {
        return PoroMonState.get(p.getServer()).getOrCreate(p.getUuid()).badges.size();
    }

    // ===== 메인 =====
    public static void open(ServerPlayerEntity player) {
        ServerMenuHandler.show(player, Text.literal("포로공학").formatted(Formatting.LIGHT_PURPLE),
                inv -> {
                    for (int i = 0; i < ServerMenuHandler.DISPLAY_SIZE; i++) inv.setStack(i, MenuIcons.pane());
                    EngineeringConfig cfg = ConfigManager.economy().engineering;
                    String unit = ConfigManager.economy().currencyDisplay;
                    inv.setStack(ShopLayout.BALANCE_SLOT, MenuIcons.icon(Items.GOLD_NUGGET,
                            "§6잔액: " + EconomyBridge.getBalance(player) + " " + unit,
                            List.of("§7정수로 포켓몬을 해제 → 기술 각인 / 특성 변경")));
                    inv.setStack(ShopLayout.BACK_SLOT, MenuIcons.icon(Items.ARROW, "§e← 메뉴로", List.of()));
                    // 기술 트랙
                    inv.setStack(STONE_SLOT, MenuIcons.iconModel(Items.PAPER, 82030, "§d포로공학 정수 · 기술머신",
                            List.of("§7포켓몬에 우클릭 → 그 포켓몬 영구 해제",
                                    "§7가격: §6" + cfg.stonePrice + " " + unit,
                                    "§7배지 " + cfg.stoneBadges + "개 필요", "§e클릭 — 구매")));
                    inv.setStack(ENGRAVE_SLOT, MenuIcons.icon(Items.ENDER_EYE, "§d기술 각인",
                            List.of("§7해제된 포켓몬에 배울 수 없는 기술 각인", "§7각인마다 골드(위력 등급가)",
                                    "§e클릭 — 포켓몬 선택")));
                    // 특성 트랙 (결정 034)
                    inv.setStack(ABILITY_STONE_SLOT, MenuIcons.iconModel(Items.PAPER, 82031, "§d포로공학 정수 · 특성",
                            List.of("§7포켓몬에 우클릭 → 그 포켓몬 영구 해제",
                                    "§7가격: §6" + cfg.abilityStonePrice + " " + unit,
                                    "§7배지 " + cfg.abilityStoneBadges + "개 필요", "§e클릭 — 구매")));
                    inv.setStack(ABILITY_SLOT, MenuIcons.icon(Items.NETHER_STAR, "§d특성 변경",
                            List.of("§7해제된 포켓몬에 어떤 특성이든 강제 부여", "§7변경마다 §6" + cfg.abilityChangePrice + " " + unit,
                                    "§e클릭 — 포켓몬 선택")));
                },
                (p, slot, button, shift) -> {
                    if (slot == ShopLayout.BACK_SLOT) MenuGuiManager.open(p);
                    else if (slot == STONE_SLOT) buyStone(p);
                    else if (slot == ENGRAVE_SLOT) openPokemonSelect(p);
                    else if (slot == ABILITY_STONE_SLOT) buyAbilityStone(p);
                    else if (slot == ABILITY_SLOT) openAbilityPokemonSelect(p);
                });
    }

    private static void buyStone(ServerPlayerEntity player) {
        EngineeringConfig cfg = ConfigManager.economy().engineering;
        if (badgeCount(player) < cfg.stoneBadges) {
            player.sendMessage(Text.literal("§c[포로공학] 정수는 배지 " + cfg.stoneBadges + "개가 필요합니다."), true);
            return;
        }
        if (!EconomyBridge.withdraw(player, cfg.stonePrice, "engineering_stone")) {
            player.sendMessage(Text.literal("§c[포로공학] 골드가 부족합니다 (필요 " + cfg.stonePrice + ")."), true);
            return;
        }
        ItemStack stone = MakeoverStone.create(MakeoverStone.Kind.TECH);
        player.getInventory().insertStack(stone);
        if (!stone.isEmpty()) {
            EconomyBridge.deposit(player, cfg.stonePrice, "engineering_stone_refund");
            player.sendMessage(Text.literal("§e[포로공학] 인벤토리 공간 부족 — 환불."), true);
            return;
        }
        player.sendMessage(Text.literal("§a[포로공학] 정수·기술머신 구매 (-" + cfg.stonePrice + "). 포켓몬에 우클릭하세요."), true);
    }

    private static void buyAbilityStone(ServerPlayerEntity player) {
        EngineeringConfig cfg = ConfigManager.economy().engineering;
        if (badgeCount(player) < cfg.abilityStoneBadges) {
            player.sendMessage(Text.literal("§c[포로공학] 특성 정수는 배지 " + cfg.abilityStoneBadges + "개가 필요합니다."), true);
            return;
        }
        if (!EconomyBridge.withdraw(player, cfg.abilityStonePrice, "engineering_ability_stone")) {
            player.sendMessage(Text.literal("§c[포로공학] 골드가 부족합니다 (필요 " + cfg.abilityStonePrice + ")."), true);
            return;
        }
        ItemStack stone = MakeoverStone.create(MakeoverStone.Kind.ABILITY);
        player.getInventory().insertStack(stone);
        if (!stone.isEmpty()) {
            EconomyBridge.deposit(player, cfg.abilityStonePrice, "engineering_ability_stone_refund");
            player.sendMessage(Text.literal("§e[포로공학] 인벤토리 공간 부족 — 환불."), true);
            return;
        }
        player.sendMessage(Text.literal("§a[포로공학] 정수·특성 구매 (-" + cfg.abilityStonePrice + "). 포켓몬에 우클릭하세요."), true);
    }

    // ===== 포켓몬 선택 =====
    public static void openPokemonSelect(ServerPlayerEntity player) {
        ServerMenuHandler.show(player, Text.literal("포로공학 — 포켓몬 선택").formatted(Formatting.LIGHT_PURPLE),
                inv -> {
                    for (int i = 0; i < ServerMenuHandler.DISPLAY_SIZE; i++) inv.setStack(i, MenuIcons.pane());
                    inv.setStack(ShopLayout.BALANCE_SLOT, MenuIcons.icon(Items.PAPER, "§d기술 각인",
                            List.of("§7해제된 포켓몬만 선택 가능", "§7각인마다 골드(위력 등급가)")));
                    inv.setStack(ShopLayout.BACK_SLOT, MenuIcons.icon(Items.ARROW, "§e← 포로공학", List.of()));
                    int[] slots = {11, 12, 13, 14, 15, 16};
                    int i = 0;
                    for (Pokemon pk : Cobblemon.INSTANCE.getStorage().getParty(player)) {
                        if (pk == null || i >= slots.length) { i++; continue; }
                        boolean mo = MakeoverService.isMakeover(player, pk);
                        inv.setStack(slots[i], MenuIcons.icon(mo ? Items.ENDER_EYE : Items.IRON_BARS,
                                MenuIcons.named(mo ? Formatting.LIGHT_PURPLE : Formatting.DARK_GRAY,
                                                pk.getSpecies().getTranslatedName())
                                        .append(Text.literal(" Lv." + pk.getLevel()).formatted(Formatting.GRAY)),
                                mo ? List.of("§a클릭 — 기술 각인") : List.of("§c미해제 §7(정수 필요)")));
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
                            else p.sendMessage(Text.literal("§c[포로공학] 정수로 먼저 해제하세요."), true);
                            return;
                        }
                        i++;
                    }
                });
    }

    // ===== 기술 선택(타입 그리드) =====
    public static void openTeach(ServerPlayerEntity player, UUID pokemonUuid) {
        TARGET.put(player.getUuid(), pokemonUuid);
        ServerMenuHandler.show(player, Text.literal("포로공학 — 기술 선택").formatted(Formatting.LIGHT_PURPLE),
                EngineeringMenu::gridPopulate, EngineeringMenu::gridClick);
    }

    private static void gridPopulate(Inventory inv) {
        for (int i = 0; i < ServerMenuHandler.DISPLAY_SIZE; i++) inv.setStack(i, MenuIcons.pane());
        inv.setStack(ShopLayout.BALANCE_SLOT, MenuIcons.icon(Items.ENDER_EYE, "§d각인할 기술 선택",
                List.of("§7타입을 고르거나 검색", "§7각인마다 골드(위력 등급가)")));
        inv.setStack(ShopLayout.BACK_SLOT, MenuIcons.icon(Items.ARROW, "§e← 포켓몬 선택", List.of()));
        inv.setStack(SEARCH_SLOT, MenuIcons.icon(Items.OAK_SIGN, "§b검색",
                List.of("§7기술 이름으로 검색", "§e클릭 — 검색어 입력")));
        for (int i = 0; i < TmCatalog.TYPES.length && i < ShopLayout.CONTENT_SLOTS.length; i++) {
            String type = TmCatalog.TYPES[i];
            String ko = TYPE_KO.getOrDefault(type, type);
            inv.setStack(ShopLayout.CONTENT_SLOTS[i], MenuIcons.icon(typeIcon(type),
                    "§b" + ko + " §f타입", List.of("§7기술 §f" + TmCatalog.ofType(type).size() + "종", "§e클릭 — 목록")));
        }
    }

    private static void gridClick(ServerPlayerEntity player, int slot, int button, boolean shift) {
        if (slot == ShopLayout.BACK_SLOT) { openPokemonSelect(player); return; }
        if (slot == SEARCH_SLOT) { promptSearch(player); return; }
        int idx = ShopLayout.contentIndexOf(slot);
        if (idx < 0 || idx >= TmCatalog.TYPES.length) return;
        showList(player, "type:" + TmCatalog.TYPES[idx], 0);
    }

    private static void promptSearch(ServerPlayerEntity player) {
        UUID target = TARGET.get(player.getUuid());
        player.closeHandledScreen();
        player.sendMessage(Text.literal("§b[포로공학] 검색할 기술 이름을 채팅에 입력하세요. §7(취소: '취소')"), false);
        ChatInputManager.await(player, msg -> {
            if (msg.equals("취소") || msg.isBlank()) { if (target != null) openTeach(player, target); return; }
            showList(player, "q:" + msg.trim(), 0);
        });
    }

    // ===== 기술 목록 =====
    private static List<TmCatalog.Entry> entriesFor(String source) {
        if (source.startsWith("type:")) return TmCatalog.ofType(source.substring(5));
        if (source.startsWith("q:")) return TmCatalog.search(source.substring(2));
        return List.of();
    }

    private static String titleFor(String source) {
        if (source.startsWith("type:")) return TYPE_KO.getOrDefault(source.substring(5), source.substring(5)) + " 타입";
        if (source.startsWith("q:")) return "검색: " + source.substring(2);
        return "포로공학";
    }

    private static void showList(ServerPlayerEntity player, String source, int page) {
        ServerMenuHandler.show(player, Text.literal(titleFor(source)).formatted(Formatting.LIGHT_PURPLE),
                inv -> listPopulate(inv, player, source, page),
                (p, slot, button, shift) -> listClick(p, slot, source, page));
    }

    private static void listPopulate(Inventory inv, ServerPlayerEntity player, String source, int page) {
        for (int i = 0; i < ServerMenuHandler.DISPLAY_SIZE; i++) inv.setStack(i, MenuIcons.pane());
        String unit = ConfigManager.economy().currencyDisplay;
        EngineeringConfig cfg = ConfigManager.economy().engineering;
        List<TmCatalog.Entry> list = entriesFor(source);
        int pages = Math.max(1, (list.size() + PER_PAGE - 1) / PER_PAGE);
        page = Math.max(0, Math.min(page, pages - 1));

        inv.setStack(ShopLayout.BALANCE_SLOT, MenuIcons.icon(Items.GOLD_NUGGET,
                "§6잔액: " + EconomyBridge.getBalance(player) + " " + unit,
                List.of("§7기술 §f" + list.size() + "종", "§7페이지 §f" + (page + 1) + " / " + pages)));
        inv.setStack(ShopLayout.BACK_SLOT, MenuIcons.icon(Items.ARROW, "§e← 타입 선택", List.of()));
        if (page > 0) inv.setStack(ShopLayout.PREV_SLOT, MenuIcons.icon(Items.SPECTRAL_ARROW, "§e◀ 이전", List.of()));
        if (page < pages - 1) inv.setStack(ShopLayout.NEXT_SLOT, MenuIcons.icon(Items.SPECTRAL_ARROW, "§e다음 ▶", List.of()));

        int start = page * PER_PAGE;
        for (int i = 0; i < PER_PAGE && start + i < list.size(); i++) {
            TmCatalog.Entry e = list.get(start + i);
            Item item = resolve(e.itemId());
            if (item == null) item = Items.PAPER;
            long price = cfg.priceFor(e.power());
            String powTxt = e.power() <= 0 ? "변화" : String.valueOf((int) e.power());
            inv.setStack(ShopLayout.CONTENT_SLOTS[i], MenuIcons.icon(item,
                    MenuIcons.named(Formatting.LIGHT_PURPLE, e.displayText()),
                    List.of("§7위력: §f" + powTxt, "§7각인가: §6" + price + " " + unit, "§a클릭 — 각인")));
        }
    }

    private static void listClick(ServerPlayerEntity player, int slot, String source, int page) {
        if (slot == ShopLayout.BACK_SLOT) { openTeach(player, TARGET.get(player.getUuid())); return; }
        if (slot == ShopLayout.PREV_SLOT) { showList(player, source, page - 1); return; }
        if (slot == ShopLayout.NEXT_SLOT) { showList(player, source, page + 1); return; }
        int idx = ShopLayout.contentIndexOf(slot);
        if (idx < 0) return;
        List<TmCatalog.Entry> list = entriesFor(source);
        int globalIdx = page * PER_PAGE + idx;
        if (globalIdx >= list.size()) return;
        engrave(player, list.get(globalIdx).moveName(), list.get(globalIdx).displayText());
    }

    // ===== 각인 =====
    private static void engrave(ServerPlayerEntity player, String moveName, Text displayText) {
        UUID pid = TARGET.get(player.getUuid());
        Pokemon pk = MakeoverService.findPartyPokemon(player, pid);
        if (pk == null) {
            player.sendMessage(Text.literal("§c[포로공학] 대상 포켓몬을 파티에서 찾을 수 없습니다."), true);
            TARGET.remove(player.getUuid()); player.closeHandledScreen(); return;
        }
        if (!MakeoverService.isMakeover(player, pk)) {
            player.sendMessage(Text.literal("§c[포로공학] 해제되지 않은 포켓몬입니다."), true);
            TARGET.remove(player.getUuid()); player.closeHandledScreen(); return;
        }
        MoveTemplate tpl = Moves.getByName(moveName);
        if (tpl == null) { player.sendMessage(Text.literal("§c[포로공학] 알 수 없는 기술."), true); return; }
        MoveSet ms = pk.getMoveSet();
        if (ms.hasSpace()) {
            long price = ConfigManager.economy().engineering.priceFor(tpl.getPower());
            if (!EconomyBridge.withdraw(player, price, "engineering:" + moveName)) {
                player.sendMessage(Text.literal("§c[포로공학] 골드가 부족합니다 (필요 " + price + ")."), true);
                return;
            }
            List<Move> withNulls = ms.getMovesWithNulls();
            int free = -1;
            for (int i = 0; i < withNulls.size(); i++) if (withNulls.get(i) == null) { free = i; break; }
            if (free < 0) free = 0;
            ms.setMove(free, tpl.create());
            finishEngrave(player, pk, displayText, price);
        } else {
            PENDING.put(player.getUuid(), moveName);
            openSlotSelect(player, displayText);
        }
    }

    private static void openSlotSelect(ServerPlayerEntity player, Text displayText) {
        ServerMenuHandler.show(player, Text.literal("포로공학 — 교체할 기술").formatted(Formatting.LIGHT_PURPLE),
                inv -> {
                    for (int i = 0; i < ServerMenuHandler.DISPLAY_SIZE; i++) inv.setStack(i, MenuIcons.pane());
                    inv.setStack(ShopLayout.BALANCE_SLOT, MenuIcons.icon(Items.PAPER,
                            MenuIcons.named(Formatting.LIGHT_PURPLE, Text.literal("각인: ").append(displayText)),
                            List.of("§7교체할 기존 기술을 클릭하세요")));
                    inv.setStack(ShopLayout.BACK_SLOT, MenuIcons.icon(Items.ARROW, "§c취소", List.of()));
                    Pokemon pk = MakeoverService.findPartyPokemon(player, TARGET.get(player.getUuid()));
                    if (pk != null) {
                        List<Move> moves = pk.getMoveSet().getMoves();
                        int[] slots = {20, 21, 22, 23};
                        for (int i = 0; i < moves.size() && i < 4; i++) {
                            inv.setStack(slots[i], MenuIcons.icon(Items.PAPER,
                                    MenuIcons.named(Formatting.WHITE, moves.get(i).getTemplate().getDisplayName()),
                                    List.of("§7클릭 — 이 기술을 교체")));
                        }
                    }
                },
                (p, slot, button, shift) -> {
                    if (slot == ShopLayout.BACK_SLOT) { PENDING.remove(p.getUuid()); openTeach(p, TARGET.get(p.getUuid())); return; }
                    int[] slots = {20, 21, 22, 23};
                    int idx = -1;
                    for (int i = 0; i < slots.length; i++) if (slots[i] == slot) { idx = i; break; }
                    if (idx < 0) return;
                    String mn = PENDING.get(p.getUuid());
                    Pokemon pk = MakeoverService.findPartyPokemon(p, TARGET.get(p.getUuid()));
                    MoveTemplate tpl = mn == null ? null : Moves.getByName(mn);
                    if (pk == null || tpl == null) { p.closeHandledScreen(); return; }
                    if (idx >= pk.getMoveSet().getMoves().size()) return;
                    long price = ConfigManager.economy().engineering.priceFor(tpl.getPower());
                    if (!EconomyBridge.withdraw(p, price, "engineering:" + mn)) {
                        p.sendMessage(Text.literal("§c[포로공학] 골드가 부족합니다 (필요 " + price + ")."), true);
                        return;
                    }
                    pk.getMoveSet().setMove(idx, tpl.create());
                    PENDING.remove(p.getUuid());
                    finishEngrave(p, pk, displayText, price);
                });
    }

    private static void finishEngrave(ServerPlayerEntity player, Pokemon pk, Text displayText, long price) {
        UUID pid = TARGET.get(player.getUuid());
        player.sendMessage(Text.literal("§a[포로공학] ").append(pk.getSpecies().getTranslatedName())
                .append(Text.literal(" §a에게 ")).append(displayText)
                .append(Text.literal(" §a각인 완료 (-" + price + ").")), false);
        if (pid != null) openTeach(player, pid);
        else player.closeHandledScreen();
    }

    // ===== 특성 마개조 (결정 034) =====
    public static void openAbilityPokemonSelect(ServerPlayerEntity player) {
        ServerMenuHandler.show(player, Text.literal("특성 변경 — 포켓몬 선택").formatted(Formatting.LIGHT_PURPLE),
                inv -> {
                    for (int i = 0; i < ServerMenuHandler.DISPLAY_SIZE; i++) inv.setStack(i, MenuIcons.pane());
                    inv.setStack(ShopLayout.BALANCE_SLOT, MenuIcons.icon(Items.NETHER_STAR, "§d특성 변경",
                            List.of("§7특성 정수로 해제된 포켓몬만 선택 가능", "§7변경마다 골드")));
                    inv.setStack(ShopLayout.BACK_SLOT, MenuIcons.icon(Items.ARROW, "§e← 포로공학", List.of()));
                    int[] slots = {11, 12, 13, 14, 15, 16};
                    int i = 0;
                    for (Pokemon pk : Cobblemon.INSTANCE.getStorage().getParty(player)) {
                        if (pk == null || i >= slots.length) { i++; continue; }
                        boolean mo = MakeoverService.isAbilityMakeover(player, pk);
                        inv.setStack(slots[i], MenuIcons.icon(mo ? Items.ENDER_EYE : Items.IRON_BARS,
                                MenuIcons.named(mo ? Formatting.LIGHT_PURPLE : Formatting.DARK_GRAY,
                                                pk.getSpecies().getTranslatedName())
                                        .append(Text.literal(" Lv." + pk.getLevel()).formatted(Formatting.GRAY)),
                                mo ? List.of("§7현재 특성: ", "§a클릭 — 특성 변경") : List.of("§c미해제 §7(특성 정수 필요)")));
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
                            if (pk != null && MakeoverService.isAbilityMakeover(p, pk)) {
                                TARGET.put(p.getUuid(), pk.getUuid());
                                showAbilityList(p, "all", 0);
                            } else {
                                p.sendMessage(Text.literal("§c[포로공학] 특성 정수로 먼저 해제하세요."), true);
                            }
                            return;
                        }
                        i++;
                    }
                });
    }

    private static List<AbilityCatalog.Entry> abilityEntriesFor(String source) {
        if (source.startsWith("q:")) return AbilityCatalog.search(source.substring(2));
        return AbilityCatalog.all();
    }

    private static void showAbilityList(ServerPlayerEntity player, String source, int page) {
        String title = source.startsWith("q:") ? "특성 검색: " + source.substring(2) : "특성 목록";
        ServerMenuHandler.show(player, Text.literal(title).formatted(Formatting.LIGHT_PURPLE),
                inv -> abilityListPopulate(inv, player, source, page),
                (p, slot, button, shift) -> abilityListClick(p, slot, source, page));
    }

    private static void abilityListPopulate(Inventory inv, ServerPlayerEntity player, String source, int page) {
        for (int i = 0; i < ServerMenuHandler.DISPLAY_SIZE; i++) inv.setStack(i, MenuIcons.pane());
        String unit = ConfigManager.economy().currencyDisplay;
        EngineeringConfig cfg = ConfigManager.economy().engineering;
        List<AbilityCatalog.Entry> list = abilityEntriesFor(source);
        int pages = Math.max(1, (list.size() + PER_PAGE - 1) / PER_PAGE);
        page = Math.max(0, Math.min(page, pages - 1));

        inv.setStack(ShopLayout.BALANCE_SLOT, MenuIcons.icon(Items.GOLD_NUGGET,
                "§6잔액: " + EconomyBridge.getBalance(player) + " " + unit,
                List.of("§7특성 §f" + list.size() + "종", "§7변경가: §6" + cfg.abilityChangePrice + " " + unit,
                        "§7페이지 §f" + (page + 1) + " / " + pages)));
        inv.setStack(ShopLayout.BACK_SLOT, MenuIcons.icon(Items.ARROW, "§e← 포켓몬 선택", List.of()));
        inv.setStack(SEARCH_SLOT, MenuIcons.icon(Items.OAK_SIGN, "§b검색",
                List.of("§7특성 이름으로 검색", "§e클릭 — 검색어 입력")));
        if (page > 0) inv.setStack(ShopLayout.PREV_SLOT, MenuIcons.icon(Items.SPECTRAL_ARROW, "§e◀ 이전", List.of()));
        if (page < pages - 1) inv.setStack(ShopLayout.NEXT_SLOT, MenuIcons.icon(Items.SPECTRAL_ARROW, "§e다음 ▶", List.of()));

        int start = page * PER_PAGE;
        for (int i = 0; i < PER_PAGE && start + i < list.size(); i++) {
            AbilityCatalog.Entry e = list.get(start + i);
            inv.setStack(ShopLayout.CONTENT_SLOTS[i], MenuIcons.icon(Items.ENCHANTED_BOOK,
                    MenuIcons.named(Formatting.LIGHT_PURPLE, Text.translatable(e.translationKey())),
                    List.of("§7변경가: §6" + cfg.abilityChangePrice + " " + unit, "§a클릭 — 이 특성 부여")));
        }
    }

    private static void abilityListClick(ServerPlayerEntity player, int slot, String source, int page) {
        if (slot == ShopLayout.BACK_SLOT) { openAbilityPokemonSelect(player); return; }
        if (slot == SEARCH_SLOT) { promptAbilitySearch(player); return; }
        if (slot == ShopLayout.PREV_SLOT) { showAbilityList(player, source, page - 1); return; }
        if (slot == ShopLayout.NEXT_SLOT) { showAbilityList(player, source, page + 1); return; }
        int idx = ShopLayout.contentIndexOf(slot);
        if (idx < 0) return;
        List<AbilityCatalog.Entry> list = abilityEntriesFor(source);
        int globalIdx = page * PER_PAGE + idx;
        if (globalIdx >= list.size()) return;
        applyAbility(player, list.get(globalIdx));
    }

    private static void promptAbilitySearch(ServerPlayerEntity player) {
        player.closeHandledScreen();
        player.sendMessage(Text.literal("§b[포로공학] 검색할 특성 이름을 채팅에 입력하세요. §7(취소: '취소')"), false);
        ChatInputManager.await(player, msg -> {
            if (msg.equals("취소") || msg.isBlank()) { showAbilityList(player, "all", 0); return; }
            showAbilityList(player, "q:" + msg.trim(), 0);
        });
    }

    private static void applyAbility(ServerPlayerEntity player, AbilityCatalog.Entry entry) {
        Pokemon pk = MakeoverService.findPartyPokemon(player, TARGET.get(player.getUuid()));
        if (pk == null) {
            player.sendMessage(Text.literal("§c[포로공학] 대상 포켓몬을 파티에서 찾을 수 없습니다."), true);
            TARGET.remove(player.getUuid()); player.closeHandledScreen(); return;
        }
        if (!MakeoverService.isAbilityMakeover(player, pk)) {
            player.sendMessage(Text.literal("§c[포로공학] 특성이 해제되지 않은 포켓몬입니다."), true);
            TARGET.remove(player.getUuid()); player.closeHandledScreen(); return;
        }
        AbilityTemplate tpl = Abilities.get(entry.name());
        if (tpl == null) { player.sendMessage(Text.literal("§c[포로공학] 알 수 없는 특성."), true); return; }
        long price = ConfigManager.economy().engineering.abilityChangePrice;
        if (!EconomyBridge.withdraw(player, price, "engineering_ability:" + entry.name())) {
            player.sendMessage(Text.literal("§c[포로공학] 골드가 부족합니다 (필요 " + price + ")."), true);
            return;
        }
        pk.setAbility$common(new Ability(tpl, true, Priority.NORMAL)); // forced=true → 임의 강제 부여
        player.sendMessage(Text.literal("§a[포로공학] ").append(pk.getSpecies().getTranslatedName())
                .append(Text.literal(" §a의 특성을 ")).append(Text.translatable(entry.translationKey()))
                .append(Text.literal(" §a(으)로 변경 (-" + price + ").")), false);
        showAbilityList(player, "all", 0);
    }

    private static Item resolve(String itemId) {
        net.minecraft.util.Identifier id = net.minecraft.util.Identifier.tryParse(itemId);
        if (id == null || !net.minecraft.registry.Registries.ITEM.containsId(id)) return null;
        return net.minecraft.registry.Registries.ITEM.get(id);
    }

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
            default -> Items.WHITE_WOOL;
        };
    }
}
