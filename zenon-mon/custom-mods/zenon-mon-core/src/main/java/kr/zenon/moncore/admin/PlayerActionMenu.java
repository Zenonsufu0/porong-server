package kr.zenon.moncore.admin;

import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.battles.BattleRegistry;
import kr.zenon.moncore.battle.BattleTowerService;
import kr.zenon.moncore.data.PlayerProgress;
import kr.zenon.moncore.data.ZenonMonState;
import kr.zenon.moncore.economy.EconomyBridge;
import kr.zenon.moncore.encounter.EncounterService;
import kr.zenon.moncore.gym.GymBattleService;
import kr.zenon.moncore.gym.GymInfo;
import kr.zenon.moncore.item.MenuItemManager;
import kr.zenon.moncore.menu.MenuIcons;
import kr.zenon.moncore.menu.ServerMenuHandler;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;
import java.util.UUID;

/** 운영자 GUI — 특정 플레이어 관리(골드/배지/패스/진행/강제해제). */
public final class PlayerActionMenu {
    private PlayerActionMenu() {}

    private static final int GOLD_1K = 19, GOLD_10K = 20, GOLD_100K = 21;
    private static final int BADGE_ALL = 23, BADGE_CLEAR = 24;
    private static final int PASS = 29, PROGRESS = 30, RELEASE = 32;
    private static final int BACK = 49;

    public static void open(ServerPlayerEntity admin, UUID targetId) {
        ServerMenuHandler.show(admin, Text.literal("플레이어 관리").formatted(Formatting.RED),
                inv -> populate(inv, admin, targetId), (p, slot, btn, sh) -> onClick(p, slot, targetId));
    }

    private static ServerPlayerEntity target(ServerPlayerEntity admin, UUID id) {
        return admin.getServer().getPlayerManager().getPlayer(id);
    }

    private static void populate(Inventory inv, ServerPlayerEntity admin, UUID targetId) {
        for (int i = 0; i < ServerMenuHandler.DISPLAY_SIZE; i++) inv.setStack(i, MenuIcons.pane());
        inv.setStack(BACK, MenuIcons.icon(Items.ARROW, "§e← 목록", List.of()));

        ServerPlayerEntity t = target(admin, targetId);
        if (t == null) {
            inv.setStack(22, MenuIcons.icon(Items.BARRIER, "§c오프라인", List.of("§7접속 중이 아닙니다")));
            return;
        }
        PlayerProgress p = ZenonMonState.get(admin.getServer()).getOrCreate(targetId);
        inv.setStack(4, MenuIcons.icon(Items.PLAYER_HEAD, "§e" + t.getGameProfile().getName(),
                List.of("§7골드: §6" + p.balance, "§7배지: §f" + p.badges.size() + "/8",
                        "§7배틀타워: §f" + p.battleTowerHighestClearedFloor)));

        inv.setStack(GOLD_1K, MenuIcons.icon(Items.GOLD_NUGGET, "§6골드 +1,000", List.of("§e클릭")));
        inv.setStack(GOLD_10K, MenuIcons.icon(Items.GOLD_INGOT, "§6골드 +10,000", List.of("§e클릭")));
        inv.setStack(GOLD_100K, MenuIcons.icon(Items.GOLD_BLOCK, "§6골드 +100,000", List.of("§e클릭")));
        inv.setStack(BADGE_ALL, MenuIcons.icon(Items.NETHER_STAR, "§a배지 전체 부여", List.of("§7관장 8 전부", "§e클릭")));
        inv.setStack(BADGE_CLEAR, MenuIcons.icon(Items.IRON_BARS, "§c배지 전체 초기화", List.of("§e클릭")));
        inv.setStack(PASS, MenuIcons.icon(Items.CLOCK, "§b리그 패스 재지급", List.of("§e클릭")));
        inv.setStack(PROGRESS, MenuIcons.icon(Items.BOOK, "§b진행도 조회", List.of("§7채팅으로 출력", "§e클릭")));
        inv.setStack(RELEASE, MenuIcons.icon(Items.TNT, "§c갇힘 강제 해제", List.of("§7조우/배틀 종료·복귀", "§e클릭")));
    }

    private static void onClick(ServerPlayerEntity admin, int slot, UUID targetId) {
        if (slot == BACK) { PlayerAdminMenu.open(admin); return; }
        ServerPlayerEntity t = target(admin, targetId);
        if (t == null) { admin.sendMessage(Text.literal("§c[운영자] 대상이 오프라인입니다."), true); return; }
        ZenonMonState state = ZenonMonState.get(admin.getServer());
        PlayerProgress p = state.getOrCreate(targetId);

        switch (slot) {
            case GOLD_1K -> giveGold(admin, t, 1000);
            case GOLD_10K -> giveGold(admin, t, 10000);
            case GOLD_100K -> giveGold(admin, t, 100000);
            case BADGE_ALL -> {
                for (GymInfo.Gym g : GymInfo.GYMS) p.badges.add(g.id());
                state.markDirty();
                admin.sendMessage(Text.literal("§a[운영자] " + t.getGameProfile().getName() + " 배지 8 전체 부여"), true);
            }
            case BADGE_CLEAR -> {
                p.badges.clear(); state.markDirty();
                admin.sendMessage(Text.literal("§a[운영자] " + t.getGameProfile().getName() + " 배지 초기화"), true);
            }
            case PASS -> {
                MenuItemManager.ensure(t);
                admin.sendMessage(Text.literal("§a[운영자] 리그 패스 재지급: " + t.getGameProfile().getName()), true);
            }
            case PROGRESS -> admin.sendMessage(Text.literal("§e[" + t.getGameProfile().getName()
                    + "] 골드 " + p.balance + " / 배지 " + p.badges.size() + "/8 / 타워 " + p.battleTowerHighestClearedFloor
                    + " / 제단 " + p.altarsUnlocked.size()), false);
            case RELEASE -> {
                PokemonBattle b = BattleRegistry.getBattleByParticipatingPlayer(t);
                if (b != null) try { b.end(); } catch (Throwable ignored) {}
                GymBattleService.forceEnd(t); BattleTowerService.forceEnd(t); EncounterService.forceEnd(t);
                admin.sendMessage(Text.literal("§a[운영자] " + t.getGameProfile().getName() + " 갇힘 해제"), true);
            }
            default -> { return; }
        }
        open(admin, targetId); // 갱신
    }

    private static void giveGold(ServerPlayerEntity admin, ServerPlayerEntity t, long amount) {
        EconomyBridge.deposit(t, amount, "admin_gui");
        admin.sendMessage(Text.literal("§a[운영자] " + t.getGameProfile().getName() + " +"
                + amount + " (잔액 " + EconomyBridge.getBalance(t) + ")"), true);
    }
}
