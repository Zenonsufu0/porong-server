package kr.zenon.rpg.common.registry.master;

import kr.zenon.rpg.common.registry.master.model.TownMaster;

public final class TownMasterRegistry extends AbstractMasterRegistry<TownMaster> {
    public TownMasterRegistry() {
        super(TownMaster::townId);
    }
}
