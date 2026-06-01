package com.poro.rpg.boss.engine;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class BossPhaseController {
    private final BossPatternRegistry patternRegistry;

    public BossPhaseController(BossPatternRegistry patternRegistry) {
        this.patternRegistry = Objects.requireNonNull(patternRegistry, "patternRegistry");
    }

    public PhaseUpdate initialize(BossRun run) {
        run.setBossHpPercent(100.0d);
        run.setCurrentPhase(1);
        run.markPhaseEntered(1);
        List<String> forced = patternRegistry.forcedByPhaseEnter(run.bossId(), 1).stream()
                .map(BossPattern::patternId)
                .toList();
        return new PhaseUpdate(1, List.of(1), forced);
    }

    public PhaseUpdate evaluate(BossRun run, double bossHpPercent) {
        run.setBossHpPercent(bossHpPercent);

        int previousPhase = run.currentPhase();
        int resolvedPhase = resolvePhase(run.bossId(), bossHpPercent);
        if (resolvedPhase <= previousPhase) {
            return new PhaseUpdate(previousPhase, List.of(), List.of());
        }

        List<Integer> enteredPhases = new ArrayList<>();
        List<String> forcedPatternIds = new ArrayList<>();
        for (int phaseNo = previousPhase + 1; phaseNo <= resolvedPhase; phaseNo++) {
            if (run.hasEnteredPhase(phaseNo)) {
                continue;
            }
            run.setCurrentPhase(phaseNo);
            run.markPhaseEntered(phaseNo);
            enteredPhases.add(phaseNo);
            patternRegistry.forcedByPhaseEnter(run.bossId(), phaseNo).stream()
                    .map(BossPattern::patternId)
                    .forEach(forcedPatternIds::add);
        }

        return new PhaseUpdate(run.currentPhase(), List.copyOf(enteredPhases), List.copyOf(forcedPatternIds));
    }

    private int resolvePhase(String bossId, double hpPercent) {
        Map<Integer, Double> thresholdByPhase = new LinkedHashMap<>();
        for (BossPattern pattern : patternRegistry.findByBoss(bossId)) {
            if (pattern.phaseNo() <= 0) {
                continue;
            }
            thresholdByPhase.merge(pattern.phaseNo(), pattern.unlockHpThreshold(), Math::max);
        }
        if (thresholdByPhase.isEmpty()) {
            return 1;
        }

        int resolved = 1;
        List<Map.Entry<Integer, Double>> sorted = thresholdByPhase.entrySet().stream()
                .sorted(Comparator.comparingInt(Map.Entry::getKey))
                .toList();
        for (Map.Entry<Integer, Double> entry : sorted) {
            if (hpPercent <= entry.getValue()) {
                resolved = Math.max(resolved, entry.getKey());
            }
        }
        return resolved;
    }

    public record PhaseUpdate(
            int currentPhase,
            List<Integer> enteredPhases,
            List<String> forcedPatternIds
    ) {
    }
}
