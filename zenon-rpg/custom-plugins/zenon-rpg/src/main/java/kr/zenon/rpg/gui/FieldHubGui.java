package kr.zenon.rpg.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public final class FieldHubGui {
    private FieldHubGui() {}

    public record FieldDef(String id, String displayName, Material icon) {}

    public static final List<FieldDef> FIELDS = List.of(
            new FieldDef("plain",   "수도 외곽 평원", Material.GRASS_BLOCK),
            new FieldDef("mine",    "폐광 지대",      Material.COAL_ORE),
            new FieldDef("sewer",   "오염된 수로",    Material.WATER_BUCKET),
            new FieldDef("outpost", "무너진 초소",    Material.CHISELED_STONE_BRICKS),
            new FieldDef("ruins",   "고대 성벽 잔해", Material.MOSSY_COBBLESTONE)
    );

    // 슬롯 배치: row1 한 줄 (27슬롯)
    private static final int[] FIELD_SLOTS = {9, 10, 11, 12, 13};
    /** 정예 모드 토글 버튼 슬롯 (하단 줄). */
    public static final int ELITE_TOGGLE_SLOT = 22;

    public static void open(Player player, ExploreHubGui.FieldStateProvider provider, boolean eliteOn) {
        Inventory inv = Bukkit.createInventory(null, 27, GuiTitles.FIELD_HUB);
        ItemStack pane = MainHubGui.icon(Material.BLACK_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < 27; i++) inv.setItem(i, pane);

        // 정예 모드 토글 (DL-129 추가#6) — 본인 주변 웨이브 정예화
        if (eliteOn) {
            inv.setItem(ELITE_TOGGLE_SLOT, MainHubGui.icon(Material.LIME_DYE, "§a§l정예 모드: ON",
                    List.of("§7──────────────",
                            "§7주변 필드 웨이브가 §f정예 몹§7으로 등장",
                            "§8(수 적고 강함)",
                            "§7──────────────",
                            "§c클릭하여 끄기")));
        } else {
            inv.setItem(ELITE_TOGGLE_SLOT, MainHubGui.icon(Material.GRAY_DYE, "§7정예 모드: OFF",
                    List.of("§7──────────────",
                            "§7일반 몹이 등장합니다",
                            "§7──────────────",
                            "§a클릭하여 켜기")));
        }

        for (int i = 0; i < FIELDS.size(); i++) {
            FieldDef f = FIELDS.get(i);
            ExploreHubGui.BossStatus status   = provider.status(f.id());
            int respawnMin                     = provider.respawnMinutes(f.id());
            int count                          = provider.playerCount(f.id());

            String statusLine = switch (status) {
                case ALIVE      -> "§c필드보스 출현 중";
                case DEAD       -> "§e필드보스 처치됨";
                case RESPAWNING -> "§7필드보스 리스폰 §f" + respawnMin + "분 후";
            };

            inv.setItem(FIELD_SLOTS[i], MainHubGui.icon(f.icon(), "§f" + f.displayName(),
                    List.of("§7──────────────",
                            statusLine,
                            "§7현재 플레이어: §e" + count + "명",
                            "",
                            "§a클릭하여 이동")));
        }

        inv.setItem(18, MainHubGui.icon(Material.ARROW, "§7뒤로", List.of("§7메인 메뉴")));
        player.openInventory(inv);
    }

    /** 슬롯 번호로 fieldId 반환. 해당 없으면 null. */
    public static String fieldIdAt(int slot) {
        for (int i = 0; i < FIELD_SLOTS.length; i++) {
            if (FIELD_SLOTS[i] == slot) return FIELDS.get(i).id();
        }
        return null;
    }
}
