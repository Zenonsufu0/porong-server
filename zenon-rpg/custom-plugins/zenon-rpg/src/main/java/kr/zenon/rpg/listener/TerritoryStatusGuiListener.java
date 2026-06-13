package kr.zenon.rpg.listener;

import kr.zenon.rpg.growth.GrowthStateStore;
import kr.zenon.rpg.growth.engine.PlayerGrowthState;
import kr.zenon.rpg.growth.island.IslandRank;
import kr.zenon.rpg.growth.island.IslandRank.UpgradeMaterial;
import kr.zenon.rpg.growth.island.IslandStorage;
import kr.zenon.rpg.growth.island.IslandStorageStore;
import kr.zenon.rpg.growth.island.IslandTerritoryState;
import kr.zenon.rpg.growth.island.IslandTerritoryStateStore;
import kr.zenon.rpg.gui.StorageGui;
import kr.zenon.rpg.gui.TerritoryHubGui;
import kr.zenon.rpg.gui.TerritoryStatusGui;
import kr.zenon.rpg.gui.WorkshopGui;
import kr.zenon.rpg.storage.PlayerDataManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.Optional;

public final class TerritoryStatusGuiListener implements Listener {

    private static final long CONV_COST = 50_000L;
    private static final NumberFormat FMT = NumberFormat.getNumberInstance(Locale.KOREA);

    private final IslandTerritoryStateStore islandTerritoryStateStore;
    private final IslandStorageStore islandStorageStore;
    private final GrowthStateStore growthStateStore;
    @SuppressWarnings("unused")
    private final PlayerDataManager playerDataManager;
    private final kr.zenon.rpg.scoreboard.ScoreboardService scoreboardService;

    public TerritoryStatusGuiListener(
            IslandTerritoryStateStore islandTerritoryStateStore,
            IslandStorageStore islandStorageStore,
            GrowthStateStore growthStateStore,
            PlayerDataManager playerDataManager,
            kr.zenon.rpg.scoreboard.ScoreboardService scoreboardService
    ) {
        this.islandTerritoryStateStore = islandTerritoryStateStore;
        this.islandStorageStore        = islandStorageStore;
        this.growthStateStore          = growthStateStore;
        this.playerDataManager         = playerDataManager;
        this.scoreboardService         = scoreboardService;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!TerritoryStatusGui.isTitle(event.getView().title())) return;

        event.setCancelled(true);
        if (event.getCurrentItem() == null) return;

        IslandTerritoryState territory = islandTerritoryStateStore.getOrCreate(
                player.getUniqueId(), player.getName());
        IslandStorage storage = islandStorageStore.getOrCreate(player.getUniqueId());

