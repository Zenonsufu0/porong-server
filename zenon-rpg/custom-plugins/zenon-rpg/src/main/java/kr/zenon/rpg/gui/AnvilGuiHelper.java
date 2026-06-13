package kr.zenon.rpg.gui;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MenuType;
import org.bukkit.inventory.view.AnvilView;

/**
 * Paper 1.21+ 호환 Anvil GUI 오픈 헬퍼.
 *
 * <p>{@code server.createInventory(null, InventoryType.ANVIL, ...)}로 만든 인벤토리는
 * Paper 1.21.x에서 {@code CraftInventoryCustom}(기능 없는 껍데기)로 반환되어
 * {@code (AnvilInventory)} 캐스팅 시 {@link ClassCastException}이 난다.
 * 반드시 {@link MenuType#ANVIL}로 실제 {@link AnvilView}를 생성해야
 * rename 텍스트 추적 등 anvil 고유 동작이 살아난다.</p>
 */
public final class AnvilGuiHelper {
    private AnvilGuiHelper() {}

    /** 제목과 slot 0 초기 아이템으로 anvil GUI를 열고 view를 돌려준다. */
    public static AnvilView open(Player player, Component title, ItemStack slot0) {
        AnvilView view = MenuType.ANVIL.create(player, title);
        view.getTopInventory().setItem(0, slot0);
        player.openInventory(view);
        return view;
    }
}
