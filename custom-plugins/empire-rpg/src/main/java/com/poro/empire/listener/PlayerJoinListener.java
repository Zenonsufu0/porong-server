package com.poro.empire.listener;

import com.poro.empire.growth.GrowthStateStore;
import com.poro.empire.growth.engine.PlayerGrowthState;
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
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class PlayerJoinListener implements Listener {
    private final PlayerDataManager playerDataManager;
    private final HotbarService hotbarService;
    private final ScoreboardService scoreboardService;
    private final PlayerPersistenceService playerPersistenceService;
    private final GrowthStateStore growthStateStore;
    private final IslandTerritoryStateStore islandTerritoryStateStore;
    private final IslandStorageStore islandStorageStore;
    private final AuctionStore auctionStore;
    private final ClassInitService classInitService;
    private final Plugin plugin;

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
        this.plugin = plugin;
        this.playerDataManager = playerDataManager;
        this.hotbarService = hotbarService;
        this.scoreboardService = scoreboardService;
        this.playerPersistenceService = playerPersistenceService;
        this.growthStateStore = growthStateStore;
        this.islandTerritoryStateStore = islandTerritoryStateStore;
        this.islandStorageStore = islandStorageStore;
        this.auctionStore = auctionStore;
        this.classInitService = classInitService;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        playerDataManager.onPlayerJoin(player);
        playerPersistenceService.load(player.getUniqueId(), player.getName());
        collectWorkshopResults(player);
        hotbarService.updateHotbar(player);
        scoreboardService.refresh(player);
        classInitService.openSelectionGuiIfNeeded(player);
        claimAuctionPending(player);
    }

    private void claimAuctionPending(Player player) {
        UUID uuid = player.getUniqueId();
        // 1단계(비동기): DB 조회만 수행 — 삭제 없음
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<AuctionStore.PendingDelivery> deliveries = auctionStore.fetchPending(uuid);
            if (deliveries.isEmpty()) return;

            // 2단계(메인스레드): 메모리 지급
            Bukkit.getScheduler().runTask(plugin, () -> {
                IslandTerritoryState territory = islandTerritoryStateStore.get(uuid).orElse(null);
                PlayerGrowthState growth = growthStateStore.get(uuid).orElse(null);

                // 3단계(메인): 지급 — 상태 미로드 항목은 deliveredIds에서 제외해 다음 로그인에서 재시도
                List<Long> deliveredIds = new ArrayList<>();
                for (AuctionStore.PendingDelivery d : deliveries) {
                    if (d.gold() > 0 && growth != null) {
                        growth.addCurrency("gold", d.gold());
                        deliveredIds.add(d.id());
                        player.sendMessage("§a[경매장] §7판매 완료 수익: §e"
                                + fmt(d.gold()) + "G §7자동 지급됨.");
                    } else if (d.itemId() != null && territory != null) {
                        territory.addCustomItem(d.itemId(), d.quantity());
                        deliveredIds.add(d.id());
                        player.sendMessage("§a[경매장] §7아이템 §f" + d.itemId()
                                + " §7창고에 지급됐습니다.");
                    }
                }

                // 4단계(비동기): 실제 지급 성공한 ID만 삭제
                if (!deliveredIds.isEmpty()) {
                    Bukkit.getScheduler().runTaskAsynchronously(plugin,
                            () -> auctionStore.deletePendingByIds(deliveredIds));
                }
            });
        });
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

    private static String fmt(long value) {
        return NumberFormat.getIntegerInstance(Locale.KOREA).format(value);
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
