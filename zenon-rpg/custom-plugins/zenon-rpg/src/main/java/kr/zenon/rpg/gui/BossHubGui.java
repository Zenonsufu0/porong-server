package kr.zenon.rpg.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class BossHubGui {
    private BossHubGui() {}

    public record BossDef(String id, String name, String tier, Material icon,
                          String recLevel, boolean needsUnlock, long hp, int atk) {}

    private static final Map<Integer, BossDef> SLOT_MAP = new HashMap<>();

    static {
        // 시즌보스 6종 — row1·2, 컬럼 2·4·6 (여백 2·1·1·2 가운데 정렬)
        SLOT_MAP.put(11, new BossDef("fallen_knight",  "타락 기사장",   "시즌1", Material.NETHERITE_SWORD,   "6~10강",  false, 150000,  85));
        SLOT_MAP.put(13, new BossDef("corrupted_lord", "오염된 군주",   "시즌2", Material.POISONOUS_POTATO,  "10~13강", false, 155000, 105));
        SLOT_MAP.put(15, new BossDef("stone_colossus", "석조 거상",     "시즌3", Material.STONE,              "13~16강", false, 158000, 120));
        SLOT_MAP.put(20, new BossDef("storm_sorcerer", "폭풍 술사",     "시즌4", Material.LIGHTNING_ROD,      "16~18강", false, 160000, 130));
        SLOT_MAP.put(22, new BossDef("abyss_guardian", "심연 수호자",   "시즌5", Material.CRYING_OBSIDIAN,    "18~20강", false, 165000, 155));
        SLOT_MAP.put(24, new BossDef("void_herald",    "공허 사자",     "시즌6", Material.CHORUS_FRUIT,       "20~22강", false, 170000, 185));
        // 최종보스 3종 — row3, 컬럼 2·4·6 (시즌과 세로 정렬)
        SLOT_MAP.put(29, new BossDef("rift_king",      "균열왕",        "최종",  Material.NETHER_STAR,        "22강+",   true,  890000, 220));
        SLOT_MAP.put(31, new BossDef("corrupted_dyad", "타락한 이중체", "최종",  Material.WITHER_ROSE,        "22강+",   true,  445000, 175));
        SLOT_MAP.put(33, new BossDef("spirit_watcher", "진혼의 주시자", "최종",  Material.SOUL_LANTERN,       "22강+",   true,  890000, 200));
    }

    // 27슬롯 중간 허브 — 파티생성/관리·목록·보스정보·클리어기록
    public static void open(Player player, boolean inParty) {
        Inventory inv = Bukkit.createInventory(null, 27, GuiTitles.BOSS_HUB);
        ItemStack blk = MainHubGui.icon(Material.BLACK_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < 27; i++) inv.setItem(i, blk);

        // slot10: 파티 유무로 생성/관리 분기
        if (inParty) {
            inv.setItem(10, MainHubGui.icon(Material.CHEST, "§a파티 관리",
                    List.of("§7내 파티 보기·준비·입장", "§8▶ 클릭하여 열기")));
        } else {
            inv.setItem(10, MainHubGui.icon(Material.GREEN_WOOL, "§a파티 생성",
                    List.of("§7보스·인원·제목을 정해 생성", "§8▶ 클릭하여 열기")));
        }
        inv.setItem(12, MainHubGui.icon(Material.BOOK,                  "§f파티 목록",   List.of("§7열린 파티에 참가",     "§8▶ 클릭하여 열기")));
        inv.setItem(14, MainHubGui.icon(Material.WITHER_SKELETON_SKULL, "§c보스 정보",   List.of("§7보스 정보·권장 스펙 확인", "§8▶ 클릭하여 열기")));
        inv.setItem(16, MainHubGui.icon(Material.NETHER_STAR,           "§e클리어 기록", List.of("§7보스별 개인·서버 기록", "§8▶ 클릭하여 열기")));

        inv.setItem(18, MainHubGui.icon(Material.ARROW, "§7뒤로", List.of("§7메인 메뉴")));
        player.openInventory(inv);
    }

    /** 파티 생성 — 보스 선택. */
    public static void openPartyCreateBoss(Player player) {
        renderBossGrid(player, GuiTitles.PARTY_CREATE_BOSS, "§a클릭하여 이 보스로 파티 생성");
    }

    /** 보스 정보/선택 — 시즌 6(좌측 세로 컬럼) + 최종 3(우측 세로 컬럼). */
    public static void openBossInfo(Player player) {
        renderBossGrid(player, GuiTitles.BOSS_INFO, "§e▶ 클릭 — 패턴·페이즈·데미지 상세");
    }

    /** 보스 상세 정보 — 스탯 + 페이즈별 패턴·데미지 (DL-129 추가#18). */
    public static void openBossDetail(Player player, String bossId) {
        BossDef b = null;
        for (BossDef d : SLOT_MAP.values()) if (d.id().equals(bossId)) { b = d; break; }
        if (b == null) { openBossInfo(player); return; }
        Inventory inv = Bukkit.createInventory(null, 27, GuiTitles.BOSS_DETAIL);
        ItemStack pane = MainHubGui.icon(Material.BLACK_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < 27; i++) inv.setItem(i, pane);

        Double sHp  = kr.zenon.rpg.admin.config.MobStatOverrideService.seededHp(bossId);
        Double sAtk = kr.zenon.rpg.admin.config.MobStatOverrideService.seededAtk(bossId);
        Double sDef = kr.zenon.rpg.admin.config.MobStatOverrideService.seededDef(bossId);
        long hp = sHp != null ? sHp.longValue() : b.hp();
        int  atk = sAtk != null ? sAtk.intValue() : b.atk();

        // 스탯 (slot 11)
        inv.setItem(11, MainHubGui.icon(b.icon(), "§f[" + b.tier() + "] " + b.name(),
                List.of("§7──────────────",
                        "§7체력: §c" + String.format("%,d", hp),
                        "§8  1인 §f×1.0 §8/ 2인 §f×1.8 §8/ 3인 §f×2.5 §8(실입장 인원)",
                        "§7공격력: §c" + atk,
                        "§7방어: §f" + (sDef != null ? String.valueOf(sDef.intValue()) : "—")
                            + " §8(피해 ×200/(200+유효DEF) 경감)",
                        "§7권장: §e" + b.recLevel() + " §7· 파티 §f1~3인")));

        // 패턴/페이즈 (slot 15)
        inv.setItem(15, MainHubGui.icon(Material.WRITTEN_BOOK, "§c페이즈·패턴 상세",
                bossDetailLore(bossId)));

        inv.setItem(22, MainHubGui.icon(Material.ARROW, "§7뒤로", List.of("§7보스 정보")));
        player.openInventory(inv);
    }

    private static void renderBossGrid(Player player, net.kyori.adventure.text.Component title, String action) {
        Inventory inv = Bukkit.createInventory(null, 54, title);
        ItemStack bluPane = MainHubGui.icon(Material.BLUE_STAINED_GLASS_PANE,   " ", List.of());
        for (int i = 0; i <= 26; i++) inv.setItem(i, bluPane);
        ItemStack purPane = MainHubGui.icon(Material.PURPLE_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 27; i <= 44; i++) inv.setItem(i, purPane);
        ItemStack blkPane = MainHubGui.icon(Material.BLACK_STAINED_GLASS_PANE,  " ", List.of());
        for (int i = 45; i <= 53; i++) inv.setItem(i, blkPane);
        inv.setItem(4,  MainHubGui.icon(Material.BLUE_STAINED_GLASS_PANE,   "§b시즌 보스 §8(6 ~ 22강)", List.of()));
        inv.setItem(27, MainHubGui.icon(Material.PURPLE_STAINED_GLASS_PANE, "§5최종 보스", List.of("§8공허 사자(시즌6) 클리어 필요")));

        for (Map.Entry<Integer, BossDef> e : SLOT_MAP.entrySet()) {
            BossDef b = e.getValue();
            // 요약 아이콘 — 핵심만. 상세(패턴·페이즈·데미지)는 클릭 시 상세 GUI (DL-129 추가#18).
            Double sHp  = kr.zenon.rpg.admin.config.MobStatOverrideService.seededHp(b.id());
            long hp = sHp != null ? sHp.longValue() : b.hp();
            java.util.List<String> lore = new java.util.ArrayList<>();
            lore.add("§7──────────────");
            lore.add("§7체력: §c" + String.format("%,d", hp) + " §8(1인 기준)");
            lore.add("§7권장: §e" + b.recLevel() + " §7· 파티 §f1~3인");
            if (b.needsUnlock()) lore.add("§c공허 사자 클리어 필요");
            lore.add("");
            lore.add(action);
            inv.setItem(e.getKey(), MainHubGui.icon(b.icon(), "§f[" + b.tier() + "] " + b.name(), lore));
        }
        inv.setItem(45, MainHubGui.icon(Material.ARROW, "§7뒤로", List.of("§7보스 허브")));
        player.openInventory(inv);
    }

    public static String  bossIdAt(int slot)          { BossDef d = SLOT_MAP.get(slot); return d == null ? null : d.id(); }
    public static String  bossNameAt(int slot)         { BossDef d = SLOT_MAP.get(slot); return d == null ? null : d.name(); }

    /** 보스 id → 표시명. 미존재 시 id 그대로. */
    public static String  bossNameById(String id) {
        for (BossDef d : SLOT_MAP.values()) if (d.id().equals(id)) return d.name();
        return id;
    }
    public static boolean bossNeedsUnlockAt(int slot)  { BossDef d = SLOT_MAP.get(slot); return d != null && d.needsUnlock(); }

    /**
     * 보스 상세 — 실제 인게임 패턴·데미지 (정본 = MythicMobs season_bosses.yml / boss_patterns.yml).
     * 데미지 ×N = 보스 공격력 배율(mdamage). 페이즈 전환 시 무적 진입(현재 SP 해제 메카닉 미구현 → 자동 해제) (DL-129 추가#18).
     */
    private static List<String> bossDetailLore(String id) {
        return switch (id) {
            case "fallen_knight" -> List.of(
                    "§7── 3페이즈 (전환 60%·30%) ──",
                    "§8공통: 기본 공격 §c×0.6",
                    "§e[P1 100~60%] 전방 강타 §c×1.2§e · 직선 돌진 §c×1.5",
                    "§e[P2 60~30%] §7+ 부채꼴 휩쓸기 §c×1.1",
                    "§e[P3 30~0%] §7+ 원형 폭발 §c×1.3 §8(쿨 단축)",
                    "§b전환 60%·30% → 무적 §8(자동 해제 ~120s)");
            case "corrupted_lord" -> List.of(
                    "§7── 3페이즈 (전환 55%·20%) ──",
                    "§8기본 §c×0.6§8 · 직선 돌진 §c×1.5",
                    "§e[<55%] 원형 폭발 §c×1.3",
                    "§b전환 55%·20% → 무적");
            case "stone_colossus" -> List.of(
                    "§7── 3페이즈 (전환 60%·25%) ──",
                    "§8기본 §c×0.6§8 · 전방 강타 §c×1.2§8 · 부채꼴 §c×1.1",
                    "§e[P1] 직선 돌진 §c×1.5 §8(느린 쿨)",
                    "§e[<60%] 직선 돌진 빨라짐 · 원형 폭발 §c×1.3",
                    "§b전환 60%·25% → 무적");
            case "storm_sorcerer" -> List.of(
                    "§7── 3페이즈 (전환 60%·25%) ──",
                    "§8기본 §c×0.6§8 · 투사체 §c×1.0",
                    "§e[>25%] 소환 — Vex 2기 호출(압박)",
                    "§e[<60%] 산탄 — 화살 6발 확산",
                    "§b전환 60%·25% → 무적");
            case "abyss_guardian" -> List.of(
                    "§7── 3페이즈 (전환 60%·25%) ──",
                    "§8기본 §c×0.6§8 · 원형 폭발 §c×1.3",
                    "§e[<60%] 부채꼴 §c×1.1§e · 연속 타격 §c×0.6§e 연타",
                    "§e[<25%] 낙하 충격 §c×1.6§e · 실명(블라인드) 광역",
                    "§b전환 60%·25% → 무적");
            case "void_herald" -> List.of(
                    "§7── 3페이즈 (전환 55%·20%) ──",
                    "§8기본 §c×0.6§8 · 직선 돌진 §c×1.5",
                    "§e[<55%] 투사체 §c×1.0§e · 유도 구체 §c×1.4",
                    "§b전환 55%·20% → 무적");
            case "rift_king" -> List.of(
                    "§7── 4페이즈 (전환 75·50·25·10%) ──",
                    "§8기본 §c×0.6§8 · 전방 강타 §c×1.2§8 · 원형 폭발 §c×1.3",
                    "§e[<50%] 산탄 — 화살 6발 확산 추가",
                    "§e[<25%] 모든 패턴 쿨 단축(분노)",
                    "§b전환 75·50·25·10% → 무적 4회");
            case "corrupted_dyad" -> List.of(
                    "§7── 이중체 (전환 60%·30%) ──",
                    "§8두 개체 동시 전투 · 전방 강타 §c×1.2§8 · 직선 돌진 §c×1.5",
                    "§e[<30%] 돌진/강타 쿨 단축",
                    "§b전환 60%·30% → 무적");
            case "spirit_watcher" -> List.of(
                    "§7── 3페이즈 (전환 60%·30%) ──",
                    "§8기본 §c×0.6§8 · 산탄(화살 6발)",
                    "§e[>70%] 소환 — 정령 마법사 호출",
                    "§e[<60%] 원형 폭발 §c×1.3 §e[<30%] 투사체 빨라짐",
                    "§b전환 60%·30% → 무적");
            default -> List.of("§83페이즈 페이즈 전환");
        };
    }

    /** 보스별 패턴 요약 (season_boss_patterns.md, DL-129 추가#16 C). 보스정보 GUI 표시용. */
    private static String bossPattern(String id) {
        return switch (id) {
            case "fallen_knight"  -> "3페이즈(60%·30%) · 에너지 석판 · 유도 구체";
            case "corrupted_lord" -> "3페이즈(55%·20%) · 전도체 기둥 · 쉴드+반사 · 관통";
            case "stone_colossus" -> "3페이즈(60%·25%) · 지진 발걸음 · 거석 투척";
            case "storm_sorcerer" -> "3페이즈(60%·25%) · 소환체 봉인 · 번개 장판/포화";
            case "abyss_guardian" -> "3페이즈(60%·25%) · 어둠 결정 · 심연 진동";
            case "void_herald"    -> "3페이즈(55%·20%) · 포탈 파괴 · 허상 처치";
            case "rift_king"      -> "5페이즈(75/50/25/10%) · 무적딜타임×4 · 분신/격자폭발";
            case "corrupted_dyad" -> "이중체 동시전투 · 페이즈 전환 · 반사/연계";
            case "spirit_watcher" -> "진혼 광역 · 페이즈 전환 · 영혼 처치 관문";
            default -> "3페이즈 페이즈 전환";
        };
    }
}
