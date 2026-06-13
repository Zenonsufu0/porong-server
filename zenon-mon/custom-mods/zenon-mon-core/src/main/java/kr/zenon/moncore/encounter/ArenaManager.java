package kr.zenon.moncore.encounter;

import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 동적 조우 아레나 (결정: 개인 조우방 = 자동 생성 격리방).
 * 오버월드 높은 Y에 플레이어별 임시 방(바닥+벽+천장 배리어)을 grid로 배정·생성·정리.
 * 맵 빌드와 무관(공중 격리). 종료 시 블록 제거.
 */
public final class ArenaManager {
    private ArenaManager() {}

    private static final int Y = 250;       // 방 바닥 높이(빌드와 겹치지 않는 공중)
    private static final int SIZE = 11;      // 11×11 바닥
    private static final int WALL_H = 5;     // 벽 높이
    private static final int GAP = 24;       // 셀 간격
    private static final int COLS = 64;      // 한 줄 셀 수
    private static final int OFFSET = 1200;  // 보더 중심에서 이격(보더 안)

    private static final Set<Integer> USED = ConcurrentHashMap.newKeySet();

    /** 빈 셀 배정(가장 작은 번호). */
    public static int allocate() {
        int i = 0;
        while (!USED.add(i)) i++;
        return i;
    }

    public static void free(int cell) {
        USED.remove(cell);
    }

    /** 셀의 바닥 최소 코너(x,z 시작, y=바닥). */
    public static BlockPos corner(ServerWorld world, int cell) {
        int bx = (int) world.getWorldBorder().getCenterX() - OFFSET;
        int bz = (int) world.getWorldBorder().getCenterZ() - OFFSET;
        return new BlockPos(bx + (cell % COLS) * GAP, Y, bz + (cell / COLS) * GAP);
    }

    /** 방 중앙 위(플레이어/포켓몬 배치점). */
    public static BlockPos spawnPos(BlockPos corner) {
        return corner.add(SIZE / 2, 1, SIZE / 2);
    }

    /** 방 생성(바닥 잔디 + 벽/천장 배리어). */
    public static void build(ServerWorld world, BlockPos corner) {
        int x0 = corner.getX(), y0 = corner.getY(), z0 = corner.getZ();
        for (int dx = 0; dx < SIZE; dx++) {
            for (int dz = 0; dz < SIZE; dz++) {
                world.setBlockState(new BlockPos(x0 + dx, y0, z0 + dz), Blocks.GRASS_BLOCK.getDefaultState());
                world.setBlockState(new BlockPos(x0 + dx, y0 + WALL_H + 1, z0 + dz), Blocks.BARRIER.getDefaultState());
            }
        }
        for (int h = 1; h <= WALL_H; h++) {
            for (int d = 0; d < SIZE; d++) {
                world.setBlockState(new BlockPos(x0, y0 + h, z0 + d), Blocks.BARRIER.getDefaultState());
                world.setBlockState(new BlockPos(x0 + SIZE - 1, y0 + h, z0 + d), Blocks.BARRIER.getDefaultState());
                world.setBlockState(new BlockPos(x0 + d, y0 + h, z0), Blocks.BARRIER.getDefaultState());
                world.setBlockState(new BlockPos(x0 + d, y0 + h, z0 + SIZE - 1), Blocks.BARRIER.getDefaultState());
            }
        }
    }

    /** 방 제거(전체 공기). */
    public static void clear(ServerWorld world, BlockPos corner) {
        int x0 = corner.getX(), y0 = corner.getY(), z0 = corner.getZ();
        for (int dx = 0; dx < SIZE; dx++)
            for (int dy = 0; dy <= WALL_H + 1; dy++)
                for (int dz = 0; dz < SIZE; dz++)
                    world.setBlockState(new BlockPos(x0 + dx, y0 + dy, z0 + dz), Blocks.AIR.getDefaultState());
    }
}
