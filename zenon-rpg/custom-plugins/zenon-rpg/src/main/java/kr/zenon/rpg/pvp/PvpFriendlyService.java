package kr.zenon.rpg.pvp;

import kr.zenon.rpg.field.SafeZoneService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 친선대전 — 요청 발송 + 30초 응답 윈도우 (CANON §2).
 * <p>요청 수신자는 영지에서만 수락 가능 (현재는 영지 검증은 후속 — 일단 수락 시 매치 시작).
 */
public final class PvpFriendlyService {

    public static final long REQUEST_TIMEOUT_TICKS = 20L * 30; // 30초

    public record Request(UUID requesterUuid, String requesterName, long expiryMs) {}

    private final Plugin          plugin;
    private final PvpMatchService matchService;
    private SafeZoneService       safeZoneService; // setter — 영지 검증
    private boolean               enforceIsland;   // WorldGuard 환경에서만 영지 검증 활성화

    /** target UUID → Request. */
    private final Map<UUID, Request> pendingRequests = new ConcurrentHashMap<>();

    public PvpFriendlyService(Plugin plugin, PvpMatchService matchService) {
        this.plugin       = plugin;
        this.matchService = matchService;
    }

    public void attachSafeZone(SafeZoneService safeZoneService, boolean enforceIsland) {
        this.safeZoneService = safeZoneService;
        this.enforceIsland   = enforceIsland;
    }

    /** 친선 요청 발송. */
    public boolean sendRequest(Player requester, String targetName) {
        if (matchService.isInMatch(requester.getUniqueId())) {
            requester.sendMessage("§c[친선] 대전 중에는 요청할 수 없습니다.");
            return false;
        }
        if (!isInIsland(requester)) {
            requester.sendMessage("§c[친선] 영지에서만 신청할 수 있습니다.");
            return false;
        }
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            requester.sendMessage("§c[친선] 현재 접속 중인 플레이어가 아닙니다.");
            return false;
        }
        if (target.getUniqueId().equals(requester.getUniqueId())) {
            requester.sendMessage("§c[친선] 본인에게 요청할 수 없습니다.");
            return false;
        }
        if (matchService.isInMatch(target.getUniqueId())) {
            requester.sendMessage("§c[친선] 상대가 이미 대전 중입니다.");
            return false;
        }
        if (pendingRequests.containsKey(target.getUniqueId())) {
            requester.sendMessage("§c[친선] 상대가 이미 다른 요청을 받은 상태입니다.");
            return false;
        }

        long expiry = System.currentTimeMillis() + 30_000L;
        pendingRequests.put(target.getUniqueId(),
                new Request(requester.getUniqueId(), requester.getName(), expiry));
        requester.sendMessage("§a[친선] §f" + target.getName() + "§a님에게 친선대전을 신청했습니다. §7(30초)");
        target.sendMessage("§e[친선] §f" + requester.getName() + "§e님이 친선대전을 신청했습니다.");
        target.sendMessage("§a[수락] §7→ §f/친선 수락   §c[거절] §7→ §f/친선 거절   §8(30초 내)");

        // 30초 만료
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Request r = pendingRequests.get(target.getUniqueId());
            if (r != null && r.expiryMs() <= System.currentTimeMillis()) {
                pendingRequests.remove(target.getUniqueId());
                Player t = Bukkit.getPlayer(target.getUniqueId());
                if (t != null) t.sendMessage("§7[친선] 친선대전 요청이 만료되었습니다.");
                Player rq = Bukkit.getPlayer(r.requesterUuid());
                if (rq != null) rq.sendMessage("§7[친선] " + target.getName() + "님이 응답하지 않았습니다.");
            }
        }, REQUEST_TIMEOUT_TICKS);
        return true;
    }

    /** 친선 요청 수락. */
    public boolean accept(Player target) {
        Request r = pendingRequests.remove(target.getUniqueId());
        if (r == null || r.expiryMs() < System.currentTimeMillis()) {
            target.sendMessage("§c[친선] 대기 중인 요청이 없습니다.");
            return false;
        }
        // 수락 시점 재검증 — 신청 후 다른 대전이 시작됐을 수 있음
        if (matchService.isInMatch(target.getUniqueId())) {
            target.sendMessage("§c[친선] 이미 대전 중이라 수락할 수 없습니다.");
            return false;
        }
        if (!isInIsland(target)) {
            target.sendMessage("§c[친선] 영지에서만 수락할 수 있습니다.");
            return false;
        }
        if (matchService.isInMatch(r.requesterUuid())) {
            target.sendMessage("§c[친선] 신청자가 다른 대전에 참여 중입니다.");
            Player rq = Bukkit.getPlayer(r.requesterUuid());
            if (rq != null) rq.sendMessage("§c[친선] 요청을 수락하려 했으나 본인이 다른 대전 중입니다.");
            return false;
        }
        Player requester = Bukkit.getPlayer(r.requesterUuid());
        if (requester == null) {
            target.sendMessage("§c[친선] 신청자가 오프라인 상태입니다.");
            return false;
        }
        PvpMatchService.StartResult result = matchService.startFriendly(requester, target);
        return result == PvpMatchService.StartResult.SUCCESS;
    }

    /**
     * 영지 검사. 개인 섬 월드(IridiumSkyblock/island)에 있으면 영지로 인정한다
     * — WG 세이프존 미설정 환경에서도 영지에서 친선이 가능하도록.
     * 그 외 월드는 WorldGuard 세이프존으로 판정(미설치=enforceIsland false면 항상 true).
     */
    private boolean isInIsland(Player player) {
        String world = player.getWorld().getName();
        if (world.equals("IridiumSkyblock") || world.equals("island")) return true;
        if (!enforceIsland || safeZoneService == null) return true;
        return safeZoneService.isSafeZone(player.getLocation());
    }

    /** 친선 요청 거절. */
    public boolean reject(Player target) {
        Request r = pendingRequests.remove(target.getUniqueId());
        if (r == null) {
            target.sendMessage("§c[친선] 대기 중인 요청이 없습니다.");
            return false;
        }
        target.sendMessage("§7[친선] 친선대전 요청을 거절했습니다.");
        Player requester = Bukkit.getPlayer(r.requesterUuid());
        if (requester != null) requester.sendMessage("§7[친선] §f" + target.getName() + "§7님이 거절했습니다.");
        return true;
    }
}
