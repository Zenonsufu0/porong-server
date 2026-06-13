package kr.zenon.rpg.boss.engine;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record BossResultSummary(
        String runId,
        String bossId,
        boolean clearSuccess,
        int phaseReached,
        long clearTimeSeconds,
        int remainingDeathCount,
        String failureReasonCode,
        List<Map<String, Object>> participantSummaryPlaceholder,
        long startedAt,
        int partySize
) {
    public Map<String, Object> toDashboardPayload() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("run_id", runId);
        payload.put("boss_id", bossId);
        payload.put("clear_success", clearSuccess);
        payload.put("phase_reached", phaseReached);
        payload.put("clear_time_seconds", clearTimeSeconds);
        payload.put("remaining_death_count", remainingDeathCount);
        payload.put("failure_reason_code", failureReasonCode);
        payload.put("started_at", startedAt);
        payload.put("party_size", partySize);
        payload.put("participant_summary", participantSummaryPlaceholder);
        return Map.copyOf(payload);
    }
}
