package com.poro.empire.pvp;

import com.poro.empire.pvp.db.PvpMatchLogRepository;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * PvP 매칭 큐 + 매치 진행 엔진 (CANON §2, §5).
 *
 * <ul>
 *   <li>자유/정규 큐: FIFO. 매칭 성사 즉시 아레나 텔레포트.</li>
 *   <li>매치 진행: 3분 타임아웃, 사망 = 패배, 자동 영지 귀환.</li>
 *   <li>정규대전 종료 시 PvpRatingService에 결과 반영.</li>
 * </ul>
 */
public final class PvpMatchService {

    public static final long TIMEOUT_TICKS = 20L * 60 * 3; // 3분
    public static final long AUTO_RETURN_TICKS = 20L * 3;  // 사망 후 3초

    private final Plugin              plugin;
    private final PvpArenaManager     arenaManager;
    private final PvpRatingService    ratingService;
    private PvpMatchLogRepository     matchLogRepository; // optional

    /** 자유대전 큐: FIFO. */
    private final Deque<UUID> freeQueue   = new ArrayDeque<>();
    /** 정규대전 큐: FIFO. */
    private final Deque<UUID> rankedQueue = new ArrayDeque<>();

    /** UUID → 현재 매치. */
    private final Map<UUID, PvpMatch>      playerToMatch = new ConcurrentHashMap<>();
    /** matchId → PvpMatch. */
    private final Map<UUID, PvpMatch>      activeMatches = new ConcurrentHashMap<>();
    /** matchId → 정규대전 PvpContext (양측). */
    private final Map<UUID, PvpContext[]>  rankedContexts = new ConcurrentHashMap<>();
    /** matchId → 타임아웃 task id. */
    private final Map<UUID, Integer>       timeoutTasks  = new ConcurrentHashMap<>();

    public PvpMatchService(Plugin plugin, PvpArenaManager arenaManager, PvpRatingService ratingService) {
        this.plugin       = plugin;
        this.arenaManager = arenaManager;
        this.ratingService = ratingService;
    }

    public void attachMatchLog(PvpMatchLogRepository repository) {
        this.matchLogRepository = repository;
    }

    /** startMatch 결과 — tryMatch와 startFriendly가 사유별로 처리. */
    public enum StartResult {
        SUCCESS,
        ALREADY_IN_MATCH,  // 한쪽 또는 양쪽이 이미 매치 중 — 큐 재삽입 X (해당 플레이어 큐 정리)
        NO_ARENA           // 빈 아레나 없음 — 큐 재삽입 O (재대기)
    }

    // ─── 큐 진입 ─────────────────────────────────────────────────────

    public boolean enqueue(Player player, PvpMatchType type) {
        if (playerToMatch.containsKey(player.getUniqueId())) {
            player.sendMessage("§c[PvP] 이미 대전 중입니다.");
            return false;
        }
        Deque<UUID> queue = queueFor(type);
        if (queue == null) {
            player.sendMessage("§c[PvP] 친선대전은 직접 신청만 가능합니다.");
            return false;
        }
        synchronized (queue) {
            if (queue.contains(player.getUniqueId())) {
                player.sendMessage("§c[PvP] 이미 대기열에 있습니다.");
                return false;
            }
            queue.add(player.getUniqueId());
            player.sendMessage("§a[PvP] " + typeName(type) + " 대기열 진입. §7(현재 " + queue.size() + "명)");
            tryMatch(type);
        }
        return true;
    }

    public boolean dequeue(Player player) {
        boolean removed = false;
        synchronized (freeQueue)   { removed |= freeQueue.remove(player.getUniqueId()); }
        synchronized (rankedQueue) { removed |= rankedQueue.remove(player.getUniqueId()); }
        if (removed) player.sendMessage("§7[PvP] 대기열에서 나갔습니다.");
        return removed;
    }

    private Deque<UUID> queueFor(PvpMatchType type) {
        return switch (type) {
            case FREE   -> freeQueue;
            case RANKED -> rankedQueue;
            case FRIENDLY -> null;
        };
    }

