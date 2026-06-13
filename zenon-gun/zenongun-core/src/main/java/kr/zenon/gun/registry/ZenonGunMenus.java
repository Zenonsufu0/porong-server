package kr.zenon.gun.registry;

import kr.zenon.gun.ZenonGunCore;
import kr.zenon.gun.base.BaseHubMenu;
import kr.zenon.gun.base.BaseManageMenu;
import kr.zenon.gun.inv.BackpackProtoMenu;
import kr.zenon.gun.shop.ShopMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * MenuType 레지스트리.
 *
 * M0 프로토타입: 인벤 GUI(무게 점유칸·보안칸·유리 식별) 검증용 backpack_proto 한 개.
 * 실제 M-Inv/M-Currency에선 backpack·shop·base 등 메뉴를 여기에 추가한다(impl_plan §4).
 */
public final class ZenonGunMenus {

    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, ZenonGunCore.MODID);

    /** 인벤 GUI 프로토타입 메뉴. 클라 팩토리는 (id, inv, buf) 생성자. */
    public static final RegistryObject<MenuType<BackpackProtoMenu>> BACKPACK_PROTO =
            MENU_TYPES.register("backpack_proto",
                    () -> IForgeMenuType.create(BackpackProtoMenu::new));

    /** 상점 메뉴(M-Currency). 거점 허브에서 진입. */
    public static final RegistryObject<MenuType<ShopMenu>> SHOP =
            MENU_TYPES.register("shop", () -> IForgeMenuType.create(ShopMenu::new));

    /** 거점 허브 메뉴(M-Core). 코어 우클릭 시 첫 진입(상점·기지·정보). */
    public static final RegistryObject<MenuType<BaseHubMenu>> BASE_HUB =
            MENU_TYPES.register("base_hub", () -> IForgeMenuType.create(BaseHubMenu::new));

    /** 기지 관리 메뉴(M-Core 2단계). 코어 레벨 정보·업그레이드. */
    public static final RegistryObject<MenuType<BaseManageMenu>> BASE_MANAGE =
            MENU_TYPES.register("base_manage", () -> IForgeMenuType.create(BaseManageMenu::new));

    private ZenonGunMenus() {}

    public static void register(IEventBus modBus) {
        MENU_TYPES.register(modBus);
    }
}
