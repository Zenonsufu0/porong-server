package kr.zenon.rpg.operations.http;

import com.google.gson.Gson;
import kr.zenon.rpg.pvp.db.PvpMatchLogRepository;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;

/**
 * GET /api/v1/pvp/* — PvP 판단 지표 (INBOX-004 #6).
 * <pre>
 *   /api/v1/pvp/balance   — 무기(클래스)별 승/패/승률 (클래스 밸런스)
 * </pre>
 * Authorization: Bearer {api-secret-key} 헤더 필수. 미설정 시 503.
 */
public final class PvpApiHandler implements HttpHandler {
    private static final Gson GSON = new Gson();

    private final PvpMatchLogRepository matchLog;
    private final String secretKey;

    public PvpApiHandler(PvpMatchLogRepository matchLog, String secretKey) {
        this.matchLog = Objects.requireNonNull(matchLog, "matchLog");
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

        String tail = exchange.getRequestURI().getPath()
                .replaceFirst("^/api/v1/pvp", "").replaceFirst("^/", "");
        try {
            if (tail.equals("balance")) {
                respond(exchange, 200, GSON.toJson(Map.of("weapon_win_rates", matchLog.weaponWinRates())));
                return;
            }
            respond(exchange, 404, "{\"error\":\"Not Found\"}");
        } catch (Exception e) {
            respond(exchange, 500, "{\"error\":\"Internal Server Error\"}");
        }
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
