package com.poro.empire.listener;

import com.poro.empire.boss.room.BossRoomManager;
import com.poro.empire.boss.room.BossRoomSlot;
import com.poro.empire.growth.GrowthStateStore;
import com.poro.empire.growth.island.IslandTerritoryStateStore;
import com.poro.empire.gui.AdminHubGui;
import com.poro.empire.gui.AdminInspectGui;
import com.poro.empire.gui.AdminMatchesGui;
import com.poro.empire.gui.AdminStatsGui;
import com.poro.empire.gui.GuiTitles;
import com.poro.empire.pvp.PvpArenaManager;
import com.poro.empire.pvp.PvpArenaSlot;
import com.poro.empire.pvp.PvpMatchService;
import com.poro.empire.pvp.PvpRatingService;
import com.poro.empire.storage.PlayerDataManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class AdminGuiListener implements Listener {

    private final PlayerDataManager          playerDataManager;
    private final GrowthStateStore           growthStateStore;
    private final IslandTerritoryStateStore  islandStore;
    private final PvpRatingService           pvpRatingService;
    private final PvpMatchService            pvpMatchService;
    private final PvpArenaManager            pvpArenaManager;
    private final BossRoomManager            bossRoomManager;
    private final long                       seasonStartEpoch;

    /** Anvil 인스펙트 닉네임 입력 대기. */
    private final Set<UUID> pendingInspectAnvil = Collections.newSetFromMap(new ConcurrentHashMap<>());
    /** 진행 중 매치 GUI에서 슬롯 인덱스 → matchId. */
    private final Map<UUID, List<UUID>> matchSlotMapping = new ConcurrentHashMap<>();

    public AdminGuiListener(PlayerDataManager playerDataManager,
                            GrowthStateStore growthStateStore,
                            IslandTerritoryStateStore islandStore,
                            PvpRatingService pvpRatingService,
                            PvpMatchService pvpMatchService,
                            PvpArenaManager pvpArenaManager,
                            BossRoomManager bossRoomManager,
                            long seasonStartEpoch) {
        this.playerDataManager  = playerDataManager;
        this.growthStateStore   = growthStateStore;
        this.islandStore        = islandStore;
        this.pvpRatingService   = pvpRatingService;
        this.pvpMatchService    = pvpMatchService;
        this.pvpArenaManager    = pvpArenaManager;
        this.bossRoomManager    = bossRoomManager;
        this.seasonStartEpoch   = seasonStartEpoch;
    }

    public void openHub(Player admin) {
        AdminHubGui.open(admin);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        UUID uid = player.getUniqueId();

        if (GuiTitles.ADMIN_HUB.equals(event.getView().title())) {
            event.setCancelled(true);
            handleHub(player, event.getRawSlot(), event.getClick());

        } else if (GuiTitles.ADMIN_INSPECT.equals(event.getView().title())) {
            event.setCancelled(true);
            int slot = event.getRawSlot();
            if (slot == AdminInspectGui.SLOT_BACK) AdminHubGui.open(player);
            else if (slot == AdminInspectGui.SLOT_CLOSE) player.closeInventory();

        } else if (GuiTitles.ADMIN_MATCHES.equals(event.getView().title())) {
            event.setCancelled(true);
            handleMatchesClick(player, event.getRawSlot(), event.getClick());

        } else if (GuiTitles.ADMIN_STATS.equals(event.getView().title())) {
            event.setCancelled(true);
            int slot = event.getRawSlot();
            if (slot == AdminStatsGui.SLOT_BACK) AdminHubGui.open(player);
            else if (slot == AdminStatsGui.SLOT_CLOSE) player.closeInventory();

        } else if (pendingInspectAnvil.contains(uid)
                && event.getInventory() instanceof AnvilInventory anvil
                && event.getRawSlot() == 2) {
            event.setCancelled(true);
            String name = anvil.getRenameText();
            pendingInspectAnvil.remove(uid);
            if (name == null || name.isBlank()) {
                player.sendMessage("§c[관리자] 닉네임을 입력하세요.");
                return;
            }
            org.bukkit.OfflinePlayer target = Bukkit.getOfflinePlayerIfCached(name);
            if (target == null) target = Bukkit.getOfflinePlayer(name);
            if (target.getUniqueId() == null) {
                player.sendMessage("§c[관리자] 플레이어를 찾을 수 없습니다.");
                return;
            }
            player.closeInventory();
            AdminInspectGui.open(player, target.getUniqueId(),
                    playerDataManager, growthStateStore, islandStore, pvpRatingService);
        }
    }

    @EventHandler
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        if (!(event.getView().getPlayer() instanceof Player player)) return;
        if (!pendingInspectAnvil.contains(player.getUniqueId())) return;
        String text = event.getInventory().getRenameText();
        if (text == null || text.isBlank()) return;
        event.getInventory().setRepairCost(0);
        ItemStack result = new ItemStack(Material.PAPER);
        ItemMeta meta = result.getItemMeta();
        meta.displayName(Component.text("§e인스펙트: " + text));
        result.setItemMeta(meta);
        event.setResult(result);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player p) {
            pendingInspectAnvil.remove(p.getUniqueId());
        }
    }

    // ─── 핸들러 ──────────────────────────────────────────────────────

    private void handleHub(Player admin, int slot, ClickType click) {
        switch (slot) {
            case AdminHubGui.SLOT_INSPECT -> openInspectAnvil(admin);
            case AdminHubGui.SLOT_MATCHES -> matchSlotMapping.put(admin.getUniqueId(),
                    AdminMatchesGui.open(admin, pvpMatchService));
            case AdminHubGui.SLOT_STATS   -> AdminStatsGui.open(admin,
                    seasonStartEpoch, pvpArenaManager, bossRoomManager, pvpMatchService);
            case AdminHubGui.SLOT_RELEASE -> {
                if (click.isShiftClick()) {
                    forceReleaseAllSlots(admin);
                } else {
                    admin.sendMessage("§e[관리자] §7확인을 위해 §fShift+클릭§7 필요");
                }
            }
            case AdminHubGui.SLOT_CLOSE -> admin.closeInventory();
        }
    }

    private void handleMatchesClick(Player admin, int slot, ClickType click) {
        if (slot == AdminMatchesGui.SLOT_BACK)  { AdminHubGui.open(admin); return; }
        if (slot == AdminMatchesGui.SLOT_CLOSE) { admin.closeInventory(); return; }
        if (slot < 0 || slot > AdminMatchesGui.ITEM_AREA_END) return;
        List<UUID> mapping = matchSlotMapping.getOrDefault(admin.getUniqueId(), List.of());
        if (slot >= mapping.size()) return;
        UUID matchId = mapping.get(slot);
        if (!click.isLeftClick()) return;
        pvpMatchService.adminForceEnd(matchId, "admin_force");
        admin.sendMessage("§a[관리자] 매치 강제 종료 (무승부 처리).");
        // GUI 갱신
        matchSlotMapping.put(admin.getUniqueId(), AdminMatchesGui.open(admin, pvpMatchService));
    }

    private void openInspectAnvil(Player admin) {
        AnvilInventory anvil = (AnvilInventory) admin.getServer()
                .createInventory(null, InventoryType.ANVIL, Component.text("§c플레이어 인스펙트"));
        ItemStack tag = new ItemStack(Material.NAME_TAG);
        ItemMeta meta = tag.getItemMeta();
        meta.displayName(Component.text("닉네임 입력"));
        tag.setItemMeta(meta);
        anvil.setItem(0, tag);
        pendingInspectAnvil.add(admin.getUniqueId());
        admin.openInventory(anvil);
    }

    private void forceReleaseAllSlots(Player admin) {
        int boss = 0;
        for (BossRoomSlot s : bossRoomManager.allSlots()) {
            if (!s.isFree()) { bossRoomManager.releaseSlot(s.id()); boss++; }
        }
        int arena = 0;
        for (PvpArenaSlot s : pvpArenaManager.allSlots()) {
            if (!s.isFree()) {
                pvpArenaManager.releaseByMatchId(s.matchId());
                arena++;
            }
        }
        admin.sendMessage("§a[관리자] 슬롯 강제 해제 — §c보스룸 " + boss + "개§7, §cPvP 아레나 " + arena + "개");
        AdminStatsGui.open(admin, seasonStartEpoch, pvpArenaManager, bossRoomManager, pvpMatchService);
    }
}
