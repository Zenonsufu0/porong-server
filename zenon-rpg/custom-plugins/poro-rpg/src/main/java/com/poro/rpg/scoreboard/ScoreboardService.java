package com.poro.rpg.scoreboard;

import com.poro.rpg.combat.weapon.WeaponType;
import com.poro.rpg.growth.GrowthStateStore;
import com.poro.rpg.growth.engine.EquipmentSlot;
import com.poro.rpg.growth.engine.PlayerEquipmentItem;
import com.poro.rpg.growth.engine.PlayerGrowthState;
import com.poro.rpg.growth.island.IslandTerritoryStateStore;
import com.poro.rpg.storage.PlayerDataManager;
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
    // 보스룸 파티 패널용 — setter 주입 (PartyManager·BossRoomManager는 ScoreboardService 이후 생성).
    private com.poro.rpg.boss.room.BossRoomManager   bossRoomManager;
    private com.poro.rpg.boss.party.PartyManager     partyManager;

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

    /** runId → 남은 전투 시간(초). 보스 타이머 표시용 (BossRunService.remainingSeconds 위임). */
    private java.util.function.Function<String, Long> bossTimeRemaining;

    /** 보스룸 파티 패널(데스카운트·파티원 HP·남은시간) 표시용 컨텍스트 주입. */
    public void attachBossContext(com.poro.rpg.boss.room.BossRoomManager bossRoomManager,
                                  com.poro.rpg.boss.party.PartyManager partyManager,
                                  java.util.function.Function<String, Long> bossTimeRemaining) {
        this.bossRoomManager   = bossRoomManager;
        this.partyManager      = partyManager;
        this.bossTimeRemaining = bossTimeRemaining;
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
                // 보스룸 안에서는 파티원 HP·데스카운트가 실시간 변하므로 매 초 강제 갱신.
                boolean inBossRoom = bossRoomManager != null
                        && bossRoomManager.isInBossRoom(player.getUniqueId());
                if (!now.equals(prev) || inBossRoom) {
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

        // 보스룸 입장 중이면 전투 중심 압축 레이아웃 — 사이드바 15줄 제한 안에서 파티 패널을 확보하기 위해
        // 재화/레벨/IL 블록을 생략한다(전투 중 불필요). 그 외에는 기존 전체 레이아웃.
        boolean inBossRoom = bossRoomManager != null
                && bossRoomManager.isInBossRoom(player.getUniqueId());

        // 행 구성 (score 높을수록 위에 표시)
        int row = 14;

        row = setRow(obj, LINE_SEP, row);

        // 직업·각인
        if (stateOpt.isPresent() && wt != WeaponType.NONE) {
            PlayerGrowthState state = stateOpt.get();
            String className = weaponClassName(wt);
            // 무기별 각인(DL-110)의 한글명 표시 — 정본 렌더러 위임(영어 ID 노출 해소).
            // 직업명(§e)과 시각 구분 위해 각인은 청록(§b)으로 강조.
            String engravingName = com.poro.rpg.gui.EquipmentLoreRenderer.engravingNameOrEmpty(state.classEngravingId());
            String engraving = engravingName.isBlank() ? "" : "  §b" + engravingName;
            row = setRow(obj, "§e" + className + engraving, row);
        } else {
            row = setRow(obj, "§7직업 미선택", row);
        }

        row = setRow(obj, LINE_SEP + "§0.", row); // separator 중복 방지용 invisible suffix

        if (stateOpt.isPresent() && !inBossRoom) {
            PlayerGrowthState state = stateOpt.get();

            // 골드·강화석·큐브 (poro:hud PNG 아이콘 + 수치)
            long gold   = state.currency("gold");
            long stone  = state.currency("mat_stone_enhance");
            long cube   = state.currency("mat_cube");
            row = setIconRow(board, obj, GOLD_ICON,    "골드: "   + fmtNum(gold),  NamedTextColor.YELLOW,      row);
            row = setIconRow(board, obj, ENHANCE_ICON, "강화석: " + fmtNum(stone), NamedTextColor.AQUA,         row);
            row = setIconRow(board, obj, CUBE_ICON,    "큐브: "   + fmtNum(cube),  NamedTextColor.DARK_PURPLE,  row);

            row = setRow(obj, LINE_SEP + "§0..", row);

            // 레벨·경험치·스탯 포인트
            int level   = state.playerLevel();
            int pts     = state.unspentPts();
            // 진행도 = 커스텀 레벨링(currentExp/다음레벨). 바닐라 XP는 억제되어 0이라 못 씀.
            long xpNeed = com.poro.rpg.leveling.PlayerLevelingService.expToNextLevel(level);
            int xpPctVal = xpNeed > 0 ? (int) Math.min(100, state.currentExp() * 100 / xpNeed) : 0;
            String lvLine = "§7Lv.§f" + level + "  §e" + xpPctVal + "%";
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

        // 보스룸 파티 패널 (데스카운트·보스·파티원 HP) — 보스룸 입장 중에만
        row = appendBossPanel(obj, player, row);

        player.setScoreboard(board);
    }

    /**
     * 보스룸 입장 중인 플레이어에게 파티 패널을 추가한다.
     * 남은 부활(데스카운트) · 도전 보스명 · 파티원 HP(현재/최대)를 표시한다.
     * 보스룸이 아니거나 컨텍스트 미주입이면 아무것도 추가하지 않는다.
     */
    private int appendBossPanel(Objective obj, Player player, int row) {
        if (bossRoomManager == null) return row;
        UUID uuid = player.getUniqueId();
        if (!bossRoomManager.isInBossRoom(uuid)) return row;
        int slotId = bossRoomManager.slotOf(uuid).orElse(-1);
        if (slotId < 0) return row;

        row = setRow(obj, LINE_SEP + "§0,", row);

        // 도전 보스
        String bossId = bossRoomManager.bossInSlot(slotId).orElse(null);
        if (bossId != null) {
            row = setRow(obj, "§6보스 §f" + com.poro.rpg.gui.BossHubGui.bossNameById(bossId), row);
        }

        // 남은 시간 (BossRun 타임아웃 기준)
        if (bossTimeRemaining != null) {
            long sec = bossRoomManager.runIdOf(slotId).map(bossTimeRemaining).orElse(-1L);
            if (sec >= 0) {
                String color = sec <= 60 ? "§c" : "§e";
                row = setRow(obj, "§7남은시간 " + color
                        + String.format("%d:%02d", sec / 60, sec % 60), row);
            }
        }

        // 남은 부활(공유 데스카운트)
        int rem = bossRoomManager.deathsRemaining(slotId);
        int max = bossRoomManager.deathsMax(slotId);
        if (max > 0) {
            row = setRow(obj, "§c남은 부활 §f" + rem + "§7/§f" + max, row);
        }

        // 파티원 HP (현재/최대). 본인을 항상 맨 위에 포함하고, 파티원을 뒤이어 표시(중복 제거).
        row = setRow(obj, "§7파티원", row);
        java.util.LinkedHashSet<UUID> members = new java.util.LinkedHashSet<>();
        members.add(uuid); // 본인 우선
        if (partyManager != null) {
            partyManager.findParty(uuid).ifPresent(p -> members.addAll(p.members()));
        }
        for (UUID memberId : members) {
            Player member = Bukkit.getPlayer(memberId);
            if (member == null) continue; // 오프라인 멤버는 표시 생략
            boolean self = memberId.equals(uuid);
            row = setRow(obj, hpLine(member, self), row);
        }
        return row;
    }

    /** "{▶}§f{이름} §a{현재}§7/{최대}" 형식 HP 라인. 본인은 ▶ 표시, HP 비율에 따라 색상 변화. */
    private static String hpLine(Player p, boolean self) {
        int cur = (int) Math.ceil(Math.max(0, p.getHealth()));
        var attr = p.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
        int max = attr != null ? (int) Math.round(attr.getValue()) : cur;
        double ratio = max > 0 ? (double) cur / max : 0;
        String hpColor = ratio > 0.5 ? "§a" : ratio > 0.25 ? "§e" : "§c";
        String name = p.getName();
        if (name.length() > 10) name = name.substring(0, 10); // 사이드바 폭 보호
        String prefix = self ? "§b▶ " : "§f";
        return prefix + name + " " + hpColor + cur + "§7/" + max;
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
        // 사이드바 라인 = prefix + entry + suffix 가 그대로 출력된다.
        // entry를 평문("poro_eN")으로 두면 그 글자가 라인에 노출돼 깨져 보이므로,
        // 색코드만으로 된 보이지 않는 고유 entry를 쓴다(아이콘·수치는 전부 prefix).
        String entryKey = "§" + Integer.toHexString((score >> 4) & 0xF)
                        + "§" + Integer.toHexString(score & 0xF);
        Team team = board.getTeam("poro_t" + score);
        if (team == null) team = board.registerNewTeam("poro_t" + score);
        if (!team.hasEntry(entryKey)) team.addEntry(entryKey);
        // 아이콘과 텍스트를 empty 아래 형제로 — 텍스트가 HUD_FONT를 상속해 깨지는 것 방지(기본 폰트 렌더).
        team.prefix(Component.empty()
                .append(Component.text(String.valueOf(icon)).font(HUD_FONT))
                .append(Component.text(" " + text).color(color)));
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
