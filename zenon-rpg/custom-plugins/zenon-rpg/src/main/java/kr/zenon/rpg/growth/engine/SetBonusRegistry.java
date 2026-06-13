package kr.zenon.rpg.growth.engine;

import kr.zenon.rpg.common.registry.InMemoryRegistry;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class SetBonusRegistry {
    private final InMemoryRegistry<String, SetBonusRule> delegate = new InMemoryRegistry<>();

    public void register(SetBonusRule rule) {
        String key = rule.setId() + ":" + rule.pieceCount() + ":" + rule.effectOrder() + ":" + rule.effectType();
        delegate.register(key, rule);
    }

    public List<SetBonusRule> findBySetId(String setId) {
        String normalized = normalize(setId);
        return delegate.snapshot().values().stream()
                .filter(rule -> rule.setId().equals(normalized))
                .sorted(Comparator.comparingInt(SetBonusRule::pieceCount).thenComparingInt(SetBonusRule::effectOrder))
                .toList();
    }

    public Map<String, SetBonusRule> all() {
        return delegate.snapshot();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
