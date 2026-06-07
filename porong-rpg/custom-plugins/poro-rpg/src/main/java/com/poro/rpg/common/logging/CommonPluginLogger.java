package com.poro.rpg.common.logging;

import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class CommonPluginLogger {
    private final Logger delegate;
    private final String pluginName;

    public CommonPluginLogger(Logger delegate, String pluginName) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.pluginName = pluginName == null || pluginName.isBlank() ? "plugin" : pluginName;
    }

    public DomainLogger domain(String domain) {
        return new DomainLogger(this, domain);
    }

    public void info(String domain, String message) {
        delegate.info(format(domain, message));
    }

    public void warn(String domain, String message) {
        delegate.warning(format(domain, message));
    }

    public void error(String domain, String message) {
        delegate.severe(format(domain, message));
    }

    public void error(String domain, String message, Throwable throwable) {
        delegate.log(Level.SEVERE, format(domain, message), throwable);
    }

    private String format(String domain, String message) {
        String safeDomain = domain == null || domain.isBlank() ? "general" : domain;
        String safeMessage = message == null ? "" : message;
        return "[" + pluginName + "][" + safeDomain + "] " + safeMessage;
    }
}
