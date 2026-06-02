package com.poro.rpg.init;

import com.poro.rpg.combat.weapon.WeaponType;
import com.poro.rpg.combat.weapon.WeaponTypeResolver;
import com.poro.rpg.growth.GrowthStateStore;
import com.poro.rpg.growth.engine.EquipmentSlot;
import com.poro.rpg.growth.engine.ItemGrade;
import com.poro.rpg.growth.engine.PlayerEquipmentItem;
import com.poro.rpg.growth.engine.PlayerGrowthState;
import com.poro.rpg.gui.GuiTitles;
import com.poro.rpg.gui.WeaponGui;
import com.poro.rpg.gui.WeaponItemFactory;
import com.poro.rpg.persistence.PlayerPersistenceService;
import com.poro.rpg.storage.PlayerDataManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;

public final class ClassInitService {

    private final Plugin plugin;
    private final PlayerDataManager playerDataManager;
    private final GrowthStateStore growthStateStore;
    private final PlayerPersistenceService playerPersistenceService;
    private final Logger logger;

    public ClassInitService(Plugin plugin,
                            PlayerDataManager playerDataManager,
                            GrowthStateStore growthStateStore,
                            PlayerPersistenceService playerPersistenceService) {
        this.plugin = plugin;
        this.playerDataManager = playerDataManager;
        this.growthStateStore = growthStateStore;
        this.playerPersistenceService = playerPersistenceService;
        this.logger = plugin.getLogger();
    }

    // ─── 첫 접속 GUI 오픈 ─────────────────────────────────────

