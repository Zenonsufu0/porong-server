package kr.zenon.moncore.home;

import kr.zenon.moncore.ZenonMonCore;
import kr.zenon.moncore.config.ConfigManager;
import kr.zenon.moncore.config.CoreConfig;
import kr.zenon.moncore.data.Home;
import kr.zenon.moncore.data.PlayerProgress;
import kr.zenon.moncore.data.ZenonMonState;
import kr.zenon.moncore.economy.EconomyBridge;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 홈 등록/해금/텔레포트 (결정 029).
 * 슬롯 5칸: 기본 freeSlots(1)개 개방, 나머지는 unlockCosts 골드로 순차 해금.
 * 이동은 쿨다운 + 웜업(채널링): 웜업 중 이동/피격 시 취소(매 틱 점검).
 */
public final class HomeManager {
    private HomeManager() {}

    private record Warmup(int slot, long startTick, double x, double y, double z, float health) {}

    private static final Map<UUID, Warmup> PENDING = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> COOLDOWN = new ConcurrentHashMap<>(); // uuid → 재사용 가능 tick

    private static PlayerProgress progress(ServerPlayerEntity player) {
        return ZenonMonState.get(player.getServer()).getOrCreate(player.getUuid());
    }

    private static void markDirty(ServerPlayerEntity player) {
        ZenonMonState.get(player.getServer()).markDirty();
    }

    /** 다음 해금 슬롯의 비용(해금 불가면 -1). */
    public static long nextUnlockCost(PlayerProgress p) {
        CoreConfig.Home cfg = ConfigManager.core().home;
        int maxSlots = Math.min(cfg.maxSlots, PlayerProgress.HOME_MAX);
        if (p.homesUnlocked >= maxSlots) return -1;
        int idx = p.homesUnlocked - cfg.freeSlots;
        if (idx < 0) return 0;
        if (idx >= cfg.unlockCosts.length) return cfg.unlockCosts.length == 0 ? 0 : cfg.unlockCosts[cfg.unlockCosts.length - 1];
        return cfg.unlockCosts[idx];
    }

    /** 다음 슬롯 해금(골드 차감). */
    public static void unlockNext(ServerPlayerEntity player) {
        CoreConfig.Home cfg = ConfigManager.core().home;
        if (!cfg.enabled) return;
        PlayerProgress p = progress(player);
        long cost = nextUnlockCost(p);
        if (cost < 0) {
            player.sendMessage(Text.literal("§e[홈] 모든 홈 슬롯이 이미 해금되었습니다."), true);
            return;
        }
        if (!EconomyBridge.withdraw(player, cost, "home_unlock")) {
            player.sendMessage(Text.literal("§c[홈] 골드가 부족합니다 (필요 " + cost + ")."), true);
            return;
        }
        p.homesUnlocked++;
        markDirty(player);
        player.sendMessage(Text.literal("§a[홈] " + p.homesUnlocked + "번 홈 슬롯 해금! (-" + cost + ")"), true);
    }

    /** 슬롯에 현재 위치 등록(덮어쓰기, 기존 이름 유지). */
    public static void setHome(ServerPlayerEntity player, int slot) {
        PlayerProgress p = progress(player);
        if (slot < 0 || slot >= p.homesUnlocked) {
            player.sendMessage(Text.literal("§c[홈] 잠긴 슬롯입니다."), true);
            return;
        }
        String keepName = p.homes[slot] != null ? p.homes[slot].name : null;
        p.homes[slot] = Home.ofCurrent(player, keepName);
        markDirty(player);
        player.sendMessage(Text.literal("§a[홈] " + p.homes[slot].displayName(slot)
                + " 을(를) 현재 위치로 등록했습니다."), true);
    }

    /** 홈 이름 변경(등록된 슬롯만). 32자 제한. */
    public static void renameHome(ServerPlayerEntity player, int slot, String name) {
        PlayerProgress p = progress(player);
        if (slot < 0 || slot >= p.homesUnlocked || p.homes[slot] == null) {
            player.sendMessage(Text.literal("§c[홈] 이름을 변경할 홈이 없습니다."), true);
            return;
        }
        String trimmed = name == null ? "" : name.trim();
        if (trimmed.length() > 32) trimmed = trimmed.substring(0, 32);
        p.homes[slot].name = trimmed.isBlank() ? null : trimmed;
        markDirty(player);
        player.sendMessage(Text.literal("§a[홈] " + (slot + 1) + "번 홈 이름: §f"
                + p.homes[slot].displayName(slot)), true);
    }

