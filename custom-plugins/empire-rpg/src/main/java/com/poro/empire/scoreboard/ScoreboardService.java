package com.poro.empire.scoreboard;

import com.poro.empire.combat.weapon.WeaponType;
import com.poro.empire.growth.GrowthStateStore;
import com.poro.empire.growth.engine.EquipmentSlot;
import com.poro.empire.growth.engine.PlayerEquipmentItem;
import com.poro.empire.growth.engine.PlayerGrowthState;
import com.poro.empire.growth.island.IslandTerritoryStateStore;
import com.poro.empire.storage.PlayerDataManager;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ScoreboardService {

    private final GrowthStateStore          growthStore;
    private final PlayerDataManager         playerDataManager;
    private final IslandTerritoryStateStore territoryStore;

    private static final String OBJ_NAME = "poro_sidebar";
    private static final String LINE_SEP = "§7──────────";
    private static final NumberFormat NUM_FMT = NumberFormat.getInstance(Locale.ROOT);

    private static final Key HUD_FONT       = Key.key("poro", "hud");
    private static final char GOLD_ICON     = ''; // poro:hud gold.png
    private static final char ENHANCE_ICON  = ''; // poro:hud enhance.png
    private static final char CUBE_ICON     = ''; // poro:hud cube.png

    /** 위치명 변경 감지용 — UUID당 마지막으로 표시한 위치명. 변경 시에만 refresh해 깜빡임 방지. */
    private final Map<UUID, String> lastLocation = new ConcurrentHashMap<>();

    public ScoreboardService(GrowthStateStore growthStore,
                              PlayerDataManager playerDataManager,
                              IslandTerritoryStateStore territoryStore) {
        this.growthStore       = growthStore;
        this.playerDataManager = playerDataManager;
        this.territoryStore    = territoryStore;
    }

    /**
     * 위치 감시 태스크 시작 — 1초마다 각 플레이어의 현재 위치명을 계산해
     * 직전과 달라진 경우에만 스코어보드를 갱신한다. (필드↔보스룸↔영지↔수도 이동 반영)
     * refresh()가 새 스코어보드를 통째로 재생성하므로, 변경 시에만 호출해 깜빡임을 막는다.
     */
    public void startLocationWatcher(Plugin plugin) {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                String now  = resolveLocationName(player);
                String prev = lastLocation.put(player.getUniqueId(), now);
                if (!now.equals(prev)) {
                    refresh(player);
                }
            }
            // 오프라인 플레이어 캐시 정리 (누수 방지)
            lastLocation.keySet().removeIf(id -> Bukkit.getPlayer(id) == null);
        }, 40L, 20L);
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
            // 무기별 각인(DL-110)의 한글명 표시 — 정본 렌더러 위임(영어 ID 노출 해소).
            // 직업명(§e)과 시각 구분 위해 각인은 청록(§b)으로 강조.
            String engravingName = com.poro.empire.gui.EquipmentLoreRenderer.engravingNameOrEmpty(state.classEngravingId());
            String engraving = engravingName.isBlank() ? "" : "  §b" + engravingName;
            row = setRow(obj, "§e" + className + engraving, row);
        } else {
            row = setRow(obj, "§7직업 미선택", row);
        }

        row = setRow(obj, LINE_SEP + "§0.", row); // separator 중복 방지용 invisible suffix

        if (stateOpt.isPresent()) {
            PlayerGrowthState state = stateOpt.get();

            // 골드·강화석·큐브 (poro:hud PNG 아이콘 + 수치)
            long gold   = state.currency("gold");
            long stone  = state.currency("mat_stone_enhance");
            long cube   = state.currency("mat_cube");
            row = setIconRow(board, obj, GOLD_ICON,    fmtNum(gold)  + "G",  NamedTextColor.YELLOW,      row);
            row = setIconRow(board, obj, ENHANCE_ICON, fmtNum(stone) + "개", NamedTextColor.AQUA,         row);
            row = setIconRow(board, obj, CUBE_ICON,    fmtNum(cube)  + "개", NamedTextColor.DARK_PURPLE,  row);

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

    /**
     * poro:hud 폰트 아이콘 + 컬러 텍스트를 Team prefix로 사이드바에 표시한다.
     * Team entry = "poro_entry_N" 형식으로 score 값마다 고유.
     */
    private static int setIconRow(Scoreboard board, Objective obj,
                                   char icon, String text,
                                   net.kyori.adventure.text.format.TextColor color, int score) {
        String entryKey = "poro_e" + score;
        Team team = board.getTeam("poro_t" + score);
        if (team == null) team = board.registerNewTeam("poro_t" + score);
        if (!team.hasEntry(entryKey)) team.addEntry(entryKey);
        team.prefix(
                Component.text(String.valueOf(icon)).font(HUD_FONT)
                        .append(Component.text(" " + text).color(color))
        );
        team.suffix(Component.empty());
        obj.getScore(entryKey).setScore(score);
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

    private String resolveLocationName(Player player) {
        return switch (player.getWorld().getName()) {
            case "world"           -> resolveWorldArea(player.getLocation());
            case "world_hub"       -> "수도";
            case "world_boss"      -> "보스 인스턴스";
            // IridiumSkyblock = 개인 섬(영지). territory store의 영지명(rename 반영)을 표시.
            // IS API JAR 미포함이라 "현재 발 딛은 섬의 소유자" 역조회는 불가 — 본인 영지명을 표시한다
            // (기존 player.getName() 가정과 동일 범위, rename만 추가 반영). 미생성 시 기본 표기로 폴백.
            case "IridiumSkyblock" -> territoryName(player);
            case "island"          -> "영지";
            default                -> player.getWorld().getName();
        };
    }

    /** territory store의 영지명을 조회한다. 미생성·공백이면 "{이름}의 영지" 기본 표기로 폴백. */
    private String territoryName(Player player) {
        String fallback = player.getName() + "의 영지";
        return territoryStore.get(player.getUniqueId())
                .map(state -> {
                    String name = state.islandName();
                    return (name == null || name.isBlank()) ? fallback : name;
                })
                .orElse(fallback);
    }

    /**
     * 단일 평지 월드 "world" 안에서 좌표로 구역명을 구분한다.
     * 필드 5종(X 0/1000/2000/3000/4000, 각 ±150) / 보스룸(X 10000~10400, Z 10000~10300) / PvP(X≥20000).
     * config 좌표 변경 시 이 상수도 함께 조정해야 함.
     */
    private static String resolveWorldArea(Location loc) {
        double x = loc.getX();
        double z = loc.getZ();
        if (x >= 20000) return "PvP 아레나";
        if (x >= 10000 && x <= 10400 && z >= 9950 && z <= 10350) return "보스 인스턴스";
        // 필드 5종 — 중심 ±175(경계 여유)
        if (Math.abs(x - 0)    <= 175) return "평원 필드";
        if (Math.abs(x - 1000) <= 175) return "광산 필드";
        if (Math.abs(x - 2000) <= 175) return "하수도 필드";
        if (Math.abs(x - 3000) <= 175) return "전초기지 필드";
        if (Math.abs(x - 4000) <= 175) return "폐허 필드";
        return "수도 외곽 평원";
    }
}
