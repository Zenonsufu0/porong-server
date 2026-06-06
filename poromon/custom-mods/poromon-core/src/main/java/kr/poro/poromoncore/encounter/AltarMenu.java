package kr.poro.poromoncore.encounter;

import kr.poro.poromoncore.config.ConfigManager;
import kr.poro.poromoncore.config.EconomyConfig.ShopEntry;
import kr.poro.poromoncore.config.EncounterConfig;
import kr.poro.poromoncore.data.PoroMonState;
import kr.poro.poromoncore.economy.EconomyBridge;
import kr.poro.poromoncore.menu.MenuGuiManager;
import kr.poro.poromoncore.menu.MenuIcons;
import kr.poro.poromoncore.menu.ServerMenuHandler;
import kr.poro.poromoncore.shop.ShopLayout;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 전설 제단 메뉴 (결정 030: 가상 제단 = 메뉴 슬롯37). 조우권을 골드로 즉시 구매·사용.
 * 등급/컨셉 풀 클릭 → 배지 게이트 + 골드 차감 → EncounterService 개인 조우방 소환.
 * 가격/게이트 = economy.json ticketPrices, 풀 = legendary_pools.json.
 */
public final class AltarMenu {
    private AltarMenu() {}

    public static void open(ServerPlayerEntity player) {
        ServerMenuHandler.show(player, Text.literal("전설 제단").formatted(Formatting.DARK_PURPLE),
                inv -> populate(inv, player), AltarMenu::onClick);
    }

    private static int badgeCount(ServerPlayerEntity player) {
        return PoroMonState.get(player.getServer()).getOrCreate(player.getUuid()).badges.size();
    }

    /** ticketPrices 순서 = 제단 진열 순서. */
    private static List<String> pools() {
        return new ArrayList<>(ConfigManager.economy().ticketPrices.keySet());
    }

    private static void populate(Inventory inv, ServerPlayerEntity player) {
        for (int i = 0; i < ServerMenuHandler.DISPLAY_SIZE; i++) inv.setStack(i, MenuIcons.pane());
        String unit = ConfigManager.economy().currencyDisplay;
        int badges = badgeCount(player);

        inv.setStack(ShopLayout.BALANCE_SLOT, MenuIcons.icon(Items.GOLD_NUGGET,
                "§6잔액: " + EconomyBridge.getBalance(player) + " " + unit,
                List.of("§7클릭 — 조우권 구매 후 개인 조우방으로", "§7보유 배지: §f" + badges)));
        inv.setStack(ShopLayout.BACK_SLOT, MenuIcons.icon(Items.ARROW, "§e← 메뉴로", List.of()));

        List<String> list = pools();
        Map<String, ShopEntry> prices = ConfigManager.economy().ticketPrices;
        for (int i = 0; i < list.size() && i < ShopLayout.CONTENT_SLOTS.length; i++) {
            String poolId = list.get(i);
            ShopEntry se = prices.get(poolId);
            EncounterConfig.Pool pool = ConfigManager.encounter().pools.get(poolId);
            if (se == null || pool == null) continue;
            boolean locked = badges < se.minBadges;
            List<String> lore = new ArrayList<>();
            lore.add("§7가격: §6" + se.price + " " + unit);
            lore.add("§7요구 배지: §f" + se.minBadges);
            lore.add("§7예시: §f" + examples(pool));
            if (locked) {
                lore.add("§c배지 " + se.minBadges + "개 필요 (현재 " + badges + ")");
                inv.setStack(ShopLayout.CONTENT_SLOTS[i], MenuIcons.icon(Items.IRON_BARS,
                        "§8" + pool.displayNameKo + " §7(잠김)", lore));
            } else {
                lore.add("§a클릭 — 조우권 사용");
                inv.setStack(ShopLayout.CONTENT_SLOTS[i], MenuIcons.icon(icon(pool.type),
                        "§d" + pool.displayNameKo, lore));
            }
        }
    }

    private static void onClick(ServerPlayerEntity player, int slot, int button, boolean shift) {
        if (slot == ShopLayout.BACK_SLOT) { MenuGuiManager.open(player); return; }
        int idx = ShopLayout.contentIndexOf(slot);
        if (idx < 0) return;
        List<String> list = pools();
        if (idx >= list.size()) return;
        String poolId = list.get(idx);
        ShopEntry se = ConfigManager.economy().ticketPrices.get(poolId);
        if (se == null) return;

        if (badgeCount(player) < se.minBadges) {
            player.sendMessage(Text.literal("§c[제단] 배지 " + se.minBadges + "개가 필요합니다."), true);
            return;
        }
        if (EncounterService.isInEncounter(player)) {
            player.sendMessage(Text.literal("§e[제단] 이미 조우 중입니다."), true);
            return;
        }
        if (!EconomyBridge.withdraw(player, se.price, "ticket:" + poolId)) {
            player.sendMessage(Text.literal("§c[제단] 골드가 부족합니다 (필요 " + se.price + ")."), true);
            return;
        }
        player.closeHandledScreen();
        boolean ok = EncounterService.start(player, poolId, false);
        if (!ok) { // 소환 실패 시 환불
            EconomyBridge.deposit(player, se.price, "ticket_refund:" + poolId);
        }
    }

    private static String examples(EncounterConfig.Pool pool) {
        List<String> names = new ArrayList<>();
        for (EncounterConfig.Candidate c : pool.candidates) {
            if (c.enabled) { names.add(c.displayNameKo); if (names.size() >= 3) break; }
        }
        return names.isEmpty() ? "-" : String.join(", ", names) + " 등";
    }

    private static Item icon(String type) {
        if (type == null) return Items.ENDER_EYE;
        return switch (type) {
            case "rare" -> Items.SLIME_BALL;
            case "basic" -> Items.IRON_INGOT;
            case "intermediate" -> Items.GOLD_INGOT;
            case "advanced" -> Items.DIAMOND;
            case "apex" -> Items.NETHER_STAR;
            case "theme" -> Items.END_CRYSTAL;
            default -> Items.ENDER_EYE;
        };
    }
}
