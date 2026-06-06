package kr.poro.poromoncore.shop;

/** 상점 GUI 공용 레이아웃(6×9). 내용 슬롯 = 테두리 제외 가운데 4×7(28칸). */
public final class ShopLayout {
    private ShopLayout() {}

    public static final int BALANCE_SLOT = 4;   // 상단 중앙: 잔액 표시
    public static final int BACK_SLOT = 49;     // 하단 중앙: 메인 메뉴로
    public static final int SELL_ALL_SLOT = 53; // (매입소) 전부 팔기

    /** 내용 슬롯(아이템 진열) — row 1..4 × col 1..7. */
    public static final int[] CONTENT_SLOTS = buildContentSlots();

    private static int[] buildContentSlots() {
        int[] slots = new int[4 * 7];
        int idx = 0;
        for (int row = 1; row <= 4; row++) {
            for (int col = 1; col <= 7; col++) {
                slots[idx++] = row * 9 + col;
            }
        }
        return slots;
    }

    /** 내용 슬롯 인덱스 → 진열 순번(없으면 -1). */
    public static int contentIndexOf(int slot) {
        for (int i = 0; i < CONTENT_SLOTS.length; i++) {
            if (CONTENT_SLOTS[i] == slot) return i;
        }
        return -1;
    }
}
