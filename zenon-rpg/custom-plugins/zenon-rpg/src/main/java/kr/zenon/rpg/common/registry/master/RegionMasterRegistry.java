package kr.zenon.rpg.common.registry.master;

import kr.zenon.rpg.common.registry.master.model.RegionMaster;

public final class RegionMasterRegistry extends AbstractMasterRegistry<RegionMaster> {
    public RegionMasterRegistry() {
        super(RegionMaster::regionCode);
    }
}
