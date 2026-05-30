package com.poro.empire.combat;

import com.poro.empire.combat.weapon.WeaponType;
import com.poro.empire.common.registry.master.ItemMasterRegistry;
import com.poro.empire.growth.GrowthStateStore;
import com.poro.empire.growth.engine.PlayerEquipmentItem;
import com.poro.empire.growth.engine.PlayerGrowthState;
import com.poro.empire.growth.engine.PotentialLine;
import com.poro.empire.growth.engine.PotentialService;
import com.poro.empire.storage.PlayerDataManager;
import org.bukkit.entity.Player;

public class SkillContext {
    private final PlayerDataManager playerDataManager;
    private final CooldownManager cooldownManager;
    private final ResourceTracker resourceTracker;
    private final GrowthStateStore growthStateStore;
    private final ItemMasterRegistry itemMasterRegistry;
    private final PotentialService potentialService;

    public SkillContext(PlayerDataManager playerDataManager,
                        CooldownManager cooldownManager,
                        ResourceTracker resourceTracker,
                        GrowthStateStore growthStateStore,
                        ItemMasterRegistry itemMasterRegistry,
                        PotentialService potentialService) {
        this.playerDataManager   = playerDataManager;
        this.cooldownManager     = cooldownManager;
        this.resourceTracker     = resourceTracker;
        this.growthStateStore    = growthStateStore;
        this.itemMasterRegistry  = itemMasterRegistry;
        this.potentialService    = potentialService;
    }

    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }

    public CooldownManager getCooldownManager() {
        return cooldownManager;
    }

    public ResourceTracker getResourceTracker() {
        return resourceTracker;
    }

    /** 플레이어의 현재 성장 상태를 반환한다. */
    public PlayerGrowthState playerState(Player player) {
        WeaponType wt = playerDataManager.getWeaponType(player.getUniqueId());
        return growthStateStore.getOrCreate(player.getUniqueId(), wt.name().toLowerCase());
    }

    /**
     * 플레이어의 현재 유효 무기 ATK를 반환한다.
     * 장비 없을 경우 T1 기본값(80) 반환.
     */
    public double weaponPower(Player player) {
        WeaponType wt = playerDataManager.getWeaponType(player.getUniqueId());
        PlayerGrowthState state = growthStateStore.getOrCreate(
                player.getUniqueId(), wt.name().toLowerCase());
        return WeaponPowerCalculator.calculate(state, itemMasterRegistry, potentialService);
    }

    /**
     * 장착 장비 잠재의 일반 피해증가(general_damage_increase) 합을 승수로 반환 (DL-092).
     * 1 + Σ(general_damage_increase%)/100. 장착분만 집계(여분 아이템 제외).
     */
    public double generalDamageMultiplier(Player player) {
        PlayerGrowthState state = playerState(player);
        double gdi = 0.0d;
        for (String instanceId : state.equippedItems().values()) {
            PlayerEquipmentItem item = state.inventoryItem(instanceId).orElse(null);
            if (item == null || item.potentialProfile() == null) continue;
            for (PotentialLine line : item.potentialProfile().lines()) {
                if ("general_damage_increase".equals(line.optionCode())) gdi += Math.max(0.0d, line.value());
            }
        }
        return 1.0d + gdi / 100.0d;
    }

    /** 치명타 확률 (스탯 배분 치명 트리, 0.3%/pt). 0~1 클램프 (DL-092). */
    public double critChance(Player player) {
        return Math.min(1.0d, Math.max(0, playerState(player).critPts()) * 0.003d);
    }
}
