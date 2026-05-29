package com.poro.empire.listener;

import com.poro.empire.growth.island.IslandTerritoryState;
import com.poro.empire.growth.island.IslandTerritoryStateStore;
import com.poro.empire.gui.GuiTitles;
import com.poro.empire.gui.TerritoryFacilityGui;
import com.poro.empire.gui.TerritoryHubGui;
import com.poro.empire.gui.TerritorySettingsGui;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public final class TerritorySettingsGuiListener implements Listener {

    private final IslandTerritoryStateStore islandTerritoryStateStore;

    public TerritorySettingsGuiListener(IslandTerritoryStateStore islandTerritoryStateStore) {
        this.islandTerritoryStateStore = islandTerritoryStateStore;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        if (GuiTitles.TERRITORY_SETTINGS.equals(event.getView().title())) {
            event.setCancelled(true);
            handleSettings(player, event.getRawSlot());
        } else if (GuiTitles.TERRITORY_FACILITY.equals(event.getView().title())) {
            event.setCancelled(true);
            if (event.getRawSlot() == TerritoryFacilityGui.SLOT_BACK) {
                IslandTerritoryState territory = territory(player);
                TerritorySettingsGui.open(player, territory);
            }
        }
    }

    private void handleSettings(Player player, int slot) {
        switch (slot) {
            case TerritorySettingsGui.SLOT_FACILITY -> {
                TerritoryFacilityGui.open(player, territory(player));
            }
            case TerritorySettingsGui.SLOT_BACK -> TerritoryHubGui.open(player);
        }
        // row0 설정 버튼·멤버 슬롯: 준비 중 (무반응)
    }

    private IslandTerritoryState territory(Player player) {
        return islandTerritoryStateStore.getOrCreate(player.getUniqueId(), player.getName());
    }
}
