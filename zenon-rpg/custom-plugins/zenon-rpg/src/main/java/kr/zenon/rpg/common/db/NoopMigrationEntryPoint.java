package kr.zenon.rpg.common.db;

import kr.zenon.rpg.common.result.Result;

import java.sql.Connection;

public final class NoopMigrationEntryPoint implements MigrationEntryPoint {
    @Override
    public Result<Void> migrate(Connection connection) {
        return Result.success();
    }
}
