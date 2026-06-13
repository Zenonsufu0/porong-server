package kr.zenon.moncore.menu;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

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
        return icon(item, Text.literal(name), loreLines);
    }

    /**
     * translatable {@link Text} 이름 아이콘. 모드/포켓몬/기술명의 번역 Text를 그대로 넘기면
     * 클라(ko_kr)가 자동으로 한글 렌더한다(서버 locale 영어 문자열로 박제하지 않음).
     */
    public static ItemStack icon(Item item, Text name, List<String> loreLines) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponentTypes.CUSTOM_NAME, name);
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

    /** 수량 표시 + translatable 이름 아이콘. */
    public static ItemStack iconCount(Item item, int count, Text name, List<String> loreLines) {
        ItemStack stack = icon(item, name, loreLines);
        stack.setCount(Math.max(1, Math.min(64, count)));
        return stack;
    }

    /**
     * 색(Formatting)을 입힌 translatable 이름. 빈 베이스에 색을 주고 번역 Text를 자식으로 붙여
     * 자식이 색을 상속하도록 한다(번역 키는 보존). 추가 텍스트는 반환값에 append.
     */
    public static MutableText named(Formatting color, Text base) {
        return Text.empty().append(base).formatted(color);
    }
}
