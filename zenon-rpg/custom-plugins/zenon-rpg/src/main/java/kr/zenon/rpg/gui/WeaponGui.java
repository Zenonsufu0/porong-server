package kr.zenon.rpg.gui;

import kr.zenon.rpg.combat.weapon.WeaponType;
import org.bukkit.Material;

import java.util.Locale;

/**
 * 무기 선택/변경 GUI 공용 레이아웃 — 튜토리얼 최초 선택("클래스 선택")과
 * 캐릭터 GUI "무기 변경"이 같은 36칸 배치를 재사용한다 (docs/04_combat_weapon_skills/index.md §86).
 *
 * <p>슬롯 배치(4행×9열): 1행 검10·도끼13·창16 / 2행 석궁19·낫22·스태프25 / 4행 뒤로27.
 * 컨텍스트에 따라 타이틀과 뒤로가기 버튼 표시 여부만 달라진다.
 *
 * <p>무기별 독립 성장(DL-104): 무기 타입마다 {@link #weaponInstanceId} 인스턴스를 따로 두고,
 * 교체 시 해당 인스턴스를 WEAPON 슬롯에 장착한다. 강화/큐브/잠재는 장착된 인스턴스를 대상으로 하므로
 * 자연히 무기별로 분리된다.
 */
public final class WeaponGui {

    private WeaponGui() {}

    /** GUI 크기(4행). */
    public static final int SIZE = 36;
    /** 뒤로가기 버튼 슬롯(무기 변경 컨텍스트에서만 표시). */
    public static final int BACK_SLOT = 27;

    /** 스펙 슬롯 배치 — 인덱스 = 표시 순서. */
    public static final WeaponType[] ORDER = {
            WeaponType.SWORD, WeaponType.AXE, WeaponType.SPEAR,
            WeaponType.CROSSBOW, WeaponType.SCYTHE, WeaponType.STAFF
    };
    public static final int[] SLOTS = {10, 13, 16, 19, 22, 25};

    /** GUI 슬롯 → 무기 타입 (없으면 null). */
    public static WeaponType slotToWeapon(int slot) {
        for (int i = 0; i < SLOTS.length; i++) {
            if (SLOTS[i] == slot) return ORDER[i];
        }
        return null;
    }

    /** 무기 타입 → GUI 슬롯 (없으면 -1). */
    public static int weaponToSlot(WeaponType wt) {
        for (int i = 0; i < ORDER.length; i++) {
            if (ORDER[i] == wt) return SLOTS[i];
        }
        return -1;
    }

    /** 무기 선택 아이콘 기본 재질 (스펙 §104 표). */
    public static Material iconMaterial(WeaponType wt) {
        return switch (wt) {
            case SWORD    -> Material.NETHERITE_SWORD;
            case AXE      -> Material.NETHERITE_AXE;
            case SPEAR    -> Material.TRIDENT;
            case CROSSBOW -> Material.CROSSBOW;
            case SCYTHE   -> Material.NETHERITE_HOE;
            case STAFF    -> Material.BLAZE_ROD;
            case NONE     -> Material.STICK;
        };
    }

    /** 무기 한글 표시명. */
    public static String displayName(WeaponType wt) {
        return switch (wt) {
            case SWORD    -> "검";
            case AXE      -> "도끼";
            case SPEAR    -> "창";
            case CROSSBOW -> "석궁";
            case SCYTHE   -> "낫";
            case STAFF    -> "스태프";
            case NONE     -> "무기";
        };
    }

    /** 무기별 독립 성장 인스턴스 ID (예: weapon_SCYTHE). */
    public static String weaponInstanceId(WeaponType wt) {
        return "weapon_" + wt.name();
    }

    /** 무기별 스타터 item_master ID (예: t1_scythe_starter). */
    public static String starterItemId(WeaponType wt) {
        return "t1_" + wt.name().toLowerCase(Locale.ROOT) + "_starter";
    }
}
