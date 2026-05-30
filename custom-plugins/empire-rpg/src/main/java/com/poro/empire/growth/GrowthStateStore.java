package com.poro.empire.growth;

import com.poro.empire.growth.engine.CurrencyFlowListener;
import com.poro.empire.growth.engine.PlayerGrowthState;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class GrowthStateStore {
    private final Map<UUID, PlayerGrowthState> states = new ConcurrentHashMap<>();
    private CurrencyFlowListener flowListener; // 경제 흐름(DL-080), 생성되는 모든 상태에 주입

    /** 경제 흐름 리스너 주입 (플레이어 join 전, onEnable에서 설정). */
    public void attachFlowListener(CurrencyFlowListener flowListener) {
        this.flowListener = flowListener;
    }

    public PlayerGrowthState getOrCreate(UUID uuid, String classId) {
        return states.computeIfAbsent(uuid, key -> {
            PlayerGrowthState state = new PlayerGrowthState(key.toString(), classId);
            state.setFlowListener(flowListener);
            return state;
        });
    }

    public Optional<PlayerGrowthState> get(UUID uuid) {
        return Optional.ofNullable(states.get(uuid));
    }

    public void put(UUID uuid, PlayerGrowthState state) {
        state.setFlowListener(flowListener);
        states.put(uuid, state);
    }

    public void remove(UUID uuid) {
        states.remove(uuid);
    }
}
