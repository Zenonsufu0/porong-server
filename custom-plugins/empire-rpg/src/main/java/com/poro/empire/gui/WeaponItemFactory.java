package com.poro.empire.gui;

import com.poro.empire.combat.weapon.WeaponType;
import com.poro.empire.combat.weapon.WeaponTypeResolver;
import com.poro.empire.growth.engine.ItemGrade;
import com.poro.empire.growth.engine.PlayerEquipmentItem;
import com.poro.empire.growth.engine.PlayerGrowthState;
import com.poro.empire.growth.engine.PotentialGrade;
import com.poro.empire.growth.engine.PotentialLine;
import com.poro.empire.growth.engine.PotentialProfile;
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
        item.setItemMeta(meta);
        return item;
    }

    /** 무기 변경 GUI와 동일 형식의 lore (강화/등급/잠재/세부스탯/각인). */
    public static List<Component> buildLore(PlayerGrowthState state, WeaponType wt) {
        List<Component> lore = new ArrayList<>();
        lore.add(legacy("§7──────────────────"));

        PlayerEquipmentItem eq = state.inventoryItem(WeaponGui.weaponInstanceId(wt)).orElse(null);
        if (eq != null) {
            lore.add(legacy("§7강화   : §e+" + eq.enhanceLevel() + "강"));
            lore.add(legacy("§7등급   : " + gradeColor(eq.grade()) + eq.grade().displayName()));
            PotentialProfile pp = eq.potentialProfile();
            if (pp != null && !pp.lines().isEmpty()) {
                lore.add(legacy("§7잠재   : " + gradeColor(pp.grade()) + pp.grade().name()
                        + " §7(" + pp.lines().size() + "라인)"));
            } else {
                lore.add(legacy("§7잠재   : §8없음"));
            }
            List<PotentialLine> sub = eq.substatLines();
            lore.add(legacy("§7세부스탯: " + (sub.isEmpty() ? "§8없음" : "§f" + sub.size() + "줄")));
        } else {
            lore.add(legacy("§8장착 장비 없음"));
        }

        String eid = state.classEngravingId();
        lore.add(legacy("§7각인   : " + (eid != null && !eid.isBlank() ? "§a" + eid : "§8없음")));
        lore.add(legacy("§7──────────────────"));
        return lore;
    }

    private static String gradeColor(ItemGrade g) {
        return switch (g) {
            case COMMON -> "§7";
            case RARE   -> "§9";
            case EPIC   -> "§5";
            case UNIQUE -> "§6";
            case LEGENDARY -> "§c";
        };
    }

    private static String gradeColor(PotentialGrade g) {
        return switch (g) {
            case COMMON -> "§7";
            case RARE   -> "§9";
            case EPIC   -> "§5";
            case UNIQUE -> "§6";
            case LEGENDARY -> "§c";
        };
    }

    private static Component legacy(String s) {
        return LegacyComponentSerializer.legacySection().deserialize(s)
                .decoration(TextDecoration.ITALIC, false);
    }
}
