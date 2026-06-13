package kr.zenon.rpg.combat.weapon;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Locale;

public final class WeaponTypeResolver {
    public static final NamespacedKey WEAPON_TYPE_KEY = NamespacedKey.fromString("poro_rpg:weapon_type");

    private WeaponTypeResolver() {
    }

    public static WeaponType resolve(Player player) {
        if (player == null) {
            return WeaponType.NONE;
        }

        return resolve(player.getInventory().getItemInMainHand());
    }

    public static WeaponType resolve(ItemStack itemStack) {
        if (itemStack == null) {
            return WeaponType.NONE;
        }
        // PDC 태그 기반으로만 인식한다.
        // Material 폴백을 허용하면 영지 도구(네더라이트 곡괭이/호미/도끼 등)가
        // 무기 타입으로 잘못 인식되어 스킬이 발동된다.
        return resolveTaggedType(itemStack);
    }

    public static WeaponType resolve(Material material) {
        if (material == null || material.isAir()) {
            return WeaponType.NONE;
        }

        return switch (material) {
            case NETHERITE_SWORD -> WeaponType.SWORD;
            case MACE, NETHERITE_AXE -> WeaponType.AXE;
            case TRIDENT -> WeaponType.SPEAR;
            case CROSSBOW -> WeaponType.CROSSBOW;
            case NETHERITE_HOE -> WeaponType.SCYTHE;
            case BLAZE_ROD, STICK -> WeaponType.STAFF;
            default -> WeaponType.NONE;
        };
    }

    private static WeaponType resolveTaggedType(ItemStack itemStack) {
        if (WEAPON_TYPE_KEY == null || !itemStack.hasItemMeta()) {
            return WeaponType.NONE;
        }
        ItemMeta meta = itemStack.getItemMeta();
        String raw = meta.getPersistentDataContainer().get(WEAPON_TYPE_KEY, PersistentDataType.STRING);
        if (raw == null || raw.isBlank()) {
            return WeaponType.NONE;
        }
        try {
            return WeaponType.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return WeaponType.NONE;
        }
    }
}
