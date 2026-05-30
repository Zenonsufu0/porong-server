package com.poro.empire.listener;

import com.poro.empire.hub.HubWorldService;
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
