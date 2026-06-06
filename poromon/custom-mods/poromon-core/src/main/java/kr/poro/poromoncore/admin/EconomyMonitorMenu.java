package kr.poro.poromoncore.admin;

import kr.poro.poromoncore.data.PoroMonState;
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
import java.util.Map;
import java.util.TreeSet;

/**
 * 운영자 GUI — 경제 모니터(텔레메트리). 출처 그룹별 골드 유입/유출 누적 + 총계(가격 조정 근거).
 */
public final class EconomyMonitorMenu {
    private EconomyMonitorMenu() {}

    public static void open(ServerPlayerEntity admin) {
        ServerMenuHandler.show(admin, Text.literal("경제 모니터").formatted(Formatting.GOLD),
                inv -> populate(inv, admin), EconomyMonitorMenu::onClick);
    }

    private static void populate(Inventory inv, ServerPlayerEntity admin) {
        for (int i = 0; i < ServerMenuHandler.DISPLAY_SIZE; i++) inv.setStack(i, MenuIcons.pane());
        PoroMonState s = PoroMonState.get(admin.getServer());

        long totalIn = s.goldIn.values().stream().mapToLong(Long::longValue).sum();
        long totalOut = s.goldOut.values().stream().mapToLong(Long::longValue).sum();
        inv.setStack(ShopLayout.BALANCE_SLOT, MenuIcons.icon(Items.GOLD_BLOCK, "§6골드 흐름 총계",
                List.of("§a유입 합계: §f" + totalIn, "§c유출 합계: §f" + totalOut,
                        "§7순증: §f" + (totalIn - totalOut))));
        inv.setStack(ShopLayout.BACK_SLOT, MenuIcons.icon(Items.ARROW, "§e← 운영자 패널", List.of()));

        // 출처 그룹 합집합(유입+유출 키)
        TreeSet<String> groups = new TreeSet<>();
        groups.addAll(s.goldIn.keySet());
        groups.addAll(s.goldOut.keySet());
        List<String> list = new ArrayList<>(groups);
        for (int i = 0; i < list.size() && i < ShopLayout.CONTENT_SLOTS.length; i++) {
            String g = list.get(i);
            long in = s.goldIn.getOrDefault(g, 0L);
            long out = s.goldOut.getOrDefault(g, 0L);
            inv.setStack(ShopLayout.CONTENT_SLOTS[i], MenuIcons.icon(Items.PAPER,
                    "§f" + g, List.of("§a유입 +" + in, "§c유출 -" + out)));
        }
        if (list.isEmpty()) {
            inv.setStack(ShopLayout.CONTENT_SLOTS[0], MenuIcons.icon(Items.BARRIER,
                    "§7기록 없음", List.of("§8아직 골드 거래가 없습니다")));
        }
    }

    private static void onClick(ServerPlayerEntity admin, int slot, int button, boolean shift) {
        if (slot == ShopLayout.BACK_SLOT) AdminMenu.open(admin);
    }
}
