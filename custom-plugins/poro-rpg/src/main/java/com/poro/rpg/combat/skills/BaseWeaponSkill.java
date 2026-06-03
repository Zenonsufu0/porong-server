package com.poro.rpg.combat.skills;

import com.poro.rpg.combat.DamageNumberService;
import com.poro.rpg.combat.SkillContext;
import com.poro.rpg.combat.WeaponSkill;
import com.poro.rpg.combat.weapon.WeaponType;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public abstract class BaseWeaponSkill implements WeaponSkill {
    private final String key;
    private final String displayName;
    private final WeaponType weaponType;
    private final long cooldown;

    protected BaseWeaponSkill(String key, String displayName, WeaponType weaponType, long cooldown) {
        this.key         = key;
        this.displayName = displayName;
        this.weaponType  = weaponType;
        this.cooldown    = cooldown;
    }

    @Override public final String key()         { return key; }
    @Override public final String displayName()  { return displayName; }
    @Override public final WeaponType weaponType(){ return weaponType; }
    @Override public final long cooldown()       { return cooldown; }

    // --- damage ---

    protected double scaledDamage(SkillContext ctx, Player player, double coeff) {
        return ctx.weaponPower(player) * coeff;
    }

    protected double scaledDamageWithStacks(SkillContext ctx, Player player,
                                             double baseCoeff, double stackBonusPct) {
        int stacks = getStacks(ctx, player);
        return ctx.weaponPower(player) * (baseCoeff + stackBonusPct * stacks);
    }

    /**
     * 만충 스파이크 데미지 (DL-124). 핵심기 전용.
     * 누진(base + perStack×stacks)에 더해, <b>소모형이 만충(stacks ≥ 3)일 때만</b> 디스크리트 배율 fullChargeMult를 곱한다.
     * 비만충(부분 스택)이면 배율 미적용 → "끝까지 채워야 터지는" 충전형 폭발 질감.
     * 유지형 각인(*_retained_01)은 cap 6 + F 소모 없음으로 만충이 상시이므로 <b>배율을 적용하지 않는다</b>
     * (누진만 받음. 소모형=폭발 / 유지형=지속 정체성 분리, dps_balance_pass_v2 §D-3).
     * per-stack은 누진 절반으로 재배분돼 있어 cap6 누진 폭주도 구조적으로 둔화된다.
     */
    protected double scaledDamageFullChargeSpike(SkillContext ctx, Player player,
                                                  double baseCoeff, double perStack, double fullChargeMult) {
        int stacks = getStacks(ctx, player);
        boolean retained = ctx.playerState(player).classEngravingId().endsWith("_retained_01");
        double coeff = baseCoeff + perStack * stacks;
        if (!retained && stacks >= 3) coeff *= fullChargeMult;   // 유지형 제외, 소모형 만충(3)에서만 폭발
        return ctx.weaponPower(player) * coeff;
    }

    /**
     * 스킬 피해 적용 (DL-092/096/128#14). rawDamage(=ATK×계수)에 스킬피해%·보스피해%·치명·DEF경감을 곱해 적용.
     * ATK의 attack_percent는 weaponPower에 이미 반영. boss_damage_increase는 보스 대상일 때만.
     * DEF 경감: 대상 PDC의 DEF로 200/(200+유효DEF), 유효DEF=DEF×(1−방어력무시%). DEF 미기록 몹은 1.0(영향 없음).
     */
    protected void dealDamage(SkillContext ctx, Player attacker, LivingEntity target, double rawDamage) {
        double dmg = rawDamage
                * ctx.generalDamageMultiplier(attacker)
                * ctx.bossDamageMultiplier(attacker, target)
                * ctx.skillTypeMultiplier(attacker, com.poro.rpg.combat.SkillType.fromKey(key()))
                * ctx.defenseMitigation(attacker, target);
        boolean crit = java.util.concurrent.ThreadLocalRandom.current().nextDouble() < ctx.critChance(attacker);
        if (crit) dmg *= ctx.critDamageMultiplier(attacker);
        double finalDmg = Math.max(0.01d, dmg);
        // 스킬 데미지가 재발생시키는 EntityDamageByEntityEvent를 평타 리스너가 덮어쓰지 않도록 가드.
        com.poro.rpg.combat.SkillDamageGuard.run(() -> target.damage(finalDmg, attacker));
        if (ctx.damageNumber() != null) {
            ctx.damageNumber().addDamage(attacker, target, finalDmg,
                    crit ? DamageNumberService.Type.CRITICAL_DAMAGE : DamageNumberService.Type.SKILL_DAMAGE);
        }
    }

    // --- stacks ---

    protected int getStacks(SkillContext ctx, Player player) {
        return ctx.getResourceTracker().getStack(player.getUniqueId());
    }

    protected void gainStack(SkillContext ctx, Player player, int baseMax) {
        // 유지형 각인(*_retained_01)이면 무기 공통 최대 6스택 (weapon_skills_v1.md §자원 시스템)
        String engId = ctx.playerState(player).classEngravingId();
        int max = engId.endsWith("_retained_01") ? 6 : baseMax;
        ctx.getResourceTracker().incrementStack(player.getUniqueId(), max);
    }

    protected void consumeStacks(SkillContext ctx, Player player) {
        ctx.getResourceTracker().resetStack(player.getUniqueId());
    }

    // --- movement ---

    protected void dashForward(Player player, double blocks) {
        Vector dir = player.getLocation().getDirection().setY(0);
        if (dir.lengthSquared() < 0.001) return;
        player.setVelocity(dir.normalize().multiply(blocks * 0.6));
    }

    protected void dashBackward(Player player, double blocks) {
        Vector dir = player.getLocation().getDirection().setY(0);
        if (dir.lengthSquared() < 0.001) return;
        player.setVelocity(dir.normalize().multiply(-blocks * 0.6));
    }

    protected void dashSideways(Player player, double blocks) {
        Vector facing = player.getLocation().getDirection().setY(0);
        if (facing.lengthSquared() < 0.001) return;
        Vector right = new Vector(-facing.normalize().getZ(), 0, facing.getX()).normalize();
        player.setVelocity(right.multiply(blocks * 0.6));
    }

    /**
     * 플레이어의 현재 이동 속도 벡터 방향으로 대시한다.
     * 정지 중일 때는 시선 기준 우측(기본 측면)으로 대시한다.
     */
    protected void dashInInputDirection(Player player, double blocks) {
        Vector vel = player.getVelocity().setY(0);
        Vector dir;
        if (vel.lengthSquared() > 0.01) {
            dir = vel.normalize();
        } else {
            // 이동 입력 없음 → 시선 기준 우측
            Vector facing = player.getLocation().getDirection().setY(0);
            if (facing.lengthSquared() < 0.001) return;
            dir = new Vector(-facing.normalize().getZ(), 0, facing.getX()).normalize();
        }
        player.setVelocity(dir.multiply(blocks * 0.6));
    }

    // --- effects ---

    /** 플레이어 위치 기준 사운드 재생. */
    protected void playSound(Player player, Sound sound, float volume, float pitch) {
        player.getWorld().playSound(player.getLocation(), sound, volume, pitch);
    }

    /**
     * 플레이어 전방 호(arc) 방향으로 파티클을 배치한다.
     * @param radius  호의 반지름
     * @param angleDeg 호의 전체 각도 (예: 150도 전방 부채꼴)
     * @param count   파티클 개수
     * @param data    Particle.DustOptions 등, null이면 데이터 없는 파티클로 처리
     */
    protected void spawnParticleArc(Player player, Particle particle, Object data,
                                     double radius, double angleDeg, int count) {
        double yaw = Math.toRadians(player.getLocation().getYaw());
        double halfArc = Math.toRadians(angleDeg / 2.0);
        Location base = player.getLocation().add(0, 1.0, 0);
        World world = player.getWorld();
        for (int i = 0; i < count; i++) {
            double angle = count > 1 ? -halfArc + halfArc * 2.0 * i / (count - 1) : 0;
            double x = -Math.sin(yaw + angle) * radius;
            double z =  Math.cos(yaw + angle) * radius;
            spawnAt(world, particle, base.clone().add(x, 0, z), data);
        }
    }

    /**
     * 플레이어 주변 원형으로 파티클을 배치한다.
     * @param radius 원의 반지름
     * @param count  파티클 개수
     * @param data   Particle.DustOptions 등, null이면 데이터 없는 파티클로 처리
     */
    protected void spawnParticleCircle(Player player, Particle particle, Object data,
                                        double radius, int count) {
        Location base = player.getLocation().add(0, 0.5, 0);
        World world = player.getWorld();
        for (int i = 0; i < count; i++) {
            double angle = 2 * Math.PI * i / count;
            spawnAt(world, particle, base.clone().add(
                    Math.cos(angle) * radius, 0, Math.sin(angle) * radius), data);
        }
    }

    /**
     * 플레이어 전방으로 직선 파티클을 배치한다.
     * @param length 직선 길이
     * @param count  파티클 개수
     * @param data   Particle.DustOptions 등, null이면 데이터 없는 파티클로 처리
     */
    protected void spawnParticleLine(Player player, Particle particle, Object data,
                                      double length, int count) {
        double yaw = Math.toRadians(player.getLocation().getYaw());
        double dirX = -Math.sin(yaw);
        double dirZ =  Math.cos(yaw);
        Location start = player.getLocation().add(0, 1.0, 0);
        World world = player.getWorld();
        for (int i = 0; i < count; i++) {
            double t = count > 1 ? length * i / (count - 1) : 0;
            spawnAt(world, particle, start.clone().add(dirX * t, 0, dirZ * t), data);
        }
    }

    /** 전방 slash 이펙트: 외호 + 내호 2겹 arc. */
    protected void spawnSlashEffect(Player player, Particle particle, Object data, double radius) {
        spawnParticleArc(player, particle, data, radius,       150, 12);
        spawnParticleArc(player, particle, data, radius * 0.5, 150,  8);
    }

    /** 특정 위치에 폭발형 임팩트 파티클을 뿌린다. world가 null이면 무시한다. */
    protected void spawnImpactEffect(Location loc, Particle particle, Object data, int count) {
        World world = loc.getWorld();
        if (world == null) return;
        spawnAt(world, particle, loc, data, count, 0.2, 0.2, 0.2, 0.05);
    }

    /**
     * 시선 방향(상하 포함) 빔 파티클 — 원거리 스킬 궤적용. 솔리드 블록에서 멈춘다.
     * {@code projectileRaycast}와 동일하게 eyeLocation + 시선 벡터를 따라간다.
     * @param range 빔 최대 길이
     * @param step  파티클 간격 (작을수록 촘촘)
     */
    protected void spawnBeam(Player player, Particle particle, Object data, double range, double step) {
        Vector dir = player.getEyeLocation().getDirection().normalize();
        Location eye = player.getEyeLocation();
        World world = player.getWorld();
        for (double d = 0.5; d <= range; d += step) {
            Location p = eye.clone().add(dir.clone().multiply(d));
            if (p.getBlock().getType().isSolid()) break;
            spawnAt(world, particle, p, data);
        }
    }

    private void spawnAt(World world, Particle particle, Location loc, Object data) {
        if (data != null) {
            world.spawnParticle(particle, loc, 1, 0, 0, 0, 0, data);
        } else {
            world.spawnParticle(particle, loc, 1, 0, 0, 0, 0);
        }
    }

    private void spawnAt(World world, Particle particle, Location loc,
                          Object data, int count, double dx, double dy, double dz, double speed) {
        if (data != null) {
            world.spawnParticle(particle, loc, count, dx, dy, dz, speed, data);
        } else {
            world.spawnParticle(particle, loc, count, dx, dy, dz, speed);
        }
    }

    // --- utility ---

    protected void lifesteal(SkillContext ctx, Player player, double amount) {
        var maxHpAttr = player.getAttribute(Attribute.MAX_HEALTH);
        if (maxHpAttr == null) return;
        double before = player.getHealth();
        player.setHealth(Math.min(maxHpAttr.getValue(), before + amount));
        double healed = player.getHealth() - before; // 실제 회복량(최대 HP 캡 반영)
        if (healed > 0 && ctx.damageNumber() != null) {
            ctx.damageNumber().addHeal(player, healed, DamageNumberService.Type.LIFESTEAL);
        }
    }
}
