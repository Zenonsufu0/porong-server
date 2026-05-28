package com.poro.empire.listener;

import com.poro.empire.combat.weapon.WeaponTypeResolver;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import static org.bukkit.event.inventory.InventoryType.SlotType;

/**
 * 바닐라 방어구 장착 차단.
 * empire_rpg PDC 태그 없는 방어구를 장착 슬롯에 놓거나
 * shift-click으로 장착하려 할 때 취소한다.
 */
public final class EmpireItemGuardListener implements Listener {

    private static final String DENY_MSG = "§c[장비] 바닐라 방어구는 장착할 수 없습니다. RPG 장비는 영지 GUI를 이용하세요.";

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        // Case 1: 방어구 슬롯 직접 클릭 — 배치·탈착 모두 차단 (방어구 빼기 불가)
        // top inventory 무관하게 SlotType.ARMOR 기준으로 검사한다.
        if (event.getSlotType() == SlotType.ARMOR) {
            event.setCancelled(true);
            ItemStack cursor = event.getCursor();
            if (cursor != null && !cursor.getType().isAir() && !isTagged(cursor)) {
                player.sendMessage(DENY_MSG);
            }
            return;
        }

        // Case 2: shift-click 자동 장착 — 외부 인벤토리(상자·창고 등)에서도 차단
        if (event.isShiftClick()) {
            ItemStack item = event.getCurrentItem();
            if (item != null && !item.getType().isAir()
                    && isArmorMaterial(item)
                    && !isTagged(item)) {
                event.setCancelled(true);
                player.sendMessage(DENY_MSG);
            }
        }
    }

    private static boolean isArmorMaterial(ItemStack item) {
        String name = item.getType().name();
        return name.endsWith("_HELMET")
                || name.endsWith("_CHESTPLATE")
                || name.endsWith("_LEGGINGS")
                || name.endsWith("_BOOTS")
                || name.equals("TURTLE_HELMET")
                || name.equals("ELYTRA");
    }

    private static boolean isTagged(ItemStack item) {
        if (!item.hasItemMeta()) return false;
        var pdc = item.getItemMeta().getPersistentDataContainer();
        // empire_rpg:weapon_type — 현재 RPG 아이템 공통 태그
        // 추후 RPG 방어구 물리 아이템 추가 시 empire_rpg:item_tag 도 여기에 추가
        return pdc.has(WeaponTypeResolver.WEAPON_TYPE_KEY, PersistentDataType.STRING);
    }
}
