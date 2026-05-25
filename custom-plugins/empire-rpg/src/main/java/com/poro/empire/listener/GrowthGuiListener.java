package com.poro.empire.listener;

import com.poro.empire.combat.CombatStateService;
import com.poro.empire.combat.weapon.WeaponType;
import com.poro.empire.common.registry.master.ItemMasterRegistry;
import com.poro.empire.growth.GrowthStateStore;
import com.poro.empire.growth.engine.GrowthEngineRuntime;
import com.poro.empire.growth.engine.PlayerGrowthState;
import com.poro.empire.gui.GuiTitles;
import com.poro.empire.gui.MainHubGui;
import com.poro.empire.scoreboard.ScoreboardService;
import com.poro.empire.storage.PlayerDataManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.Locale;

public final class GrowthGuiListener implements Listener {

    private final GrowthStateStore growthStateStore;
    private final PlayerDataManager playerDataManager;

    public GrowthGuiListener(
            GrowthStateStore growthStateStore,
            GrowthEngineRuntime growthEngineRuntime,
            PlayerDataManager playerDataManager,
            ScoreboardService scoreboardService,
            ItemMasterRegistry itemMasters,
            CombatStateService combatStateService,
            Plugin plugin
    ) {
        this.growthStateStore = growthStateStore;
        this.playerDataManager = playerDataManager;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        if (GuiTitles.EQUIPMENT_HUB.equals(event.getView().title())) {
            event.setCancelled(true);
            handleEquipmentHub(player, event.getRawSlot());
            return;
        }
        if (GuiTitles.GROWTH_ENHANCE.equals(event.getView().title())) {
            event.setCancelled(true);
            if (event.getRawSlot() == 45) MainHubGui.open(player);
            else if (event.getRawSlot() == 49) player.closeInventory();
            return;
        }
        if (GuiTitles.GROWTH_POTENTIAL.equals(event.getView().title())) {
            event.setCancelled(true);
            if (event.getRawSlot() == 45) MainHubGui.open(player);
            else if (event.getRawSlot() == 49) player.closeInventory();
        }
    }

    private void handleEquipmentHub(Player player, int slot) {
        switch (slot) {
            case 29 -> openGrowthEnhance(player);
            case 31 -> openGrowthPotential(player);
            case 33 -> player.sendMessage("§8전승 — 준비 중");
            case 45 -> MainHubGui.open(player);
            case 49 -> player.closeInventory();
        }
    }

    // ─── 강화 GUI (플레이스홀더) ──────────────────────────────────

    private void openGrowthEnhance(Player player) {
        WeaponType wt = playerDataManager.getWeaponType(player);
        PlayerGrowthState state = growthStateStore.getOrCreate(
                player.getUniqueId(), wt.name().toLowerCase(Locale.ROOT));

        Inventory gui = Bukkit.createInventory(null, 54, GuiTitles.GROWTH_ENHANCE);
        ItemStack pane = MainHubGui.icon(Material.BLACK_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < 54; i++) gui.setItem(i, pane);

        gui.setItem(22, MainHubGui.icon(Material.ANVIL, "§f강화 시스템",
                List.of("§7──────────────",
                        "§7현재 장착 아이템 강화",
                        "§7인벤토리 수: §e" + state.inventorySnapshot().size() + "개",
                        "§8──────────────",
                        "§8상세 UI 구현 예정")));
        gui.setItem(45, MainHubGui.icon(Material.ARROW,   "§7뒤로",  List.of("§7메인 메뉴")));
        gui.setItem(49, MainHubGui.icon(Material.BARRIER, "§c닫기",  List.of()));

        player.openInventory(gui);
    }

    // ─── 잠재능력 GUI (플레이스홀더) ─────────────────────────────

    private void openGrowthPotential(Player player) {
        WeaponType wt = playerDataManager.getWeaponType(player);
        PlayerGrowthState state = growthStateStore.getOrCreate(
                player.getUniqueId(), wt.name().toLowerCase(Locale.ROOT));

        Inventory gui = Bukkit.createInventory(null, 54, GuiTitles.GROWTH_POTENTIAL);
        ItemStack pane = MainHubGui.icon(Material.BLACK_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < 54; i++) gui.setItem(i, pane);

        gui.setItem(22, MainHubGui.icon(Material.NETHER_STAR, "§f잠재능력 시스템",
                List.of("§7──────────────",
                        "§7큐브 소비 → 잠재 재롤",
                        "§7인벤토리 수: §e" + state.inventorySnapshot().size() + "개",
                        "§8──────────────",
                        "§8상세 UI 구현 예정")));
        gui.setItem(45, MainHubGui.icon(Material.ARROW,   "§7뒤로",  List.of("§7메인 메뉴")));
        gui.setItem(49, MainHubGui.icon(Material.BARRIER, "§c닫기",  List.of()));

        player.openInventory(gui);
    }
}
