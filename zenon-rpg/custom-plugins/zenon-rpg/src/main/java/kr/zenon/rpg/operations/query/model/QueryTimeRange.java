package kr.zenon.rpg.operations.query.model;

import java.time.Instant;
import java.util.Objects;

public record QueryTimeRange(
        Instant from,
        Instant to,
        String label
) {
    public QueryTimeRange {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        label = label == null ? "" : label.trim();
        if (to.isBefore(from)) {
            throw new IllegalArgumentException("to must be >= from");
        }
    }

    public boolean contains(Instant target) {
        if (target == null) {
            return false;
        }
        return !target.isBefore(from) && !target.isAfter(to);
    }
}
