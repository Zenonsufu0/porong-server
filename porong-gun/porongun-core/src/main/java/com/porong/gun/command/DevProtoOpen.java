package com.porong.gun.command;

import com.porong.gun.PorongunCore;
import com.porong.gun.inv.BackpackProtoMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkHooks;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 프로토타입 검증용 GUI 오픈 트리거(키보드 입력 불가 환경 대비, 마우스/자동만 사용).
 *  - 로그인 ~2초 후 1회 자동 오픈(무입력 검증).
 *  - 이후 우클릭(블록/아이템)으로 재오픈 — 실제 설계의 "코어 우클릭" 접근과 동일 결.
 *
 * 착수 후 정식 트리거(코어 블록 우클릭/Shift+F)로 교체될 임시 코드.
 */
@Mod.EventBusSubscriber(modid = PorongunCore.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class DevProtoOpen {

    private static final Set<UUID> autoOpened = new HashSet<>();
    private static final Map<UUID, Integer> ticks = new HashMap<>();

    private DevProtoOpen() {}

    private static void open(ServerPlayer player) {
        NetworkHooks.openScreen(player, new SimpleMenuProvider(
                (id, inv, p) -> new BackpackProtoMenu(id, inv),
                Component.literal("Porongun 인벤 프로토타입")));
    }

    @SubscribeEvent
    public static void onTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer player)) return;
        UUID id = player.getUUID();
        if (autoOpened.contains(id)) return;
        if (ticks.merge(id, 1, Integer::sum) < 40) return; // ~2초
        autoOpened.add(id);
        open(player);
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            open(player);
        }
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            open(player);
        }
    }
}
