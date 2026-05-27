package com.poro.empire.operations.http;

import com.poro.empire.common.logging.DomainLogger;
import com.sun.net.httpserver.HttpServer;

import java.net.InetSocketAddress;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * EmpireRPG HTTP API 서버 — 포트 8765, JDK 내장 HttpServer 사용.
 * 바인드 주소는 config common.api-bind-host (기본 127.0.0.1).
 * onEnable에서 start(), onDisable에서 stop() 호출.
 */
public final class EmpireHttpServer {
    private static final int PORT = 8765;

    private final HttpServer server;
    private final ExecutorService executor;
    private final DomainLogger logger;

    private EmpireHttpServer(HttpServer server, ExecutorService executor, DomainLogger logger) {
        this.server = server;
        this.executor = executor;
        this.logger = logger;
    }

    public static EmpireHttpServer create(
            BossApiHandler bossApiHandler,
            PlayerApiHandler playerApiHandler,
            String bindHost,
            DomainLogger logger) throws Exception {
        Objects.requireNonNull(bossApiHandler, "bossApiHandler");
        Objects.requireNonNull(playerApiHandler, "playerApiHandler");
        Objects.requireNonNull(logger, "logger");
        String host = (bindHost == null || bindHost.isBlank()) ? "127.0.0.1" : bindHost;

        HttpServer server = HttpServer.create(new InetSocketAddress(host, PORT), 0);
        server.createContext("/api/v1/boss", bossApiHandler);
        server.createContext("/player/by-nick", playerApiHandler);
        server.createContext("/island/by-nick", playerApiHandler);
        server.createContext("/boss-history/by-nick", playerApiHandler);
        ExecutorService executor = Executors.newFixedThreadPool(2, r -> {
            Thread t = new Thread(r, "empire-http");
            t.setDaemon(true);
            return t;
        });
        server.setExecutor(executor);
        return new EmpireHttpServer(server, executor, logger);
    }

    public void start() {
        server.start();
        logger.info("Empire HTTP API started on " + server.getAddress() + " — /api/v1/boss/*");
    }

    public void stop() {
        server.stop(1);
        executor.shutdownNow();
        logger.info("Empire HTTP API stopped.");
    }
}
