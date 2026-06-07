package kr.poro.poromoncore.menu;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/**
 * 범용 서버 권위 메뉴 ScreenHandler (menu_design.md §5, 6행×9열 읽기전용).
 * 모든 아이템 이동/추출을 차단하고 디스플레이 슬롯 클릭만 콜백으로 라우팅한다.
 * 안티-듀프: super(이동 로직) 미호출 + syncState()로 클라 예측 되돌림.
 * 메뉴 전환은 show()로 재오픈 없이 내용 교체(커서 점프 방지).
 */
public class ServerMenuHandler extends GenericContainerScreenHandler {
    public static final int ROWS = 6;
    public static final int DISPLAY_SIZE = ROWS * 9; // 54

    /** (player, slot, button, shift) — button 0=좌클릭, 1=우클릭, shift=쉬프트 클릭. */
    @FunctionalInterface
    public interface ClickHandler {
        void onClick(ServerPlayerEntity player, int slot, int button, boolean shift);
    }

    /** 진열 인벤토리를 채우는 함수(메뉴별 구성). */
    @FunctionalInterface
    public interface Populator {
        void populate(Inventory display);
    }

    private final ServerPlayerEntity owner;
    private final Inventory display;
    private ClickHandler clickHandler;

    public ServerMenuHandler(int syncId, PlayerInventory playerInv, Inventory display,
                             ServerPlayerEntity owner, ClickHandler clickHandler) {
        super(ScreenHandlerType.GENERIC_9X6, syncId, playerInv, display, ROWS);
        this.owner = owner;
        this.clickHandler = clickHandler;
        this.display = display;
    }

    public Inventory getDisplay() {
        return display;
    }

    public void setClickHandler(ClickHandler handler) {
        this.clickHandler = handler;
    }

    /**
     * 메뉴를 표시한다. 이미 ServerMenuHandler가 열려 있으면 재오픈 없이 내용/콜백만 교체
     * (커서 점프 없음). 없으면 새 화면을 연다(title 사용). 거래 후 갱신도 이 경로.
     */
    public static void show(ServerPlayerEntity player, Text title, Populator populator, ClickHandler handler) {
        if (player.currentScreenHandler instanceof ServerMenuHandler smh) {
            populator.populate(smh.getDisplay());
            smh.setClickHandler(handler);
            smh.sendContentUpdates();
            return;
        }
        SimpleInventory inv = new SimpleInventory(DISPLAY_SIZE);
        populator.populate(inv);
        player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                (syncId, playerInv, p) -> new ServerMenuHandler(syncId, playerInv, inv, (ServerPlayerEntity) p, handler),
                title));
    }

    @Override
    public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {
        if (slotIndex >= 0 && slotIndex < DISPLAY_SIZE && clickHandler != null) {
            boolean shift = actionType == SlotActionType.QUICK_MOVE;
            clickHandler.onClick(owner, slotIndex, button, shift);
        }
        this.syncState(); // 서버 상태 불변 + 클라 예측 강제 동기화
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canInsertIntoSlot(ItemStack stack, Slot slot) {
        return false;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return true;
    }
}
