package kr.zenon.rpg.combat;

import java.util.Locale;
import java.util.Map;

/**
 * 스킬 입력 유형 — 잠재 "스킬타입 피해%" 4종 매핑용 (정본 potential_options_v1 §1, DL-129 2단계).
 *
 * <p>입력 슬롯 정의(정본): 기본기=좌클릭(LC) / 이동기=우클릭(RC) / 특수기=Shift+우클릭(SRC) / 핵심기=F.
 * 스킬 키가 어느 슬롯인지는 {@code SkillInputListener}의 slot1~4 테이블이 결정론적으로 정한다.
 * {@link #fromKey(String)}의 키 매핑은 그 테이블과 <b>반드시 동기</b> 상태를 유지해야 한다.</p>
 */
public enum SkillType {
    BASIC("basic_skill_damage"),       // slot1 LC
    MOBILITY("mobility_skill_damage"), // slot2 RC
    SPECIAL("special_skill_damage"),   // slot3 SRC
    CORE("core_skill_damage");         // slot4 F

    private final String optionCode;

    SkillType(String optionCode) {
        this.optionCode = optionCode;
    }

    /** 잠재 옵션 코드 (CSV option_code / sumEquippedPotential 키). */
    public String optionCode() {
        return optionCode;
    }

    // SkillInputListener.slot1Key~slot4Key 와 동기. 6무기 × 4슬롯 = 24키.
    private static final Map<String, SkillType> BY_KEY = Map.ofEntries(
            // slot1 기본기 (LC)
            Map.entry("sword:flash_slash", BASIC),
            Map.entry("axe:smash", BASIC),
            Map.entry("spear:thrust", BASIC),
            Map.entry("crossbow:rapid_fire", BASIC),
            Map.entry("scythe:death_slash", BASIC),
            Map.entry("staff:arcane_orb", BASIC),
            // slot2 이동기 (RC)
            Map.entry("sword:triple_strike", MOBILITY),
            Map.entry("axe:crush_charge", MOBILITY),
            Map.entry("spear:crescent", MOBILITY),
            Map.entry("crossbow:evade_fire", MOBILITY),
            Map.entry("scythe:shadow_spin", MOBILITY),
            Map.entry("staff:elemental_burst", MOBILITY),
            // slot3 특수기 (SRC)
            Map.entry("sword:guard_counter", SPECIAL),
            Map.entry("axe:unyielding", SPECIAL),
            Map.entry("spear:charge", SPECIAL),
            Map.entry("crossbow:pierce_bolt", SPECIAL),
            Map.entry("scythe:grim_strike", SPECIAL),
            Map.entry("staff:arcane_rush", SPECIAL),
            // slot4 핵심기 (F)
            Map.entry("sword:final_strike", CORE),
            Map.entry("axe:colossal_drop", CORE),
            Map.entry("spear:thunderstrike", CORE),
            Map.entry("crossbow:sniper", CORE),
            Map.entry("scythe:execution", CORE),
            Map.entry("staff:starburst", CORE)
    );

    /** 스킬 키 → 입력 유형. 미등록 키(예: 평타·내부 스킬)는 null → 스킬타입 배율 미적용. */
    public static SkillType fromKey(String key) {
        if (key == null) {
            return null;
        }
        return BY_KEY.get(key.toLowerCase(Locale.ROOT));
    }
}
