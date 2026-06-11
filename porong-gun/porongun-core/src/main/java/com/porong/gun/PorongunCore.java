package com.porong.gun;

import com.mojang.logging.LogUtils;
import com.porong.gun.registry.PorongunItems;
import com.porong.gun.registry.PorongunMenus;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

/**
 * porongun-core 진입점.
 *
 * 구현은 impl_plan.md(모듈 M-Core/Block/Material/Currency/Inv/POI/World/Auth/Scav/Raid좀비/Finale)를 따른다.
 * 게임플레이 동작은 design/ 기획서 근거 — 없는 동작은 임의 구현 금지(impl_plan §0).
 *
 * 현재: M0 스캐폴딩(빈 모드). 빌드·dev 로드 확인용.
 */
@Mod(PorongunCore.MODID)
public final class PorongunCore {

    public static final String MODID = "porongun";
    private static final Logger LOGGER = LogUtils.getLogger();

    public PorongunCore() {
        final IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        // M0 인벤 GUI 프로토타입: MenuType 등록(Screen 바인딩=PorongunClientEvents, 커맨드=PorongunCommands).
        PorongunMenus.register(modBus);
        PorongunItems.register(modBus); // M-Material 재료 4종(텅스텐·열화우라늄·군용·전자)
        // TODO(M1~): 코어 블록/아이템·화폐 등 나머지 모듈 등록(impl_plan §4).

        LOGGER.info("[porongun-core] M0 로드 — 인벤 GUI 프로토타입(/porongun invtest)");
    }
}
