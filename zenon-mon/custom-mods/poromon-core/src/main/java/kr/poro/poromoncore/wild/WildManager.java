package kr.poro.poromoncore.wild;

import kr.poro.poromoncore.PoroMonCore;
import kr.poro.poromoncore.config.ConfigManager;
import kr.poro.poromoncore.config.CoreConfig;
import net.minecraft.block.BlockState;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;
import net.minecraft.world.border.WorldBorder;
import net.minecraft.world.chunk.ChunkStatus;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 야생 랜덤 이동 (결정 030). 월드보더 안 임의 좌표로 안전 착지 텔레포트.
 * 쿨다운 + 웜업(채널링: 이동/피격 취소, 매 초 카운트다운). 웜업 종료 후 후보 좌표의 청크를
 * **비동기 로드**(getChunkFutureSyncOnMainThread)해 메인 스레드 멈춤 없이 표면을 찾는다.
 */
public final class WildManager {
    private WildManager() {}

    private record Warmup(long startTick, double x, double y, double z, float health) {}

    private static final Map<UUID, Long> COOLDOWN = new ConcurrentHashMap<>();
    private static final Map<UUID, Warmup> PENDING = new ConcurrentHashMap<>();
    private static final Set<UUID> SEARCHING = ConcurrentHashMap.newKeySet();

    public static void requestTeleport(ServerPlayerEntity player) {
        CoreConfig.Wild cfg = ConfigManager.core().wild;
        if (!cfg.enabled) {
            player.sendMessage(Text.literal("§c[야생] 비활성화되어 있습니다."), true);
            return;
        }
        UUID id = player.getUuid();
        if (PENDING.containsKey(id) || SEARCHING.contains(id)) return; // 이미 진행 중
        long now = player.getServer().getTicks();
        Long ready = COOLDOWN.get(id);
        if (ready != null && now < ready) {
            long sec = (ready - now + 19) / 20;
            player.sendMessage(Text.literal("§e[야생] 쿨다운 중입니다 (" + sec + "초 남음)."), true);
            return;
        }
        if (cfg.warmupSeconds <= 0) {
            beginSearch(player);
            return;
        }
        PENDING.put(id, new Warmup(now, player.getX(), player.getY(), player.getZ(), player.getHealth()));
        // 카운트다운은 tickWarmups가 매 초 액션바로 갱신
    }

    /** 매 틱 호출(PoroMonCore): 웜업 진행/취소/완료. */
    public static void tickWarmups(MinecraftServer server) {
        if (PENDING.isEmpty()) return;
        CoreConfig.Wild cfg = ConfigManager.core().wild;
        long now = server.getTicks();
        Iterator<Map.Entry<UUID, Warmup>> it = PENDING.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Warmup> e = it.next();
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(e.getKey());
            if (player == null) { it.remove(); continue; }
            Warmup w = e.getValue();
            if (cfg.cancelOnMove && movedTooFar(player, w)) {
                it.remove();
                player.sendMessage(Text.literal("§c[야생] 이동하여 취소되었습니다."), true);
                continue;
            }
            if (cfg.cancelOnDamage && player.getHealth() < w.health) {
                it.remove();
                player.sendMessage(Text.literal("§c[야생] 피격으로 취소되었습니다."), true);
                continue;
            }
            long elapsed = now - w.startTick;
            if (elapsed >= (long) cfg.warmupSeconds * 20L) {
                it.remove();
                beginSearch(player);
                continue;
            }
            if (elapsed % 20L == 0L) {
                long remain = cfg.warmupSeconds - elapsed / 20L;
                if (remain > 0) {
                    player.sendMessage(Text.literal("§2[야생] " + remain + "초 후 이동... §7(움직이면 취소)"), true);
                }
            }
        }
    }

    private static boolean movedTooFar(ServerPlayerEntity player, Warmup w) {
        double dx = player.getX() - w.x, dy = player.getY() - w.y, dz = player.getZ() - w.z;
        return dx * dx + dy * dy + dz * dz > 0.10;
    }

    private static void beginSearch(ServerPlayerEntity player) {
        if (!SEARCHING.add(player.getUuid())) return;
        player.sendMessage(Text.literal("§2[야생] 안전한 위치를 찾는 중..."), true);
        tryCandidate(player.getServer(), player.getUuid(), 0);
    }

    /** 후보 좌표 1개를 비동기 청크 로드 후 평가 → 부적합하면 다음 후보(재귀, 멈춤 없음). */
    private static void tryCandidate(MinecraftServer server, UUID id, int attempt) {
        CoreConfig.Wild cfg = ConfigManager.core().wild;
        ServerPlayerEntity player = server.getPlayerManager().getPlayer(id);
        if (player == null) { SEARCHING.remove(id); return; }
        if (attempt >= cfg.maxAttempts) {
            SEARCHING.remove(id);
            player.sendMessage(Text.literal("§c[야생] 안전한 위치를 찾지 못했습니다. 다시 시도해 주세요."), true);
            return;
        }
        ServerWorld world = player.getServerWorld();
        WorldBorder wb = world.getWorldBorder();
        int range = (int) (wb.getSize() / 2.0) - cfg.edgeMargin;
        if (range < 16) range = 16;
        Random rand = world.getRandom();
        int x = (int) wb.getCenterX() + rand.nextBetween(-range, range);
        int z = (int) wb.getCenterZ() + rand.nextBetween(-range, range);
        if (!wb.contains(x, z)) { tryCandidate(server, id, attempt + 1); return; }

        ServerChunkManager scm = world.getChunkManager();
        scm.getChunkFutureSyncOnMainThread(x >> 4, z >> 4, ChunkStatus.FULL, true).thenAccept(opt -> {
            // SyncOnMainThread → 콜백은 서버 스레드에서 실행
            ServerPlayerEntity p = server.getPlayerManager().getPlayer(id);
            if (p == null) { SEARCHING.remove(id); return; }
            if (!opt.isPresent()) { tryCandidate(server, id, attempt + 1); return; }

            int topY = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z);
            if (topY <= cfg.minSurfaceY) { tryCandidate(server, id, attempt + 1); return; }
            BlockState bs = world.getBlockState(new BlockPos(x, topY - 1, z));
            if (!bs.getFluidState().isEmpty() || bs.isAir()) { tryCandidate(server, id, attempt + 1); return; }

            p.teleport(world, x + 0.5, topY, z + 0.5, p.getYaw(), p.getPitch());
            COOLDOWN.put(id, (long) server.getTicks() + (long) cfg.cooldownSeconds * 20L);
            SEARCHING.remove(id);
            p.sendMessage(Text.literal("§a[야생] 야생으로 이동했습니다. §7(" + x + ", " + topY + ", " + z + ")"), true);
            if (ConfigManager.core().logging.auditEnabled) {
                PoroMonCore.LOGGER.info("[Wild] {} → ({}, {}, {})", p.getGameProfile().getName(), x, topY, z);
            }
        });
    }
}
