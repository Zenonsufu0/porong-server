package com.poro.empire.listener;

import com.poro.empire.combat.weapon.WeaponType;
import com.poro.empire.hub.HubWorldService;
import com.poro.empire.storage.PlayerDataManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;

import java.util.Objects;

/**
 * 접속 시 허브 이동 (INBOX-006 온보딩 / DL-NNN).
 * 복귀 유저(무기 선택 완료)는 허브 월드 스폰으로 이동. 첫 접속(무기 NONE)은 온보딩(튜토리얼→영지)에서 분기 —
 * 현재 1차 코어는 복귀 유저 허브 이동까지. 1틱 지연으로 {@code PlayerJoinListener}의 데이터 로드 후 판정.
 */
public final class HubSpawnListener implements Listener {

    private final Plugin plugin;
    private final HubWorldService hubWorldService;
    private final PlayerDataManager playerDataManager;

    public HubSpawnListener(Plugin plugin, HubWorldService hubWorldService, PlayerDataManager playerDataManager) {
        this.plugin            = Objects.requireNonNull(plugin, "plugin");
        this.hubWorldService   = Objects.requireNonNull(hubWorldService, "hubWorldService");
        this.playerDataManager = Objects.requireNonNull(playerDataManager, "playerDataManager");
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!player.isOnline()) return;
            WeaponType weapon = playerDataManager.getWeaponType(player.getUniqueId());
            if (weapon != WeaponType.NONE) {
                // 복귀 유저 → 허브 스폰 (거기서 /필드·/영지 이동)
                hubWorldService.sendToHub(player);
            }
            // 첫 접속(NONE)은 온보딩 단계에서 처리 (튜토리얼→영지, 후속 piece)
        });
    }
}
