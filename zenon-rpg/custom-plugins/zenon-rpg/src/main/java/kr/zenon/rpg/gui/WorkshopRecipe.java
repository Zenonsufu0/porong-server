package kr.zenon.rpg.gui;

import org.bukkit.Material;
import java.util.List;

/** 공방 제작 레시피 데이터. */
public record WorkshopRecipe(
        String recipeId,
        String displayName,
        Material guiIcon,
        String resultItemId,
        int resultAmount,
        int durationMinutes,
        List<RecipeMaterial> materials
) {
    public record RecipeMaterial(String itemId, long amount) {
        /** 바닐라 Bukkit Material이면 true (플레이어 인벤토리에서 차감) */
        public boolean isVanilla() {
            return Material.matchMaterial(itemId.toUpperCase()) != null;
        }

        public Material asMaterial() {
            return Material.matchMaterial(itemId.toUpperCase());
        }
    }
}
