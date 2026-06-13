package kr.zenon.rpg.common.registry.master;

import kr.zenon.rpg.common.registry.master.model.QuestMaster;

public final class QuestMasterRegistry extends AbstractMasterRegistry<QuestMaster> {
    public QuestMasterRegistry() {
        super(QuestMaster::questId);
    }
}
