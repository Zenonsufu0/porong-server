package com.poro.rpg.operations.query.discord;

import java.util.List;

public record DiscordCardResponse(
        String command,
        String title,
        List<String> lines,
        String footer
) {
}
