package kr.zenon.rpg.listener;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Ambient;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.WaterMob;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.event.entity.EntityCombustByBlockEvent;
import org.bukkit.event.entity.EntityCombustByEntityEvent;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.plugin.Plugin;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * 커스텀 RPG 월드에서 바닐라 콘텐츠(자연 스폰·아이템 드랍·일광 화상)를 제거한다 (INBOX-011).
 *
 * <p><b>① 스폰 차단</b>(대상 월드): 적대몹·동물 등 모든 생물의 비(非)의도 스폰(NATURAL 등) 취소.
 * 허용은 {@code CUSTOM}(MythicMobs·동적 필드 스폰)·{@code COMMAND}·{@code SPAWNER_EGG}. Citizens NPC 보호.</p>
 *
 * <p><b>② 바닐라 드랍 제거</b>(전역): 몹 사망 시 바닐라 아이템 드랍 제거. ZenonRPG 보상은 전부 DB
 * 재화({@code addCurrency})라 {@code event.getDrops()}를 비워도 안전. 플레이어·NPC 제외.</p>
 *
 * <p><b>③ 일광 화상 방지</b>(대상 월드): 커스텀 몹(좀비/스켈레톤 등)이 햇빛에 타지 않게 sun 연소만 취소.
 * 용암/불(ByBlock)·화염 공격(ByEntity)은 유지.</p>
 *
 * <p><b>④ 기존 동물 정리</b>(주기 sweep): 패치 전부터 잔존하는 바닐라 동물(passive는 디스폰 안 됨)을
 * 주기적으로 제거. 동물은 {@code Animals}/{@code WaterMob}/{@code Ambient} 타입이고 커스텀 필드몹은
 * 전부 {@code Monster}라 타입으로 안전 구분(NPC 제외).</p>
 */
public final class VanillaContentControlListener implements Listener {

    private static final Set<SpawnReason> ALLOWED = EnumSet.of(
            SpawnReason.CUSTOM,
            SpawnReason.COMMAND,
            SpawnReason.SPAWNER_EGG);

    private static final long SWEEP_PERIOD_TICKS = 1200L; // 60초마다 잔존 동물 정리

    private final Set<String> worldNames;

    public VanillaContentControlListener(Set<String> worldNames) {
        this.worldNames = Objects.requireNonNull(worldNames);
    }

    /** 기존 동물 주기 sweep 시작 (등록과 별도). */
    public void startSweep(Plugin plugin) {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> sweepExistingAnimals(plugin),
                SWEEP_PERIOD_TICKS, SWEEP_PERIOD_TICKS);
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

    /** 바닐라 아이템 드랍 제거(전역) — ZenonRPG 보상은 DB라 무영향. */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof Player) return;          // 플레이어 인벤 드랍 보존
        if (event.getEntity().hasMetadata("NPC")) return;         // Citizens NPC 제외
        event.getDrops().clear();
    }

    /** 일광 화상 방지 — sun 연소만 취소(용암/불/화염공격은 유지). */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onCombust(EntityCombustEvent event) {
        if (event instanceof EntityCombustByBlockEvent || event instanceof EntityCombustByEntityEvent) return;
        if (event.getEntity() instanceof Player) return;
        if (event.getEntity().hasMetadata("NPC")) return;
        if (!worldNames.contains(event.getEntity().getWorld().getName())) return;
        event.setCancelled(true);
    }

    private void sweepExistingAnimals(Plugin plugin) {
        int removed = 0;
        for (String name : worldNames) {
            World world = Bukkit.getWorld(name);
            if (world == null) continue;
            for (Entity e : world.getEntities()) {
                if (e.hasMetadata("NPC")) continue;
                if (e instanceof Animals || e instanceof WaterMob || e instanceof Ambient) {
                    e.remove();
                    removed++;
                }
            }
        }
        if (removed > 0) {
            plugin.getLogger().info("[VanillaContentControl] 잔존 바닐라 동물 " + removed + "마리 정리.");
        }
    }
}
