package kr.poro.poromoncore.encounter;

import kr.poro.poromoncore.config.ConfigManager;
import kr.poro.poromoncore.config.EconomyConfig.ShopEntry;
import kr.poro.poromoncore.config.EncounterConfig;
import kr.poro.poromoncore.data.PlayerProgress;
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

/**
 * 전설 제단 (결정 030 가상 슬롯37 + 결정 031 선행 해금).
 * 등급(tier) 제단을 골드+배지로 1회 해금 → 그 등급 조우권을 반복 사용(저가). 사용 시 이로치 5%.
 * 가격: economy.json altarUnlock(해금)·ticketUse(사용). 풀: legendary_pools.json.
 */
public final class AltarMenu {
    private AltarMenu() {}

    public static void open(ServerPlayerEntity player) {
        ServerMenuHandler.show(player, Text.literal("전설 제단").formatted(Formatting.DARK_PURPLE),
                inv -> populate(inv, player), AltarMenu::onClick);
    }

    private static PlayerProgress progress(ServerPlayerEntity p) {
        return PoroMonState.get(p.getServer()).getOrCreate(p.getUuid());
    }

    /** 진열 순서 = legendary_pools.json 풀 순서. */
    private static List<String> pools() {
        return new ArrayList<>(ConfigManager.encounter().pools.keySet());
    }

    private static void populate(Inventory inv, ServerPlayerEntity player) {
        for (int i = 0; i < ServerMenuHandler.DISPLAY_SIZE; i++) inv.setStack(i, MenuIcons.pane());
        String unit = ConfigManager.economy().currencyDisplay;
        PlayerProgress pr = progress(player);
        int badges = pr.badges.size();

        inv.setStack(ShopLayout.BALANCE_SLOT, MenuIcons.icon(Items.GOLD_NUGGET,
                "§6잔액: " + EconomyBridge.getBalance(player) + " " + unit,
                List.of("§7등급 제단을 먼저 해금해야 조우권 사용 가능", "§7보유 배지: §f" + badges)));
        inv.setStack(ShopLayout.BACK_SLOT, MenuIcons.icon(Items.ARROW, "§e← 메뉴로", List.of()));

        List<String> list = pools();
        int slotIdx = 0;
        for (String poolId : list) {
            if (poolId.equals("field_event_legendary_pool")) continue; // 제단 판매 제외
            if (slotIdx >= ShopLayout.CONTENT_SLOTS.length) break;
            EncounterConfig.Pool pool = ConfigManager.encounter().pools.get(poolId);
            if (pool == null) continue;
            String tier = pool.type;
            boolean unlocked = pr.altarsUnlocked.contains(tier);
            ShopEntry unlock = ConfigManager.economy().altarUnlock.get(tier);
            Long use = ConfigManager.economy().ticketUse.get(tier);
            long usePrice = use == null ? 0 : use;

            List<String> lore = new ArrayList<>();
            lore.add("§7예시: §f" + examples(pool));
            if (unlocked) {
                lore.add("§a제단 해금됨 ✔");
                lore.add("§7조우권: §6" + usePrice + " " + unit);
                lore.add("§a클릭 — 조우권 사용 §7(이로치 " + ConfigManager.economy().shinyChancePercent + "%)");
                inv.setStack(ShopLayout.CONTENT_SLOTS[slotIdx], MenuIcons.icon(icon(tier),
                        "§d" + pool.displayNameKo, lore));
            } else {
                int needBadge = unlock == null ? 0 : unlock.minBadges;
                long unlockPrice = unlock == null ? 0 : unlock.price;
                boolean canUnlock = badges >= needBadge;
                lore.add("§7제단 해금: §6" + unlockPrice + " " + unit + " §7(배지 " + needBadge + ")");
                lore.add(canUnlock ? "§e클릭 — 제단 해금" : "§c배지 " + needBadge + "개 필요 (현재 " + badges + ")");
                inv.setStack(ShopLayout.CONTENT_SLOTS[slotIdx], MenuIcons.icon(
                        canUnlock ? Items.CHAIN : Items.IRON_BARS,
                        (canUnlock ? "§e" : "§8") + pool.displayNameKo + " §7(제단 잠김)", lore));
            }
            slotIdx++;
        }
    }

    private static void onClick(ServerPlayerEntity player, int slot, int button, boolean shift) {
        if (slot == ShopLayout.BACK_SLOT) { MenuGuiManager.open(player); return; }
        int contentIdx = ShopLayout.contentIndexOf(slot);
        if (contentIdx < 0) return;

        // 진열 순서(field_event 제외)와 동일하게 poolId 매핑
        List<String> shown = new ArrayList<>();
        for (String pid : pools()) if (!pid.equals("field_event_legendary_pool")) shown.add(pid);
        if (contentIdx >= shown.size()) return;
        String poolId = shown.get(contentIdx);
        EncounterConfig.Pool pool = ConfigManager.encounter().pools.get(poolId);
        if (pool == null) return;
        String tier = pool.type;
        PlayerProgress pr = progress(player);

        if (!pr.altarsUnlocked.contains(tier)) {
            // 제단 해금
            ShopEntry unlock = ConfigManager.economy().altarUnlock.get(tier);
            if (unlock == null) return;
            if (pr.badges.size() < unlock.minBadges) {
                player.sendMessage(Text.literal("§c[제단] 배지 " + unlock.minBadges + "개가 필요합니다."), true);
                return;
            }
            if (!EconomyBridge.withdraw(player, unlock.price, "altar_unlock:" + tier)) {
                player.sendMessage(Text.literal("§c[제단] 골드가 부족합니다 (필요 " + unlock.price + ")."), true);
                return;
            }
            pr.altarsUnlocked.add(tier);
            PoroMonState.get(player.getServer()).markDirty();
            player.sendMessage(Text.literal("§a[제단] " + pool.displayNameKo + " 등급 제단을 해금했습니다! 이제 조우권을 사용할 수 있습니다."), false);
            open(player); // 갱신
            return;
        }

        // 조우권 사용
        if (EncounterService.isInEncounter(player)) {
            player.sendMessage(Text.literal("§e[제단] 이미 조우 중입니다."), true);
            return;
        }
        Long use = ConfigManager.economy().ticketUse.get(tier);
        long price = use == null ? 0 : use;
        if (!EconomyBridge.withdraw(player, price, "ticket:" + tier)) {
            player.sendMessage(Text.literal("§c[제단] 골드가 부족합니다 (필요 " + price + ")."), true);
            return;
        }
        boolean shiny = player.getServerWorld().getRandom().nextInt(100) < ConfigManager.economy().shinyChancePercent;
        player.closeHandledScreen();
        boolean ok = EncounterService.start(player, poolId, shiny);
        if (!ok) EconomyBridge.deposit(player, price, "ticket_refund:" + tier);
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
