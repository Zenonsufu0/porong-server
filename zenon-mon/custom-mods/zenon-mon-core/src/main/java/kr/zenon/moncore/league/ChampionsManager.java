package kr.zenon.moncore.league;

import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.battles.model.actor.BattleActor;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.api.events.battles.BattleVictoryEvent;
import com.cobblemon.mod.common.battles.BattleBuilder;
import com.cobblemon.mod.common.battles.BattleRegistry;
import com.cobblemon.mod.common.battles.BattleStartResult;
import com.cobblemon.mod.common.battles.actor.PlayerBattleActor;
import kr.zenon.moncore.ZenonMonCore;
import kr.zenon.moncore.data.ZenonMonState;
import net.minecraft.block.Blocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 챔피언스리그 (league_season §6, IB-004): 운영자 시작 → 접속자 랜덤 단일 토너먼트.
 * 대진표 공지 → 1분 카운트다운 → 왼쪽부터 순차 매치(아레나 TP + pvp1v1 lvl50, 비참가자=관전석 TP)
 * → 승자 진출 → 결승 → 챔피언(챔피언 홀 기록). 관전 = Cobblemon 네이티브(근처 TP 후 인터랙트 휠).
 */
public final class ChampionsManager {
    private ChampionsManager() {}

    private enum Phase { IDLE, COUNTDOWN, FIGHTING, INTERMISSION }
    private static Phase phase = Phase.IDLE;

    private static final List<UUID> roundQueue = new ArrayList<>(); // 이번 라운드(대진 순서)
    private static final List<UUID> nextRound = new ArrayList<>();  // 진출자
    private static int roundNo = 0;
    private static UUID curA, curB;          // 현재 매치
    private static long phaseUntilTick;       // 카운트다운/인터미션 종료
    private static long matchStartTick;       // 매치 타임아웃 안전
    private static BlockPos arenaCenter;      // 경기장 중앙(바닥 위)

    private static final int HALF = 12;       // 25×25 평면 경기장
    private static final long COUNTDOWN_TICKS = 60 * 20L;
    private static final long INTERMISSION_TICKS = 5 * 20L;
    private static final long MATCH_TIMEOUT_TICKS = 5 * 60 * 20L;

    public static void registerEvents() {
        CobblemonEvents.BATTLE_VICTORY.subscribe(Priority.NORMAL, ChampionsManager::onVictory);
    }

    public static boolean isRunning() { return phase != Phase.IDLE; }

    // ── 시작/취소 ────────────────────────────────────────────────
    public static boolean start(MinecraftServer server) {
        if (phase != Phase.IDLE) return false;
        List<ServerPlayerEntity> online = server.getPlayerManager().getPlayerList();
        if (online.size() < 2) return false;

        roundQueue.clear(); nextRound.clear();
        for (ServerPlayerEntity p : online) roundQueue.add(p.getUuid());
        shuffle(roundQueue, server.getOverworld().getRandom());
        roundNo = 1;
        buildArena(server.getOverworld());
        phase = Phase.COUNTDOWN;
        phaseUntilTick = server.getTicks() + COUNTDOWN_TICKS;

        broadcast(server, "§5§l═══ 챔피언스리그 개최! ═══");
        broadcast(server, "§d참가자 §f" + roundQueue.size() + "명§d. 1분 후 시작합니다!");
        broadcastBracket(server);
        return true;
    }

    public static void cancel(MinecraftServer server) {
        if (phase == Phase.IDLE) return;
        endBattleIfAny(server, curA, curB);
        clearArena(server.getOverworld());
        teleportAllOut(server);
        reset();
        broadcast(server, "§7[챔피언스리그] 취소되었습니다.");
    }

    private static void reset() {
        phase = Phase.IDLE; roundQueue.clear(); nextRound.clear();
        curA = curB = null; arenaCenter = null; roundNo = 0;
    }

    // ── 틱 ──────────────────────────────────────────────────────
    public static void tick(MinecraftServer server) {
        if (phase == Phase.IDLE) return;
        long now = server.getTicks();
        switch (phase) {
            case COUNTDOWN -> { if (now >= phaseUntilTick) startNextMatch(server); }
            case INTERMISSION -> { if (now >= phaseUntilTick) startNextMatch(server); }
            case FIGHTING -> {
                if (now - matchStartTick > MATCH_TIMEOUT_TICKS) {
                    // 안전: 너무 길면 강제 종료 → A 진출 처리
                    broadcast(server, "§7[챔피언스리그] 시간 초과 — 매치 강제 종료.");
                    endBattleIfAny(server, curA, curB);
                    advanceWinner(server, curA, curB);
                }
            }
            default -> { }
        }
    }

