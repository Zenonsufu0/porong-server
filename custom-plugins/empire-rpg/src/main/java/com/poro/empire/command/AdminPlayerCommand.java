package com.poro.empire.command;

import com.poro.empire.combat.weapon.WeaponType;
import com.poro.empire.growth.GrowthStateStore;
import com.poro.empire.growth.engine.EquipmentSlot;
import com.poro.empire.growth.engine.PlayerEquipmentItem;
import com.poro.empire.growth.engine.PlayerGrowthState;
import com.poro.empire.growth.island.IslandRank;
import com.poro.empire.growth.island.IslandStorageStore;
import com.poro.empire.growth.island.IslandTerritoryState;
import com.poro.empire.growth.island.IslandTerritoryStateStore;
import com.poro.empire.pvp.PvpMatchService;
import com.poro.empire.pvp.PvpRatingService;
import com.poro.empire.storage.PlayerDataManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.UUID;

/**
 * 운영자 단건 변경 명령 통합 핸들러. Phase 1 인스펙트에 대응하는 명령어 인터페이스.
 * <ul>
 *   <li>/empire-give &lt;player&gt; &lt;itemId&gt; [qty]</li>
 *   <li>/empire-currency &lt;player&gt; &lt;code&gt; &lt;±N&gt;</li>
 *   <li>/empire-rank &lt;player&gt; &lt;rank&gt;</li>
 *   <li>/empire-enhance &lt;player&gt; &lt;slot&gt; &lt;level&gt;</li>
 *   <li>/empire-level &lt;player&gt; &lt;lv&gt;</li>
 *   <li>/empire-pvp-score &lt;player&gt; &lt;±N&gt;</li>
 *   <li>/empire-cleanse &lt;player&gt;</li>
 *   <li>/empire-island-reset &lt;player&gt;</li>
 * </ul>
 */
public final class AdminPlayerCommand implements CommandExecutor {

    private final PlayerDataManager         playerDataManager;
    private final GrowthStateStore          growthStateStore;
    private final IslandTerritoryStateStore islandStore;
    private final IslandStorageStore        islandStorageStore;
    private final PvpRatingService          pvpRatingService;
    private final PvpMatchService           pvpMatchService;

    public AdminPlayerCommand(PlayerDataManager playerDataManager,
                              GrowthStateStore growthStateStore,
                              IslandTerritoryStateStore islandStore,
                              IslandStorageStore islandStorageStore,
                              PvpRatingService pvpRatingService,
                              PvpMatchService pvpMatchService) {
        this.playerDataManager  = playerDataManager;
        this.growthStateStore   = growthStateStore;
        this.islandStore        = islandStore;
        this.islandStorageStore = islandStorageStore;
        this.pvpRatingService   = pvpRatingService;
        this.pvpMatchService    = pvpMatchService;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        String cmd = command.getName().toLowerCase(Locale.ROOT);

        if (args.length < 1) {
            sender.sendMessage("§c사용법: /" + cmd + " <플레이어> ...");
            return true;
        }
        OfflinePlayer target = resolvePlayer(args[0]);
        if (target == null || target.getUniqueId() == null) {
            sender.sendMessage("§c플레이어를 찾을 수 없습니다: " + args[0]);
            return true;
        }
        UUID uuid = target.getUniqueId();
        String name = target.getName() != null ? target.getName() : args[0];

        return switch (cmd) {
            case "empire-give"          -> handleGive(sender, uuid, name, args);
            case "empire-currency"      -> handleCurrency(sender, uuid, name, args);
            case "empire-rank"          -> handleRank(sender, uuid, name, args);
            case "empire-enhance"       -> handleEnhance(sender, uuid, name, args);
            case "empire-level"         -> handleLevel(sender, uuid, name, args);
            case "empire-pvp-score"     -> handlePvpScore(sender, uuid, name, args);
            case "empire-cleanse"       -> handleCleanse(sender, uuid, name);
            case "empire-island-reset"  -> handleIslandReset(sender, uuid, name);
            default -> { sender.sendMessage("§c알 수 없는 명령: " + cmd); yield true; }
        };
    }

    // ─── /empire-give <player> <itemId> [qty] ────────────────────────
    private boolean handleGive(CommandSender s, UUID uuid, String name, String[] args) {
        if (args.length < 2) { s.sendMessage("§c사용법: /empire-give <플레이어> <itemId> [수량]"); return true; }
        String itemId = args[1];
        long qty = args.length >= 3 ? parseLong(args[2], 1) : 1;
        IslandTerritoryState t = islandStore.getOrCreate(uuid, name);
        t.addCustomItem(itemId, qty);
        s.sendMessage("§a[관리자] §f" + name + "§a에게 §e" + itemId + " ×" + qty + "§a 영지 창고 지급.");
        return true;
    }

    // ─── /empire-currency <player> <code> <±N> ───────────────────────
    private boolean handleCurrency(CommandSender s, UUID uuid, String name, String[] args) {
        if (args.length < 3) { s.sendMessage("§c사용법: /empire-currency <플레이어> <code> <±N>"); return true; }
        String code = args[1];
        long delta = parseLong(args[2], 0);
        PlayerGrowthState st = ensureGrowthState(uuid);
        if (st == null) { s.sendMessage("§c성장 데이터 없음"); return true; }
        if (delta >= 0) st.addCurrency(code, delta);
        else            st.consumeCurrency(code, -delta);
        s.sendMessage("§a[관리자] §f" + name + "§a §e" + code + " " + (delta >= 0 ? "+" : "") + delta + "§a → 잔액 §f" + st.currency(code));
        return true;
    }