    /** 홈으로 이동 요청 — 쿨다운 확인 후 웜업 시작(또는 즉시 이동). */
    public static void requestTeleport(ServerPlayerEntity player, int slot) {
        CoreConfig.Home cfg = ConfigManager.core().home;
        PlayerProgress p = progress(player);
        if (slot < 0 || slot >= p.homesUnlocked) {
            player.sendMessage(Text.literal("§c[홈] 잠긴 슬롯입니다."), true);
            return;
        }
        if (p.homes[slot] == null) {
            player.sendMessage(Text.literal("§e[홈] 등록되지 않은 홈입니다. 먼저 등록하세요."), true);
            return;
        }
        long now = player.getServer().getTicks();
        Long ready = COOLDOWN.get(player.getUuid());
        if (ready != null && now < ready) {
            long sec = (ready - now + 19) / 20;
            player.sendMessage(Text.literal("§e[홈] 쿨다운 중입니다 (" + sec + "초 남음)."), true);
            return;
        }
        if (cfg.warmupSeconds <= 0) {
            doTeleport(player, slot, now);
            return;
        }
        PENDING.put(player.getUuid(), new Warmup(slot, now,
                player.getX(), player.getY(), player.getZ(), player.getHealth()));
        // 카운트다운은 tickWarmups가 매 초 액션바로 갱신
    }

    /** 매 틱 호출(ZenonMonCore): 웜업 진행/취소/완료 처리. */
    public static void tickWarmups(MinecraftServer server) {
        if (PENDING.isEmpty()) return;
        CoreConfig.Home cfg = ConfigManager.core().home;
        long now = server.getTicks();
        Iterator<Map.Entry<UUID, Warmup>> it = PENDING.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Warmup> e = it.next();
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(e.getKey());
            if (player == null) { it.remove(); continue; }
            Warmup w = e.getValue();
            if (cfg.cancelOnMove && movedTooFar(player, w)) {
                it.remove();
                player.sendMessage(Text.literal("§c[홈] 이동하여 취소되었습니다."), true);
                continue;
            }
            if (cfg.cancelOnDamage && player.getHealth() < w.health) {
                it.remove();
                player.sendMessage(Text.literal("§c[홈] 피격으로 취소되었습니다."), true);
                continue;
            }
            long elapsed = now - w.startTick;
            if (elapsed >= (long) cfg.warmupSeconds * 20L) {
                it.remove();
                doTeleport(player, w.slot, now);
                continue;
            }
            // 매 초 경계마다 남은 시간 액션바 갱신 (N초 후 이동)
            if (elapsed % 20L == 0L) {
                long remain = cfg.warmupSeconds - elapsed / 20L;
                if (remain > 0) {
                    player.sendMessage(Text.literal("§b[홈] " + remain + "초 후 이동... §7(움직이면 취소)"), true);
                }
            }
        }
    }

    private static boolean movedTooFar(ServerPlayerEntity player, Warmup w) {
        double dx = player.getX() - w.x, dy = player.getY() - w.y, dz = player.getZ() - w.z;
        return dx * dx + dy * dy + dz * dz > 0.10; // ~0.3블록 이상 이동
    }

    private static void doTeleport(ServerPlayerEntity player, int slot, long now) {
        CoreConfig.Home cfg = ConfigManager.core().home;
        PlayerProgress p = progress(player);
        Home home = p.homes[slot];
        if (home == null) return;
        MinecraftServer server = player.getServer();
        Identifier dimId = Identifier.tryParse(home.dimension);
        ServerWorld world = dimId == null ? null
                : server.getWorld(RegistryKey.of(RegistryKeys.WORLD, dimId));
        if (world == null) {
            player.sendMessage(Text.literal("§c[홈] 홈 차원을 찾을 수 없습니다: " + home.dimension), true);
            return;
        }
        player.teleport(world, home.x, home.y, home.z, home.yaw, home.pitch);
        COOLDOWN.put(player.getUuid(), now + (long) cfg.cooldownSeconds * 20L);
        player.sendMessage(Text.literal("§a[홈] " + home.displayName(slot) + " 으로 이동했습니다."), true);
        if (ConfigManager.core().logging.auditEnabled) {
            ZenonMonCore.LOGGER.info("[Home] {} → 슬롯 {} ({}, {}, {}, {})",
                    player.getGameProfile().getName(), slot + 1, home.dimension,
                    (int) home.x, (int) home.y, (int) home.z);
        }
    }
}
