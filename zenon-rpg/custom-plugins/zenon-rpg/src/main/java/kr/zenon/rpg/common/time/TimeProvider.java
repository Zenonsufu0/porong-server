package kr.zenon.rpg.common.time;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public interface TimeProvider {
    Instant nowInstant();

    default ZonedDateTime now(ZoneId zoneId) {
        return ZonedDateTime.ofInstant(nowInstant(), zoneId);
    }
}
