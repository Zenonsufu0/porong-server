package kr.zenon.rpg.npc.citizens;

import kr.zenon.rpg.common.result.ErrorCode;
import kr.zenon.rpg.common.result.Result;

import java.util.Objects;

public final class CitizensNpcTraitBinder {
    private final InteractionProfileHook interactionProfileHook;
    private final BetonQuestConversationHook betonQuestConversationHook;

    public CitizensNpcTraitBinder(
            InteractionProfileHook interactionProfileHook,
            BetonQuestConversationHook betonQuestConversationHook
    ) {
        this.interactionProfileHook = Objects.requireNonNull(interactionProfileHook, "interactionProfileHook");
        this.betonQuestConversationHook = Objects.requireNonNull(betonQuestConversationHook, "betonQuestConversationHook");
    }

    public Result<Void> bind(CitizensNpcGateway gateway, CitizensNpcHandle npc, CitizensNpcSeed seed) {
        Objects.requireNonNull(gateway, "gateway");
        Objects.requireNonNull(npc, "npc");
        Objects.requireNonNull(seed, "seed");

        Result<Void> protection = gateway.setProtection(npc, seed.protectedNpc());
        if (protection.isFailure()) {
            return failure("set protection", seed, protection);
        }
        Result<Void> lookClose = gateway.setLookClose(npc, seed.lookClose());
        if (lookClose.isFailure()) {
            return failure("set lookclose", seed, lookClose);
        }
        Result<Void> skin = gateway.applySkin(npc, seed);
        if (skin.isFailure()) {
            return failure("apply skin", seed, skin);
        }

        Result<Void> metaResult = applyManagedMetadata(gateway, npc, seed);
        if (metaResult.isFailure()) {
            return metaResult;
        }

        Result<Void> interactionResult = interactionProfileHook.bind(gateway, npc, seed);
        if (interactionResult.isFailure()) {
            return interactionResult;
        }
        return betonQuestConversationHook.bind(gateway, npc, seed);
    }

    private Result<Void> applyManagedMetadata(CitizensNpcGateway gateway, CitizensNpcHandle npc, CitizensNpcSeed seed) {
        Result<Void> result = gateway.setMetadata(npc, CitizensNpcGateway.META_SEED_ID, seed.npcSeedId());
        if (result.isFailure()) {
            return failure("bind metadata seed", seed, result);
        }
        result = gateway.setMetadata(npc, CitizensNpcGateway.META_MANAGED_MARK, "true");
        if (result.isFailure()) {
            return failure("bind metadata managed_mark", seed, result);
        }
        result = gateway.setMetadata(npc, CitizensNpcGateway.META_NPC_MASTER_ID, seed.npcMasterId());
        if (result.isFailure()) {
            return failure("bind metadata npc_master_id", seed, result);
        }
        result = gateway.setMetadata(npc, CitizensNpcGateway.META_REGION_CODE, seed.regionCode());
        if (result.isFailure()) {
            return failure("bind metadata region_code", seed, result);
        }
        result = gateway.setMetadata(npc, CitizensNpcGateway.META_TOWN_ID, seed.townId());
        if (result.isFailure()) {
            return failure("bind metadata town_id", seed, result);
        }
        result = gateway.setMetadata(npc, CitizensNpcGateway.META_ROLE_TYPE, seed.roleType());
        if (result.isFailure()) {
            return failure("bind metadata role_type", seed, result);
        }

        if (seed.hasQuestStartId()) {
            result = gateway.setMetadata(npc, CitizensNpcGateway.META_QUEST_START_ID, seed.questStartId());
            if (result.isFailure()) {
                return failure("bind metadata quest_start_id", seed, result);
            }
        }
        return Result.success();
    }

    private Result<Void> failure(String action, CitizensNpcSeed seed, Result<Void> source) {
        return Result.failure(
                ErrorCode.UNKNOWN,
                "Failed to " + action + ": npc_seed_id=" + seed.npcSeedId(),
                source.cause()
        );
    }
}

