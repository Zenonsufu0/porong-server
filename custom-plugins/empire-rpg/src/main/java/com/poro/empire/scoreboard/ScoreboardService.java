package com.poro.empire.scoreboard;

import com.poro.empire.combat.weapon.WeaponType;
import com.poro.empire.growth.GrowthStateStore;
import com.poro.empire.growth.engine.EquipmentSlot;
import com.poro.empire.growth.engine.PlayerEquipmentItem;
import com.poro.empire.growth.engine.PlayerGrowthState;
import com.poro.empire.storage.PlayerDataManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class ScoreboardService {

    private final GrowthStateStore   growthStore;
    private final PlayerDataManager  playerDataManager;

    private static final String OBJ_NAME = "poro_sidebar";
    private static final String LINE_SEP = "§7──────────";
    private static final NumberFormat NUM_FMT = NumberFormat.getInstance(Locale.ROOT);

    public ScoreboardService(GrowthStateStore growthStore,
                              PlayerDataManager playerDataManager) {
        this.growthStore       = growthStore;
        this.playerDataManager = playerDataManager;
    }

    /** 플레이어의 사이드바를 최신 데이터로 갱신한다. */
    public void refresh(Player player) {
        ScoreboardManager mgr = Bukkit.getScoreboardManager();
        if (mgr == null) return;

        Scoreboard board = mgr.getNewScoreboard();
        Objective obj = board.registerNewObjective(OBJ_NAME, "dummy", "§6포로 서버");
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        // 데이터 수집
        WeaponType wt = playerDataManager.getWeaponType(player.getUniqueId());
        Optional<PlayerGrowthState> stateOpt = growthStore.get(player.getUniqueId());

        // 행 구성 (score 높을수록 위에 표시)
        int row = 14;

        row = setRow(obj, LINE_SEP, row);

        // 직업·각인
        if (stateOpt.isPresent() && wt != WeaponType.NONE) {
            PlayerGrowthState state = stateOpt.get();
            String className = weaponClassName(wt);
            String engraving = state.classEngravingId().isBlank()
                    ? "" : "  §7" + state.classEngravingId();
            row = setRow(obj, "§e" + className + engraving, row);
        } else {
            row = setRow(obj, "§7직업 미선택", row);
        }

        row = setRow(obj, LINE_SEP + "§0.", row); // separator 중복 방지용 invisible suffix

        if (stateOpt.isPresent()) {
            PlayerGrowthState state = stateOpt.get();

            // 골드·강화석·큐브
            long gold   = state.currency("gold");
            long stone  = state.currency("mat_stone_enhance");
            long cube   = state.currency("mat_cube");
            row = setRow(obj, " §e" + fmtNum(gold) + "§7G", row);
            row = setRow(obj, " §b" + fmtNum(stone) + "§7개", row);
            row = setRow(obj, " §5" + fmtNum(cube) + "§7개", row);

            row = setRow(obj, LINE_SEP + "§0..", row);

            // 레벨·경험치·스탯 포인트
            int level   = state.playerLevel();
            int pts     = state.unspentPts();
            String xpPct = String.format("%.0f%%", (double) Math.round(player.getExp() * 100));
            String lvLine = "§7Lv.§f" + level + "  §e" + xpPct;
            if (pts > 0) lvLine += "  §a+" + pts + "§7포인트";
            row = setRow(obj, lvLine, row);

            // 평균 IL
            int il = calcAverageIl(state);
            row = setRow(obj, "§7IL §f" + il, row);
        }

        row = setRow(obj, LINE_SEP + "§0...", row);

        // 현재 위치 (월드 이름 기반 간이 표시)
        String location = resolveLocationName(player);
        row = setRow(obj, "§7" + location, row);

        player.setScoreboard(board);
    }

    // ─── helpers ──────────────────────────────────────────────────────────

    private static int setRow(Objective obj, String text, int score) {
        Score s = obj.getScore(text);
        s.setScore(score);
        return score - 1;
    }

    private static String fmtNum(long n) {
        return NUM_FMT.format(n);
    }

    private static int calcAverageIl(PlayerGrowthState state) {
        // IL = 장착 슬롯 5종 강화 합산 ÷ 5 × 5 (미장착 슬롯은 0강)
        EquipmentSlot[] slots = {
            EquipmentSlot.WEAPON,
            EquipmentSlot.HELMET,
            EquipmentSlot.CHESTPLATE,
            EquipmentSlot.LEGGINGS,
            EquipmentSlot.BOOTS
        };
        int totalEnhance = 0;
        for (EquipmentSlot slot : slots) {
            Optional<PlayerEquipmentItem> item = state.equippedItem(slot);
            totalEnhance += item.map(PlayerEquipmentItem::enhanceLevel).orElse(0);
        }
        return (totalEnhance / 5) * 5; // 평균 강화 × 5 = IL
    }

    private static String weaponClassName(WeaponType wt) {
        return switch (wt) {
            case SWORD    -> "검사";
            case AXE      -> "도끼전사";
            case STAFF    -> "마법사";
            case CROSSBOW -> "석궁사수";
            case SCYTHE   -> "사신";
            case SPEAR    -> "창기병";
            case NONE     -> "없음";
        };
    }

    private static String resolveLocationName(Player player) {
        return switch (player.getWorld().getName()) {
            case "world"      -> "수도 외곽 평원";
            case "world_boss" -> "보스 인스턴스";
            case "island"     -> "영지";
            default           -> player.getWorld().getName();
        };
    }
}
