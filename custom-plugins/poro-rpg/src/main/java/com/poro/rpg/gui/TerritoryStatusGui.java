package com.poro.rpg.gui;

import com.poro.rpg.growth.island.IslandRank;
import com.poro.rpg.growth.island.IslandStorage;
import com.poro.rpg.growth.island.IslandTerritoryState;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 영지 상태 GUI (54슬롯, gui_functional_specs.md §6).
 *
 * <pre>
 * 슬롯  0 : 영지명·작위·최대 보관량
 * 슬롯  9 : 작위 진행도
 * 슬롯 14 : 자동 재배기 상태
 * 슬롯 18 : 저장고 요약
 * 슬롯 23 : 공방 가공기 상태
 * 슬롯 24 : 영지 저장고 상태
 * 슬롯 27 : 자동 입금 토글
 * 슬롯 28 : 자동 심기 토글
 * 슬롯 45 : 뒤로
 * 슬롯 53 : 닫기
 * </pre>
 */
public final class TerritoryStatusGui {

    public static final Component TITLE = Component.text("영지 상태").color(NamedTextColor.DARK_GRAY);

    public static final int SLOT_ISLAND_NAME     = 0;
    public static final int SLOT_RANK_PROGRESS   = 9;
    public static final int SLOT_REAPER          = 14;
    public static final int SLOT_STORAGE_SUMMARY = 18;
    public static final int SLOT_WORKSHOP_MACHINE = 23;
    public static final int SLOT_STORAGE_MACHINE  = 24;
    public static final int SLOT_TOGGLE_DEPOSIT  = 27;
    public static final int SLOT_TOGGLE_PLANT    = 28;
    public static final int SLOT_BACK            = 45;
    public static final int SLOT_CLOSE           = 53;

    private static final NumberFormat FMT = NumberFormat.getNumberInstance(Locale.KOREA);

    private TerritoryStatusGui() {}

