package com.poro.rpg.listener;

import com.poro.rpg.gui.GuiTitles;
import com.poro.rpg.gui.MainHubGui;
import com.poro.rpg.gui.PvpHubGui;
import com.poro.rpg.gui.PvpRankingGui;
import com.poro.rpg.pvp.PvpFriendlyService;
import com.poro.rpg.pvp.PvpMatchService;
import com.poro.rpg.pvp.PvpMatchType;
import com.poro.rpg.pvp.PvpRatingService;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PvpHubListener implements Listener {

    private final PvpRatingService    ratingService;
    private final PvpMatchService     matchService;
    private final PvpFriendlyService  friendlyService;
    private final Set<UUID> pendingFriendlyChat = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public PvpHubListener(PvpRatingService ratingService,
                          PvpMatchService matchService,
                          PvpFriendlyService friendlyService) {
        this.ratingService    = ratingService;
        this.matchService     = matchService;
        this.friendlyService  = friendlyService;
    }

    public void openHub(Player player) {
        PvpHubGui.open(player, ratingService);
    }

    public void openRanking(Player player) {
        PvpRankingGui.open(player, ratingService);
    }

    public PvpFriendlyService friendlyService() {
        return friendlyService;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        if (GuiTitles.PVP_HUB.equals(event.getView().title())) {
            event.setCancelled(true);
            handleHub(player, event.getRawSlot());
        } else if (GuiTitles.PVP_RANKING.equals(event.getView().title())) {
            event.setCancelled(true);
            if (event.getRawSlot() == PvpRankingGui.SLOT_BACK) {
                PvpHubGui.open(player, ratingService);
            }
        }
    }

    /** 친선 닉네임 채팅 입력 처리. */
    @EventHandler
    public void onChat(io.papermc.paper.event.player.AsyncChatEvent event) {
        Player player = event.getPlayer();
        UUID uid = player.getUniqueId();
        if (!pendingFriendlyChat.contains(uid)) return;
        event.setCancelled(true); // 입력 메시지는 채팅에 노출하지 않음
        pendingFriendlyChat.remove(uid);
        String msg = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
                .plainText().serialize(event.message()).trim();
        // Bukkit API는 메인 스레드에서
        Bukkit.getScheduler().runTask(
                org.bukkit.plugin.java.JavaPlugin.getProvidingPlugin(getClass()), () -> {
            if (msg.isBlank() || msg.equalsIgnoreCase("취소")) {
                player.sendMessage("§7[친선] 신청을 취소했습니다.");
                return;
            }
            friendlyService.sendRequest(player, msg);
        });
    }

    private void handleHub(Player player, int slot) {
        switch (slot) {
            case PvpHubGui.SLOT_FREE -> {
                player.closeInventory();
                matchService.enqueue(player, PvpMatchType.FREE);
            }
            case PvpHubGui.SLOT_RANKED -> {
                player.closeInventory();
                matchService.enqueue(player, PvpMatchType.RANKED);
            }
            case PvpHubGui.SLOT_FRIENDLY -> openFriendlyChat(player);
            case PvpHubGui.SLOT_RANKING  -> PvpRankingGui.open(player, ratingService);
            case PvpHubGui.SLOT_BACK     -> MainHubGui.open(player);
        }
    }

    private void openFriendlyChat(Player player) {
        pendingFriendlyChat.add(player.getUniqueId());
        player.closeInventory();
        player.sendMessage(Component.text("§e[친선] 채팅에 상대 닉네임을 입력하세요. §7(취소: '취소')"));
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player p = event.getEntity();
        if (matchService.isInMatch(p.getUniqueId())) {
            matchService.onPlayerDeath(p);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        matchService.onPlayerQuit(event.getPlayer());
        pendingFriendlyChat.remove(event.getPlayer().getUniqueId());
    }
}
