package com.poro.rpg.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

/**
 * 1차 시즌 사망 페널티 정책: 템·경험치 전부 유지(keepInventory). (DL-107x 사망 정책)
 *
 * <p>gamerule 대신 이벤트로 처리 — 영지(IridiumSkyblock)·필드·허브·보스 등 다수/동적 월드에
 * 월드별 gamerule 누락 없이 일괄 적용한다. PvP 매치 죽음 처리({@code PvpHubListener.onDeath})는
 * 인벤토리를 건드리지 않으므로 충돌하지 않는다.</p>
 */
public final class DeathKeepInventoryListener implements Listener {

    @EventHandler(priority = EventPriority.HIGH)
    public void onDeath(PlayerDeathEvent event) {
        event.setKeepInventory(true);
        event.getDrops().clear();
        event.setKeepLevel(true);
        event.setDroppedExp(0);
    }
}
