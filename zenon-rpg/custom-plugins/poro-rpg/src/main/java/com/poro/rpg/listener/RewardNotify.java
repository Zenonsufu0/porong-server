package com.poro.rpg.listener;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 처치/획득 알림(경험치·재화) ON/OFF 플레이어 설정 (in-memory, 기본 ON).
 * UI 선호값이라 재시작 시 초기화(전원 ON)된다.
 */
public final class RewardNotify {
    private static final Set<UUID> disabled = ConcurrentHashMap.newKeySet();

    private RewardNotify() {}

    public static boolean isEnabled(UUID uuid) { return !disabled.contains(uuid); }

    /** 토글 후 새 상태 반환(true=ON). */
    public static boolean toggle(UUID uuid) {
        if (disabled.remove(uuid)) return true;
        disabled.add(uuid);
        return false;
    }
}
