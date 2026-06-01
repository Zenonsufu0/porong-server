package com.poro.rpg.growth.island.db;

import com.poro.rpg.common.db.ConnectionProvider;
import com.poro.rpg.common.logging.DomainLogger;
import com.poro.rpg.common.result.Result;
import com.poro.rpg.growth.island.IslandTerritoryState;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * island_settings + island_members + island_role_permissions 통합 Repository.
 */
public final class IslandSettingsRepository {

    public record SettingsRow(UUID ownerUuid, IslandTerritoryState.VisitMode visitMode) {}
    public record MemberRow(UUID memberUuid, String memberName, IslandTerritoryState.Role role) {}
    public record PermRow(IslandTerritoryState.Role role, int permMask) {}
    public record IslandBundle(UUID ownerUuid,
                               IslandTerritoryState.VisitMode visitMode,
                               List<MemberRow> members,
                               Map<IslandTerritoryState.Role, Integer> permissions) {}

    private final ConnectionProvider connectionProvider;
    private final DomainLogger logger;

    public IslandSettingsRepository(ConnectionProvider connectionProvider, DomainLogger logger) {
        this.connectionProvider = Objects.requireNonNull(connectionProvider);
        this.logger             = Objects.requireNonNull(logger);
    }

    // ─── 영지 설정 (visit_mode) ──────────────────────────────────────

