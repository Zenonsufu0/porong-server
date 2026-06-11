package com.porong.gun.core;

import com.porong.gun.PorongunCore;
import com.porong.gun.registry.PorongunBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 코어 설치/파괴 처리 (M-Core — base_raid 「코어 설치 제약」 근거).
 *
 * 설치 검증(1단계): ①기존 영역(64×64) 겹침 금지 ②영역이 월드보더 안.
 * (②POI 내 불가는 M-POI 연동 후속.) 파괴 = 등록 해제(완파·레벨 초기화는 후속).
 */
@Mod.EventBusSubscriber(modid = PorongunCore.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CoreEvents {

    private CoreEvents() {}

    @SubscribeEvent
    public static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (!event.getPlacedBlock().is(PorongunBlocks.CORE.get())) return;
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        BlockPos pos = event.getPos();
        Entity placer = event.getEntity();
        CoreManager mgr = CoreManager.get(level);

        BlockPos conflict = mgr.findOverlap(pos);
        if (conflict != null) {
            event.setCanceled(true);
            msg(placer, "§c코어 설치 불가 — 기존 거점 영역(64×64)과 겹침");
            return;
        }

        WorldBorder wb = level.getWorldBorder();
        if (!wb.isWithinBounds(pos.offset(-CoreManager.RADIUS, 0, -CoreManager.RADIUS))
                || !wb.isWithinBounds(pos.offset(CoreManager.RADIUS, 0, CoreManager.RADIUS))) {
            event.setCanceled(true);
            msg(placer, "§c맵 경계에 너무 가까움 — 거점 영역 64×64가 안 들어감");
            return;
        }

        mgr.addCore(pos, placer instanceof Player p ? p.getUUID() : null);
        msg(placer, "§a거점 코어 설치 — 보호 영역 64×64 등록 (중심 "
                + pos.getX() + ", " + pos.getZ() + " / 총 " + mgr.count() + "개)");
    }

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (!event.getState().is(PorongunBlocks.CORE.get())) return;
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        CoreManager.get(level).removeCore(event.getPos());
        msg(event.getPlayer(), "§e거점 코어 파괴 — 보호 영역 해제");
        // 1단계: 등록 해제만. 완파·레벨 초기화·자물쇠 해제는 후속.
    }

    private static void msg(Entity entity, String text) {
        if (entity instanceof Player p) {
            p.displayClientMessage(Component.literal(text), false);
        }
    }
}
