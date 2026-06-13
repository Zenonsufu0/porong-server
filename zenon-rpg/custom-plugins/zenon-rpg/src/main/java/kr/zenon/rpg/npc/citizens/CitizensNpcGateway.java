package kr.zenon.rpg.npc.citizens;

import kr.zenon.rpg.common.result.Result;
import org.bukkit.entity.Entity;

import java.util.List;
import java.util.Optional;

public interface CitizensNpcGateway {
    String META_SEED_ID = "poro.npc_seed_id";
    String META_NPC_MASTER_ID = "poro.npc_master_id";
    String META_REGION_CODE = "poro.region_code";
    String META_TOWN_ID = "poro.town_id";
    String META_ROLE_TYPE = "poro.npc_role_type";
    String META_INTERACTION_PROFILE_ID = "poro.interaction_profile_id";
    String META_QUEST_START_ID = "poro.quest_start_id";
    String META_BETON_CONVERSATION_ID = "poro.beton_conversation_id";
    String META_MANAGED_MARK = "poro.npc_managed";

    boolean isAvailable();

    Result<List<CitizensNpcHandle>> listManagedNpcs();

    Result<Optional<CitizensNpcHandle>> findNpcByEntity(Entity entity);

    Result<CitizensNpcHandle> createNpc(CitizensNpcSeed seed);

    Result<CitizensNpcHandle> recreateNpc(CitizensNpcHandle existing, CitizensNpcSeed seed);

    Result<CitizensNpcHandle> updateNameAndLocation(CitizensNpcHandle existing, CitizensNpcSeed seed);

    Result<Void> applySkin(CitizensNpcHandle npc, CitizensNpcSeed seed);

    Result<Void> setProtection(CitizensNpcHandle npc, boolean value);

    Result<Void> setLookClose(CitizensNpcHandle npc, boolean value);

    Result<Void> setMetadata(CitizensNpcHandle npc, String key, String value);

    Result<String> getMetadata(CitizensNpcHandle npc, String key);

    Result<Void> deleteNpc(CitizensNpcHandle npc);
}
