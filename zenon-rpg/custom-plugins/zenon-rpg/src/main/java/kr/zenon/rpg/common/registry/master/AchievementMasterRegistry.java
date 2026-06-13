package kr.zenon.rpg.common.registry.master;

import kr.zenon.rpg.common.registry.master.model.AchievementMaster;

public final class AchievementMasterRegistry extends AbstractMasterRegistry<AchievementMaster> {
    public AchievementMasterRegistry() {
        super(AchievementMaster::achievementId);
    }
}
