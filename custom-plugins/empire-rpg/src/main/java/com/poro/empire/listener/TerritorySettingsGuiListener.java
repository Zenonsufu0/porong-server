package com.poro.empire.listener;

import com.poro.empire.combat.CombatStateService;
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
    private final CombatStateService        combatStateService;

    public TerritorySettingsGuiListener(IslandTerritoryStateStore islandTerritoryStateStore,
                                        CombatStateService combatStateService) {
        this.islandTerritoryStateStore = islandTerritoryStateStore;
        this.combatStateService        = combatStateService;
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
                if (combatStateService.isInCombat(player.getUniqueId())) {
                    player.sendMessage("§c[영지] 전투 중에는 영지 설정을 열 수 없습니다.");
                    return;
                }
                TerritorySettingsGui.open(player, territory(player));
            }
        }
    }

    private void handleSettings(Player player, int slot) {
        switch (slot) {
            case TerritorySettingsGui.SLOT_FACILITY -> {
                if (combatStateService.isInCombat(player.getUniqueId())) {
                    player.sendMessage("§c[영지] 전투 중에는 시설 현황을 열 수 없습니다.");
                    return;
                }
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
