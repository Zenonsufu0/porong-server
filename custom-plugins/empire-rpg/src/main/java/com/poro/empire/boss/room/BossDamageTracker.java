package com.poro.empire.boss.room;

import com.poro.empire.field.ContributionTracker;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 인스턴스 보스(보스룸) 데미지 기여 추적 (INBOX-004 #5 / DL-084).
 * 스폰된 보스 mob UUID ↔ runId를 연결하고, 범용 {@link ContributionTracker}로 참여자별 데미지를 누적한다.
 * 종료 시 finalizeShares로 참여자별 점유율(%)을 산출 → boss_session_player.damage_share에 기록.
 *
 * <p>한계: 보스가 소환한 add(소환수)는 다른 UUID라 미집계(메인 보스 데미지만). 페이즈 전환으로 보스 엔티티가
 * 교체되면 추적이 끊긴다(1차 시즌 바닐라 강화 보스는 단일 엔티티 전제).</p>
 */
public final class BossDamageTracker {
    private final ContributionTracker contributions = new ContributionTracker();
    private final Map<UUID, String> mobToRun = new ConcurrentHashMap<>();
    private final Map<String, UUID> runToMob = new ConcurrentHashMap<>();

    /** 스폰 성공 후 보스 mob ↔ run 등록. */
    public void registerMob(String runId, UUID mobUuid) {
        if (runId == null || mobUuid == null) return;
        mobToRun.put(mobUuid, runId);
        runToMob.put(runId, mobUuid);
    }

    /** 데미지 리스너가 추적 대상 보스인지 빠르게 확인. */
    public boolean isTracked(UUID mobUuid) {
        return mobToRun.containsKey(mobUuid);
    }

    /** 보스 mob UUID → runId (미추적이면 null). 보스 사망→클리어 종료 브리지용 (DL-091). */
    public String runIdForMob(UUID mobUuid) {
        return mobUuid == null ? null : mobToRun.get(mobUuid);
    }

    /** 추적 대상 보스에 대한 데미지 누적 (추적 대상이 아니면 무시). */
    public void recordDamage(UUID mobUuid, UUID playerUuid, double damage) {
        if (mobToRun.containsKey(mobUuid)) {
            contributions.recordDamage(mobUuid, playerUuid, damage);
        }
    }

    /** 종료 시 runId 기준 참여자별 점유율(%) 산출 + 매핑·데이터 정리. */
    public Map<UUID, Double> finalizeShares(String runId) {
        UUID mob = runToMob.remove(runId);
        if (mob == null) return Map.of();
        mobToRun.remove(mob);
        return contributions.finalizeShares(mob);
    }
}
