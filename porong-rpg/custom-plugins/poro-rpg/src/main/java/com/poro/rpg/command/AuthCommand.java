package com.poro.rpg.command;

import com.poro.rpg.auth.AuthService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Optional;

/**
 * 인게임 {@code /인증} — 디스코드 인증 코드 발급 (DL-138, 인게임 발급 → 봇 검증).
 *
 * <p>로그인된 플레이어의 uuid·name에 바인드된 1회용 코드를 발급하고, 디스코드 인증 버튼/모달에
 * 입력하도록 안내한다. 봇이 {@code POST /auth/verify}로 검증한다.
 */
public final class AuthCommand implements CommandExecutor {
    private static final String PREFIX = "§8[§e포로§8] ";

    private final AuthService authService;

    public AuthCommand(AuthService authService) {
        this.authService = Objects.requireNonNull(authService, "authService");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(PREFIX + "§c플레이어만 사용할 수 있는 명령어입니다.");
            return true;
        }

        Optional<String> code = authService.issueCode(
                player.getUniqueId().toString(), player.getName());
        if (code.isEmpty()) {
            player.sendMessage(PREFIX + "§c인증 코드 발급에 실패했습니다. 잠시 후 다시 시도해 주세요.");
            return true;
        }

        long minutes = Math.max(1, authService.ttlMillis() / 60_000L);
        player.sendMessage("");
        player.sendMessage(PREFIX + "§a디스코드 인증 코드가 발급되었습니다.");
        player.sendMessage("§f  코드: §e§l" + code.get());
        player.sendMessage("§7  디스코드 인증 채널의 §f인증 버튼§7을 눌러 코드를 입력하세요.");
        player.sendMessage("§7  유효시간: §f" + minutes + "분§7 · 1회용");
        player.sendMessage("");
        return true;
    }
}
