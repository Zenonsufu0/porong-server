package kr.zenon.rpg.common.db;

import kr.zenon.rpg.common.result.Result;

import java.sql.Connection;

public interface MigrationEntryPoint {
    Result<Void> migrate(Connection connection);
}
