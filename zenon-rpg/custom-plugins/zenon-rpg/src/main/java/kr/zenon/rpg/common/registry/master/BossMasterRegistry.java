package kr.zenon.rpg.common.registry.master;

import kr.zenon.rpg.common.registry.master.model.BossMaster;

public final class BossMasterRegistry extends AbstractMasterRegistry<BossMaster> {
    public BossMasterRegistry() {
        super(BossMaster::bossId);
    }
}
