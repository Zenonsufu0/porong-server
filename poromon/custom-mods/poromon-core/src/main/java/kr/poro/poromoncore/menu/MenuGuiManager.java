package kr.poro.poromoncore.menu;

import kr.poro.poromoncore.config.ConfigManager;
import kr.poro.poromoncore.data.PlayerProgress;
import kr.poro.poromoncore.data.PoroMonState;
import kr.poro.poromoncore.home.HomeMenu;
import kr.poro.poromoncore.hub.HubManager;
import kr.poro.poromoncore.shop.BuyShopMenu;
import kr.poro.poromoncore.shop.SellShopMenu;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

/**
 * 메인 메뉴 GUI 구성·오픈·액션 처리 (menu_design.md §3, MenuGuiManager).
 * 0.1 활성: 허브 TP(19) · 진행도(10) · 매입소(28) · 편의 상점(29) · 닫기(49) · 플레이어 정보(4).
 * 나머지(배지/가이드/허브 시설 안내)는 "준비 중" placeholder.
 */
public final class MenuGuiManager {
    private MenuGuiManager() {}

    private static final int SLOT_PLAYER_INFO = 4;
    private static final int SLOT_PROGRESS = 10;
    private static final int SLOT_BADGES = 11;
    private static final int SLOT_GYM_GUIDE = 12;
    private static final int SLOT_LEAGUE = 13;
    private static final int SLOT_SERVER_GUIDE = 14;
    private static final int SLOT_HUB_TP = 19;
    private static final int SLOT_HOME = 20;
    private static final int SLOT_SELL_SHOP = 28;
    private static final int SLOT_CONVENIENCE = 29;
    private static final int SLOT_ALTAR = 37;
    private static final int SLOT_MEGA_LAB = 38;
    private static final int SLOT_TM = 39;
    private static final int SLOT_EGG = 40;
    private static final int SLOT_TRAINING = 41;
    private static final int SLOT_CLOSE = 49;

    public static void open(ServerPlayerEntity player) {
        ServerMenuHandler.show(player, Text.literal("포로몬 메뉴").formatted(Formatting.DARK_AQUA),
                inv -> populate(inv, player), MenuGuiManager::handleClick);
    }

    private static void populate(Inventory inv, ServerPlayerEntity player) {
        for (int i = 0; i < ServerMenuHandler.DISPLAY_SIZE; i++) inv.setStack(i, MenuIcons.pane());

        PoroMonState state = PoroMonState.get(player.getServer());
        PlayerProgress p = state.getOrCreate(player.getUuid());
        String unit = ConfigManager.economy().currencyDisplay;

        inv.setStack(SLOT_PLAYER_INFO, MenuIcons.icon(Items.PLAYER_HEAD,
                "§e" + player.getGameProfile().getName(),
                List.of("§7" + unit + ": §6" + p.balance,
                        "§7배틀타워 최고층: §f" + p.battleTowerHighestClearedFloor,
                        "§7리그 패스: " + (p.leaguePassGiven ? "§a보유" : "§c없음"))));

        inv.setStack(SLOT_PROGRESS, MenuIcons.icon(Items.BOOK, "§b내 진행도",
                List.of("§7클릭 — 진행 상황을 채팅으로 표시")));
        inv.setStack(SLOT_BADGES, soon(Items.GOLD_INGOT, "§6배지"));
        inv.setStack(SLOT_GYM_GUIDE, soon(Items.OAK_SIGN, "§e짐 가이드"));
        inv.setStack(SLOT_LEAGUE, soon(Items.GOLDEN_APPLE, "§d리그 / 챔피언"));
        inv.setStack(SLOT_SERVER_GUIDE, soon(Items.MAP, "§a서버 가이드"));

        inv.setStack(SLOT_HUB_TP, MenuIcons.icon(Items.ENDER_PEARL, "§b허브로 이동",
                List.of("§7클릭 — 허브로 텔레포트")));
        inv.setStack(SLOT_HOME, MenuIcons.icon(Items.RED_BED, "§a홈",
                List.of("§7홈 등록/이동 (최대 5칸)", "§7클릭 — 홈 메뉴 열기")));

        inv.setStack(SLOT_SELL_SHOP, MenuIcons.icon(Items.CHEST, "§6매입소",
                List.of("§7광물·농작물·베리를 " + unit + "로 매입", "§7클릭 — 매입소 열기")));
        inv.setStack(SLOT_CONVENIENCE, MenuIcons.icon(Items.SLIME_BALL, "§a편의 상점",
                List.of("§7몬스터볼·회복약 구매", "§7클릭 — 편의 상점 열기")));

        inv.setStack(SLOT_ALTAR, soon(Items.PURPLE_STAINED_GLASS, "§5전설 제단 안내"));
        inv.setStack(SLOT_MEGA_LAB, soon(Items.NETHER_STAR, "§d메가 연구소 안내"));
        inv.setStack(SLOT_TM, soon(Items.PAPER, "§b기술머신 안내"));
        inv.setStack(SLOT_EGG, soon(Items.EGG, "§e알 상점 안내"));
        inv.setStack(SLOT_TRAINING, soon(Items.EXPERIENCE_BOTTLE, "§a육성 상점 안내"));

        inv.setStack(SLOT_CLOSE, MenuIcons.icon(Items.BARRIER, "§c닫기", List.of()));
    }

    public static void handleClick(ServerPlayerEntity player, int slot, int button, boolean shift) {
        switch (slot) {
            case SLOT_HUB_TP -> {
                player.closeHandledScreen();
                HubManager.teleportToHub(player);
            }
            case SLOT_PROGRESS -> sendProgress(player);
            case SLOT_HOME -> HomeMenu.open(player);
            case SLOT_SELL_SHOP -> SellShopMenu.open(player);     // 매입소(아이콘 슬롯 28)
            case SLOT_CONVENIENCE -> BuyShopMenu.open(player);   // 편의 상점(슬롯 29)
            case SLOT_CLOSE -> player.closeHandledScreen();
            case SLOT_PLAYER_INFO -> { /* 읽기 전용 */ }
            case SLOT_BADGES, SLOT_GYM_GUIDE, SLOT_LEAGUE, SLOT_SERVER_GUIDE,
                 SLOT_ALTAR, SLOT_MEGA_LAB, SLOT_TM, SLOT_EGG, SLOT_TRAINING ->
                    player.sendMessage(Text.literal("§e[PoroMon]§r 준비 중인 기능입니다 (0.1)."), true);
            default -> { /* 테두리/빈칸 무시 */ }
        }
    }

    private static void sendProgress(ServerPlayerEntity player) {
        PoroMonState state = PoroMonState.get(player.getServer());
        PlayerProgress p = state.getOrCreate(player.getUuid());
        String unit = ConfigManager.economy().currencyDisplay;
        player.sendMessage(Text.literal(
                "§e[PoroMon] 내 진행도§r — " + unit + ": §6" + p.balance
                        + " §r/ 배틀타워 최고층: §f" + p.battleTowerHighestClearedFloor
                        + " §r/ 리그패스: " + (p.leaguePassGiven ? "§a보유" : "§c없음")), false);
    }

    private static net.minecraft.item.ItemStack soon(net.minecraft.item.Item item, String name) {
        return MenuIcons.icon(item, name, List.of("§8준비 중 (0.1) — 추후 허브 NPC/메뉴에서 제공"));
    }
}
