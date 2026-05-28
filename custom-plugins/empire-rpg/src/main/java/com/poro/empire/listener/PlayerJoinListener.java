package com.poro.empire.listener;

import com.poro.empire.combat.ResourceTracker;
import com.poro.empire.growth.GrowthStateStore;
import com.poro.empire.growth.engine.PlayerGrowthSnapshotBuilder;
import com.poro.empire.growth.engine.PlayerGrowthState;
import com.poro.empire.growth.island.IslandStorage;
import com.poro.empire.life.engine.EstateSnapshotBuilder;
import com.poro.empire.growth.island.IslandStorageStore;
import com.poro.empire.growth.island.IslandTerritoryState;
import com.poro.empire.growth.island.IslandTerritoryStateStore;
import com.poro.empire.growth.island.WorkshopJob;
import com.poro.empire.gui.WorkshopRecipeRegistry;
import com.poro.empire.hotbar.HotbarService;
import com.poro.empire.init.ClassInitService;
import com.poro.empire.boss.party.PartyManager;
import com.poro.empire.boss.room.BossRoomManager;
import com.poro.empire.market.AuctionStore;
import com.poro.empire.operations.query.model.PlayerProfileRecord;
import com.poro.empire.operations.query.store.OperationsDataStore;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class PlayerJoinListener implements Listener {
    private final PlayerDataManager          playerDataManager;
    private final HotbarService              hotbarService;
    private final ScoreboardService          scoreboardService;
    private final PlayerPersistenceService   playerPersistenceService;
    private final GrowthStateStore           growthStateStore;
    private final IslandTerritoryStateStore  islandTerritoryStateStore;
    private final IslandStorageStore         islandStorageStore;
    private final AuctionStore               auctionStore;
    private final ClassInitService           classInitService;
    private final PartyManager               partyManager;
    private final BossRoomManager            bossRoomManager;
    private final Plugin                     plugin;
    private final OperationsDataStore        operationsDataStore;
    private final PlayerGrowthSnapshotBuilder growthSnapshotBuilder;
    private final ResourceTracker             resourceTracker;

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
            ClassInitService classInitService,
            PartyManager partyManager,
            BossRoomManager bossRoomManager,
            OperationsDataStore operationsDataStore,
            PlayerGrowthSnapshotBuilder growthSnapshotBuilder,
            ResourceTracker resourceTracker
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
        this.partyManager = partyManager;
        this.bossRoomManager = bossRoomManager;
        this.operationsDataStore = operationsDataStore;
        this.growthSnapshotBuilder = growthSnapshotBuilder;
        this.resourceTracker = resourceTracker;
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
        classInitService.ensureMenuCompass(player);
        claimAuctionPending(player);
        syncOperationsSnapshot(player);
    }

    private void syncOperationsSnapshot(Player player) {
        String userId = player.getUniqueId().toString();
        String classId = playerDataManager.getWeaponType(player).name().toLowerCase(Locale.ROOT);
        operationsDataStore.upsertPlayerProfile(new PlayerProfileRecord(userId, player.getName(), classId));
        growthStateStore.get(player.getUniqueId()).ifPresent(state ->
                operationsDataStore.upsertGrowthSnapshot(userId, growthSnapshotBuilder.build(state)));
        islandTerritoryStateStore.get(player.getUniqueId()).ifPresent(territory ->
                operationsDataStore.upsertLifeSnapshot(userId, buildLifeSnapshot(territory)));
    }

    private EstateSnapshotBuilder.EstateSnapshot buildLifeSnapshot(IslandTerritoryState territory) {
        // 영지 창고 아이템 전체를 단일 pseudo-facility로 표현
        Map<String, Long> stored = new LinkedHashMap<>();
        territory.customItemsSnapshot().forEach((k, v) -> {
            if (v > 0) stored.put(k, v);
        });

        List<EstateSnapshotBuilder.FacilitySnapshot> facilities = new ArrayList<>();
        if (territory.reaperCount() > 0) {
            facilities.add(new EstateSnapshotBuilder.FacilitySnapshot(
                    0, "생산기×" + territory.reaperCount(), 1, null, 0L, Map.copyOf(stored)));
        }
        if (territory.storageCount() > 0) {
            facilities.add(new EstateSnapshotBuilder.FacilitySnapshot(
                    1, "저장고×" + territory.storageCount(), 1, null, 0L, Map.of()));
        }
        if (facilities.isEmpty()) {
            facilities.add(new EstateSnapshotBuilder.FacilitySnapshot(
                    0, "영지창고", 1, null, 0L, Map.copyOf(stored)));
        }
        return new EstateSnapshotBuilder.EstateSnapshot(
                territory.islandName().isBlank() ? "영지" : territory.islandName(),
                null, true, List.copyOf(facilities), null);
    }

    private void claimAuctionPending(Player player) {
        UUID uuid = player.getUniqueId();
        if (!auctionStore.tryStartClaim(uuid)) return; // 동시 수령 방지

        // 1단계(비동기): DB 조회만 수행 — 삭제 없음
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<AuctionStore.PendingDelivery> deliveries = auctionStore.fetchPending(uuid);
            if (deliveries.isEmpty()) {
                auctionStore.endClaim(uuid);
                return;
            }

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

                if (deliveredIds.isEmpty()) {
                    auctionStore.endClaim(uuid);
                    return;
                }
                // 4단계(비동기): 실제 지급 성공한 ID만 삭제 후 lock 해제
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    try { auctionStore.deletePendingByIds(deliveredIds); }
                    finally { auctionStore.endClaim(uuid); }
                });
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
        String userId = uuid.toString();
        // quit 전 최신 스냅샷 보존 (오프라인 조회용)
        growthStateStore.get(uuid).ifPresent(state ->
                operationsDataStore.upsertGrowthSnapshot(userId, growthSnapshotBuilder.build(state)));
        islandTerritoryStateStore.get(uuid).ifPresent(territory ->
                operationsDataStore.upsertLifeSnapshot(userId, buildLifeSnapshot(territory)));
        playerPersistenceService.save(uuid);
        playerDataManager.onPlayerQuit(uuid);
        growthStateStore.remove(uuid);
        islandTerritoryStateStore.remove(uuid);
        islandStorageStore.remove(uuid);
        partyManager.leaveParty(uuid);
        bossRoomManager.exitRoom(uuid);
        resourceTracker.cleanup(uuid);
    }
}
