package com.poro.empire.growth.island;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

/** 영지 작위 8단계 (island_system_design.md §2.1). */
public enum IslandRank {
    // 승급 재료: 전장의 파편(mat_battle_shard) + 골드만 요구 (DL-066 확정)
    FRONTIER ("개척지",  0,   500,    20_000,
            List.of(new UpgradeMaterial("mat_battle_shard", 50))),
    KNIGHT   ("기사령",  1,   900,    35_000,
            List.of(new UpgradeMaterial("mat_battle_shard", 80))),
    BARONET  ("준남작령",2, 1_500,    55_000,
            List.of(new UpgradeMaterial("mat_battle_shard", 100))),
    BARON    ("남작령",  3, 2_500,    75_000,
            List.of(new UpgradeMaterial("mat_battle_shard", 80))),
    VISCOUNT ("자작령",  4, 3_500,    95_000,
            List.of(new UpgradeMaterial("mat_battle_shard", 60))),
    COUNT    ("백작령",  5, 5_000,   120_000,
            List.of(new UpgradeMaterial("mat_battle_shard", 120))),
    MARQUESS ("후작령",  6, 7_000,   150_000,
            List.of(new UpgradeMaterial("mat_battle_shard", 200))),
    DUKE     ("공작령",  7,10_000,         0,
            List.of());

    public final String displayName;
    public final int  tier;
    public final int  storagePerItem;
    /** 다음 작위로 승급하기 위한 골드 비용. DUKE = 0 (최고 단계). */
    public final long goldUpgradeCost;
    /** 다음 작위 승급에 필요한 재료 목록. */
    public final List<UpgradeMaterial> upgradeMaterials;

    private static final NumberFormat FMT = NumberFormat.getNumberInstance(Locale.KOREA);

    IslandRank(String displayName, int tier, int storagePerItem, long goldUpgradeCost,
               List<UpgradeMaterial> upgradeMaterials) {
        this.displayName     = displayName;
        this.tier            = tier;
        this.storagePerItem  = storagePerItem;
        this.goldUpgradeCost = goldUpgradeCost;
        this.upgradeMaterials = upgradeMaterials;
    }

    public IslandRank next() {
        IslandRank[] vals = values();
        return tier < vals.length - 1 ? vals[tier + 1] : null;
    }

    /** 작위별 공방 대기열 최대 슬롯 (5~12). */
    public int workshopQueueMax() {
        return Math.min(5 + tier, 12);
    }

    /** "150,000G" 형식 골드 비용 문자열. */
    public String goldUpgradeCostDisplay() {
        return FMT.format(goldUpgradeCost) + "G";
    }

    /** 재료 ID와 수량 쌍. */
    public record UpgradeMaterial(String itemId, long amount) {}
}
