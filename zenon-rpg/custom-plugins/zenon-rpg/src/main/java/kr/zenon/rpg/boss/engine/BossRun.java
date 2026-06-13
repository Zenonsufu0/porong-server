package kr.zenon.rpg.boss.engine;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class BossRun {
    private final String runId;
    private final String bossId;
    private final String leaderUserId;
    private final List<String> participants;
    private final Instant enteredAt;
    private final int totalDeathCount;
    private final Map<String, Integer> participantDeaths = new LinkedHashMap<>();
    /** 도중 포기(이탈)한 참가자 — 보상·HP 스케일·종료 판정에서 제외 (DL-129 추가#20). */
    private final Set<String> abandoned = new LinkedHashSet<>();
    private final Map<String, Instant> patternLastUsedAt = new LinkedHashMap<>();
    private final Deque<String> forcedPatternQueue = new ArrayDeque<>();
    private final Set<Integer> enteredPhases = new LinkedHashSet<>();

    private Instant endedAt;
    private int currentPhase;
    private int phaseReached;
    private double bossHpPercent;
    private int remainingDeathCount;
    private int totalDeaths;
    private String lastPatternId;
    private int consecutivePatternUse;
    private boolean berserkEntered;
    private boolean clearSuccess;
    private boolean ended;
    private String failureReasonCode;

    public BossRun(
            String runId,
            String bossId,
            String leaderUserId,
            List<String> participants,
            Instant enteredAt,
            int totalDeathCount
    ) {
        this.runId = runId;
        this.bossId = bossId;
        this.leaderUserId = leaderUserId;
        this.participants = List.copyOf(participants);
        this.enteredAt = enteredAt;
        this.totalDeathCount = totalDeathCount;
        this.remainingDeathCount = totalDeathCount;
        this.bossHpPercent = 100.0d;
        this.currentPhase = 1;
        this.phaseReached = 1;
        for (String participant : participants) {
            participantDeaths.put(participant, 0);
        }
    }

    public String runId() {
        return runId;
    }

    public String bossId() {
        return bossId;
    }

    public String leaderUserId() {
        return leaderUserId;
    }

    public List<String> participants() {
        return participants;
    }

    // ─── 이탈(포기) 추적 (DL-129 추가#20) ───────────────────────────
    public void markAbandoned(String userId) { if (userId != null) abandoned.add(userId); }
    public boolean isAbandoned(String userId) { return abandoned.contains(userId); }
    /** 남은(미이탈) 참가자 수. */
    public int activeCount() {
        return (int) participants.stream().filter(p -> !abandoned.contains(p)).count();
    }

    public int partySize() {
        return participants.size();
    }

    public Instant enteredAt() {
        return enteredAt;
    }

    public Instant endedAt() {
        return endedAt;
    }

    public int totalDeathCount() {
        return totalDeathCount;
    }

    public int remainingDeathCount() {
        return remainingDeathCount;
    }

    public int totalDeaths() {
        return totalDeaths;
    }

    public int currentPhase() {
        return currentPhase;
    }

    public int phaseReached() {
        return phaseReached;
    }

    public double bossHpPercent() {
        return bossHpPercent;
    }

    public String lastPatternId() {
        return lastPatternId;
    }

    public int consecutivePatternUse() {
        return consecutivePatternUse;
    }

    public boolean berserkEntered() {
        return berserkEntered;
    }

    public boolean clearSuccess() {
        return clearSuccess;
    }

    public boolean ended() {
        return ended;
    }

    public String failureReasonCode() {
        return failureReasonCode;
    }

    public Map<String, Integer> participantDeaths() {
        return Map.copyOf(participantDeaths);
    }

    public List<String> forcedPatternQueue() {
        return List.copyOf(new ArrayList<>(forcedPatternQueue));
    }

    public Map<String, Instant> patternLastUsedAt() {
        return Map.copyOf(patternLastUsedAt);
    }

    void setBossHpPercent(double value) {
        bossHpPercent = Math.max(0.0d, Math.min(100.0d, value));
    }

    void setCurrentPhase(int value) {
        currentPhase = Math.max(1, value);
        phaseReached = Math.max(phaseReached, currentPhase);
    }

    void markPhaseEntered(int phaseNo) {
        enteredPhases.add(phaseNo);
        phaseReached = Math.max(phaseReached, phaseNo);
    }

    boolean hasEnteredPhase(int phaseNo) {
        return enteredPhases.contains(phaseNo);
    }

    void enqueueForcedPattern(String patternId) {
        forcedPatternQueue.addLast(patternId);
    }

    String pollForcedPattern() {
        return forcedPatternQueue.pollFirst();
    }

    void markPatternUsed(String patternId, Instant usedAt) {
        patternLastUsedAt.put(patternId, usedAt);
        if (patternId.equals(lastPatternId)) {
            consecutivePatternUse++;
        } else {
            lastPatternId = patternId;
            consecutivePatternUse = 1;
        }
    }

    Instant lastUsedAt(String patternId) {
        return patternLastUsedAt.get(patternId);
    }

    void markBerserkEntered() {
        berserkEntered = true;
    }

    void registerDeath(String userId) {
        if (!participantDeaths.containsKey(userId)) {
            return;
        }
        participantDeaths.put(userId, participantDeaths.getOrDefault(userId, 0) + 1);
        totalDeaths++;
        if (remainingDeathCount > 0) {
            remainingDeathCount--;
        }
    }

    void end(Instant endedAt, boolean clearSuccess, String failureReasonCode) {
        this.ended = true;
        this.endedAt = endedAt;
        this.clearSuccess = clearSuccess;
        this.failureReasonCode = failureReasonCode == null ? "" : failureReasonCode;
    }
}
