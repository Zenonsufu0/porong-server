package com.poro.empire.common.db;

/**
 * 영지 설정·멤버·권한 영속화 DDL (gui_territory_settings.md §14).
 *
 * <ul>
 *   <li>island_settings: visit_mode (영지명·convenienceUnlocks·rank·기계수는 PlayerSaveData에 이미 영속화됨)</li>
 *   <li>island_members: 영지 멤버 UUID + Role + 이름 캐시</li>
 *   <li>island_role_permissions: 등급별 권한 비트마스크</li>
 * </ul>
 */
public final class IslandSettingsDdl {
    private IslandSettingsDdl() {}

    public static final String CREATE_ISLAND_SETTINGS = """
        CREATE TABLE IF NOT EXISTS island_settings (
            owner_uuid  TEXT    PRIMARY KEY,
            visit_mode  TEXT    NOT NULL DEFAULT 'PUBLIC',
            updated_at  INTEGER NOT NULL
        )
        """;

    public static final String CREATE_ISLAND_MEMBERS = """
        CREATE TABLE IF NOT EXISTS island_members (
            owner_uuid   TEXT    NOT NULL,
            member_uuid  TEXT    NOT NULL,
            member_name  TEXT,
            role         TEXT    NOT NULL DEFAULT 'RESIDENT',
            added_at     INTEGER NOT NULL,
            PRIMARY KEY (owner_uuid, member_uuid)
        )
        """;

    public static final String CREATE_INDEX_MEMBERS_OWNER =
            "CREATE INDEX IF NOT EXISTS idx_island_members_owner ON island_members (owner_uuid)";

    public static final String CREATE_ROLE_PERMISSIONS = """
        CREATE TABLE IF NOT EXISTS island_role_permissions (
            owner_uuid   TEXT    NOT NULL,
            role         TEXT    NOT NULL,
            perm_mask    INTEGER NOT NULL DEFAULT 0,
            updated_at   INTEGER NOT NULL,
            PRIMARY KEY (owner_uuid, role)
        )
        """;
}
