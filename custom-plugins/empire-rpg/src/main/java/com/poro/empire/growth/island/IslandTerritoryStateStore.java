package com.poro.empire.growth.island;

import com.poro.empire.growth.island.db.IslandSettingsRepository;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class IslandTerritoryStateStore {
    private final Map<UUID, IslandTerritoryState> territories = new ConcurrentHashMap<>();
    private IslandSettingsRepository repository; // optional

    public IslandTerritoryState getOrCreate(UUID uuid) {
        return getOrCreate(uuid, "플레이어");
    }

    public IslandTerritoryState getOrCreate(UUID uuid, String playerName) {
        return territories.computeIfAbsent(uuid, key -> {
            IslandTerritoryState state = new IslandTerritoryState(playerName);
            applyBundle(uuid, state);
            return state;
        });
    }

    /** repository에서 단건 로드해 state에 적용 (재로그인 시 멤버/권한/visit 복원). */
    private void applyBundle(UUID uuid, IslandTerritoryState state) {
        if (repository == null) return;
        IslandSettingsRepository.IslandBundle bundle = repository.loadOne(uuid);
        if (bundle == null) return;
        state.setVisitMode(bundle.visitMode());
        for (var memberRow : bundle.members()) {
            state.addMember(memberRow.memberUuid(), memberRow.memberName(), memberRow.role());
        }
        bundle.permissions().forEach((role, mask) -> {
            int diff = mask ^ state.rolePermissionMask(role);
            for (var perm : IslandTerritoryState.Permission.values()) {
                if ((diff & perm.bit) != 0) state.togglePermission(role, perm);
            }
        });
    }

    public Optional<IslandTerritoryState> get(UUID uuid) {
        return Optional.ofNullable(territories.get(uuid));
    }

    public void put(UUID uuid, IslandTerritoryState state) {
        territories.put(uuid, state);
    }

    public void remove(UUID uuid) {
        territories.remove(uuid);
    }

    /** 모든 영지 (UUID → State) 스냅샷. */
    public Map<UUID, IslandTerritoryState> snapshot() {
        return Map.copyOf(territories);
    }

    // ─── 영속화 hook (IslandSettingsRepository) ─────────────────────

    public void attachRepository(IslandSettingsRepository repository) {
        this.repository = repository;
        if (repository == null) return;
        // DB 캐시 로드 — IslandTerritoryState 생성 후 visit/members/perms 적용
        repository.loadAll().forEach((owner, bundle) -> {
            IslandTerritoryState s = getOrCreate(owner);
            s.setVisitMode(bundle.visitMode());
            for (var memberRow : bundle.members()) {
                s.addMember(memberRow.memberUuid(), memberRow.memberName(), memberRow.role());
            }
            bundle.permissions().forEach((role, mask) -> {
                int diff = mask ^ s.rolePermissionMask(role);
                // bit-by-bit toggle until masks match
                for (var perm : IslandTerritoryState.Permission.values()) {
                    if ((diff & perm.bit) != 0) s.togglePermission(role, perm);
                }
            });
        });
    }

    public IslandSettingsRepository repository() {
        return repository;
    }

    /** 영지 설정 영속화 헬퍼 — owner UUID 기준으로 visit 저장. */
    public void persistVisitMode(UUID ownerUuid) {
        if (repository == null) return;
        IslandTerritoryState state = territories.get(ownerUuid);
        if (state != null) repository.saveSettings(ownerUuid, state.visitMode());
    }

    /** 영지 멤버 전체 영속화 (변경 후 일괄). */
    public void persistMembers(UUID ownerUuid) {
        if (repository == null) return;
        IslandTerritoryState state = territories.get(ownerUuid);
        if (state == null) return;
        Map<UUID, String> names = new java.util.HashMap<>();
        for (var e : state.memberList()) {
            String name = state.memberName(e.getKey());
            if (name != null) names.put(e.getKey(), name);
        }
        repository.saveMembers(ownerUuid, state.memberList(), names);
    }

    /** 등급별 권한 영속화. */
    public void persistPermissions(UUID ownerUuid, IslandTerritoryState.Role role) {
        if (repository == null) return;
        IslandTerritoryState state = territories.get(ownerUuid);
        if (state != null) repository.savePermissions(ownerUuid, role, state.rolePermissionMask(role));
    }
}
