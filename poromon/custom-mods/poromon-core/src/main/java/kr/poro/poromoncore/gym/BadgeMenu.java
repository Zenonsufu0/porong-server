package kr.poro.poromoncore.gym;

import kr.poro.poromoncore.data.PlayerProgress;
import kr.poro.poromoncore.data.PoroMonState;
import kr.poro.poromoncore.menu.MenuGuiManager;
import kr.poro.poromoncore.menu.MenuIcons;
import kr.poro.poromoncore.menu.ServerMenuHandler;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

/**
 * 배지 컬렉션 뷰 (gym_badge_design.md §6) — 읽기 전용.
 * 획득한 배지는 타입색 배지 아이템으로, 미획득은 회색(가림)으로 표시.
 * (정보/순차 안내는 GymBoardMenu = "관장 가이드"로 분리.)
 */
public final class BadgeMenu {
    private BadgeMenu() {}

    private static final int SUMMARY_SLOT = 4;
    private static final int BACK_SLOT = 49;
    // 4칸 × 2행, 가로 여백 한 칸씩(col 1·3·5·7), 세로로 row1/row3
    private static final int[] BADGE_SLOTS = {10, 12, 14, 16, 28, 30, 32, 34};

    public static void open(ServerPlayerEntity player) {
        ServerMenuHandler.show(player, Text.literal("배지").formatted(Formatting.GOLD),
                inv -> populate(inv, player), BadgeMenu::onClick);
    }

    private static void populate(Inventory inv, ServerPlayerEntity player) {
        for (int i = 0; i < ServerMenuHandler.DISPLAY_SIZE; i++) inv.setStack(i, MenuIcons.pane());

        PlayerProgress p = PoroMonState.get(player.getServer()).getOrCreate(player.getUuid());
        List<GymInfo.Gym> gyms = GymInfo.GYMS;
        int owned = 0;
        for (GymInfo.Gym g : gyms) if (p.badges.contains(g.id())) owned++;

        inv.setStack(SUMMARY_SLOT, MenuIcons.icon(Items.GOLD_INGOT,
                "§6배지 컬렉션 " + owned + " / " + gyms.size(),
                List.of("§7획득한 배지가 색으로 표시됩니다",
                        "§7관장 정보는 §f관장 가이드§7에서")));
        inv.setStack(BACK_SLOT, MenuIcons.icon(Items.ARROW, "§e← 메뉴로", List.of()));

        for (int i = 0; i < gyms.size(); i++) {
            GymInfo.Gym g = gyms.get(i);
            boolean has = p.badges.contains(g.id());
            if (has) {
                // 모드 내장 커스텀 배지 텍스처: paper + CustomModelData(81000+order)
                inv.setStack(BADGE_SLOTS[i], MenuIcons.iconModel(Items.PAPER, 81000 + g.order(),
                        "§a" + g.badgeKo(),
                        List.of("§7" + g.order() + "번 · " + g.typeKo() + " 관장",
                                "§7레벨캡: §f" + g.levelCap(),
                                "§a✔ 획득 완료")));
            } else {
                inv.setStack(BADGE_SLOTS[i], MenuIcons.icon(Items.LIGHT_GRAY_STAINED_GLASS_PANE,
                        "§7??? 배지",
                        List.of("§8" + g.order() + "번 · " + g.typeKo() + " 관장",
                                "§8아직 획득하지 못했습니다")));
            }
        }
    }

    private static void onClick(ServerPlayerEntity player, int slot, int button, boolean shift) {
        if (slot == BACK_SLOT) {
            MenuGuiManager.open(player);
        }
    }
}