    public static void open(Player player, IslandTerritoryState state, IslandStorage storage) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE);
        render(inv, state, storage);
        player.openInventory(inv);
    }

    public static boolean isTitle(Component title) {
        return TITLE.equals(title);
    }

    public static void render(Inventory inv, IslandTerritoryState state, IslandStorage storage) {
        var filler = MainHubGui.icon(Material.BLACK_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < 54; i++) inv.setItem(i, filler);

        inv.setItem(SLOT_ISLAND_NAME,      buildIslandNameIcon(state));
        inv.setItem(SLOT_RANK_PROGRESS,    buildRankProgressIcon(state));
        inv.setItem(SLOT_REAPER,           buildReaperIcon(state));
        inv.setItem(SLOT_STORAGE_SUMMARY,  buildStorageSummaryIcon(state, storage));
        inv.setItem(SLOT_WORKSHOP_MACHINE, buildWorkshopMachineIcon(state));
        inv.setItem(SLOT_STORAGE_MACHINE,  buildStorageMachineIcon(state));

        inv.setItem(SLOT_TOGGLE_DEPOSIT, buildConvToggle(state, IslandTerritoryState.CONV_AUTO_DEPOSIT,
                "자동 입금", "채굴·수확 아이템을 창고에 자동 입금합니다.", 50_000));
        inv.setItem(SLOT_TOGGLE_PLANT,   buildConvToggle(state, IslandTerritoryState.CONV_AUTO_PLANT,
                "자동 심기", "작위 해금 후 씨앗을 자동으로 심습니다.", 50_000));

        inv.setItem(SLOT_BACK, MainHubGui.icon(Material.ARROW, "§7뒤로", List.of("§7영지 메뉴")));
    }

    // ─── 아이템 빌더 ──────────────────────────────────────────────

    private static org.bukkit.inventory.ItemStack buildIslandNameIcon(IslandTerritoryState state) {
        IslandRank rank = state.rank();
        return MainHubGui.icon(Material.NAME_TAG, "§f" + state.islandName(), List.of(
                "§7──────────────",
                "§7작위: §e" + rank.displayName + " §8(T" + (rank.tier + 1) + " / 8)",
                "§7창고 보관: §f" + FMT.format(rank.storagePerItem) + "개 / 재료"
        ));
    }

    private static org.bukkit.inventory.ItemStack buildRankProgressIcon(IslandTerritoryState state) {
        IslandRank rank = state.rank();
        IslandRank next = rank.next();
        List<String> lore = new ArrayList<>();
        lore.add("§7──────────────");
        lore.add("§7현재 작위: §e" + rank.displayName);
        if (next != null) {
            lore.add("§7다음 작위: §f" + next.displayName);
            lore.add("§7──────────────");
            lore.add("§7승급 비용 (골드): §e" + rank.goldUpgradeCostDisplay());
            lore.add("§8자세한 조건 — 추후 공개");
        } else {
            lore.add("§6§l☆ 최고 단계 달성!");
        }
        return MainHubGui.icon(Material.TOTEM_OF_UNDYING, "§e작위 진행도", lore);
    }

    private static org.bukkit.inventory.ItemStack buildReaperIcon(IslandTerritoryState state) {
        int count = state.reaperCount();
        if (count == 0) {
            return MainHubGui.icon(Material.GRAY_STAINED_GLASS_PANE, "§7자동 재배기",
                    List.of("§7──────────────", "§7설치 수: §f0개", "§7상태: §8미설치"));
        }
        return MainHubGui.icon(Material.LIME_STAINED_GLASS_PANE, "§f자동 재배기", List.of(
                "§7──────────────",
                "§7설치 수: §e" + count + "개",
                "§7생산 품목: §e제국 약초",
                "§7주기: §f20분",
                "§7상태: §a작동 중"
        ));
    }

    private static org.bukkit.inventory.ItemStack buildStorageSummaryIcon(
            IslandTerritoryState state, IslandStorage storage) {
        int typeCount = storage.materialTypeCount();
        Map.Entry<Material, Long> top = storage.topEntry();
        List<String> lore = new ArrayList<>();
        lore.add("§7──────────────");
        lore.add("§7등록 재료 종류: §e" + typeCount + "종");
        if (top != null) {
            lore.add("§7가장 많은 재료: §e" + top.getKey().name() + " §f×" + FMT.format(top.getValue()));
        } else {
            lore.add("§7보유 재료: §8없음");
        }
        lore.add("§7최대 보관량: §f" + FMT.format(state.rank().storagePerItem) + "개 / 재료");
        lore.add("§7──────────────");
        lore.add("§8창고 열기: 영지 메뉴 → 창고");
        return MainHubGui.icon(Material.CHEST, "§e영지 저장고", lore);
    }

    private static org.bukkit.inventory.ItemStack buildWorkshopMachineIcon(IslandTerritoryState state) {
        int used = state.workshopQueueUsed();
        int max  = state.workshopQueueMax();
        if (max == 0) {
            return MainHubGui.icon(Material.GRAY_STAINED_GLASS_PANE, "§7공방 가공기",
                    List.of("§7──────────────", "§7상태: §8미설치"));
        }
        boolean active = !state.workshopJobs().isEmpty();
        String title = active ? "§6공방 가공기 §a[가동 중]" : "§f공방 가공기";
        return MainHubGui.icon(active ? Material.BLAST_FURNACE : Material.FURNACE, title, List.of(
                "§7──────────────",
                "§7대기열: §f" + used + " / " + max + "슬롯",
                active ? "§7상태: §a가동 중" : "§8대기 중"
        ));
    }

    private static org.bukkit.inventory.ItemStack buildStorageMachineIcon(IslandTerritoryState state) {
        int count = state.storageCount();
        if (count == 0) {
            return MainHubGui.icon(Material.GRAY_STAINED_GLASS_PANE, "§7영지 저장고",
                    List.of("§7──────────────", "§7상태: §8미설치"));
        }
        return MainHubGui.icon(Material.LIME_STAINED_GLASS_PANE, "§f영지 저장고", List.of(
                "§7──────────────",
                "§7설치 수: §e" + count + "개",
                "§7상태: §a설치 완료"
        ));
    }

    private static org.bukkit.inventory.ItemStack buildConvToggle(
            IslandTerritoryState state, int bit, String name, String desc, long price) {
        boolean on = state.hasConvenience(bit);
        if (on) {
            return MainHubGui.icon(Material.LIME_DYE, "§a" + name + ": §2[ON]",
                    List.of("§7──────────────", "§7" + desc,
                            "§7구매가: §f" + FMT.format(price) + "G §8(구매 완료)"));
        }
        return MainHubGui.icon(Material.GRAY_DYE, "§7" + name + ": §8[OFF]",
                List.of("§7──────────────", "§7" + desc,
                        "§7구매가: §e" + FMT.format(price) + "G",
                        "§7──────────────",
                        "§7클릭 §f구매 (골드 차감 — 추후 연동)"));
    }
}