    private String typeName(PvpMatchType type) {
        return switch (type) {
            case FREE     -> "자유대전";
            case RANKED   -> "정규대전";
            case FRIENDLY -> "친선대전";
        };
    }

    // ─── 매칭 ────────────────────────────────────────────────────────

    private void tryMatch(PvpMatchType type) {
        Deque<UUID> queue = queueFor(type);
        if (queue == null) return;
        synchronized (queue) {
            // 큐 앞에서부터 정상 후보 2명을 찾는다.
            // 매치 중인 플레이어는 자동 제거 (stale 정리).
            UUID a = nextValidCandidate(queue);
            if (a == null) return;
            UUID b = nextValidCandidate(queue);
            if (b == null) {
                queue.addFirst(a);  // 단독 후보 — 다음 매칭 트리거 대기
                return;
            }
            Player pa = Bukkit.getPlayer(a);
            Player pb = Bukkit.getPlayer(b);
            if (pa == null) {        // a 오프라인 — 제거하고 b만 복귀
                queue.addFirst(b);
                return;
            }
            if (pb == null) {        // b 오프라인 — 제거하고 a만 복귀
                queue.addFirst(a);
                return;
            }
            StartResult result = startMatch(pa, pb, type);
            switch (result) {
                case SUCCESS -> { /* 양측 매치 진입 — 큐에서 빠진 그대로 */ }
                case NO_ARENA -> {
                    // 일시적 — 양측 큐 재대기. 매치 종료 시 endMatch에서 자동 재시도 트리거.
                    queue.addFirst(b);
                    queue.addFirst(a);
                }
                case ALREADY_IN_MATCH -> {
                    // 사전 필터로 막혔어야 하지만 race로 통과한 경우 — 안전망:
                    // 실제 매치 중인 쪽만 제거, 정상 쪽만 복귀.
                    if (!playerToMatch.containsKey(a)) queue.addFirst(a);
                    if (!playerToMatch.containsKey(b)) queue.addFirst(b);
                }
            }
        }
    }

    /** 큐에서 매치 중이지 않은 첫 후보를 꺼낸다. 매치 중인 stale 항목은 자동 제거. */
    private UUID nextValidCandidate(Deque<UUID> queue) {
        while (!queue.isEmpty()) {
            UUID uuid = queue.poll();
            if (!playerToMatch.containsKey(uuid)) return uuid;
            // stale: 매치 중인데 큐에 남아 있던 항목 — 그냥 버린다.
        }
        return null;
    }

    /** 친선대전 — 양측 합의 후 호출. */
    public StartResult startFriendly(Player a, Player b) {
        return startMatch(a, b, PvpMatchType.FRIENDLY);
    }

