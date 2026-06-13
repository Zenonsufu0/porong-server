package kr.zenon.rpg.common.registry.master;

import kr.zenon.rpg.common.registry.master.model.NpcMaster;

public final class NpcMasterRegistry extends AbstractMasterRegistry<NpcMaster> {
    public NpcMasterRegistry() {
        super(NpcMaster::npcId);
    }
}
