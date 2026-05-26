package com.poro.empire.listener;

import com.poro.empire.boss.party.PartyManager;
import com.poro.empire.boss.room.BossRoomManager;
import com.poro.empire.boss.room.BossRoomSlot;
import com.poro.empire.common.registry.master.BossMasterRegistry;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.Plugin;

import java.util.Optional;
import java.util.UUID;

public final class BossRoomListener implements Listener {

    private final BossRoomManager bossRoomManager;
    private final PartyManager    partyManager;

    public BossRoomListener(Plugin plugin, BossRoomManager bossRoomManager,
                            BossMasterRegistry bossMasters, PartyManager partyManager) {
        this.bossRoomManager = bossRoomManager;
        this.partyManager    = partyManager;
    }

    /**
     * "[보스]" 표지판 우클릭 → 빈 방 배정 → 파티 전원 텔레포트 입장.
     * 방이 모두 사용 중이면 대기 안내.
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
            long free  = bossRoomManager.freeCount();
            int  total = bossRoomManager.totalCount();
            player.sendMessage("§c[보스] 현재 모든 보스룸(" + total + "개)이 사용 중입니다. 잠시 후 다시 시도하세요.");
            return;
        }

        BossRoomSlot slot = slotOpt.get();
        bossRoomManager.clearPendingBoss(uuid);
        bossRoomManager.enterRoom(uuid, slot.id());
        player.teleport(slot.playerSpawn());

        // 파티 리더이면 온라인 파티원 동반 입장
        int coEntered = 0;
        if (party.isPresent()) {
            for (UUID memberId : party.get().members()) {
                if (memberId.equals(uuid)) continue;
                if (bossRoomManager.isInBossRoom(memberId)) continue;
                Player member = Bukkit.getPlayer(memberId);
                if (member == null) continue;
                bossRoomManager.enterRoom(memberId, slot.id());
                bossRoomManager.clearPendingBoss(memberId);
                member.teleport(slot.playerSpawn());
                member.sendMessage("§6[보스] §7파티 리더 §f" + player.getName()
                        + "§7의 입장으로 §f" + bossId + " §7보스룸 §e" + slot.id() + "§7번에 입장합니다!");
                coEntered++;
            }
        }

        if (coEntered > 0) {
            player.sendMessage("§6[보스] §f" + bossId + " §7룸 §e" + slot.id()
                    + "§7번 입장. 파티원 §e" + coEntered + "명 §7함께 입장.");
        } else {
            player.sendMessage("§6[보스] §f" + bossId + " §7룸 §e" + slot.id() + "§7번 입장. 준비하세요!");
        }
    }
}
