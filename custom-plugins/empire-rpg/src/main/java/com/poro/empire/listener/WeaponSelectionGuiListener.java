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

        event.setCancelled(true);

        // 공용 36칸 레이아웃(WeaponGui)과 같은 슬롯 매핑 — 무기 변경 GUI와 재사용.
        WeaponType selected = com.poro.empire.gui.WeaponGui.slotToWeapon(event.getRawSlot());
        if (selected == null) return;
        if (playerDataManager.hasSelectedWeapon(player)) {
            player.sendMessage("§c이미 무기 클래스를 선택했습니다.");
            return;
        }
        playerDataManager.setWeaponType(player, selected);
        classInitService.grantStarterEquipment(player, selected);
        scoreboardService.refresh(player);
        player.closeInventory();
        player.sendMessage("§a무기 클래스 선택: " + selected.name().toLowerCase(java.util.Locale.ROOT));

        // 첫 접속 온보딩 — IS 영지(섬) 자동 생성 + 이동 (DL-102). poro 스키매틱 강제(스키매틱 GUI 회피).
        // IS create는 비동기 — 생성 후 IS가 새 섬으로 자동 텔레포트한다.
        player.sendMessage("§e[영지] §7당신의 영지를 생성합니다. 잠시만 기다려 주세요...");
        player.performCommand("is create poro");
    }
}
