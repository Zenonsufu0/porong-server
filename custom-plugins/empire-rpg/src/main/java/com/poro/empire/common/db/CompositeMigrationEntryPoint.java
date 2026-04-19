package com.poro.empire.common.db;

import com.poro.empire.common.result.Result;

import java.sql.Connection;
import java.util.List;

public final class CompositeMigrationEntryPoint implements MigrationEntryPoint {
    private final List<MigrationEntryPoint> delegates;

    public CompositeMigrationEntryPoint(List<MigrationEntryPoint> delegates) {
        this.delegates = List.copyOf(delegates);
    }

    @Override
    public Result<Void> migrate(Connection connection) {
        for (MigrationEntryPoint delegate : delegates) {
            Result<Void> result = delegate.migrate(connection);
            if (result.isFailure()) {
                return result;
            }
        }
        return Result.success();
    }
}
