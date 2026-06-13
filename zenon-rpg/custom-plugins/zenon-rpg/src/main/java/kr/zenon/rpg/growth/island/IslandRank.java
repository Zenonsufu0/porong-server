package kr.zenon.rpg.growth.island;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

/** 영지 작위 8단계 (island_system_design.md §2.1). */
public enum IslandRank {
    // 승급 재료: 전장의 파편(mat_battle_shard) + 골드만 요구 (DL-066 / 요구량 ×50 상향 DL-129#32 — 파편 과잉)
    FRONTIER ("개척지",  0,   500,    20_000,
            List.of(new UpgradeMaterial("mat_battle_shard", 2_500))),
    KNIGHT   ("기사령",  1,   900,    35_000,
            List.of(new UpgradeMaterial("mat_battle_shard", 4_000))),
    BARONET  ("준남작령",2, 1_500,    55_000,
            List.of(new UpgradeMaterial("mat_battle_shard", 5_000))),
    BARON    ("남작령",  3, 2_500,    75_000,
            List.of(new UpgradeMaterial("mat_battle_shard", 4_000))),
    VISCOUNT ("자작령",  4, 3_500,    95_000,
            List.of(new UpgradeMaterial("mat_battle_shard", 3_000))),
    COUNT    ("백작령",  5, 5_000,   120_000,
            List.of(new UpgradeMaterial("mat_battle_shard", 6_000))),
    MARQUESS ("후작령",  6, 7_000,   150_000,
            List.of(new UpgradeMaterial("mat_battle_shard", 10_000))),
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

    /** 작위별 최대 시설 슬롯 (island_system_design.md §2.1 — 개척지 4 → 공작령 18). tier 인덱스. */
    private static final int[] FACILITY_SLOTS = {4, 6, 7, 9, 11, 13, 15, 18};

    /** 작위별 최대 시설 슬롯 (약초 재배지 + 광물 채굴기 + 공방 가공기 합산 한계). */
    public int maxFacilitySlots() {
        return FACILITY_SLOTS[Math.min(Math.max(0, tier), FACILITY_SLOTS.length - 1)];
    }

    /** @deprecated 공방 슬롯은 가공기 수×3 기준으로 이전(DL-129 추가#7). IslandTerritoryState.workshopQueueMax() 사용. */
    @Deprecated
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
