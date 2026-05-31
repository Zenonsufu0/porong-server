package com.poro.empire.listener;

import com.poro.empire.combat.CombatStateService;
import com.poro.empire.combat.SkillService;
import com.poro.empire.combat.WeaponPowerCalculator;
import com.poro.empire.combat.WeaponSkill;
import com.poro.empire.combat.weapon.WeaponType;
import com.poro.empire.combat.weapon.WeaponTypeResolver;
import net.kyori.adventure.text.Component;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.attribute.Attribute;
import com.poro.empire.common.registry.master.ItemMasterRegistry;
import com.poro.empire.common.registry.master.model.ItemMaster;
import com.poro.empire.growth.GrowthStateStore;
import com.poro.empire.gui.WeaponGui;
import com.poro.empire.gui.WeaponItemFactory;
import com.poro.empire.growth.island.IslandTerritoryState;
import com.poro.empire.growth.island.IslandTerritoryStateStore;
import com.poro.empire.growth.engine.EnhancementRule;
import com.poro.empire.growth.engine.EnhancementService;
import com.poro.empire.growth.engine.EngravingMaster;
import com.poro.empire.growth.engine.EquipmentSlot;
import com.poro.empire.growth.engine.GrowthEngineRuntime;
import com.poro.empire.growth.engine.GrowthTier;
import com.poro.empire.growth.engine.ItemGrade;
import com.poro.empire.growth.engine.PlayerEquipmentItem;
import com.poro.empire.growth.engine.PlayerGrowthState;
import com.poro.empire.growth.engine.PotentialGrade;
import com.poro.empire.growth.engine.PotentialLine;
import com.poro.empire.growth.engine.PotentialProfile;
import com.poro.empire.growth.engine.PotentialService;
import com.poro.empire.growth.engine.SuccessionService;
import com.poro.empire.gui.EquipmentLoreRenderer;
import com.poro.empire.gui.GuiTitles;
import com.poro.empire.gui.MainHubGui;
import com.poro.empire.scoreboard.ScoreboardService;
import com.poro.empire.storage.PlayerDataManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public final class GrowthGuiListener implements Listener {

    // ═══════════════════════════════════════════════════════════════
    // 강화 GUI 슬롯 상수 (gui_enhancement.md 기준, 45슬롯)
    // 장비 선택: col8 세로 / 강화 정보: 좌·중앙 분산
    // ═══════════════════════════════════════════════════════════════
    // 장비 선택 (col8)
    private static final int ENH_SLOT_SEL_WEAPON     = 8;
    private static final int ENH_SLOT_SEL_HELMET     = 17;
    private static final int ENH_SLOT_SEL_CHESTPLATE = 26;
    private static final int ENH_SLOT_SEL_LEGGINGS   = 35;
    private static final int ENH_SLOT_SEL_BOOTS      = 44;
    // 강화 정보 표시
    private static final int ENH_SLOT_SUCCESS_RATE   = 10;
    private static final int ENH_SLOT_CEILING        = 15;
    private static final int ENH_SLOT_PREVIEW        = 22;
    private static final int ENH_SLOT_GOLD           = 28;
    private static final int ENH_SLOT_STONE          = 33;
    // 강화 흔적 토글 (성공률 곱연산 보정, +10강 이상, 3종 동시 / DL-090)
    private static final int ENH_SLOT_TRACE_BULK     = 18; // 일괄 ON/OFF
    private static final int ENH_SLOT_TRACE_STAR     = 19; // 별 토글
    private static final int ENH_SLOT_TRACE_MOON     = 20; // 달 토글
    private static final int ENH_SLOT_TRACE_SUN      = 21; // 태양 토글
    // 액션
    private static final int ENH_SLOT_BACK           = 36;
    private static final int ENH_SLOT_ACTION         = 40;

    private static final Map<Integer, String> ENH_SLOT_TO_TRACE = Map.of(
            ENH_SLOT_TRACE_STAR, EnhancementService.TRACE_STAR,
            ENH_SLOT_TRACE_MOON, EnhancementService.TRACE_MOON,
            ENH_SLOT_TRACE_SUN,  EnhancementService.TRACE_SUN);

    private static final Map<Integer, EquipmentSlot> ENH_GUI_TO_EQUIP = Map.of(
        ENH_SLOT_SEL_WEAPON,     EquipmentSlot.WEAPON,
        ENH_SLOT_SEL_HELMET,     EquipmentSlot.HELMET,
        ENH_SLOT_SEL_CHESTPLATE, EquipmentSlot.CHESTPLATE,
        ENH_SLOT_SEL_LEGGINGS,   EquipmentSlot.LEGGINGS,
        ENH_SLOT_SEL_BOOTS,      EquipmentSlot.BOOTS
    );

    private static final Map<EquipmentSlot, Integer> ENH_EQUIP_TO_GUI = Map.of(
        EquipmentSlot.WEAPON,     ENH_SLOT_SEL_WEAPON,
        EquipmentSlot.HELMET,     ENH_SLOT_SEL_HELMET,
        EquipmentSlot.CHESTPLATE, ENH_SLOT_SEL_CHESTPLATE,
        EquipmentSlot.LEGGINGS,   ENH_SLOT_SEL_LEGGINGS,
        EquipmentSlot.BOOTS,      ENH_SLOT_SEL_BOOTS
    );

    // ═══════════════════════════════════════════════════════════════
    // 잠재능력 GUI 슬롯 상수 (gui_potential.md 스펙 기준)
    // 오른쪽 열(col8) = 장비 선택, 좌패널 = 현재, 우패널 = 신규
    // ═══════════════════════════════════════════════════════════════
    // 오른쪽 열: 장비 선택 버튼
    private static final int POT_SLOT_SEL_WEAPON     = 8;
    private static final int POT_SLOT_SEL_HELMET     = 17;
    private static final int POT_SLOT_SEL_CHESTPLATE = 26;
    private static final int POT_SLOT_SEL_LEGGINGS   = 35;
    private static final int POT_SLOT_SEL_BOOTS      = 44;
    // 좌패널: 현재 잠재
    private static final int POT_SLOT_CUR_GRADE      = 1;
    private static final int POT_SLOT_CUR_LINE1      = 10;
    private static final int POT_SLOT_CUR_LINE2      = 19;
    private static final int POT_SLOT_CUR_LINE3      = 28;
    private static final int POT_SLOT_CUR_KEEP       = 37;  // [현재 유지] 버튼
    // 중앙
    private static final int POT_SLOT_PREVIEW        = 21;  // 선택 장비 미리보기
    private static final int POT_SLOT_RESOURCE       = 30;  // 보유 골드/큐브
    private static final int POT_SLOT_USE_CUBE       = 39;  // [큐브 사용] 버튼
    // 우패널: 새 옵션 (큐브 사용 후)
    private static final int POT_SLOT_NEW_GRADE      = 5;
    private static final int POT_SLOT_NEW_LINE1      = 14;
    private static final int POT_SLOT_NEW_LINE2      = 23;
    private static final int POT_SLOT_NEW_LINE3      = 32;
    private static final int POT_SLOT_NEW_SELECT     = 41;  // [새 옵션 선택] 버튼
    // 공통
    private static final int POT_SLOT_BACK           = 45;
    private static final int POT_SLOT_CLOSE          = 53;

    private static final Map<Integer, EquipmentSlot> POT_SELECTOR_TO_EQUIP = Map.of(
        POT_SLOT_SEL_WEAPON,     EquipmentSlot.WEAPON,
        POT_SLOT_SEL_HELMET,     EquipmentSlot.HELMET,
        POT_SLOT_SEL_CHESTPLATE, EquipmentSlot.CHESTPLATE,
        POT_SLOT_SEL_LEGGINGS,   EquipmentSlot.LEGGINGS,
        POT_SLOT_SEL_BOOTS,      EquipmentSlot.BOOTS
    );

    private static final Map<EquipmentSlot, Integer> EQUIP_TO_POT_SELECTOR = Map.of(
        EquipmentSlot.WEAPON,     POT_SLOT_SEL_WEAPON,
        EquipmentSlot.HELMET,     POT_SLOT_SEL_HELMET,
        EquipmentSlot.CHESTPLATE, POT_SLOT_SEL_CHESTPLATE,
        EquipmentSlot.LEGGINGS,   POT_SLOT_SEL_LEGGINGS,
        EquipmentSlot.BOOTS,      POT_SLOT_SEL_BOOTS
    );

    // 방어구는 가상 장비(DL-103) — 재질 선택 없이 네더라이트로 고정 표시. 무기는 cosmetic 재질을 따로 조회.
    private static final Map<EquipmentSlot, Material> EQUIP_MATERIAL = Map.of(
        EquipmentSlot.WEAPON,     Material.DIAMOND_SWORD, // 미장착 무기 슬롯 fallback(장착 시 cosmetic 재질 사용)
        EquipmentSlot.HELMET,     Material.NETHERITE_HELMET,
        EquipmentSlot.CHESTPLATE, Material.NETHERITE_CHESTPLATE,
        EquipmentSlot.LEGGINGS,   Material.NETHERITE_LEGGINGS,
        EquipmentSlot.BOOTS,      Material.NETHERITE_BOOTS
    );

    // ═══════════════════════════════════════════════════════════════
    // 전승 GUI 슬롯 상수 (gui_succession.md 스펙 기준, 45슬롯)
    // ═══════════════════════════════════════════════════════════════
    private static final int HEIR_SLOT_SEL_WEAPON      = 8;
    private static final int HEIR_SLOT_SEL_HELMET      = 17;
    private static final int HEIR_SLOT_SEL_CHESTPLATE  = 26;
    private static final int HEIR_SLOT_SEL_LEGGINGS    = 35;
    private static final int HEIR_SLOT_SEL_BOOTS       = 44;
    private static final int HEIR_SLOT_SOURCE          = 11;  // [흔적] — 클릭으로 순환 선택
    private static final int HEIR_SLOT_ARROW           = 13;  // [→] 장식
    private static final int HEIR_SLOT_TARGET          = 15;  // [대상] — 오른쪽 장비 클릭 시 자동
    private static final int HEIR_SLOT_TYPE            = 22;  // [전승권] — 클릭으로 유형 순환
    private static final int HEIR_SLOT_PREVIEW_SRC     = 29;  // 흔적 현재 옵션 미리보기
    private static final int HEIR_SLOT_PREVIEW_TGT     = 33;  // 전승 후 대상 미리보기
    private static final int HEIR_SLOT_EXECUTE         = 40;  // [전승!]
    private static final int HEIR_SLOT_BACK            = 36;

    private static final Map<Integer, EquipmentSlot> HEIR_SELECTOR_TO_EQUIP = Map.of(
        HEIR_SLOT_SEL_WEAPON,     EquipmentSlot.WEAPON,
        HEIR_SLOT_SEL_HELMET,     EquipmentSlot.HELMET,
        HEIR_SLOT_SEL_CHESTPLATE, EquipmentSlot.CHESTPLATE,
        HEIR_SLOT_SEL_LEGGINGS,   EquipmentSlot.LEGGINGS,
        HEIR_SLOT_SEL_BOOTS,      EquipmentSlot.BOOTS
    );

    // ═══════════════════════════════════════════════════════════════
    // 장비 허브 구역 슬롯 세트 (gui_hub_structure.md §3, 54슬롯 6×9)
    // 구역 내 어느 슬롯을 클릭해도 해당 GUI로 이동
    // ═══════════════════════════════════════════════════════════════
    private static final java.util.Set<Integer> HUB_ENHANCE_SLOTS  =
            java.util.Set.of(0,1,2, 9,10,11, 18,19,20);
    private static final java.util.Set<Integer> HUB_ENGRAVING_SLOTS =
            java.util.Set.of(6,7,8, 15,16,17, 24,25,26);
    private static final java.util.Set<Integer> HUB_POTENTIAL_SLOTS =
            java.util.Set.of(27,28,29, 36,37,38, 45,46,47);
    private static final java.util.Set<Integer> HUB_HEIRLOOM_SLOTS  =
            java.util.Set.of(33,34,35, 42,43,44, 51,52,53);
    private static final java.util.Set<Integer> HUB_CHARACTER_SLOTS =
            java.util.Set.of(3,4,5, 12,13,14, 21,22,23, 30,31,32, 39,40,41, 48,49,50);

    // ═══════════════════════════════════════════════════════════════
    // 플레이어별 상태 (선택 + 대기 결과)
    // ═══════════════════════════════════════════════════════════════
    private final Map<UUID, String>                     selectedEnhanceId       = new ConcurrentHashMap<>();
    // 강화 흔적 토글 ON 집합 (mat_trace_*). 비어있으면 미사용 (DL-090)
    private final Map<UUID, java.util.Set<String>>      enabledEnhanceTraces    = new ConcurrentHashMap<>();
    private final Map<UUID, String>                     selectedPotentialId     = new ConcurrentHashMap<>();
    private final Map<UUID, PotentialService.PotentialOperationResult> pendingPotentialResult = new ConcurrentHashMap<>();
    // 전승 상태 (traceId = "equip_trace_broken" 등 customItems 키)
    private final Map<UUID, String>                            selectedHeirloomTarget  = new ConcurrentHashMap<>();
    private final Map<UUID, String>                            selectedHeirloomTraceId = new ConcurrentHashMap<>();
    private final Map<UUID, SuccessionService.SuccessionType> selectedHeirloomType    = new ConcurrentHashMap<>();
    // 스탯 배분 초기화 확인 대기
    private final Map<UUID, Boolean>                           pendingStatReset        = new ConcurrentHashMap<>();
    // 장비 세부 GUI — 어느 슬롯을 보는 중인지 기억
    private final Map<UUID, EquipmentSlot>                     equipDetailSlot         = new ConcurrentHashMap<>();

    // ═══════════════════════════════════════════════════════════════
    // 의존성
    // ═══════════════════════════════════════════════════════════════
    private final GrowthStateStore           growthStateStore;
    private final IslandTerritoryStateStore  islandTerritoryStateStore;
    private final GrowthEngineRuntime        growthEngineRuntime;
    private final PlayerDataManager          playerDataManager;
    private final ItemMasterRegistry         itemMasters;
    private final ScoreboardService          scoreboardService;
    private final CombatStateService         combatStateService;
    @SuppressWarnings("unused")
    private final Plugin                     plugin;
    private SkillService                     skillService;       // setter injection (초기화 순서상)
    private HealthHudListener                healthHudListener;  // setter injection (HUD 알림 오버라이드)

    public GrowthGuiListener(
            GrowthStateStore growthStateStore,
            IslandTerritoryStateStore islandTerritoryStateStore,
            GrowthEngineRuntime growthEngineRuntime,
            PlayerDataManager playerDataManager,
            ScoreboardService scoreboardService,
            ItemMasterRegistry itemMasters,
            CombatStateService combatStateService,
            Plugin plugin
    ) {
        this.growthStateStore          = growthStateStore;
        this.islandTerritoryStateStore = islandTerritoryStateStore;
        this.growthEngineRuntime       = growthEngineRuntime;
        this.playerDataManager         = playerDataManager;
        this.scoreboardService         = scoreboardService;
        this.itemMasters               = itemMasters;
        this.combatStateService        = combatStateService;
        this.plugin                    = plugin;
    }

    // ═══════════════════════════════════════════════════════════════
    // 커맨드 진입점 (PlayerCommandRouter에서 호출)
    // ═══════════════════════════════════════════════════════════════

    public void setSkillService(SkillService svc) { this.skillService = svc; }
    public void setHealthHudListener(HealthHudListener listener) { this.healthHudListener = listener; }

    public void openEquipHub(Player player)   { openEquipmentHub(player); }

    public void openEnhancement(Player player) {
        if (isNoneClass(player)) return;
        openGrowthEnhance(player);
    }

    public void openPotential(Player player) {
        if (isNoneClass(player)) return;
        openGrowthPotential(player);
    }

    public void openHeirloom(Player player) {
        if (isNoneClass(player)) return;
        openGrowthHeirloom(player);
    }

    public void openEngraving(Player player) {
        if (isNoneClass(player)) return;
        openGrowthEngraving(player);
    }

    private boolean isNoneClass(Player player) {
        if (playerDataManager.getWeaponType(player.getUniqueId()) == WeaponType.NONE) {
            player.sendMessage("§c[장비] 직업을 먼저 선택해야 합니다.");
            return true;
        }
        return false;
    }

    // ═══════════════════════════════════════════════════════════════
    // 이벤트 처리
    // ═══════════════════════════════════════════════════════════════

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        UUID uid = player.getUniqueId();
        if (GuiTitles.GROWTH_POTENTIAL.equals(event.getView().title())) {
            PotentialService.PotentialOperationResult pending = pendingPotentialResult.remove(uid);
            if (pending == null) return;
            String instanceId = selectedPotentialId.get(uid);
            if (instanceId == null) return;
            PlayerGrowthState state = getState(player);
            if (pending.before() != null) {
                state.updatePotentialProfile(instanceId, pending.before());
            }
            player.sendMessage("§7[잠재] GUI를 닫아 기존 옵션이 유지되었습니다.");
        } else if (GuiTitles.STAT_ALLOCATION.equals(event.getView().title())) {
            pendingStatReset.remove(uid);
        } else if (GuiTitles.EQUIP_DETAIL.equals(event.getView().title())) {
            equipDetailSlot.remove(uid);
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        if (GuiTitles.EQUIPMENT_HUB.equals(event.getView().title())) {
            event.setCancelled(true);
            handleEquipmentHub(player, event.getRawSlot());
        } else if (GuiTitles.GROWTH_ENHANCE.equals(event.getView().title())) {
            event.setCancelled(true);
            handleEnhanceClick(player, event.getRawSlot());
        } else if (GuiTitles.GROWTH_POTENTIAL.equals(event.getView().title())) {
            event.setCancelled(true);
            handlePotentialClick(player, event.getRawSlot());
        } else if (GuiTitles.GROWTH_HEIRLOOM.equals(event.getView().title())) {
            event.setCancelled(true);
            handleHeirloomClick(player, event.getRawSlot());
        } else if (GuiTitles.GROWTH_ENGRAVING.equals(event.getView().title())) {
            event.setCancelled(true);
            handleEngravingClick(player, event.getRawSlot());
        } else if (GuiTitles.CHARACTER_HUB.equals(event.getView().title())) {
            event.setCancelled(true);
            handleCharacterHubClick(player, event.getRawSlot());
        } else if (GuiTitles.STAT_ALLOCATION.equals(event.getView().title())) {
            event.setCancelled(true);
            handleStatAllocClick(player, event.getRawSlot());
        } else if (GuiTitles.EQUIP_DETAIL.equals(event.getView().title())) {
            event.setCancelled(true);
            handleEquipDetailClick(player, event.getRawSlot());
        } else if (GuiTitles.WEAPON_CHANGE.equals(event.getView().title())) {
            event.setCancelled(true);
            handleWeaponChangeClick(player, event.getRawSlot());
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 장비 허브
    // ═══════════════════════════════════════════════════════════════

    private void handleEquipmentHub(Player player, int slot) {
        if (HUB_ENHANCE_SLOTS.contains(slot))    { openGrowthEnhance(player);   return; }
        if (HUB_ENGRAVING_SLOTS.contains(slot))   { openGrowthEngraving(player); return; }
        if (HUB_POTENTIAL_SLOTS.contains(slot))   { openGrowthPotential(player); return; }
        if (HUB_HEIRLOOM_SLOTS.contains(slot))    { openGrowthHeirloom(player);  return; }
        if (HUB_CHARACTER_SLOTS.contains(slot))   { openCharacterHub(player); }
    }

    // ═══════════════════════════════════════════════════════════════
    // 강화 GUI
    // ═══════════════════════════════════════════════════════════════

    private void openGrowthEnhance(Player player) {
        selectedEnhanceId.remove(player.getUniqueId());
        enabledEnhanceTraces.remove(player.getUniqueId());
        PlayerGrowthState state = getState(player);

        Inventory gui = Bukkit.createInventory(null, 45, GuiTitles.GROWTH_ENHANCE);
        for (int i = 0; i < 45; i++) gui.setItem(i, pane());

        fillEnhancementEquipSlots(gui, state, playerDataManager.getWeaponType(player.getUniqueId()));

        // 초기 안내 아이콘 (장비 미선택)
        gui.setItem(ENH_SLOT_SUCCESS_RATE, MainHubGui.icon(Material.PAPER,      "§8성공률",    List.of("§7장비를 선택하세요")));
        gui.setItem(ENH_SLOT_CEILING,      MainHubGui.icon(Material.CLOCK,      "§8가호 천장", List.of("§7장비를 선택하세요")));
        gui.setItem(ENH_SLOT_PREVIEW, MainHubGui.icon(Material.BOOK, "§7강화 안내",
                List.of("§7──────────────",
                        "§7오른쪽 장비 클릭 → 선택",
                        "§e강화 시도§7 버튼으로 강화",
                        "§7──────────────",
                        "§7골드: §e"    + state.currency("gold"),
                        "§7강화석: §e" + state.currency("mat_stone_enhance"))));
        gui.setItem(ENH_SLOT_GOLD,  MainHubGui.icon(Material.GOLD_INGOT, "§8골드 비용", List.of("§7장비를 선택하세요")));
        gui.setItem(ENH_SLOT_STONE, MainHubGui.icon(Material.PAPER,      "§8강화석",    List.of("§7장비를 선택하세요")));
        gui.setItem(ENH_SLOT_ACTION, MainHubGui.icon(Material.GRAY_STAINED_GLASS_PANE, "§8강화 시도",
                List.of("§7먼저 아이템을 선택하세요")));
        refreshTraceToggles(player, gui, null);
        gui.setItem(ENH_SLOT_BACK, MainHubGui.icon(Material.ARROW, "§7뒤로", List.of("§7장비 관리")));

        player.openInventory(gui);
    }

    private void handleEnhanceClick(Player player, int slot) {
        if (slot == ENH_SLOT_BACK) { openEquipmentHub(player); return; }

        EquipmentSlot equipSlot = ENH_GUI_TO_EQUIP.get(slot);
        if (equipSlot != null) {
            PlayerGrowthState state = getState(player);
            String instanceId = state.equippedItems().get(equipSlot);
            if (instanceId == null) { player.sendMessage("§7해당 슬롯에 장착된 아이템이 없습니다."); return; }
            selectedEnhanceId.put(player.getUniqueId(), instanceId);
            refreshEnhanceInfo(player, player.getOpenInventory().getTopInventory(), state, instanceId);
            return;
        }

        if (slot == ENH_SLOT_TRACE_BULK) {
            toggleAllEnhanceTraces(player);
            return;
        }
        String clickedTrace = ENH_SLOT_TO_TRACE.get(slot);
        if (clickedTrace != null) {
            toggleEnhanceTrace(player, clickedTrace);
            return;
        }

        if (slot == ENH_SLOT_ACTION) {
            String instanceId = selectedEnhanceId.get(player.getUniqueId());
            if (instanceId == null) { player.sendMessage("§c먼저 강화할 아이템을 선택하세요."); return; }
            if (combatStateService.isInCombat(player.getUniqueId())) {
                player.sendMessage("§c전투 중에는 강화할 수 없습니다."); return;
            }
            attemptEnhancement(player, instanceId);
        }
    }

    private void fillEnhancementEquipSlots(Inventory inv, PlayerGrowthState state, WeaponType wt) {
        Map<EquipmentSlot, String> equipped = state.equippedItems();
        for (Map.Entry<EquipmentSlot, Integer> e : ENH_EQUIP_TO_GUI.entrySet()) {
            EquipmentSlot es  = e.getKey();
            int guiSlot       = e.getValue();
            String instanceId = equipped.get(es);
            if (instanceId == null) {
                inv.setItem(guiSlot, MainHubGui.icon(
                        EQUIP_MATERIAL.getOrDefault(es, Material.GRAY_STAINED_GLASS_PANE),
                        "§8" + slotLabel(es) + " (미장착)", List.of()));
            } else {
                PlayerEquipmentItem item = state.inventoryItem(instanceId).orElse(null);
                Material mat = es == EquipmentSlot.WEAPON
                        ? weaponCosmeticMaterial(wt, state.getCosmeticMaterial(weaponCosmeticKey(wt)))
                        : EQUIP_MATERIAL.getOrDefault(es, Material.NETHER_STAR);
                List<String> lore = equipBaseLore(item, es == EquipmentSlot.WEAPON, state.classEngravingId());
                lore.add("§7클릭하여 선택");
                inv.setItem(guiSlot, MainHubGui.icon(mat, "§f" + equipDisplayName(es, wt), lore));
            }
        }
    }

    private void refreshEnhanceInfo(Player player, Inventory inv, PlayerGrowthState state, String instanceId) {
        PlayerEquipmentItem item = state.inventoryItem(instanceId).orElse(null);
        if (item == null) {
            inv.setItem(ENH_SLOT_PREVIEW, MainHubGui.icon(Material.BARRIER, "§c오류", List.of("§7아이템 없음")));
            inv.setItem(ENH_SLOT_ACTION,  MainHubGui.icon(Material.GRAY_STAINED_GLASS_PANE, "§8강화 시도", List.of("§7오류")));
            refreshTraceToggles(player, inv, null);
            return;
        }
        int curLv = item.enhanceLevel();

        // 강화 흔적 토글 4슬롯 갱신 (선택 상태 + 10강 게이트 + 보유/요구 수량 반영)
        refreshTraceToggles(player, inv, instanceId);

        // 미리보기 (중앙 고정) — 선택한 장비의 무기 타입/재질/정본 lore 반영
        EquipmentSlot prevSlot = findEquipSlot(state, instanceId);
        WeaponType prevWt = playerDataManager.getWeaponType(java.util.UUID.fromString(state.userId()));
        Material prevMat = prevSlot == EquipmentSlot.WEAPON
                ? weaponCosmeticMaterial(prevWt, state.getCosmeticMaterial(weaponCosmeticKey(prevWt)))
                : EQUIP_MATERIAL.getOrDefault(prevSlot, Material.NETHER_STAR);
        inv.setItem(ENH_SLOT_PREVIEW, MainHubGui.icon(prevMat,
                "§f" + equipDisplayName(prevSlot, prevWt) + " §e+" + curLv,
                equipBaseLore(item, prevSlot == EquipmentSlot.WEAPON, state.classEngravingId())));
        // 강화 시도 버튼 등에 쓸 표시명 — 정본 한글명(무기=검/도끼…, 방어구=투구…) 사용(영어 itemName 대체).
        String itemName = equipDisplayName(prevSlot, prevWt);

        if (curLv >= EnhancementService.MAX_ENHANCE_LEVEL) {
            inv.setItem(ENH_SLOT_SUCCESS_RATE, MainHubGui.icon(Material.LIME_DYE, "§a성공률 §2[최대]", List.of()));
            inv.setItem(ENH_SLOT_CEILING, pane());
            inv.setItem(ENH_SLOT_GOLD,  pane());
            inv.setItem(ENH_SLOT_STONE, pane());
            inv.setItem(ENH_SLOT_ACTION, MainHubGui.icon(Material.LIME_STAINED_GLASS_PANE, "§a최대 강화 달성",
                    List.of("§7+" + curLv + " — 더 이상 강화 불가")));
            return;
        }

        GrowthTier tier = masterFor(item).map(m -> GrowthTier.from(m.tier())).orElse(GrowthTier.T1);
        EnhancementRule rule = growthEngineRuntime.enhancementRuleRegistry().find(tier, curLv + 1).orElse(null);
        if (rule == null) {
            inv.setItem(ENH_SLOT_SUCCESS_RATE, MainHubGui.icon(Material.PAPER, "§8성공률", List.of("§7규칙 없음")));
            inv.setItem(ENH_SLOT_CEILING, pane());
            inv.setItem(ENH_SLOT_GOLD,  pane());
            inv.setItem(ENH_SLOT_STONE, pane());
            inv.setItem(ENH_SLOT_ACTION, MainHubGui.icon(Material.GRAY_STAINED_GLASS_PANE, "§8강화 불가",
                    List.of("§7강화 규칙 없음")));
            return;
        }

        // 가호 천장 계산
        EquipmentSlot equipSlot = state.equippedItems().entrySet().stream()
                .filter(e -> e.getValue().equals(instanceId))
                .map(Map.Entry::getKey).findFirst().orElse(null);
        int ceilingCap   = (int) Math.ceil(200.0 / rule.successRate());
        int ceilingCount = equipSlot != null
                ? state.getCeilingCounter(equipSlot.name().toLowerCase() + "_" + (curLv + 1))
                : 0;

        // 강화 흔적 곱연산 보정 (현재 +10강 이상 & 토글 ON & 요구 수량 이상 보유) — 유효 성공률 계산
        double baseRate = rule.successRate();
        java.util.List<String> activeTraces = activeEnhanceTraces(player, curLv);
        double traceMult = 1.0;
        for (String t : activeTraces) traceMult += EnhancementService.traceMultiplierBonus(t);
        double effRate = Math.min(baseRate * traceMult, 100.0);

        // 성공률
        List<String> rateLore = new ArrayList<>(List.of(
                "§7──────────────",
                "§7현재 단계: §e+" + curLv,
                "§7기본 성공률: §e" + String.format("%.2f%%", baseRate)));
        if (!activeTraces.isEmpty()) {
            rateLore.add("§d흔적 보정: §e×" + String.format("%.2f", traceMult)
                    + " §7→ §a" + String.format("%.2f%%", effRate));
        }
        rateLore.add("§7가호     : §e" + ceilingCount + " / " + ceilingCap + "회");
        inv.setItem(ENH_SLOT_SUCCESS_RATE, MainHubGui.icon(Material.PAPER, "§e강화 성공률", rateLore));

        // 가호 천장
        inv.setItem(ENH_SLOT_CEILING, MainHubGui.icon(Material.CLOCK, "§e가호 천장",
                List.of("§7──────────────",
                        "§7연속 실패: §e" + ceilingCount + " / " + ceilingCap + "회",
                        ceilingCount >= ceilingCap
                                ? "§a다음 강화 100% 보정!"
                                : "§7" + (ceilingCap - ceilingCount) + "회 더 실패 시 보정")));

        // 골드 비용
        long myGold = state.currency("gold");
        inv.setItem(ENH_SLOT_GOLD, MainHubGui.icon(Material.GOLD_INGOT, "§6골드 비용",
                List.of("§7──────────────",
                        "§7차감 골드: §6" + rule.goldCost() + "G",
                        myGold >= rule.goldCost()
                                ? "§7보유: §e" + myGold + "G"
                                : "§c부족: §e" + myGold + "G")));

        // 강화석
        long myStone = state.currency("mat_stone_enhance");
        inv.setItem(ENH_SLOT_STONE, MainHubGui.icon(Material.PAPER, "§e강화석",
                List.of("§7──────────────",
                        "§7필요: §e" + rule.stoneCost() + "개",
                        myStone >= rule.stoneCost()
                                ? "§7보유: §e" + myStone + "개"
                                : "§c부족: §e" + myStone + "개")));

        // 강화 실행 버튼
        boolean okGold  = myGold  >= rule.goldCost();
        boolean okStone = myStone >= rule.stoneCost();
        if (okGold && okStone) {
            List<String> actionLore = new ArrayList<>(List.of(
                    "§7──────────────",
                    "§7" + itemName + " §e+" + curLv + " → +" + (curLv + 1),
                    "§7성공률: §e" + String.format("%.2f", effRate) + "%"
                            + (!activeTraces.isEmpty() ? " §d(흔적 ×" + String.format("%.2f", traceMult) + ")" : ""),
                    "§7비용: 골드 §e" + rule.goldCost() + " §7/ 강화석 §e" + rule.stoneCost()));
            for (String t : activeTraces) {
                actionLore.add("§d· " + enhanceTraceName(t)
                        + " §7" + EnhancementService.traceCostForLevel(t, curLv) + "개 소모");
            }
            actionLore.add("§7──────────────");
            actionLore.add("§a클릭하여 강화 시도");
            inv.setItem(ENH_SLOT_ACTION, MainHubGui.icon(Material.ANVIL, "§e§l강화 시도", actionLore));
        } else {
            List<String> lacks = new ArrayList<>();
            if (!okGold)  lacks.add("§c골드 부족 (필요: " + rule.goldCost() + ")");
            if (!okStone) lacks.add("§c강화석 부족 (필요: " + rule.stoneCost() + ")");
            inv.setItem(ENH_SLOT_ACTION, MainHubGui.icon(Material.RED_STAINED_GLASS_PANE, "§c재화 부족", lacks));
        }
    }

    private void attemptEnhancement(Player player, String instanceId) {
        UUID uid = player.getUniqueId();
        PlayerGrowthState state = getState(player);
        PlayerEquipmentItem cur = state.inventoryItem(instanceId).orElse(null);
        int curLv = cur != null ? cur.enhanceLevel() : 0;
        IslandTerritoryState island = islandTerritoryStateStore.getOrCreate(uid, player.getName());
        // 강화 흔적: 현재 +10강 이상 & 토글 ON & 요구 수량 이상 보유한 흔적만 전달 (서비스가 전량 소모하며 곱연산 보정)
        java.util.List<String> traces = activeEnhanceTraces(player, curLv);
        var result = growthEngineRuntime.enhancementService().attempt(state, instanceId, island, traces, null);
        if (result.isFailure()) {
            player.sendMessage("§c강화 실패: " + result.message());
            refreshEnhanceInfo(player, player.getOpenInventory().getTopInventory(), state, instanceId);
            return;
        }
        var r = result.value();
        if (r.traceId() != null && !r.traceId().isBlank()) {
            String names = java.util.Arrays.stream(r.traceId().split(","))
                    .map(GrowthGuiListener::enhanceTraceName).collect(Collectors.joining(", "));
            player.sendMessage("§d[강화 흔적] §f" + names + " §7사용 — 성공률 곱연산 보정 적용");
        }
        if (r.success()) {
            player.sendMessage("§6§l[강화 성공!] §e" + itemDisplayNameById(r.itemId())
                    + " §a+" + r.finalLevel() + " 달성!" + (r.forcedByCeiling() ? " §7(천장 보정)" : ""));
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1.0f, 1.2f);
            if (healthHudListener != null) {
                healthHudListener.showAlert(player.getUniqueId(),
                        Component.text("§6§l✦ 강화 성공! §e+" + r.finalLevel()), 60);
            }
        } else {
            player.sendMessage("§7[강화] §e" + itemDisplayNameById(r.itemId()) + " §c+" + r.targetLevel() + " 실패."
                    + " §7(" + String.format("%.1f%%", r.successRate()) + ")");
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.7f, 0.8f);
            if (healthHudListener != null) {
                healthHudListener.showAlert(player.getUniqueId(),
                        Component.text("§c§l✗ 강화 실패 §7(+" + r.targetLevel() + ")"), 60);
            }
        }
        scoreboardService.refresh(player);
        Inventory inv = player.getOpenInventory().getTopInventory();
        fillEnhancementEquipSlots(inv, state, playerDataManager.getWeaponType(player.getUniqueId()));
        refreshEnhanceInfo(player, inv, state, instanceId);
        refreshHeldWeapon(player, state); // 강화 후 손무기 lore 실시간 반영
    }

    // ═══════════════════════════════════════════════════════════════
    // 잠재능력 GUI
    // ═══════════════════════════════════════════════════════════════

    private void openGrowthPotential(Player player) {
        selectedPotentialId.remove(player.getUniqueId());
        pendingPotentialResult.remove(player.getUniqueId());
        PlayerGrowthState state = getState(player);

        Inventory gui = Bukkit.createInventory(null, 54, GuiTitles.GROWTH_POTENTIAL);
        ItemStack pane = pane();
        for (int i = 0; i < 54; i++) gui.setItem(i, pane);

        fillPotentialEquipSelector(gui, state, null);
        gui.setItem(POT_SLOT_PREVIEW, MainHubGui.icon(Material.BOOK, "§7잠재능력 안내",
                List.of("§7──────────────",
                        "§7오른쪽 슬롯의 장비를 클릭하여 선택",
                        "§7큐브 1개 + 골드 500으로 재롤",
                        "§7──────────────",
                        "§7큐브: §e" + state.currency("mat_cube"),
                        "§7골드: §e" + state.currency("gold"))));
        gui.setItem(POT_SLOT_USE_CUBE, MainHubGui.icon(Material.GRAY_STAINED_GLASS_PANE, "§8큐브 사용",
                List.of("§7먼저 장비를 선택하세요")));
        gui.setItem(POT_SLOT_BACK, MainHubGui.icon(Material.ARROW, "§7뒤로", List.of("§7장비 관리")));

        player.openInventory(gui);
    }

    private void handlePotentialClick(Player player, int slot) {
        // 대기 결과 중에도 큐브 재클릭은 허용(선택 없이 연속 재롤, DL-112) — CUR_KEEP·NEW_SELECT·USE_CUBE 외만 차단.
        if (pendingPotentialResult.containsKey(player.getUniqueId())
                && slot != POT_SLOT_CUR_KEEP && slot != POT_SLOT_NEW_SELECT && slot != POT_SLOT_USE_CUBE) {
            player.sendMessage("§c먼저 결과를 선택하세요: §e[현재 유지] §cor §b[새 옵션 선택]");
            return;
        }
        if (slot == POT_SLOT_BACK) { openEquipmentHub(player); return; }

        // 장비 선택 (오른쪽 열)
        EquipmentSlot equipSlot = POT_SELECTOR_TO_EQUIP.get(slot);
        if (equipSlot != null) {
            pendingPotentialResult.remove(player.getUniqueId());
            PlayerGrowthState state = getState(player);
            String instanceId = state.equippedItems().get(equipSlot);
            if (instanceId == null) { player.sendMessage("§7해당 슬롯에 장착된 아이템이 없습니다."); return; }
            selectedPotentialId.put(player.getUniqueId(), instanceId);
            Inventory inv = player.getOpenInventory().getTopInventory();
            fillPotentialEquipSelector(inv, state, instanceId);
            refreshPotentialCurrentPanel(inv, state, instanceId);
            clearPotentialNewPanel(inv);
            refreshPotentialCubeButton(inv, state, instanceId, false);
            return;
        }

        // 큐브 사용
        if (slot == POT_SLOT_USE_CUBE) {
            String instanceId = selectedPotentialId.get(player.getUniqueId());
            if (instanceId == null) { player.sendMessage("§c먼저 장비를 선택하세요."); return; }
            if (combatStateService.isInCombat(player.getUniqueId())) {
                player.sendMessage("§c전투 중에는 잠재능력을 변경할 수 없습니다."); return;
            }
            // 미확정 결과가 있으면 자동으로 기존 유지(before 복원) 후 재굴림 — 선택 없이 연속 재롤(DL-112).
            PotentialService.PotentialOperationResult prev = pendingPotentialResult.remove(player.getUniqueId());
            if (prev != null && prev.before() != null) {
                getState(player).updatePotentialProfile(instanceId, prev.before());
            }
            rollPotential(player, instanceId);
            return;
        }

        // [현재 유지] 버튼
        if (slot == POT_SLOT_CUR_KEEP) {
            PotentialService.PotentialOperationResult pending = pendingPotentialResult.remove(player.getUniqueId());
            if (pending == null) return;
            String instanceId = selectedPotentialId.get(player.getUniqueId());
            if (instanceId == null) return;
            PlayerGrowthState state = getState(player);
            // useCube()가 already applied 'after' → revert to before
            if (pending.before() != null) {
                state.updatePotentialProfile(instanceId, pending.before());
            }
            player.sendMessage("§7[잠재] §f" + itemDisplayName(state.inventoryItem(instanceId).orElse(null)) + " §7기존 옵션 유지.");
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            scoreboardService.refresh(player);
            refreshHeldWeapon(player, state); // 잠재 유지 후 손무기 lore 반영
            Inventory inv = player.getOpenInventory().getTopInventory();
            refreshPotentialCurrentPanel(inv, state, instanceId);
            clearPotentialNewPanel(inv);
            refreshPotentialCubeButton(inv, state, instanceId, false);
            return;
        }

        // [새 옵션 선택] 버튼
        if (slot == POT_SLOT_NEW_SELECT) {
            PotentialService.PotentialOperationResult pending = pendingPotentialResult.remove(player.getUniqueId());
            if (pending == null) return;
            String instanceId = selectedPotentialId.get(player.getUniqueId());
            PlayerGrowthState state = getState(player);
            player.sendMessage("§b[잠재] §f" + itemDisplayName(state.inventoryItem(instanceId).orElse(null)) + " §b새 옵션 확정!");
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.6f, 1.1f);
            scoreboardService.refresh(player);
            refreshHeldWeapon(player, state); // 잠재 새 옵션 확정 후 손무기 lore 반영
            Inventory inv = player.getOpenInventory().getTopInventory();
            refreshPotentialCurrentPanel(inv, state, instanceId);
            clearPotentialNewPanel(inv);
            refreshPotentialCubeButton(inv, state, instanceId, false);
        }
    }

    private void rollPotential(Player player, String instanceId) {
        PlayerGrowthState state = getState(player);
        PlayerEquipmentItem item = state.inventoryItem(instanceId).orElse(null);
        if (item == null) { player.sendMessage("§c아이템을 찾을 수 없습니다."); return; }

        // 잠재 미초기화 시 자동 초기화 (COMMON 등급)
        if (item.potentialProfile() == null) {
            var initResult = growthEngineRuntime.potentialService().generateInitial(state, instanceId, PotentialGrade.COMMON);
            if (initResult.isFailure()) {
                player.sendMessage("§c잠재능력 초기화 실패: " + initResult.message()); return;
            }
        }

        var result = growthEngineRuntime.potentialService().useCube(state, instanceId);
        if (result.isFailure()) {
            player.sendMessage("§c큐브 사용 실패: " + result.message()); return;
        }

        pendingPotentialResult.put(player.getUniqueId(), result.value());
        player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 0.8f, 1.0f);

        Inventory inv = player.getOpenInventory().getTopInventory();
        // 왼쪽: before 패널 표시 (이미 적용됐으므로 before를 수동으로 표시)
        renderPotentialPanel(inv, result.value().before(), POT_SLOT_CUR_GRADE, POT_SLOT_CUR_LINE1,
                POT_SLOT_CUR_LINE2, POT_SLOT_CUR_LINE3, "§7");
        inv.setItem(POT_SLOT_CUR_KEEP, MainHubGui.icon(Material.RED_STAINED_GLASS, "§c◀ 현재 유지",
                List.of("§7──────────────",
                        "§7기존 잠재능력 유지",
                        "§7새 결과를 버림",
                        "§7──────────────",
                        "§c클릭하여 현재 유지")));
        // 오른쪽: 새 옵션 표시
        renderPotentialPanel(inv, result.value().selectedAfter(), POT_SLOT_NEW_GRADE, POT_SLOT_NEW_LINE1,
                POT_SLOT_NEW_LINE2, POT_SLOT_NEW_LINE3, "§b");
        inv.setItem(POT_SLOT_NEW_SELECT, MainHubGui.icon(Material.LIME_STAINED_GLASS, "§a새 옵션 선택 ▶",
                List.of("§7──────────────",
                        (result.value().success() ? "§6§l[등급 상승!] " + result.value().beforeGrade() + " → " + result.value().selectedGrade() : "§7등급 유지: " + result.value().selectedGrade()),
                        "§7──────────────",
                        "§b클릭하여 새 옵션 확정")));
        // 자원 표시 업데이트
        inv.setItem(POT_SLOT_RESOURCE, MainHubGui.icon(Material.NETHER_STAR, "§7보유 자원",
                List.of("§7큐브: §e" + state.currency("mat_cube"),
                        "§7골드: §e" + state.currency("gold"))));
        // 큐브 버튼 활성 유지 — 선택 없이 큐브를 다시 눌러 연속 재굴림(직전 후보는 자동 폐기, DL-112).
        refreshPotentialCubeButton(inv, state, instanceId, false);
    }

    private void fillPotentialEquipSelector(Inventory inv, PlayerGrowthState state, String selectedInstanceId) {
        Map<EquipmentSlot, String> equipped = state.equippedItems();
        for (Map.Entry<EquipmentSlot, Integer> e : EQUIP_TO_POT_SELECTOR.entrySet()) {
            EquipmentSlot es  = e.getKey();
            int guiSlot       = e.getValue();
            String instanceId = equipped.get(es);
            boolean isSelected = instanceId != null && instanceId.equals(selectedInstanceId);
            if (instanceId == null) {
                inv.setItem(guiSlot, MainHubGui.icon(
                        EQUIP_MATERIAL.getOrDefault(es, Material.GRAY_STAINED_GLASS_PANE),
                        "§8" + slotLabel(es) + " (미장착)", List.of()));
            } else {
                PlayerEquipmentItem item = state.inventoryItem(instanceId).orElse(null);
                WeaponType wt = playerDataManager.getWeaponType(java.util.UUID.fromString(state.userId()));
                Material mat = es == EquipmentSlot.WEAPON
                        ? weaponCosmeticMaterial(wt, state.getCosmeticMaterial(weaponCosmeticKey(wt)))
                        : EQUIP_MATERIAL.getOrDefault(es, Material.NETHER_STAR);
                List<String> lore = new ArrayList<>();
                lore.add(isSelected ? "§a▶ 선택됨" : "§7클릭하여 선택");
                lore.addAll(equipBaseLore(item, es == EquipmentSlot.WEAPON, state.classEngravingId()));
                inv.setItem(guiSlot, MainHubGui.icon(mat,
                        (isSelected ? "§a§l" : "§f") + equipDisplayName(es, wt), lore));
            }
        }
    }

    private void refreshPotentialCurrentPanel(Inventory inv, PlayerGrowthState state, String instanceId) {
        PlayerEquipmentItem item = state.inventoryItem(instanceId).orElse(null);
        PotentialProfile profile = item != null ? item.potentialProfile() : null;
        renderPotentialPanel(inv, profile, POT_SLOT_CUR_GRADE, POT_SLOT_CUR_LINE1,
                POT_SLOT_CUR_LINE2, POT_SLOT_CUR_LINE3, "§7");
        inv.setItem(POT_SLOT_CUR_KEEP, pane());  // 선택 대기 중 아닐 때는 숨김

        // 미리보기 슬롯 (선택된 장비 정보) — 무기는 cosmetic 재질 + 정본 무기명
        EquipmentSlot prevSlot = findEquipSlot(state, instanceId);
        Material prevMat;
        String itemName;
        if (prevSlot == EquipmentSlot.WEAPON) {
            WeaponType wt = playerDataManager.getWeaponType(java.util.UUID.fromString(state.userId()));
            prevMat  = weaponCosmeticMaterial(wt, state.getCosmeticMaterial(weaponCosmeticKey(wt)));
            itemName = WeaponGui.displayName(wt);
        } else {
            prevMat  = EQUIP_MATERIAL.getOrDefault(prevSlot, Material.NETHER_STAR);
            itemName = item != null ? itemDisplayName(item) : instanceId;
        }
        // 선택한 장비의 현재 잠재능력 안내 — 등급 + 옵션 라인 전체 나열(DL-112).
        List<String> previewLore = new ArrayList<>();
        previewLore.add("§7──────────────");
        if (profile != null && !profile.lines().isEmpty()) {
            previewLore.add("§7잠재 등급: " + gradeColor(profile.grade()) + EquipmentLoreRenderer.potentialGradeKr(profile.grade()));
            previewLore.add("§7현재 옵션:");
            for (PotentialLine pl : profile.lines()) {
                previewLore.add("§8 · " + EquipmentLoreRenderer.potentialOptionKr(pl.optionCode())
                        + " §e+" + String.format("%.2f", pl.value()));
            }
        } else {
            previewLore.add("§7잠재 등급: §8없음");
            previewLore.add("§7큐브를 돌려 잠재능력을 부여하세요");
        }
        inv.setItem(POT_SLOT_PREVIEW, MainHubGui.icon(
                prevMat,
                "§f" + itemName + (item != null ? " §e+" + item.enhanceLevel() + "강" : ""),
                previewLore));
        // 자원 표시
        inv.setItem(POT_SLOT_RESOURCE, MainHubGui.icon(Material.NETHER_STAR, "§7보유 자원",
                List.of("§7큐브: §e" + state.currency("mat_cube"),
                        "§7골드: §e" + state.currency("gold"))));
    }

    private void clearPotentialNewPanel(Inventory inv) {
        ItemStack pane = pane();
        inv.setItem(POT_SLOT_NEW_GRADE,  pane);
        inv.setItem(POT_SLOT_NEW_LINE1,  pane);
        inv.setItem(POT_SLOT_NEW_LINE2,  pane);
        inv.setItem(POT_SLOT_NEW_LINE3,  pane);
        inv.setItem(POT_SLOT_NEW_SELECT, pane);
    }

    private void refreshPotentialCubeButton(Inventory inv, PlayerGrowthState state, String instanceId, boolean pending) {
        if (pending) {
            inv.setItem(POT_SLOT_USE_CUBE, MainHubGui.icon(Material.GRAY_STAINED_GLASS_PANE, "§8큐브 사용",
                    List.of("§7선택 확정 후 다시 사용 가능")));
            return;
        }
        boolean hasCube = state.currency(PotentialService.MATERIAL_CUBE) >= 1;
        boolean hasGold = state.currency("gold") >= 500L;
        if (hasCube && hasGold) {
            inv.setItem(POT_SLOT_USE_CUBE, MainHubGui.icon(Material.NETHER_STAR, "§b큐브 사용",
                    List.of("§7──────────────",
                            "§7큐브 1개 + 골드 500 소모",
                            "§7잠재 3라인 전체 재롤",
                            "§7──────────────",
                            "§b클릭하여 재롤")));
        } else {
            List<String> lacks = new ArrayList<>();
            if (!hasCube) lacks.add("§c큐브 부족 (보유: " + state.currency(PotentialService.MATERIAL_CUBE) + ")");
            if (!hasGold) lacks.add("§c골드 부족 (필요: 500)");
            inv.setItem(POT_SLOT_USE_CUBE, MainHubGui.icon(Material.RED_STAINED_GLASS_PANE, "§c재화 부족", lacks));
        }
    }

    /** 좌/우 패널에 잠재능력 라인을 렌더링한다. */
    private void renderPotentialPanel(Inventory inv, PotentialProfile profile,
                                      int gradeSlot, int line1Slot, int line2Slot, int line3Slot,
                                      String colorPrefix) {
        if (profile == null) {
            inv.setItem(gradeSlot, MainHubGui.icon(Material.GRAY_STAINED_GLASS_PANE, "§8잠재능력 없음", List.of()));
            inv.setItem(line1Slot, pane());
            inv.setItem(line2Slot, pane());
            inv.setItem(line3Slot, pane());
            return;
        }
        inv.setItem(gradeSlot, MainHubGui.icon(Material.PAPER,
                colorPrefix + "등급: " + gradeColor(profile.grade()) + EquipmentLoreRenderer.potentialGradeKr(profile.grade()), List.of()));
        List<PotentialLine> lines = profile.lines();
        inv.setItem(line1Slot, lines.size() > 0 ? potentialLineIcon(lines.get(0), colorPrefix) : pane());
        inv.setItem(line2Slot, lines.size() > 1 ? potentialLineIcon(lines.get(1), colorPrefix) : pane());
        inv.setItem(line3Slot, lines.size() > 2 ? potentialLineIcon(lines.get(2), colorPrefix) : pane());
    }

    private ItemStack potentialLineIcon(PotentialLine line, String colorPrefix) {
        return MainHubGui.icon(Material.PAPER,
                colorPrefix + EquipmentLoreRenderer.potentialOptionKr(line.optionCode()) + " §e+" + String.format("%.2f", line.value()),
                List.of("§7라인 " + line.lineNo() + " / " + gradeColor(line.grade()) + EquipmentLoreRenderer.potentialGradeKr(line.grade())));
    }

    // ═══════════════════════════════════════════════════════════════
    // 전승 GUI
    // ═══════════════════════════════════════════════════════════════

    private void openGrowthHeirloom(Player player) {
        UUID uid = player.getUniqueId();
        selectedHeirloomTarget.remove(uid);
        selectedHeirloomTraceId.remove(uid);
        selectedHeirloomType.put(uid, SuccessionService.SuccessionType.BASIC);

        PlayerGrowthState state = getState(player);
        IslandTerritoryState islandState = islandTerritoryStateStore.getOrCreate(uid, player.getName());
        Inventory gui = Bukkit.createInventory(null, 45, GuiTitles.GROWTH_HEIRLOOM);
        for (int i = 0; i < 45; i++) gui.setItem(i, pane());

        fillHeirloomEquipSelector(gui, state, null);

        List<String> tracePool = availableTraceIds(islandState);
        if (!tracePool.isEmpty()) {
            String firstTrace = tracePool.get(0);
            selectedHeirloomTraceId.put(uid, firstTrace);
            gui.setItem(HEIR_SLOT_SOURCE, buildTraceIcon(firstTrace, islandState));
        } else {
            gui.setItem(HEIR_SLOT_SOURCE, MainHubGui.icon(Material.GRAY_STAINED_GLASS_PANE, "§8흔적 (없음)",
                    List.of("§7──────────────",
                            "§7보유 중인 장비의 흔적이 없습니다",
                            "§7필드 엘리트 몹 처치 시 획득",
                            "§7──────────────")));
        }
        gui.setItem(HEIR_SLOT_ARROW,  MainHubGui.icon(Material.ARROW, "§7→", List.of()));
        gui.setItem(HEIR_SLOT_TARGET, MainHubGui.icon(Material.GRAY_STAINED_GLASS_PANE, "§8대상 (미선택)",
                List.of("§7오른쪽 슬롯에서 장착 장비 선택")));
        gui.setItem(HEIR_SLOT_TYPE, buildTypeIcon(SuccessionService.SuccessionType.BASIC));
        gui.setItem(HEIR_SLOT_EXECUTE, MainHubGui.icon(Material.GRAY_STAINED_GLASS_PANE, "§8전승!",
                List.of("§7흔적과 대상을 먼저 선택하세요")));
        gui.setItem(HEIR_SLOT_BACK, MainHubGui.icon(Material.ARROW, "§7뒤로", List.of("§7장비 관리")));
        player.openInventory(gui);
    }

    private void handleHeirloomClick(Player player, int slot) {
        UUID uid = player.getUniqueId();

        if (slot == HEIR_SLOT_BACK) { openEquipmentHub(player); return; }

        // 오른쪽 열: 대상 선택
        EquipmentSlot equipSlot = HEIR_SELECTOR_TO_EQUIP.get(slot);
        if (equipSlot != null) {
            PlayerGrowthState state = getState(player);
            String instanceId = state.equippedItems().get(equipSlot);
            if (instanceId == null) { player.sendMessage("§7해당 슬롯에 장착된 아이템이 없습니다."); return; }
            selectedHeirloomTarget.put(uid, instanceId);
            Inventory inv = player.getOpenInventory().getTopInventory();
            IslandTerritoryState islandState = islandTerritoryStateStore.getOrCreate(uid, player.getName());
            fillHeirloomEquipSelector(inv, state, instanceId);
            refreshHeirloomPanels(inv, state, islandState, uid);
            return;
        }

        // 흔적 순환 선택
        if (slot == HEIR_SLOT_SOURCE) {
            IslandTerritoryState islandState = islandTerritoryStateStore.getOrCreate(uid, player.getName());
            List<String> pool = availableTraceIds(islandState);
            if (pool.isEmpty()) { player.sendMessage("§7보유 중인 장비의 흔적이 없습니다."); return; }
            String current = selectedHeirloomTraceId.get(uid);
            int idx = pool.indexOf(current);
            int next = (idx + 1) % pool.size();
            selectedHeirloomTraceId.put(uid, pool.get(next));
            PlayerGrowthState state = getState(player);
            refreshHeirloomPanels(player.getOpenInventory().getTopInventory(), state, islandState, uid);
            return;
        }

        // 전승 유형 순환
        if (slot == HEIR_SLOT_TYPE) {
            SuccessionService.SuccessionType type = selectedHeirloomType.getOrDefault(uid, SuccessionService.SuccessionType.BASIC).next();
            selectedHeirloomType.put(uid, type);
            player.getOpenInventory().getTopInventory().setItem(HEIR_SLOT_TYPE, buildTypeIcon(type));
            PlayerGrowthState state = getState(player);
            IslandTerritoryState islandState = islandTerritoryStateStore.getOrCreate(uid, player.getName());
            refreshHeirloomExecuteButton(player.getOpenInventory().getTopInventory(), state, islandState, uid);
            return;
        }

        // [전승!]
        if (slot == HEIR_SLOT_EXECUTE) {
            String traceId = selectedHeirloomTraceId.get(uid);
            String targetId = selectedHeirloomTarget.get(uid);
            if (traceId == null || targetId == null) { player.sendMessage("§c흔적과 대상을 모두 선택하세요."); return; }
            if (combatStateService.isInCombat(uid)) { player.sendMessage("§c전투 중에는 전승할 수 없습니다."); return; }
            PlayerGrowthState state = getState(player);
            IslandTerritoryState islandState = islandTerritoryStateStore.getOrCreate(uid, player.getName());
            SuccessionService.SuccessionType type = selectedHeirloomType.getOrDefault(uid, SuccessionService.SuccessionType.BASIC);
            var result = growthEngineRuntime.successionService().apply(state, islandState, traceId, targetId, type);
            if (result.isFailure()) { player.sendMessage("§c전승 실패: " + result.message()); return; }
            selectedHeirloomTraceId.remove(uid);
            selectedHeirloomTarget.remove(uid);
            selectedHeirloomType.put(uid, SuccessionService.SuccessionType.BASIC);
            scoreboardService.refresh(player);
            String tgtName = itemDisplayName(state.inventoryItem(targetId).orElse(null));
            player.sendMessage("§6§l[전승 완료] §f" + tgtName + " §e" + type.displayName() + " §a적용!");
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.7f, 1.0f);
            openGrowthHeirloom(player);
        }
    }

    private void fillHeirloomEquipSelector(Inventory inv, PlayerGrowthState state, String selectedTargetId) {
        Map<EquipmentSlot, String> equipped = state.equippedItems();
        for (Map.Entry<Integer, EquipmentSlot> e : HEIR_SELECTOR_TO_EQUIP.entrySet()) {
            int guiSlot  = e.getKey();
            EquipmentSlot es = e.getValue();
            String instanceId = equipped.get(es);
            boolean selected = instanceId != null && instanceId.equals(selectedTargetId);
            if (instanceId == null) {
                inv.setItem(guiSlot, MainHubGui.icon(
                        EQUIP_MATERIAL.getOrDefault(es, Material.GRAY_STAINED_GLASS_PANE),
                        "§8" + slotLabel(es) + " (미장착)", List.of()));
            } else {
                PlayerEquipmentItem item = state.inventoryItem(instanceId).orElse(null);
                WeaponType wt = playerDataManager.getWeaponType(java.util.UUID.fromString(state.userId()));
                Material mat = es == EquipmentSlot.WEAPON
                        ? weaponCosmeticMaterial(wt, state.getCosmeticMaterial(weaponCosmeticKey(wt)))
                        : EQUIP_MATERIAL.getOrDefault(es, Material.NETHER_STAR);
                List<String> lore = new ArrayList<>();
                lore.add(selected ? "§a▶ 대상 선택됨" : "§7클릭하여 대상 선택");
                lore.addAll(equipBaseLore(item, es == EquipmentSlot.WEAPON, state.classEngravingId()));
                inv.setItem(guiSlot, MainHubGui.icon(mat,
                        (selected ? "§a§l" : "§f") + equipDisplayName(es, wt), lore));
            }
        }
    }

    private void refreshHeirloomPanels(Inventory inv, PlayerGrowthState state, IslandTerritoryState islandState, UUID uid) {
        String traceId = selectedHeirloomTraceId.get(uid);
        String targetId = selectedHeirloomTarget.get(uid);
        SuccessionService.SuccessionType type = selectedHeirloomType.getOrDefault(uid, SuccessionService.SuccessionType.BASIC);

        // 흔적 슬롯
        if (traceId != null) {
            inv.setItem(HEIR_SLOT_SOURCE, buildTraceIcon(traceId, islandState));
            ItemGrade traceGrade = SuccessionService.traceGrade(traceId);
            List<String> srcLore = new ArrayList<>();
            srcLore.add("§7등급: " + gradeColor(traceGrade) + traceGrade.displayName());
            srcLore.add("§7보유: §e" + islandState.getCustomItem(traceId) + "개");
            srcLore.add("§7──────────────");
            srcLore.add("§8서브스탯은 전승 시 무작위 생성");
            inv.setItem(HEIR_SLOT_PREVIEW_SRC, MainHubGui.icon(Material.PAPER, "§7흔적 정보", srcLore));
        } else {
            inv.setItem(HEIR_SLOT_SOURCE, MainHubGui.icon(Material.GRAY_STAINED_GLASS_PANE, "§8흔적 (미선택)",
                    List.of("§7클릭 → 보유 흔적 순환")));
            inv.setItem(HEIR_SLOT_PREVIEW_SRC, pane());
        }

        // 대상 슬롯
        if (targetId != null) {
            PlayerEquipmentItem tgt = state.inventoryItem(targetId).orElse(null);
            inv.setItem(HEIR_SLOT_TARGET, MainHubGui.icon(
                    Material.DIAMOND_SWORD, "§b대상: §f" + itemDisplayName(tgt),
                    List.of("§7등급: " + gradeColor(tgt != null ? tgt.grade() : ItemGrade.COMMON) + (tgt != null ? tgt.grade().displayName() : "?"),
                            "§7서브스탯: §e" + (tgt != null ? tgt.substatLines().size() : 0) + "라인")));
            if (traceId != null) {
                inv.setItem(HEIR_SLOT_PREVIEW_TGT, buildHeirloomAfterPreviewIcon(traceId, tgt, type));
            } else {
                inv.setItem(HEIR_SLOT_PREVIEW_TGT, buildHeirloomPreviewIcon(tgt, "§b대상 현재 옵션", "§b"));
            }
        } else {
            inv.setItem(HEIR_SLOT_TARGET, MainHubGui.icon(Material.GRAY_STAINED_GLASS_PANE, "§8대상 (미선택)",
                    List.of("§7오른쪽 슬롯에서 장착 장비 선택")));
            inv.setItem(HEIR_SLOT_PREVIEW_TGT, pane());
        }

        refreshHeirloomExecuteButton(inv, state, islandState, uid);
    }

    private void refreshHeirloomExecuteButton(Inventory inv, PlayerGrowthState state, IslandTerritoryState islandState, UUID uid) {
        String traceId = selectedHeirloomTraceId.get(uid);
        String targetId = selectedHeirloomTarget.get(uid);
        SuccessionService.SuccessionType type = selectedHeirloomType.getOrDefault(uid, SuccessionService.SuccessionType.BASIC);
        if (traceId == null || targetId == null) {
            inv.setItem(HEIR_SLOT_EXECUTE, MainHubGui.icon(Material.GRAY_STAINED_GLASS_PANE, "§8전승!",
                    List.of("§7흔적과 대상을 먼저 선택하세요"))); return;
        }
        long cost = type.goldCost();
        boolean canAfford = cost == 0 || state.currency("gold") >= cost;
        boolean hasTrace = islandState.getCustomItem(traceId) >= 1;
        if (canAfford && hasTrace) {
            String traceName = traceDisplayName(traceId);
            String tgtName = itemDisplayName(state.inventoryItem(targetId).orElse(null));
            inv.setItem(HEIR_SLOT_EXECUTE, MainHubGui.icon(Material.NETHER_STAR, "§e§l전승!",
                    List.of("§7──────────────",
                            "§7흔적: §e" + traceName + " §c(소모됨)",
                            "§7대상: §b" + tgtName,
                            "§7유형: " + type.displayName(),
                            cost > 0 ? "§7비용: §e" + cost + "G" : "§a무료",
                            "§7──────────────",
                            "§e클릭하여 전승 실행")));
        } else if (!hasTrace) {
            inv.setItem(HEIR_SLOT_EXECUTE, MainHubGui.icon(Material.RED_STAINED_GLASS_PANE, "§c흔적 부족",
                    List.of("§c" + traceDisplayName(traceId) + " 보유량 부족")));
        } else {
            inv.setItem(HEIR_SLOT_EXECUTE, MainHubGui.icon(Material.RED_STAINED_GLASS_PANE, "§c골드 부족",
                    List.of("§c필요: " + type.goldCost() + "G",
                            "§c보유: " + state.currency("gold") + "G")));
        }
    }

    private ItemStack buildTypeIcon(SuccessionService.SuccessionType type) {
        Material mat = switch (type) {
            case BASIC        -> Material.PAPER;
            case GRADE_ONLY   -> Material.GOLD_INGOT;
            case SUBSTAT_ONLY -> Material.DIAMOND;
        };
        return MainHubGui.icon(mat, "§f전승 유형: §e" + type.displayName(),
                List.of("§7──────────────",
                        switch (type) {
                            case BASIC        -> "§7등급 + 서브스탯 전체 이전";
                            case GRADE_ONLY   -> "§7등급만 이전, 서브스탯 무시";
                            case SUBSTAT_ONLY -> "§7서브스탯만 이전, 등급 무시";
                        },
                        "§7──────────────",
                        "§7클릭 → 다음 유형으로"));
    }

    private ItemStack buildHeirloomPreviewIcon(PlayerEquipmentItem item, String title, String color) {
        if (item == null) return pane();
        List<String> lore = new ArrayList<>();
        lore.add(color + "등급: " + gradeColor(item.grade()) + item.grade().displayName());
        List<PotentialLine> substats = item.substatLines();
        if (substats.isEmpty()) {
            lore.add("§8서브스탯: 없음");
        } else {
            substats.forEach(l -> lore.add("§7  " + l.optionCode() + " §e+" + String.format("%.2f", l.value())));
        }
        return MainHubGui.icon(Material.PAPER, color + title, lore);
    }

    private ItemStack buildHeirloomAfterPreviewIcon(String traceId, PlayerEquipmentItem tgt,
                                                    SuccessionService.SuccessionType type) {
        if (traceId == null || tgt == null) return pane();
        ItemGrade traceGrade = SuccessionService.traceGrade(traceId);
        ItemGrade resultGrade = (type == SuccessionService.SuccessionType.SUBSTAT_ONLY) ? tgt.grade() : traceGrade;
        List<String> lore = new ArrayList<>();
        lore.add("§b전승 후 등급: " + gradeColor(resultGrade) + resultGrade.displayName());
        if (type == SuccessionService.SuccessionType.GRADE_ONLY) {
            List<PotentialLine> substats = tgt.substatLines();
            if (substats.isEmpty()) {
                lore.add("§8서브스탯: 유지 (없음)");
            } else {
                lore.add("§7서브스탯: §f유지됨");
                substats.forEach(l -> lore.add("§7  " + l.optionCode() + " §e+" + String.format("%.2f", l.value())));
            }
        } else {
            lore.add("§7서브스탯: §e무작위 생성");
            lore.add("§8(실행 시 확정)");
        }
        return MainHubGui.icon(Material.NETHER_STAR, "§b전승 후 대상 옵션", lore);
    }

    private List<String> availableTraceIds(IslandTerritoryState islandState) {
        return SuccessionService.TRACE_GRADE_MAP.keySet().stream()
                .filter(id -> islandState.getCustomItem(id) > 0)
                .sorted()
                .collect(Collectors.toList());
    }

    private ItemStack buildTraceIcon(String traceId, IslandTerritoryState islandState) {
        ItemGrade grade = SuccessionService.traceGrade(traceId);
        String name = traceDisplayName(traceId);
        long count = islandState.getCustomItem(traceId);
        return MainHubGui.icon(Material.PAPER,
                gradeColor(grade) + name,
                List.of("§7등급: " + gradeColor(grade) + grade.displayName(),
                        "§7보유: §e" + count + "개",
                        "§7──────────────",
                        "§7클릭 → 다음 흔적으로"));
    }

    private String traceDisplayName(String traceId) {
        return itemMasters.find(traceId).map(ItemMaster::itemName).orElse(traceId);
    }

    // ═══════════════════════════════════════════════════════════════
    // 강화 흔적 (성공률 곱연산 보정 소모품, mat_trace_* / DL-090)
    // 3종 독립 토글(별/달/태양) + 일괄 토글. 보너스 합산 후 곱: ×(1+Σ). 전부 ×1.70.
    // ※ 전승 흔적(equip_trace_*)과는 별개 개념
    // ═══════════════════════════════════════════════════════════════

    /** 강화 흔적 표시 이름 (시드 의존 없이 고정). */
    private static String enhanceTraceName(String id) {
        if (id == null) return "없음";
        return switch (id) {
            case EnhancementService.TRACE_STAR -> "별의 흔적";
            case EnhancementService.TRACE_MOON -> "달의 흔적";
            case EnhancementService.TRACE_SUN  -> "태양의 흔적";
            default -> id;
        };
    }

    /** 현재 토글 ON 집합 (비어있을 수 있음, 영속 안 함 — GUI 세션 한정). */
    private java.util.Set<String> enabledTraces(UUID uid) {
        return enabledEnhanceTraces.computeIfAbsent(uid, k -> new java.util.LinkedHashSet<>());
    }

    /**
     * 실제 적용되는 흔적 목록 (별→달→태양 순): curLv >= 10 & 토글 ON & 요구 수량 이상 보유.
     * GUI 표시·서비스 전달 양쪽에서 사용하는 단일 기준.
     */
    private java.util.List<String> activeEnhanceTraces(Player player, int curLv) {
        if (curLv < EnhancementService.TRACE_MIN_LEVEL) return java.util.List.of();
        java.util.Set<String> on = enabledEnhanceTraces.get(player.getUniqueId());
        if (on == null || on.isEmpty()) return java.util.List.of();
        IslandTerritoryState island = islandTerritoryStateStore.getOrCreate(player.getUniqueId(), player.getName());
        java.util.List<String> out = new ArrayList<>();
        for (String t : EnhancementService.TRACE_ORDER) {
            if (on.contains(t) && island.getCustomItem(t) >= EnhancementService.traceCostForLevel(t, curLv)) out.add(t);
        }
        return out;
    }

    /** 선택된 장비의 현재 강화 단계 (미선택/없음 = -1). */
    private int selectedEnhanceLevel(Player player) {
        String instanceId = selectedEnhanceId.get(player.getUniqueId());
        if (instanceId == null) return -1;
        PlayerEquipmentItem item = getState(player).inventoryItem(instanceId).orElse(null);
        return item != null ? item.enhanceLevel() : -1;
    }

    /** 개별 흔적 토글. 10강 이상 게이트 + 안내. */
    private void toggleEnhanceTrace(Player player, String traceId) {
        UUID uid = player.getUniqueId();
        String instanceId = selectedEnhanceId.get(uid);
        if (instanceId == null) { player.sendMessage("§c먼저 강화할 아이템을 선택하세요."); return; }
        int curLv = selectedEnhanceLevel(player);
        if (curLv < EnhancementService.TRACE_MIN_LEVEL) {
            player.sendMessage("§7강화 흔적은 §e+" + EnhancementService.TRACE_MIN_LEVEL + "강 이상§7부터 사용할 수 있습니다.");
            return;
        }
        java.util.Set<String> on = enabledTraces(uid);
        if (!on.remove(traceId)) on.add(traceId);
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, on.contains(traceId) ? 1.2f : 0.8f);
        refreshEnhanceInfo(player, player.getOpenInventory().getTopInventory(), getState(player), instanceId);
    }

    /** 일괄 토글: 3종 모두 ON이면 전부 OFF, 아니면 전부 ON. */
    private void toggleAllEnhanceTraces(Player player) {
        UUID uid = player.getUniqueId();
        String instanceId = selectedEnhanceId.get(uid);
        if (instanceId == null) { player.sendMessage("§c먼저 강화할 아이템을 선택하세요."); return; }
        int curLv = selectedEnhanceLevel(player);
        if (curLv < EnhancementService.TRACE_MIN_LEVEL) {
            player.sendMessage("§7강화 흔적은 §e+" + EnhancementService.TRACE_MIN_LEVEL + "강 이상§7부터 사용할 수 있습니다.");
            return;
        }
        java.util.Set<String> on = enabledTraces(uid);
        boolean allOn = on.containsAll(EnhancementService.TRACE_ORDER);
        on.clear();
        if (!allOn) on.addAll(EnhancementService.TRACE_ORDER);
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, allOn ? 0.8f : 1.2f);
        refreshEnhanceInfo(player, player.getOpenInventory().getTopInventory(), getState(player), instanceId);
    }

    /** 강화 흔적 4슬롯(일괄 + 별/달/태양) 갱신. */
    private void refreshTraceToggles(Player player, Inventory inv, String instanceId) {
        inv.setItem(ENH_SLOT_TRACE_BULK, buildTraceBulkIcon(player, instanceId));
        inv.setItem(ENH_SLOT_TRACE_STAR, buildTraceToggleIcon(player, EnhancementService.TRACE_STAR, instanceId));
        inv.setItem(ENH_SLOT_TRACE_MOON, buildTraceToggleIcon(player, EnhancementService.TRACE_MOON, instanceId));
        inv.setItem(ENH_SLOT_TRACE_SUN,  buildTraceToggleIcon(player, EnhancementService.TRACE_SUN,  instanceId));
    }

    /** 일괄 토글 아이콘. */
    private ItemStack buildTraceBulkIcon(Player player, String instanceId) {
        int curLv = instanceId == null ? -1 : selectedEnhanceLevel(player);
        java.util.Set<String> on = enabledEnhanceTraces.getOrDefault(player.getUniqueId(), java.util.Set.of());
        if (instanceId == null) {
            return MainHubGui.icon(Material.BOOK, "§e강화 흔적 §7(곱연산 보정)",
                    List.of("§7──────────────",
                            "§7성공률 ×배수 소모품 (선택, 3종 동시)",
                            "§7별 ×1.15 · 달 ×1.25 · 태양 ×1.30",
                            "§7전부 = ×1.70",
                            "§7──────────────",
                            "§7장비 선택 후 §e+10강 이상§7에서 사용"));
        }
        if (curLv < EnhancementService.TRACE_MIN_LEVEL) {
            return MainHubGui.icon(Material.GRAY_STAINED_GLASS_PANE, "§8강화 흔적 §7(+10강 이상)",
                    List.of("§7현재 §e+" + curLv + "강 §7— +10강 이상부터 사용"));
        }
        boolean allOn = on.containsAll(EnhancementService.TRACE_ORDER);
        return MainHubGui.icon(allOn ? Material.LIME_DYE : Material.BOOK,
                "§e일괄 " + (allOn ? "§a[전체 ON]" : "§7[전체 OFF로]"),
                List.of("§7──────────────",
                        "§7클릭: 3종 " + (allOn ? "전부 OFF" : "전부 ON"),
                        "§7개별 토글은 옆 칸(별/달/태양)",
                        "§7──────────────",
                        "§d곱연산: 켠 흔적 보너스 합산 후 곱"));
    }

    /** 개별 흔적 토글 아이콘 (ON/OFF·요구 수량·보유 반영). */
    private ItemStack buildTraceToggleIcon(Player player, String traceId, String instanceId) {
        UUID uid = player.getUniqueId();
        IslandTerritoryState island = islandTerritoryStateStore.getOrCreate(uid, player.getName());
        long held = island.getCustomItem(traceId);
        double bonus = EnhancementService.traceMultiplierBonus(traceId);
        String name = enhanceTraceName(traceId);
        String bonusStr = "×" + String.format("%.2f", 1.0 + bonus);

        if (instanceId == null) {
            return MainHubGui.icon(Material.GLOWSTONE_DUST, "§7" + name + " " + bonusStr,
                    List.of("§7보유: §e" + held + "개", "§7장비 선택 후 사용"));
        }
        int curLv = selectedEnhanceLevel(player);
        if (curLv < EnhancementService.TRACE_MIN_LEVEL) {
            return MainHubGui.icon(Material.GRAY_STAINED_GLASS_PANE, "§8" + name + " " + bonusStr,
                    List.of("§7보유: §e" + held + "개", "§7+10강 이상부터 사용"));
        }
        int cost = EnhancementService.traceCostForLevel(traceId, curLv);
        boolean on = enabledTraces(uid).contains(traceId);
        boolean enough = held >= cost;
        Material mat = on ? (enough ? Material.LIME_DYE : Material.RED_DYE) : Material.GRAY_DYE;
        String state = on ? (enough ? "§a[ON]" : "§c[ON·부족]") : "§7[OFF]";
        return MainHubGui.icon(mat, (on && enough ? "§a" : "§f") + name + " " + bonusStr + " " + state,
                List.of("§7──────────────",
                        "§7성공률 보정: §a" + bonusStr,
                        "§7이 단계 요구: §e" + cost + "개 §7(전량 소모)",
                        (enough ? "§7보유: §e" : "§c보유: §e") + held + "개",
                        "§7천장 강제 성공 시 미소모",
                        "§7──────────────",
                        "§e클릭 → " + (on ? "OFF" : "ON")));
    }

    // ═══════════════════════════════════════════════════════════════
    // 장비 허브 열기 (내부)
    // ═══════════════════════════════════════════════════════════════

    private void openEquipmentHub(Player player) {
        if (isNoneClass(player)) return;

        Inventory gui = Bukkit.createInventory(null, 54, GuiTitles.EQUIPMENT_HUB);

        // 구역별 색상 채움 — 중앙 아이콘은 fillZone 뒤에 덮어쓴다
        ItemStack lgPane = MainHubGui.icon(Material.LIGHT_GRAY_STAINED_GLASS_PANE, " ", List.of());
        ItemStack puPane = MainHubGui.icon(Material.PURPLE_STAINED_GLASS_PANE,     " ", List.of());
        ItemStack bkPane = MainHubGui.icon(Material.BLACK_STAINED_GLASS_PANE,      " ", List.of());
        ItemStack cyPane = MainHubGui.icon(Material.CYAN_STAINED_GLASS_PANE,       " ", List.of());
        ItemStack orPane = MainHubGui.icon(Material.ORANGE_STAINED_GLASS_PANE,     " ", List.of());
        for (int s : HUB_ENHANCE_SLOTS)   gui.setItem(s, lgPane);
        for (int s : HUB_ENGRAVING_SLOTS) gui.setItem(s, puPane);
        for (int s : HUB_CHARACTER_SLOTS) gui.setItem(s, bkPane);
        for (int s : HUB_POTENTIAL_SLOTS) gui.setItem(s, cyPane);
        for (int s : HUB_HEIRLOOM_SLOTS)  gui.setItem(s, orPane);

        // 중앙 아이콘 (각 구역 row1 center)
        gui.setItem(10, MainHubGui.icon(Material.ANVIL,          "§f강화",     List.of("§7장비 강화  ·  성공 확률", "§8▶ 클릭하여 열기")));
        gui.setItem(16, MainHubGui.icon(Material.NAME_TAG,       "§d각인",     List.of("§7능력 강화  ·  각인 선택", "§8▶ 클릭하여 열기")));
        gui.setItem(22, MainHubGui.icon(Material.PAPER,          "§7캐릭터",   List.of("§7장착 장비  ·  스탯",     "§8▶ 클릭하여 열기")));
        gui.setItem(37, MainHubGui.icon(Material.NETHER_STAR,    "§b잠재능력", List.of("§7큐브로 잠재 재구성",      "§8▶ 클릭하여 열기")));
        gui.setItem(43, MainHubGui.icon(Material.ENCHANTED_BOOK, "§6전승",     List.of("§7흔적을 이용한 능력 이전", "§8▶ 클릭하여 열기")));

        player.openInventory(gui);
    }

    // ══════════════════════════════════════════════════════════════════
    // 캐릭터 허브 (54슬롯, gui_equipment_panel.md)
    // ══════════════════════════════════════════════════════════════════

    private void openCharacterHub(Player player) {
        WeaponType wt = playerDataManager.getWeaponType(player.getUniqueId());
        PlayerGrowthState state = growthStateStore.getOrCreate(
                player.getUniqueId(), wt.name().toLowerCase(Locale.ROOT));

        Inventory gui = Bukkit.createInventory(null, 54, GuiTitles.CHARACTER_HUB);
        for (int i = 0; i < 54; i++) gui.setItem(i, pane());

        // 장착 슬롯 (읽기 전용 표시)
        gui.setItem(13, equipHubSlotIcon(state, EquipmentSlot.HELMET,     wt));
        gui.setItem(20, equipHubSlotIcon(state, EquipmentSlot.WEAPON,     wt));
        gui.setItem(22, equipHubSlotIcon(state, EquipmentSlot.CHESTPLATE, wt));
        gui.setItem(31, equipHubSlotIcon(state, EquipmentSlot.LEGGINGS,   wt));
        gui.setItem(40, equipHubSlotIcon(state, EquipmentSlot.BOOTS,      wt));

        // 치장 슬롯 (2차 확장 — 치장 시스템 미구현)
        for (int s : new int[]{14, 21, 23, 32, 41}) {
            gui.setItem(s, MainHubGui.icon(Material.GRAY_STAINED_GLASS_PANE, "§7치장 §8[2차 확장]", List.of()));
        }
        gui.setItem(16, pane()); // 재질 일괄선택 제거(방어구 가상장비 — 재질 변경 없음, DL-103)

        // 스탯 요약 (slot 25, 읽기 전용)
        gui.setItem(25, buildStatSummaryIcon(player, state));

        // 스탯 찍기 버튼 (slot 34)
        int unspent = state.unspentPts();
        gui.setItem(34, MainHubGui.icon(Material.EXPERIENCE_BOTTLE,
                unspent > 0 ? "§a스탯 배분 §e(+" + unspent + "pt)" : "§f스탯 배분",
                List.of("§7클릭하여 스탯 투자")));

        gui.setItem(43, pane()); // 일괄 보이기/숨김 제거(외형 토글 — 방어구 가상장비라 불필요)
        gui.setItem(44, pane());

        // 스킬 슬롯 lore (slots 46/48/50/52, 읽기 전용)
        gui.setItem(46, skillSlotIcon(wt, 1, "[LC]"));
        gui.setItem(48, skillSlotIcon(wt, 2, "[RC]"));
        gui.setItem(50, skillSlotIcon(wt, 3, "[SRC]"));
        gui.setItem(52, skillSlotIcon(wt, 4, "[F]"));

        // 뒤로 (slot 45)
        gui.setItem(45, MainHubGui.icon(Material.ARROW, "§7뒤로", List.of("§7장비 관리")));

        player.openInventory(gui);
    }

    private void handleCharacterHubClick(Player player, int slot) {
        switch (slot) {
            case 45 -> openEquipmentHub(player);
            case 34 -> openStatAllocation(player);
            case 13 -> openEquipDetail(player, EquipmentSlot.HELMET);
            case 20 -> openEquipDetail(player, EquipmentSlot.WEAPON);
            case 22 -> openEquipDetail(player, EquipmentSlot.CHESTPLATE);
            case 31 -> openEquipDetail(player, EquipmentSlot.LEGGINGS);
            case 40 -> openEquipDetail(player, EquipmentSlot.BOOTS);
        }
    }

    // ══════════════════════════════════════════════════════════════════
    // 장비 세부 GUI (27슬롯, gui_equipment_detail.md §1)
    // 재질 선택·숨김·보이기는 2차 확장 — 현재는 아이템 정보 표시만 구현
    // ══════════════════════════════════════════════════════════════════

    private void openEquipDetail(Player player, EquipmentSlot equipSlot) {
        WeaponType wt = playerDataManager.getWeaponType(player.getUniqueId());
        PlayerGrowthState state = growthStateStore.getOrCreate(
                player.getUniqueId(), wt.name().toLowerCase(Locale.ROOT));
        equipDetailSlot.put(player.getUniqueId(), equipSlot);

        Inventory gui = Bukkit.createInventory(null, 27, GuiTitles.EQUIP_DETAIL);
        for (int i = 0; i < 27; i++) gui.setItem(i, pane());

        gui.setItem(4, buildEquipDetailIcon(state, equipSlot, wt));

        if (equipSlot == EquipmentSlot.WEAPON) {
            gui.setItem(8, MainHubGui.icon(Material.IRON_SWORD, "§e무기 변경",
                    List.of("§7──────────────", "§7클릭하여 무기 종류 선택")));
        }

        fillDetailMaterialRow(gui, state, equipSlot, wt);

        gui.setItem(18, MainHubGui.icon(Material.ARROW, "§7뒤로", List.of("§7캐릭터 허브")));

        // 숨김/보이기 — 치장템 시스템 미구현, 항상 비활성
        gui.setItem(24, MainHubGui.icon(Material.GRAY_STAINED_GLASS_PANE, "§8숨김",
                List.of("§8치장템 없음 (비활성)")));
        gui.setItem(26, MainHubGui.icon(Material.GRAY_STAINED_GLASS_PANE, "§8보이기",
                List.of("§8치장템 없음 (비활성)")));

        player.openInventory(gui);
    }

    private static final String[] ARMOR_MAT_KEYS   = {"leather","chainmail","copper","iron","golden","diamond","netherite"};
    private static final String[] ARMOR_MAT_LABELS  = {"가죽","사슬","구리","철","금","다이아","네더"};
    private static final Material[] ARMOR_MAT_ICONS = {
        Material.LEATHER, Material.CHAINMAIL_CHESTPLATE, Material.COPPER_INGOT,
        Material.IRON_INGOT, Material.GOLD_INGOT, Material.DIAMOND, Material.NETHERITE_INGOT
    };
    private static final String[] SWORD_AXE_SCYTHE_MAT_KEYS   = {"wood","stone","iron","golden","diamond","netherite",""};
    private static final String[] SWORD_AXE_SCYTHE_MAT_LABELS = {"나무","돌","철","금","다이아","네더",""};
    private static final String[] STAFF_MAT_KEYS   = {"","","stick","blaze_rod","","",""};
    private static final String[] SPEAR_XBOW_MAT_KEYS = {"","","","default","","",""};

    private void fillDetailMaterialRow(Inventory gui, PlayerGrowthState state,
                                       EquipmentSlot equipSlot, WeaponType wt) {
        String selected = equipSlot == EquipmentSlot.WEAPON
                ? state.getCosmeticMaterial(weaponCosmeticKey(wt))
                : state.getCosmeticMaterial(equipSlot);
        if (selected.isEmpty()) selected = defaultCosmeticMat(equipSlot, wt);

        // 방어구는 가상 장비(DL-103) — 재질 선택 없음. 강화 정보만 표시하고 재질 row는 비운다.
        if (equipSlot != EquipmentSlot.WEAPON) {
            return;
        }

        switch (wt) {
            case SWORD, AXE, SCYTHE -> {
                for (int i = 0; i < 6; i++) {
                    boolean sel = SWORD_AXE_SCYTHE_MAT_KEYS[i].equals(selected);
                    Material icon = sel ? Material.LIME_STAINED_GLASS_PANE
                                       : weaponCosmeticMaterial(wt, SWORD_AXE_SCYTHE_MAT_KEYS[i]);
                    gui.setItem(10 + i, MainHubGui.icon(icon,
                            (sel ? "§a§l" : "§7") + SWORD_AXE_SCYTHE_MAT_LABELS[i],
                            sel ? List.of("§a▶ 현재 재질") : List.of("§7클릭하여 선택")));
                }
                // slot 16 — 무기는 6종이므로 빈칸
            }
            case STAFF -> {
                boolean sel0 = "stick".equals(selected);
                boolean sel1 = "blaze_rod".equals(selected);
                gui.setItem(12, MainHubGui.icon(
                        sel0 ? Material.LIME_STAINED_GLASS_PANE : Material.STICK,
                        (sel0 ? "§a§l" : "§7") + "막대기",
                        sel0 ? List.of("§a▶ 현재 재질") : List.of("§7클릭하여 선택")));
                gui.setItem(13, MainHubGui.icon(
                        sel1 ? Material.LIME_STAINED_GLASS_PANE : Material.BLAZE_ROD,
                        (sel1 ? "§a§l" : "§7") + "블레이즈 막대",
                        sel1 ? List.of("§a▶ 현재 재질") : List.of("§7클릭하여 선택")));
            }
            case SPEAR, CROSSBOW -> {
                gui.setItem(13, MainHubGui.icon(Material.LIME_STAINED_GLASS_PANE,
                        "§a§l기본 재질", List.of("§a▶ 현재 재질", "§8재질 변경 없음 (1종)")));
            }
            default -> {}
        }
    }

    private String defaultCosmeticMat(EquipmentSlot slot, WeaponType wt) {
        if (slot != EquipmentSlot.WEAPON) return "netherite";
        return switch (wt) {
            case SWORD, AXE, SCYTHE -> "netherite";
            case STAFF -> "blaze_rod";
            case SPEAR, CROSSBOW -> "default";
            default -> "";
        };
    }

    private String materialKeyAt(EquipmentSlot equipSlot, WeaponType wt, int index) {
        if (index < 0 || index > 6) return "";
        if (equipSlot != EquipmentSlot.WEAPON) return ARMOR_MAT_KEYS[index];
        return switch (wt) {
            case SWORD, AXE, SCYTHE -> SWORD_AXE_SCYTHE_MAT_KEYS[index];
            case STAFF -> STAFF_MAT_KEYS[index];
            case SPEAR, CROSSBOW -> SPEAR_XBOW_MAT_KEYS[index];
            default -> "";
        };
    }

    private Material weaponCosmeticMaterial(WeaponType wt, String tier) {
        return switch (wt) {
            case SWORD -> switch (tier) {
                case "wood"    -> Material.WOODEN_SWORD;
                case "stone"   -> Material.STONE_SWORD;
                case "iron"    -> Material.IRON_SWORD;
                case "golden"  -> Material.GOLDEN_SWORD;
                case "diamond" -> Material.DIAMOND_SWORD;
                default        -> Material.NETHERITE_SWORD;
            };
            case AXE -> switch (tier) {
                case "wood"    -> Material.WOODEN_AXE;
                case "stone"   -> Material.STONE_AXE;
                case "iron"    -> Material.IRON_AXE;
                case "golden"  -> Material.GOLDEN_AXE;
                case "diamond" -> Material.DIAMOND_AXE;
                default        -> Material.NETHERITE_AXE;
            };
            case SCYTHE -> switch (tier) {
                case "wood"    -> Material.WOODEN_HOE;
                case "stone"   -> Material.STONE_HOE;
                case "iron"    -> Material.IRON_HOE;
                case "golden"  -> Material.GOLDEN_HOE;
                case "diamond" -> Material.DIAMOND_HOE;
                default        -> Material.NETHERITE_HOE;
            };
            case STAFF -> "stick".equals(tier) ? Material.STICK : Material.BLAZE_ROD;
            case SPEAR -> Material.TRIDENT;
            case CROSSBOW -> Material.CROSSBOW;
            default -> Material.STICK;
        };
    }

    private void applyWeaponMaterial(Player player, WeaponType wt, String matKey) {
        ItemStack current = player.getInventory().getItem(0);
        if (current == null || current.getType() == Material.AIR) return;
        Material newMat = weaponCosmeticMaterial(wt, matKey);
        if (current.getType() == newMat) return;
        ItemStack newItem = new ItemStack(newMat, 1);
        org.bukkit.inventory.meta.ItemMeta meta = current.getItemMeta();
        if (meta != null) newItem.setItemMeta(meta);
        player.getInventory().setItem(0, newItem);
    }

    private ItemStack buildEquipDetailIcon(PlayerGrowthState state, EquipmentSlot slot, WeaponType wt) {
        String name = equipDisplayName(slot, wt);
        String instanceId = state.equippedItems().get(slot);
        if (instanceId == null) {
            return MainHubGui.icon(Material.GRAY_STAINED_GLASS_PANE, "§7" + name + " §8[미장착]", List.of());
        }
        PlayerEquipmentItem item = state.inventoryItem(instanceId).orElse(null);
        if (item == null) {
            return MainHubGui.icon(Material.GRAY_STAINED_GLASS_PANE, "§7" + name + " §8[미장착]", List.of());
        }
        Material mat = slot == EquipmentSlot.WEAPON
                ? weaponCosmeticMaterial(wt, state.getCosmeticMaterial(weaponCosmeticKey(wt)))
                : EQUIP_MATERIAL.getOrDefault(slot, Material.PAPER);
        return MainHubGui.icon(mat, "§f" + equipDisplayName(slot, wt),
                equipBaseLore(item, slot == EquipmentSlot.WEAPON, state.classEngravingId()));
    }

    private void handleEquipDetailClick(Player player, int slot) {
        EquipmentSlot equipSlot = equipDetailSlot.get(player.getUniqueId());
        if (equipSlot == null) { player.closeInventory(); return; }

        if (slot == 18) {
            equipDetailSlot.remove(player.getUniqueId());
            openCharacterHub(player);
            return;
        }

        if (slot == 8 && equipSlot == EquipmentSlot.WEAPON) {
            openWeaponChange(player);
            return;
        }

        if (slot >= 10 && slot <= 16) {
            if (equipSlot != EquipmentSlot.WEAPON) return; // 방어구는 재질 변경 없음(DL-103)
            WeaponType wt = playerDataManager.getWeaponType(player.getUniqueId());
            PlayerGrowthState state = getState(player);
            String matKey = materialKeyAt(equipSlot, wt, slot - 10);
            if (!matKey.isEmpty() && !"default".equals(matKey)) {
                state.setCosmeticMaterial(weaponCosmeticKey(wt), matKey);
                applyWeaponMaterial(player, wt, matKey);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
                openEquipDetail(player, equipSlot);
            }
        }
        // slot 24/26: 숨김/보이기 — 치장템 없으므로 무동작
    }

    private ItemStack buildStatSummaryIcon(Player player, PlayerGrowthState state) {
        double atk      = WeaponPowerCalculator.calculate(state, itemMasters, growthEngineRuntime.potentialService());
        var maxHpAttr   = player.getAttribute(Attribute.MAX_HEALTH);
        double hp       = maxHpAttr != null ? maxHpAttr.getValue() : 20.0;
        int critPts     = state.critPts();
        int endurPts    = state.endurPts();
        double critRate    = 5.0   + critPts  * 0.30;
        double critDmgPct  = 150.0 + critPts  * 0.15;
        double defBonus    = endurPts * 0.4;
        double dmgRedPct   = endurPts * 0.15;
        double bossDmgPct  = sumOptionFromEquipped(state, "boss_damage_increase");
        List<String> lore = List.of(
                "§7──────────────────",
                "§7 공격력  : §e" + (int) atk,
                "§7 방어력  : §e" + String.format("%.1f", defBonus),
                "§7 체력    : §e" + (int) hp,
                "§7 치명확률: §e" + String.format("%.1f", critRate) + "%",
                "§7 치명피해: §e" + String.format("%.0f", critDmgPct) + "%",
                "§7 보스피해: §e" + String.format("%.1f", bossDmgPct) + "%",
                "§7 피해감소: §e" + String.format("%.1f", dmgRedPct) + "%"
        );
        return MainHubGui.icon(Material.PAPER, "§e현재 스탯", lore);
    }

    /** 모든 장착 슬롯의 잠재 라인 + 서브스탯 라인에서 특정 optionCode 값을 합산. */
    private double sumOptionFromEquipped(PlayerGrowthState state, String optionCode) {
        double total = 0.0;
        for (String instanceId : state.equippedItems().values()) {
            PlayerEquipmentItem item = state.inventoryItem(instanceId).orElse(null);
            if (item == null) continue;
            PotentialProfile pp = item.potentialProfile();
            if (pp != null) {
                for (PotentialLine line : pp.lines()) {
                    if (optionCode.equals(line.optionCode())) total += line.value();
                }
            }
            for (PotentialLine line : item.substatLines()) {
                if (optionCode.equals(line.optionCode())) total += line.value();
            }
        }
        return total;
    }

    private record SkillInfo(String desc, String coeff, String range, String tags) {}

    private static final Map<String, SkillInfo> SKILL_INFO = Map.ofEntries(
        // ── 검 ─────────────────────────────────────────────────────
        Map.entry("sword:flash_slash",    new SkillInfo("전진하며 전방을 빠르게 베어낸다. 명중 시 검세 1충전.", "160% (+8%/스택)", "호형 2.5블럭·120° / 전방 2블럭", "근접·자원생성")),
        Map.entry("sword:triple_strike",  new SkillInfo("전방에 3연속 베기를 가한다.", "70% × 3", "호형 2.5블럭·120° ×3", "근접·다타")),
        Map.entry("sword:guard_counter",  new SkillInfo("짧은 방어 자세. 피격 시 반격, 미피격 시 약한 참격.", "반격 220% / 일반 120%", "자신 기준", "방어·반격")),
        Map.entry("sword:final_strike",   new SkillInfo("검세 소모/유지. 전방 직선 강한 참격.", "320% (+15%/스택)", "직선 6블럭", "핵심기·자원소모")),
        // ── 도끼 ────────────────────────────────────────────────────
        Map.entry("axe:smash",            new SkillInfo("전방의 적을 강하게 내려친다. 명중 시 충격 1충전.", "210% (+8%/스택)", "호형 2.5블럭·100°", "근접·자원생성")),
        Map.entry("axe:crush_charge",     new SkillInfo("짧게 전진한 뒤 강하게 내려찍는다.", "280%", "hitbox 직선 4블럭 (이동 3블럭)", "근접·돌진")),
        Map.entry("axe:unyielding",       new SkillInfo("충격 소모/유지. 3초간 피해 감소, 다음 강타 강화.", "다음 강타 +80% (+6%/스택)", "자신 기준", "방어·자기강화")),
        Map.entry("axe:colossal_drop",    new SkillInfo("제자리 거대 내려찍기. 주변 적 광역 피해.", "420% (+10%/스택)", "원형 4.5블럭", "핵심기·광역")),
        // ── 창 ──────────────────────────────────────────────────────
        Map.entry("spear:thrust",         new SkillInfo("전방 긴 직선을 찌른다. 명중 시 압박 1충전.", "170% (+5%/스택)", "직선 5블럭", "근접·자원생성")),
        Map.entry("spear:crescent",       new SkillInfo("짧은 부채꼴 범위 휘두르기.", "190%", "호형 3블럭·150°", "근접·광역")),
        Map.entry("spear:charge",         new SkillInfo("전방 돌진하며 경로상 적을 찌른다.", "260%", "직선 5블럭 돌진", "이동기·관통")),
        Map.entry("spear:thunderstrike",  new SkillInfo("압박 소모/유지. 긴 직선 강력한 창격.", "360% (+8%/스택)", "직선 9블럭", "핵심기·관통")),
        // ── 석궁 ────────────────────────────────────────────────────
        Map.entry("crossbow:rapid_fire",  new SkillInfo("화살 3발을 빠르게 발사한다. 명중 시 명중 스택 +1.", "75% × 3", "투사체 20블럭·3발", "원거리·다발")),
        Map.entry("crossbow:evade_fire",  new SkillInfo("뒤로 물러나며 사격한다. 명중 시 명중 스택 +1.", "170%", "투사체 25블럭·후방 2.5블럭", "원거리·이동기")),
        Map.entry("crossbow:pierce_bolt", new SkillInfo("직선으로 관통하는 강한 볼트를 발사한다.", "220%", "투사체 30블럭·관통", "원거리·관통")),
        Map.entry("crossbow:sniper",      new SkillInfo("명중 스택 소모/유지. 강력한 단일 사격.", "400% (+12%/스택)", "투사체 50블럭·시전시간", "핵심기·단일")),
        // ── 낫 ──────────────────────────────────────────────────────
        Map.entry("scythe:death_slash",   new SkillInfo("넓은 전방 범위를 베어낸다.", "180% (+5%/스택)", "호형 3블럭·150°", "근접·광역")),
        Map.entry("scythe:shadow_spin",   new SkillInfo("측면 이동하며 주변 적을 회전 베기로 타격한다.", "60% × 4", "원형 3블럭·측면 2.5블럭", "이동기·광역")),
        Map.entry("scythe:grim_strike",   new SkillInfo("전방 부채꼴 강타. 흐름 스택에 따라 흡혈 발동.", "240% (+8%/스택)", "원뿔 4블럭·60°", "근접·흡혈")),
        Map.entry("scythe:execution",     new SkillInfo("흐름 소모/유지. 잔여 체력 비례 추가 피해.", "280~480% (+10%/스택)", "직선 4블럭", "핵심기·처형")),
        // ── 스태프 ──────────────────────────────────────────────────
        Map.entry("staff:arcane_orb",      new SkillInfo("마력 투사체를 발사한다. 명중 시 마력 1충전.", "150%", "투사체 20블럭", "원거리·자원생성")),
        Map.entry("staff:elemental_burst", new SkillInfo("착탄 지점에 즉발 폭발을 일으킨다.", "240%", "투사체 18블럭·AoE 2.5블럭", "원거리·광역")),
        Map.entry("staff:arcane_rush",     new SkillInfo("전방 즉발 광역 폭발. 발동 시 미세 후방 이동.", "200%", "원형 4블럭·후방 1.5블럭", "이동기·광역")),
        Map.entry("staff:starburst",       new SkillInfo("마력 소모/유지. 전방 강력한 마법 탄환.", "380% (+10%/스택)", "투사체 22블럭·시전시간", "핵심기·단일"))
    );

    private ItemStack skillSlotIcon(WeaponType wt, int skillNo, String label) {
        String key = skillKeyByNo(wt, skillNo);
        WeaponSkill sk = (key != null && skillService != null) ? skillService.getSkill(key) : null;
        if (sk == null) {
            return MainHubGui.icon(Material.PAPER, "§7" + label + " §8미등록", List.of());
        }
        long cdSec = sk.cooldown() / 1000;
        SkillInfo info = key != null ? SKILL_INFO.get(key) : null;
        String desc  = info != null ? info.desc()  : "N/A";
        String coeff = info != null ? info.coeff() : "N/A";
        String range = info != null ? info.range() : "N/A";
        String tags  = info != null ? info.tags()  : "N/A";
        return MainHubGui.icon(Material.PAPER, "§e" + label + " §f" + sk.displayName(),
                List.of("§7──────────────────",
                        "§7설명  : §f" + desc,
                        "§7계수  : §e" + coeff,
                        "§7쿨타임: §e" + cdSec + "초",
                        "§7범위  : §7" + range,
                        "§7태그  : §7" + tags));
    }

    private static String skillKeyByNo(WeaponType wt, int no) {
        return switch (no) {
            case 1 -> switch (wt) {
                case SWORD -> "sword:flash_slash";    case AXE -> "axe:smash";
                case SPEAR -> "spear:thrust";         case CROSSBOW -> "crossbow:rapid_fire";
                case SCYTHE -> "scythe:death_slash";  case STAFF -> "staff:arcane_orb";
                case NONE -> null;
            };
            case 2 -> switch (wt) {
                case SWORD -> "sword:triple_strike";  case AXE -> "axe:crush_charge";
                case SPEAR -> "spear:crescent";       case CROSSBOW -> "crossbow:evade_fire";
                case SCYTHE -> "scythe:shadow_spin";  case STAFF -> "staff:elemental_burst";
                case NONE -> null;
            };
            case 3 -> switch (wt) {
                case SWORD -> "sword:guard_counter";  case AXE -> "axe:unyielding";
                case SPEAR -> "spear:charge";         case CROSSBOW -> "crossbow:pierce_bolt";
                case SCYTHE -> "scythe:grim_strike";  case STAFF -> "staff:arcane_rush";
                case NONE -> null;
            };
            case 4 -> switch (wt) {
                case SWORD -> "sword:final_strike";   case AXE -> "axe:colossal_drop";
                case SPEAR -> "spear:thunderstrike";  case CROSSBOW -> "crossbow:sniper";
                case SCYTHE -> "scythe:execution";    case STAFF -> "staff:starburst";
                case NONE -> null;
            };
            default -> null;
        };
    }

    // ══════════════════════════════════════════════════════════════════
    // 스탯 배분 GUI (54슬롯, gui_stat_allocation.md)
    // ══════════════════════════════════════════════════════════════════

    private void openStatAllocation(Player player) {
        PlayerGrowthState state = getState(player);
        boolean inCombat = combatStateService.isInCombat(player.getUniqueId());
        int unspent = state.unspentPts();
        int crit    = state.critPts();
        int spec    = state.specPts();
        int endur   = state.endurPts();
        int total   = crit + spec + endur + unspent;

        Inventory gui = Bukkit.createInventory(null, 54, GuiTitles.STAT_ALLOCATION);
        for (int i = 0; i < 54; i++) gui.setItem(i, pane());

        // 잔여 포인트 (slot 4)
        gui.setItem(4, MainHubGui.icon(Material.EXPERIENCE_BOTTLE, "§e잔여 스탯 포인트", List.of(
                "§7──────────────────",
                "§7 잔여  : §e" + unspent + "pt",
                "§7 사용  : §7" + (total - unspent) + "pt",
                "§7 총계  : §7" + total + "pt",
                "§7──────────────────",
                "§7 비전투 중에만 재배분 가능.")));

        // 치명 트리 (row 1: slots 10~17)
        gui.setItem(10, MainHubGui.icon(Material.QUARTZ,        "§e치명 트리", List.of("§7치명타 확률/피해량")));
        gui.setItem(11, statMinusBtn(crit,   10, inCombat));
        gui.setItem(12, statMinusBtn(crit,    1, inCombat));
        gui.setItem(13, treePtsIcon("치명", crit, crit * 0.30, "치명타 확률%", crit * 0.15, "치명타 피해량%"));
        gui.setItem(14, statPlusBtn(unspent,  1, inCombat));
        gui.setItem(15, statPlusBtn(unspent, 10, inCombat));
        gui.setItem(16, statValueIcon("치명타 확률",   5.0 + crit * 0.30, "%"));
        gui.setItem(17, statValueIcon("치명타 피해량", 150.0 + crit * 0.15, "%"));

        // 특화 트리 (row 2: slots 19~26)
        gui.setItem(19, MainHubGui.icon(Material.BLAZE_POWDER,  "§e특화 트리", List.of("§7스킬/태그 피해량")));
        gui.setItem(20, statMinusBtn(spec,   10, inCombat));
        gui.setItem(21, statMinusBtn(spec,    1, inCombat));
        gui.setItem(22, treePtsIcon("특화", spec, spec * 0.30, "스킬 피해%", spec * 0.15, "태그 피해%"));
        gui.setItem(23, statPlusBtn(unspent,  1, inCombat));
        gui.setItem(24, statPlusBtn(unspent, 10, inCombat));
        gui.setItem(25, statValueIcon("스킬 피해",  spec * 0.30,  "%"));
        gui.setItem(26, statValueIcon("태그 피해",  spec * 0.15,  "%"));

        // 인내 트리 (row 3: slots 28~35)
        gui.setItem(28, MainHubGui.icon(Material.SHIELD,        "§e인내 트리", List.of("§7피해 감소/방어력")));
        gui.setItem(29, statMinusBtn(endur,  10, inCombat));
        gui.setItem(30, statMinusBtn(endur,   1, inCombat));
        gui.setItem(31, treePtsIcon("인내", endur, endur * 0.15, "피해감소%", endur * 0.4, "방어력+"));
        gui.setItem(32, statPlusBtn(unspent,  1, inCombat));
        gui.setItem(33, statPlusBtn(unspent, 10, inCombat));
        gui.setItem(34, statValueIcon("받는 피해 감소", endur * 0.15, "%"));
        gui.setItem(35, statValueIcon("방어력 보너스",  endur * 0.4,  ""));

        // 뒤로 / 초기화
        gui.setItem(45, MainHubGui.icon(Material.ARROW, "§7뒤로", List.of("§7캐릭터")));
        boolean resetting = pendingStatReset.getOrDefault(player.getUniqueId(), false);
        if (resetting) {
            gui.setItem(50, MainHubGui.icon(Material.FIRE_CHARGE, "§c확인하려면 다시 클릭",
                    List.of("§7재클릭 → 초기화 실행", "§7다른 슬롯 클릭 → 취소")));
        } else {
            gui.setItem(50, MainHubGui.icon(Material.BARRIER, "§c전체 초기화",
                    List.of("§7──────────────────",
                            "§7 투자한 모든 포인트를 초기화합니다.",
                            "§7 클릭 후 확인 클릭으로 적용됩니다.",
                            inCombat ? "§c 전투 중에는 사용할 수 없습니다." : "")));
        }

        player.openInventory(gui);
    }

    private void handleStatAllocClick(Player player, int slot) {
        UUID uid = player.getUniqueId();
        if (slot == 45) { pendingStatReset.remove(uid); openCharacterHub(player); return; }

        boolean inCombat = combatStateService.isInCombat(uid);
        PlayerGrowthState state = getState(player);

        if (slot == 50) {
            if (inCombat) { player.sendMessage("§c[스탯] 전투 중에는 초기화할 수 없습니다."); return; }
            if (pendingStatReset.getOrDefault(uid, false)) {
                int total = state.critPts() + state.specPts() + state.endurPts() + state.unspentPts();
                state.setCritPts(0); state.setSpecPts(0); state.setEndurPts(0);
                state.setUnspentPts(total);
                pendingStatReset.remove(uid);
                player.sendMessage("§a[스탯] 포인트가 모두 초기화되었습니다.");
                scoreboardService.refresh(player);
            } else {
                pendingStatReset.put(uid, true);
            }
            openStatAllocation(player);
            return;
        }

        pendingStatReset.remove(uid);

        if (inCombat) { player.sendMessage("§c[스탯] 전투 중에는 포인트를 변경할 수 없습니다."); return; }

        switch (slot) {
            case 11 -> { for (int i = 0; i < 10; i++) state.deallocatePt("crit"); }
            case 12 -> state.deallocatePt("crit");
            case 14 -> state.allocatePt("crit");
            case 15 -> { for (int i = 0; i < 10; i++) state.allocatePt("crit"); }
            case 20 -> { for (int i = 0; i < 10; i++) state.deallocatePt("spec"); }
            case 21 -> state.deallocatePt("spec");
            case 23 -> state.allocatePt("spec");
            case 24 -> { for (int i = 0; i < 10; i++) state.allocatePt("spec"); }
            case 29 -> { for (int i = 0; i < 10; i++) state.deallocatePt("endur"); }
            case 30 -> state.deallocatePt("endur");
            case 32 -> state.allocatePt("endur");
            case 33 -> { for (int i = 0; i < 10; i++) state.allocatePt("endur"); }
            default -> { return; }
        }
        scoreboardService.refresh(player);
        openStatAllocation(player);
    }

    private ItemStack statMinusBtn(int currentPts, int amount, boolean inCombat) {
        boolean disabled = inCombat || currentPts < amount;
        if (disabled) {
            return MainHubGui.icon(Material.GRAY_STAINED_GLASS_PANE, "§8-" + amount,
                    List.of(inCombat ? "§c전투 중 사용 불가" : "§c포인트 부족"));
        }
        Material mat = amount >= 10 ? Material.RED_STAINED_GLASS_PANE : Material.ORANGE_STAINED_GLASS_PANE;
        return MainHubGui.icon(mat, "§c-" + amount, List.of());
    }

    private ItemStack statPlusBtn(int unspent, int amount, boolean inCombat) {
        boolean disabled = inCombat || unspent < amount;
        if (disabled) {
            return MainHubGui.icon(Material.GRAY_STAINED_GLASS_PANE, "§8+" + amount,
                    List.of(inCombat ? "§c전투 중 사용 불가" : "§c잔여 포인트 부족"));
        }
        Material mat = amount >= 10 ? Material.GREEN_STAINED_GLASS_PANE : Material.LIME_STAINED_GLASS_PANE;
        return MainHubGui.icon(mat, "§a+" + amount, List.of());
    }

    private ItemStack treePtsIcon(String treeName, int pts,
                                   double mainVal, String mainName,
                                   double subVal,  String subName) {
        return MainHubGui.icon(Material.PAPER, "§e" + treeName + " 트리", List.of(
                "§7──────────────────",
                "§7 투자 포인트: §e" + pts + "pt",
                "§7──────────────────",
                "§a주 효과§7 : " + mainName + " §e+" + String.format("%.2f", mainVal),
                "§7부 효과§7 : " + subName  + " §7+" + String.format("%.2f", subVal)));
    }

    private ItemStack statValueIcon(String name, double value, String unit) {
        return MainHubGui.icon(Material.BOOK, "§f" + name,
                List.of("§7현재값: §e" + String.format("%.2f", value) + unit));
    }

    private ItemStack equipHubSlotIcon(PlayerGrowthState state, EquipmentSlot slot, WeaponType wt) {
        String label = equipDisplayName(slot, wt);
        String instanceId = state.equippedItems().get(slot);
        PlayerEquipmentItem item = instanceId == null ? null : state.inventoryItem(instanceId).orElse(null);
        if (item == null) {
            return MainHubGui.icon(Material.GRAY_STAINED_GLASS_PANE, "§7" + label + " §8[미장착]", List.of());
        }
        Material mat = slot == EquipmentSlot.WEAPON
                ? weaponCosmeticMaterial(wt, state.getCosmeticMaterial(weaponCosmeticKey(wt)))
                : EQUIP_MATERIAL.getOrDefault(slot, Material.NETHER_STAR);
        return MainHubGui.icon(mat, "§f" + label,
                equipBaseLore(item, slot == EquipmentSlot.WEAPON, state.classEngravingId()));
    }

    private Material weaponMaterial(WeaponType wt) {
        return switch (wt) {
            case SWORD, SPEAR -> Material.NETHERITE_SWORD;
            case AXE          -> Material.NETHERITE_AXE;
            case CROSSBOW     -> Material.CROSSBOW;
            case SCYTHE       -> Material.NETHERITE_HOE;
            case STAFF        -> Material.BLAZE_ROD;
            case NONE         -> Material.STICK;
        };
    }

    private String weaponDisplayName(WeaponType wt) {
        return switch (wt) {
            case SWORD    -> "검";
            case AXE      -> "도끼";
            case SPEAR    -> "창";
            case CROSSBOW -> "석궁";
            case SCYTHE   -> "낫";
            case STAFF    -> "스태프";
            case NONE     -> "없음";
        };
    }

    /** 무기 타입별 치장 재질 저장 키 */
    private static String weaponCosmeticKey(WeaponType wt) {
        return "weapon_" + wt.name();
    }

    /** 무기 타입의 스킬 4개 이름을 " / "로 연결한 요약 문자열 */
    private String weaponSkillSummary(WeaponType wt) {
        if (wt == WeaponType.NONE) return "없음";
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= 4; i++) {
            String key = skillKeyByNo(wt, i);
            WeaponSkill sk = (key != null && skillService != null) ? skillService.getSkill(key) : null;
            if (sb.length() > 0) sb.append(" / ");
            if (sk != null) sb.append(sk.displayName());
            else if (key != null) sb.append(key.contains(":") ? key.split(":")[1] : key);
            else sb.append("?");
        }
        return sb.toString();
    }

    // ══════════════════════════════════════════════════════════════════
    // 무기 변경 GUI (45슬롯, gui_equipment_detail.md §2)
    // ══════════════════════════════════════════════════════════════════

    private void openWeaponChange(Player player) {
        WeaponType currentWt = playerDataManager.getWeaponType(player.getUniqueId());
        // GrowthStateStore는 UUID당 1개 — 모든 무기 타입이 같은 state 공유
        PlayerGrowthState sharedState = getState(player);

        // 스펙 36칸 공용 레이아웃(WeaponGui) — 튜토리얼 최초 선택과 같은 배치 재사용 (DL-104).
        Inventory gui = Bukkit.createInventory(null, WeaponGui.SIZE, GuiTitles.WEAPON_CHANGE);
        for (int i = 0; i < WeaponGui.SIZE; i++) gui.setItem(i, pane());

        for (int i = 0; i < WeaponGui.ORDER.length; i++) {
            WeaponType wt = WeaponGui.ORDER[i];
            gui.setItem(WeaponGui.SLOTS[i], buildWeaponChangeIcon(wt, sharedState, wt == currentWt));
        }

        gui.setItem(WeaponGui.BACK_SLOT, MainHubGui.icon(Material.ARROW, "§7뒤로", List.of("§7장비 세부")));
        player.openInventory(gui);
    }

    private ItemStack buildWeaponChangeIcon(WeaponType wt, PlayerGrowthState sharedState, boolean isCurrent) {
        // 무기별 독립 데이터 — 각 무기 타입의 고유 인스턴스(weapon_<타입>)를 읽는다 (DL-104).
        // 장비 lore는 정본 렌더러에 위임(강화/등급/잠재/세부스탯/각인) — 손무기/상세 GUI와 형식 통일.
        PlayerEquipmentItem item = sharedState.inventoryItem(WeaponGui.weaponInstanceId(wt)).orElse(null);
        // 무기별 독립 각인(DL-110) — 각 무기 아이콘은 자기 무기 타입(classId)의 각인을 표시.
        String engravingId = sharedState.classEngravingId(wt.name().toLowerCase(Locale.ROOT));
        List<String> lore = new ArrayList<>(EquipmentLoreRenderer.baseLore(
                item, true, engravingId, "§8미육성 §7(교체 시 +0강으로 생성)"));
        // 마지막 구분선 앞에 스킬 줄 삽입(무기 변경 GUI 전용)
        lore.remove(lore.size() - 1);
        lore.add("§7스킬   : §7" + weaponSkillSummary(wt));
        lore.add("§7──────────────────");
        if (isCurrent) {
            lore.add("§a§l[현재 장착 중]");
        } else {
            lore.add("§7[클릭] §e이 무기로 교체");
        }

        String cosmeticMat = sharedState.getCosmeticMaterial(weaponCosmeticKey(wt));
        Material icon = weaponCosmeticMaterial(wt, cosmeticMat);
        String title = (isCurrent ? "§a§l" : "§e") + weaponDisplayName(wt) + (isCurrent ? " §a[장착 중]" : "");
        return MainHubGui.icon(icon, title, lore);
    }

    private void handleWeaponChangeClick(Player player, int slot) {
        if (slot == WeaponGui.BACK_SLOT) {
            EquipmentSlot prevSlot = equipDetailSlot.getOrDefault(player.getUniqueId(), EquipmentSlot.WEAPON);
            openEquipDetail(player, prevSlot);
            return;
        }

        WeaponType newWt = WeaponGui.slotToWeapon(slot);
        if (newWt == null) return;

        WeaponType currentWt = playerDataManager.getWeaponType(player.getUniqueId());
        if (newWt == currentWt) {
            player.sendMessage("§7이미 §e" + weaponDisplayName(newWt) + "§7 무기를 사용 중입니다.");
            return;
        }

        if (combatStateService.isInCombat(player.getUniqueId())) {
            player.sendMessage("§c전투 중에는 무기를 교체할 수 없습니다.");
            return;
        }

        playerDataManager.setWeaponType(player.getUniqueId(), newWt);

        // 무기별 독립 성장 — 해당 무기의 고유 인스턴스를 보장하고 WEAPON 슬롯에 장착한다 (DL-104).
        // 강화/큐브/잠재는 장착된 인스턴스를 대상으로 하므로, 교체 후 작업은 이 무기에만 적용된다.
        PlayerGrowthState state = getState(player);
        // 무기별 독립 각인(DL-110) — classId만 새 무기로 갱신하면 각인/스킬이 자동으로 새 무기 기준이 된다.
        // 각 무기가 자기 각인을 보유하므로 이전 각인 해제 불필요(단일 필드 시절의 잔존 버그 해소).
        state.setClassId(newWt.name().toLowerCase(java.util.Locale.ROOT));
        ensureWeaponInstance(state, newWt);
        state.equipItem(EquipmentSlot.WEAPON, WeaponGui.weaponInstanceId(newWt));

        // 손에 든 무기 — 치장 재질 반영 + 무기변경 형식 lore (강화/등급/잠재/세부스탯/각인)
        String cosmeticMat = state.getCosmeticMaterial(weaponCosmeticKey(newWt));
        Material weapMat = weaponCosmeticMaterial(newWt, cosmeticMat);
        player.getInventory().setItem(0, WeaponItemFactory.build(state, newWt, weapMat));

        scoreboardService.refresh(player);
        player.sendMessage("§a[무기 교체] §e" + weaponDisplayName(newWt) + "§a 무기로 교체되었습니다.");
        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 0.8f, 1.2f);
        openWeaponChange(player);
    }

    /** 무기 타입의 독립 성장 인스턴스(weapon_<타입>)가 없으면 +0강 기본으로 생성한다. */
    private void ensureWeaponInstance(PlayerGrowthState state, WeaponType wt) {
        String iid = WeaponGui.weaponInstanceId(wt);
        if (state.inventoryItem(iid).isEmpty()) {
            state.addInventoryItem(PlayerEquipmentItem.restore(
                    iid, WeaponGui.starterItemId(wt), 0, ItemGrade.COMMON, null, List.of()));
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 공통 유틸
    // ═══════════════════════════════════════════════════════════════

    private PlayerGrowthState getState(Player player) {
        WeaponType wt = playerDataManager.getWeaponType(player.getUniqueId());
        return growthStateStore.getOrCreate(player.getUniqueId(), wt.name().toLowerCase(Locale.ROOT));
    }

    private ItemStack buildEnhInfoIcon(PlayerEquipmentItem item, String itemName, int curLv, EnhancementRule rule) {
        List<String> lore = new ArrayList<>();
        lore.add("§7──────────────");
        lore.add("§7현재 강화: §e+" + curLv);
        if (rule != null) {
            lore.add("§7목표: §e+" + (curLv + 1) + " §7| 성공률: §e" + String.format("%.1f%%", rule.successRate()));
            lore.add("§7비용: 골드 §e" + rule.goldCost() + " §7/ 강화석 §e" + rule.stoneCost());
        }
        lore.add("§7잠재: " + potentialOneLiner(item));
        return MainHubGui.icon(Material.PAPER, "§f" + itemName + " §e+" + curLv, lore);
    }

    private String itemDisplayName(PlayerEquipmentItem item) {
        if (item == null) return "알 수 없음";
        return itemDisplayNameById(item.itemId());
    }

    private String itemDisplayNameById(String itemId) {
        return itemMasters.find(itemId).map(ItemMaster::itemName).orElse(itemId);
    }

    private java.util.Optional<ItemMaster> masterFor(PlayerEquipmentItem item) {
        if (item == null) return java.util.Optional.empty();
        return itemMasters.find(item.itemId());
    }

    private String potentialOneLiner(PlayerEquipmentItem item) {
        if (item == null) return "§8잠재: 없음";
        PotentialProfile p = item.potentialProfile();
        if (p == null) return "§8잠재: 없음";
        return gradeColor(p.grade()) + EquipmentLoreRenderer.potentialGradeKr(p.grade()) + " §7(" + p.lines().size() + "라인)";
    }

    private String gradeColor(PotentialGrade grade) {
        return switch (grade) {
            case COMMON    -> "§7";
            case RARE      -> "§9";
            case EPIC      -> "§5";
            case UNIQUE    -> "§6";
            case LEGENDARY -> "§c";
        };
    }

    private String gradeColor(ItemGrade grade) {
        return switch (grade) {
            case COMMON    -> "§7";
            case RARE      -> "§9";
            case EPIC      -> "§5";
            case UNIQUE    -> "§6";
            case LEGENDARY -> "§c";
        };
    }

    private String slotLabel(EquipmentSlot slot) {
        return switch (slot) {
            case WEAPON      -> "무기";
            case HELMET      -> "투구";
            case CHESTPLATE  -> "흉갑";
            case LEGGINGS    -> "레깅스";
            case BOOTS       -> "부츠";
            default          -> slot.name();
        };
    }


    /** 손에 든 무기(slot 0) lore를 현재 성장 상태로 재빌드 — 강화/잠재/각인 후 실시간 반영(DL-104 후속). */
    private void refreshHeldWeapon(Player player, PlayerGrowthState state) {
        WeaponType wt = playerDataManager.getWeaponType(player.getUniqueId());
        if (wt == WeaponType.NONE) return;
        ItemStack held = player.getInventory().getItem(0);
        if (held == null || !held.hasItemMeta()
                || !held.getItemMeta().getPersistentDataContainer()
                        .has(WeaponTypeResolver.WEAPON_TYPE_KEY, PersistentDataType.STRING)) return;
        Material mat = weaponCosmeticMaterial(wt, state.getCosmeticMaterial(weaponCosmeticKey(wt)));
        player.getInventory().setItem(0, WeaponItemFactory.build(state, wt, mat));
    }

    /** 장비 표시명 — 무기는 타입명(검/도끼…), 방어구는 슬롯명(투구/흉갑/레깅스/부츠). 모든 GUI 공용. */
    private String equipDisplayName(EquipmentSlot slot, WeaponType wt) {
        return slot == EquipmentSlot.WEAPON ? WeaponGui.displayName(wt) : slotLabel(slot);
    }

    /**
     * 장비 아이콘 정본 lore (WeaponItemFactory.buildLore와 동일 형식) — 강화/잠재/전승/캐릭터/상세 GUI 공용.
     * 구분선 / 강화 / 등급 / 잠재 / 세부스탯 / 각인(무기만) / 구분선.
     */
    private List<String> equipBaseLore(PlayerEquipmentItem item, boolean isWeapon, String engravingId) {
        return EquipmentLoreRenderer.baseLore(item, isWeapon, engravingId);
    }

    private EquipmentSlot findEquipSlot(PlayerGrowthState state, String instanceId) {
        for (Map.Entry<EquipmentSlot, String> e : state.equippedItems().entrySet()) {
            if (e.getValue().equals(instanceId)) return e.getKey();
        }
        return null;
    }

    private ItemStack pane() {
        return MainHubGui.icon(Material.BLACK_STAINED_GLASS_PANE, " ", List.of());
    }

    // ═══════════════════════════════════════════════════════════════
    // 각인 GUI (27슬롯)
    //   slot 10: 현재각인(해제) / slot 14: 소모형(각인A) / slot 16: 유지형(각인B) / slot 18: 뒤로 / slot 26: 닫기
    // ═══════════════════════════════════════════════════════════════

    private void openGrowthEngraving(Player player) {
        WeaponType wt = playerDataManager.getWeaponType(player.getUniqueId());
        PlayerGrowthState state = growthStateStore.getOrCreate(
                player.getUniqueId(), wt.name().toLowerCase(Locale.ROOT));
        String current = state.classEngravingId();
        String weaponId = wt.name().toLowerCase(Locale.ROOT);

        // 각인 A=소모형, 각인 B=유지형 (weapon_skills_v1.md §자원 시스템)
        String consumedId = weaponId + "_consumed_01";
        String retainedId = weaponId + "_retained_01";
        EngravingMaster consumed = growthEngineRuntime.engravingRegistry().find(consumedId).orElse(null);
        EngravingMaster retained = growthEngineRuntime.engravingRegistry().find(retainedId).orElse(null);

        Inventory gui = Bukkit.createInventory(null, 27, GuiTitles.GROWTH_ENGRAVING);
        for (int i = 0; i < 27; i++) gui.setItem(i, pane());

        // slot 10: 현재 장착 각인 (클릭 → 해제)
        if (current.isEmpty()) {
            gui.setItem(10, MainHubGui.icon(Material.GRAY_STAINED_GLASS_PANE,
                    "§7[없음] 각인 미장착", List.of()));
        } else {
            EngravingMaster cur = growthEngineRuntime.engravingRegistry().find(current).orElse(null);
            String name = cur != null ? cur.engravingName() : current;
            gui.setItem(10, MainHubGui.icon(Material.ENCHANTED_BOOK,
                    "§e[장착 중] §f" + name, List.of("§7클릭하여 해제")));
        }

        int baseMax = (wt == WeaponType.SPEAR || wt == WeaponType.STAFF) ? 5 : 3;

        // slot 14: 각인 A (소모형)
        if (consumed != null) {
            boolean sel = consumedId.equals(current);
            gui.setItem(14, engravingIcon(consumed, sel, "소모형 (각인 A)",
                    List.of("§7최대 " + baseMax + "스택. F 발동 시 전체 소모.",
                            sel ? "§a현재 장착 중" : "§7클릭하여 선택")));
        }
        // slot 16: 각인 B (유지형)
        if (retained != null) {
            boolean sel = retainedId.equals(current);
            gui.setItem(16, engravingIcon(retained, sel, "유지형 (각인 B)",
                    List.of("§7최대 6스택. F 소모 없음.",
                            sel ? "§a현재 장착 중" : "§7클릭하여 선택")));
        }

        gui.setItem(18, MainHubGui.icon(Material.ARROW, "§7뒤로", List.of("§7장비 허브")));
        player.openInventory(gui);
    }

    private void handleEngravingClick(Player player, int slot) {
        WeaponType wt = playerDataManager.getWeaponType(player.getUniqueId());
        if (wt == WeaponType.NONE) return;
        String weaponId = wt.name().toLowerCase(Locale.ROOT);
        PlayerGrowthState state = growthStateStore.getOrCreate(player.getUniqueId(), weaponId);

        switch (slot) {
            case 18 -> openEquipmentHub(player);
            case 10 -> { // 현재 각인 해제
                if (!state.classEngravingId().isEmpty()) {
                    state.setClassEngravingId("");
                    scoreboardService.refresh(player);
                    refreshHeldWeapon(player, state); // 각인 해제 후 손무기 lore 반영
                    player.sendMessage("§7[각인] 각인을 해제했습니다.");
                    openGrowthEngraving(player);
                }
            }
            case 14 -> equipEngraving(player, state, weaponId + "_consumed_01");
            case 16 -> equipEngraving(player, state, weaponId + "_retained_01");
        }
    }

    private void equipEngraving(Player player, PlayerGrowthState state, String engravingId) {
        var result = growthEngineRuntime.engravingService().equipClassEngraving(state, engravingId);
        if (result.isOk()) {
            scoreboardService.refresh(player);
            refreshHeldWeapon(player, state); // 각인 후 손무기 lore 실시간 반영
            EngravingMaster em = growthEngineRuntime.engravingRegistry().find(engravingId).orElse(null);
            String name = em != null ? em.engravingName() : engravingId;
            player.sendMessage("§a[각인] §f" + name + " §a각인을 장착했습니다.");
            openGrowthEngraving(player);
        } else {
            player.sendMessage("§c[각인] " + result.message());
        }
    }

    private static ItemStack engravingIcon(EngravingMaster e, boolean selected,
                                            String type, List<String> desc) {
        Material mat = selected ? Material.ENCHANTED_BOOK : Material.BOOK;
        String name = (selected ? "§e[장착 중] " : "§f") + e.engravingName() + " §8(" + type + ")";
        return MainHubGui.icon(mat, name, desc);
    }
}
