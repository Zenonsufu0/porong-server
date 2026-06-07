package kr.poro.poromoncore.league;

import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.battles.model.actor.BattleActor;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.api.events.battles.BattleVictoryEvent;
import com.cobblemon.mod.common.battles.BattleBuilder;
import com.cobblemon.mod.common.battles.BattleFormat;
import com.cobblemon.mod.common.battles.BattleRegistry;
import com.cobblemon.mod.common.battles.BattleStartResult;
import com.cobblemon.mod.common.battles.actor.PlayerBattleActor;
import kr.poro.poromoncore.PoroMonCore;
import kr.poro.poromoncore.config.ConfigManager;
import kr.poro.poromoncore.config.SeasonConfig;
import kr.poro.poromoncore.data.PlayerProgress;
import kr.poro.poromoncore.data.PoroMonState;
import kr.poro.poromoncore.encounter.ArenaManager;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 정규리그 (league_season_design §4): 점수제 래더 + 실시간 큐 매칭(AI 없음) + lvl50 pvp.
 * 레벨 정규화 = BattleFormat.adjustLevel(Showdown, §7 옵션 A). 승리 감지 = BATTLE_VICTORY.
 * 매칭 윈도우는 대기 시간에 따라 확대, 동일 상대 재대전 쿨다운으로 파밍 방지.
 * 0.1 코어: 무효배틀(접속 끊김)=노카운트, 짜고지기 방지 미구현(TBD).
 */
public final class LeagueManager {
    private LeagueManager() {}

    /** 큐: 플레이어 → 큐 진입 틱. */
    private static final Map<UUID, Long> QUEUE = new ConcurrentHashMap<>();
    /** 진행 중 리그 배틀: 플레이어 → 대전 정보(상대·아레나·원위치). 양쪽 모두 등록. */
    private static final Map<UUID, BoutSide> BOUTS = new ConcurrentHashMap<>();
    /** 대전 중 접속종료자 → 재접속 시 복귀할 원위치. */
    private static final Map<UUID, ReturnLoc> PENDING = new ConcurrentHashMap<>();
    /** 재대전 쿨다운: 플레이어 → (상대 → 마지막 대전 틱). */
    private static final Map<UUID, Map<UUID, Long>> RECENT = new ConcurrentHashMap<>();

    private record ReturnLoc(String dim, double x, double y, double z) {}
    /** 한 플레이어 입장의 대전 정보(아레나 cell/corner는 양쪽 동일). */
    private record BoutSide(UUID opponent, int cell, BlockPos corner, ReturnLoc origin) {}

    public static void registerEvents() {
        CobblemonEvents.BATTLE_VICTORY.subscribe(Priority.NORMAL, LeagueManager::onVictory);
    }

    public static boolean isQueued(UUID uuid) { return QUEUE.containsKey(uuid); }

    // ── 큐 참가/취소 ─────────────────────────────────────────────
    public static void joinQueue(ServerPlayerEntity player) {
        SeasonConfig.RankedLeague cfg = ConfigManager.season().rankedLeague;
        PoroMonState state = PoroMonState.get(player.getServer());
        PlayerProgress p = state.getOrCreate(player.getUuid());

        if (p.badges.size() < cfg.requireBadges) {
            player.sendMessage(Text.literal("§c[정규리그] 자격 미달 — 배지 " + cfg.requireBadges
                    + "개 필요 (현재 " + p.badges.size() + "개)."), false);
            return;
        }
        if (BOUTS.containsKey(player.getUuid())) {
            player.sendMessage(Text.literal("§e[정규리그] 이미 리그 배틀 중입니다."), false);
            return;
        }
        if (BattleRegistry.getBattleByParticipatingPlayer(player) != null) {
            player.sendMessage(Text.literal("§e[정규리그] 다른 배틀 중에는 큐에 참가할 수 없습니다."), false);
            return;
        }
        if (QUEUE.containsKey(player.getUuid())) {
            player.sendMessage(Text.literal("§e[정규리그] 이미 큐에서 상대를 찾는 중입니다. §7(/poromon league leave 로 취소)"), false);
            return;
        }
        if (!p.rankedInit) {
            p.rankedInit = true;
            p.rankedScore = cfg.startScore;
            state.markDirty();
        }
        QUEUE.put(player.getUuid(), (long) player.getServer().getTicks());
        player.sendMessage(Text.literal("§a[정규리그] 큐 참가! 현재 점수 §e" + p.rankedScore
                + "§a. 상대를 찾는 중... §7(취소: /poromon league leave)"), false);
    }

