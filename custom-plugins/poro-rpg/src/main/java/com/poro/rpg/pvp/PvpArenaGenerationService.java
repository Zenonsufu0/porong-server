package com.poro.rpg.pvp;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * PvP 아레나 10개 (5×2) 를 월드에 자동 생성.
 * 방당 20×20×20 석재벽돌 박스, 내부 18×18×18 빈 공간.
 * spawn-a (서쪽 모서리), spawn-b (동쪽 모서리)에 양측 플레이어 배치.
 *
 * 방 1개당 1초 간격 → 총 10초 예상.
 */
public final class PvpArenaGenerationService {

    private static final int COLS = 5;
    private static final int ROWS = 2;
    private static final int TOTAL = COLS * ROWS; // 10
    private static final int SIZE  = 20;
    private static final int GAP   = 30;

    private final Plugin plugin;

    public PvpArenaGenerationService(Plugin plugin) {
        this.plugin = plugin;
    }

    public void generate(CommandSender sender, World world, int baseX, int baseY, int baseZ) {
        sender.sendMessage("§6[아레나 생성] §e" + TOTAL + "개 방 생성 시작. §7월드: " + world.getName()
                + " 기준: (" + baseX + ", " + baseY + ", " + baseZ + ")");
        sender.sendMessage("§7방 1개당 1초 간격으로 처리. 총 §e10초 §7예상.");

        new BukkitRunnable() {
            int room = 0;
            @Override
            public void run() {
                if (room >= TOTAL) {
                    sender.sendMessage("§a[아레나 생성] 완료! " + TOTAL + "개 생성됨.");
                    sender.sendMessage("§7config.yml pvp-room-slots 섹션에 좌표 등록 필요.");
                    this.cancel();
                    return;
                }
                int col = room % COLS;
                int row = room / COLS;
                int rx  = baseX + col * GAP;
                int rz  = baseZ + row * GAP;
                buildRoom(world, rx, baseY, rz);
                sender.sendMessage("§7[아레나] §e" + (room + 1) + "/" + TOTAL
                        + " §7완료 (" + rx + ", " + baseY + ", " + rz + ")");
                room++;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    private void buildRoom(World world, int rx, int ry, int rz) {
        // 외벽 석재벽돌
        for (int y = ry; y < ry + SIZE; y++)
            for (int x = rx; x < rx + SIZE; x++)
                for (int z = rz; z < rz + SIZE; z++)
                    world.getBlockAt(x, y, z).setType(Material.STONE_BRICKS, false);
        // 내부 공기
        for (int y = ry + 1; y < ry + SIZE - 1; y++)
            for (int x = rx + 1; x < rx + SIZE - 1; x++)
                for (int z = rz + 1; z < rz + SIZE - 1; z++)
                    world.getBlockAt(x, y, z).setType(Material.AIR, false);
        // 바닥 (y = ry+1) 매끄러운 돌
        for (int x = rx + 1; x < rx + SIZE - 1; x++)
            for (int z = rz + 1; z < rz + SIZE - 1; z++)
                world.getBlockAt(x, ry + 1, z).setType(Material.SMOOTH_STONE, false);
    }
}
