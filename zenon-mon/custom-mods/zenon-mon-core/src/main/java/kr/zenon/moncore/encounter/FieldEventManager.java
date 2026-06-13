package kr.zenon.moncore.encounter;

import com.cobblemon.mod.common.api.entity.Despawner;
import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import kr.zenon.moncore.ZenonMonCore;
import kr.zenon.moncore.config.ConfigManager;
import kr.zenon.moncore.config.CoreConfig;
import kr.zenon.moncore.config.EncounterConfig;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;
import net.minecraft.world.border.WorldBorder;
import net.minecraft.world.chunk.ChunkStatus;

import java.util.UUID;

/**
 * 하급·중급 전설 필드 이벤트 (결정 019 → 038).
 * 주기적으로 field_event 풀에서 1마리를 월드보더 안 야생에 통제 스폰 → 채팅으로 종/좌표 공지 →
 * 30분 후 디스폰(또는 포획/처치 시 종료). 동시 1마리. 자연 디스폰은 no-op Despawner로 차단.
 */
public final class FieldEventManager {
    private FieldEventManager() {}

    private record Active(String displayKo, UUID entityUuid, long expireTick) {}
    private static volatile Active active = null;
    private static volatile boolean spawning = false;
    private static long idleSinceTick = -1L;   // 마지막 이벤트 종료(또는 서버 시작) 시각 = 다음 주기 기준점

    public static boolean isActive() { return active != null; }

    /** 매 틱(20틱)에서 호출: 만료/소멸 정리 + 주기 도래 시 스폰. */
    public static void tick(MinecraftServer server) {
        CoreConfig.FieldEvent cfg = ConfigManager.core().fieldEvent;
        if (!cfg.enabled) return;
        ServerWorld world = server.getOverworld();
        long now = server.getTicks();
        if (idleSinceTick < 0) idleSinceTick = now;

        if (active != null) {
            Entity e = world.getEntity(active.entityUuid());
            boolean expired = now >= active.expireTick();
            boolean gone = e == null;       // 포획/처치/제거
            if (!expired && !gone) return;
            if (e != null) e.discard();
            broadcast(server, gone
                    ? "§7[전설] 야생의 §f" + active.displayKo() + "§7 이(가) 자취를 감췄습니다."
                    : "§7[전설] 시간이 다 되어 §f" + active.displayKo() + "§7 이(가) 사라졌습니다.");
            active = null;
            idleSinceTick = now;
            return;
        }
        // 동적 주기: idle 기준점 + interval(부스트 시 절반) — 토글이 즉시 반영됨
        if (spawning || now < idleSinceTick + intervalTicks(cfg)) return;
        spawnEvent(server, cfg);
    }

    /** 운영자 강제 발생(테스트). 이미 진행/스폰 중이면 false. */
    public static boolean spawnNow(MinecraftServer server) {
        if (active != null || spawning) return false;
        spawnEvent(server, ConfigManager.core().fieldEvent);
        return true;
    }

    /** 주기(틱). 운영자 "주기 2배 단축" 부스트 활성 시 절반. */
    private static long intervalTicks(CoreConfig.FieldEvent cfg) {
        long base = Math.max(1, cfg.intervalMinutes) * 1200L; // 분 × 1200틱
        return kr.zenon.moncore.event.EventManager.isFieldEventFast() ? Math.max(1200L, base / 2) : base;
    }

    private static void spawnEvent(MinecraftServer server, CoreConfig.FieldEvent cfg) {
        EncounterConfig.Pool pool = ConfigManager.encounter().pools.get(cfg.poolId);
        if (pool == null) {
            ZenonMonCore.LOGGER.warn("[FieldEvent] 풀 없음: {}", cfg.poolId);
            return;
        }
        EncounterConfig.Candidate pick = EncounterConfig.weightedPick(pool,
                server.getOverworld().getRandom().nextDouble());
        if (pick == null) return;
        spawning = true;
        findAndSpawn(server, cfg, pick, 0);
    }

    /** 비동기 청크 로드로 경계 안 안전 지표를 찾고, 찾으면 스폰(멈춤 없음, 재귀). */
    private static void findAndSpawn(MinecraftServer server, CoreConfig.FieldEvent cfg,
                                     EncounterConfig.Candidate pick, int attempt) {
        ServerWorld world = server.getOverworld();
        if (attempt >= cfg.maxAttempts) {
            spawning = false;
            ZenonMonCore.LOGGER.warn("[FieldEvent] 안전 위치 탐색 실패 — 다음 주기 재시도");
            return;
        }
        WorldBorder wb = world.getWorldBorder();
        int range = (int) (wb.getSize() / 2.0) - cfg.edgeMargin;
        if (range < 16) range = 16;
        Random rand = world.getRandom();
        int x = (int) wb.getCenterX() + rand.nextBetween(-range, range);
        int z = (int) wb.getCenterZ() + rand.nextBetween(-range, range);
        if (!wb.contains(x, z)) { findAndSpawn(server, cfg, pick, attempt + 1); return; }

        ServerChunkManager scm = world.getChunkManager();
        scm.getChunkFutureSyncOnMainThread(x >> 4, z >> 4, ChunkStatus.FULL, true).thenAccept(opt -> {
            if (!opt.isPresent()) { findAndSpawn(server, cfg, pick, attempt + 1); return; }
            int topY = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z);
            if (topY <= cfg.minSurfaceY) { findAndSpawn(server, cfg, pick, attempt + 1); return; }
            BlockState below = world.getBlockState(new BlockPos(x, topY - 1, z));
            if (!below.getFluidState().isEmpty() || below.isAir()) { findAndSpawn(server, cfg, pick, attempt + 1); return; }

            try {
                String species = pick.species.contains(":")
                        ? pick.species.substring(pick.species.indexOf(':') + 1) : pick.species;
                PokemonProperties props = PokemonProperties.Companion.parse(
                        species + " level=" + cfg.level, " ", "=");
                PokemonEntity entity = props.createEntity(world);
                entity.refreshPositionAndAngles(x + 0.5, topY, z + 0.5, 0.0f, 0.0f);
                // 자연 디스폰 차단(30분은 우리가 관리)
                entity.setDespawner(NO_DESPAWN);
                world.spawnEntity(entity);

                long expire = server.getTicks() + Math.max(1, cfg.durationMinutes) * 1200L;
                active = new Active(pick.displayNameKo, entity.getUuid(), expire);
                broadcast(server, "§6§l[전설 출현] §r§e야생에 §f" + pick.displayNameKo + " §e출현! "
                        + "§f좌표 (" + x + ", " + topY + ", " + z + ") §7— " + cfg.durationMinutes + "분 후 사라집니다!");
                ZenonMonCore.LOGGER.info("[FieldEvent] {} Lv{} @ ({}, {}, {})", species, cfg.level, x, topY, z);
            } catch (Throwable t) {
                ZenonMonCore.LOGGER.error("[FieldEvent] 스폰 실패", t);
            } finally {
                spawning = false;
            }
        });
    }

    private static void broadcast(MinecraftServer server, String msg) {
        server.getPlayerManager().broadcast(Text.literal(msg), false);
    }

    /** 자연 디스폰을 막는 no-op Despawner. */
    private static final Despawner<PokemonEntity> NO_DESPAWN = new Despawner<>() {
        @Override public void beginTracking(PokemonEntity entity) { }
        @Override public boolean shouldDespawn(PokemonEntity entity) { return false; }
    };
}
