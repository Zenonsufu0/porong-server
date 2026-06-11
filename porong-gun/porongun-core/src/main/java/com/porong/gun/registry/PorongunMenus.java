package com.porong.gun.registry;

import com.porong.gun.PorongunCore;
import com.porong.gun.inv.BackpackProtoMenu;
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
public final class PorongunMenus {

    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, PorongunCore.MODID);

    /** 인벤 GUI 프로토타입 메뉴. 클라 팩토리는 (id, inv, buf) 생성자. */
    public static final RegistryObject<MenuType<BackpackProtoMenu>> BACKPACK_PROTO =
            MENU_TYPES.register("backpack_proto",
                    () -> IForgeMenuType.create(BackpackProtoMenu::new));

    private PorongunMenus() {}

    public static void register(IEventBus modBus) {
        MENU_TYPES.register(modBus);
    }
}
