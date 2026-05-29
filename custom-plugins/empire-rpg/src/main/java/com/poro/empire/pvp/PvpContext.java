package com.poro.empire.pvp;

import java.util.UUID;

/**
 * 정규대전 가상 컨텍스트 (CANON §3).
 * <p>1차 시즌 압축: IL 60 = 5슬롯 모두 12강.
 * 실제 인벤토리 변경 없이 PvpContext를 만들어 전투 시스템에 주입.
 * 향후 PvpDamageHook 등에서 사용. 현재는 데이터 보관만.
 */
public record PvpContext(
        UUID matchId,
        UUID playerUuid,
        boolean equipmentNormalized,  // true=12강 동일화, false=현재 장비
        int    virtualEnhanceLevel,   // 동일화 시 12, 미동일화 시 0
        int    virtualIl              // 동일화 시 60 (5 × 12)
) {
    public static PvpContext ranked(UUID matchId, UUID player) {
        return new PvpContext(matchId, player, true, 12, 60);
    }
    public static PvpContext raw(UUID matchId, UUID player) {
        return new PvpContext(matchId, player, false, 0, 0);
    }
}
