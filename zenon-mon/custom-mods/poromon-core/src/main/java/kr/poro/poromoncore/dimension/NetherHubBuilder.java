package kr.poro.poromoncore.dimension;

import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.MobSpawnerBlockEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * 네더 허브 물리 건설 (결정 039 IB-005 3단계).
 * 고정 좌표에 안전 플랫폼 + 귀환 네더 포탈 + 블레이즈 스포너를 배치.
 * 허브 보호(NetherManager.isProtectedBreak, 반경 protectRadius)가 포탈·스포너 파괴를 막는다.
 */
public final class NetherHubBuilder {
    private NetherHubBuilder() {}

    private static final int HALF = 10;      // 21×21 플랫폼(보호 반경 기본 10과 일치)
    private static final int CLEAR_H = 6;     // 위로 비울 높이

    /**
     * center(플레이어 발 위치 기준)에 허브 건설. 반환 = 플레이어 도착 지점(플랫폼 위 중앙).
     * floor = center.y-1 에 바닥, center.y..+CLEAR_H 공기.
     */
    public static BlockPos build(ServerWorld nether, BlockPos center) {
        int cx = center.getX(), fy = center.getY(), cz = center.getZ();

        // 1) 플랫폼(블랙스톤 바닥) + 머리 위 공기
        for (int dx = -HALF; dx <= HALF; dx++) {
            for (int dz = -HALF; dz <= HALF; dz++) {
                nether.setBlockState(new BlockPos(cx + dx, fy - 1, cz + dz), Blocks.BLACKSTONE.getDefaultState());
                for (int dy = 0; dy < CLEAR_H; dy++) {
                    nether.setBlockState(new BlockPos(cx + dx, fy + dy, cz + dz), Blocks.AIR.getDefaultState());
                }
            }
        }
        // 1-1) 둘레 벽(블랙스톤, 천장 1칸까지) — 용암/몹 유입 차단. -Z면 중앙에 3×3 출입구.
        for (int dx = -HALF; dx <= HALF; dx++) {
            for (int dy = 0; dy <= CLEAR_H; dy++) {
                wall(nether, cx + dx, fy + dy, cz + HALF);
                boolean door = (dx >= -1 && dx <= 1) && (dy <= 2);   // -Z 벽 출입구
                if (!door) wall(nether, cx + dx, fy + dy, cz - HALF);
            }
        }
        for (int dz = -HALF; dz <= HALF; dz++) {
            for (int dy = 0; dy <= CLEAR_H; dy++) {
                wall(nether, cx - HALF, fy + dy, cz + dz);
                wall(nether, cx + HALF, fy + dy, cz + dz);
            }
        }
        // 천장(블랙스톤) — 위쪽 용암/몹 차단
        for (int dx = -HALF; dx <= HALF; dx++)
            for (int dz = -HALF; dz <= HALF; dz++)
                nether.setBlockState(new BlockPos(cx + dx, fy + CLEAR_H, cz + dz), Blocks.BLACKSTONE.getDefaultState());

        // 2) 귀환 네더 포탈(오른쪽). AXIS=X, 내부 2×3, 외곽 흑요석.
        buildPortal(nether, new BlockPos(cx + 6, fy, cz));

        // 3) 블레이즈 스포너(왼쪽). 보호 반경 안 → 파괴 불가.
        BlockPos spawnerPos = new BlockPos(cx - 6, fy, cz);
        nether.setBlockState(spawnerPos, Blocks.SPAWNER.getDefaultState());
        BlockEntity be = nether.getBlockEntity(spawnerPos);
        if (be instanceof MobSpawnerBlockEntity spawner) {
            spawner.setEntityType(EntityType.BLAZE, nether.getRandom());
            spawner.markDirty();
        }

        // 도착 지점 = 중앙
        return new BlockPos(cx, fy, cz);
    }

    private static void wall(ServerWorld w, int x, int y, int z) {
        w.setBlockState(new BlockPos(x, y, z), Blocks.BLACKSTONE.getDefaultState());
    }

    /** base = 포탈 내부 좌하단. 내부 2(x)×3(y), z 고정. 외곽 흑요석 프레임 + 내부 NETHER_PORTAL. */
    private static void buildPortal(ServerWorld w, BlockPos base) {
        int bx = base.getX(), by = base.getY(), bz = base.getZ();
        // 프레임(흑요석): 바닥/천장 + 양 기둥
        for (int x = -1; x <= 2; x++) {
            w.setBlockState(new BlockPos(bx + x, by - 1, bz), Blocks.OBSIDIAN.getDefaultState());
            w.setBlockState(new BlockPos(bx + x, by + 3, bz), Blocks.OBSIDIAN.getDefaultState());
        }
        for (int y = 0; y < 3; y++) {
            w.setBlockState(new BlockPos(bx - 1, by + y, bz), Blocks.OBSIDIAN.getDefaultState());
            w.setBlockState(new BlockPos(bx + 2, by + y, bz), Blocks.OBSIDIAN.getDefaultState());
        }
        // 내부 포탈 블록(AXIS=X)
        for (int x = 0; x < 2; x++) {
            for (int y = 0; y < 3; y++) {
                w.setBlockState(new BlockPos(bx + x, by + y, bz),
                        Blocks.NETHER_PORTAL.getDefaultState().with(Properties.HORIZONTAL_AXIS, Direction.Axis.X));
            }
        }
    }
}
