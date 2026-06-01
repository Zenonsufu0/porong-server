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
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PvpHubListener implements Listener {

    private final PvpRatingService    ratingService;
    private final PvpMatchService     matchService;
    private final PvpFriendlyService  friendlyService;
    private final Set<UUID> pendingFriendlyAnvil = Collections.newSetFromMap(new ConcurrentHashMap<>());

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
        } else if (pendingFriendlyAnvil.contains(player.getUniqueId())
                && event.getInventory() instanceof AnvilInventory anvil
                && event.getRawSlot() == 2) {
            event.setCancelled(true);
            String targetName = anvil.getRenameText();
            pendingFriendlyAnvil.remove(player.getUniqueId());
            if (targetName == null || targetName.isBlank()) {
                player.sendMessage("§c[친선] 닉네임을 입력하세요.");
                return;
            }
            player.closeInventory();
            friendlyService.sendRequest(player, targetName);
        }
    }

    @EventHandler
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        if (!(event.getView().getPlayer() instanceof Player player)) return;
        if (!pendingFriendlyAnvil.contains(player.getUniqueId())) return;
        String name = event.getInventory().getRenameText();
        if (name == null || name.isBlank()) return;

        event.getInventory().setRepairCost(0);
        ItemStack result = new ItemStack(Material.PAPER);
        ItemMeta meta = result.getItemMeta();
        meta.displayName(Component.text("§a친선 신청: " + name));
        result.setItemMeta(meta);
        event.setResult(result);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player p) {
            pendingFriendlyAnvil.remove(p.getUniqueId());
        }
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
            case PvpHubGui.SLOT_FRIENDLY -> openFriendlyAnvil(player);
            case PvpHubGui.SLOT_RANKING  -> PvpRankingGui.open(player, ratingService);
            case PvpHubGui.SLOT_BACK     -> MainHubGui.open(player);
        }
    }

    private void openFriendlyAnvil(Player player) {
        ItemStack tag = new ItemStack(Material.PAPER);
        ItemMeta meta = tag.getItemMeta();
        meta.displayName(Component.text("상대 닉네임"));
        tag.setItemMeta(meta);
        pendingFriendlyAnvil.add(player.getUniqueId());
        com.poro.rpg.gui.AnvilGuiHelper.open(player, Component.text("친선대전 신청"), tag);
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
    }
}
