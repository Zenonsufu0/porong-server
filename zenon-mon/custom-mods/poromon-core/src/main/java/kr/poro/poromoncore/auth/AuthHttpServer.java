package kr.poro.poromoncore.auth;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import kr.poro.poromoncore.PoroMonCore;
import kr.poro.poromoncore.config.ConfigManager;
import kr.poro.poromoncore.config.CoreConfig;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.Executors;

/**
 * 디스코드 봇용 인증 HTTP API (결정 041, JDK 내장 HttpServer — 무의존).
 *  - POST /auth/verify  헤더 X-API-Key, 바디 {"code","discordId"} → 코드 검증 → {"ok","uuid"}.
 *  - GET  /auth/ping    헬스체크.
 * 봇(integrations/poromon_api.py)이 API 키로 호출. 검증 로직은 AuthManager(서버 스레드 위임).
 */
public final class AuthHttpServer {
    private AuthHttpServer() {}

    private static final Gson GSON = new Gson();
    private static HttpServer http;

    public static void start() {
        CoreConfig.DiscordAuth cfg = ConfigManager.core().discordAuth;
        if (!cfg.enabled) return;
        stop();
        try {
            http = HttpServer.create(new InetSocketAddress(cfg.bindAddress, cfg.httpPort), 0);
            http.setExecutor(Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "PoroMon-AuthHttp");
                t.setDaemon(true);
                return t;
            }));
            http.createContext("/auth/verify", AuthHttpServer::handleVerify);
            http.createContext("/auth/ping", AuthHttpServer::handlePing);
            http.start();
            PoroMonCore.LOGGER.info("[Auth] 인증 HTTP API 시작: {}:{}", cfg.bindAddress, cfg.httpPort);
            if ("CHANGE_ME".equals(cfg.apiKey)) {
                PoroMonCore.LOGGER.warn("[Auth] ⚠️ apiKey가 기본값(CHANGE_ME) — core.json에서 반드시 변경하세요.");
            }
        } catch (IOException e) {
            PoroMonCore.LOGGER.error("[Auth] HTTP API 시작 실패 ({}:{})", cfg.bindAddress, cfg.httpPort, e);
        }
    }

    public static void stop() {
        if (http != null) {
            http.stop(0);
            http = null;
        }
    }

    private static void handlePing(HttpExchange ex) throws IOException {
        respond(ex, 200, "{\"ok\":true,\"service\":\"poromon-auth\"}");
    }

    private static void handleVerify(HttpExchange ex) throws IOException {
        try {
            if (!"POST".equalsIgnoreCase(ex.getRequestMethod())) { respond(ex, 405, err("method")); return; }
            CoreConfig.DiscordAuth cfg = ConfigManager.core().discordAuth;
            String key = ex.getRequestHeaders().getFirst("X-API-Key");
            if (key == null || !key.equals(cfg.apiKey)) { respond(ex, 401, err("unauthorized")); return; }

            String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            JsonObject json = GSON.fromJson(body, JsonObject.class);
            String code = json != null && json.has("code") ? json.get("code").getAsString() : null;
            String discordId = json != null && json.has("discordId") ? json.get("discordId").getAsString() : null;
            if (code == null || discordId == null) { respond(ex, 400, err("missing code/discordId")); return; }

            UUID uuid = AuthManager.verify(code, discordId);
            if (uuid == null) { respond(ex, 404, err("invalid or expired code")); return; }
            respond(ex, 200, "{\"ok\":true,\"uuid\":\"" + uuid + "\"}");
        } catch (Exception e) {
            PoroMonCore.LOGGER.error("[Auth] /auth/verify 처리 오류", e);
            try { respond(ex, 500, err("server error")); } catch (IOException ignored) {}
        }
    }

    private static String err(String msg) { return "{\"ok\":false,\"error\":\"" + msg + "\"}"; }

    private static void respond(HttpExchange ex, int status, String json) throws IOException {
        byte[] b = json.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json");
        ex.sendResponseHeaders(status, b.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(b); }
    }
}
