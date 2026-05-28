package com.poro.empire.gui;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;

public final class GuiTitles {
    private GuiTitles() {}

    private static final Key    GUI_FONT   = Key.key("poro", "gui");
    // U+F800 = poro:gui space font -176px 이동 (인벤토리 폭 전체 복귀)
    private static final String SHIFT_FULL = "";

    /** 배경 PNG 글리프 + 커서 복귀 + 타이틀 텍스트 Component */
    private static Component hubTitle(char bgGlyph, String text) {
        return Component.text(bgGlyph + SHIFT_FULL).font(GUI_FONT)
                .append(Component.text(text));
    }

    public static final Component WEAPON_SELECTION = Component.text("클래스 선택");
    // poro:gui 글리프: =menu_main  =menu_equipment  =menu_territory  =menu_boss
    public static final Component MAIN_HUB         = hubTitle('', "제국의 거점");
    public static final Component EQUIPMENT_HUB    = hubTitle('', "장비 관리");
    public static final Component TERRITORY_HUB    = hubTitle('', "영지 관리");
    public static final Component BOSS_HUB         = hubTitle('', "보스 도전");
    public static final Component EXPLORE_HUB      = Component.text("탐험 지도");
    public static final Component STORAGE          = Component.text("영지 저장고");
    public static final Component TERRITORY_STATUS = Component.text("영지 상태");
    public static final Component WORKSHOP         = Component.text("공방");
    public static final Component GROWTH_ENHANCE   = Component.text("강화");
    public static final Component GROWTH_POTENTIAL = Component.text("잠재능력");
    public static final Component GROWTH_HEIRLOOM  = Component.text("전승");
    public static final Component FIELD_HUB        = Component.text("필드 이동");
    public static final Component PARTY_HUB        = Component.text("파티 관리");
    public static final Component PARTY_LIST       = Component.text("파티 목록");
    public static final Component AUCTION          = Component.text("경매장");
    public static final Component AUCTION_REGISTER = Component.text("경매 등록");
    public static final Component AUCTION_MY       = Component.text("내 등록 목록");
}
