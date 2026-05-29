package com.poro.empire.listener;

import com.poro.empire.field.FieldTeleportService;
import com.poro.empire.gui.ExploreHubGui;
import com.poro.empire.gui.FieldHubGui;
import com.poro.empire.gui.GuiTitles;
import com.poro.empire.gui.MainHubGui;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public final class FieldHubListener implements Listener {

    private final ExploreHubGui.FieldStateProvider fieldStateProvider;
    private final FieldTeleportService             fieldTeleportService;

    public FieldHubListener(ExploreHubGui.FieldStateProvider fieldStateProvider,
                             FieldTeleportService fieldTeleportService) {
        this.fieldStateProvider   = fieldStateProvider;
        this.fieldTeleportService = fieldTeleportService;
    }

    public void openFieldHub(Player player) {
        FieldHubGui.open(player, fieldStateProvider);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!GuiTitles.FIELD_HUB.equals(event.getView().title())) return;
        event.setCancelled(true);

        int slot = event.getRawSlot();
        String fieldId = FieldHubGui.fieldIdAt(slot);
        if (fieldId != null) {
            fieldTeleportService.teleportToField(player, fieldId);
            return;
        }
        switch (slot) {
            case 45 -> MainHubGui.open(player);
        }
    }
}