    public static void leaveQueue(ServerPlayerEntity player) {
        if (QUEUE.remove(player.getUuid()) != null) {
            player.sendMessage(Text.literal("§7[정규리그] 큐에서 나왔습니다."), false);
        } else {
            player.sendMessage(Text.literal("§7[정규리그] 큐에 참가 중이 아닙니다."), false);
        }
    }

    /**
     * 접속 종료: 큐 제거 + 진행 중이면 노카운트 정리. 아레나 철거 + 온라인 상대 복귀,
     * 끊은 쪽은 PENDING에 원위치 저장(재접속 시 복귀 — 아레나가 철거돼 허공에 로그인하는 것 방지).
     */
    public static void onDisconnect(MinecraftServer server, UUID uuid) {
        QUEUE.remove(uuid);
        BoutSide side = BOUTS.get(uuid);
        if (side == null) return;
        PENDING.put(uuid, side.origin());                 // 끊은 쪽: 재접속 시 복귀
        ServerPlayerEntity opp = server.getPlayerManager().getPlayer(side.opponent());
        endBout(server, uuid, side.opponent(), side.cell(), side.corner(), opp);
    }

    /** 재접속 시 대전 중 끊겼던 플레이어를 원위치로 복귀(허공 로그인 방지). */
    public static void checkPendingReturn(ServerPlayerEntity player) {
        ReturnLoc loc = PENDING.remove(player.getUuid());
        if (loc != null) teleportBack(player.getServer(), player, loc);
    }

    /** 운영자 강제 해제: 큐/대전 정리 + 양쪽 원위치 복귀 + 아레나 철거. 처리 시 true. */
    public static boolean forceEnd(ServerPlayerEntity player) {
        BoutSide side = BOUTS.get(player.getUuid());
        if (side == null) return QUEUE.remove(player.getUuid()) != null;
        MinecraftServer server = player.getServer();
        ServerPlayerEntity opp = server.getPlayerManager().getPlayer(side.opponent());
        endBout(server, player.getUuid(), side.opponent(), side.cell(), side.corner(), opp);
        return true;
    }

    // ── 매칭 틱 (PoroMonCore에서 1초마다) ───────────────────────────
    public static void tick(MinecraftServer server) {
        if (QUEUE.size() < 2) return;
        SeasonConfig.RankedLeague cfg = ConfigManager.season().rankedLeague;
        long now = server.getTicks();

        // 온라인 + 미배틀 대기자만, 오래 기다린 순
        List<UUID> waiting = new ArrayList<>();
        for (Map.Entry<UUID, Long> e : QUEUE.entrySet()) {
            ServerPlayerEntity pl = server.getPlayerManager().getPlayer(e.getKey());
            if (pl == null) continue;
            waiting.add(e.getKey());
        }
        waiting.sort((a, b) -> Long.compare(QUEUE.getOrDefault(a, 0L), QUEUE.getOrDefault(b, 0L)));

        Set<UUID> paired = new HashSet<>();
        PoroMonState state = PoroMonState.get(server);
        for (UUID a : waiting) {
            if (paired.contains(a)) continue;
            int sa = state.getOrCreate(a).rankedScore;
            int wa = window(cfg, now - QUEUE.getOrDefault(a, now));
            UUID best = null;
            int bestDiff = Integer.MAX_VALUE;
            for (UUID b : waiting) {
                if (b.equals(a) || paired.contains(b)) continue;
                if (onCooldown(cfg, a, b, now)) continue;
                int sb = state.getOrCreate(b).rankedScore;
                int wb = window(cfg, now - QUEUE.getOrDefault(b, now));
                int diff = Math.abs(sa - sb);
                if (diff <= Math.max(wa, wb) && diff < bestDiff) { best = b; bestDiff = diff; }
            }
            if (best != null && startBattle(server, a, best)) {
                paired.add(a);
                paired.add(best);
            }
        }
    }

    private static int window(SeasonConfig.RankedLeague cfg, long waitedTicks) {
        double sec = Math.max(0, waitedTicks) / 20.0;
        return (int) Math.min(cfg.matchmaking.windowMax,
                cfg.matchmaking.windowStart + sec * cfg.matchmaking.windowStepPerSec);
    }

