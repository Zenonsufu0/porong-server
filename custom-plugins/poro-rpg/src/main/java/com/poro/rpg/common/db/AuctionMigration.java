package com.poro.rpg.common.db;

import com.poro.rpg.common.logging.DomainLogger;
import com.poro.rpg.common.result.ErrorCode;
import com.poro.rpg.common.result.Result;

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
            if (isLegacySchema(connection)) {
                int rowCount = legacyRowCount(connection);
                if (rowCount > 0) {
                    // 데이터 있음 → 수동 조치 필요. item_data(직렬화된 ItemStack)에서
                    // item_id를 SQL 단계에서 추출할 수 없으므로 자동 이관 불가.
                    // 관리자가 DB를 직접 백업·삭제한 뒤 서버를 재시작해야 합니다.
                    return Result.failure(ErrorCode.DB_CONNECTION_FAILED,
                            "auction_listings 구 스키마에 활성 매물 " + rowCount + "건이 있습니다. "
                            + "storage/poro.sqlite 백업 후 auction_listings 테이블을 직접 삭제하고 재시작하세요.");
                }
                logger.info("auction_listings 구 스키마 감지 (데이터 없음). 드롭 후 재생성합니다.");
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
            return rs.next();
        } catch (Exception e) {
            return false;
        }
    }

    private int legacyRowCount(Connection connection) {
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM auction_listings")) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (Exception e) {
            return 0;
        }
    }
}
