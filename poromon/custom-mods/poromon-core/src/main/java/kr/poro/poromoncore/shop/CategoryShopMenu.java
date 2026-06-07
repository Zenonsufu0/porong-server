package kr.poro.poromoncore.shop;

import com.cobblemon.mod.common.api.moves.MoveTemplate;
import com.cobblemon.mod.common.api.moves.Moves;
import kr.poro.poromoncore.config.ConfigManager;
import kr.poro.poromoncore.config.EconomyConfig.ShopEntry;
import kr.poro.poromoncore.data.PoroMonState;
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
 * 공용 카테고리 구매 상점 (결정 030). 가격 + 배지 게이트(minBadges) + 페이지네이션.
 * 좌클릭 1개 / 우클릭 8개. 서버 권위 트랜잭션. (제목, 가격맵, 출처태그)만 바꿔 재사용.
 */
public final class CategoryShopMenu {
    private CategoryShopMenu() {}

    private static final int BUY_RIGHT_QTY = 8;
    private static final int PER_PAGE = ShopLayout.CONTENT_SLOTS.length; // 28

    public static void open(ServerPlayerEntity player, Text title, Map<String, ShopEntry> entries, String tag) {
        open(player, title, entries, tag, 0);
    }

    public static void open(ServerPlayerEntity player, Text title, Map<String, ShopEntry> entries,
                            String tag, int page) {
        ServerMenuHandler.show(player, title,
                inv -> populate(inv, player, entries, page),
                (p, slot, button, shift) -> onClick(p, slot, button, title, entries, tag, page));
    }

    private static int badgeCount(ServerPlayerEntity player) {
        return PoroMonState.get(player.getServer()).getOrCreate(player.getUuid()).badges.size();
    }

    private static void populate(Inventory inv, ServerPlayerEntity player, Map<String, ShopEntry> entries, int page) {
        for (int i = 0; i < ServerMenuHandler.DISPLAY_SIZE; i++) inv.setStack(i, MenuIcons.pane());
        String unit = ConfigManager.economy().currencyDisplay;
        int badges = badgeCount(player);
        List<Map.Entry<String, ShopEntry>> list = new ArrayList<>(entries.entrySet());
        int pages = Math.max(1, (list.size() + PER_PAGE - 1) / PER_PAGE);
        page = Math.max(0, Math.min(page, pages - 1));

        inv.setStack(ShopLayout.BALANCE_SLOT, MenuIcons.icon(Items.GOLD_NUGGET,
                "§6잔액: " + EconomyBridge.getBalance(player) + " " + unit,
                List.of("§7좌클릭 = 1개 §8| §7우클릭 = " + BUY_RIGHT_QTY + "개",
                        "§7보유 배지: §f" + badges,
                        "§7페이지 §f" + (page + 1) + " / " + pages)));
        inv.setStack(ShopLayout.BACK_SLOT, MenuIcons.icon(Items.ARROW, "§e← 메뉴로", List.of()));
        if (page > 0) inv.setStack(ShopLayout.PREV_SLOT, MenuIcons.icon(Items.SPECTRAL_ARROW, "§e◀ 이전 페이지", List.of()));
        if (page < pages - 1) inv.setStack(ShopLayout.NEXT_SLOT, MenuIcons.icon(Items.SPECTRAL_ARROW, "§e다음 페이지 ▶", List.of()));

        int start = page * PER_PAGE;
        for (int i = 0; i < PER_PAGE && start + i < list.size(); i++) {
            Map.Entry<String, ShopEntry> e = list.get(start + i);
            Item item = resolve(e.getKey());
            if (item == null) continue;
            ShopEntry se = e.getValue();
            if (badges < se.minBadges) {
                inv.setStack(ShopLayout.CONTENT_SLOTS[i], MenuIcons.icon(Items.IRON_BARS,
                        MenuIcons.named(Formatting.DARK_GRAY, shopName(item, e.getKey()))
                                .append(Text.literal(" (잠김)").formatted(Formatting.GRAY)),
                        List.of("§7가격: §6" + se.price + " " + unit,
                                "§c배지 " + se.minBadges + "개 필요 §7(현재 " + badges + ")")));
            } else {
                inv.setStack(ShopLayout.CONTENT_SLOTS[i], MenuIcons.icon(item,
                        MenuIcons.named(Formatting.WHITE, shopName(item, e.getKey())),
                        List.of("§7가격: §6" + se.price + " " + unit + "§7/개",
                                "§a좌클릭 1개 §8| §a우클릭 " + BUY_RIGHT_QTY + "개")));
            }
        }
    }

