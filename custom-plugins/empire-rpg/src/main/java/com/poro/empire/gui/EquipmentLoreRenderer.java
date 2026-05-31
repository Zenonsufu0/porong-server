package com.poro.empire.gui;

import com.poro.empire.combat.WeaponPowerCalculator;
import com.poro.empire.growth.engine.EngravingRegistry;
import com.poro.empire.growth.engine.ItemGrade;
import com.poro.empire.growth.engine.PlayerEquipmentItem;
import com.poro.empire.growth.engine.PotentialGrade;
import com.poro.empire.growth.engine.PotentialLine;
import com.poro.empire.growth.engine.PotentialProfile;

import java.util.ArrayList;
import java.util.List;

/**
 * 장비 lore 정본 렌더러 — 강화/잠재/전승/캐릭터/상세 GUI, 무기 변경 GUI 아이콘,
 * 손에 든 무기({@link WeaponItemFactory})가 모두 이 클래스를 통해 동일한 형식·한글 표기를 사용한다.
 *
 * <p>DL-109 후속: 같은 장비 lore가 세 곳에 복제돼 잠재 등급/각인이 제각각(영어 ID, "(N라인)")
 * 으로 갈라졌던 문제를 단일 정본으로 통합한다. {@code §}색상 코드 문자열을 반환하므로
 * Bukkit 인벤토리 lore(legacy)와 Adventure Component(legacy 역직렬화) 양쪽에서 재사용된다.</p>
 */
public final class EquipmentLoreRenderer {

    private static EngravingRegistry engravingRegistry;

    private EquipmentLoreRenderer() {}

    /** 플러그인 enable 시 1회 주입. 각인 ID → 한글명 조회에 사용된다. */
    public static void init(EngravingRegistry registry) {
        engravingRegistry = registry;
    }

    /**
     * 장비 정본 lore — 구분선 / 강화 / 등급 / 잠재(+옵션 라인) / 세부스탯 / 각인(무기만) / 구분선.
     *
     * @param item        장착 인스턴스(없으면 "장착 장비 없음")
     * @param isWeapon    무기 슬롯이면 각인 줄을 추가
     * @param engravingId 표시할 클래스 각인 ID(무기일 때만 사용)
     */
    public static List<String> baseLore(PlayerEquipmentItem item, boolean isWeapon, String engravingId) {
        return baseLore(item, isWeapon, engravingId, "§8장착 장비 없음");
    }

    /**
     * 빈 슬롯 라벨을 지정하는 변형 — 무기 변경 GUI는 미장착 무기를 "장착 장비 없음" 대신
     * "미육성(교체 시 +0강 생성)"으로 안내한다.
     */
    public static List<String> baseLore(PlayerEquipmentItem item, boolean isWeapon, String engravingId, String emptyLabel) {
        List<String> lore = new ArrayList<>();
        lore.add("§7──────────────────");
        if (item != null) {
            // 무기는 강화 단계별 고정 ATK 보너스(WeaponPowerCalculator) 병기 — 강화 체감 가시화(DL-112).
            // 방어구 DEF 보너스는 정본/계산 미구현이라 별도 작업으로 분리.
            if (isWeapon) {
                int atkBonus = WeaponPowerCalculator.enhanceAtkBonus(item.enhanceLevel());
                lore.add("§7강화   : §e+" + item.enhanceLevel() + "강"
                        + (atkBonus > 0 ? " §7(공격력 §c+" + atkBonus + "§7)" : ""));
            } else {
                lore.add("§7강화   : §e+" + item.enhanceLevel() + "강");
            }
            lore.add("§7등급   : " + gradeColor(item.grade()) + item.grade().displayName());
            PotentialProfile pp = item.potentialProfile();
            if (pp != null && !pp.lines().isEmpty()) {
                lore.add("§7잠재   : " + gradeColor(pp.grade()) + potentialGradeKr(pp.grade()));
                for (PotentialLine pl : pp.lines()) {
                    lore.add("§8  " + potentialOptionKr(pl.optionCode()) + " §e+" + String.format("%.2f", pl.value()));
                }
            } else {
                lore.add("§7잠재   : §8없음");
            }
            List<PotentialLine> sub = item.substatLines();
            lore.add("§7세부스탯: " + (sub.isEmpty() ? "§8없음" : "§f" + sub.size() + "줄"));
        } else {
            lore.add(emptyLabel);
        }
        if (isWeapon) {
            lore.add("§7각인   : " + engravingDisplayName(engravingId));
        }
        lore.add("§7──────────────────");
        return lore;
    }

    /** 각인 ID → 한글 표시명. 미부여/미발견 시 "§8없음"/원문. */
    public static String engravingDisplayName(String engravingId) {
        if (engravingId == null || engravingId.isBlank()) return "§8없음";
        if (engravingRegistry == null) return "§a" + engravingId;
        return engravingRegistry.find(engravingId)
                .map(em -> "§a" + em.engravingName())
                .orElse("§a" + engravingId);
    }

    /** 각인 ID → 색상 없는 한글명. 미부여 시 "", 미발견 시 원문. 스코어보드 등 자체 색상 적용처용. */
    public static String engravingNameOrEmpty(String engravingId) {
        if (engravingId == null || engravingId.isBlank()) return "";
        if (engravingRegistry == null) return engravingId;
        return engravingRegistry.find(engravingId).map(em -> em.engravingName()).orElse(engravingId);
    }

    /** 잠재 옵션 코드 → 한글명. 미정의 코드는 원문 표시. */
    public static String potentialOptionKr(String code) {
        return switch (code) {
            case "attack_percent"                -> "공격력";
            case "general_damage_increase"       -> "일반 피해 증가";
            case "boss_damage_increase"          -> "보스 피해 증가";
            case "combo_tag_damage_increase"     -> "연계 피해 증가";
            case "core_tag_damage_increase"      -> "코어 피해 증가";
            case "precision_tag_damage_increase" -> "정밀 피해 증가";
            case "mark_target_damage_increase"   -> "표식 대상 피해 증가";
            case "conditional_damage_bonus"      -> "조건부 피해";
            case "max_hp_percent"                -> "최대 체력";
            case "damage_reduction"              -> "피해 감소";
            case "move_speed_percent"            -> "이동 속도";
            case "crack_efficiency"              -> "균열 효율";
            case "recovery_efficiency"           -> "회복 효율";
            case "shield_efficiency"             -> "보호막 효율";
            case "resonance_effect_up"           -> "공명 효과";
            case "resource_gain_percent"         -> "자원 획득";
            case "status_resistance_percent"     -> "상태이상 저항";
            case "survival_trigger_reduction"    -> "생존기 쿨감";
            case "playstyle_completion_boost"    -> "플레이스타일 완성";
            default                              -> code;
        };
    }

    /** 잠재 등급 한글명. */
    public static String potentialGradeKr(PotentialGrade g) {
        return switch (g) {
            case COMMON    -> "커먼";
            case RARE      -> "레어";
            case EPIC      -> "에픽";
            case UNIQUE    -> "유니크";
            case LEGENDARY -> "전설";
        };
    }

    public static String gradeColor(PotentialGrade grade) {
        return switch (grade) {
            case COMMON    -> "§7";
            case RARE      -> "§9";
            case EPIC      -> "§5";
            case UNIQUE    -> "§6";
            case LEGENDARY -> "§c";
        };
    }

    public static String gradeColor(ItemGrade grade) {
        return switch (grade) {
            case COMMON    -> "§7";
            case RARE      -> "§9";
            case EPIC      -> "§5";
            case UNIQUE    -> "§6";
            case LEGENDARY -> "§c";
        };
    }
}