    public void openSelectionGuiIfNeeded(Player player) {
        if (playerDataManager.hasSelectedWeapon(player)) return;
        // 다음 틱으로 밀어야 PlayerJoinEvent 중 openInventory가 동작함
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) openSelectionGui(player);
        }, 1L);
    }

    private void openSelectionGui(Player player) {
        // 스펙 36칸 공용 레이아웃(WeaponGui) — 무기 변경 GUI와 같은 슬롯 배치를 재사용 (DL-104).
        Inventory gui = Bukkit.createInventory(null, WeaponGui.SIZE, GuiTitles.WEAPON_SELECTION);
        ItemStack pane = glassPane();
        for (int i = 0; i < WeaponGui.SIZE; i++) gui.setItem(i, pane);
        for (int i = 0; i < WeaponGui.ORDER.length; i++) {
            WeaponType wt = WeaponGui.ORDER[i];
            gui.setItem(WeaponGui.SLOTS[i], weaponIcon(wt, WeaponGui.displayName(wt)));
        }
        // 첫 진입(튜토리얼) 컨텍스트 — 뒤로가기 버튼 없음.
        player.openInventory(gui);
    }

    // ─── 초기 5슬롯 장비 지급 ─────────────────────────────────

    public void grantStarterEquipment(Player player, WeaponType weaponType) {
        String classId = weaponType.name().toLowerCase(Locale.ROOT);
        PlayerGrowthState state = growthStateStore.getOrCreate(player.getUniqueId(), classId);
        // 직업 변경(/직업) 시 기존 state의 classId를 갱신 — 각인/스킬이 새 직업(무기) 기준으로 동작.
        // (getOrCreate는 기존 state가 있으면 classId 인자를 무시하므로 명시 갱신 필요.)
        // 무기별 독립 각인(DL-110)이므로 이전 각인 해제 불필요 — 각 무기가 자기 각인을 보유.
        state.setClassId(classId);
        // 무기는 무기별 독립 인스턴스(weapon_<타입>) — 교체 시 각 무기의 강화/등급/잠재를 따로 가져간다 (DL-104).
        addAndEquipIfMissing(state, EquipmentSlot.WEAPON,     WeaponGui.weaponInstanceId(weaponType), WeaponGui.starterItemId(weaponType));
        addAndEquipIfMissing(state, EquipmentSlot.HELMET,     "starter_helmet",      "t1_helmet_starter");
        addAndEquipIfMissing(state, EquipmentSlot.CHESTPLATE, "starter_chestplate",  "t1_chestplate_starter");
        addAndEquipIfMissing(state, EquipmentSlot.LEGGINGS,   "starter_leggings",    "t1_leggings_starter");
        addAndEquipIfMissing(state, EquipmentSlot.BOOTS,      "starter_boots",       "t1_boots_starter");
        // 손에 든 무기 — 무기 변경 GUI와 동일한 lore 형식(강화/등급/잠재/세부스탯/각인).
        player.getInventory().setItem(0, WeaponItemFactory.build(state, weaponType));
        grantIslandTools(player);
        playerPersistenceService.save(player.getUniqueId());
        logger.info("[Class] " + player.getUniqueId() + " selected " + weaponType);
    }

    // ─── 영지 도구 지급 (네더라이트 4종, Efficiency V + Fortune III, Unbreakable) ─
    public void grantIslandTools(Player player) {
        player.getInventory().setItem(1, islandTool(Material.NETHERITE_PICKAXE));
        player.getInventory().setItem(2, islandTool(Material.NETHERITE_SHOVEL));
        player.getInventory().setItem(3, islandTool(Material.NETHERITE_HOE));
        player.getInventory().setItem(4, islandTool(Material.NETHERITE_AXE));
        player.getInventory().setItem(MENU_ITEM_SLOT, createMenuItem());
    }

    /** 메뉴 아이템 슬롯 — 핫바 9번(index 8). 항상 이 슬롯에 고정한다. */
    public static final int MENU_ITEM_SLOT = 8;
    /** 메뉴 아이템 식별 PDC 태그 — 재질과 무관하게 판별(나침반 등과 혼동 방지). */
    public static final NamespacedKey MENU_ITEM_KEY = NamespacedKey.fromString("poro_rpg:menu_item");

    /** 재접속 시 slot 8의 메뉴 아이템이 없으면 복구한다. */
    public void ensureMenuCompass(Player player) {
        if (isMenuItem(player.getInventory().getItem(MENU_ITEM_SLOT))) return;
        player.getInventory().setItem(MENU_ITEM_SLOT, createMenuItem());
    }

    /** 메뉴 아이템 생성 — 나침반과 구분되는 별도 재질(NETHER_STAR) + PDC 태그. */
    public static ItemStack createMenuItem() {
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("§e메뉴"));
        meta.getPersistentDataContainer().set(MENU_ITEM_KEY, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    /** PDC 태그 기반 메뉴 아이템 판별(재질 무관). */
    public static boolean isMenuItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer()
                .has(MENU_ITEM_KEY, PersistentDataType.BYTE);
    }

    private ItemStack islandTool(Material material) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setUnbreakable(true);
        meta.addEnchant(Enchantment.EFFICIENCY, 5, true);
        meta.addEnchant(Enchantment.FORTUNE, 3, true);
        item.setItemMeta(meta);
        return item;
    }

    private void addAndEquipIfMissing(PlayerGrowthState state, EquipmentSlot slot,
                                      String instanceId, String itemId) {
        if (state.inventoryItem(instanceId).isEmpty()) {
            state.addInventoryItem(PlayerEquipmentItem.restore(
                    instanceId, itemId, 0, ItemGrade.COMMON, null, List.of()));
        }
        if (state.equippedItemInstanceId(slot).isEmpty()) {
            state.equipItem(slot, instanceId);
        }
    }

    // ─── GUI 아이콘 빌더 ───────────────────────────────────────

    private ItemStack weaponIcon(WeaponType type, String displayName) {
        ItemStack item = new ItemStack(materialFor(type));
        ItemMeta meta = item.getItemMeta();
        meta.displayName(legacy("§e" + displayName));
        meta.lore(List.of(legacy("§7클릭하여 이 무기 클래스를 선택")));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack glassPane() {
        ItemStack pane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = pane.getItemMeta();
        meta.displayName(Component.text(" "));
        pane.setItemMeta(meta);
        return pane;
    }

    /** §색상코드를 해석하고, 아이템 이름/lore의 기본 이탤릭을 끈다. */
    private Component legacy(String text) {
        return LegacyComponentSerializer.legacySection().deserialize(text)
                .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false);
    }

    private Material materialFor(WeaponType type) {
        return switch (type) {
            case SWORD        -> Material.NETHERITE_SWORD;
            case SPEAR        -> Material.TRIDENT;
            case AXE          -> Material.NETHERITE_AXE;
            case CROSSBOW     -> Material.CROSSBOW;
            case SCYTHE       -> Material.NETHERITE_HOE;
            case STAFF        -> Material.BLAZE_ROD;
            case NONE         -> Material.STICK;
        };
    }

}
