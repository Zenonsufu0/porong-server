package com.poro.empire.gui;

import com.poro.empire.growth.island.IslandTerritoryState;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 영지 설정 GUI (36슬롯, 4×9).
 *
 * row0: 설정 버튼 9개 (영지명·방문·채굴·농사·날씨·시간·작물보호·물보호·권한)
 * row1: 자동입금(slot9) + gray×7 + 시설현황(slot17)
 * row2: 멤버 슬롯 8개 (slot18~25)
 * row3: 뒤로(slot27) + gray×8
 */
public final class TerritorySettingsGui {
    private TerritorySettingsGui() {}

    public static final int SLOT_NAME            = 0;
    public static final int SLOT_VISIT           = 1;
    public static final int SLOT_VISITOR_MINE    = 2;
    public static final int SLOT_VISITOR_FARM    = 3;
    public static final int SLOT_WEATHER         = 4;
    public static final int SLOT_TIME            = 5;
    public static final int SLOT_CROP_PROTECT    = 6;
    public static final int SLOT_WATER_PROTECT   = 7;
    public static final int SLOT_PERMISSION      = 8;
    public static final int SLOT_FACILITY        = 17;
    public static final int SLOT_MEMBER_START    = 18;
    public static final int SLOT_MEMBER_END      = 25;
    public static final int SLOT_BACK            = 27;

    /** weatherState: 0=서버기본, 1=맑음, 2=비 */
    public static final int WEATHER_DEFAULT = 0;
    public static final int WEATHER_CLEAR   = 1;
    public static final int WEATHER_RAIN    = 2;

    /** timeState: 0=서버기본, 1=낮, 2=밤 */
    public static final int TIME_DEFAULT = 0;
    public static final int TIME_DAY     = 1;
    public static final int TIME_NIGHT   = 2;

    public static void open(Player player, IslandTerritoryState territory) {
        open(player, territory, WEATHER_DEFAULT, TIME_DEFAULT);
    }

    public static void open(Player player, IslandTerritoryState territory,
                            int weatherState, int timeState) {
        Inventory inv = Bukkit.createInventory(null, 36, GuiTitles.TERRITORY_SETTINGS);
        ItemStack gray = MainHubGui.icon(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < 36; i++) inv.setItem(i, gray);

        // row0 — 설정 버튼
        inv.setItem(SLOT_NAME, MainHubGui.icon(Material.NAME_TAG, "§f영지명 변경",
                List.of("§7현재: §e" + territory.islandName(), "§8▶ 클릭하여 변경")));
        inv.setItem(SLOT_VISIT,         visitIcon(territory));
        inv.setItem(SLOT_VISITOR_MINE,  toggleIcon(territory, IslandTerritoryState.CONV_VISITOR_MINE,
                Material.IRON_PICKAXE, "방문자 채굴", "방문자가 영지에서 채굴할 수 있습니다."));
        inv.setItem(SLOT_VISITOR_FARM,  toggleIcon(territory, IslandTerritoryState.CONV_VISITOR_FARM,
                Material.WHEAT,         "방문자 농사", "방문자가 영지에서 농사지을 수 있습니다."));
        inv.setItem(SLOT_WEATHER, weatherIcon(weatherState));
        inv.setItem(SLOT_TIME,    timeIcon(timeState));
        inv.setItem(SLOT_CROP_PROTECT,  toggleIcon(territory, IslandTerritoryState.CONV_CROP_PROTECT,
                Material.WHEAT,         "농작물 보호", "다 자라지 않은 작물 파괴 방지."));
        inv.setItem(SLOT_WATER_PROTECT, toggleIcon(territory, IslandTerritoryState.CONV_WATER_PROTECT,
                Material.WATER_BUCKET,  "물 파괴 보호", "방문자가 영지 내 물을 제거할 수 없습니다."));
        inv.setItem(SLOT_PERMISSION, MainHubGui.icon(Material.BOOK, "§f권한 설정",
                List.of("§7부영주·영지민·방문자 등급별 권한 편집", "§8▶ 클릭하여 열기")));

        // row1 — 자동입금 상태 표시 + 시설현황
        inv.setItem(9,  autoDepositIcon(territory));
        inv.setItem(17, MainHubGui.icon(Material.PISTON, "§f시설 현황",
                List.of("§7약초 재배기 현황 확인", "§8▶ 클릭하여 열기")));

        // row2 — 멤버 슬롯 (영지 멤버 표시, 빈 슬롯은 초대 가능)
        java.util.List<Map.Entry<UUID, IslandTerritoryState.Role>> members = territory.memberList();
        for (int i = 0; i < (SLOT_MEMBER_END - SLOT_MEMBER_START + 1); i++) {
            int slot = SLOT_MEMBER_START + i;
            if (i < members.size()) {
                inv.setItem(slot, memberIcon(territory, members.get(i)));
            } else {
                inv.setItem(slot, inviteIcon());
            }
        }

        // row3 — 네비게이션
        inv.setItem(SLOT_BACK, MainHubGui.icon(Material.ARROW, "§7뒤로", List.of("§7영지 관리")));

        player.openInventory(inv);
    }

