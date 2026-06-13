package kr.zenon.moncore.item;

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
 * 포로공학 정수 (결정 033-a / 034). 포켓몬에 우클릭 → 그 포켓몬 영구 해제(소모).
 * 리그패스와 동일 패턴(paper + CUSTOM_DATA 태그)으로 신규 아이템 등록 없이 식별.
 * 2종: {@link Kind#TECH}(기술머신=off-learnset 기술 각인) / {@link Kind#ABILITY}(특성=임의 강제 부여).
 */
public final class MakeoverStone {
    private MakeoverStone() {}

    /** 정수 종류. 태그/모델(CMD)/이름/lore가 종류별로 다르다. */
    public enum Kind {
        TECH("zenonmoncore_makeover_stone", 82030, "포로공학 정수 · 기술머신", List.of(
                "포로공학의 정수이다",
                "모든 기술머신을 배울 수 있게 된다")),
        ABILITY("zenonmoncore_ability_stone", 82031, "포로공학 정수 · 특성", List.of(
                "포로공학의 정수이다",
                "어떤 특성이든 새길 수 있게 된다"));

        public final String tagKey;
        public final int customModelData;
        public final String displayName;
        public final List<String> flavor;

        Kind(String tagKey, int cmd, String displayName, List<String> flavor) {
            this.tagKey = tagKey;
            this.customModelData = cmd;
            this.displayName = displayName;
            this.flavor = flavor;
        }
    }

    public static ItemStack create(Kind kind) {
        ItemStack stack = new ItemStack(Items.PAPER);
        NbtCompound tag = new NbtCompound();
        tag.putByte(kind.tagKey, (byte) 1);
        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(tag));
        stack.set(DataComponentTypes.CUSTOM_MODEL_DATA,
                new net.minecraft.component.type.CustomModelDataComponent(kind.customModelData));
        stack.set(DataComponentTypes.CUSTOM_NAME,
                Text.literal(kind.displayName).formatted(Formatting.LIGHT_PURPLE, Formatting.BOLD));
        java.util.List<Text> lore = new java.util.ArrayList<>();
        for (String f : kind.flavor) lore.add(Text.literal(f).formatted(Formatting.AQUA, Formatting.ITALIC));
        lore.add(Text.literal("포켓몬에 우클릭 — 그 포켓몬 영구 해제").formatted(Formatting.GRAY));
        lore.add(Text.literal("사용 시 소모").formatted(Formatting.DARK_GRAY));
        stack.set(DataComponentTypes.LORE, new LoreComponent(lore));
        return stack;
    }

    /** 이 아이템이 어떤 정수인지(아니면 null). */
    public static Kind kindOf(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        NbtComponent comp = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (comp == null) return null;
        for (Kind k : Kind.values()) if (comp.contains(k.tagKey)) return k;
        return null;
    }

    /** 정수(기술/특성 무관) 여부. */
    public static boolean isStone(ItemStack stack) {
        return kindOf(stack) != null;
    }
}
