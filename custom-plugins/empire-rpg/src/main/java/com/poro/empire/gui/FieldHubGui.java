package com.poro.empire.gui;

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

    // 슬롯 배치: 3 + 2 레이아웃
    private static final int[] FIELD_SLOTS = {10, 12, 14, 21, 23};

    public static void open(Player player, ExploreHubGui.FieldStateProvider provider) {
        Inventory inv = Bukkit.createInventory(null, 54, GuiTitles.FIELD_HUB);
        ItemStack pane = MainHubGui.icon(Material.BLACK_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < 54; i++) inv.setItem(i, pane);

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

        inv.setItem(45, MainHubGui.icon(Material.ARROW,   "§7뒤로", List.of("§7메인 메뉴")));
        inv.setItem(49, MainHubGui.icon(Material.BARRIER, "§c닫기", List.of()));
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
