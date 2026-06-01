package com.poro.rpg.listener;

import com.poro.rpg.combat.CooldownManager;
import com.poro.rpg.combat.ResourceTracker;
import com.poro.rpg.combat.weapon.WeaponType;
import com.poro.rpg.combat.weapon.WeaponTypeResolver;
import com.poro.rpg.growth.GrowthStateStore;
import com.poro.rpg.growth.engine.PlayerGrowthState;
import com.poro.rpg.storage.PlayerDataManager;
import com.poro.rpg.util.HealthHudFormatter;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class HealthHudListener implements Listener {

    private final Plugin plugin;
    private final CooldownManager cdm;
    private final ResourceTracker rt;
    private final GrowthStateStore growthStore;
    private final PlayerDataManager playerDataManager;

    // UUID → 오버라이드 만료 시각 (epoch ms)
    private final Map<UUID, Long>      overrideUntil = new ConcurrentHashMap<>();
    private final Map<UUID, Component> overrideMsg   = new ConcurrentHashMap<>();

    public HealthHudListener(Plugin plugin,
                              CooldownManager cdm,
                              ResourceTracker rt,
                              GrowthStateStore growthStore,
                              PlayerDataManager playerDataManager) {
        this.plugin            = plugin;
        this.cdm               = cdm;
        this.rt                = rt;
        this.growthStore       = growthStore;
        this.playerDataManager = playerDataManager;
        startHudTask();
    }

    /**
     * 강화 성공/실패, 레벨업 등 임시 알림을 표시한다.
     * ticks 후 자동으로 HUD로 복귀.
     */
    public void showAlert(UUID uuid, Component message, int ticks) {
        overrideMsg.put(uuid, message);
        overrideUntil.put(uuid, System.currentTimeMillis() + (long) ticks * 50L);
    }

    // ─── private ──────────────────────────────────────────────────────────

    private void startHudTask() {
        plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            long now = System.currentTimeMillis();
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                UUID uuid = player.getUniqueId();
                Long expiry = overrideUntil.get(uuid);
                if (expiry != null && now < expiry) {
                    player.sendActionBar(overrideMsg.getOrDefault(uuid, Component.empty()));
                } else {
                    if (expiry != null) {
                        overrideUntil.remove(uuid);
                        overrideMsg.remove(uuid);
                    }
                    sendHud(player);
                }
            }
        }, 1L, 1L);
    }

    private void sendHud(Player player) {
        // 현재 손에 든 무기 타입
        WeaponType wt = WeaponTypeResolver.resolve(player);

        // 성장 상태: 이미 로드된 상태만 사용 (DB 조회 없이)
        PlayerGrowthState state = growthStore.get(player.getUniqueId()).orElseGet(() -> {
            // 무기 없이도 XP 레벨이 필요하므로, 등록된 직업 기반으로 조회
            WeaponType classWt = playerDataManager.getWeaponType(player.getUniqueId());
            if (classWt == WeaponType.NONE) return null;
            return growthStore.get(player.getUniqueId()).orElse(null);
        });

        player.sendActionBar(HealthHudFormatter.build(player, cdm, rt, state, wt));
    }
}