    /** 다음 매치 시작(부전승/오프라인 자동 해소 포함). */
    private static void startNextMatch(MinecraftServer server) {
        while (true) {
            if (roundQueue.size() < 2) {
                if (roundQueue.size() == 1) nextRound.add(roundQueue.remove(0)); // 부전승
                if (nextRound.size() == 1) { finish(server, nextRound.get(0)); return; }
                if (nextRound.isEmpty()) { broadcast(server, "§7[챔피언스리그] 참가자가 없어 종료."); cleanupEnd(server); return; }
                roundQueue.addAll(nextRound); nextRound.clear();
                roundNo++;
                broadcast(server, "§d§l라운드 " + roundNo + " §f(" + roundQueue.size() + "명 진출)");
                continue;
            }
            UUID a = roundQueue.remove(0), b = roundQueue.remove(0);
            ServerPlayerEntity pa = server.getPlayerManager().getPlayer(a);
            ServerPlayerEntity pb = server.getPlayerManager().getPlayer(b);
            if (pa == null && pb == null) continue;
            if (pa == null) { nextRound.add(b); continue; }
            if (pb == null) { nextRound.add(a); continue; }
            startMatch(server, pa, pb);
            return;
        }
    }

    private static void startMatch(MinecraftServer server, ServerPlayerEntity a, ServerPlayerEntity b) {
        ServerWorld w = server.getOverworld();
        double cx = arenaCenter.getX() + 0.5, y = arenaCenter.getY(), cz = arenaCenter.getZ() + 0.5;
        a.teleport(w, cx, y, cz - 4, 0.0f, 0.0f);
        b.teleport(w, cx, y, cz + 4, 180.0f, 0.0f);

        // 비참가자 = 관전석(경기장 가장자리, 64칸 내)으로 분산 TP
        int i = 0;
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            if (p.getUuid().equals(a.getUuid()) || p.getUuid().equals(b.getUuid())) continue;
            double sx = arenaCenter.getX() + (-HALF + 1 + (i % (HALF * 2 - 1)));
            p.teleport(w, sx + 0.5, y, arenaCenter.getZ() - (HALF - 1) + 0.5, 0.0f, 0.0f);
            i++;
        }

