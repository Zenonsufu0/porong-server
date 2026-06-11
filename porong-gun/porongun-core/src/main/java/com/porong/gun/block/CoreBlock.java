package com.porong.gun.block;

import com.porong.gun.shop.ShopMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

/**
 * 거점 코어 블록 (M-Core — base_raid 「코어 v2」 근거).
 *
 * 옵시디언급 내구. 설치/검증/영역 등록은 {@link com.porong.gun.core.CoreEvents}(설치 시점),
 * 단일 출처는 {@link com.porong.gun.core.CoreManager}(SavedData). BlockEntity는 후속 단계
 * (자물쇠·자가복구 tick)용 골격.
 */
public class CoreBlock extends Block implements EntityBlock {

    public CoreBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CoreBlockEntity(pos, state);
    }

    /** 코어 우클릭 = 거점 GUI(현재 상점). economy 「코어 우클릭/Shift+F」. 권한·탭은 후속. */
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            NetworkHooks.openScreen(serverPlayer, new SimpleMenuProvider(
                    (id, inv, p) -> new ShopMenu(id, inv), Component.literal("상점")));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
