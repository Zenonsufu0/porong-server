package kr.zenon.moncore.admin;

import kr.zenon.moncore.menu.MenuIcons;
import kr.zenon.moncore.menu.ServerMenuHandler;
import kr.zenon.moncore.shop.ShopLayout;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

/** 운영자 GUI — 온라인 플레이어 목록. 클릭 시 해당 플레이어 관리(PlayerActionMenu). */
public final class PlayerAdminMenu {
    private PlayerAdminMenu() {}

    public static void open(ServerPlayerEntity admin) {
        ServerMenuHandler.show(admin, Text.literal("플레이어 관리").formatted(Formatting.RED),
                inv -> populate(inv, admin), PlayerAdminMenu::onClick);
    }

    private static List<ServerPlayerEntity> online(ServerPlayerEntity admin) {
        return admin.getServer().getPlayerManager().getPlayerList();
    }

    private static void populate(Inventory inv, ServerPlayerEntity admin) {
        for (int i = 0; i < ServerMenuHandler.DISPLAY_SIZE; i++) inv.setStack(i, MenuIcons.pane());
        inv.setStack(ShopLayout.BALANCE_SLOT, MenuIcons.icon(Items.PLAYER_HEAD,
                "§c플레이어 관리", List.of("§7관리할 접속자를 클릭")));
        inv.setStack(ShopLayout.BACK_SLOT, MenuIcons.icon(Items.ARROW, "§e← 운영자 패널", List.of()));

        List<ServerPlayerEntity> list = online(admin);
        for (int i = 0; i < list.size() && i < ShopLayout.CONTENT_SLOTS.length; i++) {
            ServerPlayerEntity t = list.get(i);
            inv.setStack(ShopLayout.CONTENT_SLOTS[i], MenuIcons.icon(Items.PLAYER_HEAD,
                    "§f" + t.getGameProfile().getName(), List.of("§7클릭 — 관리")));
        }
    }

    private static void onClick(ServerPlayerEntity admin, int slot, int button, boolean shift) {
        if (slot == ShopLayout.BACK_SLOT) { AdminMenu.open(admin); return; }
        int idx = ShopLayout.contentIndexOf(slot);
        if (idx < 0) return;
        List<ServerPlayerEntity> list = online(admin);
        if (idx >= list.size()) return;
        PlayerActionMenu.open(admin, list.get(idx).getUuid());
    }
}
