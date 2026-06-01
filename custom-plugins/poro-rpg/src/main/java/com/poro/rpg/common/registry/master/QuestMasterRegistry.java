package com.poro.rpg.common.registry.master;

import com.poro.rpg.common.registry.master.model.QuestMaster;

public final class QuestMasterRegistry extends AbstractMasterRegistry<QuestMaster> {
    public QuestMasterRegistry() {
        super(QuestMaster::questId);
    }
}
