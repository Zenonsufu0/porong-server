package com.poro.empire.listener;

import com.poro.empire.boss.engine.BossEngineRuntime;
import com.poro.empire.boss.engine.BossEntryRequest;
import com.poro.empire.boss.engine.BossRun;
import com.poro.empire.boss.party.PartyManager;
import com.poro.empire.boss.room.BossRoomManager;
import com.poro.empire.boss.room.BossRoomSlot;
import com.poro.empire.common.registry.master.BossMasterRegistry;
import com.poro.empire.common.result.Result;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiConsumer;

public final class BossRoomListener implements Listener {

    private final BossRoomManager             bossRoomManager;
    private final BossEngineRuntime           bossEngineRuntime;
    private final PartyManager                partyManager;
    /** MythicMobs가 활성화된 경우에만 non-null. (mobId, spawnLocation) → void */
    private final BiConsumer<String, Location> mythicSpawner;

    public BossRoomListener(BossRoomManager bossRoomManager,
                            BossMasterRegistry bossMasters,
                            PartyManager partyManager,
                            BossEngineRuntime bossEngineRuntime,
                            BiConsumer<String, Location> mythicSpawner) {
        this.bossRoomManager   = bossRoomManager;
        this.bossEngineRuntime = bossEngineRuntime;
        this.partyManager      = partyManager;
        this.mythicSpawner     = mythicSpawner;
    }

    /**
     * "[보스]" 표지판 우클릭 → 빈 방 배정 → startRun() → 온라인 파티원 전원 텔레포트 입장 → MythicMob 스폰.
     */
    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block block = event.getClickedBlock();
        if (block == null) return;
        if (!(block.getState() instanceof Sign sign)) return;

        String line0 = PlainTextComponentSerializer.plainText()
                .serialize(sign.getSide(Side.FRONT).line(0)).trim();
        if (!line0.equals("[보스]")) return;

        event.setCancelled(true);
        Player player = event.getPlayer();
        UUID   uuid   = player.getUniqueId();

        // 비리더 파티원: 리더 안내 메시지
        Optional<PartyManager.Party> party = partyManager.findParty(uuid);
        if (party.isPresent() && !party.get().leaderId().equals(uuid)) {
            String leaderName = Bukkit.getOfflinePlayer(party.get().leaderId()).getName();
            player.sendMessage("§e[보스] §7파티 리더 §f" + leaderName + "§7이(가) 이 표지판을 클릭해야 입장됩니다.");
            return;
        }

        String bossId = bossRoomManager.getPendingBoss(uuid).orElse(null);
        if (bossId == null) {
            player.sendMessage("§c[보스] §l/보스§c 메뉴에서 먼저 도전할 보스를 선택하세요.");
            return;
        }

        if (bossRoomManager.isInBossRoom(uuid)) {
            player.sendMessage("§c[보스] 이미 보스룸에 입장 중입니다.");
            return;
        }

        // 빈 방 배정
        Optional<BossRoomSlot> slotOpt = bossRoomManager.assignRoom(uuid, bossId);
        if (slotOpt.isEmpty()) {
            player.sendMessage("§c[보스] 현재 모든 보스룸(" + bossRoomManager.totalCount() + "개)이 사용 중입니다. 잠시 후 다시 시도하세요.");
            return;
        }
        BossRoomSlot slot = slotOpt.get();

        // 온라인 멤버만 수집 — run participant = 실제 입장 가능한 인원
        List<Player> onlineMembers = new ArrayList<>();
        List<String> memberIds    = new ArrayList<>();
        memberIds.add(uuid.toString());
        if (party.isPresent()) {
            for (UUID memberId : party.get().members()) {
                if (memberId.equals(uuid)) continue;
                Player member = Bukkit.getPlayer(memberId);
                if (member != null) {
                    onlineMembers.add(member);
                    memberIds.add(memberId.toString());
                }
            }
        }

        // 보스 런 시작
        BossEntryRequest request = new BossEntryRequest(bossId, uuid.toString(), memberIds);
        Result<BossRun> runResult = bossEngineRuntime.runService().startRun(request);
        if (runResult.isFailure()) {
            bossRoomManager.releaseSlot(slot.id());
            player.sendMessage("§c[보스] 입장 실패: " + runResult.errorCode().name());
            return;
        }

        BossRun run = runResult.value();
        bossRoomManager.registerRun(run.runId(), slot.id());
        bossRoomManager.clearPendingBoss(uuid);

        // 리더 입장
        bossRoomManager.enterRoom(uuid, slot.id());
        player.teleport(slot.playerSpawn());

        // 온라인 파티원 동반 입장
        for (Player member : onlineMembers) {
            if (bossRoomManager.isInBossRoom(member.getUniqueId())) continue;
            bossRoomManager.enterRoom(member.getUniqueId(), slot.id());
            bossRoomManager.clearPendingBoss(member.getUniqueId());
            member.teleport(slot.playerSpawn());
            member.sendMessage("§6[보스] §7파티 리더 §f" + player.getName()
                    + "§7의 입장으로 §f" + bossId + " §7보스룸 §e" + slot.id() + "§7번에 입장합니다!");
        }

        // MythicMob 스폰 (MM 비활성화 시 mythicSpawner == null)
        if (mythicSpawner != null) {
            mythicSpawner.accept(bossId, slot.bossSpawn());
        }

        int coCount = onlineMembers.size();
        if (coCount > 0) {
            player.sendMessage("§6[보스] §f" + bossId + " §7룸 §e" + slot.id()
                    + "§7번 입장. 파티원 §e" + coCount + "명 §7함께 입장.");
        } else {
            player.sendMessage("§6[보스] §f" + bossId + " §7룸 §e" + slot.id() + "§7번 입장. 준비하세요!");
        }
    }
}
