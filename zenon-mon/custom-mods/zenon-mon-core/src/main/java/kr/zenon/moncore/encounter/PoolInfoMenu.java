package kr.zenon.moncore.encounter;

import kr.zenon.moncore.config.ConfigManager;
import kr.zenon.moncore.config.EncounterConfig;
import kr.zenon.moncore.menu.MenuIcons;
import kr.zenon.moncore.menu.ServerMenuHandler;
import kr.zenon.moncore.shop.ShopLayout;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;

/**
 * 조우권 풀 상세(후보 포켓몬 + 출현 확률%) — 읽기 전용. 제단에서 풀 우클릭으로 진입.
 * 확률 = EncounterConfig.probabilities(2단계 가중) — 실제 추첨 분포와 동일.
 */
public final class PoolInfoMenu {
    private PoolInfoMenu() {}

    private static final int PER_PAGE = ShopLayout.CONTENT_SLOTS.length; // 28

    // stage 키 → 한글 등급 표시
    private static String stageKo(String stage) {
        if (stage == null) return "";
        return switch (stage) {
            case "basic" -> "기본형";
            case "middle" -> "중간진화";
            case "final" -> "최종진화";
            case "intermediate" -> "중급";
            case "advanced" -> "상급";
            case "apex" -> "최상급";
            case "low" -> "하급";       // 필드이벤트
            case "mid" -> "중급";        // 필드이벤트
            default -> stage;
        };
    }

    public static void open(ServerPlayerEntity player, String poolId) {
        open(player, poolId, 0);
    }

    public static void open(ServerPlayerEntity player, String poolId, int page) {
        ServerMenuHandler.show(player,
                Text.literal("조우 확률").formatted(Formatting.DARK_PURPLE),
                inv -> populate(inv, poolId, page),
                (p, slot, button, shift) -> onClick(p, slot, poolId, page));
    }

    private static void populate(Inventory inv, String poolId, int page) {
        for (int i = 0; i < ServerMenuHandler.DISPLAY_SIZE; i++) inv.setStack(i, MenuIcons.pane());
        EncounterConfig.Pool pool = ConfigManager.encounter().pools.get(poolId);
        if (pool == null) return;

        List<EncounterConfig.Candidate> list = EncounterConfig.enabled(pool);
        double apexMult = kr.zenon.moncore.event.EventManager.apexMultiplier();
        java.util.Map<EncounterConfig.Candidate, Double> probs = EncounterConfig.probabilities(pool, apexMult);
        int pages = Math.max(1, (list.size() + PER_PAGE - 1) / PER_PAGE);
        page = Math.max(0, Math.min(page, pages - 1));

        List<String> headLore = new ArrayList<>();
        headLore.add("§7후보 §f" + list.size() + "종");
        headLore.add("§7페이지 §f" + (page + 1) + " / " + pages);
        if (apexMult > 1.0) headLore.add("§c✦ 최상급 부스트 이벤트 진행 중");
        inv.setStack(ShopLayout.BALANCE_SLOT, MenuIcons.icon(Items.WRITABLE_BOOK,
                "§d" + pool.displayNameKo + " §7출현 확률", headLore));
        inv.setStack(ShopLayout.BACK_SLOT, MenuIcons.icon(Items.ARROW, "§e← 제단으로", List.of()));
        if (page > 0) inv.setStack(ShopLayout.PREV_SLOT, MenuIcons.icon(Items.SPECTRAL_ARROW, "§e◀ 이전", List.of()));
        if (page < pages - 1) inv.setStack(ShopLayout.NEXT_SLOT, MenuIcons.icon(Items.SPECTRAL_ARROW, "§e다음 ▶", List.of()));

        int start = page * PER_PAGE;
        for (int i = 0; i < PER_PAGE && start + i < list.size(); i++) {
            EncounterConfig.Candidate c = list.get(start + i);
            double pct = probs.getOrDefault(c, 0.0) * 100.0;
            String tier = stageKo(c.stage);
            List<String> lore = new ArrayList<>();
            lore.add("§7출현 확률: §e" + String.format("%.1f", pct) + "%");
            if (!tier.isEmpty()) lore.add("§8등급 " + tier);
            inv.setStack(ShopLayout.CONTENT_SLOTS[i], MenuIcons.icon(Items.NAME_TAG,
                    "§f" + c.displayNameKo, lore));
        }
    }

    private static void onClick(ServerPlayerEntity player, int slot, String poolId, int page) {
        if (slot == ShopLayout.BACK_SLOT) { AltarMenu.open(player); return; }
        if (slot == ShopLayout.PREV_SLOT) { open(player, poolId, page - 1); return; }
        if (slot == ShopLayout.NEXT_SLOT) { open(player, poolId, page + 1); return; }
        // 후보 칸은 정보 표시 전용(동작 없음)
    }
}
