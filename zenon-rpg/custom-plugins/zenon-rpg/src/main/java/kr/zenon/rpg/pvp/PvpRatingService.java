package kr.zenon.rpg.pvp;

import kr.zenon.rpg.pvp.db.PvpRatingRepository;

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
    private PvpRatingRepository repository; // optional — DB 영속화. null이면 in-memory만.

    /** 서버 시작 후 DB 캐시 로드 + 이후 변경 시 자동 영속화. */
    public void attachRepository(PvpRatingRepository repository) {
        this.repository = repository;
        if (repository != null) {
            repository.loadAll().forEach(r -> ratings.put(r.uuid(), r));
        }
    }

    public Rating getOrInit(UUID uuid, String name) {
        return ratings.computeIfAbsent(uuid, k -> new Rating(uuid, name, INITIAL_SCORE, 0, 0));
    }

    public Rating recordWin(UUID uuid, String name) {
        Rating r = getOrInit(uuid, name).withDelta(WIN_DELTA);
        ratings.put(uuid, r);
        if (repository != null) repository.save(r);
        return r;
    }

    public Rating recordLoss(UUID uuid, String name) {
        Rating r = getOrInit(uuid, name).withDelta(LOSS_DELTA);
        ratings.put(uuid, r);
        if (repository != null) repository.save(r);
        return r;
    }

    /** 관리자 임의 점수 조정. 승/패 카운터는 그대로. */
    public Rating adminAdjustScore(UUID uuid, String name, int delta) {
        Rating r = getOrInit(uuid, name);
        Rating updated = new Rating(r.uuid(), r.name(), Math.max(0, r.score() + delta), r.wins(), r.losses());
        ratings.put(uuid, updated);
        if (repository != null) repository.save(updated);
        return updated;
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
