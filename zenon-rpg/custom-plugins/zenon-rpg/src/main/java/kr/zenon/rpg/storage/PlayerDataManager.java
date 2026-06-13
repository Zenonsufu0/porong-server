package kr.zenon.rpg.storage;

import kr.zenon.rpg.combat.weapon.WeaponType;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerDataManager {
    private final Map<UUID, PlayerData> playerDataMap = new ConcurrentHashMap<>();

    public PlayerData getOrCreate(UUID uuid) {
        return playerDataMap.computeIfAbsent(uuid, PlayerData::new);
    }

    public PlayerData getOrCreate(Player player) {
        return getOrCreate(player.getUniqueId());
    }

    public Optional<PlayerData> find(UUID uuid) {
        return Optional.ofNullable(playerDataMap.get(uuid));
    }

    public Optional<PlayerData> find(Player player) {
        return find(player.getUniqueId());
    }

    public void onPlayerJoin(UUID uuid) {
        getOrCreate(uuid);
    }

    public void onPlayerJoin(Player player) {
        onPlayerJoin(player.getUniqueId());
    }

    public void onPlayerQuit(UUID uuid) {
        playerDataMap.remove(uuid);
    }

    public void onPlayerQuit(Player player) {
        onPlayerQuit(player.getUniqueId());
    }

    public boolean hasSelectedWeapon(UUID uuid) {
        return getOrCreate(uuid).hasSelectedWeapon();
    }

    public boolean hasSelectedWeapon(Player player) {
        return hasSelectedWeapon(player.getUniqueId());
    }

    public void setWeaponType(UUID uuid, WeaponType weaponType) {
        getOrCreate(uuid).setWeaponType(weaponType);
    }

    public void setWeaponType(Player player, WeaponType weaponType) {
        setWeaponType(player.getUniqueId(), weaponType);
    }

    public WeaponType getWeaponType(UUID uuid) {
        return getOrCreate(uuid).getWeaponType();
    }

    public WeaponType getWeaponType(Player player) {
        return getWeaponType(player.getUniqueId());
    }

    /** 필드 정예 모드 여부 (FieldSpawnService eliteMode predicate 백킹). */
    public boolean isFieldElite(UUID uuid) {
        return find(uuid).map(PlayerData::isFieldEliteMode).orElse(false);
    }

    /** 필드 정예 모드 토글 — 변경 후 값 반환. */
    public boolean toggleFieldElite(UUID uuid) {
        PlayerData data = getOrCreate(uuid);
        data.setFieldEliteMode(!data.isFieldEliteMode());
        return data.isFieldEliteMode();
    }

    /** 필드 정예 모드 명시 설정. */
    public void setFieldElite(UUID uuid, boolean on) {
        getOrCreate(uuid).setFieldEliteMode(on);
    }

    public void clear() {
        playerDataMap.clear();
    }

    public int size() {
        return playerDataMap.size();
    }
}
