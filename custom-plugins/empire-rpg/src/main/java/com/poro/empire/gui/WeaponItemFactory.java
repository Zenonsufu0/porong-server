package com.poro.empire.gui;

import com.poro.empire.combat.weapon.WeaponType;
import com.poro.empire.combat.weapon.WeaponTypeResolver;
import com.poro.empire.growth.engine.PlayerEquipmentItem;
import com.poro.empire.growth.engine.PlayerGrowthState;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

/**
 * 손에 드는(슬롯 0) 물리 무기 아이템 생성 — 무기 변경 GUI와 동일한 lore 형식을 사용한다.
 * 강화/등급/잠재/세부스탯/각인을 무기별 독립 인스턴스({@link WeaponGui#weaponInstanceId}) 기준으로 표시한다.
 */
public final class WeaponItemFactory {

    private WeaponItemFactory() {}

    /** 기본 재질(스펙 §104)로 손에 든 무기를 생성. */
    public static ItemStack build(PlayerGrowthState state, WeaponType wt) {
        return build(state, wt, WeaponGui.iconMaterial(wt));
    }

    /** 지정 재질(치장 등)로 손에 든 무기를 생성. */
    public static ItemStack build(PlayerGrowthState state, WeaponType wt, Material material) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(legacy("§b" + WeaponGui.displayName(wt)));
        meta.lore(buildLore(state, wt));
        if (WeaponTypeResolver.WEAPON_TYPE_KEY != null) {
            meta.getPersistentDataContainer().set(
                    WeaponTypeResolver.WEAPON_TYPE_KEY, PersistentDataType.STRING, wt.name());
        }
        meta.setUnbreakable(true); // 성장 무기는 내구도 소모 없음(데미지는 스킬/평타 ATK 기반)
        meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_UNBREAKABLE, org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * 장비 lore — 정본 렌더러({@link EquipmentLoreRenderer})에 위임하여 GUI 아이콘/무기 변경 GUI와
     * 완전히 동일한 형식·한글 표기(잠재 등급·각인명)를 사용한다. 무기별 독립 인스턴스 기준.
     */
    public static List<Component> buildLore(PlayerGrowthState state, WeaponType wt) {
        PlayerEquipmentItem eq = state.inventoryItem(WeaponGui.weaponInstanceId(wt)).orElse(null);
        // 무기별 독립 각인(DL-110) — 해당 무기 타입(classId)의 각인을 표시.
        String engravingId = state.classEngravingId(wt.name().toLowerCase(java.util.Locale.ROOT));
        List<Component> lore = new ArrayList<>();
        for (String line : EquipmentLoreRenderer.baseLore(eq, true, engravingId)) {
            lore.add(legacy(line));
        }
        return lore;
    }

    private static Component legacy(String s) {
        return LegacyComponentSerializer.legacySection().deserialize(s)
                .decoration(TextDecoration.ITALIC, false);
    }
}