    private static boolean onCooldown(SeasonConfig.RankedLeague cfg, UUID a, UUID b, long now) {
        Map<UUID, Long> m = RECENT.get(a);
        if (m == null) return false;
        Long t = m.get(b);
        return t != null && (now - t) < cfg.matchmaking.rematchCooldownSeconds * 20L;
    }

    private static void recordRecent(UUID a, UUID b, long now) {
        RECENT.computeIfAbsent(a, k -> new ConcurrentHashMap<>()).put(b, now);
        RECENT.computeIfAbsent(b, k -> new ConcurrentHashMap<>()).put(a, now);
    }

    // ── 배틀 시작 (동적 아레나: 방 생성 → 양쪽 마주보게 텔레포트 → pvp1v1) ──────────
    private static boolean startBattle(MinecraftServer server, UUID aUuid, UUID bUuid) {
        ServerPlayerEntity a = server.getPlayerManager().getPlayer(aUuid);
        ServerPlayerEntity b = server.getPlayerManager().getPlayer(bUuid);
        if (a == null || b == null) return false;

        SeasonConfig.RankedLeague cfg = ConfigManager.season().rankedLeague;
        ServerWorld arena = server.getOverworld();
        int cell = ArenaManager.allocate();
        BlockPos corner = ArenaManager.corner(arena, cell);
        ReturnLoc oa = origin(a), ob = origin(b);
        try {
            ArenaManager.build(arena, corner);
            // 11×11 방 중앙축(x+5) 양 끝(z+2 / z+8)에 마주보게 배치
            double cx = corner.getX() + 5 + 0.5;
            double y = corner.getY() + 1;
            double za = corner.getZ() + 2 + 0.5, zb = corner.getZ() + 8 + 0.5;
            a.teleport(arena, cx, y, za, 0.0f, 0.0f);     // +Z(남) 바라봄 → 상대 방향
            b.teleport(arena, cx, y, zb, 180.0f, 0.0f);   // −Z(북) 바라봄

            // GEN_9_SINGLES 복제 후 레벨 정규화(싱글톤 보호: copy로 새 인스턴스).
            // 생성자 순서 = (mod, battleType, ruleSet, gen, adjustLevel) — adjustLevel만 cfg로 교체.
            BattleFormat base = BattleFormat.Companion.getGEN_9_SINGLES();
            BattleFormat format = base.copy(base.getMod(), base.getBattleType(),
                    new HashSet<>(base.getRuleSet()), base.getGen(), cfg.adjustLevel);
            BattleStartResult result = BattleBuilder.INSTANCE.pvp1v1(a, b, null, null, format);
            boolean ok = result.getClass().getSimpleName().toLowerCase().contains("success");
            if (!ok) {
                // 시작 실패: 원위치 복귀 + 아레나 철거, 둘 다 큐에 남겨 다음 틱 재시도(메시지 없음)
                teleportBack(server, a, oa);
                teleportBack(server, b, ob);
                ArenaManager.clear(arena, corner);
                ArenaManager.free(cell);
                return false;
            }
            QUEUE.remove(aUuid);
            QUEUE.remove(bUuid);
            BOUTS.put(aUuid, new BoutSide(bUuid, cell, corner, oa));
            BOUTS.put(bUuid, new BoutSide(aUuid, cell, corner, ob));
            recordRecent(aUuid, bUuid, server.getTicks());
            int sa = PoroMonState.get(server).getOrCreate(aUuid).rankedScore;
            int sb = PoroMonState.get(server).getOrCreate(bUuid).rankedScore;
            a.sendMessage(Text.literal("§b[정규리그] 매칭 성사! 상대: §f" + b.getGameProfile().getName()
                    + " §7(점수 " + sb + ") §8· Lv" + cfg.adjustLevel + " 정규화"), false);
            b.sendMessage(Text.literal("§b[정규리그] 매칭 성사! 상대: §f" + a.getGameProfile().getName()
                    + " §7(점수 " + sa + ") §8· Lv" + cfg.adjustLevel + " 정규화"), false);
            return true;
        } catch (Throwable t) {
            teleportBack(server, a, oa);
            teleportBack(server, b, ob);
            ArenaManager.clear(arena, corner);
            ArenaManager.free(cell);
            PoroMonCore.LOGGER.error("[League] 배틀 시작 실패", t);
            return false;
        }
    }

