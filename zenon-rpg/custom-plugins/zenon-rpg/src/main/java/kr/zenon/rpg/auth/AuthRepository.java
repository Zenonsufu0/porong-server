package kr.zenon.rpg.auth;

import kr.zenon.rpg.common.db.ConnectionProvider;
import kr.zenon.rpg.common.logging.DomainLogger;
import kr.zenon.rpg.common.result.Result;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Objects;
import java.util.Optional;

/**
 * 디스코드 인증 저장소 (DL-138). {@code auth_pending_code} 발급/소모 + {@code discord_link} 확정.
 *
 * <p>verify는 {@link #consumeAndLink}로 <b>원자적</b>으로 처리한다(읽기→삭제→링크 1트랜잭션).
 * 코드 삭제 성공(1행)한 호출만 링크를 확정하므로 1회용·동시요청 경합이 안전하다.
 */
public final class AuthRepository {
    private final ConnectionProvider connectionProvider;
    private final DomainLogger logger;

    public AuthRepository(ConnectionProvider connectionProvider, DomainLogger logger) {
        this.connectionProvider = Objects.requireNonNull(connectionProvider, "connectionProvider");
        this.logger             = Objects.requireNonNull(logger, "logger");
    }

    /** uuid당 활성 코드 1개 — 발급 전 기존 미사용 코드를 제거한다. */
    public void deletePendingByUuid(String playerUuid) {
        Result<Connection> conn = connectionProvider.getConnection();
        if (conn.isFailure()) return;
        try (Connection c = conn.value();
             PreparedStatement ps = c.prepareStatement(
                     "DELETE FROM auth_pending_code WHERE player_uuid = ?")) {
            ps.setString(1, playerUuid);
            ps.executeUpdate();
        } catch (Exception e) {
            logger.warn("deletePendingByUuid failed: " + e.getMessage());
        }
    }

    /** 발급 코드 저장. */
    public boolean insertPending(String code, String playerUuid, String playerName,
                                 long createdAtEpochMs, long expiresAtEpochMs) {
        Result<Connection> conn = connectionProvider.getConnection();
        if (conn.isFailure()) return false;
        try (Connection c = conn.value();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO auth_pending_code (code, player_uuid, player_name, created_at, expires_at) "
                             + "VALUES (?, ?, ?, ?, ?)")) {
            ps.setString(1, code);
            ps.setString(2, playerUuid);
            ps.setString(3, playerName);
            ps.setLong(4, createdAtEpochMs);
            ps.setLong(5, expiresAtEpochMs);
            ps.executeUpdate();
            return true;
        } catch (Exception e) {
            logger.warn("insertPending failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * 코드 검증 + 소모 + 링크 확정 (원자적). 만료·없음이면 비어 있는 Optional.
     *
     * @param code      봇이 제출한 코드
     * @param discordId interaction discord_id
     * @param nowEpochMs 현재 시각(만료 비교)
     * @return 성공 시 링크된 {uuid, name}
     */
    public Optional<LinkedIdentity> consumeAndLink(String code, String discordId, long nowEpochMs) {
        Result<Connection> conn = connectionProvider.getConnection();
        if (conn.isFailure()) return Optional.empty();

        Connection c = conn.value();
        try {
            c.setAutoCommit(false);

            String uuid;
            String name;
            long expiresAt;
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT player_uuid, player_name, expires_at FROM auth_pending_code WHERE code = ?")) {
                ps.setString(1, code);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        c.rollback();
                        return Optional.empty();
                    }
                    uuid = rs.getString("player_uuid");
                    name = rs.getString("player_name");
                    expiresAt = rs.getLong("expires_at");
                }
            }

            // 코드 소모 — 만료/유효 무관 즉시 제거(만료 코드도 청소).
            int deleted;
            try (PreparedStatement del = c.prepareStatement(
                    "DELETE FROM auth_pending_code WHERE code = ?")) {
                del.setString(1, code);
                deleted = del.executeUpdate();
            }

            if (deleted != 1 || nowEpochMs > expiresAt) {
                // 경합에서 졌거나(이미 소모) 만료 → 링크하지 않음.
                c.commit();
                return Optional.empty();
            }

            try (PreparedStatement up = c.prepareStatement(
                    "INSERT INTO discord_link (discord_id, player_uuid, player_name, linked_at) "
                            + "VALUES (?, ?, ?, ?) "
                            + "ON CONFLICT(discord_id) DO UPDATE SET "
                            + "  player_uuid = excluded.player_uuid, "
                            + "  player_name = excluded.player_name, "
                            + "  linked_at   = excluded.linked_at")) {
                up.setString(1, discordId);
                up.setString(2, uuid);
                up.setString(3, name);
                up.setLong(4, nowEpochMs);
                up.executeUpdate();
            }

            c.commit();
            return Optional.of(new LinkedIdentity(uuid, name));
        } catch (Exception e) {
            try { c.rollback(); } catch (Exception ignored) { /* best-effort */ }
            logger.warn("consumeAndLink failed: " + e.getMessage());
            return Optional.empty();
        } finally {
            try { c.setAutoCommit(true); } catch (Exception ignored) { /* best-effort */ }
            try { c.close(); } catch (Exception ignored) { /* best-effort */ }
        }
    }

    /** verify 응답에 담기는 확정 신원. */
    public record LinkedIdentity(String uuid, String name) {
    }
}
