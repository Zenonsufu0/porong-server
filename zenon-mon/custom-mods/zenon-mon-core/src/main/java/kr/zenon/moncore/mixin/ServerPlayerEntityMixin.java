package kr.zenon.moncore.mixin;

import kr.zenon.moncore.config.ConfigManager;
import kr.zenon.moncore.item.MenuItemManager;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 리그 패스 드롭 방지 (menu_design.md §2 preventDrop).
 * Q-드롭 경로(dropSelectedItem)를 가로채 선택 슬롯이 정품 패스면 취소한다.
 * 취소 후 playerScreenHandler.syncState()로 클라 예측 드롭(손에서 사라짐)을 되돌린다.
 */
@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin {

    @Inject(method = "dropSelectedItem", at = @At("HEAD"), cancellable = true)
    private void zenonmoncore$preventPassDrop(boolean entireStack, CallbackInfoReturnable<Boolean> cir) {
        if (!ConfigManager.core().menuItem.preventDrop) return;
        ServerPlayerEntity self = (ServerPlayerEntity) (Object) this;
        ItemStack selected = self.getInventory().getMainHandStack();
        if (MenuItemManager.isPass(selected)) {
            cir.setReturnValue(false);              // 드롭 취소(아이템 슬롯 유지)
            self.playerScreenHandler.syncState();   // 클라 예측 되돌림(손에 재표시)
        }
    }
}
