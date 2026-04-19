package com.poro.empire.common.flag;

import com.poro.empire.common.db.TransactionHelper;
import com.poro.empire.common.logging.DomainLogger;
import com.poro.empire.common.result.Result;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * {@code player_flag} 테이블의 JDBC CRUD 리포지토리 (v0.1).
 *
 * <p>메서드 단위 트랜잭션을 {@link TransactionHelper#inTransaction}으로 위임한다.
 * Result 패턴으로 예외를 값으로 변환해 상위에서 실패 분기 처리 가능.
 */
public final class PlayerFlagRepository {

    private static final String COLUMNS =
            "player_uuid, flag_key, value_type, value_text, value_long, updated_at, version";

    private static final String UPSERT_SQL = """
            INSERT INTO player_flag (player_uuid, flag_key, value_type, value_text, value_long, updated_at, version)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(player_uuid, flag_key) DO UPDATE SET
                value_type = excluded.value_type,
                value_text = excluded.value_text,
                value_long = excluded.value_long,
                updated_at = excluded.updated_at,
                version = player_flag.version + 1
            """;

    private static final String FIND_ONE_SQL =
            "SELECT " + COLUMNS + " FROM player_flag WHERE player_uuid = ? AND flag_key = ?";

    private static final String FIND_BY_PLAYER_SQL =
            "SELECT " + COLUMNS + " FROM player_flag WHERE player_uuid = ? ORDER BY flag_key";

    private static final String FIND_BY_FLAG_SQL =
            "SELECT " + COLUMNS + " FROM player_flag WHERE flag_key = ? ORDER BY player_uuid";

    private static final String DELETE_SQL =
            "DELETE FROM player_flag WHERE player_uuid = ? AND flag_key = ?";

    private final TransactionHelper transactionHelper;
    private final DomainLogger logger;

    public PlayerFlagRepository(TransactionHelper transactionHelper, DomainLogger logger) {
        this.transactionHelper = Objects.requireNonNull(transactionHelper, "transactionHelper");
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    public Result<Optional<PlayerFlag>> find(UUID playerUuid, FlagKey flagKey) {
        Objects.requireNonNull(playerUuid, "playerUuid");
        Objects.requireNonNull(flagKey, "flagKey");
        return transactionHelper.inTransaction(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(FIND_ONE_SQL)) {
                statement.setString(1, playerUuid.toString());
                statement.setString(2, flagKey.value());
                try (ResultSet rs = statement.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(mapRow(rs));
                    }
                    return Optional.empty();
                }
            }
        });
    }

    public Result<List<PlayerFlag>> findByPlayer(UUID playerUuid) {
        Objects.requireNonNull(playerUuid, "playerUuid");
        return transactionHelper.inTransaction(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(FIND_BY_PLAYER_SQL)) {
                statement.setString(1, playerUuid.toString());
                return readAll(statement);
            }
        });
    }

    public Result<List<PlayerFlag>> findByFlag(FlagKey flagKey) {
        Objects.requireNonNull(flagKey, "flagKey");
        return transactionHelper.inTransaction(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(FIND_BY_FLAG_SQL)) {
                statement.setString(1, flagKey.value());
                return readAll(statement);
            }
        });
    }

    /**
     * UPSERT: 없으면 insert(version=1), 있으면 값 갱신 + version++.
     *
     * @param nowMillis 업데이트 시각(epoch millis) — 테스트 용이성 위해 주입
     */
    public Result<Void> upsert(UUID playerUuid, FlagKey flagKey, FlagValue value, long nowMillis) {
        Objects.requireNonNull(playerUuid, "playerUuid");
        Objects.requireNonNull(flagKey, "flagKey");
        Objects.requireNonNull(value, "value");
        return transactionHelper.inTransaction(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(UPSERT_SQL)) {
                bindUpsertParams(statement, playerUuid, flagKey, value, nowMillis);
                int affected = statement.executeUpdate();
                if (affected == 0) {
                    logger.warn("upsert affected 0 rows: player=" + playerUuid + " key=" + flagKey);
                }
                return null;
            }
        });
    }

    /**
     * @return true = 실제로 삭제됨, false = 대상 행 없음
     */
    public Result<Boolean> delete(UUID playerUuid, FlagKey flagKey) {
        Objects.requireNonNull(playerUuid, "playerUuid");
        Objects.requireNonNull(flagKey, "flagKey");
        return transactionHelper.inTransaction(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(DELETE_SQL)) {
                statement.setString(1, playerUuid.toString());
                statement.setString(2, flagKey.value());
                int affected = statement.executeUpdate();
                return affected > 0;
            }
        });
    }

    private static void bindUpsertParams(
            PreparedStatement statement,
            UUID playerUuid,
            FlagKey flagKey,
            FlagValue value,
            long nowMillis
    ) throws Exception {
        statement.setString(1, playerUuid.toString());
        statement.setString(2, flagKey.value());
        statement.setString(3, value.type().name());
        switch (value.type()) {
            case BOOL -> {
                statement.setString(4, Boolean.toString(value.asBool()));
                statement.setNull(5, java.sql.Types.INTEGER);
            }
            case LONG -> {
                statement.setNull(4, java.sql.Types.VARCHAR);
                statement.setLong(5, value.asLong());
            }
            case STRING -> {
                statement.setString(4, value.asString());
                statement.setNull(5, java.sql.Types.INTEGER);
            }
        }
        statement.setLong(6, nowMillis);
        statement.setLong(7, 1L); // 신규 INSERT 시 version=1, UPDATE 시 excluded 무시하고 version + 1
    }

    private static List<PlayerFlag> readAll(PreparedStatement statement) throws Exception {
        List<PlayerFlag> result = new ArrayList<>();
        try (ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                result.add(mapRow(rs));
            }
        }
        return result;
    }

    private static PlayerFlag mapRow(ResultSet rs) throws Exception {
        UUID playerUuid = UUID.fromString(rs.getString("player_uuid"));
        FlagKey flagKey = FlagKey.of(rs.getString("flag_key"));
        FlagValueType type = FlagValueType.valueOf(rs.getString("value_type"));
        FlagValue value = switch (type) {
            case BOOL -> FlagValue.ofBool(Boolean.parseBoolean(rs.getString("value_text")));
            case LONG -> FlagValue.ofLong(rs.getLong("value_long"));
            case STRING -> FlagValue.ofString(rs.getString("value_text"));
        };
        long updatedAt = rs.getLong("updated_at");
        long version = rs.getLong("version");
        return new PlayerFlag(playerUuid, flagKey, value, updatedAt, version);
    }
}