    private StartResult startMatch(Player a, Player b, PvpMatchType type) {
        // 매치 중복 검증 — 어느 한쪽이라도 이미 대전 중이면 거부
        if (playerToMatch.containsKey(a.getUniqueId())) {
            a.sendMessage("§c[PvP] 이미 대전 중입니다.");
            return StartResult.ALREADY_IN_MATCH;
        }
        if (playerToMatch.containsKey(b.getUniqueId())) {
            b.sendMessage("§c[PvP] 이미 대전 중입니다.");
            return StartResult.ALREADY_IN_MATCH;
        }
        UUID matchId = UUID.randomUUID();
        Optional<PvpArenaSlot> slot = arenaManager.tryAssign(matchId.toString());
        if (slot.isEmpty()) {
            a.sendMessage("§c[PvP] 빈 아레나가 없습니다. 잠시 후 다시 시도하세요.");
            b.sendMessage("§c[PvP] 빈 아레나가 없습니다. 잠시 후 다시 시도하세요.");
            return StartResult.NO_ARENA;
        }
        PvpArenaSlot arena = slot.get();
        PvpMatch match = new PvpMatch(
                matchId, type,
                a.getUniqueId(), a.getName(), a.getLocation().clone(),
                b.getUniqueId(), b.getName(), b.getLocation().clone(),
                arena.id(), System.currentTimeMillis()
        );
        activeMatches.put(matchId, match);
        playerToMatch.put(a.getUniqueId(), match);
        playerToMatch.put(b.getUniqueId(), match);

        // 정규대전: 가상 컨텍스트 주입
        if (type == PvpMatchType.RANKED) {
            rankedContexts.put(matchId, new PvpContext[]{
                    PvpContext.ranked(matchId, a.getUniqueId()),
                    PvpContext.ranked(matchId, b.getUniqueId())
            });
        }

        // 텔레포트
        a.teleport(arena.spawnA());
        b.teleport(arena.spawnB());
        a.sendMessage("§6[PvP] " + typeName(type) + " §7vs §f" + b.getName() + " §7시작! §c3분 제한");
        b.sendMessage("§6[PvP] " + typeName(type) + " §7vs §f" + a.getName() + " §7시작! §c3분 제한");
        if (type == PvpMatchType.RANKED) {
            a.sendMessage("§e[PvP] 정규대전 — 장비가 §f12강 IL 60§e으로 가상 동일화됩니다. (전투 시스템 적용)");
            b.sendMessage("§e[PvP] 정규대전 — 장비가 §f12강 IL 60§e으로 가상 동일화됩니다. (전투 시스템 적용)");
        }

        // 3분 타임아웃 스케줄
        int taskId = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (activeMatches.containsKey(matchId)) endByTimeout(matchId);
        }, TIMEOUT_TICKS).getTaskId();
        timeoutTasks.put(matchId, taskId);

        // 매치 시작 시 양측을 다른 큐에서도 제거 — 동시 매치 진입 방지
        synchronized (freeQueue)   { freeQueue.remove(a.getUniqueId());   freeQueue.remove(b.getUniqueId()); }
        synchronized (rankedQueue) { rankedQueue.remove(a.getUniqueId()); rankedQueue.remove(b.getUniqueId()); }

        return StartResult.SUCCESS;
    }

    // ─── 매치 종료 ───────────────────────────────────────────────────

    /** 사망 = 패배 (CANON §5). */
    public void onPlayerDeath(Player deceased) {
        PvpMatch match = playerToMatch.get(deceased.getUniqueId());
        if (match == null) return;
        UUID winnerUuid = match.opponentOf(deceased.getUniqueId());
        Player winner = Bukkit.getPlayer(winnerUuid);
        endMatch(match, winnerUuid, deceased.getUniqueId(), false, "사망");
    }

    private void endByTimeout(UUID matchId) {
        PvpMatch match = activeMatches.get(matchId);
        if (match == null) return;
        Player pa = Bukkit.getPlayer(match.playerA());
        Player pb = Bukkit.getPlayer(match.playerB());
        // 남은 HP 비율 비교
        double hpA = pa != null ? hpRatio(pa) : 0;
        double hpB = pb != null ? hpRatio(pb) : 0;
        if (Math.abs(hpA - hpB) < 0.01) {
            endMatch(match, null, null, true, "타임아웃 무승부");
        } else if (hpA > hpB) {
            endMatch(match, match.playerA(), match.playerB(), false, "타임아웃");
        } else {
            endMatch(match, match.playerB(), match.playerA(), false, "타임아웃");
        }
    }

    private double hpRatio(Player p) {
        var maxAttr = p.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
        double max = maxAttr != null ? maxAttr.getValue() : 20.0;
        return max > 0 ? p.getHealth() / max : 0;
    }

    /** 매치 종료 공통 처리: 점수 반영 + 텔레포트 + 정리. */
    public void endMatch(PvpMatch match, UUID winnerUuid, UUID loserUuid, boolean draw, String reason) {
        if (!activeMatches.containsKey(match.matchId())) return; // 중복 호출 방지

        // 타임아웃 task 취소
        Integer taskId = timeoutTasks.remove(match.matchId());
        if (taskId != null) Bukkit.getScheduler().cancelTask(taskId);

        // 정규대전 점수 갱신
        if (match.type() == PvpMatchType.RANKED && !draw) {
            String winnerName = winnerUuid.equals(match.playerA()) ? match.nameA() : match.nameB();
            String loserName  = loserUuid.equals(match.playerA())  ? match.nameA() : match.nameB();
            var winRating  = ratingService.recordWin(winnerUuid, winnerName);
            var lossRating = ratingService.recordLoss(loserUuid, loserName);
            Player wp = Bukkit.getPlayer(winnerUuid);
            Player lp = Bukkit.getPlayer(loserUuid);
            if (wp != null) wp.sendMessage("§6§l[승리!] §a+" + PvpRatingService.WIN_DELTA + "점 §7(현재 §e" + winRating.score() + "§7)");
            if (lp != null) lp.sendMessage("§c§l[패배] §c" + PvpRatingService.LOSS_DELTA + "점 §7(현재 §e" + lossRating.score() + "§7)");
        }

        // 결과 메시지 (자유/친선)
        if (match.type() != PvpMatchType.RANKED) {
            if (draw) {
                broadcastMatch(match, "§7[PvP] 대전 종료 — 무승부 §8(" + reason + ")");
            } else {
                String winnerName = winnerUuid.equals(match.playerA()) ? match.nameA() : match.nameB();
                broadcastMatch(match, "§6[PvP] 승자: §f" + winnerName + " §8(" + reason + ")");
            }
        }

        // DB 매치 로그
        if (matchLogRepository != null) {
            int durationS = (int) ((System.currentTimeMillis() - match.startedAt()) / 1000L);
            matchLogRepository.record(match.type(), winnerUuid, loserUuid, draw, durationS, reason);
        }

        // 즉시 정리 — 외부 추적 상태만. playerToMatch는 귀환 후 해제 (3초간 새 매치 진입 차단).
        activeMatches.remove(match.matchId());
        rankedContexts.remove(match.matchId());

        // 3초 후: 자동 영지 귀환 + playerToMatch 해제 + 아레나 해제 + 큐 재매칭 트리거
        // (귀환 전 release/playerToMatch 해제하면 새 매치가 충돌하거나 귀환이 새 매치에 영향)
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Player pa = Bukkit.getPlayer(match.playerA());
            Player pb = Bukkit.getPlayer(match.playerB());
            if (pa != null && pa.isOnline()) {
                if (pa.isDead()) { pa.spigot().respawn(); }
                pa.performCommand("is home");
            }
            if (pb != null && pb.isOnline()) {
                if (pb.isDead()) { pb.spigot().respawn(); }
                pb.performCommand("is home");
            }
            playerToMatch.remove(match.playerA());
            playerToMatch.remove(match.playerB());
            arenaManager.releaseByMatchId(match.matchId().toString());
            tryMatch(PvpMatchType.FREE);
            tryMatch(PvpMatchType.RANKED);
        }, AUTO_RETURN_TICKS);
    }

    private void broadcastMatch(PvpMatch match, String msg) {
        Player pa = Bukkit.getPlayer(match.playerA());
        Player pb = Bukkit.getPlayer(match.playerB());
        if (pa != null) pa.sendMessage(msg);
        if (pb != null) pb.sendMessage(msg);
    }

    // ─── 조회 ────────────────────────────────────────────────────────

    public Optional<PvpMatch> matchOf(UUID uuid) {
        return Optional.ofNullable(playerToMatch.get(uuid));
    }

    public boolean isInMatch(UUID uuid) {
        return playerToMatch.containsKey(uuid);
    }

    public Optional<PvpContext> contextOf(UUID matchId, UUID uuid) {
        PvpContext[] ctxs = rankedContexts.get(matchId);
        if (ctxs == null) return Optional.empty();
        for (PvpContext c : ctxs) {
            if (c.playerUuid().equals(uuid)) return Optional.of(c);
        }
        return Optional.empty();
    }

    /** 대전 중 서버 이탈 = 자동 패배 (CANON §5). */
    public void onPlayerQuit(Player player) {
        PvpMatch match = playerToMatch.get(player.getUniqueId());
        if (match == null) return;
        UUID winnerUuid = match.opponentOf(player.getUniqueId());
        endMatch(match, winnerUuid, player.getUniqueId(), false, "서버 이탈");
    }
}
