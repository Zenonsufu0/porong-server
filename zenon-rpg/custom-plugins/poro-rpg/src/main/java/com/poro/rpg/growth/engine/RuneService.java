package com.poro.rpg.growth.engine;

import com.poro.rpg.common.registry.master.ItemMasterRegistry;
import com.poro.rpg.common.registry.master.model.ItemMaster;
import com.poro.rpg.common.result.ErrorCode;
import com.poro.rpg.common.result.Result;

import java.util.Locale;
import java.util.Objects;

public final class RuneService {
    private static final int MIN_RUNE_SLOT = 1;
    private static final int MAX_RUNE_SLOT = 3;

    private final RuneRegistry runeRegistry;
    private final ItemMasterRegistry itemMasterRegistry;

    public RuneService(RuneRegistry runeRegistry, ItemMasterRegistry itemMasterRegistry) {
        this.runeRegistry = Objects.requireNonNull(runeRegistry, "runeRegistry");
        this.itemMasterRegistry = Objects.requireNonNull(itemMasterRegistry, "itemMasterRegistry");
    }

    public Result<Void> equip(PlayerGrowthState state, int runeSlot, String runeId) {
        if (runeSlot < MIN_RUNE_SLOT || runeSlot > MAX_RUNE_SLOT) {
            return Result.failure(ErrorCode.INVALID_ARGUMENT, "Rune slot must be between 1 and 3.");
        }
        RuneMaster rune = runeRegistry.find(runeId).orElse(null);
        if (rune == null) {
            return Result.failure(ErrorCode.INVALID_ARGUMENT, "Unknown rune_id: " + runeId);
        }
        String normalizedRuneId = normalized(runeId);
        if (state.equippedRunes().containsValue(normalizedRuneId)) {
            return Result.failure(ErrorCode.INVALID_ARGUMENT, "Same rune is already equipped in another slot.");
        }

        String filter = normalized(rune.slotFilter());
        if (!filter.isBlank() && !"any".equals(filter) && !hasEquippedSlotType(state, filter)) {
            return Result.failure(
                    ErrorCode.INVALID_ARGUMENT,
                    "Rune slot filter is not satisfied. rune=" + rune.runeId() + ", required_slot_type=" + rune.slotFilter()
            );
        }

        state.equipRune(runeSlot, normalizedRuneId);
        return Result.success();
    }

    public Result<Void> unequip(PlayerGrowthState state, int runeSlot) {
        if (runeSlot < MIN_RUNE_SLOT || runeSlot > MAX_RUNE_SLOT) {
            return Result.failure(ErrorCode.INVALID_ARGUMENT, "Rune slot must be between 1 and 3.");
        }
        state.unequipRune(runeSlot);
        return Result.success();
    }

    public GrowthStatBlock buildStatBlock(PlayerGrowthState state) {
        GrowthStatBlock block = new GrowthStatBlock();
        for (String runeId : state.equippedRunes().values()) {
            RuneMaster rune = runeRegistry.find(runeId).orElse(null);
            if (rune == null) {
                continue;
            }
            if ("FLAG".equalsIgnoreCase(rune.valueType())) {
                block.addFlag(rune.effectType());
            } else {
                block.add(rune.effectType(), rune.valueAmount());
            }
        }
        return block;
    }

    // DL-129 3단계: 방어구 slot_type을 head/chest/legs/feet로 세분. 룬 slot_filter="armor"(예: rune_survival_minor)
    // 하위호환 — "armor"는 4개 부위 어느 하나라도 장착돼 있으면 충족으로 본다.
    private static final java.util.Set<String> ARMOR_SUBSLOTS =
            java.util.Set.of("head", "chest", "legs", "feet");

    private boolean hasEquippedSlotType(PlayerGrowthState state, String slotType) {
        boolean armorGroup = "armor".equals(slotType);
        for (String itemInstanceId : state.equippedItems().values()) {
            PlayerEquipmentItem item = state.inventoryItem(itemInstanceId).orElse(null);
            if (item == null) {
                continue;
            }
            ItemMaster itemMaster = itemMasterRegistry.find(item.itemId()).orElse(null);
            if (itemMaster == null) {
                continue;
            }
            String itemSlot = normalized(itemMaster.slotType());
            if (slotType.equals(itemSlot) || (armorGroup && ARMOR_SUBSLOTS.contains(itemSlot))) {
                return true;
            }
        }
        return false;
    }

    private String normalized(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
