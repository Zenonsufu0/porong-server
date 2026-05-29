package com.poro.empire.listener;

import com.poro.empire.combat.CombatStateService;
import com.poro.empire.growth.GrowthStateStore;
import com.poro.empire.growth.engine.PlayerGrowthState;
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
    private final CombatStateService combatStateService;

    // 플레이어별 현재 탭 (재시작 시 초기화)
    private final Map<UUID, ShopGui.Tab> activeTab = new ConcurrentHashMap<>();

    public ShopGuiListener(GrowthStateStore growthStateStore,
                           CombatStateService combatStateService) {
        this.growthStateStore   = growthStateStore;
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

        // 아이템 구매
        ShopGui.ShopItem item = ShopGui.itemAt(tab, slot);
        if (item == null) return;
        int multiplier = (event.getClick() == ClickType.RIGHT) ? 64 : 1;
        attemptPurchase(player, item, multiplier);
    }

    private void switchTab(Player player, UUID uid, ShopGui.Tab tab) {
        activeTab.put(uid, tab);
        ShopGui.open(player, tab);
    }

    private void attemptPurchase(Player player, ShopGui.ShopItem item, int multiplier) {
        Optional<PlayerGrowthState> growthOpt = growthStateStore.get(player.getUniqueId());
        if (growthOpt.isEmpty()) {
            player.sendMessage("§c[상점] 성장 데이터를 찾을 수 없습니다.");
            return;
        }
        PlayerGrowthState growth = growthOpt.get();

        long totalCost = item.price() * multiplier;
        long have = growth.currency("gold");
        if (have < totalCost) {
            player.sendMessage("§c[상점] 골드 부족: 필요 §e" + totalCost + "G§c, 보유 §e" + have + "G");
            return;
        }
        if (!growth.consumeCurrency("gold", totalCost)) {
            player.sendMessage("§c[상점] 골드 차감 실패.");
            return;
        }

        // 인벤토리 적재
        for (int i = 0; i < multiplier; i++) {
            ItemStack stack = new ItemStack(item.material(), Math.max(1, item.amount()));
            Map<Integer, ItemStack> overflow = player.getInventory().addItem(stack);
            if (!overflow.isEmpty()) {
                // 인벤토리 풀: 환불
                long refund = item.price() * (multiplier - i);
                growth.addCurrency("gold", refund);
                player.sendMessage("§c[상점] 인벤토리 가득참. " + i + "개 구매 후 중단 (환불: " + refund + "G).");
                return;
            }
        }
        player.sendMessage("§a[상점] §f" + item.displayName() + " §a×" + multiplier + " 구매 §7(-" + totalCost + "G)");
    }
}