    // ─── /empire-rank <player> <rank> ────────────────────────────────
    private boolean handleRank(CommandSender s, UUID uuid, String name, String[] args) {
        if (args.length < 2) { s.sendMessage("§c사용법: /empire-rank <플레이어> <FRONTIER|KNIGHT|BARONET|BARON|VISCOUNT|COUNT|MARQUESS|DUKE>"); return true; }
        IslandRank rank;
        try { rank = IslandRank.valueOf(args[1].toUpperCase(Locale.ROOT)); }
        catch (Exception e) { s.sendMessage("§c알 수 없는 작위: " + args[1]); return true; }
        islandStore.getOrCreate(uuid, name).setRank(rank);
        s.sendMessage("§a[관리자] §f" + name + "§a 작위 → §e" + rank.displayName);
        return true;
    }

    // ─── /empire-enhance <player> <slot> <level> ─────────────────────
    private boolean handleEnhance(CommandSender s, UUID uuid, String name, String[] args) {
        if (args.length < 3) { s.sendMessage("§c사용법: /empire-enhance <플레이어> <WEAPON|HELMET|CHESTPLATE|LEGGINGS|BOOTS> <강화레벨>"); return true; }
        EquipmentSlot slot;
        try { slot = EquipmentSlot.valueOf(args[1].toUpperCase(Locale.ROOT)); }
        catch (Exception e) { s.sendMessage("§c알 수 없는 슬롯: " + args[1]); return true; }
        int level = (int) parseLong(args[2], 0);
        PlayerGrowthState st = ensureGrowthState(uuid);
        if (st == null) { s.sendMessage("§c성장 데이터 없음"); return true; }
        PlayerEquipmentItem item = st.equippedItem(slot).orElse(null);
        if (item == null) { s.sendMessage("§c슬롯이 비어있음: " + slot); return true; }
        item.setEnhanceLevel(level);
        s.sendMessage("§a[관리자] §f" + name + "§a §e" + slot + " → +" + level + "강");
        return true;
    }

    // ─── /empire-level <player> <lv> ─────────────────────────────────
    private boolean handleLevel(CommandSender s, UUID uuid, String name, String[] args) {
        if (args.length < 2) { s.sendMessage("§c사용법: /empire-level <플레이어> <레벨>"); return true; }
        int lv = (int) parseLong(args[1], 1);
        PlayerGrowthState st = ensureGrowthState(uuid);
        if (st == null) { s.sendMessage("§c성장 데이터 없음"); return true; }
        st.setPlayerLevel(lv);
        s.sendMessage("§a[관리자] §f" + name + "§a 레벨 → §eLv " + lv);
        return true;
    }

    // ─── /empire-pvp-score <player> <±N> ─────────────────────────────
    private boolean handlePvpScore(CommandSender s, UUID uuid, String name, String[] args) {
        if (args.length < 2) { s.sendMessage("§c사용법: /empire-pvp-score <플레이어> <±N>"); return true; }
        int delta = (int) parseLong(args[1], 0);
        PvpRatingService.Rating updated = pvpRatingService.adminAdjustScore(uuid, name, delta);
        s.sendMessage("§a[관리자] §f" + name + "§a 점수 " + (delta >= 0 ? "+" : "") + delta + " → §e" + updated.score());
        return true;
    }

    // ─── /empire-cleanse <player> ────────────────────────────────────
    private boolean handleCleanse(CommandSender s, UUID uuid, String name) {
        if (pvpMatchService.isInMatch(uuid)) {
            pvpMatchService.matchOf(uuid).ifPresent(m -> pvpMatchService.adminForceEnd(m.matchId(), "admin_cleanse"));
        }
        s.sendMessage("§a[관리자] §f" + name + "§a 매치/큐/대기 상태 정리 완료.");
        return true;
    }

    // ─── /empire-island-reset <player> ───────────────────────────────
    private boolean handleIslandReset(CommandSender s, UUID uuid, String name) {
        // 영지 상태 보장 후 공통 초기화 헬퍼 호출 (GUI와 동일 로직)
        islandStore.getOrCreate(uuid, name);
        islandStore.resetSocialSettings(uuid);
        s.sendMessage("§a[관리자] §f" + name + "§a 영지 멤버/권한/방문설정 초기화 완료.");
        return true;
    }

    // ─── 헬퍼 ────────────────────────────────────────────────────────

    private OfflinePlayer resolvePlayer(String arg) {
        OfflinePlayer cached = Bukkit.getOfflinePlayerIfCached(arg);
        if (cached != null) return cached;
        return Bukkit.getOfflinePlayer(arg);
    }

    private long parseLong(String s, long def) {
        try { return Long.parseLong(s); } catch (Exception e) { return def; }
    }

    private PlayerGrowthState ensureGrowthState(UUID uuid) {
        PlayerGrowthState st = growthStateStore.get(uuid).orElse(null);
        if (st != null) return st;
        // 미접속 플레이어 — 직업 미선택이면 null 반환
        WeaponType wt = playerDataManager.getWeaponType(uuid);
        if (wt == WeaponType.NONE) return null;
        return growthStateStore.getOrCreate(uuid, wt.name().toLowerCase(Locale.ROOT));
    }
}
