package com.poro.rpg.common.registry.master;

import com.poro.rpg.common.registry.master.model.SkillMaster;

public final class SkillMasterRegistry extends AbstractMasterRegistry<SkillMaster> {
    public SkillMasterRegistry() {
        super(SkillMaster::skillId);
    }
}
