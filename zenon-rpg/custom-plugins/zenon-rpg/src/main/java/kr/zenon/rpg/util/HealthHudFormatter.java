package kr.zenon.rpg.util;

import kr.zenon.rpg.combat.CooldownManager;
import kr.zenon.rpg.combat.ResourceTracker;
import kr.zenon.rpg.combat.weapon.WeaponType;
import kr.zenon.rpg.growth.engine.PlayerGrowthState;
import kr.zenon.rpg.leveling.PlayerLevelingService;
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

    // ─── 픽셀 advance 테이블 (poro:hud 폰트 실측, advance=round(trimmed*height/imgH)+1) ──
    private static final int W_BAR   = 107; // hp/xp 바 (136×9 → height7)
    private static final int W_CDBAR = 45;  // cd 바 (49×9 → height8)
    private static final int W_STACK = 10;  // 스택 아이콘 (9×9)
    private static final int W_SPACE = 4;   // minecraft:default 공백 " " advance
    // 좌측 앵커: 전체 advance를 -ANCHOR로 고정 → 무기 유무 무관 + content 좌단을 화면중앙-(ANCHOR/2)에 둔다.
    // 핫바 폭=182px. 작을수록 오른쪽으로 이동(좌단=중앙-ANCHOR/2). 인게임 튜닝값.
    private static final int LEFT_ANCHOR = 174;

    // 쿨타임 두 열의 2열 시작 X(고정) — 라벨+바를 담아 2x2를 spread. (LC↔RC 간격)
    private static final int CD_COL2_X = 91;
    // 바↔초 간격(px) — "RC와 초 사이" 좁힘.
    private static final int BAR_TIME_GAP = 2;
    // 라벨 칸 고정 폭 — 가장 긴 "SRC"(18px) 기준. 라벨 길이와 무관하게 바 시작 X를 통일한다.
    private static final int LABEL_W = 18;
    // 시간 칸 고정 폭 — 최대 "18s"(18px) 기준. "-"/숫자 모두 동일 폭 → 바·열 위치 불변.
    private static final int TIME_W = 18;
    // 쿨타임 행만 좌측으로 미세 이동(px) — HP/XP·스택은 그대로. 인게임 시각 튜닝값.
    private static final int CD_SHIFT = 3;

    private HealthHudFormatter() {}

    /** 한 행의 컴포넌트와 그 행의 픽셀 폭(되감기에 사용). */
    private record Row(Component comp, int width) {}

    /**
     * 5레이어 HUD Component를 빌드한다.
     * HP/XP는 항상 포함, 쿨타임·스택은 무기 착용 시만.
     *
     * 정렬: 맨 앞 negSpace(-ANCHOR)로 전체 advance를 -ANCHOR에 고정한다.
     * 각 행은 [content][negSpace(-content폭)]이라 net 0 → 다음 행이 같은 X에서 시작(좌우 정렬),
     * 전체 폭이 무기 유무와 무관하게 -ANCHOR로 일정 → 액션바 중앙정렬이 흔들리지 않는다.
     */
    public static Component build(Player player,
                                   CooldownManager cdm,
                                   ResourceTracker rt,
                                   PlayerGrowthState state,
                                   WeaponType wt) {
        Component out = negSpace(LEFT_ANCHOR)
                .append(appendRow(buildHp(player), 0))
                .append(appendRow(buildXp(player, state), 0));

        if (wt != WeaponType.NONE) {
            out = out
                    .append(appendRow(buildCdRow1(player, cdm, wt), CD_SHIFT))
                    .append(appendRow(buildCdRow2(player, cdm, wt), CD_SHIFT))
                    .append(appendRow(buildStack(player, rt, wt, state), 0));
        }
        return out;
    }

    /**
     * 행 content + 그 폭만큼 되감기(net 0). {@code shiftLeft}>0이면 그 행만 좌측으로 이동(net 0 유지).
     * 구조: [neg(shift)][content][neg(폭)][pos(shift)] → 합 0, 내용은 shift만큼 왼쪽.
     */
    private static Component appendRow(Row row, int shiftLeft) {
        return Component.empty()
                .append(negSpace(shiftLeft))
                .append(row.comp())
                .append(negSpace(row.width()))
                .append(posSpace(shiftLeft));
    }

    // ─── row builders ─────────────────────────────────────────────────────

    private static Row buildHp(Player player) {
        double cur = Math.max(0, player.getHealth());
        double max = resolveMax(player);
        int step = Math.min(20, (int) (cur / max * 100) / 5);
        // HP는 정수로 표기 — 소수점(.) 폭 변동으로 바가 밀리던 문제 해소(DL-129 추가#13).
        String t = Math.round(cur) + "/" + Math.round(max);
        Component c = Component.empty()
                .append(glyph(HP_BASE + step))
                .append(txt(" "))
                .append(rowText(HP_TEXT, t));
        return new Row(c, W_BAR + W_SPACE + textW(t));
    }

    private static Row buildXp(Player player, PlayerGrowthState state) {
        int level  = state != null ? state.playerLevel() : player.getLevel();
        long cexp  = state != null ? state.currentExp() : 0L;
        long need  = state != null ? PlayerLevelingService.expToNextLevel(level) : 0L;
        // 바 채움 = 커스텀 레벨링 진행도. 바닐라 XP(player.getExp())는 억제되어 0이라 못 씀.
        float prog = need > 0 ? Math.min(1f, (float) cexp / need) : 0f;
        int step = Math.min(20, (int) (prog * 20));

        String lv = "Lv." + level;
        Component c = Component.empty()
                .append(glyph(XP_BASE + step))
                .append(txt(" "))
                .append(rowText(XP_TEXT, lv));
        int w = W_BAR + W_SPACE + textW(lv);

        // 경험치 수치 (현재/다음레벨) — 큰 수는 K/M 단축표기로 자릿수 고정(바 밀림 방지, DL-129 추가#13).
        if (state != null && need > 0) {
            String exp = abbrev(cexp) + "/" + abbrev(need);
            c = c.append(txt(" ")).append(rowText(XP_TEXT, exp));
            w += W_SPACE + textW(exp);
        }
        return new Row(c, w);
    }

    // 슬롯별 입력 라벨 — slot1=LMB, slot2=RMB, slot3=Shift+RMB, slot4=F (SkillInputListener와 일치)
    private static final String LBL_SLOT1 = "LC", LBL_SLOT2 = "RC", LBL_SLOT3 = "SRC", LBL_SLOT4 = "F";

    private static Row buildCdRow1(Player player, CooldownManager cdm, WeaponType wt) {
        return cdRow(player, cdm, slot1Key(wt), LBL_SLOT1, slot2Key(wt), LBL_SLOT2, CD1_BASE, CD1_TEXT);
    }

    private static Row buildCdRow2(Player player, CooldownManager cdm, WeaponType wt) {
        return cdRow(player, cdm, slot3Key(wt), LBL_SLOT3, slot4Key(wt), LBL_SLOT4, CD2_BASE, CD2_TEXT);
    }

    /** 쿨타임 한 행 = [라벨 열1 엔트리][고정 X까지 pad][라벨 열2 엔트리] — 2x2를 넓게 spread. */
    private static Row cdRow(Player player, CooldownManager cdm,
                             String k1, String l1, String k2, String l2, int glyphBase, int textBase) {
        Row a = cdEntry(player, cdm, k1, l1, glyphBase, textBase);
        Row b = cdEntry(player, cdm, k2, l2, glyphBase, textBase);
        int pad = Math.max(W_SPACE, CD_COL2_X - a.width());   // 2열을 고정 X에서 시작(최소 간격 보장)
        Component c = Component.empty().append(a.comp()).append(posSpace(pad)).append(b.comp());
        return new Row(c, a.width() + pad + b.width());
    }

    private static Row buildStack(Player player, ResourceTracker rt, WeaponType wt,
                                   PlayerGrowthState state) {
        int idx = weaponIdx(wt);
        if (idx < 0) return new Row(Component.empty(), 0);
        char filled = (char) (0xE140 + idx * 2);
        char empty  = (char) (0xE141 + idx * 2);
        int stacks = rt.getStack(player.getUniqueId());
        int max = weaponDefaultStackMax(wt, state);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(stacks, max); i++) sb.append(filled);
        for (int i = stacks; i < max; i++) sb.append(empty);
        String num = stacks + "/" + max;
        Component c = Component.empty()
                .append(Component.text(sb.toString()).font(HUD_FONT))
                .append(txt(" "))
                .append(rowText(SK_TEXT, num));
        return new Row(c, max * W_STACK + W_SPACE + textW(num));
    }

    private static Row cdEntry(Player player, CooldownManager cdm,
                               String key, String label, int glyphBase, int textBase) {
        if (key == null) return new Row(Component.empty(), 0);
        long remaining = cdm.getRemainingMillis(player.getUniqueId(), key);
        long total     = cdm.getTotalMillis(player.getUniqueId(), key);

        int step;
        if (remaining <= 0 || total <= 0) {
            step = 9;
        } else {
            long elapsed = total - remaining;
            step = Math.min(8, (int) (elapsed * 9 / total));
        }

        String timeStr;
        Component timeComp;
        if (remaining <= 0) {
            timeStr = "-";
            timeComp = rowText(textBase, timeStr).color(NamedTextColor.GREEN);
        } else {
            long secs = (long) Math.ceil(remaining / 1000.0);   // 정수 초(올림) — HUD 가독성·고정폭
            timeStr = secs + "s";
            timeComp = rowText(textBase, timeStr)
                    .color(remaining <= 5000 ? NamedTextColor.RED : NamedTextColor.YELLOW);
        }
        // [라벨(고정폭)][바][시간(고정폭)] — 라벨·시간 칸을 모두 고정폭으로 잡아
        // 라벨 길이/쿨타임 상태("-"↔숫자)와 무관하게 바·열 위치를 통일(정렬·밀림 방지).
        Component c = Component.empty()
                .append(rowText(textBase, label).color(NamedTextColor.GRAY))
                .append(posSpace(LABEL_W - textW(label) + W_SPACE))
                .append(glyph(glyphBase + step))
                .append(posSpace(BAR_TIME_GAP))
                .append(timeComp)
                .append(posSpace(TIME_W - textW(timeStr)));
        return new Row(c, LABEL_W + W_SPACE + W_CDBAR + BAR_TIME_GAP + TIME_W);
    }

    // ─── helpers ──────────────────────────────────────────────────────────

    /**
     * 좌측으로 {@code px}만큼 커서를 되감는 negative-space 글리프 조합.
     * poro:hud 폰트의 음수 스페이스(-1,-2,-4,…,-128)를 비트 분해로 합성한다.
     */
    private static Component negSpace(int px) {
        if (px <= 0) return Component.empty();
        int[] v  = {128, 64, 32, 16, 8, 4, 2, 1};
        char[] g = {'', '', '', '', '', '', '', ''};
        StringBuilder sb = new StringBuilder();
        int rem = px;
        for (int i = 0; i < v.length; i++) {
            while (rem >= v[i]) { sb.append(g[i]); rem -= v[i]; }
        }
        return Component.text(sb.toString()).font(HUD_FONT);
    }

    /**
     * 우측으로 {@code px}만큼 커서를 전진시키는 positive-space 글리프 조합.
     * poro:hud 폰트의 양수 스페이스(+15,+6,+3,+2,+1)를 그리디 분해로 합성한다.
     */
    private static Component posSpace(int px) {
        if (px <= 0) return Component.empty();
        int[] v  = {15, 6, 3, 2, 1};
        int[] cp = {0xEF00, 0xEF06, 0xEF03, 0xEF02, 0xEF01};
        StringBuilder sb = new StringBuilder();
        int rem = px;
        for (int i = 0; i < v.length; i++) {
            while (rem >= v[i]) { sb.append((char) cp[i]); rem -= v[i]; }
        }
        return Component.text(sb.toString()).font(HUD_FONT);
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

    /** rowText가 실제로 렌더하는 문자들의 픽셀 폭 합(advance 테이블 기준). */
    private static int textW(String s) {
        int w = 0;
        for (char c : s.toCharArray()) {
            if (charIdx(c) >= 0) w += charW(c);
        }
        return w;
    }

    private static int charW(char c) {
        return switch (c) {
            case ',' -> 3;
            case '.' -> 2;
            default  -> 6;   // 숫자·/·L·v·s·- 등 chars.png 6px advance
        };
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
            case 'C' -> 24; // 스킬 입력 라벨(LC/RC/SRC)용 — chars.png 25번째 칸
            case 'K' -> 25; case 'M' -> 26; // XP K/M 단축표기용 (chars.png 26·27번째 칸, DL-129 추가#13)
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

    /** 큰 수 단축표기 — 1000↑=K, 1,000,000↑=M (소수 1자리, .0 생략). XP 자릿수 고정용. */
    private static String abbrev(long v) {
        if (v >= 1_000_000L) return round1(v / 1_000_000.0) + "M";
        if (v >= 1_000L)     return round1(v / 1_000.0) + "K";
        return Long.toString(v);
    }

    private static String round1(double d) {
        long whole = (long) d;
        long frac  = Math.round((d - whole) * 10);
        if (frac >= 10) { whole++; frac = 0; }
        return frac == 0 ? Long.toString(whole) : whole + "." + frac;
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
