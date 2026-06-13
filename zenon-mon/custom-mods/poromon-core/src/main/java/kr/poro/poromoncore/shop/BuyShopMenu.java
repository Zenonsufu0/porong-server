package kr.poro.poromoncore.shop;

import kr.poro.poromoncore.config.ConfigManager;
import kr.poro.poromoncore.economy.EconomyBridge;
import kr.poro.poromoncore.menu.MenuGuiManager;
import kr.poro.poromoncore.menu.MenuIcons;
import kr.poro.poromoncore.menu.ServerMenuHandler;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 편의 상점 GUI (menu_design.md §4.2, 결정 024: 9번 메뉴=편의·어디서나).
 * 볼·회복약을 골드로 구매. 좌클릭 1개 / 우클릭 8개. 서버 권위 트랜잭션.
 * 품목·가격은 economy.json buyPrices 단일 출처(제작 가능 → 편의·저비중).
 */
public final class BuyShopMenu {
    private BuyShopMenu() {}

    private static final int BUY_RIGHT_QTY = 8;

    public static void open(ServerPlayerEntity player) {
        ServerMenuHandler.show(player,
                Text.literal("편의 상점").formatted(Formatting.DARK_GREEN),
                inv -> populate(inv, player), BuyShopMenu::onClick);
    }

    /** economy.json buyPrices 순서대로 진열(LinkedHashMap = 안정적 순서). */
    private static List<Map.Entry<String, Long>> entries() {
        return new ArrayList<>(ConfigManager.economy().buyPrices.entrySet());
    }

    private static void populate(Inventory inv, ServerPlayerEntity player) {
        for (int i = 0; i < ServerMenuHandler.DISPLAY_SIZE; i++) inv.setStack(i, MenuIcons.pane());

        String unit = ConfigManager.economy().currencyDisplay;
        inv.setStack(ShopLayout.BALANCE_SLOT, MenuIcons.icon(Items.GOLD_NUGGET,
                "§6잔액: " + EconomyBridge.getBalance(player) + " " + unit,
                List.of("§7좌클릭 = 1개 구매", "§7우클릭 = " + BUY_RIGHT_QTY + "개 구매")));
        inv.setStack(ShopLayout.BACK_SLOT, MenuIcons.icon(Items.ARROW, "§e← 메뉴로", List.of()));

        List<Map.Entry<String, Long>> list = entries();
        for (int i = 0; i < list.size() && i < ShopLayout.CONTENT_SLOTS.length; i++) {
            Map.Entry<String, Long> e = list.get(i);
            Item item = resolve(e.getKey());
            if (item == null) continue;
            inv.setStack(ShopLayout.CONTENT_SLOTS[i], MenuIcons.icon(item,
                    MenuIcons.named(Formatting.WHITE, item.getName()),
                    List.of("§7가격: §6" + e.getValue() + " " + unit + "§7/개",
                            "§a좌클릭 1개 §8| §a우클릭 " + BUY_RIGHT_QTY + "개")));
        }
    }

    private static void onClick(ServerPlayerEntity player, int slot, int button, boolean shift) {
        if (slot == ShopLayout.BACK_SLOT) {
            MenuGuiManager.open(player);
            return;
        }
        int idx = ShopLayout.contentIndexOf(slot);
        if (idx < 0) return;
        List<Map.Entry<String, Long>> list = entries();
        if (idx >= list.size()) return;

        Map.Entry<String, Long> e = list.get(idx);
        int qty = (button == 1) ? BUY_RIGHT_QTY : 1;
        buy(player, e.getKey(), e.getValue(), qty);
        open(player); // show()=재오픈 없이 잔액·진열 갱신(커서 유지)
    }

    private static void buy(ServerPlayerEntity player, String itemId, long unitPrice, int qty) {
        Item item = resolve(itemId);
        if (item == null) {
            player.sendMessage(Text.literal("§c[상점] 알 수 없는 품목: " + itemId), true);
            return;
        }
        long total = unitPrice * qty;
        if (!EconomyBridge.withdraw(player, total, "buy:" + itemId)) {
            player.sendMessage(Text.literal("§c[상점] 골드가 부족합니다 (필요 " + total + ")."), true);
            return;
        }
        ItemStack stack = new ItemStack(item, qty);
        player.getInventory().insertStack(stack);
        if (!stack.isEmpty()) {
            // 인벤 공간 부족분 환불
            int left = stack.getCount();
            EconomyBridge.deposit(player, unitPrice * left, "buy_refund:" + itemId);
            player.sendMessage(Text.literal("§e[상점] 인벤토리 공간 부족 — " + left + "개 환불."), true);
        }
        int bought = qty - stack.getCount();
        if (bought > 0) {
            player.sendMessage(Text.literal("§a[상점] ").append(item.getName())
                    .append(Text.literal(" §a" + bought + "개 구매 (-" + (unitPrice * bought) + ").")), true);
        }
    }

    private static Item resolve(String itemId) {
        Identifier id = Identifier.tryParse(itemId);
        if (id == null || !Registries.ITEM.containsId(id)) return null;
        return Registries.ITEM.get(id);
    }
}