    private static ReturnLoc origin(ServerPlayerEntity p) {
        return new ReturnLoc(p.getServerWorld().getRegistryKey().getValue().toString(),
                p.getX(), p.getY(), p.getZ());
    }

    private static void teleportBack(MinecraftServer server, ServerPlayerEntity p, ReturnLoc loc) {
        if (p == null || loc == null) return;
        Identifier dimId = Identifier.tryParse(loc.dim());
        ServerWorld back = dimId == null ? server.getOverworld()
                : server.getWorld(RegistryKey.of(RegistryKeys.WORLD, dimId));
        if (back == null) back = server.getOverworld();
        p.teleport(back, loc.x(), loc.y(), loc.z(), p.getYaw(), p.getPitch());
    }

    /** 대전 종료 공통: 양쪽 BOUTS 해제 + 온라인 플레이어 원위치 복귀 + 아레나 철거·반납. */
    private static void endBout(MinecraftServer server, UUID u1, UUID u2, int cell, BlockPos corner,
                               ServerPlayerEntity online2) {
        BoutSide s1 = BOUTS.remove(u1);
        BoutSide s2 = BOUTS.remove(u2);
        ServerPlayerEntity p1 = server.getPlayerManager().getPlayer(u1);
        if (p1 != null && s1 != null) teleportBack(server, p1, s1.origin());
        if (online2 != null && s2 != null) teleportBack(server, online2, s2.origin());
        ArenaManager.clear(server.getOverworld(), corner);
        ArenaManager.free(cell);
    }

    // ── 결과 처리 ────────────────────────────────────────────────
    private static void onVictory(BattleVictoryEvent event) {
        try {
            ServerPlayerEntity winner = firstPlayer(event.getWinners());
            ServerPlayerEntity loser = firstPlayer(event.getLosers());
            if (winner == null || loser == null) return; // pvn/단측이면 리그 아님

            UUID w = winner.getUuid(), l = loser.getUuid();
            BoutSide bw = BOUTS.get(w);
            if (bw == null || !bw.opponent().equals(l)) return; // 우리 리그 배틀 아님

            MinecraftServer server = winner.getServer();
            SeasonConfig.RankedLeague cfg = ConfigManager.season().rankedLeague;
            PoroMonState state = PoroMonState.get(server);
            PlayerProgress pw = state.getOrCreate(w);
            PlayerProgress pl = state.getOrCreate(l);
            pw.rankedScore += cfg.winDelta;
            pw.rankedWins++;
            pl.rankedScore = Math.max(cfg.scoreFloor, pl.rankedScore + cfg.lossDelta);
            pl.rankedLosses++;
            state.markDirty();

            // 아레나 철거 + 양쪽 원위치 복귀(약간의 결과 확인 여유 없이 즉시 — 배틀 UI는 이미 종료)
            endBout(server, w, l, bw.cell(), bw.corner(), loser);

            winner.sendMessage(Text.literal("§a[정규리그] 승리! §e+" + cfg.winDelta
                    + " §7→ 점수 §e" + pw.rankedScore + " §7(" + pw.rankedWins + "승 " + pw.rankedLosses + "패)"), false);
            loser.sendMessage(Text.literal("§c[정규리그] 패배 §e" + cfg.lossDelta
                    + " §7→ 점수 §e" + pl.rankedScore + " §7(" + pl.rankedWins + "승 " + pl.rankedLosses + "패)"), false);
            PoroMonCore.LOGGER.info("[League] {} 승 vs {} 패 ({}→{} / {}→{})",
                    winner.getGameProfile().getName(), loser.getGameProfile().getName(),
                    pw.rankedScore - cfg.winDelta, pw.rankedScore,
                    pl.rankedScore - cfg.lossDelta, pl.rankedScore);
        } catch (Throwable t) {
            PoroMonCore.LOGGER.error("[League] onVictory 처리 실패", t);
        }
    }

    private static ServerPlayerEntity firstPlayer(Iterable<BattleActor> actors) {
        for (BattleActor actor : actors) {
            if (actor instanceof PlayerBattleActor pba && pba.getEntity() != null) return pba.getEntity();
        }
        return null;
    }
}
