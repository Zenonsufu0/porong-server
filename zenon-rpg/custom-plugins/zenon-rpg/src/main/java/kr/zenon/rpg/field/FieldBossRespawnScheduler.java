package kr.zenon.rpg.field;

import kr.zenon.rpg.gui.ExploreHubGui;
import org.bukkit.plugin.Plugin;

public final class FieldBossRespawnScheduler implements ExploreHubGui.FieldStateProvider {
    private final Plugin plugin;

    public FieldBossRespawnScheduler(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public ExploreHubGui.BossStatus status(String fieldId) {
        return ExploreHubGui.BossStatus.RESPAWNING;
    }

    @Override
    public int respawnMinutes(String fieldId) {
        return 30;
    }

    @Override
    public int playerCount(String fieldId) {
        return 0;
    }

    public Plugin plugin() {
        return plugin;
    }
}
