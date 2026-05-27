package com.poro.empire.listener;

import com.poro.empire.combat.CombatStateService;
import com.poro.empire.combat.weapon.WeaponType;
import com.poro.empire.field.FieldTeleportService;
import com.poro.empire.growth.GrowthStateStore;
import com.poro.empire.growth.engine.EquipmentSlot;
import com.poro.empire.growth.engine.GrowthEngineRuntime;
import com.poro.empire.growth.engine.PlayerEquipmentItem;
import com.poro.empire.growth.engine.PlayerGrowthState;
import com.poro.empire.growth.island.IslandStorageStore;
import com.poro.empire.growth.island.IslandTerritoryState;
import com.poro.empire.growth.island.IslandTerritoryStateStore;
import com.poro.empire.gui.ExploreHubGui;
import com.poro.empire.gui.GuiTitles;
import com.poro.empire.gui.MainHubGui;
import com.poro.empire.gui.StorageGui;
import com.poro.empire.gui.TerritoryHubGui;
import com.poro.empire.gui.TerritoryStatusGui;
import com.poro.empire.gui.WorkshopGui;
import com.poro.empire.scoreboard.ScoreboardService;
import com.poro.empire.storage.PlayerDataManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class MainHubListener implements Listener {

    private final GrowthStateStore          growthStateStore;
    private final IslandTerritoryStateStore islandTerritoryStateStore;
    private final IslandStorageStore        islandStorageStore;
    private final PlayerDataManager         playerDataManager;
    private final AuctionGuiListener        auctionGuiListener;
    private final FieldHubListener          fieldHubListener;
    private final BossHubListener           bossHubListener;

    public MainHubListener(
            Plugin plugin,
            ExploreHubGui.FieldStateProvider fieldStateProvider,
            GrowthStateStore growthStateStore,
            GrowthEngineRuntime growthEngineRuntime,
            ScoreboardService scoreboardService,
            PlayerDataManager playerDataManager,
            IslandStorageStore islandStorageStore,
            IslandTerritoryStateStore islandTerritoryStateStore,
            FieldTeleportService fieldTeleportService,
            CombatStateService combatStateService,
            AuctionGuiListener auctionGuiListener,
            BossRoomListener bossRoomListener,
            FieldHubListener fieldHubListener,
            BossHubListener bossHubListener
    ) {
        this.growthStateStore          = growthStateStore;
        this.islandTerritoryStateStore = islandTerritoryStateStore;
        this.islandStorageStore        = islandStorageStore;
        this.playerDataManager         = playerDataManager;
        this.auctionGuiListener        = auctionGuiListener;
        this.fieldHubListener          = fieldHubListener;
        this.bossHubListener           = bossHubListener;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;
        Player player = event.getPlayer();
        if (player.getInventory().getHeldItemSlot() != 8) return;
        ItemStack held = player.getInventory().getItem(8);
        if (held == null || held.getType() != Material.COMPASS) return;
        event.setCancelled(true);
        MainHubGui.open(player);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        if (GuiTitles.MAIN_HUB.equals(event.getView().title())) {
            event.setCancelled(true);
            switch (event.getRawSlot()) {
                case 20 -> openEquipmentHub(player);
                case 22 -> openTerritoryHub(player);
                case 24 -> bossHubListener.openBossHub(player);
                case 26 -> auctionGuiListener.openMain(player);
                case 31 -> fieldHubListener.openFieldHub(player);
                case 49 -> player.closeInventory();
            }
            return;
        }

        if (GuiTitles.TERRITORY_HUB.equals(event.getView().title())) {
            event.setCancelled(true);
            handleTerritoryHub(player, event.getRawSlot());
        }
    }

    // ─── 서브 GUI 오픈 ───────────────────────────────────────────

    private void openEquipmentHub(Player player) {
        WeaponType wt = playerDataManager.getWeaponType(player);
        if (wt == WeaponType.NONE) {
            player.sendMessage("§c[장비] 직업을 먼저 선택해야 합니다.");
            return;
        }
        PlayerGrowthState state = growthStateStore.getOrCreate(
                player.getUniqueId(), wt.name().toLowerCase(Locale.ROOT));

        Inventory gui = Bukkit.createInventory(null, 54, GuiTitles.EQUIPMENT_HUB);
        ItemStack pane = MainHubGui.icon(Material.BLACK_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < 54; i++) gui.setItem(i, pane);

        gui.setItem(10, equipSlotIcon(state, EquipmentSlot.WEAPON,     "무기",  weaponMat(wt)));
        gui.setItem(11, equipSlotIcon(state, EquipmentSlot.HELMET,     "투구",  Material.NETHERITE_HELMET));
        gui.setItem(12, equipSlotIcon(state, EquipmentSlot.CHESTPLATE, "갑옷",  Material.NETHERITE_CHESTPLATE));
        gui.setItem(13, equipSlotIcon(state, EquipmentSlot.LEGGINGS,   "각반",  Material.NETHERITE_LEGGINGS));
        gui.setItem(14, equipSlotIcon(state, EquipmentSlot.BOOTS,      "신발",  Material.NETHERITE_BOOTS));

        gui.setItem(29, MainHubGui.icon(Material.ANVIL,         "§f강화",     List.of("§7──────────────", "§7클릭하여 열기")));
        gui.setItem(31, MainHubGui.icon(Material.NETHER_STAR,   "§f잠재능력", List.of("§7──────────────", "§7클릭하여 열기")));
        gui.setItem(33, MainHubGui.icon(Material.ENCHANTED_BOOK,"§f전승",     List.of("§7──────────────", "§7클릭하여 열기")));
        gui.setItem(45, MainHubGui.icon(Material.ARROW,         "§7뒤로",     List.of("§7메인 메뉴")));
        gui.setItem(49, MainHubGui.icon(Material.BARRIER,       "§c닫기",     List.of()));

        player.openInventory(gui);
    }

    private void openTerritoryHub(Player player) {
        TerritoryHubGui.open(player);
    }

    private void handleTerritoryHub(Player player, int slot) {
        if (TerritoryHubGui.ZONE_STATUS.contains(slot)) {
            IslandTerritoryState territory = islandTerritoryStateStore.getOrCreate(
                    player.getUniqueId(), player.getName());
            var storage = islandStorageStore.getOrCreate(player.getUniqueId());
            TerritoryStatusGui.open(player, territory, storage);
        } else if (TerritoryHubGui.ZONE_STORAGE.contains(slot)) {
            var storage = islandStorageStore.getOrCreate(player.getUniqueId());
            StorageGui.open(player, storage, 0);
        } else if (TerritoryHubGui.ZONE_WORKSHOP.contains(slot)) {
            IslandTerritoryState territory = islandTerritoryStateStore.getOrCreate(
                    player.getUniqueId(), player.getName());
            WorkshopGui.open(player, WorkshopGui.WorkshopTab.ESTATE, territory);
        } else if (TerritoryHubGui.ZONE_AUCTION.contains(slot)) {
            auctionGuiListener.openMain(player);
        }
        // 나머지 구역(이동/시설/상점/설정) 및 장식 슬롯은 클릭 무반응
    }

    // ─── 아이템 빌더 ─────────────────────────────────────────────

    private ItemStack equipSlotIcon(PlayerGrowthState state, EquipmentSlot slot,
                                    String slotName, Material fallbackMat) {
        Optional<String> instId = state.equippedItemInstanceId(slot);
        if (instId.isEmpty()) {
            return MainHubGui.icon(Material.GRAY_STAINED_GLASS_PANE,
                    "§7" + slotName + " §8[미장착]", List.of());
        }
        Optional<PlayerEquipmentItem> eq = state.inventoryItem(instId.get());
        if (eq.isEmpty()) {
            return MainHubGui.icon(Material.GRAY_STAINED_GLASS_PANE,
                    "§7" + slotName + " §8[미장착]", List.of());
        }
        PlayerEquipmentItem item = eq.get();
        return MainHubGui.icon(fallbackMat, "§f" + slotName, List.of(
                "§7──────────────",
                "§7ID: §f" + item.itemId(),
                "§7등급: §e" + item.grade().displayName(),
                "§7강화: §a+" + item.enhanceLevel()
        ));
    }

    private Material weaponMat(WeaponType wt) {
        return switch (wt) {
            case SWORD, SPEAR -> Material.NETHERITE_SWORD;
            case AXE          -> Material.NETHERITE_AXE;
            case CROSSBOW     -> Material.CROSSBOW;
            case SCYTHE       -> Material.NETHERITE_HOE;
            case STAFF        -> Material.BLAZE_ROD;
            case NONE         -> Material.STICK;
        };
    }
}