        int slot = event.getRawSlot();
        switch (slot) {
            case TerritoryStatusGui.SLOT_RANK_PROGRESS   -> handleRankUpgrade(player, territory, storage);
            case TerritoryStatusGui.SLOT_TOGGLE_DEPOSIT  ->
                    handleConvToggle(player, territory, storage, IslandTerritoryState.CONV_AUTO_DEPOSIT, "자동 입금");
            case TerritoryStatusGui.SLOT_TOGGLE_PLANT    ->
                    handleConvToggle(player, territory, storage, IslandTerritoryState.CONV_AUTO_PLANT, "자동 심기");
            case TerritoryStatusGui.SLOT_WORKSHOP_MACHINE -> WorkshopGui.open(player, WorkshopGui.WorkshopTab.ESTATE, territory);
            case TerritoryStatusGui.SLOT_STORAGE_MACHINE  -> StorageGui.open(player, territory, storage, 0);
            case TerritoryStatusGui.SLOT_BACK -> TerritoryHubGui.open(player);
            default -> { /* 정보 전용 슬롯 — 무시 */ }
        }
    }

    private void handleRankUpgrade(Player player, IslandTerritoryState territory, IslandStorage storage) {
        IslandRank current = territory.rank();
        IslandRank next = current.next();
        if (next == null) {
            player.sendMessage("§6§l☆ 이미 최고 단계(공작령)입니다.");
            return;
        }

        Optional<PlayerGrowthState> growthOpt = growthStateStore.get(player.getUniqueId());
        if (growthOpt.isEmpty()) {
            player.sendMessage("§c성장 데이터를 찾을 수 없습니다. 잠시 후 다시 시도해 주세요.");
            return;
        }
        PlayerGrowthState growth = growthOpt.get();

        // 골드 검증
        if (growth.currency("gold") < current.goldUpgradeCost) {
            long have = growth.currency("gold");
            player.sendMessage("§c골드 부족: 필요 §e" + FMT.format(current.goldUpgradeCost)
                    + "G§c, 보유 §e" + FMT.format(have) + "G");
            return;
        }

        // 재료 검증 — IslandTerritoryState.customItems (필드/보스 드랍 저장 경로와 동일)
        for (UpgradeMaterial mat : current.upgradeMaterials) {
            long have = territory.getCustomItem(mat.itemId());
            if (have < mat.amount()) {
                player.sendMessage("§c재료 부족: §e" + kr.zenon.rpg.gui.WorkshopRecipeRegistry.displayName(mat.itemId())
                        + " §c" + FMT.format(mat.amount()) + "개 필요, 보유 §e" + FMT.format(have) + "개");
                return;
            }
        }

        // 차감 — 골드
        growth.consumeCurrency("gold", current.goldUpgradeCost);

        // 차감 — 재료 (customItems)
        for (UpgradeMaterial mat : current.upgradeMaterials) {
            territory.withdrawCustomItem(mat.itemId(), mat.amount());
        }

        territory.setRank(next);

        // 시설 자동 레벨업 알림 (레벨 파생은 rank.tier 기반, 별도 필드 없음)
        boolean lv2Unlocked = current.tier < 3 && next.tier >= 3; // BARON 달성
        boolean lv3Unlocked = current.tier < 5 && next.tier >= 5; // COUNT 달성
        if (lv2Unlocked) player.sendMessage("§b[영지] 약초 재배지·광물 채굴기가 Lv2로 자동 승급되었습니다!");
        if (lv3Unlocked) player.sendMessage("§b[영지] 약초 재배지·광물 채굴기가 Lv3로 자동 승급되었습니다!");

        // TODO: IridiumSkyblock API로 섬 XZ 한도 확장 (API JAR 미포함 — §7+ 연동 예정)
        // SkyblockAPI.getIslandManager().getIslandByOwner(player).ifPresent(island -> island.setBorderSize(next의 XZ 크기));

        TerritoryStatusGui.open(player, territory, storage);
        scoreboardService.refresh(player);
        player.sendMessage("§a[영지] 작위 승급: §e" + current.displayName + " §7→ §e" + next.displayName + "§a!");
    }

    private void handleConvToggle(Player player, IslandTerritoryState territory,
                                  IslandStorage storage, int bit, String name) {
        if (territory.hasConvenience(bit)) {
            player.sendMessage("§7" + name + "은(는) 이미 해금되어 있습니다.");
            return;
        }

        Optional<PlayerGrowthState> growthOpt = growthStateStore.get(player.getUniqueId());
        if (growthOpt.isEmpty()) {
            player.sendMessage("§c성장 데이터를 찾을 수 없습니다. 잠시 후 다시 시도해 주세요.");
            return;
        }
        PlayerGrowthState growth = growthOpt.get();
        if (!growth.consumeCurrency("gold", CONV_COST)) {
            long have = growth.currency("gold");
            player.sendMessage("§c골드 부족: 필요 §e" + FMT.format(CONV_COST) + "G§c, 보유 §e" + FMT.format(have) + "G");
            return;
        }

        territory.unlockConvenience(bit);
        TerritoryStatusGui.open(player, territory, storage);
        scoreboardService.refresh(player);
        player.sendMessage("§a" + name + " 해금 완료! §7(-" + FMT.format(CONV_COST) + "G)");
    }
}
