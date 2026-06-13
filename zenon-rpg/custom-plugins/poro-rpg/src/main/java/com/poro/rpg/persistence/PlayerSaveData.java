package com.poro.rpg.persistence;

import java.util.List;
import java.util.Map;

/**
 * 플레이어 전체 데이터 직렬화 DTO.
 * plugins/PoroRPG/playerdata/{uuid}.json 에 저장.
 */
public record PlayerSaveData(
        int schemaVersion,
        String weaponType,
        String classId,
        // legacy(v5 이하) 단일 클래스 각인 — v6부터 classEngravingByClass로 이관, 마이그레이션 소스로만 유지.
        String classEngravingId,
        // 무기별 독립 클래스 각인 (v6, DL-110) — key = classId(무기 타입), value = engravingId.
        Map<String, String> classEngravingByClass,
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
        Map<String, String> cosmeticMaterials,
        // 장비 흔적 개별 인스턴스 (v7, DL-129 추가#38). 구 세이브엔 없음 → null → 마이그레이션에서 List.of().
        List<TraceInstanceSaveData> traceInstances
) {
    public static final int CURRENT_VERSION = 7;

    public record ItemSaveData(
            String instanceId,
            String itemId,
            int    enhanceLevel,
            String grade,
            PotentialSaveData         potential,
            List<PotentialLineSaveData> substats,
            int    pityCount  // 잠재 천장 카운터 (DL-129 추가#4). 구 세이브엔 없음 → 역직렬화 시 0.
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
            // NOTE: 필드명은 ownerName이지만 실제로 담는 값은 영지명(islandName)이다.
            // JSON 직렬화 키 호환을 위해 레거시 명칭 유지 — toTerritoryDto가 t.islandName()을 넣고
            // applyTerritory가 setIslandName()으로 꺼낸다. 영지명은 이 경로로 영속화된다(별도 DB 컬럼 없음).
            String ownerName,
            String rankName,
            int    convenienceUnlocks,
            int    reaperCount,   // DL-129 추가#11부터 마이그레이션 전용(읽기) — 신 세이브는 herbProducedAt 사용
            int    storageCount,
            int    minerCount,    // 〃 마이그레이션 전용 — 신 세이브는 oreProducedAt 사용
            long   lastProductionAt,
            int    workshopMachineCount, // 공방 가공기 대수 (DL-129 추가#7)
            java.util.List<Long> herbProducedAt, // 약초 재배지별 마지막 생산시각 (DL-129 추가#11). 구 세이브 null → reaperCount로 마이그레이션.
            java.util.List<Long> oreProducedAt,  // 광물 채굴기별 마지막 생산시각
            int    timeState,    // 영지 시간 고정 (0 기본/1 낮/2 밤, DL-129#33). 구 세이브 → 0.
            int    weatherState  // 영지 날씨 고정 (0 기본/1 비/2 천둥)
    ) {
        /** legacy 10-arg 호환 (시간/날씨 없음 → 0) */
        public TerritorySaveData(String ownerName, String rankName, int convenienceUnlocks,
                                 int reaperCount, int storageCount, int minerCount, long lastProductionAt, int workshopMachineCount,
                                 java.util.List<Long> herbProducedAt, java.util.List<Long> oreProducedAt) {
            this(ownerName, rankName, convenienceUnlocks, reaperCount, storageCount, minerCount, lastProductionAt, workshopMachineCount, herbProducedAt, oreProducedAt, 0, 0);
        }

        /** legacy 8-arg 호환 (시설 타임스탬프 리스트 없음 → null = 마이그레이션) */
        public TerritorySaveData(String ownerName, String rankName, int convenienceUnlocks,
                                 int reaperCount, int storageCount, int minerCount, long lastProductionAt, int workshopMachineCount) {
            this(ownerName, rankName, convenienceUnlocks, reaperCount, storageCount, minerCount, lastProductionAt, workshopMachineCount, null, null, 0, 0);
        }

        /** legacy 7-arg 호환 (workshopMachineCount 없음 → 0) */
        public TerritorySaveData(String ownerName, String rankName, int convenienceUnlocks,
                                 int reaperCount, int storageCount, int minerCount, long lastProductionAt) {
            this(ownerName, rankName, convenienceUnlocks, reaperCount, storageCount, minerCount, lastProductionAt, 0, null, null, 0, 0);
        }

        /** legacy 6-arg 호환 (lastProductionAt 없음 → 0 = 최초로 취급, DL-088) */
        public TerritorySaveData(String ownerName, String rankName, int convenienceUnlocks,
                                 int reaperCount, int storageCount, int minerCount) {
            this(ownerName, rankName, convenienceUnlocks, reaperCount, storageCount, minerCount, 0L, 0, null, null, 0, 0);
        }

        /** legacy 5-arg deserialization 호환 */
        public TerritorySaveData(String ownerName, String rankName, int convenienceUnlocks,
                                 int reaperCount, int storageCount) {
            this(ownerName, rankName, convenienceUnlocks, reaperCount, storageCount, 0, 0L, 0, null, null, 0, 0);
        }
    }

    public record WorkshopJobSaveData(
            String recipeId,
            long   startedAt,
            long   completeAt
    ) {}

    /** 흔적 인스턴스 직렬화 DTO (v7, DL-129 추가#38). grade·세부스탯 라인 보관. */
    public record TraceInstanceSaveData(
            String instanceId,
            String grade,
            List<PotentialLineSaveData> substats
    ) {}
}