    public static ItemStack weatherIcon(int state) {
        return switch (state) {
            case WEATHER_CLEAR -> MainHubGui.icon(Material.SUNFLOWER, "§a날씨 설정 §f[맑음 고정]",
                    List.of("§7클릭 → §b비 고정"));
            case WEATHER_RAIN  -> MainHubGui.icon(Material.WATER_BUCKET, "§b날씨 설정 §f[비 고정]",
                    List.of("§7클릭 → §7서버 기본"));
            default            -> MainHubGui.icon(Material.CLOCK, "§7날씨 설정 §f[서버 기본]",
                    List.of("§7클릭 → §a맑음 고정"));
        };
    }

    public static ItemStack timeIcon(int state) {
        return switch (state) {
            case TIME_DAY   -> MainHubGui.icon(Material.SUNFLOWER, "§e시간 설정 §f[낮 고정]",
                    List.of("§7클릭 → §9밤 고정"));
            case TIME_NIGHT -> MainHubGui.icon(Material.INK_SAC,   "§9시간 설정 §f[밤 고정]",
                    List.of("§7클릭 → §7서버 기본"));
            default         -> MainHubGui.icon(Material.CLOCK,     "§7시간 설정 §f[서버 기본]",
                    List.of("§7클릭 → §e낮 고정"));
        };
    }

    public static ItemStack memberIcon(IslandTerritoryState territory, Map.Entry<UUID, IslandTerritoryState.Role> entry) {
        UUID uuid = entry.getKey();
        IslandTerritoryState.Role role = entry.getValue();
        String name = territory.memberName(uuid);
        if (name == null) {
            OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
            name = op.getName() != null ? op.getName() : uuid.toString().substring(0, 8);
        }
        String roleColor = switch (role) {
            case VICE_LORD -> "§b";
            case RESIDENT  -> "§e";
            case VISITOR   -> "§7";
        };
        String roleLabel = switch (role) {
            case VICE_LORD -> "부영주";
            case RESIDENT  -> "영지민";
            case VISITOR   -> "방문자";
        };

        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        meta.setOwningPlayer(Bukkit.getOfflinePlayer(uuid));
        meta.displayName(Component.text("§f" + name));
        meta.lore(List.of(
                Component.text("§7등급: " + roleColor + roleLabel),
                Component.text("§7──────────────"),
                Component.text("§7좌클릭  §f등급 변경"),
                Component.text("§7우클릭  §c강퇴")
        ));
        head.setItemMeta(meta);
        return head;
    }

    public static ItemStack inviteIcon() {
        return MainHubGui.icon(Material.WHITE_STAINED_GLASS_PANE, "§7플레이어 초대",
                List.of("§7──────────────",
                        "§7클릭 후 플레이어 이름 입력",
                        "§8(Anvil GUI)"));
    }

    public static ItemStack visitIcon(IslandTerritoryState t) {
        return switch (t.visitMode()) {
            case PUBLIC -> MainHubGui.icon(Material.LIME_STAINED_GLASS_PANE,
                    "§a방문 설정: §f전체 공개",
                    List.of("§7모든 플레이어가 방문 가능", "§7클릭 → §e친구만"));
            case FRIENDS -> MainHubGui.icon(Material.YELLOW_STAINED_GLASS_PANE,
                    "§e방문 설정: §f친구만",
                    List.of("§7친구 목록의 플레이어만 방문 가능", "§7클릭 → §c비공개"));
            case PRIVATE -> MainHubGui.icon(Material.RED_STAINED_GLASS_PANE,
                    "§c방문 설정: §f비공개",
                    List.of("§7소유자/공동관리자만 입장", "§7클릭 → §a전체 공개"));
        };
    }

    public static ItemStack toggleIcon(IslandTerritoryState t, int bit, Material mat,
                                       String name, String description) {
        boolean on = t.hasConvenience(bit);
        return on
                ? MainHubGui.icon(mat, "§a" + name + " §2[허용]",
                        List.of("§7" + description, "§7클릭 → §c비허용"))
                : MainHubGui.icon(mat, "§7" + name + " §8[비허용]",
                        List.of("§7" + description, "§7클릭 → §a허용"));
    }

    private static ItemStack autoDepositIcon(IslandTerritoryState t) {
        boolean on = t.hasConvenience(IslandTerritoryState.CONV_AUTO_DEPOSIT);
        return on
                ? MainHubGui.icon(Material.HOPPER, "§a자동 입금 §2[활성화]",
                        List.of("§7채굴·수확 산출물이 창고에 자동 적재", "§8영지 상태 GUI에서 설정"))
                : MainHubGui.icon(Material.HOPPER, "§7자동 입금 §8[비활성화]",
                        List.of("§7채굴·수확 산출물이 인벤토리로 이동", "§8영지 상태 GUI에서 설정"));
    }

    private static ItemStack stub(Material mat, String name) {
        return MainHubGui.icon(mat, name, List.of("§8준비 중"));
    }
}
