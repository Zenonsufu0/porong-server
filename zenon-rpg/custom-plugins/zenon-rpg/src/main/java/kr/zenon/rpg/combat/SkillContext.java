package kr.zenon.rpg.combat;

import kr.zenon.rpg.boss.room.BossDamageTracker;
import kr.zenon.rpg.combat.effect.EffectDisplayService;
import kr.zenon.rpg.combat.weapon.WeaponType;
import kr.zenon.rpg.common.registry.master.ItemMasterRegistry;
import kr.zenon.rpg.growth.GrowthStateStore;
import kr.zenon.rpg.growth.engine.EquipmentSlot;
import kr.zenon.rpg.growth.engine.PlayerEquipmentItem;
import kr.zenon.rpg.growth.engine.PlayerGrowthState;
import kr.zenon.rpg.growth.engine.PotentialLine;
import kr.zenon.rpg.growth.engine.PotentialService;
import kr.zenon.rpg.listener.MobTagHelper;
import kr.zenon.rpg.storage.PlayerDataManager;
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

    // 방어구 부위별 기본 HP (DL-128#12 상향: 강화 체감 확보 + 보스 데미지 역산 기준 마련).
    // 기본HP 합 180, 강화계수 0.11(DEF와 분리). 목표: +0≈200, +8≈358, +18≈556.
    // 비율 투구>상의>신발 유지, 다리는 DEF 전담이라 소량.
    private static final Map<EquipmentSlot, Double> BASE_HP = Map.of(
            EquipmentSlot.HELMET, 90.0d,
            EquipmentSlot.CHESTPLATE, 50.0d,
            EquipmentSlot.LEGGINGS, 20.0d,
            EquipmentSlot.BOOTS, 20.0d
    );

    /** 강화 스탯 선형 배율(DEF용) — 기본 스탯 × (1 + 0.04 × 강화단계) (combat_balance_v2 §1). */
    private static double enhanceMultiplier(int level) {
        return 1.0d + 0.04d * Math.max(0, level);
    }

    /** HP 전용 강화 배율 — 기본 HP × (1 + 0.11 × 강화단계). DEF(0.04)와 분리해 강화 체감을 키운다(DL-128#12). */
    private static double hpEnhanceMultiplier(int level) {
        return 1.0d + 0.11d * Math.max(0, level);
    }

    // ── lore 표시용 공개 접근자 (단일 출처 — 위 BASE_DEF/BASE_HP·배율과 동일 산식) ──
    /** 방어구 부위가 현재 강화단계에서 제공하는 DEF(베이스×(1+0.04×강화)). 무기/미정의 슬롯=0. */
    public static double armorDefAt(EquipmentSlot slot, int level) {
        return BASE_DEF.getOrDefault(slot, 0.0d) * enhanceMultiplier(level);
    }
    /** 방어구 부위가 현재 강화단계에서 제공하는 최대 HP(베이스×(1+0.11×강화)). 무기/미정의 슬롯=0. */
    public static double armorHpAt(EquipmentSlot slot, int level) {
        return BASE_HP.getOrDefault(slot, 0.0d) * hpEnhanceMultiplier(level);
    }
    /** 다음 강화(+1)로 늘어나는 DEF 증가분. */
    public static double armorDefGainNext(EquipmentSlot slot, int level) {
        return armorDefAt(slot, level + 1) - armorDefAt(slot, level);
    }
    /** 다음 강화(+1)로 늘어나는 HP 증가분. */
    public static double armorHpGainNext(EquipmentSlot slot, int level) {
        return armorHpAt(slot, level + 1) - armorHpAt(slot, level);
    }

    /** 장착 방어구가 제공하는 최대 HP 보너스 — Σ 기본 HP × (1+0.11×강화). max health attribute에 가산한다. */
    public double armorMaxHealth(Player player) {
        PlayerGrowthState state = playerState(player);
        double sum = 0.0d;
        for (Map.Entry<EquipmentSlot, String> e : state.equippedItems().entrySet()) {
            Double base = BASE_HP.get(e.getKey());
            if (base == null || base <= 0) continue;
            PlayerEquipmentItem item = state.inventoryItem(e.getValue()).orElse(null);
            if (item == null) continue;
            sum += base * hpEnhanceMultiplier(item.enhanceLevel());
        }
        return sum;
    }

    /**
     * 방어구 HP를 max health에 반영(기본 20 + 방어구 HP) × 잠재 HP%. 접속·강화·장착·잠재 변경 시 호출.
     * 잠재 max_hp_percent는 (기본 20 + 방어구 HP) 전체에 곱산한다(정본 "최대 HP % 증가", combat_balance_v2 §1 / DL-129).
     */
    public void applyMaxHealth(Player player) {
        AttributeInstance attr = player.getAttribute(Attribute.MAX_HEALTH);
        if (attr == null) return;
        double potHpPct = sumEquippedPotential(player, "max_hp_percent") / 100.0d;
        double target = (20.0d + armorMaxHealth(player)) * (1.0d + potHpPct);
        attr.setBaseValue(target);
        if (player.getHealth() > target) player.setHealth(target);
    }

    // 잠재 이동 속도% 적용용 — 바닐라 기본 이동 속도 attribute 베이스값(정본 캡 +13% 내, +40% 안전 클램프).
    private static final double BASE_MOVE_SPEED = 0.1d;

    /** 잠재 move_speed_percent를 MOVEMENT_SPEED attribute에 반영. 접속·장착·잠재 변경 시 호출 (DL-129). */
    public void applyMoveSpeed(Player player) {
        AttributeInstance attr = player.getAttribute(Attribute.MOVEMENT_SPEED);
        if (attr == null) return;
        double pct = Math.min(40.0d, sumEquippedPotential(player, "move_speed_percent"));
        attr.setBaseValue(BASE_MOVE_SPEED * (1.0d + pct / 100.0d));
    }

    /** 잠재 의존 attribute(최대 HP·이동 속도)를 일괄 재적용한다. 접속·강화·장착·큐브 사용 후 호출. */
    public void applyDerivedAttributes(Player player) {
        applyMaxHealth(player);
        applyMoveSpeed(player);
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
        // 잠재 defense_percent — (방어구 베이스+강화+인내) 총 DEF에 곱산 (정본 "방어력 % 증가", DL-129).
        sum *= 1.0d + sumEquippedPotential(player, "defense_percent") / 100.0d;
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

    /**
     * 스킬타입 피해 승수 — 발동 스킬의 입력 유형(기본/이동/특수/핵심)에 해당하는 잠재%만 적용 (DL-129 2단계).
     * type이 null(평타·미등록 스킬)이면 1.0.
     */
    public double skillTypeMultiplier(Player player, SkillType type) {
        if (type == null) return 1.0d;
        return 1.0d + sumEquippedPotential(player, type.optionCode()) / 100.0d;
    }

    /** 잠재 쿨타임 감소(cooldown_reduction_percent) 합산(%). 0~50 클램프 — 쿨 0 방지 (DL-129 2단계). */
    public double cooldownReductionPercent(Player player) {
        return Math.min(50.0d, sumEquippedPotential(player, "cooldown_reduction_percent"));
    }

    /** 치명타 확률 = 치명 트리(0.30%/pt) + 잠재 crit_chance_percent. 0~1 클램프 (DL-092/095/129). */
    public double critChance(Player player) {
        double tree = Math.max(0, playerState(player).critPts()) * 0.003d;
        double potential = sumEquippedPotential(player, "crit_chance_percent") / 100.0d;
        return Math.min(1.0d, tree + potential);
    }

    /** 치명타 피해 배율 = 1.5 + 치명 트리(0.15%/pt) + 잠재 crit_damage_percent (DL-096/129, level_stat_system §2). */
    public double critDamageMultiplier(Player player) {
        double potential = sumEquippedPotential(player, "crit_damage_percent") / 100.0d;
        return 1.5d + Math.max(0, playerState(player).critPts()) * 0.0015d + potential;
    }

    /** 잠재 받는 피해 감소(damage_reduction) 합산(%). 0~80 클램프 — 무적 방지 (DL-129, 유니크+ 전용 옵션). */
    public double damageReductionPercent(Player player) {
        return Math.min(80.0d, sumEquippedPotential(player, "damage_reduction"));
    }

    // ─── 보스/몹 DEF 경감 + 방어력무시 (DL-128#14) ─────────────────────────
    /** 몹/보스 DEF를 스폰 시 기록하는 PDC 키 (MobStatOverrideSpawnListener가 set). */
    public static final org.bukkit.NamespacedKey MOB_DEF_KEY =
            org.bukkit.NamespacedKey.fromString("zenon_rpg:mob_def");

    /** 대상의 DEF — 스폰 시 PDC에 기록된 값(없으면 0 = 경감 없음). */
    public double targetDefense(LivingEntity target) {
        if (target == null) return 0.0d;
        Double d = target.getPersistentDataContainer()
                .get(MOB_DEF_KEY, org.bukkit.persistence.PersistentDataType.DOUBLE);
        return d == null ? 0.0d : Math.max(0.0d, d);
    }

    /** 무기 잠재 방어력무시(%) 합산 — 대상 DEF를 이만큼 무시. 0~100 클램프 (DL-128#14). */
    public double defenseIgnorePercent(Player player) {
        return Math.min(100.0d, sumEquippedPotential(player, "defense_ignore"));
    }

    /**
     * 플레이어→대상 피해의 DEF 경감 배율 = 200/(200+유효DEF).
     * 유효DEF = 대상DEF × (1 − 방어력무시%/100). DEF 0이면 1.0(경감 없음).
     */
    public double defenseMitigation(Player attacker, LivingEntity target) {
        double def = targetDefense(target);
        if (def <= 0) return 1.0d;
        double effDef = def * (1.0d - defenseIgnorePercent(attacker) / 100.0d);
        return 200.0d / (200.0d + Math.max(0.0d, effDef));
    }
}
