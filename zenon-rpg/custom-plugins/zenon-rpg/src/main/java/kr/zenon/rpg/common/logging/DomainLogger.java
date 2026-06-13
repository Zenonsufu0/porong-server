package kr.zenon.rpg.common.logging;

import java.util.Objects;

public final class DomainLogger {
    private final CommonPluginLogger rootLogger;
    private final String domain;

    DomainLogger(CommonPluginLogger rootLogger, String domain) {
        this.rootLogger = Objects.requireNonNull(rootLogger, "rootLogger");
        this.domain = domain == null || domain.isBlank() ? "general" : domain;
    }

    public void info(String message) {
        rootLogger.info(domain, message);
    }

    public void warn(String message) {
        rootLogger.warn(domain, message);
    }

    public void error(String message) {
        rootLogger.error(domain, message);
    }

    public void error(String message, Throwable throwable) {
        rootLogger.error(domain, message, throwable);
    }

    public String domain() {
        return domain;
    }
}
