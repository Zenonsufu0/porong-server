package kr.zenon.rpg.growth.engine;

import java.util.Comparator;
import java.util.List;

public record PotentialProfile(
        PotentialGrade grade,
        List<PotentialLine> lines
) {
    public PotentialProfile {
        lines = List.copyOf(lines.stream().sorted(Comparator.comparingInt(PotentialLine::lineNo)).toList());
    }
}
