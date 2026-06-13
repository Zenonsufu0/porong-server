package kr.zenon.gun;

import com.mojang.logging.LogUtils;
import kr.zenon.gun.registry.ZenonGunBlockEntities;
import kr.zenon.gun.registry.ZenonGunBlocks;
import kr.zenon.gun.registry.ZenonGunItems;
import kr.zenon.gun.registry.ZenonGunMenus;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

/**
 * zenongun-core 진입점.
 *
 * 구현은 impl_plan.md(모듈 M-Core/Block/Material/Currency/Inv/POI/World/Auth/Scav/Raid좀비/Finale)를 따른다.
 * 게임플레이 동작은 design/ 기획서 근거 — 없는 동작은 임의 구현 금지(impl_plan §0).
 *
 * 현재: M0 스캐폴딩(빈 모드). 빌드·dev 로드 확인용.
 */
@Mod(ZenonGunCore.MODID)
public final class ZenonGunCore {

    public static final String MODID = "zenongun";
    private static final Logger LOGGER = LogUtils.getLogger();

    public ZenonGunCore() {
        final IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        // M0 인벤 GUI 프로토타입: MenuType 등록(Screen 바인딩=ZenonGunClientEvents, 커맨드=ZenonGunCommands).
        ZenonGunBlocks.register(modBus);         // M-Core 코어 블록
        ZenonGunBlockEntities.register(modBus);  // M-Core 코어 BE
        ZenonGunItems.register(modBus);          // M-Material 재료 4종 + M-Core 코어 아이템
        ZenonGunMenus.register(modBus);
        // TODO(M1~): 화폐·POI·월드 등 나머지 모듈 등록(impl_plan §4).

        LOGGER.info("[zenongun-core] M0 로드 — 인벤 GUI 프로토타입(/zenongun invtest)");
    }
}
