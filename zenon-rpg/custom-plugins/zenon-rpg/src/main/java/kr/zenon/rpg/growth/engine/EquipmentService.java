package kr.zenon.rpg.growth.engine;

import kr.zenon.rpg.common.registry.master.ItemMasterRegistry;
import kr.zenon.rpg.common.registry.master.model.ItemMaster;
import kr.zenon.rpg.common.result.ErrorCode;
import kr.zenon.rpg.common.result.Result;

import java.util.Locale;
import java.util.Objects;

public final class EquipmentService {
    private final ItemMasterRegistry itemMasterRegistry;
    private final StatRecalculationHook recalculationHook;

    public EquipmentService(ItemMasterRegistry itemMasterRegistry, StatRecalculationHook recalculationHook) {
        this.itemMasterRegistry = Objects.requireNonNull(itemMasterRegistry, "itemMasterRegistry");
        this.recalculationHook = Objects.requireNonNull(recalculationHook, "recalculationHook");
    }

    public Result<Void> equip(PlayerGrowthState state, EquipmentSlot slot, String itemInstanceId) {
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(slot, "slot");

        PlayerEquipmentItem item = state.inventoryItem(itemInstanceId).orElse(null);
        if (item == null) {
            return Result.failure(ErrorCode.INVALID_ARGUMENT, "Item instance not found: " + itemInstanceId);
        }

        ItemMaster itemMaster = itemMasterRegistry.find(item.itemId()).orElse(null);
        if (itemMaster == null) {
            return Result.failure(ErrorCode.INVALID_ARGUMENT, "Unknown item master: " + item.itemId());
        }

        if (!slot.itemSlotType().equals(normalized(itemMaster.slotType()))) {
            return Result.failure(
                    ErrorCode.INVALID_ARGUMENT,
                    "Slot mismatch. slot=" + slot + ", item_slot_type=" + itemMaster.slotType()
            );
        }

        state.equipItem(slot, item.itemInstanceId());
        recalculationHook.onRecalculate(state);
        return Result.success();
    }

    public Result<Void> unequip(PlayerGrowthState state, EquipmentSlot slot) {
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(slot, "slot");
        state.unequipItem(slot);
        recalculationHook.onRecalculate(state);
        return Result.success();
    }

    private String normalized(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