        BattleStartResult res = BattleBuilder.INSTANCE.pvp1v1(a, b, null, null, LeagueManager.leagueFormat());
        boolean ok = res.getClass().getSimpleName().toLowerCase().contains("success");
        if (!ok) {
            // 시작 실패 → 둘 다 다음 라운드로 미루지 않고 a 진출(안전), 다음 매치
            ZenonMonCore.LOGGER.warn("[Champions] 매치 시작 실패: {} vs {}", a.getGameProfile().getName(), b.getGameProfile().getName());
            nextRound.add(a.getUuid());
            startNextMatch(server);
            return;
        }
        curA = a.getUuid(); curB = b.getUuid();
        phase = Phase.FIGHTING;
        matchStartTick = server.getTicks();
        broadcast(server, "§e§l▶ 매치: §f" + a.getGameProfile().getName() + " §7vs §f" + b.getGameProfile().getName());
        broadcast(server, "§7관전: 경기장 가장자리에서 전투 포켓몬을 보고 [인터랙트 휠]로 관전하세요.");
    }

    // ── 결과 ────────────────────────────────────────────────────
    private static void onVictory(BattleVictoryEvent event) {
        try {
            if (phase != Phase.FIGHTING) return;
            ServerPlayerEntity winner = firstPlayer(event.getWinners());
            ServerPlayerEntity loser = firstPlayer(event.getLosers());
            if (winner == null || loser == null) return;
            UUID w = winner.getUuid(), l = loser.getUuid();
            boolean isOurs = (w.equals(curA) && l.equals(curB)) || (w.equals(curB) && l.equals(curA));
            if (!isOurs) return;

            MinecraftServer server = winner.getServer();
            nextRound.add(w);
            curA = curB = null;
            broadcast(server, "§a§l승자: §f" + winner.getGameProfile().getName()
                    + " §7(패: " + loser.getGameProfile().getName() + ")");
            phase = Phase.INTERMISSION;
            phaseUntilTick = server.getTicks() + INTERMISSION_TICKS;
        } catch (Throwable t) {
            ZenonMonCore.LOGGER.error("[Champions] onVictory 처리 실패", t);
        }
    }

    /** 타임아웃/실패 시 한쪽(우선 a, 없으면 b) 진출 처리 후 다음. */
    private static void advanceWinner(MinecraftServer server, UUID a, UUID b) {
        UUID adv = server.getPlayerManager().getPlayer(a) != null ? a : b;
        if (adv != null) nextRound.add(adv);
        curA = curB = null;
        startNextMatch(server);
    }

    private static void finish(MinecraftServer server, UUID championId) {
        ServerPlayerEntity champ = server.getPlayerManager().getPlayer(championId);
        String name = champ != null ? champ.getGameProfile().getName() : championId.toString().substring(0, 8);
        ZenonMonState state = ZenonMonState.get(server);
        state.championHistory.add(name);
        state.markDirty();
        broadcast(server, "§6§l★═══ 최종 챔피언: " + name + " ═══★");
        broadcast(server, "§e챔피언 홀에 영구 등재되었습니다!");
        ZenonMonCore.LOGGER.info("[Champions] 최종 챔피언: {}", name);
        cleanupEnd(server);
    }

    private static void cleanupEnd(MinecraftServer server) {
        clearArena(server.getOverworld());
        teleportAllOut(server);
        reset();
    }

    // ── 경기장 빌드/철거 ─────────────────────────────────────────
    private static void buildArena(ServerWorld w) {
        int acx = (int) w.getWorldBorder().getCenterX();
        int acz = (int) w.getWorldBorder().getCenterZ();
        int ay = 250;
        arenaCenter = new BlockPos(acx, ay, acz);
        for (int dx = -HALF; dx <= HALF; dx++) {
            for (int dz = -HALF; dz <= HALF; dz++) {
                w.setBlockState(new BlockPos(acx + dx, ay - 1, acz + dz), Blocks.BLACKSTONE.getDefaultState());
                for (int dy = 0; dy < 5; dy++) w.setBlockState(new BlockPos(acx + dx, ay + dy, acz + dz), Blocks.AIR.getDefaultState());
                boolean edge = Math.abs(dx) == HALF || Math.abs(dz) == HALF;
                if (edge) w.setBlockState(new BlockPos(acx + dx, ay, acz + dz), Blocks.BLACKSTONE_WALL.getDefaultState());
            }
        }
    }

    private static void clearArena(ServerWorld w) {
        if (arenaCenter == null) return;
        int acx = arenaCenter.getX(), ay = arenaCenter.getY(), acz = arenaCenter.getZ();
        for (int dx = -HALF; dx <= HALF; dx++)
            for (int dy = -1; dy < 5; dy++)
                for (int dz = -HALF; dz <= HALF; dz++)
                    w.setBlockState(new BlockPos(acx + dx, ay + dy, acz + dz), Blocks.AIR.getDefaultState());
    }

    private static void teleportAllOut(MinecraftServer server) {
        ServerWorld ow = server.getOverworld();
        BlockPos spawn = ow.getSpawnPos();
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            // 경기장(y250) 근처에 있는 사람만 스폰으로 회수
            if (p.getServerWorld() == ow && p.getY() > 200) {
                p.teleport(ow, spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5, p.getYaw(), p.getPitch());
            }
        }
    }

    private static void endBattleIfAny(MinecraftServer server, UUID a, UUID b) {
        for (UUID u : new UUID[]{a, b}) {
            if (u == null) continue;
            ServerPlayerEntity p = server.getPlayerManager().getPlayer(u);
            if (p == null) continue;
            var battle = BattleRegistry.getBattleByParticipatingPlayer(p);
            if (battle != null) try { battle.end(); } catch (Throwable ignored) {}
        }
    }

    // ── 유틸 ────────────────────────────────────────────────────
    private static ServerPlayerEntity firstPlayer(Iterable<BattleActor> actors) {
        for (BattleActor actor : actors)
            if (actor instanceof PlayerBattleActor pba && pba.getEntity() != null) return pba.getEntity();
        return null;
    }

    private static void shuffle(List<UUID> list, Random rnd) {
        for (int i = list.size() - 1; i > 0; i--) {
            int j = rnd.nextInt(i + 1);
            UUID t = list.get(i); list.set(i, list.get(j)); list.set(j, t);
        }
    }

    private static void broadcastBracket(MinecraftServer server) {
        StringBuilder sb = new StringBuilder("§7대진: ");
        for (int i = 0; i < roundQueue.size(); i += 2) {
            String a = nameOf(server, roundQueue.get(i));
            String b = (i + 1 < roundQueue.size()) ? nameOf(server, roundQueue.get(i + 1)) : "(부전승)";
            sb.append("§f").append(a).append("§7vs§f").append(b).append("  ");
        }
        broadcast(server, sb.toString());
    }

    private static String nameOf(MinecraftServer server, UUID u) {
        ServerPlayerEntity p = server.getPlayerManager().getPlayer(u);
        return p != null ? p.getGameProfile().getName() : u.toString().substring(0, 6);
    }

    private static void broadcast(MinecraftServer server, String msg) {
        server.getPlayerManager().broadcast(Text.literal(msg), false);
    }
}
