package com.poro.empire.growth.island;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 플레이어 영지 상태 도메인.
 * 작위·자동재배기·공방·편의해금 비트마스크를 보관.
 *
 * <p>편의해금 비트 레이아웃:
 * <pre>
 *  bit 0 (0x01) : 자동 심기
 *  bit 1 (0x02) : 자동 입금 (채굴·수확)
 *  bit 2 (0x04) : 가공기 창고 자동 연동
 *  bit 3 (0x08) : 경매장 창고 자동 연동
 * </pre>
 */
public final class IslandTerritoryState {

    public static final int CONV_AUTO_PLANT    = 0x01;
    public static final int CONV_AUTO_DEPOSIT  = 0x02;
    public static final int CONV_WORKSHOP_LINK = 0x04;
    public static final int CONV_AUCTION_LINK  = 0x08;

    private IslandRank rank;
    private int  convenienceUnlocks;
    private String islandName;

    /** 기계 설치 대수. */
    private int reaperCount  = 0; // 자동 재배기
    private int storageCount = 0; // 영지 저장고

    /** 공방 대기열. 완료된 작업은 GUI 열 때 collect 처리. */
    private final List<WorkshopJob> workshopJobs = new ArrayList<>();

    /** 커스텀 아이템 재고 (큐브 조각, 흔적 등). String ID → 수량. */
    private final Map<String, Long> customItems = new LinkedHashMap<>();

    public IslandTerritoryState(String ownerName) {
        this.rank               = IslandRank.FRONTIER;
        this.convenienceUnlocks = 0;
        this.islandName         = ownerName + "의 영지";
    }

    // ─── 작위 ─────────────────────────────────────────────────────
    public IslandRank rank() { return rank; }
    public void setRank(IslandRank rank) { this.rank = rank; }

    // ─── 기계 대수 ────────────────────────────────────────────────
    public int reaperCount()  { return reaperCount;  }
    public int storageCount() { return storageCount; }

    public void setReaperCount(int n)  { this.reaperCount  = Math.max(0, n); }
    public void setStorageCount(int n) { this.storageCount = Math.max(0, n); }

    // ─── 공방 ─────────────────────────────────────────────────────
    public List<WorkshopJob> workshopJobs() { return workshopJobs; }
    public int workshopQueueUsed() { return workshopJobs.size(); }
    public int workshopQueueMax() { return rank.workshopQueueMax(); }
    public boolean workshopFull() { return workshopJobs.size() >= workshopQueueMax(); }

    public boolean addWorkshopJob(WorkshopJob job) {
        if (workshopFull()) return false;
        workshopJobs.add(job);
        return true;
    }

    public WorkshopJob removeWorkshopJob(int index) {
        if (index < 0 || index >= workshopJobs.size()) return null;
        return workshopJobs.remove(index);
    }

    // ─── 커스텀 아이템 ────────────────────────────────────────────
    public long getCustomItem(String id) { return customItems.getOrDefault(id, 0L); }
    public void addCustomItem(String id, long amount) {
        if (amount > 0) customItems.merge(id, amount, Long::sum);
    }
    public boolean withdrawCustomItem(String id, long amount) {
        long held = customItems.getOrDefault(id, 0L);
        if (held < amount) return false;
        long remaining = held - amount;
        if (remaining == 0) customItems.remove(id);
        else customItems.put(id, remaining);
        return true;
    }

    public java.util.Map<String, Long> customItemsSnapshot() { return java.util.Map.copyOf(customItems); }

    // ─── 편의 해금 ────────────────────────────────────────────────
    public boolean hasConvenience(int bit) { return (convenienceUnlocks & bit) != 0; }
    public void unlockConvenience(int bit) { convenienceUnlocks |= bit; }
    public int convenienceUnlocks() { return convenienceUnlocks; }
    public void setConvenienceUnlocks(int mask) { this.convenienceUnlocks = mask; }

    // ─── 영지명 ───────────────────────────────────────────────────
    public String islandName() { return islandName; }
    public void setIslandName(String name) { this.islandName = name; }

}
