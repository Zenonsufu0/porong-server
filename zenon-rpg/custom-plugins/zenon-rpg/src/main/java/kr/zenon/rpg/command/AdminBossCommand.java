package kr.zenon.rpg.command;

import kr.zenon.rpg.boss.engine.BossRun;
import kr.zenon.rpg.boss.engine.BossRunService;
import kr.zenon.rpg.common.result.Result;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 보스 디버그 명령 (Phase 2 Step 4). 한 인스턴스가 두 명령을 라벨로 분기.
 * <ul>
 *   <li>/rpg-boss-list — 진행 중 보스 런 목록</li>
 *   <li>/rpg-boss-end &lt;runId&gt; — 강제 종료 (runId 접두어 매칭)</li>
 * </ul>
 */
public final class AdminBossCommand implements CommandExecutor {

    private final BossRunService runService;

    public AdminBossCommand(BossRunService runService) {
        this.runService = runService;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (command.getName().equalsIgnoreCase("rpg-boss-end")) {
            return handleEnd(sender, args);
        }
        return handleList(sender);
    }

    private boolean handleList(CommandSender sender) {
        var runs = runService.activeRuns();
        sender.sendMessage("§e[보스 런] §7진행 중 " + runs.size() + "건");
        for (BossRun r : runs.values()) {
            sender.sendMessage("§7- §8" + shortId(r.runId()) + " §f" + r.bossId()
                    + " §7리더 §f" + nameOf(r.leaderUserId())
                    + " §7파티 §f" + r.partySize() + "명 §7페이즈 §e" + r.currentPhase()
                    + " §7HP §c" + String.format("%.0f%%", r.bossHpPercent()));
        }
        if (!runs.isEmpty()) sender.sendMessage("§8강제 종료: /rpg-boss-end <runId 앞 8자리>");
        return true;
    }

    private boolean handleEnd(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§c사용법: /rpg-boss-end <runId 앞 8자리>");
            return true;
        }
        String prefix = args[0].toLowerCase();
        List<String> matches = new ArrayList<>();
        for (String runId : runService.activeRuns().keySet()) {
            if (runId.toLowerCase().startsWith(prefix)) matches.add(runId);
        }
        if (matches.isEmpty()) {
            sender.sendMessage("§c[보스] 일치하는 런이 없습니다: " + prefix);
            return true;
        }
        if (matches.size() > 1) {
            sender.sendMessage("§c[보스] 접두어가 모호합니다 (" + matches.size() + "건). 더 길게 입력하세요.");
            return true;
        }
        String runId = matches.get(0);
        Result<?> result = runService.endRun(runId, false, "admin_force");
        if (result.isFailure()) {
            sender.sendMessage("§c[보스] 강제 종료 실패: " + result.errorCode().name());
        } else {
            sender.sendMessage("§a[보스] 런 강제 종료 완료 — §8" + shortId(runId) + " §7(슬롯 해제됨)");
            Bukkit.getLogger().info("[zenon-rpg-boss] " + sender.getName() + " force-ended run " + runId);
        }
        return true;
    }

    private static String shortId(String runId) {
        if (runId == null) return "?";
        return runId.length() > 8 ? runId.substring(0, 8) : runId;
    }

    private static String nameOf(String uuidStr) {
        if (uuidStr == null || uuidStr.isBlank()) return "?";
        try {
            String name = Bukkit.getOfflinePlayer(UUID.fromString(uuidStr)).getName();
            return name != null ? name : shortId(uuidStr);
        } catch (IllegalArgumentException e) {
            return uuidStr;
        }
    }
}
