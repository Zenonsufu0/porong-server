package com.porong.gun.client;

import com.porong.gun.inv.BackpackProtoMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/**
 * 인벤 GUI 프로토타입 화면 — 3대 유리 레이어를 슬롯 위에 오버레이 렌더.
 *
 * 🔴 핵심 리스크 검증: "표준 슬롯 렌더 위에 반투명 유리를 겹쳐 그릴 수 있는가",
 * "TaCZ 총 같은 복잡한 아이템도 커스텀 슬롯에서 정상 렌더/이동되는가".
 *
 * 오버레이는 {@link #renderSlot}에서 아이템 위(z=300)에 그린다(아이템 z≈250, 툴팁 z≈400).
 * 배경 패널·슬롯 프레임은 텍스처 없이 {@link #renderBg}에서 직접 그린다(에셋 0).
 */
public class BackpackProtoScreen extends AbstractContainerScreen<BackpackProtoMenu> {

    private static final int COL_BLOCKED = 0x99A8C8FF; // 무게막 = 푸른 유리
    private static final int COL_SECURE  = 0xAA555555; // 보안칸 = 회색 봉인 유리(진하게)
    private static final int COL_CONCEAL = 0xD0202020; // 검사 가림 = 짙은 유리
    private static final int COL_SEC_FRAME = 0xFFD4AF37; // 보안칸 금색 테두리
    private static final int COL_SEC_FLOOR = 0xFF6B6B55; // 보안칸 어두운 바닥

    private boolean inspectMode = false;

    public BackpackProtoScreen(BackpackProtoMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 156;
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(Button.builder(Component.literal("검사 모드: 끔"), b -> {
            inspectMode = !inspectMode;
            b.setMessage(Component.literal("검사 모드: " + (inspectMode ? "켬" : "끔")));
        }).bounds(leftPos + imageWidth + 6, topPos + 20, 96, 20).build());
    }

    /** 배경 패널 + 슬롯 프레임(텍스처 없이 직접 그림). 비변환 좌표(leftPos/topPos 가산). */
    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        g.fill(x, y, x + imageWidth, y + imageHeight, 0xFFC6C6C6);
        for (Slot s : menu.slots) {
            int sx = x + s.x, sy = y + s.y;
            boolean sec = menu.isSecure(s);
            g.fill(sx - 1, sy - 1, sx + 17, sy + 17, sec ? COL_SEC_FRAME : 0xFF373737); // 보안=금테
            g.fill(sx, sy, sx + 16, sy + 16, sec ? COL_SEC_FLOOR : 0xFF8B8B8B);          // 보안=어두운 바닥
            if (sec && !s.hasItem()) {
                drawLock(g, sx, sy); // 빈 보안칸 = 자물쇠 아이콘
            }
        }
    }

    /** 자물쇠 아이콘(에셋 없이 픽셀로). 빈 보안칸에 표시. */
    private void drawLock(GuiGraphics g, int sx, int sy) {
        int body = 0xFFC8A028;  // 금색 몸통
        int dark = 0xFF3A2A00;  // 외곽/열쇠구멍
        // 고리(shackle, U자)
        g.fill(sx + 6, sy + 3, sx + 10, sy + 5, body);
        g.fill(sx + 5, sy + 4, sx + 6, sy + 8, body);
        g.fill(sx + 10, sy + 4, sx + 11, sy + 8, body);
        // 몸통(box)
        g.fill(sx + 4, sy + 8, sx + 12, sy + 14, body);
        g.fill(sx + 4, sy + 8, sx + 12, sy + 9, dark);  // 위 그림자선
        // 열쇠구멍
        g.fill(sx + 7, sy + 10, sx + 9, sy + 13, dark);
    }

    /**
     * 유리 오버레이를 슬롯 위에 렌더.
     *
     * renderSlot이 이 매핑에서 private이라, 슬롯 아이템이 모두 그려진 뒤·툴팁 전에 호출되는
     * renderLabels에서 그린다(좌표는 leftPos/topPos로 변환된 상태 = slot.x/slot.y 직접 사용).
     * z를 올려(translate 300) 아이템 위에 겹친다.
     */
    private void renderGlassLayers(GuiGraphics g) {
        for (Slot slot : menu.slots) {
            if (menu.isBlocked(slot)) {
                overlay(g, slot.x, slot.y, COL_BLOCKED); // 무게막(빈 칸)
                continue;
            }
            if (menu.isSecure(slot)) {
                overlay(g, slot.x, slot.y, COL_SECURE);  // 보안칸 봉인
            }
            boolean conceal = inspectMode && menu.isBackpack(slot) && !menu.isSecure(slot)
                    && this.hoveredSlot != slot;
            if (conceal && slot.hasItem()) {
                overlay(g, slot.x, slot.y, COL_CONCEAL); // 검사 전 가림(보안칸 제외, 마우스오버 해제)
            }
        }
    }

    private void overlay(GuiGraphics g, int sx, int sy, int color) {
        g.pose().pushPose();
        g.pose().translate(0, 0, 300); // 아이템 위로
        g.fill(sx, sy, sx + 16, sy + 16, color);
        g.pose().popPose();
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(this.font, this.title, 8, 6, 0x404040, false);
        g.drawString(this.font,
                Component.literal("푸른=무게(왼쪽) · 회색=보안(오른쪽) · 검사=가림"),
                8, 58, 0x808080, false);
        renderGlassLayers(g); // 슬롯 아이템 위에 유리 오버레이
    }
}
