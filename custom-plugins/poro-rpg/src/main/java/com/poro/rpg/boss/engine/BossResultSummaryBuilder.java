package com.poro.rpg.boss.engine;

import com.poro.rpg.common.time.TimeProvider;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class BossResultSummaryBuilder {
    private final TimeProvider timeProvider;

    public BossResultSummaryBuilder(TimeProvider timeProvider) {
        this.timeProvider = Objects.requireNonNull(timeProvider, "timeProvider");
    }

    public BossResultSummary fromRun(BossRun run) {
        Instant endedAt = run.endedAt() == null ? timeProvider.nowInstant() : run.endedAt();
        long clearTimeSeconds = Math.max(0L, Duration.between(run.enteredAt(), endedAt).toSeconds());

        List<Map<String, Object>> participantSummary = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : run.participantDeaths().entrySet()) {
            if (run.isAbandoned(entry.getKey())) continue; // 이탈자는 보상·종료 텔레포트 대상에서 제외 (DL-129 추가#20)
            Map<String, Object> placeholder = new LinkedHashMap<>();
            placeholder.put("user_id", entry.getKey());
            placeholder.put("damage_total", 0L);
            placeholder.put("damage_share", 0.0d);
            placeholder.put("deaths", entry.getValue());
            placeholder.put("stagger_contribution", 0.0d);
            participantSummary.add(Map.copyOf(placeholder));
        }

        return new BossResultSummary(
                run.runId(),
                run.bossId(),
                run.clearSuccess(),
                run.phaseReached(),
                clearTimeSeconds,
                run.remainingDeathCount(),
                run.failureReasonCode(),
                List.copyOf(participantSummary),
                run.enteredAt().getEpochSecond(),
                run.partySize()
        );
    }
}
