package com.poro.rpg.common.registry.master;

import com.poro.rpg.common.registry.master.model.AchievementMaster;

public final class AchievementMasterRegistry extends AbstractMasterRegistry<AchievementMaster> {
    public AchievementMasterRegistry() {
        super(AchievementMaster::achievementId);
    }
}
