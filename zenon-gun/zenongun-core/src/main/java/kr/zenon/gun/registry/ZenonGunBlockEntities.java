package kr.zenon.gun.registry;

import kr.zenon.gun.ZenonGunCore;
import kr.zenon.gun.block.CoreBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * BlockEntity 레지스트리 (M-Core).
 */
public final class ZenonGunBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, ZenonGunCore.MODID);

    public static final RegistryObject<BlockEntityType<CoreBlockEntity>> CORE =
            BLOCK_ENTITIES.register("core",
                    () -> BlockEntityType.Builder.of(CoreBlockEntity::new, ZenonGunBlocks.CORE.get()).build(null));

    private ZenonGunBlockEntities() {}

    public static void register(IEventBus modBus) {
        BLOCK_ENTITIES.register(modBus);
    }
}
