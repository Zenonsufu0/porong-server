package kr.zenon.rpg.field;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public final class FieldTeleportService {

    private final Plugin plugin;

    public FieldTeleportService(Plugin plugin) {
        this.plugin = plugin;
    }

    /**
     * config.yml 키 구조:
     *   fields:
     *     field_grove:
     *       world: world
     *       x: 100.5
     *       y: 64.0
     *       z: 200.5
     *       yaw: 0.0   # optional
     *       pitch: 0.0 # optional
     */
    public void teleportToField(Player player, String fieldId) {
        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("fields." + fieldId);
        if (sec == null) {
            player.sendMessage("§c[필드] §7" + fieldId + " 좌표가 config.yml에 설정되지 않았습니다.");
            player.closeInventory();
            return;
        }

        String worldName = sec.getString("world", "world");
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            player.sendMessage("§c[필드] §7월드 '" + worldName + "'을 찾을 수 없습니다.");
            player.closeInventory();
            return;
        }

        double x     = sec.getDouble("x");
        double y     = sec.getDouble("y");
        double z     = sec.getDouble("z");
        float  yaw   = (float) sec.getDouble("yaw", 0.0);
        float  pitch = (float) sec.getDouble("pitch", 0.0);

        player.closeInventory();
        player.teleport(new Location(world, x, y, z, yaw, pitch));
        player.sendMessage("§a[필드] §f" + fieldId + " §7로 이동합니다.");
    }
}
