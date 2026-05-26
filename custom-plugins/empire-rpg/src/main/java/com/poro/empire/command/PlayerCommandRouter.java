package com.poro.empire.command;

import com.poro.empire.growth.island.IslandStorage;
import com.poro.empire.growth.island.IslandStorageStore;
import com.poro.empire.growth.island.IslandTerritoryState;
import com.poro.empire.growth.island.IslandTerritoryStateStore;
import com.poro.empire.gui.StorageGui;
import com.poro.empire.gui.TerritoryStatusGui;
import com.poro.empire.gui.WorkshopGui;
import com.poro.empire.listener.AuctionGuiListener;
import com.poro.empire.listener.GrowthGuiListener;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * 플레이어용 한글 단축 커맨드 라우터.
 * 각 커맨드는 해당 GUI를 바로 열어줌.
 * GUI 클래스가 미구현인 경우 "준비 중" 메시지 표시.
 */
public class PlayerCommandRouter implements CommandExecutor {

    private static final String PREFIX = "§8[§e포로§8] ";
    private static final String NO_PERM = PREFIX + "§c사용할 수 없는 명령어입니다.";

    private final IslandStorageStore        storageStore;
    private final IslandTerritoryStateStore territoryStore;
    private final AuctionGuiListener        auctionGuiListener;
    private final GrowthGuiListener         growthGuiListener;

    public PlayerCommandRouter(IslandStorageStore storageStore,
                               IslandTerritoryStateStore territoryStore,
                               AuctionGuiListener auctionGuiListener,
                               GrowthGuiListener growthGuiListener) {
        this.storageStore       = storageStore;
        this.territoryStore     = territoryStore;
        this.auctionGuiListener = auctionGuiListener;
        this.growthGuiListener  = growthGuiListener;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(PREFIX + "§c플레이어만 사용할 수 있습니다.");
            return true;
        }
        if (!player.hasPermission("empire.use")) {
            player.sendMessage(NO_PERM);
            return true;
        }

        String cmd = command.getName().toLowerCase(java.util.Locale.ROOT);
        switch (cmd) {
            // ── 영지 계열 ──────────────────────────────────────────
            case "창고"     -> openStorage(player);
            case "영지상태"  -> openTerritoryStatus(player);
            case "공방"     -> WorkshopGui.open(player, WorkshopGui.WorkshopTab.ESTATE);
            // 이하 GUI 클래스 구현 후 연동
            case "메뉴"     -> stub(player, "메인 메뉴");
            case "장비"     -> growthGuiListener.openEquipHub(player);
            case "강화"     -> growthGuiListener.openEnhancement(player);
            case "잠재"     -> growthGuiListener.openPotential(player);
            case "각인"     -> stub(player, "각인");
            case "캐릭터"   -> stub(player, "캐릭터");
            case "전승"     -> growthGuiListener.openHeirloom(player);
            case "영지"     -> stub(player, "영지 메뉴");
            case "영지이동"  -> stub(player, "영지이동");
            case "작물"     -> stub(player, "작물 관리");
            case "상점"     -> stub(player, "상점");
            case "경매장"   -> auctionGuiListener.openMain(player);
            case "영지설정"  -> stub(player, "영지 설정");
            // ── 보스 계열 ──────────────────────────────────────────
            case "보스"     -> stub(player, "보스 메뉴");
            case "파티"     -> stub(player, "파티 생성");
            case "파티목록"  -> stub(player, "파티 목록");
            case "보스정보"  -> stub(player, "보스 정보");
            case "클리어"   -> stub(player, "클리어 기록");
            // ── 필드 ──────────────────────────────────────────────
            case "필드"     -> stub(player, "필드 이동");
            default          -> stub(player, label);
        }
        return true;
    }

    private void openStorage(Player player) {
        IslandStorage storage = storageStore.getOrCreate(player.getUniqueId());
        StorageGui.open(player, storage, 0);
    }

    private void openTerritoryStatus(Player player) {
        IslandTerritoryState state = territoryStore.getOrCreate(player.getUniqueId());
        IslandStorage storage = storageStore.getOrCreate(player.getUniqueId());
        TerritoryStatusGui.open(player, state, storage);
    }

    private void stub(Player player, String feature) {
        player.sendMessage(PREFIX + "§e" + feature + " §7기능 준비 중입니다.");
    }
}
