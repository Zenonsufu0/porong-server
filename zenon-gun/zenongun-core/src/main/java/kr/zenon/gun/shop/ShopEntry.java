package kr.zenon.gun.shop;

import net.minecraft.resources.ResourceLocation;

/**
 * 상점 품목 1줄 (M-Currency — economy 상점가표 근거).
 *
 * @param item      아이템 ID (zenongun/바닐라/런타임 모드 아이템 모두 ID로 참조 → config화 용이)
 * @param buyPrice  구매가(코인). 음수 = 구매 불가(고급 총·고급탄·비파밍 재료 = 게이트)
 * @param sellPrice 매입가(코인). 음수 = 매입 불가
 */
public record ShopEntry(ResourceLocation item, int buyPrice, int sellPrice) {

    public boolean canBuy() {
        return buyPrice >= 0;
    }

    public boolean canSell() {
        return sellPrice >= 0;
    }

    public static ShopEntry of(String id, int buy, int sell) {
        return new ShopEntry(new ResourceLocation(id), buy, sell);
    }
}
