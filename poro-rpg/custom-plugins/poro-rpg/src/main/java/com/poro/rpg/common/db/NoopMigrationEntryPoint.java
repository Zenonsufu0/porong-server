package com.poro.rpg.common.db;

import com.poro.rpg.common.result.Result;

import java.sql.Connection;

public final class NoopMigrationEntryPoint implements MigrationEntryPoint {
    @Override
    public Result<Void> migrate(Connection connection) {
        return Result.success();
    }
}
