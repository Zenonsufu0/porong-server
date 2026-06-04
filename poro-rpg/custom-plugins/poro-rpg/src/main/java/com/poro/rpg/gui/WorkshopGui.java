package com.poro.rpg.gui;

import com.poro.rpg.growth.island.IslandTerritoryState;
import com.poro.rpg.growth.island.WorkshopJob;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * 공방 GUI (54슬롯).
 *
 * <pre>
 * row0 (슬롯 0~7) : 탭 버튼 8종 (LIME or GRAY pane)
 * row1 (슬롯 9~17): 레시피 목록 (최대 9개)
 * row2 (슬롯 18~26): 대기열 1~9
 * row3 (슬롯 27~35): 대기열 10~18
 * row4 (슬롯 36~44): 여백
 * row5 (슬롯 45~53): 뒤로(45) / 닫기(53)
 * </pre>
 */
public final class WorkshopGui {

    public enum WorkshopTab {
        ESTATE      ("영지 제작",     Material.GRASS_BLOCK,       "영지 재료로 정수·흔적을 제작합니다."),
        TRACE       ("강화 흔적",     Material.ENCHANTED_BOOK,    "강화 성공률 보정 흔적을 제작합니다."),
        SMELT       ("제련",         Material.BLAST_FURNACE,     "원석을 마도합금으로 제련합니다."),
        REFINE      ("정제",         Material.BREWING_STAND,     "약초·정수를 정제 약초로 가공합니다."),
        ALCHEMY_HEAL("연금술 (치료)", Material.POTION,            "치료 포션을 제조합니다."),
        ALCHEMY_BOOST("연금술 (부스트)", Material.SPLASH_POTION,  "효과 부스트 포션을 제조합니다."),
        COOK        ("요리",         Material.CAMPFIRE,          "만찬 음식을 요리합니다."),
        ORE_CONVERT ("광물 변환",    Material.DIAMOND,           "광물 과잉분을 희귀 광물로 변환합니다.");

        public final String displayName;
        public final Material icon;
        public final String description;

        WorkshopTab(String displayName, Material icon, String description) {
            this.displayName = displayName;
            this.icon = icon;
            this.description = description;
        }
    }

    public static final Component TITLE_PREFIX = Component.text("공방 — ");
    public static final int SLOT_BACK  = 45;
    public static final int SLOT_CLOSE = 53;

    private static final WorkshopTab[] TABS = WorkshopTab.values();

    private WorkshopGui() {}

    // ─── 오픈 ────────────────────────────────────────────────────

    public static void open(Player player, WorkshopTab tab) {
        open(player, tab, null);
    }

    public static void open(Player player, WorkshopTab tab, IslandTerritoryState territory) {
        Component title = TITLE_PREFIX.append(Component.text(tab.displayName).color(NamedTextColor.YELLOW));
        Inventory inv = Bukkit.createInventory(null, 54, title);
        render(inv, tab, territory);
        player.openInventory(inv);
    }

    // ─── 유틸 ────────────────────────────────────────────────────

    public static boolean isTitle(Component title) {
        String plain = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
                .plainText().serialize(title);
        return plain.startsWith("공방 — ");
    }

    public static WorkshopTab tabFromTitle(Component title) {
        String plain = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
                .plainText().serialize(title);
        for (WorkshopTab t : TABS) {
            if (plain.equals("공방 — " + t.displayName)) return t;
        }
        return null;
    }

    // ─── 렌더 ────────────────────────────────────────────────────

    public static void render(Inventory inv, WorkshopTab activeTab, IslandTerritoryState territory) {
        ItemStack filler = icon(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < 54; i++) inv.setItem(i, filler);

        // row0: 탭 버튼 (슬롯 0~7)
        for (int i = 0; i < TABS.length; i++) {
            inv.setItem(i, buildTabItem(TABS[i], TABS[i] == activeTab));
        }

        // row1: 레시피 목록 (슬롯 9~17)
        List<WorkshopRecipe> recipes = WorkshopRecipeRegistry.getRecipes(activeTab);
        for (int i = 0; i < 9; i++) {
            if (i < recipes.size()) {
                inv.setItem(9 + i, buildRecipeIcon(recipes.get(i)));
            }
        }

        // row2-3: 대기열 (슬롯 18~35)
        if (territory != null) {
            int maxSlots = Math.min(territory.workshopQueueMax(), 18);
            List<WorkshopJob> jobs = territory.workshopJobs();
            for (int i = 0; i < maxSlots; i++) {
                if (i < jobs.size()) {
                    inv.setItem(18 + i, buildJobIcon(jobs.get(i)));
                } else {
                    inv.setItem(18 + i, buildEmptyQueueSlot());
                }
            }
        }

        inv.setItem(SLOT_BACK, icon(Material.DARK_OAK_DOOR, "§7◀ 뒤로", List.of("§7영지 메뉴")));
    }

