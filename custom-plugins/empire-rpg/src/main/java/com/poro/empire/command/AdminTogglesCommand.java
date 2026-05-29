package com.poro.empire.command;

import com.poro.empire.admin.AdminTogglesService;
import com.poro.empire.admin.AdminTogglesService.Toggle;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public final class AdminTogglesCommand implements CommandExecutor {

    private final AdminTogglesService service;

    public AdminTogglesCommand(AdminTogglesService service) {
        this.service = service;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (args.length == 0 || "list".equalsIgnoreCase(args[0])) {
            sender.sendMessage("§e[관리자 토글]");
            for (var e : service.all().entrySet()) {
                sender.sendMessage("§7- §f" + e.getKey().name() + " §7(" + e.getKey().displayName + "): "
                        + (e.getValue() ? "§a[ON]" : "§8[OFF]"));
            }
            return true;
        }
        Toggle t;
        try { t = Toggle.valueOf(args[0].toUpperCase(Locale.ROOT)); }
        catch (Exception e) { sender.sendMessage("§c알 수 없는 플래그: " + args[0]); return true; }

        if (args.length >= 2) {
            String mode = args[1].toLowerCase(Locale.ROOT);
            if (mode.equals("on"))  { service.setOn(t);  sender.sendMessage("§a[관리자] " + t + " §a[ON]"); return true; }
            if (mode.equals("off")) { service.setOff(t); sender.sendMessage("§7[관리자] " + t + " §8[OFF]"); return true; }
        }
        boolean next = service.toggle(t);
        sender.sendMessage("§e[관리자] " + t + " §7→ " + (next ? "§a[ON]" : "§8[OFF]"));
        return true;
    }
}
