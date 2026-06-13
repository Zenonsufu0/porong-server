package kr.zenon.rpg.operations.query.discord;

import kr.zenon.rpg.operations.query.model.QueryTimeRange;
import kr.zenon.rpg.operations.query.service.PublicSnapshotQueryService;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class DiscordCommandQueryAdapter {
    private final PublicSnapshotQueryService publicSnapshotQueryService;

    public DiscordCommandQueryAdapter(PublicSnapshotQueryService publicSnapshotQueryService) {
        this.publicSnapshotQueryService = Objects.requireNonNull(publicSnapshotQueryService, "publicSnapshotQueryService");
    }

    public DiscordCardResponse handleCommand(String command, String userId, String argument, QueryTimeRange range) {
        String normalized = normalize(command);
        return switch (normalized) {
            case "/내정보" -> DiscordResponseBuilder.playerInfo(normalized, publicSnapshotQueryService.playerSnapshot(userId));
            case "/내장비" -> DiscordResponseBuilder.equipment(normalized, publicSnapshotQueryService.equipmentSnapshot(userId));
            case "/보스기록" -> DiscordResponseBuilder.bossRecords(normalized, publicSnapshotQueryService.bossRecordsSnapshot(userId));
            case "/시세" -> DiscordResponseBuilder.marketPrice(
                    normalized,
                    publicSnapshotQueryService.marketPriceSnapshot(argument == null ? "" : argument, range)
            );
            case "/내영지" -> DiscordResponseBuilder.life(normalized, publicSnapshotQueryService.lifeSnapshot(userId));
            case "/명예의전당" -> DiscordResponseBuilder.hallOfFame(normalized, publicSnapshotQueryService.hallOfFameSnapshot(range));
            default -> new DiscordCardResponse(
                    normalized,
                    "지원되지 않는 명령",
                    List.of("지원 명령: /내정보 /내장비 /보스기록 /시세 /내영지 /명예의전당"),
                    "zenon discord adapter"
            );
        };
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
