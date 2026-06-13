package com.porong.gun.block;

import com.porong.gun.registry.PorongunBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 코어 블록 엔티티 (M-Core 골격).
 *
 * 1단계는 EntityBlock 요건을 채우는 최소 형태. 소유자·레벨·보안칸·손상 블록 맵은
 * 현재 {@link com.porong.gun.core.CoreManager}(SavedData)가 단일 출처로 관리하고,
 * 자물쇠·자가복구 tick이 들어가는 후속 단계에서 이 BE로 책임을 넓힌다.
 */
public class CoreBlockEntity extends BlockEntity {

    public CoreBlockEntity(BlockPos pos, BlockState state) {
        super(PorongunBlockEntities.CORE.get(), pos, state);
    }
}
