package kr.poro.poromoncore.data;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 월드 부속 PersistentState (database_schema.md §1: 0.1 = PersistentState 채택).
 * 오버월드 DimensionDataStorage 의 "poromoncore_progress" 키. 월드 백업/롤백과 자동 정합.
 */
public class PoroMonState extends PersistentState {
    private static final String KEY = "poromoncore_progress";

    private final Map<UUID, PlayerProgress> players = new HashMap<>();

    public static final PersistentState.Type<PoroMonState> TYPE = new PersistentState.Type<>(
            PoroMonState::new,
            PoroMonState::readNbt,
            null
    );

    /** 서버 오버월드의 영속 상태를 가져온다(없으면 생성). */
    public static PoroMonState get(MinecraftServer server) {
        ServerWorld overworld = server.getOverworld();
        PersistentStateManager psm = overworld.getPersistentStateManager();
        return psm.getOrCreate(TYPE, KEY);
    }

    /** 진행 엔트리 조회(없으면 생성 + dirty). */
    public PlayerProgress getOrCreate(UUID uuid) {
        PlayerProgress existing = players.get(uuid);
        if (existing != null) return existing;
        PlayerProgress fresh = new PlayerProgress();
        players.put(uuid, fresh);
        markDirty();
        return fresh;
    }

    /** 조회만(없으면 null). */
    public PlayerProgress peek(UUID uuid) {
        return players.get(uuid);
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        NbtCompound playersNbt = new NbtCompound();
        for (Map.Entry<UUID, PlayerProgress> e : players.entrySet()) {
            playersNbt.put(e.getKey().toString(), e.getValue().writeNbt(new NbtCompound()));
        }
        nbt.put("players", playersNbt);
        return nbt;
    }

    public static PoroMonState readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        PoroMonState state = new PoroMonState();
        if (nbt.contains("players", NbtElement.COMPOUND_TYPE)) {
            NbtCompound playersNbt = nbt.getCompound("players");
            for (String key : playersNbt.getKeys()) {
                try {
                    state.players.put(UUID.fromString(key), PlayerProgress.readNbt(playersNbt.getCompound(key)));
                } catch (IllegalArgumentException ignored) {
                    // 잘못된 UUID 키는 건너뜀
                }
            }
        }
        return state;
    }
}