    public void saveSettings(UUID ownerUuid, IslandTerritoryState.VisitMode visitMode) {
        String sql = """
            INSERT INTO island_settings (owner_uuid, visit_mode, updated_at)
            VALUES (?, ?, ?)
            ON CONFLICT(owner_uuid) DO UPDATE SET
                visit_mode = excluded.visit_mode,
                updated_at = excluded.updated_at
            """;
        try (Connection c = openConn();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, ownerUuid.toString());
            ps.setString(2, visitMode.name());
            ps.setLong(3, System.currentTimeMillis());
            ps.executeUpdate();
        } catch (Exception e) {
            logger.warn("saveSettings failed [" + ownerUuid + "]: " + e.getMessage());
        }
    }

    // ─── 멤버 ────────────────────────────────────────────────────────

    public void saveMembers(UUID ownerUuid, List<Map.Entry<UUID, IslandTerritoryState.Role>> members,
                             Map<UUID, String> memberNames) {
        try (Connection c = openConn()) {
            c.setAutoCommit(false);
            try (PreparedStatement clear = c.prepareStatement("DELETE FROM island_members WHERE owner_uuid = ?")) {
                clear.setString(1, ownerUuid.toString());
                clear.executeUpdate();
            }
            String sql = """
                INSERT INTO island_members (owner_uuid, member_uuid, member_name, role, added_at)
                VALUES (?, ?, ?, ?, ?)
                """;
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                long now = System.currentTimeMillis();
                for (Map.Entry<UUID, IslandTerritoryState.Role> e : members) {
                    ps.setString(1, ownerUuid.toString());
                    ps.setString(2, e.getKey().toString());
                    ps.setString(3, memberNames.getOrDefault(e.getKey(), null));
                    ps.setString(4, e.getValue().name());
                    ps.setLong(5, now);
                    ps.addBatch();
                }
                ps.executeBatch();
            }
            c.commit();
            c.setAutoCommit(true);
        } catch (Exception e) {
            logger.warn("saveMembers failed [" + ownerUuid + "]: " + e.getMessage());
        }
    }

    // ─── 권한 ────────────────────────────────────────────────────────

    public void savePermissions(UUID ownerUuid, IslandTerritoryState.Role role, int permMask) {
        String sql = """
            INSERT INTO island_role_permissions (owner_uuid, role, perm_mask, updated_at)
            VALUES (?, ?, ?, ?)
            ON CONFLICT(owner_uuid, role) DO UPDATE SET
                perm_mask  = excluded.perm_mask,
                updated_at = excluded.updated_at
            """;
        try (Connection c = openConn();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, ownerUuid.toString());
            ps.setString(2, role.name());
            ps.setInt(3, permMask);
            ps.setLong(4, System.currentTimeMillis());
            ps.executeUpdate();
        } catch (Exception e) {
            logger.warn("savePermissions failed [" + ownerUuid + ", " + role + "]: " + e.getMessage());
        }
    }

    // ─── 단건 로드 (재로그인 lazy 복원) ─────────────────────────────

    /** 특정 owner의 설정/멤버/권한을 한번에 로드. 없으면 null. */
    public IslandBundle loadOne(UUID ownerUuid) {
        IslandTerritoryState.VisitMode visit = IslandTerritoryState.VisitMode.PUBLIC;
        List<MemberRow> members = new ArrayList<>();
        Map<IslandTerritoryState.Role, Integer> perms = new LinkedHashMap<>();
        boolean found = false;

        try (Connection c = openConn()) {
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT visit_mode FROM island_settings WHERE owner_uuid = ?")) {
                ps.setString(1, ownerUuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        visit = parseVisit(rs.getString("visit_mode"));
                        found = true;
                    }
                }
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT member_uuid, member_name, role FROM island_members WHERE owner_uuid = ?")) {
                ps.setString(1, ownerUuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        found = true;
                        members.add(new MemberRow(
                                UUID.fromString(rs.getString("member_uuid")),
                                rs.getString("member_name"),
                                parseRole(rs.getString("role"))
                        ));
                    }
                }
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT role, perm_mask FROM island_role_permissions WHERE owner_uuid = ?")) {
                ps.setString(1, ownerUuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        found = true;
                        perms.put(parseRole(rs.getString("role")), rs.getInt("perm_mask"));
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("IslandSettingsRepository.loadOne failed [" + ownerUuid + "]: " + e.getMessage());
            return null;
        }
        return found ? new IslandBundle(ownerUuid, visit, members, perms) : null;
    }

    // ─── 통합 로드 (서버 시작 시) ────────────────────────────────────

    /** 모든 영지의 설정/멤버/권한을 한번에 로드. */
    public Map<UUID, IslandBundle> loadAll() {
        Map<UUID, IslandTerritoryState.VisitMode> settings = new LinkedHashMap<>();
        Map<UUID, List<MemberRow>>                members  = new LinkedHashMap<>();
        Map<UUID, Map<IslandTerritoryState.Role, Integer>> perms = new LinkedHashMap<>();

        try (Connection c = openConn()) {
            try (PreparedStatement ps = c.prepareStatement("SELECT owner_uuid, visit_mode FROM island_settings");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    UUID owner = UUID.fromString(rs.getString("owner_uuid"));
                    settings.put(owner, parseVisit(rs.getString("visit_mode")));
                }
            }
            try (PreparedStatement ps = c.prepareStatement("SELECT owner_uuid, member_uuid, member_name, role FROM island_members");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    UUID owner = UUID.fromString(rs.getString("owner_uuid"));
                    members.computeIfAbsent(owner, k -> new ArrayList<>()).add(new MemberRow(
                            UUID.fromString(rs.getString("member_uuid")),
                            rs.getString("member_name"),
                            parseRole(rs.getString("role"))
                    ));
                }
            }
            try (PreparedStatement ps = c.prepareStatement("SELECT owner_uuid, role, perm_mask FROM island_role_permissions");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    UUID owner = UUID.fromString(rs.getString("owner_uuid"));
                    perms.computeIfAbsent(owner, k -> new LinkedHashMap<>())
                            .put(parseRole(rs.getString("role")), rs.getInt("perm_mask"));
                }
            }
        } catch (Exception e) {
            logger.warn("IslandSettingsRepository.loadAll failed: " + e.getMessage());
        }

        // 통합 — owners union
        Map<UUID, IslandBundle> out = new LinkedHashMap<>();
        java.util.Set<UUID> owners = new java.util.LinkedHashSet<>();
        owners.addAll(settings.keySet());
        owners.addAll(members.keySet());
        owners.addAll(perms.keySet());
        for (UUID owner : owners) {
            out.put(owner, new IslandBundle(
                    owner,
                    settings.getOrDefault(owner, IslandTerritoryState.VisitMode.PUBLIC),
                    members.getOrDefault(owner, List.of()),
                    perms.getOrDefault(owner, Map.of())
            ));
        }
        return out;
    }

    private IslandTerritoryState.VisitMode parseVisit(String s) {
        try { return IslandTerritoryState.VisitMode.valueOf(s); }
        catch (Exception e) { return IslandTerritoryState.VisitMode.PUBLIC; }
    }
    private IslandTerritoryState.Role parseRole(String s) {
        try { return IslandTerritoryState.Role.valueOf(s); }
        catch (Exception e) { return IslandTerritoryState.Role.RESIDENT; }
    }

    private Connection openConn() throws Exception {
        Result<Connection> r = connectionProvider.getConnection();
        if (r.isFailure()) throw new Exception("DB connection failed: " + r.message());
        return r.value();
    }
}
