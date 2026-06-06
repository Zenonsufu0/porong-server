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

    // 레이아웃(행 분리): 1행=5등급, 3·4행=컨셉 10(5+5)
    private static final String[] TIER_POOLS = {
            "rare_encounter_pool", "basic_legendary_ticket_pool", "intermediate_legendary_ticket_pool",
            "advanced_legendary_ticket_pool", "apex_legendary_ticket_pool"};
    private static final int[] TIER_SLOTS = {11, 12, 13, 14, 15};
    private static final int[] CONCEPT_SLOTS = {29, 30, 31, 32, 33, 38, 39, 40, 41, 42};

    public static void open(ServerPlayerEntity player) {
        ServerMenuHandler.show(player, Text.literal("전설 제단").formatted(Formatting.DARK_PURPLE),
                inv -> populate(inv, player), AltarMenu::onClick);
    }

    private static PlayerProgress progress(ServerPlayerEntity p) {
        return PoroMonState.get(p.getServer()).getOrCreate(p.getUuid());
    }

    /** 슬롯 → poolId 매핑(등급 5 + 컨셉 10). populate·onClick 공용. */
    private static java.util.Map<Integer, String> slotMap() {
        java.util.Map<Integer, String> m = new java.util.LinkedHashMap<>();
        var pools = ConfigManager.encounter().pools;
        for (int i = 0; i < TIER_POOLS.length; i++) {
            if (pools.containsKey(TIER_POOLS[i])) m.put(TIER_SLOTS[i], TIER_POOLS[i]);
        }
        List<String> concepts = new ArrayList<>();
        for (var e : pools.entrySet()) if ("theme".equals(e.getValue().type)) concepts.add(e.getKey());
        for (int i = 0; i < concepts.size() && i < CONCEPT_SLOTS.length; i++) {
            m.put(CONCEPT_SLOTS[i], concepts.get(i));
        }
        return m;
    }

    private static void populate(Inventory inv, ServerPlayerEntity player) {
        for (int i = 0; i < ServerMenuHandler.DISPLAY_SIZE; i++) inv.setStack(i, MenuIcons.pane());
        String unit = ConfigManager.economy().currencyDisplay;
        PlayerProgress pr = progress(player);
        int badges = pr.badges.size();

        inv.setStack(ShopLayout.BALANCE_SLOT, MenuIcons.icon(Items.GOLD_NUGGET,
                "§6잔액: " + EconomyBridge.getBalance(player) + " " + unit,
                List.of("§7윗줄=등급, 아랫줄=컨셉. 제단 해금 후 조우권 사용", "§7보유 배지: §f" + badges)));
        inv.setStack(ShopLayout.BACK_SLOT, MenuIcons.icon(Items.ARROW, "§e← 메뉴로", List.of()));

        for (var en : slotMap().entrySet()) {
            int slot = en.getKey();
            String poolId = en.getValue();
            EncounterConfig.Pool pool = ConfigManager.encounter().pools.get(poolId);
            if (pool == null) continue;
            String tier = pool.type;
            String unlockKey = unlockKey(tier, poolId);
            boolean unlocked = pr.altarsUnlocked.contains(unlockKey);
            ShopEntry unlock = ConfigManager.economy().altarUnlock.get(unlockKey);
            Long use = ConfigManager.economy().ticketUse.get(tier);
            long usePrice = use == null ? 0 : use;

            List<String> lore = new ArrayList<>();
            lore.add("§7예시: §f" + examples(pool));
            lore.add("§8우클릭 — 출현 확률 보기");
            if (unlocked) {
                lore.add("§a제단 해금됨 ✔");
                lore.add("§7조우권: §6" + usePrice + " " + unit);
                lore.add("§a클릭 — 조우권 사용 §7(이로치 " + ConfigManager.economy().shinyChancePercent + "%)");
                inv.setStack(slot, MenuIcons.iconModel(Items.PAPER, cmdFor(tier, poolId), "§d" + pool.displayNameKo, lore));
            } else {
                int needBadge = unlock == null ? 0 : unlock.minBadges;
                long unlockPrice = unlock == null ? 0 : unlock.price;
                boolean canUnlock = badges >= needBadge;
                lore.add("§7제단 해금: §6" + unlockPrice + " " + unit + " §7(배지 " + needBadge + ")");
                lore.add(canUnlock ? "§e클릭 — 제단 해금" : "§c배지 " + needBadge + "개 필요 (현재 " + badges + ")");
                inv.setStack(slot, MenuIcons.icon(canUnlock ? Items.CHAIN : Items.IRON_BARS,
                        (canUnlock ? "§e" : "§8") + pool.displayNameKo + " §7(제단 잠김)", lore));
            }
        }
    }

    private static void onClick(ServerPlayerEntity player, int slot, int button, boolean shift) {
        if (slot == ShopLayout.BACK_SLOT) { MenuGuiManager.open(player); return; }
        String poolId = slotMap().get(slot);
        if (poolId == null) return;
        EncounterConfig.Pool pool = ConfigManager.encounter().pools.get(poolId);
        if (pool == null) return;
        if (button == 1) { PoolInfoMenu.open(player, poolId); return; } // 우클릭 = 확률 상세
        String tier = pool.type;
        String unlockKey = unlockKey(tier, poolId);
        PlayerProgress pr = progress(player);

        if (!pr.altarsUnlocked.contains(unlockKey)) {
            // 제단 해금 (컨셉=개별 poolId, 일반=tier)
            ShopEntry unlock = ConfigManager.economy().altarUnlock.get(unlockKey);
            if (unlock == null) return;
            if (pr.badges.size() < unlock.minBadges) {
                player.sendMessage(Text.literal("§c[제단] 배지 " + unlock.minBadges + "개가 필요합니다."), true);
                return;
            }
            if (!EconomyBridge.withdraw(player, unlock.price, "altar_unlock:" + unlockKey)) {
                player.sendMessage(Text.literal("§c[제단] 골드가 부족합니다 (필요 " + unlock.price + ")."), true);
                return;
            }
            pr.altarsUnlocked.add(unlockKey);
            PoroMonState.get(player.getServer()).markDirty();
            player.sendMessage(Text.literal("§a[제단] " + pool.displayNameKo + " 제단을 해금했습니다! 이제 조우권을 사용할 수 있습니다."), false);
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

    /** 해금 단위 키: 컨셉(theme)=개별 poolId, 일반 등급=tier(type). */
    private static String unlockKey(String tier, String poolId) {
        return "theme".equals(tier) ? poolId : tier;
    }

    private static String examples(EncounterConfig.Pool pool) {
        List<String> names = new ArrayList<>();
        for (EncounterConfig.Candidate c : pool.candidates) {
            if (c.enabled) { names.add(c.displayNameKo); if (names.size() >= 3) break; }
        }
        return names.isEmpty() ? "-" : String.join(", ", names) + " 등";
    }

    /** 조우권 텍스처 CustomModelData (paper override). 등급=type, 컨셉=theme suffix. */
    private static int cmdFor(String tier, String poolId) {
        if ("theme".equals(tier)) {
            String k = poolId.replace("theme_", "").replace("_pool", "");
            return switch (k) {
                case "sky" -> 82011; case "deep_sea" -> 82012; case "earth" -> 82013;
                case "time" -> 82014; case "space" -> 82015; case "reverse" -> 82016;
                case "light" -> 82017; case "dragon_king" -> 82018; case "guardian" -> 82019;
                case "eternity" -> 82020; default -> 82011;
            };
        }
        return switch (tier) {
            case "rare" -> 82001; case "basic" -> 82002; case "intermediate" -> 82003;
            case "advanced" -> 82004; case "apex" -> 82005; default -> 82001;
        };
    }
}
