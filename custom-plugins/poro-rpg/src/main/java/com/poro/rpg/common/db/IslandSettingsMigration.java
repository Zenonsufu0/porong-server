package com.poro.rpg.common.db;

import com.poro.rpg.common.logging.DomainLogger;
import com.poro.rpg.common.result.ErrorCode;
import com.poro.rpg.common.result.Result;

import java.sql.Connection;
import java.sql.Statement;
import java.util.Objects;

public final class IslandSettingsMigration implements MigrationEntryPoint {
    private final DomainLogger logger;

    public IslandSettingsMigration(DomainLogger logger) {
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    @Override
    public Result<Void> migrate(Connection connection) {
        try (Statement st = connection.createStatement()) {
            st.execute(IslandSettingsDdl.CREATE_ISLAND_SETTINGS);
            st.execute(IslandSettingsDdl.CREATE_ISLAND_MEMBERS);
            st.execute(IslandSettingsDdl.CREATE_INDEX_MEMBERS_OWNER);
            st.execute(IslandSettingsDdl.CREATE_ROLE_PERMISSIONS);
            logger.info("island_settings + island_members + island_role_permissions DDL applied.");
            return Result.success();
        } catch (Exception e) {
            return Result.failure(ErrorCode.DB_CONNECTION_FAILED,
                    "Failed to apply island_settings DDL", e);
        }
    }
}
