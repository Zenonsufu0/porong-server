package kr.zenon.gun.base;

import kr.zenon.gun.core.CoreLevels;
import kr.zenon.gun.core.CoreManager;
import kr.zenon.gun.registry.ZenonGunMenus;
import kr.zenon.gun.shop.CurrencyUtil;
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

import java.util.UUID;

/**
 * 기지 관리 메뉴 (M-Core 2단계 — base_raid 「코어 레벨」). 코어 레벨 정보 표시 + 업그레이드.
 *
 * 표시 데이터는 {@code level}만 클라로 전달하고, 내구·보안칸·도구효율·비용은 클라/서버 공용
 * 순수 계산({@link CoreLevels})으로 파생한다. 업그레이드(버튼0) = 서버에서 소유자·재료·화폐
 * 검증·차감 후 레벨업, 갱신된 레벨로 화면 재오픈.
 */
public class BaseManageMenu extends AbstractContainerMenu {

    public static final int BTN_UPGRADE = 0;

    private final BlockPos corePos;
    private final int level; // 이 화면 생성 시점 레벨(표시용)

    /** 클라 팩토리(IForgeMenuType). buf = 코어 좌표 + 레벨. */
    public BaseManageMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, buf.readBlockPos(), buf.readInt());
    }

    public BaseManageMenu(int id, Inventory inv, BlockPos corePos, int level) {
        super(ZenonGunMenus.BASE_MANAGE.get(), id);
        this.corePos = corePos;
        this.level = level;
    }

    public int level() {
        return level;
    }

    public BlockPos corePos() {
        return corePos;
    }

    @Override
    public boolean clickMenuButton(Player player, int buttonId) {
        if (buttonId != BTN_UPGRADE) return false;
        if (!(player instanceof ServerPlayer sp)) return false;

        ServerLevel sl = sp.serverLevel();
        CoreManager mgr = CoreManager.get(sl);
        CoreManager.CoreData d = mgr.get(corePos);
        if (d == null) {
            msg(sp, "§c코어 데이터를 찾을 수 없음");
            return false;
        }
        // 소유자만(연합 OPaC 예외는 후속).
        UUID owner = d.owner();
        if (owner != null && !owner.equals(sp.getUUID())) {
            msg(sp, "§c소유자만 업그레이드할 수 있음");
            return false;
        }
        int lv = d.level();
        if (lv >= CoreLevels.MAX) {
            msg(sp, "§e이미 최고 레벨(LV" + CoreLevels.MAX + ")");
            return false;
        }
        CoreLevels.UpgradeCost cost = CoreLevels.upgradeCost(lv);
        if (cost == null) {
            msg(sp, "§e업그레이드 불가");
            return false;
        }
        // 검증(차감 전).
        if (CurrencyUtil.balance(sp) < cost.coins()) {
            msg(sp, "§c코인 부족 (필요 " + cost.coins() + " / 보유 " + CurrencyUtil.balance(sp) + ")");
            return false;
        }
        for (CoreLevels.Ingredient ing : cost.ingredients()) {
            if (countItem(sp, ing) < ing.count()) {
                msg(sp, "§c재료 부족: " + new ItemStack(ing.item()).getHoverName().getString()
                        + " (필요 " + ing.count() + ")");
                return false;
            }
        }
        // 차감 → 레벨업.
        CurrencyUtil.take(sp, cost.coins());
        for (CoreLevels.Ingredient ing : cost.ingredients()) {
            takeItem(sp, ing);
        }
        int next = lv + 1;
        mgr.setLevel(corePos, next);
        msg(sp, "§a기지 업그레이드 완료 — LV" + next
                + " (블록내구 " + CoreLevels.blockDurability(next)
                + " / 보안칸 " + CoreLevels.securitySlots(next) + ")");
        // 갱신된 레벨로 재오픈.
        NetworkHooks.openScreen(sp, new SimpleMenuProvider(
                        (id, inv, p) -> new BaseManageMenu(id, inv, corePos, next),
                        Component.literal("기지 관리")),
                buf -> {
                    buf.writeBlockPos(corePos);
                    buf.writeInt(next);
                });
        return true;
    }

    private static int countItem(Player player, CoreLevels.Ingredient ing) {
        int total = 0;
        var items = player.getInventory().items;
        for (ItemStack s : items) {
            if (s.is(ing.item())) total += s.getCount();
        }
        return total;
    }

    private static void takeItem(Player player, CoreLevels.Ingredient ing) {
        int remaining = ing.count();
        var items = player.getInventory().items;
        for (int i = 0; i < items.size() && remaining > 0; i++) {
            ItemStack s = items.get(i);
            if (s.is(ing.item())) {
                int take = Math.min(remaining, s.getCount());
                s.shrink(take);
                remaining -= take;
            }
        }
    }

    private static void msg(Player player, String text) {
        player.displayClientMessage(Component.literal(text), true);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY; // 슬롯 없음
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}
