package kr.zenon.gun.shop;

import kr.zenon.gun.registry.ZenonGunMenus;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * 상점 메뉴 (M-Currency). 품목 목록은 {@link ShopCatalog}, 거래는 {@link #clickMenuButton}.
 *
 * 슬롯 = 플레이어 인벤(이동/보관)만. 구매·매입은 슬롯이 아니라 화면의 품목별 버튼 →
 * {@code clickMenuButton(buttonId)} 서버 처리(buttonId = index*2 + (구매?0:1)).
 */
public class ShopMenu extends AbstractContainerMenu {

    /** 플레이어 인벤 영역 시작 Y(화면 상단=품목 목록, 하단=인벤). Screen과 공유.
     *  품목 8행(16px) + 안내문 아래에 위치하도록 충분히 내림. */
    public static final int PLAYER_INV_Y = 162;

    public ShopMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv);
    }

    public ShopMenu(int id, Inventory inv) {
        super(ZenonGunMenus.SHOP.get(), id);
        addPlayerSlots(inv);
    }

    private void addPlayerSlots(Inventory inv) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, PLAYER_INV_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inv, col, 8 + col * 18, PLAYER_INV_Y + 58));
        }
    }

    @Override
    public boolean clickMenuButton(Player player, int buttonId) {
        int index = buttonId / 2;
        boolean buy = buttonId % 2 == 0;
        if (index < 0 || index >= ShopCatalog.ENTRIES.size()) return false;

        ShopEntry entry = ShopCatalog.ENTRIES.get(index);
        Item item = BuiltInRegistries.ITEM.get(entry.item());

        if (buy) {
            if (!entry.canBuy()) return false;
            if (CurrencyUtil.take(player, entry.buyPrice())) {
                ItemStack give = new ItemStack(item);
                if (!player.getInventory().add(give)) {
                    player.drop(give, false);
                }
                feedback(player, "§a구매: " + item.getDescription().getString()
                        + " (-" + entry.buyPrice() + " 코인 / 잔액 " + CurrencyUtil.balance(player) + ")");
            } else {
                feedback(player, "§c코인 부족 (필요 " + entry.buyPrice()
                        + " / 보유 " + CurrencyUtil.balance(player) + ")");
            }
        } else {
            if (!entry.canSell()) return false;
            if (removeOne(player, item)) {
                CurrencyUtil.give(player, entry.sellPrice());
                feedback(player, "§e매입: " + item.getDescription().getString()
                        + " (+" + entry.sellPrice() + " 코인 / 잔액 " + CurrencyUtil.balance(player) + ")");
            } else {
                feedback(player, "§c팔 아이템이 없음");
            }
        }
        return true;
    }

    private static boolean removeOne(Player player, Item item) {
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.items.size(); i++) {
            ItemStack s = inv.items.get(i);
            if (s.is(item)) {
                s.shrink(1);
                return true;
            }
        }
        return false;
    }

    private static void feedback(Player player, String text) {
        player.displayClientMessage(Component.literal(text), true); // 액션바
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY; // 상점 슬롯 없음 — 인벤 내 shift 이동 불필요
    }

    @Override
    public boolean stillValid(Player player) {
        return true; // 거점 GUI(코어 우클릭) — 거리 검사 후속(영역 이탈 시 닫기)
    }
}
