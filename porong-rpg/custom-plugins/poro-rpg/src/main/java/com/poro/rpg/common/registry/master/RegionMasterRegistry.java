package com.poro.rpg.common.registry.master;

import com.poro.rpg.common.registry.master.model.RegionMaster;

public final class RegionMasterRegistry extends AbstractMasterRegistry<RegionMaster> {
    public RegionMasterRegistry() {
        super(RegionMaster::regionCode);
    }
}
