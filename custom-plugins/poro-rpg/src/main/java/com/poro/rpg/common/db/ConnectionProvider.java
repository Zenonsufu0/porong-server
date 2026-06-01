package com.poro.rpg.common.db;

import com.poro.rpg.common.result.Result;

import java.sql.Connection;

public interface ConnectionProvider {
    Result<Connection> getConnection();
}
