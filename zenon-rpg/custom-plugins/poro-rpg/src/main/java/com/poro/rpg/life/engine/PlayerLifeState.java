package com.poro.rpg.life.engine;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class PlayerLifeState {
    private final String userId;
    private final Map<String, Long> wallet = new LinkedHashMap<>();
    private final Map<String, Long> itemInventory = new LinkedHashMap<>();
    private final Map<String, Long> acquiredBySource = new LinkedHashMap<>();
    private final Map<LifeType, LifeSkillProfile> lifeProfiles = new EnumMap<>(LifeType.class);
    private final Set<String> unlockedBlueprintIds = new LinkedHashSet<>();
    private final Set<String> completedQuestIds = new LinkedHashSet<>();

    private EstateState estateState;

    public PlayerLifeState(String userId) {
        this.userId = normalize(userId);
    }

    public String userId() {
        return userId;
    }

    public long currency(String code) {
        return wallet.getOrDefault(normalize(code), 0L);
    }

    public void addCurrency(String code, long amount) {
        if (amount <= 0L) {
            return;
        }
        wallet.merge(normalize(code), amount, Long::sum);
    }

    public boolean consumeCurrency(String code, long amount) {
        if (amount <= 0L) {
            return true;
        }
        String key = normalize(code);
        long current = wallet.getOrDefault(key, 0L);
        if (current < amount) {
            return false;
        }
        wallet.put(key, current - amount);
        return true;
    }

    public Map<String, Long> walletSnapshot() {
        return Map.copyOf(new LinkedHashMap<>(wallet));
    }

    public long itemAmount(String itemId) {
        return itemInventory.getOrDefault(normalize(itemId), 0L);
    }

    public void addItem(String itemId, long amount) {
        if (amount <= 0L) {
            return;
        }
        itemInventory.merge(normalize(itemId), amount, Long::sum);
    }

    public boolean consumeItem(String itemId, long amount) {
        if (amount <= 0L) {
            return true;
        }
        String key = normalize(itemId);
        long current = itemInventory.getOrDefault(key, 0L);
        if (current < amount) {
            return false;
        }
        itemInventory.put(key, current - amount);
        return true;
    }

    public void recordAcquisition(String itemId, long amount, LifeSourceType sourceType) {
        if (amount <= 0L) {
            return;
        }
        String key = sourceType.code() + ":" + normalize(itemId);
        acquiredBySource.merge(key, amount, Long::sum);
    }

    public Map<String, Long> itemInventorySnapshot() {
        return Map.copyOf(new LinkedHashMap<>(itemInventory));
    }

    public Map<String, Long> acquisitionSourceSnapshot() {
        return Map.copyOf(new LinkedHashMap<>(acquiredBySource));
    }

    public LifeSkillProfile lifeProfile(LifeType lifeType) {
        return lifeProfiles.computeIfAbsent(lifeType, ignored -> new LifeSkillProfile(1, 0L));
    }

    public void setLifeProfile(LifeType lifeType, LifeSkillProfile profile) {
        lifeProfiles.put(lifeType, profile);
    }

    public Map<LifeType, LifeSkillProfile> lifeProfilesSnapshot() {
        return Map.copyOf(new EnumMap<>(lifeProfiles));
    }

    public void unlockBlueprint(String blueprintId) {
        String normalized = normalize(blueprintId);
        if (normalized.isBlank() || "-".equals(normalized)) {
            return;
        }
        unlockedBlueprintIds.add(normalized);
    }

    public boolean hasBlueprint(String blueprintId) {
        String normalized = normalize(blueprintId);
        if (normalized.isBlank() || "-".equals(normalized)) {
            return true;
        }
        return unlockedBlueprintIds.contains(normalized);
    }

    public Set<String> unlockedBlueprints() {
        return Set.copyOf(new LinkedHashSet<>(unlockedBlueprintIds));
    }

    public void markQuestCompleted(String questId) {
        String normalized = normalize(questId);
        if (normalized.isBlank() || "none".equals(normalized) || "-".equals(normalized)) {
            return;
        }
        completedQuestIds.add(normalized);
    }

    public boolean isQuestCompleted(String questId) {
        String normalized = normalize(questId);
        if (normalized.isBlank() || "none".equals(normalized) || "-".equals(normalized)) {
            return true;
        }
        return completedQuestIds.contains(normalized);
    }

    public Set<String> completedQuestSnapshot() {
        return Set.copyOf(new LinkedHashSet<>(completedQuestIds));
    }

    public Optional<EstateState> estateState() {
        return Optional.ofNullable(estateState);
    }

    public void setEstateState(EstateState estateState) {
        this.estateState = estateState;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    public record LifeSkillProfile(
            int level,
            long exp
    ) {
        public LifeSkillProfile {
            level = Math.max(1, level);
            exp = Math.max(0L, exp);
        }
    }
}
