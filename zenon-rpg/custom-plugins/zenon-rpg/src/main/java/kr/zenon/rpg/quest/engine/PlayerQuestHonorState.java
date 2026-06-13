package kr.zenon.rpg.quest.engine;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class PlayerQuestHonorState {
    private final String userId;
    private final Map<String, Long> wallet = new LinkedHashMap<>();
    private final Map<String, Long> itemInventory = new LinkedHashMap<>();
    private final Map<String, Long> lifeExpByType = new LinkedHashMap<>();
    private final Set<String> unlockFlags = new LinkedHashSet<>();
    private final Map<String, QuestProgress> activeQuests = new LinkedHashMap<>();
    private final Map<String, Integer> completedQuestCounts = new LinkedHashMap<>();
    private final Set<String> unlockedQuestIds = new LinkedHashSet<>();
    private final Set<String> claimedQuestRewardKeys = new LinkedHashSet<>();
    private final Set<String> discoveredRegionCodes = new LinkedHashSet<>();

    private final Map<String, Long> achievementProgress = new LinkedHashMap<>();
    private final Set<String> completedAchievementIds = new LinkedHashSet<>();
    private final Deque<String> recentAchievementIds = new ArrayDeque<>();
    private final Set<String> unlockedTitles = new LinkedHashSet<>();
    private final Set<String> unlockedBadges = new LinkedHashSet<>();
    private final List<String> featuredAchievementIds = new ArrayList<>();
    private final Map<String, Long> recordStats = new LinkedHashMap<>();

    private String equippedTitleId = "";
    private String equippedBadgeId = "";
    private Instant lastQuestCompletedAt;
    private Instant lastAchievementCompletedAt;

    public PlayerQuestHonorState(String userId) {
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

    public Map<String, Long> itemInventorySnapshot() {
        return Map.copyOf(new LinkedHashMap<>(itemInventory));
    }

    public void addLifeExp(String lifeTypeCode, long amount) {
        if (amount <= 0L) {
            return;
        }
        lifeExpByType.merge(normalize(lifeTypeCode), amount, Long::sum);
    }

    public Map<String, Long> lifeExpSnapshot() {
        return Map.copyOf(new LinkedHashMap<>(lifeExpByType));
    }

    public void unlockFlag(String unlockCode) {
        String normalized = normalize(unlockCode);
        if (normalized.isBlank() || "-".equals(normalized) || "none".equals(normalized)) {
            return;
        }
        unlockFlags.add(normalized);
    }

    public boolean hasUnlockFlag(String unlockCode) {
        String normalized = normalize(unlockCode);
        if (normalized.isBlank() || "-".equals(normalized) || "none".equals(normalized)) {
            return true;
        }
        return unlockFlags.contains(normalized);
    }

    public Set<String> unlockFlags() {
        return Set.copyOf(new LinkedHashSet<>(unlockFlags));
    }

    public void unlockQuest(String questId) {
        String normalized = normalize(questId);
        if (normalized.isBlank() || "none".equals(normalized) || "-".equals(normalized)) {
            return;
        }
        unlockedQuestIds.add(normalized);
    }

    public boolean isQuestUnlocked(String questId) {
        return unlockedQuestIds.contains(normalize(questId));
    }

    public Set<String> unlockedQuestSnapshot() {
        return Set.copyOf(new LinkedHashSet<>(unlockedQuestIds));
    }

    public void startQuest(QuestProgress progress) {
        activeQuests.put(progress.questId(), progress);
    }

    public Optional<QuestProgress> activeQuest(String questId) {
        return Optional.ofNullable(activeQuests.get(normalize(questId)));
    }

    public Map<String, QuestProgress> activeQuestsSnapshot() {
        return Map.copyOf(new LinkedHashMap<>(activeQuests));
    }

    public void removeActiveQuest(String questId) {
        activeQuests.remove(normalize(questId));
    }

    public int activeQuestCount() {
        return activeQuests.size();
    }

    public int completedQuestCount(String questId) {
        return completedQuestCounts.getOrDefault(normalize(questId), 0);
    }

    public void markQuestCompleted(String questId, Instant completedAt) {
        String normalized = normalize(questId);
        completedQuestCounts.merge(normalized, 1, Integer::sum);
        activeQuests.remove(normalized);
        lastQuestCompletedAt = completedAt;
    }

    public int totalCompletedQuestCount() {
        return completedQuestCounts.values().stream().mapToInt(Integer::intValue).sum();
    }

    public Set<String> completedQuestIds() {
        return Set.copyOf(completedQuestCounts.keySet());
    }

    public Instant lastQuestCompletedAt() {
        return lastQuestCompletedAt;
    }

    public boolean hasClaimedQuestReward(String rewardKey) {
        return claimedQuestRewardKeys.contains(normalize(rewardKey));
    }

    public void markQuestRewardClaimed(String rewardKey) {
        String normalized = normalize(rewardKey);
        if (!normalized.isBlank()) {
            claimedQuestRewardKeys.add(normalized);
        }
    }

    public Set<String> claimedQuestRewardKeys() {
        return Set.copyOf(new LinkedHashSet<>(claimedQuestRewardKeys));
    }

    public void discoverRegion(String regionCode) {
        String normalized = normalize(regionCode);
        if (!normalized.isBlank() && !"-".equals(normalized) && !"none".equals(normalized)) {
            discoveredRegionCodes.add(normalized);
        }
    }

    public boolean isRegionDiscovered(String regionCode) {
        return discoveredRegionCodes.contains(normalize(regionCode));
    }

    public Set<String> discoveredRegions() {
        return Set.copyOf(new LinkedHashSet<>(discoveredRegionCodes));
    }

    public long achievementProgress(String achievementId) {
        return achievementProgress.getOrDefault(normalize(achievementId), 0L);
    }

    public void addAchievementProgress(String achievementId, long amount) {
        if (amount <= 0L) {
            return;
        }
        achievementProgress.merge(normalize(achievementId), amount, Long::sum);
    }

    public void setAchievementProgress(String achievementId, long amount) {
        achievementProgress.put(normalize(achievementId), Math.max(0L, amount));
    }

    public Map<String, Long> achievementProgressSnapshot() {
        return Map.copyOf(new LinkedHashMap<>(achievementProgress));
    }

    public boolean isAchievementCompleted(String achievementId) {
        return completedAchievementIds.contains(normalize(achievementId));
    }

    public void markAchievementCompleted(String achievementId, Instant completedAt) {
        String normalized = normalize(achievementId);
        completedAchievementIds.add(normalized);
        recentAchievementIds.remove(normalized);
        recentAchievementIds.addFirst(normalized);
        while (recentAchievementIds.size() > 10) {
            recentAchievementIds.removeLast();
        }
        lastAchievementCompletedAt = completedAt;
    }

    public Set<String> completedAchievementIds() {
        return Set.copyOf(new LinkedHashSet<>(completedAchievementIds));
    }

    public List<String> recentAchievementIds() {
        return List.copyOf(recentAchievementIds);
    }

    public Instant lastAchievementCompletedAt() {
        return lastAchievementCompletedAt;
    }

    public void unlockTitle(String titleId) {
        String normalized = normalize(titleId);
        if (!normalized.isBlank() && !"-".equals(normalized) && !"none".equals(normalized)) {
            unlockedTitles.add(normalized);
        }
    }

    public boolean hasTitle(String titleId) {
        return unlockedTitles.contains(normalize(titleId));
    }

    public Set<String> unlockedTitles() {
        return Set.copyOf(new LinkedHashSet<>(unlockedTitles));
    }

    public void unlockBadge(String badgeId) {
        String normalized = normalize(badgeId);
        if (!normalized.isBlank() && !"-".equals(normalized) && !"none".equals(normalized)) {
            unlockedBadges.add(normalized);
        }
    }

    public boolean hasBadge(String badgeId) {
        return unlockedBadges.contains(normalize(badgeId));
    }

    public Set<String> unlockedBadges() {
        return Set.copyOf(new LinkedHashSet<>(unlockedBadges));
    }

    public String equippedTitleId() {
        return equippedTitleId;
    }

    public void setEquippedTitleId(String equippedTitleId) {
        this.equippedTitleId = normalize(equippedTitleId);
    }

    public String equippedBadgeId() {
        return equippedBadgeId;
    }

    public void setEquippedBadgeId(String equippedBadgeId) {
        this.equippedBadgeId = normalize(equippedBadgeId);
    }

    public List<String> featuredAchievementIds() {
        return List.copyOf(featuredAchievementIds);
    }

    public void setFeaturedAchievementIds(List<String> achievementIds) {
        featuredAchievementIds.clear();
        if (achievementIds == null) {
            return;
        }
        for (String achievementId : achievementIds) {
            String normalized = normalize(achievementId);
            if (normalized.isBlank() || featuredAchievementIds.contains(normalized)) {
                continue;
            }
            featuredAchievementIds.add(normalized);
            if (featuredAchievementIds.size() >= 3) {
                break;
            }
        }
    }

    public void addRecordStat(String recordId, long amount) {
        if (amount <= 0L) {
            return;
        }
        recordStats.merge(normalize(recordId), amount, Long::sum);
    }

    public Map<String, Long> recordStats() {
        return Map.copyOf(new LinkedHashMap<>(recordStats));
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
