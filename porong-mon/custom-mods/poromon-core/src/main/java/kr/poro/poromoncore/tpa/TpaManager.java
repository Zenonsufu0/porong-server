package kr.poro.poromoncore.tpa;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TPA — 플레이어가 다른 플레이어에게 순간이동 요청(친구에게 가기).
 * 요청 → 대상에게 [수락][거절] 클릭 메시지 → 수락 시 요청자가 대상 위치로 이동.
 * 대상 1명당 최신 요청 1건 유지, 60초 만료.
 */
public final class TpaManager {
    private TpaManager() {}

    private static final long EXPIRE_TICKS = 60L * 20L;

    private record Request(UUID requester, long expireTick) {}

    /** key = 대상 UUID. */
    private static final Map<UUID, Request> PENDING = new ConcurrentHashMap<>();

    /** requester가 targetName에게 TP 요청. */
    public static void request(ServerPlayerEntity requester, String targetName) {
        MinecraftServer server = requester.getServer();
        ServerPlayerEntity target = server.getPlayerManager().getPlayer(targetName);
        if (target == null) {
            requester.sendMessage(Text.literal("§c[TPA] '" + targetName + "' 플레이어를 찾을 수 없습니다."), false);
            return;
        }
        if (target == requester) {
            requester.sendMessage(Text.literal("§c[TPA] 자기 자신에게는 요청할 수 없습니다."), false);
            return;
        }
        long now = server.getTicks();
        PENDING.put(target.getUuid(), new Request(requester.getUuid(), now + EXPIRE_TICKS));

        requester.sendMessage(Text.literal("§a[TPA] " + target.getGameProfile().getName()
                + "§a 님에게 순간이동을 요청했습니다. §7(60초 내 수락 대기)"), false);

        Text accept = Text.literal(" §a§l[수락]").styled(s ->
                s.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/poromon tpa accept")));
        Text deny = Text.literal(" §c§l[거절]").styled(s ->
                s.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/poromon tpa deny")));
        target.sendMessage(Text.literal("§e[TPA] §f" + requester.getGameProfile().getName()
                + "§e 님이 당신에게 순간이동을 요청했습니다.").append(accept).append(deny), false);
    }

    /** 대상이 수락 → 요청자를 대상 위치로 이동. */
    public static void accept(ServerPlayerEntity target) {
        Request req = PENDING.remove(target.getUuid());
        if (req == null) {
            target.sendMessage(Text.literal("§7[TPA] 대기 중인 요청이 없습니다."), false);
            return;
        }
        if (target.getServer().getTicks() > req.expireTick) {
            target.sendMessage(Text.literal("§7[TPA] 요청이 만료되었습니다."), false);
            return;
        }
        ServerPlayerEntity requester = target.getServer().getPlayerManager().getPlayer(req.requester);
        if (requester == null) {
            target.sendMessage(Text.literal("§7[TPA] 요청자가 오프라인입니다."), false);
            return;
        }
        ServerWorld world = target.getServerWorld();
        requester.teleport(world, target.getX(), target.getY(), target.getZ(),
                requester.getYaw(), requester.getPitch());
        requester.sendMessage(Text.literal("§a[TPA] " + target.getGameProfile().getName()
                + " 님에게 이동했습니다.").formatted(Formatting.GREEN), false);
        target.sendMessage(Text.literal("§a[TPA] " + requester.getGameProfile().getName()
                + " 님의 요청을 수락했습니다."), false);
    }

    public static void deny(ServerPlayerEntity target) {
        Request req = PENDING.remove(target.getUuid());
        target.sendMessage(Text.literal(req == null
                ? "§7[TPA] 대기 중인 요청이 없습니다."
                : "§7[TPA] 요청을 거절했습니다."), false);
    }

    /** 만료 요청 정리(주기 호출). */
    public static void cleanup(long now) {
        PENDING.values().removeIf(r -> now > r.expireTick);
    }
}
