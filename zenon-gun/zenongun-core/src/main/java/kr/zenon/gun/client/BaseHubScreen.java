package kr.zenon.gun.client;

import kr.zenon.gun.base.BaseHubMenu;
import kr.zenon.gun.registry.ZenonGunItems;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

/**
 * 거점 허브 화면 (코어 우클릭 진입). chest형 칸에 기능 아이콘을 배치하고
 * 칸을 클릭하면 해당 기능({@link BaseHubMenu#clickMenuButton})을 연다.
 *
 * 슬롯이 없는 메뉴라 클릭은 {@link #mouseClicked}에서 셀 히트 판정으로 직접 처리한다.
 */
public class BaseHubScreen extends AbstractContainerScreen<BaseHubMenu> {

    /** 항목 = (아이콘, 라벨). 순서 = BaseHubMenu.ITEM_* 인덱스와 일치. */
    private record Entry(ItemStack icon, String label) {}

    private final List<Entry> entries = List.of(
            new Entry(new ItemStack(Items.EMERALD), "상점"),
            new Entry(new ItemStack(ZenonGunItems.CORE.get()), "기지 관리"),
            new Entry(new ItemStack(Items.BOOK), "정보"));

    private static final int CELL = 32;   // 셀 한 변
    private static final int GAP = 6;
    private static final int GRID_Y = 22; // 셀 윗변(타이틀 아래)

    public BaseHubScreen(BaseHubMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = GAP + BaseHubMenu.ITEM_COUNT * (CELL + GAP);
        this.imageHeight = GRID_Y + CELL + 18; // 셀 + 라벨 여백
    }

    private int cellX(int i) {
        return GAP + i * (CELL + GAP);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int relX = (int) mx - leftPos, relY = (int) my - topPos;
        for (int i = 0; i < entries.size(); i++) {
            int cx = cellX(i);
            if (relX >= cx && relX < cx + CELL && relY >= GRID_Y && relY < GRID_Y + CELL) {
                if (minecraft != null && minecraft.gameMode != null) {
                    minecraft.gameMode.handleInventoryButtonClick(menu.containerId, i);
                }
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        g.fill(x, y, x + imageWidth, y + imageHeight, 0xFFC6C6C6); // 패널
        int hover = hoveredCell(mouseX, mouseY);
        for (int i = 0; i < entries.size(); i++) {
            int cx = x + cellX(i), cy = y + GRID_Y;
            g.fill(cx - 1, cy - 1, cx + CELL + 1, cy + CELL + 1, 0xFF373737); // 테두리
            g.fill(cx, cy, cx + CELL, cy + CELL, i == hover ? 0xFF6A6A6A : 0xFF8B8B8B); // 칸(호버 밝게)
        }
    }

    private int hoveredCell(int mouseX, int mouseY) {
        int relX = mouseX - leftPos, relY = mouseY - topPos;
        for (int i = 0; i < entries.size(); i++) {
            int cx = cellX(i);
            if (relX >= cx && relX < cx + CELL && relY >= GRID_Y && relY < GRID_Y + CELL) return i;
        }
        return -1;
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(this.font, this.title, GAP, 6, 0x404040, false);
        for (int i = 0; i < entries.size(); i++) {
            Entry e = entries.get(i);
            int cx = cellX(i);
            g.renderItem(e.icon(), cx + (CELL - 16) / 2, GRID_Y + (CELL - 16) / 2); // 아이콘 중앙
            int lw = this.font.width(e.label());
            g.drawString(this.font, e.label(), cx + (CELL - lw) / 2, GRID_Y + CELL + 3, 0x303030, false);
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
    }
}
