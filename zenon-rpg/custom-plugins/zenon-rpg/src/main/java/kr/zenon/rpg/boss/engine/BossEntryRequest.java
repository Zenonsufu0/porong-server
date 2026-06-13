package kr.zenon.rpg.boss.engine;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

public record BossEntryRequest(
        String bossId,
        String leaderUserId,
        List<String> partyMemberIds
) {
    public BossEntryRequest {
        Objects.requireNonNull(bossId, "bossId");
        Objects.requireNonNull(leaderUserId, "leaderUserId");
        Objects.requireNonNull(partyMemberIds, "partyMemberIds");

        bossId = normalize(bossId);
        leaderUserId = normalize(leaderUserId);
        partyMemberIds = List.copyOf(partyMemberIds.stream().map(BossEntryRequest::normalize).toList());
    }

    public int partySize() {
        return partyMemberIds.size();
    }

    public Set<String> uniquePartyMemberIds() {
        return Set.copyOf(new LinkedHashSet<>(partyMemberIds));
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
