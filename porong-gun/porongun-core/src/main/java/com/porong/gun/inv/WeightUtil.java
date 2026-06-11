package com.porong.gun.inv;

import net.minecraft.world.item.ItemStack;

/**
 * 아이템 무게(= 점유 칸 수) 유틸.
 *
 * survival.md 「무게 = 점유 칸 시스템」: 아이템 lore/태그에 무게(점유 칸수)를 부여하고,
 * 배낭에 넣으면 그만큼 칸을 점유한다. 초과분(자기 1칸 외 N-1칸)은 유리로 막힌다.
 * 예) 대물저격총 무게 3 → 자기 1칸 + 유리 2칸 = 배낭 3칸 점유.
 *
 * 프로토타입에선 NBT 태그(int) 한 개로 무게를 읽는다. 실제 구현에선
 * TaCZ 총/부착물 아이템에 동일 태그를 얹어(또는 아이템별 config 매핑) 재사용한다.
 */
public final class WeightUtil {

    /** ItemStack NBT 무게 키. */
    public static final String TAG = "porongun_weight";

    private WeightUtil() {}

    /** 점유 칸 수. 빈 칸=0, 태그 없으면 기본 1칸. */
    public static int weightOf(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return 0;
        }
        var tag = stack.getTag();
        if (tag != null && tag.contains(TAG)) {
            return Math.max(1, tag.getInt(TAG));
        }
        return 1;
    }

    /** 무게 태그 설정(프로토타입 검증용 — /porongun setweight). */
    public static ItemStack setWeight(ItemStack stack, int weight) {
        stack.getOrCreateTag().putInt(TAG, Math.max(1, weight));
        return stack;
    }
}
