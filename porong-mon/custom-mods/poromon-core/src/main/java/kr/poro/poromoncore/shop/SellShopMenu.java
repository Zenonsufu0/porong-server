package kr.poro.poromoncore.shop;

import kr.poro.poromoncore.config.ConfigManager;
import kr.poro.poromoncore.economy.EconomyBridge;
import kr.poro.poromoncore.menu.MenuGuiManager;
import kr.poro.poromoncore.menu.MenuIcons;
import kr.poro.poromoncore.menu.ServerMenuHandler;
import net.minecraft.entity.player.PlayerInventory;
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
 * 매입소 GUI (menu_design.md §4.1, 결정 024: 9번 메뉴=매입·어디서나).
 * 인벤토리의 판매 가능 품목(광물·농작물·베리)을 골드로 일괄 매입.
 * 진열 아이템 클릭 = 해당 종류 전부 판매 / 전부 팔기 버튼 = 모든 판매가능 품목.
 * 가격은 economy.json sellPrices 단일 출처.
 */
public final class SellShopMenu {
    private SellShopMenu() {}

    private record Holding(String itemId, Item item, int count, long unitPrice) {}

    public static void open(ServerPlayerEntity player) {
        ServerMenuHandler.show(player,
                Text.literal("매입소").formatted(Formatting.GOLD),
                inv -> populate(inv, player), SellShopMenu::onClick);
    }

    /** 현재 인벤토리에서 판매 가능한 품목(sellPrices 순서, 보유 수량>0). */
    private static List<Holding> holdings(ServerPlayerEntity player) {
        PlayerInventory inv = player.getInventory();
        List<Holding> out = new ArrayList<>();
        for (Map.Entry<String, Long> e : ConfigManager.economy().sellPrices.entrySet()) {
            Identifier id = Identifier.tryParse(e.getKey());
            if (id == null || !Registries.ITEM.containsId(id)) continue;
            Item item = Registries.ITEM.get(id);
            int count = countOf(inv, id);
            if (count > 0) out.add(new Holding(e.getKey(), item, count, e.getValue()));
        }
        return out;
    }

    private static void populate(Inventory inv, ServerPlayerEntity player) {
        for (int i = 0; i < ServerMenuHandler.DISPLAY_SIZE; i++) inv.setStack(i, MenuIcons.pane());

        String unit = ConfigManager.economy().currencyDisplay;
        List<Holding> list = holdings(player);
        long grand = 0;
        for (Holding h : list) grand += h.count * h.unitPrice;

        inv.setStack(ShopLayout.BALANCE_SLOT, MenuIcons.icon(Items.GOLD_NUGGET,
                "§6잔액: " + EconomyBridge.getBalance(player) + " " + unit,
                List.of("§7판매 가능 합계: §6" + grand + " " + unit)));
        inv.setStack(ShopLayout.BACK_SLOT, MenuIcons.icon(Items.ARROW, "§e← 메뉴로", List.of()));
        inv.setStack(ShopLayout.SELL_ALL_SLOT, MenuIcons.icon(Items.HOPPER, "§a전부 팔기",
                List.of("§7판매 가능한 모든 품목 매각", "§7예상 수익: §6" + grand + " " + unit)));

        if (list.isEmpty()) {
            inv.setStack(ShopLayout.CONTENT_SLOTS[0], MenuIcons.icon(Items.BARRIER,
                    "§c판매 가능한 아이템 없음",
                    List.of("§7광물·농작물·베리를 모아 오세요")));
            return;
        }
        for (int i = 0; i < list.size() && i < ShopLayout.CONTENT_SLOTS.length; i++) {
            Holding h = list.get(i);
            inv.setStack(ShopLayout.CONTENT_SLOTS[i], MenuIcons.iconCount(h.item, h.count,
                    MenuIcons.named(Formatting.WHITE, h.item.getName()),
                    List.of("§7보유: §f" + h.count + "개",
                            "§7개당: §6" + h.unitPrice + " " + unit,
                            "§7합계: §6" + (h.count * h.unitPrice) + " " + unit,
                            "§a클릭 — 이 품목 전부 판매")));
        }
    }

    private static void onClick(ServerPlayerEntity player, int slot, int button, boolean shift) {
        if (slot == ShopLayout.BACK_SLOT) {
            MenuGuiManager.open(player);
            return;
        }
        if (slot == ShopLayout.SELL_ALL_SLOT) {
            sellAll(player);
            open(player);
            return;
        }
        int idx = ShopLayout.contentIndexOf(slot);
        if (idx < 0) return;
        List<Holding> list = holdings(player);
        if (idx >= list.size()) return;
        Holding h = list.get(idx);
        long gained = sellType(player, h.itemId, h.unitPrice);
        if (gained > 0) {
            player.sendMessage(Text.literal("§a[매입소] ").append(h.item.getName())
                    .append(Text.literal(" §a판매 (+" + gained + ").")), true);
        }
        open(player); // show()=재오픈 없이 진열·잔액 갱신(커서 유지)
    }

    private static void sellAll(ServerPlayerEntity player) {
        long total = 0;
        for (Map.Entry<String, Long> e : ConfigManager.economy().sellPrices.entrySet()) {
            total += sellType(player, e.getKey(), e.getValue());
        }
        player.sendMessage(Text.literal(total > 0
                ? "§a[매입소] 전부 판매 완료 (+" + total + ")."
                : "§e[매입소] 판매할 품목이 없습니다."), true);
    }

    /** 지정 품목을 인벤토리에서 전부 제거하고 골드 지급. 지급액 반환. */
    private static long sellType(ServerPlayerEntity player, String itemId, long unitPrice) {
        Identifier id = Identifier.tryParse(itemId);
        if (id == null) return 0;
        PlayerInventory inv = player.getInventory();
        int count = 0;
        for (int i = 0; i < inv.size(); i++) {
            ItemStack s = inv.getStack(i);
            if (s.isEmpty()) continue;
            if (Registries.ITEM.getId(s.getItem()).equals(id)) {
                count += s.getCount();
                inv.setStack(i, ItemStack.EMPTY);
            }
        }
        if (count <= 0) return 0;
        long gold = Math.round(count * unitPrice * kr.poro.poromoncore.event.EventManager.goldMultiplier());
        EconomyBridge.deposit(player, gold, "sell:" + itemId);
        return gold;
    }

    private static int countOf(PlayerInventory inv, Identifier id) {
        int count = 0;
        for (int i = 0; i < inv.size(); i++) {
            ItemStack s = inv.getStack(i);
            if (!s.isEmpty() && Registries.ITEM.getId(s.getItem()).equals(id)) count += s.getCount();
        }
        return count;
    }
}
