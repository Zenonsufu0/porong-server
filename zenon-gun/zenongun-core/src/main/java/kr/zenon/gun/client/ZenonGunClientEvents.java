package kr.zenon.gun.client;

import kr.zenon.gun.ZenonGunCore;
import kr.zenon.gun.registry.ZenonGunMenus;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * 클라이언트 전용 설정. MenuType ↔ Screen 바인딩.
 * Dist.CLIENT 한정 — 전용 서버에선 이 클래스가 로드되지 않는다(클라 전용 클래스 참조 안전).
 */
@Mod.EventBusSubscriber(modid = ZenonGunCore.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ZenonGunClientEvents {

    private ZenonGunClientEvents() {}

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(ZenonGunMenus.BACKPACK_PROTO.get(), BackpackProtoScreen::new);
            MenuScreens.register(ZenonGunMenus.SHOP.get(), ShopScreen::new);
            MenuScreens.register(ZenonGunMenus.BASE_HUB.get(), BaseHubScreen::new);
            MenuScreens.register(ZenonGunMenus.BASE_MANAGE.get(), BaseManageScreen::new);
        });
    }
}
