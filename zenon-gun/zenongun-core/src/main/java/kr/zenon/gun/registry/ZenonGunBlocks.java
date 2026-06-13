package kr.zenon.gun.registry;

import kr.zenon.gun.ZenonGunCore;
import kr.zenon.gun.block.CoreBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * 블록 레지스트리 (M-Core). 코어 블록(옵시디언급 내구).
 */
public final class ZenonGunBlocks {

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, ZenonGunCore.MODID);

    /** 거점 코어 — 옵시디언급(strength 50/1200, 곡괭이 요구). */
    public static final RegistryObject<Block> CORE = BLOCKS.register("core",
            () -> new CoreBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK)
                    .strength(50.0F, 1200.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.STONE)));

    private ZenonGunBlocks() {}

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
    }
}
