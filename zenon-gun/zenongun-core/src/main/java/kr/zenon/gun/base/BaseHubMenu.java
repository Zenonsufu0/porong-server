package kr.zenon.gun.base;

import kr.zenon.gun.core.CoreManager;
import kr.zenon.gun.registry.ZenonGunMenus;
import kr.zenon.gun.shop.ShopMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkHooks;

/**
 * 거점 허브 메뉴 (M-Core 「코어 우클릭 GUI」 — economy 「코어 우클릭/Shift+F」).
 *
 * 코어 우클릭 시 가장 먼저 뜨는 chest형 진입 메뉴. 슬롯은 없고(아이템 보관 X),
 * 화면({@link kr.zenon.gun.client.BaseHubScreen})의 아이콘 클릭 → {@link #clickMenuButton}
 * 으로 각 기능 화면(상점/기지 관리/…)을 연다. 우클릭한 코어 좌표({@link #corePos})를 들고 다닌다.
 *
 * 버튼 ID = 항목 인덱스({@link #ITEM_SHOP} 등). Screen의 셀 배치와 동일 순서.
 *
 * ※ 배낭은 별도 컨테이너가 아니라 바닐라 플레이어 인벤 자체(설계 「인벤=배낭」)이므로
 *   허브 항목에서 제외. 정식 통합은 M-Inv. 프로토타입 확인은 {@code /zenongun invtest}.
 */
public class BaseHubMenu extends AbstractContainerMenu {

    public static final int ITEM_SHOP = 0; // 상점
    public static final int ITEM_BASE = 1; // 기지 관리
    public static final int ITEM_INFO = 2; // 정보 (후속)
    public static final int ITEM_COUNT = 3;

    private final BlockPos corePos;

    /** 클라 팩토리(IForgeMenuType). buf = 코어 좌표. */
    public BaseHubMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, buf.readBlockPos());
    }

    public BaseHubMenu(int id, Inventory inv, BlockPos corePos) {
        super(ZenonGunMenus.BASE_HUB.get(), id);
        this.corePos = corePos;
        // 슬롯 없음 — 허브는 클릭 진입 전용.
    }

    @Override
    public boolean clickMenuButton(Player player, int buttonId) {
        if (!(player instanceof ServerPlayer sp)) return false;
        switch (buttonId) {
            case ITEM_SHOP -> NetworkHooks.openScreen(sp, new SimpleMenuProvider(
                    (id, inv, p) -> new ShopMenu(id, inv), Component.literal("상점")));
            case ITEM_BASE -> openBaseManage(sp);
            case ITEM_INFO -> sp.displayClientMessage(
                    Component.literal("§7정보 탭은 준비 중입니다"), true);
            default -> {
                return false;
            }
        }
        return true;
    }

    private void openBaseManage(ServerPlayer sp) {
        int level = currentLevel(sp);
        if (level < 0) {
            sp.displayClientMessage(Component.literal("§c코어 데이터를 찾을 수 없음"), true);
            return;
        }
        NetworkHooks.openScreen(sp, new SimpleMenuProvider(
                        (id, inv, p) -> new BaseManageMenu(id, inv, corePos, level),
                        Component.literal("기지 관리")),
                buf -> {
                    buf.writeBlockPos(corePos);
                    buf.writeInt(level);
                });
    }

    /** 코어 현재 레벨(없으면 -1). */
    private int currentLevel(ServerPlayer sp) {
        ServerLevel sl = sp.serverLevel();
        CoreManager.CoreData d = CoreManager.get(sl).get(corePos);
        return d == null ? -1 : d.level();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY; // 슬롯 없음
    }

    @Override
    public boolean stillValid(Player player) {
        return true; // 거점 GUI(코어 우클릭). 거리/영역 검사는 후속.
    }
}
