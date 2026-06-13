package kr.zenon.rpg.combat;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ResourceTracker {
    private final Map<UUID, Integer> stacks = new ConcurrentHashMap<>();

    // 낫 전용: 월영회전 사용 타임스탬프 (2초 윈도우)
    private final Map<UUID, Long> shadowSpinTimestamp = new ConcurrentHashMap<>();
    private static final long SHADOW_SPIN_WINDOW_MS = 2000L;

    /** 월영회전 사용 시 호출. 2초 윈도우를 (재)시작한다. */
    public void recordShadowSpin(UUID uuid) {
        shadowSpinTimestamp.put(uuid, System.currentTimeMillis());
    }

    /**
     * 윈도우가 활성 상태이면 소모하고 true 반환. 만료 또는 미기록이면 false.
     * 한 번 소모되면 다음 사신베기에서 재발동 불가.
     */
    public boolean consumeShadowSpinWindow(UUID uuid) {
        Long ts = shadowSpinTimestamp.remove(uuid);
        return ts != null && System.currentTimeMillis() - ts <= SHADOW_SPIN_WINDOW_MS;
    }

    public int getStack(UUID uuid) {
        return stacks.getOrDefault(uuid, 0);
    }

    public void setStack(UUID uuid, int value) {
        if (value <= 0) {
            stacks.remove(uuid);
            return;
        }
        stacks.put(uuid, value);
    }

    public int incrementStack(UUID uuid, int max) {
        int next = Math.min(Math.max(1, max), getStack(uuid) + 1);
        stacks.put(uuid, next);
        return next;
    }

    public void resetStack(UUID uuid) {
        stacks.remove(uuid);
    }

    /** 플레이어 퇴장 시 모든 상태를 제거한다. */
    public void cleanup(UUID uuid) {
        stacks.remove(uuid);
        shadowSpinTimestamp.remove(uuid);
    }
}
