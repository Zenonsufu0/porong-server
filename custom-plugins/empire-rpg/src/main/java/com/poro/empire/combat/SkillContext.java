package com.poro.empire.combat;

import com.poro.empire.boss.room.BossDamageTracker;
import com.poro.empire.combat.weapon.WeaponType;
import com.poro.empire.common.registry.master.ItemMasterRegistry;
import com.poro.empire.growth.GrowthStateStore;
import com.poro.empire.growth.engine.PlayerEquipmentItem;
import com.poro.empire.growth.engine.PlayerGrowthState;
import com.poro.empire.growth.engine.PotentialLine;
import com.poro.empire.growth.engine.PotentialService;
import com.poro.empire.listener.MobTagHelper;
import com.poro.empire.storage.PlayerDataManager;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class SkillContext {
    private final PlayerDataManager playerDataManager;
    private final CooldownManager cooldownManager;
    private final ResourceTracker resourceTracker;
    private final GrowthStateStore growthStateStore;
    private final ItemMasterRegistry itemMasterRegistry;
    private final PotentialService potentialService;
    private final BossDamageTracker bossDamageTracker;

    public SkillContext(PlayerDataManager playerDataManager,
                        CooldownManager cooldownManager,
                        ResourceTracker resourceTracker,
                        GrowthStateStore growthStateStore,
                        ItemMasterRegistry itemMasterRegistry,
                        PotentialService potentialService,
                        BossDamageTracker bossDamageTracker) {
        this.playerDataManager   = playerDataManager;
        this.cooldownManager     = cooldownManager;
        this.resourceTracker     = resourceTracker;
        this.growthStateStore    = growthStateStore;
        this.itemMasterRegistry  = itemMasterRegistry;
        this.potentialService    = potentialService;
        this.bossDamageTracker   = bossDamageTracker;
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

    /** 장착 장비 잠재에서 optionCode 값(%) 합산 (여분 아이템 제외). */
    private double sumEquippedPotential(Player player, String optionCode) {
        PlayerGrowthState state = playerState(player);
        double sum = 0.0d;
        for (String instanceId : state.equippedItems().values()) {
            PlayerEquipmentItem item = state.inventoryItem(instanceId).orElse(null);
            if (item == null || item.potentialProfile() == null) continue;
            for (PotentialLine line : item.potentialProfile().lines()) {
                if (optionCode.equals(line.optionCode())) sum += Math.max(0.0d, line.value());
            }
        }
        return sum;
    }

    /** 일반 피해증가(general_damage_increase) 승수 = 1 + Σ%/100 (DL-092). */
    public double generalDamageMultiplier(Player player) {
        return 1.0d + sumEquippedPotential(player, "general_damage_increase") / 100.0d;
    }

    /** 보스 콘텐츠 대상 여부 — 필드보스 태그 또는 인스턴스 보스(추적 중) (DL-096). */
    public boolean isBossTarget(LivingEntity target) {
        return target != null
                && (MobTagHelper.isFieldBoss(target) || bossDamageTracker.isTracked(target.getUniqueId()));
    }

    /** 보스 피해증가(boss_damage_increase) 승수 — 보스 대상일 때만 적용, 아니면 1.0 (DL-096). */
    public double bossDamageMultiplier(Player player, LivingEntity target) {
        if (!isBossTarget(target)) return 1.0d;
        return 1.0d + sumEquippedPotential(player, "boss_damage_increase") / 100.0d;
    }

    /** 치명타 확률 (치명 트리 주효과, 0.30%/pt). 0~1 클램프 (DL-092/095). */
    public double critChance(Player player) {
        return Math.min(1.0d, Math.max(0, playerState(player).critPts()) * 0.003d);
    }

    /** 치명타 피해 배율 = 기본 1.5 + 치명 트리 부효과(0.15%/pt) (DL-096, level_stat_system §2). */
    public double critDamageMultiplier(Player player) {
        return 1.5d + Math.max(0, playerState(player).critPts()) * 0.0015d;
    }
}
