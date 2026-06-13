package kr.zenon.moncore.config;

/**
 * seasons.json 매핑 (league_season_design.md §10). 정규리그 코어용.
 * 챔피언스리그/시즌 운영 필드는 후속(Phase 6) — 현재는 정규리그만.
 */
public class SeasonConfig {
    public int configVersion = 1;
    public RankedLeague rankedLeague = new RankedLeague();

    public static class RankedLeague {
        public int requireBadges = 8;     // 자격: 8배지 (결정 028/league §4)
        public int startScore = 1000;     // 첫 참가 시 시작 점수
        public int winDelta = 10;         // 승 +10
        public int lossDelta = -7;        // 패 −7
        public int scoreFloor = 0;        // 점수 하한
        public int adjustLevel = 50;      // 레벨 정규화(Showdown adjustLevel, §7 옵션 A)
        public Matchmaking matchmaking = new Matchmaking();
    }

    public static class Matchmaking {
        public int windowStart = 50;            // 시작 매칭 점수폭 ±50
        public double windowStepPerSec = 2.5;   // 초당 ±확대
        public int windowMax = 400;             // 상한 ±400
        public int rematchCooldownSeconds = 600; // 동일 상대 재대전 쿨다운(파밍 방지)
    }
}
