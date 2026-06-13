package kr.zenon.gun.client;

import kr.zenon.gun.base.BaseManageMenu;
import kr.zenon.gun.core.CoreLevels;
import kr.zenon.gun.shop.CurrencyUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * 기지 관리 화면 (M-Core 2단계). 코어 레벨 정보 + 업그레이드 버튼.
 *
 * 표시 값은 {@link CoreLevels}(순수 계산)로 메뉴의 level에서 파생. 재료/화폐 충분 여부는
 * 클라 인벤에서 계산해 색으로 표시(서버가 최종 검증). 버튼 → 서버 {@link BaseManageMenu#clickMenuButton}.
 */
public class BaseManageScreen extends AbstractContainerScreen<BaseManageMenu> {

    private Button upgradeButton;

    public BaseManageScreen(BaseManageMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 200;
        this.imageHeight = 170;
    }

    @Override
    protected void init() {
        super.init();
        int lv = menu.level();
        boolean max = lv >= CoreLevels.MAX;
        upgradeButton = Button.builder(
                        Component.literal(max ? "최고 레벨" : "업그레이드 → LV" + (lv + 1)),
                        b -> {
                            if (minecraft != null && minecraft.gameMode != null) {
                                minecraft.gameMode.handleInventoryButtonClick(
                                        menu.containerId, BaseManageMenu.BTN_UPGRADE);
                            }
                        })
                .bounds(leftPos + 20, topPos + imageHeight - 28, imageWidth - 40, 18)
                .build();
        upgradeButton.active = !max && canAfford();
        addRenderableWidget(upgradeButton);
    }

    private boolean canAfford() {
        if (minecraft == null || minecraft.player == null) return false;
        int lv = menu.level();
        CoreLevels.UpgradeCost cost = CoreLevels.upgradeCost(lv);
        if (cost == null) return false;
        if (CurrencyUtil.balance(minecraft.player) < cost.coins()) return false;
        for (CoreLevels.Ingredient ing : cost.ingredients()) {
            if (count(ing) < ing.count()) return false;
        }
        return true;
    }

    private int count(CoreLevels.Ingredient ing) {
        int total = 0;
        if (minecraft == null || minecraft.player == null) return 0;
        for (ItemStack s : minecraft.player.getInventory().items) {
            if (s.is(ing.item())) total += s.getCount();
        }
        return total;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        g.fill(x, y, x + imageWidth, y + imageHeight, 0xFFC6C6C6);
        g.fill(x + 6, y + 14, x + imageWidth - 6, y + 15, 0xFF8B8B8B); // 구분선
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        int lv = menu.level();
        g.drawString(this.font, this.title, 8, 5, 0x404040, false);
        int balance = minecraft != null && minecraft.player != null
                ? CurrencyUtil.balance(minecraft.player) : 0;
        Component bal = Component.literal("잔액 " + balance + " 코인");
        g.drawString(this.font, bal, imageWidth - 8 - this.font.width(bal), 5, 0x705000, false);

        int ty = 22;
        line(g, "현재 레벨: §0LV" + lv, ty);
        line(g, "블록 내구: §0" + CoreLevels.blockDurability(lv), ty += 11);
        line(g, "보안칸: §0" + CoreLevels.securitySlots(lv) + " §7(M-Inv 후속)", ty += 11);
        line(g, "도구 효율 해금: §0" + CoreLevels.toolEfficiency(lv), ty += 11);

        ty += 8;
        if (lv >= CoreLevels.MAX) {
            line(g, "§6최고 레벨입니다", ty);
            return;
        }
        CoreLevels.UpgradeCost cost = CoreLevels.upgradeCost(lv);
        line(g, "§8다음 (LV" + (lv + 1) + ") 비용:", ty);
        ty += 11;
        // 화폐
        boolean coinOk = balance >= cost.coins();
        line(g, (coinOk ? "§2" : "§4") + "· 코인 " + cost.coins() + " §7(보유 " + balance + ")", ty);
        // 재료
        for (CoreLevels.Ingredient ing : cost.ingredients()) {
            ty += 11;
            int have = count(ing);
            boolean ok = have >= ing.count();
            String name = new ItemStack(ing.item()).getHoverName().getString();
            line(g, (ok ? "§2" : "§4") + "· " + name + " " + ing.count() + " §7(보유 " + have + ")", ty);
        }
    }

    private void line(GuiGraphics g, String text, int y) {
        g.drawString(this.font, Component.literal(text), 10, y, 0x303030, false);
    }
}
