package com.poro.empire.combat.weapon;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public final class WeaponTypeResolver {
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

        return resolve(itemStack.getType());
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
}
