package com.poro.empire.listener;

import com.poro.empire.admin.AdminTogglesService;
import com.poro.empire.admin.AdminTogglesService.Toggle;
import com.poro.empire.boss.engine.BossRunService;
import com.poro.empire.boss.room.BossRoomManager;
import com.poro.empire.boss.room.BossRoomSlot;
import com.poro.empire.growth.GrowthStateStore;
import com.poro.empire.growth.island.IslandRank;
import com.poro.empire.growth.island.IslandTerritoryState;
import com.poro.empire.growth.island.IslandTerritoryStateStore;
import com.poro.empire.growth.engine.InMemoryEnhancementLogHook;
import com.poro.empire.gui.AdminBossGui;
import com.poro.empire.gui.AdminHubGui;
import com.poro.empire.gui.AdminInspectGui;
import com.poro.empire.gui.AdminLogGui;
import com.poro.empire.gui.AdminMatchesGui;
import com.poro.empire.gui.AdminStatsGui;
import com.poro.empire.gui.AdminTerritoryGui;
import com.poro.empire.gui.AdminTogglesGui;
import com.poro.empire.gui.GuiTitles;
import com.poro.empire.market.AuctionStore;
import com.poro.empire.pvp.PvpArenaManager;
import com.poro.empire.pvp.PvpArenaSlot;
import com.poro.empire.pvp.PvpMatchService;
import com.poro.empire.pvp.PvpRatingService;
import com.poro.empire.pvp.db.PvpMatchLogRepository;
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
    private final AdminTogglesService        togglesService;
    private final InMemoryEnhancementLogHook enhanceLog;
    private final AuctionStore               auctionStore;
    private final PvpMatchLogRepository       pvpLog;
    private final BossRunService             bossRunService;
    private final long                       seasonStartEpoch;

    /** Anvil 인스펙트 닉네임 입력 대기. */
    private final Set<UUID> pendingInspectAnvil = Collections.newSetFromMap(new ConcurrentHashMap<>());
    /** 진행 중 매치 GUI에서 슬롯 인덱스 → matchId. */
    private final Map<UUID, List<UUID>> matchSlotMapping = new ConcurrentHashMap<>();
    /** 운영 토글 GUI에서 슬롯 인덱스 → Toggle (open 호출 시 갱신). */
    private final Map<UUID, Toggle[]> toggleSlotMapping = new ConcurrentHashMap<>();
    /** 로그 GUI 현재 보기 상태 (탭 + 페이지). */
    private final Map<UUID, LogView> logView = new ConcurrentHashMap<>();
    /** 보스 디버그 GUI에서 슬롯 인덱스 → runId. */
    private final Map<UUID, List<String>> bossRunMapping = new ConcurrentHashMap<>();
    /** 영지 관리 GUI 보기 상태 (슬롯 인덱스 → ownerUuid + 현재 페이지). */
    private final Map<UUID, AdminTerritoryGui.View> territoryView = new ConcurrentHashMap<>();

    private record LogView(AdminLogGui.LogTab tab, int page) {}

    public AdminGuiListener(PlayerDataManager playerDataManager,
                            GrowthStateStore growthStateStore,
                            IslandTerritoryStateStore islandStore,
                            PvpRatingService pvpRatingService,
                            PvpMatchService pvpMatchService,
                            PvpArenaManager pvpArenaManager,
                            BossRoomManager bossRoomManager,
                            AdminTogglesService togglesService,
                            InMemoryEnhancementLogHook enhanceLog,
                            AuctionStore auctionStore,
                            PvpMatchLogRepository pvpLog,
                            BossRunService bossRunService,
                            long seasonStartEpoch) {
        this.playerDataManager  = playerDataManager;
        this.growthStateStore   = growthStateStore;
        this.islandStore        = islandStore;
        this.pvpRatingService   = pvpRatingService;
        this.pvpMatchService    = pvpMatchService;
        this.pvpArenaManager    = pvpArenaManager;
        this.bossRoomManager    = bossRoomManager;
        this.togglesService     = togglesService;
        this.enhanceLog         = enhanceLog;
        this.auctionStore       = auctionStore;
        this.pvpLog             = pvpLog;
        this.bossRunService     = bossRunService;
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

        } else if (GuiTitles.ADMIN_TOGGLES.equals(event.getView().title())) {
            event.setCancelled(true);
            handleTogglesClick(player, event.getRawSlot());

        } else if (GuiTitles.ADMIN_LOGS.equals(event.getView().title())) {
            event.setCancelled(true);
            handleLogClick(player, event.getRawSlot());

        } else if (GuiTitles.ADMIN_BOSS.equals(event.getView().title())) {
            event.setCancelled(true);
            handleBossClick(player, event.getRawSlot(), event.getClick());

        } else if (GuiTitles.ADMIN_TERRITORY.equals(event.getView().title())) {
            event.setCancelled(true);
            handleTerritoryClick(player, event.getRawSlot(), event.getClick());

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
            case AdminHubGui.SLOT_TOGGLES -> toggleSlotMapping.put(admin.getUniqueId(),
                    AdminTogglesGui.open(admin, togglesService));
            case AdminHubGui.SLOT_LOGS    -> openLog(admin, AdminLogGui.LogTab.ENHANCE, 0);
            case AdminHubGui.SLOT_BOSS    -> bossRunMapping.put(admin.getUniqueId(),
                    AdminBossGui.open(admin, bossRunService));
            case AdminHubGui.SLOT_TERRITORY -> openTerritory(admin, 0);
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

    private void handleTogglesClick(Player admin, int slot) {
        if (slot == AdminTogglesGui.SLOT_BACK)  { AdminHubGui.open(admin); return; }
        if (slot == AdminTogglesGui.SLOT_CLOSE) { admin.closeInventory(); return; }
        Toggle[] mapping = toggleSlotMapping.getOrDefault(admin.getUniqueId(), new Toggle[0]);
        int idx = slot - 9;
        if (idx < 0 || idx >= mapping.length) return;
        Toggle t = mapping[idx];
        boolean next = togglesService.toggle(t);
        admin.sendMessage("§e[관리자] §f" + t.displayName + " §7→ " + (next ? "§a[ON]" : "§c[OFF]"));
        Bukkit.getLogger().info("[empire-admin] " + admin.getName() + " toggled " + t.name() + " → " + next);
        // GUI refresh
        toggleSlotMapping.put(admin.getUniqueId(), AdminTogglesGui.open(admin, togglesService));
    }

    private void openLog(Player admin, AdminLogGui.LogTab tab, int page) {
        int total = AdminLogGui.open(admin, tab, page, enhanceLog, auctionStore, pvpLog);
        int safe  = Math.max(0, Math.min(page, total - 1));
        logView.put(admin.getUniqueId(), new LogView(tab, safe));
    }

    private void handleLogClick(Player admin, int slot) {
        LogView v = logView.getOrDefault(admin.getUniqueId(), new LogView(AdminLogGui.LogTab.ENHANCE, 0));
        switch (slot) {
            case AdminLogGui.SLOT_BACK        -> AdminHubGui.open(admin);
            case AdminLogGui.SLOT_CLOSE       -> admin.closeInventory();
            case AdminLogGui.SLOT_TAB_ENHANCE -> openLog(admin, AdminLogGui.LogTab.ENHANCE, 0);
            case AdminLogGui.SLOT_TAB_TRADE   -> openLog(admin, AdminLogGui.LogTab.TRADE,   0);
            case AdminLogGui.SLOT_TAB_PVP     -> openLog(admin, AdminLogGui.LogTab.PVP,     0);
            case AdminLogGui.SLOT_PREV        -> openLog(admin, v.tab(), v.page() - 1);
            case AdminLogGui.SLOT_NEXT        -> openLog(admin, v.tab(), v.page() + 1);
            default -> { /* 로그 항목 클릭 — 읽기 전용, 무시 */ }
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

    private void openTerritory(Player admin, int page) {
        territoryView.put(admin.getUniqueId(), AdminTerritoryGui.open(admin, islandStore, page));
    }

    private void handleTerritoryClick(Player admin, int slot, ClickType click) {
        AdminTerritoryGui.View view = territoryView.get(admin.getUniqueId());
        int page = view != null ? view.page() : 0;
        switch (slot) {
            case AdminTerritoryGui.SLOT_BACK  -> { AdminHubGui.open(admin); return; }
            case AdminTerritoryGui.SLOT_CLOSE -> { admin.closeInventory(); return; }
            case AdminTerritoryGui.SLOT_PREV  -> { openTerritory(admin, page - 1); return; }
            case AdminTerritoryGui.SLOT_NEXT  -> { openTerritory(admin, page + 1); return; }
            default -> { /* 아이템 영역 — 아래 처리 */ }
        }
        if (slot < 0 || slot >= AdminTerritoryGui.PAGE_SIZE || view == null) return;
        if (slot >= view.pageOwners().size()) return;
        UUID owner = view.pageOwners().get(slot);
        IslandTerritoryState state = islandStore.get(owner).orElse(null);
        if (state == null) return;

        if (click.isShiftClick() && click.isRightClick()) {
            islandStore.resetSocialSettings(owner);
            admin.sendMessage("§a[관리자] §f" + nameOf(owner) + "§a 영지 소셜 설정 초기화 완료.");
            Bukkit.getLogger().info("[empire-admin] " + admin.getName() + " reset territory social " + owner);
        } else if (click.isLeftClick()) {
            IslandRank changed = shiftRank(state, +1);
            admin.sendMessage("§a[관리자] §f" + nameOf(owner) + "§a 작위 ▲ → §e" + changed.displayName);
        } else if (click.isRightClick()) {
            IslandRank changed = shiftRank(state, -1);
            admin.sendMessage("§e[관리자] §f" + nameOf(owner) + "§e 작위 ▼ → §f" + changed.displayName);
        } else {
            return;
        }
        openTerritory(admin, page); // 갱신
    }

    /** 작위를 인접 단계로 이동 (clamp). 변경된 작위 반환. */
    private IslandRank shiftRank(IslandTerritoryState state, int delta) {
        IslandRank[] ranks = IslandRank.values();
        int next = Math.max(0, Math.min(state.rank().ordinal() + delta, ranks.length - 1));
        IslandRank changed = ranks[next];
        state.setRank(changed);
        return changed;
    }

    private static String nameOf(UUID uuid) {
        String name = Bukkit.getOfflinePlayer(uuid).getName();
        return name != null ? name : uuid.toString().substring(0, 8);
    }

    private void handleBossClick(Player admin, int slot, ClickType click) {
        if (slot == AdminBossGui.SLOT_BACK)  { AdminHubGui.open(admin); return; }
        if (slot == AdminBossGui.SLOT_CLOSE) { admin.closeInventory(); return; }
        if (slot < 0 || slot > AdminBossGui.ITEM_AREA_END) return;
        List<String> mapping = bossRunMapping.getOrDefault(admin.getUniqueId(), List.of());
        if (slot >= mapping.size()) return;
        if (!click.isLeftClick()) return;
        String runId = mapping.get(slot);
        var result = bossRunService.endRun(runId, false, "admin_force");
        if (result.isFailure()) {
            admin.sendMessage("§c[관리자] 보스 런 강제 종료 실패: " + result.errorCode().name());
        } else {
            admin.sendMessage("§a[관리자] 보스 런 강제 종료 — 슬롯 해제됨.");
            Bukkit.getLogger().info("[empire-admin] " + admin.getName() + " force-ended boss run " + runId);
        }
        // GUI 갱신
        bossRunMapping.put(admin.getUniqueId(), AdminBossGui.open(admin, bossRunService));
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
