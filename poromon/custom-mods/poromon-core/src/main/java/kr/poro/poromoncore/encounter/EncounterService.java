package kr.poro.poromoncore.encounter;

import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import kr.poro.poromoncore.PoroMonCore;
import kr.poro.poromoncore.config.ConfigManager;
import kr.poro.poromoncore.config.EncounterConfig;
import net.minecraft.entity.Entity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 전설 조우 (결정 018~026, 030). 풀 가중추첨 → 동적 아레나 격리 → 야생 전설 스폰(포획) → 정리.
 * 0.1: stage_weight 미적용(후보 weight 직접 추첨). 레벨=등급별 고정. 제한시간 후 자동 종료.
 */
public final class EncounterService {
    private EncounterService() {}

    private static final long DURATION_TICKS = 180L * 20L; // 3분

    private record Session(int cell, BlockPos corner, String returnDim,
                           double rx, double ry, double rz, UUID pokemonUuid, long expireTick) {}

    private static final Map<UUID, Session> ACTIVE = new ConcurrentHashMap<>();

    public static boolean isInEncounter(ServerPlayerEntity player) {
        return ACTIVE.containsKey(player.getUuid());
    }

    /** 조우권 사용 — 풀에서 추첨해 개인 아레나에 전설 소환. 성공 시 true. */
    public static boolean start(ServerPlayerEntity player, String poolId, boolean shiny) {
        if (ACTIVE.containsKey(player.getUuid())) {
            player.sendMessage(Text.literal("§e[조우] 이미 조우 중입니다."), false);
            return false;
        }
        EncounterConfig.Pool pool = ConfigManager.encounter().pools.get(poolId);
        if (pool == null) {
            player.sendMessage(Text.literal("§c[조우] 알 수 없는 풀: " + poolId), false);
            return false;
        }
        EncounterConfig.Candidate pick = weightedPick(player.getServerWorld(), pool);
        if (pick == null) {
            player.sendMessage(Text.literal("§c[조우] 소환 가능한 포켓몬이 없습니다."), false);
            return false;
        }

        MinecraftServer server = player.getServer();
        ServerWorld arena = server.getOverworld();
        int cell = ArenaManager.allocate();
        BlockPos corner = ArenaManager.corner(arena, cell);
        try {
            ArenaManager.build(arena, corner);
            BlockPos sp = ArenaManager.spawnPos(corner);

            // 복귀 좌표 저장 + 아레나로 이동
            String returnDim = player.getServerWorld().getRegistryKey().getValue().toString();
            double rx = player.getX(), ry = player.getY(), rz = player.getZ();
            player.teleport(arena, sp.getX() + 0.5, sp.getY(), sp.getZ() + 0.5, player.getYaw(), player.getPitch());

            // 야생 전설 스폰
            int level = levelFor(pool.type);
            String species = pick.species.contains(":") ? pick.species.substring(pick.species.indexOf(':') + 1) : pick.species;
            String spec = species + " level=" + level + (shiny ? " shiny=true" : "");
            PokemonProperties props = PokemonProperties.Companion.parse(spec, " ", "=");
            PokemonEntity entity = props.createEntity(arena);
            entity.refreshPositionAndAngles(sp.getX() + 3.5, sp.getY(), sp.getZ() + 0.5, 180.0f, 0.0f);
            arena.spawnEntity(entity);

            long expire = server.getTicks() + DURATION_TICKS;
            ACTIVE.put(player.getUuid(), new Session(cell, corner, returnDim, rx, ry, rz, entity.getUuid(), expire));

            player.sendMessage(Text.literal("§d[조우] 야생 " + (shiny ? "§b✦이로치 " : "") + "§d"
                    + pick.displayNameKo + " §d출현! 잡아보세요. §7(3분 제한)"), false);
            PoroMonCore.LOGGER.info("[Encounter] {} → {} ({}{} Lv{})", player.getGameProfile().getName(),
                    poolId, species, shiny ? " shiny" : "", level);
            return true;
        } catch (Throwable t) {
            ArenaManager.clear(arena, corner);
            ArenaManager.free(cell);
            PoroMonCore.LOGGER.error("[Encounter] start 실패", t);
            player.sendMessage(Text.literal("§c[조우] 소환 실패(로그 확인)."), false);
            return false;
        }
    }

    /** 매 틱(20틱): 제한시간 경과/포켓몬 사라짐(포획·도주) 시 정리·복귀. */
    public static void tick(MinecraftServer server) {
        if (ACTIVE.isEmpty()) return;
        ServerWorld arena = server.getOverworld();
        long now = server.getTicks();
        ACTIVE.entrySet().removeIf(e -> {
            UUID id = e.getKey();
            Session s = e.getValue();
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(id);
            Entity mon = arena.getEntity(s.pokemonUuid);
            boolean timedOut = now >= s.expireTick;
            boolean gone = mon == null;          // 포획/도주/디스폰
            if (!timedOut && !gone && player != null) return false; // 진행 중

            // 종료 처리
            if (mon != null) mon.discard();       // 시간초과 시 남은 개체 제거
            ArenaManager.clear(arena, s.corner);
            ArenaManager.free(s.cell);
            if (player != null) {
                Identifier dimId = Identifier.tryParse(s.returnDim);
                ServerWorld back = dimId == null ? server.getOverworld()
                        : server.getWorld(RegistryKey.of(RegistryKeys.WORLD, dimId));
                if (back == null) back = server.getOverworld();
                player.teleport(back, s.rx, s.ry, s.rz, player.getYaw(), player.getPitch());
                player.sendMessage(Text.literal(timedOut
                        ? "§7[조우] 시간이 초과되어 종료되었습니다."
                        : "§a[조우] 조우가 종료되었습니다."), false);
            }
            return true;
        });
    }

    private static EncounterConfig.Candidate weightedPick(ServerWorld world, EncounterConfig.Pool pool) {
        List<EncounterConfig.Candidate> en = new ArrayList<>();
        int total = 0;
        for (EncounterConfig.Candidate c : pool.candidates) {
            if (c.enabled && c.weight > 0) { en.add(c); total += c.weight; }
        }
        if (total <= 0) return null;
        int r = world.getRandom().nextInt(total);
        for (EncounterConfig.Candidate c : en) {
            r -= c.weight;
            if (r < 0) return c;
        }
        return en.get(en.size() - 1);
    }

    private static int levelFor(String type) {
        if (type == null) return 70;
        return switch (type) {
            case "rare" -> 50;
            case "basic", "field_event" -> 60;
            case "intermediate" -> 65;
            case "advanced" -> 70;
            case "theme" -> 75;
            case "apex" -> 80;
            default -> 70;
        };
    }
}
