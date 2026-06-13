package kr.zenon.rpg.admin.config;

import kr.zenon.rpg.common.db.ConnectionProvider;
import kr.zenon.rpg.common.logging.DomainLogger;
import kr.zenon.rpg.common.result.Result;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** mob_stat_override CRUD. */
public final class MobStatOverrideRepository {

    private final ConnectionProvider connectionProvider;
    private final DomainLogger logger;

    public MobStatOverrideRepository(ConnectionProvider connectionProvider, DomainLogger logger) {
        this.connectionProvider = Objects.requireNonNull(connectionProvider);
        this.logger             = Objects.requireNonNull(logger);
    }

    /** 전체 로드 → mobKey 순서 보존 맵. 부팅 캐시 구성용. */
    public Map<String, MobStatOverride> findAll() {
        Map<String, MobStatOverride> out = new LinkedHashMap<>();
        String sql = "SELECT mob_key, max_hp, def, atk FROM mob_stat_override";
        try (Connection c = openConn();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String key = rs.getString("mob_key");
                out.put(key, new MobStatOverride(key,
                        nullableDouble(rs, "max_hp"),
                        nullableDouble(rs, "def"),
                        nullableDouble(rs, "atk")));
            }
        } catch (Exception e) {
            logger.warn("mob_stat_override findAll 실패: " + e.getMessage());
        }
        return out;
    }

    /** 전체 행 upsert. */
    public void save(MobStatOverride row, String updatedBy) {
        String sql = """
            INSERT INTO mob_stat_override (mob_key, max_hp, def, atk, updated_by, updated_at)
            VALUES (?, ?, ?, ?, ?, ?)
            ON CONFLICT(mob_key) DO UPDATE SET
                max_hp     = excluded.max_hp,
                def        = excluded.def,
                atk        = excluded.atk,
                updated_by = excluded.updated_by,
                updated_at = excluded.updated_at
            """;
        try (Connection c = openConn();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, row.mobKey());
            setNullableDouble(ps, 2, row.maxHp());
            setNullableDouble(ps, 3, row.def());
            setNullableDouble(ps, 4, row.atk());
            ps.setString(5, updatedBy);
            ps.setLong(6, System.currentTimeMillis());
            ps.executeUpdate();
        } catch (Exception e) {
            logger.warn("mob_stat_override save 실패 [" + row.mobKey() + "]: " + e.getMessage());
        }
    }

    /** 시드 전용 — 이미 존재하면 건드리지 않음(운영자 편집 보존). 신규 삽입 시 true. */
    public boolean insertIfAbsent(MobStatOverride row, String updatedBy) {
        String sql = """
            INSERT OR IGNORE INTO mob_stat_override (mob_key, max_hp, def, atk, updated_by, updated_at)
            VALUES (?, ?, ?, ?, ?, ?)
            """;
        try (Connection c = openConn();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, row.mobKey());
            setNullableDouble(ps, 2, row.maxHp());
            setNullableDouble(ps, 3, row.def());
            setNullableDouble(ps, 4, row.atk());
            ps.setString(5, updatedBy);
            ps.setLong(6, System.currentTimeMillis());
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            logger.warn("mob_stat_override seed 실패 [" + row.mobKey() + "]: " + e.getMessage());
            return false;
        }
    }

    public void delete(String mobKey) {
        try (Connection c = openConn();
             PreparedStatement ps = c.prepareStatement("DELETE FROM mob_stat_override WHERE mob_key = ?")) {
            ps.setString(1, mobKey);
            ps.executeUpdate();
        } catch (Exception e) {
            logger.warn("mob_stat_override delete 실패 [" + mobKey + "]: " + e.getMessage());
        }
    }

    private static Double nullableDouble(ResultSet rs, String col) throws Exception {
        double v = rs.getDouble(col);
        return rs.wasNull() ? null : v;
    }

    private static void setNullableDouble(PreparedStatement ps, int idx, Double v) throws Exception {
        if (v == null) ps.setNull(idx, java.sql.Types.REAL);
        else ps.setDouble(idx, v);
    }

    private Connection openConn() throws Exception {
        Result<Connection> r = connectionProvider.getConnection();
        if (r.isFailure()) throw new Exception("DB connection failed: " + r.message());
        return r.value();
    }
}
