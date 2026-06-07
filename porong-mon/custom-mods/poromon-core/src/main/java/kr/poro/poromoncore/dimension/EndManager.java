package kr.poro.poromoncore.dimension;

import kr.poro.poromoncore.PoroMonCore;
import kr.poro.poromoncore.config.ConfigManager;
import kr.poro.poromoncore.config.CoreConfig;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * 엔드 차원 정책 (결정 039): 드래곤전 없음 + 엔드시티(바깥섬) 탐험.
 *  - 엔더 드래곤 스폰 시 제거(드래곤전 없음).
 *  - 오버월드→엔드 입장 = 바깥섬 허브로 TP(중앙 섬/드래곤 우회 → 엔드시티 권역 진입).
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

    /** 차원 변경 직후: 오버월드→엔드 = 바깥섬 허브 TP. */
    public static void onChangeWorld(ServerPlayerEntity player, ServerWorld origin, ServerWorld dest) {
        CoreConfig.End cfg = ConfigManager.core().end;
        if (!cfg.enabled || !cfg.hubRedirect) return;
        if (origin.getRegistryKey() == World.OVERWORLD && dest.getRegistryKey() == World.END) {
            BlockPos hub = safeEnd(dest, cfg);
            if (hub != null) {
                player.teleport(dest, hub.getX() + 0.5, hub.getY(), hub.getZ() + 0.5, cfg.hubYaw, 0.0f);
            }
        }
    }

    /** 운영자: 바깥 엔드에서 안전한 섬 표면을 찾아 엔드 허브로 설정(없으면 플랫폼 강제 생성). */
    public static BlockPos buildEndHubAuto(MinecraftServer server) {
        ServerWorld end = server.getWorld(World.END);
        if (end == null) return null;
        CoreConfig.End cfg = ConfigManager.core().end;
        // 중앙 섬(반경 ~1000) 바깥의 외곽 섬을 탐색
        int[] radii = {1100, 1300, 1500, 1700, 2000};
        double[] angles = {0, Math.PI / 2, Math.PI, 3 * Math.PI / 2, Math.PI / 4, 3 * Math.PI / 4};
        for (int r : radii) {
            for (double a : angles) {
                int x = (int) (Math.cos(a) * r);
                int z = (int) (Math.sin(a) * r);
                end.getChunk(x >> 4, z >> 4); // 동기 생성/로드
                for (int y = 90; y > 36; y--) {
                    if (isStandable(end, x, y, z)) {
                        return finalizeEnd(end, cfg, new BlockPos(x, y, z));
                    }
                }
            }
        }
        // 폴백: 외곽에 흑요석 플랫폼 강제 생성
        BlockPos base = new BlockPos(1536, 64, 0);
        for (int dx = -3; dx <= 3; dx++)
            for (int dz = -3; dz <= 3; dz++)
                end.setBlockState(base.add(dx, -1, dz), Blocks.OBSIDIAN.getDefaultState());
        PoroMonCore.LOGGER.warn("[End] 외곽 섬 탐색 실패 — 플랫폼 강제 생성 @ {}", base);
        return finalizeEnd(end, cfg, base);
    }

    private static BlockPos finalizeEnd(ServerWorld end, CoreConfig.End cfg, BlockPos spot) {
        cfg.hubX = spot.getX() + 0.5;
        cfg.hubY = spot.getY();
        cfg.hubZ = spot.getZ() + 0.5;
        ConfigManager.saveCore();
        PoroMonCore.LOGGER.info("[End] 엔드 허브 설정: ({}, {}, {})", spot.getX(), spot.getY(), spot.getZ());
        return spot;
    }

    /** 허브 좌표가 안전하면 그 좌표, 아니면 같은 x/z에서 안전한 표면 Y 탐색(허공 도착 방지). */
    private static BlockPos safeEnd(ServerWorld end, CoreConfig.End cfg) {
        int x = (int) Math.floor(cfg.hubX);
        int z = (int) Math.floor(cfg.hubZ);
        int hy = (int) Math.floor(cfg.hubY);
        if (isStandable(end, x, hy, z)) return new BlockPos(x, hy, z);
        end.getChunk(x >> 4, z >> 4);
        for (int y = 90; y > 36; y--) {
            if (isStandable(end, x, y, z)) return new BlockPos(x, y, z);
        }
        PoroMonCore.LOGGER.warn("[End] 허브 안전 좌표 탐색 실패 (x{} z{}) — 리다이렉트 생략", x, z);
        return null;
    }

    private static boolean isStandable(ServerWorld w, int x, int y, int z) {
        BlockState ground = w.getBlockState(new BlockPos(x, y - 1, z));
        BlockState feet = w.getBlockState(new BlockPos(x, y, z));
        BlockState head = w.getBlockState(new BlockPos(x, y + 1, z));
        return !ground.isAir() && ground.getFluidState().isEmpty() && feet.isAir() && head.isAir();
    }
}
