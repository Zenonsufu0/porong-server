package com.poro.empire.npc.citizens;

import com.poro.empire.common.logging.CommonPluginLogger;
import com.poro.empire.common.logging.DomainLogger;
import com.poro.empire.common.registry.master.AchievementMasterRegistry;
import com.poro.empire.common.registry.master.BossMasterRegistry;
import com.poro.empire.common.registry.master.ItemMasterRegistry;
import com.poro.empire.common.registry.master.MasterRegistryContext;
import com.poro.empire.common.registry.master.NpcMasterRegistry;
import com.poro.empire.common.registry.master.QuestMasterRegistry;
import com.poro.empire.common.registry.master.RegionMasterRegistry;
import com.poro.empire.common.registry.master.SkillMasterRegistry;
import com.poro.empire.common.registry.master.TownMasterRegistry;
import com.poro.empire.common.registry.master.model.NpcMaster;
import com.poro.empire.common.registry.master.model.RegionMaster;
import com.poro.empire.common.registry.master.model.TownMaster;
import com.poro.empire.common.result.Result;
import org.bukkit.entity.EntityType;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CitizensNpcSyncSampleTest {
    @Test
    void shouldSyncCapitalAndRegionNpcsFromSeed() {
        DomainLogger logger = new CommonPluginLogger(Logger.getLogger("npc-sync-test"), "EmpireRPG").domain("npc-sync-test");
        CitizensNpcSeedLoader seedLoader = new CitizensNpcSeedLoader(Path.of("src/main/resources/seeds/npc_spawn_seed.csv"));
        InMemoryCitizensNpcGateway gateway = new InMemoryCitizensNpcGateway();
        gateway.preload("npc_capital_leonid_main", EntityType.PLAYER, "Old Leonid");
        gateway.preload("npc_orphan_obsolete", EntityType.PLAYER, "Obsolete NPC");

        CitizensNpcTraitBinder traitBinder = new CitizensNpcTraitBinder(
                new MetadataInteractionProfileHook(),
                (g, npc, seed) -> {
                    if (seed.hasBetonConversation()) {
                        return g.setMetadata(npc, CitizensNpcGateway.META_BETON_CONVERSATION_ID, seed.betonConversationId());
                    }
                    return Result.success();
                }
        );

        CitizensNpcSyncService service = new CitizensNpcSyncService(
                logger,
                seedLoader,
                gateway,
                traitBinder,
                buildMasterContext()
        );

        CitizensNpcSyncReport report = service.sync().orElseThrow();

        assertEquals(3, report.totalSeeds());
        assertEquals(3, report.activeSeeds());
        assertEquals(2, report.createdCount());
        assertEquals(1, report.updatedCount());
        assertEquals(0, report.removedCount());
        assertEquals(0, report.invalidCount());
        assertEquals(1, report.orphanCount());

        List<String> syncedNames = gateway.currentManaged().stream()
                .map(CitizensNpcHandle::displayName)
                .sorted()
                .toList();
        assertTrue(syncedNames.contains("레오니드"));
        assertTrue(syncedNames.contains("영지 관리관"));
        assertTrue(syncedNames.contains("남부 결전 안내관"));

        System.out.println("npc_sync_report=total:" + report.totalSeeds()
                + ", created:" + report.createdCount()
                + ", updated:" + report.updatedCount()
                + ", orphan:" + report.orphanSeedIds());
        System.out.println("capital_npc_example_1=npc_capital_leonid_main -> 레오니드 synced/updated");
        System.out.println("capital_npc_example_2=npc_capital_estate_mgr -> 영지 관리관 created");
        System.out.println("region_npc_example_1=npc_south_gatekeeper -> 남부 결전 안내관 created");
    }

    private MasterRegistryContext buildMasterContext() {
        ItemMasterRegistry itemMasterRegistry = new ItemMasterRegistry();
        SkillMasterRegistry skillMasterRegistry = new SkillMasterRegistry();
        BossMasterRegistry bossMasterRegistry = new BossMasterRegistry();
        QuestMasterRegistry questMasterRegistry = new QuestMasterRegistry();
        AchievementMasterRegistry achievementMasterRegistry = new AchievementMasterRegistry();
        RegionMasterRegistry regionMasterRegistry = new RegionMasterRegistry();
        TownMasterRegistry townMasterRegistry = new TownMasterRegistry();
        NpcMasterRegistry npcMasterRegistry = new NpcMasterRegistry();

        regionMasterRegistry.register(new RegionMaster("capital", "Capital", "imperial", true, ""));
        regionMasterRegistry.register(new RegionMaster("south", "South", "volcano", false, ""));

        townMasterRegistry.register(new TownMaster("capital_main", "capital", "capital", "Imperial Capital", true, true, true, true));
        townMasterRegistry.register(new TownMaster("south_c_01", "south", "count", "South Forge Hub", true, true, true, false));

        npcMasterRegistry.register(new NpcMaster("npc_capital_leonid", "capital", "capital_main", "Leonid", "imperial_envoy", "capital_main_hub", true, ""));
        npcMasterRegistry.register(new NpcMaster("npc_capital_estate_mgr", "capital", "capital_main", "Estate Manager", "estate_unlock_npc", "estate_unlock_profile", false, ""));
        npcMasterRegistry.register(new NpcMaster("npc_south_gatekeeper", "south", "south_c_01", "South Gatekeeper", "boss_entry_npc", "boss_entry_south", false, ""));

        return new MasterRegistryContext(
                itemMasterRegistry,
                skillMasterRegistry,
                bossMasterRegistry,
                questMasterRegistry,
                achievementMasterRegistry,
                regionMasterRegistry,
                townMasterRegistry,
                npcMasterRegistry
        );
    }

    private static final class InMemoryCitizensNpcGateway implements CitizensNpcGateway {
        private final AtomicInteger idSequence = new AtomicInteger(2000);
        private final Map<Integer, NpcState> byId = new LinkedHashMap<>();

        @Override
        public boolean isAvailable() {
            return true;
        }

        @Override
        public Result<List<CitizensNpcHandle>> listManagedNpcs() {
            return Result.success(currentManaged());
        }

        @Override
        public Result<Optional<CitizensNpcHandle>> findNpcByEntity(org.bukkit.entity.Entity entity) {
            return Result.success(Optional.empty());
        }

        @Override
        public Result<CitizensNpcHandle> createNpc(CitizensNpcSeed seed) {
            int id = idSequence.incrementAndGet();
            NpcState state = new NpcState(id, seed.npcSeedId(), seed.entityType(), seed.displayName());
            byId.put(id, state);
            return Result.success(state.toHandle());
        }

        @Override
        public Result<CitizensNpcHandle> recreateNpc(CitizensNpcHandle existing, CitizensNpcSeed seed) {
            byId.remove(existing.npcId());
            return createNpc(seed);
        }

        @Override
        public Result<CitizensNpcHandle> updateNameAndLocation(CitizensNpcHandle existing, CitizensNpcSeed seed) {
            NpcState state = byId.get(existing.npcId());
            if (state == null) {
                return createNpc(seed);
            }
            state.displayName = seed.displayName();
            state.entityType = seed.entityType();
            state.seedId = seed.npcSeedId();
            return Result.success(state.toHandle());
        }

        @Override
        public Result<Void> applySkin(CitizensNpcHandle npc, CitizensNpcSeed seed) {
            return Result.success();
        }

        @Override
        public Result<Void> setProtection(CitizensNpcHandle npc, boolean value) {
            return Result.success();
        }

        @Override
        public Result<Void> setLookClose(CitizensNpcHandle npc, boolean value) {
            return Result.success();
        }

        @Override
        public Result<Void> setMetadata(CitizensNpcHandle npc, String key, String value) {
            NpcState state = byId.get(npc.npcId());
            if (state == null) {
                return Result.success();
            }
            state.metadata.put(key, value == null ? "" : value);
            if (META_SEED_ID.equals(key)) {
                state.seedId = value == null ? "" : value;
            }
            return Result.success();
        }

        @Override
        public Result<String> getMetadata(CitizensNpcHandle npc, String key) {
            NpcState state = byId.get(npc.npcId());
            if (state == null) {
                return Result.success("");
            }
            return Result.success(state.metadata.getOrDefault(key, ""));
        }

        @Override
        public Result<Void> deleteNpc(CitizensNpcHandle npc) {
            byId.remove(npc.npcId());
            return Result.success();
        }

        void preload(String seedId, EntityType entityType, String displayName) {
            int id = idSequence.incrementAndGet();
            NpcState state = new NpcState(id, seedId, entityType, displayName);
            state.metadata.put(META_SEED_ID, seedId);
            byId.put(id, state);
        }

        List<CitizensNpcHandle> currentManaged() {
            List<CitizensNpcHandle> handles = new ArrayList<>();
            for (NpcState state : byId.values()) {
                if (state.seedId == null || state.seedId.isBlank()) {
                    continue;
                }
                handles.add(state.toHandle());
            }
            return List.copyOf(handles);
        }

        private static final class NpcState {
            private final int id;
            private String seedId;
            private EntityType entityType;
            private String displayName;
            private final Map<String, String> metadata = new LinkedHashMap<>();

            private NpcState(int id, String seedId, EntityType entityType, String displayName) {
                this.id = id;
                this.seedId = seedId;
                this.entityType = entityType;
                this.displayName = displayName;
            }

            private CitizensNpcHandle toHandle() {
                return new CitizensNpcHandle(id, seedId, entityType, displayName);
            }
        }
    }
}
