package com.poro.rpg.common.registry.master;

import com.poro.rpg.common.registry.master.model.NpcMaster;

public final class NpcMasterRegistry extends AbstractMasterRegistry<NpcMaster> {
    public NpcMasterRegistry() {
        super(NpcMaster::npcId);
    }
}
