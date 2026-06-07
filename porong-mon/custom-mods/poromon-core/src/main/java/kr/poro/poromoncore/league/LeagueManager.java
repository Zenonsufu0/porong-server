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
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

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
    /** 진행 중 리그 배틀: 플레이어 → 상대(양방향 기록). */
    private static final Map<UUID, UUID> ACTIVE = new ConcurrentHashMap<>();
    /** 재대전 쿨다운: 플레이어 → (상대 → 마지막 대전 틱). */
    private static final Map<UUID, Map<UUID, Long>> RECENT = new ConcurrentHashMap<>();

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
        if (ACTIVE.containsKey(player.getUuid())) {
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

    /** 접속 종료: 큐 제거 + 진행 중이면 노카운트로 정리(상대도 해제). */
    public static void onDisconnect(UUID uuid) {
        QUEUE.remove(uuid);
        UUID opp = ACTIVE.remove(uuid);
        if (opp != null) ACTIVE.remove(opp);
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

    // ── 배틀 시작 ────────────────────────────────────────────────
    private static boolean startBattle(MinecraftServer server, UUID aUuid, UUID bUuid) {
        ServerPlayerEntity a = server.getPlayerManager().getPlayer(aUuid);
        ServerPlayerEntity b = server.getPlayerManager().getPlayer(bUuid);
        if (a == null || b == null) return false;
        try {
            SeasonConfig.RankedLeague cfg = ConfigManager.season().rankedLeague;
            // GEN_9_SINGLES 복제 후 레벨 정규화(싱글톤 보호: copy로 새 인스턴스).
            // 생성자 순서 = (mod, battleType, ruleSet, gen, adjustLevel) — adjustLevel만 cfg로 교체.
            BattleFormat base = BattleFormat.Companion.getGEN_9_SINGLES();
            BattleFormat format = base.copy(base.getMod(), base.getBattleType(),
                    new HashSet<>(base.getRuleSet()), base.getGen(), cfg.adjustLevel);

            BattleStartResult result = BattleBuilder.INSTANCE.pvp1v1(a, b, null, null, format);
            boolean ok = result.getClass().getSimpleName().toLowerCase().contains("success");
            if (!ok) {
                // 시작 실패: 둘 다 큐에 남겨두고 다음 틱 재시도(스팸 방지 위해 메시지 없음)
                return false;
            }
            QUEUE.remove(aUuid);
            QUEUE.remove(bUuid);
            ACTIVE.put(aUuid, bUuid);
            ACTIVE.put(bUuid, aUuid);
            recordRecent(aUuid, bUuid, server.getTicks());
            int sa = PoroMonState.get(server).getOrCreate(aUuid).rankedScore;
            int sb = PoroMonState.get(server).getOrCreate(bUuid).rankedScore;
            a.sendMessage(Text.literal("§b[정규리그] 매칭 성사! 상대: §f" + b.getGameProfile().getName()
                    + " §7(점수 " + sb + ") §8· Lv" + cfg.adjustLevel + " 정규화"), false);
            b.sendMessage(Text.literal("§b[정규리그] 매칭 성사! 상대: §f" + a.getGameProfile().getName()
                    + " §7(점수 " + sa + ") §8· Lv" + cfg.adjustLevel + " 정규화"), false);
            return true;
        } catch (Throwable t) {
            PoroMonCore.LOGGER.error("[League] 배틀 시작 실패", t);
            return false;
        }
    }

    // ── 결과 처리 ────────────────────────────────────────────────
    private static void onVictory(BattleVictoryEvent event) {
        try {
            ServerPlayerEntity winner = firstPlayer(event.getWinners());
            ServerPlayerEntity loser = firstPlayer(event.getLosers());
            if (winner == null || loser == null) return; // pvn/단측이면 리그 아님

            UUID w = winner.getUuid(), l = loser.getUuid();
            UUID wOpp = ACTIVE.get(w);
            if (wOpp == null || !wOpp.equals(l)) return; // 우리 리그 배틀 아님
            ACTIVE.remove(w);
            ACTIVE.remove(l);

            SeasonConfig.RankedLeague cfg = ConfigManager.season().rankedLeague;
            PoroMonState state = PoroMonState.get(winner.getServer());
            PlayerProgress pw = state.getOrCreate(w);
            PlayerProgress pl = state.getOrCreate(l);
            pw.rankedScore += cfg.winDelta;
            pw.rankedWins++;
            pl.rankedScore = Math.max(cfg.scoreFloor, pl.rankedScore + cfg.lossDelta);
            pl.rankedLosses++;
            state.markDirty();

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
