package com.poro.empire.hub;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Objects;

/**
 * 허브(수도) 월드 관리 (INBOX-006 온보딩 / DL-NNN).
 * 별도 평지 월드 {@code world_hub}를 부팅 시 보장(없으면 생성, 표면 y=64로 필드 world와 통일)하고,
 * 복귀 유저를 접속 시 허브 스폰으로 이동시킨다. 첫 접속 유저는 온보딩(튜토리얼→영지)으로 분기.
 */
public final class HubWorldService {

    public static final String HUB_WORLD = "world_hub";
    // 필드 world와 동일한 평지 프리셋 (표면 y=64) — 사장님이 수도를 건축.
    private static final String FLAT_PRESET =
            "{\"biome\":\"minecraft:plains\",\"layers\":["
            + "{\"block\":\"minecraft:bedrock\",\"height\":1},"
            + "{\"block\":\"minecraft:stone\",\"height\":124},"
            + "{\"block\":\"minecraft:dirt\",\"height\":2},"
            + "{\"block\":\"minecraft:grass_block\",\"height\":1}]}";

    private final Plugin plugin;

    public HubWorldService(Plugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    /** 부팅 시 호출 — world_hub가 없으면 평지로 생성. */
    public World ensureHubWorld() {
        World existing = Bukkit.getWorld(HUB_WORLD);
        if (existing != null) return existing;
        plugin.getLogger().info("[Hub] " + HUB_WORLD + " 생성 (평지, 표면 y=64)...");
        World w = new WorldCreator(HUB_WORLD)
                .type(WorldType.FLAT)
                .generatorSettings(FLAT_PRESET)
                .createWorld();
        if (w != null) {
            w.setSpawnLocation(0, 64, 0);
            w.setPVP(false);
            w.setStorm(false);
            w.setDifficulty(org.bukkit.Difficulty.PEACEFUL); // 허브 = 안전지대(적대몹 없음)
        }
        return w;
    }

    /** 허브 스폰 위치 (없으면 생성 시도). */
    public Location hubSpawn() {
        World w = Bukkit.getWorld(HUB_WORLD);
        if (w == null) w = ensureHubWorld();
        if (w == null) return null;
        return w.getSpawnLocation();
    }

    /** 플레이어를 허브 스폰으로 이동. */
    public void sendToHub(Player player) {
        Location spawn = hubSpawn();
        if (spawn != null) player.teleport(spawn);
    }
}
