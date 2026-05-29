package com.poro.empire.persistence;

import java.util.List;
import java.util.Map;

/**
 * 플레이어 전체 데이터 직렬화 DTO.
 * plugins/EmpireRPG/playerdata/{uuid}.json 에 저장.
 */
public record PlayerSaveData(
        int schemaVersion,
        String weaponType,
        String classId,
        String classEngravingId,
        Map<String, Long>   wallet,
        Map<String, String> equippedSlots,
        List<ItemSaveData>  inventory,
        Map<String, String> equippedRunes,
        Map<String, CommonEngravingSaveData> commonEngravings,
        TerritorySaveData   territory,
        Map<String, Long>   storage,
        Map<String, Long>   customItems,
        List<WorkshopJobSaveData> workshopJobs,
        int  playerLevel,
        int  unspentPts,
        int  critPts,
        int  specPts,
        int  endurPts,
        long currentExp,
        Map<String, Integer> ceilingCounters,
        int ilWarningCount,
        int mobIlHitCount,
        int catalystBonusPct,
        Map<String, String> cosmeticMaterials
) {
    public static final int CURRENT_VERSION = 5;

    public record ItemSaveData(
            String instanceId,
            String itemId,
            int    enhanceLevel,
            String grade,
            PotentialSaveData         potential,
            List<PotentialLineSaveData> substats
    ) {}

    public record PotentialSaveData(
            String grade,
            List<PotentialLineSaveData> lines
    ) {}

    public record PotentialLineSaveData(
            int    lineNo,
            String grade,
            String optionCode,
            double value
    ) {}

    public record CommonEngravingSaveData(
            String engravingId,
            int    level
    ) {}

    public record TerritorySaveData(
            String ownerName,
            String rankName,
            int    convenienceUnlocks,
            int    reaperCount,
            int    storageCount,
            int    minerCount
    ) {
        /** legacy 5-arg deserialization 호환 */
        public TerritorySaveData(String ownerName, String rankName, int convenienceUnlocks,
                                 int reaperCount, int storageCount) {
            this(ownerName, rankName, convenienceUnlocks, reaperCount, storageCount, 0);
        }
    }

    public record WorkshopJobSaveData(
            String recipeId,
            long   startedAt,
            long   completeAt
    ) {}
}
