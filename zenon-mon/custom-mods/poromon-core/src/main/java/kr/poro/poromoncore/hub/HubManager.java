package kr.poro.poromoncore.hub;

import kr.poro.poromoncore.PoroMonCore;
import kr.poro.poromoncore.config.ConfigManager;
import kr.poro.poromoncore.config.CoreConfig;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

/**
 * 허브 텔레포트 (menu_design.md §3 슬롯19, commands.md /poromon hub).
 * 목적지는 core.json §hub. teleportCommandEnabled=false면 거부.
 */
public final class HubManager {
    private HubManager() {}

    /** 허브로 텔레포트. 성공 시 true. */
    public static boolean teleportToHub(ServerPlayerEntity player) {
        CoreConfig.Hub cfg = ConfigManager.core().hub;
        if (!cfg.teleportCommandEnabled) {
            player.sendMessage(Text.literal("§c[PoroMon]§r 허브 텔레포트가 비활성화되어 있습니다."), false);
            return false;
        }
        MinecraftServer server = player.getServer();
        if (server == null) return false;

        Identifier worldId = Identifier.tryParse(cfg.world);
        if (worldId == null) {
            PoroMonCore.LOGGER.error("[Hub] world id 파싱 실패: {}", cfg.world);
            return false;
        }
        RegistryKey<World> key = RegistryKey.of(RegistryKeys.WORLD, worldId);
        ServerWorld world = server.getWorld(key);
        if (world == null) {
            PoroMonCore.LOGGER.error("[Hub] 월드 미존재: {}", cfg.world);
            player.sendMessage(Text.literal("§c[PoroMon]§r 허브 월드를 찾을 수 없습니다(설정 확인)."), false);
            return false;
        }

        CoreConfig.Spawn s = cfg.spawn;
        player.teleport(world, s.x, s.y, s.z, s.yaw, s.pitch);
        player.sendMessage(Text.literal("[PoroMon] 허브로 이동했습니다.").formatted(Formatting.GREEN), false);
        return true;
    }
}
