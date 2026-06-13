package kr.zenon.rpg.storage;

import kr.zenon.rpg.combat.weapon.WeaponType;

import java.util.Objects;
import java.util.UUID;

public class PlayerData {
    private final UUID uuid;
    private WeaponType weaponType;
    private int reputation;
    private boolean tutorialComplete;
    /** 필드 정예 모드 — ON이면 이 플레이어 주변 웨이브가 정예 몹으로 스폰(수 적게·강함). 세션 메모리(재접속 시 OFF). */
    private boolean fieldEliteMode;

    public PlayerData(UUID uuid) {
        this.uuid = Objects.requireNonNull(uuid, "uuid");
        this.weaponType = WeaponType.NONE;
        this.reputation = 0;
        this.tutorialComplete = false;
        this.fieldEliteMode = false;
    }

    public boolean isFieldEliteMode() {
        return fieldEliteMode;
    }

    public void setFieldEliteMode(boolean fieldEliteMode) {
        this.fieldEliteMode = fieldEliteMode;
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
