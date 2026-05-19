package com.poro.empire.command;

import com.poro.empire.combat.SkillService;
import com.poro.empire.combat.weapon.WeaponType;
import com.poro.empire.growth.engine.CatalystConfig;
import com.poro.empire.reputation.ReputationManager;
import com.poro.empire.reputation.ReputationTier;
import com.poro.empire.storage.PlayerData;
import com.poro.empire.storage.PlayerDataManager;
import com.poro.empire.util.HealthHudFormatter;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class EmpireCommand implements CommandExecutor, TabCompleter {
    private static final List<String> WEAPON_NAMES =
            List.of("sword", "hammer", "spear", "crossbow", "scythe", "staff");
    private static final List<String> REPUTATION_ACTIONS = List.of("add", "remove", "set");
    private static final List<String> SUBCOMMANDS =
            List.of("class", "skill", "info", "setclass", "hud", "reputation", "reload");

    private final PlayerDataManager playerDataManager;
    private final SkillService skillService;
    private final ReputationManager reputationManager;
    private final Plugin plugin;

    public EmpireCommand(PlayerDataManager playerDataManager, SkillService skillService,
                         ReputationManager reputationManager, Plugin plugin) {
        this.playerDataManager = playerDataManager;
        this.skillService = skillService;
        this.reputationManager = reputationManager;
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase(Locale.ROOT);
        return switch (subCommand) {
            case "class" -> handleClassCommand(sender, args);
            case "skill" -> handleSkillCommand(sender, args);
            case "info" -> handleInfoCommand(sender);
            case "setclass" -> handleSetClassCommand(sender, args);
            case "hud" -> handleHudCommand(sender);
            case "reputation" -> handleReputationCommand(sender, args);
            case "reload"     -> handleReloadCommand(sender);
            default -> {
                sendUsage(sender);
                yield true;
            }
        };
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return filterByPrefix(SUBCOMMANDS, args[0]);
        }
        if (args.length == 2 && "class".equalsIgnoreCase(args[0])) {
            return filterByPrefix(WEAPON_NAMES, args[1]);
        }
        if (args.length == 2 && "skill".equalsIgnoreCase(args[0])) {
            return filterByPrefix(skillService.getSkillKeys().stream().toList(), args[1]);
        }
        if (args.length == 2 && "setclass".equalsIgnoreCase(args[0])) {
            List<String> onlinePlayers = Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .toList();
            return filterByPrefix(onlinePlayers, args[1]);
        }
        if (args.length == 3 && "setclass".equalsIgnoreCase(args[0])) {
            return filterByPrefix(WEAPON_NAMES, args[2]);
        }
        if (args.length == 2 && "reputation".equalsIgnoreCase(args[0])) {
            return filterByPrefix(REPUTATION_ACTIONS, args[1]);
        }
        if (args.length == 3 && "reputation".equalsIgnoreCase(args[0])) {
            if (REPUTATION_ACTIONS.contains(args[1].toLowerCase(Locale.ROOT))) {
                List<String> onlinePlayers = Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .toList();
                return filterByPrefix(onlinePlayers, args[2]);
            }
        }
        if (args.length == 4 && "reputation".equalsIgnoreCase(args[0])) {
            if (REPUTATION_ACTIONS.contains(args[1].toLowerCase(Locale.ROOT))) {
                return filterByPrefix(List.of("10", "50", "100", "500"), args[3]);
            }
        }
        return List.of();
    }

    private boolean handleClassCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use /empire class.");
            return true;
        }
        if (!player.hasPermission("empire.class")) {
            player.sendMessage(ChatColor.RED + "You do not have permission: empire.class");
            return true;
        }
        if (args.length < 2) {
            player.sendMessage(ChatColor.YELLOW + "Usage: /empire class <sword|hammer|spear|crossbow|scythe|staff>");
            return true;
        }
        return handleClassSelection(player, args[1]);
    }

    private boolean handleSkillCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use /empire skill.");
            return true;
        }
        if (!player.hasPermission("empire.skill")) {
            player.sendMessage(ChatColor.RED + "You do not have permission: empire.skill");
            return true;
        }
        if (args.length < 2) {
            player.sendMessage(ChatColor.YELLOW + "Usage: /empire skill <skill_key>");
            return true;
        }
        return skillService.useSkill(player, args[1]);
    }

    private boolean handleInfoCommand(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use /empire info.");
            return true;
        }
        if (!player.hasPermission("empire.info")) {
            player.sendMessage(ChatColor.RED + "You do not have permission: empire.info");
            return true;
        }

        PlayerData playerData = playerDataManager.getOrCreate(player.getUniqueId());
        WeaponType weaponType = playerData.getWeaponType();
        double health = player.getHealth();

        player.sendMessage(ChatColor.GOLD + "=== Empire Debug Info ===");
        player.sendMessage(ChatColor.YELLOW + "무기 클래스: " + ChatColor.WHITE + weaponType.name().toLowerCase(Locale.ROOT));
        player.sendMessage(ChatColor.YELLOW + "튜토리얼: " + ChatColor.WHITE + (playerData.isTutorialComplete() ? "완료" : "미완료"));
        player.sendMessage(ChatColor.YELLOW + "체력: " + ChatColor.WHITE + String.format(Locale.US, "%.1f", health));
        return true;
    }

    private boolean handleSetClassCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("empire.admin.setclass")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission: empire.admin.setclass");
            return true;
        }
        if (args.length < 3) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /empire setclass <player> <sword|hammer|spear|crossbow|scythe|staff>");
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found or offline: " + args[1]);
            return true;
        }

        WeaponType weaponType = parseWeaponType(args[2]);
        if (weaponType == null) {
            sender.sendMessage(ChatColor.RED + "Invalid weapon type. Choose: sword, hammer, spear, crossbow, scythe, staff");
            return true;
        }

        playerDataManager.setWeaponType(target.getUniqueId(), weaponType);
        sender.sendMessage(ChatColor.GREEN + target.getName() + "의 무기 클래스를 "
                + weaponType.name().toLowerCase(Locale.ROOT) + "(으)로 설정했습니다.");
        target.sendMessage(ChatColor.YELLOW + "관리자에 의해 무기 클래스가 "
                + weaponType.name().toLowerCase(Locale.ROOT) + "(으)로 설정되었습니다.");
        return true;
    }

    private boolean handleHudCommand(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use /empire hud.");
            return true;
        }
        if (!player.hasPermission("empire.hud")) {
            player.sendMessage(ChatColor.RED + "You do not have permission: empire.hud");
            return true;
        }

        String hudMessage = HealthHudFormatter.format(player);
        player.sendActionBar(Component.text(hudMessage));
        player.sendMessage(ChatColor.GREEN + "HUD test message sent: " + ChatColor.WHITE + hudMessage);
        return true;
    }

    private boolean handleReputationCommand(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return showOwnReputation(sender);
        }

        String action = args[1].toLowerCase(Locale.ROOT);
        if (!REPUTATION_ACTIONS.contains(action)) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /empire reputation");
            sender.sendMessage(ChatColor.YELLOW + "Usage: /empire reputation add <player> <amount>");
            sender.sendMessage(ChatColor.YELLOW + "Usage: /empire reputation remove <player> <amount>");
            sender.sendMessage(ChatColor.YELLOW + "Usage: /empire reputation set <player> <amount>");
            return true;
        }

        if (!sender.hasPermission("empire.admin.reputation")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission: empire.admin.reputation");
            return true;
        }
        if (args.length < 4) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /empire reputation " + action + " <player> <amount>");
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[2]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found or offline: " + args[2]);
            return true;
        }

        Integer amount = parseAmount(args[3]);
        if (amount == null) {
            sender.sendMessage(ChatColor.RED + "Amount must be a non-negative integer.");
            return true;
        }

        return switch (action) {
            case "add" -> handleReputationAdd(sender, target, amount);
            case "remove" -> handleReputationRemove(sender, target, amount);
            case "set" -> handleReputationSet(sender, target, amount);
            default -> true;
        };
    }

    private boolean showOwnReputation(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use /empire reputation.");
            return true;
        }
        if (!player.hasPermission("empire.reputation")) {
            player.sendMessage(ChatColor.RED + "You do not have permission: empire.reputation");
            return true;
        }

        int reputation = reputationManager.getReputation(player.getUniqueId());
        ReputationTier tier = reputationManager.getTier(player.getUniqueId());
        ReputationTier nextTier = getNextTier(tier);

        player.sendMessage(ChatColor.GOLD + "=== Empire Reputation ===");
        player.sendMessage(ChatColor.YELLOW + "명성치: " + ChatColor.WHITE + reputation);
        player.sendMessage(ChatColor.YELLOW + "단계: " + ChatColor.WHITE + tier.getDisplayName()
                + ChatColor.DARK_GRAY + " (" + tier.name() + ")");

        if (nextTier != null) {
            int needed = Math.max(0, nextTier.getMinimumReputation() - reputation);
            player.sendMessage(ChatColor.YELLOW + "다음 단계: " + ChatColor.WHITE + nextTier.getDisplayName()
                    + ChatColor.GRAY + " (+" + needed + ")");
        } else {
            player.sendMessage(ChatColor.YELLOW + "다음 단계: " + ChatColor.WHITE + "최고 단계");
        }
        return true;
    }

    private boolean handleReputationAdd(CommandSender sender, Player target, int amount) {
        reputationManager.addReputation(target.getUniqueId(), amount);
        int updated = reputationManager.getReputation(target.getUniqueId());
        ReputationTier tier = reputationManager.getTier(target.getUniqueId());

        sender.sendMessage(ChatColor.GREEN + "Added " + amount + " reputation to " + target.getName()
                + ". Current: " + updated + " (" + tier.getDisplayName() + ")");
        target.sendMessage(ChatColor.YELLOW + "명성치가 " + ChatColor.GREEN + "+" + amount
                + ChatColor.YELLOW + " 변경되었습니다. 현재: " + ChatColor.WHITE + updated
                + ChatColor.GRAY + " (" + tier.getDisplayName() + ")");
        return true;
    }

    private boolean handleReputationRemove(CommandSender sender, Player target, int amount) {
        reputationManager.removeReputation(target.getUniqueId(), amount);
        int updated = reputationManager.getReputation(target.getUniqueId());
        ReputationTier tier = reputationManager.getTier(target.getUniqueId());

        sender.sendMessage(ChatColor.GREEN + "Removed " + amount + " reputation from " + target.getName()
                + ". Current: " + updated + " (" + tier.getDisplayName() + ")");
        target.sendMessage(ChatColor.YELLOW + "명성치가 " + ChatColor.RED + "-" + amount
                + ChatColor.YELLOW + " 변경되었습니다. 현재: " + ChatColor.WHITE + updated
                + ChatColor.GRAY + " (" + tier.getDisplayName() + ")");
        return true;
    }

    private boolean handleReputationSet(CommandSender sender, Player target, int amount) {
        reputationManager.setReputation(target.getUniqueId(), amount);
        int updated = reputationManager.getReputation(target.getUniqueId());
        ReputationTier tier = reputationManager.getTier(target.getUniqueId());

        sender.sendMessage(ChatColor.GREEN + "Set " + target.getName() + "'s reputation to "
                + updated + " (" + tier.getDisplayName() + ")");
        target.sendMessage(ChatColor.YELLOW + "명성치가 " + ChatColor.WHITE + updated
                + ChatColor.YELLOW + "(으)로 설정되었습니다."
                + ChatColor.GRAY + " (" + tier.getDisplayName() + ")");
        return true;
    }

    private boolean handleClassSelection(Player player, String input) {
        WeaponType weaponType = parseWeaponType(input);
        if (weaponType == null) {
            player.sendMessage(ChatColor.RED + "잘못된 무기 클래스입니다. 선택 가능: sword, hammer, spear, crossbow, scythe, staff");
            return true;
        }

        if (playerDataManager.hasSelectedWeapon(player.getUniqueId())) {
            WeaponType current = playerDataManager.getWeaponType(player.getUniqueId());
            player.sendMessage(ChatColor.RED + "이미 무기 클래스가 선택되어 있습니다: "
                    + current.name().toLowerCase(Locale.ROOT));
            return true;
        }

        playerDataManager.setWeaponType(player.getUniqueId(), weaponType);
        player.sendMessage(ChatColor.GREEN + "무기 클래스 선택: " + weaponType.name().toLowerCase(Locale.ROOT));
        return true;
    }

    private @Nullable WeaponType parseWeaponType(String input) {
        return switch (input.toLowerCase(Locale.ROOT)) {
            case "sword" -> WeaponType.SWORD;
            case "hammer" -> WeaponType.HAMMER;
            case "spear" -> WeaponType.SPEAR;
            case "crossbow" -> WeaponType.CROSSBOW;
            case "scythe" -> WeaponType.SCYTHE;
            case "staff" -> WeaponType.STAFF;
            default -> null;
        };
    }

    private @Nullable Integer parseAmount(String input) {
        try {
            int amount = Integer.parseInt(input);
            if (amount < 0) {
                return null;
            }
            return amount;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private @Nullable ReputationTier getNextTier(ReputationTier currentTier) {
        ReputationTier[] values = ReputationTier.values();
        int currentIndex = Arrays.asList(values).indexOf(currentTier);
        if (currentIndex < 0 || currentIndex + 1 >= values.length) {
            return null;
        }
        return values[currentIndex + 1];
    }

    private List<String> filterByPrefix(List<String> values, String prefix) {
        String loweredPrefix = prefix.toLowerCase(Locale.ROOT);
        return values.stream()
                .filter(value -> value.toLowerCase(Locale.ROOT).startsWith(loweredPrefix))
                .toList();
    }

    private boolean handleReloadCommand(CommandSender sender) {
        if (!sender.hasPermission("empire.admin.reload")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission: empire.admin.reload");
            return true;
        }
        plugin.reloadConfig();
        CatalystConfig.reload(plugin.getConfig());
        sender.sendMessage(ChatColor.GREEN + "[EmpireRPG] config.yml 리로드 완료. 강화 촉진제 요구량이 갱신됐습니다.");
        return true;
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(ChatColor.YELLOW + "Usage: /empire skill <skill_key>");
        sender.sendMessage(ChatColor.YELLOW + "Usage: /empire info");
        sender.sendMessage(ChatColor.YELLOW + "Usage: /empire hud");
        sender.sendMessage(ChatColor.YELLOW + "Usage: /empire setclass <player> <sword|hammer|spear|crossbow|scythe|staff>");
        sender.sendMessage(ChatColor.YELLOW + "Usage: /empire reputation [add|remove|set <player> <amount>]");
        sender.sendMessage(ChatColor.YELLOW + "Usage: /empire reload  §8(관리자 전용)");
    }
}
