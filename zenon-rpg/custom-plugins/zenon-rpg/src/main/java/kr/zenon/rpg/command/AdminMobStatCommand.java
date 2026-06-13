package kr.zenon.rpg.command;

import kr.zenon.rpg.admin.config.MobStatOverride;
import kr.zenon.rpg.admin.config.MobStatOverrideService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

/**
 * 몹 스탯 런타임 오버라이드 명령 (INBOX-010 축 A MVP).
 *
 * <pre>
 *   /poro-mobstat list
 *   /poro-mobstat get  &lt;mobKey&gt;
 *   /poro-mobstat set  &lt;mobKey&gt; &lt;hp|def|atk&gt; &lt;value|none&gt;
 *   /poro-mobstat reset &lt;mobKey&gt;
 * </pre>
 *
 * 변경은 신규 스폰부터 반영(기존 개체 미소급). 값은 클램프(0~100000).
 */
public final class AdminMobStatCommand implements CommandExecutor {

    private static final double MAX_VALUE = 100_000.0;

    private final MobStatOverrideService service;

    public AdminMobStatCommand(MobStatOverrideService service) {
        this.service = service;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) { usage(sender); return true; }
        String sub = args[0].toLowerCase(Locale.ROOT);

        switch (sub) {
            case "list" -> {
                sender.sendMessage("§e[몹 스탯 오버라이드] §7(" + service.all().size() + "건)");
                for (MobStatOverride o : service.all().values()) {
                    sender.sendMessage("§7- §f" + o.mobKey() + "§7: " + describe(o));
                }
                return true;
            }
            case "get" -> {
                if (args.length < 2) { usage(sender); return true; }
                MobStatOverride o = service.get(args[1]);
                sender.sendMessage("§e[" + o.mobKey() + "] §f" + describe(o));
                return true;
            }
            case "set" -> {
                if (args.length < 4) { usage(sender); return true; }
                String mobKey = args[1];
                String field = args[2].toLowerCase(Locale.ROOT);
                if (!field.equals("hp") && !field.equals("def") && !field.equals("atk")) {
                    sender.sendMessage("§c필드는 hp|def|atk 중 하나여야 합니다.");
                    return true;
                }
                Double value;
                if (args[3].equalsIgnoreCase("none") || args[3].equalsIgnoreCase("null")) {
                    value = null; // 오버라이드 해제
                } else {
                    try { value = Double.parseDouble(args[3]); }
                    catch (NumberFormatException e) { sender.sendMessage("§c숫자 또는 none: " + args[3]); return true; }
                    if (value < 0 || value > MAX_VALUE) {
                        sender.sendMessage("§c값은 0~" + (long) MAX_VALUE + " 범위여야 합니다.");
                        return true;
                    }
                }
                service.set(mobKey, field, value, senderName(sender));
                sender.sendMessage("§a[설정] " + mobKey + " " + field + " → "
                        + (value == null ? "§7해제(YAML 원본)" : "§f" + trim(value))
                        + " §8(신규 스폰부터 반영)");
                return true;
            }
            case "reset" -> {
                if (args.length < 2) { usage(sender); return true; }
                service.reset(args[1], senderName(sender));
                sender.sendMessage("§a[리셋] " + args[1] + " §7→ DL-116 시드값 복원 §8(신규 스폰부터 반영)");
                return true;
            }
            default -> usage(sender);
        }
        return true;
    }

    private static String describe(MobStatOverride o) {
        if (o.isEmpty()) return "§8(오버라이드 없음 — YAML 원본)";
        StringBuilder sb = new StringBuilder();
        sb.append("hp=").append(o.maxHp() == null ? "§8-" : "§f" + trim(o.maxHp())).append("§7 ");
        sb.append("def=").append(o.def() == null ? "§8-" : "§f" + trim(o.def())).append("§7 ");
        sb.append("atk=").append(o.atk() == null ? "§8-" : "§f" + trim(o.atk()));
        return sb.toString();
    }

    private static String trim(double v) {
        return (v == Math.floor(v)) ? String.valueOf((long) v) : String.valueOf(v);
    }

    private static String senderName(CommandSender sender) {
        return sender.getName();
    }

    private void usage(CommandSender sender) {
        sender.sendMessage("§e[몹 스탯 오버라이드]");
        sender.sendMessage("§7/poro-mobstat list");
        sender.sendMessage("§7/poro-mobstat get <mobKey>");
        sender.sendMessage("§7/poro-mobstat set <mobKey> <hp|def|atk> <value|none>");
        sender.sendMessage("§7/poro-mobstat reset <mobKey>");
        sender.sendMessage("§8mobKey = MythicMobs mobId (예: Plains_Predator). def는 저장만(2단계 적용).");
    }
}
