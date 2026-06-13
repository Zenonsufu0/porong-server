package kr.zenon.rpg.operations.query.service;

import kr.zenon.rpg.boss.engine.BossEngineRuntime;
import kr.zenon.rpg.boss.engine.BossResultSummary;
import kr.zenon.rpg.growth.engine.PlayerGrowthSnapshotBuilder;
import kr.zenon.rpg.life.engine.EstateSnapshotBuilder;
import kr.zenon.rpg.operations.query.model.PlayerProfileRecord;
import kr.zenon.rpg.operations.query.store.OperationsDataStore;
import kr.zenon.rpg.quest.engine.PlayerQuestAndHonorSnapshotBuilder;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class PlayerDetailQueryService {
    private final OperationsDataStore dataStore;
    private final BossEngineRuntime bossEngineRuntime;

    public PlayerDetailQueryService(OperationsDataStore dataStore, BossEngineRuntime bossEngineRuntime) {
        this.dataStore = Objects.requireNonNull(dataStore, "dataStore");
        this.bossEngineRuntime = Objects.requireNonNull(bossEngineRuntime, "bossEngineRuntime");
    }

    public PlayerDetailProjection query(String userId) {
        String normalizedUserId = normalize(userId);
        PlayerProfileRecord profile = dataStore.playerProfile(normalizedUserId)
                .orElse(new PlayerProfileRecord(normalizedUserId, normalizedUserId, ""));
        PlayerGrowthSnapshotBuilder.PlayerGrowthSnapshot growth = dataStore.growthSnapshot(normalizedUserId).orElse(null);
        EstateSnapshotBuilder.EstateSnapshot life = dataStore.lifeSnapshot(normalizedUserId).orElse(null);
        PlayerQuestAndHonorSnapshotBuilder.Snapshot quest = dataStore.questSnapshot(normalizedUserId).orElse(null);

        List<PlayerBossRecordProjection> recentBossRecords = recentBossRecords(normalizedUserId);
        EquipmentProjection equipment = equipmentProjection(growth);
        LifeProjection lifeProjection = lifeProjection(life, dataStore.lifeLevels(normalizedUserId));
        QuestHonorProjection questHonor = questHonorProjection(quest);

        return new PlayerDetailProjection(
                profile.userId(),
                profile.nickname(),
                profile.classId(),
                equipment,
                recentBossRecords,
                lifeProjection,
                questHonor
        );
    }

    public PlayerSnapshotProjection playerSnapshot(String userId) {
        PlayerDetailProjection detail = query(userId);
        return new PlayerSnapshotProjection(
                detail.userId(),
                detail.nickname(),
                detail.classId(),
                detail.equipment().enhancementSum(),
                detail.recentBossRecords().stream().findFirst().map(PlayerBossRecordProjection::bossId).orElse(""),
                detail.life().estateUnlocked(),
                detail.questHonor().completedQuestCount()
        );
    }

    public EquipmentProjection equipmentSnapshot(String userId) {
        return query(userId).equipment();
    }

    public List<PlayerBossRecordProjection> bossRecordsSnapshot(String userId) {
        return query(userId).recentBossRecords();
    }

    public LifeProjection lifeSnapshot(String userId) {
        return query(userId).life();
    }

    public Optional<PlayerSnapshotProjection> playerSnapshotByNick(String nick) {
        return queryByNick(nick).map(d -> new PlayerSnapshotProjection(
                d.userId(), d.nickname(), d.classId(),
                d.equipment().enhancementSum(),
                d.recentBossRecords().stream().findFirst().map(PlayerBossRecordProjection::bossId).orElse(""),
                d.life().estateUnlocked(),
                d.questHonor().completedQuestCount()
        ));
    }

    public Optional<LifeProjection> lifeSnapshotByNick(String nick) {
        return queryByNick(nick).map(PlayerDetailProjection::life);
    }

    public Optional<List<PlayerBossRecordProjection>> bossRecordsSnapshotByNick(String nick) {
        return queryByNick(nick).map(PlayerDetailProjection::recentBossRecords);
    }

    private Optional<PlayerDetailProjection> queryByNick(String nick) {
        return dataStore.playerProfileByNick(nick == null ? "" : nick.trim())
                .map(profile -> query(profile.userId()));
    }

    private List<PlayerBossRecordProjection> recentBossRecords(String userId) {
        List<BossResultSummary> all = new ArrayList<>(bossEngineRuntime.runRecordHook().summaries());
        all.addAll(dataStore.additionalBossSummaries());

        List<PlayerBossRecordProjection> records = new ArrayList<>();
        for (BossResultSummary summary : all) {
            Map<String, Object> participant = participantOf(summary, userId);
            if (participant == null) {
                continue;
            }
            int deaths = 0;
            Object rawDeaths = participant.get("deaths");
            if (rawDeaths instanceof Number number) {
                deaths = Math.max(0, number.intValue());
            }
            records.add(new PlayerBossRecordProjection(
                    summary.runId(),
                    summary.bossId(),
                    summary.clearSuccess(),
                    summary.phaseReached(),
                    summary.clearTimeSeconds(),
                    deaths
            ));
        }
        records.sort(Comparator.comparing(PlayerBossRecordProjection::runId).reversed());
        return List.copyOf(records.stream().limit(20).toList());
    }

    private Map<String, Object> participantOf(BossResultSummary summary, String userId) {
        for (Map<String, Object> participant : summary.participantSummaryPlaceholder()) {
            String participantUserId = normalize(String.valueOf(participant.getOrDefault("user_id", "")));
            if (participantUserId.equals(userId)) {
                return participant;
            }
        }
        return null;
    }

    private EquipmentProjection equipmentProjection(PlayerGrowthSnapshotBuilder.PlayerGrowthSnapshot growth) {
        if (growth == null) {
            return new EquipmentProjection(List.of(), 0, List.of(), List.of(), List.of(), List.of(), List.of(), Map.of(), List.of());
        }
        List<String> equippedItems = growth.equipmentSummary().stream()
                .map(item -> item.equippedSlot() + ":" + item.itemName() + "(+" + item.enhanceLevel() + ")")
                .toList();
        List<String> potential = growth.potentialSummaryByItem().entrySet().stream()
                .map(entry -> entry.getKey() + ":" + entry.getValue().stream()
                        .map(line -> line.optionCode() + "=" + line.value())
                        .toList())
                .toList();
        List<String> setEffects = growth.setBonusSummary().activeEffects().entrySet().stream()
                .map(entry -> entry.getKey() + ":" + entry.getValue())
                .toList();
        List<String> runes = growth.runeSummary().equippedRunes().entrySet().stream()
                .map(entry -> "slot" + entry.getKey() + ":" + entry.getValue())
                .toList();
        List<String> engravings = new ArrayList<>();
        engravings.add("class:" + growth.engravingSummary().classEngraving());
        engravings.addAll(growth.engravingSummary().commonEngravings().entrySet().stream()
                .map(entry -> "common" + entry.getKey() + ":" + entry.getValue())
                .toList());
        List<String> flags = List.copyOf(growth.finalStatFlags());
        return new EquipmentProjection(
                equippedItems,
                growth.enhancementSum(),
                potential,
                setEffects,
                runes,
                engravings,
                List.copyOf(growth.finalStatValues().keySet()),
                growth.finalStatValues(),
                flags
        );
    }

    private LifeProjection lifeProjection(EstateSnapshotBuilder.EstateSnapshot life, Map<String, Integer> levels) {
        if (life == null) {
            return new LifeProjection(false, List.of(), Map.of(), levels, "");
        }
        List<String> facilities = life.facilities().stream()
                .map(facility -> "slot" + facility.slotNo() + ":" + facility.facilityId() + "@L" + facility.level())
                .toList();
        Map<String, Long> stored = new java.util.LinkedHashMap<>();
        for (EstateSnapshotBuilder.FacilitySnapshot facility : life.facilities()) {
            for (Map.Entry<String, Long> item : facility.currentStoredItems().entrySet()) {
                stored.merge(item.getKey(), item.getValue(), Long::sum);
            }
        }
        String recentHarvest = life.recentHarvest() == null ? "" : life.recentHarvest().harvestedItems().toString();
        return new LifeProjection(
                life.unlocked(),
                facilities,
                Map.copyOf(stored),
                levels,
                recentHarvest
        );
    }

    private QuestHonorProjection questHonorProjection(PlayerQuestAndHonorSnapshotBuilder.Snapshot quest) {
        if (quest == null) {
            return new QuestHonorProjection(List.of(), 0, List.of(), "", "");
        }
        List<String> active = quest.activeQuestSummaries().stream()
                .map(activeQuest -> activeQuest.questId() + "(" + activeQuest.currentAmount() + "/" + activeQuest.requiredAmount() + ")")
                .toList();
        List<String> featured = quest.featuredAchievements().stream()
                .map(PlayerQuestAndHonorSnapshotBuilder.FeaturedAchievementSummary::achievementId)
                .toList();
        return new QuestHonorProjection(
                active,
                quest.completedQuestCount(),
                featured,
                quest.equippedTitleId(),
                quest.equippedBadgeId()
        );
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    public record PlayerDetailProjection(
            String userId,
            String nickname,
            String classId,
            EquipmentProjection equipment,
            List<PlayerBossRecordProjection> recentBossRecords,
            LifeProjection life,
            QuestHonorProjection questHonor
    ) {
    }

    public record EquipmentProjection(
            List<String> equippedItems,
            int enhancementSum,
            List<String> potentialSummary,
            List<String> setEffects,
            List<String> runes,
            List<String> engravings,
            List<String> finalStatKeys,
            Map<String, Double> finalStatValues,
            List<String> finalFlags
    ) {
    }

    public record PlayerBossRecordProjection(
            String runId,
            String bossId,
            boolean clearSuccess,
            int phaseReached,
            long clearTimeSeconds,
            int deaths
    ) {
    }

    public record LifeProjection(
            boolean estateUnlocked,
            List<String> facilities,
            Map<String, Long> currentStoredItems,
            Map<String, Integer> lifeLevels,
            String recentHarvestSummary
    ) {
    }

    public record QuestHonorProjection(
            List<String> activeQuests,
            int completedQuestCount,
            List<String> featuredAchievements,
            String equippedTitleId,
            String equippedBadgeId
    ) {
    }

    public record PlayerSnapshotProjection(
            String userId,
            String nickname,
            String classId,
            int enhancementSum,
            String latestBossId,
            boolean estateUnlocked,
            int completedQuestCount
    ) {
    }
}
