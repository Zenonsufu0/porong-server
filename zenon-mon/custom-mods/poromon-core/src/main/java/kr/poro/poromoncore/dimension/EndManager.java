package kr.poro.poromoncore.dimension;

import kr.poro.poromoncore.PoroMonCore;
import kr.poro.poromoncore.config.ConfigManager;
import kr.poro.poromoncore.config.CoreConfig;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

/**
 * 엔드 차원 정책 (결정 039): 드래곤전 없음 + 엔드시티(바깥섬) 탐험.
 *  - 엔더 드래곤 스폰 시 제거.
 *  - 오버월드→엔드 입장 = **무작위 외곽 섬으로 착지**(입장마다 다른 곳 → 신선한 시티/함선,
 *    겉날개 함선당 1개 고갈 완화). 중앙 섬/드래곤 우회.
 *  - 복귀 = /poromon hub(기존 오버월드 허브 TP).
 */
public final class EndManager {
    private EndManager() {}

    /** 엔더 드래곤 로드/스폰 시 제거. */
    public static void onEntityLoad(Entity entity, ServerWorld world) {
        CoreConfig.End cfg = ConfigManager.core().end;
        if (!cfg.enabled || !cfg.removeDragon) return;
        if (entity instanceof EnderDragonEntity) {
            entity.discard();
            PoroMonCore.LOGGER.info("[End] 엔더 드래곤 제거(드래곤전 비활성)");
        }
    }

    /** 차원 변경 직후: 오버월드→엔드 = 무작위 외곽 섬 착지. */
    public static void onChangeWorld(ServerPlayerEntity player, ServerWorld origin, ServerWorld dest) {
        CoreConfig.End cfg = ConfigManager.core().end;
        if (!cfg.enabled || !cfg.randomLanding) return;
        if (origin.getRegistryKey() != World.OVERWORLD || dest.getRegistryKey() != World.END) return;

        BlockPos spot = findRandomOuter(dest, cfg);
        player.teleport(dest, spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, 0.0f, 0.0f);
        player.sendMessage(net.minecraft.text.Text.literal(
                "§5[엔드] 바깥 엔드로 진입했습니다. §7엔드시티를 탐험하세요. (복귀: /poromon hub)"), false);
    }

    /** 외곽 엔드(중앙 섬 바깥)에서 무작위 안전 섬 표면 탐색. 실패 시 마지막 좌표에 흑요석 플랫폼. */
    private static BlockPos findRandomOuter(ServerWorld end, CoreConfig.End cfg) {
        Random rnd = end.getRandom();
        int band = Math.max(1, cfg.maxRadius - cfg.minRadius);
        int lastX = cfg.minRadius, lastZ = 0;
        for (int attempt = 0; attempt < cfg.maxAttempts; attempt++) {
            int r = cfg.minRadius + rnd.nextInt(band);
            double a = rnd.nextDouble() * Math.PI * 2.0;
            int x = (int) (Math.cos(a) * r), z = (int) (Math.sin(a) * r);
            lastX = x; lastZ = z;
            end.getChunk(x >> 4, z >> 4); // 동기 생성/로드
            for (int y = 90; y > 36; y--) {
                if (isStandable(end, x, y, z)) return new BlockPos(x, y, z);
            }
        }
        // 폴백: 빈 공간이면 마지막 좌표에 작은 흑요석 플랫폼
        for (int dx = -2; dx <= 2; dx++)
            for (int dz = -2; dz <= 2; dz++)
                end.setBlockState(new BlockPos(lastX + dx, 63, lastZ + dz), Blocks.OBSIDIAN.getDefaultState());
        PoroMonCore.LOGGER.warn("[End] 외곽 섬 탐색 실패 — 플랫폼 생성 @ ({}, 64, {})", lastX, lastZ);
        return new BlockPos(lastX, 64, lastZ);
    }

    private static boolean isStandable(ServerWorld w, int x, int y, int z) {
        BlockState ground = w.getBlockState(new BlockPos(x, y - 1, z));
        BlockState feet = w.getBlockState(new BlockPos(x, y, z));
        BlockState head = w.getBlockState(new BlockPos(x, y + 1, z));
        return !ground.isAir() && ground.getFluidState().isEmpty() && feet.isAir() && head.isAir();
    }
}
