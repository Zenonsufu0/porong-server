package com.porong.gun.registry;

import com.porong.gun.PorongunCore;
import com.porong.gun.block.CoreBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * BlockEntity 레지스트리 (M-Core).
 */
public final class PorongunBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, PorongunCore.MODID);

    public static final RegistryObject<BlockEntityType<CoreBlockEntity>> CORE =
            BLOCK_ENTITIES.register("core",
                    () -> BlockEntityType.Builder.of(CoreBlockEntity::new, PorongunBlocks.CORE.get()).build(null));

    private PorongunBlockEntities() {}

    public static void register(IEventBus modBus) {
        BLOCK_ENTITIES.register(modBus);
    }
}
