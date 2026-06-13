package com.poro.rpg.common.registry.master;

import com.poro.rpg.common.registry.master.model.TownMaster;

public final class TownMasterRegistry extends AbstractMasterRegistry<TownMaster> {
    public TownMasterRegistry() {
        super(TownMaster::townId);
    }
}
