package com.poro.rpg.operations.query.model;

import java.util.Locale;

public record PlayerProfileRecord(
        String userId,
        String nickname,
        String classId
) {
    public PlayerProfileRecord {
        userId = normalize(userId);
        nickname = nickname == null ? "" : nickname.trim();
        classId = normalize(classId);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
