package com.poro.rpg.common.seed;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public final class CsvRow {
    private final int lineNumber;
    private final Map<String, String> values;

    public CsvRow(int lineNumber, Map<String, String> values) {
        this.lineNumber = lineNumber;
        this.values = Map.copyOf(new LinkedHashMap<>(Objects.requireNonNull(values, "values")));
    }

    public int lineNumber() {
        return lineNumber;
    }

    public String required(String key) {
        String value = values.get(normalizeKey(key));
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing required column value for '" + key + "' at line " + lineNumber);
        }
        return value.trim();
    }

    public String optional(String key) {
        String value = values.get(normalizeKey(key));
        return value == null ? "" : value.trim();
    }

    public int optionalInt(String key, int fallback) {
        String raw = optional(key);
        if (raw.isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    "Invalid integer for column '" + key + "' at line " + lineNumber + ": " + raw,
                    exception
            );
        }
    }

    public double optionalDouble(String key, double fallback) {
        String raw = optional(key);
        if (raw.isBlank()) {
            return fallback;
        }
        try {
            return Double.parseDouble(raw);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    "Invalid decimal for column '" + key + "' at line " + lineNumber + ": " + raw,
                    exception
            );
        }
    }

    public boolean optionalBoolean(String key, boolean fallback) {
        String raw = optional(key);
        if (raw.isBlank()) {
            return fallback;
        }

        String lowered = raw.toLowerCase(Locale.ROOT);
        return switch (lowered) {
            case "true", "1", "yes", "y" -> true;
            case "false", "0", "no", "n" -> false;
            default -> throw new IllegalArgumentException(
                    "Invalid boolean for column '" + key + "' at line " + lineNumber + ": " + raw
            );
        };
    }

    private String normalizeKey(String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("column key must not be blank");
        }
        return key.trim().toLowerCase(Locale.ROOT);
    }
}
