package com.poro.empire.listener;

import com.poro.empire.combat.CombatStateService;
import com.poro.empire.combat.weapon.WeaponType;
import com.poro.empire.common.registry.master.ItemMasterRegistry;
import com.poro.empire.common.registry.master.model.ItemMaster;
import com.poro.empire.growth.GrowthStateStore;
import com.poro.empire.growth.engine.EnhancementRule;
import com.poro.empire.growth.engine.EnhancementService;
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
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class GrowthGuiListener implements Listener {

    // ═══════════════════════════════════════════════════════════════
    // 강화 GUI 슬롯 상수
    // 5슬롯(무기/투구/상의/하의/신발) — 악세서리 제외(1차 시즌 확정)
    // ═══════════════════════════════════════════════════════════════
    private static final int ENH_SLOT_WEAPON     = 10;
    private static final int ENH_SLOT_HELMET     = 12;
    private static final int ENH_SLOT_CHESTPLATE = 14;
    private static final int ENH_SLOT_LEGGINGS   = 16;
    private static final int ENH_SLOT_BOOTS      = 28;
    private static final int ENH_SLOT_INFO       = 22;
    private static final int ENH_SLOT_ACTION     = 40;
    private static final int ENH_SLOT_BACK       = 45;
    private static final int ENH_SLOT_CLOSE      = 53;

    private static final Map<Integer, EquipmentSlot> ENH_GUI_TO_EQUIP = Map.of(
        ENH_SLOT_WEAPON,     EquipmentSlot.WEAPON,
        ENH_SLOT_HELMET,     EquipmentSlot.HELMET,
        ENH_SLOT_CHESTPLATE, EquipmentSlot.CHESTPLATE,
        ENH_SLOT_LEGGINGS,   EquipmentSlot.LEGGINGS,
        ENH_SLOT_BOOTS,      EquipmentSlot.BOOTS
    );

    private static final Map<EquipmentSlot, Integer> ENH_EQUIP_TO_GUI = Map.of(
        EquipmentSlot.WEAPON,     ENH_SLOT_WEAPON,
        EquipmentSlot.HELMET,     ENH_SLOT_HELMET,
        EquipmentSlot.CHESTPLATE, ENH_SLOT_CHESTPLATE,
        EquipmentSlot.LEGGINGS,   ENH_SLOT_LEGGINGS,
        EquipmentSlot.BOOTS,      ENH_SLOT_BOOTS
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

    private static final Map<EquipmentSlot, Material> EQUIP_MATERIAL = Map.of(
        EquipmentSlot.WEAPON,     Material.DIAMOND_SWORD,
        EquipmentSlot.HELMET,     Material.DIAMOND_HELMET,
        EquipmentSlot.CHESTPLATE, Material.DIAMOND_CHESTPLATE,
        EquipmentSlot.LEGGINGS,   Material.DIAMOND_LEGGINGS,
        EquipmentSlot.BOOTS,      Material.DIAMOND_BOOTS
    );

    // ═══════════════════════════════════════════════════════════════
    // 플레이어별 상태 (선택 + 대기 결과)
    // ═══════════════════════════════════════════════════════════════
    private final Map<UUID, String>                     selectedEnhanceId       = new ConcurrentHashMap<>();
    private final Map<UUID, String>                     selectedPotentialId     = new ConcurrentHashMap<>();
    private final Map<UUID, PotentialService.PotentialOperationResult> pendingPotentialResult = new ConcurrentHashMap<>();

    // ═══════════════════════════════════════════════════════════════
    // 의존성
    // ═══════════════════════════════════════════════════════════════
    private final GrowthStateStore    growthStateStore;
    private final GrowthEngineRuntime growthEngineRuntime;
    private final PlayerDataManager   playerDataManager;
    private final ItemMasterRegistry  itemMasters;
    private final ScoreboardService   scoreboardService;
    private final CombatStateService  combatStateService;
    @SuppressWarnings("unused")
    private final Plugin              plugin;

    public GrowthGuiListener(
            GrowthStateStore growthStateStore,
            GrowthEngineRuntime growthEngineRuntime,
            PlayerDataManager playerDataManager,
            ScoreboardService scoreboardService,
            ItemMasterRegistry itemMasters,
            CombatStateService combatStateService,
            Plugin plugin
    ) {
        this.growthStateStore    = growthStateStore;
        this.growthEngineRuntime = growthEngineRuntime;
        this.playerDataManager   = playerDataManager;
        this.scoreboardService   = scoreboardService;
        this.itemMasters         = itemMasters;
        this.combatStateService  = combatStateService;
        this.plugin              = plugin;
    }

    // ═══════════════════════════════════════════════════════════════
    // 이벤트 처리
    // ═══════════════════════════════════════════════════════════════

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
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 장비 허브
    // ═══════════════════════════════════════════════════════════════

    private void handleEquipmentHub(Player player, int slot) {
        switch (slot) {
            case 29 -> openGrowthEnhance(player);
            case 31 -> openGrowthPotential(player);
            case 33 -> player.sendMessage("§8전승 — 준비 중");
            case 45 -> MainHubGui.open(player);
            case 49 -> player.closeInventory();
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 강화 GUI
    // ═══════════════════════════════════════════════════════════════

    private void openGrowthEnhance(Player player) {
        selectedEnhanceId.remove(player.getUniqueId());
        PlayerGrowthState state = getState(player);

        Inventory gui = Bukkit.createInventory(null, 54, GuiTitles.GROWTH_ENHANCE);
        ItemStack pane = pane();
        for (int i = 0; i < 54; i++) gui.setItem(i, pane);

        fillEnhancementEquipSlots(gui, state);
        gui.setItem(ENH_SLOT_INFO, MainHubGui.icon(Material.BOOK, "§7강화 안내",
                List.of("§7──────────────",
                        "§7장착 슬롯 클릭 → 아이템 선택",
                        "§e강화 시도§7 버튼으로 강화",
                        "§7──────────────",
                        "§7골드: §e" + state.currency("gold"),
                        "§7강화석: §e" + state.currency("mat_stone_enhance"))));
        gui.setItem(ENH_SLOT_ACTION, MainHubGui.icon(Material.GRAY_STAINED_GLASS_PANE, "§8강화 시도",
                List.of("§7먼저 아이템을 선택하세요")));
        gui.setItem(ENH_SLOT_BACK,  MainHubGui.icon(Material.ARROW,   "§7뒤로", List.of("§7장비 관리")));
        gui.setItem(ENH_SLOT_CLOSE, MainHubGui.icon(Material.BARRIER, "§c닫기", List.of()));

        player.openInventory(gui);
    }

    private void handleEnhanceClick(Player player, int slot) {
        if (slot == ENH_SLOT_BACK)  { openEquipmentHub(player); return; }
        if (slot == ENH_SLOT_CLOSE) { player.closeInventory(); return; }

        EquipmentSlot equipSlot = ENH_GUI_TO_EQUIP.get(slot);
        if (equipSlot != null) {
            PlayerGrowthState state = getState(player);
            String instanceId = state.equippedItems().get(equipSlot);
            if (instanceId == null) { player.sendMessage("§7해당 슬롯에 장착된 아이템이 없습니다."); return; }
            selectedEnhanceId.put(player.getUniqueId(), instanceId);
            refreshEnhanceInfo(player.getOpenInventory().getTopInventory(), state, instanceId);
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

    private void fillEnhancementEquipSlots(Inventory inv, PlayerGrowthState state) {
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
                String name = itemDisplayName(item);
                String tier = masterFor(item).map(m -> " §7[" + m.tier() + "]").orElse("");
                inv.setItem(guiSlot, MainHubGui.icon(
                        EQUIP_MATERIAL.getOrDefault(es, Material.NETHER_STAR),
                        "§f" + name + tier,
                        List.of("§e+" + (item != null ? item.enhanceLevel() : 0) + "강",
                                potentialOneLiner(item),
                                "§7──────────────",
                                "§7클릭하여 선택")));
            }
        }
    }

    private void refreshEnhanceInfo(Inventory inv, PlayerGrowthState state, String instanceId) {
        PlayerEquipmentItem item = state.inventoryItem(instanceId).orElse(null);
        if (item == null) {
            inv.setItem(ENH_SLOT_INFO,   MainHubGui.icon(Material.BARRIER, "§c오류", List.of("§7아이템 없음")));
            inv.setItem(ENH_SLOT_ACTION, MainHubGui.icon(Material.GRAY_STAINED_GLASS_PANE, "§8강화 시도", List.of("§7오류")));
            return;
        }
        String itemName = itemDisplayName(item);
        int curLv = item.enhanceLevel();

        if (curLv >= EnhancementService.MAX_ENHANCE_LEVEL) {
            inv.setItem(ENH_SLOT_INFO, MainHubGui.icon(Material.PAPER, "§f" + itemName + " §e+" + curLv,
                    List.of("§7──────────────", "§a최대 강화 달성")));
            inv.setItem(ENH_SLOT_ACTION, MainHubGui.icon(Material.LIME_STAINED_GLASS_PANE, "§a최대 강화 달성",
                    List.of("§7+" + curLv + " — 더 이상 강화 불가")));
            return;
        }

        GrowthTier tier = masterFor(item).map(m -> GrowthTier.from(m.tier())).orElse(GrowthTier.T1);
        EnhancementRule rule = growthEngineRuntime.enhancementRuleRegistry().find(tier, curLv + 1).orElse(null);

        inv.setItem(ENH_SLOT_INFO, buildEnhInfoIcon(item, itemName, curLv, rule));

        if (rule == null) {
            inv.setItem(ENH_SLOT_ACTION, MainHubGui.icon(Material.GRAY_STAINED_GLASS_PANE, "§8강화 불가",
                    List.of("§7강화 규칙 없음")));
        } else {
            boolean okGold  = state.currency("gold") >= rule.goldCost();
            boolean okStone = state.currency("mat_stone_enhance") >= rule.stoneCost();
            if (okGold && okStone) {
                inv.setItem(ENH_SLOT_ACTION, MainHubGui.icon(Material.ANVIL, "§e§l강화 시도",
                        List.of("§7──────────────",
                                "§7" + itemName + " §e+" + curLv + " → +" + (curLv + 1),
                                "§7성공률: §e" + String.format("%.1f", rule.successRate()) + "%",
                                "§7비용: 골드 §e" + rule.goldCost() + " §7/ 강화석 §e" + rule.stoneCost(),
                                "§7──────────────",
                                "§a클릭하여 강화 시도")));
            } else {
                List<String> lacks = new ArrayList<>();
                if (!okGold)  lacks.add("§c골드 부족 (필요: " + rule.goldCost() + ")");
                if (!okStone) lacks.add("§c강화석 부족 (필요: " + rule.stoneCost() + ")");
                inv.setItem(ENH_SLOT_ACTION, MainHubGui.icon(Material.RED_STAINED_GLASS_PANE, "§c재화 부족", lacks));
            }
        }
    }

    private void attemptEnhancement(Player player, String instanceId) {
        PlayerGrowthState state = getState(player);
        var result = growthEngineRuntime.enhancementService().attempt(state, instanceId);
        if (result.isFailure()) {
            player.sendMessage("§c강화 실패: " + result.message());
            refreshEnhanceInfo(player.getOpenInventory().getTopInventory(), state, instanceId);
            return;
        }
        var r = result.value();
        if (r.success()) {
            player.sendMessage("§6§l[강화 성공!] §e" + itemDisplayNameById(r.itemId())
                    + " §a+" + r.finalLevel() + " 달성!" + (r.forcedByCeiling() ? " §7(천장 보정)" : ""));
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1.0f, 1.2f);
        } else {
            player.sendMessage("§7[강화] §e" + itemDisplayNameById(r.itemId()) + " §c+" + r.targetLevel() + " 실패."
                    + " §7(" + String.format("%.1f%%", r.successRate()) + ")");
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.7f, 0.8f);
        }
        scoreboardService.refresh(player);
        Inventory inv = player.getOpenInventory().getTopInventory();
        fillEnhancementEquipSlots(inv, state);
        refreshEnhanceInfo(inv, state, instanceId);
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
            if (pendingPotentialResult.containsKey(player.getUniqueId())) {
                player.sendMessage("§c먼저 현재 결과를 선택(확정/유지)하세요."); return;
            }
            if (combatStateService.isInCombat(player.getUniqueId())) {
                player.sendMessage("§c전투 중에는 잠재능력을 변경할 수 없습니다."); return;
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
            player.sendMessage("§7[잠재] §f" + itemDisplayNameById(instanceId) + " §7기존 옵션 유지.");
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            scoreboardService.refresh(player);
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
            player.sendMessage("§b[잠재] §f" + itemDisplayNameById(instanceId) + " §b새 옵션 확정!");
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.6f, 1.1f);
            scoreboardService.refresh(player);
            PlayerGrowthState state = getState(player);
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
        // 큐브 버튼 비활성화 (선택 전까지)
        inv.setItem(POT_SLOT_USE_CUBE, MainHubGui.icon(Material.GRAY_STAINED_GLASS_PANE, "§8큐브 사용",
                List.of("§7선택 확정 후 다시 사용 가능")));
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
                String name = itemDisplayName(item);
                List<String> lore = new ArrayList<>();
                lore.add(isSelected ? "§a▶ 선택됨" : "§7클릭하여 선택");
                lore.add("§e+" + (item != null ? item.enhanceLevel() : 0) + "강");
                lore.add(potentialOneLiner(item));
                inv.setItem(guiSlot, MainHubGui.icon(
                        EQUIP_MATERIAL.getOrDefault(es, Material.NETHER_STAR),
                        (isSelected ? "§a§l" : "§f") + name,
                        lore));
            }
        }
    }

    private void refreshPotentialCurrentPanel(Inventory inv, PlayerGrowthState state, String instanceId) {
        PlayerEquipmentItem item = state.inventoryItem(instanceId).orElse(null);
        PotentialProfile profile = item != null ? item.potentialProfile() : null;
        renderPotentialPanel(inv, profile, POT_SLOT_CUR_GRADE, POT_SLOT_CUR_LINE1,
                POT_SLOT_CUR_LINE2, POT_SLOT_CUR_LINE3, "§7");
        inv.setItem(POT_SLOT_CUR_KEEP, pane());  // 선택 대기 중 아닐 때는 숨김

        // 미리보기 슬롯 (선택된 장비 정보)
        String itemName = item != null ? itemDisplayName(item) : instanceId;
        inv.setItem(POT_SLOT_PREVIEW, MainHubGui.icon(
                EQUIP_MATERIAL.getOrDefault(findEquipSlot(state, instanceId), Material.NETHER_STAR),
                "§f" + itemName + (item != null ? " §e+" + item.enhanceLevel() + "강" : ""),
                List.of("§7──────────────",
                        "§7잠재 등급: " + (profile != null ? gradeColor(profile.grade()) + profile.grade().name() : "§8없음"))));
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
                colorPrefix + "등급: " + gradeColor(profile.grade()) + profile.grade().name(), List.of()));
        List<PotentialLine> lines = profile.lines();
        inv.setItem(line1Slot, lines.size() > 0 ? potentialLineIcon(lines.get(0), colorPrefix) : pane());
        inv.setItem(line2Slot, lines.size() > 1 ? potentialLineIcon(lines.get(1), colorPrefix) : pane());
        inv.setItem(line3Slot, lines.size() > 2 ? potentialLineIcon(lines.get(2), colorPrefix) : pane());
    }

    private ItemStack potentialLineIcon(PotentialLine line, String colorPrefix) {
        return MainHubGui.icon(Material.PAPER,
                colorPrefix + line.optionCode() + " §e+" + String.format("%.2f", line.value()),
                List.of("§7라인 " + line.lineNo() + " / " + gradeColor(line.grade()) + line.grade().name()));
    }

    // ═══════════════════════════════════════════════════════════════
    // 장비 허브 열기 (내부)
    // ═══════════════════════════════════════════════════════════════

    private void openEquipmentHub(Player player) {
        Inventory gui = Bukkit.createInventory(null, 54, GuiTitles.EQUIPMENT_HUB);
        ItemStack pane = pane();
        for (int i = 0; i < 54; i++) gui.setItem(i, pane);
        gui.setItem(29, MainHubGui.icon(Material.ANVIL,              "§f강화",      List.of("§7아이템 강화 / 강화석 소모")));
        gui.setItem(31, MainHubGui.icon(Material.NETHER_STAR,        "§f잠재능력",  List.of("§7큐브 소비 → 잠재 재롤")));
        gui.setItem(33, MainHubGui.icon(Material.EXPERIENCE_BOTTLE,  "§7전승",      List.of("§8준비 중")));
        gui.setItem(45, MainHubGui.icon(Material.ARROW,              "§7뒤로",      List.of("§7메인 메뉴")));
        gui.setItem(49, MainHubGui.icon(Material.BARRIER,            "§c닫기",      List.of()));
        player.openInventory(gui);
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
        return gradeColor(p.grade()) + p.grade().name() + " §7(" + p.lines().size() + "라인)";
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

    private String slotLabel(EquipmentSlot slot) {
        return switch (slot) {
            case WEAPON      -> "무기";
            case HELMET      -> "투구";
            case CHESTPLATE  -> "상의";
            case LEGGINGS    -> "하의";
            case BOOTS       -> "신발";
            default          -> slot.name();
        };
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
}
