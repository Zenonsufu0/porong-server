package com.poro.empire.util;

import com.poro.empire.combat.CooldownManager;
import com.poro.empire.combat.ResourceTracker;
import com.poro.empire.combat.weapon.WeaponType;
import com.poro.empire.growth.engine.PlayerGrowthState;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;

public final class HealthHudFormatter {

    private static final Key HUD_FONT     = Key.key("poro", "hud");
    private static final Key DEFAULT_FONT = Key.key("minecraft", "default");

    // ─── 글리프 베이스 (bar 이미지) ───────────────────────────────
    private static final int HP_BASE  = 0xE100; // U+E100~E114 (ascent=-36)
    private static final int XP_BASE  = 0xE150; // U+E150~E164 (ascent=-26)
    private static final int CD1_BASE = 0xE120; // U+E120~E129 Row1 (ascent=-6)
    private static final int CD2_BASE = 0xE130; // U+E130~E139 Row2 (ascent=-16)

    // ─── 행별 텍스트 베이스 (chars.png 각 ascent 버전) ─────────────
    // chars.png 24자 순서: 0~9, /, comma, ., L, v, R, S, F, s, -, E, A, D, Y
    private static final int HP_TEXT  = 0xE200; // ascent=-37
    private static final int XP_TEXT  = 0xE250; // ascent=-27
    private static final int CD1_TEXT = 0xE280; // ascent=-7
    private static final int CD2_TEXT = 0xE2A0; // ascent=-17
    private static final int SK_TEXT  = 0xE2C0; // ascent=+3

    private HealthHudFormatter() {}

    /**
     * 5레이어 HUD Component를 빌드한다.
     * HP/XP는 항상 포함, 쿨타임·스택은 무기 착용 시만.
     */
    public static Component build(Player player,
                                   CooldownManager cdm,
                                   ResourceTracker rt,
                                   PlayerGrowthState state,
                                   WeaponType wt) {
        // 각 행을 overlay: rewind() (-176px)로 커서를 되감아 다음 행이 같은 X에서 시작
        Component out = buildHp(player)
                .append(rewind())
                .append(buildXp(player, state));

        if (wt != WeaponType.NONE) {
            out = out
                    .append(rewind())
                    .append(buildCdRow1(player, cdm, wt))
                    .append(rewind())
                    .append(buildCdRow2(player, cdm, wt))
                    .append(rewind())
                    .append(buildStack(player, rt, wt, state));
        }
        return out;
    }

    // ─── row builders ─────────────────────────────────────────────────────

    private static Component buildHp(Player player) {
        double cur = Math.max(0, player.getHealth());
        double max = resolveMax(player);
        int step = Math.min(20, (int) (cur / max * 100) / 5);
        return Component.empty()
                .append(glyph(HP_BASE + step))
                .append(txt(" "))
                .append(rowText(HP_TEXT, fmt(cur) + "/" + fmt(max)));
    }

    private static Component buildXp(Player player, PlayerGrowthState state) {
        float prog = player.getExp();
        int step = Math.min(20, (int) (prog * 20));
        int level = state != null ? state.playerLevel() : player.getLevel();
        return Component.empty()
                .append(glyph(XP_BASE + step))
                .append(txt(" "))
                .append(rowText(XP_TEXT, "Lv." + level));
    }

    private static Component buildCdRow1(Player player, CooldownManager cdm, WeaponType wt) {
        return Component.empty()
                .append(cdEntry(player, cdm, slot1Key(wt), CD1_BASE, CD1_TEXT))
                .append(txt("  "))
                .append(cdEntry(player, cdm, slot2Key(wt), CD1_BASE, CD1_TEXT));
    }

    private static Component buildCdRow2(Player player, CooldownManager cdm, WeaponType wt) {
        return Component.empty()
                .append(cdEntry(player, cdm, slot3Key(wt), CD2_BASE, CD2_TEXT))
                .append(txt("  "))
                .append(cdEntry(player, cdm, slot4Key(wt), CD2_BASE, CD2_TEXT));
    }

