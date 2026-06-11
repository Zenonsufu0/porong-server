package com.porong.gun.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
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
}
