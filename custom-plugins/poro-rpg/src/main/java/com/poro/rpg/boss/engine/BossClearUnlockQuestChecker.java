package com.poro.rpg.boss.engine;

import com.poro.rpg.boss.room.BossRoomManager;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * 보스 입장 언락 게이트 — 특정 보스 클리어 여부로 판정 (DL-044 / DL-091).
 * 현재 규칙: {@code quest_boss6_clear} = 시즌 6번째 보스 {@code void_herald} 클리어 시 최종보스 3종 입장 해금.
 *
 * <p>요구 코드가 없으면(blank) 통과. 미정의 코드는 통과(미모델 콘텐츠를 막지 않음) — 실제 게이트는
 * 매핑된 코드에서만 강제된다. 클리어 기록은 {@link BossRoomManager}(in-memory)에서 조회 —
 * 재시작 시 소실되므로 영속화는 후속 과제.
 */
public final class BossClearUnlockQuestChecker implements UnlockQuestChecker {

    /** 언락 퀘스트 코드 → 요구 보스 id. */
    private static final Map<String, String> QUEST_TO_REQUIRED_BOSS = Map.of(
            "quest_boss6_clear", "void_herald"
    );

    private final BossRoomManager bossRoomManager;

    public BossClearUnlockQuestChecker(BossRoomManager bossRoomManager) {
        this.bossRoomManager = Objects.requireNonNull(bossRoomManager, "bossRoomManager");
    }

    @Override
    public boolean hasUnlocked(String userId, String unlockQuestCode) {
        if (unlockQuestCode == null || unlockQuestCode.isBlank()) return true;
        String requiredBoss = QUEST_TO_REQUIRED_BOSS.get(unlockQuestCode.trim());
        if (requiredBoss == null) return true; // 미모델 코드는 막지 않음
        UUID uuid = parseUuid(userId);
        return uuid != null && bossRoomManager.hasCleared(uuid, requiredBoss);
    }

    private static UUID parseUuid(String userId) {
        if (userId == null || userId.isBlank()) return null;
        try {
            return UUID.fromString(userId.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
