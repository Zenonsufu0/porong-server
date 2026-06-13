package kr.zenon.rpg.command;

import kr.zenon.rpg.growth.engine.EnhancementService.EnhancementResult;
import kr.zenon.rpg.growth.engine.InMemoryEnhancementLogHook;
import kr.zenon.rpg.market.AuctionListing;
import kr.zenon.rpg.market.AuctionStore;
import kr.zenon.rpg.pvp.db.PvpMatchLogRepository;
import kr.zenon.rpg.pvp.db.PvpMatchLogRepository.PvpMatchLogRow;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * /rpg-log [enhance|trade|pvp] — 최근 로그 텍스트 출력 (콘솔 가능).
 * GUI는 /rpg-admin → 로그/감시. 동일 데이터를 명령어로도 노출 (C 방식 일관성).
 */
public final class AdminLogCommand implements CommandExecutor {

    private static final int TEXT_LIMIT = 10;

    private final InMemoryEnhancementLogHook enhanceLog;
    private final AuctionStore auctionStore;
    private final PvpMatchLogRepository pvpLog;

    public AdminLogCommand(InMemoryEnhancementLogHook enhanceLog,
                           AuctionStore auctionStore,
                           PvpMatchLogRepository pvpLog) {
        this.enhanceLog   = enhanceLog;
        this.auctionStore = auctionStore;
        this.pvpLog       = pvpLog;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        String tab = args.length > 0 ? args[0].toLowerCase(Locale.ROOT) : "enhance";
        switch (tab) {
            case "enhance" -> printEnhance(sender);
            case "trade"   -> printTrade(sender);
            case "pvp"     -> printPvp(sender);
            default -> {
                sender.sendMessage("§c사용법: /rpg-log [enhance|trade|pvp]");
                return true;
            }
        }
        return true;
    }

    private void printEnhance(CommandSender sender) {
        List<EnhancementResult> all = new ArrayList<>(enhanceLog.logs());
        Collections.reverse(all);
        sender.sendMessage("§e[강화 로그] §7최근 " + Math.min(TEXT_LIMIT, all.size()) + "건 §8(in-memory)");
        for (int i = 0; i < all.size() && i < TEXT_LIMIT; i++) {
            EnhancementResult r = all.get(i);
            String result = r.forcedByCeiling() ? "§6천장" : (r.success() ? "§a성공" : "§c실패");
            sender.sendMessage("§7- §f" + nameOf(r.userId()) + " §7" + r.itemId()
                    + " §8+" + r.beforeLevel() + "→+" + r.finalLevel() + " " + result
                    + " §8(" + String.format("%.1f%%", r.successRate()) + ")");
        }
    }

    private void printTrade(CommandSender sender) {
        List<AuctionListing> sold = auctionStore.recentSold(TEXT_LIMIT);
        sender.sendMessage("§e[거래 로그] §7최근 " + sold.size() + "건");
        for (AuctionListing l : sold) {
            sender.sendMessage("§7- §f" + l.sellerName() + " §7" + l.itemId() + "×" + l.quantity()
                    + " §8→ §6" + l.price() + "G");
        }
    }

    private void printPvp(CommandSender sender) {
        List<PvpMatchLogRow> rows = pvpLog.recentMatches(TEXT_LIMIT);
        sender.sendMessage("§e[PvP 로그] §7최근 " + rows.size() + "건");
        for (PvpMatchLogRow r : rows) {
            String head = r.draw() ? "§7무승부"
                    : "§a" + nameOf(r.winnerUuid()) + " §7▶ §c" + nameOf(r.loserUuid());
            sender.sendMessage("§7- §f" + r.matchType() + " " + head
                    + " §8(" + r.durationS() + "s, " + (r.reason() == null ? "-" : r.reason()) + ")");
        }
    }

    private static String nameOf(String uuidStr) {
        if (uuidStr == null || uuidStr.isBlank()) return "?";
        try {
            String name = Bukkit.getOfflinePlayer(UUID.fromString(uuidStr)).getName();
            return name != null ? name : uuidStr.substring(0, 8);
        } catch (IllegalArgumentException e) {
            return uuidStr;
        }
    }
}
