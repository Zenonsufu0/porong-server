package com.poro.empire.npc.citizens;

import com.poro.empire.common.logging.DomainLogger;
import com.poro.empire.common.registry.master.MasterRegistryContext;
import com.poro.empire.common.registry.master.model.NpcMaster;
import com.poro.empire.common.result.ErrorCode;
import com.poro.empire.common.result.Result;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class CitizensNpcSyncService {
    private final DomainLogger logger;
    private final CitizensNpcSeedLoader seedLoader;
    private final CitizensNpcGateway gateway;
    private final CitizensNpcTraitBinder traitBinder;
    private final MasterRegistryContext masterRegistryContext;

    public CitizensNpcSyncService(
            DomainLogger logger,
            CitizensNpcSeedLoader seedLoader,
            CitizensNpcGateway gateway,
            CitizensNpcTraitBinder traitBinder,
            MasterRegistryContext masterRegistryContext
    ) {
        this.logger = Objects.requireNonNull(logger, "logger");
        this.seedLoader = Objects.requireNonNull(seedLoader, "seedLoader");
        this.gateway = Objects.requireNonNull(gateway, "gateway");
        this.traitBinder = Objects.requireNonNull(traitBinder, "traitBinder");
        this.masterRegistryContext = Objects.requireNonNull(masterRegistryContext, "masterRegistryContext");
    }

    public Result<CitizensNpcSyncReport> sync() {
        if (!gateway.isAvailable()) {
            return Result.success(new CitizensNpcSyncReport(0, 0, 0, 0, 0, 0, 0, 0, List.of()));
        }

        Result<List<CitizensNpcSeed>> loadResult = seedLoader.load();
        if (loadResult.isFailure()) {
            return Result.failure(loadResult.errorCode(), loadResult.message(), loadResult.cause());
        }

        Map<String, CitizensNpcSeed> seedsById = new LinkedHashMap<>();
        for (CitizensNpcSeed seed : loadResult.value()) {
            CitizensNpcSeed previous = seedsById.putIfAbsent(seed.npcSeedId(), seed);
            if (previous != null) {
                return Result.failure(
                        ErrorCode.SEED_LOAD_FAILED,
                        "Duplicate npc_seed_id found in npc_spawn_seed.csv: " + seed.npcSeedId()
                );
            }
        }

        Result<List<CitizensNpcHandle>> managedResult = gateway.listManagedNpcs();
        if (managedResult.isFailure()) {
            return Result.failure(managedResult.errorCode(), managedResult.message(), managedResult.cause());
        }

        int removed = 0;
        Map<String, CitizensNpcHandle> existingBySeedId = new LinkedHashMap<>();
        for (CitizensNpcHandle managedNpc : managedResult.value()) {
            if (managedNpc.seedId() == null || managedNpc.seedId().isBlank()) {
                continue;
            }
            CitizensNpcHandle previous = existingBySeedId.putIfAbsent(managedNpc.seedId(), managedNpc);
            if (previous != null) {
                Result<Void> deleteDuplicate = gateway.deleteNpc(managedNpc);
                if (deleteDuplicate.isFailure()) {
                    return Result.failure(deleteDuplicate.errorCode(), deleteDuplicate.message(), deleteDuplicate.cause());
                }
                removed++;
                logger.warn("Removed duplicated managed NPC for same npc_seed_id="
                        + managedNpc.seedId()
                        + ", kept_npc_id=" + previous.npcId()
                        + ", removed_npc_id=" + managedNpc.npcId());
            }
        }

        int activeSeeds = 0;
        int created = 0;
        int updated = 0;
        int skipped = 0;
        int invalid = 0;

        Set<String> synchronizedSeedIds = new LinkedHashSet<>();
        for (CitizensNpcSeed seed : seedsById.values()) {
            if (!seed.shouldSpawn()) {
                CitizensNpcHandle existing = existingBySeedId.get(seed.npcSeedId());
                if (existing == null) {
                    skipped++;
                } else {
                    Result<Void> deleteResult = gateway.deleteNpc(existing);
                    if (deleteResult.isFailure()) {
                        return Result.failure(deleteResult.errorCode(), deleteResult.message(), deleteResult.cause());
                    }
                    removed++;
                }
                continue;
            }

            activeSeeds++;
            if (!validateAgainstMaster(seed)) {
                invalid++;
                skipped++;
                continue;
            }

            CitizensNpcHandle existing = existingBySeedId.get(seed.npcSeedId());
            CitizensNpcHandle targetNpc;
            if (existing == null) {
                Result<CitizensNpcHandle> createResult = gateway.createNpc(seed);
                if (createResult.isFailure()) {
                    return Result.failure(createResult.errorCode(), createResult.message(), createResult.cause());
                }
                targetNpc = createResult.value();
                created++;
            } else if (existing.entityType() != seed.entityType()) {
                Result<CitizensNpcHandle> recreateResult = gateway.recreateNpc(existing, seed);
                if (recreateResult.isFailure()) {
                    return Result.failure(recreateResult.errorCode(), recreateResult.message(), recreateResult.cause());
                }
                targetNpc = recreateResult.value();
                updated++;
            } else {
                Result<CitizensNpcHandle> updateResult = gateway.updateNameAndLocation(existing, seed);
                if (updateResult.isFailure()) {
                    return Result.failure(updateResult.errorCode(), updateResult.message(), updateResult.cause());
                }
                targetNpc = updateResult.value();
                updated++;
            }

            Result<Void> traitResult = traitBinder.bind(gateway, targetNpc, seed);
            if (traitResult.isFailure()) {
                return Result.failure(traitResult.errorCode(), traitResult.message(), traitResult.cause());
            }
            synchronizedSeedIds.add(seed.npcSeedId());
        }

        List<String> orphanSeedIds = new ArrayList<>();
        for (String existingSeedId : existingBySeedId.keySet()) {
            if (!seedsById.containsKey(existingSeedId)) {
                orphanSeedIds.add(existingSeedId);
                logger.warn("Orphan managed Citizens NPC detected: npc_seed_id=" + existingSeedId);
            }
        }

        CitizensNpcSyncReport report = new CitizensNpcSyncReport(
                seedsById.size(),
                activeSeeds,
                created,
                updated,
                removed,
                skipped,
                invalid,
                orphanSeedIds.size(),
                List.copyOf(orphanSeedIds)
        );

        logger.info("Citizens NPC sync completed. total=" + report.totalSeeds()
                + ", active=" + report.activeSeeds()
                + ", created=" + report.createdCount()
                + ", updated=" + report.updatedCount()
                + ", removed=" + report.removedCount()
                + ", skipped=" + report.skippedCount()
                + ", invalid=" + report.invalidCount()
                + ", orphans=" + report.orphanCount());

        return Result.success(report);
    }

    private boolean validateAgainstMaster(CitizensNpcSeed seed) {
        Optional<NpcMaster> npcMasterOpt = masterRegistryContext.npcMasters().find(seed.npcMasterId());
        if (npcMasterOpt.isEmpty()) {
            logger.warn("npc_spawn_seed references missing npc_master_id. npc_seed_id=" + seed.npcSeedId()
                    + ", npc_master_id=" + seed.npcMasterId());
            return false;
        }
        NpcMaster npcMaster = npcMasterOpt.get();
        if (!isBlank(seed.regionCode()) && !isBlank(npcMaster.regionCode())
                && !seed.regionCode().equalsIgnoreCase(npcMaster.regionCode())) {
            logger.warn("region_code mismatch for npc_seed_id=" + seed.npcSeedId()
                    + ". seed=" + seed.regionCode()
                    + ", npc_master=" + npcMaster.regionCode());
        }
        if (!isBlank(seed.townId()) && !isBlank(npcMaster.townId())
                && !seed.townId().equalsIgnoreCase(npcMaster.townId())) {
            logger.warn("town_id mismatch for npc_seed_id=" + seed.npcSeedId()
                    + ". seed=" + seed.townId()
                    + ", npc_master=" + npcMaster.townId());
        }
        if (!masterRegistryContext.regionMasters().contains(seed.regionCode())) {
            logger.warn("npc_spawn_seed references unknown region_code. npc_seed_id=" + seed.npcSeedId()
                    + ", region_code=" + seed.regionCode());
        }
        if (!masterRegistryContext.townMasters().contains(seed.townId())) {
            logger.warn("npc_spawn_seed references unknown town_id. npc_seed_id=" + seed.npcSeedId()
                    + ", town_id=" + seed.townId());
        }
        return true;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
