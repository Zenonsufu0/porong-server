package com.poro.empire.command;

import com.poro.empire.combat.weapon.WeaponType;
import com.poro.empire.init.ClassInitService;
import com.poro.empire.storage.PlayerDataManager;
import org.bukkit.Bukkit;
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
 * /직업 <플레이어> <검|도끼|창|석궁|낫|스태프>
 * 운용자 전용 — 대상 플레이어의 직업을 변경하고 초기 장비를 지급한다.
 */
public class ClassAdminCommand implements CommandExecutor, TabCompleter {

    private static final String PREFIX = "§8[§e포로§8] ";
    private static final List<String> WEAPON_NAMES_KR =
            List.of("검", "도끼", "창", "석궁", "낫", "스태프");

    private final ClassInitService classInitService;
    private final PlayerDataManager playerDataManager;

    public ClassAdminCommand(ClassInitService classInitService, PlayerDataManager playerDataManager) {
        this.classInitService = classInitService;
        this.playerDataManager = playerDataManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("empire.admin")) {
            sender.sendMessage(PREFIX + "§c권한이 없습니다.");
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(PREFIX + "§7사용법: /직업 <플레이어> <검|도끼|창|석궁|낫|스태프>");
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage(PREFIX + "§c" + args[0] + "§7 플레이어를 찾을 수 없거나 오프라인입니다.");
            return true;
        }

        WeaponType weaponType = parseWeaponType(args[1]);
        if (weaponType == null) {
            sender.sendMessage(PREFIX + "§c알 수 없는 직업입니다. 선택 가능: 검, 도끼, 창, 석궁, 낫, 스태프");
            return true;
        }

        playerDataManager.setWeaponType(target.getUniqueId(), weaponType);
        classInitService.grantStarterEquipment(target, weaponType);

        sender.sendMessage(PREFIX + "§f" + target.getName() + "§7의 직업을 §e" + displayName(weaponType) + "§7(으)로 설정하고 초기 장비를 지급했습니다.");
        target.sendMessage(PREFIX + "§7관리자에 의해 직업이 §e" + displayName(weaponType) + "§7(으)로 설정되었습니다.");
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
        if (args.length == 2) {
            String prefix = args[1];
            return WEAPON_NAMES_KR.stream()
                    .filter(n -> n.startsWith(prefix))
                    .toList();
        }
        return List.of();
    }

    private @Nullable WeaponType parseWeaponType(String input) {
        return switch (input) {
            case "검"   -> WeaponType.SWORD;
            case "도끼" -> WeaponType.AXE;
            case "창"   -> WeaponType.SPEAR;
            case "석궁" -> WeaponType.CROSSBOW;
            case "낫"   -> WeaponType.SCYTHE;
            case "스태프" -> WeaponType.STAFF;
            default -> null;
        };
    }

    private String displayName(WeaponType type) {
        return switch (type) {
            case SWORD    -> "검";
            case AXE      -> "도끼";
            case SPEAR    -> "창";
            case CROSSBOW -> "석궁";
            case SCYTHE   -> "낫";
            case STAFF    -> "스태프";
            case NONE     -> "없음";
        };
    }
}
