package com.poro.rpg.common.db;

import com.poro.rpg.common.logging.DomainLogger;
import com.poro.rpg.common.result.ErrorCode;
import com.poro.rpg.common.result.Result;

import java.sql.Connection;
import java.util.Objects;

public final class JdbcTransactionHelper implements TransactionHelper {
    private final ConnectionProvider connectionProvider;
    private final DomainLogger logger;

    public JdbcTransactionHelper(ConnectionProvider connectionProvider, DomainLogger logger) {
        this.connectionProvider = Objects.requireNonNull(connectionProvider, "connectionProvider");
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    @Override
    public <T> Result<T> inTransaction(TransactionWork<T> work) {
        Result<Connection> connectionResult = connectionProvider.getConnection();
        if (connectionResult.isFailure()) {
            return Result.failure(connectionResult.errorCode(), connectionResult.message(), connectionResult.cause());
        }

        try (Connection connection = connectionResult.value()) {
            boolean originalAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);

            try {
                T value = work.execute(connection);
                connection.commit();
                connection.setAutoCommit(originalAutoCommit);
                return Result.success(value);
            } catch (Exception exception) {
                connection.rollback();
                logger.error("Transaction rolled back due to an exception.", exception);
                return Result.failure(
                        ErrorCode.DB_TRANSACTION_FAILED,
                        "Transaction failed and was rolled back",
                        exception
                );
            }
        } catch (Exception exception) {
            return Result.failure(
                    ErrorCode.DB_TRANSACTION_FAILED,
                    "Failed to execute transaction",
                    exception
            );
        }
    }
}
