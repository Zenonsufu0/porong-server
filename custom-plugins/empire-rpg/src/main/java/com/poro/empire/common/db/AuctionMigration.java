package com.poro.empire.common.db;

import com.poro.empire.common.logging.DomainLogger;
import com.poro.empire.common.result.ErrorCode;
import com.poro.empire.common.result.Result;

import java.sql.Connection;
import java.sql.Statement;
import java.util.Objects;

public final class AuctionMigration implements MigrationEntryPoint {
    private final DomainLogger logger;

    public AuctionMigration(DomainLogger logger) {
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    @Override
    public Result<Void> migrate(Connection connection) {
        try (Statement st = connection.createStatement()) {
            st.execute(AuctionDdl.CREATE_LISTINGS);
            st.execute(AuctionDdl.CREATE_INDEX_STATUS_ITEM);
            st.execute(AuctionDdl.CREATE_INDEX_SELLER);
            st.execute(AuctionDdl.CREATE_INDEX_EXPIRES);
            st.execute(AuctionDdl.CREATE_PENDING_DELIVERY);
            st.execute(AuctionDdl.CREATE_INDEX_PENDING_PLAYER);
            logger.info("auction_listings + auction_pending_delivery DDL applied (idempotent).");
            return Result.success();
        } catch (Exception e) {
            return Result.failure(ErrorCode.DB_CONNECTION_FAILED,
                    "Failed to apply auction DDL", e);
        }
    }
}
