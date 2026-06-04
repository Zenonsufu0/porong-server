package com.poro.rpg.growth.engine;

import java.util.Locale;

public enum EquipmentSlot {
    // itemSlotType = item_master.slot_type 와 매칭(장착 검증 + 잠재 풀 resolvePoolId).
    // DL-129 3단계: 방어구 슬롯을 정본 부위(head/chest/legs/feet)로 세분 — 부위별 잠재 풀 분리.
    WEAPON("weapon"),
    HELMET("head"),
    CHESTPLATE("chest"),
    LEGGINGS("legs"),
    BOOTS("feet"),
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
