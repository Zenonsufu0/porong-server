package kr.zenon.rpg.operations.http;

import com.google.gson.Gson;
import kr.zenon.rpg.boss.db.BossSessionRepository;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
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
 * Authorization: Bearer {api-secret-key} 헤더 필수.
 * api-secret-key 미설정 시 모든 요청 503 차단.
 */
public final class BossApiHandler implements HttpHandler {
    private static final Gson GSON = new Gson();

    private final BossSessionRepository repository;
    private final String secretKey;

    public BossApiHandler(BossSessionRepository repository, String secretKey) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.secretKey = secretKey == null ? "" : secretKey;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (secretKey.isBlank()) {
            respond(exchange, 503, "{\"error\":\"API key not configured. Set common.api-secret-key in config.yml.\"}");
            return;
        }

        String auth = exchange.getRequestHeaders().getFirst("Authorization");
        if (auth == null || !auth.equals("Bearer " + secretKey)) {
            respond(exchange, 401, "{\"error\":\"Unauthorized\"}");
            return;
        }

        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            respond(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
            return;
        }

        String path = exchange.getRequestURI().getPath();
        String tail = path.replaceFirst("^/api/v1/boss", "");

        try {
            if (tail.isEmpty() || tail.equals("/") || tail.equals("/stats")) {
                respond(exchange, 200, GSON.toJson(repository.queryAllStats()));
                return;
            }

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
