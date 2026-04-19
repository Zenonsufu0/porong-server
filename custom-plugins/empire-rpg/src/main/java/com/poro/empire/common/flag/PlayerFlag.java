package com.poro.empire.common.flag;

import java.util.Objects;
import java.util.UUID;

/**
 * {@code player_flag} 테이블 한 행에 대응하는 읽기 전용 레코드.
 *
 * @param playerUuid     플레이어 UUID
 * @param flagKey        플래그 키 (도메인.카테고리.키)
 * @param value          플래그 값 (BOOL/LONG/STRING)
 * @param updatedAtMillis 마지막 갱신 시각 (epoch millis)
 * @param version        낙관적 락 예약 슬롯(v0.2에서 활용)
 */
public record PlayerFlag(
        UUID playerUuid,
        FlagKey flagKey,
        FlagValue value,
        long updatedAtMillis,
        long version
) {
    public PlayerFlag {
        Objects.requireNonNull(playerUuid, "playerUuid");
        Objects.requireNonNull(flagKey, "flagKey");
        Objects.requireNonNull(value, "value");
    }
}
