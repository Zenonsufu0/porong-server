package com.poro.rpg.growth.engine;

public record PotentialLine(
        int lineNo,
        PotentialGrade grade,
        String optionCode,
        double value
) {
}
