package com.poro.rpg.npc.citizens;

import com.poro.rpg.common.result.ErrorCode;
import com.poro.rpg.common.result.Result;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Locale;
import java.util.Objects;

public final class MetadataBetonQuestConversationHook implements BetonQuestConversationHook {
    private final JavaPlugin plugin;

    public MetadataBetonQuestConversationHook(JavaPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    @Override
    public Result<Void> bind(CitizensNpcGateway gateway, CitizensNpcHandle npc, CitizensNpcSeed seed) {
        if (!seed.hasBetonConversation()) {
            return Result.success();
        }

        Plugin betonQuest = plugin.getServer().getPluginManager().getPlugin("BetonQuest");
        if (betonQuest == null || !betonQuest.isEnabled()) {
            return Result.success();
        }

        Result<Void> meta = gateway.setMetadata(npc, CitizensNpcGateway.META_BETON_CONVERSATION_ID, seed.betonConversationId());
        if (meta.isFailure()) {
            return Result.failure(
                    ErrorCode.UNKNOWN,
                    "Failed to bind BetonQuest conversation metadata: npc_seed_id=" + seed.npcSeedId(),
                    meta.cause()
            );
        }

        // Guard key can be used by listener layer to avoid double-triggering click events.
        Result<Void> guard = gateway.setMetadata(
                npc,
                "poro.beton_click_guard",
                ("guard:" + seed.npcSeedId()).toLowerCase(Locale.ROOT)
        );
        if (guard.isFailure()) {
            return Result.failure(
                    ErrorCode.UNKNOWN,
                    "Failed to bind BetonQuest click-guard metadata: npc_seed_id=" + seed.npcSeedId(),
                    guard.cause()
            );
        }
        return Result.success();
    }
}

