package com.poro.rpg.common.db;

import com.poro.rpg.common.result.Result;

import java.sql.Connection;

public interface TransactionHelper {
    <T> Result<T> inTransaction(TransactionWork<T> work);

    @FunctionalInterface
    interface TransactionWork<T> {
        T execute(Connection connection) throws Exception;
    }
}
