package kr.poro.poromoncore.admin;

import com.cobblemon.mod.common.battles.BattleRegistry;
import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import kr.poro.poromoncore.battle.BattleTowerService;
import kr.poro.poromoncore.encounter.EncounterService;
import kr.poro.poromoncore.event.EventManager;
import kr.poro.poromoncore.gym.GymBattleService;
import kr.poro.poromoncore.menu.MenuIcons;
import kr.poro.poromoncore.menu.ServerMenuHandler;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

/**
 * 운영자 GUI (op2). 전역 이벤트 부스트 토글 + 갇힘(조우방/배틀) 강제 해제.
 * 0.1: 부스트 2종 + 강제 해제. (플레이어 관리·공지·모니터는 추후 그룹.)
 */
public final class AdminMenu {
    private AdminMenu() {}

    private static final int SLOT_XP = 20;
    private static final int SLOT_GOLD = 22;
    private static final int SLOT_RELEASE = 24;
    private static final int SLOT_PLAYERS = 30;
    private static final int SLOT_ANNOUNCE = 32;
    private static final int SLOT_ENCOUNTER = 38;
    private static final int SLOT_APEX = 40;
    private static final int SLOT_MONITOR = 42;
    private static final int SLOT_CLOSE = 49;

    public static void open(ServerPlayerEntity player) {
        ServerMenuHandler.show(player, Text.literal("운영자 패널").formatted(Formatting.RED),
                AdminMenu::populate, AdminMenu::onClick);
    }

    private static void populate(net.minecraft.inventory.Inventory inv) {
        for (int i = 0; i < ServerMenuHandler.DISPLAY_SIZE; i++) inv.setStack(i, MenuIcons.pane());

        boolean xp = EventManager.isXpBoost();
        boolean gold = EventManager.isGoldBoost();
        inv.setStack(SLOT_XP, MenuIcons.icon(xp ? Items.EXPERIENCE_BOTTLE : Items.GLASS_BOTTLE,
                (xp ? "§a경험치 ×2 ON" : "§7경험치 ×2 OFF"),
                List.of("§7전역 경험치 2배 이벤트", "§e클릭 — 토글")));
        inv.setStack(SLOT_GOLD, MenuIcons.icon(gold ? Items.GOLD_BLOCK : Items.GOLD_NUGGET,
                (gold ? "§a골드 ×2 ON" : "§7골드 ×2 OFF"),
                List.of("§7매입가(상점 판매) 2배", "§8※ 야생 처치 보상은 구현 후 적용", "§e클릭 — 토글")));
        inv.setStack(SLOT_RELEASE, MenuIcons.icon(Items.TNT, "§c갇힘 강제 해제(전체)",
                List.of("§7조우방/배틀에 갇힌 모든 접속자를", "§7배틀 종료 + 복귀 처리", "§e클릭 — 실행")));
        inv.setStack(SLOT_PLAYERS, MenuIcons.icon(Items.PLAYER_HEAD, "§e플레이어 관리",
                List.of("§7골드·배지·패스·진행·강제해제", "§e클릭 — 접속자 목록")));
        inv.setStack(SLOT_ANNOUNCE, MenuIcons.icon(Items.OAK_SIGN, "§b공지 보내기",
                List.of("§7전체 채팅 공지", "§e클릭 — 내용 입력")));
        inv.setStack(SLOT_ENCOUNTER, MenuIcons.icon(Items.ENDER_EYE, "§d조우 강제 소환",
                List.of("§7풀 선택 → 본인에게 소환(무료)", "§e클릭")));
        boolean apex = EventManager.isApexBoost();
        inv.setStack(SLOT_APEX, MenuIcons.icon(apex ? Items.NETHER_STAR : Items.ENDER_PEARL,
                (apex ? "§a컨셉 최상급 ×2 ON" : "§7컨셉 최상급 ×2 OFF"),
                List.of("§7컨셉 조우권 최상급 가중 2배 이벤트", "§8(제단 확률 표시도 반영)", "§e클릭 — 토글")));
        inv.setStack(SLOT_MONITOR, MenuIcons.icon(Items.GOLD_BLOCK, "§6경제 모니터",
                List.of("§7골드 유입/유출 통계", "§e클릭")));
        inv.setStack(SLOT_CLOSE, MenuIcons.icon(Items.BARRIER, "§c닫기", List.of()));
    }

    private static void onClick(ServerPlayerEntity player, int slot, int button, boolean shift) {
        switch (slot) {
            case SLOT_XP -> { EventManager.toggleXp(); open(player); }
            case SLOT_GOLD -> { EventManager.toggleGold(); open(player); }
            case SLOT_APEX -> { EventManager.toggleApex(); open(player); }
            case SLOT_RELEASE -> { releaseAll(player.getServer());
                player.sendMessage(Text.literal("§a[운영자] 갇힘 강제 해제를 실행했습니다."), false); }
            case SLOT_PLAYERS -> PlayerAdminMenu.open(player);
            case SLOT_ANNOUNCE -> promptAnnounce(player);
            case SLOT_ENCOUNTER -> EncounterAdminMenu.open(player);
            case SLOT_MONITOR -> EconomyMonitorMenu.open(player);
            case SLOT_CLOSE -> player.closeHandledScreen();
            default -> { /* 무시 */ }
        }
    }

    /** 공지: 채팅으로 내용 입력 → 전체 방송. */
    private static void promptAnnounce(ServerPlayerEntity player) {
        player.closeHandledScreen();
        player.sendMessage(Text.literal("§b[운영자] 공지 내용을 채팅에 입력하세요. §7(취소: '취소')"), false);
        kr.poro.poromoncore.util.ChatInputManager.await(player, msg -> {
            if (msg.equals("취소") || msg.isBlank()) {
                player.sendMessage(Text.literal("§7[운영자] 공지를 취소했습니다."), false);
                return;
            }
            player.getServer().getPlayerManager().broadcast(
                    Text.literal("§6§l[공지] §r§e" + msg), false);
        });
    }

    /** 모든 접속자의 조우/배틀을 강제 종료·복귀. */
    private static void releaseAll(net.minecraft.server.MinecraftServer server) {
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            PokemonBattle battle = BattleRegistry.getBattleByParticipatingPlayer(p);
            if (battle != null) {
                try { battle.end(); } catch (Throwable ignored) {}
            }
            GymBattleService.forceEnd(p);
            BattleTowerService.forceEnd(p);
            EncounterService.forceEnd(p);
            kr.poro.poromoncore.league.LeagueManager.forceEnd(p);
        }
    }
}
