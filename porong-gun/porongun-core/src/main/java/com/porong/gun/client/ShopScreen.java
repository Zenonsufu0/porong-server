package com.porong.gun.client;

import com.porong.gun.shop.CurrencyUtil;
import com.porong.gun.shop.ShopCatalog;
import com.porong.gun.shop.ShopEntry;
import com.porong.gun.shop.ShopMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 상점 화면 (M-Currency). 품목 목록(아이콘·이름·구매/매입 버튼) + 플레이어 인벤.
 *
 * 버튼 클릭 → {@code handleInventoryButtonClick(containerId, buttonId)} → 서버
 * {@link ShopMenu#clickMenuButton}. 잔액은 클라 인벤에서 계산(거래 후 동기화로 갱신).
 */
public class ShopScreen extends AbstractContainerScreen<ShopMenu> {

    private static final int ROW_H = 14;
    private static final int ROW_TOP = 16;

    public ShopScreen(ShopMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 200;
        this.imageHeight = 220;
    }

    @Override
    protected void init() {
        super.init();
        var entries = ShopCatalog.ENTRIES;
        for (int i = 0; i < entries.size(); i++) {
            ShopEntry e = entries.get(i);
            int y = topPos + ROW_TOP + i * ROW_H;
            final int idx = i;
            if (e.canBuy()) {
                addRenderableWidget(Button.builder(Component.literal("사 " + e.buyPrice()),
                        b -> click(idx * 2)).bounds(leftPos + 124, y, 36, 13).build());
            }
            if (e.canSell()) {
                addRenderableWidget(Button.builder(Component.literal("팔 " + e.sellPrice()),
                        b -> click(idx * 2 + 1)).bounds(leftPos + 163, y, 32, 13).build());
            }
        }
    }

    private void click(int buttonId) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, buttonId);
        }
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        g.fill(x, y, x + imageWidth, y + imageHeight, 0xFFC6C6C6);
        g.fill(x + 4, y + ROW_TOP - 2, x + imageWidth - 4, y + ROW_TOP - 1, 0xFF8B8B8B); // 품목 구분선
        for (Slot s : menu.slots) {
            int sx = x + s.x, sy = y + s.y;
            g.fill(sx - 1, sy - 1, sx + 17, sy + 17, 0xFF373737);
            g.fill(sx, sy, sx + 16, sy + 16, 0xFF8B8B8B);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(this.font, this.title, 8, 5, 0x404040, false);
        int balance = minecraft != null && minecraft.player != null
                ? CurrencyUtil.balance(minecraft.player) : 0;
        g.drawString(this.font, Component.literal("잔액 " + balance + " 코인"), 120, 5, 0x705000, false);

        var entries = ShopCatalog.ENTRIES;
        for (int i = 0; i < entries.size(); i++) {
            ShopEntry e = entries.get(i);
            int ly = ROW_TOP + i * ROW_H;
            ItemStack icon = new ItemStack(BuiltInRegistries.ITEM.get(e.item()));
            g.renderItem(icon, 6, ly - 1);
            g.drawString(this.font, icon.getHoverName(), 26, ly + 2, 0x303030, false);
        }
    }
}
