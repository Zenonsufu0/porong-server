package com.poro.rpg.npc.citizens;

public record NpcSyncRuntime(
        CitizensNpcSeedLoader seedLoader,
        CitizensNpcSyncService syncService,
        CitizensNpcGateway gateway,
        CitizensNpcSyncReport lastReport,
        boolean citizensAvailable
) {
}

