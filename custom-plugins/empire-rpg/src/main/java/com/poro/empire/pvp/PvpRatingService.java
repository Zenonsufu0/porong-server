package com.poro.empire.pvp;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 정규대전 점수 관리 (in-memory, 1차 시즌).
 * <p>1차 시즌 압축: pvp_rating DB는 후속 Phase에서 추가. 현재는 인메모리만.
 */
public final class PvpRatingService {

    public static final int INITIAL_SCORE = 100;
    public static final int WIN_DELTA     = 15;
    public static final int LOSS_DELTA    = -10;

    public record Rating(UUID uuid, String name, int score, int wins, int losses) {
        public Rating withDelta(int delta) {
            int newScore = Math.max(0, score + delta);
            int newWins  = delta > 0 ? wins + 1 : wins;
            int newLoss  = delta < 0 ? losses + 1 : losses;
            return new Rating(uuid, name, newScore, newWins, newLoss);
        }
    }

    private final Map<UUID, Rating> ratings = new ConcurrentHashMap<>();

    public Rating getOrInit(UUID uuid, String name) {
        return ratings.computeIfAbsent(uuid, k -> new Rating(uuid, name, INITIAL_SCORE, 0, 0));
    }

    public Rating recordWin(UUID uuid, String name) {
        Rating r = getOrInit(uuid, name).withDelta(WIN_DELTA);
        ratings.put(uuid, r);
        return r;
    }

    public Rating recordLoss(UUID uuid, String name) {
        Rating r = getOrInit(uuid, name).withDelta(LOSS_DELTA);
        ratings.put(uuid, r);
        return r;
    }

    /** 점수 순위 — 점수 desc → 승 desc → 이름 asc. */
    public List<Rating> rankings() {
        List<Rating> sorted = new ArrayList<>(ratings.values());
        sorted.sort(Comparator
                .comparingInt((Rating r) -> -r.score())
                .thenComparingInt(r -> -r.wins())
                .thenComparing(Rating::name));
        return sorted;
    }
}
