package kr.zenon.gun.inv;

import kr.zenon.gun.registry.ZenonGunMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * 인벤 GUI 프로토타입 메뉴 (M0 최대 기술 리스크 검증 — launch_plan §1-6).
 *
 * 검증 대상 3대 유리 레이어(survival.md)를 **하나의 인벤 그리드**에 통합:
 *  1. 보안칸    — 죽어도 안 잃는 칸. **윗줄 오른쪽부터** 채움(회색 봉인). 무게/페널티 제외.
 *  2. 무게 잠김 — 무게 초과분(Σ 무게-1)을 **왼쪽부터** 잠금(푸른 유리). 보안칸 만나면 건너뛰어 다음 줄 왼쪽.
 *  3. 검사 가림 — 데스박스/POI 상자처럼 일반 아이템칸 내용물 가림(검사 모드 토글, Screen 처리).
 *
 * 실제 설계상 인벤=배낭=바닐라 플레이어 인벤(별도 컨테이너 아님). 프로토타입은
 * 단일 `SimpleContainer`로 유리 레이어·배치 규칙을 검증한다(바닐라 인벤 후킹은 정식 M-Inv).
 */
public class BackpackProtoMenu extends AbstractContainerMenu {

    public static final int BACKPACK_SIZE = 18; // 2줄 × 9 (배낭=인벤, 프로토타입)
    public static final int COLS = 9;
    public static final int SECURE_COUNT = 3;   // 보안칸 수(윗줄 오른쪽부터). 실제=코어 레벨 2~6.

    private final SimpleContainer backpack = new SimpleContainer(BACKPACK_SIZE);

    /** 클라이언트 팩토리(IForgeMenuType). buf는 미사용(상태는 슬롯 동기화). */
    public BackpackProtoMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv);
    }

    public BackpackProtoMenu(int id, Inventory inv) {
        super(ZenonGunMenus.BACKPACK_PROTO.get(), id);
        seedDemo();
        addBackpackSlots();
        addPlayerSlots(inv);
    }

    private void seedDemo() {
        // 일반칸(가운데): 무게는 커스텀 총·청사진만. 바닐라=무게1(자기 칸만).
        backpack.setItem(3, WeightUtil.setWeight(new ItemStack(Items.NETHERITE_BLOCK), 3)); // 커스텀 총
        backpack.setItem(4, WeightUtil.setWeight(new ItemStack(Items.PAPER), 2));           // 청사진
        backpack.setItem(5, new ItemStack(Items.BREAD, 8));   // 바닐라(무게1)
        // 보안칸(윗줄 오른쪽 6·7·8): 화폐·티켓 stand-in
        backpack.setItem(8, new ItemStack(Items.GOLD_INGOT, 16));
        backpack.setItem(7, new ItemStack(Items.PAPER));
        // → 무게초과 = (3-1)+(2-1) = 3칸 → 왼쪽부터 3칸(슬롯 0·1·2) 잠김 유리.
    }

    private void addBackpackSlots() {
        for (int i = 0; i < BACKPACK_SIZE; i++) {
            int row = i / COLS, col = i % COLS;
            addSlot(new ProtoSlot(backpack, i, 8 + col * 18, 20 + row * 18));
        }
    }

    private void addPlayerSlots(Inventory inv) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 72 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inv, col, 8 + col * 18, 132));
        }
    }

    // --- 유리 레이어 판정 (Screen 렌더 + mayPlace에 사용) ---

    public boolean isBackpack(Slot slot) {
        return slot.container == backpack;
    }

    /** 보안칸 = 윗줄(0~8) 오른쪽 SECURE_COUNT개. */
    private boolean isSecurityPos(int pos) {
        return pos < COLS && pos >= COLS - SECURE_COUNT;
    }

    public boolean isSecure(Slot slot) {
        return isBackpack(slot) && isSecurityPos(slot.getContainerSlot());
    }

    /** 무게 초과로 잠가야 할 칸 수 = Σ(무게-1). 보안칸 아이템은 제외(무게 부담 없음). */
    private int lockedCount() {
        int extra = 0;
        for (int i = 0; i < BACKPACK_SIZE; i++) {
            if (isSecurityPos(i)) continue;
            int w = WeightUtil.weightOf(backpack.getItem(i));
            if (w > 1) extra += w - 1;
        }
        return Math.min(extra, BACKPACK_SIZE - SECURE_COUNT);
    }

    /**
     * 무게 점유로 막힌 칸인지. 보안칸은 제외(항상 안전).
     * 잠김은 **비보안 칸을 왼쪽(0)부터** 세어 앞 K개 = 보안칸을 건너뛰며 다음 줄로 넘어감
     * (survival.md 「무게=왼쪽부터, 보안=오른쪽부터, 만나면 다음 줄 왼쪽」).
     */
    public boolean isBlocked(Slot slot) {
        if (!isBackpack(slot)) return false;
        int pos = slot.getContainerSlot();
        if (isSecurityPos(pos)) return false;
        int rank = 0; // 이 칸이 비보안 칸 중 몇 번째(0-based)
        for (int i = 0; i < pos; i++) {
            if (!isSecurityPos(i)) rank++;
        }
        return rank < lockedCount();
    }

    /** 무게로 막힌 칸은 아이템을 받지 않는다(보안칸은 항상 배치 가능). */
    private class ProtoSlot extends Slot {
        ProtoSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return !isBlocked(this);
        }
    }

    // --- 표준 메뉴 동작 ---

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            if (index < BACKPACK_SIZE) {
                if (!moveItemStackTo(stack, BACKPACK_SIZE, slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!moveItemStackTo(stack, 0, BACKPACK_SIZE, false)) {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return true; // 프로토타입(커맨드/자동 오픈) — 거리/소유 검사 생략
    }
}
