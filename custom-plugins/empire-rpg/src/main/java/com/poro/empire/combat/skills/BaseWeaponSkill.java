package com.poro.empire.combat.skills;

import com.poro.empire.combat.SkillContext;
import com.poro.empire.combat.WeaponSkill;
import com.poro.empire.combat.weapon.WeaponType;
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

    protected void dealDamage(Player attacker, LivingEntity target, double damage) {
        target.damage(Math.max(0.01, damage), attacker);
    }

    // --- stacks ---

    protected int getStacks(SkillContext ctx, Player player) {
        return ctx.getResourceTracker().getStack(player.getUniqueId());
    }

    protected void gainStack(SkillContext ctx, Player player, int max) {
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

    /** 특정 위치에 폭발형 임팩트 파티클을 뿌린다. */
    protected void spawnImpactEffect(Location loc, Particle particle, Object data, int count) {
        spawnAt(loc.getWorld(), particle, loc, data, count, 0.2, 0.2, 0.2, 0.05);
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

    protected void lifesteal(Player player, double amount) {
        var maxHpAttr = player.getAttribute(Attribute.MAX_HEALTH);
        if (maxHpAttr == null) return;
        player.setHealth(Math.min(maxHpAttr.getValue(), player.getHealth() + amount));
    }
}
