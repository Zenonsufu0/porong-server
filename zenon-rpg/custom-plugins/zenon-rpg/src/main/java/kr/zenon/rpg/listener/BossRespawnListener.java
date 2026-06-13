package kr.zenon.rpg.listener;

import kr.zenon.rpg.boss.engine.BossEngineRuntime;
import kr.zenon.rpg.boss.room.BossRoomManager;
import kr.zenon.rpg.boss.room.BossRoomSlot;
import kr.zenon.rpg.hub.HubWorldService;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.Plugin;

import java.util.Optional;
import java.util.UUID;

/**
 * 사망 후 부활 위치 라우팅 (DL: 사망 정책).
 *
 * <ul>
 *   <li>보스룸 사망 + 공유 부활 토큰(데스카운트) 잔여 → 보스룸 플레이어 스폰에 부활(토큰 1 소모).</li>
 *   <li>보스룸 사망 + 토큰 소진 → 파티 전멸: 런 실패 종료(귀환은 {@code BossRewardService}가 처리).</li>
 *   <li>그 외(필드·영지 등) 일반 사망 → 수도({@code world_hub})에서 부활.</li>
 * </ul>
 *
 * <p>인벤토리·경험치 유지는 {@link DeathKeepInventoryListener}(PlayerDeathEvent)가 별도로 담당한다.</p>
 */
public final class BossRespawnListener implements Listener {

    private final Plugin            plugin;
    private final BossRoomManager   bossRoomManager;
    private final HubWorldService   hubWorldService;
    private final BossEngineRuntime bossEngineRuntime;

    public BossRespawnListener(Plugin plugin,
                               BossRoomManager bossRoomManager,
                               HubWorldService hubWorldService,
                               BossEngineRuntime bossEngineRuntime) {
        this.plugin            = plugin;
        this.bossRoomManager   = bossRoomManager;
        this.hubWorldService   = hubWorldService;
        this.bossEngineRuntime = bossEngineRuntime;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        UUID   id     = player.getUniqueId();

        int slotId = bossRoomManager.isInBossRoom(id)
                ? bossRoomManager.slotOf(id).orElse(-1)
                : -1;

        if (slotId >= 0) {
            if (bossRoomManager.consumeDeath(slotId)) {
                // 토큰 잔여 → 보스룸 재부활
                Optional<BossRoomSlot> slot = bossRoomManager.slotById(slotId);
                if (slot.isPresent()) {
                    event.setRespawnLocation(slot.get().playerSpawn());
                    // 사망 시 포션효과가 초기화되므로 리스폰 완료 1틱 뒤에 야간투시 재적용.
                    Bukkit.getScheduler().runTaskLater(plugin,
                            () -> BossRoomListener.applyBossVision(player), 1L);
                    int rem = bossRoomManager.deathsRemaining(slotId);
                    int max = bossRoomManager.deathsMax(slotId);
                    notifyRoom(slotId, "§c[전투] §f" + player.getName()
                            + " §7사망 — 남은 부활 §e" + rem + "§7/§e" + max);
                    return;
                }
            }
            // 토큰 소진 → 파티 전멸: 런 실패 종료(메시지·영지 귀환은 BossRewardService.handleRunExit)
            sendToHub(event); // 즉시 폴백 위치(귀환 텔레포트 전)
            notifyRoom(slotId, "§4§l[전멸] §c부활 횟수를 모두 소진했습니다. 도전 실패!");
            bossRoomManager.runIdOf(slotId).ifPresent(runId ->
                    Bukkit.getScheduler().runTaskLater(plugin,
                            () -> bossEngineRuntime.runService().endRun(runId, false, "party_wipe"), 1L));
            return;
        }

        // 일반 사망 → 수도 부활
        sendToHub(event);
    }

    private void sendToHub(PlayerRespawnEvent event) {
        Location hub = hubWorldService.hubSpawn();
        if (hub != null) event.setRespawnLocation(hub);
    }

    /** 같은 보스룸 슬롯에 있는 온라인 플레이어 전원에게 메시지. */
    private void notifyRoom(int slotId, String message) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (bossRoomManager.slotOf(p.getUniqueId()).map(s -> s == slotId).orElse(false)) {
                p.sendMessage(message);
            }
        }
    }
}
