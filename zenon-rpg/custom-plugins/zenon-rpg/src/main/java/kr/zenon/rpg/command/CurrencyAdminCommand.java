package kr.zenon.rpg.command;

import kr.zenon.rpg.combat.weapon.WeaponType;
import kr.zenon.rpg.growth.GrowthStateStore;
import kr.zenon.rpg.growth.engine.PlayerGrowthState;
import kr.zenon.rpg.scoreboard.ScoreboardService;
import kr.zenon.rpg.storage.PlayerDataManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * 운용자 전용 재화 지급 한글 커맨드 — 명령 이름에 따라 대상 재화가 결정된다.
 *
 * <ul>
 *   <li>/골드 &lt;플레이어&gt; [수량]   (기본 {@value #DEFAULT_GOLD})</li>
 *   <li>/강화석 &lt;플레이어&gt; [수량] (기본 {@value #DEFAULT_STONE})</li>
 *   <li>/큐브 &lt;플레이어&gt; [수량]   (기본 {@value #DEFAULT_CUBE})</li>
 *   <li>/큐브조각 &lt;플레이어&gt; [수량] (기본 {@value #DEFAULT_FRAG})</li>
 * </ul>
 *
 * <p>수량 생략 시 재화별 기본값. 음수 입력 시 회수. 내부적으로 {@code /poro-currency}와 동일한
 * {@code addCurrency/consumeCurrency} 경로를 쓴다(DL-129 검증 편의). 온라인 대상은 스코어보드 즉시 갱신.</p>
 */
public final class CurrencyAdminCommand implements CommandExecutor, TabCompleter {

    private static final String PREFIX = "§8[§e포로§8] ";
    private static final long DEFAULT_GOLD  = 10000L;
    private static final long DEFAULT_STONE = 100L;
    private static final long DEFAULT_CUBE  = 64L;
    private static final long DEFAULT_FRAG  = 100L;

    private final GrowthStateStore growthStateStore;
    private final PlayerDataManager playerDataManager;
    private final ScoreboardService scoreboardService;

    public CurrencyAdminCommand(GrowthStateStore growthStateStore,
                                PlayerDataManager playerDataManager,
                                ScoreboardService scoreboardService) {
        this.growthStateStore = growthStateStore;
        this.playerDataManager = playerDataManager;
        this.scoreboardService = scoreboardService;
    }

    /** 명령 이름 → (재화코드, 표기명, 기본수량). 미지원 명령은 null. */
    private static @Nullable Currency currencyOf(String commandName) {
        return switch (commandName.toLowerCase(Locale.ROOT)) {
            case "poro-gold"     -> new Currency("gold",              "골드",   DEFAULT_GOLD);
            case "poro-stone"    -> new Currency("mat_stone_enhance", "강화석", DEFAULT_STONE);
            case "poro-cube"     -> new Currency("mat_cube",          "큐브",   DEFAULT_CUBE);
            case "poro-cubefrag" -> new Currency("mat_cube_fragment", "큐브조각", DEFAULT_FRAG);
            default -> null;
        };
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("poro.admin")) {
            sender.sendMessage(PREFIX + "§c권한이 없습니다.");
            return true;
        }
        Currency cur = currencyOf(command.getName());
        if (cur == null) {
            sender.sendMessage(PREFIX + "§c내부 오류: 알 수 없는 재화 명령(" + command.getName() + ").");
            return true;
        }
        if (args.length < 1) {
            sender.sendMessage(PREFIX + "§7사용법: /" + label + " <플레이어> [수량]  §8(생략 시 " + cur.defaultAmount + ")");
            return true;
        }

        OfflinePlayer target = resolvePlayer(args[0]);
        if (target == null || (target.getName() == null && !target.hasPlayedBefore() && !target.isOnline())) {
            sender.sendMessage(PREFIX + "§c" + args[0] + "§7 플레이어를 찾을 수 없습니다.");
            return true;
        }
        UUID uuid = target.getUniqueId();
        String name = target.getName() != null ? target.getName() : args[0];

        long delta = args.length >= 2 ? parseLong(args[1], cur.defaultAmount) : cur.defaultAmount;

        PlayerGrowthState st = ensureGrowthState(uuid);
        if (st == null) {
            sender.sendMessage(PREFIX + "§c" + name + "§7 성장 데이터 없음(직업 미선택/미접속). 온라인+직업 선택 후 시도하세요.");
            return true;
        }

        if (delta >= 0) st.addCurrency(cur.code, delta);
        else            st.consumeCurrency(cur.code, -delta);
        long balance = st.currency(cur.code);

        sender.sendMessage(PREFIX + "§a" + name + "§7 " + cur.label + " §e"
                + (delta >= 0 ? "+" : "") + delta + " §7→ 잔액 §f" + balance);

        Player online = Bukkit.getPlayerExact(name);
        if (online != null) {
            online.sendMessage(PREFIX + "§7관리자에게 " + cur.label + " §e" + (delta >= 0 ? "+" : "") + delta + "§7을(를) 받았습니다.");
            if (scoreboardService != null) scoreboardService.refresh(online); // 큐브/골드 수량 즉시 반영
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(n -> n.toLowerCase(Locale.ROOT).startsWith(prefix))
                    .toList();
        }
        return List.of();
    }

    // ─── 헬퍼 ──────────────────────────────────────────────────────────

    private @Nullable OfflinePlayer resolvePlayer(String arg) {
        Player online = Bukkit.getPlayerExact(arg);
        if (online != null) return online;
        return Bukkit.getOfflinePlayerIfCached(arg);
    }

    private long parseLong(String s, long def) {
        try { return Long.parseLong(s.trim()); } catch (Exception e) { return def; }
    }

    private @Nullable PlayerGrowthState ensureGrowthState(UUID uuid) {
        PlayerGrowthState st = growthStateStore.get(uuid).orElse(null);
        if (st != null) return st;
        WeaponType wt = playerDataManager.getWeaponType(uuid);
        if (wt == WeaponType.NONE) return null;
        return growthStateStore.getOrCreate(uuid, wt.name().toLowerCase(Locale.ROOT));
    }

    private record Currency(String code, String label, long defaultAmount) {
    }
}