    private static Component buildStack(Player player, ResourceTracker rt, WeaponType wt,
                                         PlayerGrowthState state) {
        int idx = weaponIdx(wt);
        if (idx < 0) return Component.empty();
        char filled = (char) (0xE140 + idx * 2);
        char empty  = (char) (0xE141 + idx * 2);
        int stacks = rt.getStack(player.getUniqueId());
        int max = weaponDefaultStackMax(wt, state);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(stacks, max); i++) sb.append(filled);
        for (int i = stacks; i < max; i++) sb.append(empty);
        return Component.empty()
                .append(Component.text(sb.toString()).font(HUD_FONT))
                .append(txt(" "))
                .append(rowText(SK_TEXT, stacks + "/" + max));
    }

    private static Component cdEntry(Player player, CooldownManager cdm,
                                      String key, int glyphBase, int textBase) {
        if (key == null) return Component.empty();
        long remaining = cdm.getRemainingMillis(player.getUniqueId(), key);
        long total     = cdm.getTotalMillis(player.getUniqueId(), key);

        int step;
        if (remaining <= 0 || total <= 0) {
            step = 9;
        } else {
            long elapsed = total - remaining;
            step = Math.min(8, (int) (elapsed * 9 / total));
        }

        Component timeComp;
        if (remaining <= 0) {
            timeComp = rowText(textBase, "-").color(NamedTextColor.GREEN);
        } else if (remaining <= 5000) {
            timeComp = rowText(textBase, CooldownManager.formatSeconds(remaining) + "s")
                    .color(NamedTextColor.RED);
        } else {
            timeComp = rowText(textBase, CooldownManager.formatSeconds(remaining) + "s")
                    .color(NamedTextColor.YELLOW);
        }
        return Component.empty()
                .append(glyph(glyphBase + step))
                .append(txt(" "))
                .append(timeComp);
    }

    // ─── helpers ──────────────────────────────────────────────────────────

    /** poro:hud 폰트의  = -176px advance로 커서를 행 시작으로 되감는다. */
    private static Component rewind() {
        return Component.text("").font(HUD_FONT);
    }

    /**
     * 문자열을 행 전용 글리프로 변환한다.
     * chars.png 인덱스: 0-9(0~9) /(10) .(12) L(13) v(14) s(18) -(19)
     */
    private static Component rowText(int base, String s) {
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            int idx = charIdx(c);
            if (idx >= 0) sb.append((char) (base + idx));
        }
        return sb.isEmpty() ? Component.empty()
                : Component.text(sb.toString()).font(HUD_FONT);
    }

    private static int charIdx(char c) {
        return switch (c) {
            case '0' -> 0;  case '1' -> 1;  case '2' -> 2;  case '3' -> 3;  case '4' -> 4;
            case '5' -> 5;  case '6' -> 6;  case '7' -> 7;  case '8' -> 8;  case '9' -> 9;
            case '/' -> 10; case ',' -> 11; case '.' -> 12;
            case 'L' -> 13; case 'v' -> 14;
            case 'R' -> 15; case 'S' -> 16; case 'F' -> 17;
            case 's' -> 18; case '-' -> 19;
            case 'E' -> 20; case 'A' -> 21; case 'D' -> 22; case 'Y' -> 23;
            default  -> -1;
        };
    }

    private static Component glyph(int codePoint) {
        return Component.text(String.valueOf((char) codePoint)).font(HUD_FONT);
    }

    private static Component txt(String s) {
        return Component.text(s).font(DEFAULT_FONT);
    }

    private static double resolveMax(Player player) {
        AttributeInstance attr = player.getAttribute(Attribute.MAX_HEALTH);
        return (attr == null || attr.getValue() <= 0) ? 20.0 : attr.getValue();
    }

    private static String fmt(double v) {
        return Math.abs(v - Math.rint(v)) < 0.01 ? Integer.toString((int) v) : String.format("%.1f", v);
    }

    private static int weaponIdx(WeaponType wt) {
        return switch (wt) {
            case SWORD    -> 0;
            case AXE      -> 1;
            case STAFF    -> 2;
            case CROSSBOW -> 3;
            case SCYTHE   -> 4;
            case SPEAR    -> 5;
            case NONE     -> -1;
        };
    }

    private static int weaponDefaultStackMax(WeaponType wt, PlayerGrowthState state) {
        // 유지형 각인(*_retained_01): 무기 공통 최대 6스택
        if (state != null && state.classEngravingId().endsWith("_retained_01")) return 6;
        // 소모형 default: 전 무기 3 통일 (구: 창·지팡이 5)
        return 3;
    }

    // 스킬 슬롯 키 — SkillInputListener와 동일하게 유지
    private static String slot1Key(WeaponType t) {
        return switch (t) {
            case SWORD -> "sword:flash_slash"; case AXE -> "axe:smash";
            case SPEAR -> "spear:thrust"; case CROSSBOW -> "crossbow:rapid_fire";
            case SCYTHE -> "scythe:death_slash"; case STAFF -> "staff:arcane_orb";
            case NONE -> null;
        };
    }

    private static String slot2Key(WeaponType t) {
        return switch (t) {
            case SWORD -> "sword:triple_strike"; case AXE -> "axe:crush_charge";
            case SPEAR -> "spear:crescent"; case CROSSBOW -> "crossbow:evade_fire";
            case SCYTHE -> "scythe:shadow_spin"; case STAFF -> "staff:elemental_burst";
            case NONE -> null;
        };
    }

    private static String slot3Key(WeaponType t) {
        return switch (t) {
            case SWORD -> "sword:guard_counter"; case AXE -> "axe:unyielding";
            case SPEAR -> "spear:charge"; case CROSSBOW -> "crossbow:pierce_bolt";
            case SCYTHE -> "scythe:grim_strike"; case STAFF -> "staff:arcane_rush";
            case NONE -> null;
        };
    }

    private static String slot4Key(WeaponType t) {
        return switch (t) {
            case SWORD -> "sword:final_strike"; case AXE -> "axe:colossal_drop";
            case SPEAR -> "spear:thunderstrike"; case CROSSBOW -> "crossbow:sniper";
            case SCYTHE -> "scythe:execution"; case STAFF -> "staff:starburst";
            case NONE -> null;
        };
    }
}
