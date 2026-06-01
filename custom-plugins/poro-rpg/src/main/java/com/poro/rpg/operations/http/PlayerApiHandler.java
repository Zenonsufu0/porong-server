package com.poro.rpg.operations.http;

import com.google.gson.Gson;
import com.poro.rpg.operations.query.discord.DiscordCardResponse;
import com.poro.rpg.operations.query.discord.DiscordResponseBuilder;
import com.poro.rpg.operations.query.service.PlayerDetailQueryService;
import com.poro.rpg.operations.query.service.PublicSnapshotQueryService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * 닉네임 기반 플레이어 조회 3개 엔드포인트.
 * <pre>
 *   GET /player/by-nick/{nick}       — 프로필
 *   GET /island/by-nick/{nick}       — 영지
 *   GET /boss-history/by-nick/{nick} — 보스 기록
 * </pre>
 * X-Api-Key 헤더 필수.
 */
public final class PlayerApiHandler implements HttpHandler {
    private static final Gson GSON = new Gson();

    private final PublicSnapshotQueryService publicSnapshotQueryService;
    private final String secretKey;

    public PlayerApiHandler(PublicSnapshotQueryService publicSnapshotQueryService, String secretKey) {
        this.publicSnapshotQueryService = Objects.requireNonNull(publicSnapshotQueryService, "publicSnapshotQueryService");
        this.secretKey = secretKey == null ? "" : secretKey;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (secretKey.isBlank()) {
            respond(exchange, 503, "{\"error\":\"API key not configured. Set common.api-secret-key in config.yml.\"}");
            return;
        }

        String apiKey = exchange.getRequestHeaders().getFirst("X-Api-Key");
        if (!secretKey.equals(apiKey)) {
            respond(exchange, 401, "{\"error\":\"Unauthorized\"}");
            return;
        }

        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            respond(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
            return;
        }

        String path = exchange.getRequestURI().getPath();
        try {
            DiscordCardResponse card = resolve(path);
            if (card == null) {
                respond(exchange, 404, "{\"error\":\"Not Found\"}");
            } else {
                respond(exchange, 200, GSON.toJson(card));
            }
        } catch (PlayerNotFoundException e) {
            respond(exchange, 404, "{\"error\":\"player not found: " + e.getMessage() + "\"}");
        } catch (Exception e) {
            respond(exchange, 500, "{\"error\":\"Internal Server Error\"}");
        }
    }

    private DiscordCardResponse resolve(String path) throws PlayerNotFoundException {
        if (path.startsWith("/player/by-nick/")) {
            String nick = path.substring("/player/by-nick/".length());
            Optional<PlayerDetailQueryService.PlayerSnapshotProjection> snap =
                    publicSnapshotQueryService.playerSnapshotByNick(nick);
            if (snap.isEmpty()) throw new PlayerNotFoundException(nick);
            return DiscordResponseBuilder.playerInfo("/프로필", snap.get());
        }
        if (path.startsWith("/island/by-nick/")) {
            String nick = path.substring("/island/by-nick/".length());
            Optional<PlayerDetailQueryService.LifeProjection> life =
                    publicSnapshotQueryService.lifeSnapshotByNick(nick);
            if (life.isEmpty()) throw new PlayerNotFoundException(nick);
            return DiscordResponseBuilder.life("/영지", life.get());
        }
        if (path.startsWith("/boss-history/by-nick/")) {
            String nick = path.substring("/boss-history/by-nick/".length());
            Optional<List<PlayerDetailQueryService.PlayerBossRecordProjection>> records =
                    publicSnapshotQueryService.bossRecordsSnapshotByNick(nick);
            if (records.isEmpty()) throw new PlayerNotFoundException(nick);
            return DiscordResponseBuilder.bossRecords("/보스", records.get());
        }
        return null;
    }

    private static void respond(HttpExchange exchange, int status, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static final class PlayerNotFoundException extends Exception {
        PlayerNotFoundException(String nick) {
            super(nick);
        }
    }
}
