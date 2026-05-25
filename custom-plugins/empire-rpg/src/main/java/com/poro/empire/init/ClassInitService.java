package com.poro.empire.init;

import com.poro.empire.combat.weapon.WeaponType;
import com.poro.empire.combat.weapon.WeaponTypeResolver;
import com.poro.empire.growth.GrowthStateStore;
import com.poro.empire.growth.engine.EquipmentSlot;
import com.poro.empire.growth.engine.ItemGrade;
import com.poro.empire.growth.engine.PlayerEquipmentItem;
import com.poro.empire.growth.engine.PlayerGrowthState;
import com.poro.empire.gui.GuiTitles;
import com.poro.empire.persistence.PlayerPersistenceService;
import com.poro.empire.storage.PlayerDataManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
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
        Inventory gui = Bukkit.createInventory(null, 27, GuiTitles.WEAPON_SELECTION);
        ItemStack pane = glassPane();
        for (int i = 0; i < 27; i++) gui.setItem(i, pane);
        gui.setItem(10, weaponIcon(WeaponType.SWORD,    "검"));
        gui.setItem(11, weaponIcon(WeaponType.AXE,      "도끼"));
        gui.setItem(12, weaponIcon(WeaponType.SPEAR,    "창"));
        gui.setItem(13, weaponIcon(WeaponType.CROSSBOW, "석궁"));
        gui.setItem(14, weaponIcon(WeaponType.SCYTHE,   "낫"));
        gui.setItem(15, weaponIcon(WeaponType.STAFF,    "스태프"));
        player.openInventory(gui);
    }

    // ─── 초기 5슬롯 장비 지급 ─────────────────────────────────

    public void grantStarterEquipment(Player player, WeaponType weaponType) {
        String classId = weaponType.name().toLowerCase(Locale.ROOT);
        PlayerGrowthState state = growthStateStore.getOrCreate(player.getUniqueId(), classId);
        addAndEquipIfMissing(state, EquipmentSlot.WEAPON,     "starter_" + classId, "equip_" + classId);
        addAndEquipIfMissing(state, EquipmentSlot.HELMET,     "starter_helmet",     "equip_helmet");
        addAndEquipIfMissing(state, EquipmentSlot.CHESTPLATE, "starter_chestplate", "equip_chestplate");
        addAndEquipIfMissing(state, EquipmentSlot.LEGGINGS,   "starter_leggings",   "equip_leggings");
        addAndEquipIfMissing(state, EquipmentSlot.BOOTS,      "starter_boots",      "equip_boots");
        player.getInventory().setItem(0, taggedWeapon(weaponType));
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
        player.getInventory().setItem(8, menuCompass());
    }

    private ItemStack menuCompass() {
        ItemStack item = new ItemStack(Material.COMPASS);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("§e메뉴"));
        item.setItemMeta(meta);
        return item;
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
        meta.displayName(Component.text("§f" + displayName));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack taggedWeapon(WeaponType weaponType) {
        ItemStack item = new ItemStack(materialFor(weaponType));
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("§f" + displayNameFor(weaponType)));
        if (WeaponTypeResolver.WEAPON_TYPE_KEY != null) {
            meta.getPersistentDataContainer().set(
                    WeaponTypeResolver.WEAPON_TYPE_KEY,
                    PersistentDataType.STRING,
                    weaponType.name());
        }
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

    private Material materialFor(WeaponType type) {
        return switch (type) {
            case SWORD, SPEAR -> Material.NETHERITE_SWORD;
            case AXE          -> Material.NETHERITE_AXE;
            case CROSSBOW     -> Material.CROSSBOW;
            case SCYTHE       -> Material.NETHERITE_HOE;
            case STAFF        -> Material.BLAZE_ROD;
            case NONE         -> Material.STICK;
        };
    }

    private String displayNameFor(WeaponType type) {
        return switch (type) {
            case SWORD    -> "검";
            case AXE      -> "도끼";
            case SPEAR    -> "창";
            case CROSSBOW -> "석궁";
            case SCYTHE   -> "낫";
            case STAFF    -> "스태프";
            case NONE     -> "무기";
        };
    }
}
