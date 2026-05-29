package com.poro.empire.listener;

import com.poro.empire.combat.CombatStateService;
import com.poro.empire.growth.GrowthStateStore;
import com.poro.empire.growth.engine.PlayerGrowthState;
import com.poro.empire.growth.island.IslandStorage;
import com.poro.empire.growth.island.IslandStorageStore;
import com.poro.empire.gui.GuiTitles;
import com.poro.empire.gui.TerritoryHubGui;
import com.poro.empire.market.ShopGui;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public final class ShopGuiListener implements Listener {

    private final GrowthStateStore   growthStateStore;
    private final IslandStorageStore islandStorageStore;
    private final CombatStateService combatStateService;

    // 플레이어별 현재 탭 (재시작 시 초기화)
    private final Map<UUID, ShopGui.Tab> activeTab = new ConcurrentHashMap<>();

    public ShopGuiListener(GrowthStateStore growthStateStore,
                           IslandStorageStore islandStorageStore,
                           CombatStateService combatStateService) {
        this.growthStateStore   = growthStateStore;
        this.islandStorageStore = islandStorageStore;
        this.combatStateService = combatStateService;
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
        if (!GuiTitles.SHOP.equals(event.getView().title())) return;
        event.setCancelled(true);

        UUID uid = player.getUniqueId();
        int slot = event.getRawSlot();
        ShopGui.Tab tab = activeTab.getOrDefault(uid, ShopGui.Tab.MATERIAL);

        // 탭 전환
        switch (slot) {
            case ShopGui.SLOT_TAB_MATERIAL -> { switchTab(player, uid, ShopGui.Tab.MATERIAL); return; }
            case ShopGui.SLOT_TAB_BLOCK    -> { switchTab(player, uid, ShopGui.Tab.BLOCK);    return; }
            case ShopGui.SLOT_TAB_COSMETIC -> { switchTab(player, uid, ShopGui.Tab.COSMETIC); return; }
            case ShopGui.SLOT_TAB_SPECIAL  -> { switchTab(player, uid, ShopGui.Tab.SPECIAL);  return; }
            case ShopGui.SLOT_BACK         -> { TerritoryHubGui.open(player); return; }
            case ShopGui.SLOT_SELL_BUTTON  -> { player.sendMessage("§7[상점] 판매는 준비 중입니다."); return; }
        }

        // 아이템 구매 — 좌클릭: 1세트(amount개), 우클릭: ~64아이템에 해당하는 세트
        ShopGui.ShopItem item = ShopGui.itemAt(tab, slot);
        if (item == null) return;
        int sets = (event.getClick() == ClickType.RIGHT)
                ? Math.max(1, 64 / Math.max(1, item.amount()))
                : 1;
        attemptPurchase(player, item, sets);
    }

    private void switchTab(Player player, UUID uid, ShopGui.Tab tab) {
        activeTab.put(uid, tab);
        ShopGui.open(player, tab);
    }

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

        // 인벤토리 우선 적재 → 풀일 시 영지 창고 자동 적재
        int amountPerSet = Math.max(1, item.amount());
        long storedToStorage = 0;
        for (int i = 0; i < sets; i++) {
            ItemStack stack = new ItemStack(item.material(), amountPerSet);
            Map<Integer, ItemStack> overflow = player.getInventory().addItem(stack);
            if (overflow.isEmpty()) continue;

            // 인벤토리 못 들어간 잔량 → 영지 창고
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
    }
}
