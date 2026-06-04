package com.poro.rpg.npc.citizens;

import com.poro.rpg.common.result.Result;
import com.poro.rpg.common.seed.CsvSeedLoader;
import com.poro.rpg.common.seed.CsvRow;
import org.bukkit.entity.EntityType;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class CitizensNpcSeedLoader {
    private final Path seedPath;

    public CitizensNpcSeedLoader(Path seedPath) {
        this.seedPath = Objects.requireNonNull(seedPath, "seedPath");
    }

    public Result<List<CitizensNpcSeed>> load() {
        CsvSeedLoader<CitizensNpcSeed> loader = new CsvSeedLoader<>(
                "npc_spawn_seed",
                seedPath,
                this::map
        );
        return loader.load();
    }

    private CitizensNpcSeed map(CsvRow row) {
        return new CitizensNpcSeed(
                row.required("npc_seed_id"),
                row.required("npc_master_id"),
                row.required("region_code"),
                row.required("town_id"),
                row.required("world_name"),
                row.optionalDouble("x", 0.0d),
                row.optionalDouble("y", 0.0d),
                row.optionalDouble("z", 0.0d),
                (float) row.optionalDouble("yaw", 0.0d),
                (float) row.optionalDouble("pitch", 0.0d),
                parseEntityType(row.optional("entity_type")),
                row.required("display_name"),
                row.optional("skin_type"),
                row.optional("skin_value"),
                row.optional("role_type"),
                row.optional("interaction_profile_id"),
                row.optional("quest_start_id"),
                row.optional("beton_conversation_id"),
                row.optionalBoolean("is_protected", true),
                row.optionalBoolean("lookclose", true),
                row.optionalBoolean("should_spawn", true)
        );
    }

    private EntityType parseEntityType(String raw) {
        if (raw == null || raw.isBlank()) {
            return EntityType.PLAYER;
        }
        try {
            return EntityType.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return EntityType.PLAYER;
        }
    }
}

