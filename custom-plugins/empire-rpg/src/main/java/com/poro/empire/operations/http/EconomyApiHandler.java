package com.poro.empire.operations.http;

import com.google.gson.Gson;
import com.poro.empire.growth.engine.DbEnhancementLogHook;
import com.poro.empire.persistence.EconomyFlowRepository;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;

/**
 * GET /api/v1/economy/* — 경제 판단 지표 (INBOX-004 #2·#3).
 * <pre>
 *   /api/v1/economy/enhancement   — 강화 요약(표기 vs 실제 성공률·총 소모) + 티어·단계별 성공률
 *   /api/v1/economy/flow          — 통화별 발행량/소각량/net + 골드 최근 30일 일별 net
 * </pre>
 * Authorization: Bearer {api-secret-key} 헤더 필수. 미설정 시 503.
 */
public final class EconomyApiHandler implements HttpHandler {
    private static final Gson GSON = new Gson();
    private static final int FLOW_DAILY_DAYS = 30;

    private final DbEnhancementLogHook enhancementLog;
    private final EconomyFlowRepository economyFlow;
    private final String secretKey;

    public EconomyApiHandler(DbEnhancementLogHook enhancementLog, EconomyFlowRepository economyFlow,
                             String secretKey) {
        this.enhancementLog = Objects.requireNonNull(enhancementLog, "enhancementLog");
        this.economyFlow = Objects.requireNonNull(economyFlow, "economyFlow");
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
                .replaceFirst("^/api/v1/economy", "").replaceFirst("^/", "");
        try {
            if (tail.equals("enhancement")) {
                respond(exchange, 200, GSON.toJson(Map.of(
                        "summary", enhancementLog.summary(),
                        "by_tier_level", enhancementLog.statsByTierLevel())));
                return;
            }
            if (tail.equals("flow")) {
                respond(exchange, 200, GSON.toJson(Map.of(
                        "by_currency", economyFlow.netByCurrency(),
                        "gold_daily", economyFlow.dailyNet("gold", FLOW_DAILY_DAYS))));
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
