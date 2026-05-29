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

    /** 권한 편집 GUI에서 현재 편집 중인 등급 추적. */
    private final Map<UUID, IslandTerritoryState.Role> editingRole = new ConcurrentHashMap<>();

    // 플레이어별 날씨·시간 상태 (in-memory, 재시작 시 초기화)
    private final Map<UUID, Integer> weatherStates   = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> timeStates      = new ConcurrentHashMap<>();
    private final Set<UUID>          pendingNameChange = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public TerritorySettingsGuiListener(IslandTerritoryStateStore islandTerritoryStateStore,
                                        CombatStateService combatStateService) {
        this.islandTerritoryStateStore = islandTerritoryStateStore;
        this.combatStateService        = combatStateService;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        UUID uid = player.getUniqueId();

        if (GuiTitles.TERRITORY_SETTINGS.equals(event.getView().title())) {
            event.setCancelled(true);
            handleSettings(player, event.getRawSlot());

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
        }
    }

    @EventHandler
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        if (!(event.getView().getPlayer() instanceof Player player)) return;
        if (!pendingNameChange.contains(player.getUniqueId())) return;

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
