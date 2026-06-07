package com.poro.rpg.combat.hitbox;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public final class SkillHitboxHelper {
    private SkillHitboxHelper() {}

    // 대형 몹(RAVAGER 폭~1.95, WARDEN, IRON_GOLEM 등) 누락 방지를 위한 1차 탐색 여유 반경.
    private static final double SEARCH_MARGIN = 2.5;

    // 시전자 + 타 플레이어 모두 제외 (PvE 전용 서버)
    private static boolean isValidTarget(LivingEntity e, Player caster) {
        return !e.equals(caster) && !(e instanceof Player);
    }

    /** 엔티티 가로 히트박스 반경(폭/2의 큰 쪽). 근접 판정을 발밑 점이 아닌 몸통 가장자리로 넓히는 데 사용. */
    private static double horizontalRadius(LivingEntity e) {
        BoundingBox bb = e.getBoundingBox();
        return Math.max(bb.getWidthX(), bb.getWidthZ()) / 2.0;
    }

    public static List<LivingEntity> arc(Player player, double radius, double angleDeg) {
        Vector facing = player.getLocation().getDirection().setY(0);
        if (facing.lengthSquared() < 0.001) return List.of();
        facing.normalize();
        double halfAngle = Math.toRadians(angleDeg / 2.0);
        var origin = player.getLocation();
        Vector originVec = origin.toVector();
        return player.getWorld().getNearbyLivingEntities(origin, radius + SEARCH_MARGIN).stream()
                .filter(e -> isValidTarget(e, player))
                .filter(e -> {
                    double er = horizontalRadius(e);
                    Vector toTarget = e.getLocation().toVector().subtract(originVec).setY(0);
                    double dist = toTarget.length();
                    // 히트박스 가장자리 기준 사거리 판정 (큰 몹은 중심이 멀어도 몸통이 닿으면 명중)
                    if (dist - er > radius) return false;
                    if (dist < 0.001) return true;
                    double angle = Math.acos(Math.max(-1.0, Math.min(1.0, facing.dot(toTarget.normalize()))));
                    // 엔티티 반경이 차지하는 각도만큼 부채꼴 허용폭 확장 (좁은 cone에서 큰 몹 누락 방지)
                    double angularRadius = Math.atan2(er, Math.max(0.1, dist));
                    return angle <= halfAngle + angularRadius;
                })
                .collect(Collectors.toList());
    }

    public static List<LivingEntity> line(Player player, double length, double width) {
        Vector facing = player.getLocation().getDirection().setY(0);
        if (facing.lengthSquared() < 0.001) return List.of();
        facing.normalize();
        Vector right = new Vector(-facing.getZ(), 0, facing.getX());
        var origin = player.getLocation();
        Vector originVec = origin.toVector();
        double searchRadius = Math.sqrt(length * length + width * width) + SEARCH_MARGIN;
        return player.getWorld().getNearbyLivingEntities(origin, searchRadius).stream()
                .filter(e -> isValidTarget(e, player))
                .filter(e -> {
                    double er = horizontalRadius(e);
                    Vector toTarget = e.getLocation().toVector().subtract(originVec).setY(0);
                    double fwd = toTarget.dot(facing);
                    double side = Math.abs(toTarget.dot(right));
                    // 히트박스 반경만큼 직선 폭·길이 허용 확장
                    return fwd >= -er && fwd <= length + er && side <= width + er;
                })
                .collect(Collectors.toList());
    }

    public static List<LivingEntity> burst(Player player, double radius) {
        var origin = player.getLocation();
        Vector originVec = origin.toVector();
        return player.getWorld().getNearbyLivingEntities(origin, radius + SEARCH_MARGIN).stream()
                .filter(e -> isValidTarget(e, player))
                // 히트박스 가장자리 기준 반경 판정 (수평) — 큰 몹 포함
                .filter(e -> e.getLocation().toVector().subtract(originVec).setY(0).length()
                        - horizontalRadius(e) <= radius)
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
