package com.poro.rpg.operations.http;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.poro.rpg.auth.AuthRepository;
import com.poro.rpg.auth.AuthService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@code POST /auth/verify} — 봇이 인게임 발급 코드를 검증 (DL-132).
 *
 * <pre>
 *   요청: 헤더 X-Api-Key: &lt;키&gt; · 바디 {"code":"...","discordId":"..."}
 *   응답: 200 {"ok":true,"uuid":"...","name":"..."}
 *         404 {"ok":false,"error":"..."}   — 코드 없음/만료
 *         401 {"error":"Unauthorized"}     — 키 불일치
 *         429 {"error":"rate limited"}     — verify rate-limit 초과
 * </pre>
 *
 * <p>코드 보안(DL-132): 짧은 TTL·1회용·충분한 엔트로피는 {@link AuthService}가, verify rate-limit은
 * 본 핸들러가 담당(discord_id 단위 슬라이딩 윈도우).
 */
public final class AuthApiHandler implements HttpHandler {
    private static final Gson GSON = new Gson();

    /** discord_id당 윈도우(ms) 내 최대 검증 시도. */
    private static final long RATE_WINDOW_MS = 60_000L;
    private static final int RATE_MAX_ATTEMPTS = 10;

    private final AuthService authService;
    private final String secretKey;
    private final ConcurrentHashMap<String, Deque<Long>> attempts = new ConcurrentHashMap<>();

    public AuthApiHandler(AuthService authService, String secretKey) {
        this.authService = Objects.requireNonNull(authService, "authService");
        this.secretKey   = secretKey == null ? "" : secretKey;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            if (secretKey.isBlank()) {
                respond(exchange, 503, "{\"error\":\"API key not configured. Set common.api-secret-key in config.yml.\"}");
                return;
            }
            String apiKey = exchange.getRequestHeaders().getFirst("X-Api-Key");
            if (!secretKey.equals(apiKey)) {
                respond(exchange, 401, "{\"error\":\"Unauthorized\"}");
                return;
            }
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                respond(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
                return;
            }

            String body = readBody(exchange);
            String code;
            String discordId;
            try {
                JsonObject json = JsonParser.parseString(body).getAsJsonObject();
                code      = json.has("code")      && !json.get("code").isJsonNull()      ? json.get("code").getAsString()      : null;
                discordId = json.has("discordId") && !json.get("discordId").isJsonNull() ? json.get("discordId").getAsString() : null;
            } catch (Exception e) {
                respond(exchange, 400, "{\"error\":\"Bad Request: invalid JSON\"}");
                return;
            }
            if (code == null || code.isBlank() || discordId == null || discordId.isBlank()) {
                respond(exchange, 400, "{\"error\":\"Bad Request: code and discordId required\"}");
                return;
            }

            if (!allow(discordId)) {
                respond(exchange, 429, "{\"error\":\"rate limited\"}");
                return;
            }

            Optional<AuthRepository.LinkedIdentity> linked = authService.verify(code, discordId);
            if (linked.isEmpty()) {
                respond(exchange, 404, "{\"ok\":false,\"error\":\"code not found or expired\"}");
                return;
            }

            JsonObject ok = new JsonObject();
            ok.addProperty("ok", true);
            ok.addProperty("uuid", linked.get().uuid());
            ok.addProperty("name", linked.get().name());
            respond(exchange, 200, GSON.toJson(ok));
        } catch (Exception e) {
            respond(exchange, 500, "{\"error\":\"Internal Server Error\"}");
        }
    }

    /** discord_id 단위 슬라이딩 윈도우 rate-limit. */
    private boolean allow(String discordId) {
        long now = System.currentTimeMillis();
        Deque<Long> dq = attempts.computeIfAbsent(discordId, k -> new ArrayDeque<>());
        synchronized (dq) {
            while (!dq.isEmpty() && now - dq.peekFirst() > RATE_WINDOW_MS) {
                dq.pollFirst();
            }
            if (dq.size() >= RATE_MAX_ATTEMPTS) {
                return false;
            }
            dq.addLast(now);
            return true;
        }
    }

    private static String readBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
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
