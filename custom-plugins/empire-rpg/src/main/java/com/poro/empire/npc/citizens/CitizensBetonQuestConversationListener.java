package com.poro.empire.npc.citizens;

import com.poro.empire.common.logging.DomainLogger;
import com.poro.empire.common.result.Result;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class CitizensBetonQuestConversationListener implements Listener {
    private static final long CLICK_GUARD_MS = 300L;

    private final JavaPlugin plugin;
    private final DomainLogger logger;
    private final CitizensNpcGateway gateway;
    private final Map<UUID, Long> recentClickAt = new ConcurrentHashMap<>();

    public CitizensBetonQuestConversationListener(
            JavaPlugin plugin,
            DomainLogger logger,
            CitizensNpcGateway gateway
    ) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.logger = Objects.requireNonNull(logger, "logger");
        this.gateway = Objects.requireNonNull(gateway, "gateway");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onPlayerRightClickEntity(PlayerInteractEntityEvent event) {
        if (!gateway.isAvailable()) {
            return;
        }
        if (!(event.getRightClicked() != null && event.getPlayer() != null)) {
            return;
        }
        if (event.getHand() != null && event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        if (!plugin.getServer().getPluginManager().isPluginEnabled("BetonQuest")) {
            return;
        }

        Result<java.util.Optional<CitizensNpcHandle>> npcResult = gateway.findNpcByEntity(event.getRightClicked());
        if (npcResult.isFailure()) {
            logger.warn("Failed to inspect clicked entity for Citizens NPC bridge: " + npcResult.message());
            return;
        }
        if (npcResult.value().isEmpty()) {
            return;
        }
        CitizensNpcHandle npc = npcResult.value().get();
        String seedId = readMetadata(npc, CitizensNpcGateway.META_SEED_ID);
        if (!"npc_capital_leonid_main".equalsIgnoreCase(seedId)) {
            return;
        }

        String conversationEventId = readMetadata(npc, CitizensNpcGateway.META_BETON_CONVERSATION_ID);
        if (conversationEventId.isBlank()) {
            return;
        }

        Player player = event.getPlayer();
        if (!tryEnterClickWindow(player.getUniqueId())) {
            return;
        }

        event.setCancelled(true);
        String eventId = normalizeEventIdForCompatibility(npc, conversationEventId);
        String command = "q event " + player.getName() + " " + eventId;
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);

        logger.info("Triggered BetonQuest conversation bridge. player=" + player.getName()
                + ", npc_id=" + npc.npcId()
                + ", npc_seed_id=" + seedId
                + ", event_id=" + eventId);
    }

    private String readMetadata(CitizensNpcHandle npc, String key) {
        Result<String> metadataResult = gateway.getMetadata(npc, key);
        if (metadataResult.isFailure()) {
            logger.warn("Failed to read NPC metadata key=" + key + ", npc_id=" + npc.npcId());
            return "";
        }
        return metadataResult.value() == null ? "" : metadataResult.value().trim();
    }

    private boolean tryEnterClickWindow(UUID playerId) {
        long now = System.currentTimeMillis();
        Long previous = recentClickAt.get(playerId);
        if (previous != null && now - previous < CLICK_GUARD_MS) {
            return false;
        }
        recentClickAt.put(playerId, now);
        return true;
    }

    private String normalizeEventIdForCompatibility(CitizensNpcHandle npc, String raw) {
        String normalized = raw.trim();
        if (!normalized.isBlank()) {
            return normalized;
        }
        String seedId = readMetadata(npc, CitizensNpcGateway.META_SEED_ID).toLowerCase(Locale.ROOT);
        if ("npc_capital_leonid_main".equals(seedId)) {
            return "conv_capital_intro";
        }
        return raw;
    }
}
