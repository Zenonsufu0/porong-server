package com.poro.empire.growth.island;

import java.text.NumberFormat;
import java.util.Locale;

/** 영지 작위 8단계 (master_plan.md §8.2). */
public enum IslandRank {
    FRONTIER ("개척지",  0,   500,    50_000),
    KNIGHT   ("기사령",  1,   900,   150_000),
    BARONET  ("준남작령",2, 1_500,   400_000),
    BARON    ("남작령",  3, 2_500,   800_000),
    VISCOUNT ("자작령",  4, 3_500, 1_500_000),
    COUNT    ("백작령",  5, 5_000, 3_000_000),
    MARQUESS ("후작령",  6, 7_000, 6_000_000),
    DUKE     ("공작령",  7,10_000,          0);

    public final String displayName;
    public final int  tier;
    public final int  storagePerItem;
    /** 다음 작위로 승급하기 위한 골드 비용. DUKE = 0 (최고 단계). */
    public final long goldUpgradeCost;

    private static final NumberFormat FMT = NumberFormat.getNumberInstance(Locale.KOREA);

    IslandRank(String displayName, int tier, int storagePerItem, long goldUpgradeCost) {
        this.displayName     = displayName;
        this.tier            = tier;
        this.storagePerItem  = storagePerItem;
        this.goldUpgradeCost = goldUpgradeCost;
    }

    public IslandRank next() {
        IslandRank[] vals = values();
        return tier < vals.length - 1 ? vals[tier + 1] : null;
    }

    /** 작위별 공방 대기열 최대 슬롯 (5~12). */
    public int workshopQueueMax() {
        return Math.min(5 + tier, 12);
    }

    /** "6,000,000G" 형식 골드 비용 문자열. */
    public String goldUpgradeCostDisplay() {
        return FMT.format(goldUpgradeCost) + "G";
    }
}
