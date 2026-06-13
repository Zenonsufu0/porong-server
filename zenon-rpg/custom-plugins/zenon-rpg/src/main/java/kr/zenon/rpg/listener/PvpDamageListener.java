package kr.zenon.rpg.listener;

import kr.zenon.rpg.pvp.PvpArenaManager;
import kr.zenon.rpg.pvp.PvpMatch;
import kr.zenon.rpg.pvp.PvpMatchService;
import kr.zenon.rpg.pvp.PvpMatchType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.Optional;

/**
 * PvP 데미지 게이트 (CANON §5 — PvP 허용 구역 = 아레나 방 내부만).
 * 매치 중인 두 플레이어 외에는 PvP 차단은 WorldGuard pvp flag에 위임.
 * 이 리스너는 "매치 외부 location에서의 PvP 데미지" + "비매치 상대 데미지"를 차단.
 */
public final class PvpDamageListener implements Listener {

    private final PvpMatchService matchService;
    private final PvpArenaManager arenaManager;

    public PvpDamageListener(PvpMatchService matchService, PvpArenaManager arenaManager) {
        this.matchService = matchService;
        this.arenaManager = arenaManager;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        Player attacker = resolvePlayer(event.getDamager());
        if (attacker == null) return;

        // 매치 중인 양측 페어만 통과시킴 — 그 외 PvP는 모두 차단 (1차 시즌: PvE 전용 정책)
        Optional<PvpMatch> match = matchService.matchOf(attacker.getUniqueId());
        if (match.isEmpty()) {
            // 양측 모두 매치 외 — 플러그인 차원에서 PvP 차단 (WorldGuard 미설치 환경 보장)
            event.setCancelled(true);
            attacker.sendMessage("§c[PvP] 대전 외부에서는 다른 플레이어를 공격할 수 없습니다.");
            return;
        }
        // 공격자 매치 중 — 같은 매치 상대인지 확인
        if (!match.get().involves(victim.getUniqueId())) {
            event.setCancelled(true);
            attacker.sendMessage("§c[PvP] 대전 상대만 공격할 수 있습니다.");
            return;
        }
        // 같은 매치 페어 — 아레나 내부인지 확인
        if (!arenaManager.isInArena(attacker.getLocation()) || !arenaManager.isInArena(victim.getLocation())) {
            event.setCancelled(true);
            attacker.sendMessage("§c[PvP] 아레나 내부에서만 공격할 수 있습니다.");
            return;
        }

        // 정규대전 — IL 60 가상 동일화 (CANON §3, DL-077).
        // 공격자 PvpContext의 damageScale (VIRTUAL_IL / actualAvgIl) 적용.
        // attacker IL > 60: 데미지 감소 (강자의 강타가 IL60 기준으로 줄어듬)
        // attacker IL < 60: 데미지 증가 (약자의 공격이 IL60 기준으로 보정됨)
        if (match.get().type() == PvpMatchType.RANKED) {
            matchService.contextOf(match.get().matchId(), attacker.getUniqueId()).ifPresent(ctx -> {
                // 스케일 + 안전 클램프 항상 적용 (IL60 동일화 가정에서도 극단 회피)
                double scaled = event.getDamage() * ctx.damageScale();
                scaled = Math.max(RANKED_MIN_DAMAGE, Math.min(scaled, RANKED_MAX_DAMAGE));
                event.setDamage(scaled);
            });
        }
    }

    /** 정규대전 데미지 상·하한 — 비율 적용 후 극단값 보호. */
    private static final double RANKED_MAX_DAMAGE = 18.0;
    private static final double RANKED_MIN_DAMAGE = 2.0;

    private Player resolvePlayer(org.bukkit.entity.Entity entity) {
        if (entity instanceof Player p) return p;
        if (entity instanceof Projectile proj && proj.getShooter() instanceof Player p) return p;
        return null;
    }
}
