package kr.zenon.rpg.common.registry.master;

import kr.zenon.rpg.common.registry.master.model.ItemMaster;

public final class ItemMasterRegistry extends AbstractMasterRegistry<ItemMaster> {
    public ItemMasterRegistry() {
        super(ItemMaster::itemId);
    }
}
