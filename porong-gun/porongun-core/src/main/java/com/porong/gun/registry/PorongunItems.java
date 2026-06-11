package com.porong.gun.registry;

import com.porong.gun.PorongunCore;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * 커스텀 아이템 레지스트리 (M-Material — economy #1-B 재료 3층 트리).
 *
 * 재료 4종(전부 순수 Item):
 *  - ⚙️ 텅스텐 합금  : 철9+구리9 (gun_smith_table). 소총탄 탄두·총 프레임.
 *  - ☢️ 열화우라늄    : 텅스텐4+다이아4. 저격·대물 AP탄·고급 총.
 *  - 🪖 군용 합금    : drop-only(POI·스캐브). 고급 총·방어구 게이트.
 *  - 🔌 전자 부품    : drop-only(POI·스캐브). AP탄·결전 부품 게이트.
 *
 * 1단계=아이템 등록만. 레시피(gun_smith_table 오버라이드)·드랍(M-POI/M-Scav)은 후속.
 */
public final class PorongunItems {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, PorongunCore.MODID);

    public static final RegistryObject<Item> TUNGSTEN_ALLOY =
            ITEMS.register("tungsten_alloy", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> DEPLETED_URANIUM =
            ITEMS.register("depleted_uranium", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> MILITARY_ALLOY =
            ITEMS.register("military_alloy", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> ELECTRONIC_PART =
            ITEMS.register("electronic_part", () -> new Item(new Item.Properties()));

    /** 거점 코어 설치 아이템(M-Core). 우클릭 설치 → CoreEvents 검증·등록. */
    public static final RegistryObject<Item> CORE =
            ITEMS.register("core", () -> new BlockItem(PorongunBlocks.CORE.get(), new Item.Properties()));

    private PorongunItems() {}

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }
}
