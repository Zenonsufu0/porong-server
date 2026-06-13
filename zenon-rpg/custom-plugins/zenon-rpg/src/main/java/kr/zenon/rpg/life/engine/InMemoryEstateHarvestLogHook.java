package kr.zenon.rpg.life.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class InMemoryEstateHarvestLogHook implements EstateHarvestLogHook {
    private final List<EstateHarvestLogEntry> entries = new ArrayList<>();

    @Override
    public void onHarvested(EstateHarvestLogEntry entry) {
        entries.add(entry);
    }

    public List<EstateHarvestLogEntry> entries() {
        return Collections.unmodifiableList(entries);
    }
}
