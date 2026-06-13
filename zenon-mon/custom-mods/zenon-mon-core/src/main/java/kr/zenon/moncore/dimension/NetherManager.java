package kr.zenon.moncore.dimension;

import kr.zenon.moncore.ZenonMonCore;
import kr.zenon.moncore.config.ConfigManager;
import kr.zenon.moncore.config.CoreConfig;
import kr.zenon.moncore.data.PlayerProgress;
import kr.zenon.moncore.data.ZenonMonState;
import net.minecraft.block.BlockState;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.border.WorldBorder;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 네더 차원 정책 (결정 039, IB-005):
 *  - 시작 시 네더 월드보더 설정(지름 5000, ÷8 비적용).
 *  - 고정 허브 포탈: 오버월드→네더 = 허브 도착 / 네더→오버월드 = 플레이어별 진입 좌표 복귀.
 *  - 허브 보호: 중심 반경(체비셰프) 내 블록 파괴 차단(포탈·블레이즈 스포너).
 * 진입 좌표는 매 틱 마지막 오버월드 위치를 추적해 네더 진입 시 PlayerProgress에 저장.
 */
public final class NetherManager {
    private NetherManager() {}

    /** 매 틱 갱신: 오버월드에 있는 플레이어의 마지막 좌표(네더 복귀용). */
    private static final Map<UUID, double[]> LAST_OVERWORLD = new ConcurrentHashMap<>();

    /** 서버 시작 시(SERVER_STARTED) 네더 월드보더 적용. */
    public static void applyBorder(MinecraftServer server) {
        CoreConfig.Nether cfg = ConfigManager.core().nether;
        if (!cfg.enabled) return;
        ServerWorld nether = server.getWorld(World.NETHER);
        if (nether == null) return;
        WorldBorder wb = nether.getWorldBorder();
        wb.setCenter(cfg.borderCenterX, cfg.borderCenterZ);
        wb.setSize(cfg.borderDiameter);
        ZenonMonCore.LOGGER.info("[Nether] 월드보더 적용: 중심({}, {}) 지름 {}",
                cfg.borderCenterX, cfg.borderCenterZ, cfg.borderDiameter);
    }

