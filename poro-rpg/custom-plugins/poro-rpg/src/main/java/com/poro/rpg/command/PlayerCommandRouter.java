package com.poro.rpg.command;

import com.poro.rpg.combat.CombatStateService;
import com.poro.rpg.growth.island.IslandStorage;
import com.poro.rpg.growth.island.IslandStorageStore;
import com.poro.rpg.growth.island.IslandTerritoryState;
import com.poro.rpg.growth.island.IslandTerritoryStateStore;
import com.poro.rpg.gui.MainHubGui;
import com.poro.rpg.gui.StorageGui;
import com.poro.rpg.gui.TerritoryHubGui;
import com.poro.rpg.gui.TerritoryMoveGui;
import com.poro.rpg.gui.TerritorySettingsGui;
import com.poro.rpg.gui.TerritoryStatusGui;
import com.poro.rpg.gui.WorkshopGui;
import com.poro.rpg.listener.AuctionGuiListener;
import com.poro.rpg.listener.BossHubListener;
import com.poro.rpg.listener.FieldHubListener;
import com.poro.rpg.listener.GrowthGuiListener;
import com.poro.rpg.listener.PvpHubListener;
import com.poro.rpg.listener.ShopGuiListener;
import com.poro.rpg.listener.TerritorySettingsGuiListener;
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
    private final FieldHubListener          fieldHubListener;
    private final BossHubListener           bossHubListener;
    private final ShopGuiListener           shopGuiListener;
    private final CombatStateService        combatStateService;
    private final TerritorySettingsGuiListener territorySettingsGuiListener;
    private final PvpHubListener            pvpHubListener;

    public PlayerCommandRouter(IslandStorageStore storageStore,
                               IslandTerritoryStateStore territoryStore,
                               AuctionGuiListener auctionGuiListener,
                               GrowthGuiListener growthGuiListener,
                               FieldHubListener fieldHubListener,
                               BossHubListener bossHubListener,
                               ShopGuiListener shopGuiListener,
                               CombatStateService combatStateService,
                               TerritorySettingsGuiListener territorySettingsGuiListener,
                               PvpHubListener pvpHubListener) {
        this.storageStore       = storageStore;
        this.territoryStore     = territoryStore;
        this.auctionGuiListener = auctionGuiListener;
        this.growthGuiListener  = growthGuiListener;
        this.fieldHubListener   = fieldHubListener;
        this.bossHubListener    = bossHubListener;
        this.shopGuiListener    = shopGuiListener;
        this.combatStateService = combatStateService;
        this.territorySettingsGuiListener = territorySettingsGuiListener;
        this.pvpHubListener     = pvpHubListener;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(PREFIX + "§c플레이어만 사용할 수 있습니다.");
            return true;
        }
        if (!player.hasPermission("poro.use")) {
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
            case "메뉴"     -> MainHubGui.open(player);
            case "장비"     -> growthGuiListener.openEquipHub(player);
            case "강화"     -> growthGuiListener.openEnhancement(player);
            case "잠재"     -> growthGuiListener.openPotential(player);
            case "각인"     -> growthGuiListener.openEngraving(player);
            case "캐릭터"   -> stub(player, "캐릭터");
            case "전승"     -> growthGuiListener.openHeirloom(player);
            case "영지"     -> TerritoryHubGui.open(player);
            case "영지이동"  -> openTerritoryMove(player);
            case "작물"     -> stub(player, "작물 관리");
            case "상점"     -> shopGuiListener.openShop(player);
            case "경매장"   -> handleAuction(player, args);
            case "영지설정"  -> openTerritorySettings(player);
            case "영지초대"  -> handleInviteResponse(player, args);
            case "대전"     -> pvpHubListener.openHub(player);
            case "대전랭킹"  -> pvpHubListener.openRanking(player);
            case "친선"     -> handleFriendlyResponse(player, args);
            // ── 보스 계열 ──────────────────────────────────────────
            case "보스"     -> bossHubListener.openBossHub(player);
            case "파티"     -> bossHubListener.openPartyHub(player);
            case "파티목록"  -> bossHubListener.openPartyList(player);
            case "보스정보"  -> bossHubListener.openBossInfo(player);
            case "클리어"   -> bossHubListener.openClearRecords(player);
            // ── 필드 ──────────────────────────────────────────────
            case "필드"     -> fieldHubListener.openFieldHub(player);
            case "알람"     -> {
                boolean on = com.poro.rpg.listener.RewardNotify.toggle(player.getUniqueId());
                player.sendMessage(on ? "§a[알람] 획득 알림을 켰습니다." : "§7[알람] 획득 알림을 껐습니다.");
            }
            default          -> stub(player, label);
        }
        return true;
    }

    private void openStorage(Player player) {
        IslandStorage storage = storageStore.getOrCreate(player.getUniqueId());
        StorageGui.open(player, territoryStore.getOrCreate(player.getUniqueId()), storage, 0);
    }

    private void openTerritoryStatus(Player player) {
        IslandTerritoryState state = territoryStore.getOrCreate(player.getUniqueId());
        IslandStorage storage = storageStore.getOrCreate(player.getUniqueId());
        TerritoryStatusGui.open(player, state, storage);
    }

    private void openTerritoryMove(Player player) {
        IslandTerritoryState state = territoryStore.getOrCreate(player.getUniqueId());
        TerritoryMoveGui.open(player, state, territoryStore);
    }

    private void handleFriendlyResponse(Player player, String[] args) {
        if (args.length < 1) {
            player.sendMessage(PREFIX + "§7사용법: /친선 수락 §7or §f/친선 거절");
            return;
        }
        switch (args[0]) {
            case "수락", "accept", "y" -> pvpHubListener.friendlyService().accept(player);
            case "거절", "reject", "n" -> pvpHubListener.friendlyService().reject(player);
            default -> player.sendMessage(PREFIX + "§7사용법: /친선 수락 §7or §f/친선 거절");
        }
    }

    private void handleInviteResponse(Player player, String[] args) {
        if (args.length < 1) {
            player.sendMessage(PREFIX + "§7사용법: /영지초대 수락 §7or §f/영지초대 거절");
            return;
        }
        switch (args[0]) {
            case "수락", "accept", "y" -> territorySettingsGuiListener.acceptInvite(player);
            case "거절", "reject", "n" -> territorySettingsGuiListener.rejectInvite(player);
            default -> player.sendMessage(PREFIX + "§7사용법: /영지초대 수락 §7or §f/영지초대 거절");
        }
    }

    private void openTerritorySettings(Player player) {
        if (combatStateService.isInCombat(player.getUniqueId())) {
            player.sendMessage(PREFIX + "§c전투 중에는 영지 설정을 열 수 없습니다.");
            return;
        }
        IslandTerritoryState state = territoryStore.getOrCreate(player.getUniqueId());
        TerritorySettingsGui.open(player, state);
    }

    private void handleAuction(Player player, String[] args) {
        if (args.length >= 2 && "등록".equals(args[0])) {
            long price;
            try {
                price = Long.parseLong(args[1]);
            } catch (NumberFormatException e) {
                player.sendMessage(PREFIX + "§c사용법: /경매장 등록 <가격> [수량]");
                return;
            }
            long quantity = 0; // 0 = 전체
            if (args.length >= 3) {
                try {
                    quantity = Long.parseLong(args[2]);
                    if (quantity <= 0) quantity = 0;
                } catch (NumberFormatException e) {
                    player.sendMessage(PREFIX + "§c사용법: /경매장 등록 <가격> [수량]");
                    return;
                }
            }
            auctionGuiListener.handleDirectRegister(player, price, quantity);
        } else {
            auctionGuiListener.openMain(player);
        }
    }

    private void stub(Player player, String feature) {
        player.sendMessage(PREFIX + "§e" + feature + " §7기능 준비 중입니다.");
    }
}