    private static void onClick(ServerPlayerEntity player, int slot, int button,
                               Text title, Map<String, ShopEntry> entries, String tag, int page) {
        if (slot == ShopLayout.BACK_SLOT) { MenuGuiManager.open(player); return; }
        if (slot == ShopLayout.PREV_SLOT) { open(player, title, entries, tag, page - 1); return; }
        if (slot == ShopLayout.NEXT_SLOT) { open(player, title, entries, tag, page + 1); return; }

        int idx = ShopLayout.contentIndexOf(slot);
        if (idx < 0) return;
        List<Map.Entry<String, ShopEntry>> list = new ArrayList<>(entries.entrySet());
        int globalIdx = page * PER_PAGE + idx;
        if (globalIdx >= list.size()) return;

        Map.Entry<String, ShopEntry> e = list.get(globalIdx);
        ShopEntry se = e.getValue();
        if (badgeCount(player) < se.minBadges) {
            player.sendMessage(Text.literal("§c[상점] 배지 " + se.minBadges + "개가 필요합니다."), true);
            return;
        }
        int qty = (button == 1) ? BUY_RIGHT_QTY : 1;
        buy(player, e.getKey(), se.price, qty, tag);
        open(player, title, entries, tag, page); // 같은 페이지 갱신(in-place)
    }

    private static void buy(ServerPlayerEntity player, String itemId, long unitPrice, int qty, String tag) {
        Item item = resolve(itemId);
        if (item == null) {
            player.sendMessage(Text.literal("§c[상점] 알 수 없는 품목: " + itemId), true);
            return;
        }
        long total = unitPrice * qty;
        if (!EconomyBridge.withdraw(player, total, tag + ":" + itemId)) {
            player.sendMessage(Text.literal("§c[상점] 골드가 부족합니다 (필요 " + total + ")."), true);
            return;
        }
        ItemStack stack = new ItemStack(item, qty);
        player.getInventory().insertStack(stack);
        if (!stack.isEmpty()) {
            int left = stack.getCount();
            EconomyBridge.deposit(player, unitPrice * left, tag + "_refund:" + itemId);
            player.sendMessage(Text.literal("§e[상점] 인벤토리 공간 부족 — " + left + "개 환불."), true);
        }
        int bought = qty - stack.getCount();
        if (bought > 0) {
            player.sendMessage(Text.literal("§a[상점] ").append(shopName(item, itemId))
                    .append(Text.literal(" §a" + bought + "개 구매 (-" + (unitPrice * bought) + ").")), true);
        }
    }

    /**
     * 표시 이름. SimpleTMs TM 아이템(`simpletms:tm_<move>`)은 개별 번역키가 없어 raw 키/영어로 뜨므로
     * Cobblemon 기술명(MoveTemplate, ko_kr 자동)으로 대체. 그 외는 아이템 기본 이름(translatable).
     */
    private static Text shopName(Item item, String itemId) {
        if (itemId.startsWith("simpletms:tm_")) {
            MoveTemplate tpl = Moves.getByName(itemId.substring("simpletms:tm_".length()));
            if (tpl != null) return tpl.getDisplayName();
        }
        return item.getName();
    }

    private static Item resolve(String itemId) {
        Identifier id = Identifier.tryParse(itemId);
        if (id == null || !Registries.ITEM.containsId(id)) return null;
        return Registries.ITEM.get(id);
    }
}
