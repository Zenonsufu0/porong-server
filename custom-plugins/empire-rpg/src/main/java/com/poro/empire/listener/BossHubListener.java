package com.poro.empire.listener;

import com.poro.empire.boss.db.BossSessionRepository;
import com.poro.empire.boss.party.PartyManager;
import com.poro.empire.boss.room.BossRoomManager;
import com.poro.empire.growth.island.IslandTerritoryStateStore;
import com.poro.empire.gui.BossClearRecordsGui;
import com.poro.empire.gui.BossHubGui;
import com.poro.empire.gui.GuiTitles;
import com.poro.empire.gui.MainHubGui;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class BossHubListener implements Listener {

    private final PartyManager              partyManager;
    private final BossRoomManager           bossRoomManager;
    private final BossSessionRepository     bossSessionRepository;
    private final IslandTerritoryStateStore islandTerritoryStateStore;

    public BossHubListener(PartyManager partyManager,
                           BossRoomManager bossRoomManager,
                           BossSessionRepository bossSessionRepository,
                           IslandTerritoryStateStore islandTerritoryStateStore) {
        this.partyManager              = partyManager;
        this.bossRoomManager           = bossRoomManager;
        this.bossSessionRepository     = bossSessionRepository;
        this.islandTerritoryStateStore = islandTerritoryStateStore;
    }

    public void openBossHub(Player player)  { BossHubGui.open(player); }
    public void openPartyHub(Player player) { renderPartyHub(player); }
    public void openPartyList(Player player){ renderPartyList(player); }
    public void openClearRecords(Player player) {
        BossClearRecordsGui.open(player, bossSessionRepository,
                islandTerritoryStateStore.getOrCreate(player.getUniqueId(), player.getName()));
    }
    public void openBossInfo(Player player) { BossHubGui.openBossInfo(player); }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        if (GuiTitles.BOSS_HUB.equals(event.getView().title())) {
            event.setCancelled(true);
            handleBossHub(player, event.getRawSlot());
        } else if (GuiTitles.BOSS_INFO.equals(event.getView().title())) {
            event.setCancelled(true);
            handleBossInfo(player, event.getRawSlot());
        } else if (GuiTitles.BOSS_CLEAR_RECORDS.equals(event.getView().title())) {
            event.setCancelled(true);
            if (event.getRawSlot() == BossClearRecordsGui.SLOT_BACK) BossHubGui.open(player);
        } else if (GuiTitles.PARTY_HUB.equals(event.getView().title())) {
            event.setCancelled(true);
            handlePartyHub(player, event.getRawSlot());
        } else if (GuiTitles.PARTY_LIST.equals(event.getView().title())) {
            event.setCancelled(true);
            handlePartyList(player, event.getRawSlot());
        }
    }

    // ── 보스 허브 (27슬롯 중간 허브) ────────────────────────────────

    private void handleBossHub(Player player, int slot) {
        switch (slot) {
            case 10 -> renderPartyHub(player);
            case 12 -> renderPartyList(player);
            case 14 -> BossHubGui.openBossInfo(player);
            case 16 -> BossClearRecordsGui.open(player, bossSessionRepository,
                            islandTerritoryStateStore.getOrCreate(player.getUniqueId(), player.getName()));
            case 18 -> MainHubGui.open(player);
        }
    }

    // ── 보스 선택 (54슬롯) ────────────────────────────────────────

    private void handleBossInfo(Player player, int slot) {
        String bossId = BossHubGui.bossIdAt(slot);
        if (bossId != null) {
            if (BossHubGui.bossNeedsUnlockAt(slot)
                    && !bossRoomManager.hasCleared(player.getUniqueId(), "void_herald")) {
                player.sendMessage("§c[보스] §7공허 사자(시즌6)를 클리어해야 최종보스에 도전할 수 있습니다.");
                return;
            }
            Optional<PartyManager.Party> party = partyManager.findParty(player.getUniqueId());
            int size = party.map(PartyManager.Party::size).orElse(1);
            bossRoomManager.setPendingBoss(player.getUniqueId(), bossId);
            player.closeInventory();
            player.sendMessage("§6[보스] §f" + BossHubGui.bossNameAt(slot)
                    + " §7선택됨. 파티: §e" + size + "인§7."
                    + " §7보스룸 앞 §e[보스] §7표지판을 우클릭하여 입장하세요.");
            return;
        }
        if (slot == 45) BossHubGui.open(player);
    }

    // ── 파티 허브 ────────────────────────────────────────────────────

    private void handlePartyHub(Player player, int slot) {
        UUID uid = player.getUniqueId();
        Optional<PartyManager.Party> party = partyManager.findParty(uid);

        if (party.isEmpty()) {
            switch (slot) {
                case 20 -> { // 파티 생성
                    if (partyManager.createParty(uid, player.getName())) {
                        player.sendMessage("§a[파티] 파티를 생성했습니다.");
                    } else {
                        player.sendMessage("§c[파티] 이미 파티에 소속되어 있습니다.");
                    }
                    renderPartyHub(player);
                }
                case 24 -> renderPartyList(player);
                case 45 -> BossHubGui.open(player);
            }
        } else {
            switch (slot) {
                case 39 -> { // 탈퇴 / 해산
                    boolean isLeader = party.get().leaderId().equals(uid);
                    partyManager.leaveParty(uid);
                    player.sendMessage(isLeader ? "§c[파티] 파티를 해산했습니다." : "§c[파티] 파티에서 탈퇴했습니다.");
                    renderPartyHub(player);
                }
                case 45 -> BossHubGui.open(player);
            }
        }
    }

    private void renderPartyHub(Player player) {
        UUID uid = player.getUniqueId();
        Optional<PartyManager.Party> party = partyManager.findParty(uid);
        Inventory inv = Bukkit.createInventory(null, 54, GuiTitles.PARTY_HUB);
        ItemStack pane = MainHubGui.icon(Material.BLACK_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < 54; i++) inv.setItem(i, pane);

        if (party.isEmpty()) {
            inv.setItem(20, MainHubGui.icon(Material.GREEN_WOOL, "§a파티 생성",
                    List.of("§7──────────────", "§7새 파티를 만들어", "§7보스에 도전하세요.", "", "§a클릭하여 생성")));
            inv.setItem(24, MainHubGui.icon(Material.BOOK, "§f파티 목록",
                    List.of("§7──────────────", "§7열린 파티에 참가합니다.", "", "§a클릭하여 보기")));
        } else {
            PartyManager.Party p = party.get();
            boolean isLeader = p.leaderId().equals(uid);
            int[] memberSlots = {20, 22, 24};
            for (int i = 0; i < p.members().size() && i < 3; i++) {
                UUID mid = p.members().get(i);
                String role = mid.equals(p.leaderId()) ? "§6[리더] " : "§7[멤버] ";
                String name = Bukkit.getOfflinePlayer(mid).getName();
                if (name == null) name = mid.toString().substring(0, 8);
                inv.setItem(memberSlots[i], MainHubGui.icon(Material.PLAYER_HEAD,
                        role + "§f" + name, List.of("§7파티원 §e" + (i + 1))));
            }
            String actionLabel = isLeader ? "§c파티 해산" : "§c파티 탈퇴";
            inv.setItem(39, MainHubGui.icon(Material.RED_WOOL, actionLabel,
                    List.of("§7──────────────", "§c클릭하여 " + (isLeader ? "해산" : "탈퇴"))));
        }

        inv.setItem(45, MainHubGui.icon(Material.ARROW, "§7뒤로", List.of("§7보스 허브")));
        player.openInventory(inv);
    }

    // ── 파티 목록 ────────────────────────────────────────────────────

    private void handlePartyList(Player player, int slot) {
        if (slot == 45) { renderPartyHub(player); return; }

        List<PartyManager.Party> open = partyManager.openParties();
        if (slot < 36 && slot < open.size()) {
            PartyManager.Party p = open.get(slot);
            if (partyManager.joinParty(p.leaderId(), player.getUniqueId())) {
                player.sendMessage("§a[파티] §f" + p.leaderName() + "§a의 파티에 참가했습니다.");
                Player leader = Bukkit.getPlayer(p.leaderId());
                if (leader != null) leader.sendMessage("§a[파티] §f" + player.getName() + "§a이(가) 참가했습니다.");
                renderPartyHub(player);
            } else {
                player.sendMessage("§c[파티] 참가할 수 없습니다. (정원 초과 또는 이미 파티 중)");
                renderPartyList(player);
            }
        }
    }

    private void renderPartyList(Player player) {
        List<PartyManager.Party> open = partyManager.openParties();
        Inventory inv = Bukkit.createInventory(null, 54, GuiTitles.PARTY_LIST);
        ItemStack pane = MainHubGui.icon(Material.BLACK_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < 54; i++) inv.setItem(i, pane);

        for (int i = 0; i < Math.min(open.size(), 36); i++) {
            PartyManager.Party p = open.get(i);
            inv.setItem(i, MainHubGui.icon(Material.LIME_WOOL,
                    "§f" + p.leaderName() + "§7의 파티",
                    List.of("§7──────────────",
                            "§7현재 인원: §e" + p.size() + "/3",
                            "",
                            "§a클릭하여 참가")));
        }

        if (open.isEmpty()) {
            inv.setItem(22, MainHubGui.icon(Material.GRAY_WOOL, "§7열린 파티 없음",
                    List.of("§7새 파티를 생성하거나", "§7잠시 후 다시 확인하세요.")));
        }

        inv.setItem(45, MainHubGui.icon(Material.ARROW, "§7뒤로", List.of("§7파티 관리")));
        player.openInventory(inv);
    }
}
