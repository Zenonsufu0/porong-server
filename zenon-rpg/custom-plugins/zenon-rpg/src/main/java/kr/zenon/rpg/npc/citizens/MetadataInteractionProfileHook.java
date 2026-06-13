package kr.zenon.rpg.npc.citizens;

import kr.zenon.rpg.common.result.ErrorCode;
import kr.zenon.rpg.common.result.Result;

public final class MetadataInteractionProfileHook implements InteractionProfileHook {
    @Override
    public Result<Void> bind(CitizensNpcGateway gateway, CitizensNpcHandle npc, CitizensNpcSeed seed) {
        if (!seed.hasInteractionProfile()) {
            return Result.success();
        }
        Result<Void> meta = gateway.setMetadata(npc, CitizensNpcGateway.META_INTERACTION_PROFILE_ID, seed.interactionProfileId());
        if (meta.isFailure()) {
            return Result.failure(
                    ErrorCode.UNKNOWN,
                    "Failed to bind interaction profile metadata: npc_seed_id=" + seed.npcSeedId(),
                    meta.cause()
            );
        }
        return Result.success();
    }
}

