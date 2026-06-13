package com.poro.rpg.combat;

public final class SkillKeys {
    // Sword (검세 스택)
    public static final String SWORD_FLASH_SLASH   = "sword:flash_slash";
    public static final String SWORD_TRIPLE_STRIKE = "sword:triple_strike";
    public static final String SWORD_GUARD_COUNTER = "sword:guard_counter";
    public static final String SWORD_FINAL_STRIKE  = "sword:final_strike";

    // Axe (충격 스택)
    public static final String AXE_SMASH         = "axe:smash";
    public static final String AXE_CRUSH_CHARGE  = "axe:crush_charge";
    public static final String AXE_UNYIELDING    = "axe:unyielding";
    public static final String AXE_COLOSSAL_DROP = "axe:colossal_drop";

    // Spear (압박 스택)
    public static final String SPEAR_THRUST       = "spear:thrust";
    public static final String SPEAR_CRESCENT     = "spear:crescent";
    public static final String SPEAR_CHARGE       = "spear:charge";
    public static final String SPEAR_THUNDERSTRIKE = "spear:thunderstrike";

    // Crossbow (명중 스택)
    public static final String CROSSBOW_RAPID_FIRE  = "crossbow:rapid_fire";
    public static final String CROSSBOW_EVADE_FIRE  = "crossbow:evade_fire";
    public static final String CROSSBOW_PIERCE_BOLT = "crossbow:pierce_bolt";
    public static final String CROSSBOW_SNIPER      = "crossbow:sniper";

    // Scythe (그림자흐름 스택)
    public static final String SCYTHE_DEATH_SLASH  = "scythe:death_slash";
    public static final String SCYTHE_SHADOW_SPIN  = "scythe:shadow_spin";
    public static final String SCYTHE_GRIM_STRIKE  = "scythe:grim_strike";
    public static final String SCYTHE_EXECUTION    = "scythe:execution";

    // Staff (마력충전 스택)
    public static final String STAFF_ARCANE_ORB      = "staff:arcane_orb";
    public static final String STAFF_ELEMENTAL_BURST = "staff:elemental_burst";
    public static final String STAFF_ARCANE_RUSH     = "staff:arcane_rush";
    public static final String STAFF_STARBURST       = "staff:starburst";

    private SkillKeys() {
        throw new UnsupportedOperationException("Utility class");
    }
}
