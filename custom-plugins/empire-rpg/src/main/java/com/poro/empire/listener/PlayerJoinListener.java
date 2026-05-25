package com.poro.empire.listener;

import com.poro.empire.growth.GrowthStateStore;
import com.poro.empire.growth.island.IslandStorage;
import com.poro.empire.growth.island.IslandStorageStore;
import com.poro.empire.growth.island.IslandTerritoryState;
import com.poro.empire.growth.island.IslandTerritoryStateStore;
import com.poro.empire.growth.island.WorkshopJob;
import com.poro.empire.gui.WorkshopRecipeRegistry;
import com.poro.empire.hotbar.HotbarService;
import com.poro.empire.init.ClassInitService;
import com.poro.empire.market.AuctionStore;
import com.poro.empire.persistence.PlayerPersistenceService;
import com.poro.empire.scoreboard.ScoreboardService;
import com.poro.empire.storage.PlayerDataManager;
import com.poro.empire.tutorial.TutorialService;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.UUID;

public final class PlayerJoinListener implements Listener {
    private final PlayerDataManager playerDataManager;
    private final HotbarService hotbarService;
    private final ScoreboardService scoreboardService;
    private final PlayerPersistenceService playerPersistenceService;
    private final GrowthStateStore growthStateStore;
    private final IslandTerritoryStateStore islandTerritoryStateStore;
    private final IslandStorageStore islandStorageStore;
    private final ClassInitService classInitService;

    public PlayerJoinListener(
            Plugin plugin,
            PlayerDataManager playerDataManager,
            HotbarService hotbarService,
            TutorialService tutorialService,
            ScoreboardService scoreboardService,
            PlayerPersistenceService playerPersistenceService,
            GrowthStateStore growthStateStore,
            IslandTerritoryStateStore islandTerritoryStateStore,
            IslandStorageStore islandStorageStore,
            AuctionStore auctionStore,
            ClassInitService classInitService
    ) {
        this.playerDataManager = playerDataManager;
        this.hotbarService = hotbarService;
        this.scoreboardService = scoreboardService;
        this.playerPersistenceService = playerPersistenceService;
        this.growthStateStore = growthStateStore;
        this.islandTerritoryStateStore = islandTerritoryStateStore;
        this.islandStorageStore = islandStorageStore;
        this.classInitService = classInitService;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        playerDataManager.onPlayerJoin(event.getPlayer());
        playerPersistenceService.load(event.getPlayer().getUniqueId(), event.getPlayer().getName());
        collectWorkshopResults(event.getPlayer());
        hotbarService.updateHotbar(event.getPlayer());
        scoreboardService.refresh(event.getPlayer());
        classInitService.openSelectionGuiIfNeeded(event.getPlayer());
    }

    private void collectWorkshopResults(Player player) {
        UUID uuid = player.getUniqueId();
        IslandTerritoryState territory = islandTerritoryStateStore.get(uuid).orElse(null);
        if (territory == null) return;
        IslandStorage storage = islandStorageStore.get(uuid).orElse(null);

        List<WorkshopJob> done = territory.collectCompletedJobs(System.currentTimeMillis());
        if (done.isEmpty()) return;

        for (WorkshopJob job : done) {
            WorkshopRecipeRegistry.getById(job.recipeId()).ifPresent(recipe -> {
                String resultId = recipe.resultItemId();
                long amount = recipe.resultAmount();
                if (Material.matchMaterial(resultId.toUpperCase()) != null && storage != null) {
                    storage.add(Material.valueOf(resultId.toUpperCase()), amount);
                } else {
                    territory.addCustomItem(resultId, amount);
                }
            });
        }

        player.sendMessage("§a[공방] 완료된 제작 §e" + done.size()
                + "§a건의 결과물이 저장고에 입금되었습니다.");
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        playerPersistenceService.save(uuid);
        playerDataManager.onPlayerQuit(uuid);
        growthStateStore.remove(uuid);
        islandTerritoryStateStore.remove(uuid);
        islandStorageStore.remove(uuid);
    }
}
