package com.porong.gun.client;

import com.porong.gun.shop.CurrencyUtil;
import com.porong.gun.shop.ShopCatalog;
import com.porong.gun.shop.ShopEntry;
import com.porong.gun.shop.ShopMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 상점 화면 (M-Currency). 품목을 행으로 진열하고 **행 클릭**으로 거래한다(클릭식).
 *  - 좌클릭 = 구매(구매 가능 품목만), 우클릭 = 판매(매입 가능 품목만).
 *  - 구매/판매 가격은 행 우측에 표시(🟢사 / 🟡팔), 잔액은 상단.
 *
 * 클릭 → {@code handleInventoryButtonClick(containerId, buttonId)} → 서버
 * {@link ShopMenu#clickMenuButton}. buttonId = index*2 + (구매?0:1).
 */
public class ShopScreen extends AbstractContainerScreen<ShopMenu> {

    private static final int ROW_H = 16;
    private static final int ROW_TOP = 16;
    private static final int ROW_X0 = 4;

    public ShopScreen(ShopMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 200;
        this.imageHeight = 240; // 품목 8행 + 안내 + 인벤 4줄(PLAYER_INV_Y=162)
    }

    private int rowAt(double mx, double my) {
        int relX = (int) mx - leftPos, relY = (int) my - topPos;
        if (relX < ROW_X0 || relX >= imageWidth - ROW_X0) return -1;
        var entries = ShopCatalog.ENTRIES;
        for (int i = 0; i < entries.size(); i++) {
            int ly = ROW_TOP + i * ROW_H;
            if (relY >= ly && relY < ly + ROW_H) return i;
        }
        return -1;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int row = rowAt(mx, my);
        if (row >= 0) {
            ShopEntry e = ShopCatalog.ENTRIES.get(row);
            if (button == 0 && e.canBuy()) click(row * 2);          // 좌클릭 = 구매
            else if (button == 1 && e.canSell()) click(row * 2 + 1); // 우클릭 = 판매
            return true; // 품목 행은 항상 소비(인벤으로 클릭 새지 않게)
        }
        return super.mouseClicked(mx, my, button);
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
        g.fill(x + 4, y + ROW_TOP - 2, x + imageWidth - 4, y + ROW_TOP - 1, 0xFF8B8B8B); // 구분선

        int hover = rowAt(mouseX, mouseY);
        var entries = ShopCatalog.ENTRIES;
        for (int i = 0; i < entries.size(); i++) {
            int ly = y + ROW_TOP + i * ROW_H;
            int bg = (i == hover) ? 0xFFB0B0B0 : (i % 2 == 0 ? 0xFFBDBDBD : 0xFFC6C6C6);
            g.fill(x + ROW_X0, ly, x + imageWidth - ROW_X0, ly + ROW_H - 1, bg);
        }
        for (Slot s : menu.slots) { // 플레이어 인벤 칸
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
        Component bal = Component.literal("잔액 " + balance + " 코인");
        g.drawString(this.font, bal, imageWidth - 8 - this.font.width(bal), 5, 0x705000, false);

        var entries = ShopCatalog.ENTRIES;
        for (int i = 0; i < entries.size(); i++) {
            ShopEntry e = entries.get(i);
            int ly = ROW_TOP + i * ROW_H;
            ItemStack icon = new ItemStack(BuiltInRegistries.ITEM.get(e.item()));
            g.renderItem(icon, 6, ly);
            g.drawString(this.font, icon.getHoverName(), 26, ly + 4, 0x303030, false);

            // 우측 가격: 🟢사 N / 🟡팔 N (불가면 회색 — 표시)
            String price = (e.canBuy() ? "§a사 " + e.buyPrice() : "§8사 X")
                    + " §7/ " + (e.canSell() ? "§e팔 " + e.sellPrice() : "§8팔 X");
            Component pc = Component.literal(price);
            g.drawString(this.font, pc, imageWidth - 8 - this.font.width(pc), ly + 4, 0xFFFFFF, false);
        }

        // 하단 안내
        g.drawString(this.font, Component.literal("§7좌클릭=구매  우클릭=판매"),
                8, ROW_TOP + entries.size() * ROW_H + 2, 0x505050, false);
    }
}
