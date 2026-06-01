package com.poro.rpg.listener;

import com.poro.rpg.combat.CombatStateService;
import com.poro.rpg.common.registry.master.ItemMasterRegistry;
import com.poro.rpg.common.registry.master.model.ItemMaster;
import com.poro.rpg.growth.GrowthStateStore;
import com.poro.rpg.growth.engine.PlayerGrowthState;
import com.poro.rpg.growth.island.IslandTerritoryState;
import com.poro.rpg.growth.island.IslandTerritoryStateStore;
import com.poro.rpg.gui.GuiTitles;
import com.poro.rpg.market.AuctionListing;
import com.poro.rpg.market.AuctionStore;
import com.poro.rpg.market.AuctionStore.SortMode;
import com.poro.rpg.common.result.Result;
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
    // 가격 채팅 입력 대기 중인 플레이어
    private final Set<UUID> awaitingPriceInput = ConcurrentHashMap.newKeySet();
    // 등록 GUI의 팔레트(0~8) 아이템 매핑 (슬롯 → itemId)
    private final Map<UUID, List<String>> regPalette = new ConcurrentHashMap<>();

    private final Plugin               plugin;
    private final GrowthStateStore     growthStateStore;
    private final IslandTerritoryStateStore islandStore;
    private final CombatStateService   combatStateService;
    private final AuctionStore         auctionStore;
    private final ItemMasterRegistry   itemMasters;
    private final com.poro.rpg.scoreboard.ScoreboardService scoreboardService;
    private final NamespacedKey        poroItemKey;

    public AuctionGuiListener(Plugin plugin,
                               GrowthStateStore growthStateStore,
                               IslandTerritoryStateStore islandTerritoryStateStore,
                               AuctionStore auctionStore,
                               ItemMasterRegistry itemMasters,
                               CombatStateService combatStateService,
                               com.poro.rpg.scoreboard.ScoreboardService scoreboardService) {
        this.plugin             = plugin;
        this.growthStateStore   = growthStateStore;
        this.islandStore        = islandTerritoryStateStore;
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
        GuiMode mode = playerMode.get(uid);
        if (mode == null) return;

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
        if (isRegisterGui && !awaitingPriceInput.contains(uid)) {
            // 등록 GUI를 닫을 때 팔레트 선택 해제
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
    public void onChat(AsyncPlayerChatEvent event) {
        UUID uid = event.getPlayer().getUniqueId();
        if (!awaitingPriceInput.contains(uid)) return;
        event.setCancelled(true);
        awaitingPriceInput.remove(uid);

        String input = event.getMessage().trim().replace(",", "");
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTask(plugin, () -> {
            long price;
            try {
                price = Long.parseLong(input);
            } catch (NumberFormatException e) {
                player.sendMessage("§c[경매장] 숫자를 입력하세요.");
                openRegister(player);
                return;
            }
            if (price < AuctionStore.MIN_PRICE) {
                player.sendMessage("§c[경매장] 최소 등록가는 " + fmt(AuctionStore.MIN_PRICE) + "G 입니다.");
                openRegister(player);
                return;
            }
            regPrice.put(uid, price);
            openRegister(player);
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
                        inv.setItem(invSlot, named(
                                itemMaterial(l.itemId()),
                                "§f" + itemDisplayName(l.itemId()),
                                "§7──────────",
                                "§e가격: §f" + fmt(l.price()) + "G",
                                "§7판매자: §f" + l.sellerName(),
                                "§7남은 시간: §f" + l.remainingText(),
                                "§7──────────",
                                "§83일 평균시세: " + avgText,
                                "§7──────────",
                                "§7좌클릭  §f구매"
                        ));
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
                        player.sendMessage("§a[경매장] §f" + itemDisplayName(listing.itemId())
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
        List<String> palette = buildTradeablePalette(territory);
        regPalette.put(uid, palette);

        Inventory inv = Bukkit.createInventory(player, 27, GuiTitles.AUCTION_REGISTER);

        // Row 0: 팔레트 (거래 가능한 아이템 목록)
        String selectedItem = regSelectedItem.get(uid);
        for (int i = 0; i <= REG_PALETTE_END; i++) {
            if (i < palette.size()) {
                String itemId = palette.get(i);
                boolean selected = itemId.equals(selectedItem);
                long held = territory != null ? territory.getCustomItem(itemId) : 0;
                ItemStack icon = named(itemMaterial(itemId),
                        (selected ? "§a§l[선택됨] " : "§f") + itemDisplayName(itemId),
                        "§7보유: §e" + held + "개",
                        selected ? "§a현재 선택된 아이템" : "§7클릭 → 선택");
                inv.setItem(i, icon);
            } else {
                inv.setItem(i, glassPane(Material.GRAY_STAINED_GLASS_PANE));
            }
        }

        // Row 1
        for (int i = 9; i <= 17; i++) inv.setItem(i, glassPane(Material.GRAY_STAINED_GLASS_PANE));

        // 슬롯 9: 선택된 아이템 미리보기
        if (selectedItem != null) {
            long avg = auctionStore.getAveragePrice(selectedItem);
            String avgText = avg < 0 ? "§7데이터 없음" : "§7" + fmt(avg) + "G §8(참고용)";
            inv.setItem(REG_PREVIEW, named(itemMaterial(selectedItem),
                    "§f" + itemDisplayName(selectedItem),
                    "§7──────────",
                    "§83일 평균시세: " + avgText,
                    "§7──────────",
                    "§7클릭 → 선택 해제"));
        } else {
            inv.setItem(REG_PREVIEW, named(Material.WHITE_STAINED_GLASS_PANE,
                    "§7아이템 미선택",
                    "§7──────────",
                    "§7위 팔레트에서 등록할",
                    "§7아이템을 선택하세요."));
        }

        // 슬롯 12: 가격 설정
        Long price = regPrice.get(uid);
        if (price != null) {
            long fee = (long) (price * AuctionStore.FEE_RATE);
            inv.setItem(REG_PRICE, named(Material.GOLD_NUGGET,
                    "§f가격 설정",
                    "§7현재: §e" + fmt(price) + "G",
                    "§7수수료(5%): §f" + fmt(fee) + "G",
                    "§7실수령: §f" + fmt(price - fee) + "G",
                    "§7──────────",
                    "§7클릭 → 가격 변경"));
        } else {
            inv.setItem(REG_PRICE, named(Material.GRAY_STAINED_GLASS_PANE,
                    "§f가격 설정",
                    "§7현재: §c미설정",
                    "§7클릭 → 가격 입력"));
        }

        // 슬롯 14: 등록 확인
        if (selectedItem != null && price != null) {
            long fee = (long) (price * AuctionStore.FEE_RATE);
            inv.setItem(REG_CONFIRM, named(Material.LIME_DYE,
                    "§a등록 확인",
                    "§7──────────",
                    "§f" + itemDisplayName(selectedItem),
                    "§e가격: §f" + fmt(price) + "G",
                    "§7수수료: §f" + fmt(fee) + "G §8(판매 완료 시)",
                    "§7만료: §f3일 후",
                    "§7──────────",
                    "§7클릭 → 등록"));
        } else {
            inv.setItem(REG_CONFIRM, named(Material.BLACK_STAINED_GLASS_PANE,
                    "§7등록 확인 §8(비활성)",
                    "§8아이템과 가격을 설정하세요."));
        }

        // Row 2
        for (int i = 18; i <= 26; i++) inv.setItem(i, glassPane(Material.GRAY_STAINED_GLASS_PANE));
        inv.setItem(REG_BACK, named(Material.ARROW, "§f뒤로"));

        player.openInventory(inv);
    }

    private void handleRegisterClick(Player player, int slot) {
        UUID uid = player.getUniqueId();
        List<String> palette = regPalette.getOrDefault(uid, List.of());

        if (slot >= 0 && slot <= REG_PALETTE_END) {
            if (slot < palette.size()) {
                String itemId = palette.get(slot);
                if (itemId.equals(regSelectedItem.get(uid))) {
                    regSelectedItem.remove(uid); // 선택 해제
                } else {
                    regSelectedItem.put(uid, itemId);
                }
                openRegister(player);
            }
            return;
        }
        if (slot == REG_PREVIEW) {
            // 미리보기 클릭 → 선택 해제
            regSelectedItem.remove(uid);
            openRegister(player);
            return;
        }
        if (slot == REG_PRICE) {
            player.closeInventory();
            awaitingPriceInput.add(uid);
            player.sendMessage("§e[경매장] 가격을 채팅으로 입력하세요. §8(취소: /경매장)");
            return;
        }
        if (slot == REG_CONFIRM) {
            String selectedItem = regSelectedItem.get(uid);
            Long price = regPrice.get(uid);
            if (selectedItem == null || price == null) return;
            confirmRegister(player, selectedItem, 1, price);
            return;
        }
        if (slot == REG_BACK) {
            regSelectedItem.remove(uid);
            regPrice.remove(uid);
            regPalette.remove(uid);
            openMain(player);
        }
    }

    private void confirmRegister(Player player, String itemId, long quantity, long price) {
        UUID uid = player.getUniqueId();
        IslandTerritoryState territory = islandStore.get(uid).orElse(null);
        if (territory == null) { player.sendMessage("§c[경매장] 오류: 영지 데이터가 없습니다."); return; }
        if (territory.getCustomItem(itemId) < quantity) {
            player.sendMessage("§c[경매장] 보유 수량이 부족합니다.");
            openRegister(player);
            return;
        }
        int active = auctionStore.countActive(uid);
        if (active >= AuctionStore.MAX_LISTINGS) {
            player.sendMessage("§c[경매장] 최대 등록 수(" + AuctionStore.MAX_LISTINGS + "개)에 도달했습니다.");
            return;
        }

        territory.withdrawCustomItem(itemId, quantity);
        final long qty = quantity;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Result<Long> result = auctionStore.register(uid, player.getName(), itemId, (int) qty, price);
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (result.isFailure()) {
                    territory.addCustomItem(itemId, qty); // 롤백
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
                inv.setItem(i, named(itemMaterial(l.itemId()),
                        "§f" + itemDisplayName(l.itemId()),
                        "§e가격: §f" + fmt(l.price()) + "G",
                        "§7남은 시간: §f" + l.remainingText(),
                        "§7──────────",
                        "§83일 평균시세: " + avgText,
                        "§7──────────",
                        "§c우클릭 → 등록 취소 §8(창고 반환)"));
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
                    IslandTerritoryState territory = islandStore.get(uid).orElse(null);
                    if (territory != null) territory.addCustomItem(l.itemId(), l.quantity());
                    player.sendMessage("§e[경매장] §f" + itemDisplayName(l.itemId())
                            + "§7 등록을 취소했습니다. 창고에 반환됐습니다.");
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
        return territory.customItemsSnapshot().entrySet().stream()
                .filter(e -> e.getValue() > 0)
                .map(Map.Entry::getKey)
                .filter(id -> itemMasters.find(id)
                        .map(ItemMaster::tradeable).orElse(false))
                .sorted()
                .toList();
    }

    private String itemDisplayName(String itemId) {
        return itemMasters.find(itemId)
                .map(ItemMaster::itemName)
                .orElse(itemId);
    }

    private Material itemMaterial(String itemId) {
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
