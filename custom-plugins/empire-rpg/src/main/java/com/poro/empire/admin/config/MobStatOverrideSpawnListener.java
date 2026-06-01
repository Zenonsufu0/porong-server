package com.poro.empire.admin.config;

import org.bukkit.entity.Entity;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import java.util.logging.Logger;

/**
 * MythicMobs {@code MythicMobSpawnEvent}를 reflection으로 리스닝해 몹 스탯 오버라이드를 적용한다.
 *
 * <p>스폰 경로(EmpireRPG 동적 스폰 / 보스룸 / MythicMobs 네이티브 명령·스포너)와 무관하게
 * 모든 MythicMobs 스폰을 한 지점에서 커버한다. MythicMobs 미설치 시 graceful skip
 * (mythicSpawner 어댑터와 동일한 reflection 격리 원칙).</p>
 */
public final class MobStatOverrideSpawnListener {

    private MobStatOverrideSpawnListener() {}

    @SuppressWarnings("unchecked")
    public static void register(Plugin plugin, MobStatOverrideService service, Logger log) {
        try {
            Class<? extends Event> eventClass = (Class<? extends Event>)
                    Class.forName("io.lumine.mythic.bukkit.events.MythicMobSpawnEvent");
            Listener listener = new Listener() {};
            plugin.getServer().getPluginManager().registerEvent(
                    eventClass, listener, EventPriority.MONITOR,
                    (l, event) -> {
                        try {
                            Object mobType = event.getClass().getMethod("getMobType").invoke(event);
                            String internalName = (String) mobType.getClass()
                                    .getMethod("getInternalName").invoke(mobType);
                            Object entity = event.getClass().getMethod("getEntity").invoke(event);
                            if (entity instanceof Entity ent) {
                                service.applyOnSpawn(plugin, ent, internalName);
                            }
                        } catch (Exception ignored) {
                            // 이벤트 형태 변경 등 — 조용히 무시(스폰 자체에 영향 주지 않음)
                        }
                    },
                    plugin, true /* ignoreCancelled */);
            log.info("[MobStatOverride] MythicMobSpawnEvent 리스너 등록 — 전 스폰 경로 커버.");
        } catch (ClassNotFoundException e) {
            log.warning("[MobStatOverride] MythicMobs 미가용 — 몹 스탯 오버라이드 리스너 미등록.");
        } catch (Exception e) {
            log.warning("[MobStatOverride] 리스너 등록 실패: " + e.getMessage());
        }
    }
}
