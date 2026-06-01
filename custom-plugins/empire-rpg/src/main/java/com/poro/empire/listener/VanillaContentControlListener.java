package com.poro.empire.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * 커스텀 RPG 월드에서 바닐라 콘텐츠(자연 스폰 + 아이템 드랍)를 제거한다 (INBOX-011).
 *
 * <p><b>스폰 차단</b>(대상 월드): 적대몹·동물 등 모든 생물의 비(非)의도 스폰(NATURAL 등) 취소.
 * 허용(통과)은 {@code CUSTOM}(MythicMobs·동적 필드 스폰)·{@code COMMAND}(운영자 소환)·
 * {@code SPAWNER_EGG}(스폰 알). Citizens NPC(메타데이터 {@code NPC})는 절대 차단하지 않는다.</p>
 *
 * <p><b>바닐라 드랍 제거</b>(전역): 몹 사망 시 바닐라 아이템 드랍(철·썩은고기 등) 제거. EmpireRPG
 * 보상은 전부 DB 재화({@code addCurrency})라 {@code event.getDrops()}를 비워도 안전(코드 전역에서
 * drops에 추가하는 곳 없음 확인). 플레이어·NPC는 제외.</p>
 */
public final class VanillaContentControlListener implements Listener {

    /** 의도된 스폰 — 통과. 그 외 생물 스폰은 바닐라로 보고 차단. */
    private static final Set<SpawnReason> ALLOWED = EnumSet.of(
            SpawnReason.CUSTOM,
            SpawnReason.COMMAND,
            SpawnReason.SPAWNER_EGG);

    private final Set<String> worldNames;

    public VanillaContentControlListener(Set<String> worldNames) {
        this.worldNames = Objects.requireNonNull(worldNames);
    }

    /** 대상 월드에서 바닐라 자연 스폰(몹+동물) 차단. */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (event.getLocation().getWorld() == null) return;
        if (!worldNames.contains(event.getLocation().getWorld().getName())) return;
        if (event.getEntity().hasMetadata("NPC")) return;        // Citizens NPC 보호
        if (ALLOWED.contains(event.getSpawnReason())) return;     // 커스텀/명령/스폰알 허용
        event.setCancelled(true);                                 // 그 외 바닐라 생물 차단
    }

    /** 바닐라 아이템 드랍 제거(전역) — EmpireRPG 보상은 DB라 무영향. */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof Player) return;          // 플레이어 인벤 드랍 보존
        if (event.getEntity().hasMetadata("NPC")) return;         // Citizens NPC 제외
        event.getDrops().clear();
    }
}
