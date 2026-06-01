package com.poro.rpg.storage;

import com.poro.rpg.combat.weapon.WeaponType;

import java.util.Objects;
import java.util.UUID;

public class PlayerData {
    private final UUID uuid;
    private WeaponType weaponType;
    private int reputation;
    private boolean tutorialComplete;

    public PlayerData(UUID uuid) {
        this.uuid = Objects.requireNonNull(uuid, "uuid");
        this.weaponType = WeaponType.NONE;
        this.reputation = 0;
        this.tutorialComplete = false;
    }

    public UUID getUuid() {
        return uuid;
    }

    public WeaponType getWeaponType() {
        return weaponType;
    }

    public void setWeaponType(WeaponType weaponType) {
        this.weaponType = Objects.requireNonNull(weaponType, "weaponType");
    }

    public boolean hasSelectedWeapon() {
        return weaponType != WeaponType.NONE;
    }

    public int getReputation() {
        return reputation;
    }

    public void setReputation(int reputation) {
        this.reputation = Math.max(0, reputation);
    }

    public boolean isTutorialComplete() {
        return tutorialComplete;
    }

    public void setTutorialComplete(boolean tutorialComplete) {
        this.tutorialComplete = tutorialComplete;
    }
}
