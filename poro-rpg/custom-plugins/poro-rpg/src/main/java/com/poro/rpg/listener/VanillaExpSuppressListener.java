package com.poro.rpg.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * 바닐라 경험치 바 억제 (DL-085).
 * PoroRPG는 커스텀 레벨링(PlayerLevelingService, HUD 표시)을 쓰므로 바닐라 초록 XP 바는 노출하지 않는다.
 * <ul>
 *   <li>모든 바닐라 XP 획득 차단 (몹·광물·낚시 등 전부): {@link PlayerExpChangeEvent} → 0</li>
 *   <li>몹 처치 시 XP 오브 미생성: {@link EntityDeathEvent#setDroppedExp(int)} 0</li>
 *   <li>접속 시 기존 바닐라 XP/레벨 리셋: 바를 빈 상태로</li>
 * </ul>
 * 커스텀 레벨링(FieldDropListener → addExp)은 별개로 그대로 동작한다.
 */
public final class VanillaExpSuppressListener implements Listener {

    @EventHandler
    public void onExpChange(PlayerExpChangeEvent event) {
        event.setAmount(0);
    }

    // HIGHEST — MythicMobs 등이 설정한 드랍 XP를 마지막에 0으로 덮어 오브 생성 차단
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDeath(EntityDeathEvent event) {
        event.setDroppedExp(0);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        player.setLevel(0);
        player.setExp(0f);
        player.setTotalExperience(0);
    }
}
