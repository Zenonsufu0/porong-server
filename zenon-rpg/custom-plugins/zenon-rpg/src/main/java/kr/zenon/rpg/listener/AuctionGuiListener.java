package kr.zenon.rpg.listener;

import kr.zenon.rpg.combat.CombatStateService;
import kr.zenon.rpg.common.registry.master.ItemMasterRegistry;
import kr.zenon.rpg.common.registry.master.model.ItemMaster;
import kr.zenon.rpg.growth.GrowthStateStore;
import kr.zenon.rpg.growth.engine.PlayerGrowthState;
import kr.zenon.rpg.growth.engine.ItemGrade;
import kr.zenon.rpg.growth.engine.TraceInstance;
import kr.zenon.rpg.market.TraceInstanceCodec;
import kr.zenon.rpg.growth.island.IslandTerritoryState;
import kr.zenon.rpg.growth.island.IslandTerritoryStateStore;
import kr.zenon.rpg.gui.GuiTitles;
import kr.zenon.rpg.market.AuctionListing;
import kr.zenon.rpg.market.AuctionStore;
import kr.zenon.rpg.market.AuctionStore.SortMode;
import kr.zenon.rpg.common.result.Result;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.text.NumberFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class AuctionGuiListener implements Listener {

    private static final String CAT_ALL  = "전체";
    private static final String CAT_TRACE = "흔적";
    private static final String CAT_MAT  = "재료";
    private static final String CAT_DISP = "치장";
    private static final String CAT_ETC  = "기타";
    private static final String[] CATEGORIES = {CAT_ALL, CAT_TRACE, CAT_MAT, CAT_DISP, CAT_ETC};

    // 메인 GUI 슬롯 상수
    private static final int SLOT_REGISTER   = 0;
    private static final int SLOT_MY_LIST    = 1;
    private static final int SLOT_CAT_START  = 2;  // 2~6
    private static final int SLOT_SORT       = 8;
    private static final int SLOT_LISTING_START = 9;
    private static final int SLOT_LISTING_END   = 44;
    private static final int SLOT_BACK_MAIN  = 45;
    private static final int SLOT_PREV       = 48;
    private static final int SLOT_PAGE_LABEL = 49;
    private static final int SLOT_NEXT       = 50;

    // 등록 GUI 슬롯 상수 (27-slot)
    private static final int REG_PALETTE_END = 8;
    private static final int REG_PREVIEW     = 9;
    private static final int REG_PRICE       = 12;
    private static final int REG_CONFIRM     = 14;
    private static final int REG_BACK        = 18;

    // 내 목록 GUI 슬롯 상수 (54-slot)
    private static final int MY_SLOT_BACK = 45;
    private static final int MY_SLOT_PREV = 48;
    private static final int MY_SLOT_PAGE = 49;
    private static final int MY_SLOT_NEXT = 50;

    private enum GuiMode { MAIN, REGISTER, MY_LISTINGS }

    // 플레이어별 GUI 상태
    private final Map<UUID, GuiMode>   playerMode     = new ConcurrentHashMap<>();
    private final Map<UUID, String>    playerCategory = new ConcurrentHashMap<>();
    private final Map<UUID, SortMode>  playerSort     = new ConcurrentHashMap<>();
    private final Map<UUID, Integer>   playerPage     = new ConcurrentHashMap<>();
    // 메인 GUI: 현재 페이지에 표시된 listing ID (슬롯 9~44 → listings)
    private final Map<UUID, List<Long>> pageListingIds = new ConcurrentHashMap<>();
    // 내 목록 GUI: 현재 페이지 listing ID (슬롯 0~44)
    private final Map<UUID, List<Long>> myListingIds   = new ConcurrentHashMap<>();
    // 등록 GUI 상태
    private final Map<UUID, String>    regSelectedItem  = new ConcurrentHashMap<>();
    private final Map<UUID, Long>      regPrice         = new ConcurrentHashMap<>();
    private final Map<UUID, Integer>   regPage          = new ConcurrentHashMap<>(); // 등록 브라우저 페이지 (DL-129#35)
    // 가격 채팅 입력 대기 중인 플레이어
    private final Set<UUID> awaitingPriceInput = ConcurrentHashMap.newKeySet();
    // 수량 채팅 입력 대기 (가격 입력 후, DL-129#36)
    private final Set<UUID> awaitingQtyInput = ConcurrentHashMap.newKeySet();
    // 등록 GUI의 팔레트(0~8) 아이템 매핑 (슬롯 → itemId)
    private final Map<UUID, List<String>> regPalette = new ConcurrentHashMap<>();

    private final Plugin               plugin;
    private final GrowthStateStore     growthStateStore;
    private final IslandTerritoryStateStore islandStore;
    private final kr.zenon.rpg.growth.island.IslandStorageStore islandStorageStore;
    private final CombatStateService   combatStateService;
    private final AuctionStore         auctionStore;
    private final ItemMasterRegistry   itemMasters;
    private final kr.zenon.rpg.scoreboard.ScoreboardService scoreboardService;
    private final NamespacedKey        poroItemKey;

    public AuctionGuiListener(Plugin plugin,
                               GrowthStateStore growthStateStore,
                               IslandTerritoryStateStore islandTerritoryStateStore,
                               kr.zenon.rpg.growth.island.IslandStorageStore islandStorageStore,
                               AuctionStore auctionStore,
                               ItemMasterRegistry itemMasters,
                               CombatStateService combatStateService,
                               kr.zenon.rpg.scoreboard.ScoreboardService scoreboardService) {
        this.plugin             = plugin;
        this.growthStateStore   = growthStateStore;
        this.islandStore        = islandTerritoryStateStore;
        this.islandStorageStore = islandStorageStore;
        this.auctionStore       = auctionStore;
        this.itemMasters        = itemMasters;
        this.combatStateService = combatStateService;
        this.scoreboardService  = scoreboardService;
        this.poroItemKey      = new NamespacedKey(plugin, "poro_item_id");
    }

    // ── 공개 진입점 ───────────────────────────────────────────────────────

    public void openMain(Player player) {
        if (combatStateService.isInCombat(player.getUniqueId())) {
            player.sendMessage("§c[경매장] 전투 중에는 경매장을 이용할 수 없습니다.");
            return;
        }
        playerMode.put(player.getUniqueId(), GuiMode.MAIN);
        playerCategory.putIfAbsent(player.getUniqueId(), CAT_ALL);
        playerSort.putIfAbsent(player.getUniqueId(), SortMode.LATEST);
        playerPage.put(player.getUniqueId(), 0);
        refreshMain(player);
    }

    // ── 이벤트 핸들러 ─────────────────────────────────────────────────────

    @EventHandler(ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        UUID uid = player.getUniqueId();

        // 타이틀로만 경매 GUI 식별 — playerMode는 GUI 전환 시 onClose가 지워 null이 될 수 있어 가드로 쓰지 않음
        // (메인→등록 전환 시 mode 소실로 클릭이 취소 안 되어 아이템이 빠져나가던 버그 수정, DL-129#34).
        Component title = event.getView().title();
        boolean isMainGui     = GuiTitles.AUCTION.equals(title);
        boolean isRegisterGui = GuiTitles.AUCTION_REGISTER.equals(title);
        boolean isMyGui       = GuiTitles.AUCTION_MY.equals(title);

        if (!isMainGui && !isRegisterGui && !isMyGui) return;
        event.setCancelled(true);

        int slot = event.getRawSlot();
        if (slot < 0) return;

        if (isMainGui)     handleMainClick(player, slot, event.isRightClick());
        else if (isRegisterGui) handleRegisterClick(player, slot);
        else handleMyClick(player, slot, event.isRightClick());
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        UUID uid = player.getUniqueId();
        GuiMode mode = playerMode.get(uid);
        if (mode == null) return;

        Component title = event.getView().title();
        boolean isRegisterGui = GuiTitles.AUCTION_REGISTER.equals(title);
        // 가격/수량 채팅 대기 중이면 선택 상태 보존 (입력 위해 닫은 것)
        if (isRegisterGui && !awaitingPriceInput.contains(uid) && !awaitingQtyInput.contains(uid)) {
            regSelectedItem.remove(uid);
            regPrice.remove(uid);
            regPalette.remove(uid);
            playerMode.remove(uid);
        }
        if (GuiTitles.AUCTION.equals(title) || GuiTitles.AUCTION_MY.equals(title)) {
            playerMode.remove(uid);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(io.papermc.paper.event.player.AsyncChatEvent event) {
        UUID uid = event.getPlayer().getUniqueId();
        boolean priceP = awaitingPriceInput.contains(uid);
        boolean qtyP   = awaitingQtyInput.contains(uid);
        if (!priceP && !qtyP) return;
        event.setCancelled(true); // 입력 메시지 채팅 미노출

        // DL-129#33: AsyncChatEvent에서 메시지 추출
        String input = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                .serialize(event.message()).trim().replace(",", "");
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTask(plugin, () -> {
            if ("취소".equals(input) || input.isBlank()) {
                awaitingPriceInput.remove(uid); awaitingQtyInput.remove(uid);
                regSelectedItem.remove(uid); regPrice.remove(uid);
                player.sendMessage("§7[경매장] 등록을 취소했습니다.");
                openRegister(player);
                return;
            }
            String selected = regSelectedItem.get(uid);
            if (selected == null) { awaitingPriceInput.remove(uid); awaitingQtyInput.remove(uid); openRegister(player); return; }

            // ① 가격 입력 단계 → 가격 확정 후 수량 입력으로
            if (priceP) {
                awaitingPriceInput.remove(uid);
                long price;
                try { price = Long.parseLong(input); }
                catch (NumberFormatException e) { player.sendMessage("§c[경매장] 숫자를 입력하세요. 다시 등록해 주세요."); openRegister(player); return; }
                if (price < AuctionStore.MIN_PRICE) {
                    player.sendMessage("§c[경매장] 최소 등록가는 " + fmt(AuctionStore.MIN_PRICE) + "G 입니다. 다시 등록해 주세요.");
                    openRegister(player); return;
                }
                regPrice.put(uid, price);
                // 흔적 인스턴스는 수량 개념이 없음(개별 1개) → 수량 입력 생략, 바로 등록 (DL-129 추가#38 P5).
                if (isTraceId(selected)) {
                    regPrice.remove(uid);
                    confirmRegister(player, selected, 1, price);
                    return;
                }
                long held = heldOf(uid, selected);
                awaitingQtyInput.add(uid);
                player.sendMessage("§e[경매장] §f" + itemDisplayName(selected) + " §e등록 수량을 채팅으로 입력하세요. §8(보유 "
                        + fmt(held) + "개 · 취소: '취소')");
                return;
            }

            // ② 수량 입력 단계 → 보유 검증 후 등록
            awaitingQtyInput.remove(uid);
            Long price = regPrice.get(uid);
            if (price == null) { openRegister(player); return; }
            long qty;
            try { qty = Long.parseLong(input); }
            catch (NumberFormatException e) { player.sendMessage("§c[경매장] 숫자를 입력하세요. 다시 등록해 주세요."); openRegister(player); return; }
            long held = heldOf(uid, selected);
            if (qty <= 0) { player.sendMessage("§c[경매장] 1개 이상 입력하세요."); openRegister(player); return; }
            if (qty > held) {
                player.sendMessage("§c[경매장] 보유 수량이 부족합니다. §7(보유 " + fmt(held) + "개) 다시 등록해 주세요.");
                openRegister(player); return;
            }
            confirmRegister(player, selected, qty, price);
        });
    }

    // ── 메인 GUI ─────────────────────────────────────────────────────────

    private void handleMainClick(Player player, int slot, boolean rightClick) {
        UUID uid = player.getUniqueId();

        if (slot == SLOT_REGISTER) {
            openRegister(player);
            return;
        }
        if (slot == SLOT_MY_LIST) {
            openMyListings(player);
            return;
        }
        if (slot >= SLOT_CAT_START && slot <= SLOT_CAT_START + 4) {
            playerCategory.put(uid, CATEGORIES[slot - SLOT_CAT_START]);
            playerPage.put(uid, 0);
            refreshMain(player);
            return;
        }
        if (slot == SLOT_SORT) {
            playerSort.put(uid, playerSort.getOrDefault(uid, SortMode.LATEST).next());
            playerPage.put(uid, 0);
            refreshMain(player);
            return;
        }
        if (slot >= SLOT_LISTING_START && slot <= SLOT_LISTING_END) {
            int listingIdx = slot - SLOT_LISTING_START;
            List<Long> ids = pageListingIds.getOrDefault(uid, List.of());
            if (listingIdx >= ids.size()) return;
            long listingId = ids.get(listingIdx);
            handleBuy(player, listingId);
            return;
        }
        if (slot == SLOT_PREV) {
            int page = playerPage.getOrDefault(uid, 0);
            if (page > 0) { playerPage.put(uid, page - 1); refreshMain(player); }
            return;
        }
        if (slot == SLOT_NEXT) {
            String cat = playerCategory.getOrDefault(uid, CAT_ALL);
            int total = auctionStore.totalPages(cat);
            int page = playerPage.getOrDefault(uid, 0);
            if (page < total - 1) { playerPage.put(uid, page + 1); refreshMain(player); }
        }
    }

    private void refreshMain(Player player) {
        UUID uid = player.getUniqueId();
        String cat  = playerCategory.getOrDefault(uid, CAT_ALL);
        SortMode sort = playerSort.getOrDefault(uid, SortMode.LATEST);
        int page = playerPage.getOrDefault(uid, 0);

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<AuctionListing> listings = auctionStore.getPage(cat, sort, page);
            int totalPages = auctionStore.totalPages(cat);

            Bukkit.getScheduler().runTask(plugin, () -> {
                Inventory inv = Bukkit.createInventory(player, 54, GuiTitles.AUCTION);
                pageListingIds.put(uid, new ArrayList<>());

                // 헤더 버튼
                inv.setItem(SLOT_REGISTER, named(Material.GOLD_INGOT,
                        "§e경매 등록",
                        "§7──────────",
                        "§7수수료: §f판매가의 5% §7(판매 완료 시)",
                        "§7만료: §f3일 후 미판매 시 창고 반환",
                        "§7최대 등록: §f5개 §7/ 계정",
                        "§7──────────",
                        "§7클릭 → 등록 GUI"));
                inv.setItem(SLOT_MY_LIST, named(Material.PLAYER_HEAD,
                        "§f내 등록 목록",
                        "§7클릭 → 내 목록 보기"));

                // 카테고리 필터
                Material[] catMats = {Material.COMPASS, Material.PRISMARINE_CRYSTALS,
                        Material.CRAFTING_TABLE, Material.DIAMOND, Material.CHEST};
                for (int i = 0; i < CATEGORIES.length; i++) {
                    Material mat = cat.equals(CATEGORIES[i]) ? Material.LIME_STAINED_GLASS_PANE : catMats[i];
                    inv.setItem(SLOT_CAT_START + i, named(mat, "§f" + CATEGORIES[i]));
                }
                inv.setItem(7, glassPane(Material.BLACK_STAINED_GLASS_PANE));
                inv.setItem(SLOT_SORT, named(Material.HOPPER, "§f정렬: " + sort.displayName()));

                // 리스팅 슬롯
                List<Long> ids = pageListingIds.get(uid);
                for (int i = 0; i < AuctionStore.PAGE_SIZE; i++) {
                    int invSlot = SLOT_LISTING_START + i;
                    if (i < listings.size()) {
                        AuctionListing l = listings.get(i);
                        ids.add(l.id());
                        long avg = auctionStore.getAveragePrice(l.itemId());
                        String avgText = avg < 0 ? "§7데이터 없음" : "§7" + fmt(avg) + "G";
                        List<String> lore = new ArrayList<>();
                        lore.add("§7──────────");
                        lore.addAll(traceSubstatLore(l));
                        lore.add("§e가격: §f" + fmt(l.price()) + "G");
                        lore.add("§7판매자: §f" + l.sellerName());
                        lore.add("§7남은 시간: §f" + l.remainingText());
                        lore.add("§7──────────");
                        lore.add("§83일 평균시세: " + avgText);
                        lore.add("§7──────────");
                        lore.add("§7좌클릭  §f구매");
                        inv.setItem(invSlot, listingIcon(l, lore));
                    } else {
                        inv.setItem(invSlot, glassPane(Material.GRAY_STAINED_GLASS_PANE));
                    }
                }

                // 하단 내비게이션
                for (int s : new int[]{46, 47, 51, 52, 53}) inv.setItem(s, glassPane(Material.GRAY_STAINED_GLASS_PANE));
                if (page > 0)
                    inv.setItem(SLOT_PREV, named(Material.ARROW, "§f◀ 이전"));
                else
                    inv.setItem(SLOT_PREV, glassPane(Material.GRAY_STAINED_GLASS_PANE));
                inv.setItem(SLOT_PAGE_LABEL, named(Material.PAPER, "§f" + (page + 1) + " / " + totalPages));
                if (page < totalPages - 1)
                    inv.setItem(SLOT_NEXT, named(Material.ARROW, "§f다음 ▶"));
                else
                    inv.setItem(SLOT_NEXT, glassPane(Material.GRAY_STAINED_GLASS_PANE));

                playerMode.put(uid, GuiMode.MAIN);
                player.openInventory(inv);
            });
        });
    }

    private void handleBuy(Player player, long listingId) {
        UUID uid = player.getUniqueId();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Result<AuctionListing> findResult = auctionStore.findActive(listingId);
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (findResult.isFailure()) {
                    player.sendMessage("§c[경매장] 이미 판매되거나 만료된 상품입니다.");
                    refreshMain(player);
                    return;
                }
                AuctionListing listing = findResult.value();
                if (listing.sellerUuid().equals(uid)) {
                    player.sendMessage("§c[경매장] 자신의 상품은 구매할 수 없습니다.");
                    return;
                }

                // DB 구매 전 선검사: 아이템 지급 불가면 구매 자체를 차단
                IslandTerritoryState buyerTerritory = islandStore.get(uid).orElse(null);
                if (buyerTerritory == null) {
                    player.sendMessage("§c[경매장] 오류: 영지 데이터가 없습니다. 잠시 후 다시 시도하세요.");
                    return;
                }
                PlayerGrowthState growth = growthStateStore.get(uid).orElse(null);
                if (growth == null) { player.sendMessage("§c[경매장] 오류: 성장 데이터가 없습니다."); return; }
                if (growth.currency("gold") < listing.price()) {
                    player.sendMessage("§c[경매장] 골드가 부족합니다. (필요: " + fmt(listing.price()) + "G)");
                    return;
                }

                growth.consumeCurrency("gold", listing.price());

                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    Result<AuctionListing> buyResult = auctionStore.buy(listingId, uid);
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        if (buyResult.isFailure()) {
                            // 골드 복구
                            growth.addCurrency("gold", listing.price());
                            player.sendMessage("§c[경매장] 구매 실패: " + buyResult.message());
                            refreshMain(player);
                            return;
                        }
                        player.sendMessage("§a[경매장] §f" + listingDisplayName(listing)
                                + " §7을 §e" + fmt(listing.price()) + "G§7에 구매했습니다.");
                        scoreboardService.refresh(player);

                        // 구매자 아이템 pending 즉시 수령 (buy()에서 원자적으로 삽입됨)
                        deliverPendingToOnlinePlayer(player);

                        // 판매자 골드 pending 즉시 수령
                        Player seller = Bukkit.getPlayer(listing.sellerUuid());
                        if (seller != null) {
                            deliverPendingToOnlinePlayer(seller);
                            scoreboardService.refresh(seller);
                        }
                        refreshMain(player);
                    });
                });
            });
        });
    }

    // ── 등록 GUI ─────────────────────────────────────────────────────────

    private void openRegister(Player player) {
        UUID uid = player.getUniqueId();
        playerMode.put(uid, GuiMode.REGISTER);

        IslandTerritoryState territory = islandStore.get(uid).orElse(null);
        // 통화형(큐브·강화석)을 1·2번에 고정 + 창고 전체 (DL-129#36·#37)
        List<String> palette = new java.util.ArrayList<>();
        palette.add("mat_cube");
        palette.add("mat_stone_enhance");
        palette.addAll(buildTradeablePalette(territory));
        // 바닐라 IslandStorage 재료 — 영지 창고 연동(경매=영지 창고 전체).
        palette.addAll(buildVanillaPalette(uid));
        // 흔적 인스턴스 — 등급↓ 개별 등록 (DL-129 추가#38, P5).
        if (territory != null) {
            territory.traceInstancesSnapshot().stream()
                    .sorted((a, b) -> b.grade().ordinal() - a.grade().ordinal())
                    .forEach(t -> palette.add(t.instanceId()));
        }
        regPalette.put(uid, palette);

        final int per = 45;
        int totalPages = Math.max(1, (int) Math.ceil((double) palette.size() / per));
        int cp = Math.max(0, Math.min(regPage.getOrDefault(uid, 0), totalPages - 1));
        regPage.put(uid, cp);

        Inventory inv = Bukkit.createInventory(player, 54, GuiTitles.AUCTION_REGISTER);
        for (int i = 45; i < 54; i++) inv.setItem(i, glassPane(Material.GRAY_STAINED_GLASS_PANE));

        if (palette.isEmpty()) {
            inv.setItem(22, named(Material.BARRIER, "§c판매 가능한 재료가 없습니다",
                    "§7──────────",
                    "§7창고에 거래 가능한 재료(필드·보스",
                    "§7드랍·생산 재료 등)가 있어야",
                    "§7경매에 등록할 수 있습니다."));
        } else {
            int start = cp * per, end = Math.min(start + per, palette.size());
            for (int i = start; i < end; i++) {
                String itemId = palette.get(i);
                if (isTraceId(itemId)) {
                    inv.setItem(i - start, registerTraceIcon(uid, itemId));
                    continue;
                }
                long held = heldOf(uid, itemId); // 통화형=통화, 바닐라=IslandStorage, 그 외=창고
                long avg = auctionStore.getAveragePrice(itemId);
                String avgText = avg < 0 ? "§8데이터 없음" : "§7" + fmt(avg) + "G §8(참고)";
                java.util.List<String> lore = java.util.List.of(
                        "§7보유: §e" + fmt(held) + "개",
                        "§83일 평균시세: " + avgText,
                        "§7──────────", "§a클릭 → 가격·수량 입력 후 등록");
                Material vm = vanillaMaterial(itemId);
                if (vm != null) {
                    inv.setItem(i - start, vanillaIcon(vm, lore));
                } else {
                    inv.setItem(i - start, storageIcon(itemId, lore));
                }
            }
        }

        if (cp > 0) inv.setItem(SLOT_PREV, named(Material.ARROW, "§f◀ 이전", "§7" + cp + " / " + totalPages));
        inv.setItem(SLOT_PAGE_LABEL, named(Material.PAPER, "§f" + (cp + 1) + " / " + totalPages + " 페이지",
                "§7판매 가능 재료 §f" + palette.size() + "종",
                "§7최대 등록 §f" + AuctionStore.MAX_LISTINGS + "개 / 계정"));
        if (cp < totalPages - 1) inv.setItem(SLOT_NEXT, named(Material.ARROW, "§f다음 ▶", "§7" + (cp + 2) + " / " + totalPages));
        inv.setItem(SLOT_BACK_MAIN, named(Material.ARROW, "§f뒤로 (경매장)"));

        player.openInventory(inv);
    }

    private void handleRegisterClick(Player player, int slot) {
        UUID uid = player.getUniqueId();

        if (slot == SLOT_BACK_MAIN) { regPage.remove(uid); regSelectedItem.remove(uid); openMain(player); return; }
        if (slot == SLOT_PREV) { regPage.merge(uid, -1, Integer::sum); openRegister(player); return; }
        if (slot == SLOT_NEXT) { regPage.merge(uid, 1, Integer::sum); openRegister(player); return; }

        // 재료 슬롯(0~44) 클릭 → 거래가능 판정 후 가격 채팅 입력으로 (DL-129#36)
        if (slot >= 0 && slot <= 44) {
            List<String> palette = regPalette.getOrDefault(uid, List.of());
            int idx = regPage.getOrDefault(uid, 0) * 45 + slot;
            if (idx >= palette.size()) return;
            String itemId = palette.get(idx);
            if (auctionStore.countActive(uid) >= AuctionStore.MAX_LISTINGS) {
                player.sendMessage("§c[경매장] 최대 등록 수(" + AuctionStore.MAX_LISTINGS + "개)에 도달했습니다.");
                return;
            }
            regSelectedItem.put(uid, itemId);
            player.closeInventory();
            awaitingPriceInput.add(uid);
            player.sendMessage("§e[경매장] §f" + registerDisplayName(uid, itemId)
                    + " §e등록 가격을 채팅으로 입력하세요. §8(최소 " + fmt(AuctionStore.MIN_PRICE) + "G · 취소: '취소')");
        }
    }

    private void confirmRegister(Player player, String itemId, long quantity, long price) {
        UUID uid = player.getUniqueId();

        // 흔적 인스턴스 — 개별 등록(수량 무조건 1, payload에 등급+세부스탯 보관). DL-129 추가#38 P5.
        if (isTraceId(itemId)) {
            confirmRegisterTrace(player, itemId, price);
            return;
        }

        // 통화형(큐브·강화석)은 통화에서, 그 외는 창고에서 차감 (DL-129#37)
        if (heldOf(uid, itemId) < quantity) {
            player.sendMessage("§c[경매장] 보유 수량이 부족합니다.");
            openRegister(player);
            return;
        }
        int active = auctionStore.countActive(uid);
        if (active >= AuctionStore.MAX_LISTINGS) {
            player.sendMessage("§c[경매장] 최대 등록 수(" + AuctionStore.MAX_LISTINGS + "개)에 도달했습니다.");
            return;
        }

        if (!debitItem(uid, itemId, quantity)) {
            player.sendMessage("§c[경매장] 보유 수량이 부족합니다.");
            openRegister(player);
            return;
        }
        final long qty = quantity;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Result<Long> result = auctionStore.register(uid, player.getName(), itemId, (int) qty, price);
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (result.isFailure()) {
                    creditItem(uid, itemId, qty); // 롤백
                    player.sendMessage("§c[경매장] 등록 실패: " + result.message());
                } else {
                    player.sendMessage("§a[경매장] §f" + itemDisplayName(itemId) + " §7×" + qty
                            + "개를 §e" + fmt(price) + "G§7에 등록했습니다.");
                }
                regSelectedItem.remove(uid);
                regPrice.remove(uid);
                regPalette.remove(uid);
                openMain(player);
            });
        });
    }

    /** 흔적 인스턴스 등록 — payload 캡처 후 영지에서 제거, 등록 실패 시 롤백(재추가). DL-129 추가#38 P5. */
    private void confirmRegisterTrace(Player player, String instanceId, long price) {
        UUID uid = player.getUniqueId();
        IslandTerritoryState t = islandStore.get(uid).orElse(null);
        TraceInstance trace = t != null ? t.findTraceInstance(instanceId).orElse(null) : null;
        if (trace == null) {
            player.sendMessage("§c[경매장] 흔적을 찾을 수 없습니다.");
            openRegister(player);
            return;
        }
        if (auctionStore.countActive(uid) >= AuctionStore.MAX_LISTINGS) {
            player.sendMessage("§c[경매장] 최대 등록 수(" + AuctionStore.MAX_LISTINGS + "개)에 도달했습니다.");
            return;
        }
        String payload = TraceInstanceCodec.toJson(trace);
        t.removeTraceInstance(instanceId);
        String display = trace.grade().displayName() + " 장비의 흔적";
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Result<Long> result = auctionStore.register(uid, player.getName(), instanceId, 1, price, payload);
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (result.isFailure()) {
                    t.addTraceInstance(trace); // 롤백
                    player.sendMessage("§c[경매장] 등록 실패: " + result.message());
                } else {
                    player.sendMessage("§a[경매장] §f" + display + " §7을(를) §e" + fmt(price) + "G§7에 등록했습니다.");
                }
                regSelectedItem.remove(uid);
                regPrice.remove(uid);
                regPalette.remove(uid);
                openMain(player);
            });
        });
    }

    public void handleDirectRegister(Player player, long price, long quantity) {
        if (combatStateService.isInCombat(player.getUniqueId())) {
            player.sendMessage("§c[경매장] 전투 중에는 경매장을 이용할 수 없습니다.");
            return;
        }
        ItemStack held = player.getInventory().getItemInMainHand();
        if (held.getType() == Material.AIR) {
            player.sendMessage("§c[경매장] 손에 등록할 아이템을 들어야 합니다.");
            return;
        }
        if (price < AuctionStore.MIN_PRICE) {
            player.sendMessage("§c[경매장] 최소 가격은 §e" + fmt(AuctionStore.MIN_PRICE) + "G§c입니다.");
            return;
        }

        // 1. PDC 키로 itemId 조회 (태그된 아이템 우선)
        String itemId = null;
        ItemMeta meta = held.getItemMeta();
        if (meta != null && meta.getPersistentDataContainer().has(poroItemKey, PersistentDataType.STRING)) {
            itemId = meta.getPersistentDataContainer().get(poroItemKey, PersistentDataType.STRING);
        }
        // 2. 디스플레이명 → ItemMaster.itemName() 매칭 fallback
        if (itemId == null && meta != null && meta.hasDisplayName()) {
            String stripped = ChatColor.stripColor(meta.getDisplayName());
            itemId = itemMasters.all().values().stream()
                    .filter(m -> m.itemName().equals(stripped))
                    .map(ItemMaster::itemId)
                    .findFirst()
                    .orElse(null);
        }
        if (itemId == null) {
            player.sendMessage("§c[경매장] 인식할 수 없는 아이템입니다. 창고의 등록 가능 아이템만 사용하세요.");
            return;
        }
        final String resolvedId = itemId;
        if (!itemMasters.find(resolvedId).map(ItemMaster::tradeable).orElse(false)) {
            player.sendMessage("§c[경매장] §f" + itemDisplayName(resolvedId) + "§c은 거래 불가 아이템입니다.");
            return;
        }

        UUID uid = player.getUniqueId();
        IslandTerritoryState territory = islandStore.get(uid).orElse(null);
        if (territory == null) {
            player.sendMessage("§c[경매장] 영지 데이터가 없습니다.");
            return;
        }
        long available = territory.getCustomItem(resolvedId);
        if (available < 1) {
            player.sendMessage("§c[경매장] 창고에 §f" + itemDisplayName(resolvedId) + "§c이 없습니다.");
            return;
        }
        long resolvedQty = quantity <= 0 ? available : Math.min(quantity, available);
        resolvedQty = Math.min(resolvedQty, Integer.MAX_VALUE);
        confirmRegister(player, resolvedId, resolvedQty, price);
    }

    // ── 내 목록 GUI ───────────────────────────────────────────────────────

    private void openMyListings(Player player) {
        UUID uid = player.getUniqueId();
        playerMode.put(uid, GuiMode.MY_LISTINGS);
        playerPage.put(uid, 0);

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<AuctionListing> listings = auctionStore.getMyListings(uid, 0);
            Bukkit.getScheduler().runTask(plugin, () -> buildMyGui(player, listings, 0));
        });
    }

    private void buildMyGui(Player player, List<AuctionListing> listings, int page) {
        UUID uid = player.getUniqueId();
        Inventory inv = Bukkit.createInventory(player, 54, GuiTitles.AUCTION_MY);
        List<Long> ids = new ArrayList<>();
        myListingIds.put(uid, ids);

        for (int i = 0; i < AuctionStore.MY_PAGE_SIZE; i++) {
            if (i < listings.size()) {
                AuctionListing l = listings.get(i);
                ids.add(l.id());
                long avg = auctionStore.getAveragePrice(l.itemId());
                String avgText = avg < 0 ? "§7데이터 없음" : "§7" + fmt(avg) + "G";
                List<String> lore = new ArrayList<>();
                lore.addAll(traceSubstatLore(l));
                lore.add("§e가격: §f" + fmt(l.price()) + "G");
                lore.add("§7남은 시간: §f" + l.remainingText());
                lore.add("§7──────────");
                lore.add("§83일 평균시세: " + avgText);
                lore.add("§7──────────");
                lore.add("§c우클릭 → 등록 취소 §8(창고 반환)");
                inv.setItem(i, listingIcon(l, lore));
            } else {
                inv.setItem(i, glassPane(Material.GRAY_STAINED_GLASS_PANE));
            }
        }

        for (int s : new int[]{46, 47, 51, 52, 53}) inv.setItem(s, glassPane(Material.GRAY_STAINED_GLASS_PANE));
        inv.setItem(MY_SLOT_BACK, named(Material.ARROW, "§f뒤로"));
        inv.setItem(MY_SLOT_PAGE, named(Material.PAPER, "§f" + (page + 1) + "페이지"));

        playerMode.put(uid, GuiMode.MY_LISTINGS);
        player.openInventory(inv);
    }

    private void handleMyClick(Player player, int slot, boolean rightClick) {
        UUID uid = player.getUniqueId();
        if (slot == MY_SLOT_BACK) {
            openMain(player);
            return;
        }
        if (slot >= 0 && slot < AuctionStore.MY_PAGE_SIZE) {
            List<Long> ids = myListingIds.getOrDefault(uid, List.of());
            if (slot >= ids.size()) return;
            long listingId = ids.get(slot);
            if (rightClick) handleCancel(player, listingId);
        }
    }

    private void handleCancel(Player player, long listingId) {
        UUID uid = player.getUniqueId();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Result<AuctionListing> result = auctionStore.cancel(listingId, uid);
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (result.isFailure()) {
                    player.sendMessage("§c[경매장] 취소 실패: " + result.message());
                } else {
                    AuctionListing l = result.value();
                    if (l.isTrace()) {
                        creditTrace(uid, l.itemPayload(), player.getName()); // 흔적 인스턴스 반환 (DL-129 추가#38)
                        player.sendMessage("§e[경매장] §f" + auctionTraceName(l)
                                + "§7 등록을 취소했습니다. 창고에 반환됐습니다.");
                    } else {
                        creditItem(uid, l.itemId(), l.quantity()); // 통화형은 통화로, 그 외는 창고로 반환 (DL-129#37)
                        String dest = AuctionStore.isCurrencyItem(l.itemId()) ? "반환됐습니다." : "창고에 반환됐습니다.";
                        player.sendMessage("§e[경매장] §f" + itemDisplayName(l.itemId())
                                + "§7 등록을 취소했습니다. " + dest);
                    }
                }
                openMyListings(player);
            });
        });
    }

    // ── 내부 빌더 유틸 ────────────────────────────────────────────────────

    /**
     * 온라인 플레이어의 pending_delivery를 즉시 수령 처리한다.
     * 로그인 정산과 동일한 fetch → apply(main) → delete(async) 순서를 유지한다.
     * tryStartClaim으로 동일 플레이어의 동시 수령을 방지한다.
     * 실제로 지급 성공한 ID만 삭제 대상에 포함 — 상태 미로드 항목은 다음 로그인에서 재시도.
     */
    private void deliverPendingToOnlinePlayer(Player player) {
        UUID uuid = player.getUniqueId();
        if (!auctionStore.tryStartClaim(uuid)) return;

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<AuctionStore.PendingDelivery> deliveries = auctionStore.fetchPending(uuid);
            if (deliveries.isEmpty()) {
                auctionStore.endClaim(uuid);
                return;
            }

            Bukkit.getScheduler().runTask(plugin, () -> {
                PlayerGrowthState growth = growthStateStore.get(uuid).orElse(null);
                IslandTerritoryState territory = islandStore.get(uuid).orElse(null);

                List<Long> deliveredIds = new ArrayList<>();
                for (AuctionStore.PendingDelivery d : deliveries) {
                    if (d.gold() > 0 && growth != null) {
                        growth.addCurrency("gold", d.gold());
                        deliveredIds.add(d.id());
                        player.sendMessage("§a[경매장] §7판매 완료 수익: §e"
                                + fmt(d.gold()) + "G §7자동 지급됨.");
                    } else if (d.isTrace() && territory != null) {
                        // 흔적 인스턴스 배달 (DL-129 추가#38, P5) — payload 복원 후 영지에 추가.
                        TraceInstance trace = TraceInstanceCodec.fromJson(d.itemPayload());
                        if (trace != null) {
                            territory.addTraceInstance(trace);
                            deliveredIds.add(d.id());
                            player.sendMessage("§a[경매장] §7흔적 §f" + trace.grade().displayName()
                                    + " 장비의 흔적 §7이(가) 창고에 지급됐습니다.");
                        }
                    } else if (d.itemId() != null && AuctionStore.isCurrencyItem(d.itemId()) && growth != null) {
                        growth.addCurrency(d.itemId(), d.quantity()); // 통화형 배달 (DL-129#37)
                        deliveredIds.add(d.id());
                        player.sendMessage("§a[경매장] §f" + itemDisplayName(d.itemId())
                                + " §7×" + fmt(d.quantity()) + " §7지급됐습니다.");
                    } else if (d.itemId() != null && vanillaMaterial(d.itemId()) != null) {
                        // 바닐라 재료 배달 → IslandStorage (영지 창고 연동).
                        Material vm = vanillaMaterial(d.itemId());
                        islandStorageStore.get(uuid).ifPresent(s -> s.add(vm, d.quantity()));
                        deliveredIds.add(d.id());
                        player.sendMessage("§a[경매장] §7아이템 §f" + itemDisplayName(d.itemId())
                                + " §7×" + fmt(d.quantity()) + " §7창고에 지급됐습니다.");
                    } else if (d.itemId() != null && territory != null) {
                        territory.addCustomItem(d.itemId(), d.quantity());
                        deliveredIds.add(d.id());
                        player.sendMessage("§a[경매장] §7아이템 §f" + itemDisplayName(d.itemId())
                                + " §7창고에 지급됐습니다.");
                    }
                }

                if (deliveredIds.isEmpty()) {
                    auctionStore.endClaim(uuid);
                    return;
                }
                scoreboardService.refresh(player);
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    try { auctionStore.deletePendingByIds(deliveredIds); }
                    finally { auctionStore.endClaim(uuid); }
                });
            });
        });
    }

    private List<String> buildTradeablePalette(IslandTerritoryState territory) {
        if (territory == null) return List.of();
        // DL-129#36: 창고 전체(보유 커스텀 재료 전부) 표시 — 거래 가능 여부는 클릭 시 판정.
        return territory.customItemsSnapshot().entrySet().stream()
                .filter(e -> e.getValue() > 0)
                .map(Map.Entry::getKey)
                .sorted()
                .toList();
    }

    /** 바닐라 IslandStorage 재료 팔레트 — item_id = Material.name()(대문자). 영지 창고 연동. */
    private List<String> buildVanillaPalette(UUID uid) {
        return islandStorageStore.get(uid)
                .map(s -> s.materialList().stream()
                        .filter(m -> s.getAmount(m) > 0)
                        .map(Material::name)
                        .sorted()
                        .toList())
                .orElse(List.of());
    }

    /** 해당 재료 경매 거래 가능 여부 (item_master is_tradeable). */
    private boolean isTradeable(String itemId) {
        return itemMasters.find(itemId).map(ItemMaster::tradeable).orElse(false);
    }

    // ── 흔적 인스턴스 식별 (DL-129 추가#38, P5) — 인스턴스 id 접두어 "trace_" ──
    private static boolean isTraceId(String itemId) {
        return itemId != null && itemId.startsWith("trace_");
    }

    // ── 바닐라 IslandStorage 재료 식별 (영지 창고 연동) — itemId가 Material 이름(대문자) ──
    private static Material vanillaMaterial(String itemId) {
        return itemId == null ? null : Material.getMaterial(itemId);
    }
    private static boolean isVanillaMaterial(String itemId) {
        return vanillaMaterial(itemId) != null;
    }

    // ── 통화형(큐브·강화석) ↔ 창고 라우팅 (DL-129#37) ──
    /** 보유량 — 통화형=통화, 흔적=1/0, 바닐라=IslandStorage, 그 외=창고 customItems. */
    private long heldOf(UUID uid, String itemId) {
        if (AuctionStore.isCurrencyItem(itemId))
            return growthStateStore.get(uid).map(g -> g.currency(itemId)).orElse(0L);
        if (isTraceId(itemId))
            return islandStore.get(uid).map(t -> t.findTraceInstance(itemId).isPresent() ? 1L : 0L).orElse(0L);
        Material vm = vanillaMaterial(itemId);
        if (vm != null)
            return islandStorageStore.get(uid).map(s -> s.getAmount(vm)).orElse(0L);
        return islandStore.get(uid).map(t -> t.getCustomItem(itemId)).orElse(0L);
    }
    /** 차감 — 성공 여부. (흔적은 confirmRegister에서 payload 캡처 후 직접 처리) */
    private boolean debitItem(UUID uid, String itemId, long qty) {
        if (AuctionStore.isCurrencyItem(itemId))
            return growthStateStore.get(uid).map(g -> g.consumeCurrency(itemId, qty)).orElse(false);
        Material vm = vanillaMaterial(itemId);
        if (vm != null) {
            kr.zenon.rpg.growth.island.IslandStorage s = islandStorageStore.get(uid).orElse(null);
            if (s == null || s.getAmount(vm) < qty) return false;
            return s.withdraw(vm, qty) == qty;
        }
        IslandTerritoryState t = islandStore.get(uid).orElse(null);
        if (t == null || t.getCustomItem(itemId) < qty) return false;
        t.withdrawCustomItem(itemId, qty);
        return true;
    }
    /** 지급(반환/롤백/배달) — 통화/바닐라/창고 스택용. 흔적은 creditTrace 사용. */
    private void creditItem(UUID uid, String itemId, long qty) {
        if (AuctionStore.isCurrencyItem(itemId)) {
            growthStateStore.get(uid).ifPresent(g -> g.addCurrency(itemId, qty));
            return;
        }
        Material vm = vanillaMaterial(itemId);
        if (vm != null) {
            islandStorageStore.get(uid).ifPresent(s -> s.add(vm, qty));
            return;
        }
        islandStore.get(uid).ifPresent(t -> t.addCustomItem(itemId, qty));
    }
    /** 등록 palette용 흔적 아이콘 — 창고와 동일한 흔적 텍스처(CustomItemModel) + 등급명 + 세부스탯 lore. */
    private ItemStack registerTraceIcon(UUID uid, String instanceId) {
        TraceInstance t = islandStore.get(uid).flatMap(s -> s.findTraceInstance(instanceId)).orElse(null);
        if (t == null) return named(Material.PAPER, "§8흔적 (없음)");
        java.util.List<String> lore = new ArrayList<>();
        lore.add("§7등급: " + kr.zenon.rpg.gui.EquipmentLoreRenderer.gradeColor(t.grade()) + t.grade().displayName());
        t.substats().forEach(line -> lore.add("§7  "
                + kr.zenon.rpg.gui.EquipmentLoreRenderer.potentialOptionKr(line.optionCode())
                + " §e+" + String.format("%.1f", line.value())));
        lore.add("§7──────────");
        lore.add("§a클릭 → 가격 입력 후 등록");
        return traceIcon(t.grade(),
                kr.zenon.rpg.gui.EquipmentLoreRenderer.gradeColor(t.grade()) + t.grade().displayName() + " 장비의 흔적",
                lore);
    }

    /** 흔적 아이콘 빌더 — 창고(StorageGui)와 동일한 CustomItemModel 흔적 텍스처 사용(바닐라 폴백 방지). */
    private ItemStack traceIcon(ItemGrade grade, String displayName, java.util.List<String> lore) {
        ItemStack item = kr.zenon.rpg.gui.CustomItemModel.buildStack("equip_trace_unidentified", 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(displayName);
            if (lore != null && !lore.isEmpty()) meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    /** 등록 흐름 표시명 — 흔적이면 등급명, 그 외 일반명. */
    private String registerDisplayName(UUID uid, String itemId) {
        if (isTraceId(itemId)) {
            TraceInstance t = islandStore.get(uid).flatMap(s -> s.findTraceInstance(itemId)).orElse(null);
            return t != null ? t.grade().displayName() + " 장비의 흔적" : "장비의 흔적";
        }
        return itemDisplayName(itemId);
    }

    /** 흔적 인스턴스 지급 — payload(JSON) 복원 후 영지에 추가 (배달/반환/롤백). */
    private void creditTrace(UUID uid, String payload, String playerName) {
        TraceInstance trace = TraceInstanceCodec.fromJson(payload);
        if (trace == null) return;
        islandStore.getOrCreate(uid, playerName != null ? playerName : "").addTraceInstance(trace);
    }

    /** 창고와 동일한 아이콘(CMD 텍스쳐)+한글명. 경매 표시 통일 (DL-129#36). */
    private ItemStack storageIcon(String itemId, java.util.List<String> lore) {
        ItemStack item = kr.zenon.rpg.gui.CustomItemModel.buildStack(itemId, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null && !lore.isEmpty()) {
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    /** listing 아이콘 — 흔적=CustomItemModel 흔적 텍스처, 바닐라=StorageGui 동일 외형, 그 외 재질+표시명. */
    private ItemStack listingIcon(AuctionListing l, java.util.List<String> lore) {
        if (l.isTrace()) {
            TraceInstance t = TraceInstanceCodec.fromJson(l.itemPayload());
            ItemGrade g = t != null ? t.grade() : ItemGrade.COMMON;
            return traceIcon(g, "§f" + listingDisplayName(l), lore);
        }
        Material vm = vanillaMaterial(l.itemId());
        if (vm != null) return vanillaIcon(vm, lore);
        return named(listingMaterial(l), "§f" + listingDisplayName(l), lore.toArray(new String[0]));
    }

    /** listing 표시명 — 흔적이면 등급 색상명, 그 외 일반 한글명. */
    private String listingDisplayName(AuctionListing l) {
        if (l.isTrace()) return auctionTraceName(l);
        return itemDisplayName(l.itemId());
    }

    /** listing 아이콘 재질 — 흔적이면 등급별, 그 외 itemMaterial. */
    private Material listingMaterial(AuctionListing l) {
        if (l.isTrace()) {
            TraceInstance t = TraceInstanceCodec.fromJson(l.itemPayload());
            return traceGradeMaterial(t);
        }
        return itemMaterial(l.itemId());
    }

    /** 흔적 payload → "[색]등급 장비의 흔적" 표시명. */
    private String auctionTraceName(AuctionListing l) {
        TraceInstance t = TraceInstanceCodec.fromJson(l.itemPayload());
        if (t == null) return "장비의 흔적";
        return kr.zenon.rpg.gui.EquipmentLoreRenderer.gradeColor(t.grade()) + t.grade().displayName() + " 장비의 흔적";
    }

    private Material traceGradeMaterial(TraceInstance t) {
        if (t == null) return Material.PAPER;
        return switch (t.grade()) {
            case LEGENDARY -> Material.NETHER_STAR;
            case UNIQUE    -> Material.PRISMARINE_SHARD;
            case EPIC      -> Material.PRISMARINE_CRYSTALS;
            case RARE      -> Material.IRON_NUGGET;
            case COMMON    -> Material.GRAVEL;
        };
    }

    /** 흔적 세부스탯 lore 라인 목록(없으면 빈 리스트). */
    private List<String> traceSubstatLore(AuctionListing l) {
        TraceInstance t = TraceInstanceCodec.fromJson(l.itemPayload());
        if (t == null || t.substats().isEmpty()) return List.of();
        List<String> lore = new ArrayList<>();
        lore.add("§7세부스탯:");
        t.substats().forEach(line -> lore.add("§7  "
                + kr.zenon.rpg.gui.EquipmentLoreRenderer.potentialOptionKr(line.optionCode())
                + " §e+" + String.format("%.1f", line.value())));
        return lore;
    }

    private String itemDisplayName(String itemId) {
        // 바닐라 재료 — Material 이름 타이틀케이스(아이언 오어 등). 클라 번역은 GUI translatable로 별도.
        Material vm = vanillaMaterial(itemId);
        if (vm != null) return titleCase(vm.name());
        // 창고와 동일한 한글명 우선 — WorkshopRecipeRegistry(한글), 없으면 item_master(영어 가능)
        String n = kr.zenon.rpg.gui.WorkshopRecipeRegistry.displayName(itemId);
        if (n != null && !n.equals(itemId)) return n;
        return itemMasters.find(itemId).map(ItemMaster::itemName).orElse(itemId);
    }

    private static String titleCase(String raw) {
        String s = raw.replace('_', ' ').toLowerCase(java.util.Locale.ROOT);
        StringBuilder sb = new StringBuilder(s.length());
        boolean cap = true;
        for (char c : s.toCharArray()) { sb.append(cap ? Character.toUpperCase(c) : c); cap = c == ' '; }
        return sb.toString();
    }

    private Material itemMaterial(String itemId) {
        Material vm = vanillaMaterial(itemId);
        if (vm != null) return vm;
        if (itemId.startsWith("equip_trace_brilliant")) return Material.NETHER_STAR;
        if (itemId.startsWith("equip_trace_radiant"))   return Material.PRISMARINE_SHARD;
        if (itemId.startsWith("equip_trace_glowing"))   return Material.PRISMARINE_CRYSTALS;
        if (itemId.startsWith("equip_trace_faded"))     return Material.IRON_NUGGET;
        if (itemId.startsWith("equip_trace_broken"))    return Material.GRAVEL;
        if (itemId.startsWith("ancient_trace_"))        return Material.END_CRYSTAL;
        if (itemId.startsWith("mat_"))                  return Material.CRAFTING_TABLE;
        if (itemId.startsWith("display_"))              return Material.DIAMOND;
        return Material.PAPER;
    }

    /** 바닐라 재료 아이콘 — StorageGui와 동일(번역키 한글명 + 기본 아이템 텍스처). 영지 창고와 외형 일치. */
    private ItemStack vanillaIcon(Material mat, java.util.List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(net.kyori.adventure.text.Component.translatable(mat.translationKey())
                .color(net.kyori.adventure.text.format.NamedTextColor.WHITE)
                .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));
        if (lore != null && !lore.isEmpty()) meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack glassPane(Material mat) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(" "));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack named(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        if (lore.length > 0) meta.setLore(List.of(lore));
        item.setItemMeta(meta);
        return item;
    }

    private static String fmt(long value) {
        return NumberFormat.getIntegerInstance(java.util.Locale.KOREA).format(value);
    }
}
