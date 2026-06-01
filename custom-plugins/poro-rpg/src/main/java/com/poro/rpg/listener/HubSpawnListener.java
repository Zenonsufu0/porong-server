package com.poro.rpg.listener;

import com.poro.rpg.hub.HubWorldService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;

import java.util.Objects;

/**
 * 접속 시 허브 이동 (INBOX-006 온보딩 / DL-102).
 * 전원 허브 월드 스폰으로 이동(복귀 유저는 허브에서 /필드·/영지 분기, 첫 접속은 허브에서 무기 선택 후 IS가 영지로 이동).
 * 1틱 지연으로 {@code PlayerJoinListener}의 데이터 로드 이후 텔레포트.
 */
public final class HubSpawnListener implements Listener {

    private final Plugin plugin;
    private final HubWorldService hubWorldService;

    public HubSpawnListener(Plugin plugin, HubWorldService hubWorldService) {
        this.plugin          = Objects.requireNonNull(plugin, "plugin");
        this.hubWorldService = Objects.requireNonNull(hubWorldService, "hubWorldService");
    }

    /**
     * 접속 스폰 위치를 미리 허브로 못 박는다 — 마지막 로그아웃 위치(영지/필드)에 잠깐 스폰됐다가
     * 텔레포트되는 과정에서 그 위치가 IridiumSkyblock 섬 border 밖이면 빨간 비네팅이 깜빡이던 문제 방지.
     */
    @EventHandler
    public void onSpawnLocation(org.spigotmc.event.player.PlayerSpawnLocationEvent event) {
        org.bukkit.Location hub = hubWorldService.hubSpawn();
        if (hub != null) event.setSpawnLocation(hub);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!player.isOnline()) return;
            // 전원 허브 이동 — 복귀 유저는 허브에서 /필드·/영지 분기.
            // 첫 접속(무기 NONE)도 안전한 허브에서 무기 선택 → 선택 완료 시 IS가 새 영지로 이동시킨다.
            // (튜토리얼 맵 단계는 후속 piece에서 허브→튜토리얼로 교체)
            hubWorldService.sendToHub(player);
        });
    }
}
