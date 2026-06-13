package kr.zenon.rpg.gui;

import kr.zenon.rpg.growth.island.IslandTerritoryState;
import kr.zenon.rpg.growth.island.IslandTerritoryStateStore;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 영지 이동 GUI (27슬롯, 3×9).
 *
 * <pre>
 * row0: [내영지] ░ ░ ░ ░ ░ ░ ░ ░
 * row1: [공개1~9]
 * row2: [뒤로]   ░ ░ [◀] [페이지] [▶] ░ ░ ░
 * </pre>
 */
public final class TerritoryMoveGui {
    private TerritoryMoveGui() {}

    public static final int SLOT_MY_ISLAND = 0;
    public static final int SLOT_PUBLIC_START = 9;
    public static final int SLOT_PUBLIC_END   = 17;
    public static final int SLOT_BACK         = 18;
    public static final int SLOT_PREV_PAGE    = 21;
    public static final int SLOT_PAGE_INFO    = 22;
    public static final int SLOT_NEXT_PAGE    = 23;

    private static final int PAGE_SIZE = SLOT_PUBLIC_END - SLOT_PUBLIC_START + 1;

    public static void open(Player player, IslandTerritoryState territory, IslandTerritoryStateStore store) {
        open(player, territory, store, 0);
    }

    public static void open(Player player, IslandTerritoryState territory, IslandTerritoryStateStore store, int page) {
        Inventory inv = Bukkit.createInventory(null, 27, GuiTitles.TERRITORY_MOVE);
        ItemStack gray = MainHubGui.icon(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < 27; i++) inv.setItem(i, gray);

        inv.setItem(SLOT_MY_ISLAND, myIslandIcon(player, territory));

        // 공개 영지 목록 (자기 영지 제외, PUBLIC만, 멤버수 desc → 작위 desc → 이름 asc)
        UUID selfUuid = player.getUniqueId();
        List<Map.Entry<UUID, IslandTerritoryState>> publicIslands = store.snapshot().entrySet().stream()
                .filter(e -> !e.getKey().equals(selfUuid))
                .filter(e -> e.getValue().visitMode() == IslandTerritoryState.VisitMode.PUBLIC)
                .sorted(Comparator
                        .comparingInt((Map.Entry<UUID, IslandTerritoryState> e) -> -e.getValue().memberCount())
                        .thenComparingInt(e -> -e.getValue().rank().tier)
                        .thenComparing(e -> e.getValue().islandName()))
                .collect(Collectors.toList());

        int totalPages = Math.max(1, (publicIslands.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        int safePage   = Math.max(0, Math.min(page, totalPages - 1));
        int startIdx   = safePage * PAGE_SIZE;
        int endIdx     = Math.min(publicIslands.size(), startIdx + PAGE_SIZE);

        if (publicIslands.isEmpty()) {
            inv.setItem(13, MainHubGui.icon(Material.GRAY_STAINED_GLASS_PANE,
                    "§8공개된 영지가 없습니다.",
                    List.of("§7──────────────",
                            "§7다른 플레이어가 영지 설정에서",
                            "§7방문 공개를 활성화하면 표시됩니다.")));
        } else {
            for (int i = 0; i < (endIdx - startIdx); i++) {
                var entry = publicIslands.get(startIdx + i);
                inv.setItem(SLOT_PUBLIC_START + i, publicIslandIcon(entry.getKey(), entry.getValue()));
            }
        }

        // 페이지 네비게이션
        boolean hasPrev = safePage > 0;
        boolean hasNext = safePage < totalPages - 1;
        inv.setItem(SLOT_PREV_PAGE, hasPrev
                ? MainHubGui.icon(Material.ARROW,    "§f◀ 이전 페이지", List.of("§7" + safePage + " / " + totalPages))
                : MainHubGui.icon(Material.GRAY_DYE, "§8◀ 이전 페이지", List.of("§8첫 페이지입니다.")));
        inv.setItem(SLOT_PAGE_INFO, MainHubGui.icon(Material.PAPER,
                "§f" + (safePage + 1) + " / " + totalPages, List.of()));
        inv.setItem(SLOT_NEXT_PAGE, hasNext
                ? MainHubGui.icon(Material.ARROW,    "§f다음 페이지 ▶", List.of("§7" + (safePage + 2) + " / " + totalPages))
                : MainHubGui.icon(Material.GRAY_DYE, "§8다음 페이지 ▶", List.of("§8마지막 페이지입니다.")));

        inv.setItem(SLOT_BACK, MainHubGui.icon(Material.ARROW, "§7뒤로", List.of("§7영지 관리")));

        player.openInventory(inv);
    }

    private static ItemStack myIslandIcon(Player player, IslandTerritoryState territory) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        meta.setOwningPlayer(player);
        meta.displayName(Component.text("§6[내 영지]"));
        meta.lore(List.of(
                Component.text("§7──────────────"),
                Component.text("§7§e" + territory.islandName()),
                Component.text("§7작위: §f" + territory.rank().displayName),
                Component.text("§7──────────────"),
                Component.text("§7클릭 → 내 영지 스폰으로 이동")
        ));
        head.setItemMeta(meta);
        return head;
    }

    private static ItemStack publicIslandIcon(UUID ownerUuid, IslandTerritoryState territory) {
        OfflinePlayer owner = Bukkit.getOfflinePlayer(ownerUuid);
        String ownerName = owner.getName() != null ? owner.getName() : ownerUuid.toString().substring(0, 8);
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        meta.setOwningPlayer(owner);
        meta.displayName(Component.text("§f" + territory.islandName()));
        meta.lore(List.of(
                Component.text("§7──────────────"),
                Component.text("§7소유자: §f" + ownerName),
                Component.text("§7작위: §f" + territory.rank().displayName),
                Component.text("§7──────────────"),
                Component.text("§7방문자 §f" + territory.memberCount() + "명"),
                Component.text("§a▶ 클릭하여 방문")
        ));
        head.setItemMeta(meta);
        return head;
    }

    /** GUI 슬롯에서 공개 영지 UUID 추출 (현재 페이지 기준). null이면 영지 슬롯 아님. */
    public static UUID publicOwnerAt(IslandTerritoryStateStore store, UUID selfUuid, int slot, int page) {
        if (slot < SLOT_PUBLIC_START || slot > SLOT_PUBLIC_END) return null;
        List<Map.Entry<UUID, IslandTerritoryState>> publicIslands = store.snapshot().entrySet().stream()
                .filter(e -> !e.getKey().equals(selfUuid))
                .filter(e -> e.getValue().visitMode() == IslandTerritoryState.VisitMode.PUBLIC)
                .sorted(Comparator
                        .comparingInt((Map.Entry<UUID, IslandTerritoryState> e) -> -e.getValue().memberCount())
                        .thenComparingInt(e -> -e.getValue().rank().tier)
                        .thenComparing(e -> e.getValue().islandName()))
                .collect(Collectors.toList());
        int idx = page * PAGE_SIZE + (slot - SLOT_PUBLIC_START);
        return idx < publicIslands.size() ? publicIslands.get(idx).getKey() : null;
    }
}
