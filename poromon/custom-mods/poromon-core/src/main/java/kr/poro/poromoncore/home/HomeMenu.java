package kr.poro.poromoncore.home;

import kr.poro.poromoncore.config.ConfigManager;
import kr.poro.poromoncore.data.Home;
import kr.poro.poromoncore.data.PlayerProgress;
import kr.poro.poromoncore.data.PoroMonState;
import kr.poro.poromoncore.economy.EconomyBridge;
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
 * 홈 GUI (결정 029, menu_design.md §3 슬롯20).
 * 5칸 진열: 잠김(해금 비용)·비어있음(등록)·등록됨(좌클릭 이동/우클릭 재등록).
 */
public final class HomeMenu {
    private HomeMenu() {}

    private static final int BALANCE_SLOT = 4;
    private static final int BACK_SLOT = 49;
    private static final int[] HOME_SLOTS = {20, 21, 22, 23, 24}; // 5칸 가로 배치

    public static void open(ServerPlayerEntity player) {
        ServerMenuHandler.show(player, Text.literal("홈").formatted(Formatting.AQUA),
                inv -> populate(inv, player), HomeMenu::onClick);
    }

    private static void populate(Inventory inv, ServerPlayerEntity player) {
        for (int i = 0; i < ServerMenuHandler.DISPLAY_SIZE; i++) inv.setStack(i, MenuIcons.pane());

        String unit = ConfigManager.economy().currencyDisplay;
        PlayerProgress p = PoroMonState.get(player.getServer()).getOrCreate(player.getUuid());

        inv.setStack(BALANCE_SLOT, MenuIcons.icon(Items.GOLD_NUGGET,
                "§6잔액: " + EconomyBridge.getBalance(player) + " " + unit,
                List.of("§7해금된 홈: §f" + p.homesUnlocked + " / " + ConfigManager.core().home.maxSlots)));
        inv.setStack(BACK_SLOT, MenuIcons.icon(Items.ARROW, "§e← 메뉴로", List.of()));

        for (int i = 0; i < HOME_SLOTS.length; i++) {
            inv.setStack(HOME_SLOTS[i], slotIcon(player, p, i));
        }
    }

    private static net.minecraft.item.ItemStack slotIcon(ServerPlayerEntity player, PlayerProgress p, int i) {
        boolean unlocked = i < p.homesUnlocked;
        if (!unlocked) {
            if (i == p.homesUnlocked) {
                long cost = HomeManager.nextUnlockCost(p);
                String unit = ConfigManager.economy().currencyDisplay;
                return MenuIcons.icon(Items.GOLD_INGOT, "§6" + (i + 1) + "번 홈 — 잠김",
                        List.of("§7해금 비용: §6" + cost + " " + unit, "§a클릭 — 해금"));
            }
            return MenuIcons.icon(Items.IRON_BARS, "§7" + (i + 1) + "번 홈 — 잠김",
                    List.of("§8이전 슬롯을 먼저 해금하세요"));
        }
        Home h = p.homes[i];
        if (h == null) {
            return MenuIcons.icon(Items.LIME_DYE, "§f" + (i + 1) + "번 홈 §7(비어있음)",
                    List.of("§a클릭 — 현재 위치를 이 홈으로 등록"));
        }
        return MenuIcons.icon(Items.RED_BED, "§f" + h.displayName(i),
                List.of("§7" + shortDim(h.dimension) + " §f" + (int) h.x + ", " + (int) h.y + ", " + (int) h.z,
                        "§a좌클릭 — 이동",
                        "§e우클릭 — 현재 위치로 재등록",
                        "§b쉬프트+클릭 — 이름 변경"));
    }

    private static String shortDim(String dim) {
        return switch (dim) {
            case "minecraft:overworld" -> "지상";
            case "minecraft:the_nether" -> "네더";
            case "minecraft:the_end" -> "엔드";
            default -> dim;
        };
    }

    private static void onClick(ServerPlayerEntity player, int slot, int button, boolean shift) {
        if (slot == BACK_SLOT) {
            MenuGuiManager.open(player);
            return;
        }
        int i = indexOf(slot);
        if (i < 0) return;

        PlayerProgress p = PoroMonState.get(player.getServer()).getOrCreate(player.getUuid());
        if (i >= p.homesUnlocked) {
            if (i == p.homesUnlocked) {
                HomeManager.unlockNext(player);
                open(player);
            } else {
                player.sendMessage(Text.literal("§8[홈] 이전 슬롯을 먼저 해금하세요."), true);
            }
            return;
        }
        if (p.homes[i] == null) {
            HomeManager.setHome(player, i);
            open(player);
            return;
        }
        if (shift) {
            promptRename(player, i);
            return;
        }
        if (button == 1) {
            HomeManager.setHome(player, i); // 재등록
            open(player);
        } else {
            player.closeHandledScreen();    // 채널링 동안 정지 필요 → GUI 닫기
            HomeManager.requestTeleport(player, i);
        }
    }

    /** 채팅으로 새 이름 입력받아 변경(취소: "취소"). */
    private static void promptRename(ServerPlayerEntity player, int i) {
        player.closeHandledScreen();
        player.sendMessage(Text.literal("§b[홈] " + (i + 1)
                + "번 홈의 새 이름을 채팅에 입력하세요. §7(취소: '취소' 입력)"), false);
        kr.poro.poromoncore.util.ChatInputManager.await(player, input -> {
            if (input.equals("취소") || input.isBlank()) {
                player.sendMessage(Text.literal("§7[홈] 이름 변경을 취소했습니다."), false);
            } else {
                HomeManager.renameHome(player, i, input);
            }
            open(player); // 홈 메뉴 다시 열기
        });
    }

    private static int indexOf(int slot) {
        for (int i = 0; i < HOME_SLOTS.length; i++) {
            if (HOME_SLOTS[i] == slot) return i;
        }
        return -1;
    }
}
