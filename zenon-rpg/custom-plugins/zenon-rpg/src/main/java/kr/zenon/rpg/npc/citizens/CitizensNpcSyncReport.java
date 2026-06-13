package kr.zenon.rpg.npc.citizens;

import java.util.List;

public record CitizensNpcSyncReport(
        int totalSeeds,
        int activeSeeds,
        int createdCount,
        int updatedCount,
        int removedCount,
        int skippedCount,
        int invalidCount,
        int orphanCount,
        List<String> orphanSeedIds
) {
}

