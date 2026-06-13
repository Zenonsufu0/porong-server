package kr.zenon.rpg.combat.skills;

import kr.zenon.rpg.combat.hitbox.SkillHitboxHelper;
import kr.zenon.rpg.combat.weapon.WeaponType;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

public abstract class PluginWeaponSkill extends BaseWeaponSkill {
    protected final Plugin plugin;

    protected PluginWeaponSkill(Plugin plugin, String key, String displayName,
                                 WeaponType weaponType, long cooldown) {
        super(key, displayName, weaponType, cooldown);
        this.plugin = plugin;
    }

    /**
     * 다단계 돌진 (DL-129 추가#27) — 매 틱 전방 추진하며 경로상 적을 1회씩 타격, 돌진 중 무적(선택).
     * 단일 틱에 모든 burst를 돌려 출발 지점만 때리던 문제를 해소하고, 무적으로 생존기 역할도 부여한다.
     *
     * @param direction  돌진 방향(보통 시선). y는 무시.
     * @param ticks      돌진 지속 틱(무적 지속과 동일).
     * @param speedPerTick 틱당 전방 속도(블록).
     * @param hitRadius  매 틱 타격 반경.
     * @param invulnerable 돌진 중 무적 여부.
     * @param trail      틱당 잔상 색(null이면 잔상 없음).
     * @param onHit      적 1회 타격 콜백(적별 최초 1회). null 허용.
     * @param onComplete 종료 시 1회 호출(인자=명중 distinct 수). null 허용.
     */
    protected void dashStrike(Player player, Vector direction, int ticks, double speedPerTick,
                              double hitRadius, boolean invulnerable, Particle.DustOptions trail,
                              Consumer<LivingEntity> onHit, IntConsumer onComplete) {
        final Vector dir = direction.clone().setY(0);
        if (dir.lengthSquared() < 0.001) dir.setX(0).setZ(1);
        dir.normalize();
        final Set<UUID> seen = new HashSet<>();
        if (invulnerable) player.setInvulnerable(true);
        new BukkitRunnable() {
            int t = 0;
            @Override
            public void run() {
                // 종료 — 무적 반드시 해제(모든 탈출 분기)
                if (t >= ticks || !player.isOnline() || player.isDead()) {
                    if (invulnerable) player.setInvulnerable(false);
                    if (onComplete != null) onComplete.accept(seen.size());
                    cancel();
                    return;
                }
                player.setVelocity(new Vector(dir.getX() * speedPerTick,
                        player.getVelocity().getY(), dir.getZ() * speedPerTick));
                for (LivingEntity e : SkillHitboxHelper.burst(player, hitRadius)) {
                    if (seen.add(e.getUniqueId()) && onHit != null) onHit.accept(e);
                }
                if (trail != null) {
                    spawnParticleCircle(player, Particle.DUST, trail, 1.4, 8);
                    if (t % 2 == 0) spawnParticleCircle(player, Particle.SWEEP_ATTACK, null, 1.0, 3);
                }
                t++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    /**
     * 짧은 무적만 부여 (DL-129 추가#27) — 경로 타격 없는 회피기(후방 회피 등)용. 종료 시 무적 해제.
     */
    protected void invulnerableFor(Player player, int ticks) {
        player.setInvulnerable(true);
        new BukkitRunnable() {
            int t = 0;
            @Override
            public void run() {
                if (t >= ticks || !player.isOnline() || player.isDead()) {
                    player.setInvulnerable(false);
                    cancel();
                    return;
                }
                t++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
}
