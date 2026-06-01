package com.poro.rpg.growth.engine;

import com.poro.rpg.common.registry.InMemoryRegistry;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class EnhancementRuleRegistry {
    private final InMemoryRegistry<String, EnhancementRule> delegate = new InMemoryRegistry<>();

    public void register(EnhancementRule rule) {
        delegate.register(key(rule.tier(), rule.enhanceLevel()), rule);
    }

    public Optional<EnhancementRule> find(GrowthTier tier, int enhanceLevel) {
        return delegate.find(key(tier, enhanceLevel));
    }

    public Map<String, EnhancementRule> all() {
        return delegate.snapshot();
    }

    private String key(GrowthTier tier, int level) {
        String safeTier = tier == null ? GrowthTier.T1.name() : tier.name();
        return safeTier.toUpperCase(Locale.ROOT) + ":" + Math.max(0, level);
    }
}
