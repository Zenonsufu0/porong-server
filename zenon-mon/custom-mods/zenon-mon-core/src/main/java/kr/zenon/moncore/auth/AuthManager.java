package kr.zenon.moncore.auth;

import kr.zenon.moncore.ZenonMonCore;
import kr.zenon.moncore.config.ConfigManager;
import kr.zenon.moncore.config.CoreConfig;
import kr.zenon.moncore.data.PlayerProgress;
import kr.zenon.moncore.data.ZenonMonState;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.world.World;

import java.security.SecureRandom;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 디스코드 인증 (결정 041). /인증 → 코드 발급 → 디스코드 봇이 HTTP API로 검증 → 화이트리스트 완료.
 * 미인증자는 허브 감금 + 메뉴 잠금(인증하기만). 인증 상태는 PlayerProgress(영속).
 * HTTP 스레드에서 호출되는 verify()는 상태 변경을 서버 스레드로 위임(MC 데이터 스레드안전).
 */
public final class AuthManager {
    private AuthManager() {}

    private record Pending(UUID uuid, long expiryEpochMs) {}
    private static final Map<String, Pending> PENDING = new ConcurrentHashMap<>();
    private static final SecureRandom RNG = new SecureRandom();
    private static final char[] ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray(); // 혼동문자(I,O,0,1) 제외

    private static volatile MinecraftServer server;
    public static void setServer(MinecraftServer s) { server = s; }

    public static boolean isEnabled() { return ConfigManager.core().discordAuth.enabled; }

    public static boolean isVerified(ServerPlayerEntity player) {
        if (!isEnabled()) return true;
        return ZenonMonState.get(player.getServer()).getOrCreate(player.getUuid()).discordVerified;
    }

    /** /인증: 코드 발급(기존 코드 폐기 후 신규). */
    public static String generateCode(ServerPlayerEntity player) {
        UUID uuid = player.getUuid();
        PENDING.values().removeIf(p -> p.uuid().equals(uuid));
        String code;
        do {
            StringBuilder sb = new StringBuilder(6);
            for (int i = 0; i < 6; i++) sb.append(ALPHABET[RNG.nextInt(ALPHABET.length)]);
            code = sb.toString();
        } while (PENDING.containsKey(code));
        long expiry = System.currentTimeMillis() + ConfigManager.core().discordAuth.codeExpiryMinutes * 60_000L;
        PENDING.put(code, new Pending(uuid, expiry));
        return code;
    }

    /**
     * 디스코드 봇이 HTTP로 호출. 코드 검증 → 성공 시 해당 UUID 인증 완료(상태변경은 서버 스레드).
     * 반환 = 인증된 MC UUID(성공) / null(실패: 코드 없음·만료).
     */
    public static UUID verify(String code, String discordId) {
        if (code == null) return null;
        Pending p = PENDING.remove(code.trim().toUpperCase());
        if (p == null) return null;
        if (System.currentTimeMillis() > p.expiryEpochMs()) return null;
        MinecraftServer s = server;
        if (s == null) return null;
        s.execute(() -> {
            ZenonMonState state = ZenonMonState.get(s);
            PlayerProgress pp = state.getOrCreate(p.uuid());
            pp.discordVerified = true;
            pp.discordId = discordId == null ? "" : discordId;
            state.markDirty();
            ServerPlayerEntity online = s.getPlayerManager().getPlayer(p.uuid());
            if (online != null) {
                online.sendMessage(Text.literal("§a§l[인증 완료] §r§a디스코드 인증이 완료되었습니다! 이제 자유롭게 플레이하세요."), false);
            }
            ZenonMonCore.LOGGER.info("[Auth] 인증 완료: {} ↔ discord {}", p.uuid(), discordId);
        });
        return p.uuid();
    }

    /** 매 틱(20틱): 미인증자 허브 감금(반경 밖이면 복귀). */
    public static void tickConfine(MinecraftServer s) {
        CoreConfig.DiscordAuth cfg = ConfigManager.core().discordAuth;
        if (!cfg.enabled || !cfg.confine) return;
        CoreConfig.Spawn hub = ConfigManager.core().hub.spawn;
        double r2 = (double) cfg.confineRadius * cfg.confineRadius;
        for (ServerPlayerEntity player : s.getPlayerManager().getPlayerList()) {
            if (isVerified(player)) continue;
            // 오버월드 허브 반경 밖이면 복귀
            boolean overworld = player.getServerWorld().getRegistryKey() == World.OVERWORLD;
            if (!overworld) { teleportHub(player, hub); continue; } // 미인증자는 타 차원 금지
            double dx = player.getX() - hub.x, dz = player.getZ() - hub.z;
            if (dx * dx + dz * dz > r2) {
                teleportHub(player, hub);
                player.sendMessage(Text.literal("§c[인증] 인증 전에는 허브를 벗어날 수 없습니다. §7/인증"), true);
            }
        }
    }

    private static void teleportHub(ServerPlayerEntity player, CoreConfig.Spawn hub) {
        ServerWorld ow = player.getServer().getOverworld();
        player.teleport(ow, hub.x, hub.y, hub.z, hub.yaw, hub.pitch);
    }

    /** 미인증자 차단 안내(명령/메뉴 게이트 공용). */
    public static boolean blockIfUnverified(ServerPlayerEntity player) {
        if (isVerified(player)) return false;
        player.sendMessage(Text.literal("§c[인증] 디스코드 인증 후 이용할 수 있습니다. §e/인증"), false);
        return true;
    }
}
