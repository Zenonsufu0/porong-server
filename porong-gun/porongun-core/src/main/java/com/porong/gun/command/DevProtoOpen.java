package com.porong.gun.command;

import com.porong.gun.PorongunCore;
import com.porong.gun.registry.PorongunItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * dev 검증용 테스트 키트 지급 (임시 — 정식 온보딩/제작으로 교체).
 *
 * 로그인 ~2초 후 1회, 핫바에 검증용 아이템 지급(키보드 입력 불가 환경 대비).
 * 현재: M-Core 검증 — 코어 4개(설치·영역 겹침 테스트).
 *
 * (구 인벤 GUI 자동오픈/우클릭 오픈은 코어 설치 우클릭과 충돌해 제거. 인벤 GUI는
 *  /porongun invtest 또는 후속 코어 우클릭 GUI로 연다.)
 */
@Mod.EventBusSubscriber(modid = PorongunCore.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class DevProtoOpen {

    private static final Set<UUID> kitted = new HashSet<>();
    private static final Map<UUID, Integer> ticks = new HashMap<>();

    private DevProtoOpen() {}

    @SubscribeEvent
    public static void onTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer player)) return;
        UUID id = player.getUUID();
        if (kitted.contains(id)) return;
        if (ticks.merge(id, 1, Integer::sum) < 40) return; // ~2초
        kitted.add(id);

        // 핫바 슬롯0 = 코어(기본 선택, 우클릭 = 설치 / 설치된 코어 우클릭 = 거점 허브)
        player.getInventory().setItem(0, new ItemStack(PorongunItems.CORE.get(), 4));
        // 상점·업그레이드 테스트용: 코인 + 재료(LV2~3 업글 가능 분량, 그 이상은 /give)
        com.porong.gun.shop.CurrencyUtil.give(player, 3000);
        player.getInventory().add(new ItemStack(net.minecraft.world.item.Items.IRON_BLOCK, 8));
        player.getInventory().add(new ItemStack(net.minecraft.world.item.Items.DIAMOND, 16));
        player.getInventory().add(new ItemStack(PorongunItems.TUNGSTEN_ALLOY.get(), 64));
        player.getInventory().add(new ItemStack(PorongunItems.MILITARY_ALLOY.get(), 16));
        player.getInventory().add(new ItemStack(PorongunItems.ELECTRONIC_PART.get(), 8));
        player.displayClientMessage(
                net.minecraft.network.chat.Component.literal(
                        "§7[dev] 코어4·코인3000·업글재료 지급 — 코어 설치→우클릭=거점(상점·기지관리)"),
                false);
    }
}
