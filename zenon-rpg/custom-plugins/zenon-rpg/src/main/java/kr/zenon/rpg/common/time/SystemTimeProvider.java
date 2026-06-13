package kr.zenon.rpg.common.time;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

public final class SystemTimeProvider implements TimeProvider {
    private final Clock clock;

    public SystemTimeProvider(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public static SystemTimeProvider systemUtc() {
        return new SystemTimeProvider(Clock.systemUTC());
    }

    @Override
    public Instant nowInstant() {
        return clock.instant();
    }
}
