package kr.zenon.rpg.boss.room;

import net.kyori.adventure.text.Component;
import org.bukkit.block.BlockFace;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.block.sign.Side;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * 보스룸 30개 (6열 × 5행) 를 월드에 자동 생성.
 * 방당 50×50×50 석재벽돌 박스, 내부 48×48×48 빈 공간,
 * 남쪽 벽에 8×4 출입문, 출입문 위 "[보스]" 표지판.
 *
 * 방 1개당 1초(20틱) 간격으로 생성 → 총 30초 예상.
 * 서버 오픈 전 준비 단계에서 1회만 실행.
 */
public final class BossRoomGenerationService {

    private static final int COLS = 6;
    private static final int ROWS = 5;
    private static final int TOTAL = COLS * ROWS; // 30
    private static final int SIZE  = 50;
    private static final int GAP   = 60; // 50 방 + 10 간격

    private final Plugin plugin;

    public BossRoomGenerationService(Plugin plugin) {
        this.plugin = plugin;
    }

    /**
     * @param baseX 첫 방의 서쪽 모서리 X 좌표
     * @param baseY 바닥 Y 좌표 (일반적으로 64)
     * @param baseZ 첫 방의 북쪽 모서리 Z 좌표
     */
    public void generate(CommandSender sender, World world, int baseX, int baseY, int baseZ) {
        sender.sendMessage("§6[보스룸 생성] §e" + TOTAL + "개 방 생성 시작. §7월드: " + world.getName()
                + " 기준: (" + baseX + ", " + baseY + ", " + baseZ + ")");
        sender.sendMessage("§7방 1개당 1초 간격으로 처리. 총 §e30초 §7예상.");

        new BukkitRunnable() {
            int room = 0;
            @Override
            public void run() {
                if (room >= TOTAL) {
                    sender.sendMessage("§a[보스룸 생성] 완료! " + TOTAL + "개 생성됨.");
                    this.cancel();
                    return;
                }
                int col = room % COLS;
                int row = room / COLS;
                int rx  = baseX + col * GAP;
                int rz  = baseZ + row * GAP;
                buildRoom(world, rx, baseY, rz);
                sender.sendMessage("§7[보스룸] §e" + (room + 1) + "/" + TOTAL
                        + " §7완료 (" + rx + ", " + baseY + ", " + rz + ")");
                room++;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    private void buildRoom(World world, int rx, int ry, int rz) {
        // 1단계: 외부 전체 석재벽돌로 채우기
        for (int y = ry; y < ry + SIZE; y++)
            for (int x = rx; x < rx + SIZE; x++)
                for (int z = rz; z < rz + SIZE; z++)
                    world.getBlockAt(x, y, z).setType(Material.STONE_BRICKS, false);

        // 2단계: 내부 공기로 비우기 (벽 두께 1블록)
        for (int y = ry + 1; y < ry + SIZE - 1; y++)
            for (int x = rx + 1; x < rx + SIZE - 1; x++)
                for (int z = rz + 1; z < rz + SIZE - 1; z++)
                    world.getBlockAt(x, y, z).setType(Material.AIR, false);

        // 출입문·표지판 제거 — GUI 입장 버튼으로 텔레포트하므로 완전 밀폐 석재벽돌 방.
        // (남쪽 벽은 1단계에서 채운 STONE_BRICKS 그대로 유지)
    }
}
