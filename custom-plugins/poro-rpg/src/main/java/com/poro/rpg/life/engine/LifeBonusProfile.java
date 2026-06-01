package com.poro.rpg.life.engine;

public record LifeBonusProfile(
        double gatherSpeedBonusPct,
        double yieldBonusPct,
        double rareBonusPct,
        double estateOutputBonusPct
) {
    public static final LifeBonusProfile ZERO = new LifeBonusProfile(0.0d, 0.0d, 0.0d, 0.0d);
}
