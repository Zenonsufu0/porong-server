package com.poro.empire.listener;

import com.poro.empire.pvp.PvpArenaManager;
import com.poro.empire.pvp.PvpMatchService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.Set;

/**
 * 매치 중 텔레포트/명령 회피 차단 (CANON §5 안전망).
 * <p>
 * 매치 중인 플레이어가 아레나를 임의 텔레포트/명령으로 벗어나는 것을 막는다.
 * 단, PvpMatchService가 시작/종료 시점에 호출하는 정상 텔레포트(PLUGIN 사유)는 허용.
 */
public final class PvpTeleportListener implements Listener {

    private static final Set<String> BLOCKED_COMMANDS = Set.of(
            "spawn", "tp", "tpa", "tpaccept", "tphere", "back", "home", "warp",
            "rtp", "wild", "는집", "집", "스폰", "워프"
    );

    private final PvpMatchService matchService;
    private final PvpArenaManager arenaManager;

    public PvpTeleportListener(PvpMatchService matchService, PvpArenaManager arenaManager) {
        this.matchService = matchService;
        this.arenaManager = arenaManager;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        Player p = event.getPlayer();
        if (!matchService.isInMatch(p.getUniqueId())) return;

        // PvpMatchService가 trigger한 PLUGIN 텔레포트는 허용 (시작/종료 자동 텔레포트)
        if (event.getCause() == PlayerTeleportEvent.TeleportCause.PLUGIN) return;
        // 도착지가 아레나 내부라면 허용 (예: 보스룸 같은 다른 시스템과 충돌 회피)
        if (event.getTo() != null && arenaManager.isInArena(event.getTo())) return;

        event.setCancelled(true);
        p.sendMessage("§c[PvP] 대전 중에는 아레나를 벗어날 수 없습니다.");
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player p = event.getPlayer();
        if (!matchService.isInMatch(p.getUniqueId())) return;

        String msg = event.getMessage();
        if (msg == null || msg.length() < 2) return;
        int spaceIdx = msg.indexOf(' ');
        String cmd = (spaceIdx > 0 ? msg.substring(1, spaceIdx) : msg.substring(1)).toLowerCase();
        if (BLOCKED_COMMANDS.contains(cmd)) {
            event.setCancelled(true);
            p.sendMessage("§c[PvP] 대전 중에는 §f/" + cmd + "§c 명령을 사용할 수 없습니다.");
        }
    }
}
