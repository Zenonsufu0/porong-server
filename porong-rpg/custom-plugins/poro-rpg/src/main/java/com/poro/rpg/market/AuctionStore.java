package com.poro.rpg.market;

import com.poro.rpg.common.db.ConnectionProvider;
import com.poro.rpg.common.db.TransactionHelper;
import com.poro.rpg.common.result.ErrorCode;
import com.poro.rpg.common.result.Result;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public final class AuctionStore {
    public static final int    MAX_LISTINGS  = 5;
    public static final int    PAGE_SIZE     = 36;
    public static final int    MY_PAGE_SIZE  = 45;
    public static final long   MIN_PRICE     = 100L;
    public static final long   EXPIRY_MILLIS = 3L * 24 * 60 * 60 * 1_000;
    public static final double FEE_RATE      = 0.05;

    /** 실체 없는 통화형 거래 품목 — 창고(customItems) 아닌 통화(growthState)로 차감·지급 (DL-129#37). */
    public static final java.util.Set<String> CURRENCY_ITEMS = java.util.Set.of("mat_cube", "mat_stone_enhance");
    public static boolean isCurrencyItem(String itemId) { return CURRENCY_ITEMS.contains(itemId); }

    private final ConnectionProvider connectionProvider;
    private final TransactionHelper  transactionHelper;
    private final Logger             logger;
    private final Set<UUID>          claimInFlight = ConcurrentHashMap.newKeySet();

    public AuctionStore(ConnectionProvider connectionProvider,
                        TransactionHelper transactionHelper,
                        Logger logger) {
        this.connectionProvider = connectionProvider;
        this.transactionHelper  = transactionHelper;
        this.logger             = logger;
    }

    public Logger logger() { return logger; }

    /** true이면 클레임 진입 허용, false이면 이미 진행 중이므로 호출 측에서 중단해야 한다. */
    public boolean tryStartClaim(UUID uuid) { return claimInFlight.add(uuid); }
    public void    endClaim(UUID uuid)       { claimInFlight.remove(uuid); }

    public static long netPrice(long price) {
        return price - (long) (price * FEE_RATE);
    }

    // ── 등록 ──────────────────────────────────────────────────────────────

    public int countActive(UUID sellerUuid) {
        String sql = "SELECT COUNT(*) FROM auction_listings WHERE seller_uuid=? AND status='active'";
        try (Connection conn = connectionProvider.getConnection().value();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, sellerUuid.toString());
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        } catch (Exception e) {
            logger.warning("[Auction] countActive error: " + e.getMessage());
            return 0;
        }
    }

    /** 아이템은 호출 전에 customItems에서 차감해야 한다. */
    public Result<Long> register(UUID sellerUuid, String sellerName,
                                  String itemId, int quantity, long price) {
        return register(sellerUuid, sellerName, itemId, quantity, price, null);
    }

    /** 흔적 인스턴스 거래용 — itemPayload(JSON)에 등급+세부스탯 보관 (DL-129 추가#38, P5). */
    public Result<Long> register(UUID sellerUuid, String sellerName,
                                  String itemId, int quantity, long price, String itemPayload) {
        long now = System.currentTimeMillis();
        return transactionHelper.inTransaction(conn -> {
            String sql = """
                INSERT INTO auction_listings
                  (seller_uuid, seller_name, item_id, quantity, price, listed_at, expires_at, status, item_payload)
                VALUES (?, ?, ?, ?, ?, ?, ?, 'active', ?)
                """;
            try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, sellerUuid.toString());
                ps.setString(2, sellerName);
                ps.setString(3, itemId);
                ps.setInt(4, quantity);
                ps.setLong(5, price);
                ps.setLong(6, now);
                ps.setLong(7, now + EXPIRY_MILLIS);
                ps.setString(8, itemPayload);
                ps.executeUpdate();
                ResultSet keys = ps.getGeneratedKeys();
                return keys.next() ? keys.getLong(1) : -1L;
            }
        });
    }

    // ── 조회 ──────────────────────────────────────────────────────────────

    public List<AuctionListing> getPage(String category, SortMode sort, int page) {
        String where   = categoryWhere(category);
        String orderBy = switch (sort) {
            case LATEST       -> "listed_at DESC";
            case PRICE_ASC    -> "price ASC";
            case EXPIRES_SOON -> "expires_at ASC";
        };
        String sql = "SELECT * FROM auction_listings WHERE status='active'" + where
                   + " ORDER BY " + orderBy
                   + " LIMIT " + PAGE_SIZE + " OFFSET " + (page * PAGE_SIZE);
        return queryListings(sql);
    }

    public int totalPages(String category) {
        String where = categoryWhere(category);
        String sql = "SELECT COUNT(*) FROM auction_listings WHERE status='active'" + where;
        try (Connection conn = connectionProvider.getConnection().value();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            int count = rs.next() ? rs.getInt(1) : 0;
            return Math.max(1, (count + PAGE_SIZE - 1) / PAGE_SIZE);
        } catch (Exception e) {
            logger.warning("[Auction] totalPages error: " + e.getMessage());
            return 1;
        }
    }

    public List<AuctionListing> getMyListings(UUID sellerUuid, int page) {
        String sql = """
            SELECT * FROM auction_listings
            WHERE seller_uuid=? AND status='active'
            ORDER BY listed_at DESC
            LIMIT ? OFFSET ?
            """;
        try (Connection conn = connectionProvider.getConnection().value();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, sellerUuid.toString());
            ps.setInt(2, MY_PAGE_SIZE);
            ps.setInt(3, page * MY_PAGE_SIZE);
            return mapListings(ps.executeQuery());
        } catch (Exception e) {
            logger.warning("[Auction] getMyListings error: " + e.getMessage());
            return List.of();
        }
    }

    /** 관리자 로그용 — 최근 판매 완료 거래 N건 (status='sold', sold_at DESC). */
    public List<AuctionListing> recentSold(int limit) {
        String sql = "SELECT * FROM auction_listings WHERE status='sold' ORDER BY sold_at DESC LIMIT " + limit;
        return queryListings(sql);
    }

    public Result<AuctionListing> findActive(long listingId) {
        String sql = "SELECT * FROM auction_listings WHERE id=? AND status='active'";
        try (Connection conn = connectionProvider.getConnection().value();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, listingId);
            List<AuctionListing> list = mapListings(ps.executeQuery());
            if (list.isEmpty())
                return Result.failure(ErrorCode.UNKNOWN, "listing not found or not active");
            return Result.success(list.get(0));
        } catch (Exception e) {
            return Result.failure(ErrorCode.DB_CONNECTION_FAILED, e.getMessage(), e);
        }
    }

    /** 3일 평균 가격. 데이터 없으면 -1 반환. */
    public long getAveragePrice(String itemId) {
        String sql = """
            SELECT AVG(price) FROM auction_listings
            WHERE item_id=? AND status='sold' AND sold_at >= ?
            """;
        try (Connection conn = connectionProvider.getConnection().value();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, itemId);
            ps.setLong(2, System.currentTimeMillis() - EXPIRY_MILLIS);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                double avg = rs.getDouble(1);
                return rs.wasNull() ? -1L : (long) avg;
            }
            return -1L;
        } catch (Exception e) {
            logger.warning("[Auction] getAveragePrice error: " + e.getMessage());
            return -1L;
        }
    }

    // ── 구매 ──────────────────────────────────────────────────────────────

    /**
     * 구매 처리. 골드 차감은 호출 전에 GrowthState에서 해야 한다.
     * 성공 시 listing 반환 — 아이템 지급에 사용.
     * 판매자 골드 pending_delivery 삽입은 같은 트랜잭션에서 원자적으로 처리된다.
     */
    public Result<AuctionListing> buy(long listingId, UUID buyerUuid) {
        long now = System.currentTimeMillis();
        return transactionHelper.inTransaction(conn -> {
            AuctionListing listing;
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT * FROM auction_listings WHERE id=? AND status='active'")) {
                ps.setLong(1, listingId);
                List<AuctionListing> list = mapListings(ps.executeQuery());
                if (list.isEmpty()) throw new IllegalStateException("이미 판매되거나 만료된 상품입니다.");
                listing = list.get(0);
            }
            if (listing.sellerUuid().equals(buyerUuid))
                throw new IllegalStateException("자신의 등록 상품은 구매할 수 없습니다.");

            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE auction_listings SET status='sold', sold_at=? WHERE id=?")) {
                ps.setLong(1, now);
                ps.setLong(2, listingId);
                ps.executeUpdate();
            }
            // 판매자 골드 pending — 같은 트랜잭션 안에서 원자적으로 처리
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO auction_pending_delivery (player_uuid, gold, created_at) VALUES (?, ?, ?)")) {
                ps.setString(1, listing.sellerUuid().toString());
                ps.setLong(2, netPrice(listing.price()));
                ps.setLong(3, now);
                ps.executeUpdate();
            }
            // 구매자 아이템 pending — crash 시 다음 로그인에서 재전달 보장. 흔적은 item_payload 동반.
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO auction_pending_delivery (player_uuid, item_id, quantity, gold, created_at, item_payload) VALUES (?, ?, ?, 0, ?, ?)")) {
                ps.setString(1, buyerUuid.toString());
                ps.setString(2, listing.itemId());
                ps.setInt(3, listing.quantity());
                ps.setLong(4, now);
                ps.setString(5, listing.itemPayload());
                ps.executeUpdate();
            }
            return listing;
        });
    }

    // ── 취소 ──────────────────────────────────────────────────────────────

    /** 취소 성공 시 listing 반환. 아이템은 호출 측에서 즉시 customItems에 반환한다. */
    public Result<AuctionListing> cancel(long listingId, UUID sellerUuid) {
        return transactionHelper.inTransaction(conn -> {
            AuctionListing listing;
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT * FROM auction_listings WHERE id=? AND seller_uuid=? AND status='active'")) {
                ps.setLong(1, listingId);
                ps.setString(2, sellerUuid.toString());
                List<AuctionListing> list = mapListings(ps.executeQuery());
                if (list.isEmpty())
                    throw new IllegalStateException("취소할 상품을 찾을 수 없습니다.");
                listing = list.get(0);
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE auction_listings SET status='cancelled' WHERE id=?")) {
                ps.setLong(1, listingId);
                ps.executeUpdate();
            }
            return listing;
        });
    }

    // ── 만료 처리 (1분 스케줄러용) ───────────────────────────────────────

    /** 만료된 등록을 처리하고 아이템 반환 pending_delivery 삽입. 처리 건수 반환. */
    public int expireOld() {
        Result<Integer> result = transactionHelper.inTransaction(conn -> {
            long now = System.currentTimeMillis();
            List<AuctionListing> toExpire;
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT * FROM auction_listings WHERE status='active' AND expires_at <= ?")) {
                ps.setLong(1, now);
                toExpire = mapListings(ps.executeQuery());
            }
            if (toExpire.isEmpty()) return 0;

            try (PreparedStatement upPs = conn.prepareStatement(
                     "UPDATE auction_listings SET status='expired' WHERE id=?");
                 PreparedStatement insPs = conn.prepareStatement(
                     "INSERT INTO auction_pending_delivery (player_uuid, item_id, quantity, created_at, item_payload) VALUES (?, ?, ?, ?, ?)")) {
                for (AuctionListing l : toExpire) {
                    upPs.setLong(1, l.id());
                    upPs.addBatch();
                    insPs.setString(1, l.sellerUuid().toString());
                    insPs.setString(2, l.itemId());
                    insPs.setInt(3, l.quantity());
                    insPs.setLong(4, now);
                    insPs.setString(5, l.itemPayload());
                    insPs.addBatch();
                }
                upPs.executeBatch();
                insPs.executeBatch();
            }
            return toExpire.size();
        });
        return result.orElse(0);
    }

    // ── 로그인 시 대기 보상 수령 ──────────────────────────────────────────

    public record PendingDelivery(long id, UUID playerUuid, String itemId,
                                   int quantity, long gold, String itemPayload) {
        public boolean isTrace() { return itemPayload != null && !itemPayload.isBlank(); }
    }

    /**
     * 지급 대기 목록 조회 (읽기 전용, DB 변경 없음).
     * 호출 측에서 메모리에 지급 성공 후 {@link #deletePendingByIds}를 호출해야 한다.
     */
    public List<PendingDelivery> fetchPending(UUID playerUuid) {
        try (Connection conn = connectionProvider.getConnection().value();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT * FROM auction_pending_delivery WHERE player_uuid=?")) {
            ps.setString(1, playerUuid.toString());
            ResultSet rs = ps.executeQuery();
            List<PendingDelivery> deliveries = new ArrayList<>();
            while (rs.next()) {
                deliveries.add(new PendingDelivery(
                        rs.getLong("id"),
                        playerUuid,
                        rs.getString("item_id"),
                        rs.getInt("quantity"),
                        rs.getLong("gold"),
                        rs.getString("item_payload")
                ));
            }
            return deliveries;
        } catch (Exception e) {
            logger.warning("[Auction] fetchPending error: " + e.getMessage());
            return List.of();
        }
    }

    /**
     * 메모리 지급 완료 후, fetch 시점에 조회된 ID만 삭제한다.
     * player_uuid 전체 삭제 대신 id 기반으로 삭제해야 fetch↔delete 사이에
     * 추가된 새 pending 레코드를 보호할 수 있다.
     */
    public void deletePendingByIds(List<Long> ids) {
        if (ids.isEmpty()) return;
        String placeholders = String.join(",", ids.stream().map(id -> "?").toArray(String[]::new));
        String sql = "DELETE FROM auction_pending_delivery WHERE id IN (" + placeholders + ")";
        transactionHelper.inTransaction(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (int i = 0; i < ids.size(); i++) ps.setLong(i + 1, ids.get(i));
                ps.executeUpdate();
            }
            return null;
        });
    }

    // ── 내부 유틸 ─────────────────────────────────────────────────────────

    private String categoryWhere(String category) {
        return switch (category) {
            case "흔적" -> " AND (item_id LIKE 'equip_trace_%' OR item_id LIKE 'ancient_trace_%' OR item_id LIKE 'trace\\_%' ESCAPE '\\')";
            case "재료" -> " AND item_id LIKE 'mat_%'";
            case "치장" -> " AND item_id LIKE 'display_%'";
            case "기타" -> " AND item_id NOT LIKE 'equip_trace_%'"
                           + " AND item_id NOT LIKE 'ancient_trace_%'"
                           + " AND item_id NOT LIKE 'trace\\_%' ESCAPE '\\'"
                           + " AND item_id NOT LIKE 'mat_%'"
                           + " AND item_id NOT LIKE 'display_%'";
            default    -> "";
        };
    }

    private List<AuctionListing> queryListings(String sql) {
        try (Connection conn = connectionProvider.getConnection().value();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            return mapListings(rs);
        } catch (Exception e) {
            logger.warning("[Auction] queryListings error: " + e.getMessage());
            return List.of();
        }
    }

    private List<AuctionListing> mapListings(ResultSet rs) throws SQLException {
        List<AuctionListing> result = new ArrayList<>();
        while (rs.next()) {
            long soldAtRaw = rs.getLong("sold_at");
            Long soldAt = rs.wasNull() ? null : soldAtRaw;
            result.add(new AuctionListing(
                    rs.getLong("id"),
                    UUID.fromString(rs.getString("seller_uuid")),
                    rs.getString("seller_name"),
                    rs.getString("item_id"),
                    rs.getInt("quantity"),
                    rs.getLong("price"),
                    rs.getLong("listed_at"),
                    rs.getLong("expires_at"),
                    rs.getString("status"),
                    soldAt,
                    rs.getString("item_payload")
            ));
        }
        return result;
    }

    // ── 정렬 모드 ─────────────────────────────────────────────────────────

    public enum SortMode {
        LATEST, PRICE_ASC, EXPIRES_SOON;

        public SortMode next() {
            return values()[(ordinal() + 1) % values().length];
        }

        public String displayName() {
            return switch (this) {
                case LATEST       -> "최신 등록순";
                case PRICE_ASC    -> "가격 낮은순";
                case EXPIRES_SOON -> "만료 임박순";
            };
        }
    }
}
