package com.poro.rpg.boss.engine;

import com.poro.rpg.common.registry.InMemoryRegistry;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class BossPatternRegistry {
    private final InMemoryRegistry<String, BossPattern> delegate = new InMemoryRegistry<>();

    public void register(BossPattern pattern) {
        delegate.register(pattern.compositeKey(), pattern);
    }

    public Optional<BossPattern> find(String bossId, String patternId) {
        return delegate.find(compositeKey(bossId, patternId));
    }

    public List<BossPattern> findByBoss(String bossId) {
        String normalizedBossId = normalized(bossId);
        return delegate.snapshot().values().stream()
                .filter(pattern -> pattern.bossId().equals(normalizedBossId))
                .sorted(Comparator.comparingInt(BossPattern::phaseNo).thenComparing(BossPattern::patternId))
                .toList();
    }

    public List<BossPattern> findByBossAndPhase(String bossId, int phaseNo) {
        return findByBoss(bossId).stream()
                .filter(pattern -> pattern.phaseNo() == phaseNo)
                .toList();
    }

    public List<BossPattern> forcedByPhaseEnter(String bossId, int phaseNo) {
        return findByBossAndPhase(bossId, phaseNo).stream()
                .filter(BossPattern::forced)
                .filter(pattern -> "PHASE_ENTER".equals(pattern.conditionType()))
                .toList();
    }

    public List<BossPattern> forcedByHpZero(String bossId) {
        return findByBoss(bossId).stream()
                .filter(BossPattern::forced)
                .filter(pattern -> "HP_ZERO".equals(pattern.conditionType()))
                .toList();
    }

    public List<Integer> phaseNumbers(String bossId) {
        return findByBoss(bossId).stream()
                .map(BossPattern::phaseNo)
                .filter(phase -> phase > 0)
                .distinct()
                .sorted()
                .toList();
    }

    public Map<String, BossPattern> all() {
        return delegate.snapshot();
    }

    private String compositeKey(String bossId, String patternId) {
        return normalized(bossId) + ":" + normalized(patternId);
    }

    private String normalized(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
