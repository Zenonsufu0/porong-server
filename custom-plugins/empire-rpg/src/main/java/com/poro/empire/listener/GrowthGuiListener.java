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

    // ─── GUI 슬롯 상수 ────────────────────────────────────────────
    // row0: 0-8 pane, row1: 9-17, row2: 18-26, row3: 27-35, row4: 36-44, row5: 45-53
    private static final int SLOT_WEAPON      = 10;
    private static final int SLOT_HELMET      = 12;
    private static final int SLOT_CHESTPLATE  = 14;
    private static final int SLOT_LEGGINGS    = 16;
    private static final int SLOT_BOOTS       = 28;
    private static final int SLOT_ACCESSORY_1 = 30;
    private static final int SLOT_ACCESSORY_2 = 32;
    private static final int SLOT_ACCESSORY_3 = 34;
    private static final int SLOT_INFO        = 22;
    private static final int SLOT_ACTION      = 40;
    private static final int SLOT_BACK        = 45;
    private static final int SLOT_CLOSE       = 53;

    private static final Map<Integer, EquipmentSlot> GUI_SLOT_TO_EQUIP = Map.of(
        SLOT_WEAPON,      EquipmentSlot.WEAPON,
        SLOT_HELMET,      EquipmentSlot.HELMET,
        SLOT_CHESTPLATE,  EquipmentSlot.CHESTPLATE,
        SLOT_LEGGINGS,    EquipmentSlot.LEGGINGS,
        SLOT_BOOTS,       EquipmentSlot.BOOTS,
        SLOT_ACCESSORY_1, EquipmentSlot.ACCESSORY_1,
        SLOT_ACCESSORY_2, EquipmentSlot.ACCESSORY_2,
        SLOT_ACCESSORY_3, EquipmentSlot.ACCESSORY_3
    );

    private static final Map<EquipmentSlot, Integer> EQUIP_TO_GUI_SLOT = Map.of(
        EquipmentSlot.WEAPON,      SLOT_WEAPON,
        EquipmentSlot.HELMET,      SLOT_HELMET,
        EquipmentSlot.CHESTPLATE,  SLOT_CHESTPLATE,
        EquipmentSlot.LEGGINGS,    SLOT_LEGGINGS,
        EquipmentSlot.BOOTS,       SLOT_BOOTS,
        EquipmentSlot.ACCESSORY_1, SLOT_ACCESSORY_1,
        EquipmentSlot.ACCESSORY_2, SLOT_ACCESSORY_2,
        EquipmentSlot.ACCESSORY_3, SLOT_ACCESSORY_3
    );

    private static final Map<EquipmentSlot, Material> EQUIP_MATERIAL = Map.of(
        EquipmentSlot.WEAPON,      Material.DIAMOND_SWORD,
        EquipmentSlot.HELMET,      Material.DIAMOND_HELMET,
        EquipmentSlot.CHESTPLATE,  Material.DIAMOND_CHESTPLATE,
        EquipmentSlot.LEGGINGS,    Material.DIAMOND_LEGGINGS,
        EquipmentSlot.BOOTS,       Material.DIAMOND_BOOTS,
        EquipmentSlot.ACCESSORY_1, Material.NETHER_STAR,
        EquipmentSlot.ACCESSORY_2, Material.NETHER_STAR,
        EquipmentSlot.ACCESSORY_3, Material.NETHER_STAR
    );

    // ─── 선택 상태 (per-player) ───────────────────────────────────
    private final Map<UUID, String> selectedEnhanceId    = new ConcurrentHashMap<>();
    private final Map<UUID, String> selectedPotentialId  = new ConcurrentHashMap<>();

    // ─── 의존성 ──────────────────────────────────────────────────
    private final GrowthStateStore       growthStateStore;
    private final GrowthEngineRuntime    growthEngineRuntime;
    private final PlayerDataManager      playerDataManager;
    private final ItemMasterRegistry     itemMasters;
    private final ScoreboardService      scoreboardService;
    private final CombatStateService     combatStateService;
    private final Plugin                 plugin;

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

    // ─── 이벤트 처리 ─────────────────────────────────────────────

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
            handleEnhanceClick(player, event.getRawSlot());
            return;
        }
        if (GuiTitles.GROWTH_POTENTIAL.equals(event.getView().title())) {
            event.setCancelled(true);
            handlePotentialClick(player, event.getRawSlot());
        }
    }

    // ─── 장비 허브 ───────────────────────────────────────────────

    private void handleEquipmentHub(Player player, int slot) {
        switch (slot) {
            case 29 -> openGrowthEnhance(player);
            case 31 -> openGrowthPotential(player);
            case 33 -> player.sendMessage("§8전승 — 준비 중");
            case 45 -> MainHubGui.open(player);
            case 49 -> player.closeInventory();
        }
    }

    // ─── 강화 GUI ────────────────────────────────────────────────

    private void openGrowthEnhance(Player player) {
        selectedEnhanceId.remove(player.getUniqueId());
        PlayerGrowthState state = getState(player);
        Inventory gui = buildEquipmentGrid(GuiTitles.GROWTH_ENHANCE, state);

        // 아이템 선택 전 정보 슬롯
        gui.setItem(SLOT_INFO, MainHubGui.icon(Material.BOOK, "§7강화 안내",
                List.of("§7──────────────",
                        "§7장착 슬롯을 클릭하여 아이템을 선택",
                        "§7선택 후 §e강화 시도§7 버튼 클릭",
                        "§7──────────────",
                        "§7보유 골드: §e" + state.currency("gold"),
                        "§7보유 강화석: §e" + state.currency("mat_stone_enhance"))));
        gui.setItem(SLOT_ACTION, MainHubGui.icon(Material.GRAY_STAINED_GLASS_PANE, "§8강화 시도",
                List.of("§7먼저 장착 아이템을 선택하세요")));

        player.openInventory(gui);
    }

    private void handleEnhanceClick(Player player, int slot) {
        if (slot == SLOT_BACK)  { openEquipmentHub(player); return; }
        if (slot == SLOT_CLOSE) { player.closeInventory(); return; }

        // 장비 슬롯 클릭 → 아이템 선택
        EquipmentSlot equipSlot = GUI_SLOT_TO_EQUIP.get(slot);
        if (equipSlot != null) {
            PlayerGrowthState state = getState(player);
            String instanceId = state.equippedItems().get(equipSlot);
            if (instanceId == null) {
                player.sendMessage("§7해당 슬롯에 장착된 아이템이 없습니다.");
                return;
            }
            selectedEnhanceId.put(player.getUniqueId(), instanceId);
            refreshEnhanceGui(player, state, instanceId);
            return;
        }

        // 강화 시도 버튼 클릭
        if (slot == SLOT_ACTION) {
            String instanceId = selectedEnhanceId.get(player.getUniqueId());
            if (instanceId == null) {
                player.sendMessage("§c먼저 강화할 아이템을 선택하세요.");
                return;
            }
            if (combatStateService.isInCombat(player.getUniqueId())) {
                player.sendMessage("§c전투 중에는 강화할 수 없습니다.");
                return;
            }
            attemptEnhancement(player, instanceId);
        }
    }

    private void refreshEnhanceGui(Player player, PlayerGrowthState state, String instanceId) {
        PlayerEquipmentItem item = state.inventoryItem(instanceId).orElse(null);
        Inventory inv = player.getOpenInventory().getTopInventory();
        if (item == null) {
            inv.setItem(SLOT_INFO, MainHubGui.icon(Material.BARRIER, "§c오류", List.of("§7아이템을 찾을 수 없습니다.")));
            inv.setItem(SLOT_ACTION, MainHubGui.icon(Material.GRAY_STAINED_GLASS_PANE, "§8강화 시도", List.of("§7아이템 오류")));
            return;
        }
        ItemMaster master = itemMasters.find(item.itemId()).orElse(null);
        String itemName = master != null ? master.itemName() : item.itemId();
        int curLv = item.enhanceLevel();

        if (curLv >= EnhancementService.MAX_ENHANCE_LEVEL) {
            inv.setItem(SLOT_INFO, buildItemInfoIcon(item, itemName, curLv, null));
            inv.setItem(SLOT_ACTION, MainHubGui.icon(Material.LIME_STAINED_GLASS_PANE, "§a최대 강화 달성", List.of("§7+" + curLv + " — 더 이상 강화 불가")));
            return;
        }

        GrowthTier tier = master != null ? GrowthTier.from(master.tier()) : GrowthTier.T1;
        EnhancementRule rule = growthEngineRuntime.enhancementRuleRegistry().find(tier, curLv + 1).orElse(null);
        inv.setItem(SLOT_INFO, buildItemInfoIcon(item, itemName, curLv, rule));

        if (rule == null) {
            inv.setItem(SLOT_ACTION, MainHubGui.icon(Material.GRAY_STAINED_GLASS_PANE, "§8강화 불가", List.of("§7강화 규칙 없음")));
        } else {
            boolean hasGold  = state.currency("gold") >= rule.goldCost();
            boolean hasStone = state.currency("mat_stone_enhance") >= rule.stoneCost();
            if (hasGold && hasStone) {
                inv.setItem(SLOT_ACTION, MainHubGui.icon(Material.ANVIL, "§e§l강화 시도",
                        List.of("§7──────────────",
                                "§7대상: §f" + itemName + " §e+" + curLv + " → +" + (curLv + 1),
                                "§7성공률: §e" + String.format("%.1f", rule.successRate()) + "%",
                                "§7비용: §e골드 " + rule.goldCost() + " + 강화석 " + rule.stoneCost(),
                                "§7──────────────",
                                "§a클릭하여 강화 시도")));
            } else {
                List<String> lacks = new ArrayList<>();
                if (!hasGold)  lacks.add("§c골드 부족 (필요: " + rule.goldCost() + ")");
                if (!hasStone) lacks.add("§c강화석 부족 (필요: " + rule.stoneCost() + ")");
                inv.setItem(SLOT_ACTION, MainHubGui.icon(Material.RED_STAINED_GLASS_PANE, "§c재화 부족", lacks));
            }
        }
    }

    private void attemptEnhancement(Player player, String instanceId) {
        PlayerGrowthState state = getState(player);
        var result = growthEngineRuntime.enhancementService().attempt(state, instanceId);
        if (result.isFailure()) {
            player.sendMessage("§c강화 실패: " + result.message());
            refreshEnhanceGui(player, state, instanceId);
            return;
        }
        var r = result.value();
        if (r.success()) {
            player.sendMessage("§6§l[강화 성공!] §e" + itemDisplayName(r.itemId()) + " §a+" + r.finalLevel() + " 달성!"
                    + (r.forcedByCeiling() ? " §7(천장 보정)" : ""));
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1.0f, 1.2f);
        } else {
            player.sendMessage("§7[강화] §e" + itemDisplayName(r.itemId()) + " §c+" + r.targetLevel() + " 강화 실패."
                    + " §7(" + String.format("%.1f%%", r.successRate()) + ", 현재 §e+" + r.finalLevel() + "§7)");
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.7f, 0.8f);
        }
        scoreboardService.refresh(player);
        // GUI 갱신: 장비 슬롯 아이템 + 정보 슬롯 업데이트
        Inventory inv = player.getOpenInventory().getTopInventory();
        refreshEquipmentSlots(inv, state);
        refreshEnhanceGui(player, state, instanceId);
    }

    // ─── 잠재능력 GUI ─────────────────────────────────────────────

    private void openGrowthPotential(Player player) {
        selectedPotentialId.remove(player.getUniqueId());
        PlayerGrowthState state = getState(player);
        Inventory gui = buildEquipmentGrid(GuiTitles.GROWTH_POTENTIAL, state);

        gui.setItem(SLOT_INFO, MainHubGui.icon(Material.BOOK, "§7잠재능력 안내",
                List.of("§7──────────────",
                        "§7장착 슬롯을 클릭하여 아이템 선택",
                        "§7큐브 1개 + 골드 500으로 재롤",
                        "§7──────────────",
                        "§7보유 큐브: §e" + state.currency("mat_cube"),
                        "§7보유 골드: §e" + state.currency("gold"))));
        gui.setItem(SLOT_ACTION, MainHubGui.icon(Material.GRAY_STAINED_GLASS_PANE, "§8큐브 사용",
                List.of("§7먼저 장착 아이템을 선택하세요")));

        player.openInventory(gui);
    }

    private void handlePotentialClick(Player player, int slot) {
        if (slot == SLOT_BACK)  { openEquipmentHub(player); return; }
        if (slot == SLOT_CLOSE) { player.closeInventory(); return; }

        EquipmentSlot equipSlot = GUI_SLOT_TO_EQUIP.get(slot);
        if (equipSlot != null) {
            PlayerGrowthState state = getState(player);
            String instanceId = state.equippedItems().get(equipSlot);
            if (instanceId == null) {
                player.sendMessage("§7해당 슬롯에 장착된 아이템이 없습니다.");
                return;
            }
            selectedPotentialId.put(player.getUniqueId(), instanceId);
            refreshPotentialGui(player, state, instanceId);
            return;
        }

        if (slot == SLOT_ACTION) {
            String instanceId = selectedPotentialId.get(player.getUniqueId());
            if (instanceId == null) {
                player.sendMessage("§c먼저 잠재능력을 변경할 아이템을 선택하세요.");
                return;
            }
            if (combatStateService.isInCombat(player.getUniqueId())) {
                player.sendMessage("§c전투 중에는 잠재능력을 변경할 수 없습니다.");
                return;
            }
            attemptPotential(player, instanceId);
        }
    }

    private void refreshPotentialGui(Player player, PlayerGrowthState state, String instanceId) {
        PlayerEquipmentItem item = state.inventoryItem(instanceId).orElse(null);
        Inventory inv = player.getOpenInventory().getTopInventory();
        if (item == null) {
            inv.setItem(SLOT_INFO, MainHubGui.icon(Material.BARRIER, "§c오류", List.of("§7아이템을 찾을 수 없습니다.")));
            inv.setItem(SLOT_ACTION, MainHubGui.icon(Material.GRAY_STAINED_GLASS_PANE, "§8큐브 사용", List.of("§7아이템 오류")));
            return;
        }
        ItemMaster master = itemMasters.find(item.itemId()).orElse(null);
        String itemName = master != null ? master.itemName() : item.itemId();

        inv.setItem(SLOT_INFO, buildPotentialInfoIcon(item, itemName));

        boolean hasCube = state.currency(PotentialService.MATERIAL_CUBE) >= 1;
        boolean hasGold = state.currency("gold") >= 500L;
        if (hasCube && hasGold) {
            String gradeLine = item.potentialProfile() != null
                    ? "§7현재 등급: " + gradeColor(item.potentialProfile().grade()) + item.potentialProfile().grade().name()
                    : "§7잠재능력 없음 (큐브 사용 시 초기화)";
            inv.setItem(SLOT_ACTION, MainHubGui.icon(Material.NETHER_STAR, "§b§l큐브 사용",
                    List.of("§7──────────────",
                            "§7대상: §f" + itemName,
                            gradeLine,
                            "§7비용: §e큐브 1 + 골드 500",
                            "§7──────────────",
                            "§b클릭하여 잠재능력 재롤")));
        } else {
            List<String> lacks = new ArrayList<>();
            if (!hasCube) lacks.add("§c큐브 부족 (보유: " + state.currency(PotentialService.MATERIAL_CUBE) + ")");
            if (!hasGold) lacks.add("§c골드 부족 (필요: 500)");
            inv.setItem(SLOT_ACTION, MainHubGui.icon(Material.RED_STAINED_GLASS_PANE, "§c재화 부족", lacks));
        }
    }

    private void attemptPotential(Player player, String instanceId) {
        PlayerGrowthState state = getState(player);
        PlayerEquipmentItem item = state.inventoryItem(instanceId).orElse(null);
        String itemName = item != null ? itemDisplayName(item.itemId()) : instanceId;

        var result = growthEngineRuntime.potentialService().useCube(state, instanceId);
        if (result.isFailure()) {
            player.sendMessage("§c큐브 사용 실패: " + result.message());
            refreshPotentialGui(player, state, instanceId);
            return;
        }

        var r = result.value();
        PotentialProfile selected = r.selectedAfter();
        List<String> lines = buildPotentialResultLines(r.before(), selected, r.success());
        player.sendMessage("§b[잠재능력] §f" + itemName + " §7재롤 완료.");
        lines.forEach(player::sendMessage);
        if (r.success()) {
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.2f);
        } else {
            player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 0.6f, 1.0f);
        }
        scoreboardService.refresh(player);
        Inventory inv = player.getOpenInventory().getTopInventory();
        refreshEquipmentSlots(inv, state);
        refreshPotentialGui(player, state, instanceId);
    }

    // ─── 공통 GUI 빌더 ───────────────────────────────────────────

    private Inventory buildEquipmentGrid(net.kyori.adventure.text.Component title, PlayerGrowthState state) {
        Inventory gui = Bukkit.createInventory(null, 54, title);
        ItemStack pane = MainHubGui.icon(Material.BLACK_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < 54; i++) gui.setItem(i, pane);

        gui.setItem(SLOT_BACK,  MainHubGui.icon(Material.ARROW,   "§7뒤로",  List.of("§7장비 관리")));
        gui.setItem(SLOT_CLOSE, MainHubGui.icon(Material.BARRIER, "§c닫기",  List.of()));

        refreshEquipmentSlots(gui, state);
        return gui;
    }

    private void refreshEquipmentSlots(Inventory inv, PlayerGrowthState state) {
        Map<EquipmentSlot, String> equipped = state.equippedItems();
        for (Map.Entry<EquipmentSlot, Integer> entry : EQUIP_TO_GUI_SLOT.entrySet()) {
            EquipmentSlot es = entry.getKey();
            int guiSlot = entry.getValue();
            String instanceId = equipped.get(es);
            if (instanceId == null) {
                inv.setItem(guiSlot, MainHubGui.icon(
                        EQUIP_MATERIAL.getOrDefault(es, Material.GRAY_STAINED_GLASS_PANE),
                        "§8" + slotLabel(es) + " §7(미장착)", List.of()));
            } else {
                PlayerEquipmentItem item = state.inventoryItem(instanceId).orElse(null);
                if (item == null) {
                    inv.setItem(guiSlot, MainHubGui.icon(Material.BARRIER, "§c오류", List.of()));
                } else {
                    ItemMaster master = itemMasters.find(item.itemId()).orElse(null);
                    String name = master != null ? master.itemName() : item.itemId();
                    String tier  = master != null ? " §7[" + master.tier() + "]" : "";
                    inv.setItem(guiSlot, MainHubGui.icon(
                            EQUIP_MATERIAL.getOrDefault(es, Material.NETHER_STAR),
                            "§f" + name + tier,
                            List.of("§e+" + item.enhanceLevel() + "강",
                                    potentialOneLiner(item.potentialProfile()),
                                    "§7──────────────",
                                    "§7클릭하여 선택")));
                }
            }
        }
    }

    private ItemStack buildItemInfoIcon(PlayerEquipmentItem item, String itemName, int curLv, EnhancementRule rule) {
        List<String> lore = new ArrayList<>();
        lore.add("§7──────────────");
        lore.add("§7아이템: §f" + itemName);
        lore.add("§7현재 강화: §e+" + curLv);
        if (rule != null) {
            lore.add("§7목표 강화: §e+" + (curLv + 1));
            lore.add("§7성공률: §e" + String.format("%.1f", rule.successRate()) + "%");
            lore.add("§7비용: 골드 §e" + rule.goldCost() + " §7/ 강화석 §e" + rule.stoneCost());
        }
        lore.add("§7──────────────");
        lore.add("§7잠재: " + potentialOneLiner(item.potentialProfile()));
        return MainHubGui.icon(Material.PAPER, "§f" + itemName + " §e+" + curLv, lore);
    }

    private ItemStack buildPotentialInfoIcon(PlayerEquipmentItem item, String itemName) {
        List<String> lore = new ArrayList<>();
        lore.add("§7──────────────");
        lore.add("§7아이템: §f" + itemName + " §e+" + item.enhanceLevel() + "강");
        PotentialProfile profile = item.potentialProfile();
        if (profile == null) {
            lore.add("§7잠재능력: §8없음");
        } else {
            lore.add("§7등급: " + gradeColor(profile.grade()) + profile.grade().name());
            for (PotentialLine line : profile.lines()) {
                lore.add("§7  " + line.lineNo() + ". §f" + line.optionCode() + " §e+" + String.format("%.2f", line.value()));
            }
        }
        lore.add("§7──────────────");
        return MainHubGui.icon(Material.PAPER, "§f" + itemName, lore);
    }

    // ─── 내비게이션 ──────────────────────────────────────────────

    private void openEquipmentHub(Player player) {
        PlayerGrowthState state = getState(player);
        WeaponType wt = playerDataManager.getWeaponType(player);
        Inventory gui = Bukkit.createInventory(null, 54, GuiTitles.EQUIPMENT_HUB);
        ItemStack pane = MainHubGui.icon(Material.BLACK_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < 54; i++) gui.setItem(i, pane);

        gui.setItem(29, MainHubGui.icon(Material.ANVIL,       "§f강화",      List.of("§7아이템 강화 / 강화석 소모")));
        gui.setItem(31, MainHubGui.icon(Material.NETHER_STAR, "§f잠재능력",  List.of("§7큐브 소비 → 잠재 재롤")));
        gui.setItem(33, MainHubGui.icon(Material.EXPERIENCE_BOTTLE, "§7전승", List.of("§8준비 중")));
        gui.setItem(45, MainHubGui.icon(Material.ARROW,   "§7뒤로", List.of("§7메인 메뉴")));
        gui.setItem(49, MainHubGui.icon(Material.BARRIER, "§c닫기", List.of()));
        player.openInventory(gui);
    }

    // ─── 유틸 ────────────────────────────────────────────────────

    private PlayerGrowthState getState(Player player) {
        WeaponType wt = playerDataManager.getWeaponType(player.getUniqueId());
        return growthStateStore.getOrCreate(player.getUniqueId(), wt.name().toLowerCase(Locale.ROOT));
    }

    private String itemDisplayName(String itemId) {
        return itemMasters.find(itemId)
                .map(ItemMaster::itemName)
                .orElse(itemId);
    }

    private String potentialOneLiner(PotentialProfile profile) {
        if (profile == null) return "§8잠재: 없음";
        return gradeColor(profile.grade()) + profile.grade().name()
                + " §7(" + profile.lines().size() + "라인)";
    }

    private List<String> buildPotentialResultLines(PotentialProfile before, PotentialProfile after, boolean gradeUp) {
        List<String> lines = new ArrayList<>();
        if (gradeUp) {
            String prev = before != null ? before.grade().name() : "없음";
            lines.add("§6§l[등급 상승!] §e" + prev + " → " + gradeColor(after.grade()) + after.grade().name());
        }
        for (PotentialLine line : after.lines()) {
            lines.add("§7  " + line.lineNo() + ". §f" + line.optionCode() + " §e+" + String.format("%.2f", line.value()));
        }
        return lines;
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
            case CHESTPLATE  -> "갑옷";
            case LEGGINGS    -> "각반";
            case BOOTS       -> "신발";
            case ACCESSORY_1 -> "장신구1";
            case ACCESSORY_2 -> "장신구2";
            case ACCESSORY_3 -> "장신구3";
        };
    }
}
