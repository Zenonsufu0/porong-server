package com.poro.empire.listener;

import com.poro.empire.growth.GrowthStateStore;
import com.poro.empire.growth.engine.PlayerGrowthState;
import com.poro.empire.growth.island.IslandRank;
import com.poro.empire.growth.island.IslandStorage;
import com.poro.empire.growth.island.IslandStorageStore;
import com.poro.empire.growth.island.IslandTerritoryState;
import com.poro.empire.growth.island.IslandTerritoryStateStore;
import com.poro.empire.gui.StorageGui;
import com.poro.empire.gui.TerritoryHubGui;
import com.poro.empire.gui.TerritoryStatusGui;
import com.poro.empire.gui.WorkshopGui;
import com.poro.empire.storage.PlayerDataManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.Optional;

public final class TerritoryStatusGuiListener implements Listener {

    private static final long CONV_COST = 50_000L;
    private static final NumberFormat FMT = NumberFormat.getNumberInstance(Locale.KOREA);

    private final IslandTerritoryStateStore islandTerritoryStateStore;
    private final IslandStorageStore islandStorageStore;
    private final GrowthStateStore growthStateStore;
    @SuppressWarnings("unused")
    private final PlayerDataManager playerDataManager;

    public TerritoryStatusGuiListener(
            IslandTerritoryStateStore islandTerritoryStateStore,
            IslandStorageStore islandStorageStore,
            GrowthStateStore growthStateStore,
            PlayerDataManager playerDataManager
    ) {
        this.islandTerritoryStateStore = islandTerritoryStateStore;
        this.islandStorageStore        = islandStorageStore;
        this.growthStateStore          = growthStateStore;
        this.playerDataManager         = playerDataManager;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!TerritoryStatusGui.isTitle(event.getView().title())) return;

        event.setCancelled(true);
        if (event.getCurrentItem() == null) return;

        IslandTerritoryState territory = islandTerritoryStateStore.getOrCreate(
                player.getUniqueId(), player.getName());
        IslandStorage storage = islandStorageStore.getOrCreate(player.getUniqueId());

        int slot = event.getRawSlot();
        switch (slot) {
            case TerritoryStatusGui.SLOT_RANK_PROGRESS   -> handleRankUpgrade(player, territory, storage);
            case TerritoryStatusGui.SLOT_TOGGLE_DEPOSIT  ->
                    handleConvToggle(player, territory, storage, IslandTerritoryState.CONV_AUTO_DEPOSIT, "자동 입금");
            case TerritoryStatusGui.SLOT_TOGGLE_PLANT    ->
                    handleConvToggle(player, territory, storage, IslandTerritoryState.CONV_AUTO_PLANT, "자동 심기");
            case TerritoryStatusGui.SLOT_WORKSHOP_MACHINE -> WorkshopGui.open(player, WorkshopGui.WorkshopTab.ESTATE);
            case TerritoryStatusGui.SLOT_STORAGE_MACHINE  -> StorageGui.open(player, storage, 0);
            case TerritoryStatusGui.SLOT_BACK  -> TerritoryHubGui.open(player);
            case TerritoryStatusGui.SLOT_CLOSE -> player.closeInventory();
            default -> { /* 정보 전용 슬롯 — 무시 */ }
        }
    }

    private void handleRankUpgrade(Player player, IslandTerritoryState territory, IslandStorage storage) {
        IslandRank current = territory.rank();
        IslandRank next = current.next();
        if (next == null) {
            player.sendMessage("§6§l☆ 이미 최고 단계(공작령)입니다.");
            return;
        }

        long cost = current.goldUpgradeCost;
        Optional<PlayerGrowthState> growthOpt = growthStateStore.get(player.getUniqueId());
        if (growthOpt.isEmpty()) {
            player.sendMessage("§c성장 데이터를 찾을 수 없습니다. 잠시 후 다시 시도해 주세요.");
            return;
        }
        PlayerGrowthState growth = growthOpt.get();
        if (!growth.consumeCurrency("gold", cost)) {
            long have = growth.currency("gold");
            player.sendMessage("§c골드 부족: 필요 §e" + FMT.format(cost) + "G§c, 보유 §e" + FMT.format(have) + "G");
            return;
        }

        territory.setRank(next);
        TerritoryStatusGui.open(player, territory, storage);
        player.sendMessage("§a작위 승급: §e" + current.displayName + " §7→ §e" + next.displayName);
    }

    private void handleConvToggle(Player player, IslandTerritoryState territory,
                                  IslandStorage storage, int bit, String name) {
        if (territory.hasConvenience(bit)) {
            player.sendMessage("§7" + name + "은(는) 이미 해금되어 있습니다.");
            return;
        }

        Optional<PlayerGrowthState> growthOpt = growthStateStore.get(player.getUniqueId());
        if (growthOpt.isEmpty()) {
            player.sendMessage("§c성장 데이터를 찾을 수 없습니다. 잠시 후 다시 시도해 주세요.");
            return;
        }
        PlayerGrowthState growth = growthOpt.get();
        if (!growth.consumeCurrency("gold", CONV_COST)) {
            long have = growth.currency("gold");
            player.sendMessage("§c골드 부족: 필요 §e" + FMT.format(CONV_COST) + "G§c, 보유 §e" + FMT.format(have) + "G");
            return;
        }

        territory.unlockConvenience(bit);
        TerritoryStatusGui.open(player, territory, storage);
        player.sendMessage("§a" + name + " 해금 완료! §7(-" + FMT.format(CONV_COST) + "G)");
    }
}