    // ─── 아이템 빌더 ─────────────────────────────────────────────

    private static ItemStack buildTabItem(WorkshopTab tab, boolean active) {
        // 비활성 탭도 고유 아이콘으로 표시 — 회색 유리(배경과 동일)면 구분 불가했음(DL-129 추가#4).
        String nameColor = active ? "§a§l" : "§e";
        List<String> lore = List.of(
                "§7" + tab.description,
                "§7──────────",
                active ? "§a▶ 선택된 탭" : "§7클릭하여 선택");
        ItemStack item = new ItemStack(tab.icon);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text((active ? "" : "§r") + nameColor + tab.displayName));
        meta.lore(lore.stream().map(Component::text).toList());
        if (active) { // 활성 탭 강조 — 인챈트 글로우
            meta.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
        }
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack buildRecipeIcon(WorkshopRecipe recipe) {
        List<String> lore = new ArrayList<>();
        // 결과물 효과/사용법 (workshop_crafting_spec.md, DL-129 추가#8)
        List<String> effect = effectLore(recipe.resultItemId());
        if (!effect.isEmpty()) {
            lore.addAll(effect);
            lore.add("§7──────────────");
        }
        lore.add("§7재료:");
        for (var mat : recipe.materials()) {
            lore.add("  §7- §f" + WorkshopRecipeRegistry.displayName(mat.itemId())
                    + " §7×" + mat.amount());
        }
        lore.add("§7──────────────");
        lore.add("§7제작 시간: §e" + recipe.durationMinutes() + "분");
        lore.add("§7결과물: §e" + recipe.displayName() + " §7×" + recipe.resultAmount());
        lore.add("§7──────────────");
        lore.add("§e좌클릭 §7제작 등록");
        // 결과물 전용 텍스처가 있으면 paper+CMD로 렌더(리소스팩 paper.json), 없으면 기본 Material (DL-129 추가#12)
        Integer cmd = CustomItemModel.cmd(recipe.resultItemId());
        if (cmd != null) {
            ItemStack item = icon(CustomItemModel.CARRIER, "§f" + recipe.displayName(), lore);
            CustomItemModel.applyModel(item, cmd);
            return item;
        }
        return icon(recipe.guiIcon(), "§f" + recipe.displayName(), lore);
    }

