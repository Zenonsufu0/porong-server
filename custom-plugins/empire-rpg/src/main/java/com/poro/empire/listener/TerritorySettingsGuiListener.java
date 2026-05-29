package com.poro.empire.listener;

import com.poro.empire.combat.CombatStateService;
import com.poro.empire.growth.island.IslandTerritoryState;
import com.poro.empire.growth.island.IslandTerritoryStateStore;
import com.poro.empire.gui.GuiTitles;
import com.poro.empire.gui.PermissionEditGui;
import com.poro.empire.gui.PermissionRolesGui;
import com.poro.empire.gui.TerritoryFacilityGui;
import com.poro.empire.gui.TerritoryHubGui;
import com.poro.empire.gui.TerritorySettingsGui;
import com.poro.empire.gui.MainHubGui;
import org.bukkit.Bukkit;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.plugin.Plugin;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.WeatherType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class TerritorySettingsGuiListener implements Listener {

    private final IslandTerritoryStateStore islandTerritoryStateStore;
    private final CombatStateService        combatStateService;
    private final Plugin                    plugin;

    /** 권한 편집 GUI에서 현재 편집 중인 등급 추적. */
    private final Map<UUID, IslandTerritoryState.Role> editingRole = new ConcurrentHashMap<>();
    /** 영지 초대 대기: 대상 UUID → InvitePending. */
    private final Map<UUID, InvitePending> pendingInvites = new ConcurrentHashMap<>();
    /** Anvil 멤버 초대 입력 대기: 영주 UUID. */
    private final Set<UUID> pendingInviteAnvil = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public record InvitePending(UUID islandOwnerUuid, String islandOwnerName, long expiryMs) {}

    // 플레이어별 날씨·시간 상태 (in-memory, 재시작 시 초기화)
    private final Map<UUID, Integer> weatherStates   = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> timeStates      = new ConcurrentHashMap<>();
    private final Set<UUID>          pendingNameChange = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public TerritorySettingsGuiListener(IslandTerritoryStateStore islandTerritoryStateStore,
                                        CombatStateService combatStateService,
                                        Plugin plugin) {
        this.islandTerritoryStateStore = islandTerritoryStateStore;
        this.combatStateService        = combatStateService;
        this.plugin                    = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        UUID uid = player.getUniqueId();

        if (GuiTitles.TERRITORY_SETTINGS.equals(event.getView().title())) {
            event.setCancelled(true);
            int slot = event.getRawSlot();
            if (slot >= TerritorySettingsGui.SLOT_MEMBER_START && slot <= TerritorySettingsGui.SLOT_MEMBER_END) {
                handleMemberClick(player, slot, event.getClick());
            } else {
                handleSettings(player, slot);
            }

        } else if (GuiTitles.PERMISSION_ROLES.equals(event.getView().title())) {
            event.setCancelled(true);
            handlePermissionRoles(player, event.getRawSlot());

        } else if (GuiTitles.PERMISSION_EDIT.equals(event.getView().title())) {
            event.setCancelled(true);
            handlePermissionEdit(player, event.getRawSlot());

        } else if (GuiTitles.TERRITORY_FACILITY.equals(event.getView().title())) {
            event.setCancelled(true);
            if (event.getRawSlot() == TerritoryFacilityGui.SLOT_BACK) {
                if (combatStateService.isInCombat(uid)) {
                    player.sendMessage("§c[영지] 전투 중에는 영지 설정을 열 수 없습니다.");
                    return;
                }
                TerritorySettingsGui.open(player, territory(player),
                        weatherStates.getOrDefault(uid, 0), timeStates.getOrDefault(uid, 0));
            }

        } else if (pendingNameChange.contains(uid)
                && event.getInventory() instanceof AnvilInventory anvil
                && event.getRawSlot() == 2) {
            event.setCancelled(true);
            handleNameChangeResult(player, uid, anvil);

        } else if (pendingInviteAnvil.contains(uid)
                && event.getInventory() instanceof AnvilInventory anvil
                && event.getRawSlot() == 2) {
            event.setCancelled(true);
            String targetName = anvil.getRenameText();
            handleInviteAnvilResult(player, targetName);
        }
    }

    @EventHandler
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        if (!(event.getView().getPlayer() instanceof Player player)) return;
        UUID uid = player.getUniqueId();
        if (!pendingNameChange.contains(uid) && !pendingInviteAnvil.contains(uid)) return;

        String newName = event.getInventory().getRenameText();
        if (newName == null || newName.isBlank()) return;

        // 비용 0 설정
        event.getInventory().setRepairCost(0);

        // 결과 아이템 세팅
        ItemStack result = new ItemStack(Material.NAME_TAG);
        ItemMeta meta = result.getItemMeta();
        meta.displayName(Component.text("§e" + newName));
        result.setItemMeta(meta);
        event.setResult(result);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        // 모루 GUI 닫힐 때 pending 정리 (결과 없이 닫은 경우)
        pendingNameChange.remove(player.getUniqueId());
        pendingInviteAnvil.remove(player.getUniqueId());
    }

    // ─── 설정 클릭 핸들러 ────────────────────────────────────────────

    private void handleSettings(Player player, int slot) {
        UUID uid = player.getUniqueId();
        switch (slot) {
            case TerritorySettingsGui.SLOT_NAME -> openNameChangeAnvil(player);
            case TerritorySettingsGui.SLOT_VISIT -> cycleVisitMode(player);
            case TerritorySettingsGui.SLOT_VISITOR_MINE  -> toggleBit(player, IslandTerritoryState.CONV_VISITOR_MINE,
                    "방문자 채굴", TerritorySettingsGui.SLOT_VISITOR_MINE, Material.IRON_PICKAXE,
                    "방문자가 영지에서 채굴할 수 있습니다.");
            case TerritorySettingsGui.SLOT_VISITOR_FARM  -> toggleBit(player, IslandTerritoryState.CONV_VISITOR_FARM,
                    "방문자 농사", TerritorySettingsGui.SLOT_VISITOR_FARM, Material.WHEAT,
                    "방문자가 영지에서 농사지을 수 있습니다.");
            case TerritorySettingsGui.SLOT_CROP_PROTECT  -> toggleBit(player, IslandTerritoryState.CONV_CROP_PROTECT,
                    "농작물 보호", TerritorySettingsGui.SLOT_CROP_PROTECT, Material.WHEAT,
                    "다 자라지 않은 작물 파괴 방지.");
            case TerritorySettingsGui.SLOT_WATER_PROTECT -> toggleBit(player, IslandTerritoryState.CONV_WATER_PROTECT,
                    "물 파괴 보호", TerritorySettingsGui.SLOT_WATER_PROTECT, Material.WATER_BUCKET,
                    "방문자가 영지 내 물을 제거할 수 없습니다.");
            case TerritorySettingsGui.SLOT_WEATHER -> toggleWeather(player, uid);
            case TerritorySettingsGui.SLOT_TIME    -> toggleTime(player, uid);
            case TerritorySettingsGui.SLOT_FACILITY -> {
                if (combatStateService.isInCombat(uid)) {
                    player.sendMessage("§c[영지] 전투 중에는 시설 현황을 열 수 없습니다.");
                    return;
                }
                TerritoryFacilityGui.open(player, territory(player));
            }
            case TerritorySettingsGui.SLOT_PERMISSION -> PermissionRolesGui.open(player);
            case TerritorySettingsGui.SLOT_BACK -> TerritoryHubGui.open(player);
        }
    }

    private void handleMemberClick(Player player, int slot, ClickType click) {
        IslandTerritoryState territory = territory(player);
        int memberIdx = slot - TerritorySettingsGui.SLOT_MEMBER_START;
        var members = territory.memberList();

        if (memberIdx >= members.size()) {
            // 빈 슬롯: Anvil 초대
            if (territory.memberCount() >= IslandTerritoryState.MAX_MEMBERS) {
                player.sendMessage("§c[영지] 멤버는 최대 " + IslandTerritoryState.MAX_MEMBERS + "명까지 초대할 수 있습니다.");
                return;
            }
            openInviteAnvil(player);
            return;
        }

        Map.Entry<UUID, IslandTerritoryState.Role> entry = members.get(memberIdx);
        UUID memberUuid = entry.getKey();
        String memberName = territory.memberName(memberUuid);
        if (memberName == null) memberName = Bukkit.getOfflinePlayer(memberUuid).getName();
        if (memberName == null) memberName = memberUuid.toString().substring(0, 8);

        if (click == ClickType.RIGHT) {
            // 강퇴
            territory.removeMember(memberUuid);
            player.sendMessage("§c[영지] §f" + memberName + "§c님을 영지에서 강퇴했습니다.");
            TerritorySettingsGui.open(player, territory);
        } else {
            // 좌클릭: 등급 토글 (영지민 ↔ 부영주)
            IslandTerritoryState.Role current = entry.getValue();
            IslandTerritoryState.Role next = (current == IslandTerritoryState.Role.VICE_LORD)
                    ? IslandTerritoryState.Role.RESIDENT
                    : IslandTerritoryState.Role.VICE_LORD;
            territory.setMemberRole(memberUuid, next);
            String roleLabel = (next == IslandTerritoryState.Role.VICE_LORD) ? "§b부영주" : "§e영지민";
            player.sendMessage("§a[영지] §f" + memberName + "§a의 등급을 " + roleLabel + "§a로 변경");
            TerritorySettingsGui.open(player, territory);
        }
    }

    // ─── 초대 Anvil ──────────────────────────────────────────────────

    private void openInviteAnvil(Player player) {
        org.bukkit.inventory.AnvilInventory anvil = (org.bukkit.inventory.AnvilInventory) player.getServer()
                .createInventory(null, org.bukkit.event.inventory.InventoryType.ANVIL,
                        net.kyori.adventure.text.Component.text("멤버 초대"));
        org.bukkit.inventory.ItemStack tag = new org.bukkit.inventory.ItemStack(org.bukkit.Material.PAPER);
        org.bukkit.inventory.meta.ItemMeta meta = tag.getItemMeta();
        meta.displayName(net.kyori.adventure.text.Component.text("초대할 닉네임"));
        tag.setItemMeta(meta);
        anvil.setItem(0, tag);
        pendingInviteAnvil.add(player.getUniqueId());
        player.openInventory(anvil);
    }

    public void handleInviteAnvilResult(Player inviter, String targetName) {
        pendingInviteAnvil.remove(inviter.getUniqueId());
        if (targetName == null || targetName.isBlank()) {
            inviter.sendMessage("§c[영지] 닉네임을 입력하세요.");
            return;
        }
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            inviter.sendMessage("§c[영지] 현재 접속 중인 플레이어가 아닙니다.");
            return;
        }
        if (target.getUniqueId().equals(inviter.getUniqueId())) {
            inviter.sendMessage("§c[영지] 본인을 초대할 수 없습니다.");
            return;
        }
        IslandTerritoryState territory = territory(inviter);
        if (territory.hasMember(target.getUniqueId())) {
            inviter.sendMessage("§c[영지] 이미 초대된 플레이어입니다.");
            return;
        }
        if (pendingInvites.containsKey(target.getUniqueId())) {
            inviter.sendMessage("§c[영지] 이미 다른 영지의 초대를 받은 상태입니다.");
            return;
        }

        long expiry = System.currentTimeMillis() + 60_000L;
        pendingInvites.put(target.getUniqueId(),
                new InvitePending(inviter.getUniqueId(), inviter.getName(), expiry));
        inviter.closeInventory();
        inviter.sendMessage("§a[영지] §f" + target.getName() + "§a님에게 초대 알림을 보냈습니다. §7(60초)");
        target.sendMessage("§6[영지 초대] §f" + inviter.getName() + "§7님이 영지에 초대했습니다.");
        target.sendMessage("§a[수락] §7→ §f/영지초대 수락   §c[거절] §7→ §f/영지초대 거절   §8(60초 내)");

        // 60초 만료
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            InvitePending p = pendingInvites.get(target.getUniqueId());
            if (p != null && p.expiryMs() <= System.currentTimeMillis()) {
                pendingInvites.remove(target.getUniqueId());
                Player t = Bukkit.getPlayer(target.getUniqueId());
                if (t != null) t.sendMessage("§7[영지] 초대가 만료되었습니다.");
            }
        }, 20L * 60);
    }

    /** /영지초대 수락 처리 — true=처리됨, false=대기 중 초대 없음. */
    public boolean acceptInvite(Player target) {
        InvitePending p = pendingInvites.remove(target.getUniqueId());
        if (p == null || p.expiryMs() < System.currentTimeMillis()) {
            target.sendMessage("§c[영지] 대기 중인 초대가 없습니다.");
            return false;
        }
        IslandTerritoryState territory = islandTerritoryStateStore.getOrCreate(
                p.islandOwnerUuid(), p.islandOwnerName());
        if (!territory.addMember(target.getUniqueId(), target.getName(), IslandTerritoryState.Role.RESIDENT)) {
            target.sendMessage("§c[영지] 해당 영지가 가득 찼습니다.");
            return false;
        }
        target.sendMessage("§a[영지] §f" + p.islandOwnerName() + "§a님의 영지에 가입했습니다.");
        Player inviter = Bukkit.getPlayer(p.islandOwnerUuid());
        if (inviter != null) inviter.sendMessage("§a[영지] §f" + target.getName() + "§a님이 초대를 수락했습니다.");
        return true;
    }

    /** /영지초대 거절 처리. */
    public boolean rejectInvite(Player target) {
        InvitePending p = pendingInvites.remove(target.getUniqueId());
        if (p == null) {
            target.sendMessage("§c[영지] 대기 중인 초대가 없습니다.");
            return false;
        }
        target.sendMessage("§7[영지] 초대를 거절했습니다.");
        Player inviter = Bukkit.getPlayer(p.islandOwnerUuid());
        if (inviter != null) inviter.sendMessage("§7[영지] §f" + target.getName() + "§7님이 초대를 거절했습니다.");
        return true;
    }

    private void handlePermissionRoles(Player player, int slot) {
        IslandTerritoryState territory = territory(player);
        IslandTerritoryState.Role role = switch (slot) {
            case PermissionRolesGui.SLOT_VICE_LORD -> IslandTerritoryState.Role.VICE_LORD;
            case PermissionRolesGui.SLOT_RESIDENT  -> IslandTerritoryState.Role.RESIDENT;
            case PermissionRolesGui.SLOT_VISITOR   -> IslandTerritoryState.Role.VISITOR;
            default -> null;
        };
        if (role != null) {
            editingRole.put(player.getUniqueId(), role);
            PermissionEditGui.open(player, territory, role);
            return;
        }
        if (slot == PermissionRolesGui.SLOT_BACK) {
            TerritorySettingsGui.open(player, territory);
        }
    }

    private void handlePermissionEdit(Player player, int slot) {
        UUID uid = player.getUniqueId();
        IslandTerritoryState.Role role = editingRole.get(uid);
        if (role == null) {
            PermissionRolesGui.open(player);
            return;
        }
        if (slot == PermissionEditGui.SLOT_BACK) {
            editingRole.remove(uid);
            PermissionRolesGui.open(player);
            return;
        }
        if (slot < 9 || slot > 17) return;
        IslandTerritoryState.Permission perm = PermissionEditGui.SLOT_TO_PERM[slot];
        if (perm == null) return;

        IslandTerritoryState territory = territory(player);
        territory.togglePermission(role, perm);
        // 해당 슬롯만 갱신 (전체 재오픈 시 인벤토리 깜빡임 회피)
        String label = labelFor(slot);
        var inv = player.getOpenInventory().getTopInventory();
        inv.setItem(slot, PermissionEditGui.permIcon(territory, role, perm, label));
        boolean on = territory.hasPermission(role, perm);
        player.sendMessage("§a[영지] " + roleLabel(role) + " §7- §f" + label + ": " + (on ? "§2[허용]" : "§7[비허용]"));
    }

    private String labelFor(int slot) {
        return switch (slot) {
            case  9 -> "창고 입출금";
            case 10 -> "공방 가공기";
            case 11 -> "채굴";
            case 12 -> "농사";
            case 13 -> "물 제거·배치";
            case 14 -> "멤버 초대";
            case 15 -> "멤버 강퇴";
            case 16 -> "블록 파괴·배치";
            case 17 -> "영지 설정 변경";
            default -> "?";
        };
    }

    private String roleLabel(IslandTerritoryState.Role role) {
        return switch (role) {
            case VICE_LORD -> "§b부영주";
            case RESIDENT  -> "§e영지민";
            case VISITOR   -> "§7방문자";
        };
    }

    private void cycleVisitMode(Player player) {
        IslandTerritoryState t = territory(player);
        IslandTerritoryState.VisitMode next = t.cycleVisitMode();
        var inv = player.getOpenInventory().getTopInventory();
        inv.setItem(TerritorySettingsGui.SLOT_VISIT, TerritorySettingsGui.visitIcon(t));
        String label = switch (next) {
            case PUBLIC  -> "§a전체 공개";
            case FRIENDS -> "§e친구만";
            case PRIVATE -> "§c비공개";
        };
        player.sendMessage("§a[영지] 방문 설정: " + label + "§a로 변경");
    }

    private void toggleBit(Player player, int bit, String label, int guiSlot,
                           Material mat, String description) {
        IslandTerritoryState t = territory(player);
        t.toggleConvenience(bit);
        boolean on = t.hasConvenience(bit);
        var inv = player.getOpenInventory().getTopInventory();
        inv.setItem(guiSlot, TerritorySettingsGui.toggleIcon(t, bit, mat, label, description));
        player.sendMessage("§a[영지] " + label + ": " + (on ? "§2[허용]" : "§7[비허용]"));
    }

    // ─── 날씨 토글 ───────────────────────────────────────────────────

    private void toggleWeather(Player player, UUID uid) {
        int next = (weatherStates.getOrDefault(uid, 0) + 1) % 3;
        weatherStates.put(uid, next);
        switch (next) {
            case TerritorySettingsGui.WEATHER_CLEAR -> {
                player.setPlayerWeather(WeatherType.CLEAR);
                player.sendMessage("§a[영지] 날씨를 §f맑음 고정§a으로 설정했습니다.");
            }
            case TerritorySettingsGui.WEATHER_RAIN -> {
                player.setPlayerWeather(WeatherType.DOWNFALL);
                player.sendMessage("§b[영지] 날씨를 §f비 고정§b으로 설정했습니다.");
            }
            default -> {
                player.resetPlayerWeather();
                player.sendMessage("§7[영지] 날씨를 서버 기본값으로 초기화했습니다.");
            }
        }
        refreshWeatherTime(player, uid);
    }

    // ─── 시간 토글 ───────────────────────────────────────────────────

    private void toggleTime(Player player, UUID uid) {
        int next = (timeStates.getOrDefault(uid, 0) + 1) % 3;
        timeStates.put(uid, next);
        switch (next) {
            case TerritorySettingsGui.TIME_DAY -> {
                player.setPlayerTime(6000, false);
                player.sendMessage("§e[영지] 시간을 §f낮 고정§e으로 설정했습니다.");
            }
            case TerritorySettingsGui.TIME_NIGHT -> {
                player.setPlayerTime(18000, false);
                player.sendMessage("§9[영지] 시간을 §f밤 고정§9으로 설정했습니다.");
            }
            default -> {
                player.resetPlayerTime();
                player.sendMessage("§7[영지] 시간을 서버 기본값으로 초기화했습니다.");
            }
        }
        refreshWeatherTime(player, uid);
    }

    private void refreshWeatherTime(Player player, UUID uid) {
        var inv = player.getOpenInventory().getTopInventory();
        inv.setItem(TerritorySettingsGui.SLOT_WEATHER,
                TerritorySettingsGui.weatherIcon(weatherStates.getOrDefault(uid, 0)));
        inv.setItem(TerritorySettingsGui.SLOT_TIME,
                TerritorySettingsGui.timeIcon(timeStates.getOrDefault(uid, 0)));
    }

    // ─── 영지명 변경 (Anvil GUI) ─────────────────────────────────────

    private void openNameChangeAnvil(Player player) {
        AnvilInventory anvil = (AnvilInventory) player.getServer()
                .createInventory(null, org.bukkit.event.inventory.InventoryType.ANVIL,
                        Component.text("영지명 변경"));

        ItemStack nameTag = new ItemStack(Material.NAME_TAG);
        ItemMeta meta = nameTag.getItemMeta();
        meta.displayName(Component.text(territory(player).islandName()));
        nameTag.setItemMeta(meta);
        anvil.setItem(0, nameTag);

        pendingNameChange.add(player.getUniqueId());
        player.openInventory(anvil);
    }

    private void handleNameChangeResult(Player player, UUID uid, AnvilInventory anvil) {
        String newName = anvil.getRenameText();
        pendingNameChange.remove(uid);

        if (newName == null || newName.isBlank()) {
            player.sendMessage("§c[영지] 영지명을 입력하세요.");
            return;
        }
        if (newName.length() > 16) {
            player.sendMessage("§c[영지] 영지명은 최대 16자입니다.");
            return;
        }
        if (!newName.matches("[a-zA-Z0-9가-힣 ]+")) {
            player.sendMessage("§c[영지] 영지명에 특수문자를 사용할 수 없습니다.");
            return;
        }

        IslandTerritoryState territory = territory(player);
        territory.setIslandName(newName);
        player.closeInventory();
        player.sendMessage("§a[영지] 영지명이 §e" + newName + "§a으로 변경되었습니다.");
    }

    // ─── 헬퍼 ────────────────────────────────────────────────────────

    private IslandTerritoryState territory(Player player) {
        return islandTerritoryStateStore.getOrCreate(player.getUniqueId(), player.getName());
    }
}
