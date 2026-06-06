package kr.poro.poromoncore.item;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

/**
 * 마개조 해금석 (결정 033): 포켓몬에 우클릭 → off-learnset 기술 1개 각인(소모).
 * 리그패스와 동일 패턴(paper + CUSTOM_DATA 태그)으로 신규 아이템 등록 없이 식별.
 */
public final class MakeoverStone {
    private MakeoverStone() {}

    public static final String TAG_KEY = "poromon_makeover_stone";

    public static ItemStack create() {
        ItemStack stack = new ItemStack(Items.PAPER);
        NbtCompound tag = new NbtCompound();
        tag.putByte(TAG_KEY, (byte) 1);
        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(tag));
        stack.set(DataComponentTypes.CUSTOM_NAME,
                Text.literal("포로공학 해금석").formatted(Formatting.LIGHT_PURPLE, Formatting.BOLD));
        stack.set(DataComponentTypes.LORE, new LoreComponent(List.of(
                Text.literal("포켓몬에 우클릭 — 그 포켓몬 영구 해제").formatted(Formatting.GRAY),
                Text.literal("이후 포로공학에서 배울 수 없는 기술 각인").formatted(Formatting.GRAY),
                Text.literal("사용 시 소모").formatted(Formatting.DARK_GRAY)
        )));
        return stack;
    }

    public static boolean isStone(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        NbtComponent comp = stack.get(DataComponentTypes.CUSTOM_DATA);
        return comp != null && comp.contains(TAG_KEY);
    }
}
