package com.poro.rpg.field;

import org.bukkit.Location;

// 1차 시즌 placeholder — WorldGuard 어댑터 연결 전까지 안전구역 없음으로 동작.
// WorldGuard 사용 시 이 구현체를 WorldGuardSafeZoneService로 교체한다.
public final class NoopSafeZoneService implements SafeZoneService {
    @Override
    public boolean isSafeZone(Location location) {
        return false;
    }
}
