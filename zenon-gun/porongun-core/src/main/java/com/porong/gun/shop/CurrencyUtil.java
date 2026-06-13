package com.porong.gun.shop;

import com.porong.gun.registry.PorongunItems;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * 화폐(코인) 잔액·차감·지급 (M-Currency).
 *
 * 화폐 = 물리 아이템이라 잔액 = 인벤 내 코인 합산. 거래는 인벤에서 직접 차감/지급
 * (economy 「거래 = 인벤 화폐 차감」). 보안칸 코인도 인벤의 일부라 합산 대상(후속 M-Inv 통합).
 */
public final class CurrencyUtil {

    private CurrencyUtil() {}

    /** 인벤 내 코인 총합. */
    public static int balance(Player player) {
        int total = 0;
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.items.size(); i++) {
            ItemStack s = inv.items.get(i);
            if (s.is(PorongunItems.COIN.get())) {
                total += s.getCount();
            }
        }
        return total;
    }

    /** 코인 amount 차감. 잔액 부족 시 false(차감 안 함). */
    public static boolean take(Player player, int amount) {
        if (amount <= 0) return true;
        if (balance(player) < amount) return false;
        int remaining = amount;
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.items.size() && remaining > 0; i++) {
            ItemStack s = inv.items.get(i);
            if (s.is(PorongunItems.COIN.get())) {
                int take = Math.min(remaining, s.getCount());
                s.shrink(take);
                remaining -= take;
            }
        }
        return true;
    }

    /** 코인 amount 지급(스택 한도로 분할, 인벤 꽉 차면 바닥에 드랍). */
    public static void give(Player player, int amount) {
        int max = PorongunItems.COIN.get().getMaxStackSize(new ItemStack(PorongunItems.COIN.get()));
        while (amount > 0) {
            int n = Math.min(amount, max);
            ItemStack coins = new ItemStack(PorongunItems.COIN.get(), n);
            if (!player.getInventory().add(coins)) {
                player.drop(coins, false);
            }
            amount -= n;
        }
    }
}
