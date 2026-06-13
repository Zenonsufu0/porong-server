package kr.zenon.moncore.mixin;

import kr.zenon.moncore.config.ConfigManager;
import kr.zenon.moncore.config.CoreConfig;
import kr.zenon.moncore.item.MenuItemManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 리그 패스 슬롯 잠금/드롭 방지 (menu_design.md §2 lockSlot/preventDrop).
 * 인벤토리 화면(E)·셰스트 등 모든 ScreenHandler의 onSlotClick을 가로채,
 * 패스를 옮기거나(THROW/SWAP/QUICK_MOVE/PICKUP) 버리려는 모든 상호작용을 취소한다.
 * (월드에서의 Q-드롭은 ServerPlayerEntityMixin이 별도 차단.)
 */
@Mixin(ScreenHandler.class)
public abstract class ScreenHandlerMixin {

    @Inject(method = "onSlotClick", at = @At("HEAD"), cancellable = true)
    private void zenonmoncore$lockPass(int slotIndex, int button, SlotActionType actionType,
                                  PlayerEntity player, CallbackInfo ci) {
        if (!(player instanceof ServerPlayerEntity)) return;
        CoreConfig.MenuItem cfg = ConfigManager.core().menuItem;
        if (!cfg.enabled) return;
        if (!cfg.lockSlot && !cfg.preventDrop) return;

        ScreenHandler self = (ScreenHandler) (Object) this;

        boolean block = false;
        // 커서에 패스가 있으면(드래그 중) 모든 동작 취소
        if (MenuItemManager.isPass(self.getCursorStack())) {
            block = true;
        }
        // 클릭 대상 슬롯에 패스가 있으면 취소
        else if (slotIndex >= 0 && slotIndex < self.slots.size()) {
            Slot slot = self.getSlot(slotIndex);
            if (MenuItemManager.isPass(slot.getStack())) block = true;
        }
        // 숫자키 스왑(SWAP): button=대상 핫바 칸. 패스가 든 핫바 칸을 빼내려는 시도 취소.
        if (!block && actionType == SlotActionType.SWAP && button == cfg.hotbarSlot) {
            block = true;
        }

        if (block) {
            ci.cancel();
            self.syncState(); // 클라 예측 변경 되돌림
        }
    }
}
