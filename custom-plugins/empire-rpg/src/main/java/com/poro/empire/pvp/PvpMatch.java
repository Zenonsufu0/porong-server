package com.poro.empire.pvp;

import org.bukkit.Location;

import java.util.UUID;

public record PvpMatch(
        UUID matchId,
        PvpMatchType type,
        UUID playerA, String nameA, Location returnA,
        UUID playerB, String nameB, Location returnB,
        int  arenaSlotId,
        long startedAt
) {
    public boolean involves(UUID uuid) {
        return uuid.equals(playerA) || uuid.equals(playerB);
    }
    public UUID opponentOf(UUID uuid) {
        if (uuid.equals(playerA)) return playerB;
        if (uuid.equals(playerB)) return playerA;
        return null;
    }
}