    /** 결과물 효과·사용법 설명 (정본 workshop_crafting_spec.md §6~10). 없으면 빈 목록. */
    private static List<String> effectLore(String resultItemId) {
        return switch (resultItemId) {
            // 만찬 (효과 30분 지속, 만찬은 1종만 적용)
            case "con_feast_warrior"  -> List.of("§a효과: 공격력 +10%", "§830분 지속 · 만찬 1종만 적용");
            case "con_feast_slayer"   -> List.of("§a효과: 모든 피해 +8%", "§830분 지속 · 만찬 1종만 적용");
            case "con_feast_assassin" -> List.of("§a효과: 치명타 피해 +20%", "§830분 지속 · 만찬 1종만 적용");
            case "con_feast_hunter"   -> List.of("§a효과: 일반 몹 피해 +15%", "§8보스 미적용 · 30분 · 만찬 1종만");
            // 치료 포션 (필드 무제한, 보스전 횟수 제한)
            case "con_heal_minor" -> List.of("§a효과: 최대 HP 30% 즉시 회복", "§8필드 무제한 · 보스전 3회");
            case "con_heal_mid"   -> List.of("§a효과: 최대 HP 40% 즉시 회복", "§8필드 무제한 · 보스전 4회");
            case "con_heal_major" -> List.of("§a효과: 최대 HP 50% 즉시 회복", "§8필드 무제한 · 보스전 5회");
            // 부스트 포션 (30분, 3종 동시 가능)
            case "con_potion_gold"    -> List.of("§a효과: 골드 획득량 +50%", "§830분 지속 · 부스트 3종 중첩 가능");
            case "con_potion_enhance" -> List.of("§a효과: 강화석 획득량 +50%", "§830분 지속 · 부스트 3종 중첩 가능");
            case "con_potion_exp"     -> List.of("§a효과: 경험치 획득량 +50%", "§830분 지속 · 부스트 3종 중첩 가능");
            // 강화 흔적 (10강↑ 강화 시 선택 소모, 성공률 곱연산, 3종 동시 가능 최대 ×1.70)
            case "mat_trace_star" -> List.of("§b강화 성공률 ×1.15", "§810강 이상 강화 시 선택 소모 · 3종 동시 가능");
            case "mat_trace_moon" -> List.of("§b강화 성공률 ×1.25", "§810강 이상 강화 시 선택 소모 · 3종 동시 가능");
            case "mat_trace_sun"  -> List.of("§b강화 성공률 ×1.30", "§810강 이상 강화 시 선택 소모 · 3종 동시 가능");
            // 고대흔적 (미감정 흔적과 함께 사용 시 최소 등급 보장)
            case "ancient_trace_faded"     -> List.of("§d미감정 흔적과 함께 사용", "§7→ §f레어 이상§7 등급 보장");
            case "ancient_trace_glowing"   -> List.of("§d미감정 흔적과 함께 사용", "§7→ §5에픽 이상§7 등급 보장");
            case "ancient_trace_radiant"   -> List.of("§d미감정 흔적과 함께 사용", "§7→ §6유니크 이상§7 등급 보장");
            case "ancient_trace_brilliant" -> List.of("§d미감정 흔적과 함께 사용", "§7→ §a레전더리 확정");
            // 미감정 흔적 (우클릭 개봉)
            case "equip_trace_unidentified" -> List.of(
                    "§e우클릭: 장비의 흔적 1개 획득",
                    "§8단독 개봉 = 커먼~레전더리 랜덤",
                    "§8고대흔적과 함께 사용 = 최소 등급 보장");
            // 핵심 재료
            case "mat_mado_alloy"   -> List.of("§7강화 흔적·고대흔적의 핵심 재료");
            case "mat_essence_farmer", "mat_essence_miner", "mat_essence_nature"
                                    -> List.of("§7제작 체인 중간 재료");
            case "mat_refined_herb" -> List.of("§7포션·만찬 제작 재료");
            default -> List.of();
        };
    }

    private static ItemStack buildJobIcon(WorkshopJob job) {
        long now = System.currentTimeMillis();
        long remaining = Math.max(0, job.completeAt() - now);
        boolean done = remaining == 0;

        String status = done ? "§a완료 — 다음 접속 시 자동 입금" : "§e제작 중";
        String timeStr = done ? "" : formatMs(remaining);

        List<String> lore = new ArrayList<>();
        lore.add("§7──────────────");
        lore.add("§7레시피: §f" + job.recipeId());
        lore.add("§7상태: " + status);
        if (!done) lore.add("§7완료까지: §f" + timeStr);

        Material icon = done ? Material.LIME_STAINED_GLASS_PANE : Material.ORANGE_STAINED_GLASS_PANE;
        String title = done ? "§a[완료] " : "§e[제작 중] ";
        return icon(icon, title + job.recipeId(), lore);
    }

    private static ItemStack buildEmptyQueueSlot() {
        return icon(Material.WHITE_STAINED_GLASS_PANE, "§8[빈 슬롯]",
                List.of("§7레시피를 클릭하면 여기에 등록됩니다."));
    }

    private static String formatMs(long ms) {
        long totalSec = ms / 1000;
        long min = totalSec / 60;
        long sec = totalSec % 60;
        return min > 0 ? min + "분 " + sec + "초" : sec + "초";
    }

    static ItemStack icon(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name));
        meta.lore(lore.stream().map(Component::text).toList());
        item.setItemMeta(meta);
        return item;
    }
}
