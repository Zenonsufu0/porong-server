package com.poro.rpg.listener;

import com.poro.rpg.combat.SkillContext;
import com.poro.rpg.combat.SkillService;
import com.poro.rpg.combat.hitbox.SkillHitboxHelper;
import com.poro.rpg.combat.weapon.WeaponType;
import com.poro.rpg.combat.weapon.WeaponTypeResolver;
import com.poro.rpg.field.SafeZoneService;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerAnimationType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SkillInputListener implements Listener {
    private static final long SWING_WINDOW_MS = 200;

    private final SkillService skillService;
    private final SafeZoneService safeZoneService;
    private final SkillContext skillContext;
    /** 기본기 쿨다운 중 좌클릭 평타 계수 — weaponPower × 0.4 (무기 ATK·강화·잠재 반영, 바닐라 재질 데미지 대체). */
    private static final double BASIC_ATTACK_COEFF = 0.4d;
    /** 이번 스윙에서 이미 스킬을 발동한 플레이어 UUID → 발동 시각(ms). */
    private final Map<UUID, Long> swingFiredAt = new ConcurrentHashMap<>();

    public SkillInputListener(SkillService skillService, SafeZoneService safeZoneService, SkillContext skillContext) {
        this.skillService = skillService;
        this.safeZoneService = safeZoneService;
        this.skillContext = skillContext;
    }

    /** LMB 허공 스윙 → slot1 기본기 (논타겟). 엔티티 타격 시 onAttack과 중복 방지. */
    @EventHandler
    public void onSwing(PlayerAnimationEvent event) {
        if (event.getAnimationType() != PlayerAnimationType.ARM_SWING) return;
        Player player = event.getPlayer();
        if (safeZoneService.isSafeZone(player.getLocation())) return;
        WeaponType type = WeaponTypeResolver.resolve(player);
        String key = slot1Key(type);
        if (key == null) return;
        if (skillService.useSkill(player, key)) {
            swingFiredAt.put(player.getUniqueId(), System.currentTimeMillis());
        } else if (isRanged(type)) {
            // 원거리 무기(스태프·석궁)는 slot1 쿨다운 중 좌클릭 시 투사체 평타 발사 (근접 무기는 onAttack 멜리 평타).
            rangedBasicAttack(player, type);
            swingFiredAt.put(player.getUniqueId(), System.currentTimeMillis()); // 근접 시 onAttack 중복 방지
        }
    }

    private static boolean isRanged(WeaponType t) {
        return t == WeaponType.STAFF || t == WeaponType.CROSSBOW;
    }

    /** 원거리 평타 — 시선 방향 레이캐스트 투사체, weaponPower × 0.4 피해(killer 귀속). */
    private void rangedBasicAttack(Player player, WeaponType type) {
        double dmg = skillContext.weaponPower(player) * BASIC_ATTACK_COEFF;
        spawnBasicBeam(player, type);
        playBasicSound(player, type);
        SkillHitboxHelper.projectileRaycast(player, 20.0, 0.6).ifPresent(target -> {
            com.poro.rpg.combat.SkillDamageGuard.run(() -> target.damage(dmg, player));
            if (skillContext.damageNumber() != null) {
                skillContext.damageNumber().addDamage(player, target, dmg,
                        com.poro.rpg.combat.DamageNumberService.Type.NORMAL_DAMAGE);
            }
        });
    }

    private void spawnBasicBeam(Player player, WeaponType type) {
        Vector dir = player.getLocation().getDirection().normalize();
        var eye = player.getEyeLocation();
        var world = player.getWorld();
        Particle particle = (type == WeaponType.CROSSBOW) ? Particle.CRIT : Particle.WITCH;
        for (double d = 1.0; d <= 20.0; d += 1.0) {
            world.spawnParticle(particle, eye.clone().add(dir.clone().multiply(d)), 1, 0, 0, 0, 0);
        }
    }

    private void playBasicSound(Player player, WeaponType type) {
        Sound sound = (type == WeaponType.CROSSBOW) ? Sound.ENTITY_ARROW_SHOOT : Sound.ENTITY_BLAZE_SHOOT;
        player.getWorld().playSound(player.getLocation(), sound, 0.7f, 1.4f);
    }

    /**
     * LMB 엔티티 타격 → slot1 기본기.
     * 같은 스윙에서 onSwing이 이미 발동했으면 바닐라 공격만 취소.
     */
    @EventHandler
    public void onAttack(EntityDamageByEntityEvent event) {
        if (com.poro.rpg.combat.SkillDamageGuard.isApplying()) return; // 스킬 데미지 재귀 — 평타 덮어쓰기 방지
        if (!(event.getDamager() instanceof Player player)) return;
        if (safeZoneService.isSafeZone(player.getLocation())) return;

        Long swungAt = swingFiredAt.remove(player.getUniqueId());
        if (swungAt != null && System.currentTimeMillis() - swungAt < SWING_WINDOW_MS) {
            // onSwing에서 이미 발동 — 바닐라 공격만 취소
            event.setCancelled(true);
            return;
        }

        String key = slot1Key(WeaponTypeResolver.resolve(player));
        if (key == null) return; // 무기 미장착 — 바닐라 유지
        if (skillService.useSkill(player, key)) {
            event.setCancelled(true); // 기본기 발동
            return;
        }
        // 기본기 쿨다운 중 — 바닐라(재질 기본) 대신 ATK 기반 평타로 데미지 교체(무기 ATK·강화·잠재 반영).
        double basicDmg = skillContext.weaponPower(player) * BASIC_ATTACK_COEFF;
        event.setDamage(basicDmg);
        if (skillContext.damageNumber() != null) {
            skillContext.damageNumber().addDamage(player, event.getEntity(), basicDmg,
                    com.poro.rpg.combat.DamageNumberService.Type.NORMAL_DAMAGE);
        }
    }

    /** RMB → slot2 이동기 / Shift+RMB → slot3 특수기 */
    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;
        if (event.useItemInHand() == Event.Result.DENY) return;

        Player player = event.getPlayer();
        if (safeZoneService.isSafeZone(player.getLocation())) return;
        WeaponType type = WeaponTypeResolver.resolve(player);
        String key = player.isSneaking() ? slot3Key(type) : slot2Key(type);
        if (key == null) return;

        skillService.useSkill(player, key);
        // 쿨다운 여부와 무관하게 RMB 블록 상호작용은 항상 차단
        event.setUseInteractedBlock(Event.Result.DENY);
        event.setCancelled(true);
    }

    /** F키 → slot4 핵심기 */
    @EventHandler
    public void onSwap(PlayerSwapHandItemsEvent event) {
        if (safeZoneService.isSafeZone(event.getPlayer().getLocation())) return;
        String key = slot4Key(WeaponTypeResolver.resolve(event.getPlayer()));
        if (key == null) return;
        event.setCancelled(true);
        skillService.useSkill(event.getPlayer(), key);
    }

    private String slot1Key(WeaponType t) {
        return switch (t) {
            case SWORD    -> "sword:flash_slash";
            case AXE      -> "axe:smash";
            case SPEAR    -> "spear:thrust";
            case CROSSBOW -> "crossbow:rapid_fire";
            case SCYTHE   -> "scythe:death_slash";
            case STAFF    -> "staff:arcane_orb";
            case NONE     -> null;
        };
    }

    private String slot2Key(WeaponType t) {
        return switch (t) {
            case SWORD    -> "sword:triple_strike";
            case AXE      -> "axe:crush_charge";
            case SPEAR    -> "spear:crescent";
            case CROSSBOW -> "crossbow:evade_fire";
            case SCYTHE   -> "scythe:shadow_spin";
            case STAFF    -> "staff:elemental_burst";
            case NONE     -> null;
        };
    }

    private String slot3Key(WeaponType t) {
        return switch (t) {
            case SWORD    -> "sword:guard_counter";
            case AXE      -> "axe:unyielding";
            case SPEAR    -> "spear:charge";
            case CROSSBOW -> "crossbow:pierce_bolt";
            case SCYTHE   -> "scythe:grim_strike";
            case STAFF    -> "staff:arcane_rush";
            case NONE     -> null;
        };
    }

    private String slot4Key(WeaponType t) {
        return switch (t) {
            case SWORD    -> "sword:final_strike";
            case AXE      -> "axe:colossal_drop";
            case SPEAR    -> "spear:thunderstrike";
            case CROSSBOW -> "crossbow:sniper";
            case SCYTHE   -> "scythe:execution";
            case STAFF    -> "staff:starburst";
            case NONE     -> null;
        };
    }
}
