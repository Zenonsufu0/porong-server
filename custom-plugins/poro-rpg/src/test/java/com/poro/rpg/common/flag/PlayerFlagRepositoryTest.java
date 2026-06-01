package com.poro.rpg.common.flag;

import com.poro.rpg.common.db.ConnectionProvider;
import com.poro.rpg.common.db.JdbcTransactionHelper;
import com.poro.rpg.common.db.PlayerFlagDdl;
import com.poro.rpg.common.db.TransactionHelper;
import com.poro.rpg.common.logging.CommonPluginLogger;
import com.poro.rpg.common.logging.DomainLogger;
import com.poro.rpg.common.result.Result;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PR3 — {@link PlayerFlagRepository} SQLite in-memory 통합 테스트.
 *
 * <p>PR1의 DDL을 재사용해 테이블을 생성하고 CRUD 4종을 실제 SQL로 검증.
 * SQLite in-memory는 커넥션별로 DB가 분리되므로 공유 커넥션 1개를 close-proxy로
 * 감싸 {@link TransactionHelper}의 try-with-resources가 실제로 닫지 않게 한다.
 */
class PlayerFlagRepositoryTest {

    private Connection sharedConnection;
    private PlayerFlagRepository repository;

    @BeforeEach
    void setUp() throws Exception {
        sharedConnection = DriverManager.getConnection("jdbc:sqlite::memory:");
        try (Statement st = sharedConnection.createStatement()) {
            st.execute(PlayerFlagDdl.CREATE_TABLE);
            st.execute(PlayerFlagDdl.CREATE_INDEX_UPDATED_AT);
            st.execute(PlayerFlagDdl.CREATE_INDEX_FLAG_KEY);
        }

        Connection nonClosing = nonClosingConnection(sharedConnection);
        ConnectionProvider provider = () -> Result.success(nonClosing);
        DomainLogger logger = new CommonPluginLogger(Logger.getLogger("FlagTest"), "test").domain("db");
        TransactionHelper helper = new JdbcTransactionHelper(provider, logger);
        repository = new PlayerFlagRepository(helper, logger);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (sharedConnection != null) {
            sharedConnection.close();
        }
    }

    @Test
    void findReturnsEmptyForAbsentKey() {
        Result<Optional<PlayerFlag>> result = repository.find(UUID.randomUUID(), FlagKey.of("quest.capital.any"));
        assertFalse(result.isFailure());
        assertTrue(result.value().isEmpty());
    }

    @Test
    void upsertInsertsThenUpdatesWithVersionBump() {
        UUID player = UUID.randomUUID();
        FlagKey key = FlagKey.of("quest.capital.reach_radius");

        Result<Void> ins = repository.upsert(player, key, FlagValue.ofLong(100L), 1_000L);
        assertFalse(ins.isFailure());

        PlayerFlag inserted = repository.find(player, key).value().orElseThrow();
        assertEquals(100L, inserted.value().asLong());
        assertEquals(1_000L, inserted.updatedAtMillis());
        assertEquals(1L, inserted.version());

        Result<Void> upd = repository.upsert(player, key, FlagValue.ofLong(200L), 2_000L);
        assertFalse(upd.isFailure());

        PlayerFlag updated = repository.find(player, key).value().orElseThrow();
        assertEquals(200L, updated.value().asLong());
        assertEquals(2_000L, updated.updatedAtMillis());
        assertEquals(2L, updated.version());
    }

    @Test
    void findByPlayerReturnsOrderedList() {
        UUID player = UUID.randomUUID();
        repository.upsert(player, FlagKey.of("quest.capital.a"), FlagValue.ofBool(true), 1L);
        repository.upsert(player, FlagKey.of("quest.capital.b"), FlagValue.ofLong(5L), 2L);
        repository.upsert(player, FlagKey.of("quest.capital.c"), FlagValue.ofString("x"), 3L);

        List<PlayerFlag> all = repository.findByPlayer(player).value();
        assertEquals(3, all.size());
        assertEquals("quest.capital.a", all.get(0).flagKey().value());
        assertEquals("quest.capital.b", all.get(1).flagKey().value());
        assertEquals("quest.capital.c", all.get(2).flagKey().value());
    }

    @Test
    void findByFlagReturnsAcrossPlayers() {
        FlagKey key = FlagKey.of("quest.capital.shared");
        UUID p1 = UUID.randomUUID();
        UUID p2 = UUID.randomUUID();
        repository.upsert(p1, key, FlagValue.ofBool(true), 1L);
        repository.upsert(p2, key, FlagValue.ofBool(false), 2L);

        List<PlayerFlag> all = repository.findByFlag(key).value();
        assertEquals(2, all.size());
    }

    @Test
    void deleteReturnsTrueWhenRowExisted() {
        UUID player = UUID.randomUUID();
        FlagKey key = FlagKey.of("quest.capital.removed");
        repository.upsert(player, key, FlagValue.ofBool(true), 1L);

        assertTrue(repository.delete(player, key).value());
        assertFalse(repository.delete(player, key).value()); // 두 번째는 false
        assertTrue(repository.find(player, key).value().isEmpty());
    }

    @Test
    void allThreeTypesRoundTripCorrectly() {
        UUID player = UUID.randomUUID();
        repository.upsert(player, FlagKey.of("a.b.bool_flag"), FlagValue.ofBool(true), 1L);
        repository.upsert(player, FlagKey.of("a.b.long_flag"), FlagValue.ofLong(-123L), 2L);
        repository.upsert(player, FlagKey.of("a.b.string_flag"), FlagValue.ofString("hi"), 3L);

        List<PlayerFlag> flags = repository.findByPlayer(player).value();
        assertEquals(FlagValueType.BOOL, flags.get(0).value().type());
        assertTrue(flags.get(0).value().asBool());
        assertEquals(FlagValueType.LONG, flags.get(1).value().type());
        assertEquals(-123L, flags.get(1).value().asLong());
        assertEquals(FlagValueType.STRING, flags.get(2).value().type());
        assertEquals("hi", flags.get(2).value().asString());
    }

    /**
     * try-with-resources가 호출하는 {@code close()}를 무시하는 Connection 프록시.
     * 다른 모든 호출은 원본 커넥션으로 위임한다.
     */
    private static Connection nonClosingConnection(Connection delegate) {
        InvocationHandler handler = (proxy, method, args) -> {
            if ("close".equals(method.getName()) && method.getParameterCount() == 0) {
                return null;
            }
            return method.invoke(delegate, args);
        };
        return (Connection) Proxy.newProxyInstance(
                PlayerFlagRepositoryTest.class.getClassLoader(),
                new Class<?>[]{Connection.class},
                handler
        );
    }
}
