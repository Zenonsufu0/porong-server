package com.poro.rpg.listener;

import com.poro.rpg.field.FieldTeleportService;
import com.poro.rpg.gui.ExploreHubGui;
import com.poro.rpg.gui.FieldHubGui;
import com.poro.rpg.gui.GuiTitles;
import com.poro.rpg.gui.MainHubGui;
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
        if (slot == 18) MainHubGui.open(player);
    }
}
