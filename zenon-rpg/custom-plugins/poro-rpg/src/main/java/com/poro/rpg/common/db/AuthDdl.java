package com.poro.rpg.common.db;

/**
 * 디스코드 인증 DDL (DL-138 — 인게임 발급 → 봇 검증).
 *
 * <ul>
 *   <li>{@code auth_pending_code} — 인게임 {@code /인증}이 발급한 1회용 코드. MC uuid에 바인드, 짧은 TTL.
 *       uuid당 활성 코드 1개(발급 시 기존 코드 삭제).</li>
 *   <li>{@code discord_link} — verify 성공 시 확정되는 {@code discord_id ↔ {uuid, name}} 매핑.</li>
 * </ul>
 */
public final class AuthDdl {
    private AuthDdl() {}

    public static final String CREATE_AUTH_PENDING_CODE = """
        CREATE TABLE IF NOT EXISTS auth_pending_code (
            code        TEXT    PRIMARY KEY,
            player_uuid TEXT    NOT NULL,
            player_name TEXT    NOT NULL,
            created_at  INTEGER NOT NULL,
            expires_at  INTEGER NOT NULL
        )
        """;

    /** uuid당 활성 코드 정리(발급 시 기존 삭제)·만료 청소용 조회 인덱스. */
    public static final String CREATE_INDEX_PENDING_UUID =
            "CREATE INDEX IF NOT EXISTS idx_auth_pending_uuid ON auth_pending_code (player_uuid)";

    public static final String CREATE_DISCORD_LINK = """
        CREATE TABLE IF NOT EXISTS discord_link (
            discord_id  TEXT    PRIMARY KEY,
            player_uuid TEXT    NOT NULL UNIQUE,
            player_name TEXT    NOT NULL,
            linked_at   INTEGER NOT NULL
        )
        """;
}
