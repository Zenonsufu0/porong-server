package com.poro.empire.listener;

import com.poro.empire.growth.island.IslandTerritoryStateStore;
import com.poro.empire.gui.TerritoryHubGui;
import com.poro.empire.gui.WorkshopGui;
import com.poro.empire.gui.WorkshopGui.WorkshopTab;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.plugin.Plugin;

/**
 * 공방 GUI 클릭 처리.
 * 탭 슬롯(0~5) 클릭 시 해당 탭으로 GUI 재오픈.
 */
public class WorkshopGuiListener implements Listener {

    private final IslandTerritoryStateStore stateStore;
    private final Plugin plugin;

    public WorkshopGuiListener(IslandTerritoryStateStore stateStore, Plugin plugin) {
        this.stateStore = stateStore;
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onWorkshopClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!WorkshopGui.isTitle(event.getView().title())) return;

        event.setCancelled(true);
        int slot = event.getRawSlot();
        WorkshopTab currentTab = WorkshopGui.tabFromTitle(event.getView().title());
        if (currentTab == null) currentTab = WorkshopTab.ESTATE;

        // 탭 클릭 (슬롯 0~5)
        WorkshopTab[] tabs = WorkshopTab.values();
        if (slot >= 0 && slot < tabs.length) {
            WorkshopTab clicked = tabs[slot];
            if (clicked != currentTab) {
                WorkshopGui.open(player, clicked);
            }
            return;
        }

        // 뒤로
        if (slot == WorkshopGui.SLOT_BACK) {
            TerritoryHubGui.open(player);
            return;
        }
        // 닫기
        if (slot == WorkshopGui.SLOT_CLOSE) {
            player.closeInventory();
        }
    }
}
