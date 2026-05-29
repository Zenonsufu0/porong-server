package com.poro.empire.listener;

import com.poro.empire.gui.GuiTitles;
import com.poro.empire.gui.MainHubGui;
import com.poro.empire.gui.PvpHubGui;
import com.poro.empire.gui.PvpRankingGui;
import com.poro.empire.pvp.PvpRatingService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public final class PvpHubListener implements Listener {

    private final PvpRatingService ratingService;

    public PvpHubListener(PvpRatingService ratingService) {
        this.ratingService = ratingService;
    }

    public void openHub(Player player) {
        PvpHubGui.open(player, ratingService);
    }

    public void openRanking(Player player) {
        PvpRankingGui.open(player, ratingService);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        if (GuiTitles.PVP_HUB.equals(event.getView().title())) {
            event.setCancelled(true);
            handleHub(player, event.getRawSlot());
        } else if (GuiTitles.PVP_RANKING.equals(event.getView().title())) {
            event.setCancelled(true);
            if (event.getRawSlot() == PvpRankingGui.SLOT_BACK) {
                PvpHubGui.open(player, ratingService);
            }
        }
    }

    private void handleHub(Player player, int slot) {
        switch (slot) {
            case PvpHubGui.SLOT_FREE     -> player.sendMessage("§7[PvP] 자유대전은 Phase 2에서 활성화됩니다.");
            case PvpHubGui.SLOT_RANKED   -> player.sendMessage("§7[PvP] 정규대전은 Phase 2에서 활성화됩니다.");
            case PvpHubGui.SLOT_FRIENDLY -> player.sendMessage("§7[PvP] 친선대전은 Phase 2에서 활성화됩니다.");
            case PvpHubGui.SLOT_RANKING  -> PvpRankingGui.open(player, ratingService);
            case PvpHubGui.SLOT_BACK     -> MainHubGui.open(player);
        }
    }
}
