package kr.zenon.rpg.command;

import kr.zenon.rpg.listener.AdminGuiListener;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class AdminHubCommand implements CommandExecutor {
    private final AdminGuiListener listener;

    public AdminHubCommand(AdminGuiListener listener) {
        this.listener = listener;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§c플레이어 전용 명령입니다.");
            return true;
        }
        listener.openHub(player);
        return true;
    }
}
