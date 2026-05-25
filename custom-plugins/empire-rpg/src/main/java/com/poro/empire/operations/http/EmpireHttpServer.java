package com.poro.empire.operations.http;

import com.poro.empire.common.logging.DomainLogger;
import com.sun.net.httpserver.HttpServer;

import java.net.InetSocketAddress;
import java.util.Objects;
import java.util.concurrent.Executors;

/**
 * EmpireRPG HTTP API 서버 — 포트 8765, JDK 내장 HttpServer 사용.
 * onEnable에서 start(), onDisable에서 stop() 호출.
 */
public final class EmpireHttpServer {
    private static final int PORT = 8765;

    private final HttpServer server;
    private final DomainLogger logger;

    private EmpireHttpServer(HttpServer server, DomainLogger logger) {
        this.server = server;
        this.logger = logger;
    }

    public static EmpireHttpServer create(BossApiHandler bossApiHandler, DomainLogger logger) throws Exception {
        Objects.requireNonNull(bossApiHandler, "bossApiHandler");
        Objects.requireNonNull(logger, "logger");

        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/api/v1/boss", bossApiHandler);
        server.setExecutor(Executors.newFixedThreadPool(2, r -> {
            Thread t = new Thread(r, "empire-http");
            t.setDaemon(true);
            return t;
        }));
        return new EmpireHttpServer(server, logger);
    }

    public void start() {
        server.start();
        logger.info("Empire HTTP API started on port " + PORT + " — /api/v1/boss/*");
    }

    public void stop() {
        server.stop(1);
        logger.info("Empire HTTP API stopped.");
    }
}
