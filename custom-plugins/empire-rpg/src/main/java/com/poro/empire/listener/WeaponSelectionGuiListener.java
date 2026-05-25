package com.poro.empire.listener;

import com.poro.empire.combat.weapon.WeaponType;
import com.poro.empire.gui.GuiTitles;
import com.poro.empire.init.ClassInitService;
import com.poro.empire.scoreboard.ScoreboardService;
import com.poro.empire.storage.PlayerDataManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public final class WeaponSelectionGuiListener implements Listener {
    private final PlayerDataManager playerDataManager;
    private final ScoreboardService scoreboardService;
    private final ClassInitService classInitService;

    public WeaponSelectionGuiListener(PlayerDataManager playerDataManager,
                                      ScoreboardService scoreboardService,
                                      ClassInitService classInitService) {
        this.playerDataManager = playerDataManager;
        this.scoreboardService = scoreboardService;
        this.classInitService = classInitService;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!GuiTitles.WEAPON_SELECTION.equals(event.getView().title())) return;

        WeaponType selected = switch (event.getRawSlot()) {
            case 10 -> WeaponType.SWORD;
            case 11 -> WeaponType.AXE;
            case 12 -> WeaponType.SPEAR;
            case 13 -> WeaponType.CROSSBOW;
            case 14 -> WeaponType.SCYTHE;
            case 15 -> WeaponType.STAFF;
            default -> null;
        };
        if (selected == null) return;

        event.setCancelled(true);
        if (playerDataManager.hasSelectedWeapon(player)) {
            player.sendMessage("§c이미 무기 클래스를 선택했습니다.");
            return;
        }
        playerDataManager.setWeaponType(player, selected);
        classInitService.grantStarterEquipment(player, selected);
        scoreboardService.refresh(player);
        player.closeInventory();
        player.sendMessage("§a무기 클래스 선택: " + selected.name().toLowerCase(java.util.Locale.ROOT));
    }
}
