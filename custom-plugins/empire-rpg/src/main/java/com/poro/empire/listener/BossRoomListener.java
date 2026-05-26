package com.poro.empire.listener;

import com.poro.empire.boss.party.PartyManager;
import com.poro.empire.boss.room.BossRoomManager;
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
     * 보스룸 앞 "[보스]" 표지판 우클릭 → 파티 리더 기준 전원 입장.
     * 파티가 없으면 단독 입장. 비리더 멤버가 클릭하면 리더에게 확인하라고 안내.
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
        UUID uuid = player.getUniqueId();

        String bossId = bossRoomManager.getPendingBoss(uuid).orElse(null);
        if (bossId == null) {
            // 파티 멤버인데 본인 pending이 없으면 리더 안내
            partyManager.findParty(uuid).ifPresentOrElse(party -> {
                if (!party.leaderId().equals(uuid)) {
                    player.sendMessage("§e[보스] §7파티 리더 §f"
                            + Bukkit.getOfflinePlayer(party.leaderId()).getName()
                            + "§7이(가) /보스 메뉴에서 보스를 선택하고 이 표지판을 클릭해야 입장됩니다.");
                } else {
                    player.sendMessage("§c[보스] §l/보스§c 메뉴에서 먼저 보스를 선택하세요.");
                }
            }, () -> player.sendMessage("§c[보스] §l/보스§c 메뉴에서 먼저 보스를 선택하세요."));
            return;
        }

        if (bossRoomManager.isInBossRoom(uuid)) {
            player.sendMessage("§c[보스] 이미 보스룸에 입장 중입니다.");
            return;
        }

        bossRoomManager.enterRoom(uuid, bossId);
        bossRoomManager.clearPendingBoss(uuid);

        // 파티 리더인 경우 온라인 파티원 전원 동반 입장
        var party = partyManager.findParty(uuid);
        if (party.isPresent() && party.get().leaderId().equals(uuid)) {
            int coEntered = 0;
            for (UUID memberId : party.get().members()) {
                if (memberId.equals(uuid)) continue;
                if (bossRoomManager.isInBossRoom(memberId)) continue;
                Player member = Bukkit.getPlayer(memberId);
                if (member == null) continue;
                bossRoomManager.enterRoom(memberId, bossId);
                bossRoomManager.clearPendingBoss(memberId);
                member.sendMessage("§6[보스] §7파티 리더 §f" + player.getName()
                        + "§7의 입장으로 §f" + bossId + " §7보스룸에 입장합니다!");
                coEntered++;
            }
            player.sendMessage("§6[보스] §f" + bossId + " §7룸 입장. 파티원 §e" + coEntered + "명 §7함께 입장.");
        } else {
            player.sendMessage("§6[보스] §f" + bossId + " §7룸 입장합니다. 준비하세요!");
        }
    }
}
