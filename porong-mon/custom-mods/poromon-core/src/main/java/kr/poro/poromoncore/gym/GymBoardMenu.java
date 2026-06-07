package kr.poro.poromoncore.gym;

import kr.poro.poromoncore.data.PlayerProgress;
import kr.poro.poromoncore.data.PoroMonState;
import kr.poro.poromoncore.menu.MenuGuiManager;
import kr.poro.poromoncore.menu.MenuIcons;
import kr.poro.poromoncore.menu.ServerMenuHandler;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

/**
 * 관장 도전 현황 보드 (gym_badge_design.md §2/§7) — 읽기 전용.
 * 8관장의 순서·타입·레벨캡·배지·획득 현황 + 순차 강제/게이트 안내.
 * 메인 메뉴의 "배지"·"관장 가이드" 둘 다 이 보드로 연결.
 */
public final class GymBoardMenu {
    private GymBoardMenu() {}

    private static final int SUMMARY_SLOT = 4;
    private static final int BACK_SLOT = 49;
    // 4칸 × 2행, 가로 여백 한 칸씩(col 1·3·5·7), row1/row3
    private static final int[] GYM_SLOTS = {10, 12, 14, 16, 28, 30, 32, 34};

    public static void open(ServerPlayerEntity player) {
        ServerMenuHandler.show(player, Text.literal("관장 도전 현황").formatted(Formatting.GOLD),
                inv -> populate(inv, player), GymBoardMenu::onClick);
    }

    private static void populate(Inventory inv, ServerPlayerEntity player) {
        for (int i = 0; i < ServerMenuHandler.DISPLAY_SIZE; i++) inv.setStack(i, MenuIcons.pane());

        PlayerProgress p = PoroMonState.get(player.getServer()).getOrCreate(player.getUuid());
        List<GymInfo.Gym> gyms = GymInfo.GYMS;
        int owned = 0;
        for (GymInfo.Gym g : gyms) if (p.badges.contains(g.id())) owned++;

        inv.setStack(SUMMARY_SLOT, MenuIcons.icon(Items.GOLD_INGOT,
                "§6관장 배지 " + owned + " / " + gyms.size(),
                List.of("§7관장은 순서대로 도전합니다(순차 강제)",
                        "§78명 전원 클리어 시 §f배틀타워·정규리그§7 자격")));
        inv.setStack(BACK_SLOT, MenuIcons.icon(Items.ARROW, "§e← 메뉴로", List.of()));

        boolean prevCleared = true; // 1번은 항상 도전 가능
        for (int i = 0; i < gyms.size(); i++) {
            GymInfo.Gym g = gyms.get(i);
            boolean has = p.badges.contains(g.id());
            ItemStack icon;
            if (has) {
                icon = MenuIcons.icon(Items.LIME_DYE, "§a" + g.order() + ". " + g.typeKo() + " 관장",
                        gymLore(g, "§a✔ 클리어", "§7클릭 — 재도전(보상 없음)"));
            } else if (prevCleared) {
                icon = MenuIcons.icon(Items.YELLOW_DYE, "§e" + g.order() + ". " + g.typeKo() + " 관장",
                        gymLore(g, "§e▶ 도전 가능", "§a클릭 — 도전!"));
            } else {
                icon = MenuIcons.icon(Items.GRAY_DYE, "§8" + g.order() + ". " + g.typeKo() + " 관장",
                        gymLore(g, "§8✖ 잠김 (이전 관장 먼저)", null));
            }
            inv.setStack(GYM_SLOTS[i], icon);
            prevCleared = has;
        }
    }

    private static List<String> gymLore(GymInfo.Gym g, String status, String action) {
        java.util.List<String> lore = new java.util.ArrayList<>(List.of(
                "§7타입: §f" + g.typeKo(),
                "§7레벨캡: §f" + g.levelCap(),
                "§7배지: §f" + g.badgeKo(),
                status));
        if (action != null) lore.add(action);
        return lore;
    }

    private static void onClick(ServerPlayerEntity player, int slot, int button, boolean shift) {
        if (slot == BACK_SLOT) {
            MenuGuiManager.open(player);
            return;
        }
        for (int i = 0; i < GYM_SLOTS.length; i++) {
            if (GYM_SLOTS[i] == slot) {
                player.closeHandledScreen(); // 배틀 시작 → GUI 닫기
                GymBattleService.startChallenge(player, GymInfo.GYMS.get(i).id());
                return;
            }
        }
    }
}
