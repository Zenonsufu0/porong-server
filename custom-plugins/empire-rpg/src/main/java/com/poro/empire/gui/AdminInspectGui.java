package com.poro.empire.gui;

import com.poro.empire.combat.weapon.WeaponType;
import com.poro.empire.growth.GrowthStateStore;
import com.poro.empire.growth.engine.EquipmentSlot;
import com.poro.empire.growth.engine.PlayerEquipmentItem;
import com.poro.empire.growth.engine.PlayerGrowthState;
import com.poro.empire.growth.island.IslandTerritoryState;
import com.poro.empire.growth.island.IslandTerritoryStateStore;
import com.poro.empire.pvp.PvpRatingService;
import com.poro.empire.storage.PlayerDataManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 플레이어 인스펙트 GUI (27슬롯).
 * <pre>
 * row0: [head] ░ [직업] ░ [영지] ░ [PvP] ░ [재화]
 * row1: [강화] [무기] [투구] [상의] [하의] [신발] ░ ░ ░
 * row2: [뒤로] ░ ░ ░ ░ ░ ░ ░ [닫기]
 * </pre>
 */
public final class AdminInspectGui {
    private AdminInspectGui() {}

    public static final int SLOT_BACK  = 18;
    public static final int SLOT_CLOSE = 26;

    public static void open(Player admin, UUID targetUuid,
                            PlayerDataManager playerDataManager,
                            GrowthStateStore growthStateStore,
                            IslandTerritoryStateStore islandStore,
                            PvpRatingService ratingService) {
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetUuid);
        String targetName = target.getName() != null ? target.getName() : targetUuid.toString().substring(0, 8);

        Inventory inv = Bukkit.createInventory(null, 27, GuiTitles.ADMIN_INSPECT);
        ItemStack gray = MainHubGui.icon(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < 27; i++) inv.setItem(i, gray);

        // 헤드 (대상)
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        meta.setOwningPlayer(target);
        meta.displayName(Component.text("§e" + targetName));
        meta.lore(List.of(
                Component.text("§7UUID: §f" + targetUuid),
                Component.text("§7접속: " + (target.isOnline() ? "§a온라인" : "§8오프라인"))
        ));
        head.setItemMeta(meta);
        inv.setItem(0, head);

        // 직업 / 레벨
        WeaponType wt = playerDataManager.getWeaponType(targetUuid);
        PlayerGrowthState state = growthStateStore.get(targetUuid).orElse(null);
        inv.setItem(2, MainHubGui.icon(Material.NETHERITE_SWORD, "§b직업",
                List.of("§7직업: §f" + (wt == WeaponType.NONE ? "미선택" : wt.name()),
                        "§7레벨: §e" + (state != null ? state.playerLevel() : 1))));

        // 영지
        IslandTerritoryState territory = islandStore.get(targetUuid).orElse(null);
        if (territory != null) {
            inv.setItem(4, MainHubGui.icon(Material.GRASS_BLOCK, "§a영지",
                    List.of("§7이름: §f" + territory.islandName(),
                            "§7작위: §f" + territory.rank().displayName,
                            "§7멤버: §f" + territory.memberCount() + " / " + IslandTerritoryState.MAX_MEMBERS,
                            "§7약초 재배기: §f" + territory.reaperCount() + "  광물 채굴기: §f" + territory.minerCount(),
                            "§7방문 설정: §f" + territory.visitMode().name())));
        } else {
            inv.setItem(4, MainHubGui.icon(Material.GRAY_STAINED_GLASS_PANE, "§8영지",
                    List.of("§7영지 데이터 없음")));
        }

        // PvP
        if (ratingService != null) {
            PvpRatingService.Rating r = ratingService.getOrInit(targetUuid, targetName);
            inv.setItem(6, MainHubGui.icon(Material.IRON_SWORD, "§c정규대전",
                    List.of("§7점수: §f" + r.score(),
                            "§a승: §f" + r.wins() + " §c패: §f" + r.losses())));
        }

        // 재화
        if (state != null) {
            inv.setItem(8, MainHubGui.icon(Material.GOLD_INGOT, "§6재화",
                    List.of("§7골드: §e" + state.currency("gold"),
                            "§7강화석: §f" + state.currency("mat_stone_enhance"),
                            "§7큐브: §b" + state.currency("mat_cube"),
                            "§7큐브 조각: §f" + state.currency("mat_cube_fragment"),
                            "§7전장의 파편: §f" + state.currency("mat_battle_shard"))));
        }

        // 장비 5슬롯 (row1)
        if (state != null) {
            int totalIl = 0;
            int slotIdx = 10;
            for (EquipmentSlot eq : new EquipmentSlot[]{
                    EquipmentSlot.WEAPON, EquipmentSlot.HELMET, EquipmentSlot.CHESTPLATE,
                    EquipmentSlot.LEGGINGS, EquipmentSlot.BOOTS}) {
                PlayerEquipmentItem item = state.equippedItem(eq).orElse(null);
                int enhance = item != null ? item.enhanceLevel() : 0;
                totalIl += enhance * 5;
                inv.setItem(slotIdx++, MainHubGui.icon(
                        item != null ? equipMaterial(eq) : Material.GRAY_STAINED_GLASS_PANE,
                        item != null ? "§f" + eq.name() + " §e+" + enhance : "§8" + eq.name() + " (미장착)",
                        item != null
                                ? List.of("§7ID: §f" + item.itemId(), "§7IL: §e" + (enhance * 5))
                                : List.of("§8슬롯 비어있음")));
            }
            inv.setItem(9, MainHubGui.icon(Material.ANVIL, "§e평균 IL",
                    List.of("§75슬롯 평균: §e" + (totalIl / 5.0) + " IL")));
        }

        inv.setItem(SLOT_BACK,  MainHubGui.icon(Material.ARROW,   "§7뒤로", List.of("§7관리자 허브")));
        inv.setItem(SLOT_CLOSE, MainHubGui.icon(Material.BARRIER, "§c닫기", List.of()));
        admin.openInventory(inv);
    }

    private static Material equipMaterial(EquipmentSlot slot) {
        return switch (slot) {
            case WEAPON     -> Material.DIAMOND_SWORD;
            case HELMET     -> Material.DIAMOND_HELMET;
            case CHESTPLATE -> Material.DIAMOND_CHESTPLATE;
            case LEGGINGS   -> Material.DIAMOND_LEGGINGS;
            case BOOTS      -> Material.DIAMOND_BOOTS;
            default         -> Material.PAPER;
        };
    }
}
