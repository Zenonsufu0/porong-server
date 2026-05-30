package com.poro.empire.gui;

import com.poro.empire.growth.island.IslandTerritoryState;
import com.poro.empire.growth.island.IslandTerritoryStateStore;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 관리자 영지 관리 GUI (54슬롯). 전체 영지 목록 + 작위 강제 변경 + 소셜 초기화.
 * <pre>
 * row0~4 (0~44): 영지 목록 (작위 내림차순). 좌클릭=작위▲, 우클릭=작위▼, Shift+우클릭=초기화
 * row5:   [뒤로] [이전][페이지][다음] ... [닫기]
 * </pre>
 * 작위/시설/창고는 보존하고, 초기화는 멤버·권한·방문모드만 리셋한다 (/empire-island-reset 동일).
 */
public final class AdminTerritoryGui {
    private AdminTerritoryGui() {}

    public static final int PAGE_SIZE = 45;
    public static final int SLOT_BACK  = 45;
    public static final int SLOT_PREV  = 47;
    public static final int SLOT_PAGE  = 48;
    public static final int SLOT_NEXT  = 49;
    public static final int SLOT_CLOSE = 53;

    /** 슬롯 인덱스 → ownerUuid 매핑 + 페이지 정보. */
    public record View(List<UUID> pageOwners, int page, int totalPages) {}

    public static View open(Player admin, IslandTerritoryStateStore store, int page) {
        Map<UUID, IslandTerritoryState> all = store.snapshot();
        // 작위 내림차순 → owner UUID 안정 정렬
        List<UUID> owners = new ArrayList<>(all.keySet());
        owners.sort(Comparator
                .comparingInt((UUID u) -> all.get(u).rank().ordinal()).reversed()
                .thenComparing(UUID::toString));

        int totalPages = Math.max(1, (owners.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        int safePage   = Math.max(0, Math.min(page, totalPages - 1));

        Inventory inv = Bukkit.createInventory(null, 54, GuiTitles.ADMIN_TERRITORY);
        ItemStack gray = MainHubGui.icon(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < 54; i++) inv.setItem(i, gray);

        int from = safePage * PAGE_SIZE;
        int to   = Math.min(from + PAGE_SIZE, owners.size());
        List<UUID> pageOwners = new ArrayList<>();
        if (owners.isEmpty()) {
            inv.setItem(22, MainHubGui.icon(Material.PAPER, "§7등록된 영지 없음",
                    List.of("§8아직 생성된 영지가 없습니다")));
        } else {
            for (int i = from; i < to; i++) {
                UUID owner = owners.get(i);
                pageOwners.add(owner);
                inv.setItem(i - from, territoryIcon(owner, all.get(owner)));
            }
        }

        inv.setItem(SLOT_BACK,  MainHubGui.icon(Material.ARROW, "§7뒤로", List.of("§7관리자 허브")));
        inv.setItem(SLOT_PREV,  safePage > 0
                ? MainHubGui.icon(Material.SPECTRAL_ARROW, "§e◀ 이전 페이지", List.of()) : gray);
        inv.setItem(SLOT_PAGE,  MainHubGui.icon(Material.MAP,
                "§f페이지 " + (safePage + 1) + " §7/ " + totalPages,
                List.of("§8총 " + owners.size() + "개 영지")));
        inv.setItem(SLOT_NEXT,  safePage < totalPages - 1
                ? MainHubGui.icon(Material.SPECTRAL_ARROW, "§e다음 페이지 ▶", List.of()) : gray);
        inv.setItem(SLOT_CLOSE, MainHubGui.icon(Material.BARRIER, "§c닫기", List.of()));

        admin.openInventory(inv);
        return new View(pageOwners, safePage, totalPages);
    }

    private static ItemStack territoryIcon(UUID owner, IslandTerritoryState t) {
        return MainHubGui.icon(Material.GRASS_BLOCK,
                "§a" + nameOf(owner) + " §7- §e" + t.rank().displayName,
                List.of("§7──────────────",
                        "§7작위: §e" + t.rank().displayName + " §8(" + (t.rank().ordinal() + 1) + "/8)",
                        "§7시설: §f낫 " + t.reaperCount() + " §7| §f창고 " + t.storageCount() + " §7| §f광물 " + t.minerCount(),
                        "§7──────────────",
                        "§a좌클릭 = 작위 ▲",
                        "§e우클릭 = 작위 ▼",
                        "§cShift+우클릭 = 소셜 초기화 (멤버·권한·방문)"));
    }

    private static String nameOf(UUID uuid) {
        String name = Bukkit.getOfflinePlayer(uuid).getName();
        return name != null ? name : uuid.toString().substring(0, 8);
    }
}