    /** 매 틱(20틱): 오버월드 플레이어 위치 추적. */
    public static void trackOverworld(MinecraftServer server) {
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            if (p.getServerWorld().getRegistryKey() == World.OVERWORLD) {
                LAST_OVERWORLD.put(p.getUuid(), new double[]{p.getX(), p.getY(), p.getZ()});
            }
        }
    }

    /** 차원 변경 직후(AFTER_PLAYER_CHANGE_WORLD) 포탈 리다이렉트. */
    public static void onChangeWorld(ServerPlayerEntity player, ServerWorld origin, ServerWorld dest) {
        CoreConfig.Nether cfg = ConfigManager.core().nether;
        if (!cfg.enabled || !cfg.hubRedirect) return;
        var originKey = origin.getRegistryKey();
        var destKey = dest.getRegistryKey();

        // 오버월드 → 네더: 진입 좌표 저장 후 고정 허브로
        if (originKey == World.OVERWORLD && destKey == World.NETHER) {
            double[] last = LAST_OVERWORLD.get(player.getUuid());
            ZenonMonState state = ZenonMonState.get(player.getServer());
            PlayerProgress pp = state.getOrCreate(player.getUuid());
            if (last != null) {
                pp.netherReturnSet = true;
                pp.netherReturnX = last[0]; pp.netherReturnY = last[1]; pp.netherReturnZ = last[2];
                state.markDirty();
            }
            BlockPos hub = safeHub(dest, cfg);
            if (hub != null) {
                player.teleport(dest, hub.getX() + 0.5, hub.getY(), hub.getZ() + 0.5, cfg.hubYaw, 0.0f);
            }
            return;
        }
        // 네더 → 오버월드: 저장된 진입 좌표로 복귀
        if (originKey == World.NETHER && destKey == World.OVERWORLD) {
            PlayerProgress pp = ZenonMonState.get(player.getServer()).getOrCreate(player.getUuid());
            if (pp.netherReturnSet) {
                player.teleport(dest, pp.netherReturnX, pp.netherReturnY, pp.netherReturnZ,
                        player.getYaw(), player.getPitch());
            }
        }
    }

    /** 운영자: 플레이어 현재 네더 위치에 허브 건설 + config 좌표 저장. (결정 039 3단계) */
    public static boolean buildHubAt(ServerPlayerEntity player) {
        if (player.getServerWorld().getRegistryKey() != World.NETHER) {
            player.sendMessage(Text.literal("§c[네더] 네더에서 실행하세요."), false);
            return false;
        }
        ServerWorld nether = player.getServerWorld();
        CoreConfig.Nether cfg = ConfigManager.core().nether;
        BlockPos arrival = finalizeHub(nether, cfg, player.getBlockPos(), player.getYaw());
        player.teleport(nether, cfg.hubX, cfg.hubY, cfg.hubZ, cfg.hubYaw, 0.0f);
        player.sendMessage(Text.literal("§a[네더] 허브 건설 완료 — 좌표 ("
                + arrival.getX() + ", " + arrival.getY() + ", " + arrival.getZ()
                + ") 저장. 반경 " + cfg.protectRadius + " 보호 활성."), false);
        return true;
    }

    /**
     * 운영자: 보더 중심 근처에서 안전한 자리를 자동 탐색해 허브 건설(위치 선정 불필요).
     * 플레이어/콘솔 어디서나 실행 가능. 성공 시 도착 좌표, 실패 시 null.
     */
    public static BlockPos buildHubAuto(MinecraftServer server) {
        ServerWorld nether = server.getWorld(World.NETHER);
        if (nether == null) return null;
        CoreConfig.Nether cfg = ConfigManager.core().nether;
        int cx0 = (int) cfg.borderCenterX, cz0 = (int) cfg.borderCenterZ;

        BlockPos spot = null;
        outer:
        for (int r = 0; r <= 96 && spot == null; r += 16) {
            for (int dx = -r; dx <= r; dx += 16) {
                for (int dz = -r; dz <= r; dz += 16) {
                    if (r > 0 && Math.abs(dx) != r && Math.abs(dz) != r) continue; // 링 둘레만
                    int x = cx0 + dx, z = cz0 + dz;
                    nether.getChunk(x >> 4, z >> 4); // 동기 로드
                    for (int y = 100; y > 12; y--) {
                        if (isStandable(nether, x, y, z) && clearAndDry(nether, x, y, z)) {
                            spot = new BlockPos(x, y, z);
                            break outer;
                        }
                    }
                }
            }
        }
        if (spot == null) spot = new BlockPos(cx0, 40, cz0); // 폴백: 강제 카브
        return finalizeHub(nether, cfg, spot, cfg.hubYaw);
    }

    /** 후보 위치 위로 8칸이 공기이고 주변/내부에 용암이 없는지(허브 카브 안전). */
    private static boolean clearAndDry(ServerWorld w, int x, int y, int z) {
        for (int dy = 0; dy < 8; dy++) {
            BlockState bs = w.getBlockState(new BlockPos(x, y + dy, z));
            if (!bs.isAir()) return false;
        }
        for (int dx = -2; dx <= 2; dx++)
            for (int dz = -2; dz <= 2; dz++)
                for (int dy = -1; dy < 6; dy++)
                    if (!w.getBlockState(new BlockPos(x + dx, y + dy, z + dz)).getFluidState().isEmpty()) return false;
        return true;
    }

    private static BlockPos finalizeHub(ServerWorld nether, CoreConfig.Nether cfg, BlockPos center, float yaw) {
        BlockPos arrival = NetherHubBuilder.build(nether, center);
        cfg.hubX = arrival.getX() + 0.5;
        cfg.hubY = arrival.getY();
        cfg.hubZ = arrival.getZ() + 0.5;
        cfg.hubYaw = yaw;
        ConfigManager.saveCore();
        ZenonMonCore.LOGGER.info("[Nether] 허브 건설/설정: ({}, {}, {})", arrival.getX(), arrival.getY(), arrival.getZ());
        return arrival;
    }

    /** 허브 좌표가 안전하면 그 좌표, 아니면 같은 x/z에서 안전한 지표 Y 탐색(허브 미건설 대비). */
    private static BlockPos safeHub(ServerWorld nether, CoreConfig.Nether cfg) {
        int x = (int) Math.floor(cfg.hubX);
        int z = (int) Math.floor(cfg.hubZ);
        int hy = (int) Math.floor(cfg.hubY);
        if (isStandable(nether, x, hy, z)) return new BlockPos(x, hy, z);
        // 네더 천장(123) 아래부터 하강 탐색
        for (int y = Math.min(122, Math.max(hy + 8, 100)); y > 5; y--) {
            if (isStandable(nether, x, y, z)) return new BlockPos(x, y, z);
        }
        ZenonMonCore.LOGGER.warn("[Nether] 허브 안전 좌표 탐색 실패 (x{} z{}) — 리다이렉트 생략", x, z);
        return null;
    }

    private static boolean isStandable(ServerWorld w, int x, int y, int z) {
        BlockState ground = w.getBlockState(new BlockPos(x, y - 1, z));
        BlockState feet = w.getBlockState(new BlockPos(x, y, z));
        BlockState head = w.getBlockState(new BlockPos(x, y + 1, z));
        return !ground.isAir() && ground.getFluidState().isEmpty()
                && feet.isAir() && head.isAir();
    }

    /** 허브 보호: 네더 허브 반경 내 블록 파괴 금지. op 우회 가능. true=보호(차단). */
    public static boolean isProtectedBreak(World world, ServerPlayerEntity player, BlockPos pos) {
        CoreConfig.Nether cfg = ConfigManager.core().nether;
        if (!cfg.enabled || !cfg.protectHub) return false;
        if (world.getRegistryKey() != World.NETHER) return false;
        if (cfg.opBypassProtect && player != null && player.hasPermissionLevel(2)) return false;
        int dx = Math.abs(pos.getX() - (int) Math.floor(cfg.hubX));
        int dz = Math.abs(pos.getZ() - (int) Math.floor(cfg.hubZ));
        return dx <= cfg.protectRadius && dz <= cfg.protectRadius;
    }
}
