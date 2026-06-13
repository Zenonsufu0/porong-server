package com.poro.rpg.command;

import com.poro.rpg.storage.PlayerDataManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

/**
 * /정예 [on|off] — 필드 정예 모드 토글 (플레이어 본인).
 *
 * <p>ON이면 본인 주변 필드 웨이브가 정예 몹으로 스폰된다(수 적고 강함, {@code FieldSpawnService}). 인자 없으면 토글.
 * 세션 메모리 — 재접속 시 OFF로 초기화(DL-129 추가#5).</p>
 */
public final class FieldEliteCommand implements CommandExecutor, TabCompleter {

    private static final String PREFIX = "§8[§e포로§8] ";
    private final PlayerDataManager playerDataManager;

    public FieldEliteCommand(PlayerDataManager playerDataManager) {
        this.playerDataManager = playerDataManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("플레이어만 사용할 수 있습니다.");
            return true;
        }
        boolean on;
        if (args.length >= 1) {
            String a = args[0].toLowerCase(Locale.ROOT);
            if (a.equals("on") || a.equals("켜기") || a.equals("정예")) {
                on = true;
            } else if (a.equals("off") || a.equals("끄기") || a.equals("일반")) {
                on = false;
            } else {
                player.sendMessage(PREFIX + "§7사용법: /정예 [on|off] §8(생략 시 토글)");
                return true;
            }
            playerDataManager.setFieldElite(player.getUniqueId(), on);
        } else {
            on = playerDataManager.toggleFieldElite(player.getUniqueId());
        }

        if (on) {
            player.sendMessage(PREFIX + "§a§l정예 모드 ON §7— 다음 웨이브부터 §f정예 몹§7이 등장합니다. §8(수 적고 강함)");
        } else {
            player.sendMessage(PREFIX + "§7정예 모드 §fOFF §7— 일반 몹으로 돌아갑니다.");
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return List.of("on", "off").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase(Locale.ROOT)))
                    .toList();
        }
        return List.of();
    }
}
