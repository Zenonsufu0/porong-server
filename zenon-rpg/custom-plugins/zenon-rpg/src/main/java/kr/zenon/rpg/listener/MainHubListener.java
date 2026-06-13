package kr.zenon.rpg.listener;

import kr.zenon.rpg.combat.CombatStateService;
import kr.zenon.rpg.growth.island.IslandStorageStore;
import kr.zenon.rpg.growth.island.IslandTerritoryState;
import kr.zenon.rpg.growth.island.IslandTerritoryStateStore;
import kr.zenon.rpg.gui.GuiTitles;
import kr.zenon.rpg.gui.MainHubGui;
import kr.zenon.rpg.gui.StorageGui;
import kr.zenon.rpg.gui.TerritoryHubGui;
import kr.zenon.rpg.gui.TerritoryMoveGui;
import kr.zenon.rpg.gui.TerritorySettingsGui;
import kr.zenon.rpg.gui.TerritoryStatusGui;
import kr.zenon.rpg.gui.WorkshopGui;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class MainHubListener implements Listener {

    private final GrowthGuiListener         growthGuiListener;
    private final IslandTerritoryStateStore islandTerritoryStateStore;
    private final IslandStorageStore        islandStorageStore;
    private final AuctionGuiListener        auctionGuiListener;
    private final FieldHubListener          fieldHubListener;
    private final BossHubListener           bossHubListener;
    private final ShopGuiListener           shopGuiListener;
    private final PvpHubListener            pvpHubListener;
    private final CombatStateService        combatStateService;
    /** 보스룸 이동 포기 확인 (세터 주입, nullable). */
    private BossAbandonListener bossAbandonListener;

    public void setBossAbandonListener(BossAbandonListener l) { this.bossAbandonListener = l; }

    /** 영지 이동 GUI 페이지 추적 (in-memory). */
    private final Map<UUID, Integer> territoryMovePage = new ConcurrentHashMap<>();

    public MainHubListener(
            GrowthGuiListener growthGuiListener,
            IslandStorageStore islandStorageStore,
            IslandTerritoryStateStore islandTerritoryStateStore,
            AuctionGuiListener auctionGuiListener,
            FieldHubListener fieldHubListener,
            BossHubListener bossHubListener,
            ShopGuiListener shopGuiListener,
            PvpHubListener pvpHubListener,
            CombatStateService combatStateService
    ) {
        this.growthGuiListener        = growthGuiListener;
        this.islandTerritoryStateStore = islandTerritoryStateStore;
        this.islandStorageStore       = islandStorageStore;
        this.auctionGuiListener       = auctionGuiListener;
        this.fieldHubListener         = fieldHubListener;
        this.bossHubListener          = bossHubListener;
        this.shopGuiListener          = shopGuiListener;
        this.pvpHubListener           = pvpHubListener;
        this.combatStateService       = combatStateService;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;
        Player player = event.getPlayer();
        if (player.getInventory().getHeldItemSlot() != kr.zenon.rpg.init.ClassInitService.MENU_ITEM_SLOT) return;
        if (!kr.zenon.rpg.init.ClassInitService.isMenuItem(player.getInventory().getItemInMainHand())) return;
        event.setCancelled(true);
        MainHubGui.open(player);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        if (GuiTitles.MAIN_HUB.equals(event.getView().title())) {
            event.setCancelled(true);
            int slot = event.getRawSlot();
            if      (MainHubGui.ZONE_EQUIP.contains(slot))     growthGuiListener.openEquipHub(player);
            else if (MainHubGui.ZONE_TERRITORY.contains(slot)) openTerritoryHub(player);
            else if (MainHubGui.ZONE_BOSS.contains(slot))      bossHubListener.openBossHub(player);
            else if (MainHubGui.ZONE_EXPLORE.contains(slot))   fieldHubListener.openFieldHub(player);
            else if (MainHubGui.ZONE_PVP.contains(slot))       pvpHubListener.openHub(player);
            else if (MainHubGui.ZONE_AUCTION.contains(slot))   auctionGuiListener.openMain(player);
            return;
        }

        if (GuiTitles.TERRITORY_HUB.equals(event.getView().title())) {
            event.setCancelled(true);
            handleTerritoryHub(player, event.getRawSlot());
            return;
        }

        if (GuiTitles.TERRITORY_MOVE.equals(event.getView().title())) {
            event.setCancelled(true);
            handleTerritoryMove(player, event.getRawSlot());
        }
    }

    // ─── 서브 GUI 오픈 ───────────────────────────────────────────────

    private void openTerritoryHub(Player player) {
        TerritoryHubGui.open(player);
    }

    private void handleTerritoryHub(Player player, int slot) {
        if (TerritoryHubGui.ZONE_MOVE.contains(slot)) {
            IslandTerritoryState territory = islandTerritoryStateStore.getOrCreate(
                    player.getUniqueId(), player.getName());
            territoryMovePage.put(player.getUniqueId(), 0);
            TerritoryMoveGui.open(player, territory, islandTerritoryStateStore, 0);
        } else if (TerritoryHubGui.ZONE_STATUS.contains(slot)) {
            IslandTerritoryState territory = islandTerritoryStateStore.getOrCreate(
                    player.getUniqueId(), player.getName());
            var storage = islandStorageStore.getOrCreate(player.getUniqueId());
            TerritoryStatusGui.open(player, territory, storage);
        } else if (TerritoryHubGui.ZONE_STORAGE.contains(slot)) {
            var storage = islandStorageStore.getOrCreate(player.getUniqueId());
            StorageGui.open(player, islandTerritoryStateStore.getOrCreate(player.getUniqueId()), storage, 0);
        } else if (TerritoryHubGui.ZONE_WORKSHOP.contains(slot)) {
            IslandTerritoryState territory = islandTerritoryStateStore.getOrCreate(
                    player.getUniqueId(), player.getName());
            WorkshopGui.open(player, WorkshopGui.WorkshopTab.ESTATE, territory);
        } else if (TerritoryHubGui.ZONE_SHOP.contains(slot)) {
            shopGuiListener.openShop(player);
        } else if (TerritoryHubGui.ZONE_SETTINGS.contains(slot)) {
            if (combatStateService.isInCombat(player.getUniqueId())) {
                player.sendMessage("§c[영지] 전투 중에는 영지 설정을 열 수 없습니다.");
                return;
            }
            IslandTerritoryState territory = islandTerritoryStateStore.getOrCreate(
                    player.getUniqueId(), player.getName());
            TerritorySettingsGui.open(player, territory);
        }
        // 테두리 슬롯은 클릭 무반응
    }

    private void handleTerritoryMove(Player player, int slot) {
        UUID uid = player.getUniqueId();
        int page = territoryMovePage.getOrDefault(uid, 0);

        switch (slot) {
            case TerritoryMoveGui.SLOT_MY_ISLAND -> {
                // 보스룸에서 이동 시 포기 확인 (DL-129 추가#20)
                if (bossAbandonListener != null && bossAbandonListener.promptIfInRoom(player, "home")) return;
                // 영지 이동은 전투 중에도 허용 (필드 이탈 수단)
                player.closeInventory();
                if (!player.performCommand("is home")) {
                    player.sendMessage("§c[영지] 이동에 실패했습니다.");
                }
                return;
            }
            case TerritoryMoveGui.SLOT_BACK -> {
                territoryMovePage.remove(uid);
                openTerritoryHub(player);
                return;
            }
            case TerritoryMoveGui.SLOT_PREV_PAGE -> {
                if (page > 0) {
                    territoryMovePage.put(uid, page - 1);
                    var t = islandTerritoryStateStore.getOrCreate(uid, player.getName());
                    TerritoryMoveGui.open(player, t, islandTerritoryStateStore, page - 1);
                }
                return;
            }
            case TerritoryMoveGui.SLOT_NEXT_PAGE -> {
                territoryMovePage.put(uid, page + 1);
                var t = islandTerritoryStateStore.getOrCreate(uid, player.getName());
                TerritoryMoveGui.open(player, t, islandTerritoryStateStore, page + 1);
                return;
            }
        }

        // 공개 영지 슬롯
        UUID targetOwner = TerritoryMoveGui.publicOwnerAt(islandTerritoryStateStore, uid, slot, page);
        if (targetOwner == null) return;
        // 영지 이동은 전투 중에도 허용 (필드 이탈 수단)
        // 방문 설정 재확인
        var targetTerritory = islandTerritoryStateStore.get(targetOwner).orElse(null);
        if (targetTerritory == null || targetTerritory.visitMode() != IslandTerritoryState.VisitMode.PUBLIC) {
            player.sendMessage("§c[영지] 해당 영지에 입장할 수 없습니다.");
            return;
        }
        String ownerName = org.bukkit.Bukkit.getOfflinePlayer(targetOwner).getName();
        if (ownerName == null) {
            player.sendMessage("§c[영지] 소유자 정보를 찾을 수 없습니다.");
            return;
        }
        // 보스룸에서 이동 시 포기 확인 (DL-129 추가#20)
        if (bossAbandonListener != null && bossAbandonListener.promptIfInRoom(player, "visit:" + ownerName)) return;
        player.closeInventory();
        // IridiumSkyblock /is visit <player>
        if (!player.performCommand("is visit " + ownerName)) {
            player.sendMessage("§c[영지] 방문에 실패했습니다.");
        }
    }
}
