package com.poro.empire.listener;

import com.poro.empire.growth.GrowthStateStore;
import com.poro.empire.hotbar.HotbarService;
import com.poro.empire.init.ClassInitService;
import com.poro.empire.market.AuctionStore;
import com.poro.empire.persistence.PlayerPersistenceService;
import com.poro.empire.scoreboard.ScoreboardService;
import com.poro.empire.storage.PlayerDataManager;
import com.poro.empire.tutorial.TutorialService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

public final class PlayerJoinListener implements Listener {
    private final PlayerDataManager playerDataManager;
    private final HotbarService hotbarService;
    private final ScoreboardService scoreboardService;
    private final PlayerPersistenceService playerPersistenceService;
    private final ClassInitService classInitService;

    public PlayerJoinListener(
            Plugin plugin,
            PlayerDataManager playerDataManager,
            HotbarService hotbarService,
            TutorialService tutorialService,
            ScoreboardService scoreboardService,
            PlayerPersistenceService playerPersistenceService,
            GrowthStateStore growthStateStore,
            AuctionStore auctionStore,
            ClassInitService classInitService
    ) {
        this.playerDataManager = playerDataManager;
        this.hotbarService = hotbarService;
        this.scoreboardService = scoreboardService;
        this.playerPersistenceService = playerPersistenceService;
        this.classInitService = classInitService;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        playerDataManager.onPlayerJoin(event.getPlayer());
        playerPersistenceService.load(event.getPlayer().getUniqueId(), event.getPlayer().getName());
        hotbarService.updateHotbar(event.getPlayer());
        scoreboardService.refresh(event.getPlayer());
        classInitService.openSelectionGuiIfNeeded(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        playerPersistenceService.save(event.getPlayer().getUniqueId());
    }
}
