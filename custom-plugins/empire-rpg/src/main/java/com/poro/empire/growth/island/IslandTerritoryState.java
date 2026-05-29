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

    public static final int CONV_AUTO_PLANT     = 0x01;
    public static final int CONV_AUTO_DEPOSIT   = 0x02;
    public static final int CONV_WORKSHOP_LINK  = 0x04;
    public static final int CONV_AUCTION_LINK   = 0x08;
    public static final int CONV_VISITOR_MINE   = 0x10; // 방문자 채굴 허용
    public static final int CONV_VISITOR_FARM   = 0x20; // 방문자 농사 허용
    public static final int CONV_CROP_PROTECT   = 0x40; // 농작물 보호
    public static final int CONV_WATER_PROTECT  = 0x80; // 물 파괴 보호

    /** 방문 공개 설정. PUBLIC=전체공개, FRIENDS=친구만, PRIVATE=비공개. */
    public enum VisitMode { PUBLIC, FRIENDS, PRIVATE }

    /** 영지 권한 등급. 영주(LORD)는 모든 권한 고정이라 매트릭스에 포함 안됨. */
    public enum Role { VICE_LORD, RESIDENT, VISITOR }

    /** 권한 항목 (9종). 비트 인덱스로 직접 매핑. */
    public enum Permission {
        STORAGE_DEPOSIT(0),    // 창고 입출금
        WORKSHOP_USE(1),       // 공방 가공기 사용
        MINE(2),               // 채굴 (광물 채굴기)
        FARM(3),               // 농사 (약초 재배지)
        WATER_MODIFY(4),       // 물 제거·배치
        MEMBER_INVITE(5),      // 멤버 초대
        MEMBER_KICK(6),        // 멤버 강퇴
        BLOCK_MODIFY(7),       // 블록 파괴·배치
        SETTINGS_EDIT(8);      // 영지 설정 변경

        public final int bit;
        Permission(int bitIndex) { this.bit = 1 << bitIndex; }
    }

    private IslandRank rank;
    private int  convenienceUnlocks;
    private String islandName;
    /** in-memory only — 영속화는 권한 시스템과 함께 추후 추가. */
    private VisitMode visitMode = VisitMode.PUBLIC;
    /** 등급별 권한 비트마스크. in-memory only — 영속화는 권한 시스템 통합 시. */
    private final java.util.EnumMap<Role, Integer> rolePermissions = new java.util.EnumMap<>(Role.class);

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
        // 권한 기본값 — gui_territory_settings.md §12 매트릭스
        // 부영주: 채굴/농사/물 제외 모두 ON
        rolePermissions.put(Role.VICE_LORD,
                Permission.STORAGE_DEPOSIT.bit | Permission.WORKSHOP_USE.bit |
                Permission.MINE.bit | Permission.FARM.bit | Permission.WATER_MODIFY.bit |
                Permission.MEMBER_INVITE.bit | Permission.MEMBER_KICK.bit |
                Permission.BLOCK_MODIFY.bit | Permission.SETTINGS_EDIT.bit);
        // 영지민: 창고/공방/농사만
        rolePermissions.put(Role.RESIDENT,
                Permission.STORAGE_DEPOSIT.bit | Permission.WORKSHOP_USE.bit | Permission.FARM.bit);
        // 방문자: 모두 OFF
        rolePermissions.put(Role.VISITOR, 0);
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

    /** now 기준 완료된 작업을 큐에서 제거하고 반환한다. */
    public java.util.List<WorkshopJob> collectCompletedJobs(long now) {
        java.util.List<WorkshopJob> done = workshopJobs.stream()
                .filter(j -> j.completeAt() <= now)
                .toList();
        workshopJobs.removeIf(j -> j.completeAt() <= now);
        return done;
    }

    public java.util.List<WorkshopJob> workshopJobsSnapshot() { return java.util.List.copyOf(workshopJobs); }

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
    public void revokeConvenience(int bit) { convenienceUnlocks &= ~bit; }
    public void toggleConvenience(int bit) {
        if (hasConvenience(bit)) revokeConvenience(bit);
        else unlockConvenience(bit);
    }
    public int convenienceUnlocks() { return convenienceUnlocks; }
    public void setConvenienceUnlocks(int mask) { this.convenienceUnlocks = mask; }

    // ─── 권한 등급 (in-memory) ────────────────────────────────────
    public boolean hasPermission(Role role, Permission perm) {
        if (role == null) return false;
        return (rolePermissions.getOrDefault(role, 0) & perm.bit) != 0;
    }
    public void togglePermission(Role role, Permission perm) {
        if (role == null) return;
        int cur = rolePermissions.getOrDefault(role, 0);
        rolePermissions.put(role, cur ^ perm.bit);
    }
    public int rolePermissionMask(Role role) {
        return rolePermissions.getOrDefault(role, 0);
    }

    // ─── 방문 공개 설정 (in-memory) ───────────────────────────────
    public VisitMode visitMode() { return visitMode; }
    public void setVisitMode(VisitMode mode) { this.visitMode = mode == null ? VisitMode.PUBLIC : mode; }
    public VisitMode cycleVisitMode() {
        visitMode = switch (visitMode) {
            case PUBLIC  -> VisitMode.FRIENDS;
            case FRIENDS -> VisitMode.PRIVATE;
            case PRIVATE -> VisitMode.PUBLIC;
        };
        return visitMode;
    }

    // ─── 영지명 ───────────────────────────────────────────────────
    public String islandName() { return islandName; }
    public void setIslandName(String name) { this.islandName = name; }

}
