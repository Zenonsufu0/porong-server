package kr.zenon.rpg.common.registry.master;

import kr.zenon.rpg.common.registry.master.model.SkillMaster;

public final class SkillMasterRegistry extends AbstractMasterRegistry<SkillMaster> {
    public SkillMasterRegistry() {
        super(SkillMaster::skillId);
    }
}
