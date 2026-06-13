package kr.zenon.rpg.life.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class InMemoryLifeCraftLogHook implements LifeCraftLogHook {
    private final List<LifeCraftLogEntry> entries = new ArrayList<>();

    @Override
    public void onCrafted(LifeCraftLogEntry entry) {
        entries.add(entry);
    }

    public List<LifeCraftLogEntry> entries() {
        return Collections.unmodifiableList(entries);
    }
}
