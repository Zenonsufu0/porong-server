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
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;
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

    // UUID → HUD 억제 만료 시각 — 블럭/도구 슬롯 변경 시 빈 액션바를 보내 클라 native 아이템 이름이 보이게 한다.
    private final Map<UUID, Long>      suppressUntil = new ConcurrentHashMap<>();
    private static final long ITEM_NAME_SUPPRESS_MS = 2000L;

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

    /**
     * 핫바 슬롯 변경 시 — 새 아이템이 무기가 아니면(블럭/도구 등) HUD를 잠시 억제한다.
     * 클라는 아이템 이름(toolHighlight)을 액션바와 '별개 레이어'로 같은 위치에 그린다.
     * 따라서 우리가 이름을 보내면 클라 native 이름과 겹쳐 두 번 보인다 →
     * 억제 동안 '빈 액션바'를 보내 HUD 레이어만 비우면 클라 native 이름만 단독 노출된다.
     * 무기/빈손이면 억제 해제 → HUD 즉시 복귀(쿨타임·스택이 더 중요).
     */
    @EventHandler
    public void onItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        ItemStack newItem = player.getInventory().getItem(event.getNewSlot());
        boolean nonWeaponNamed = newItem != null && !newItem.getType().isAir()
                && WeaponTypeResolver.resolve(newItem) == WeaponType.NONE;
        UUID uuid = player.getUniqueId();
        if (nonWeaponNamed) {
            suppressUntil.put(uuid, System.currentTimeMillis() + ITEM_NAME_SUPPRESS_MS);
        } else {
            suppressUntil.remove(uuid);
        }
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
                    continue;
                }
                if (expiry != null) {
                    overrideUntil.remove(uuid);
                    overrideMsg.remove(uuid);
                }
                // 아이템 이름 표시 구간 — 빈 액션바로 HUD 레이어만 비운다(클라 native 이름이 단독 노출).
                Long supp = suppressUntil.get(uuid);
                if (supp != null && now < supp) {
                    player.sendActionBar(Component.empty());
                    continue;
                }
                if (supp != null) suppressUntil.remove(uuid);
                sendHud(player);
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
