package com.poro.empire.listener;

import com.poro.empire.boss.room.BossRoomManager;
import com.poro.empire.common.registry.master.BossMasterRegistry;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
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

    public BossRoomListener(Plugin plugin, BossRoomManager bossRoomManager, BossMasterRegistry bossMasters) {
        this.bossRoomManager = bossRoomManager;
    }

    /**
     * 보스룸 앞에 "[보스]" 텍스트가 적힌 표지판을 우클릭하면 입장 처리.
     * 관리자가 서버 맵에 표지판을 설치하는 방식으로 좌표 의존성 없이 동작.
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
            player.sendMessage("§c[보스] 먼저 §l/보스§c 메뉴에서 도전할 보스를 선택하세요.");
            return;
        }

        if (bossRoomManager.isInBossRoom(uuid)) {
            player.sendMessage("§c[보스] 이미 보스룸에 입장 중입니다.");
            return;
        }

        bossRoomManager.enterRoom(uuid, bossId);
        bossRoomManager.clearPendingBoss(uuid);
        player.sendMessage("§6[보스] §f" + bossId + " §7룸 입장합니다. 준비하세요!");
    }
}
