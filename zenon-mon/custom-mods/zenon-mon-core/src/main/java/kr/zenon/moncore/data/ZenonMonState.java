package kr.zenon.moncore.data;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
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
 * 오버월드 DimensionDataStorage 의 "zenonmoncore_progress" 키. 월드 백업/롤백과 자동 정합.
 */
public class ZenonMonState extends PersistentState {
    private static final String KEY = "zenonmoncore_progress";

    private final Map<UUID, PlayerProgress> players = new HashMap<>();

    // 전역 이벤트 부스트 (운영자 GUI 토글, 영속) — 결정/IB-002
    public boolean xpBoost = false;
    public boolean goldBoost = false;
    public boolean apexBoost = false;   // 컨셉 조우 최상급 가중 ×2 (결정 035, 이벤트)
    public boolean fieldEventFast = false; // 전설 필드 이벤트 주기 2배 단축 (결정 038, 이벤트)

    // 챔피언스리그 역대 챔피언 (결정 040, 챔피언 홀): "이름" 순서대로
    public final java.util.List<String> championHistory = new java.util.ArrayList<>();

    // 경제 텔레메트리 (economy_design §6): 출처 그룹별 골드 유입/유출 누적
    public final Map<String, Long> goldIn = new java.util.LinkedHashMap<>();
    public final Map<String, Long> goldOut = new java.util.LinkedHashMap<>();

    public static final PersistentState.Type<ZenonMonState> TYPE = new PersistentState.Type<>(
            ZenonMonState::new,
            ZenonMonState::readNbt,
            null
    );

    /** 서버 오버월드의 영속 상태를 가져온다(없으면 생성). */
    public static ZenonMonState get(MinecraftServer server) {
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

    /** 전체 플레이어 진행(읽기용 — 순위표 등). */
    public java.util.Map<UUID, PlayerProgress> all() {
        return java.util.Collections.unmodifiableMap(players);
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        NbtCompound playersNbt = new NbtCompound();
        for (Map.Entry<UUID, PlayerProgress> e : players.entrySet()) {
            playersNbt.put(e.getKey().toString(), e.getValue().writeNbt(new NbtCompound()));
        }
        nbt.put("players", playersNbt);
        nbt.putBoolean("xpBoost", xpBoost);
        nbt.putBoolean("goldBoost", goldBoost);
        nbt.putBoolean("apexBoost", apexBoost);
        nbt.putBoolean("fieldEventFast", fieldEventFast);
        nbt.put("goldIn", longMapToNbt(goldIn));
        nbt.put("goldOut", longMapToNbt(goldOut));
        NbtList champs = new NbtList();
        for (String c : championHistory) champs.add(net.minecraft.nbt.NbtString.of(c));
        nbt.put("championHistory", champs);
        return nbt;
    }

    private static NbtCompound longMapToNbt(Map<String, Long> map) {
        NbtCompound c = new NbtCompound();
        for (Map.Entry<String, Long> e : map.entrySet()) c.putLong(e.getKey(), e.getValue());
        return c;
    }

    private static void nbtToLongMap(NbtCompound c, Map<String, Long> map) {
        for (String k : c.getKeys()) map.put(k, c.getLong(k));
    }

    public static ZenonMonState readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        ZenonMonState state = new ZenonMonState();
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
        state.xpBoost = nbt.getBoolean("xpBoost");
        state.goldBoost = nbt.getBoolean("goldBoost");
        state.apexBoost = nbt.getBoolean("apexBoost");
        state.fieldEventFast = nbt.getBoolean("fieldEventFast");
        if (nbt.contains("goldIn", NbtElement.COMPOUND_TYPE)) nbtToLongMap(nbt.getCompound("goldIn"), state.goldIn);
        if (nbt.contains("goldOut", NbtElement.COMPOUND_TYPE)) nbtToLongMap(nbt.getCompound("goldOut"), state.goldOut);
        if (nbt.contains("championHistory", NbtElement.LIST_TYPE)) {
            NbtList champs = nbt.getList("championHistory", NbtElement.STRING_TYPE);
            for (int i = 0; i < champs.size(); i++) state.championHistory.add(champs.getString(i));
        }
        return state;
    }
}
