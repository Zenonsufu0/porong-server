package kr.poro.poromoncore.menu;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/** GUI 아이콘 ItemStack 생성 공용 헬퍼 (이름/로어, §-color 코드 지원). */
public final class MenuIcons {
    private MenuIcons() {}

    /** 테두리/빈칸용 회색 유리판(이름 없음). */
    public static ItemStack pane() {
        return icon(Items.GRAY_STAINED_GLASS_PANE, " ", List.of());
    }

    /** CustomModelData를 입힌 아이콘(커스텀 텍스처용 — 리소스/모드 모델 분기). */
    public static ItemStack iconModel(Item item, int customModelData, String name, List<String> loreLines) {
        ItemStack stack = icon(item, name, loreLines);
        stack.set(net.minecraft.component.DataComponentTypes.CUSTOM_MODEL_DATA,
                new net.minecraft.component.type.CustomModelDataComponent(customModelData));
        return stack;
    }

    public static ItemStack icon(Item item, String name, List<String> loreLines) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal(name));
        if (!loreLines.isEmpty()) {
            List<Text> lore = new ArrayList<>(loreLines.size());
            for (String line : loreLines) lore.add(Text.literal(line));
            stack.set(DataComponentTypes.LORE, new LoreComponent(lore));
        }
        return stack;
    }

    /** 수량(count)을 표시한 아이콘(매입 미리보기 등). count는 1~64로 클램프. */
    public static ItemStack iconCount(Item item, int count, String name, List<String> loreLines) {
        ItemStack stack = icon(item, name, loreLines);
        stack.setCount(Math.max(1, Math.min(64, count)));
        return stack;
    }
}
