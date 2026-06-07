package com.poro.rpg.listener;

import com.poro.rpg.combat.CombatStateService;
import com.poro.rpg.growth.GrowthStateStore;
import com.poro.rpg.growth.engine.PlayerGrowthState;
import com.poro.rpg.growth.island.IslandStorage;
import com.poro.rpg.growth.island.IslandStorageStore;
import com.poro.rpg.gui.GuiTitles;
import com.poro.rpg.gui.MainHubGui;
import com.poro.rpg.gui.TerritoryHubGui;
import com.poro.rpg.market.ShopGui;
import com.poro.rpg.scoreboard.ScoreboardService;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ShopGuiListener implements Listener {

    private final GrowthStateStore   growthStateStore;
    private final IslandStorageStore islandStorageStore;
    private final CombatStateService combatStateService;
    private final ScoreboardService  scoreboardService;

    private final Map<UUID, ShopGui.Tab> activeTab          = new ConcurrentHashMap<>();
    private final Map<UUID, Long>        sellAllConfirmAt   = new ConcurrentHashMap<>();
    private static final long CONFIRM_WINDOW_MS = 5_000L;

    public ShopGuiListener(GrowthStateStore growthStateStore,
                           IslandStorageStore islandStorageStore,
                           CombatStateService combatStateService,
                           ScoreboardService scoreboardService) {
        this.growthStateStore   = growthStateStore;
        this.islandStorageStore = islandStorageStore;
        this.combatStateService = combatStateService;
        this.scoreboardService  = scoreboardService;
    }

    public void openShop(Player player) {
        if (combatStateService.isInCombat(player.getUniqueId())) {
            player.sendMessage("§c[상점] 전투 중에는 상점을 열 수 없습니다.");
            return;
        }
        ShopGui.Tab tab = activeTab.getOrDefault(player.getUniqueId(), ShopGui.Tab.MATERIAL);
        ShopGui.open(player, tab);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        if (GuiTitles.SHOP.equals(event.getView().title())) {
            event.setCancelled(true);
            handleShopClick(player, event);
        } else if (GuiTitles.SHOP_SELL.equals(event.getView().title())) {
            event.setCancelled(true);
            handleSellClick(player, event);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (GuiTitles.SHOP_SELL.equals(event.getView().title())) {
            sellAllConfirmAt.remove(player.getUniqueId());
        }
    }

    // ─── 구매(메인) GUI 클릭 ─────────────────────────────────────────

    private void handleShopClick(Player player, InventoryClickEvent event) {
        UUID uid = player.getUniqueId();
        int slot = event.getRawSlot();
        ShopGui.Tab tab = activeTab.getOrDefault(uid, ShopGui.Tab.MATERIAL);

        switch (slot) {
            case ShopGui.SLOT_TAB_MATERIAL -> { switchTab(player, uid, ShopGui.Tab.MATERIAL); return; }
            case ShopGui.SLOT_TAB_BLOCK    -> { switchTab(player, uid, ShopGui.Tab.BLOCK);    return; }
            case ShopGui.SLOT_TAB_COSMETIC -> { switchTab(player, uid, ShopGui.Tab.COSMETIC); return; }
            case ShopGui.SLOT_TAB_SPECIAL  -> { switchTab(player, uid, ShopGui.Tab.SPECIAL);  return; }
            case ShopGui.SLOT_BACK         -> { TerritoryHubGui.open(player); return; }
            case ShopGui.SLOT_SELL_BUTTON  -> { openSellGui(player, tab); return; }
        }

        // 아이템 슬롯 — Shift+클릭은 1개 즉시 판매, 그 외 구매
        ShopGui.ShopItem item = ShopGui.itemAt(tab, slot);
        if (item == null) return;
        if (event.getClick().isShiftClick()) {
            attemptSell(player, item, 1);
            ShopGui.open(player, tab);  // 재화 반영 후 lore 갱신
            return;
        }
        int sets = (event.getClick() == ClickType.RIGHT)
                ? Math.max(1, 64 / Math.max(1, item.amount()))
                : 1;
        attemptPurchase(player, item, sets);
    }

    private void switchTab(Player player, UUID uid, ShopGui.Tab tab) {
        activeTab.put(uid, tab);
        ShopGui.open(player, tab);
    }

    // ─── 판매 GUI ────────────────────────────────────────────────────

    private void openSellGui(Player player, ShopGui.Tab tab) {
        List<ShopGui.ShopItem> items = ShopGui.items(tab);
        if (items.isEmpty()) {
            player.sendMessage("§7[상점] 판매 가능한 아이템이 없는 탭입니다.");
            return;
        }
        sellAllConfirmAt.remove(player.getUniqueId());
        renderSellGui(player, tab);
    }

    private void renderSellGui(Player player, ShopGui.Tab tab) {
        Inventory inv = ShopGui.createSellInventory(player);
        List<ShopGui.ShopItem> items = ShopGui.items(tab);
        long expectedTotal = 0;

        for (int i = 0; i < items.size() && i < (ShopGui.SELL_ITEM_AREA_END + 1); i++) {
            ShopGui.ShopItem si = items.get(i);
            long invCount     = countInInventory(player, si.material());
            long storageCount = islandStorageStore.getOrCreate(player.getUniqueId()).get(si.material().name());
            long total = invCount + storageCount;
            expectedTotal += total * si.unitSellPrice();
            inv.setItem(i, ShopGui.sellDisplayItem(si, invCount, storageCount));
        }

        boolean confirming = sellAllConfirmAt.containsKey(player.getUniqueId());
        inv.setItem(ShopGui.SLOT_SELL_ALL, confirming
                ? MainHubGui.icon(Material.FIRE_CHARGE, "§c전량 판매 확인",
                        List.of("§c재클릭하면 모든 아이템이 판매됩니다.",
                                "§7예상 수익: §e" + expectedTotal + "G"))
                : MainHubGui.icon(Material.GOLD_BLOCK, "§e전량 판매",
                        List.of("§7현재 탭의 모든 보유 아이템 판매",
                                "§7예상 수익: §e" + expectedTotal + "G",
                                "§c[클릭 후 재클릭으로 확인]")));
        inv.setItem(ShopGui.SLOT_SELL_BACK, MainHubGui.icon(Material.ARROW, "§7뒤로", List.of("§7상점")));

        player.openInventory(inv);
    }

    private void handleSellClick(Player player, InventoryClickEvent event) {
        UUID uid = player.getUniqueId();
        ShopGui.Tab tab = activeTab.getOrDefault(uid, ShopGui.Tab.MATERIAL);
        int slot = event.getRawSlot();

        if (slot == ShopGui.SLOT_SELL_BACK) {
            sellAllConfirmAt.remove(uid);
            ShopGui.open(player, tab);
            return;
        }
        if (slot == ShopGui.SLOT_SELL_ALL) {
            Long confirmAt = sellAllConfirmAt.get(uid);
            long now = System.currentTimeMillis();
            if (confirmAt != null && now - confirmAt < CONFIRM_WINDOW_MS) {
                sellAllConfirmAt.remove(uid);
                executeSellAll(player, tab);
            } else {
                sellAllConfirmAt.put(uid, now);
                renderSellGui(player, tab);
            }
            return;
        }

        // 다른 슬롯 클릭 시 전량 판매 확인 취소
        sellAllConfirmAt.remove(uid);

        List<ShopGui.ShopItem> items = ShopGui.items(tab);
        if (slot < ShopGui.SELL_ITEM_AREA_START || slot > ShopGui.SELL_ITEM_AREA_END || slot >= items.size()) {
            renderSellGui(player, tab);
            return;
        }
        ShopGui.ShopItem si = items.get(slot);

        long invCount     = countInInventory(player, si.material());
        long storageCount = islandStorageStore.getOrCreate(uid).get(si.material().name());
        long total = invCount + storageCount;
        long qty;
        if (event.getClick().isShiftClick()) qty = total;
        else if (event.getClick() == ClickType.RIGHT) qty = Math.min(64, total);
        else qty = Math.min(1, total);

        if (qty <= 0) {
            player.sendMessage("§7[상점] §f" + si.displayName() + " §7보유량이 없습니다.");
            return;
        }
        attemptSell(player, si, qty);
        renderSellGui(player, tab);
    }

    private void attemptSell(Player player, ShopGui.ShopItem item, long qty) {
        Optional<PlayerGrowthState> growthOpt = growthStateStore.get(player.getUniqueId());
        if (growthOpt.isEmpty()) {
            player.sendMessage("§c[상점] 성장 데이터를 찾을 수 없습니다.");
            return;
        }
        PlayerGrowthState growth = growthOpt.get();

        // 인벤토리 → 창고 순서로 차감
        long fromInv = removeFromInventory(player, item.material(), qty);
        long remaining = qty - fromInv;
        long fromStorage = 0;
        if (remaining > 0) {
            IslandStorage storage = islandStorageStore.getOrCreate(player.getUniqueId());
            fromStorage = storage.withdraw(item.material(), remaining);
        }
        long actuallySold = fromInv + fromStorage;
        if (actuallySold <= 0) {
            player.sendMessage("§7[상점] §f" + item.displayName() + " §7보유량이 없습니다.");
            return;
        }

        long totalGold = actuallySold * item.unitSellPrice();
        growth.addCurrency("gold", totalGold);
        player.sendMessage("§a[상점] §f" + item.displayName() + " §a×" + actuallySold + "개 §7판매 (+" + totalGold + "G)");
        scoreboardService.refresh(player);
    }

    private void executeSellAll(Player player, ShopGui.Tab tab) {
        List<ShopGui.ShopItem> items = ShopGui.items(tab);
        long totalGold = 0;
        int kinds = 0;
        long totalCount = 0;
        for (ShopGui.ShopItem si : items) {
            long invCount     = countInInventory(player, si.material());
            long storageCount = islandStorageStore.getOrCreate(player.getUniqueId()).get(si.material().name());
            long total = invCount + storageCount;
            if (total <= 0) continue;
            removeFromInventory(player, si.material(), invCount);
            islandStorageStore.getOrCreate(player.getUniqueId()).withdraw(si.material(), storageCount);
            totalGold += total * si.unitSellPrice();
            totalCount += total;
            kinds++;
        }
        if (totalGold > 0) {
            final long earned = totalGold;
            growthStateStore.get(player.getUniqueId()).ifPresent(s -> s.addCurrency("gold", earned));
            scoreboardService.refresh(player);
        }
        player.sendMessage("§a[상점] 전량 판매 — §f" + kinds + "종 / " + totalCount + "개 §7→ §e+" + totalGold + "G");
        renderSellGui(player, tab);
    }

    // ─── 인벤토리 헬퍼 ───────────────────────────────────────────────

    private long countInInventory(Player player, Material material) {
        long total = 0;
        for (ItemStack stack : player.getInventory().getContents()) {
            if (stack != null && stack.getType() == material) total += stack.getAmount();
        }
        return total;
    }

    /** material을 qty만큼 인벤토리에서 제거하고 실제 제거 수량 반환. */
    private long removeFromInventory(Player player, Material material, long qty) {
        long remaining = qty;
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length && remaining > 0; i++) {
            ItemStack stack = contents[i];
            if (stack == null || stack.getType() != material) continue;
            int take = (int) Math.min(stack.getAmount(), remaining);
            int after = stack.getAmount() - take;
            if (after <= 0) player.getInventory().setItem(i, null);
            else stack.setAmount(after);
            remaining -= take;
        }
        return qty - remaining;
    }

    // ─── 구매 ───────────────────────────────────────────────────────

    private void attemptPurchase(Player player, ShopGui.ShopItem item, int sets) {
        Optional<PlayerGrowthState> growthOpt = growthStateStore.get(player.getUniqueId());
        if (growthOpt.isEmpty()) {
            player.sendMessage("§c[상점] 성장 데이터를 찾을 수 없습니다.");
            return;
        }
        PlayerGrowthState growth = growthOpt.get();

        long totalCost = item.price() * sets;
        long have = growth.currency("gold");
        if (have < totalCost) {
            player.sendMessage("§c[상점] 골드 부족: 필요 §e" + totalCost + "G§c, 보유 §e" + have + "G");
            return;
        }
        if (!growth.consumeCurrency("gold", totalCost)) {
            player.sendMessage("§c[상점] 골드 차감 실패.");
            return;
        }

        int amountPerSet = Math.max(1, item.amount());
        long storedToStorage = 0;
        for (int i = 0; i < sets; i++) {
            ItemStack stack = new ItemStack(item.material(), amountPerSet);
            Map<Integer, ItemStack> overflow = player.getInventory().addItem(stack);
            if (overflow.isEmpty()) continue;
            IslandStorage storage = islandStorageStore.getOrCreate(player.getUniqueId());
            for (ItemStack rem : overflow.values()) {
                storage.add(rem.getType(), rem.getAmount());
                storedToStorage += rem.getAmount();
            }
        }

        long totalItems = (long) sets * amountPerSet;
        String suffix = storedToStorage > 0
                ? " §7(인벤 적재 후 §b창고 +" + storedToStorage + "개§7)"
                : "";
        player.sendMessage("§a[상점] §f" + item.displayName() + " §a×" + sets + "세트 §7= §a" + totalItems + "개 §7구매 (-" + totalCost + "G)" + suffix);
        scoreboardService.refresh(player);
    }
}
