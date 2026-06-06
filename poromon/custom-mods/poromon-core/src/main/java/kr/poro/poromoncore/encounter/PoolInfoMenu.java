package kr.poro.poromoncore.encounter;

import kr.poro.poromoncore.config.ConfigManager;
import kr.poro.poromoncore.config.EncounterConfig;
import kr.poro.poromoncore.menu.MenuIcons;
import kr.poro.poromoncore.menu.ServerMenuHandler;
import kr.poro.poromoncore.shop.ShopLayout;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;

/**
 * 조우권 풀 상세(후보 포켓몬 + 출현 확률%) — 읽기 전용. 제단에서 풀 우클릭으로 진입.
 * 확률 = 후보 weight / 활성 weight 합 × 100. (0.1: stage_weight 미적용 = weight 직접)
 */
public final class PoolInfoMenu {
    private PoolInfoMenu() {}

    private static final int PER_PAGE = ShopLayout.CONTENT_SLOTS.length; // 28

    public static void open(ServerPlayerEntity player, String poolId) {
        open(player, poolId, 0);
    }

    public static void open(ServerPlayerEntity player, String poolId, int page) {
        ServerMenuHandler.show(player,
                Text.literal("조우 확률").formatted(Formatting.DARK_PURPLE),
                inv -> populate(inv, poolId, page),
                (p, slot, button, shift) -> onClick(p, slot, poolId, page));
    }

    private static List<EncounterConfig.Candidate> enabled(EncounterConfig.Pool pool) {
        List<EncounterConfig.Candidate> list = new ArrayList<>();
        for (EncounterConfig.Candidate c : pool.candidates) if (c.enabled && c.weight > 0) list.add(c);
        return list;
    }

    private static void populate(Inventory inv, String poolId, int page) {
        for (int i = 0; i < ServerMenuHandler.DISPLAY_SIZE; i++) inv.setStack(i, MenuIcons.pane());
        EncounterConfig.Pool pool = ConfigManager.encounter().pools.get(poolId);
        if (pool == null) return;

        List<EncounterConfig.Candidate> list = enabled(pool);
        long total = 0;
        for (EncounterConfig.Candidate c : list) total += c.weight;
        int pages = Math.max(1, (list.size() + PER_PAGE - 1) / PER_PAGE);
        page = Math.max(0, Math.min(page, pages - 1));

        inv.setStack(ShopLayout.BALANCE_SLOT, MenuIcons.icon(Items.WRITABLE_BOOK,
                "§d" + pool.displayNameKo + " §7출현 확률",
                List.of("§7후보 §f" + list.size() + "종", "§7페이지 §f" + (page + 1) + " / " + pages)));
        inv.setStack(ShopLayout.BACK_SLOT, MenuIcons.icon(Items.ARROW, "§e← 제단으로", List.of()));
        if (page > 0) inv.setStack(ShopLayout.PREV_SLOT, MenuIcons.icon(Items.SPECTRAL_ARROW, "§e◀ 이전", List.of()));
        if (page < pages - 1) inv.setStack(ShopLayout.NEXT_SLOT, MenuIcons.icon(Items.SPECTRAL_ARROW, "§e다음 ▶", List.of()));

        int start = page * PER_PAGE;
        for (int i = 0; i < PER_PAGE && start + i < list.size(); i++) {
            EncounterConfig.Candidate c = list.get(start + i);
            double pct = total > 0 ? (c.weight * 100.0 / total) : 0;
            inv.setStack(ShopLayout.CONTENT_SLOTS[i], MenuIcons.icon(Items.NAME_TAG,
                    "§f" + c.displayNameKo,
                    List.of("§7출현 확률: §e" + String.format("%.1f", pct) + "%",
                            "§8가중치 " + c.weight)));
        }
    }

    private static void onClick(ServerPlayerEntity player, int slot, String poolId, int page) {
        if (slot == ShopLayout.BACK_SLOT) { AltarMenu.open(player); return; }
        if (slot == ShopLayout.PREV_SLOT) { open(player, poolId, page - 1); return; }
        if (slot == ShopLayout.NEXT_SLOT) { open(player, poolId, page + 1); return; }
        // 후보 칸은 정보 표시 전용(동작 없음)
    }
}
