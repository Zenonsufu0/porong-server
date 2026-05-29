package com.poro.empire.listener;

import com.poro.empire.gui.GuiTitles;
import com.poro.empire.gui.MainHubGui;
import com.poro.empire.gui.PvpHubGui;
import com.poro.empire.gui.PvpRankingGui;
import com.poro.empire.pvp.PvpMatchService;
import com.poro.empire.pvp.PvpMatchType;
import com.poro.empire.pvp.PvpRatingService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class PvpHubListener implements Listener {

    private final PvpRatingService ratingService;
    private final PvpMatchService  matchService;

    public PvpHubListener(PvpRatingService ratingService, PvpMatchService matchService) {
        this.ratingService = ratingService;
        this.matchService  = matchService;
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
            case PvpHubGui.SLOT_FREE -> {
                player.closeInventory();
                matchService.enqueue(player, PvpMatchType.FREE);
            }
            case PvpHubGui.SLOT_RANKED -> {
                player.closeInventory();
                matchService.enqueue(player, PvpMatchType.RANKED);
            }
            case PvpHubGui.SLOT_FRIENDLY -> player.sendMessage("§7[PvP] 친선대전은 Phase 2e에서 활성화됩니다.");
            case PvpHubGui.SLOT_RANKING  -> PvpRankingGui.open(player, ratingService);
            case PvpHubGui.SLOT_BACK     -> MainHubGui.open(player);
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player p = event.getEntity();
        if (matchService.isInMatch(p.getUniqueId())) {
            matchService.onPlayerDeath(p);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        matchService.onPlayerQuit(event.getPlayer());
    }
}
