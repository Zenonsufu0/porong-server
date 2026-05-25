package com.poro.empire.operations.http;

import com.google.gson.Gson;
import com.poro.empire.boss.db.BossSessionRepository;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * GET /api/v1/boss/* 4개 엔드포인트 처리.
 * <pre>
 *   /api/v1/boss/stats                   — 전 보스 요약
 *   /api/v1/boss/{boss_id}/stats         — 단일 보스 상세
 *   /api/v1/boss/{boss_id}/weekly        — 주차별 클리어율
 *   /api/v1/boss/{boss_id}/party-spec    — 클리어 파티 스펙 분포
 * </pre>
 */
public final class BossApiHandler implements HttpHandler {
    private static final Gson GSON = new Gson();

    private final BossSessionRepository repository;

    public BossApiHandler(BossSessionRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            respond(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
            return;
        }

        String path = exchange.getRequestURI().getPath();
        // strip leading /api/v1/boss — remaining: "" | "/{bossId}/stats" | "/{bossId}/weekly" | "/{bossId}/party-spec"
        String tail = path.replaceFirst("^/api/v1/boss", "");

        try {
            if (tail.isEmpty() || tail.equals("/")) {
                // GET /api/v1/boss/stats is also routed here via context /api/v1/boss
                respond(exchange, 200, GSON.toJson(repository.queryAllStats()));
                return;
            }

            if (tail.equals("/stats")) {
                respond(exchange, 200, GSON.toJson(repository.queryAllStats()));
                return;
            }

            // tail pattern: /{bossId}/{action}
            String[] parts = tail.replaceFirst("^/", "").split("/", 2);
            if (parts.length < 2) {
                respond(exchange, 404, "{\"error\":\"Not Found\"}");
                return;
            }
            String bossId = parts[0];
            String action = parts[1];

            Object body = switch (action) {
                case "stats"      -> repository.queryStatsByBossId(bossId);
                case "weekly"     -> Map.of("boss_id", bossId, "weekly", repository.queryWeekly(bossId));
                case "party-spec" -> Map.of("boss_id", bossId, "party_spec", repository.queryPartySpec(bossId));
                default           -> null;
            };

            if (body == null) {
                respond(exchange, 404, "{\"error\":\"Not Found\"}");
            } else {
                respond(exchange, 200, GSON.toJson(body));
            }
        } catch (Exception e) {
            respond(exchange, 500, "{\"error\":\"Internal Server Error\"}");
        }
    }

    private static void respond(HttpExchange exchange, int status, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
