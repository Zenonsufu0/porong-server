package kr.zenon.rpg.operations.http;

import com.google.gson.Gson;
import kr.zenon.rpg.persistence.GrowthSnapshotRepository;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;

/**
 * GET /api/v1/growth/* — 성장 곡선 판단 지표 (INBOX-004 #7).
 * <pre>
 *   /api/v1/growth/curve?days=N   — 최근 N일 일별 평균 레벨·IL·골드 (기본 45, 시즌 길이)
 * </pre>
 * Authorization: Bearer {api-secret-key} 헤더 필수. 미설정 시 503.
 */
public final class GrowthApiHandler implements HttpHandler {
    private static final Gson GSON = new Gson();
    private static final int DEFAULT_DAYS = 45;
    private static final int MAX_DAYS = 180;

    private final GrowthSnapshotRepository repository;
    private final String secretKey;

    public GrowthApiHandler(GrowthSnapshotRepository repository, String secretKey) {
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

        URI uri = exchange.getRequestURI();
        String tail = uri.getPath().replaceFirst("^/api/v1/growth", "").replaceFirst("^/", "");
        try {
            if (tail.equals("curve")) {
                int days = parseDays(uri.getQuery());
                respond(exchange, 200, GSON.toJson(Map.of("days", days, "curve", repository.dailyAverages(days))));
                return;
            }
            respond(exchange, 404, "{\"error\":\"Not Found\"}");
        } catch (Exception e) {
            respond(exchange, 500, "{\"error\":\"Internal Server Error\"}");
        }
    }

    private int parseDays(String query) {
        if (query == null) return DEFAULT_DAYS;
        for (String part : query.split("&")) {
            if (part.startsWith("days=")) {
                try {
                    int d = Integer.parseInt(part.substring("days=".length()));
                    return Math.max(1, Math.min(d, MAX_DAYS));
                } catch (NumberFormatException ignored) {
                    return DEFAULT_DAYS;
                }
            }
        }
        return DEFAULT_DAYS;
    }

    private void respond(HttpExchange exchange, int code, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
