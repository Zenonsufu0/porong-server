package com.poro.empire.command;

import com.poro.empire.pvp.PvpArenaGenerationService;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * /empire-genarenas [world] [x] [y] [z]
 * 인자 없이 실행 시 실행자의 현재 위치 기준 생성.
 */
public final class PvpArenaGenCommand implements CommandExecutor {

    private final PvpArenaGenerationService generationService;

    public PvpArenaGenCommand(PvpArenaGenerationService generationService) {
        this.generationService = generationService;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        World world;
        int x, y, z;

        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("§c콘솔에서 실행 시 /empire-genarenas <world> <x> <y> <z> 형식으로 입력하세요.");
                return true;
            }
            world = player.getWorld();
            x     = player.getLocation().getBlockX();
            y     = player.getLocation().getBlockY();
            z     = player.getLocation().getBlockZ();
        } else if (args.length == 4) {
            world = Bukkit.getWorld(args[0]);
            if (world == null) {
                sender.sendMessage("§c월드 '" + args[0] + "'을 찾을 수 없습니다.");
                return true;
            }
            try {
                x = Integer.parseInt(args[1]);
                y = Integer.parseInt(args[2]);
                z = Integer.parseInt(args[3]);
            } catch (NumberFormatException e) {
                sender.sendMessage("§c좌표는 정수여야 합니다.");
                return true;
            }
        } else {
            sender.sendMessage("§c사용법: /empire-genarenas [world x y z]");
            return true;
        }

        generationService.generate(sender, world, x, y, z);
        return true;
    }
}
