package kr.zenon.rpg.listener;

import kr.zenon.rpg.boss.db.BossSessionRepository;
import kr.zenon.rpg.boss.party.PartyManager;
import kr.zenon.rpg.boss.room.BossRoomManager;
import kr.zenon.rpg.growth.island.IslandTerritoryStateStore;
import kr.zenon.rpg.gui.BossClearRecordsGui;
import kr.zenon.rpg.gui.BossHubGui;
import kr.zenon.rpg.gui.GuiTitles;
import kr.zenon.rpg.gui.MainHubGui;
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
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class BossHubListener implements Listener {

    private final PartyManager              partyManager;
    private final BossRoomManager           bossRoomManager;
    private final BossSessionRepository     bossSessionRepository;
    private final IslandTerritoryStateStore islandTerritoryStateStore;
    /** 파티 목록 진입 경로 — true=보스 허브에서, false=파티 관리에서. 뒤로가기 대상 결정. */
    private final Map<UUID, Boolean> partyListFromBoss = new ConcurrentHashMap<>();
    /** 파티 생성 진행 상태 — 보스 선택 → 인원 선택 → 제목 채팅 입력. */
    private final Map<UUID, String>  pendingCreateBoss = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> pendingCreateSize = new ConcurrentHashMap<>();
    private final java.util.Set<UUID> pendingCreateTitle = ConcurrentHashMap.newKeySet();
    /** 보스룸 입장 위임(표지판과 동일 로직). 생성 순서상 setter 주입. */
    private BossRoomListener bossRoomEntry;

    public void setBossRoomEntry(BossRoomListener entry) { this.bossRoomEntry = entry; }

    /** 보스룸 내 파티 탈퇴/해산 시 포기 확인 게이트. 생성 순서상 setter 주입. */
    private BossAbandonListener bossAbandonListener;

    public void setBossAbandonListener(BossAbandonListener l) { this.bossAbandonListener = l; }

    public BossHubListener(PartyManager partyManager,
                           BossRoomManager bossRoomManager,
                           BossSessionRepository bossSessionRepository,
                           IslandTerritoryStateStore islandTerritoryStateStore) {
        this.partyManager              = partyManager;
        this.bossRoomManager           = bossRoomManager;
        this.bossSessionRepository     = bossSessionRepository;
        this.islandTerritoryStateStore = islandTerritoryStateStore;
    }

    public void openBossHub(Player player)  {
        BossHubGui.open(player, partyManager.findParty(player.getUniqueId()).isPresent());
    }
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
        } else if (GuiTitles.BOSS_DETAIL.equals(event.getView().title())) {
            event.setCancelled(true);
            if (event.getRawSlot() == 22) BossHubGui.openBossInfo(player); // 뒤로
        } else if (GuiTitles.BOSS_CLEAR_RECORDS.equals(event.getView().title())) {
            event.setCancelled(true);
            if (event.getRawSlot() == BossClearRecordsGui.SLOT_BACK) openBossHub(player);
        } else if (GuiTitles.PARTY_HUB.equals(event.getView().title())) {
            event.setCancelled(true);
            handlePartyHub(player, event.getRawSlot());
        } else if (GuiTitles.PARTY_LIST.equals(event.getView().title())) {
            event.setCancelled(true);
            handlePartyList(player, event.getRawSlot());
        } else if (GuiTitles.PARTY_CREATE_BOSS.equals(event.getView().title())) {
            event.setCancelled(true);
            handleCreateBoss(player, event.getRawSlot());
        } else if (GuiTitles.PARTY_CREATE_SIZE.equals(event.getView().title())) {
            event.setCancelled(true);
            handleCreateSize(player, event.getRawSlot());
        }
    }

    // ── 보스 허브 (27슬롯 중간 허브) ────────────────────────────────

    private void handleBossHub(Player player, int slot) {
        switch (slot) {
            case 10 -> { // 파티 있으면 관리, 없으면 생성 플로우
                if (partyManager.findParty(player.getUniqueId()).isPresent()) renderPartyHub(player);
                else BossHubGui.openPartyCreateBoss(player);
            }
            case 12 -> renderPartyList(player, true); // 보스 허브에서 진입
            case 14 -> BossHubGui.openBossInfo(player);
            case 16 -> BossClearRecordsGui.open(player, bossSessionRepository,
                            islandTerritoryStateStore.getOrCreate(player.getUniqueId(), player.getName()));
            case 18 -> MainHubGui.open(player);
        }
    }

    // ── 보스 선택 (54슬롯) ────────────────────────────────────────

    private void handleBossInfo(Player player, int slot) {
        if (slot == 45) { openBossHub(player); return; }
        // 보스 아이콘 클릭 → 상세(패턴·페이즈·데미지) GUI (DL-129 추가#18)
        String bossId = BossHubGui.bossIdAt(slot);
        if (bossId != null) BossHubGui.openBossDetail(player, bossId);
    }

    // ── 파티 허브 ────────────────────────────────────────────────────

    private void handlePartyHub(Player player, int slot) {
        UUID uid = player.getUniqueId();
        Optional<PartyManager.Party> party = partyManager.findParty(uid);

        if (party.isEmpty()) {
            switch (slot) {
                case 20 -> BossHubGui.openPartyCreateBoss(player); // 생성 플로우 진입
                case 24 -> renderPartyList(player, false); // 파티 관리에서 진입
                case 45 -> openBossHub(player);
            }
        } else {
            switch (slot) {
                case 39 -> { // 탈퇴 / 해산
                    // 보스룸 안이면 포기 확인 GUI 경유 (리더=위임·멤버=탈퇴 + 영지 귀환, DL-129 추가#25)
                    if (bossAbandonListener != null && bossAbandonListener.promptIfInRoom(player, "home")) return;
                    boolean isLeader = party.get().leaderId().equals(uid);
                    partyManager.leaveParty(uid);
                    player.sendMessage(isLeader ? "§c[파티] 파티를 해산했습니다." : "§c[파티] 파티에서 탈퇴했습니다.");
                    openBossHub(player); // 해산/탈퇴 후 보스 허브로
                }
                case 41 -> { // 보스룸 입장 (리더 + 전원 준비) — 바로 텔레포트
                    PartyManager.Party p = party.get();
                    if (!p.leaderId().equals(uid)) { player.sendMessage("§c[파티] 리더만 입장할 수 있습니다."); return; }
                    if (!p.allMembersReady()) { player.sendMessage("§c[파티] 파티원이 모두 준비해야 입장할 수 있습니다."); return; }
                    if (bossRoomEntry == null) { player.sendMessage("§c[보스] 입장 처리를 사용할 수 없습니다."); return; }
                    bossRoomManager.setPendingBoss(uid, p.bossId());
                    partyManager.markStarted(uid);
                    player.closeInventory();
                    bossRoomEntry.enterBossRoom(player); // 방 배정 + 파티 전원 텔레포트
                }
                case 45 -> openBossHub(player);
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
            // 헤더 — 제목·보스·인원
            inv.setItem(4, MainHubGui.icon(Material.WITHER_SKELETON_SKULL, "§f" + p.title(),
                    List.of("§7보스: §e" + BossHubGui.bossNameById(p.bossId()),
                            "§7인원: §e" + p.size() + "§7/§e" + p.maxSize())));
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
            // 보스룸 입장 (리더 전용 — 전원 준비 시 활성)
            if (isLeader) {
                boolean ready = p.allMembersReady();
                inv.setItem(41, MainHubGui.icon(ready ? Material.ENDER_PEARL : Material.GRAY_DYE,
                        ready ? "§a보스룸 입장" : "§7보스룸 입장 §8(대기)",
                        ready ? List.of("§7전원 준비 완료", "§a클릭하여 입장")
                              : List.of("§7파티원이 모두 준비해야 합니다")));
            }
        }

        inv.setItem(45, MainHubGui.icon(Material.ARROW, "§7뒤로", List.of("§7보스 허브")));
        player.openInventory(inv);
    }

    // ── 파티 목록 ────────────────────────────────────────────────────

    private void handlePartyList(Player player, int slot) {
        if (slot == 45) {
            // 진입 경로로 복귀 — 보스 허브에서 왔으면 보스 허브, 아니면 파티 관리
            if (partyListFromBoss.getOrDefault(player.getUniqueId(), false)) openBossHub(player);
            else renderPartyHub(player);
            return;
        }

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
        // 진입 경로 유지(재렌더 시) — 저장된 origin 사용, 없으면 파티 관리
        renderPartyList(player, partyListFromBoss.getOrDefault(player.getUniqueId(), false));
    }

    private void renderPartyList(Player player, boolean fromBoss) {
        partyListFromBoss.put(player.getUniqueId(), fromBoss);
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

        inv.setItem(45, MainHubGui.icon(Material.ARROW, "§7뒤로",
                List.of(fromBoss ? "§7보스 허브" : "§7파티 관리")));
        player.openInventory(inv);
    }

    // ── 파티 생성 플로우 (보스 → 인원 → 제목 채팅) ──────────────────────

    private void handleCreateBoss(Player player, int slot) {
        if (slot == 45) { openBossHub(player); return; }
        String bossId = BossHubGui.bossIdAt(slot);
        if (bossId == null) return;
        if (BossHubGui.bossNeedsUnlockAt(slot)
                && !bossRoomManager.hasCleared(player.getUniqueId(), "void_herald")) {
            player.sendMessage("§c[보스] §7공허 사자(시즌6)를 클리어해야 최종보스 파티를 만들 수 있습니다.");
            return;
        }
        pendingCreateBoss.put(player.getUniqueId(), bossId);
        renderCreateSize(player, BossHubGui.bossNameAt(slot));
    }

    private void renderCreateSize(Player player, String bossName) {
        Inventory inv = Bukkit.createInventory(null, 27, GuiTitles.PARTY_CREATE_SIZE);
        ItemStack pane = MainHubGui.icon(Material.BLACK_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < 27; i++) inv.setItem(i, pane);
        inv.setItem(4, MainHubGui.icon(Material.WITHER_SKELETON_SKULL, "§c대상: §f" + bossName,
                List.of("§7최대 인원을 선택하세요")));
        inv.setItem(11, MainHubGui.icon(Material.LIME_WOOL, "§a1인 (솔로)", List.of("§7나 혼자 도전")));
        inv.setItem(13, MainHubGui.icon(Material.LIME_WOOL, "§a2인",        List.of("§7최대 2명")));
        inv.setItem(15, MainHubGui.icon(Material.LIME_WOOL, "§a3인",        List.of("§7최대 3명")));
        inv.setItem(22, MainHubGui.icon(Material.ARROW, "§7뒤로", List.of("§7보스 선택")));
        player.openInventory(inv);
    }

    private void handleCreateSize(Player player, int slot) {
        UUID uid = player.getUniqueId();
        if (slot == 22) { BossHubGui.openPartyCreateBoss(player); return; }
        int size;
        switch (slot) {
            case 11 -> size = 1;
            case 13 -> size = 2;
            case 15 -> size = 3;
            default -> { return; }
        }
        if (!pendingCreateBoss.containsKey(uid)) { openBossHub(player); return; }
        pendingCreateSize.put(uid, size);
        pendingCreateTitle.add(uid);
        player.closeInventory();
        player.sendMessage(net.kyori.adventure.text.Component.text(
                "§e[파티] 채팅에 파티 제목을 입력하세요. §7(16자 이내, 취소: '취소')"));
    }

    /** 파티 제목 채팅 입력 → 파티 생성. */
    @EventHandler
    public void onCreateTitleChat(io.papermc.paper.event.player.AsyncChatEvent event) {
        Player player = event.getPlayer();
        UUID uid = player.getUniqueId();
        if (!pendingCreateTitle.contains(uid)) return;
        event.setCancelled(true); // 입력 메시지는 채팅에 노출하지 않음
        pendingCreateTitle.remove(uid);
        String raw = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
                .plainText().serialize(event.message()).trim();
        Bukkit.getScheduler().runTask(
                org.bukkit.plugin.java.JavaPlugin.getProvidingPlugin(getClass()), () -> {
            String boss = pendingCreateBoss.remove(uid);
            Integer size = pendingCreateSize.remove(uid);
            if (boss == null || size == null) return;
            if (raw.isBlank() || raw.equalsIgnoreCase("취소")) {
                player.sendMessage("§7[파티] 파티 생성을 취소했습니다.");
                return;
            }
            String title = raw.length() > 16 ? raw.substring(0, 16) : raw;
            if (partyManager.createParty(uid, player.getName(), boss, title, size)) {
                player.sendMessage("§a[파티] 파티 생성: §f" + title + " §7(최대 " + size + "인)");
                renderPartyHub(player);
            } else {
                player.sendMessage("§c[파티] 이미 파티에 소속되어 있습니다.");
            }
        });
    }
}
