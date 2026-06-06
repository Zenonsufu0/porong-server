package kr.poro.poromoncore.menu;

import kr.poro.poromoncore.config.ConfigManager;
import kr.poro.poromoncore.data.PlayerProgress;
import kr.poro.poromoncore.data.PoroMonState;
import kr.poro.poromoncore.gym.BadgeMenu;
import kr.poro.poromoncore.gym.GymBoardMenu;
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
        inv.setStack(SLOT_BADGES, MenuIcons.icon(Items.GOLD_INGOT, "§6배지",
                List.of("§7획득한 배지 컬렉션", "§7클릭 — 배지 보기")));
        inv.setStack(SLOT_GYM_GUIDE, MenuIcons.icon(Items.OAK_SIGN, "§e관장 가이드",
                List.of("§7관장 순서·타입·레벨캡 안내", "§7클릭 — 관장 보드 열기")));
        inv.setStack(SLOT_LEAGUE, MenuIcons.icon(Items.GOLDEN_APPLE, "§d리그 / 챔피언",
                List.of("§7정규리그·챔피언스리그 안내", "§7클릭 — 채팅 안내")));
        inv.setStack(SLOT_SERVER_GUIDE, MenuIcons.icon(Items.MAP, "§a서버 가이드",
                List.of("§7서버 규칙·핵심 정책", "§7클릭 — 채팅 안내")));

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
            case SLOT_BADGES -> BadgeMenu.open(player);
            case SLOT_GYM_GUIDE -> GymBoardMenu.open(player);
            case SLOT_LEAGUE -> sendLeagueInfo(player);
            case SLOT_SERVER_GUIDE -> sendServerGuide(player);
            case SLOT_SELL_SHOP -> SellShopMenu.open(player);     // 매입소(아이콘 슬롯 28)
            case SLOT_CONVENIENCE -> BuyShopMenu.open(player);   // 편의 상점(슬롯 29)
            case SLOT_CLOSE -> player.closeHandledScreen();
            case SLOT_PLAYER_INFO -> { /* 읽기 전용 */ }
            case SLOT_ALTAR, SLOT_MEGA_LAB, SLOT_TM, SLOT_EGG, SLOT_TRAINING ->
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

    private static void sendLeagueInfo(ServerPlayerEntity player) {
        player.closeHandledScreen();
        player.sendMessage(Text.literal("§6§l[리그 / 챔피언 안내]"), false);
        player.sendMessage(Text.literal("§e· 배틀타워 §7— 50층 PvE 탑. 자격: §f관장 8명 전원§7. 체크포인트식(패배해도 진행 유지)"), false);
        player.sendMessage(Text.literal("§e· 정규리그 §7— 점수제 래더(승 +10 / 패 −7). §f레벨 50 정규화§7, 실시간 매칭. 자격: 8배지"), false);
        player.sendMessage(Text.literal("§e· 배틀 아레나 §7— 친선 대전(실제 레벨, 기록·보상 없음)"), false);
        player.sendMessage(Text.literal("§e· 챔피언스리그 §7— 서버 마지막날 토너먼트. 우승자 = 최종 챔피언(챔피언 홀 영구 등재)"), false);
        player.sendMessage(Text.literal("§8※ 메가만 허용 / 테라·다이맥스·Z무브 off"), false);
    }

    private static void sendServerGuide(ServerPlayerEntity player) {
        player.closeHandledScreen();
        player.sendMessage(Text.literal("§a§l[포로몬 서버 가이드]"), false);
        player.sendMessage(Text.literal("§7· 자유롭게 탐험·포획·건축·농사. 자연 마을도 그대로 이용"), false);
        player.sendMessage(Text.literal("§7· 화폐는 §6골드§7 하나 — 매입소에서 광물·작물·베리 판매, 야생 포켓몬 처치/포획으로 획득"), false);
        player.sendMessage(Text.literal("§7· 핵심은 §f허브§7 중심: 관장(8)·전설 제단·메가 연구소·시장·배틀타워"), false);
        player.sendMessage(Text.literal("§7· 관장 8명 → §f메가/티켓 해금·배틀타워·리그§7 자격"), false);
        player.sendMessage(Text.literal("§7· 전설은 야생 랜덤 스폰 없음 → §f조우권§7으로 개인 조우방에서만"), false);
        player.sendMessage(Text.literal("§7· 유저 간 대전은 §f친선(비공식)§7 + 정규/챔피언스리그. 경매장 없음(직거래·트레이드)"), false);
        player.sendMessage(Text.literal("§7· 9번 슬롯 §f리그 패스§7 우클릭 = 이 메뉴 (또는 §f/poromon menu§7)"), false);
    }

    private static net.minecraft.item.ItemStack soon(net.minecraft.item.Item item, String name) {
        return MenuIcons.icon(item, name, List.of("§8준비 중 (0.1) — 추후 허브 NPC/메뉴에서 제공"));
    }
}
