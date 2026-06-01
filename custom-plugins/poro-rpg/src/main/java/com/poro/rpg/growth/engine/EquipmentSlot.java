package com.poro.rpg.growth.engine;

import java.util.Locale;

public enum EquipmentSlot {
    WEAPON("weapon"),
    HELMET("armor"),
    CHESTPLATE("armor"),
    LEGGINGS("armor"),
    BOOTS("armor"),
    ACCESSORY_1("accessory"),
    ACCESSORY_2("accessory"),
    ACCESSORY_3("accessory");

    private final String itemSlotType;

    EquipmentSlot(String itemSlotType) {
        this.itemSlotType = itemSlotType;
    }

    public String itemSlotType() {
        return itemSlotType;
    }

    public static EquipmentSlot from(String raw) {
        String normalized = raw.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "ARMOR_HEAD" -> HELMET;
            case "ARMOR_CHEST" -> CHESTPLATE;
            case "ARMOR_HANDS" -> LEGGINGS;
            case "ARMOR_LEGS" -> BOOTS;
            default -> valueOf(normalized);
        };
    }
}
