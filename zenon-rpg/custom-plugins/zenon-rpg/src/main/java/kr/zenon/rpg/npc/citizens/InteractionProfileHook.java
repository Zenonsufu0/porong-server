package kr.zenon.rpg.npc.citizens;

import kr.zenon.rpg.common.result.Result;

public interface InteractionProfileHook {
    Result<Void> bind(CitizensNpcGateway gateway, CitizensNpcHandle npc, CitizensNpcSeed seed);
}

