package kr.poro.poromoncore.auth;

import kr.poro.poromoncore.menu.MenuIcons;
import kr.poro.poromoncore.menu.ServerMenuHandler;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

/**
 * 미인증자 전용 메뉴 (결정 041): "인증하기"만 노출. 클릭 시 /인증 코드 발급.
 * 인증 완료 전에는 일반 메뉴(MenuGuiManager) 대신 이 화면이 열린다.
 */
public final class AuthMenu {
    private AuthMenu() {}

    private static final int SLOT_AUTH = 22;

    public static void open(ServerPlayerEntity player) {
        ServerMenuHandler.show(player,
                Text.literal("디스코드 인증 필요").formatted(Formatting.RED),
                AuthMenu::populate,
                (p, slot, button, shift) -> onClick(p, slot));
    }

    private static void populate(Inventory inv) {
        for (int i = 0; i < ServerMenuHandler.DISPLAY_SIZE; i++) inv.setStack(i, MenuIcons.pane());
        inv.setStack(SLOT_AUTH, MenuIcons.icon(Items.WRITABLE_BOOK, "§a§l인증하기",
                List.of("§7디스코드 인증이 필요합니다.",
                        "§7클릭 → 인증 코드 발급",
                        "§7발급된 코드를 §f디스코드 봇§7에게 입력하세요.",
                        "§8인증 전에는 허브를 벗어날 수 없습니다.")));
    }

    private static void onClick(ServerPlayerEntity player, int slot) {
        if (slot != SLOT_AUTH) return;
        player.closeHandledScreen();
        issueCode(player);
    }

    /** /인증 및 메뉴 버튼 공용: 코드 발급 + 안내(이미 인증 시 안내만). */
    public static void issueCode(ServerPlayerEntity player) {
        if (AuthManager.isVerified(player)) {
            player.sendMessage(Text.literal("§a[인증] 이미 인증된 계정입니다."), false);
            return;
        }
        String code = AuthManager.generateCode(player);
        player.sendMessage(Text.literal("§6§l[디스코드 인증]"), false);
        player.sendMessage(Text.literal("§7아래 코드를 §f포롱 디스코드 봇§7에게 입력하세요:"), false);
        player.sendMessage(Text.literal("   §a§l" + code), false);
        player.sendMessage(Text.literal("§8코드 유효시간 내에 입력 → 인증 완료 시 자동 안내됩니다."), false);
    }
}
