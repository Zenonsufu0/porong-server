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

    private static final String PREFIX = "§8[§e포로§8] ";
    private static final String NO_ADMIN = PREFIX + "§c권한이 없습니다.";

    private static final List<String> WEAPON_NAMES =
            List.of("sword", "axe", "spear", "crossbow", "scythe", "staff");
    private static final List<String> REPUTATION_ACTIONS = List.of("add", "remove", "set");
    private static final List<String> SUBCOMMANDS = List.of(
            "class", "skill", "info", "setclass", "hud", "reputation", "reload",
            "gui", "gold", "item", "estate", "enhance", "potential", "succession",
            "potion", "buff", "boss", "check", "visit", "island");

    // GUI 이름 목록 (탭자동완성)
    private static final List<String> GUI_NAMES = List.of(
            "main", "equipment", "territory", "boss", "field",
            "enhance", "potential", "succession", "engraving", "character",
            "storage", "workshop", "farm", "shop", "auction", "estate-setting",
            "party", "party-list", "boss-info", "clear-record");

    private static final List<String> SLOTS =
            List.of("WEAPON", "HELMET", "CHESTPLATE", "LEGGINGS", "BOOTS");
    private static final List<String> GRADES =
            List.of("COMMON", "RARE", "EPIC", "UNIQUE", "LEGENDARY");
    private static final List<String> RANKS =
            List.of("PIONEER", "KNIGHT", "BARON_JR", "BARON", "VISCOUNT", "EARL", "MARQUESS", "DUKE");

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

    // ─── 메인 디스패처 ──────────────────────────────────────────────

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) { sendUsage(sender); return true; }

        return switch (args[0].toLowerCase(Locale.ROOT)) {
            // 기존 커맨드
            case "class"      -> handleClassCommand(sender, args);
            case "skill"      -> handleSkillCommand(sender, args);
            case "info"       -> handleInfoCommand(sender);
            case "setclass"   -> handleSetClassCommand(sender, args);
            case "hud"        -> handleHudCommand(sender);
            case "reputation" -> handleReputationCommand(sender, args);
            case "reload"     -> handleReloadCommand(sender, args);
            // 관리자 커맨드
            case "gui"        -> handleGuiCommand(sender, args);
            case "gold"       -> handleGoldCommand(sender, args);
            case "item"       -> handleItemCommand(sender, args);
            case "estate"     -> handleEstateCommand(sender, args);
            case "enhance"    -> handleEnhanceAdminCommand(sender, args);
            case "potential"  -> handlePotentialCommand(sender, args);
            case "succession" -> handleSuccessionCommand(sender, args);
            case "potion"     -> handlePotionCommand(sender, args);
            case "buff"       -> handleBuffCommand(sender, args);
            case "boss"       -> handleBossCommand(sender, args);
            case "check"      -> handleCheckCommand(sender, args);
            // 내부 ClickEvent 전용
            case "visit"      -> handleVisitCommand(sender, args);
            case "island"     -> handleIslandCommand(sender, args);
            default -> { sendUsage(sender); yield true; }
        };
    }

    // ─── 탭 자동완성 ──────────────────────────────────────────────

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) return filterByPrefix(SUBCOMMANDS, args[0]);

        String sub = args[0].toLowerCase(Locale.ROOT);
        if (args.length == 2) {
            return switch (sub) {
                case "class"                            -> filterByPrefix(WEAPON_NAMES, args[1]);
                case "skill"                            -> filterByPrefix(skillService.getSkillKeys().stream().toList(), args[1]);
                case "setclass", "gui", "gold", "check" -> filterByPrefix(onlinePlayerNames(), args[1]);
                case "item"                             -> filterByPrefix(List.of("give"), args[1]);
                case "estate"                           -> filterByPrefix(List.of("rank", "slot", "produce", "public", "setting", "member", "permission"), args[1]);
                case "enhance"                          -> filterByPrefix(List.of("set"), args[1]);
                case "potential"                        -> filterByPrefix(List.of("reroll", "set"), args[1]);
                case "succession"                       -> filterByPrefix(List.of("give"), args[1]);
                case "potion"                           -> filterByPrefix(List.of("reset"), args[1]);
                case "buff"                             -> filterByPrefix(List.of("clear", "check"), args[1]);
                case "boss"                             -> filterByPrefix(List.of("spawn", "kill", "clearrecord", "drop"), args[1]);
                case "reload"                           -> filterByPrefix(List.of("drops", "recipes"), args[1]);
                case "reputation"                       -> filterByPrefix(REPUTATION_ACTIONS, args[1]);
                default -> List.of();
            };
        }

        if (args.length == 3) {
            return switch (sub) {
                case "setclass" -> filterByPrefix(WEAPON_NAMES, args[2]);
                case "gui"      -> filterByPrefix(GUI_NAMES, args[2]);
                case "gold"     -> filterByPrefix(List.of("give", "take", "set", "check"), args[2]);
                case "check"    -> filterByPrefix(List.of("estate", "equipment"), args[2]);
                case "reputation" -> REPUTATION_ACTIONS.contains(args[1].toLowerCase(Locale.ROOT))
                        ? filterByPrefix(onlinePlayerNames(), args[2]) : List.of();
                case "estate"   -> switch (args[1].toLowerCase(Locale.ROOT)) {
                    case "rank"    -> filterByPrefix(onlinePlayerNames(), args[2]);
                    case "slot"    -> filterByPrefix(List.of("set", "reset", "list"), args[2]);
                    case "produce", "public" -> filterByPrefix(onlinePlayerNames(), args[2]);
                    case "setting" -> filterByPrefix(onlinePlayerNames(), args[2]);
                    case "member"  -> filterByPrefix(List.of("add", "remove", "list"), args[2]);
                    case "permission" -> filterByPrefix(onlinePlayerNames(), args[2]);
                    default -> List.of();
                };
                case "enhance"    -> "set".equalsIgnoreCase(args[1]) ? filterByPrefix(onlinePlayerNames(), args[2]) : List.of();
                case "potential"  -> filterByPrefix(onlinePlayerNames(), args[2]);
                case "succession" -> "give".equalsIgnoreCase(args[1]) ? filterByPrefix(onlinePlayerNames(), args[2]) : List.of();
                case "boss"       -> switch (args[1].toLowerCase(Locale.ROOT)) {
                    case "spawn", "kill", "drop" -> filterByPrefix(List.of(
                            "field_boss_prairie", "field_boss_mine", "field_boss_waterway",
                            "field_boss_outpost", "field_boss_ruins",
                            "season_boss_1", "season_boss_2", "season_boss_3", "season_boss_final"), args[2]);
                    case "clearrecord" -> filterByPrefix(List.of("set", "reset"), args[2]);
                    default -> List.of();
                };
                default -> List.of();
            };
        }

        if (args.length == 4) {
            return switch (sub) {
                case "estate" -> switch (args[1].toLowerCase(Locale.ROOT)) {
                    case "rank" -> filterByPrefix(RANKS, args[3]);
                    case "slot" -> "set".equalsIgnoreCase(args[2]) ? filterByPrefix(onlinePlayerNames(), args[3]) : List.of();
                    case "setting" -> filterByPrefix(List.of(
                            "seed_protect", "time_mode", "weather_mode", "crop_protect", "water_protect"), args[3]);
                    case "member" -> switch (args[2].toLowerCase(Locale.ROOT)) {
                        case "add" -> filterByPrefix(onlinePlayerNames(), args[3]);
                        default -> List.of();
                    };
                    case "permission" -> filterByPrefix(List.of(
                            "VISITOR", "RESIDENT", "VICE_LORD"), args[3]);
                    default -> List.of();
                };
                case "enhance"   -> "set".equalsIgnoreCase(args[1]) ? filterByPrefix(SLOTS, args[3]) : List.of();
                case "potential" -> filterByPrefix(
                        "reroll".equalsIgnoreCase(args[1]) ? SLOTS : List.of("set"), args[3]);
                default -> List.of();
            };
        }

        return List.of();
    }

    // ─── 기존 커맨드 핸들러 ──────────────────────────────────────

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
            player.sendMessage(ChatColor.YELLOW + "Usage: /empire class <sword|axe|spear|crossbow|scythe|staff>");
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
        player.sendMessage(ChatColor.GOLD + "=== Empire Debug Info ===");
        player.sendMessage(ChatColor.YELLOW + "무기 클래스: " + ChatColor.WHITE + weaponType.name().toLowerCase(Locale.ROOT));
        player.sendMessage(ChatColor.YELLOW + "튜토리얼: " + ChatColor.WHITE + (playerData.isTutorialComplete() ? "완료" : "미완료"));
        player.sendMessage(ChatColor.YELLOW + "체력: " + ChatColor.WHITE
                + String.format(Locale.US, "%.1f", player.getHealth()));
        return true;
    }

    private boolean handleSetClassCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("empire.admin.setclass")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission: empire.admin.setclass");
            return true;
        }
        if (args.length < 3) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /empire setclass <player> <sword|axe|spear|crossbow|scythe|staff>");
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found or offline: " + args[1]);
            return true;
        }
        WeaponType weaponType = parseWeaponType(args[2]);
        if (weaponType == null) {
            sender.sendMessage(ChatColor.RED + "Invalid weapon type. Choose: sword, axe, spear, crossbow, scythe, staff");
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
        if (args.length == 1) return showOwnReputation(sender);
        String action = args[1].toLowerCase(Locale.ROOT);
        if (!REPUTATION_ACTIONS.contains(action)) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /empire reputation [add|remove|set <player> <amount>]");
            return true;
        }
        if (!sender.hasPermission("empire.admin.reputation")) {
            sender.sendMessage(NO_ADMIN);
            return true;
        }
        if (args.length < 4) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /empire reputation " + action + " <player> <amount>");
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[2]);
        if (target == null) { sender.sendMessage(ChatColor.RED + "Player not found or offline: " + args[2]); return true; }
        Integer amount = parseAmount(args[3]);
        if (amount == null) { sender.sendMessage(ChatColor.RED + "Amount must be a non-negative integer."); return true; }
        return switch (action) {
            case "add"    -> handleReputationAdd(sender, target, amount);
            case "remove" -> handleReputationRemove(sender, target, amount);
            case "set"    -> handleReputationSet(sender, target, amount);
            default -> true;
        };
    }

    // ─── 관리자 커맨드 핸들러 ──────────────────────────────────

    private boolean handleGuiCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("empire.admin")) { sender.sendMessage(NO_ADMIN); return true; }
        if (args.length < 3) {
            sender.sendMessage(PREFIX + "§7Usage: /empire gui <player> <" + String.join("|", GUI_NAMES) + ">");
            return true;
        }
        Player target = requireOnlinePlayer(sender, args[1]);
        if (target == null) return true;
        String guiName = args[2].toLowerCase(Locale.ROOT);
        sender.sendMessage(PREFIX + "§a" + target.getName() + "에게 §f" + guiName + " §aGUI를 강제 오픈했습니다. §8(GUI 클래스 구현 후 연동)");
        return true;
    }

    private boolean handleGoldCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("empire.admin")) { sender.sendMessage(NO_ADMIN); return true; }
        if (args.length < 3) {
            sender.sendMessage(PREFIX + "§7Usage: /empire gold <give|take|set|check> <player> [amount]");
            return true;
        }
        String action = args[1].toLowerCase(Locale.ROOT);
        Player target = requireOnlinePlayer(sender, args[2]);
        if (target == null) return true;

        if ("check".equals(action)) {
            sender.sendMessage(PREFIX + "§f" + target.getName() + " §7골드 조회 §8(골드 시스템 구현 후 연동)");
            return true;
        }
        if (args.length < 4) {
            sender.sendMessage(PREFIX + "§7Usage: /empire gold " + action + " <player> <amount>");
            return true;
        }
        Long amount = parseLong(sender, args[3]);
        if (amount == null) return true;
        sender.sendMessage(PREFIX + "§f" + target.getName() + " §7골드 §f" + action + " §e" + amount + "G §8(골드 시스템 구현 후 연동)");
        return true;
    }

    private boolean handleItemCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("empire.admin")) { sender.sendMessage(NO_ADMIN); return true; }
        if (args.length < 2) {
            sender.sendMessage(PREFIX + "§7Usage: /empire item give <player> <item-id> [amount]");
            sender.sendMessage(PREFIX + "§7Usage: /empire item storage give <player> <item-id> <amount>");
            return true;
        }
        boolean storage = args.length >= 3 && "storage".equalsIgnoreCase(args[1])
                && "give".equalsIgnoreCase(args[2]);
        boolean inv     = "give".equalsIgnoreCase(args[1]);
        if (!inv && !storage) {
            sender.sendMessage(PREFIX + "§7Usage: /empire item give <player> <item-id> [amount]");
            return true;
        }
        int playerArgIdx = storage ? 3 : 2;
        int itemArgIdx   = storage ? 4 : 3;
        if (args.length <= itemArgIdx) {
            sender.sendMessage(PREFIX + "§7Usage: /empire item " + (storage ? "storage give" : "give") + " <player> <item-id> [amount]");
            return true;
        }
        Player target = requireOnlinePlayer(sender, args[playerArgIdx]);
        if (target == null) return true;
        String itemId  = args[itemArgIdx];
        int    amount  = args.length > itemArgIdx + 1 ? parseInt(args[itemArgIdx + 1], 1) : 1;
        String dest    = storage ? "영지 창고" : "인벤토리";
        sender.sendMessage(PREFIX + "§f" + target.getName() + "§7의 §f" + dest + "§7에 §e" + itemId
                + " §f×" + amount + " §7지급 §8(아이템 레지스트리 구현 후 연동)");
        return true;
    }

    private boolean handleEstateCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("empire.admin")) { sender.sendMessage(NO_ADMIN); return true; }
        if (args.length < 2) {
            sender.sendMessage(PREFIX + "§7Usage: /empire estate <rank|slot|produce|public|setting|member|permission> ...");
            return true;
        }
        return switch (args[1].toLowerCase(Locale.ROOT)) {
            case "rank"       -> handleEstateRank(sender, args);
            case "slot"       -> handleEstateSlot(sender, args);
            case "produce"    -> handleEstateProduce(sender, args);
            case "public"     -> handleEstatePublic(sender, args);
            case "setting"    -> handleEstateSetting(sender, args);
            case "member"     -> handleEstateMember(sender, args);
            case "permission" -> handleEstatePermission(sender, args);
            default -> { sender.sendMessage(PREFIX + "§7알 수 없는 하위커맨드: " + args[1]); yield true; }
        };
    }

    private boolean handleEstateRank(CommandSender sender, String[] args) {
        if (args.length < 5 || !"set".equalsIgnoreCase(args[2])) {
            sender.sendMessage(PREFIX + "§7Usage: /empire estate rank set <player> <rank>");
            return true;
        }
        Player target = requireOnlinePlayer(sender, args[3]);
        if (target == null) return true;
        sender.sendMessage(PREFIX + "§f" + target.getName() + " §7작위를 §e" + args[4] + " §7로 설정 §8(LifeEngine 연동 후 적용)");
        return true;
    }

    private boolean handleEstateSlot(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(PREFIX + "§7Usage: /empire estate slot <set|reset|list> <player> [num] [type]");
            return true;
        }
        return switch (args[2].toLowerCase(Locale.ROOT)) {
            case "set" -> {
                if (args.length < 6) { sender.sendMessage(PREFIX + "§7Usage: /empire estate slot set <player> <num> <herb|ore|workshop>"); yield true; }
                Player t = requireOnlinePlayer(sender, args[3]); if (t == null) yield true;
                sender.sendMessage(PREFIX + "§f" + t.getName() + " §7슬롯 §f" + args[4] + "§7에 §e" + args[5] + " §7배정 §8(LifeEngine 연동 후 적용)");
                yield true;
            }
            case "reset" -> {
                if (args.length < 4) { sender.sendMessage(PREFIX + "§7Usage: /empire estate slot reset <player>"); yield true; }
                Player t = requireOnlinePlayer(sender, args[3]); if (t == null) yield true;
                sender.sendMessage(PREFIX + "§f" + t.getName() + " §7시설 슬롯 전체 초기화 §8(LifeEngine 연동 후 적용)");
                yield true;
            }
            case "list" -> {
                if (args.length < 4) { sender.sendMessage(PREFIX + "§7Usage: /empire estate slot list <player>"); yield true; }
                Player t = requireOnlinePlayer(sender, args[3]); if (t == null) yield true;
                sender.sendMessage(PREFIX + "§f" + t.getName() + " §7시설 슬롯 현황 조회 §8(LifeEngine 연동 후 적용)");
                yield true;
            }
            default -> { sender.sendMessage(PREFIX + "§7사용법: /empire estate slot <set|reset|list>"); yield true; }
        };
    }

    private boolean handleEstateProduce(CommandSender sender, String[] args) {
        if (args.length < 3) { sender.sendMessage(PREFIX + "§7Usage: /empire estate produce <player>"); return true; }
        Player t = requireOnlinePlayer(sender, args[2]); if (t == null) return true;
        sender.sendMessage(PREFIX + "§f" + t.getName() + " §7영지 즉시 생산 1사이클 §8(LifeEngine 연동 후 적용)");
        return true;
    }

    private boolean handleEstatePublic(CommandSender sender, String[] args) {
        if (args.length < 5 || !"set".equalsIgnoreCase(args[2])) {
            sender.sendMessage(PREFIX + "§7Usage: /empire estate public set <player> <true|false>"); return true;
        }
        Player t = requireOnlinePlayer(sender, args[3]); if (t == null) return true;
        sender.sendMessage(PREFIX + "§f" + t.getName() + " §7영지 공개 여부 → §e" + args[4] + " §8(연동 예정)");
        return true;
    }

    private boolean handleEstateSetting(CommandSender sender, String[] args) {
        if (args.length < 5 || !"set".equalsIgnoreCase(args[2])) {
            sender.sendMessage(PREFIX + "§7Usage: /empire estate setting set <player> <key> <value>"); return true;
        }
        Player t = requireOnlinePlayer(sender, args[3]); if (t == null) return true;
        sender.sendMessage(PREFIX + "§f" + t.getName() + " §7세부설정 §e" + args[4] + " → §f" + (args.length > 5 ? args[5] : "?") + " §8(연동 예정)");
        return true;
    }

    private boolean handleEstateMember(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(PREFIX + "§7Usage: /empire estate member <add|remove|list> <owner> [member] [role]"); return true;
        }
        return switch (args[2].toLowerCase(Locale.ROOT)) {
            case "add" -> {
                if (args.length < 6) { sender.sendMessage(PREFIX + "§7Usage: /empire estate member add <owner> <member> <RESIDENT|VICE_LORD>"); yield true; }
                sender.sendMessage(PREFIX + "§f" + args[3] + " §7영지에 §f" + args[4] + " §7(" + args[5] + ") 추가 §8(연동 예정)");
                yield true;
            }
            case "remove" -> {
                if (args.length < 5) { sender.sendMessage(PREFIX + "§7Usage: /empire estate member remove <owner> <member>"); yield true; }
                sender.sendMessage(PREFIX + "§f" + args[3] + " §7영지에서 §f" + args[4] + " §7제거 §8(연동 예정)");
                yield true;
            }
            case "list" -> {
                if (args.length < 4) { sender.sendMessage(PREFIX + "§7Usage: /empire estate member list <owner>"); yield true; }
                sender.sendMessage(PREFIX + "§f" + args[3] + " §7영지 멤버 목록 §8(연동 예정)");
                yield true;
            }
            default -> { sender.sendMessage(PREFIX + "§7사용법: /empire estate member <add|remove|list>"); yield true; }
        };
    }

    private boolean handleEstatePermission(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(PREFIX + "§7Usage: /empire estate permission <set|reset> <owner> [role] [perm] [true|false]"); return true;
        }
        return switch (args[2].toLowerCase(Locale.ROOT)) {
            case "set" -> {
                if (args.length < 7) { sender.sendMessage(PREFIX + "§7Usage: /empire estate permission set <owner> <role> <perm> <true|false>"); yield true; }
                sender.sendMessage(PREFIX + "§f" + args[3] + " §7영지 §e" + args[4] + " §7권한 §f" + args[5] + " → §e" + args[6] + " §8(연동 예정)");
                yield true;
            }
            case "reset" -> {
                if (args.length < 4) { sender.sendMessage(PREFIX + "§7Usage: /empire estate permission reset <owner>"); yield true; }
                sender.sendMessage(PREFIX + "§f" + args[3] + " §7영지 전체 권한 기본값 초기화 §8(연동 예정)");
                yield true;
            }
            default -> { sender.sendMessage(PREFIX + "§7사용법: /empire estate permission <set|reset>"); yield true; }
        };
    }

    private boolean handleEnhanceAdminCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("empire.admin")) { sender.sendMessage(NO_ADMIN); return true; }
        if (args.length < 5 || !"set".equalsIgnoreCase(args[1])) {
            sender.sendMessage(PREFIX + "§7Usage: /empire enhance set <player> <WEAPON|HELMET|CHESTPLATE|LEGGINGS|BOOTS> <level>"); return true;
        }
        Player t = requireOnlinePlayer(sender, args[2]); if (t == null) return true;
        sender.sendMessage(PREFIX + "§f" + t.getName() + " §7슬롯 §e" + args[3] + " §7강화 §f" + args[4] + "강 §8(GrowthEngine 연동 후 적용)");
        return true;
    }

    private boolean handlePotentialCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("empire.admin")) { sender.sendMessage(NO_ADMIN); return true; }
        if (args.length < 2) {
            sender.sendMessage(PREFIX + "§7Usage: /empire potential <reroll|set> <player> <slot> [grade]"); return true;
        }
        return switch (args[1].toLowerCase(Locale.ROOT)) {
            case "reroll" -> {
                if (args.length < 4) { sender.sendMessage(PREFIX + "§7Usage: /empire potential reroll <player> <slot>"); yield true; }
                Player t = requireOnlinePlayer(sender, args[2]); if (t == null) yield true;
                sender.sendMessage(PREFIX + "§f" + t.getName() + " §7슬롯 §e" + args[3] + " §7잠재 강제 재롤 §8(연동 예정)");
                yield true;
            }
            case "set" -> {
                if (args.length < 5) { sender.sendMessage(PREFIX + "§7Usage: /empire potential set <player> <slot> <grade>"); yield true; }
                Player t = requireOnlinePlayer(sender, args[2]); if (t == null) yield true;
                sender.sendMessage(PREFIX + "§f" + t.getName() + " §7슬롯 §e" + args[3] + " §7잠재 등급 → §6" + args[4] + " §8(연동 예정)");
                yield true;
            }
            default -> { sender.sendMessage(PREFIX + "§7사용법: /empire potential <reroll|set>"); yield true; }
        };
    }

    private boolean handleSuccessionCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("empire.admin")) { sender.sendMessage(NO_ADMIN); return true; }
        if (args.length < 4 || !"give".equalsIgnoreCase(args[1])) {
            sender.sendMessage(PREFIX + "§7Usage: /empire succession give <player> <equip-trace-id>"); return true;
        }
        Player t = requireOnlinePlayer(sender, args[2]); if (t == null) return true;
        sender.sendMessage(PREFIX + "§f" + t.getName() + " §7에게 장비의 흔적 §e" + args[3] + " §7지급 §8(연동 예정)");
        return true;
    }

    private boolean handlePotionCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("empire.admin")) { sender.sendMessage(NO_ADMIN); return true; }
        if (args.length < 3 || !"reset".equalsIgnoreCase(args[1])) {
            sender.sendMessage(PREFIX + "§7Usage: /empire potion reset <player>"); return true;
        }
        Player t = requireOnlinePlayer(sender, args[2]); if (t == null) return true;
        sender.sendMessage(PREFIX + "§f" + t.getName() + " §7보스전 포션 횟수 초기화 §8(연동 예정)");
        return true;
    }

    private boolean handleBuffCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("empire.admin")) { sender.sendMessage(NO_ADMIN); return true; }
        if (args.length < 3) {
            sender.sendMessage(PREFIX + "§7Usage: /empire buff <clear|check> <player>"); return true;
        }
        Player t = requireOnlinePlayer(sender, args[2]); if (t == null) return true;
        return switch (args[1].toLowerCase(Locale.ROOT)) {
            case "clear" -> { sender.sendMessage(PREFIX + "§f" + t.getName() + " §7버프 전체 제거 §8(연동 예정)"); yield true; }
            case "check" -> { sender.sendMessage(PREFIX + "§f" + t.getName() + " §7버프 목록 조회 §8(연동 예정)"); yield true; }
            default -> { sender.sendMessage(PREFIX + "§7사용법: /empire buff <clear|check>"); yield true; }
        };
    }

    private boolean handleBossCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("empire.admin")) { sender.sendMessage(NO_ADMIN); return true; }
        if (args.length < 2) {
            sender.sendMessage(PREFIX + "§7Usage: /empire boss <spawn|kill|clearrecord|drop> ..."); return true;
        }
        return switch (args[1].toLowerCase(Locale.ROOT)) {
            case "spawn" -> {
                if (args.length < 3) { sender.sendMessage(PREFIX + "§7Usage: /empire boss spawn <boss-id> [location]"); yield true; }
                sender.sendMessage(PREFIX + "§7보스 §e" + args[2] + " §7강제 소환 §8(BossEngine 연동 후 적용)");
                yield true;
            }
            case "kill" -> {
                if (args.length < 3) { sender.sendMessage(PREFIX + "§7Usage: /empire boss kill <boss-id>"); yield true; }
                sender.sendMessage(PREFIX + "§7보스 §e" + args[2] + " §7즉시 처치 §8(연동 예정)");
                yield true;
            }
            case "clearrecord" -> {
                if (args.length < 5) { sender.sendMessage(PREFIX + "§7Usage: /empire boss clearrecord <set|reset> <player> <boss-id>"); yield true; }
                Player t = requireOnlinePlayer(sender, args[3]); if (t == null) yield true;
                sender.sendMessage(PREFIX + "§f" + t.getName() + " §7보스 §e" + args[4] + " §7클리어 기록 §f" + args[2] + " §8(연동 예정)");
                yield true;
            }
            case "drop" -> {
                if (args.length < 4 || !"simulate".equalsIgnoreCase(args[2])) {
                    sender.sendMessage(PREFIX + "§7Usage: /empire boss drop simulate <boss-id>"); yield true;
                }
                sender.sendMessage(PREFIX + "§7보스 §e" + args[3] + " §7드랍 시뮬레이션 §8(연동 예정)");
                yield true;
            }
            default -> { sender.sendMessage(PREFIX + "§7사용법: /empire boss <spawn|kill|clearrecord|drop>"); yield true; }
        };
    }

    private boolean handleCheckCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("empire.admin")) { sender.sendMessage(NO_ADMIN); return true; }
        if (args.length < 2) { sender.sendMessage(PREFIX + "§7Usage: /empire check <player> [estate|equipment]"); return true; }
        Player t = requireOnlinePlayer(sender, args[1]); if (t == null) return true;
        String mode = args.length >= 3 ? args[2].toLowerCase(Locale.ROOT) : "all";
        sender.sendMessage(PREFIX + "§f" + t.getName() + " §7상태 조회 §8[" + mode + "] §8(연동 예정)");
        return true;
    }

    private boolean handleReloadCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("empire.admin.reload")) { sender.sendMessage(NO_ADMIN); return true; }
        String target = args.length >= 2 ? args[1].toLowerCase(Locale.ROOT) : "all";
        switch (target) {
            case "drops"   -> sender.sendMessage(PREFIX + "§a드랍 테이블 리로드 §8(연동 예정)");
            case "recipes" -> sender.sendMessage(PREFIX + "§a공방 레시피 리로드 §8(연동 예정)");
            default -> {
                plugin.reloadConfig();
                CatalystConfig.reload(plugin.getConfig());
                sender.sendMessage(PREFIX + "§aconfig.yml 리로드 완료. 강화 촉진제 요구량이 갱신됐습니다.");
            }
        }
        return true;
    }

    // ─── ClickEvent 전용 내부 커맨드 ──────────────────────────────

    private boolean handleVisitCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) return true;
        if (args.length < 3) return true;
        String action = args[1].toLowerCase(Locale.ROOT);
        String requesterUuid = args[2];
        switch (action) {
            case "accept" -> player.sendMessage(PREFIX + "§a방문 수락 처리 §8(IslandVisitService 연동 예정) UUID=" + requesterUuid);
            case "deny"   -> player.sendMessage(PREFIX + "§c방문 거절 처리 §8(연동 예정) UUID=" + requesterUuid);
        }
        return true;
    }

    private boolean handleIslandCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) return true;
        // /empire island invite <accept|deny> <inviterUUID>
        if (args.length < 4 || !"invite".equalsIgnoreCase(args[1])) return true;
        String action = args[2].toLowerCase(Locale.ROOT);
        String inviterUuid = args[3];
        switch (action) {
            case "accept" -> player.sendMessage(PREFIX + "§a초대 수락 §8(연동 예정) UUID=" + inviterUuid);
            case "deny"   -> player.sendMessage(PREFIX + "§c초대 거절 §8(연동 예정) UUID=" + inviterUuid);
        }
        return true;
    }

    // ─── 기존 보조 메서드 ──────────────────────────────────────

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
            player.sendMessage(ChatColor.RED + "잘못된 무기 클래스입니다. 선택 가능: sword, axe, spear, crossbow, scythe, staff");
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

    // ─── 유틸리티 ──────────────────────────────────────────────

    private @Nullable WeaponType parseWeaponType(String input) {
        return switch (input.toLowerCase(Locale.ROOT)) {
            case "sword"    -> WeaponType.SWORD;
            case "axe"      -> WeaponType.AXE;
            case "spear"    -> WeaponType.SPEAR;
            case "crossbow" -> WeaponType.CROSSBOW;
            case "scythe"   -> WeaponType.SCYTHE;
            case "staff"    -> WeaponType.STAFF;
            default -> null;
        };
    }

    private @Nullable Player requireOnlinePlayer(CommandSender sender, String name) {
        Player target = Bukkit.getPlayerExact(name);
        if (target == null) sender.sendMessage(PREFIX + "§c" + name + "§7 플레이어를 찾을 수 없거나 오프라인입니다.");
        return target;
    }

    private @Nullable Integer parseAmount(String input) {
        try {
            int amount = Integer.parseInt(input);
            return amount < 0 ? null : amount;
        } catch (NumberFormatException ignored) { return null; }
    }

    private @Nullable Long parseLong(CommandSender sender, String input) {
        try {
            long v = Long.parseLong(input);
            if (v < 0) { sender.sendMessage(PREFIX + "§c0 이상의 숫자를 입력하세요."); return null; }
            return v;
        } catch (NumberFormatException e) {
            sender.sendMessage(PREFIX + "§c올바른 숫자를 입력하세요."); return null;
        }
    }

    private static int parseInt(String s, int fallback) {
        try { return Integer.parseInt(s); } catch (NumberFormatException e) { return fallback; }
    }

    private @Nullable ReputationTier getNextTier(ReputationTier currentTier) {
        ReputationTier[] values = ReputationTier.values();
        int idx = Arrays.asList(values).indexOf(currentTier);
        return (idx < 0 || idx + 1 >= values.length) ? null : values[idx + 1];
    }

    private List<String> filterByPrefix(List<String> values, String prefix) {
        String lp = prefix.toLowerCase(Locale.ROOT);
        return values.stream().filter(v -> v.toLowerCase(Locale.ROOT).startsWith(lp)).toList();
    }

    private List<String> onlinePlayerNames() {
        return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(ChatColor.YELLOW + "Usage: /empire <class|skill|info|hud|reputation>");
        sender.sendMessage(ChatColor.YELLOW + "Admin: /empire <gui|gold|item|estate|enhance|potential|succession|potion|buff|boss|check|reload>");
    }
}
