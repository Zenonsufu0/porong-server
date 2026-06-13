package kr.zenon.rpg.common.db;

import kr.zenon.rpg.common.result.Result;

import java.sql.Connection;

public interface ConnectionProvider {
    Result<Connection> getConnection();
}
