package com.poro.empire.common.db;

import com.poro.empire.common.logging.DomainLogger;
import com.poro.empire.common.result.ErrorCode;
import com.poro.empire.common.result.Result;

import java.sql.Connection;
import java.sql.ResultSet;
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
            // 구 스키마(item_data 컬럼 존재) 감지 → 드롭 후 재생성
            if (isLegacySchema(connection)) {
                logger.info("auction_listings 구 스키마 감지 (item_data 컬럼). 재생성합니다.");
                st.execute("DROP TABLE IF EXISTS auction_listings");
            }
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

    private boolean isLegacySchema(Connection connection) {
        try (ResultSet rs = connection.getMetaData().getColumns(null, null, "auction_listings", "item_data")) {
            return rs.next(); // item_data 컬럼이 있으면 구 스키마
        } catch (Exception e) {
            return false;
        }
    }
}
