package com.poro.empire.listener;

import com.poro.empire.combat.CombatStateService;
import com.poro.empire.growth.island.IslandStorageStore;
import com.poro.empire.growth.island.IslandTerritoryState;
import com.poro.empire.growth.island.IslandTerritoryStateStore;
import com.poro.empire.gui.GuiTitles;
import com.poro.empire.gui.MainHubGui;
import com.poro.empire.gui.StorageGui;
import com.poro.empire.gui.TerritoryHubGui;
import com.poro.empire.gui.TerritorySettingsGui;
import com.poro.empire.gui.TerritoryStatusGui;
import com.poro.empire.gui.WorkshopGui;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public final class MainHubListener implements Listener {

    private final GrowthGuiListener         growthGuiListener;
    private final IslandTerritoryStateStore islandTerritoryStateStore;
    private final IslandStorageStore        islandStorageStore;
    private final AuctionGuiListener        auctionGuiListener;
    private final FieldHubListener          fieldHubListener;
    private final BossHubListener           bossHubListener;
    private final CombatStateService        combatStateService;

    public MainHubListener(
            GrowthGuiListener growthGuiListener,
            IslandStorageStore islandStorageStore,
            IslandTerritoryStateStore islandTerritoryStateStore,
            AuctionGuiListener auctionGuiListener,
            FieldHubListener fieldHubListener,
            BossHubListener bossHubListener,
            CombatStateService combatStateService
    ) {
        this.growthGuiListener        = growthGuiListener;
        this.islandTerritoryStateStore = islandTerritoryStateStore;
        this.islandStorageStore       = islandStorageStore;
        this.auctionGuiListener       = auctionGuiListener;
        this.fieldHubListener         = fieldHubListener;
        this.bossHubListener          = bossHubListener;
        this.combatStateService       = combatStateService;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;
        Player player = event.getPlayer();
        if (player.getInventory().getHeldItemSlot() != 8) return;
        ItemStack held = player.getInventory().getItem(8);
        if (held == null || held.getType() != Material.COMPASS) return;
        event.setCancelled(true);
        MainHubGui.open(player);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        if (GuiTitles.MAIN_HUB.equals(event.getView().title())) {
            event.setCancelled(true);
            int slot = event.getRawSlot();
            if      (MainHubGui.ZONE_EQUIP.contains(slot))     growthGuiListener.openEquipHub(player);
            else if (MainHubGui.ZONE_TERRITORY.contains(slot)) openTerritoryHub(player);
            else if (MainHubGui.ZONE_BOSS.contains(slot))      bossHubListener.openBossHub(player);
            else if (MainHubGui.ZONE_EXPLORE.contains(slot))   fieldHubListener.openFieldHub(player);
            else if (MainHubGui.ZONE_PVP.contains(slot))       player.sendMessage("§7[PvP] 준비 중입니다.");
            else if (MainHubGui.ZONE_AUCTION.contains(slot))   auctionGuiListener.openMain(player);
            return;
        }

        if (GuiTitles.TERRITORY_HUB.equals(event.getView().title())) {
            event.setCancelled(true);
            handleTerritoryHub(player, event.getRawSlot());
        }
    }

    // ─── 서브 GUI 오픈 ───────────────────────────────────────────────

    private void openTerritoryHub(Player player) {
        TerritoryHubGui.open(player);
    }

    private void handleTerritoryHub(Player player, int slot) {
        if (TerritoryHubGui.ZONE_STATUS.contains(slot)) {
            IslandTerritoryState territory = islandTerritoryStateStore.getOrCreate(
                    player.getUniqueId(), player.getName());
            var storage = islandStorageStore.getOrCreate(player.getUniqueId());
            TerritoryStatusGui.open(player, territory, storage);
        } else if (TerritoryHubGui.ZONE_STORAGE.contains(slot)) {
            var storage = islandStorageStore.getOrCreate(player.getUniqueId());
            StorageGui.open(player, storage, 0);
        } else if (TerritoryHubGui.ZONE_WORKSHOP.contains(slot)) {
            IslandTerritoryState territory = islandTerritoryStateStore.getOrCreate(
                    player.getUniqueId(), player.getName());
            WorkshopGui.open(player, WorkshopGui.WorkshopTab.ESTATE, territory);
        } else if (TerritoryHubGui.ZONE_SETTINGS.contains(slot)) {
            if (combatStateService.isInCombat(player.getUniqueId())) {
                player.sendMessage("§c[영지] 전투 중에는 영지 설정을 열 수 없습니다.");
                return;
            }
            IslandTerritoryState territory = islandTerritoryStateStore.getOrCreate(
                    player.getUniqueId(), player.getName());
            TerritorySettingsGui.open(player, territory);
        }
        // 이동·상점 구역 및 테두리 슬롯은 클릭 무반응
    }
}
