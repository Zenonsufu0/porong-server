package com.poro.empire.leveling;

import com.poro.empire.growth.engine.PlayerGrowthState;

public final class PlayerLevelingService {
    public static final int MAX_LEVEL = 100;
    private static final int STAT_POINTS_PER_LEVEL = 3;

    /**
     * EXP를 state에 추가하고 레벨업을 처리한다.
     * currentExp는 현재 레벨 내 누적 EXP — 레벨업 시 초과분이 이월된다.
     * @return 이번 호출로 레벨업한 횟수 (0 이상)
     */
    public int addExp(PlayerGrowthState state, long amount) {
        if (state.playerLevel() >= MAX_LEVEL || amount <= 0) return 0;
        state.addExp(amount);
        int levelsGained = 0;
        while (state.playerLevel() < MAX_LEVEL) {
            long needed = expToNextLevel(state.playerLevel());
            if (state.currentExp() < needed) break;
            state.setCurrentExp(state.currentExp() - needed);
            state.setPlayerLevel(state.playerLevel() + 1);
            state.addUnspentPts(STAT_POINTS_PER_LEVEL);
            levelsGained++;
        }
        return levelsGained;
    }

    /** Lv n → n+1 에 필요한 EXP. 기하급수 곡선: base=800, r=1.1 */
    public static long expToNextLevel(int level) {
        if (level <= 0) return 800L;
        return Math.round(800.0 * Math.pow(1.1, level - 1));
    }
}
