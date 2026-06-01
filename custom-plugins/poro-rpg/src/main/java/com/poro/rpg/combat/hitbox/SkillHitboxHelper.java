package com.poro.rpg.combat.hitbox;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public final class SkillHitboxHelper {
    private SkillHitboxHelper() {}

    // 시전자 + 타 플레이어 모두 제외 (PvE 전용 서버)
    private static boolean isValidTarget(LivingEntity e, Player caster) {
        return !e.equals(caster) && !(e instanceof Player);
    }

    public static List<LivingEntity> arc(Player player, double radius, double angleDeg) {
        Vector facing = player.getLocation().getDirection().setY(0);
        if (facing.lengthSquared() < 0.001) return List.of();
        facing.normalize();
        double halfAngle = Math.toRadians(angleDeg / 2.0);
        var origin = player.getLocation();
        return player.getWorld().getNearbyLivingEntities(origin, radius).stream()
                .filter(e -> isValidTarget(e, player))
                .filter(e -> {
                    Vector toTarget = e.getLocation().toVector().subtract(origin.toVector()).setY(0);
                    if (toTarget.lengthSquared() < 0.001) return true;
                    double angle = Math.acos(Math.max(-1.0, Math.min(1.0, facing.dot(toTarget.normalize()))));
                    return angle <= halfAngle;
                })
                .collect(Collectors.toList());
    }

    public static List<LivingEntity> line(Player player, double length, double width) {
        Vector facing = player.getLocation().getDirection().setY(0);
        if (facing.lengthSquared() < 0.001) return List.of();
        facing.normalize();
        Vector right = new Vector(-facing.getZ(), 0, facing.getX());
        var origin = player.getLocation();
        double searchRadius = Math.sqrt(length * length + width * width);
        return player.getWorld().getNearbyLivingEntities(origin, searchRadius).stream()
                .filter(e -> isValidTarget(e, player))
                .filter(e -> {
                    Vector toTarget = e.getLocation().toVector().subtract(origin.toVector()).setY(0);
                    double fwd = toTarget.dot(facing);
                    double side = Math.abs(toTarget.dot(right));
                    return fwd >= 0 && fwd <= length && side <= width;
                })
                .collect(Collectors.toList());
    }

    public static List<LivingEntity> burst(Player player, double radius) {
        return player.getWorld().getNearbyLivingEntities(player.getLocation(), radius).stream()
                .filter(e -> isValidTarget(e, player))
                .collect(Collectors.toList());
    }

    public static List<LivingEntity> cone(Player player, double length, double angleDeg) {
        return arc(player, length, angleDeg);
    }

    /** 레이캐스트 첫 번째 타겟 1명 반환 (단일 투사체) */
    public static Optional<LivingEntity> projectileRaycast(Player player, double range, double hitSize) {
        Vector dir = player.getLocation().getDirection().normalize();
        var eye = player.getEyeLocation();
        var world = player.getWorld();
        for (double d = 1.0; d <= range; d += 1.0) {
            var checkPos = eye.clone().add(dir.clone().multiply(d));
            for (Entity e : world.getNearbyEntities(checkPos, hitSize, hitSize, hitSize)) {
                if (e instanceof LivingEntity le && isValidTarget(le, player)) {
                    return Optional.of(le);
                }
            }
            if (checkPos.getBlock().getType().isSolid()) break;
        }
        return Optional.empty();
    }

    /** 레이캐스트 관통 — 경로 상 모든 타겟 반환 (관통 투사체) */
    public static List<LivingEntity> projectilePierceRaycast(Player player, double range, double hitSize) {
        Vector dir = player.getLocation().getDirection().normalize();
        var eye = player.getEyeLocation();
        var world = player.getWorld();
        Set<LivingEntity> seen = new HashSet<>();
        List<LivingEntity> results = new ArrayList<>();
        for (double d = 1.0; d <= range; d += 1.0) {
            var checkPos = eye.clone().add(dir.clone().multiply(d));
            for (Entity e : world.getNearbyEntities(checkPos, hitSize, hitSize, hitSize)) {
                if (e instanceof LivingEntity le && isValidTarget(le, player) && seen.add(le)) {
                    results.add(le);
                }
            }
            if (checkPos.getBlock().getType().isSolid()) break;
        }
        return results;
    }
}
