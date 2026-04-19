package com.poro.empire.util;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;

public final class HealthHudFormatter {
    private static final int BAR_LENGTH = 10;
    private static final String HEART_ICON = "\u2764";
    private static final String FILLED_BLOCK = "\u25A0";
    private static final String EMPTY_BLOCK = "\u25A1";

    private HealthHudFormatter() {
    }

    public static String format(Player player) {
        double currentHealth = Math.max(0.0d, player.getHealth());
        double maxHealth = resolveMaxHealth(player);

        int filled = Math.max(0, Math.min(BAR_LENGTH, (int) Math.round((currentHealth / maxHealth) * BAR_LENGTH)));
        int empty = BAR_LENGTH - filled;

        String filledBar = FILLED_BLOCK.repeat(filled);
        String emptyBar = EMPTY_BLOCK.repeat(empty);

        return HEART_ICON + " " + formatValue(currentHealth) + " / " + formatValue(maxHealth) + " [" + filledBar + emptyBar + "]";
    }

    private static double resolveMaxHealth(Player player) {
        AttributeInstance attribute = player.getAttribute(Attribute.MAX_HEALTH);
        if (attribute == null || attribute.getValue() <= 0.0d) {
            return 20.0d;
        }
        return attribute.getValue();
    }

    private static String formatValue(double value) {
        if (Math.abs(value - Math.rint(value)) < 0.01d) {
            return Integer.toString((int) Math.rint(value));
        }
        return String.format("%.1f", value);
    }
}
