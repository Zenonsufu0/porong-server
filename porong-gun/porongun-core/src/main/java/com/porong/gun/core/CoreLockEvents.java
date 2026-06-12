package com.porong.gun.core;

import com.porong.gun.PorongunCore;
import com.porong.gun.registry.PorongunBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

/**
 * 코어 자물쇠 (M-Core 2단계 — base_raid 「코어 = 보호 영역 상자의 자물쇠」).
 *
 * 코어가 살아있는 한 보호 영역(64×64) 안 모든 컨테이너는 **외부인(비소유)에게 잠김**.
 * → 벽 뚫고 들어와도 상자가 안 열림, 코어를 찾아 부숴야 약탈 가능 = 레이드 목적 부여.
 *
 * 현재 판정 = 소유자 UUID. **연합(OPaC 파티) 예외는 후속**(deobf 연동 시 isAllowed 확장).
 * 보안칸(인벤)은 무관하게 항상 안전 — M-Inv.
 */
@Mod.EventBusSubscriber(modid = PorongunCore.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CoreLockEvents {

    private CoreLockEvents() {}

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        BlockPos pos = event.getPos();

        // 코어 블록 자체는 제외(허브 GUI는 CoreBlock.use가 처리).
        if (level.getBlockState(pos).is(PorongunBlocks.CORE.get())) return;

        // 컨테이너(상자·통 등)만 대상.
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof Container) && !(be instanceof MenuProvider)) return;

        CoreManager mgr = CoreManager.get(level);
        BlockPos core = mgr.coreCovering(pos);
        if (core == null) return; // 보호 영역 밖 = 자유

        if (isAllowed(mgr.get(core), event.getEntity())) return; // 소유자(연합은 후속)

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.FAIL);
        event.getEntity().displayClientMessage(
                Component.literal("§c코어 자물쇠 — 거점 코어를 파괴해야 약탈 가능"), true);
    }

    /** 소유자(또는 owner 미지정 코어)면 허용. 연합 파티 예외는 OPaC 연동 후속. */
    private static boolean isAllowed(CoreManager.CoreData data, Player player) {
        if (data == null) return true;
        UUID owner = data.owner();
        return owner == null || owner.equals(player.getUUID());
    }
}
