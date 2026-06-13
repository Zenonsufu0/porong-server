package kr.zenon.rpg.listener;

import kr.zenon.rpg.combat.CombatStateService;
import kr.zenon.rpg.growth.island.IslandTerritoryState;
import kr.zenon.rpg.growth.island.IslandTerritoryStateStore;
import kr.zenon.rpg.gui.GuiTitles;
import kr.zenon.rpg.gui.PermissionEditGui;
import kr.zenon.rpg.gui.PermissionRolesGui;
import kr.zenon.rpg.gui.TerritoryFacilityGui;
import kr.zenon.rpg.gui.TerritoryHubGui;
import kr.zenon.rpg.gui.TerritorySettingsGui;
import kr.zenon.rpg.gui.MainHubGui;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
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
            handleFacility(player, event.getRawSlot(), event.isShiftClick());

        } else if (GuiTitles.TERRITORY_FACILITY_SELECT.equals(event.getView().title())) {
            event.setCancelled(true);
            handleFacilitySelect(player, event.getRawSlot());

        }
    }

    // ─── 시설 현황 / 설치 ────────────────────────────────────────────

    private void handleFacility(Player player, int slot, boolean shift) {
        UUID uid = player.getUniqueId();
        IslandTerritoryState t = territory(player);
        if (slot == TerritoryFacilityGui.SLOT_BACK) {
            if (combatStateService.isInCombat(uid)) {
                player.sendMessage("§c[영지] 전투 중에는 영지 설정을 열 수 없습니다.");
                return;
            }
            TerritorySettingsGui.open(player, t, t.weatherState(), t.timeState());
            return;
        }
        if (slot < 0 || slot >= 18) return; // 시설 슬롯 영역만

        IslandTerritoryState.FacilityType installed = TerritoryFacilityGui.installedTypeAt(t, slot);
        if (installed != null && shift) {
            // 철거
            if (t.removeFacility(installed)) {
                player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_DESTROY, 0.6f, 1.0f);
                player.sendMessage("§7[영지] " + facilityKr(installed) + " §71대를 철거했습니다.");
                TerritoryFacilityGui.open(player, t);
            }
            return;
        }
        if (installed == null && t.facilitySlotsAvailable()) {
            // 빈 슬롯 → 시설 선택 화면
            TerritoryFacilityGui.openSelect(player, t);
        } else if (installed != null) {
            player.sendMessage("§8[영지] 철거하려면 §7Shift+클릭§8 하세요.");
        }
    }

    private void handleFacilitySelect(Player player, int slot) {
        IslandTerritoryState t = territory(player);
        if (slot == TerritoryFacilityGui.SLOT_BACK_SELECT) {
            TerritoryFacilityGui.open(player, t);
            return;
        }
        IslandTerritoryState.FacilityType type = TerritoryFacilityGui.selectedType(slot);
        if (type == null) return;
        if (t.installFacility(type)) {
            // installFacility가 설치시각을 now로 기록 → 설치+20분 후 첫 생산(windfall 방지 내장, DL-129 추가#11).
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 0.6f, 1.2f);
            player.sendMessage("§a[영지] " + facilityKr(type) + "§a를 설치했습니다. §7(시설 슬롯 "
                    + t.facilitySlotsUsed() + "/" + t.facilitySlotsMax() + ")");
            TerritoryFacilityGui.open(player, t);
        } else {
            player.sendMessage("§c[영지] 시설 슬롯이 부족합니다. 작위를 승급하세요.");
        }
    }

    private String facilityKr(IslandTerritoryState.FacilityType type) {
        return switch (type) {
            case HERB -> "§a약초 재배지";
            case ORE -> "§b광물 채굴기";
            case WORKSHOP -> "§6공방 가공기";
        };
    }

    // ─── 설정 클릭 핸들러 ────────────────────────────────────────────

    private void handleSettings(Player player, int slot) {
        UUID uid = player.getUniqueId();
        switch (slot) {
            case TerritorySettingsGui.SLOT_NAME -> promptNameChangeChat(player);
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
            case TerritorySettingsGui.SLOT_SPAWN -> {
                player.closeInventory();
                // IS home을 현재 위치로 설정 — 영지 이동(/is home) 도착 지점이 갱신된다.
                if (player.performCommand("is sethome")) {
                    player.sendMessage("§a[영지] 현재 위치를 영지 스폰으로 설정했습니다.");
                } else {
                    player.sendMessage("§c[영지] 스폰 설정에 실패했습니다. 영지 안에서 시도하세요.");
                }
            }
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
            islandTerritoryStateStore.persistMembers(player.getUniqueId());
            player.sendMessage("§c[영지] §f" + memberName + "§c님을 영지에서 강퇴했습니다.");
            TerritorySettingsGui.open(player, territory);
        } else {
            // 좌클릭: 등급 토글 (영지민 ↔ 부영주)
            IslandTerritoryState.Role current = entry.getValue();
            IslandTerritoryState.Role next = (current == IslandTerritoryState.Role.VICE_LORD)
                    ? IslandTerritoryState.Role.RESIDENT
                    : IslandTerritoryState.Role.VICE_LORD;
            territory.setMemberRole(memberUuid, next);
            islandTerritoryStateStore.persistMembers(player.getUniqueId());
            String roleLabel = (next == IslandTerritoryState.Role.VICE_LORD) ? "§b부영주" : "§e영지민";
            player.sendMessage("§a[영지] §f" + memberName + "§a의 등급을 " + roleLabel + "§a로 변경");
            TerritorySettingsGui.open(player, territory);
        }
    }

    // ─── 초대 Anvil ──────────────────────────────────────────────────

    /** 멤버 초대 — 모루 대신 채팅 입력 방식. */
    private void openInviteAnvil(Player player) {
        pendingInviteAnvil.add(player.getUniqueId());
        player.closeInventory();
        player.sendMessage(net.kyori.adventure.text.Component.text(
                "§e[영지] 채팅에 초대할 플레이어 닉네임을 입력하세요. §7(취소: '취소')"));
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
        islandTerritoryStateStore.persistMembers(p.islandOwnerUuid());
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
        islandTerritoryStateStore.persistPermissions(player.getUniqueId(), role);
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
        islandTerritoryStateStore.persistVisitMode(player.getUniqueId());
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
        IslandTerritoryState state = islandTerritoryStateStore.getOrCreate(uid, player.getName());
        int next = (state.weatherState() + 1) % 3;
        state.setWeatherState(next);
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
        IslandTerritoryState state = islandTerritoryStateStore.getOrCreate(uid, player.getName());
        int next = (state.timeState() + 1) % 3;
        state.setTimeState(next);
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
        IslandTerritoryState state = islandTerritoryStateStore.getOrCreate(uid, player.getName());
        var inv = player.getOpenInventory().getTopInventory();
        inv.setItem(TerritorySettingsGui.SLOT_WEATHER,
                TerritorySettingsGui.weatherIcon(state.weatherState()));
        inv.setItem(TerritorySettingsGui.SLOT_TIME,
                TerritorySettingsGui.timeIcon(state.timeState()));
    }

    // ─── 영지명 변경 (Anvil GUI) ─────────────────────────────────────

    /** 영지명 변경 — 모루 대신 채팅 입력 방식(Paper Anvil API 버전 의존 회피). */
    private void promptNameChangeChat(Player player) {
        pendingNameChange.add(player.getUniqueId());
        player.closeInventory();
        player.sendMessage(Component.text(
                "§e[영지] 채팅에 새 영지명을 입력하세요. §7(한글·영문·숫자 16자 이내, 취소: '취소')"));
    }

    /** 채팅으로 입력된 영지명 적용 (메인 스레드에서 호출). */
    private void applyIslandName(Player player, String newName) {
        if (newName.equalsIgnoreCase("취소")) {
            player.sendMessage("§7[영지] 영지명 변경을 취소했습니다.");
            return;
        }
        if (newName.isBlank()) {
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
        territory(player).setIslandName(newName);
        player.sendMessage("§a[영지] 영지명이 §e" + newName + "§a으로 변경되었습니다.");
    }

    @EventHandler
    public void onTerritoryChat(io.papermc.paper.event.player.AsyncChatEvent event) {
        Player player = event.getPlayer();
        UUID uid = player.getUniqueId();
        boolean isName   = pendingNameChange.contains(uid);
        boolean isInvite = pendingInviteAnvil.contains(uid);
        if (!isName && !isInvite) return;
        event.setCancelled(true); // 입력 메시지는 채팅에 노출하지 않음
        String msg = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                .serialize(event.message()).trim();
        if (isName) {
            pendingNameChange.remove(uid);
            Bukkit.getScheduler().runTask(plugin, () -> applyIslandName(player, msg)); // Bukkit API는 메인 스레드
        } else {
            pendingInviteAnvil.remove(uid);
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (msg.equalsIgnoreCase("취소")) {
                    player.sendMessage("§7[영지] 멤버 초대를 취소했습니다.");
                    return;
                }
                handleInviteAnvilResult(player, msg);
            });
        }
    }

    // ─── 헬퍼 ────────────────────────────────────────────────────────

    private IslandTerritoryState territory(Player player) {
        return islandTerritoryStateStore.getOrCreate(player.getUniqueId(), player.getName());
    }
}
