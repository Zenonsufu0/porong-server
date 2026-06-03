package com.poro.rpg.listener;

import com.poro.rpg.field.FieldTeleportService;
import com.poro.rpg.gui.ExploreHubGui;
import com.poro.rpg.gui.FieldHubGui;
import com.poro.rpg.gui.GuiTitles;
import com.poro.rpg.gui.MainHubGui;
import com.poro.rpg.storage.PlayerDataManager;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public final class FieldHubListener implements Listener {

    private final ExploreHubGui.FieldStateProvider fieldStateProvider;
    private final FieldTeleportService             fieldTeleportService;
    private final PlayerDataManager                playerDataManager;

    public FieldHubListener(ExploreHubGui.FieldStateProvider fieldStateProvider,
                             FieldTeleportService fieldTeleportService,
                             PlayerDataManager playerDataManager) {
        this.fieldStateProvider   = fieldStateProvider;
        this.fieldTeleportService = fieldTeleportService;
        this.playerDataManager    = playerDataManager;
    }

    public void openFieldHub(Player player) {
        FieldHubGui.open(player, fieldStateProvider, playerDataManager.isFieldElite(player.getUniqueId()));
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
        if (slot == FieldHubGui.ELITE_TOGGLE_SLOT) {
            boolean on = playerDataManager.toggleFieldElite(player.getUniqueId());
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, on ? 1.2f : 0.8f);
            player.sendMessage(on
                    ? "§8[§e포로§8] §a§l정예 모드 ON §7— 다음 웨이브부터 정예 몹이 등장합니다."
                    : "§8[§e포로§8] §7정예 모드 §fOFF §7— 일반 몹으로 돌아갑니다.");
            openFieldHub(player); // 토글 반영 재오픈
            return;
        }
        if (slot == 18) MainHubGui.open(player);
    }
}
