package com.porong.gun.client;

import com.porong.gun.PorongunCore;
import com.porong.gun.registry.PorongunMenus;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * 클라이언트 전용 설정. MenuType ↔ Screen 바인딩.
 * Dist.CLIENT 한정 — 전용 서버에선 이 클래스가 로드되지 않는다(클라 전용 클래스 참조 안전).
 */
@Mod.EventBusSubscriber(modid = PorongunCore.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class PorongunClientEvents {

    private PorongunClientEvents() {}

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(PorongunMenus.BACKPACK_PROTO.get(), BackpackProtoScreen::new);
            MenuScreens.register(PorongunMenus.SHOP.get(), ShopScreen::new);
            MenuScreens.register(PorongunMenus.BASE_HUB.get(), BaseHubScreen::new);
            MenuScreens.register(PorongunMenus.BASE_MANAGE.get(), BaseManageScreen::new);
        });
    }
}
