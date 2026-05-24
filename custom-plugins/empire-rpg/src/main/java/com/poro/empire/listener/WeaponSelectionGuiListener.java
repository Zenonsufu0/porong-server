package com.poro.empire.listener;

import com.poro.empire.combat.CombatStateService;
import com.poro.empire.combat.CooldownManager;
import com.poro.empire.combat.weapon.WeaponType;
import com.poro.empire.combat.weapon.WeaponTypeResolver;
import com.poro.empire.growth.GrowthStateStore;
import com.poro.empire.growth.engine.EquipmentSlot;
import com.poro.empire.growth.engine.ItemGrade;
import com.poro.empire.growth.engine.PlayerEquipmentItem;
import com.poro.empire.growth.engine.PlayerGrowthState;
import com.poro.empire.scoreboard.ScoreboardService;
import com.poro.empire.storage.PlayerDataManager;
import com.poro.empire.tutorial.TutorialService;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.Locale;

public final class WeaponSelectionGuiListener implements Listener {
    private final PlayerDataManager playerDataManager;
    private final ScoreboardService scoreboardService;
    private final GrowthStateStore growthStateStore;

    public WeaponSelectionGuiListener(
            PlayerDataManager playerDataManager,
            TutorialService tutorialService,
            ScoreboardService scoreboardService,
            GrowthStateStore growthStateStore,
            CooldownManager cooldownManager,
            CombatStateService combatStateService
    ) {
        this.playerDataManager = playerDataManager;
        this.scoreboardService = scoreboardService;
        this.growthStateStore = growthStateStore;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof org.bukkit.entity.Player player)) {
            return;
        }
        String title = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                .serialize(event.getView().title());
        if (!title.contains("무기") && !title.toLowerCase(java.util.Locale.ROOT).contains("weapon")) {
            return;
        }
        WeaponType selected = switch (event.getRawSlot()) {
            case 10 -> WeaponType.SWORD;
            case 11 -> WeaponType.AXE;
            case 12 -> WeaponType.SPEAR;
            case 13 -> WeaponType.CROSSBOW;
            case 14 -> WeaponType.SCYTHE;
            case 15 -> WeaponType.STAFF;
            default -> null;
        };
        if (selected == null) {
            return;
        }
        event.setCancelled(true);
        if (playerDataManager.hasSelectedWeapon(player)) {
            player.sendMessage("§c이미 무기 클래스를 선택했습니다.");
            return;
        }
        playerDataManager.setWeaponType(player, selected);
        grantStarterEquipment(player, selected);
        scoreboardService.refresh(player);
        player.closeInventory();
        player.sendMessage("§a무기 클래스 선택: " + selected.name().toLowerCase(java.util.Locale.ROOT));
    }

    private void grantStarterEquipment(org.bukkit.entity.Player player, WeaponType weaponType) {
        player.getInventory().setItem(0, taggedWeapon(weaponType));
        String classId = weaponType.name().toLowerCase(Locale.ROOT);
        PlayerGrowthState state = growthStateStore.getOrCreate(player.getUniqueId(), classId);
        addAndEquipIfMissing(state, EquipmentSlot.WEAPON, "starter_" + classId, "equip_" + classId);
        addAndEquipIfMissing(state, EquipmentSlot.HELMET, "starter_helmet", "equip_helmet");
        addAndEquipIfMissing(state, EquipmentSlot.CHESTPLATE, "starter_chestplate", "equip_chestplate");
        addAndEquipIfMissing(state, EquipmentSlot.LEGGINGS, "starter_leggings", "equip_leggings");
        addAndEquipIfMissing(state, EquipmentSlot.BOOTS, "starter_boots", "equip_boots");
    }

    private void addAndEquipIfMissing(PlayerGrowthState state, EquipmentSlot slot, String instanceId, String itemId) {
        if (state.inventoryItem(instanceId).isEmpty()) {
            state.addInventoryItem(PlayerEquipmentItem.restore(
                    instanceId, itemId, 0, ItemGrade.COMMON, null, List.of()));
        }
        if (state.equippedItemInstanceId(slot).isEmpty()) {
            state.equipItem(slot, instanceId);
        }
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

    private Material materialFor(WeaponType weaponType) {
        return switch (weaponType) {
            case SWORD, SPEAR -> Material.NETHERITE_SWORD;
            case AXE -> Material.NETHERITE_AXE;
            case CROSSBOW -> Material.CROSSBOW;
            case SCYTHE -> Material.NETHERITE_HOE;
            case STAFF -> Material.BLAZE_ROD;
            case NONE -> Material.STICK;
        };
    }

    private String displayNameFor(WeaponType weaponType) {
        return switch (weaponType) {
            case SWORD -> "검";
            case AXE -> "도끼";
            case SPEAR -> "창";
            case CROSSBOW -> "석궁";
            case SCYTHE -> "낫";
            case STAFF -> "스태프";
            case NONE -> "무기";
        };
    }
}
