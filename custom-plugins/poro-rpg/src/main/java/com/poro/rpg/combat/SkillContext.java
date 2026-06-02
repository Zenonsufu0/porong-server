package com.poro.rpg.combat;

import com.poro.rpg.boss.room.BossDamageTracker;
import com.poro.rpg.combat.effect.EffectDisplayService;
import com.poro.rpg.combat.weapon.WeaponType;
import com.poro.rpg.common.registry.master.ItemMasterRegistry;
import com.poro.rpg.growth.GrowthStateStore;
import com.poro.rpg.growth.engine.EquipmentSlot;
import com.poro.rpg.growth.engine.PlayerEquipmentItem;
import com.poro.rpg.growth.engine.PlayerGrowthState;
import com.poro.rpg.growth.engine.PotentialLine;
import com.poro.rpg.growth.engine.PotentialService;
import com.poro.rpg.listener.MobTagHelper;
import com.poro.rpg.storage.PlayerDataManager;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.Map;

public class SkillContext {
    private final PlayerDataManager playerDataManager;
    private final CooldownManager cooldownManager;
    private final ResourceTracker resourceTracker;
    private final GrowthStateStore growthStateStore;
    private final ItemMasterRegistry itemMasterRegistry;
    private final PotentialService potentialService;
    private final BossDamageTracker bossDamageTracker;
    private final EffectDisplayService effectDisplayService;
    private final DamageNumberService damageNumberService;

    public SkillContext(PlayerDataManager playerDataManager,
                        CooldownManager cooldownManager,
                        ResourceTracker resourceTracker,
                        GrowthStateStore growthStateStore,
                        ItemMasterRegistry itemMasterRegistry,
                        PotentialService potentialService,
                        BossDamageTracker bossDamageTracker,
                        EffectDisplayService effectDisplayService,
                        DamageNumberService damageNumberService) {
        this.playerDataManager   = playerDataManager;
        this.cooldownManager     = cooldownManager;
        this.resourceTracker     = resourceTracker;
        this.growthStateStore    = growthStateStore;
        this.itemMasterRegistry  = itemMasterRegistry;
        this.potentialService    = potentialService;
        this.bossDamageTracker   = bossDamageTracker;
        this.effectDisplayService = effectDisplayService;
        this.damageNumberService = damageNumberService;
    }

    /** 2D 스킬 이펙트 디스플레이 서비스 (skill_effect_2d_integration_v1). */
    public EffectDisplayService effectDisplay() {
        return effectDisplayService;
    }

    /** 데미지 숫자 표시 서비스. */
    public DamageNumberService damageNumber() {
        return damageNumberService;
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

    // 방어구 부위별 베이스 DEF 정본 (combat_balance_v2 §1, 합 52 → 0강 20.6% 경감).
    // ItemMaster에 DEF 전용 필드가 없어(투구 baseStatType=HP) 상수맵으로 보유 — 강화/잠재/인내는 2~3단계(DL-113).
    private static final Map<EquipmentSlot, Double> BASE_DEF = Map.of(
            EquipmentSlot.HELMET, 0.0d,
            EquipmentSlot.CHESTPLATE, 15.0d,
            EquipmentSlot.LEGGINGS, 30.0d,
            EquipmentSlot.BOOTS, 7.0d
    );

    // 방어구 부위별 기본 HP (DL-115 하향: 정본 §1의 400/150/100=650이 바닐라 몹 데미지 대비 과해 0강 풀세트 100 목표로 스케일 다운).
    // 비율(투구>상의>신발) 유지. 강화 HP = 기본 HP × 0.04 × 단계(DEF와 동일 선형). 0강 풀 80 + 기본 20 = 100.
    private static final Map<EquipmentSlot, Double> BASE_HP = Map.of(
            EquipmentSlot.HELMET, 50.0d,
            EquipmentSlot.CHESTPLATE, 20.0d,
            EquipmentSlot.LEGGINGS, 0.0d,
            EquipmentSlot.BOOTS, 10.0d
    );

    /** 강화 스탯 선형 배율 — 기본 스탯 × (1 + 0.04 × 강화단계) (combat_balance_v2 §1). */
    private static double enhanceMultiplier(int level) {
        return 1.0d + 0.04d * Math.max(0, level);
    }

    /** 장착 방어구가 제공하는 최대 HP 보너스 — Σ 기본 HP × (1+0.04×강화). max health attribute에 가산한다. */
    public double armorMaxHealth(Player player) {
        PlayerGrowthState state = playerState(player);
        double sum = 0.0d;
        for (Map.Entry<EquipmentSlot, String> e : state.equippedItems().entrySet()) {
            Double base = BASE_HP.get(e.getKey());
            if (base == null || base <= 0) continue;
            PlayerEquipmentItem item = state.inventoryItem(e.getValue()).orElse(null);
            if (item == null) continue;
            sum += base * enhanceMultiplier(item.enhanceLevel());
        }
        return sum;
    }

    /** 방어구 HP를 max health에 반영(기본 20 + 방어구 HP). 접속·강화·장착 변경 시 호출 (combat_balance_v2 §1). */
    public void applyMaxHealth(Player player) {
        AttributeInstance attr = player.getAttribute(Attribute.MAX_HEALTH);
        if (attr == null) return;
        double target = 20.0d + armorMaxHealth(player);
        attr.setBaseValue(target);
        if (player.getHealth() > target) player.setHealth(target);
    }

    /**
     * 플레이어 총 방어력(DEF) 정본 — 방어구 4부위 (베이스 DEF + 강화 보너스) + 인내트리(0.4/pt).
     * GUI 방어력 표시와 몹→플레이어 DEF/(DEF+200) 경감이 모두 이 값을 사용한다(표시=경감 일치).
     * 잠재 def%·곱산 상한은 후속 3단계(DL-116 예정).
     */
    public double defense(Player player) {
        PlayerGrowthState state = playerState(player);
        double sum = 0.0d;
        for (Map.Entry<EquipmentSlot, String> e : state.equippedItems().entrySet()) {
            Double base = BASE_DEF.get(e.getKey());
            if (base == null) continue; // 방어구 슬롯만
            PlayerEquipmentItem item = state.inventoryItem(e.getValue()).orElse(null);
            if (item == null) continue;
            sum += base * enhanceMultiplier(item.enhanceLevel()); // 정본 선형: 기본 DEF×(1+0.04×강화)
        }
        sum += Math.max(0, state.endurPts()) * 0.4d; // 인내 트리 방어력(코드 기존 계수)
        return sum;
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
