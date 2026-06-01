package com.poro.rpg.npc.citizens;

import com.poro.rpg.common.result.Result;

public interface InteractionProfileHook {
    Result<Void> bind(CitizensNpcGateway gateway, CitizensNpcHandle npc, CitizensNpcSeed seed);
}

