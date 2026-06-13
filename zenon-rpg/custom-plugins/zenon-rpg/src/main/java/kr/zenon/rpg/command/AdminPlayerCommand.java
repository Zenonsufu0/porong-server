package kr.zenon.rpg.command;

import kr.zenon.rpg.combat.weapon.WeaponType;
import kr.zenon.rpg.growth.GrowthStateStore;
import kr.zenon.rpg.growth.engine.EquipmentSlot;
import kr.zenon.rpg.growth.engine.PlayerEquipmentItem;
import kr.zenon.rpg.growth.engine.PlayerGrowthState;
import kr.zenon.rpg.growth.island.IslandRank;
import kr.zenon.rpg.growth.island.IslandStorageStore;
import kr.zenon.rpg.growth.island.IslandTerritoryState;
import kr.zenon.rpg.growth.island.IslandTerritoryStateStore;
import kr.zenon.rpg.pvp.PvpMatchService;
import kr.zenon.rpg.pvp.PvpRatingService;
import kr.zenon.rpg.storage.PlayerDataManager;
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
 *   <li>/rpg-give &lt;player&gt; &lt;itemId&gt; [qty]</li>
 *   <li>/rpg-currency &lt;player&gt; &lt;code&gt; &lt;±N&gt;</li>
 *   <li>/rpg-rank &lt;player&gt; &lt;rank&gt;</li>
 *   <li>/rpg-enhance &lt;player&gt; &lt;slot&gt; &lt;level&gt;</li>
 *   <li>/rpg-level &lt;player&gt; &lt;lv&gt;</li>
 *   <li>/rpg-pvp-score &lt;player&gt; &lt;±N&gt;</li>
 *   <li>/rpg-cleanse &lt;player&gt;</li>
 *   <li>/rpg-island-reset &lt;player&gt;</li>
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
            case "rpg-give"          -> handleGive(sender, uuid, name, args);
            case "rpg-currency"      -> handleCurrency(sender, uuid, name, args);
            case "rpg-rank"          -> handleRank(sender, uuid, name, args);
            case "rpg-enhance"       -> handleEnhance(sender, uuid, name, args);
            case "rpg-level"         -> handleLevel(sender, uuid, name, args);
            case "rpg-pvp-score"     -> handlePvpScore(sender, uuid, name, args);
            case "rpg-cleanse"       -> handleCleanse(sender, uuid, name);
            case "rpg-island-reset"  -> handleIslandReset(sender, uuid, name);
            default -> { sender.sendMessage("§c알 수 없는 명령: " + cmd); yield true; }
        };
    }

    // ─── /rpg-give <player> <itemId> [qty] ────────────────────────
    private boolean handleGive(CommandSender s, UUID uuid, String name, String[] args) {
        if (args.length < 2) { s.sendMessage("§c사용법: /rpg-give <플레이어> <itemId> [수량]"); return true; }
        String itemId = args[1];
        long qty = args.length >= 3 ? parseLong(args[2], 1) : 1;
        IslandTerritoryState t = islandStore.getOrCreate(uuid, name);
        t.addCustomItem(itemId, qty);
        s.sendMessage("§a[관리자] §f" + name + "§a에게 §e" + itemId + " ×" + qty + "§a 영지 창고 지급.");
        return true;
    }

    // ─── /rpg-currency <player> <code> <±N> ───────────────────────
    // code는 내부 코드(gold/mat_stone_enhance/mat_cube/mat_cube_fragment) 또는 한글/약어 별칭 허용.
    private boolean handleCurrency(CommandSender s, UUID uuid, String name, String[] args) {
        if (args.length < 3) {
            s.sendMessage("§c사용법: /rpg-currency <플레이어> <재화> <±N>");
            s.sendMessage("§7재화: §f골드(gold) §7| §f강화석(stone) §7| §f큐브(cube) §7| §f큐브조각(frag)");
            return true;
        }
        String code = resolveCurrencyCode(args[1]);
        long delta = parseLong(args[2], 0);
        PlayerGrowthState st = ensureGrowthState(uuid);
        if (st == null) { s.sendMessage("§c성장 데이터 없음"); return true; }
        if (delta >= 0) st.addCurrency(code, delta);
        else            st.consumeCurrency(code, -delta);
        s.sendMessage("§a[관리자] §f" + name + "§a §e" + code + " " + (delta >= 0 ? "+" : "") + delta + "§a → 잔액 §f" + st.currency(code));
        return true;
    }

    // ─── /rpg-rank <player> <rank> ────────────────────────────────
    private boolean handleRank(CommandSender s, UUID uuid, String name, String[] args) {
        if (args.length < 2) { s.sendMessage("§c사용법: /rpg-rank <플레이어> <FRONTIER|KNIGHT|BARONET|BARON|VISCOUNT|COUNT|MARQUESS|DUKE>"); return true; }
        IslandRank rank;
        try { rank = IslandRank.valueOf(args[1].toUpperCase(Locale.ROOT)); }
        catch (Exception e) { s.sendMessage("§c알 수 없는 작위: " + args[1]); return true; }
        islandStore.getOrCreate(uuid, name).setRank(rank);
        s.sendMessage("§a[관리자] §f" + name + "§a 작위 → §e" + rank.displayName);
        return true;
    }

    // ─── /rpg-enhance <player> <slot> <level> ─────────────────────
    private boolean handleEnhance(CommandSender s, UUID uuid, String name, String[] args) {
        if (args.length < 3) { s.sendMessage("§c사용법: /rpg-enhance <플레이어> <WEAPON|HELMET|CHESTPLATE|LEGGINGS|BOOTS> <강화레벨>"); return true; }
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

    // ─── /rpg-level <player> <lv> ─────────────────────────────────
    private boolean handleLevel(CommandSender s, UUID uuid, String name, String[] args) {
        if (args.length < 2) { s.sendMessage("§c사용법: /rpg-level <플레이어> <레벨>"); return true; }
        int lv = (int) parseLong(args[1], 1);
        PlayerGrowthState st = ensureGrowthState(uuid);
        if (st == null) { s.sendMessage("§c성장 데이터 없음"); return true; }
        st.setPlayerLevel(lv);
        s.sendMessage("§a[관리자] §f" + name + "§a 레벨 → §eLv " + lv);
        return true;
    }

    // ─── /rpg-pvp-score <player> <±N> ─────────────────────────────
    private boolean handlePvpScore(CommandSender s, UUID uuid, String name, String[] args) {
        if (args.length < 2) { s.sendMessage("§c사용법: /rpg-pvp-score <플레이어> <±N>"); return true; }
        int delta = (int) parseLong(args[1], 0);
        PvpRatingService.Rating updated = pvpRatingService.adminAdjustScore(uuid, name, delta);
        s.sendMessage("§a[관리자] §f" + name + "§a 점수 " + (delta >= 0 ? "+" : "") + delta + " → §e" + updated.score());
        return true;
    }

    // ─── /rpg-cleanse <player> ────────────────────────────────────
    private boolean handleCleanse(CommandSender s, UUID uuid, String name) {
        if (pvpMatchService.isInMatch(uuid)) {
            pvpMatchService.matchOf(uuid).ifPresent(m -> pvpMatchService.adminForceEnd(m.matchId(), "admin_cleanse"));
        }
        s.sendMessage("§a[관리자] §f" + name + "§a 매치/큐/대기 상태 정리 완료.");
        return true;
    }

    // ─── /rpg-island-reset <player> ───────────────────────────────
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

    /** 재화 입력(한글/약어/내부코드) → 내부 통화 코드. 알 수 없으면 원본 그대로(임의 통화 직접 지정 허용). */
    private String resolveCurrencyCode(String input) {
        return switch (input.trim().toLowerCase(Locale.ROOT)) {
            case "골드", "gold", "g"                  -> "gold";
            case "강화석", "stone", "s"                -> "mat_stone_enhance";
            case "큐브", "cube", "c"                  -> "mat_cube";
            case "큐브조각", "fragment", "frag", "f"    -> "mat_cube_fragment";
            default -> input;
        };
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
