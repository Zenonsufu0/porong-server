package kr.zenon.moncore.admin;

import kr.zenon.moncore.config.ConfigManager;
import kr.zenon.moncore.config.EncounterConfig;
import kr.zenon.moncore.encounter.EncounterService;
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

/** 운영자 GUI — 조우 강제 소환(무료·게이트 무시, 이벤트 보상/테스트용). 본인에게 소환. */
public final class EncounterAdminMenu {
    private EncounterAdminMenu() {}

    public static void open(ServerPlayerEntity admin) {
        ServerMenuHandler.show(admin, Text.literal("조우 강제 소환").formatted(Formatting.RED),
                EncounterAdminMenu::populate, EncounterAdminMenu::onClick);
    }

    private static List<String> pools() {
        return new ArrayList<>(ConfigManager.encounter().pools.keySet());
    }

    private static void populate(Inventory inv) {
        for (int i = 0; i < ServerMenuHandler.DISPLAY_SIZE; i++) inv.setStack(i, MenuIcons.pane());
        inv.setStack(ShopLayout.BALANCE_SLOT, MenuIcons.icon(Items.ENDER_EYE, "§c조우 강제 소환",
                List.of("§7좌클릭 = 소환 §8| §7우클릭 = 이로치 소환", "§7골드·게이트 무시(운영자)")));
        inv.setStack(ShopLayout.BACK_SLOT, MenuIcons.icon(Items.ARROW, "§e← 운영자 패널", List.of()));

        List<String> list = pools();
        for (int i = 0; i < list.size() && i < ShopLayout.CONTENT_SLOTS.length; i++) {
            EncounterConfig.Pool pool = ConfigManager.encounter().pools.get(list.get(i));
            if (pool == null) continue;
            inv.setStack(ShopLayout.CONTENT_SLOTS[i], MenuIcons.icon(Items.PAPER,
                    "§d" + pool.displayNameKo, List.of("§7type: " + pool.type, "§a좌클릭 소환 §8/ §b우클릭 이로치")));
        }
    }

    private static void onClick(ServerPlayerEntity admin, int slot, int button, boolean shift) {
        if (slot == ShopLayout.BACK_SLOT) { AdminMenu.open(admin); return; }
        int idx = ShopLayout.contentIndexOf(slot);
        if (idx < 0) return;
        List<String> list = pools();
        if (idx >= list.size()) return;
        admin.closeHandledScreen();
        EncounterService.start(admin, list.get(idx), button == 1);
    }
}
