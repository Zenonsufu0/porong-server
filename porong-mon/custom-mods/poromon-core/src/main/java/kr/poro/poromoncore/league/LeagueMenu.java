package kr.poro.poromoncore.league;

import kr.poro.poromoncore.config.ConfigManager;
import kr.poro.poromoncore.config.SeasonConfig;
import kr.poro.poromoncore.data.PlayerProgress;
import kr.poro.poromoncore.data.PoroMonState;
import kr.poro.poromoncore.menu.MenuGuiManager;
import kr.poro.poromoncore.menu.MenuIcons;
import kr.poro.poromoncore.menu.ServerMenuHandler;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Items;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 리그 메뉴 (league_season_design §4). 정규리그 큐 참가/취소 + 내 전적 + 순위표.
 * 챔피언스리그는 안내만(후속 Phase 6). 읽기 전용 — 액션은 디스플레이 클릭만.
 */
public final class LeagueMenu {
    private LeagueMenu() {}

    private static final int SLOT_QUEUE = 20;
    private static final int SLOT_RECORD = 22;
    private static final int SLOT_RANKING = 24;
    private static final int SLOT_CHAMPIONS = 31;
    private static final int SLOT_BACK = 48;
    private static final int SLOT_CLOSE = 50;

    public static void open(ServerPlayerEntity player) {
        ServerMenuHandler.show(player,
                Text.literal("리그").formatted(Formatting.GOLD),
                inv -> populate(inv, player),
                (p, slot, button, shift) -> onClick(p, slot));
    }

    private static void populate(Inventory inv, ServerPlayerEntity player) {
        for (int i = 0; i < ServerMenuHandler.DISPLAY_SIZE; i++) inv.setStack(i, MenuIcons.pane());

        MinecraftServer server = player.getServer();
        SeasonConfig.RankedLeague cfg = ConfigManager.season().rankedLeague;
        PoroMonState state = PoroMonState.get(server);
        PlayerProgress p = state.getOrCreate(player.getUuid());
        boolean queued = LeagueManager.isQueued(player.getUuid());
        boolean eligible = p.badges.size() >= cfg.requireBadges;

        // 큐 참가/취소
        if (queued) {
            inv.setStack(SLOT_QUEUE, MenuIcons.icon(Items.LIME_DYE, "§a정규리그 — 매칭 대기 중",
                    List.of("§7상대를 찾는 중...", "§e클릭 — 큐 나가기")));
        } else if (eligible) {
            inv.setStack(SLOT_QUEUE, MenuIcons.icon(Items.DIAMOND, "§b정규리그 큐 참가",
                    List.of("§7실시간 매칭 · §fLv" + cfg.adjustLevel + " 정규화",
                            "§7승 §a+" + cfg.winDelta + " §7/ 패 §c" + cfg.lossDelta,
                            "§e클릭 — 큐 참가")));
        } else {
            inv.setStack(SLOT_QUEUE, MenuIcons.icon(Items.GRAY_DYE, "§7정규리그 (자격 미달)",
                    List.of("§c배지 " + cfg.requireBadges + "개 필요 §7(현재 " + p.badges.size() + "개)")));
        }

        // 내 전적
        List<Map.Entry<UUID, PlayerProgress>> ranking = ranking(state);
        int myRank = -1;
        for (int i = 0; i < ranking.size(); i++) if (ranking.get(i).getKey().equals(player.getUuid())) { myRank = i + 1; break; }
        List<String> rec = new ArrayList<>();
        if (p.rankedInit) {
            rec.add("§7점수: §e" + p.rankedScore);
            rec.add("§7전적: §a" + p.rankedWins + "승 §c" + p.rankedLosses + "패");
            rec.add("§7순위: §f" + (myRank > 0 ? myRank + "위 / " + ranking.size() + "명" : "—"));
        } else {
            rec.add("§7아직 정규리그에 참가한 적 없음");
            rec.add("§7큐에 참가하면 점수 §e" + cfg.startScore + "§7부터 시작");
        }
        inv.setStack(SLOT_RECORD, MenuIcons.icon(Items.WRITABLE_BOOK, "§e내 정규리그 전적", rec));

        // 순위표 Top 5
        List<String> top = new ArrayList<>();
        if (ranking.isEmpty()) {
            top.add("§7아직 기록 없음");
        } else {
            for (int i = 0; i < Math.min(5, ranking.size()); i++) {
                Map.Entry<UUID, PlayerProgress> e = ranking.get(i);
                top.add("§6" + (i + 1) + "위 §f" + name(server, e.getKey()) + " §7— §e" + e.getValue().rankedScore);
            }
        }
        inv.setStack(SLOT_RANKING, MenuIcons.icon(Items.GOLD_INGOT, "§6정규리그 순위표", top));

        // 챔피언스리그 안내
        inv.setStack(SLOT_CHAMPIONS, MenuIcons.icon(Items.NETHER_STAR, "§d챔피언스리그",
                List.of("§7서버 마지막날 토너먼트", "§7우승자 = 최종 챔피언(챔피언 홀 등재)", "§8※ 추후 제공")));

        inv.setStack(SLOT_BACK, MenuIcons.icon(Items.ARROW, "§e← 메뉴로", List.of()));
        inv.setStack(SLOT_CLOSE, MenuIcons.icon(Items.BARRIER, "§c닫기", List.of()));
    }

    private static void onClick(ServerPlayerEntity player, int slot) {
        switch (slot) {
            case SLOT_QUEUE -> {
                if (LeagueManager.isQueued(player.getUuid())) LeagueManager.leaveQueue(player);
                else LeagueManager.joinQueue(player);
                open(player);
            }
            case SLOT_BACK -> MenuGuiManager.open(player);
            case SLOT_CLOSE -> player.closeHandledScreen();
            default -> { /* 읽기 전용 */ }
        }
    }

    /** rankedInit 플레이어만, 점수 내림차순. */
    private static List<Map.Entry<UUID, PlayerProgress>> ranking(PoroMonState state) {
        List<Map.Entry<UUID, PlayerProgress>> list = new ArrayList<>();
        for (Map.Entry<UUID, PlayerProgress> e : state.all().entrySet()) {
            if (e.getValue().rankedInit) list.add(e);
        }
        list.sort((a, b) -> Integer.compare(b.getValue().rankedScore, a.getValue().rankedScore));
        return list;
    }

    private static String name(MinecraftServer server, UUID uuid) {
        ServerPlayerEntity online = server.getPlayerManager().getPlayer(uuid);
        if (online != null) return online.getGameProfile().getName();
        try {
            var cache = server.getUserCache();
            if (cache != null) {
                var prof = cache.getByUuid(uuid);
                if (prof.isPresent()) return prof.get().getName();
            }
        } catch (Throwable ignored) {}
        return uuid.toString().substring(0, 8);
    }
}
