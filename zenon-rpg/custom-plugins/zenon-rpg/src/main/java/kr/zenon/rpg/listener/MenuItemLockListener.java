package kr.zenon.rpg.listener;

import kr.zenon.rpg.init.ClassInitService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.PlayerInventory;

/**
 * 메뉴 아이템을 핫바 9번(slot index 8)에 고정한다.
 * 클릭·숫자키 교환·shift-click·드래그·드롭·F-스왑 등 메뉴 아이템을 옮기거나
 * 슬롯 8에 다른 아이템을 넣으려는 모든 경로를 차단한다. (식별은 PDC 태그 기반)
 */
public final class MenuItemLockListener implements Listener {

    private static final int SLOT = ClassInitService.MENU_ITEM_SLOT; // 8

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        boolean block =
                // 1) 슬롯 8(플레이어 인벤 핫바) 직접 클릭 — 꺼내기/넣기 모두 차단(예약 슬롯)
                (event.getClickedInventory() instanceof PlayerInventory && event.getSlot() == SLOT)
                // 2) 숫자키 9로 슬롯 8과 교환
                || (event.getClick() == ClickType.NUMBER_KEY && event.getHotbarButton() == SLOT)
                // 3) 메뉴 아이템 자체를 옮기려는 클릭(shift-click·집기·커서 배치, 크리에이티브 클론 포함)
                || ClassInitService.isMenuItem(event.getCurrentItem())
                || ClassInitService.isMenuItem(event.getCursor());
        if (block) {
            event.setCancelled(true);
            // 클라 리싱크 — 취소 후 남는 고스트/크리에이티브 복사 방지
            player.updateInventory();
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        boolean block = ClassInitService.isMenuItem(event.getOldCursor());
        if (!block) {
            // 드래그가 플레이어 핫바 슬롯 8을 포함하면 차단(다른 아이템 배치 금지)
            for (int raw : event.getRawSlots()) {
                if (event.getView().getInventory(raw) instanceof PlayerInventory
                        && event.getView().convertSlot(raw) == SLOT) {
                    block = true;
                    break;
                }
            }
        }
        if (block) {
            event.setCancelled(true);
            player.updateInventory();
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        if (ClassInitService.isMenuItem(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onSwapHand(PlayerSwapHandItemsEvent event) {
        // F키 — 메뉴 아이템을 오프핸드로 옮기는 것 차단
        if (ClassInitService.isMenuItem(event.getMainHandItem())
                || ClassInitService.isMenuItem(event.getOffHandItem())) {
            event.setCancelled(true);
        }
    }
}
